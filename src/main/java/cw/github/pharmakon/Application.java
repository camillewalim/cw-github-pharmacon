package cw.github.pharmakon;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import cw.github.pharmakon.ModelLocal.Credential;
import cw.github.pharmakon.ModelLocal.CredentialReport;
import cw.github.pharmakon.ModelLocal.Failure;
import cw.github.pharmakon.ModelLocal.GitAnalysis;
import cw.github.pharmakon.ModelLocal.GitWrap;

/**
 * @author camille.walim
 	Parameters :
 		server= 	String
 		user = 		String 
    	password = 	String 
    	oauth_server = 	String
		mode = 		offline or [java]-[size_min]-[number_of_pages]
		doConnect=	boolean
		doNameAndShame=	boolean 
	Example :
		https://github.com/
		camillewalim
		Github123
		java-10000-10
		false
		true
 */
@SpringBootApplication
public class Application implements CommandLineRunner {
 
 
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
  
    @Override
    public void run(String... args) throws Exception {
    	
		String 	git_server= args[0], git_user = args[1], git_password = args[2], 
				mode = args[3],
				oauth_server = args[4]; 
		Boolean doNameAndShame=Boolean.valueOf(args[5]), doConnect=Boolean.valueOf(args[6]);

		File folder = new File(System.getProperty("user.home") + File.separator + ".github-pharmakon");
		if(! folder.exists()) folder.mkdir();
		
		GitAnalysis analysis = new DoDetect(git_server, oauth_server, folder)
			.detect(
			mode.equals("offline") ?
					new DoLoadOffline().load(folder)
				:	new DoLoadOnline(git_server, git_user, git_password, folder).load(
					mode.split("-")[0], 
					Integer.valueOf(mode.split("-")[1]), 
					Integer.valueOf(mode.split("-")[2])));
		
		new DoAction(doNameAndShame, doConnect).doAction(analysis);

		util_save_csv(folder, "gits", 			analysis.gits,			GitWrap.headers);
		util_save_csv(folder, "failures", 		analysis.failures,		Failure.headers);
		util_save_csv(folder, "credentials", 	analysis.credentials,	Credential.headers);
		util_save_csv(folder, "leaks", 		analysis.reports,		CredentialReport.headers);
		
		System.exit(0);
    }
    
    
	void util_save_csv(File folder, String name, Collection<? extends Object> elements, String... headers) throws Exception {
	    File csvOutputFile = new File(folder.getAbsoluteFile() + File.separator + name + ".csv");
	    try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
	    	pw.println(Stream.of(headers).collect(Collectors.joining(",")));
	        elements.stream().forEach(pw::println);
	    }
	}
	
}