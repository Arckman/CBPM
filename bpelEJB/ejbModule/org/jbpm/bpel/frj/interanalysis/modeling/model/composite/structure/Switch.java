package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;


public class Switch extends StructureActivity{
	private List conditions=new ArrayList();
	public Switch(String str) {
		super(str);
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
	
	public void addCaseCondition(String condition){
		this.conditions.add(condition);
	}

	public List getConditions() {
		return conditions;
	}
}
