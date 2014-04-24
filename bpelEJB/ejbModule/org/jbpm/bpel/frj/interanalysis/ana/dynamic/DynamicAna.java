/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.ana.dynamic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.frj.interanalysis.ana.BPELReaRecord;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.branch.BranchBeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.repeat.RepeatNode;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.util.DatatypeUtil;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

/**
 * @author frj
 * 2012-7-11
 */
public class DynamicAna {
	private List<Node> analysedNodes=new ArrayList();

	public void dynamicallyAnalyse(ExecutionContext executionContext,Node node,BPELReaRecord record){
		Token token=executionContext.getToken();
		List futureSet=this.analyse(node,token);
		record.updateReachability(node, futureSet);
	}
	
	private List analyse(Node node,Token token){
		List fL=new ArrayList();
		if (node != null) {
			if (!node.getNodeType().equals("$ProcessEndNode")) {
				this.analysedNodes.add(node);
				if (node instanceof BranchBeginNode) {
					Node selectedNode;
					Node trueNode = ((BranchBeginNode) node).getTruePath();
					String conditionStr = ((BranchBeginNode) node)
							.getCondition();
					Expression condition = JbpmConfiguration
							.getExpression(conditionStr);
					boolean b;
					try {
						b = DatatypeUtil.toBoolean(condition.getEvaluator()
								.evaluate(token));
						if (b) {
							selectedNode = trueNode;
						} else
							selectedNode = ((BranchBeginNode) node)
									.getFalsePath();
						if (!this.analysedNodes.contains(selectedNode)) {
							List nextFL = analyse(selectedNode, token);
							fL.add(selectedNode);
							fL = mergeFL(fL, nextFL);
						}
					} catch (BpelFaultException e) {
						List next = node.getNext();
						Iterator i = next.iterator();
						while (i.hasNext()) {
							Node nextNode = (Node) i.next();
							if (!this.analysedNodes.contains(nextNode)) {
								List nextFL = analyse(nextNode, token);
								fL.add(nextNode);
								fL = mergeFL(fL, nextFL);
							}
						}
					}
				} else if (node instanceof RepeatNode) {
					String conditionStr = ((RepeatNode) node).getCondition();
					Node selectedNode;
					Expression condition = JbpmConfiguration
							.getExpression(conditionStr);
					boolean b;
					try {
						b = DatatypeUtil.toBoolean(condition.getEvaluator()
								.evaluate(token));
						if (b) {
							selectedNode = ((RepeatNode) node).getTruePath();
							List nextFL = analyse(selectedNode, token);
							fL.add(selectedNode);
							fL = mergeFL(fL, nextFL);
							selectedNode = ((RepeatNode) node)
									.getNext().get(1);
							nextFL = analyse(selectedNode, token);
							fL.add(selectedNode);
							fL = mergeFL(fL, nextFL);
						} else
							selectedNode = ((RepeatNode) node).getNext().get(1);
						if (!this.analysedNodes.contains(selectedNode)) {
							List nextFL = analyse(selectedNode, token);
							fL.add(selectedNode);
							fL = mergeFL(fL, nextFL);
						}
					} catch (BpelFaultException e) {
						List next = node.getNext();
						Iterator i = next.iterator();
						while (i.hasNext()) {
							Node nextNode = (Node) i.next();
							if (!this.analysedNodes.contains(nextNode)) {
								List nextFL = analyse(nextNode, token);
								fL.add(nextNode);
								fL = mergeFL(fL, nextFL);
							}
						}
					}
				} else {
					List next = node.getNext();
					Iterator i = next.iterator();
					while (i.hasNext()) {
						Node nextNode = (Node) i.next();
						if (!this.analysedNodes.contains(nextNode)) {
							List nextFL = analyse(nextNode, token);
							// node.fl+next
							// if(this.nodeMaps.containsValue(nextNode))
							fL.add(nextNode);
							// node.fl+next.fl
							fL = mergeFL(fL, nextFL);
						}
					}
				}
			}
		}
		return fL;
	}
	
	private List mergeFL(List fL,List nextFL){
		Iterator i=nextFL.iterator();
		while(i.hasNext()){
			Node node=(Node)i.next();
			if(!fL.contains(node))
				fL.add(node);
		}
		return fL;
	}
	
	private void noteVariables(Node node,List vs){
		
	}
}
