/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign;

/**
 * @author frj
 * 2012-5-17
 */
public class FromProperty extends From {

	private String variableName;
	private String property;
	
	/**
	 * 
	 */
	public FromProperty() {
		// TODO Auto-generated constructor stub
	}

	public FromProperty(String variableName){
		this.variableName=variableName;
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
