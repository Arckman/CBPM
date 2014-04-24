/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign;

/**
 * @author frj
 * 2012-5-7
 */
public class FromVariable extends From {

	private String variableName;
	private String part=null;
	private String query=null;
	/**
	 * 
	 */
	public FromVariable() {
		// TODO Auto-generated constructor stub
	}
	
	public FromVariable(org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromVariable fromVariable) {
		this.variableName=fromVariable.getVariableName();
		this.part=fromVariable.getPart();
		this.query=fromVariable.getQuery();
	}

	public FromVariable(String name){
		this.variableName=name;
	}
	public String getVariableName() {
		return variableName;
	}
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	public String getPart() {
		return part;
	}
	public void setPart(String part) {
		this.part = part;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
}
