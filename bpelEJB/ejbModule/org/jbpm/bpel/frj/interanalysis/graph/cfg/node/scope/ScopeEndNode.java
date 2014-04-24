/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.scope;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.EndNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-6-15
 */
public class ScopeEndNode extends EndNode {

	
	/**
	 * 
	 */
	public ScopeEndNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public ScopeEndNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public ScopeEndNode(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}
}
