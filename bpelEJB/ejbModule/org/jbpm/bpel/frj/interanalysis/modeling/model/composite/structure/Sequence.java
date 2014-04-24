/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;

/**
 * @author frj
 * 2012-3-12
 */
public class Sequence extends StructureActivity {

	/**
	 * @param str
	 */
	public Sequence(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Sequence(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void addChild(Activity activity) {
		// TODO Auto-generated method stub
		int length=activites.size();
		if(length>0){
			Activity prior=activites.get(length-1);
			//prior--->activity
			//activity--->prior
			prior.setNext(activity);
			activity.setPrior(prior);
		}else if(length==0){
			//only contain begin and end
			//begin--->activity
			//activity--->begin
		}
		//activity--->end
		///end--->activity
		activites.add(activity);
		nodesMap.put(activity.getName(), activity);
	}


}
