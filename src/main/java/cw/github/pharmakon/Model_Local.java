package cw.github.pharmakon;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;

import cw.github.pharmakon.Model_Native.RepositoryMetaInfo;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author camille.walim
 *
 */
public class Model_Local {

	@AllArgsConstructor @Getter
	static class GitWrap{
		Git git;
		RepositoryMetaInfo metaInfo;
		GitComputedInfo cInfo;
		
		static final String[] headers = {"name"," owner","size"," forks_count"," files_count","hasJenkins"," hasDocker"};
		public String toString() {

			return util_print(

				cInfo.repo_name,
				
				metaInfo==null ? "" : metaInfo.getOwner().getLogin(),
				metaInfo==null ? -1 : metaInfo.getSize(),
				metaInfo==null ? -1 : metaInfo.getForksCount(),
				
				cInfo.files_count, cInfo.hasJenkins, cInfo.hasDocker);
		}
	}

	@NoArgsConstructor @Getter @Setter
	static class GitComputedInfo{
		String repo_name;
		int files_count; 
		boolean hasJenkins, hasDocker;
	}
	
	@AllArgsConstructor @Getter @EqualsAndHashCode(exclude= {"uuid"})
	static class Credential{
		GitWrap git;
		UUID uuid;
		List<String> sequence;
		
		static final String[] headers = {"repo","uuid"};
		public String toString() {
			return util_print(Stream.concat(Stream.of(git.cInfo.repo_name,uuid),sequence.stream()).toArray());
		}
	}
	
	@AllArgsConstructor @Getter 
	static class Failure{
		Credential credential;
		String url;
		
		static final String[] headers = {"uuid","url"};
		public String toString() {
			return util_print(credential.uuid, url);
		}
	}
	
	@AllArgsConstructor @Getter 
	static class GitAnalysis{
		List<GitWrap> gits;
		Set<Credential> credentials;
		List<Failure> failures;
	}


	static String util_print(Object...objects) {
		return Stream.of(objects)
				.map(i -> i==null ? "" : i)
				.map(Object::toString)
				.collect(Collectors.joining(","))
				;
	}
	
	
}
