/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.If;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.While;



/**
 * @author frj
 * 2012-3-11
 */
public class BpelModel {
	private String name=null;
	private Scope globleScope=null;
	private Map<String,Activity> nodes;
	private String targetNS;
	private final long id=System.currentTimeMillis();
	private Map<String,String> incomePartnerLinkType=new HashMap<String,String>();//nodeName->partnerLinkType
	private Map<String,String> outgoPartnerLinkType=new HashMap<String,String>();

	public String getTargetNS() {
		return targetNS;
	}

	public void setTargetNS(String targetNS) {
		this.targetNS = targetNS;
	}

	public BpelModel(){
		nodes=new HashMap<String,Activity>();
		globleScope=new Scope("globle");
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Scope getGlobleScope() {
		return globleScope;
	}
	
	public void addActivity(Activity activity){
		this.nodes.put(activity.getName(), activity);
	}

	public Map<String, Activity> getNodes() {
		// TODO Auto-generated method stub
		return this.nodes;
	}
	
	public Activity getTheActivity(String name){
		return this.nodes.get(name);
	}
	public void addIncomePartnerLinkType(String nodeName,String name){
		if(!this.incomePartnerLinkType.keySet().contains(nodeName))
			this.incomePartnerLinkType.put(nodeName,name);
	}
	public void addOutgoPartnerLinkType(String nodeName,String name){
		if(!this.outgoPartnerLinkType.keySet().contains(nodeName))
			this.outgoPartnerLinkType.put(nodeName,name);
	}
	public Collection<String> getIncomePartnerLinkTypes(){return this.incomePartnerLinkType.values();}
	public Collection<String> getOutgoPartnerLinkTypes(){return this.outgoPartnerLinkType.values();}
	public Map<String,String> getOutgoPartnerLinkTypesMap(){return outgoPartnerLinkType;}
	public boolean isBranch(String nodeName){
		Activity activity=nodes.get(nodeName);
		if(activity==null)
			return false;
		if(activity instanceof If||activity instanceof While)
			return true;
		else return false;
	}
}
