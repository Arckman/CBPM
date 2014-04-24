/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.flow;


import java.util.HashMap;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.BeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.LinkDef;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-11
 */
public class FlowBeginNode extends BeginNode {

	private HashMap<String,LinkDef>links=new HashMap(2);
	/**
	 * 
	 */
	public FlowBeginNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public FlowBeginNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	public FlowBeginNode(Activity activity){
		super(activity);
	}

	public HashMap<String, LinkDef> getLinks() {
		return links;
	}
	
	public void addLink(LinkDef link){
		this.links.put(link.getName(), link);
	}
}
