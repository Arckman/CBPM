/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.other;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Flow.LinkDefinition;


/**
 * @author frj
 * 2012-6-25
 */
public class LinkDef {

	private String name;
	private Node source;
	private Node target;
	private String transitionCondition=null;
	public LinkDef(){}
	public LinkDef(String name){this.name=name;}
	public LinkDef(LinkDefinition link){
		this.name=link.getName();
		this.transitionCondition=link.getTransitionCondition();
	}
	public String getName(){
		return name;
	}
	public Node getSource(){
		return this.source;
	}
	public Node getTarget(){
		return this.target;
	}
	public void setName(String name){
		this.name=name;
	}
	public void setSource(Node source) {
	    this.source = source;
	  }
	public void setTarget(Node target) {
	    this.target = target;
	  }
	public String getTransitionCondition() {
		return transitionCondition;
	}
	public void setTransitionCondition(String transitionCondition) {
		this.transitionCondition = transitionCondition;
	}
}
