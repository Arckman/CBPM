/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.ana;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.InvokeNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.ReceiveNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.ReplyNode;

/**
 * @author frj
 * 2012-7-7
 */
public class BPELReaRecord {
	private CFGGraph bpelGraph;
//	private Map<Node,List<Node>> pastNodes=new HashMap<Node,List<Node>>();
	private Map<Node,List> reachability=null;
//	private Map<Node,List> dynamicReach=null;
	public BPELReaRecord(CFGGraph graph){
		this.bpelGraph=graph;
	}
	public CFGGraph getBpelGraph() {
		return bpelGraph;
	}
	public void setBpelGraph(CFGGraph bpelGraph) {
		this.bpelGraph = bpelGraph;
	}
//	public Map<Node,List<Node>>getPastNodes(){
//		return pastNodes;
//	}
//	public void addPastNodes(Node node,List<Node>list){
//		pastNodes.put(node, list);
//	}
	public Map<Node, List> getReachability() {
		return reachability;
	}
	public void setReachability(Map<Node, List> reachability) {
		this.reachability = reachability;
	}
	public void updateReachability(Node node,List list){
		if(this.reachability.containsKey(node)){
			this.reachability.remove(node);
			this.reachability.put(node, list);
		}
	}
	/**
	 * filter past and future set, containing only interacting activity( receive, reply, invoke)
	 */
	public void filter(){
//		if(pastNodes!=null){
//			for(List<Node> list:pastNodes.values()){
//				List<Node> removeList=new ArrayList<Node>();
//				for(Node node:list){
//					if(!((node instanceof ReceiveNode)||(node instanceof ReplyNode)||(node instanceof InvokeNode)))
//						removeList.add(node);
//				}
//				list.removeAll(removeList);
//			}
//		}
		if(reachability!=null){
			for(List<Node> list:reachability.values()){
				List<Node> removeList=new ArrayList<Node>();
				for(Node node:list){
					if(!((node instanceof ReceiveNode)||(node instanceof ReplyNode)||(node instanceof InvokeNode)))
						removeList.add(node);
				}
				list.removeAll(removeList);
			}
		}
	}
	
	public boolean isFuture(Node sourceNode,String targetNodeName){
		if(reachability==null)
			return false;
		if(sourceNode==null)return true;//null means first node(receive)
		List<Node> futureList=reachability.get(sourceNode);
		for(Node node:futureList){
			if(targetNodeName.equals(node.getName()))
				return true;
		}
		return false;
	}
//	public boolean isPast(Node sourceNode,String targetNodeName){
//		if(pastNodes==null)
//			return false;
//		if(sourceNode==null)return false;//null means first node(receive)
//		List<Node> pastList=pastNodes.get(sourceNode);
//		for(Node node:pastList){
//			if(node.equals(targetNodeName))
//				return true;
//		}
//		return false;
//	}
	
	public void printReaRecord(String activityName){
		Iterator k=this.reachability.keySet().iterator();
		while(k.hasNext()){
			Node tempNode=(Node)k.next();
			if (tempNode.getActivityName().equals(activityName)) {
				List list = this.reachability.get(tempNode);
				Iterator i = list.iterator();
				System.out
						.println("===========================================================================================");
				System.out.println("The future set of the specific activity *"
						+ activityName + "* is:");
				while (i.hasNext()) {
					Node temp = (Node) i.next();
					System.out.println(temp.getClass() + ":::::::"
							+ temp.getActivityName());
				}
				System.out
						.println("====================================== over ===============================================");
			}
		}
	}
	
	public void printAllReaRecord(){
		Iterator i=this.reachability.keySet().iterator();
		while(i.hasNext()){
			Node node=(Node)i.next();
			this.printReaRecord(node.getActivityName());
		}
	}
	
	/**
	 * write static analyse result to a file
	 * @param processName
	 */
	public void writeAllReaRecord(String processName){
		StringBuffer str;
		String filePath="//home//frj//桌面//temp//results//"+processName+"_static.txt";
		File file=new File(filePath);
		System.out.println(file.getAbsolutePath());
		FileWriter fileWriter = null;
		try {
			if (file.exists())
				file.delete();
			file.createNewFile();
			fileWriter=new FileWriter(file,true);
			Iterator i=this.reachability.keySet().iterator();
			while(i.hasNext()){
				Node node=(Node)i.next();
				str=this.writeReaRecord(node.getActivityName());
				//write to file
				fileWriter.write(str.toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(fileWriter!=null)
				try {
					fileWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	public StringBuffer writeReaRecord(String activityName){
		StringBuffer str=new StringBuffer();
		Iterator k=this.reachability.keySet().iterator();
		while(k.hasNext()){
			Node tempNode=(Node)k.next();
			if (tempNode.getActivityName().equals(activityName)) {
				List list = this.reachability.get(tempNode);
				Iterator i = list.iterator();
//				String name=tempNode.getClass().toString();
//				name=name.split("\\.")[name.split("\\.").length-1];
//				name=name+":::::"+activityName;
				str.append("===========================================================================================\r\n ");
				str.append("The future set of the specific activity *"
						+ getActivityClassName(tempNode.getClass().toString(),activityName)+ "* is:\r\n ");
				while (i.hasNext()) {
					Node temp = (Node) i.next();
					str.append(getActivityClassName(temp.getClass().toString(), temp.getActivityName())+"\r\n ");
				}
				str.append("====================================== over ===============================================\r\n ");
			}
		}
		return str;
	}
	
	/**
	 * write dynamic result to file
	 * @param processName
	 * @param activityName
	 */
	public void writeDynamicResult(String processName,String activityName){
		String filePath="D://temp//"+processName+"_dynamic.txt";
		File file=new File(filePath);
		FileWriter fw =null;
		try {
			fw = new FileWriter(file, true);
			StringBuffer str = new StringBuffer();
			Iterator k = this.reachability.keySet().iterator();
			while (k.hasNext()) {
				Node tempNode = (Node) k.next();
				if (tempNode.getActivityName().equals(activityName)) {
					List list = this.reachability.get(tempNode);
					Iterator i = list.iterator();
					str.append("===========================================================================================\r\n ");
					str.append("The future set of the specific activity *"
							+ getActivityClassName(tempNode.getClass().toString(),activityName) + "* is:\r\n ");
					while (i.hasNext()) {
						Node temp = (Node) i.next();
						str.append(getActivityClassName(temp.getClass().toString(), temp.getActivityName()) + "\r\n ");
					}
					str.append("====================================== over ===============================================\r\n ");
					fw.write(str.toString());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(fw!=null)
				try {
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	private String getActivityClassName(String classString,String activityName){
		 return classString.split("\\.")[classString.split("\\.").length-1]+":::::"+activityName;
	}
}
