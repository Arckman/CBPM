/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.other;

/**
 * @author frj
 *
 */
public class VariableDefinition {

	private String name;
	private String type;
	
	public VariableDefinition(){
	}
	public VariableDefinition(String name){
		this.name=name;
	}
	public VariableDefinition(String name,String type){
		this.name=name;
		this.type=type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
}
