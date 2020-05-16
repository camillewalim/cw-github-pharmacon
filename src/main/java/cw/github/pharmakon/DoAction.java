package cw.github.pharmakon;

import java.security.KeyStore;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cw.github.pharmakon.ModelLocal.Credential;
import cw.github.pharmakon.ModelLocal.CredentialReport;
import cw.github.pharmakon.ModelLocal.Failure;
import cw.github.pharmakon.ModelLocal.GitAnalysis;
import cw.github.pharmakon.ModelPattern.Target;
import cw.github.pharmakon.ModelPattern.Target_JKS;
import lombok.AllArgsConstructor;


/**
 * @author camille.walim
 * Let you perform some actions on the information system
 * Warning : this is potentially illegal, make sure you have the approval of the administrator before doing
 */
@AllArgsConstructor
public class DoAction {

	private boolean doConnect, doNameAndShame;
	
	public void doAction(GitAnalysis analysis){
		if(doConnect) {
			doConnect_Jdbc		(analysis.credentials);
			doConnect_Oauth		(analysis.credentials);
			doConnect_Jks		(analysis.credentials);
			doConnect_Windows	(analysis.credentials);
		}
		if(doNameAndShame) 	
			nameAndShame(analysis.credentials, analysis.failures);
	}
	
	Stream<CredentialReport> doConnect(
		Collection<Credential> credentials,
		Class<? extends Target> target,
		BiFunction<String,Credential,CredentialReport> attempt){
		return credentials	.stream()
			.filter(credential -> credential.getType()[0] instanceof Target_JKS)
			.flatMap(credential -> credential
				.getAdresses().stream()
				.map(adress -> attempt.apply(adress, credential)));
	}
	
	public void doConnect_Jdbc(Collection<Credential> credentials){
		
	}
	
	public void doConnect_Oauth(Collection<Credential> credentials){
		
	}
	
	public Stream<CredentialReport> doConnect_Jks(Collection<Credential> credentials){	//TODO try to get accesses to alias & URL
		return doConnect(credentials,
			Target_JKS.class,
			(adress, credential) -> 
				new CredentialReport(credential,adress,true, true,
				credential	.getFailures().stream()
							.filter(failure -> failure.getSequence().get(0).equals(adress))
							.map(failure -> {
								try {
									KeyStore ks  = KeyStore.getInstance("JKS");
									ks.load(
										failure.getHint().getGit().getGit().getRepository().open(
											 DoDetect.tree_get_in(
												 failure.getHint().getGit().getGit(), 
												 failure.getHint().getCommit(), 
												 ".jks"))
										.openStream(), 
										failure.getSequence().get(3).toCharArray());
									String val = "";
									Enumeration<String> aliases = ks.aliases(); 
									while(aliases.hasMoreElements())
										val = val + aliases.nextElement();
									return val;
								}catch(Exception e) {
									return null;	
								}
							})
							.filter(c -> c!=null)
							.collect(Collectors.joining("\n"))));
	}
	
	public void doConnect_Windows(Collection<Credential> credentials){
		
	}
	
	public void doConnect_Email(Collection<Credential> credentials){
		
	}
	
	public void nameAndShame(Collection<Credential> credentials, List<Failure> failures){
		
	}
	
}
