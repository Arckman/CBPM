/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.While;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class WhileReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public WhileReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}
	public Activity read(Element element, CompositeActivity parent){
		// TODO Auto-generated method stub
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME))
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		else
			name="While#"+(++num);
		While whilex=new While(name);
		//set property
		setProperty(element,whilex,parent);
		//set links
		setConnection(element,whilex,parent);
		//set activities of this activity
	    readWhile(element,whilex);
	    return whilex;
	}
	private void readWhile(Element element,While parent){
		//condition
		Element conditionElement =this.getParser().getTheElement(element, BpelConstants.ElEM_CONDITION);
		String conditionContext;
		if(conditionElement==null){
			conditionContext=element.getAttribute(BpelConstants.ATTR_CONDITION);
		}else
			conditionContext=conditionElement.getTextContent();
		parent.setCondition(conditionContext);
		//activity
		List nodes=this.getParser().getActivityElements(element);
		if(nodes.size()==1){
			Element child=(Element)nodes.get(0);
			this.getParser().readActivity(child, parent);
			}
		else{}
	}

}
