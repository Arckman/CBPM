/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.ana;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;

/**
 * @author frj
 * 2012-7-7
 */
public class NodeRefer {

	private Node node;
	private String nodeName;
	private String nodeType;
	public NodeRefer(Node node){
		this.node=node;
		this.nodeName=node.getName();
		this.nodeType=node.getNodeType();
	}
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
		this.setNodeName(node.getName());
		this.setNodeType(node.getNodeType());
	}
	public String getNodeName() {
		return nodeName;
	}
	private void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	public String getNodeType() {
		return nodeType;
	}
	private void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
}
