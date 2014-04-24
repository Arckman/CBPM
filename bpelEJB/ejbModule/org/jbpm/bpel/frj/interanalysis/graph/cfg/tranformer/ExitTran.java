/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.ExitNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-12
 */
public class ExitTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public ExitTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		ExitNode exitNode=new ExitNode(activity);
		setStandardProperty(currentPrior.getGraph()	, exitNode);
		setConnection(currentPrior, currentNext, exitNode);
		return exitNode;
	}

}
