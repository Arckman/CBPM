/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.basic;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.AssignOperation;


/**
 * @author frj
 * 2012-3-12
 */
public class Assign extends Activity {

	private List<AssignOperation> operations=null;
	
	public Assign(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	public List<AssignOperation> getOperations() {
		return operations;
	}

	public void setOperations(List<AssignOperation> operations) {
		this.operations = operations;
	}
	
	public void addOperation(AssignOperation operation){
		if(this.operations==null){
			this.operations=new ArrayList();
		}
		this.operations.add(operation);
	}
}
