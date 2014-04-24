/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign;

import org.w3c.dom.Element;

/**
 * @author frj
 * 2012-5-17
 */
public class FromElement extends From {

	private Element literal;
	/**
	 * 
	 */
	public FromElement() {
		// TODO Auto-generated constructor stub
	}
	public Element getLiteral() {
		return literal;
	}
	public void setLiteral(Element literal) {
		this.literal = literal;
	}
}
