package org.jbpm.bpel.frj.interanalysis.mgr;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.jbpm.bpel.frj.interanalysis.ana.BPELReaRecord;
import org.jbpm.bpel.frj.interanalysis.ana.dynamic.DynamicAna;
import org.jbpm.bpel.frj.interanalysis.ana.sta.StaticAna;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.InvokeNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;
import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.graph.exe.ExecutionContext;
import org.w3c.dom.Document;


public class Analyser {

	//using for test======================================================================================
	private File bpelFile=null;
	private BpelModel bpelModel=null;
	private CFGGraph graph=null;
	private BPELReaRecord reaRecord=null;
	public Analyser(){}
//	public Analyser(File bpelFile){this.bpelFile=bpelFile;}
	public File getBpelFile() {
		return bpelFile;
	}

	public void setBpelFile(File bpelFile) {
		this.bpelFile = bpelFile;
	}

	public BpelModel getBpelModel() {
		return bpelModel;
	}

	public CFGGraph getGraph() {
		return graph;
	}

	public BPELReaRecord getReaRecord() {
		return reaRecord;
	}

	public void doModeling(){
		this.bpelModel=new Parser().parse(bpelFile);
	}
	public void doModeling(File bpelFile){
		this.bpelFile=bpelFile;
		doModeling();
	}
	public void doModeling(Document document){
		this.bpelModel=new Parser().parse(document);
	}
	public void doGraph(){
		this.graph=new BuildCFG().buidlCFG(bpelModel);
	}
	public void doAna(){
		BPELReaRecord record=new BPELReaRecord(graph);
		StaticAna a=new StaticAna();
		a.staticallyAnalyse(record);
		this.reaRecord=record;
		reaRecord.filter();
	}
	
	public boolean isFuture(String currentNodeName,String partnerLinkType){
		boolean result=false;
		if(graph==null)
			return result;
		Node sourceNode=graph.findNode(currentNodeName);
		for(Entry<String,String> entry:bpelModel.getOutgoPartnerLinkTypesMap().entrySet()){
			if(entry.getValue().equals(partnerLinkType)){
				result|=reaRecord.isFuture(sourceNode, entry.getKey());
				if(result)return result;
			}
		}
		return result;
	}
//	public boolean isPast(String currentNodeName,String partnerLinkType){
//		if(graph==null)
//			return false;
//		Node sourceNode=graph.findNode(currentNodeName);
//		for(Entry<String,String> entry:bpelModel.getOutgoPartnerLinkTypesMap().entrySet()){
//			if(entry.getValue().equals(partnerLinkType)){
//				if(reaRecord.isPast(sourceNode, entry.getKey()))
//					return true;
//			}
//		}
//		return false;
//	}
	public boolean isS(String currentNodeName,String partnerLinkType){
		if(graph==null)
			return false;
		Node sourceNode=graph.findNode(currentNodeName);
		if(sourceNode==null)return false;//null means first node(receive)
		String plt=bpelModel.getOutgoPartnerLinkTypesMap().get(currentNodeName);
		if(plt==null||(!plt.equals(partnerLinkType)))
			return false;
		if(sourceNode instanceof InvokeNode)
			return true;
		return false;
	}
	public boolean isBranchNode(String nodeName){
		return bpelModel.isBranch(nodeName);
	}
	/**
	 * 
	 * @param lastNodeName
	 * @param currentNodeName
	 * @return partnerLinkType which will not be used
	 */
	public List<String> futureSetChange(String lastNodeName,String currentNodeName){
		List<String> result=new ArrayList<String>();
		if(graph!=null){
			Node lastNode=graph.findNode(lastNodeName);
			Node currentNode=graph.findNode(currentNodeName);
			List<Node> lastList=reaRecord.getReachability().get(lastNode);
			List<Node> currentList=reaRecord.getReachability().get(currentNode);
			for(Node node:lastList){
				if(!currentList.contains(node)){
					String partnerLinkType=bpelModel.getOutgoPartnerLinkTypesMap().get(node.getName());
					if(partnerLinkType!=null)
						result.add(partnerLinkType);
				}
			}
		}
		return result;
	}
	/**
	 * Is the node using the specific partnerlinktype
	 * @param nodeName
	 * @param partnerLinkType
	 * @return
	 */
	public boolean isMatch(String nodeName,String partnerLinkType){
		if(graph==null)
			return false;
		String plt=bpelModel.getOutgoPartnerLinkTypesMap().get(nodeName);
		return partnerLinkType.equals(plt);
	}
	
	public void doDynamicAna(ExecutionContext executionContext,String activityName){
		Node node=this.graph.getNodesMap().get(activityName);
		DynamicAna an=new DynamicAna();
		an.dynamicallyAnalyse(executionContext, node, reaRecord);
		this.printFutureSet(activityName);
		this.writeDynamicAnaResult(this.graph.getProcessName(), activityName);
	}
	//===============================================================================================================
	public void printFutureSet(String activityName) {
		// TODO Auto-generated method stub
		this.reaRecord.printReaRecord(activityName);
	}
	public void printAllFutureSet(){
		this.reaRecord.printAllReaRecord();
	}
	public void writeStaticAnaResult(String processName){
		this.reaRecord.writeAllReaRecord(processName);
	}
	public void writeDynamicAnaResult(String processName,String activityName){
		this.reaRecord.writeDynamicResult(processName, activityName);
	}
}
