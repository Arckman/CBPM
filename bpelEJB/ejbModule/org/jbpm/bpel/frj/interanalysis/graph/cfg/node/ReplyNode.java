/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-5-12
 */
public class ReplyNode extends Node {

	private String partnerLink;
	private String portType=null;
	private String operation;
	private String variableName;
	private String faultName=null;
	private String msgExchange=null;
	/**
	 * 
	 */
	public ReplyNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public ReplyNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public ReplyNode(Activity activity) {
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

	public String getFaultName() {
		return faultName;
	}

	public void setFaultName(String faultName) {
		this.faultName = faultName;
	}

	public String getMsgExchange() {
		return msgExchange;
	}

	public void setMsgExchange(String msgExchange) {
		this.msgExchange = msgExchange;
	}

}
