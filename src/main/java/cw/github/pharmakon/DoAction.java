package cw.github.pharmakon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cw.github.pharmakon.ModelLocal.Credential;
import cw.github.pharmakon.ModelLocal.CredentialReport;
import cw.github.pharmakon.ModelLocal.Failure;
import cw.github.pharmakon.ModelLocal.GitAnalysis;
import cw.github.pharmakon.ModelPattern.Sequence_JDBC;
import cw.github.pharmakon.ModelPattern.Sequence_Oauth;
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

	{
		try {
			DriverManager.registerDriver(new org.postgresql.Driver());
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	private boolean doConnect, doNameAndShame;
	
	public void doAction(GitAnalysis analysis){
		analysis.reports = Stream.<BiFunction<Collection<Credential>, Boolean, Stream<CredentialReport>>>of(
				 this::doConnect_Jdbc
				,this::doConnect_Oauth
				,this::doConnect_Jks
			)
			.flatMap(f -> f.apply(analysis.credentials, doConnect))
			.collect(Collectors.toList());
		if(doNameAndShame) 	
			nameAndShame(analysis.credentials, analysis.failures);
	}
	
	Stream<CredentialReport> doConnect(
		Collection<Credential> credentials,
		Class<? extends Target> target,
		BiFunction<String,Credential,CredentialReport> attempt){
		return credentials	.stream()
			.filter(credential -> target.isInstance(credential.getType()[0]))
			.flatMap(credential -> credential
				.getAdresses().stream()
				.map(adress -> attempt.apply(adress, credential)));
	}
	boolean isComplete(Credential credential) {
		return ! credential.getPassword().isEmpty() && ! credential.getUsername().isEmpty();
	}
	String print(ResultSet rs) throws SQLException{
		return IntStream.range(1, rs.getMetaData().getColumnCount())
						.mapToObj(i ->{
							try {
								return rs.getString(i);
							}catch(Exception e) {
								return "";
							}
						})
						.filter(i -> i!=null)
						.filter(i-> i.matches("[a-zA-Z0-9_]+"))
						.collect(Collectors.joining("/"))
						;
	}
	
	
	public Stream<CredentialReport> doConnect_Jdbc(Collection<Credential> credentials, Boolean doConnect){
		return doConnect(credentials,
			Sequence_JDBC.class,
			(adress,credential)->{
				Multimap<String,String> report = HashMultimap.create();
				if(doConnect && adress.contains("/") && isComplete(credential))
					try (
						Connection con = DriverManager.getConnection("jdbc:" + adress, credential.getUsername(), credential.getPassword());
						ResultSet db_rs = con.getMetaData().getCatalogs();						
						){
						while(db_rs.next()) {
							String db = db_rs.getString(1);
							try(
								Connection con_siblings =
									adress.contains(db) ? con :
									DriverManager.getConnection(
										Optional.of(adress.split("\\/"))
												.map(arr -> 
													"jdbc:" 
													+ Stream
														.of(Arrays.copyOf(arr, arr.length-1))
														.collect(Collectors.joining("/"))
													+ "/" + db)
												.get(), 
										credential.getUsername(), credential.getPassword());
								ResultSet tab_rs = con_siblings .getMetaData().getTables("","","%",null);
								){
								while(tab_rs.next()) {
									String print = print(tab_rs);
									if(!(print.contains("information_schema") || print.contains("pg_catalog") || print.contains("pg_toast")))
										report.put(db, print(tab_rs) + "   ");
								}
							}catch(Exception e) {}
						}
						return new CredentialReport(credential, adress, true, true, report.toString());
					}catch(Exception e) {
						return new CredentialReport(credential, adress, true, false, report.toString() + e.getMessage());
					}
				return new CredentialReport(credential, adress, false, false, "");
			}); 
	}
	
	public Stream<CredentialReport> doConnect_Oauth(Collection<Credential> credentials, Boolean doConnect){
		return doConnect(credentials,
				Sequence_Oauth.class,
				(adress,credential)->{
					if(doConnect & isComplete(credential))
						try {
							HttpsURLConnection httpClient = (HttpsURLConnection) new URL(adress
								+"?" + "grant_type" + "=" + "client_credentials"
								+"&" + "scope"		+ "=" + Stream	.of("openid","profile")
																	.collect(Collectors.joining("%20"))
								)
								.openConnection();
							httpClient.setRequestMethod("POST");
							httpClient.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary(
								(credential.getUsername() + ":" + credential.getPassword()).getBytes()));
							httpClient.setRequestProperty("Content-Type", "äpplication/x-www-form-urlencoded");
							httpClient.setDoOutput(true);
							int res = httpClient.getResponseCode();
							if(res == 200)
								try(BufferedReader buf = new BufferedReader(new InputStreamReader(httpClient.getInputStream(), StandardCharsets.UTF_8));){
									return new CredentialReport(credential, adress, true, true, buf.lines().collect(Collectors.joining("   ")));	
								}
						}catch(Exception e) {
							return new CredentialReport(credential, adress, true, false, "");
						}
					return new CredentialReport(credential, adress, false, false, "");
				});
	}
	
	public Stream<CredentialReport> doConnect_Jks(Collection<Credential> credentials, Boolean doConnect){	
		return doConnect(credentials,
			Target_JKS.class,
			(adress, credential) -> {
				if(doConnect)
					try {
						String r = credential	.getFailures().stream()
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
										failure.getSequence().get(2).toCharArray());
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
							.collect(Collectors.joining(","));
						return r.isEmpty() ?  
								new CredentialReport(credential, adress, true, false, "")
							:	new CredentialReport(credential, adress, true, true, r);	
					}catch(Exception e) {}
				return new CredentialReport(credential, adress, false, false, "");
				
			});
	}
	
	public Stream<CredentialReport>  doConnect_Windows(Collection<Credential> credentials, Boolean doConnect){
		return Stream.empty();
	}
	
	public Stream<CredentialReport>  doConnect_Email(Collection<Credential> credentials, Boolean doConnect){
		return Stream.empty();
	}
	
	public void nameAndShame(Collection<Credential> credentials, List<Failure> failures){
		
	}
	
}
