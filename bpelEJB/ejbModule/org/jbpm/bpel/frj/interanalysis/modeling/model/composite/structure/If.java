package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.reader.ActivityReader;


public class If extends StructureActivity {
	
	private Map<String,Activity> ifActivity=new HashMap();
	private List<String> conditions=new ArrayList();
	
	public If(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	public If(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addChild(Activity activity) {
		// TODO Auto-generated method stub
		activity.setPrior(null);
		activity.setNext(null);
		activites.add(activity);
		nodesMap.put(activity.getName(), activity);
	}
	public void addActivity(String condition,Activity activity){
		ifActivity.put(condition, activity);
		conditions.add(condition);
	}

	public Map<String, Activity> getIfActivity() {
		return ifActivity;
	}

	public List<String> getConditions() {
		return conditions;
	}
}
