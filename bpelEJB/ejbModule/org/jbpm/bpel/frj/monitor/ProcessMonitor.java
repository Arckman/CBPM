package org.jbpm.bpel.frj.monitor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jbpm.bpel.frj.VersionControlManager;
import org.jbpm.bpel.frj.communication.SimpleCommunicatorImpl;
import org.jbpm.bpel.frj.communication.remoting.RemotingCommunicator;
import org.jbpm.bpel.frj.interanalysis.mgr.Analyser;
import org.jbpm.bpel.frj.monitor.locator.LocatorUnit;
import org.jbpm.bpel.frj.monitor.locator.LocatorUnit.LinkUnit;
import org.jbpm.bpel.frj.util.ComConstants;
import org.jbpm.bpel.frj.util.MonitorConstants;
import org.jbpm.bpel.frj.util.TestWriter;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
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
	private List<String> uris=new ArrayList<String>(3);//uri in catalina
	//varialbes for quiescence method
	private boolean passive=false;
	private boolean stopInitRootTX=false;
	private boolean allLocalRootTXEnd=false;
	
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
	/**
	 * setup static edge in local environment
	 */
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
							System.out.println(partnerLinkType);
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
	/**
	 * setup static edge in distributed environment, using ProcessLocator.xml
	 * @param lu
	 */
	public void setupStaticEdges(LocatorUnit lu){
		//income
		Set<LinkUnit>incomes=lu.getIncomes();
		if(incomes.size()!=0)
			isRoot=false;
		for(LinkUnit income:incomes){
			StaticEdge edge=new StaticEdge(income.getProcessName(),processName);
			incomeEdge.add(edge);
		}
//		System.out.println(incomeEdge);
		//ougo
		for(LinkUnit outgo:lu.getOutgoes()){
			StaticEdge edge=new StaticEdge(processName,outgo.getProcessName());
			outgoEdge.add(edge);
			edgeMapPartnerLinkType.put(edge, outgo.getPlt());
			partnerLinkTypeMapEdge.put(outgo.getPlt(), edge);
			System.out.println(edge);
		}
//		System.out.println(outgoEdge);
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
		synchronized(instanceMonitors){
		if(instanceMonitors.containsKey(processInstance.getId()))
			System.out.println("Error! Duplicated process instance ID!");
		else
			instanceMonitors.put(processInstance.getId(), im);
		}
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
	public synchronized void resume(){
		System.out.println(processName+" will resumes all instance!");
		suspend=false;
		synchronized(instanceMonitors){
			for(InstanceMonitor im:instanceMonitors.values()){
				synchronized(im){
					im.notifyAll();
				}
			}
		}
		notifyAll();
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
	public void addURI(String uri){
//		if(uris.size()==2)
//			uris.remove(0);//comment for test
		uris.add(uri);
	}
	public String getOldURI(){
		if(uris.size()>0)
			return uris.get(0);
		return null;
	}
	public String getNewURI(){
		int index=uris.size()-1;
		if(index<0)
			return null;
//		return uris.get(index);
		return uris.get(1);//for test
	}
	public List<String> getURIs(){return uris;}
	public int uriNum(){return uris.size();}
	public void Passivate(){passive=true;}
	public boolean isPassive(){return passive;}
	public void activate(){passive=false;}
	public boolean isStopInitRootTX(){return stopInitRootTX;}
	
	private void sendCMD(String source,String target,String cmd,Object o){
//		SimpleCommunicatorImpl c=new SimpleCommunicatorImpl(source,target);
		RemotingCommunicator c=new RemotingCommunicator(source,target);
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
//		System.out.println(processName+" recieve SCOPE_REQ "+edge);
//		System.out.println(toString());
		if(edge!=null){
			if(!scope.contains(edge))
				scope.add(edge);
			synchronized(outgoEdgeConfirm){outgoEdgeConfirm.add(edge);}
		}
		//compute scope
		if(incomeEdge.size()==0)//first process
			receiveScopeACK(null);
		else if(scope.size()==1||scope.size()==0){//first time REQ visit
			synchronized(incomeEdgeConfirm){
				for(StaticEdge e:incomeEdge){
					incomeEdgeConfirm.add(e);
				sendCMD(processName,e.getSource(),ComConstants.SCOPE_REQUEST,e.clone());
				}
			}
//			//TODO clone set for solving asynchronous and concurrent problem
//			Set<StaticEdge> sendSet=new HashSet(incomeEdge);
//			for(StaticEdge e:sendSet)//change synchronize communication to asynchronize
//				sendCMD(e.getTarget(),e.getSource(),ComConstants.SCOPE_REQUEST,e.clone());
		}else{// REQ visit n time
			if(incomeEdgeConfirm.size()==0)//all scope computation before this process done
				receiveScopeACK(edge);
		}
	}
	
	/**
	 * get compute scope ack
	 * @param edge
	 */
	public void receiveScopeACK(StaticEdge edge){
//		System.out.println(processName+" recieve SCOPE_ACK "+edge);
//		System.out.println(toString());
		if(edge!=null){
			synchronized(incomeEdgeConfirm){
				if(incomeEdgeConfirm.contains(edge))
					incomeEdgeConfirm.remove(edge);
			}
		}
		if(incomeEdgeConfirm.size()==0){//all scope computation before this process done
			if(scope.size()==0){//last process,send itself setup request
				receiveSetupREQ(null);
			}else{
//				Set<StaticEdge>remove=new HashSet<StaticEdge>();
				synchronized(outgoEdgeConfirm){
					for(StaticEdge e:outgoEdgeConfirm){
//						remove.add(e);
						sendCMD(processName,e.getTarget(),ComConstants.SCOPE_ACK,e.clone());
					}
					outgoEdgeConfirm.clear();
				}
//				outgoEdgeConfirm.removeAll(remove);
//				for(StaticEdge e:remove)
//					sendCMD(processName,e.getTarget(),ComConstants.SCOPE_ACK,e.clone());
			}
		}
	}
	
	/**
	 * receive setup request
	 * @param edge
	 */
	public void receiveSetupREQ(StaticEdge edge){
//		System.out.println(processName+" recieve Setup_REQ "+edge);
//		System.out.println(toString());
		//suspend
		if(!isSuspend()){
			setSuspend(true);
			System.out.println(processName+" suspend all instance!");
		}
		if(edge!=null){
			synchronized(outgoEdgeConfirm){outgoEdgeConfirm.add(edge);}
		}
		if(outgoEdgeConfirm.size()==scope.size()){//receive all setup request from scope
			setupState=MonitorConstants.STATE_ONDEMAND;
			System.out.println(processName+" set state: "+setupState);
			synchronized(incomeEdgeConfirm){
				for(StaticEdge e:incomeEdge){
					incomeEdgeConfirm.add(e);
					sendCMD(processName,e.getSource(),ComConstants.SETUP_REQUEST,e.clone());
				}
			}
			//TODO clone set for solving asynchronous and concurrent error
//			Set<StaticEdge> sendSet=new HashSet<StaticEdge>(incomeEdge);
//			for(StaticEdge e:sendSet)
//				sendCMD(e.getTarget(),e.getSource(),ComConstants.SETUP_REQUEST,e.clone());
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
//		System.out.println(processName+" recieve Setup_ACK "+edge);
//		System.out.println(toString());
		if(edge!=null)
			if(incomeEdgeConfirm.contains(edge))
				synchronized(incomeEdgeConfirm){incomeEdgeConfirm.remove(edge);}
		if(incomeEdgeConfirm.size()==0&&setupState.equals(MonitorConstants.STATE_SETUP)){//all process setup before done setup and itself setup done
			setupState=MonitorConstants.STATE_VALID;
			System.out.println(processName+" set state: "+setupState);
			synchronized(outgoEdgeConfirm){
				Set<StaticEdge> remove=new HashSet<StaticEdge>();
				for(StaticEdge e:outgoEdgeConfirm){
					remove.add(e);
					sendCMD(processName,e.getTarget(),ComConstants.SETUP_ACK,e.clone());
				}
				outgoEdgeConfirm.removeAll(remove);
			}
			resume();
			if(scope.size()==0&&setupState.equals(MonitorConstants.STATE_VALID)){//last process
				//do freeness to update
				doUpdate();
			}
		}
	}
	
	public void setup(){
		System.out.println(processName+" starts setup...");
		synchronized(instanceMonitors){
		synchronized(notifyOutgoFutureDependencies){
		synchronized(notifyOutgoPastDependencies){
		synchronized(subNotifyOutgoFutureDependencies){
		synchronized(subNotifyOutgoPastDependencies){
		for(InstanceMonitor im:instanceMonitors.values()){
			DynamicDependency lfe=new DynamicDependency(processName,processName,im.getRootMonitorName(),im.getRootInstanceId(),MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
			DynamicDependency lpe=new DynamicDependency(processName,processName,im.getRootMonitorName(),im.getRootInstanceId(),MonitorConstants.DYNAMICDEPENDENCY_PAST);
			OES.add(lpe);OES.add(lfe);
			IES.add(lpe);IES.add(lfe);
			if(im.isRoot()){
				for(StaticEdge edge:scope){
					String currentNodeName=im.getCurrentNode();
					String partnerLinkType=edgeMapPartnerLinkType.get(edge);
					//f(T)
					if(internalAnalyser.isFuture(currentNodeName,partnerLinkType)){
						DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
								MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
						if(!OES.contains(dynamicDependency)){
							OES.add(dynamicDependency);
							notifyOutgoFutureDependencies.add(dynamicDependency);
							sendCMD(processName, edge.getTarget(), ComConstants.FUTURE_NOTIFY, dynamicDependency.clone());
						}
					}
					//p(T)
					if(im.isPast(currentNodeName, partnerLinkType,internalAnalyser)){
						DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
								MonitorConstants.DYNAMICDEPENDENCY_PAST);
						if(!OES.contains(dynamicDependency)){
							OES.add(dynamicDependency);
							notifyOutgoPastDependencies.add(dynamicDependency);
							sendCMD(processName,edge.getTarget(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
						}
					}
					//s(T)
					if(internalAnalyser.isS(currentNodeName, partnerLinkType)){
						if(!internalAnalyser.isFuture(currentNodeName, partnerLinkType)){
							DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
									null);
							subNotifyOutgoFutureDependencies.add(dynamicDependency);
							sendCMD(processName, edge.getTarget(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency);
						}
						if(!im.isPast(currentNodeName, partnerLinkType,internalAnalyser)){
							DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),processName,im.getInstanceId(),
									null);
							subNotifyOutgoPastDependencies.add(dynamicDependency);
							sendCMD(processName, edge.getTarget(), ComConstants.SUB_PAST_NOTIFY,dynamicDependency);
						}
					}
				}
				//TODO Clone set is not a good idea for solving asynchronous error, should call @link{sendCollectionCMD} function
//				Set<DynamicDependency> set;
//				set=new HashSet<DynamicDependency>(notifyOutgoFutureDependencies);
//				for(DynamicDependency dd:set){
//					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.FUTURE_NOTIFY, dd.clone());
//				}
//				set=new HashSet<DynamicDependency>(notifyOutgoPastDependencies);
//				for(DynamicDependency dd:set)
//					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.PAST_NOTIFY, dd.clone());
//				set=new HashSet<DynamicDependency>(subNotifyOutgoFutureDependencies);
//				for(DynamicDependency dd:set)
//					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.SUB_FUTURE_NOTIFY, dd.clone());
//				set=new HashSet<DynamicDependency>(subNotifyOutgoPastDependencies);
//				for(DynamicDependency dd:set)
//					sendCMD(dd.getSourceMonitorName(), dd.getTargetMonitorName(), ComConstants.SUB_PAST_NOTIFY, dd.clone());
			}
		}
		}}}}}
		setupOver();
	}
	
	public void receiveFutureNotify(DynamicDependency dd){
//		System.out.println(processName+" recieve future notify..."+dd);
		if(dd!=null){
			if(!notifyIncomeFutureDependencies.contains(dd)){
				if(!IES.contains(dd))IES.add(dd);
				synchronized(notifyIncomeFutureDependencies){notifyIncomeFutureDependencies.add(dd);}
				synchronized(notifyOutgoFutureDependencies){
					for(StaticEdge edge:scope){
						DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
								dd.getType());
						OES.add(dynamicDependency);
						if(!notifyOutgoFutureDependencies.contains(dynamicDependency)){
							notifyOutgoFutureDependencies.add(dynamicDependency);
							sendCMD(processName,edge.getTarget(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
						}
					}
				}
				//TODO clone set
//				Set<DynamicDependency>sendSet=new HashSet<DynamicDependency>(notifyOutgoFutureDependencies);
//				for(DynamicDependency d:sendSet)
//					sendCMD(processName,d.getTargetMonitorName(),ComConstants.FUTURE_NOTIFY,d.clone());
				receiveFutureACK(dd);
			}
		}
	}
	public void receiveFutureACK(DynamicDependency dd){
//		System.out.println(processName+" recieve future ack..."+dd);
		if(dd!=null){
			if(notifyOutgoFutureDependencies.contains(dd)||
					dd.getTargetMonitorName().equals(processName)){//recieve ack invoked by itself
				synchronized(notifyOutgoFutureDependencies){notifyOutgoFutureDependencies.remove(dd);}
				boolean getAllACK=true;//ACK for root instance dd.rootInstance
				synchronized(notifyOutgoFutureDependencies){
					for(DynamicDependency outgoD:notifyOutgoFutureDependencies){
						if(dd.equalRootTX(outgoD)){
							getAllACK=false;
							break;
						}
					}
				}
				if(getAllACK){
					Set<DynamicDependency> remove=new HashSet<DynamicDependency>();
					synchronized(notifyIncomeFutureDependencies){
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
				}
				if(isRoot)//if want to merge future notify and future create notify, add setupState here
					setupOver();
			}else if(subNotifyOutgoFutureDependencies.contains(dd)){//request sent by sub future
				receiveSubFutureACK(dd);
			}
		}
	}
	public void receivePastNotify(DynamicDependency dd){
//		System.out.println(processName+" recieve past notify..."+dd);
		if(dd!=null){
			if(!notifyIncomePastDependencies.contains(dd)){
				if(!IES.contains(dd))IES.add(dd);
				synchronized(notifyIncomePastDependencies){notifyIncomePastDependencies.add(dd);}
				synchronized(notifyOutgoPastDependencies){
					for(StaticEdge edge:scope){
						DynamicDependency dynamicDependency=new DynamicDependency(processName,edge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
								dd.getType());
						OES.add(dynamicDependency);
						if(!notifyOutgoPastDependencies.contains(dynamicDependency)){
							notifyOutgoPastDependencies.add(dynamicDependency);
							sendCMD(processName,edge.getTarget(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
						}
					}
				}
				//TODO clone set
//				Set<DynamicDependency>sendSet=new HashSet<DynamicDependency>(notifyOutgoPastDependencies);
//				for(DynamicDependency dynamicDependency:sendSet)
//					sendCMD(processName,dynamicDependency.getTargetMonitorName(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
				receivePastACK(dd);
			}
		}
	}
	public void receivePastACK(DynamicDependency dd){
//		System.out.println(processName+" recieve past ack..."+dd);
		if(dd!=null){
			if(notifyOutgoPastDependencies.contains(dd)||
					dd.getTargetMonitorName().equals(processName)){//recieve ack invoked by itself
				synchronized(notifyOutgoPastDependencies){notifyOutgoPastDependencies.remove(dd);}
				boolean getAllACK=true;//ACK for root instance dd.rootInstance
				synchronized(notifyOutgoPastDependencies){
					for(DynamicDependency outgoD:notifyOutgoPastDependencies){
						if(dd.equalRootTX(outgoD)){
							getAllACK=false;
							break;
						}
					}
				}
				if(getAllACK){
					synchronized(notifyIncomePastDependencies){
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
				}
				if(isRoot)
					setupOver();
			}else if(subNotifyOutgoPastDependencies.contains(dd))
				receiveSubPastACK(dd);
		}
	}
	
	public void receiveSubFutureNotify(DynamicDependency dd){
//		System.out.println(processName+" recieve sub future notify..."+dd);
		if(dd!=null){
			if(!subNotifyIncomeFutureDependencies.contains(dd)){
				synchronized(subNotifyIncomeFutureDependencies){
					subNotifyIncomeFutureDependencies.add(dd);
				}
				InstanceMonitor im=getSubInstance(dd.getRootMonitorName(),dd.getRootInstanceId());
				if(im!=null){
					synchronized(subNotifyOutgoFutureDependencies){
						for(StaticEdge outEdge:scope){
							String currentNodeName=im.getCurrentNode();
							String partnerLinkType=edgeMapPartnerLinkType.get(outEdge);
							if(internalAnalyser.isFuture(currentNodeName, partnerLinkType)){
								DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
										MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
								if(!OES.contains(dynamicDependency)){
									OES.add(dynamicDependency);
									subNotifyOutgoFutureDependencies.add(dynamicDependency);//add to sub notify outgo future list
									sendCMD(processName,outEdge.getTarget(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
								}
							}
							if(internalAnalyser.isS(currentNodeName, partnerLinkType)){
								if(!internalAnalyser.isFuture(currentNodeName, partnerLinkType)){
									DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
											null);
									subNotifyOutgoFutureDependencies.add(dynamicDependency);
									sendCMD(processName, outEdge.getTarget(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency.clone());
								}
							}
						}
						//TODO colone set
//					Set<DynamicDependency>sendSet=new HashSet<DynamicDependency>(subNotifyOutgoFutureDependencies);
//					for(DynamicDependency dynamicDependency:sendSet)
//						if(dynamicDependency.getType()==null)
//							sendCMD(processName, dynamicDependency.getTargetMonitorName(), ComConstants.SUB_FUTURE_NOTIFY,dynamicDependency.clone());
//						else if(dynamicDependency.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_FUTURE))
//							sendCMD(processName,dynamicDependency.getTargetMonitorName(),ComConstants.FUTURE_NOTIFY,dynamicDependency.clone());
					}
				}
				receiveSubFutureACK(dd);
			}
		}
	}
	public void receiveSubFutureACK(DynamicDependency dd){
//		System.out.println(processName+" recieve sub future ack..."+dd);
		if(dd!=null){
			if(subNotifyOutgoFutureDependencies.contains(dd))
				synchronized(subNotifyOutgoFutureDependencies){
					subNotifyOutgoFutureDependencies.remove(dd);
				}
		}
		boolean getAllACK=true;//ACK for root instance dd.rootInstance
		synchronized(subNotifyOutgoFutureDependencies){
			for(DynamicDependency outgoD:subNotifyOutgoFutureDependencies){
				if(dd.equalRootTX(outgoD)){
					getAllACK=false;
					break;
				}
			}
		}
		if(getAllACK){
			synchronized(subNotifyIncomeFutureDependencies){
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
		}
		if(isRoot)
			setupOver();
	}
	
	public void receiveSubPastNotify(DynamicDependency dd){
//		System.out.println(processName+" recieve sub past notify..."+dd);
		if(dd!=null){
			if(!subNotifyIncomePastDependencies.contains(dd)){
				synchronized(subNotifyIncomePastDependencies){
					subNotifyIncomePastDependencies.add(dd);
				}
				InstanceMonitor im=getSubInstance(dd.getRootMonitorName(),dd.getRootInstanceId());
				if(im!=null){
					synchronized(subNotifyOutgoPastDependencies){
						for(StaticEdge outEdge:scope){
							String currentNodeName=im.getCurrentNode();
							String partnerLinkType=edgeMapPartnerLinkType.get(outEdge);
							if(im.isPast(currentNodeName, partnerLinkType,internalAnalyser)){
								DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
										MonitorConstants.DYNAMICDEPENDENCY_PAST);
								if(!OES.contains(dynamicDependency)){
									OES.add(dynamicDependency);
									subNotifyOutgoPastDependencies.add(dynamicDependency);//add to sub notify outgo future list
									sendCMD(processName,outEdge.getTarget(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
								}
							}
							if(internalAnalyser.isS(currentNodeName, partnerLinkType)){
								if(!im.isPast(currentNodeName, partnerLinkType,internalAnalyser)){
									DynamicDependency dynamicDependency=new DynamicDependency(processName,outEdge.getTarget(),dd.getRootMonitorName(),dd.getRootInstanceId(),
											null);
									subNotifyOutgoPastDependencies.add(dynamicDependency);
									sendCMD(processName, outEdge.getTarget(), ComConstants.SUB_PAST_NOTIFY,dynamicDependency.clone());
								}
							}
						}
						//TODO clone set
//						Set<DynamicDependency>sendSet=new HashSet<DynamicDependency>(subNotifyOutgoPastDependencies);
//						for(DynamicDependency dynamicDependency:sendSet)
//							if(dynamicDependency.getType()==null)
//								sendCMD(processName, dynamicDependency.getTargetMonitorName(), ComConstants.SUB_PAST_NOTIFY,dynamicDependency.clone());
//							else if(dynamicDependency.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_PAST))
//								sendCMD(processName,dynamicDependency.getTargetMonitorName(),ComConstants.PAST_NOTIFY,dynamicDependency.clone());
					}
				}
				receiveSubPastACK(dd);
			}
		}
	}
	public void receiveSubPastACK(DynamicDependency dd){
//		System.out.println(processName+" recieve sub past ack..."+dd);
		if(dd!=null){
			if(subNotifyOutgoPastDependencies.contains(dd))
				synchronized(subNotifyOutgoPastDependencies){
					subNotifyOutgoPastDependencies.remove(dd);
				}
		}
		boolean getAllACK=true;//ACK for root instance dd.rootInstance
		synchronized(subNotifyOutgoPastDependencies){
			for(DynamicDependency outgoD:subNotifyOutgoPastDependencies){
				if(dd.equalRootTX(outgoD)){
					getAllACK=false;
					break;
				}
			}
		}
		if(getAllACK){
			synchronized(subNotifyIncomePastDependencies){
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
		}
		if(isRoot)
			setupOver();
	}
	
	/**
	 * get sub transaction/instance according to root
	 * @param id
	 * @return
	 */
	private synchronized InstanceMonitor getSubInstance(String rootMonitorName,long id){
		if(instanceMonitors!=null)
			synchronized(instanceMonitors){
			for(InstanceMonitor temp:instanceMonitors.values()){
				if(temp.getRootMonitorName().equals(rootMonitorName)&&temp.getRootInstanceId()==id)
					return temp;
			}
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
//		System.out.println(processName+" in setupOver ");
//		System.out.println(toString());
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
	public void rootTXInit(InstanceMonitor im){
		if(checkInfoUpdateNeeded(im.getRootMonitorName(), im.getRootInstanceId())){
			if(true){
//				System.out.println(im.getPm().getProcessName()+":"+im.getInstanceId()+" in rootTXInit");
				DynamicDependency lfe=new DynamicDependency(processName, processName, processName, im.getInstanceId(),
						MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
				DynamicDependency lpe=new DynamicDependency(processName, processName, processName, im.getInstanceId(),
						MonitorConstants.DYNAMICDEPENDENCY_PAST);
				synchronized(OES){
				OES.add(lfe);OES.add(lpe);
				}
				synchronized(IES){
				IES.add(lfe);IES.add(lpe);
				}
				for(StaticEdge edge:scope){
					DynamicDependency dd=new DynamicDependency(processName, edge.getTarget(), processName, im.getInstanceId(),
							MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
					synchronized(OES){OES.add(dd);}
					sendCMD(processName,edge.getTarget(),ComConstants.FUTURE_CREATE_NOTIFY,dd.clone());
				}
			}
		}
	}
	public synchronized void subTXInit(InstanceMonitor im){
		if(checkInfoUpdateNeeded(im.getRootMonitorName(), im.getRootInstanceId())){
//			System.out.println(im.getPm().getProcessName()+":"+im.getInstanceId()+" in subTXInit");
			DynamicDependency lfe=new DynamicDependency(processName, processName, im.getRootMonitorName(), im.getRootInstanceId(),
					MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
			DynamicDependency lpe=new DynamicDependency(processName, processName, im.getRootMonitorName(), im.getRootInstanceId(),
					MonitorConstants.DYNAMICDEPENDENCY_PAST);
			synchronized(OES){
				OES.add(lfe);OES.add(lpe);
				}
				synchronized(IES){
				IES.add(lfe);IES.add(lpe);
				}
			DynamicDependency dd=new DynamicDependency(im.getParentMonitorName(), processName,
					im.getRootMonitorName(),im.getRootInstanceId(),null);
			sendCMD(processName,im.getParentMonitorName(),ComConstants.SUBTX_INIT_ACK,dd.clone());
		}
	}
	public void receiveFutureCreate(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
//			System.out.println("Child of "+dd.getRootMonitorName()+":"+dd.getRootInstanceId()+" in receiveFutureCreate at "+processName);
			if(dd!=null&&(!IES.contains(dd))){
				synchronized(IES){IES.add(dd);}
				for(StaticEdge edge:scope){
					DynamicDependency d=new DynamicDependency(processName, edge.getTarget(), dd.getRootMonitorName(), dd.getRootInstanceId(),
							MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
					if(!OES.contains(d)){
						synchronized(OES){OES.add(d);}
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
	private void removeFuture(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
//			System.out.println("Child of "+dd.getRootMonitorName()+":"+dd.getRootInstanceId()+" in removeFuture at "+processName);
//			System.out.println(processName+" try remove future "+dd);
//			System.out.println(this);
			List<DynamicDependency> remove=new ArrayList<DynamicDependency>();
			List<DynamicDependency> clone=new ArrayList<DynamicDependency>(OES);//TODO multi-thread will not has synchronous and concurrent problems
			for(DynamicDependency outgo:clone){
				if(!outgo.getTargetMonitorName().equals(processName)&&outgo.equalRootTX(dd)&&outgo.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_FUTURE)){
					boolean futureIncome=false;
					synchronized(IES){
						for(DynamicDependency income:IES){
							if((!income.getSourceMonitorName().equals(processName))&&income.equalRootTX(dd)&&income.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_FUTURE)){//future income
								futureIncome=true;
								break;
							}
						}
					}
					System.out.println("Child of "+dd.getRootMonitorName()+":"+dd.getRootInstanceId()+" has future income?"+futureIncome);
					if(!futureIncome){
						InstanceMonitor im=getSubInstance(dd.getRootMonitorName(), dd.getRootInstanceId());
						if(im!=null){
							StaticEdge edge=new StaticEdge(processName,outgo.getTargetMonitorName());
							String partnerLinkType=edgeMapPartnerLinkType.get(edge);
							if(!internalAnalyser.isFuture(im.getCurrentNode(), partnerLinkType)){//no future use
								System.out.println("Child of "+dd.getRootMonitorName()+":"+dd.getRootInstanceId()+" no future use");
								remove.add(outgo);
								sendCMD(processName,outgo.getTargetMonitorName(),ComConstants.FUTURE_REMOVE_NOTIFY,outgo.clone());
							}
						}else{//im==null means current instance end
							remove.add(outgo);
							sendCMD(processName,outgo.getTargetMonitorName(),ComConstants.FUTURE_REMOVE_NOTIFY,outgo.clone());
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
	public void receiveFutureRemove(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
//			System.out.println("Child of "+dd.getRootMonitorName()+":"+dd.getRootInstanceId()+" in receiveFutureRemove at "+processName);
			synchronized(IES){IES.remove(dd);}
			removeFuture(dd);
			//remove future may lead to freeness
			if(needUpdate)
				doUpdate();
		}
	}
	public void rootTXEnd(InstanceMonitor im){
//		System.out.println(instanceMonitors.toString());
		if(vcManager.getMethod().equals(MonitorConstants.METHOD_QUIESCENCE))
			receivePassiveACK(null);
		
	}
	public void subTXEnd(InstanceMonitor im){
		if(checkInfoUpdateNeeded(im.getRootMonitorName(), im.getRootInstanceId())){
//			System.out.println(im.getPm().getProcessName()+":"+im.getInstanceId()+"-"+im.getRootInstanceId()+" in subTXEnd");
			DynamicDependency dd=new DynamicDependency(im.getParentMonitorName(),processName,im.getRootMonitorName(),im.getRootInstanceId(),
					null);
			sendCMD(processName,im.getParentMonitorName(),ComConstants.SUBTX_END_NOTIFY,dd.clone());
		}
	}
	
	public void receiveSubTXEnd(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
//			System.out.println("Child of "+dd.getRootInstanceId()+" recieve subTXEnd at "+processName);
			InstanceMonitor im=getSubInstance(dd.getRootMonitorName(), dd.getRootInstanceId());
			DynamicDependency past=new DynamicDependency(processName,dd.getTargetMonitorName(),im.getRootMonitorName(),im.getRootInstanceId(),
					MonitorConstants.DYNAMICDEPENDENCY_PAST);
			OES.add(past);
			sendCMD(processName,dd.getTargetMonitorName(),ComConstants.PAST_CREATE_NOTIFY,past.clone());
		}
	}
	public void receivePastCreate(DynamicDependency dd){
		if(checkInfoUpdateNeeded(dd.getRootMonitorName(), dd.getRootInstanceId())){
//			System.out.println("Child of "+dd.getRootMonitorName()+":"+dd.getRootInstanceId()+" in receivePastCreate at "+processName);
			synchronized(IES){IES.add(dd);}
			boolean noSameRootTX=true;
			synchronized(instanceMonitors){
			for(InstanceMonitor im:instanceMonitors.values()){
				if(im.getRootMonitorName().equals(dd.getRootMonitorName())&&im.getRootInstanceId()==dd.getRootInstanceId()){
					noSameRootTX=false;
					break;
				}
			}
			}
			if(noSameRootTX){
				DynamicDependency lfe=new DynamicDependency(processName, processName, dd.getRootMonitorName(),dd.getRootInstanceId(),
						MonitorConstants.DYNAMICDEPENDENCY_FUTURE);
				DynamicDependency lpe=new DynamicDependency(processName, processName, dd.getRootMonitorName(), dd.getRootInstanceId(),
						MonitorConstants.DYNAMICDEPENDENCY_PAST);
				if(OES.contains(lfe)){
//					System.out.println(processName+" create past and remove "+lfe);
					synchronized(OES){OES.remove(lfe);OES.remove(lpe);}
					synchronized(IES){IES.remove(lfe);IES.remove(lpe);}
				}else
					System.out.println(processName+" create past and try remove "+lfe+" ,but not found");
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
			if(vcManager.getMethod().equals(MonitorConstants.METHOD_QUIESCENCE)){
				System.out.println("Updateing "+processName+" using quiescence method...");
				setupState=MonitorConstants.STATE_UPDATE;
				vcManager.doUpdate();
				needUpdate=false;
				receiveCleanupREQ();
				setupState=MonitorConstants.STATE_NORMAL;
				synchronized(this){this.notifyAll();}
			}else if(vcManager.getMethod().equals(MonitorConstants.METHOD_VC)){
				if(vcManager.getStrategy().equals(MonitorConstants.STRATEGY_CONCURRENT)){
					if(vcManager.checkDynamicUpdatable()){
						System.out.println("Updating "+processName+" using cv strategy...");
						setupState=MonitorConstants.STATE_UPDATE;
						vcManager.doUpdate();
						//case sleep,state<-update,vm.doUpdate,state<-valid,case wake up,case invoking new url which hasn't been deployed
						//don't need this sleep if cases never use sleep.
//					try {
//    					Thread.currentThread().sleep(3000);
//    				} catch (InterruptedException e) {
//    					// TODO Auto-generated catch block
//    					e.printStackTrace();
//    				}
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
						synchronized(this){
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
//		System.out.println("Child of "+rootMonitorName+":"+rootId+" in checkHasPast");
		DynamicDependency dd=new DynamicDependency(null, null, rootMonitorName, rootId, null);
		synchronized(IES){
			for(DynamicDependency past:IES){
				if(past.getType().equals(MonitorConstants.DYNAMICDEPENDENCY_PAST)&&past.equalRootTX(dd))
					return true;
			}
		}
		return false;
	}
	private synchronized boolean checkFreeness(){
//		System.out.println(IES);
		synchronized(IES){
			for(int i=0;i<IES.size();i++){
				DynamicDependency p=IES.get(i);
				for(int j=i+1;j<IES.size();j++){
					DynamicDependency q=IES.get(j);
					if(p.equalRootTX(q)&&(!p.getType().equals(q.getType())))
						return false;
				}
			}
		}
		return true;
	}
	//=======================quiescence=================================================
	public void receivePassiveREQ(StaticEdge edge){
		System.out.println(processName+" recieve passive request:"+edge);
		if(edge!=null){
			synchronized(outgoEdgeConfirm){outgoEdgeConfirm.add(edge);}
		}
//		System.out.println(toString());
		if(!stopInitRootTX){
			stopInitRootTX=true;
			synchronized(incomeEdgeConfirm){
				for(StaticEdge e:incomeEdge){
					incomeEdgeConfirm.add(e);
					sendCMD(processName,e.getSource(),ComConstants.PASSIVE_REQUEST,e.clone());
				}
			}
		}
		receivePassiveACK(edge);
	}
	public boolean checkPassive(){
//		System.out.println(processName+" in checkPassive()");
//		System.out.println(toString());
		if(stopInitRootTX&&(!isLocalRootTXRunning())){
			passive=true;
			System.out.println(processName+" is passive!");
		}
		return passive;
	}
	private boolean isLocalRootTXRunning(){
		if(!stopInitRootTX)
			return true;
		synchronized(instanceMonitors){
			for(InstanceMonitor im:instanceMonitors.values()){
				if(im.isRoot()){
					allLocalRootTXEnd=false;
					return true;
				}
			}
		}
		allLocalRootTXEnd=true;
		return false;
	}
	public void receivePassiveACK(StaticEdge edge){
		System.out.println(processName+" recieve passive ack:"+edge);
		if(incomeEdgeConfirm.contains(edge)){
			synchronized(incomeEdgeConfirm){
				incomeEdgeConfirm.remove(edge);
			}
		}
//		System.out.println(toString());
		//passive and recieve all ack
		if((passive||checkPassive())&&(incomeEdgeConfirm.size()==0)){
			if(outgoEdgeConfirm.size()==0&&needUpdate){//target service
				doUpdate();
			}else{//normal service
				synchronized(outgoEdgeConfirm){
					for(StaticEdge e:outgoEdgeConfirm){
						sendCMD(processName,e.getTarget(),ComConstants.PASSIVE_ACK,e.clone());
					}
					outgoEdgeConfirm.clear();
				}
			}
		}
	}
	//====================clean up
	public void receiveCleanupREQ(){
		System.out.println(processName+" clean up!");
		String str="************** Clean up time..."+System.currentTimeMillis()+" *************";
		System.out.println(str);
		TestWriter.writeResult(System.currentTimeMillis());
		if(vcManager.getMethod().equals(MonitorConstants.METHOD_QUIESCENCE)){
			synchronized(this){
				stopInitRootTX=false;
				allLocalRootTXEnd=false;
				passive=false;
				notifyAll();
			}
		}else if(vcManager.getMethod().equals(MonitorConstants.METHOD_VC)){
			if(!setupState.equals(MonitorConstants.STATE_NORMAL)){
				setupState=MonitorConstants.STATE_NORMAL;
			}
		}
		cleanup();//clean up set
		for(StaticEdge edge:incomeEdge){
			sendCMD(processName,edge.getSource(),ComConstants.CLEANUP_REQUEST,null);
		}
	}
	private void cleanup(){
		scope.clear();
		confirmSetClear();
		synchronized(IES){IES.clear();}
		synchronized(OES){OES.clear();}
		if(urls.size()==2)
//			urls.remove(0);
			urls.remove(1);//for test
		if(uris.size()==3)
			uris.remove(2);
		TestWriter.clear();//for test
	}
	
	@Override
	public String toString(){
		StringBuffer s=new StringBuffer();
		s.append("---------------------------------------------------------------------------\t\n");
		s.append(processName+"\t\n");
//		s.append("IncomeEdge: "+incomeEdge.toString()+"\t\n");
//		s.append("OutgoEdge: "+outgoEdge.toString()+"\t\n");
//		s.append("Passive:"+passive+",StopInitRootTX:"+stopInitRootTX+",AllLocalRootTXEnd:"+allLocalRootTXEnd+"\t\n");
//		s.append("Scope: "+scope.toString()+"\t\n");
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
