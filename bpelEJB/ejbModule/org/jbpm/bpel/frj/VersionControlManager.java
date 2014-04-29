package org.jbpm.bpel.frj;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.bpel.frj.communication.Communicator;
import org.jbpm.bpel.frj.communication.SimpleCommunicatorImpl;
import org.jbpm.bpel.frj.interanalysis.mgr.Analyser;
import org.jbpm.bpel.frj.monitor.DynamicDependency;
import org.jbpm.bpel.frj.monitor.ProcessMonitor;
import org.jbpm.bpel.frj.monitor.StaticEdge;
import org.jbpm.bpel.frj.util.ComConstants;
import org.jbpm.bpel.frj.util.MonitorConstants;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class VersionControlManager {

	private boolean needUpdate=false;//indicate need for update
	private Map<String,ProcessMonitor> monitors=new HashMap<String,ProcessMonitor>();
	private String strategy=MonitorConstants.STRATEGY_CONCURRENT;
//	private String strategy=MonitorConstants.STRATEGY_WAIT;
//	private String strategy=MonitorConstants.STRATEGY_BLOCK;
	private Map<URL,ProcessMonitor> urlMapPM=new HashMap<URL,ProcessMonitor>();
	
	public boolean checkProcessDeployed(String processName){
		return monitors.containsKey(processName);
	}
	public void addProcessMonitor(BpelProcessDefinition bpelProcessDefinition,Analyser analyser){
		System.out.println("Add process monitor: "+bpelProcessDefinition.getName());
		ProcessMonitor processMonitor=new ProcessMonitor(bpelProcessDefinition,analyser,this);
		//pre-setup, setup static edges for process monitor
		processMonitor.setupStaticEdges();
		monitors.put(bpelProcessDefinition.getName(), processMonitor);
	}
	public void addInstanceMonitor(BpelProcessDefinition bpelProcessDefinition,ProcessInstance processInstance){
		monitors.get(bpelProcessDefinition.getName()).addInstanceMonitor(bpelProcessDefinition, processInstance);
	}
	public void removeInstanceMonitor(ProcessInstance processInstance){
		monitors.get(processInstance.getProcessDefinition().getName()).removeInstanceMonitor(processInstance);
	}
	public void addURL(ProcessDefinition bpelProcessDefinition,String urlStr){
		ProcessMonitor pm=monitors.get(bpelProcessDefinition.getName());
		if(pm!=null){
			URL url;
			try {
				url = new URL(urlStr);
				pm.addURL(url);
				urlMapPM.put(url, pm);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public ProcessMonitor getPMFromURL(URL url){return urlMapPM.get(url);}
	public Collection<ProcessMonitor> getProcessMonitors(){return monitors.values();}
	public ProcessMonitor getProcessMonitor(String processName){return monitors.get(processName);}
	public void setDynamicUpdatable(boolean b){needUpdate=b;}
	public boolean checkDynamicUpdatable(){return needUpdate;}
	public String getStrategy() {return strategy;}
	public void setStrategy(String strategy) {this.strategy = strategy;}
	
	public void send(Communicator c){
		c.send();
	}
	public void receive(Communicator com){
		SimpleCommunicatorImpl c=(SimpleCommunicatorImpl)com;
		String cmd=c.getCommand();
		ProcessMonitor pm=monitors.get(c.getTarget());
		if(cmd.equals(ComConstants.STATICEDGE_ADD)){//static edge add msg
			pm.addIncomeStaticEdge((StaticEdge)c.getMsg());
		}else if(cmd.equals(ComConstants.SCOPE_REQUEST))
			pm.receiveScopeREQ((StaticEdge)c.getMsg());
		else if(cmd.equals(ComConstants.SCOPE_ACK))
			pm.receiveScopeACK((StaticEdge)c.getMsg());
		else if(cmd.equals(ComConstants.SETUP_REQUEST))
			pm.receiveSetupREQ((StaticEdge)c.getMsg());
		else if(cmd.equals(ComConstants.SETUP_ACK))
			pm.receiveSetupACK((StaticEdge)c.getMsg());
		else if(cmd.equals(ComConstants.FUTURE_NOTIFY))
			pm.receiveFutureNotify((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.FUTURE_ACK))
			pm.receiveFutureACK((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.PAST_NOTIFY))
			pm.receivePastNotify((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.PAST_ACK))
			pm.receivePastACK((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.SUB_FUTURE_NOTIFY))
			pm.receiveSubFutureNotify((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.SUB_FUTURE_ACK))
			pm.receiveSubFutureACK((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.SUB_PAST_NOTIFY))
			pm.receiveSubPastNotify((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.SUB_PAST_ACK))
			pm.receiveSubPastACK((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.FUTURE_CREATE_NOTIFY))
			pm.receiveFutureCreate((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.FUTURE_REMOVE_NOTIFY))
			pm.receiveFutureRemove((DynamicDependency)c.getMsg());
		else if(cmd.endsWith(ComConstants.SUBTX_INIT_ACK))
			pm.receiveSubTXInitACK((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.SUBTX_END_NOTIFY))
			pm.receiveSubTXEnd((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.PAST_CREATE_NOTIFY))
			pm.receivePastCreate((DynamicDependency)c.getMsg());
		else if(cmd.equals(ComConstants.CLEANUP_REQUEST))
			pm.receiveCleanupREQ();
	}
	public void dynamicUpdate(ProcessDefinition process,String fileName){
		String processName=process.getName();
		ProcessMonitor pm=monitors.get(processName);
		//check process monitor state
		if(pm.getSetupState().equals(MonitorConstants.STATE_NORMAL)){//need on-demand setup
			pm.setNeedUpdate(true);
			pm.receiveScopeREQ(null);
		}else if(pm.getSetupState().equals(MonitorConstants.STATE_VALID)){//valid, do update
			
		}
	}
	public void doUpdate(){
		if(needUpdate)
			synchronized(this){
				System.out.println("VM updating..");
				needUpdate=false;
//				if(strategy.equals(MonitorConstants.STRATRGY_WAIT))
					notifyAll();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
