/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-7-8
 */
public class BeginNode extends Node {

	private EndNode end;
	/**
	 * 
	 */
	public BeginNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public BeginNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public BeginNode(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	public EndNode getEnd() {
		return end;
	}

	public void setEnd(EndNode end) {
		this.end = end;
	}
}
