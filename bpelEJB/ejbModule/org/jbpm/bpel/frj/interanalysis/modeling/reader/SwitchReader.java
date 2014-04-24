/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.List;

import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Switch;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;



/**
 * @author frj
 * 2012-3-12
 */
public class SwitchReader extends ActivityReader {
	private static int num=0;
	/**
	 * @param p
	 */
	public SwitchReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}
	@Override
	public Activity read(Element element, CompositeActivity parent) {
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME))
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		else
			name="Switch#"+(++num);
		//new a activity
		Switch _switch=new Switch(name);
		//set property
		setProperty(element,_switch,parent);
		//set links
		setConnection(element,_switch,parent);
		//set activites of this activity
		readSwitch(element,_switch);
		return _switch;
	}
	private void readSwitch(Element element, Switch _switch) {
		// TODO Auto-generated method stub
		String condition=null;
		List cases=this.getParser().getTheElements(element,BpelConstants.ELEM_CASE);
		if (cases.size()>0){
			for (int i=0;i<cases.size();i++){
				Element caseElem=(Element)(cases.get(i));
				condition=caseElem.getAttribute(BpelConstants.ATTR_CONDITION);
				_switch.addCaseCondition(condition);
				Element caseActivity=this.getParser().getActivityElement(caseElem);
				this.getParser().readActivity(caseActivity, _switch);
			}
		}
		Element otherwiseElem=this.getParser().getTheElement(element, BpelConstants.ELEM_OTHERWISE);
		if(otherwiseElem!=null){
			Element otherwiseActi=this.getParser().getActivityElement(otherwiseElem);
			this.getParser().readActivity(otherwiseActi, _switch);
		}
	}

}
