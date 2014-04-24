/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Receive;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class ReceiveReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public ReceiveReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}
	public Activity read(Element element, CompositeActivity parent) {
		// TODO Auto-generated method stub
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME))
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		else
			name="Receive#"+(++num);
		//new a activity
		Receive receive=new Receive(name);
		//set property
		setProperty(element,receive,parent);
		//set links
		setConnection(element,receive,parent);
		this.readReceive(element, receive);
		return receive;
	}

	private void readReceive(Element element,Receive receive){
		//partnerLink
		String partnerLink=element.getAttribute(BpelConstants.ATTR_PARTNERLINK);
		receive.setPartnerLink(partnerLink);
		//set income partnerLinkType
		Scope globleScope=receive.getBpelModel().getGlobleScope();
		globleScope.getBpelModel().addIncomePartnerLinkType(receive.getName(),globleScope.getPartnerLinks().get(partnerLink).getPartnerLinkType());
		//portType
		String portType=element.getAttribute(BpelConstants.ATTR_PORTTYPE);
		if(portType!=null)
			receive.setPortType(portType);
		//operation
		String operation=element.getAttribute(BpelConstants.ATTR_OPERATION);
		receive.setOperation(operation);
		//createInstance
		String createInstance=element.getAttribute(BpelConstants.ATTR_CREATEINSTANCE);
		if(createInstance!=null)
			if(createInstance.equals("yes"))
				receive.setCreateInstance(true);
		//msgExchange
		String msgExchange=element.getAttribute(BpelConstants.ATTR_MSGEXCHANGE);
		if(msgExchange!=null)
			receive.setMsgExchange(msgExchange);
		//variable
		String variableName=element.getAttribute(BpelConstants.ATTR_VARIABLE);
		if(variableName!=null)
			receive.setVariableName(variableName);
	}
}
