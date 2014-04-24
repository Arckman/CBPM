/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign;

/**
 * @author frj
 * 2012-5-17
 */
public class ToProperty extends To {

	private String variableName;
	private String property;
	/**
	 * 
	 */
	public ToProperty() {
		// TODO Auto-generated constructor stub
	}
	
	public ToProperty(org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToProperty toProperty) {
		this.property=toProperty.getProperty();
		this.variableName=toProperty.getVariableName();
	}

	public String getVariableName() {
		return variableName;
	}
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	public String getProperty() {
		return property;
	}
	public void setProperty(String property) {
		this.property = property;
	}

}
