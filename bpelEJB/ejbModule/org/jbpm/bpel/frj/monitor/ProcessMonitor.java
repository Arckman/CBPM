package org.jbpm.bpel.frj.monitor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.bpel.frj.VersionControlManager;
import org.jbpm.bpel.frj.communication.SimpleCommunicatorImpl;
import org.jbpm.bpel.frj.interanalysis.mgr.Analyser;
import org.jbpm.bpel.frj.util.ComConstants;
import org.jbpm.bpel.frj.util.MonitorConstants;
import org.jbpm.bpel.frj.util.TestWriter;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.jms.IntegrationControl;
import org.jbpm.graph.exe.ProcessInstance;
//need synchronize for multi-threads
public class ProcessMonitor {
	private VersionControlManager vcManager;
	private String processName;
	private long processId;
	private List<DynamicDependency> OES=Collections.synchronizedList(new ArrayList<DynamicDependency>());
	private List<DynamicDependency> IES=Collections.synchronizedList(new ArrayList<DynamicDependency>());
	private boolean needUpdate=false;
	//namespace
	private String nameSpace;
	private String setupState=MonitorConstants.STATE_NORMAL;
	private Analyser internalAnalyser;
	private boolean isRoot=true;
	private List<StaticEdge> incomeEdge=new ArrayList<StaticEdge>();
	private List<StaticEdge> outgoEdge=new ArrayList<StaticEdge>();
	private Map<StaticEdge,String>edgeMapPartnerLinkType=new HashMap<StaticEdge,String>();//edge-->partnerLinkType
	private Map<String,StaticEdge>partnerLinkTypeMapEdge=new HashMap<String,StaticEdge>();//partnerLinkType-->edge
	private boolean suspend=false;
	private Map<Long,InstanceMonitor> instanceMonitors=Collections.synchronizedMap(new HashMap<Long,InstanceMonitor>());
	private Set<StaticEdge> scope=new HashSet<StaticEdge>();
	private Set<StaticEdge> incomeEdgeConfirm=new HashSet<StaticEdge>();//record income process which need ack
	private Set<StaticEdge> outgoEdgeConfirm=new HashSet<StaticEdge>();//record outgo process to which ack send
	private Set<DynamicDependency> notifyIncomeFutureDependencies=new HashSet<DynamicDependency>();//record notify edges which need ack
	private Set<DynamicDependency> notifyOutgoFutureDependencies=new HashSet<DynamicDependency>();
	private Set<DynamicDependency> notifyIncomePastDependencies=new HashSet<DynamicDependency>();//record notify edges which need ack
	private Set<DynamicDependency> notifyOutgoPastDependencies=new HashSet<DynamicDependency>();
	private Set<DynamicDependency> subNotifyIncomeFutureDependencies=new HashSet<DynamicDependency>();//record sub notify edges which need ack
	private Set<DynamicDependency> subNotifyOutgoFutureDependencies=new HashSet<DynamicDependency>();
	private Set<DynamicDependency> subNotifyIncomePastDependencies=new HashSet<DynamicDependency>();//record sub notify edges which need ack
	private Set<DynamicDependency> subNotifyOutgoPastDependencies=new HashSet<DynamicDependency>();
	
	private List<URL> urls=new ArrayList<URL>(2);//endpoint address
	
