/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.RepeatUntil;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class RepeatUntilReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public RepeatUntilReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Activity read(Element element, CompositeActivity parent) {
		// TODO Auto-generated method stub
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME))
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		else
			name="RepeatUntil#"+(++num);
		RepeatUntil repeatUntil=new RepeatUntil(name);
		//set property
		setProperty(element,repeatUntil,parent);
		//set links
		setConnection(element,repeatUntil,parent);
		//set activities of this activity
	    readRepeatUntil(element,repeatUntil);
	    return repeatUntil;
	}
	
	public void readRepeatUntil(Element element,RepeatUntil parent){
		//condition
		Element conditionElement =this.getParser().getTheElement(element,BpelConstants.ElEM_CONDITION);
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
