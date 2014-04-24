/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.branch;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.BeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-11
 */
public class BranchBeginNode extends BeginNode {

	private String condition;
	private Node truePath;
	private Node falsePath;
	/**
	 * 
	 */
	public BranchBeginNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public BranchBeginNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	public BranchBeginNode(Activity activity){
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
