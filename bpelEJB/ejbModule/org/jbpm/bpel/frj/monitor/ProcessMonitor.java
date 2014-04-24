package org.jbpm.bpel.frj.monitor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.integration.jms.IntegrationControl;
import org.jbpm.graph.exe.ProcessInstance;
//need synchronize for multi-threads
public class ProcessMonitor {
	private VersionControlManager vcManager;
	private String processName;
	private long processId;
	private List<DynamicDependency> OES=new ArrayList<DynamicDependency>();
	private List<DynamicDependency> IES=new ArrayList<DynamicDependency>();
	private boolean needUpdate=false;
	//namespace
	private String nameSpace;
	private String setupState=MonitorConstants.SETUPSTATE_NORMAL;
	private Analyser internalAnalyser;
	private boolean isRoot=true;
	private List<StaticEdge> incomeEdge=new ArrayList<StaticEdge>();
	private List<StaticEdge> outgoEdge=new ArrayList<StaticEdge>();
	private Map<StaticEdge,String>edgeMapPartnerLinkType=new HashMap<StaticEdge,String>();//edge-->partnerLinkType
	private Map<String,StaticEdge>partnerLinkTypeMapEdge=new HashMap<String,StaticEdge>();//partnerLinkType-->edge
	private boolean suspend=false;
	private Map<Long,InstanceMonitor> instanceMonitors=new HashMap<Long,InstanceMonitor>();
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
	
