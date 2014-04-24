/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node;


import java.util.ArrayList;
import java.util.List;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.VariableDef;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-11
 */
public class Node {

	private String name;
	private String activityName;
	private String activityType;
	private String nodeType;
	private String joinCondition;
	List<Node> prior=new ArrayList<Node>(1);
	List<Node> next=new ArrayList<Node>(2);
	private CFGGraph graph=null;
	
	public Node(){}
	public Node(String name){this.name=name;this.activityName=name;}
	public Node(Activity activity){
		this.name=activity.getName();
		this.activityName=this.name;
		this.activityType=activity.getClass().getSimpleName();
		this.nodeType=this.activityType;
		this.joinCondition=activity.getJoinCondition();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getActivityName() {
		return activityName;
	}
	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}
	public String getActivityType() {
		return activityType;
	}
	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}
	public String getNodeType() {
		return nodeType;
	}
	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
	
	public String getJoinCondition() {
		return joinCondition;
	}
	public void setJoinCondition(String joinCondition) {
		this.joinCondition = joinCondition;
	}
	public CFGGraph getGraph() {
		return graph;
	}
	public void setGraph(CFGGraph graph) {
		this.graph = graph;
	}
	public List<Node> getPrior() {
		return prior;
	}
	/**
	 * node in list of this.prior or not
	 * @param node
	 * @return
	 */
	public boolean hasPriorNode(Node node){
		if(this.prior==null)
			return false;
		if(this.prior.contains(node))
			return true;
		else
			return false;
	}
	public List<Node> getNext() {
		return next;
	}
	/**
	 * node in list of this.next or not
	 * @param node
	 * @return
	 */
	public boolean hasNextNode(Node node){
		if(this.next==null)
			return false;
		if(this.next.contains(node))
			return true;
		else
			return false;
	}
	
	public void setType(String type){
		this.setActivityType(type);
		this.setNodeType(type);
	}
	
	public void addPriorNode(Node node){
		if(this.prior==null)
			this.prior=new ArrayList<Node>();
		this.prior.add(node);
	}
	public void removePriorNode(Node node){
		if(this.prior!=null){
			this.prior.remove(node);
		}
	}
	
	public void addNextNode(Node node){
		if(this.next==null)
			this.next=new ArrayList<Node>();
		this.next.add(node);
	}
	public void removeNextNode(Node node){
		if(this.next!=null){
			this.next.remove(node);
		}
	}
	/**
	 * return variableDef
	 * @param node current node which is using this variable
	 * @param variableName
	 * @return
	 */
	public VariableDef getVariabledef(Node node,String variableName){
		Node temp;
		if(this instanceof EndNode){
			temp=((EndNode)this).getBegin();
		}else{
			if(this.getPrior().size()==0)//start node
				temp=null;
			else
				//prior list must contain only one node
				temp=this.getPrior().get(0);
		}
		return temp==null?null:temp.getVariabledef(node, variableName);
	}
}
