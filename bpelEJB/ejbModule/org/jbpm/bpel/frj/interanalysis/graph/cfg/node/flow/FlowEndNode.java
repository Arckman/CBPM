/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.flow;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.EndNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-11
 */
public class FlowEndNode extends EndNode {

	/**
	 * 
	 */
	public FlowEndNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public FlowEndNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	public FlowEndNode(Activity activity){
		super(activity);
	}
}