	//======================on-demand functions
	/**
	 * get compute scope request
	 * @param edge
	 */
	public void receiveScopeREQ(StaticEdge edge){
		//suspend
		if(!isSuspend())
			setSuspend(true);
		if(edge!=null){
			if(!scope.contains(edge))
				scope.add(edge);
			outgoEdgeConfirm.add(edge);
		}
		//compute scope
		if(incomeEdge.size()==0)//first process
			receiveScopeACK(null);
		else if(scope.size()==0){//first time REQ visit
			for(StaticEdge e:incomeEdge){
				incomeEdgeConfirm.add(e);
//				sendCMD(processName,e.getSource(),ComConstants.SCOPE_REQUEST,e.clone());
			}
			for(StaticEdge e:incomeEdgeConfirm)//change synchronize communication to asynchronize
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
		if(edge!=null)
			incomeEdgeConfirm.remove(edge);
		if(incomeEdgeConfirm.size()==0){//all scope computation before this process done
			if(scope.size()==0){//last process,send itself setup request
				receiveSetupREQ(null);
			}else{
				Set<StaticEdge>remove=new HashSet<StaticEdge>();
				for(StaticEdge e:outgoEdgeConfirm){
					remove.add(e);
					sendCMD(processName,e.getTarget(),ComConstants.SCOPE_ACK,e.clone());
				}
				outgoEdgeConfirm.removeAll(remove);
			}
		}
	}
	
	/**
	 * receive setup request
	 * @param edge
	 */
	public void receiveSetupREQ(StaticEdge edge){
		if(edge!=null){
			outgoEdgeConfirm.add(edge);
		}
		if(outgoEdgeConfirm.size()==scope.size()){//receive all setup request from scope
			System.out.println(processName+" scope: "+scope.toString());
			setupState=MonitorConstants.SETUPSTATE_ONDEMAND;
			System.out.println(processName+" set state: "+setupState);
			for(StaticEdge e:incomeEdge){
				incomeEdgeConfirm.add(e);
//				sendCMD(processName,e.getSource(),ComConstants.SETUP_REQUEST,e.clone());
			}
			for(StaticEdge e:incomeEdgeConfirm)
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
		if(edge!=null)
			incomeEdgeConfirm.remove(edge);
		if(incomeEdgeConfirm.size()==0&&setupState.equals(MonitorConstants.SETUPSTATE_SETUP)){//all process setup before done setup and itself setup done
			setupState=MonitorConstants.SETUPSTATE_VALID;
			System.out.println(processName+" set state: "+setupState);
			Set<StaticEdge> remove=new HashSet<StaticEdge>();
			for(StaticEdge e:outgoEdgeConfirm){
				remove.add(e);
				sendCMD(processName,e.getTarget(),ComConstants.SETUP_ACK,e.clone());
			}
			outgoEdgeConfirm.removeAll(remove);
			resume();
			if(scope.size()==0&&setupState.equals(MonitorConstants.SETUPSTATE_VALID)){//last process
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
					if(internalAnalyser.isPast(currentNodeName, partnerLinkType)){
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
						if(!internalAnalyser.isPast(currentNodeName, partnerLinkType)){
							DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
									null);
							subNotifyOutgoPastDependencies.add(dynamicDependency);
//							sendCMD(processName, edge.getTarget(), ComConstants.SUB_PAST_NOTIFY,dynamicDependency);
						}
					}
				}
				for(DynamicDependency dd:notifyOutgoFutureDependencies){
					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.FUTURE_NOTIFY, dd.clone());
				}
				for(DynamicDependency dd:notifyOutgoPastDependencies)
					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.PAST_NOTIFY, dd.clone());
				for(DynamicDependency dd:subNotifyOutgoFutureDependencies)
					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.SUB_FUTURE_NOTIFY, dd.clone());
				for(DynamicDependency dd:subNotifyOutgoPastDependencies)
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
					if(!notifyOutgoFutureDependencies.contains(dynamicDependency)){
						notifyOutgoFutureDependencies.add(dynamicDependency);
//						sendCMD(processName,edge.getTarget(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
					}
				}
				for(DynamicDependency d:notifyOutgoFutureDependencies)
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
					if(!notifyOutgoPastDependencies.contains(dynamicDependency)){
						notifyOutgoPastDependencies.add(dynamicDependency);
//						sendCMD(processName,edge.getTarget(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
					}
				}
				for(DynamicDependency dynamicDependency:notifyOutgoPastDependencies)
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
	
	public void receiveSubFutureREQ(DynamicDependency dd){
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
					for(DynamicDependency dynamicDependency:subNotifyOutgoFutureDependencies)
						if(dynamicDependency.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_FUTURE))
							sendCMD(processName,dynamicDependency.getTargetMonitorName(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
						else if(dynamicDependency.getType()==null)
							sendCMD(processName, dynamicDependency.getTargetMonitorName(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency.clone());
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
						if(internalAnalyser.isPast(currentNodeName, partnerLinkType)){
							DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
									MonitorConstants.DYNAMICDEPENDENCY_PAST);
							if(!OES.contains(dynamicDependency)){
								OES.add(dynamicDependency);
								subNotifyOutgoPastDependencies.add(dynamicDependency);//add to sub notify outgo future list
		//						sendCMD(processName,outEdge.getTarget(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
							}
						}
						if(internalAnalyser.isS(currentNodeName, partnerLinkType)){
							if(!internalAnalyser.isPast(currentNodeName, partnerLinkType)){
								DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
										null);
								subNotifyOutgoPastDependencies.add(dynamicDependency);
		//						sendCMD(processName, outEdge.getTarget(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency.clone());
							}
						}
					}
					for(DynamicDependency dynamicDependency:subNotifyOutgoPastDependencies)
						if(dynamicDependency.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_PAST))
							sendCMD(processName,dynamicDependency.getTargetMonitorName(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
						else if(dynamicDependency.getType()==null)
							sendCMD(processName, dynamicDependency.getTargetMonitorName(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency.clone());
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
				notifyOutgoPastDependencies.size()==0&&subNotifyOutgoPastDependencies.size()==0&&setupState==MonitorConstants.SETUPSTATE_ONDEMAND){
			setupState=MonitorConstants.SETUPSTATE_SETUP;
			System.out.println(processName+" setup ends!");
			if(incomeEdgeConfirm.size()==0)//two way to invoke last process setup ACK. 1.finish setup;2.receive all ACK.
				//We don't know which event comes first
				receiveSetupACK(null);
		}
	}
	
	public void blockInstances(){
		
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
			for(DynamicDependency outgo:OES){
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
	
	public synchronized void receiveSubTXInitACK(DynamicDependency dd){
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
		
	}
	public synchronized void subTXEnd(InstanceMonitor im){
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
					System.out.println(IES);
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
			if(vcManager.getStrategy().equals(MonitorConstants.STATRGY_CONCURRENT)){
				if(vcManager.checkDynamicUpdatable()){
					System.out.println("Updating "+processName+" using cv strategy...");
					vcManager.doUpdate();
				}
				if(checkFreeness()&&setupState.equals(MonitorConstants.SETUPSTATE_VALID)){
					needUpdate=false;
					receiveCleanupREQ();
				}
			}else if(vcManager.getStrategy().equals(MonitorConstants.STRATRGY_WAIT))
				//check freeness
				if(checkFreeness()&&setupState.equals(MonitorConstants.SETUPSTATE_VALID)){
					System.out.println("Updating "+processName+" using wait strategy...");
					vcManager.doUpdate();
					needUpdate=false;
					receiveCleanupREQ();
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
		if(!needUpdate||vcManager.getStrategy().equals(MonitorConstants.STRATRGY_WAIT))
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
		if(vcManager.getStrategy().equals(MonitorConstants.STATRGY_CONCURRENT)){
			DynamicDependency dd=new DynamicDependency(null, null, rootMonitorName, rootId, null);
			for(DynamicDependency past:IES){
				if(past.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_PAST)&&past.equalRootTX(dd))
					return true;
			}
		}
		return false;
	}
	private boolean checkFreeness(){
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
		if(!setupState.equals(MonitorConstants.SETUPSTATE_NORMAL)){
			System.out.println(processName+" clean up!");
			setupState=MonitorConstants.SETUPSTATE_NORMAL;
			cleanup();
			for(StaticEdge edge:incomeEdge){
				sendCMD(processName,edge.getSource(),ComConstants.CLEANUP_REQUEST,null);
			}
		}
	}
	private void cleanup(){
		scope.clear();
		confirmSetClear();
		IES.clear();OES.clear();
		if(urls.size()==2)
			urls.remove(0);
	}
}