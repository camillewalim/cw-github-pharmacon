package cw.github.pharmakon;

import java.util.List;
import java.util.Set;

import cw.github.pharmakon.Model_Local.*;
import lombok.AllArgsConstructor;


/**
 * @author camille.walim
 * Let you perform some actions on the information system
 * Warning : this is potentially illegal, make sure you have the approval of the administrator before doing
 */
@AllArgsConstructor
public class DoAction {

	private boolean doConnect, doEmails;
	
	public void doAction(GitAnalysis analysis){
		if(doConnect) 	doConnect(analysis.credentials);
		if(doEmails) 	doEmails(analysis.credentials, analysis.failures);
	}
	
	public void doConnect(Set<Credential> credentials){
		
	}
	
	public void doEmails(Set<Credential> credentials, List<Failure> failures){
		
	}
	
}
