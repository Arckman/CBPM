/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.Iterator;
import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Pick;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.AlarmAction;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.ReceiveAction;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class PickReader extends ActivityReader {
	private static int num=0;

	/**
	 * @param p
	 */
	public PickReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Activity read(Element element, CompositeActivity parent) {
		// TODO Auto-generated method stub
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME)){
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		}else
			name="Pick#"+(++num);
		Pick pick=new Pick(name);
		setProperty(element, pick, parent);
		setConnection(element, pick, parent);
		//OnMessage
		List onMegElements=this.getParser().getTheElements(element, BpelConstants.ElEM_PICK_ONMESSAGE);
		if(onMegElements!=null){
			Iterator i=onMegElements.iterator();
			while(i.hasNext()){
				Element onMsgE=(Element)i.next();
				//recieve action
				ReceiveAction recieveAction=new ReceiveAction();
				readReceiveAction(recieveAction, onMsgE);
				Element activityE=this.getParser().getActivityElement(onMsgE);
				Activity activity=this.getParser().readActivity(activityE, pick);
				pick.addOnMsgAction(recieveAction, activity);
			}
		}
		//onAlarm
		List onAlarmElements=this.getParser().getTheElements(element, BpelConstants.ELEM_PICK_ONALARM);
		if(onAlarmElements!=null){
			Iterator i=onAlarmElements.iterator();
			while(i.hasNext()){
				Element onAlarmE=(Element)i.next();
				//Alarm action
				AlarmAction alarmAction=new AlarmAction();
				readAlarmAction(alarmAction, onAlarmE);
				Element activityE=this.getParser().getActivityElement(onAlarmE);
				Activity activity=this.getParser().readActivity(activityE, pick);
				pick.addOnAlarmAction(alarmAction, activity);
			}
		}
		return pick;
	}
	
	private void readReceiveAction(ReceiveAction receiveAction,Element onMsgE){
		receiveAction.setPartnerLink(onMsgE.getAttribute(BpelConstants.ATTR_PARTNERLINK));
		receiveAction.setOperation(onMsgE.getAttribute(BpelConstants.ATTR_OPERATION));
		if(onMsgE.hasAttribute(BpelConstants.ATTR_VARIABLE)){
			receiveAction.setVariableName(onMsgE.getAttribute(BpelConstants.ATTR_VARIABLE));
		}
		if(onMsgE.hasAttribute(BpelConstants.ATTR_PORTTYPE)){
			receiveAction.setPortType(onMsgE.getAttribute(BpelConstants.ATTR_PORTTYPE));
		}
		if(onMsgE.hasAttribute(BpelConstants.ATTR_MSGEXCHANGE))
			receiveAction.setMsgExchange(onMsgE.getAttribute(BpelConstants.ATTR_MSGEXCHANGE));
	}
	
	private void readAlarmAction(AlarmAction alarmAction,Element onAlarmE){
		Element forE=this.getParser().getTheElement(onAlarmE, BpelConstants.ELEM_FOR);
		if(forE!=null){
			alarmAction.set_for(forE.getTextContent());
			alarmAction.set_for_lang(forE.getAttribute(BpelConstants.ATTR_EXPRESSIONLANGUAGE));
		}
		Element untilE=this.getParser().getTheElement(onAlarmE, BpelConstants.ELEM_UNTIL);
		if(untilE!=null){
			alarmAction.setUntil(untilE.getTextContent());
			alarmAction.setUntil_lang(untilE.getAttribute(BpelConstants.ATTR_EXPRESSIONLANGUAGE));
		}
	}
}