	public ProcessMonitor(){}
	public ProcessMonitor(BpelProcessDefinition bpelProcessDefinition,Analyser analyser,VersionControlManager vcManager){
		setProcessDefinition(bpelProcessDefinition,analyser,vcManager);
	}
	public void setProcessDefinition(BpelProcessDefinition bpelProcessDefinition,Analyser analyser,VersionControlManager vcManager){
		this.processName=bpelProcessDefinition.getName();
		this.processId=bpelProcessDefinition.getId();
		this.nameSpace=bpelProcessDefinition.getTargetNamespace();
		this.internalAnalyser=analyser;
		this.vcManager=vcManager;
	}
	public VersionControlManager getVcManager() {
		return vcManager;
	}
	public void setVcManager(VersionControlManager vcManager) {
		this.vcManager = vcManager;
	}
	public String getProcessName(){return this.processName;}
	public String getNameSpace(){return this.nameSpace;}
	public boolean isNeedUpdate() {
		return needUpdate;
	}
	public void setNeedUpdate(boolean needUpdate) {
		this.needUpdate = needUpdate;
	}
	public boolean isRoot() {
		return isRoot;
	}
	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}
	public void setupStaticEdges(){
		Collection<String> partnerLinkTypes;
//		//income edges
//		partnerLinkTypes=internalAnalyser.getBpelModel().getIncomePartnerLinkTypes();
//		for(String partnerLinkType:partnerLinkTypes){
//			for(ProcessMonitor otherMonitor:this.vcManager.getProcessMonitors()){
//				if(otherMonitor!=this){
//					if(otherMonitor.internalAnalyser.getBpelModel().getOutgoPartnerLinkTypes().contains(partnerLinkType)){
//						//form static edge
//						StaticEdge staticEdge=new StaticEdge(otherMonitor.getProcessName(),this.getProcessName());
//						addIncomeStaticEdge(staticEdge);
//						//send msg to other monitor
//						SimpleCommunicatorImpl c=new SimpleCommunicatorImpl(this.getProcessName(),otherMonitor.getProcessName());
//						c.setCommand(ComConstants.STATICEDGE_ADD);
//						c.setMsg(staticEdge.clone());
//						vcManager.send(c);
//					}
//				}
//			}
//		}
		//outgo edges
		partnerLinkTypes=internalAnalyser.getBpelModel().getOutgoPartnerLinkTypes();
		for(String partnerLinkType:partnerLinkTypes){
			//TODO should be distributed
			for(ProcessMonitor otherMonitor:vcManager.getProcessMonitors()){
				if(otherMonitor!=this){
					if(otherMonitor.internalAnalyser.getBpelModel().getIncomePartnerLinkTypes().contains(partnerLinkType)){
						//form static edge
						StaticEdge staticEdge=new StaticEdge(this.getProcessName(),otherMonitor.getProcessName());
						if(!outgoEdge.contains(staticEdge)){
							System.out.println(staticEdge.toString());
							addOutgoStaticEdge(staticEdge);
							edgeMapPartnerLinkType.put(staticEdge,partnerLinkType);//only contains outgo edge map
							partnerLinkTypeMapEdge.put(partnerLinkType, staticEdge);
							//send msg to other monitor
							SimpleCommunicatorImpl c=new SimpleCommunicatorImpl(this.getProcessName(),otherMonitor.getProcessName());
							c.setCommand(ComConstants.STATICEDGE_ADD);
							c.setMsg(staticEdge.clone());
							vcManager.send(c);
						}
					}
				}
			}
		}
	}
	public String getSetupState(){return setupState;}
	public void setSetupState(String state){setupState=state;}
	public void addIncomeStaticEdge(StaticEdge edge){this.incomeEdge.add(edge);isRoot=false;}
	public void addOutgoStaticEdge(StaticEdge edge){this.outgoEdge.add(edge);}
	public List<StaticEdge> getIncomeStaticEdges(){return this.incomeEdge;}
	public List<StaticEdge> getOutgoStaticEdges(){return this.outgoEdge;}
	private void confirmSetClear(){
		incomeEdgeConfirm.clear();outgoEdgeConfirm.clear();
		notifyIncomeFutureDependencies.clear();notifyOutgoFutureDependencies.clear();
		notifyIncomePastDependencies.clear();notifyOutgoPastDependencies.clear();
		subNotifyIncomeFutureDependencies.clear();subNotifyOutgoFutureDependencies.clear();
		subNotifyIncomePastDependencies.clear();subNotifyOutgoPastDependencies.clear();
	}
	public InstanceMonitor addInstanceMonitor(BpelProcessDefinition bpelProcessDefinition,ProcessInstance processInstance){
		InstanceMonitor im=new InstanceMonitor(this,processInstance);
		if(instanceMonitors.containsKey(processInstance.getId()))
			System.out.println("Error! Duplicated process instance ID!");
		else
			instanceMonitors.put(processInstance.getId(), im);
		return im;
	}
	public InstanceMonitor getInstanceMonitor(long instanceId){return instanceMonitors.get((Long)instanceId);}
	public void removeInstanceMonitor(ProcessInstance processInstance){
		instanceMonitors.remove(processInstance.getId());
	}
	public void removeInstanceMonitor(long id){
		instanceMonitors.remove(id);
	}
	public void setSuspend(boolean b){suspend=b;}
	public boolean isSuspend(){return suspend;}
	public void resume(){
		System.out.println(processName+" will resumes all instance!");
		suspend=false;
		for(InstanceMonitor im:instanceMonitors.values()){
			synchronized(im){
				im.notifyAll();
			}
		}
	}
	public void addURL(URL url){
		if(urls.size()==2)
			urls.remove(0);
		urls.add(url);
	}
	public URL getOldURL(){
		if(urls.size()>0)
			return urls.get(0);
		return null;
	}
	public URL getNewURL(){
		int index=urls.size()-1;
		if(index<0)
			return null;
		return urls.get(index);
	}
	
	
	private void sendCMD(String source,String target,String cmd,Object o){
		SimpleCommunicatorImpl c=new SimpleCommunicatorImpl(source,target);
		c.setCommand(cmd);
		c.setMsg(o);
		vcManager.send(c);
	}
	/**
	 * Send message for a collection. It should be the right way to solve asynchronized problems.
	 * For each Object in Collection, new a thread for sending.
	 * This function must be synchronized on the Collection by developer.
	 * @param cmd
	 * @param c
	 */
	private void sendCollectionCMD(final String cmd,Collection c){
		for(Object o:c){
			final Object msg=o;
			Thread t=new Thread(){
				public void run(){
					String source=null;//source=msg.source
					String target=null;//target=msg.target
					sendCMD(source,target,cmd,msg);
				}
			};
			t.start();
		}
	}
	//======================on-demand functions
	/**
	 * get compute scope request
	 * @param edge
	 */
	public void receiveScopeREQ(StaticEdge edge){
//		System.out.println(processName+" recieve SCOPE_REQ");
//		System.out.println(toString());
		if(edge!=null){
			if(!scope.contains(edge))
				scope.add(edge);
			outgoEdgeConfirm.add(edge);
		}
		//compute scope
		if(incomeEdge.size()==0)//first process
			receiveScopeACK(null);
		else if(scope.size()==1||scope.size()==0){//first time REQ visit
			for(StaticEdge e:incomeEdge){
				incomeEdgeConfirm.add(e);
//				sendCMD(processName,e.getSource(),ComConstants.SCOPE_REQUEST,e.clone());
			}
			//TODO clone set for solving asynchronous and concurrent problem
			Set<StaticEdge> sendSet=new HashSet(incomeEdge);
			for(StaticEdge e:sendSet)//change synchronize communication to asynchronize
				sendCMD(e.getTarget(),e.getSource(),ComConstants.SCOPE_REQUEST,e.clone());
		}else{// REQ visit n time
			if(incomeEdgeConfirm.size()==0)//all scope computation before this process done
				receiveScopeACK(null);
		}
	}
	
	/**
	 * get compute scope ack
	 * @param edge
	 */
	public void receiveScopeACK(StaticEdge edge){
//		System.out.println(processName+" recieve SCOPE_ACK");
//		System.out.println(toString());
		if(edge!=null)
			incomeEdgeConfirm.remove(edge);
		if(incomeEdgeConfirm.size()==0){//all scope computation before this process done
			if(scope.size()==0){//last process,send itself setup request
				receiveSetupREQ(null);
			}else{
				Set<StaticEdge>remove=new HashSet<StaticEdge>();
				for(StaticEdge e:outgoEdgeConfirm){
					remove.add(e);
				}
				outgoEdgeConfirm.removeAll(remove);
				for(StaticEdge e:remove)
					sendCMD(processName,e.getTarget(),ComConstants.SCOPE_ACK,e.clone());
			}
		}
	}
	
	/**
	 * receive setup request
	 * @param edge
	 */
	public void receiveSetupREQ(StaticEdge edge){
//		System.out.println(processName+" recieve Setup_REQ");
//		System.out.println(toString());
		//suspend
		if(!isSuspend())
			setSuspend(true);
		if(edge!=null){
			outgoEdgeConfirm.add(edge);
		}
		if(outgoEdgeConfirm.size()==scope.size()){//receive all setup request from scope
			setupState=MonitorConstants.STATE_ONDEMAND;
			System.out.println(processName+" set state: "+setupState);
			for(StaticEdge e:incomeEdge){
				incomeEdgeConfirm.add(e);
//				sendCMD(processName,e.getSource(),ComConstants.SETUP_REQUEST,e.clone());
			}
			//TODO clone set for solving asynchronous and concurrent error
			Set<StaticEdge> sendSet=new HashSet<StaticEdge>(incomeEdge);
			for(StaticEdge e:sendSet)
				sendCMD(e.getTarget(),e.getSource(),ComConstants.SETUP_REQUEST,e.clone());
			//do setup
			setup();
			if(incomeEdgeConfirm.size()==0)//two way to invoke last process setup ACK. 1.finish setup;2.receive all ACK.
				//We don't know which event comes first
				receiveSetupACK(null);
		}
	}
	
	/**
	 * receive setup ack
	 * @param edge
	 */
	@SuppressWarnings("static-access")
	public void receiveSetupACK(StaticEdge edge){
//		System.out.println(processName+" recieve Setup_ACK");
//		System.out.println(toString());
		if(edge!=null)
			incomeEdgeConfirm.remove(edge);
		if(incomeEdgeConfirm.size()==0&&setupState.equals(MonitorConstants.STATE_SETUP)){//all process setup before done setup and itself setup done
			setupState=MonitorConstants.STATE_VALID;
			System.out.println(processName+" set state: "+setupState);
			Set<StaticEdge> remove=new HashSet<StaticEdge>();
			for(StaticEdge e:outgoEdgeConfirm){
				remove.add(e);
				sendCMD(processName,e.getTarget(),ComConstants.SETUP_ACK,e.clone());
			}
			outgoEdgeConfirm.removeAll(remove);
			resume();
			if(scope.size()==0&&setupState.equals(MonitorConstants.STATE_VALID)){//last process
				//do freeness to update
				doUpdate();
			}
		}
	}
	
	public void setup(){
		System.out.println(processName+" starts setup...");
		for(InstanceMonitor im:instanceMonitors.values()){
			DynamicDependency lfe=new DynamicDependency(processName,processName,im.getRootMonitorName(),im.getRootInstanceId(),MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
			DynamicDependency lpe=new DynamicDependency(processName,processName,im.getRootMonitorName(),im.getRootInstanceId(),MonitorConstants.DYNAMICDEPENDENCY_PAST);
			OES.add(lpe);OES.add(lfe);
			IES.add(lpe);IES.add(lfe);
			if(isRoot){
				for(StaticEdge edge:outgoEdge){
					String currentNodeName=im.getCurrentNode();
					String partnerLinkType=edgeMapPartnerLinkType.get(edge);
					//f(T)
					if(internalAnalyser.isFuture(currentNodeName,partnerLinkType)){
						DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
								MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
						if(!OES.contains(dynamicDependency)){
							OES.add(dynamicDependency);
							notifyOutgoFutureDependencies.add(dynamicDependency);
//							sendCMD(processName, edge.getTarget(), ComConstants.FUTURE_NOTIFY, dynamicDependency.clone());
						}
					}
					//p(T)
					if(im.isPast(currentNodeName, partnerLinkType,internalAnalyser)){
						DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
								MonitorConstants.DYNAMICDEPENDENCY_PAST);
						if(!OES.contains(dynamicDependency)){
							OES.add(dynamicDependency);
							notifyOutgoPastDependencies.add(dynamicDependency);
//							sendCMD(processName,edge.getTarget(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
						}
					}
					//s(T)
					if(internalAnalyser.isS(currentNodeName, partnerLinkType)){
						if(!internalAnalyser.isFuture(currentNodeName, partnerLinkType)){
							DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
									null);
							subNotifyOutgoFutureDependencies.add(dynamicDependency);
//							sendCMD(processName, edge.getTarget(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency);
						}
						if(!im.isPast(currentNodeName, partnerLinkType,internalAnalyser)){
							DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
									null);
							subNotifyOutgoPastDependencies.add(dynamicDependency);
//							sendCMD(processName, edge.getTarget(), ComConstants.SUB_PAST_NOTIFY,dynamicDependency);
						}
					}
				}
				//TODO Clone set is not a good idea for solving asynchronous error, should call @link{sendCollectionCMD} function
				Set<DynamicDependency> set;
				set=new HashSet<DynamicDependency>(notifyOutgoFutureDependencies);
				for(DynamicDependency dd:set){
					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.FUTURE_NOTIFY, dd.clone());
				}
				set=new HashSet<DynamicDependency>(notifyOutgoPastDependencies);
				for(DynamicDependency dd:set)
					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.PAST_NOTIFY, dd.clone());
				set=new HashSet<DynamicDependency>(subNotifyOutgoFutureDependencies);
				for(DynamicDependency dd:set)
					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.SUB_FUTURE_NOTIFY, dd.clone());
				set=new HashSet<DynamicDependency>(subNotifyOutgoPastDependencies);
				for(DynamicDependency dd:set)
					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.SUB_PAST_NOTIFY, dd.clone());
			}
		}
		setupOver();
	}
	
	public void receiveFutureNotify(DynamicDependency dd){
		if(dd!=null){
			if(!notifyIncomeFutureDependencies.contains(dd)){
				IES.add(dd);
				notifyIncomeFutureDependencies.add(dd);
				for(StaticEdge edge:outgoEdge){
					DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
							dd.getType());
					OES.add(dynamicDependency);
					if(!notifyOutgoFutureDependencies.contains(dynamicDependency)){
						notifyOutgoFutureDependencies.add(dynamicDependency);
//						sendCMD(processName,edge.getTarget(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
					}
				}
				//TODO clone set
				Set<DynamicDependency>sendSet=new HashSet<DynamicDependency>(notifyOutgoFutureDependencies);
				for(DynamicDependency d:sendSet)
					sendCMD(processName,d.getTargetMonitorName(),ComConstants.FUTURE_NOTIFY,d.clone());
				receiveFutureACK(null);
			}
		}
	}
	public void receiveFutureACK(DynamicDependency dd){
		if(dd!=null){
			if(notifyOutgoFutureDependencies.contains(dd))
				notifyOutgoFutureDependencies.remove(dd);
			else if(subNotifyOutgoFutureDependencies.contains(dd)){//request sent by sub future
//				subNotifyOutgoFutureDependencies.remove(dd);
				receiveSubFutureACK(dd);
			}
		}
		boolean getAllACK=true;//ACK for root instance dd.rootInstance
		for(DynamicDependency outgoD:notifyOutgoFutureDependencies){
			if(dd==null||dd.equalRootTX(outgoD)){
				getAllACK=false;
				break;
			}
		}
		if(getAllACK){
			Set<DynamicDependency> remove=new HashSet<DynamicDependency>();
			for(DynamicDependency incomeD:notifyIncomeFutureDependencies){
				if(dd==null){
					remove.add(incomeD);
					sendCMD(processName,incomeD.getSourceMonitorName(),ComConstants.FUTURE_ACK,incomeD.clone());
				}else if(dd.equalRootTX(incomeD)){
					remove.add(incomeD);
					sendCMD(processName,incomeD.getSourceMonitorName(),ComConstants.FUTURE_ACK,incomeD.clone());
					break;
				}
			}
			notifyIncomeFutureDependencies.removeAll(remove);
		}
		if(isRoot)//if want to merge future notify and future create notify, add setupState here
			setupOver();
	}
	public void receivePastNotify(DynamicDependency dd){
		if(dd!=null){
			if(!notifyIncomePastDependencies.contains(dd)){
				IES.add(dd);
				notifyIncomePastDependencies.add(dd);
				for(StaticEdge edge:outgoEdge){
					DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
							dd.getType());
					OES.add(dynamicDependency);
					if(!notifyOutgoPastDependencies.contains(dynamicDependency)){
						notifyOutgoPastDependencies.add(dynamicDependency);
//						sendCMD(processName,edge.getTarget(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
					}
				}
				//TODO clone set
				Set<DynamicDependency>sendSet=new HashSet<DynamicDependency>(notifyOutgoPastDependencies);
				for(DynamicDependency dynamicDependency:sendSet)
					sendCMD(processName,dynamicDependency.getTargetMonitorName(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
				receivePastACK(null);
			}
		}
	}
	public void receivePastACK(DynamicDependency dd){
		if(dd!=null){
			if(notifyOutgoPastDependencies.contains(dd))
				notifyOutgoPastDependencies.remove(dd);
			else if(subNotifyOutgoPastDependencies.contains(dd))
				receiveSubPastACK(dd);
		}
		boolean getAllACK=true;//ACK for root instance dd.rootInstance
		for(DynamicDependency outgoD:notifyOutgoPastDependencies){
			if(dd==null||dd.equalRootTX(outgoD)){
				getAllACK=false;
				break;
			}
		}
		if(getAllACK){
			Set<DynamicDependency> remove=new HashSet<DynamicDependency>();
			for(DynamicDependency incomeD:notifyIncomePastDependencies){
				if(dd==null){
					remove.add(incomeD);
					sendCMD(processName,incomeD.getSourceMonitorName(),ComConstants.PAST_ACK,incomeD.clone());
				}else if(dd.equalRootTX(incomeD)){
					remove.add(incomeD);
					sendCMD(processName,incomeD.getSourceMonitorName(),ComConstants.PAST_ACK,incomeD.clone());
					break;
				}
			}
			notifyIncomePastDependencies.removeAll(remove);
		}
		if(isRoot)
			setupOver();
	}
	
	public void receiveSubFutureNotify(DynamicDependency dd){
		if(dd!=null){
			if(!subNotifyIncomeFutureDependencies.contains(dd)){
				subNotifyIncomeFutureDependencies.add(dd);
				InstanceMonitor im=getSubInstance(dd.getRootMonitorName(),dd.getRootInstanceId());
				if(im!=null){
					for(StaticEdge outEdge:outgoEdge){
						String currentNodeName=im.getCurrentNode();
						String partnerLinkType=edgeMapPartnerLinkType.get(outEdge);
						if(internalAnalyser.isFuture(currentNodeName, partnerLinkType)){
							DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
									MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
							if(!OES.contains(dynamicDependency)){
								OES.add(dynamicDependency);
								subNotifyOutgoFutureDependencies.add(dynamicDependency);//add to sub notify outgo future list
		//						sendCMD(processName,outEdge.getTarget(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
							}
						}
						if(internalAnalyser.isS(currentNodeName, partnerLinkType)){
							if(!internalAnalyser.isFuture(currentNodeName, partnerLinkType)){
								DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
										null);
								subNotifyOutgoFutureDependencies.add(dynamicDependency);
		//						sendCMD(processName, outEdge.getTarget(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency.clone());
							}
						}
					}
					//TODO colone set
					Set<DynamicDependency>sendSet=new HashSet<DynamicDependency>(subNotifyOutgoFutureDependencies);
					for(DynamicDependency dynamicDependency:sendSet)
						if(dynamicDependency.getType()==null)
							sendCMD(processName, dynamicDependency.getTargetMonitorName(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency.clone());
						else if(dynamicDependency.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_FUTURE))
							sendCMD(processName,dynamicDependency.getTargetMonitorName(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
				}
				receiveSubFutureACK(null);
			}
		}
	}
	public void receiveSubFutureACK(DynamicDependency dd){
		if(dd!=null){
			if(subNotifyOutgoFutureDependencies.contains(dd))
				subNotifyOutgoFutureDependencies.remove(dd);
		}
		boolean getAllACK=true;//ACK for root instance dd.rootInstance
		for(DynamicDependency outgoD:subNotifyOutgoFutureDependencies){
			if(dd==null||dd.equalRootTX(outgoD)){
				getAllACK=false;
				break;
			}
		}
		if(getAllACK){
			Set<DynamicDependency> remove=new HashSet<DynamicDependency>();
			for(DynamicDependency incomeD:subNotifyIncomeFutureDependencies){
				if(dd==null){
					remove.add(incomeD);
					sendCMD(processName,incomeD.getSourceMonitorName(),ComConstants.SUB_FUTURE_ACK,incomeD.clone());
				}else if(dd.equalRootTX(incomeD)){
					remove.add(incomeD);
					sendCMD(processName,incomeD.getSourceMonitorName(),ComConstants.SUB_FUTURE_ACK,incomeD.clone());
					break;
				}
			}
			subNotifyIncomeFutureDependencies.removeAll(remove);
		}
		if(isRoot)
			setupOver();
	}
	
	public void receiveSubPastNotify(DynamicDependency dd){
		if(dd!=null){
			if(!subNotifyIncomePastDependencies.contains(dd)){
				subNotifyIncomePastDependencies.add(dd);
				InstanceMonitor im=getSubInstance(dd.getRootMonitorName(),dd.getRootInstanceId());
				if(im!=null){
					for(StaticEdge outEdge:outgoEdge){
						String currentNodeName=im.getCurrentNode();
						String partnerLinkType=edgeMapPartnerLinkType.get(outEdge);
						if(im.isPast(currentNodeName, partnerLinkType,internalAnalyser)){
							DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
									MonitorConstants.DYNAMICDEPENDENCY_PAST);
							if(!OES.contains(dynamicDependency)){
								OES.add(dynamicDependency);
								subNotifyOutgoPastDependencies.add(dynamicDependency);//add to sub notify outgo future list
		//						sendCMD(processName,outEdge.getTarget(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
							}
						}
						if(internalAnalyser.isS(currentNodeName, partnerLinkType)){
							if(!im.isPast(currentNodeName, partnerLinkType,internalAnalyser)){
								DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
										null);
								subNotifyOutgoPastDependencies.add(dynamicDependency);
		//						sendCMD(processName, outEdge.getTarget(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency.clone());
							}
						}
					}
					//TODO clone set
					Set<DynamicDependency>sendSet=new HashSet<DynamicDependency>(subNotifyOutgoPastDependencies);
					for(DynamicDependency dynamicDependency:sendSet)
						if(dynamicDependency.getType()==null)
							sendCMD(processName, dynamicDependency.getTargetMonitorName(), ComConstants.SUB_PAST_NOTIFY,dynamicDependency.clone());
						else if(dynamicDependency.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_PAST))
							sendCMD(processName,dynamicDependency.getTargetMonitorName(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
				}
				receiveSubPastACK(null);
			}
		}
	}
	public void receiveSubPastACK(DynamicDependency dd){
		if(dd!=null){
			if(subNotifyOutgoPastDependencies.contains(dd))
				subNotifyOutgoPastDependencies.remove(dd);
		}
		boolean getAllACK=true;//ACK for root instance dd.rootInstance
		for(DynamicDependency outgoD:subNotifyOutgoPastDependencies){
			if(dd==null||dd.equalRootTX(outgoD)){
				getAllACK=false;
				break;
			}
		}
		if(getAllACK){
			Set<DynamicDependency> remove=new HashSet<DynamicDependency>();
			for(DynamicDependency incomeD:subNotifyIncomePastDependencies){
				if(dd==null){
					remove.add(incomeD);
					sendCMD(processName,incomeD.getSourceMonitorName(),ComConstants.SUB_PAST_ACK,incomeD.clone());
				}else if(dd.equalRootTX(incomeD)){
					remove.add(incomeD);
					sendCMD(processName,incomeD.getSourceMonitorName(),ComConstants.SUB_PAST_ACK,incomeD.clone());
					break;
				}
			}
			subNotifyIncomePastDependencies.removeAll(remove);
		}
		if(isRoot)
			setupOver();
	}
	
	/**
	 * get sub transaction/instance according to root
	 * @param id
	 * @return
	 */
	private InstanceMonitor getSubInstance(String rootMonitorName,long id){
		if(instanceMonitors!=null)
			for(InstanceMonitor temp:instanceMonitors.values()){
				if(temp.getRootMonitorName().equals(rootMonitorName)&&temp.getRootInstanceId()==id)
					return temp;
			}
		return null;
	}
//	/**
//	 * see {@link ProcessMonitor.getSubInstance}
//	 * @return
//	 */
//	private InstanceMonitor getInstance(){
//		InstanceMonitor im=null;
//		if(instanceMonitors!=null)
//			im=(InstanceMonitor) instanceMonitors.values().toArray()[0];
//		return im;
//	}
	private void setupOver(){
		if(notifyOutgoFutureDependencies.size()==0&&subNotifyOutgoFutureDependencies.size()==0&&
				notifyOutgoPastDependencies.size()==0&&subNotifyOutgoPastDependencies.size()==0&&setupState==MonitorConstants.STATE_ONDEMAND){
			setupState=MonitorConstants.STATE_SETUP;
			System.out.println(processName+" setup ends!");
			if(incomeEdgeConfirm.size()==0)//two way to invoke last process setup ACK. 1.finish setup;2.receive all ACK.
				//We don't know which event comes first
				receiveSetupACK(null);
		}
	}
	
	//=========================on-going management functions
	/*
	 * TODO
	 * ACK during on-going dynamic dependency management seems to be trivial, so neglect them! 
	 * Two past/future create functions can be merge with past/future notify functions above. Using two new functions for simplification.
	 */
	public synchronized void rootTXInit(InstanceMonitor im){
		if(checkInfoUpdateNeeded(im.getRootMonitorName(), im.getRootInstanceId())){
			if(true){
				DynamicDependency lfe=new DynamicDependency(processName, processName, processName, im.getInstanceId(),
						MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
				DynamicDependency lpe=new DynamicDependency(processName, processName, processName, im.getInstanceId(),
						MonitorConstants.DYNAMICDEPENDENCY_PAST);
				OES.add(lfe);OES.add(lpe);
				IES.add(lfe);IES.add(lpe);
				for(StaticEdge edge:outgoEdge){
					DynamicDependency dd=new DynamicDependency(processName, edge.getTarget(), processName, im.getInstanceId(),
							MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
					OES.add(dd);
					sendCMD(processName,edge.getTarget(),ComConstants.FUTURE_CREATE_NOTIFY,dd.clone());
				}
			}
		}
	}
	public synchronized void subTXInit(InstanceMonitor im){
		if(checkInfoUpdateNeeded(im.getRootMonitorName(), im.getRootInstanceId())){
			DynamicDependency lfe=new DynamicDependency(processName, processName, im.getRootMonitorName(), im.getRootInstanceId(),
					MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
			DynamicDependency lpe=new DynamicDependency(processName, processName, im.getRootMonitorName(), im.getRootInstanceId(),
					MonitorConstants.DYNAMICDEPENDENCY_PAST);
			OES.add(lpe);OES.add(lfe);
			IES.add(lpe);IES.add(lfe);
			DynamicDependency dd=new DynamicDependency(im.getParentMonitorName(), processName,
					im.getRootMonitorName(),im.getRootInstanceId(),null);
			sendCMD(processName,im.getParentMonitorName(),ComConstants.SUBTX_INIT_ACK,dd.clone());
		}
	}
	public synchronized void receiveFutureCreate(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
			if(dd!=null&&(!IES.contains(dd))){
				IES.add(dd);
				for(StaticEdge edge:outgoEdge){
					DynamicDependency d=new DynamicDependency(processName, edge.getTarget(), dd.getRootMonitorName(), dd.getRootInstanceId(),
							MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
					if(!OES.contains(d)){
						OES.add(d);
						sendCMD(processName,edge.getTarget(),ComConstants.FUTURE_CREATE_NOTIFY,d.clone());
					}
				}
			}
		}
	}
	/**
	 * remove all outgo future paths having same root TX with dd.root
	 * @param dd
	 */
	private synchronized void removeFuture(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
			List<DynamicDependency> remove=new ArrayList<DynamicDependency>();
			List<DynamicDependency> clone=new ArrayList<DynamicDependency>(OES);//TODO multi-thread will not has synchronous and concurrent problems
			for(DynamicDependency outgo:clone){
				if(!outgo.getTargetMonitorName().equals(processName)&&outgo.equalRootTX(dd)&&outgo.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_FUTURE)){
					boolean futureIncome=false;
					for(DynamicDependency income:IES){
						if((!income.getSourceMonitorName().equals(processName))&&income.equalRootTX(dd)&&income.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_FUTURE)){//future income
							futureIncome=true;
							break;
						}
					}
					if(!futureIncome){
						InstanceMonitor im=getSubInstance(dd.getRootMonitorName(), dd.getRootInstanceId());
						if(im!=null){
							StaticEdge edge=new StaticEdge(processName,outgo.getTargetMonitorName());
							String partnerLinkType=edgeMapPartnerLinkType.get(edge);
							if(!internalAnalyser.isFuture(im.getCurrentNode(), partnerLinkType)){//no future use
								remove.add(outgo);
								sendCMD(processName,dd.getTargetMonitorName(),ComConstants.FUTURE_REMOVE_NOTIFY,outgo.clone());
							}
						}
					}
				}
			}
			OES.removeAll(remove);
		}
	}
	
	public void receiveSubTXInitACK(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId()))
			removeFuture(dd);
	}
	public synchronized void receiveFutureRemove(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
			IES.remove(dd);
			removeFuture(dd);
			//remove future may lead to freeness
			if(needUpdate)
				doUpdate();
		}
	}
	public void rootTXEnd(InstanceMonitor im){
		System.out.println(instanceMonitors.toString());
	}
	public void subTXEnd(InstanceMonitor im){
		if(checkInfoUpdateNeeded(im.getRootMonitorName(), im.getRootInstanceId())){
			DynamicDependency dd=new DynamicDependency(im.getParentMonitorName(),processName,im.getRootMonitorName(),im.getRootInstanceId(),
					null);
			sendCMD(processName,im.getParentMonitorName(),ComConstants.SUBTX_END_NOTIFY,dd.clone());
		}
	}
	
	public synchronized void receiveSubTXEnd(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
			InstanceMonitor im=getSubInstance(dd.getRootMonitorName(), dd.getRootInstanceId());
			DynamicDependency past=new DynamicDependency(processName,dd.getTargetMonitorName(),im.getRootMonitorName(),im.getRootInstanceId(),
					MonitorConstants.DYNAMICDEPENDENCY_PAST);
			OES.add(past);
			sendCMD(processName,dd.getTargetMonitorName(),ComConstants.PAST_CREATE_NOTIFY,past.clone());
		}
	}
	public synchronized void receivePastCreate(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
			IES.add(dd);
			boolean noSameRootTX=true;
			for(InstanceMonitor im:instanceMonitors.values()){
				if(im.getRootMonitorName().equals(dd.getRootMonitorName())&&im.getRootInstanceId()==dd.getRootInstanceId()){
					noSameRootTX=false;
					break;
				}
			}
			if(noSameRootTX){
				DynamicDependency lfe=new DynamicDependency(processName, processName, dd.getRootMonitorName(),dd.getRootInstanceId(),
						MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
				DynamicDependency lpe=new DynamicDependency(processName, processName, dd.getRootMonitorName(), dd.getRootInstanceId(),
						MonitorConstants.DYNAMICDEPENDENCY_PAST);
				if(OES.contains(lfe)){
					OES.remove(lfe);OES.remove(lpe);
					IES.remove(lfe);IES.remove(lpe);
				}
				//tx past,and there is any other future?
				removeFuture(dd);
				//past create may lead to freeness
				if(needUpdate){
					doUpdate();
				}
			}
		}
	}
	public void updateCurrentNode(InstanceMonitor im){
		String lastNode=im.getLastNode();
		if(lastNode!=null){
			if(internalAnalyser.isBranchNode(lastNode)){
				reachBranch(im);
			}
		}
	}
	private void reachBranch(InstanceMonitor im) {
		//only future remove should be handle, past can be ignore
		String lastNode=im.getLastNode();
		String currentNode=im.getCurrentNode();
		List<String> futureChange=internalAnalyser.futureSetChange(lastNode, currentNode);
		for(String plt:futureChange){
			StaticEdge edge=partnerLinkTypeMapEdge.get(plt);
			if(edge==null)
				System.out.println(plt+" is not a valid partnerLinkType and static edge");
			else{
				DynamicDependency dd=new DynamicDependency(processName,edge.getTarget(),im.getRootMonitorName(),im.getRootInstanceId(),null);
				removeFuture(dd);
			}
		}
	}
	public synchronized void doUpdate(){
		if(needUpdate){
			if(vcManager.getStrategy().equals(MonitorConstants.STRATEGY_CONCURRENT)){
				if(vcManager.checkDynamicUpdatable()){
					System.out.println("Updating "+processName+" using cv strategy...");
					setupState=MonitorConstants.STATE_UPDATE;
					vcManager.doUpdate();
					//case sleep,state<-update,vm.doUpdate,state<-valid,case wake up,case invoking new url which hasn't been deployed
					//don't need this sleep if cases never use sleep.
					try {
    					Thread.currentThread().sleep(3000);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
					setupState=MonitorConstants.STATE_VALID;
					synchronized(this){
						this.notifyAll();
						
					}
				}
				if(checkFreeness()&&setupState.equals(MonitorConstants.STATE_VALID)){
					receiveCleanupREQ();
					needUpdate=false;
				}
			}else{
				//check freeness//update using double check
				if(checkFreeness()&&setupState.equals(MonitorConstants.STATE_VALID)){
					if(vcManager.getStrategy().equals(MonitorConstants.STRATEGY_WAIT))
						System.out.println("Updating "+processName+" using wait strategy...");
					else if(vcManager.getStrategy().equals(MonitorConstants.STRATEGY_BLOCK))
						System.out.println("Updating "+processName+" using block strategy...");
					setupState=MonitorConstants.STATE_UPDATE;
					if(checkFreeness()){
						vcManager.doUpdate();
						needUpdate=false;
						receiveCleanupREQ();
						synchronized(this){this.notifyAll();}
					}
					else{
						System.out.println("Double check false, update cancelled!");
						setupState=MonitorConstants.STATE_VALID;
						if(vcManager.getStrategy().equals(MonitorConstants.STRATEGY_WAIT))
							synchronized(this){this.notifyAll();}
					}
				}
			}
		}
	}
	
	/**
	 * check whether past and future information needs update. When strategy is wait, it always return true. When strategy is concurrent,
	 * it return true only when this service need update and it has past dependency in IES with same root
	 * @param rootMonitorName
	 * @param rootId
	 * @return
	 */
	private boolean checkInfoUpdateNeeded(String rootMonitorName,long rootId){
		if(!needUpdate||!vcManager.getStrategy().equals(MonitorConstants.STRATEGY_CONCURRENT))
			return true;
		return checkHasPast(rootMonitorName, rootId);
	}
	/**
	 * check whether IES contains past(rootMonitorName,rootId)
	 * @param rootMonitorName
	 * @param rootId
	 * @return
	 */
	public boolean checkHasPast(String rootMonitorName,long rootId){
		DynamicDependency dd=new DynamicDependency(null, null, rootMonitorName, rootId, null);
		for(DynamicDependency past:IES){
			if(past.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_PAST)&&past.equalRootTX(dd))
				return true;
		}
		return false;
	}
	private boolean checkFreeness(){
		System.out.println(IES);
		for(int i=0;i<IES.size();i++){
			DynamicDependency p=IES.get(i);
			for(int j=i+1;j<IES.size();j++){
				DynamicDependency q=IES.get(j);
				if(p.equalRootTX(q)&&(!p.getType().equals(q.getType())))
					return false;
			}
		}
		return true;
	}
	//====================clean up
	public void receiveCleanupREQ(){
		if(!setupState.equals(MonitorConstants.STATE_NORMAL)){
			System.out.println(processName+" clean up!");
			String str="************** Clean up time..."+System.currentTimeMillis()+" *************";
			System.out.println(str);
			TestWriter.writeResult(str);
			setupState=MonitorConstants.STATE_NORMAL;
			cleanup();
			for(StaticEdge edge:incomeEdge){
				sendCMD(processName,edge.getSource(),ComConstants.CLEANUP_REQUEST,null);
			}
		}
	}
	private synchronized void cleanup(){
		scope.clear();
		confirmSetClear();
		IES.clear();OES.clear();
		if(urls.size()==2)
			urls.remove(0);
	}
	
	@Override
	public String toString(){
		StringBuffer s=new StringBuffer();
		s.append(processName+"---------------------------------------------------------------------\t\n");
		s.append("IncomeEdge: "+incomeEdge.toString()+"\t\n");
		s.append("OutgoEdge: "+outgoEdge.toString()+"\t\n");
		s.append("Scope: "+scope.toString()+"\t\n");
		s.append("IES: "+IES.toString()+"\t\n");
		s.append("OES: "+OES.toString()+"\t\n");
		s.append("IncomeEdgeConfirm: "+incomeEdgeConfirm.toString()+"\t\n");
		s.append("OutgoEdgeConfirm: "+outgoEdgeConfirm.toString()+"\t\n");
		s.append("NotifyIncomeFutureDependency: "+notifyIncomeFutureDependencies+"\t\n");
		s.append("NotifyOutgoFutureDependency: "+notifyOutgoFutureDependencies+"\t\n");
		s.append("NotifyIncomePastDependency: "+notifyIncomePastDependencies+"\t\n");
		s.append("NotifyOutgoPastDependency: "+notifyOutgoPastDependencies+"\t\n");
		s.append("SubNotifyIncomeFutureDependency: "+subNotifyIncomeFutureDependencies+"\t\n");
		s.append("SubNotifyOutgoFutureDependency: "+subNotifyOutgoFutureDependencies+"\t\n");
		s.append("SubNotifyIncomePastDependency: "+subNotifyIncomePastDependencies+"\t\n");
		s.append("SubNotifyOutgoPastDependency: "+subNotifyOutgoPastDependencies+"\t\n");
		s.append("/////////////////////////////end///////////////////////////////////////////////////////");
		return s.toString();
	}
}
