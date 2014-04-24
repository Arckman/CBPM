/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-6-25
 */
public class EventHandler extends Handler {

	private String partnerLink=null;
	private String operation;
	private String portType=null;
	private String msgType=null;
	private String element=null;
	private String variableName=null;
	private String msgExchange=null;
	private String part;
	/**
	 * 
	 */
	public EventHandler() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public EventHandler(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public EventHandler(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}

	public String getPartnerLink() {
		return partnerLink;
	}

	public void setPartnerLink(String partnerLink) {
		this.partnerLink = partnerLink;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
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
}
