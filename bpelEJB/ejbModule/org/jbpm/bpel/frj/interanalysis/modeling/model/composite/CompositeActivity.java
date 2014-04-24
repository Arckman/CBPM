/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite;


import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Flow.LinkDefinition;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;

/**
 * @author frj
 * 2012-3-12
 */
public abstract class CompositeActivity extends Activity {

	/**
	 * @param str
	 */
	public CompositeActivity(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public CompositeActivity(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}
	/**
	 * find the specific link, used by a activity, not process
	 * @param name
	 * @return
	 */
	 public LinkDefinition findLink(String name) {
		    CompositeActivity parent = getCompositeActivity();
		    return parent != null ? parent.findLink(name) : null;
		  }
	 
	 /**
	  * find the specific variable, used by a activity, not process
	  * @param name
	  * @return
	  */
	 public VariableDefinition findVariable(String name){
		 CompositeActivity parent=getCompositeActivity();
		 return parent!=null?parent.findVariable(name):null;
	 }
	 
	public abstract void addChild(Activity activity);
}
