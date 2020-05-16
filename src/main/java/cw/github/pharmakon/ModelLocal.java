package cw.github.pharmakon;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revwalk.RevCommit;

import cw.github.pharmakon.ModelNative.RepositoryMetaInfo;
import cw.github.pharmakon.ModelPattern.Sequence_Human;
import cw.github.pharmakon.ModelPattern.Target;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author camille.walim
 *
 */
public class ModelLocal {

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
	
	@AllArgsConstructor @Getter 
	static class Hint{
		GitWrap git;
		RevCommit commit;
		DiffEntry diff;
		int location;
		String text;
	}
	
	@AllArgsConstructor @Getter @Setter
	static class Failure{
		Credential credential;
		Hint hint;
		
		Target[] type;
		List<String> sequence;
		String url;
		
		static final String[] headers = {"uuid","url"};
		public String toString() {
			return util_print(credential.uuid, url);
		}
	}

	
	@AllArgsConstructor @Getter @EqualsAndHashCode(of = {"username","password"})
	static class Credential{
		UUID uuid;
		Target[] type;
		
		String username;
		String password;

		Set<String> adresses;
		List<Failure> failures;
		
		
		public void merge(Failure failure) {
			if(getUsername().isEmpty()) 					username=failure.getSequence().get(1);
			if(getPassword().isEmpty()) 					password=failure.getSequence().get(2);
			if(getType()[0] instanceof Sequence_Human)		type = failure.getType();
			adresses.add(failure.getSequence().get(0));
			detach(failure);
		}
		
		public void merge(Credential other) {
			adresses.addAll(other.adresses);
			failures.forEach(this::detach);
		}
		
		public void detach(Failure failure) {
			failures.add(failure);
			failure.credential = this;
		}
		
		static final String[] headers = {"uuid","type","username","password"};
		public String toString() {
			return util_print(
				uuid,
				type[0].getClass().getSimpleName(),
				username,password);
		}
	}

	@AllArgsConstructor @Getter
	static class CredentialReport{
		Credential credential;
		String adress;
		boolean isReachable, isValid;
		String data = "";
		
		static final String[] headers = {"uuid","adress","isReachable","isValid","data"};
		public String toString() {
			return util_print(
				credential.uuid,
				adress,isReachable, isValid, data);
		}
	}

	
	@AllArgsConstructor @Getter 
	static class GitAnalysis{
		List<GitWrap> gits;
		Collection<Credential> credentials;
		List<Failure> failures;
		List<CredentialReport> reports;
	}

	static String util_print(Object...objects) {
		return Stream.of(objects)
				.map(i -> i==null ? "" : i)
				.map(Object::toString)
				.collect(Collectors.joining(","))
				;
	}
	
	
}
