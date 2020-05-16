package cw.github.pharmakon;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import cw.github.pharmakon.Model_Local.Credential;
import cw.github.pharmakon.Model_Local.Failure;
import cw.github.pharmakon.Model_Local.GitAnalysis;
import cw.github.pharmakon.Model_Local.GitWrap;

/**
 * @author camille.walim
 	Parameters :
 		server= 	String
 		user = 		String 
    	password = 	String
		mode = 		offline or [java]-[size_min]-[number_of_pages]
		doEmail=	boolean 
		doConnect=	boolean
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
    	
		String 	server= args[0], user = args[1], password = args[2], mode = args[3]; 
		Boolean doEmail=Boolean.valueOf(args[4]), doConnect=Boolean.valueOf(args[5]);

		File folder = new File(System.getProperty("user.home") + File.separator + ".github-pharmakon");
		if(! folder.exists()) folder.mkdir();
		
		GitAnalysis analysis = new DoDetect(server, folder)
			.detect(
			mode.equals("offline") ?
					new DoLoadOffline().load(folder)
				:	new DoLoadOnline(server, user, password, folder).load(
					mode.split("-")[0], 
					Integer.valueOf(mode.split("-")[1]), 
					Integer.valueOf(mode.split("-")[2])));
		
		new DoAction(doEmail,doConnect).doAction(analysis);

		util_save_csv(folder, "gits", 			analysis.gits,			GitWrap.headers);
		util_save_csv(folder, "credentials", 	analysis.credentials,	Credential.headers);
		util_save_csv(folder, "failures", 		analysis.failures,		Failure.headers);
		
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