/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.ana.sta;

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
 *
 */
public class SimpleAna {
	private Map<Node,List> futureList;
	private Map nodeMaps;
	private Set analysedNodes=new HashSet<Node>();
	private BPELReaRecord reachabilityRecord;
	
	public void staticallyAnalyse(BPELReaRecord reachabilityRecord){
		this.reachabilityRecord=reachabilityRecord;
		CFGGraph graph=reachabilityRecord.getBpelGraph();
		futureList=new HashMap<Node,List>();
		this.nodeMaps=graph.getNodesMap();
//		Node start=graph.getStart();
		Iterator i=this.nodeMaps.values().iterator();
		while(i.hasNext()){
			Node tempNode=(Node) i.next();
			this.analysedNodes.clear();
//			List tempList=this.analyse(tempNode,new ArrayList<Node>());
			List tempList=this.analyse(tempNode);
//			reachabilityRecord.addPastNodes(tempNode,new ArrayList<Node>());
			this.futureList.put(tempNode, tempList);
		}
		reachabilityRecord.setReachability(futureList);
		this.reachabilityRecord=null;
	}
}
