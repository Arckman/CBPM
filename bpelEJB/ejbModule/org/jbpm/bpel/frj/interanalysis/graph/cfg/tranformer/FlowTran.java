/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;


import java.util.Iterator;
import java.util.List;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.flow.FlowBeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.flow.FlowEndNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.LinkDef;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Flow;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Flow.LinkDefinition;


/**
 * @author frj
 * 2012-5-12
 */
public class FlowTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public FlowTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jbpm.frj.graph.cfg.tranformer.Transformer#transform(org.jbpm.frj.graph.cfg.node.Node, org.jbpm.frj.graph.cfg.node.Node, modeling.model.Activity)
	 */
	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		FlowBeginNode flowBeginNode=new FlowBeginNode(activity);
		FlowEndNode flowEndNode=new FlowEndNode(activity);
		this.setStandardProperty(flowBeginNode, flowEndNode, currentPrior.getGraph());
		this.setConnection(currentPrior, currentNext, flowBeginNode, flowEndNode);
		this.transformFlow(flowBeginNode, flowEndNode, (Flow)activity);
		return flowEndNode;
	}
	
	private void setStandardProperty(FlowBeginNode beginNode,FlowEndNode endNode,CFGGraph graph){
		//begin.end=end;	end.begin=begin
		beginNode.setEnd(endNode);
		endNode.setBegin(beginNode);
		//begin.graph=org.jbpm.frj.graph;	end.graph=org.jbpm.frj.graph
		beginNode.setGraph(graph);
		endNode.setGraph(graph);
		//org.jbpm.frj.graph.add(begin);
		graph.addNode(beginNode);
	}
	
	private void setConnection(Node prior,Node next,Node beginNode,Node endNode){
		// prior.remove(next)
		if (prior.hasNextNode(next))
			prior.removeNextNode(next);
		// next.remove(prior)
		if (next.hasPriorNode(prior))
			next.removePriorNode(prior);
		//prior.next=begin
		prior.addNextNode(beginNode);
		beginNode.addPriorNode(prior);
		//next.prior=end
		next.addPriorNode(endNode);
		endNode.addNextNode(next);
		// begin.next=end; end.prior=begin
		beginNode.addNextNode(endNode);
		endNode.addPriorNode(beginNode);
	}
	
	private void transformFlow(FlowBeginNode begin,FlowEndNode end,Flow flow){
		//activities
		List<Activity> activities=flow.getActivites();
		Iterator i=activities.iterator();
		while(i.hasNext()){
			Activity activity=(Activity)i.next();
			this.getBuildCFG().transformActivity(activity, begin, end);
		}
		//links
		Iterator linkI=flow.getLink().values().iterator();
		while(linkI.hasNext()){
			LinkDefinition linkD=(LinkDefinition)linkI.next();
			LinkDef linkDef=new LinkDef(linkD);
			Activity sourceAct=linkD.getSource();
			Activity targetAct=linkD.getTarget();
			Node sourceN=begin.getGraph().findNode(sourceAct);
			Node targetN=begin.getGraph().findNode(targetAct);
			linkDef.setSource(sourceN);
			linkDef.setTarget(targetN);
			begin.addLink(linkDef);
		}
	}
}
