/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.Iterator;
import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Assign;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.Copy;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.From;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromElement;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromExpression;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromPartnerLink;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromProperty;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromVariable;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.To;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToExpression;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToPartnerLink;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToProperty;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.ToVariable;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class AssignReader extends ActivityReader {
    private static int num=0;
	public AssignReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Activity read(Element element, CompositeActivity parent) {
		
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME))
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		else
			name="Assign#"+(++num);
		Assign assign=new Assign(name);
		//set property
		setProperty(element,assign,parent);
		//set links
		setConnection(element,assign,parent);
		//set activites of this activity
		readAssign(element,assign);
		return assign;
	}
	
	private void readAssign(Element element, Assign assign){
		List copyElements=this.getParser().getTheElements(element, BpelConstants.ELEM_ASSIGN_COPY);
		if(copyElements!=null){
			Iterator i=copyElements.iterator();
			while(i.hasNext()){
				Element copyElement=(Element)i.next();
				//<from>
				Element fromElement=this.getParser().getTheElement(copyElement, BpelConstants.ELEM_ASSIGN_COPY_FROM);
				From from=readFrom(fromElement);
				//<to>
				Element toElement=this.getParser().getTheElement(copyElement, BpelConstants.ELEM_ASSGIN_COPY_TO);
				To to=readTo(toElement);
				Copy copy=new Copy();
				copy.setFrom(from);
				copy.setTo(to);
				assign.addOperation(copy);
			}
		}
	}
	/**
	 * 5 classes
	 * from variable= part=? ;
	 * from partnerLink= ;
	 * from variable= property= ; 
	 * from expressionLanguage= ;
	 * from literal ;
	 * @param fromElement
	 * @return
	 */
	private From readFrom(Element fromElement){
		From from=null;
		if(fromElement.hasAttribute(BpelConstants.ATTR_ASSIGN_PROPERTY))
			from=this.readFromProperty(fromElement);
		else if(fromElement.hasAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE))
			from=this.readFromVariable(fromElement);
		else if(fromElement.hasAttribute(BpelConstants.ATTR_ASSIGN_PARTNERLINK))
			from=this.readFromPartnerLink(fromElement);
		else{
			Element literalElement=this.getParser().getTheElement(fromElement, BpelConstants.ELEM_ASSIGN_LITERAL);
			if(literalElement!=null){
				from=new FromElement();
				((FromElement)from).setLiteral(literalElement);
			}else
				from=this.readFromExpression(fromElement);
		}
		return from;
	}
	
	/**
	 * 4 classes
	 * to variable
	 * to partnerLink
	 * to variable property
	 * to expressionLanguage
	 * @param toElement
	 * @return
	 */
	private To readTo(Element toElement){
		To to=null;
		if(toElement.hasAttribute(BpelConstants.ATTR_ASSIGN_PROPERTY))
			to=this.readToProperty(toElement);
		else if(toElement.hasAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE))
			to=this.readToVariable(toElement);
		else if(toElement.hasAttribute(BpelConstants.ATTR_ASSIGN_PARTNERLINK))
			to=this.readToPartnerLink(toElement);
		else
			to=this.readToExpression(toElement);
		return to;
	}
	
	//=========================from=================================
	private From readFromProperty(Element fromElement){
		String variableName=fromElement.getAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE);
		FromProperty fromProperty=new FromProperty(variableName);
		String property=fromElement.getAttribute(BpelConstants.ATTR_ASSIGN_PROPERTY);
		fromProperty.setProperty(property);
		return fromProperty;
	}
	
	private From readFromVariable(Element fromElement){
		String variableName=fromElement.getAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE);
		FromVariable fromVariable=new FromVariable(variableName);
		if(fromElement.hasAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE_PART))
			fromVariable.setPart(fromElement.getAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE_PART));
		Element queryElem=this.getParser().getTheElement(fromElement, BpelConstants.ELEM_ASSIGN_VARIABLE_QUERY);
		if(queryElem!=null)
			fromVariable.setQuery(readQuery(queryElem));
		return fromVariable;
	}
	
	private From readFromPartnerLink(Element fromElement){
		FromPartnerLink fromPartnerLink=new FromPartnerLink();
		fromPartnerLink.setPartnerLink(fromElement.getAttribute(BpelConstants.ATTR_ASSIGN_PARTNERLINK));
		fromPartnerLink.setEndpointReference(fromElement.getAttribute(BpelConstants.ATTR_ASSIGN_PARTNERLINK_ENDPOINTREFERENCE));
		return fromPartnerLink;
	}
	
	private From readFromExpression(Element fromElement){
		FromExpression fromExpression=new FromExpression();
		if(fromElement.hasAttribute(BpelConstants.ATTR_ASSIGN_EXPRESSION))
			fromExpression.setExpression(fromElement.getAttribute(BpelConstants.ATTR_ASSIGN_EXPRESSION));
		else
			fromExpression.setExpression(fromElement.getTextContent());
		fromExpression.setExpressionLanguage(fromElement.getAttribute(BpelConstants.ATTR_ASSIGN_EXPRESSIONLANGUAGE));
		return fromExpression;
	}
	
	private From readFromLiteral(Element fromElement){
		FromElement fromLiteral=new FromElement();
		Element literal=(Element)fromElement.getFirstChild();
		fromLiteral.setLiteral(literal);
		return fromLiteral;
	}
	
	
	//===============================to==================================
	private To readToProperty(Element toElement){
		ToProperty toProperty=new ToProperty();
		String variableName=toElement.getAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE);
		toProperty.setVariableName(variableName);
		String property=toElement.getAttribute(BpelConstants.ATTR_ASSIGN_PROPERTY);
		toProperty.setProperty(property);
		return toProperty;
	}
	
	private To readToVariable(Element toElement){
		ToVariable toVariable=new ToVariable();
		String variableName=toElement.getAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE);
		toVariable.setVariableName(variableName);
		if(toElement.hasAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE_PART))
			toVariable.setPart(toElement.getAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE_PART));
		Element queryElem=this.getParser().getTheElement(toElement, BpelConstants.ELEM_ASSIGN_VARIABLE_QUERY);
		if(queryElem!=null)
			toVariable.setQuery(readQuery(queryElem));
		return toVariable;
	}
	
	private To readToPartnerLink(Element toElement){
		ToPartnerLink toPartnerLink=new ToPartnerLink();
		toPartnerLink.setPartnerLink(toElement.getAttribute(BpelConstants.ATTR_ASSIGN_PARTNERLINK));
		toPartnerLink.setEndpointReference(toElement.getAttribute(BpelConstants.ATTR_ASSIGN_PARTNERLINK_ENDPOINTREFERENCE));
		return toPartnerLink;
	}
	
	private To readToExpression(Element toElement){
		ToExpression toExpression=new ToExpression();
		if(toElement.hasAttribute(BpelConstants.ATTR_ASSIGN_EXPRESSION))
			toExpression.setExpression(toElement.getAttribute(BpelConstants.ATTR_ASSIGN_EXPRESSION));
		else
			toExpression.setExpression(toElement.getTextContent());
		toExpression.setExpressionLanguage(toElement.getAttribute(BpelConstants.ATTR_ASSIGN_EXPRESSIONLANGUAGE));
		return toExpression;
	}
	
	/**
	 * ���װ汾
	 * @param queryElem
	 * @return
	 */
	private String readQuery(Element queryElem){
		String query=queryElem.getTextContent();
		String queryLangauage=queryElem.getAttribute(BpelConstants.ATTR_ASSIGN_VARIABLE_QUERY_LANGUAGE);
		return query;
	}
}
