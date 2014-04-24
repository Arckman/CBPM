/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.ana.sta;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.bpel.frj.interanalysis.ana.BPELReaRecord;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;


/**
 * @author frj
 * 2012-7-7
 */
public class StaticAna {

	private Map<Node,List> futureList;
	private Map nodeMaps;
	private Set analysedNodes=new HashSet<Node>();
	private BPELReaRecord reachabilityRecord;
	public void staticallyAnalyse(BPELReaRecord reachabilityRecord){
		this.reachabilityRecord=reachabilityRecord;
		CFGGraph graph=reachabilityRecord.getBpelGraph();
		futureList=new HashMap<Node,List>();
		this.nodeMaps=graph.getNodesMap();
//		Map nodeMap=org.jbpm.frj.graph.getNodesMap();
//		Node start=graph.getStart();
		Iterator i=this.nodeMaps.values().iterator();
		while(i.hasNext()){
			Node tempNode=(Node) i.next();
			this.analysedNodes.clear();
			List tempList=this.analyse(tempNode,new ArrayList<Node>());
			reachabilityRecord.addPastNodes(tempNode,new ArrayList<Node>());
			this.futureList.put(tempNode, tempList);
		}
		reachabilityRecord.setReachability(futureList);
		this.reachabilityRecord=null;
	}
	
	private List analyse(Node node,List<Node>pastList){
		List fL=new ArrayList();
		if(!node.getNodeType().equals("$ProcessEndNode")){
			this.analysedNodes.add(node);
			pastList.add(node);
			List next=node.getNext();
			Iterator i=next.iterator();
			while(i.hasNext()){
				Node nextNode=(Node)i.next();
				if(!this.analysedNodes.contains(nextNode)){
					this.reachabilityRecord.addPastNodes(nextNode, pastList);
				List nextFL=analyse(nextNode,new ArrayList<Node>(pastList));
				//node.fl+next
//				if(this.nodeMaps.containsValue(nextNode))
					fL.add(nextNode);
				//node.fl+next.fl
				fL=mergeFL(fL, nextFL);
				}
			}
//			this.futureList.put(node, fL);
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
	
//	public static void ana(String[] args){
//		StaticAna a=new StaticAna();
//	
//	}
}
