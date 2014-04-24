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
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.sequence.SequenceBeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.sequence.SequenceEndNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Sequence;


/**
 * @author frj
 * 2012-5-11
 */
public class SequenceTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public SequenceTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jbpm.frj.graph.cfg.tranformer.Transformer#transform(org.jbpm.frj.graph.cfg.node.Node, org.jbpm.frj.graph.cfg.node.Node, modeling.model.Activity)
	 */
	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		SequenceBeginNode begin=new SequenceBeginNode(activity);
		SequenceEndNode end=new SequenceEndNode(activity);
		this.setStandardProperty(begin, end, currentPrior.getGraph());
		this.setConnection(currentPrior, currentNext, begin, end);
		List<Activity> subActivities=((Sequence)activity).getActivites();
		Node node=begin;
		Iterator i=subActivities.iterator();
		while(i.hasNext()){
			Activity subActivity=(Activity)i.next();
			node=this.getBuildCFG().transformActivity(subActivity, node, end);
		}
		return end;
	}
	private void setStandardProperty(SequenceBeginNode begin,SequenceEndNode end,CFGGraph graph){
		//begin.end=end;	end.begin=begin
		begin.setEnd(end);
		end.setBegin(begin);
		//begin.graph=org.jbpm.frj.graph;	end.graph=org.jbpm.frj.graph
		begin.setGraph(graph);
		end.setGraph(graph);
		//org.jbpm.frj.graph.add(begin);
		graph.addNode(begin);
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
}
