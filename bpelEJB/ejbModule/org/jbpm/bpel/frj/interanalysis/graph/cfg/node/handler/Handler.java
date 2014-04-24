/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-6-25
 */
public class Handler extends Node {

	private Node node;
	/**
	 * 
	 */
	public Handler() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public Handler(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public Handler(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}
}
