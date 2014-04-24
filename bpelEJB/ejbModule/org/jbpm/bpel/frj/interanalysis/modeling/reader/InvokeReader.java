/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.Iterator;
import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Invoke;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Catch;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Handler;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class InvokeReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public InvokeReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}
	public Activity read(Element element, CompositeActivity parent) {
		// TODO Auto-generated method stub
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME))
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		else
			name="Invoke#"+(++num);
		//new a activity
		Invoke invoke=new Invoke(name);
		
		Element catchElem=this.getParser().getTheElement(element, BpelConstants.ELEM_CATCH);
		Element catchAll=this.getParser().getTheElement(element, BpelConstants.ELEM_CATCHALL);
		Element compensationHandler=this.getParser().getTheElement(element, BpelConstants.ELEM_COMPENSATIONHANDLER);
		if(catchElem==null&&(catchAll==null)&&(compensationHandler==null)){
			//set links
			setConnection(element,invoke,parent);
		}else{
			Scope scope=new Scope(name+"#implicit_scope");
			//<catch>
			List catches=this.getParser().getTheElements(element, BpelConstants.ELEM_CATCH);
			Iterator i=catches.iterator();
			while(i.hasNext()){
				Catch _catch=new Catch(null);
				_catch.setBpelModel(parent.getBpelModel());
				Element _catchElement=(Element)i.next();
				this.getParser().readCatch(_catchElement, _catch,scope);
				scope.addFaultHandler(_catch);
			}
			//<catchAll>
			if(catchAll!=null){
				Handler handler=new Handler(null);
				handler.setBpelModel(parent.getBpelModel());
				scope.setCatchAll(handler);
				Element subActivityElem=this.getParser().getActivityElement(catchAll);
				Activity subActivity=this.getParser().readActivity(subActivityElem, handler);
				handler.setActivity(subActivity);
			}
			//compensationHandler
			if(compensationHandler!=null){
				this.getParser().configureCompensationHandler(element, scope);
			}
			//=============setProperty====scope
			setProperty(element,scope,parent);
			setConnection(element, scope, parent);
			//=============setConnection======invoke
			setConnection(element, invoke, scope);
		}
		//set property
		setProperty(element,invoke,parent);
		this.readInvoke(element, invoke);
		return invoke;
	}

	private void readInvoke(Element element,Invoke invoke){
		//partnerLink
		String partnerLink=element.getAttribute(BpelConstants.ATTR_PARTNERLINK);
		invoke.setPartnerLink(partnerLink);
		//set outgo partnerLinkType
		Scope globleScope=invoke.getBpelModel().getGlobleScope();
		globleScope.getBpelModel().addOutgoPartnerLinkType(invoke.getName(),globleScope.getPartnerLinks().get(partnerLink).getPartnerLinkType());
		//portType
		String portType=element.getAttribute(BpelConstants.ATTR_PORTTYPE);
		if(portType!=null)
			invoke.setPortType(portType);
		//operation
		String operation=element.getAttribute(BpelConstants.ATTR_OPERATION);
		invoke.setOperation(operation);
		//input variable
		this.readInputVariable(element, invoke);
		//output variable
		this.readOutputVariable(element, invoke);
	}
	
	private void readInputVariable(Element element,Invoke invoke){
		String inputVariableName=null;
		String inputVaraibleAttr=element.getAttribute(BpelConstants.ATTR_INPUTVARIABLE);
		if(inputVaraibleAttr!=null)
			inputVariableName=inputVaraibleAttr;
		invoke.setInputVariableName(inputVariableName);
	}
	
	private void readOutputVariable(Element element,Invoke invoke){
		String outputVariableName=null;
		String outputVaraibleAttr=element.getAttribute(BpelConstants.ATTR_OUTPUTVARIABLE);
		if(outputVaraibleAttr!=null)
			outputVariableName=outputVaraibleAttr;
		invoke.setOutputVariableName(outputVariableName);
	}
}
