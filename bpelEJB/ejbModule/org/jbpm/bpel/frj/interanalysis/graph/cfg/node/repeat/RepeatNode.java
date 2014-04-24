/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.repeat;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-11
 */
public class RepeatNode extends Node {

	private String condition;
	private Node truePath;
	private Node falsePath;
	
	/**
	 * 
	 */
	public RepeatNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public RepeatNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	public RepeatNode(Activity activity){
		super(activity);
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public Node getTruePath() {
		return truePath;
	}

	public void setTruePath(Node truePath) {
		this.truePath = truePath;
	}

	public Node getFalsePath() {
		return falsePath;
	}

	public void setFalsePath(Node falsePath) {
		this.falsePath = falsePath;
	}

	
}
