/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.ValidateNode;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-12
 */
public class ValidateTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public ValidateTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		ValidateNode validateNode=new ValidateNode(activity);
		setStandardProperty(currentPrior.getGraph(), validateNode);
		setConnection(currentPrior, currentNext, validateNode);
		return validateNode;
	}

}
