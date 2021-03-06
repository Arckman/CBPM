/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign;

/**
 * @author frj
 * 2012-5-17
 */
public class ToVariable extends To {

	private String variableName;
	private String part;
	private String query;
	/**
	 * 
	 */
	public ToVariable() {
		// TODO Auto-generated constructor stub
	}
	
	public ToVariable(org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToVariable toVariable) {
		this.variableName=toVariable.getVariableName();
		this.part=toVariable.getPart();
		this.query=toVariable.getQuery();
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
