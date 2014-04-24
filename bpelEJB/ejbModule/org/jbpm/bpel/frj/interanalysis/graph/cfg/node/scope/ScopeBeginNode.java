/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.scope;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.BeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.AlarmHandler;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.CatchHandler;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.EventHandler;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.Handler;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.PartnerLinkDef;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.VariableDef;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-6-15
 */
/**
 * @author frj
 * 2012-7-9
 */
public class ScopeBeginNode extends BeginNode {

	
	private Map<String,PartnerLinkDef> partnerLinks=new HashMap<String,PartnerLinkDef>(2);
	private Map<String,VariableDef> variables=new HashMap<String,VariableDef>(2);
	private Handler compensationHandler;
	private Handler terminationHandler;
	private Handler catchAll;
	private List<CatchHandler> faultHandlers=new ArrayList<CatchHandler>();
	private List<EventHandler> EventHandlers=new ArrayList<EventHandler>();
	private List<AlarmHandler> AlarmHanders=new ArrayList<AlarmHandler>();
	/**
	 * 
	 */
	public ScopeBeginNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public ScopeBeginNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public ScopeBeginNode(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	public Handler getCompensationHandler() {
		return compensationHandler;
	}

	public void setCompensationHandler(Handler compensationHandler) {
		this.compensationHandler = compensationHandler;
	}

	public Handler getTerminationHandler() {
		return terminationHandler;
	}

	public void setTerminationHandler(Handler terminationHandler) {
		this.terminationHandler = terminationHandler;
	}

	public Handler getCatchAll() {
		return catchAll;
	}

	public void setCatchAll(Handler catchAll) {
		this.catchAll = catchAll;
	}

	public List<CatchHandler> getFaultHandlers() {
		return faultHandlers;
	}

	public List<EventHandler> getEventHandlers() {
		return EventHandlers;
	}

	public List<AlarmHandler> getAlarmHanders() {
		return AlarmHanders;
	}
	public void addFaultHandler(CatchHandler handler){
		this.faultHandlers.add(handler);
	}
	public void addEventHandler(EventHandler eventHandler){
		this.EventHandlers.add(eventHandler);
	}
	public void addAlarmHandler(AlarmHandler alarmHandler){
		this.AlarmHanders.add(alarmHandler);
	}

	public Map<String, PartnerLinkDef> getPartnerLinks() {
		return partnerLinks;
	}

	public Map<String, VariableDef> getVariables() {
		return variables;
	}
	public void addVariableDef(VariableDef v){
		this.variables.put(v.getName(), v);
	}
	public VariableDef getVariable(String variableName){
		return (VariableDef)this.variables.get(variableName);
	}
	public void addPartnerLinkDef(PartnerLinkDef p){
		this.partnerLinks.put(p.getName(), p);
	}
	public PartnerLinkDef getPartnerLink(String partnerLinkName){
		return (PartnerLinkDef)this.partnerLinks.get(partnerLinkName);
	}

	@Override
	public VariableDef getVariabledef(Node node, String variableName) {
		// TODO Auto-generated method stub
		if(this.variables.containsKey(variableName))
			return this.variables.get(variableName);
		else
			return super.getVariabledef(node, variableName);
	}
}
