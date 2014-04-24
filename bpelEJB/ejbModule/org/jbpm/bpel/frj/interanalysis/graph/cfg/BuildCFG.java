/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg;


import java.util.HashMap;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.graph.Graph;
import org.jbpm.bpel.frj.interanalysis.graph.GraphBuilder;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.AssignTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.BranchTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.EmptyTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.ExitTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.FlowTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.InvokeTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.PickTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.ReceiveTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.RepeatUntilTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.ReplyTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.ScopeTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.SequenceTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.Transformer;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.ValidateTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.WaitTran;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer.WhileTran;
import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;


/**
 * @author frj
 * 2012-5-11
 */
public class BuildCFG implements GraphBuilder{

	private Map<String,Transformer> transformersMap;
	
	public BuildCFG(){
		initalMap();
	}
	
	private void initalMap(){
		if(transformersMap==null){
			transformersMap=new HashMap<String,Transformer>();
		}
		transformersMap.put("assign", new AssignTran(this));
		transformersMap.put("compensate", new EmptyTran(this));
		transformersMap.put("compensateScope", new EmptyTran(this));
		transformersMap.put("empty", new EmptyTran(this));
		transformersMap.put("exit", new ExitTran(this));
		transformersMap.put("flow", new FlowTran(this));
		transformersMap.put("if", new BranchTran(this));
		transformersMap.put("invoke", new InvokeTran(this));
		transformersMap.put("pick", new PickTran(this));
		transformersMap.put("receive", new ReceiveTran(this));
		transformersMap.put("repeatuntil", new RepeatUntilTran(this));
		transformersMap.put("reply", new ReplyTran(this));
		transformersMap.put("rethrow", new EmptyTran(this));
		transformersMap.put("scope", new ScopeTran(this));
		transformersMap.put("sequence", new SequenceTran(this));
		transformersMap.put("switch", new BranchTran(this));
		transformersMap.put("throw", new EmptyTran(this));
		transformersMap.put("validate", new ValidateTran(this));
		transformersMap.put("wait", new WaitTran(this));
		transformersMap.put("while", new WhileTran(this));
	}
	
	public CFGGraph buidlCFG(BpelModel bpelModel){
		CFGGraph graph=new CFGGraph(bpelModel);
		Scope globleScope=bpelModel.getGlobleScope();
		this.transformActivity(globleScope, graph.getStart(), graph.getEnd());
		return graph;
	}
	
	
	public Transformer getActivityTransformer(Activity activity){
		String activityName=activity.getClass().getSimpleName().toLowerCase();
		return this.transformersMap.get(activityName);
	}
	
	public Node transformActivity(Activity activity,Node prior,Node next){
		Transformer transformer=this.getActivityTransformer(activity);
		Node node=transformer.transform(prior, next, activity);
		return node;
	}

	public Graph buildGraph(BpelModel bpelModel) {
		// TODO Auto-generated method stub
		return this.buidlCFG(bpelModel);
	}

}
