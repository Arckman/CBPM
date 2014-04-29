/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg;


import java.util.HashMap;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.graph.Graph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-11
 */
public class CFGGraph extends Graph{
	private String name;
	private String processName;
	private long processId;
	private Map<String,Node> map=new HashMap<String,Node>();
	private Node start;
	private Node end;
	
	public CFGGraph(){};
	public CFGGraph(BpelModel model){
		this.name=model.getName();
		this.processName=model.getName();
		this.processId=model.getId();
		
		this.start=new Node(this.name);
		this.start.setGraph(this);
		this.end=new Node(this.name);
		this.end.setGraph(this);
		this.start.setNodeType("$ProcessStartNode");
		this.end.setNodeType("$ProcessEndNode");
		this.start.addNextNode(end);
		this.end.addPriorNode(start);
	}
	public String getName() {
		return name;
	}
//	public void setName(String name) {
//		this.name = name;
//	}
	public String getProcessName() {
		return processName;
	}
//	public void setProcessName(String processName) {
//		this.processName = processName;
//	}
	public long getProcessId() {
		return processId;
	}
//	public void setProcessId(long processId) {
//		this.processId = processId;
//	}
	public Map<String, Node> getNodesMap() {
		return map;
	}
	public void addNode(Node node){
		this.map.put(node.getActivityName(), node);
	}
	
	public Node getStart() {
		return start;
	}
	
	public Node getEnd() {
		return end;
	}
	public Node findNode(String name){
		return map.get(name);
	}
	public Node findNode(Activity activity){
		return map.get(activity.getName());
	}
}
