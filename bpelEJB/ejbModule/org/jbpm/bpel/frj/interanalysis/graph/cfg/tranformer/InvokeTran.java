/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.InvokeNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Invoke;


/**
 * @author frj
 * 2012-5-12
 */
public class InvokeTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public InvokeTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		InvokeNode invokeNode=new InvokeNode(activity);
		setStandardProperty(currentPrior.getGraph(), invokeNode);
		setConnection(currentPrior, currentNext, invokeNode);
		this.transformInvoke(invokeNode, (Invoke)activity);
		return invokeNode;
	}

	private void transformInvoke(InvokeNode node,Invoke activity){
		node.setPartnerLink(activity.getPartnerLink());
		node.setPortType(activity.getPortType());
		node.setOperation(activity.getOperation());
		node.setInputVariableName(activity.getInputVariableName());
		node.setOutputVariableName(activity.getOutputVariableName());
	}
}
