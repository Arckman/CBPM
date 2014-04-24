/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.pick;


import java.util.HashMap;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.BeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.PickOnAlarm;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.PickOnMsg;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-6-4
 */
public class PickBeginNode extends BeginNode {
	
	private HashMap<PickOnMsg,Node> onMsgNodes=new HashMap<PickOnMsg,Node>(1);
	private HashMap<PickOnAlarm,Node> onAlarmNodes=new HashMap<PickOnAlarm,Node>(1);

	/**
	 * 
	 */
	public PickBeginNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public PickBeginNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public PickBeginNode(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	public void addOnMsgNode(PickOnMsg onMsg,Node node){
		this.onMsgNodes.put(onMsg, node);
	}
	
	public void addOnAlarmNode(PickOnAlarm onAlarm,Node node){
		this.onAlarmNodes.put(onAlarm, node);
	}
	
	public HashMap<PickOnMsg,Node> getAllOnMsgNodes(){
		return this.onMsgNodes;
	}
	
	public HashMap<PickOnAlarm,Node> getAllOnAlarmNodes(){
		return this.onAlarmNodes;
	}
	
	public Node getOnMsgNode(PickOnMsg omMsg){
		return this.onMsgNodes.get(omMsg);
	}
	
	public Node getOnAlarmNode(PickOnAlarm onAlarm){
		return this.onAlarmNodes.get(onAlarm);
	}
}
