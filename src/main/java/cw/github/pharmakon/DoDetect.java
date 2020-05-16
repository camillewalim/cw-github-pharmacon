package cw.github.pharmakon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import com.google.common.collect.Multimap;

import cw.github.pharmakon.Model_Local.Credential;
import cw.github.pharmakon.Model_Local.Failure;
import cw.github.pharmakon.Model_Local.GitAnalysis;
import cw.github.pharmakon.Model_Local.GitWrap;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author camille.walim
 * Let you detect potential hints, credentials & failures.
 */
@AllArgsConstructor
public class DoDetect {
	
	static final String[] hints = {
		"pass","user","jdbc",
		"key", "Key",
		"clientId", "clientSecret"
		//,"url"
		};
	
	static final Pattern positives = Pattern.compile("("
		+ Stream.of(hints).collect(Collectors.joining("|"))
		+ ")"
		+ "\"?"
		+ "\s*"
		+ "[:|=]"
		+ "(.*[,|\n|;])");
	
	static final Pattern falsepositives_hints = Pattern.compile("("
		+ "passthrough"
		+ ")");
	
	static final String endline = "\\s*\\n?";
	static final Pattern falsepositives_syntax = Pattern.compile("("
		+ Stream.of(
			"\\w*" + "[:|=]" + "\\s*" + "[\\$|#]" + ".*" + endline,
			".*" + "\\(?" + "\\)" + "[;|,]" + endline,
			"(.*" + Stream.of("'", "\\)", "\\]", "\\)", "\\{").collect(Collectors.joining("\\s*")) + endline +")"
			).collect(Collectors.joining("|"))
		+ ")"); 
	
	
	enum Sequence_JDBC{
		jdbc, username, password
	}
	enum Sequence_Oauth{
		clientId, clientSecret
	}
	enum Sequence_JKS{
		keystorePassword, keystoreName
	}
	
	
	
	
	
	String server;
	File folder;
	
	
	public GitAnalysis detect(List<GitWrap> gits) throws Exception {
		
		HashMap<Credential, Credential> credentials = new HashMap<>();
		List<Failure> failures= new ArrayList<>();
		
		for(GitWrap git : gits) {
			get_report(git);
			Map<String,Multimap<String,Hint>> hints = get_hints(git);

			String repo_name = Optional
				.of(git.getGit().getRepository().getDirectory())
				.map(r -> r.getParentFile().getParentFile().getName() + "/" + r.getParentFile().getName())
				.get();	
			
			get_failures(git, repo_name, hints, credentials, failures);
			
			util_save_txt(repo_name, hints);
		}
		
		return new GitAnalysis(gits, credentials.keySet(),failures);
		
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
											new Hint(commit, diff, m.end(), hint.trim()	));
						}
					}
				}
				}
			}
		}
		
		return hints;
	}
	
	List<Failure> get_failures(
		GitWrap git, String repo_name, Map<String,Multimap<String,Hint>> hints, 
		HashMap<Credential, Credential> credentials, List<Failure> failures) throws Exception{
		
		hints.forEach((file, commits) -> {
			
			AtomicReference<Object> currentStatus = new AtomicReference<>();
			AtomicReference<List<String>> currentSequence = new AtomicReference<>();
			
			Runnable resetSequence = () -> currentSequence.set(new ArrayList<>());
			
			commits	.asMap().entrySet().stream()
					.sorted(Comparator.comparing(e -> e.getKey()))
					.forEach(commit -> {
						resetSequence.run();
						commit	.getValue().stream()
								.sorted(Comparator.comparing(hint -> hint.getText().length() < 150))
								.forEach(hint -> {
									decode : for(Enum<?>[] sequence :
										Stream.<Enum<?>[]>of(
											Sequence_JDBC.values(),
											Sequence_Oauth.values(),
											Sequence_JKS.values()) 
										.collect(Collectors.toList())){
										for(int index = 0; index  < sequence.length ; index++ ) {
											
											if(hint.getText().contains(sequence[index].name())) {
												if(index==0)	
													resetSequence.run();
												
												if(index==0 || currentStatus.get()==sequence[index-1]) {
													while(currentSequence.get().size()<index)
														currentSequence.get().add("");
													currentSequence.get().add(hint.getText()
														.trim()
														.substring(Math.max(hint.getText().indexOf("="), 
																			hint.getText().indexOf(":"))+1));
												}
													
												
												if(index == sequence.length-1) {
													Credential credential = new Credential (
														git, UUID.randomUUID(), 
														currentSequence.get());
													Optional.ofNullable(credentials.get(credential))
															.orElseGet(()->{
																credentials.put(credential,credential);
																return credential;
															});
													failures.add(new Failure(
														credential,
														Stream	.of(server,repo_name,"blob",hint.getCommit().getId().name(),file)
																.collect(Collectors.joining("/"))	));
												}
												
												currentStatus.set(sequence[index]);
												break decode;
											}
											
										}
									}
								});
					});
			
		});
		return failures;
	}
	
	TreeWalk tree_find(Git git, String regex) throws Exception {
		
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
	
	int tree_count(TreeWalk t) throws Exception {
		int i = 0;
		while(t.next()) i++;
		return i;
	}
	
	List<DiffEntry> tree_diff(Git git, Config cf, ObjectId oldTree , ObjectId newTree ) throws Exception{
		
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
	
	
	public void util_save_txt(String repo_name, Map<String,Multimap<String,Hint>> hints) throws Exception {
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


	@AllArgsConstructor @Getter 
	static class Hint{
		RevCommit commit;
		DiffEntry diff;
		int location;
		String text;
	}
}
