/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign;

/**
 * @author frj
 * 2012-5-17
 */
public class ToExpression extends To {

	private String expression;
	private String expressionLanguage;
	/**
	 * 
	 */
	public ToExpression() {
		// TODO Auto-generated constructor stub
	}
	
	public ToExpression(org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToExpression toExpression) {
		this.expression=toExpression.getExpression();
		this.expressionLanguage=toExpression.getExpressionLanguage();
	}

	public String getExpression() {
		return expression;
	}
	public void setExpression(String expression) {
		this.expression = expression;
	}
	public String getExpressionLanguage() {
		return expressionLanguage;
	}
	public void setExpressionLanguage(String expressionLanguage) {
		this.expressionLanguage = expressionLanguage;
	}
}
