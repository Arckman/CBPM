/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.AlarmAction;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.ReceiveAction;


/**
 * @author frj
 * 2012-3-12
 */
public class Pick extends StructureActivity {
	
	private Map<ReceiveAction,Activity> onMessage=new HashMap<ReceiveAction,Activity>();
	private Map<AlarmAction,Activity> onAlarm=new HashMap<AlarmAction, Activity>();

	/**
	 * @param str
	 */
	public Pick(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Pick(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}
	
	public void addOnMsgAction(ReceiveAction recieveAction,Activity activity){
		this.onMessage.put(recieveAction, activity);
	}

	public void addOnAlarmAction(AlarmAction alarmAction,Activity activity){
		this.onAlarm.put(alarmAction, activity);
	}
	
	public Map<ReceiveAction, Activity> getOnMessage() {
		return onMessage;
	}
	
	public Map<AlarmAction, Activity> getOnAlarm() {
		return onAlarm;
	}

	@Override
	public void addChild(Activity activity){
		this.activites.add(activity);
		this.nodesMap.put(activity.getName(), activity);
	}
	
}
