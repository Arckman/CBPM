/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.other;

import java.util.HashSet;
import java.util.Set;

/**
 * @author frj
 *
 */
public class CorrelationSetDefinition {

	private String name;
	private String properties;
	public CorrelationSetDefinition(String name){
		this.name=name;
	}
	public CorrelationSetDefinition(String name,String property){
		this.name=name;
		this.properties=property;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getProperties() {
		return properties;
	}
}
