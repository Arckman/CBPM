/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign;

/**
 * @author frj
 * 2012-5-17
 */
public class FromExpression extends From {

	private String expressionLanguage;
	private String expression;
	/**
	 * 
	 */
	public FromExpression() {
		// TODO Auto-generated constructor stub
	}
	public String getExpressionLanguage() {
		return expressionLanguage;
	}
	public void setExpressionLanguage(String expressionLanguage) {
		this.expressionLanguage = expressionLanguage;
	}
	public String getExpression() {
		return expression;
	}
	public void setExpression(String expression) {
		this.expression = expression;
	}

}
