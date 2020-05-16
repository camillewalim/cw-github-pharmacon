package githubsnd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import com.google.common.collect.ImmutableList;

import githubsnd.Search.GitAndRepo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Destroy {
	
	static String[] patterns = {"pass","user","jdbc"
		//,"url"
		};
	
	File save;
	
	
	public void destroy(List<GitAndRepo> gits) throws Exception {
		
		Pattern pattern = Pattern.compile("("
			+ Stream.of(patterns).collect(Collectors.joining("|"))
			+ ")"
			+ "\"?"
			+ "\s*"
			+ "[:|=]"
			+ "(.*[,|\n|;])");
		
		List<Report> reports = new ArrayList<>();
		List<Credential> credentials = new ArrayList<>();
		for(GitAndRepo git : gits) {

			reports.add(new Report(
				git.getGit().getRepository().getIdentifier(),
				git.getRepo().getOwner().getLogin(),
				git.getRepo().getSize(),
				git.getRepo().getForksCount(),
				tree_count(tree_target(git.getGit(), null)),
				tree_count(tree_target(git.getGit(), "Dockerfile"))>0,
				tree_count(tree_target(git.getGit(), "Jenkinsfile"))>0
				));
			HashSet<String> types = new HashSet<>();
			
			for(RevCommit commit : git.getGit().log().call()) {
				for(int i = 0 ; i < commit.getParentCount() ; i++) {
					List<DiffEntry> diff = diff(git.getGit(), commit.getParent(i).getTree(), commit.getTree());
					
					for(DiffEntry entry : diff) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						DiffFormatter df = new DiffFormatter(bos);
						df.setRepository(git.getGit().getRepository());
						df.format(entry);
						df.close();
						String msg = new String(bos.toByteArray(), StandardCharsets.UTF_8);
						Matcher m = pattern.matcher(msg);
						while(m.find()) {
							String pass = m.group();
							types.add(pass);
						}
					}
				}
			}
			types.forEach(System.out::println);
		}
		csv_save(ImmutableList.of("name"," owner",
			"size"," forks_count"," files_count",
			"hasJenkins"," hasDocker"),reports);
		csv_save(ImmutableList.of("adress","user","password"),credentials);
		
		// TODO code a jdbc tester
	}
	
	int tree_count(TreeWalk t) throws Exception {
		int i = 0;
		while(t.next()) i++;
		return i;
	}
	TreeWalk tree_target(Git git, String regex) throws Exception {
		
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
	
	List<DiffEntry> diff(Git git, ObjectId oldTree , ObjectId newTree ) throws Exception{
		org.eclipse.jgit.lib.ObjectReader reader = git.getRepository().newObjectReader();
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset( reader, oldTree );
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset( reader, newTree );
		return git.diff()
			  .setOldTree( oldTreeIter )
			  .setNewTree( newTreeIter )
			  .call();
	}
	
	void csv_save(List<String> headers, List<? extends Object> elements) throws Exception {
	    File csvOutputFile = new File(save.getAbsoluteFile() + File.separator + new Date().getTime() + ".csv");
	    try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
	    	pw.println(headers.stream().collect(Collectors.joining(",")));
	        elements.stream().forEach(pw::println);
	    }
	}
	
	@AllArgsConstructor @Getter
	static class Report{
		String name, owner;
		int size, forks_count, files_count; 
		boolean hasJenkins, hasDocker;
		public String toString() {
			return Stream.of(name, owner,
			size, forks_count, files_count, 
			hasJenkins, hasDocker)
				.map(Object::toString)
				.collect(Collectors.joining(","))
				;
		}
	}
	
	@AllArgsConstructor @Getter
	static class Credential{
		String adress;
		String user;
		String password;
		public String toString() {
			return Stream.of(adress,user,password)
				.map(Object::toString)
				.collect(Collectors.joining(","))
				;
		}
	}

}
