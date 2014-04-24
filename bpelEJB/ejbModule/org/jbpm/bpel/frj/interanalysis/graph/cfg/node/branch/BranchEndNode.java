/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.branch;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.EndNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-11
 */
public class BranchEndNode extends EndNode {


	/**
	 * 
	 */
	public BranchEndNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public BranchEndNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	public BranchEndNode(Activity activity){
		super(activity);
	}
}
