/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.List;

import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.If;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;



/**
 * @author frj
 * 2012-3-12
 */
public class IfReader extends ActivityReader {

	static private int num=0;
	/**
	 * @param p
	 */
	public IfReader(Parser p) {
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
			name="If#"+(++num);
		//new a activity
		If _if=new If(name);
		//set property
		setProperty(element,_if,parent);
		//set links
		setConnection(element,_if,parent);
		//set activites of this activity
		readIf(element,_if);
		return _if;
		
	}
	
	private void readIf(Element element,If parent){
		String condition;
		Activity activity;
		int i=0;
		//if condition
		Element condElem=this.getParser().getTheElement(element,BpelConstants.ElEM_CONDITION );
		condition=condElem.getTextContent();
		//if activity
		Element ifActiElem=this.getParser().getActivityElement(element);
		this.getParser().readActivity(ifActiElem, parent);
		activity=(Activity) parent.getActivites().get(i++);
		if(activity!=null){
			parent.addActivity(condition, activity);
		}
		else{}
		List elseIfElems=this.getParser().getTheElements(element, BpelConstants.ELEM_ELSEIF);
		if(elseIfElems.size()>0){
			for(int j=0;j<elseIfElems.size();j++){
				Element elseIfElem=(Element)(elseIfElems.get(j));
				//elseif condition
				Element elseIfCondElem=this.getParser().getTheElement(elseIfElem, BpelConstants.ElEM_CONDITION);
				condition=elseIfCondElem.getTextContent();
				//elseif activity
				Element elseIfActiElem=this.getParser().getActivityElement(elseIfElem);
				this.getParser().readActivity(elseIfActiElem, parent);
				activity=(Activity) parent.getActivites().get(i++);
				if(activity!=null){
					parent.addActivity(condition, activity);
				}
				else{}
			}
		}
		Element elseElem=this.getParser().getTheElement(element, BpelConstants.ELEM_ELSE);
		if(elseElem!=null){
			//else activity
			Element elseActiElem=this.getParser().getActivityElement(elseElem);
			this.getParser().readActivity(elseActiElem, parent);
			activity=(Activity) parent.getActivites().get(i++);
			if(activity!=null){
				parent.addActivity("else", activity);
			}
			else{
				parent.addActivity("else", null);
			}
		}
	}

}
