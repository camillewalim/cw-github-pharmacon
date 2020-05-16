package cw.github.pharmakon;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.RepositoryBuilder;

import cw.github.pharmakon.Model_Local.GitComputedInfo;
import cw.github.pharmakon.Model_Local.GitWrap;
import cw.github.pharmakon.Model_Native.Repositories;
import cw.github.pharmakon.Model_Native.RepositoryMetaInfo;
import lombok.AllArgsConstructor;

/**
 * @author camille.walim
 * Let you load metadata from local git
 */
@AllArgsConstructor
public class DoLoadOffline {

	public List<GitWrap> load(File folder){
		
		Supplier<Stream<File>> files = () -> Stream	
			.of(folder.list())
			.map(name -> new File(folder.getAbsolutePath() + File.separator + name)); 
		
		List<RepositoryMetaInfo> repos = files.get()
    		.filter(File::isFile)
    		.filter(file -> file.getName().matches("\\w*"))
    		.flatMap(file -> {
    			try(
    				ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));){
    				return ((Repositories) in.readObject()).getItems().stream();
    			}catch(Exception e) {
    				e.printStackTrace();
    				return Stream.empty();
    			}
    		})
    		.collect(Collectors.toList());
		
    	return files.get()
    		.filter(File::isDirectory)
			.flatMap(f->loadProject(f,repos))
			.collect(Collectors.toList());
    }
	
	public Stream<GitWrap> loadProject(File folder, List<RepositoryMetaInfo> repos){
    	return Stream	.of(folder.list())
    					.map(f -> {
    						try {
    							return new GitWrap(
    								new Git(new RepositoryBuilder()
    											.setGitDir(new File(folder.getAbsolutePath() + File.separator + ".git"))
    											.build()), 
    								repos.stream()
    									.filter(r -> folder.getAbsolutePath().contains(r.getFullName()))
    									.findAny()
    									.orElseGet(()-> new RepositoryMetaInfo()), 
    								new GitComputedInfo());
    						}catch(Exception e) {
    							throw new RuntimeException(e);
    						}
    					});
    }
	
}
