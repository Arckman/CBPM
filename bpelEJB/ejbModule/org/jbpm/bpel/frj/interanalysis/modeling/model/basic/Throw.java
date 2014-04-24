package org.jbpm.bpel.frj.interanalysis.modeling.model.basic;

import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;

public class Throw extends Activity {
	private String faultName;
	private String variableName;
	private VariableDefinition faultVariable=null;

	public Throw(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}
	/**
	 * @param str
	 * @param model
	 * @param parent
	 */
	public Throw(String str, BpelModel model, CompositeActivity parent) {
		super(str, model, parent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Throw(String str, Activity a, Activity b) {
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
	public Throw(String str, Activity a, Activity b, BpelModel model,
			CompositeActivity parent) {
		super(str, a, b, model, parent);
		// TODO Auto-generated constructor stub
	}
	public String getFaultName() {
		return faultName;
	}
	public void setFaultName(String faultName) {
		this.faultName = faultName;
	}
	public VariableDefinition getFaultVariable() {
		return faultVariable;
	}
	public void setFaultVariable(VariableDefinition faultVariable) {
		this.faultVariable = faultVariable;
	}
	public String getVariableName() {
		return variableName;
	}
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	
}
