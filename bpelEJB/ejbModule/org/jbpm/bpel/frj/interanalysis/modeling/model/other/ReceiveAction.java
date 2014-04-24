/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.other;

/**
 * @author frj
 *
 */
public class ReceiveAction {
	
	private String partnerLink;
	private String operation;
	private String portType=null;
	private String variableName=null;
	private String msgExchange=null;
	private String part=null;
	
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
	public String getVariableName() {
		return variableName;
	}
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	public String getMsgExchange() {
		return msgExchange;
	}
	public void setMsgExchange(String msgExchage) {
		this.msgExchange = msgExchage;
	}
	public String getPart() {
		return part;
	}
	public void setPart(String part) {
		this.part = part;
	}

}
