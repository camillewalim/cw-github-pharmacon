package cw.github.pharmakon;

import static cw.github.pharmakon.ModelPattern.falsepositives_hints;
import static cw.github.pharmakon.ModelPattern.falsepositives_syntax;
import static cw.github.pharmakon.ModelPattern.positives;
import static cw.github.pharmakon.ModelPattern.sequence_all;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.NullOutputStream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import cw.github.pharmakon.ModelLocal.Credential;
import cw.github.pharmakon.ModelLocal.Failure;
import cw.github.pharmakon.ModelLocal.GitAnalysis;
import cw.github.pharmakon.ModelLocal.GitWrap;
import cw.github.pharmakon.ModelLocal.Hint;
import cw.github.pharmakon.ModelPattern.Sequence_Human;
import cw.github.pharmakon.ModelPattern.Sequence_Oauth;
import cw.github.pharmakon.ModelPattern.Target;
import lombok.AllArgsConstructor;

/**
 * @author camille.walim
 * Let you detect potential hints, credentials & failures.
 */
@AllArgsConstructor
public class DoDetect {
	
	String gitserver, oauthserver;
	File folder;
	
	
	public GitAnalysis detect(List<GitWrap> gits) {
		
		List<Failure> failures = new ArrayList<>();
		List<Credential> credentials = new ArrayList<>();
		
		gits.parallelStream()
			.forEach(git  -> { try {
				
				get_report(git);
				Map<String,Multimap<String,Hint>> hints = get_hints(git);

				String repo_name = Optional
					.of(git.getGit().getRepository().getDirectory())
					.map(r -> r.getParentFile().getParentFile().getName() + "/" + r.getParentFile().getName())
					.get();	
				
				List<Failure> temp_failures = get_failures(git, repo_name, hints);
				
				failures.addAll(temp_failures);
				credentials.addAll(get_credentials(git, failures));
				
				util_save_txt(repo_name, hints);
				
			}catch(Exception e) {
				throw new RuntimeException(e);
			}});
		
		HashMap<String,Credential> distinct_credentials = new HashMap<>();
		
		credentials	.stream()
					.forEach(credential -> {
						String key =	credential.getType()[0].getClass().getSimpleName() 
								+":"+ 	credential.getUsername()
								+":"+ 	credential.getPassword();
						Optional<Credential> find = Optional.ofNullable(distinct_credentials.get(key));
							find.ifPresent(find_credential -> find_credential.merge(credential));
							if(! find.isPresent()) distinct_credentials.put(key, credential);
					});
				
		return new GitAnalysis(gits, distinct_credentials.values(), failures, new ArrayList<>());
		
	}
	
	void get_report(GitWrap git) throws Exception {
		git.cInfo.setFiles_count	(tree_count(tree_find(git.getGit(), null)));
		git.cInfo.setHasDocker		(tree_count(tree_find(git.getGit(), "Dockerfile"))	>0);
		git.cInfo.setHasJenkins		(tree_count(tree_find(git.getGit(), "Jenkinsfile"))	>0);
	}
	
	Map<String,Multimap<String,Hint>> get_hints(GitWrap git) throws Exception{
		Config cf = git.getGit().getRepository().getConfig();
		List<RevCommit> commits = StreamSupport.stream(git.getGit().log().call().spliterator(), false).collect(Collectors.toList());
		
		Map<String,Multimap<String,Hint>>  hints = new HashMap<>();
		
		for(RevCommit commit : commits) {
			for(int parent_index = 0; parent_index<commit.getParentCount(); parent_index++) {
				for(DiffEntry diff : tree_diff(git.getGit(), cf, commit.getParent(parent_index).getTree(), commit.getTree())) {
					try (
						ObjectReader reader = git.getGit().getRepository().newObjectReader();
						ByteArrayOutputStream bos = new ByteArrayOutputStream ();
						DiffFormatter df = new DiffFormatter(bos);
					){
					df.setReader(reader, cf);
					df.format(diff);
					String msg = new String(bos.toByteArray(), StandardCharsets.UTF_8);
					
					Matcher m = positives.matcher(msg);
					
					while(m.find()) {
						String hint = m.group();
						if(		! falsepositives_syntax.matcher(hint).matches()
							&&	! falsepositives_hints.matcher(hint).find()) {
							String leaking_file = diff.getNewPath();
							Optional.ofNullable(hints.get(leaking_file))
									.orElseGet(()->{
										Multimap<String,Hint> leaking_commits = HashMultimap.create();
										hints.put(leaking_file, leaking_commits );
										return leaking_commits;
									})
									.put(	diff.getOldId().name() + " > " + diff.getNewId().name(), 
											new Hint(git, commit, diff, m.end(), hint.trim()	));
						}
					}
				}
				}
			}
		}
		
		return hints;
	}
	
