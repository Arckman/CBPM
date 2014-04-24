/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.ReceiveNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Receive;


/**
 * @author frj
 * 2012-5-12
 */
public class ReceiveTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public ReceiveTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jbpm.frj.graph.cfg.tranformer.Transformer#transform(org.jbpm.frj.graph.cfg.node.Node, org.jbpm.frj.graph.cfg.node.Node, modeling.model.Activity)
	 */
	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		ReceiveNode receiveNode=new ReceiveNode(activity);
		setStandardProperty(currentPrior.getGraph(), receiveNode);
		setConnection(currentPrior, currentNext, receiveNode);
		this.transformReceive(receiveNode, (Receive)activity);
		return receiveNode;
	}

	private void transformReceive(ReceiveNode node,Receive activity){
		node.setPartnerLink(activity.getPartnerLink());
		node.setPortType(activity.getPortType());
		node.setOperation(activity.getOperation());
		node.setVariableName(activity.getVariableName());
		node.setMsgExchange(activity.getMsgExchange());
	}
}
