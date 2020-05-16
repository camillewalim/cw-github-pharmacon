package cw.github.pharmakon;

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

import cw.github.pharmakon.ModelLocal.GitWrap;
import cw.github.pharmakon.ModelLocal.GitComputedInfo;
import cw.github.pharmakon.ModelNative.Repositories;
import cw.github.pharmakon.ModelNative.RepositoryMetaInfo;
import lombok.AllArgsConstructor;

/**
 * @author camille.walim
 * Let you load metadata from github, store it, then clone all the gits
 */
@AllArgsConstructor
public class DoLoadOnline {
	
	static String
	url = "/search/repositories",
	sort = "forks", order="desc";
	static int per_page=20;
	
	String server, user, password;
	File save;
	
	public List<GitWrap> load(String language, int size, int pageLimit) throws Exception {
		return 
			save_repo_ssi(
				save_repo_meta( language,  size,  pageLimit).getItems());
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
						.getForEntity(server + url
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
					ImmutableList.<RepositoryMetaInfo>builder()
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
	
	
	List<GitWrap> save_repo_ssi(List<RepositoryMetaInfo> repos) throws Exception {
		List<GitWrap> list = new ArrayList<>();
		
		for(RepositoryMetaInfo repo : repos){
			File local = new File(save.getAbsolutePath() + File.separator + repo.getFullName());
			if(! local.exists()) {
				local.mkdir();
				System.out.println("cloning : " + repo.getCloneUrl());
				list.add(
					new GitWrap(
						Git
							.cloneRepository()
					        .setURI(repo.getCloneUrl())
					        .setDirectory(local)
					        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password))
					        .call(),
					    repo,new GitComputedInfo()));	
			}else {
				list.add(new GitWrap(
						new Git(new RepositoryBuilder()
							.setGitDir(new File(local.getAbsolutePath() + File.separator + ".git"))
							.build()),
						repo,new GitComputedInfo()));
			}
		}
		
		return list;
	}
}
