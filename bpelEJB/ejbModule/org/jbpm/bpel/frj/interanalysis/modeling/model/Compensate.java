/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model;

import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;

/**
 * @author frj
 *
 */
public class Compensate extends Activity {

	/**
	 * @param str
	 */
	public Compensate(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param model
	 * @param parent
	 */
	public Compensate(String str, BpelModel model, CompositeActivity parent) {
		super(str, model, parent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Compensate(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 * @param model
	 * @param parent
	 */
	public Compensate(String str, Activity a, Activity b, BpelModel model,
			CompositeActivity parent) {
		super(str, a, b, model, parent);
		// TODO Auto-generated constructor stub
	}

}
