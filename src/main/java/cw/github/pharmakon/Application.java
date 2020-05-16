package cw.github.pharmakon;

import java.io.File;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {
 
 
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
  
    @Override
    public void run(String... args) throws Exception {
		File file = new File(System.getProperty("user.home") + File.separator + ".githubsnd");
		if(! file.exists()) file.mkdir();
    	
		String user = args[0], password = args[1];
		
		Search search = new Search(user, password, file);
		Destroy destroy = new Destroy(file);
		
		destroy.destroy(search.search("java", 10000, 1));
		
		System.exit(0);
    }
	
}