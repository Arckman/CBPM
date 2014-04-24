/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.ReplyNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Reply;


/**
 * @author frj
 * 2012-5-12
 */
public class ReplyTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public ReplyTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jbpm.frj.graph.cfg.tranformer.Transformer#transform(org.jbpm.frj.graph.cfg.node.Node, org.jbpm.frj.graph.cfg.node.Node, modeling.model.Activity)
	 */
	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		ReplyNode replyNode=new ReplyNode(activity);
		setStandardProperty(currentPrior.getGraph(), replyNode);
		setConnection(currentPrior, currentNext, replyNode);
		this.transformReply(replyNode, (Reply)activity);
		return replyNode;
	}

	private void transformReply(ReplyNode node,Reply activity){
		node.setPartnerLink(activity.getPartnerLink());
		node.setPortType(activity.getPortType());
		node.setOperation(activity.getOperation());
		node.setVariableName(activity.getVariableName());
		node.setMsgExchange(activity.getMsgExchange());
		node.setFaultName(activity.getFaultName());
	}
}
