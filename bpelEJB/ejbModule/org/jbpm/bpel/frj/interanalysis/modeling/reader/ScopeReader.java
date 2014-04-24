/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.Iterator;
import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.PartnerLinkDefinition;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class ScopeReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public ScopeReader(Parser p) {
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
			name="Scope#"+(++num);
		//new a activity
		Scope scope=new Scope(name);
		//set property
		setProperty(element,scope,parent);
		//set links
		setConnection(element,scope,parent);
		//set activites of this activity
		this.getParser().readScope(element,scope);
		
		return scope;
	}

//	private void readScope(Element element, Scope scope) {
//		// TODO Auto-generated method stub
//		//configure properties
//		configurePartnerLinks(element,scope);
//		configureVariables(element, scope);
//		
//		Element activityElem=this.getParser().getActivityElement(element);
//		this.getParser().readActivity(activityElem, scope);
//	}

	/**
	 * add partnerLinks to scope
	 * @param element
	 * @param scope
	 */
	private void configurePartnerLinks(Element element,Scope scope){
		Element partnerLinksElement=this.getParser().getTheElement(element, BpelConstants.ELEM_PARTNERLINKS);
		if(partnerLinksElement!=null){
			List partnerLinksElements=this.getParser().getTheElements(partnerLinksElement, BpelConstants.ELEM_PARTNERLINK);
			Iterator i=partnerLinksElements.iterator();
			while(i.hasNext()){
				Element partnerLinkE=(Element)i.next();
				PartnerLinkDefinition partnerLinkD=new PartnerLinkDefinition(partnerLinkE.getAttribute(BpelConstants.ATTR_NAME));
				partnerLinkD.setPartnerLinkType(partnerLinkE.getAttribute(BpelConstants.ATTR_PARTNERLINKTYPE).split(":")[1]);
				if(partnerLinkE.hasAttribute(BpelConstants.ATTR_PARTNERLINK_MYROLE))
					partnerLinkD.setMyRole(partnerLinkE.getAttribute(BpelConstants.ATTR_PARTNERLINK_MYROLE));
				if(partnerLinkE.hasAttribute(BpelConstants.ATTR_PARTNERLINK_PARTNERROLE))
					partnerLinkD.setPartnerRole(partnerLinkE.getAttribute(BpelConstants.ATTR_PARTNERLINK_PARTNERROLE));
				scope.addPartnerLink(partnerLinkD);
			}
		}
	}
	
	private void configureVariables(Element element,Scope scope){
		Element variablesElement=this.getParser().getTheElement(element, BpelConstants.ELEM_VARIABLES);
		if(variablesElement!=null){
			List variablesElements=this.getParser().getTheElements(variablesElement, BpelConstants.ELEM_VARIABLE);
			Iterator i=variablesElements.iterator();
			while(i.hasNext()){
				Element variableE=(Element)i.next();
				VariableDefinition variableD=new VariableDefinition(variableE.getAttribute(BpelConstants.ATTR_NAME));
				if(variableE.hasAttribute(BpelConstants.ATTR_VARIABLE_TYPE))
					variableD.setType(variableE.getAttribute(BpelConstants.ATTR_VARIABLE_TYPE));
				if(variableE.hasAttribute(BpelConstants.ATTR_VARIABLE_MSGTYPE))
					variableD.setType(variableE.getAttribute(BpelConstants.ATTR_VARIABLE_MSGTYPE));
				scope.addVariable(variableD);
			}
		}
	}
}
