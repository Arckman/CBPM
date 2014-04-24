/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-5-12
 */
public class InvokeNode extends Node {
	
	private String partnerLink;
	private String portType=null;
	private String operation;
	private String inputVariableName;
	private String outputVariableName;
	/**
	 * 
	 */
	public InvokeNode() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public InvokeNode(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public InvokeNode(Activity activity) {
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

	public String getInputVariableName() {
		return inputVariableName;
	}

	public void setInputVariableName(String inputVariableName) {
		this.inputVariableName = inputVariableName;
	}

	public String getOutputVariableName() {
		return outputVariableName;
	}

	public void setOutputVariableName(String outputVariableName) {
		this.outputVariableName = outputVariableName;
	}
}
