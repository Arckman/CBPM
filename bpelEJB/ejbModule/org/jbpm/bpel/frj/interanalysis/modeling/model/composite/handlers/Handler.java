/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;

/**
 * @author frj
 *
 */
public class Handler extends CompositeActivity {
	private Activity activity;

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	/**
	 * @param str
	 */
	public Handler(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Handler(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addChild(Activity activity) {
		// TODO Auto-generated method stub
		setActivity(activity);
	}
}
