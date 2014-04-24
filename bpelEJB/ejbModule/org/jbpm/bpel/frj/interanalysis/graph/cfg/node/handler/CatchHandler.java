/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.handler;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers.Catch;

/**
 * @author frj
 * 2012-6-26
 */
public class CatchHandler extends Handler {
	private String faultName;
	private String variableName;
	/**
	 * 
	 */
	public CatchHandler() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 */
	public CatchHandler(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param activity
	 */
	public CatchHandler(Activity activity) {
		super(activity);
		// TODO Auto-generated constructor stub
	}
	public String getFaultName() {
		return faultName;
	}

	public void setFaultName(String faultName) {
		this.faultName = faultName;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
}
