/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 * 2012-3-12
 */
public abstract class RepetitiveActivity extends StructureActivity {
	
	protected String condition;
	protected Loop loop = new Loop("Loop");
	/**
	 * @param str
	 */
	public RepetitiveActivity(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public RepetitiveActivity(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}
	
	public void setCondition(String condition){
		this.condition=condition;
	}
	
	public String getCondition() {
		return condition;
	}

	public Activity getLoopActivity(){
		return this.loop.getLoopActivity();
	}
	
	 public static class Loop extends Activity {
		 
		public Loop(String str) {
			super(str);
			// TODO Auto-generated constructor stub
		}
		protected Activity getLoopActivity(){
			return this.getNext();
		}
	 }

}
