/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;



/**
 * @author frj
 * 2012-3-20
 */
public abstract class StructureActivity extends CompositeActivity {
	//private List<Activity> activites;
	//private Map<String,Activity> nodesMap;
	protected List<Activity> activites;
	protected Map<String,Activity> nodesMap;

	/**
	 * @param str
	 */
	public StructureActivity(String str) {
		super(str);
		// TODO Auto-generated constructor stub
		activites=new ArrayList<Activity>();
		nodesMap=new HashMap<String,Activity>();
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public StructureActivity(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
		activites=new ArrayList<Activity>();
		nodesMap=new HashMap<String,Activity>();
	}
	
	public abstract void addChild(Activity activity);
	
	public List getActivites(){
		return this.activites;
	}
	
	public Activity getLastActivity(){
		int length=this.activites.size();
		if(length>0){
			return this.activites.get(length-1);
		}
		else
			return null;
	}
  
}
