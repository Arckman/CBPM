/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model;

import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;

/**
 * @author frj
 *
 */
public class CompensateScope extends Activity {
	
	private Scope scope;

	/**
	 * @param str
	 */
	public CompensateScope(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param model
	 * @param parent
	 */
	public CompensateScope(String str, BpelModel model, CompositeActivity parent) {
		super(str, model, parent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public CompensateScope(String str, Activity a, Activity b) {
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
	public CompensateScope(String str, Activity a, Activity b, BpelModel model,
			CompositeActivity parent) {
		super(str, a, b, model, parent);
		// TODO Auto-generated constructor stub
	}

	public Scope getScope() {
		return scope;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}
}
