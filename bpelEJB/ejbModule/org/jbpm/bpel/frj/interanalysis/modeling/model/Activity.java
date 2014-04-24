/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Flow.LinkDefinition;


/**
 * parent of all nodes of bpel activity class
 * @author frj
 * 2012-3-11
 */
public class Activity {
	
	private String name;
	private Activity prior;
	private Activity next;
	private BpelModel bpelModel=null;
	private String joinCondition=null;
	private CompositeActivity compositeActivity=null;
	
	//=======links==========
	private List sources=new ArrayList();
	private List targets=new ArrayList();
	
	public Activity(String str){
		name=str;
		prior=null;
		next=null;
	}
	public Activity(String str,BpelModel model,CompositeActivity parent){
		name=str;
		prior=null;
		next=null;
		bpelModel=model;
		compositeActivity=parent;
	}
	public Activity(String str,Activity a,Activity b){
		name=str;
		prior=a;
		next=b;
	}
	public Activity(String str,Activity a,Activity b,BpelModel model,CompositeActivity parent){
		name=str;
		prior=a;
		next=b;
		bpelModel=model;
		compositeActivity=parent;
	}
	
	public Activity getPrior() {
		return prior;
	}
	public void setPrior(Activity prior) {
		this.prior = prior;
	}
	public Activity getNext() {
		return next;
	}
	public void setNext(Activity next) {
		this.next = next;
	}
	public String getName() {
		return name;
	}
	public BpelModel getBpelModel() {
		return bpelModel;
	}
	public void setBpelModel(BpelModel bpelModel) {
		this.bpelModel = bpelModel;
	}
	public String getJoinCondition() {
		return joinCondition;
	}
	public void setJoinCondition(String joinCondition) {
		this.joinCondition = joinCondition;
	}
	public CompositeActivity getCompositeActivity() {
		return compositeActivity;
	}
	public void setCompositeActivity(CompositeActivity compositeActivity) {
		this.compositeActivity = compositeActivity;
	}
	public void addSources(LinkDefinition link){
		if (link == null)
		      throw new IllegalArgumentException("link is null");

		    sources.add(link);
		    link.setSource(this);
	}
	public void addTargets(LinkDefinition link){
		if (link == null)
		      throw new IllegalArgumentException("link is null");
		    targets.add(link);
		    link.setTarget(this);
	}
	
	/**
	 * prior activity of this activity or this activity.compositeActivity
	 * @return
	 */
	public Activity getPriorActivity(){
		if(this.prior!=null)
			return this.prior;
		else if(this.compositeActivity!=null)
			return this.compositeActivity.getPriorActivity();
		else
			return null;
	}
	
	/**
	 * next activity of this activity or this activity.compositeActivity
	 * @return
	 */
	public Activity getNextActivity(){
		if(this.next!=null)
			return this.next;
		else if(this.compositeActivity!=null)
			return this.compositeActivity.getNextActivity();
		else
			return null;
	}
}
