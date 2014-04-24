/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.tranformer;


import java.util.Iterator;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.graph.cfg.BuildCFG;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.CFGGraph;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.Node;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.pick.PickBeginNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.node.pick.PickEndNode;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.PickOnAlarm;
import org.jbpm.bpel.frj.interanalysis.graph.cfg.other.PickOnMsg;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Pick;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.AlarmAction;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.ReceiveAction;


/**
 * @author frj
 * 2012-6-4
 */
public class PickTran extends Transformer {

	/**
	 * @param buildCFG
	 */
	public PickTran(BuildCFG buildCFG) {
		super(buildCFG);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jbpm.frj.graph.cfg.tranformer.Transformer#transform(org.jbpm.frj.graph.cfg.node.Node, org.jbpm.frj.graph.cfg.node.Node, modeling.model.Activity)
	 */
	@Override
	public Node transform(Node currentPrior, Node currentNext, Activity activity) {
		// TODO Auto-generated method stub
		PickBeginNode begin=new PickBeginNode(activity);
		PickEndNode end=new PickEndNode(activity);
		Pick pick=(Pick)activity;
		Map<ReceiveAction,Activity> onMsgActivities=pick.getOnMessage();
		Map<AlarmAction,Activity> onAlarmActivities=pick.getOnAlarm();
		this.setStandardProperty(begin, end, currentPrior);
		this.setConnection(currentPrior, currentNext, begin, end);
		this.tranformPick(begin, end, onMsgActivities, onAlarmActivities);
		return end;
	}

	
	private void setConnection(Node prior,Node next,PickBeginNode begin,PickEndNode end){
		//prior.remove(next);	next.remove(prior)
		if(prior.getNext().contains(next))
			prior.getNext().remove(next);
		if(next.getPrior().contains(prior))
			next.getPrior().remove(prior);
		//prior->begin;	end->next; begin->prior; next->end
		prior.addNextNode(begin);
		begin.addPriorNode(prior);
		end.addNextNode(next);
		next.addPriorNode(end);
		//begin->end; end->begin
		begin.addNextNode(end);
		end.addPriorNode(begin);
	}
	
	private void setStandardProperty(PickBeginNode begin,PickEndNode end,Node prior){
		//begin->end;end->begin
		begin.setEnd(end);
		end.setBegin(begin);
		//begin/end->org.jbpm.frj.graph
		CFGGraph graph=prior.getGraph();
		begin.setGraph(graph);
		end.setGraph(graph);
		//org.jbpm.frj.graph.add(begin)
		graph.addNode(begin);
	}
	
	private void tranformPick(PickBeginNode begin,PickEndNode end,
			Map<ReceiveAction,Activity> onMsgActivities,Map<AlarmAction,Activity> onAlarmActivities){
		Iterator i;
		//onMsg
		if(onMsgActivities!=null){
			i=onMsgActivities.keySet().iterator();
			while(i.hasNext()){
				ReceiveAction receiveAction=(ReceiveAction)i.next();
				PickOnMsg onMsg=new PickOnMsg(receiveAction);
				Activity subActivity=onMsgActivities.get(receiveAction);
				Node subNode=this.getBuildCFG().transformActivity(subActivity, begin, end);
				begin.addOnMsgNode(onMsg, subNode);
			}
		}
		//onAlarm
		if(onAlarmActivities!=null){
			i=onAlarmActivities.keySet().iterator();
			while(i.hasNext()){
				AlarmAction alarmAction=(AlarmAction)i.next();
				PickOnAlarm pickOnAlarm=new PickOnAlarm(alarmAction);
				Activity subActivity=onAlarmActivities.get(alarmAction);
				Node subNode=this.getBuildCFG().transformActivity(subActivity, begin, end);
				begin.addOnAlarmNode(pickOnAlarm, subNode);
			}
		}
	}
}
