/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-7-8
 */
public class EndNode extends Node {

	private BeginNode begin;
	/**
	 * 
	 */
	public EndNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public EndNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public EndNode(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	public BeginNode getBegin() {
		return begin;
	}

	public void setBegin(BeginNode begin) {
		this.begin = begin;
	}
}
