/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 *
 */
public class OnEvent extends Handler {

	private String partnerLink=null;
	private String operation;
	private String portType=null;
	private String msgType=null;
	private String element=null;
	private String variableName=null;
	private String msgExchange=null;
	private String part;
	/**
	 * @param str
	 */
	public OnEvent(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public OnEvent(String str, Activity a, Activity b) {
		super(str, a, b);
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

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getElement() {
		return element;
	}

	public void setElement(String element) {
		this.element = element;
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

	public String getPart() {
		return part;
	}

	public void setPart(String part) {
		this.part = part;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}
	
}