	List<Failure> get_failures(
		GitWrap git, String repo_name, 
		Map<String,Multimap<String,Hint>> hints) throws Exception{
		
		return hints
			.entrySet().parallelStream()
			.flatMap(file -> {
				
				AtomicReference<Object> currentStatus = new AtomicReference<>();
				AtomicReference<List<String>> currentSequence = new AtomicReference<>();
				
				Runnable resetSequence = () -> currentSequence.set(new ArrayList<>());
				
				return file	
					.getValue().asMap().entrySet().stream()
					.sorted(Comparator.comparing(e -> e.getKey()))
					.flatMap(commit -> {
						
						resetSequence.run();
						
						return
						commit	.getValue().stream()
								.sorted(Comparator.comparing(hint -> hint.getText().length() < 150))
								.flatMap(hint -> {
									
									List<Failure> failures = new ArrayList<>();
									
									UnaryOperator<String> trim = text -> text 
										.trim()
										.substring(Math.max(hint.getText().indexOf("="), 
															hint.getText().indexOf(":"))+1);
									
									BiConsumer<Target[],List<String>> add_sequence= (type,sequence) -> {
										failures.add(new Failure(null, hint,
											type,
											sequence,
											Stream	.of(gitserver,repo_name,"blob",hint.getCommit().getId().name(),file.getKey())
													.collect(Collectors.joining("/"))	));
									};
									
									
									decode : for(Target[] sequence : sequence_all){
											for(int index = 0; index  < sequence.length ; index++ ) {
												
												if(hint.getText().contains(sequence[index].name())) {
													if(index==0)	
														resetSequence.run();
													
													if(index==0 || currentStatus.get()==sequence[index-1]) {
														while(currentSequence.get().size()<index)
															currentSequence.get().add("");
														currentSequence.get().add(trim.apply(hint.getText()));
													}
													
													if(index == sequence.length-1) 
														add_sequence.accept(sequence,currentSequence.get());
													
													currentStatus.set(sequence[index]);
													break decode;
												}
												
											}
										}
									return failures.stream();
								});
						});
				})
			.collect(Collectors.toList());
	}
	
	List<Credential> get_credentials(GitWrap git, List<Failure> failures ){
		
		Map<String,String> template = ImmutableMap.of(
			Sequence_Oauth.class.getSimpleName(), oauthserver
		);
		
		ArrayList<Credential> credentials = new ArrayList<>();
		
		failures.stream()
				.peek(failure -> failure.getType()[0].shift(failure, template))
				.forEach(failure -> {
					Optional<Credential> find = credentials.stream()
						.filter(c ->	(c.getType()==failure.getType()	|| c.getType()[0] instanceof Sequence_Human || failure.getType()[0] instanceof Sequence_Human)  
								&&		(c.getUsername().isEmpty() || c.getUsername().equals(failure.getSequence().get(1)))
								&&		(c.getPassword().isEmpty() || c.getPassword().equals(failure.getSequence().get(2)))	)
						.findAny();
					find.ifPresent(c -> c.merge(failure));
					if(! find.isPresent())
						credentials.add(new Credential(
							UUID.randomUUID(), 
							failure.type, 
							failure.getSequence().get(1), 
							failure.getSequence().get(2), 
							Stream	.of(failure.getSequence().get(0))
									.collect(Collectors.toSet()),
							Arrays.asList(failure)));
				});
		
		return credentials;
	}
	
	void util_save_txt(String repo_name, Map<String,Multimap<String,Hint>> hints) throws Exception {
		try(PrintWriter p = new PrintWriter(new FileWriter(folder.getAbsoluteFile()+File.separator + "logs_" + repo_name.replace("/", "_") + ".log")) ;){
			hints.forEach((file, failures)->{
				p.println(file);
				failures.asMap().entrySet().stream()
						.sorted(Comparator.comparing(e -> e.getKey()))
						.forEach(commit ->{
							p.println("   " + commit.getKey());
							commit	.getValue().stream()
									.sorted(Comparator.comparing(hint -> hint.getLocation()))
									.forEach(hint -> p.println("      " + hint.getText()));
						});
			});
		}
	}

	static TreeWalk tree_find(Git git, String regex) throws Exception {
		
		Repository repo= git.getRepository();
        ObjectId lastCommitId = repo.resolve(Constants.HEAD);
        TreeWalk objectId ;
        
        try (RevWalk revWalk = new RevWalk(repo)) {
            RevTree tree = revWalk.parseCommit(lastCommitId).getTree();

            try (TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                if(regex!=null)
                treeWalk.setFilter(PathSuffixFilter.create(regex));
                objectId = treeWalk;
            }

            revWalk.dispose();
        }	
        return objectId;
	}

	
	static ObjectId tree_get_in(Git git, RevCommit commit, String regex) throws Exception {

		Repository repo= git.getRepository();
		
		// and using commit's tree find the path
		RevTree tree = commit.getTree();
		try(TreeWalk treeWalk = new TreeWalk(repo)){
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			treeWalk.setFilter(PathSuffixFilter.create(regex));
			if (!treeWalk.next()) return null;
			return treeWalk.getObjectId(0);
		}
	}
	
	static int tree_count(TreeWalk t) throws Exception {
		int i = 0;
		while(t.next()) i++;
		return i;
	}
	
	static List<DiffEntry> tree_diff(Git git, Config cf, ObjectId oldTree , ObjectId newTree ) throws Exception{
		
		try (
				ObjectReader reader = git.getRepository().newObjectReader();
				DiffFormatter df = new DiffFormatter(NullOutputStream.INSTANCE);
			){
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset( reader, oldTree );
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset( reader, newTree );
			
			df.setReader(reader, cf);
			return df.scan(oldTreeIter, newTreeIter);
		}
	}

}
