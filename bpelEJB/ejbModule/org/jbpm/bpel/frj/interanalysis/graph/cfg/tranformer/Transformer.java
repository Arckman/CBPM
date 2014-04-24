/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


/**
 * @author frj
 * 2012-5-11
 */
public abstract class Transformer {

	private BuildCFG buidCFG;
	public Transformer(BuildCFG buildCFG){
		this.buidCFG=buildCFG;
	}
	public BuildCFG getBuildCFG(){
		return this.buidCFG;
	}
	
	public abstract Node transform(Node currentPrior, Node currentNext, Activity activity);
	
	protected void setConnection(Node currentPrior,Node currentNext,Node node){
		//prior.remove(next)
		if(currentPrior.hasNextNode(currentNext))
			currentPrior.removeNextNode(currentNext);
		//next.remove(prior)
		if(currentNext.hasPriorNode(currentPrior))
			currentNext.removePriorNode(currentPrior);
		//node.prior=prior;prior.next=node
		node.addPriorNode(currentPrior);
		currentPrior.addNextNode(node);
		//node.next=next;next.prior=node
		node.addNextNode(currentNext);
		currentNext.addPriorNode(node);
		
	}
	
	protected void setStandardProperty(CFGGraph graph, Node node){
		//node----->org.jbpm.frj.graph
		node.setGraph(graph);
		//org.jbpm.frj.graph----->node
		graph.addNode(node);
	}
}
