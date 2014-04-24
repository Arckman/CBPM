/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Catch;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Handler;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.OnAlarm;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.OnEvent;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.CorrelationSetDefinition;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.PartnerLinkDefinition;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;


/**
 * @author frj
 * 2012-3-12
 */
public class Scope extends CompositeActivity {
	
	private Activity activity;
	private Map<String,VariableDefinition> variables;
	private Map<String,PartnerLinkDefinition> partnerLinks;
	private Map<String,CorrelationSetDefinition> correlationSets;
	private Handler compensationHandler;
	private Handler terminationHandler;
	private Handler catchAll;
	private List<Handler> faultHandlers=new ArrayList<Handler>();
	private List<OnEvent> onEvents=new ArrayList<OnEvent>();
	private List<OnAlarm> onAlarms=new ArrayList<OnAlarm>();
	

	/**
	 * @param str
	 */
	public Scope(String str) {
		super(str);
		this.correlationSets=new HashMap<String,CorrelationSetDefinition>();
		this.partnerLinks=new HashMap<String,PartnerLinkDefinition>();
		this.variables=new HashMap<String,VariableDefinition>();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Scope(String str, Activity a, Activity b) {
		super(str, a, b);
		this.correlationSets=new HashMap<String,CorrelationSetDefinition>();
		this.partnerLinks=new HashMap<String,PartnerLinkDefinition>();
		this.variables=new HashMap<String,VariableDefinition>();
		// TODO Auto-generated constructor stub
	}

	
	
	@Override
	public void addChild(Activity activity){
		setActivity(activity);
	}
	
	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public Map<String, VariableDefinition> getVariables() {
		return variables;
	}

	public Map<String, PartnerLinkDefinition> getPartnerLinks() {
		return partnerLinks;
	}

	public Map<String, CorrelationSetDefinition> getCorrelationSets() {
		return correlationSets;
	}
	
	public void addVariable(VariableDefinition variable){
		this.variables.put(variable.getName(), variable);
	}
	
	public void addPartnerLink(PartnerLinkDefinition partnerLink){
		this.partnerLinks.put(partnerLink.getName(), partnerLink);
	}
	
	public void addCorrelationSet(CorrelationSetDefinition correlationSet){
		this.correlationSets.put(correlationSet.getName(), correlationSet);
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

	public List<Handler> getFaultHandlers() {
		return faultHandlers;
	}

	public List<OnEvent> getOnEvents() {
		return onEvents;
	}

	public List<OnAlarm> getOnAlarms() {
		return onAlarms;
	}
	
	public void addFaultHandler(Catch _catch){
		this.faultHandlers.add(_catch);
	}
	public void addOnEvent(OnEvent onEvent){
		this.onEvents.add(onEvent);
	}
	public void addOnAlarm(OnAlarm onAlarm){
		this.onAlarms.add(onAlarm);
	}
	
	public VariableDefinition findVariable(String name){
		VariableDefinition variable=this.variables.get(name);
		if(variable==null){
			variable=super.findVariable(name);
		}
		return variable;
	}
}

