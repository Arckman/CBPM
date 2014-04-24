/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * @author frj
 * 2012-3-12
 */
public class Flow extends StructureActivity {
	private Map<String , LinkDefinition> link=new HashMap(2);
	/**
	 * @param str
	 */
	public Flow(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public Flow(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}
	public void addChild(Activity activity) {
		// TODO Auto-generated method stub
		activity.setPrior(null);
		activity.setNext(null);
		activites.add(activity);
		nodesMap.put(activity.getName(), activity);
	}
	
	
	public void addLink(LinkDefinition link){
		this.link.put( link.getName(),link);
	}
	
	public LinkDefinition findLink(String name) {
	    LinkDefinition theLink = this.link.get(name);
	    return theLink != null ? theLink : super.findLink(name);
	  }
	
	public Map<String, LinkDefinition> getLink() {
		return link;
	}


	public class LinkDefinition{
		private String name;
		private Activity source;
		private Activity target;
		private String transitionCondition=null;
		public LinkDefinition(){};
		public LinkDefinition(String name){this.name=name;};
		public String getName(){
			return name;
		}
		public Activity getSource(){
			return this.source;
		}
		public Activity getTarget(){
			return this.target;
		}
		public void setName(String name){
			this.name=name;
		}
		public void setSource(Activity source) {
		    this.source = source;
		  }
		public void setTarget(Activity target) {
		    this.target = target;
		  }
		public String getTransitionCondition() {
			return transitionCondition;
		}
		public void setTransitionCondition(String transitionCondition) {
			this.transitionCondition = transitionCondition;
		}
	}
}
 