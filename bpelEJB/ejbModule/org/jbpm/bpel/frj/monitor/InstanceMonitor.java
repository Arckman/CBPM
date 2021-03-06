package org.jbpm.bpel.frj.monitor;

import java.util.HashSet;
import java.util.Set;

import org.jbpm.bpel.frj.interanalysis.mgr.Analyser;
import org.jbpm.bpel.frj.util.MonitorConstants;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class InstanceMonitor {
	private ProcessMonitor pm;
	/**
	 * when instance is running, its data has not been persisted in DB, using this reference to instance instead
	 */
	private ProcessInstance instance;
	private long instanceId;
	private String rootMonitorName=null;
	private long rootInstanceId=-1;
	private String parentMonitorName=null;
	private long parentInstanceId=-1;
	private boolean isRoot=true;
	private String lastNode;
	private String currentNode;
	private Set<String> pastNodes=new HashSet<String>();
	
	public InstanceMonitor(){}
	public InstanceMonitor(ProcessMonitor pm,ProcessInstance processInstance){
		this.pm=pm;
		instance=processInstance;
		this.instanceId=processInstance.getId();
	}
	public InstanceMonitor(ProcessMonitor pm,long id){
		this.pm=pm;
		this.instanceId=id;
	}
	public ProcessMonitor getPm() {
		return pm;
	}
	public void setPm(ProcessMonitor pm) {
		this.pm = pm;
	}
	public long getInstanceId() {
		return instanceId;
	}
	public void setInstanceId(long instanceId) {
		this.instanceId = instanceId;
	}
	public String getRootMonitorName() {
		return rootMonitorName;
	}
	public void setRootMonitorName(String rootMonitorName) {
		this.rootMonitorName = rootMonitorName;
	}
	public long getRootInstanceId() {
		return rootInstanceId;
	}
	public void setRootInstanceId(long rootInstanceId) {
		this.rootInstanceId = rootInstanceId;
	}
	public String getParentMonitorName() {
		return parentMonitorName;
	}
	public void setParentMonitorName(String parentMonitorName) {
		this.parentMonitorName = parentMonitorName;
	}
	public long getParentInstanceId() {
		return parentInstanceId;
	}
	public void setParentInstanceId(long parentInstanceId) {
		this.parentInstanceId = parentInstanceId;
	}
	public boolean isRoot() {
		return isRoot;
	}
	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}
	public String getLastNode() {
		return lastNode;
	}
	public String getCurrentNode() {
		return currentNode;
	}
	/**
	 * TODO trick, token can have many children
	 * when instance is running, it has not been stored in DB, so InstanceMonitor has to make a reference to instance
	 * @return
	 */
	public String getCurrentNodeName(){
		Token token=null;
		token=instance.getRootToken();
		while(!(token.getChildren()==null||token.getChildren().size()==0)){
			token=(Token)token.getChildren().values().iterator().next();
		}
		if(token==null)
			return null;
		else
			return token.getNode().getName();
	}
	
	public void newTXInit(){
		if(pm.getVcManager().getMethod().equals(MonitorConstants.METHOD_VC))
			if(!pm.getSetupState().equals(MonitorConstants.STATE_NORMAL)){
				if(pm.isRoot())
					pm.rootTXInit(this);
				else
					pm.subTXInit(this);
			}
	}
	public void TXEnd(){
//		pm.removeInstanceMonitor(instanceId);
//		System.out.println("recievie TXEND");
//		System.out.println(toString());
		//TODO instance on process which is not root could be root tx
		if(pm.getVcManager().getMethod().equals(MonitorConstants.METHOD_QUIESCENCE)||
				(pm.getVcManager().getMethod().equals(MonitorConstants.METHOD_VC)//)){
						&&!pm.getSetupState().equals(MonitorConstants.STATE_NORMAL))){
			if(pm.getVcManager().getMethod().equals(MonitorConstants.METHOD_QUIESCENCE)){
				if(pm.isRoot())
					pm.rootTXEnd(this);
			}else if(pm.getVcManager().getMethod().equals(MonitorConstants.METHOD_VC)){
				if(pm.getSetupState().equals(MonitorConstants.STATE_VALID)){
					if(pm.isRoot())
						pm.rootTXEnd(this);
					else
						pm.subTXEnd(this);
				}
//				if(pm.isRoot())//for test
//					pm.rootTXEnd(this);
			}
		}
	}
	public void updateCurrentNode(String nodeName){
		if(pm.getVcManager().getMethod().equals(MonitorConstants.METHOD_VC)){
			lastNode=currentNode;
			currentNode=nodeName;
			pastNodes.add(lastNode);
			if(pm.getVcManager().useDD()){
//		System.out.println(instanceId+" Updated Node======last: "+lastNode+" $$ current: "+currentNode+"==========");
				if(pm.getSetupState().equals(MonitorConstants.STATE_VALID))
					pm.updateCurrentNode(this);
			}
		}
	}
	public boolean isPast(String currentNode,String partnerLinkType,Analyser internalAnalyser){
		for(String node:pastNodes){
			if(internalAnalyser.isMatch(node, partnerLinkType))
				return true;
		}
		return false;
	}
	@Override
	public String toString(){
		StringBuffer s=new StringBuffer();
		s.append("------------------------------------------------------------------------------\t\n");
		s.append("Instance: "+pm.getProcessName()+"#"+instanceId+"-"+rootInstanceId+"\t\n");
		s.append("RootMonitorName: "+rootMonitorName+"\t\n");
		s.append("RootId: "+rootInstanceId+"\t\n");
		s.append("ParentMonitorName: "+parentMonitorName+"\t\n");
		s.append("ParentId: "+parentInstanceId+"\t\n");
//		s.append("LastNode: "+lastNode+" $$ CurrentNode: "+currentNode+"\t\n");
//		s.append("PastNodes: "+pastNodes.toString()+"\t\n");
		s.append("/////////////////////////////end///////////////////////////////////////////////////////");
		return s.toString();
	}
}
