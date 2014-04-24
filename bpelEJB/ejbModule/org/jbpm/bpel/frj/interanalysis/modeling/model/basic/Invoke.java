package org.jbpm.bpel.frj.interanalysis.modeling.model.basic;

import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;

public class Invoke extends Activity{
	
	private String partnerLink;
	private String portType=null;
	private String operation;
	private String inputVariableName;
	private String outputVariableName;
	/**
	 * @param str
	 */
	public Invoke(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param model
	 * @param parent
	 */
	public Invoke(String str, BpelModel model, CompositeActivity parent) {
		super(str, model, parent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Invoke(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 * @param model
	 * @param parent
	 */
	public Invoke(String str, Activity a, Activity b, BpelModel model,
			CompositeActivity parent) {
		super(str, a, b, model, parent);
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
