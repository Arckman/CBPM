/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Reply;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class ReplyReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public ReplyReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}
	
	public Activity read(Element element, CompositeActivity parent) {
		// TODO Auto-generated method stub
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME))
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		else
			name="Reply#"+(++num);
		//new a activity
		Reply reply=new Reply(name);
		//set property
		setProperty(element,reply,parent);
		//set links
		setConnection(element,reply,parent);
		this.readReply(element, reply);
		return reply;
	}

	private void readReply(Element element,Reply reply){
		//partnerLink
		String partnerLink=element.getAttribute(BpelConstants.ATTR_PARTNERLINK);
		reply.setPartnerLink(partnerLink);
		//portType
		String portType=element.getAttribute(BpelConstants.ATTR_PORTTYPE);
		if(portType!=null)
			reply.setPortType(portType);
		//operation
		String operation=element.getAttribute(BpelConstants.ATTR_OPERATION);
		reply.setOperation(operation);
		//faultName
		String faultName=element.getAttribute(BpelConstants.ATTR_FAULTNAME);
		if(faultName!=null)
			reply.setFaultName(faultName);
		//msgExchange
		String msgExchange=element.getAttribute(BpelConstants.ATTR_MSGEXCHANGE);
		if(msgExchange!=null)
			reply.setMsgExchange(msgExchange);
		//variable
		String variableName=element.getAttribute(BpelConstants.ATTR_VARIABLE);
		if(variableName!=null)
			reply.setVariableName(variableName);
	}
}
