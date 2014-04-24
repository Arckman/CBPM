/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;

import java.util.List;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.branch.BranchBeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.branch.BranchEndNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.If;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Switch;


/**
 * @author frj
 * 2012-5-12
 */
public class BranchTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public BranchTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jbpm.frj.graph.cfg.tranformer.Transformer#transform(org.jbpm.frj.graph.cfg.node.Node, org.jbpm.frj.graph.cfg.node.Node, modeling.model.Activity)
	 */
	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		If ifActivity=null;
		Switch switchActivity=null;
		List<Activity> subActivities=null;
		List<String> conditions=null;
		if(activity instanceof If){
			ifActivity=(If)activity;
			subActivities=ifActivity.getActivites();
			conditions=ifActivity.getConditions();
		}else if(activity instanceof Switch){
			switchActivity=(Switch)activity;
			subActivities=switchActivity.getActivites();
			conditions=switchActivity.getConditions();
		}
		int subActivitiesNum=subActivities.size();
		int conditionsNum=conditions.size();
		Node prior=currentPrior;
		Node next=currentNext;
		Node lastNode=null;
		for(int i=0;i<subActivitiesNum-1;i++){
			BranchBeginNode beginNode=new BranchBeginNode(activity);
			BranchEndNode endNode=new BranchEndNode(activity);
			//rename&&condition
			beginNode.setName(beginNode.getName()+"%"+conditions.get(i));
			beginNode.setCondition(conditions.get(i));
			endNode.setName(endNode.getName()+"%"+conditions.get(i));
			if(i==0)
				lastNode=endNode;
			this.setStandardProperty(beginNode, endNode, prior);
			this.setConnection(beginNode, endNode, prior, next);
			prior=beginNode;
			next=endNode;
			this.getBuildCFG().transformActivity((Activity)subActivities.get(i), prior, next);
		}
		if(conditionsNum+1==subActivitiesNum){
			Node subNode=this.getBuildCFG().transformActivity(subActivities.get(conditionsNum), prior, next);
			//true/falsePath
			BranchBeginNode b=(BranchBeginNode)prior;
			b.setTruePath(b.getNext().get(0));
			b.setFalsePath(b.getNext().get(1));
		}else{
			prior.addNextNode(next);
			next.addPriorNode(prior);
			BranchBeginNode b=(BranchBeginNode)prior;
			b.setTruePath(b.getNext().get(0));
			b.setFalsePath(next);
		}
		return lastNode;
	}

	private void setStandardProperty(BranchBeginNode beginNode,BranchEndNode endNode,Node prior){
		//begin->end; end->begin
		beginNode.setEnd(endNode);
		endNode.setBegin(beginNode);
		//begin/end->org.jbpm.frj.graph
		CFGGraph graph=prior.getGraph();
		beginNode.setGraph(graph);
		endNode.setGraph(graph);
		//org.jbpm.frj.graph->begin
		graph.addNode(beginNode);
	}
	
	private void setConnection(BranchBeginNode beginNode,BranchEndNode endNode,Node prior,Node next){
		//prior.remove(next);	next.remove(prior)
		if(prior.getNext().contains(next))
			prior.getNext().remove(next);
		if(next.getPrior().contains(prior))
			next.getPrior().remove(prior);
		//prior->begin;	end->next; begin->prior; next->end
		prior.addNextNode(beginNode);
		beginNode.addPriorNode(prior);
		endNode.addNextNode(next);
		next.addPriorNode(endNode);
		//begin->end; end->begin
		beginNode.addNextNode(endNode);
		endNode.addPriorNode(beginNode);
		//configure true/falsePath
		if(prior instanceof BranchBeginNode){
			BranchBeginNode b=(BranchBeginNode)prior;
			b.setTruePath(b.getNext().get(0));
			b.setFalsePath(beginNode);
		}
	}
}
