/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.repeat.RepeatNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.RepetitiveActivity;


/**
 * @author frj
 * 2012-5-14
 */
public class RepeatUntilTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public RepeatUntilTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jbpm.frj.graph.cfg.tranformer.Transformer#transform(org.jbpm.frj.graph.cfg.node.Node, org.jbpm.frj.graph.cfg.node.Node, modeling.model.Activity)
	 */
	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		RepeatNode repeatNode=new RepeatNode(activity);
		repeatNode.setCondition(((RepetitiveActivity)activity).getCondition());
		this.setStandardProperty(currentPrior.getGraph(), repeatNode, (RepetitiveActivity)activity);
		this.setConnection(currentPrior, currentNext, repeatNode);
		this.transformRepeatUntil(currentPrior, repeatNode, (RepetitiveActivity)activity);
		return repeatNode;
	}
	
	private void setStandardProperty(CFGGraph graph, RepeatNode node,RepetitiveActivity repetitiveActivity){
		this.setStandardProperty(graph, node);
		node.setCondition(repetitiveActivity.getCondition());
	}

	private void transformRepeatUntil(Node prior,RepeatNode repeatNode,RepetitiveActivity repetitiveActivity){
		Activity activity=repetitiveActivity.getLoopActivity();
		Node loopNode=this.getBuildCFG().transformActivity(activity, prior, repeatNode);
		//configure true/falsePath
		repeatNode.addNextNode(prior.getNext().get(0));
		repeatNode.setTruePath(prior.getNext().get(0));
		repeatNode.setFalsePath(repeatNode.getNext().get(0));
		repeatNode.getNext().clear();
		repeatNode.addNextNode(repeatNode.getTruePath());
		repeatNode.addNextNode(repeatNode.getFalsePath());
	}
}
