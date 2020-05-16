package cw.github.pharmakon;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cw.github.pharmakon.ModelLocal.Failure;
import lombok.AllArgsConstructor;

/**
 * @author camille.walim
 * Let you search for different things
 */
public class ModelPattern {

	interface Target{
		default void shift(Failure failure, Map<String,String> template) {};
		String name();
	}
	
	interface Target_JKS extends Target{
		default void shift(Failure failure, Map<String,String> template) {
			try {
				failure.getSequence().add(0, DoDetect.tree_get_in(
					failure.getHint().getGit().getGit(), 
					failure.getHint().getCommit(), 
					".jks")
					.getName());
			}catch(Exception e) {} 
		};
	}
	
	
	interface Key extends Target{}
	interface SpecialKey extends Target{}
	interface Regex extends Target{}
	
	
	
	enum Sequence_JDBC implements Key{
		jdbc, username, password
	}
	enum Sequence_Oauth implements Key{
		clientId, clientSecret 
		;
		public void shift( Failure failure, Map<String,String> template) {
			failure.getSequence().add(0, template.get(Sequence_Oauth.class.getSimpleName()));
		};
	}
	
	enum Sequence_JKS_1 implements Target_JKS, Key{
		keystoreName, keystorePassword,
	}

	@AllArgsConstructor
	enum Sequence_JKS_2 implements Target_JKS, SpecialKey{
		keystoreName	("key-store"), 
		keystorePassword("key-password")
		
		;
		String regex;
	}

	@AllArgsConstructor
	enum Sequence_JKS_3 implements Target_JKS, SpecialKey{
		keystoreName	("trust-store"), 
		keystorePassword("trust-password")
		
		;
		String regex;
	}
	
	@AllArgsConstructor
	enum Sequence_Windows implements Regex{
		username	(target("[a-z]*\\d{6}"))
		
		;
		String regex;
		public void shift( Failure failure, Map<String,String> template) {
			failure.getSequence().add(0,"");	// TODO set up Windows check
			failure.getSequence().add("");
		};
	}
	
	@AllArgsConstructor
	enum Sequence_Email implements Regex{
		username	(target("[a-zA-Z0-9.]+@[a-zA-Z0-9]+.[a-z]{2,3}"))
		
		;
		String regex;
		public void shift( Failure failure, Map<String,String> template) {
			failure.getSequence().add(0,"");	// TODO set up Email check
			failure.getSequence().add("");
		};
	}

	
	@AllArgsConstructor
	enum Sequence_Human implements Regex{
		password	(target("[a-zA-Z]*[0-9]{0,3}"))
		
		;
		String regex;
		public void shift( Failure failure, Map<String,String> template) {
			failure.getSequence().add(0,"");
			failure.getSequence().add(0,"");
		};
	}
	
	@AllArgsConstructor
	enum Sequence_URL implements Regex{
		url				(target("https?:\\/{2}[a-zA-Z0-9.\\/]+..[a-z]{2,3}"))
		;
		
		String regex;
		public void shift( Failure failure, Map<String,String> template) {
			failure.getSequence().add("");	
			failure.getSequence().add("");
		};
	}
	
	static final List<Target[]> sequence_all = Arrays.asList(); 
	
	static final String[] hints = Stream
		.concat(
			Stream.of("pass", "user", "key"),
			sequence_all.stream()
						.flatMap(Stream::of)
						.filter(e -> e instanceof Key || e instanceof SpecialKey)
						.map(Target::name))
		.toArray(String[]::new);

	static final Pattern positives = Pattern.compile(Stream
		.concat(
			Stream.of("("
				+ Stream.of(hints).collect(Collectors.joining("|"))
				+ ")"
				+ "\"?"
				+ "\s*"
				+ "[:|=]"
				+ "(.*[,|\n|;]"
				+ ")"),
			sequence_all.stream()
				.flatMap(Stream::of)
				.filter(e -> e instanceof Regex)
				.<String>map(Target::name))
		.collect(Collectors.joining("|")));
	
	static final Pattern falsepositives_hints = Pattern.compile("("
		+ "passthrough"
		+ ")");
	
	static final String endline = "\\s*\\n?";
	static final Pattern falsepositives_syntax = Pattern.compile("("
		+ Stream.of(
			"\\w*" + "[:|=]" + "\\s*" + "[\\$|#]" + ".*" + endline,
			".*" + "\\(?" + "\\)" + "[;|,]" + endline,
			"(.*" + Stream.of("'", "\\)", "\\]", "\\)", "\\{").collect(Collectors.joining("\\s*")) + endline +")"
			).collect(Collectors.joining("|"))
		+ ")"); 
	
	static final String target(String val) {return "(\\W*"+ val+ "\\W*)";}
}
