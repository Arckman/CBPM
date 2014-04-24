/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;
import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Flow.LinkDefinition;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public abstract class ActivityReader {
	private Parser parser;

	public ActivityReader(Parser p){
		this.parser=p;
	}
	
	public Parser getParser() {
		return parser;
	}

	public  abstract Activity read(Element element,CompositeActivity parent);
	
	protected void setProperty(Element element,Activity activity,CompositeActivity parent){
		activity.setBpelModel(parent.getBpelModel());
		parent.getBpelModel().addActivity(activity);
	}
	protected void setTarget(Element element,Activity activity,CompositeActivity parent){
		//set targets
		Element targetElem=this.parser.getTheElement(element,BpelConstants.ElEM_TARGETS);
		if(targetElem==null)
			return;
		//joinCondition
		Element joinConditionElem=this.parser.getTheElement(element, BpelConstants.ELEM_JOINCONDITION);
		if(joinConditionElem!=null){
			String joinCondition=joinConditionElem.getTextContent();
			activity.setJoinCondition(joinCondition);
		}
		List targetList=this.parser.getTheElements(targetElem, BpelConstants.ElEM_TARGET);
		for(int i=0;i<targetList.size();i++){
			String linkName =((Element)targetList.get(i)).getAttribute(BpelConstants.ATTR_LINKNAME);
			activity.addTargets(parent.findLink(linkName));
		}
	}
	protected void setSource(Element element,Activity activity,CompositeActivity parent){
		//set sources
		Element sourceElem=this.parser.getTheElement(element, BpelConstants.ElEM_SOURCES);
		if(sourceElem==null)
			return;
		List sourceList=this.parser.getTheElements(sourceElem, BpelConstants.ElEM_SOURCE);
		for(int i=0;i<sourceList.size();i++){
			String linkName =((Element)sourceList.get(i)).getAttribute(BpelConstants.ATTR_LINKNAME);
			LinkDefinition link=parent.findLink(linkName);
			activity.addSources(link);
			//transitionCondition
			Element transitionConditionElem=this.parser.getTheElement(sourceElem, BpelConstants.ELEM_TRANSITIONCONDITION);
			if(transitionConditionElem!=null){
				String transitionCondition=transitionConditionElem.getTextContent();
				link.setTransitionCondition(transitionCondition);
			}
		}
	}
	protected void setConnection(Element element,Activity activity,CompositeActivity parent){
		this.setSource(element, activity, parent);
		this.setTarget(element, activity, parent);
		//add connections
		parent.addChild(activity);
		activity.setCompositeActivity(parent);
	}
	
}
