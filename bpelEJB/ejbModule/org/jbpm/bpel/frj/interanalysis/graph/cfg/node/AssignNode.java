/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node;


import java.util.ArrayList;
import java.util.List;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.AssignOperation;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-12
 */
public class AssignNode extends Node {

	private List<AssignOperation> operations=new ArrayList(4);
	/**
	 * 
	 */
	public AssignNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public AssignNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public AssignNode(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	public List<AssignOperation> getOperations() {
		return operations;
	}

	public void addOperation(AssignOperation operation){
		this.operations.add(operation);
	}
}
