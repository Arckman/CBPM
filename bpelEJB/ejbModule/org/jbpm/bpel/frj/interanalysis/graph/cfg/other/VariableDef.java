/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.other;

import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;

/**
 * @author frj
 * 2012-7-9
 */
public class VariableDef {

	private String name;
	private String type;
	
	public VariableDef(){
	}
	public VariableDef(VariableDefinition variabledefinition){
		this.name=variabledefinition.getName();
		this.type=variabledefinition.getType();
	}
	public VariableDef(String name){
		this.name=name;
	}
	public VariableDef(String name,String type){
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
