/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;


import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.AlarmHandler;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.CatchHandler;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.EventHandler;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.scope.ScopeBeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.scope.ScopeEndNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.PartnerLinkDef;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.VariableDef;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Catch;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.OnAlarm;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.OnEvent;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.PartnerLinkDefinition;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;


/**
 * @author frj
 * 2012-5-11
 */
public class ScopeTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public ScopeTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jbpm.frj.graph.cfg.tranformer.Transformer#transform(org.jbpm.frj.graph.cfg.node.Node, org.jbpm.frj.graph.cfg.node.Node, modeling.model.Activity)
	 */
	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		ScopeBeginNode begin=new ScopeBeginNode(activity);
		ScopeEndNode end=new ScopeEndNode(activity);
		Scope scope=(Scope)activity;
		this.setStandardProperty(currentPrior, begin, end);
		this.setConnection(currentPrior, currentNext, begin, end);
		this.transformScope(scope, begin, end);
		return end;
	}

	private void setStandardProperty(Node prior,ScopeBeginNode begin,ScopeEndNode end){
		//begin->end;end->begin
		begin.setEnd(end);
		end.setBegin(begin);
		//begin&end->org.jbpm.frj.graph
		CFGGraph graph=prior.getGraph();
		begin.setGraph(graph);
		end.setGraph(graph);
		//org.jbpm.frj.graph->begin
		graph.addNode(begin);
	}
	private void setConnection(Node prior,Node next,ScopeBeginNode begin,ScopeEndNode end){
		// prior.remove(next)
		if (prior.hasNextNode(next))
			prior.removeNextNode(next);
		// next.remove(prior)
		if (next.hasPriorNode(prior))
			next.removePriorNode(prior);
		//prior.next=begin
		prior.addNextNode(begin);
		begin.addPriorNode(prior);
		//next.prior=end
		next.addPriorNode(end);
		end.addNextNode(next);
		// begin.next=end; end.prior=begin
		begin.addNextNode(end);
		end.addPriorNode(begin);
	}
	private void transformScope(Scope scope,ScopeBeginNode begin,ScopeEndNode end){
		//variables and partnerlinks
		this.transformerVariables(scope, begin);
		this.transformPartnerLinks(scope, begin);
		
		//nested activities
		Activity subActivity=scope.getActivity();
		this.getBuildCFG().transformActivity(subActivity, begin, end);
		
		//configure all handlers...
		org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Handler tempHandler=null;
		//compensationHandler
		tempHandler=scope.getCompensationHandler();
		if(tempHandler!=null){
			org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.Handler compensationHandler=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.Handler(scope.getCompensationHandler());
			this.transformHandler(compensationHandler, scope.getCompensationHandler(),begin);
			begin.setCompensationHandler(compensationHandler);
		}
		//terminationHandler
		tempHandler=scope.getTerminationHandler();
		if(tempHandler!=null){
			org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.Handler terminationHandler=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.Handler(scope.getTerminationHandler());
			this.transformHandler(terminationHandler, scope.getTerminationHandler(),begin);
			begin.setTerminationHandler(terminationHandler);
		}
		//catchAll
		tempHandler=scope.getCatchAll();
		if(tempHandler!=null){
			org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.Handler catchAll=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.Handler(scope.getCatchAll());
			this.transformHandler(catchAll, scope.getCatchAll(),begin);
			begin.setCatchAll(catchAll);
		}
		List list=null;
		Iterator i=null;
		//faultHandlers
		list=scope.getFaultHandlers();
		i=list.iterator();
		while(i.hasNext()){
			Catch _catch=(Catch)i.next();
			CatchHandler catchHandler=new CatchHandler(_catch);
			catchHandler.setGraph(begin.getGraph());
			this.transformCatch(_catch, catchHandler);
			begin.addFaultHandler(catchHandler);
		}
		//eventHandlers
		list=scope.getOnEvents();
		i=list.iterator();
		while(i.hasNext()){
			OnEvent onEvent=(OnEvent)i.next();
			EventHandler eventHandler=new EventHandler(onEvent);
			eventHandler.setGraph(begin.getGraph());
			this.transformHandler(eventHandler, onEvent, begin);
			begin.addEventHandler(eventHandler);
		}
		//alarmHandlers
		list=scope.getOnAlarms();
		i=list.iterator();
		while(i.hasNext()){
			OnAlarm onAlarm=(OnAlarm)i.next();
			AlarmHandler alarmHandler=new AlarmHandler(onAlarm);
			alarmHandler.setGraph(begin.getGraph());
			this.transformHandler(alarmHandler, onAlarm, begin);
			begin.addAlarmHandler(alarmHandler);
		}
	}
	private void transformHandler(org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler.Handler handler,org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Handler originalHandler,ScopeBeginNode begin){
		handler.setGraph(begin.getGraph());
		Activity activity=originalHandler.getActivity();
		this.getBuildCFG().transformActivity(activity, handler, handler);
		handler.setNode(handler.getNext().get(0));
	}
	private void transformCatch(Catch _catch,CatchHandler catchHandler){
		Activity activity=_catch.getActivity();
		this.getBuildCFG().transformActivity(activity, catchHandler, catchHandler);
		catchHandler.setNode(catchHandler.getNext().get(0));
	}
	private void transformerVariables(Scope scope,ScopeBeginNode begin){
		Map scopeV=scope.getVariables();
		Iterator i=scopeV.values().iterator();
		while(i.hasNext()){
			VariableDefinition scopeVD=(VariableDefinition)i.next();
			VariableDef beginVD=new VariableDef(scopeVD);
			begin.addVariableDef(beginVD);
		}
	}
	private void transformPartnerLinks(Scope scope,ScopeBeginNode begin){
		Map scopeP=scope.getPartnerLinks();
		Iterator i=scopeP.values().iterator();
		while(i.hasNext()){
			PartnerLinkDefinition scopePD=(PartnerLinkDefinition)i.next();
			PartnerLinkDef beginPD=new PartnerLinkDef(scopePD);
			begin.addPartnerLinkDef(beginPD);
		}
	}
}
