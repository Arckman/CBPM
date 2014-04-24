/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-3-12
 */
public class RepeatUntil extends RepetitiveActivity {

	/**
	 * @param str
	 */
	public RepeatUntil(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public RepeatUntil(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addChild(Activity activity) {
		// TODO Auto-generated method stub
		activites.add(activity);
		nodesMap.put(activity.getName(), activity);
		activity.setNext(loop);
		loop.setNext(activity);
	}

}
