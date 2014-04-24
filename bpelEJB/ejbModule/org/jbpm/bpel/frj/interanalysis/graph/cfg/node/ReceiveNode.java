/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-5-12
 */
public class ReceiveNode extends Node {

	private String partnerLink;
	private String portType=null;
	private String operation;
	private String variableName;
	private String msgExchange=null;
	/**
	 * 
	 */
	public ReceiveNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public ReceiveNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public ReceiveNode(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	public String getPartnerLink() {
		return partnerLink;
	}

	public void setPartnerLink(String partnerLink) {
		this.partnerLink = partnerLink;
	}

	public String getPortType() {
		return portType;
	}

	public void setPortType(String portType) {
		this.portType = portType;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getMsgExchange() {
		return msgExchange;
	}

	public void setMsgExchange(String msgExchange) {
		this.msgExchange = msgExchange;
	}
}
