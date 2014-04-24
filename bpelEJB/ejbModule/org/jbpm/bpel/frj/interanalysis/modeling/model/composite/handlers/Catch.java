/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;

/**
 * @author frj
 *
 */
public class Catch extends Handler {
	private String faultName;
	private String variableName;
	private VariableDefinition variable=null;

	/**
	 * @param str
	 */
	public Catch(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Catch(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}

	public String getFaultName() {
		return faultName;
	}

	public void setFaultName(String faultName) {
		this.faultName = faultName;
	}

	public VariableDefinition getVariable() {
		return variable;
	}

	public void setVariable(VariableDefinition variable) {
		this.variable = variable;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
}
