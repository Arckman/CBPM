/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;


import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Catch;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Handler;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.OnAlarm;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.OnEvent;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.CorrelationSetDefinition;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.PartnerLinkDefinition;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.ActivityReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.AssignReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.CompensateReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.CompensateScopeReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.EmptyReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.ExitReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.FlowReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.IfReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.InvokeReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.PickReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.ReceiveReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.RepeatUntilReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.ReplyReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.RethrowReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.ScopeReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.SequenceReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.SwitchReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.ThrowReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.ValidateReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.WaitReader;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.WhileReader;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.jbpm.bpel.frj.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


/**
 * @author frj
 * 2012-3-11
 */
public class Parser {
	private Map<String,ActivityReader> parsers;
	
	/**
	 * do modeling
	 * @param bpelFile
	 * @return BpelModel
	 */
	public  BpelModel parse(File bpelFile){
		System.out.println("Parser.parse()...");
		BpelModel bpelModel=new BpelModel();
		DocumentBuilder documentbuilder=XMLUtil.getDocumentBuilder();
		try {
			createParsers();
			Document document=documentbuilder.parse(new FileInputStream(bpelFile));
			Element element=document.getDocumentElement();
			//set process definition properties
			setProcessProperty(bpelModel,element);
			//set scope property
			Scope globleScope=bpelModel.getGlobleScope();
			System.out.println("setting globle scope property...");
			//**read scope property
			globleScope.setBpelModel(bpelModel);
			readScope(element,globleScope);
			System.out.println("parse over...");
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bpelModel;
	}

	public BpelModel parse(Document document){
		createParsers();
		BpelModel bpelModel=new BpelModel();
		Element element=document.getDocumentElement();
		//set process definition properties
		setProcessProperty(bpelModel,element);
		System.out.println("start parse process: "+bpelModel.getName()+" ...");
		//set scope property
		Scope globleScope=bpelModel.getGlobleScope();
		//**read scope property
		globleScope.setBpelModel(bpelModel);
		readScope(element,globleScope);
		System.out.println("process "+bpelModel.getName()+" parse over...");
		return bpelModel;
	}
	/**
	 * set property of process to bpelModel
	 * @param bpelModel
	 * @param element
	 */
	private  void setProcessProperty(BpelModel bpelModel,Element element){
		//set process name
		String name=element.getAttribute(BpelConstants.ATTR_NAME);
		String targetNS=element.getAttribute(BpelConstants.ATTR_TARGETNAMESPACE);
		bpelModel.setName(name);
		bpelModel.setTargetNS(targetNS);
		//partner link
	}
	
	public void readScope(Element element,Scope scope){
		configurePartnerLinks(element,scope);
		configureVariables(element, scope);
		configureCorrelations(element, scope);
		//get main sequence
		Element activityElement=getActivityElement(element);
//		System.out.println(activityElement.getLocalName());
		readActivity(activityElement,scope);
		//deal with other elements such as handlers
		configuerFaultHandlers(element, scope);
		configureCompensationHandler(element, scope);
		configureTerminationHandler(element, scope);
		configureEventHandlers(element, scope);
	}
	
	/**
	 * add partnerLinks to scope
	 * @param element
	 * @param scope
	 */
	private void configurePartnerLinks(Element element,Scope scope){
		Element partnerLinksElement=getTheElement(element, BpelConstants.ELEM_PARTNERLINKS);
		if(partnerLinksElement!=null){
			List partnerLinksElements=getTheElements(partnerLinksElement, BpelConstants.ELEM_PARTNERLINK);
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
		Element variablesElement=getTheElement(element, BpelConstants.ELEM_VARIABLES);
		if(variablesElement!=null){
			List variablesElements=getTheElements(variablesElement, BpelConstants.ELEM_VARIABLE);
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
	
	private void configureCorrelations(Element element,Scope scope){
		Element correlationSetsElement=getTheElement(element, BpelConstants.ELEM_CORRELATIONSETS);
		if(correlationSetsElement!=null){
			List correlationSetElements=getTheElements(correlationSetsElement, BpelConstants.ELEM_CORRELATIONSET);
			Iterator i=correlationSetElements.iterator();
			while(i.hasNext()){
				Element correlationSetE=(Element)i.next();
				CorrelationSetDefinition correlationSetD=new CorrelationSetDefinition(
						correlationSetE.getAttribute(BpelConstants.ATTR_NAME),
						correlationSetE.getAttribute(BpelConstants.ATTR_CORRELATIONSET_PROPERTIES));
				scope.addCorrelationSet(correlationSetD);
			}
		}
	}
	
	private void configuerFaultHandlers(Element element,Scope scope){
		Element faultHandlersElement=getTheElement(element,BpelConstants.ELEM_FAULTHANDLERS);
		if(faultHandlersElement!=null){
			//configure <catch>
			List catchElements=getTheElements(faultHandlersElement,BpelConstants.ELEM_CATCH);
			Iterator i=catchElements.iterator();
			while(i.hasNext()){
				Element _catchE=(Element)i.next();
				Catch _catch=new Catch(null);
				_catch.setBpelModel(scope.getBpelModel());
				readCatch(_catchE,_catch,scope);
				scope.addFaultHandler(_catch);
			}
			//configure <catchAll>
			Element catchAllElement=getTheElement(faultHandlersElement, BpelConstants.ELEM_CATCHALL);
			if(catchAllElement!=null){
				Handler handler=new Handler(null);
				handler.setBpelModel(scope.getBpelModel());
				scope.setCatchAll(handler);
				Element activityE=getActivityElement(catchAllElement);
				Activity activity=readActivity(activityE, handler);
				handler.setActivity(activity);
			}
		}
	}
	
	public void readCatch(Element _catchE,Catch _catch,Scope scope){
		//faultname
		String faultName=_catchE.getAttribute(BpelConstants.ATTRI_CATCH_FAULTNAME);
		_catch.setFaultName(faultName);
		//variable
		if(_catchE.hasAttribute(BpelConstants.ATTR_FAULTVARIABLE)){
			String variableName=_catchE.getAttribute(BpelConstants.ATTR_FAULTVARIABLE);
			_catch.setVariableName(variableName);
			VariableDefinition variable=scope.findVariable(variableName);
			_catch.setVariable(variable);
		}
		//activity
		Element activityE=getActivityElement(_catchE);
		Activity activity=readActivity(activityE, _catch);
		
		_catch.setActivity(activity);
	}
	
	public void configureCompensationHandler(Element element,Scope scope){
		Element compensationHandlerElement=getTheElement(element, BpelConstants.ELEM_COMPENSATIONHANDLER);
		if(compensationHandlerElement!=null){
			Handler handler=new Handler(null);
			handler.setBpelModel(scope.getBpelModel());
			scope.setCompensationHandler(handler);
			Element activityE=getActivityElement(compensationHandlerElement);
			Activity activity=readActivity(activityE, handler);
			handler.setActivity(activity);
		}
	}
	
	private void configureTerminationHandler(Element element,Scope scope){
		Element terminationHandlerElement=getTheElement(element, BpelConstants.ELEM_TERMINATIONNHANDLER);
		if(terminationHandlerElement!=null){
			Handler handler=new Handler(null);
			handler.setBpelModel(scope.getBpelModel());
			scope.setTerminationHandler(handler);
			Element activityE=getActivityElement(terminationHandlerElement);
			Activity activity=readActivity(activityE, handler);
			handler.setActivity(activity);
		}
	}
	
	private void configureEventHandlers(Element element,Scope scope){
		Element eventHandlersElement=getTheElement(element, BpelConstants.ELEM_EVENTHANDLERS);
		if(eventHandlersElement!=null){
			//onEvents
			List<Element> onEventElements=getTheElements(eventHandlersElement, BpelConstants.ELEM_ONEVENT);
			Iterator i=onEventElements.iterator();
			while(i.hasNext()){
				Element onEventE=(Element)i.next();
				OnEvent onEvent=new OnEvent(null);
				onEvent.setBpelModel(scope.getBpelModel());
				scope.addOnEvent(onEvent);
				//read onEvent Element
				String partnerLink=onEventE.getAttribute(BpelConstants.ATTR_PARTNERLINK);
				onEvent.setPartnerLink(partnerLink);
				onEvent.setOperation(onEventE.getAttribute(BpelConstants.ATTR_OPERATION));
				if(onEventE.hasAttribute(BpelConstants.ATTR_VARIABLE)){
					String variableName=onEventE.getAttribute(BpelConstants.ATTR_VARIABLE);
					onEvent.setVariableName(variableName);
					if(onEventE.hasAttribute(BpelConstants.ATTR_MSGEXCHANGE))
						onEvent.setMsgExchange(onEventE.getAttribute(BpelConstants.ATTR_MSGEXCHANGE));
					if(onEventE.hasAttribute(BpelConstants.ATTR_VARIABLE_MSGTYPE))
						onEvent.setMsgType(onEventE.getAttribute(BpelConstants.ATTR_VARIABLE_MSGTYPE));
					if(onEventE.hasAttribute(BpelConstants.ATTR_ELEMENT))
						onEvent.setElement(onEventE.getAttribute(BpelConstants.ATTR_ELEMENT));
				}
				Element fromPartsE=getTheElement(onEventE, BpelConstants.ELEM_FROMPARTS);
				if(fromPartsE!=null){
					Element fromPartE=getTheElement(fromPartsE, BpelConstants.ELEM_FROMPART);
					onEvent.setVariableName(fromPartE.getAttribute(BpelConstants.ATTR_TOVARIABLE));
					onEvent.setPart(fromPartE.getAttribute(BpelConstants.ATTR_PART));
				}
				//enclosed <scope>
				Element scopeE=getTheElement(onEventE,"scope");
				if(scopeE!=null){
					Activity activity=readActivity(scopeE, onEvent);
					onEvent.setActivity(activity);
				}
			}
			//onAlarms
			List<Element> onAlarmElements=getTheElements(eventHandlersElement, BpelConstants.ELEM_ONALARM);
			i=onAlarmElements.iterator();
			while(i.hasNext()){
				Element onAlarmE=(Element)i.next();
				OnAlarm onAlarm=new OnAlarm(null);
				onAlarm.setBpelModel(scope.getBpelModel());
				scope.addOnAlarm(onAlarm);
				//read onAlarm Element
				Element forE=getTheElement(onAlarmE, BpelConstants.ELEM_FOR);
				if(forE!=null){
					onAlarm.set_for(forE.getTextContent());
					onAlarm.set_for_lang(forE.getAttribute(BpelConstants.ATTR_EXPRESSIONLANGUAGE));
				}else{
					Element untilE=getTheElement(onAlarmE, BpelConstants.ELEM_UNTIL);
					onAlarm.set_until(untilE.getTextContent());
					onAlarm.set_until_lang(untilE.getAttribute(BpelConstants.ATTR_EXPRESSIONLANGUAGE));
				}
				Element repeatEveryE=getTheElement(onAlarmE, BpelConstants.ELEM_REPEATEVERY);
				if(repeatEveryE!=null){
					onAlarm.setRepeatEvery(repeatEveryE.getTextContent());
					onAlarm.setRepeatEvery_lang(repeatEveryE.getAttribute(BpelConstants.ATTR_EXPRESSIONLANGUAGE));
				}
				//enclosed <scope>
				Element scopeE=getTheElement(onAlarmE,"scope");
				if(scopeE!=null){
					Activity activity=readActivity(scopeE, onAlarm);
					onAlarm.setActivity(activity);
				}
			}
		}
	}
	
	/**
	 * get Element of legal activity
	 * @param element
	 * @return
	 */
	public  Element getActivityElement(Element element){
		for(Node child=element.getFirstChild();child!=null;child=child.getNextSibling()){
			{
				String localName=child.getLocalName();
//				System.out.println("type="+child.getNodeType()+"&&"+(child.getNodeType()==Node.ELEMENT_NODE));
//				System.out.println("local name="+localName);
				if(element.getNodeType()==Node.ELEMENT_NODE&&localName!=null)
					if(parsers.containsKey(child.getLocalName().toLowerCase()))
						return (Element) child;
			}
		}
		return null;
	}
	
	
	
	private void createParsers(){
		if(parsers==null){
			parsers=new HashMap<String,ActivityReader>();
		}
		parsers.put("assign", new AssignReader(this));
		parsers.put("compensate", new CompensateReader(this));
		parsers.put("compensateScope", new CompensateScopeReader(this));
		parsers.put("empty", new EmptyReader(this));
		parsers.put("exit", new ExitReader(this));
		parsers.put("flow", new FlowReader(this));
		parsers.put("if", new IfReader(this));
		parsers.put("invoke", new InvokeReader(this));
		parsers.put("pick", new PickReader(this));
		parsers.put("receive", new ReceiveReader(this));
		parsers.put("repeatuntil", new RepeatUntilReader(this));
		parsers.put("reply", new ReplyReader(this));
		parsers.put("rethrow", new RethrowReader(this));
		parsers.put("scope", new ScopeReader(this));
		parsers.put("sequence", new SequenceReader(this));
		parsers.put("switch", new SwitchReader(this));
		parsers.put("throw", new ThrowReader(this));
		parsers.put("validate", new ValidateReader(this));
		parsers.put("wait", new WaitReader(this));
		parsers.put("while", new WhileReader(this));
	}
	/**
	 * choose right parser and parse
	 * @param element
	 * @param parent
	 */
	public Activity readActivity(Element element, CompositeActivity parent){
		ActivityReader activityReader=parsers.get(element.getLocalName().toLowerCase());
		if(activityReader!=null){
			return activityReader.read(element, parent);
		}else
			System.out.println("bpel elements mapping error!");
		return null;
	}
	
	/**
	 * get all activity elements of parent element
	 * @param element
	 * @return list or null
	 */
	public List getActivityElements(Element element){
		List list=null;
		int length=element.getChildNodes().getLength();
		if(length>0){
			list=new ArrayList();
			for(int i=0;i<length;i++){
				Node child=element.getChildNodes().item(i);
				//changed by zhu
				//if(child.getNodeType()==Node.ELEMENT_NODE)
				if(child.getNodeType()==Node.ELEMENT_NODE && parsers.containsKey(child.getLocalName().toLowerCase()))
					list.add(child);
			}
		}
		return list;
	}
	
	/**
	 * get this specific activity element with name
	 * @param element
	 * @param name
	 * @return
	 */
	public Element getTheElement(Element element,String name){
		for(Node child=element.getFirstChild();child!=null;child=child.getNextSibling()){
			if(child.getNodeType()==Node.ELEMENT_NODE && child.getLocalName().toLowerCase().matches(name.toLowerCase())){
				return (Element)child;
			}
		}
		return null;
	}
	/**
	 * 
	 * @param element
	 * @param name
	 * @return
	 */
	public List getTheElements(Element element,String name){
		List list=null;
		int length=element.getChildNodes().getLength();
		if(length>0){
			list=new ArrayList();
			for(int i=0;i<length;i++){
				Node child=element.getChildNodes().item(i);
				if(child.getNodeType()==Node.ELEMENT_NODE && child.getLocalName().toLowerCase().matches(name.toLowerCase()))
					list.add(child);
			}
		}
		return list;
	}
	
	//use for test
	public static void main(String arg[]){
		String url="D:\\trip.bpel";
		//String url="d:\\��Ŀ\\���\\bpeltest.bpel";
		File file=new File(url);
		Parser parser=new Parser();
		parser.parse(file);
	}
}
