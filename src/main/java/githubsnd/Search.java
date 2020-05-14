package githubsnd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import githubsnd.Model.Repositories;
import githubsnd.Model.Repository;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Search {
	
	static String
	url = "https://api.github.com/search/repositories",
	sort = "forks", order="desc";
	static int per_page=100;
	
	String user, password;
	File save;
	
	public List<GitAndRepo> search(String language, int size, int pageLimit) throws Exception {
		return 
			save_repo_ssi(
				save_repo_meta( language,  size,  pageLimit).getItems()
				.stream().filter(g -> 
				g.getName().contains("okhttp")
				).collect(Collectors.toList())
				);
	}
	
	
	Repositories save_repo_meta(String language, int size, int pageLimit) throws Exception {
		
		File file = new File(save.getAbsolutePath() + File.separator + Stream
			.of(language, size, pageLimit)
			.map(Object::toString)
			.collect(Collectors.joining("-")));
		Repositories repos;
		
		if(file.exists()) {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(file)); 
			repos = (Repositories) in.readObject();
			in.close();
			
		}else {
			RestTemplate rest = new RestTemplateBuilder()
				.basicAuthentication(user, password)
				.build();
			
			repos = 
			IntStream.range(0,pageLimit)
				.mapToObj(i -> rest
						.getForEntity(url
							+ "?q={q}&sort={sort}&order={order}&page={page}&per_page={per_page}"
							, Repositories.class, ImmutableMap.of(
							"q",		"language:"+language+"+size:>="+size,
							"sort",		sort,
							"order",	order,
							"page",		i+1,
							"per_page",	per_page))
						.getBody())
				.reduce((r0,r1)-> new Repositories(
					r0.getTotalCount(), 
					false, 
					ImmutableList.<Repository>builder()
						.addAll(r0.getItems())
						.addAll(r1.getItems())
						.build(), 
					ImmutableMap.of()))
				.get();
			
	        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
	        out.writeObject(repos);
	        out.close();
		}
        
		return repos;
		
	}
	
	
	List<GitAndRepo> save_repo_ssi(List<Repository> repos) throws Exception {
		List<GitAndRepo> list = new ArrayList<>();
		
		for(Repository r : repos){
			File local = new File(save.getAbsolutePath() + File.separator + r.getFullName().replace("/", "_") + File.separator + ".git");
			if(! local.exists()) {
				local.mkdir();
				list.add(
					new GitAndRepo(
					Git
						.cloneRepository()
				        .setURI(r.getCloneUrl())
				        .setDirectory(local)
				        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password))
				        .call(),
				    r));	
			}else {
				list.add(new GitAndRepo(new Git(new RepositoryBuilder().setGitDir(local).build()),r));
			}
		}
		
		return list;
	}

	@AllArgsConstructor @Getter
	static class GitAndRepo{
		Git git;
		Repository repo;
	}
}
