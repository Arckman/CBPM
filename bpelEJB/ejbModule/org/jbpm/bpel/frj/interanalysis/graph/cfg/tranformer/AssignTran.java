/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;


import java.util.Iterator;
import java.util.List;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.AssignNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Assign;


/**
 * @author frj
 * 2012-5-12
 */
public class AssignTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public AssignTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		AssignNode assignNode=new AssignNode(activity);
		setStandardProperty(currentPrior.getGraph(), assignNode);
		setConnection(currentPrior, currentNext, assignNode);
		this.transformAssign(assignNode, (Assign)activity);
		return assignNode;
	}

	private void transformAssign(AssignNode node,Assign activity){
		List<org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.AssignOperation> originalOps=activity.getOperations();
		if(originalOps!=null){
			Iterator<org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.AssignOperation> originalOpsI=originalOps.iterator();
			while(originalOpsI.hasNext()){
				//original ones
				org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.Copy originalCopy=(org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.Copy)originalOpsI.next();
				 org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.From originalFrom;
				 org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.To originalTo;
				 originalFrom=originalCopy.getFrom();
				 originalTo=originalCopy.getTo();
				 //new ones
				 org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.Copy copy=new  org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.Copy();
				 org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.From from=null;
				 org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.To to=null;
				 //folk
				 if(originalFrom instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromElement)
					 from=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.FromElement((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromElement)originalFrom);
				 else if(originalFrom instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromExpression)
					 from=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.FromExpression((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromExpression)originalFrom);
				 else if(originalFrom instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromPartnerLink)
					 from=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.FromPartnerLink((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromPartnerLink)originalFrom);
				 else if(originalFrom instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromProperty)
					 from=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.FromProperty((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromProperty)originalFrom);
				 else if(originalFrom instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromVariable)
					 from=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.FromVariable((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromVariable)originalFrom);
				 
				 if(originalTo instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToExpression)
					 to=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.ToExpression((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToExpression)originalTo);
				 else if(originalTo instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToPartnerLink)
					 to=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.ToPartnerLink((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToPartnerLink)originalTo);
				 else if(originalTo instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToProperty)
					 to=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.ToProperty((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToProperty)originalTo);
				 else if(originalTo instanceof org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToVariable)
					 to=new org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign.ToVariable((org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToVariable)originalTo);
				 
				 copy.setFrom(from);
				 copy.setTo(to);
				 node.addOperation(copy);
			}
		}
		
	}
}
