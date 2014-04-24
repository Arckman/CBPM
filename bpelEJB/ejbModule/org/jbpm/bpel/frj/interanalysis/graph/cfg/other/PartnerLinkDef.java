/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.other;

import org.jbpm.bpel.frj.interanalysis.modeling.model.other.PartnerLinkDefinition;

/**
 * @author frj
 * 2012-7-9
 */
public class PartnerLinkDef{
	private String name;
	private String partnerLinkType;
	private String myRole=null;
	private String partnerRole=null;
	public PartnerLinkDef(){};
	public PartnerLinkDef(String name){
		this.name=name;
	}
	public PartnerLinkDef(PartnerLinkDefinition partnerLinkDefinition){
		this.name=partnerLinkDefinition.getName();
		this.partnerLinkType=partnerLinkDefinition.getPartnerLinkType();
		this.myRole=partnerLinkDefinition.getMyRole();
		this.partnerRole=partnerLinkDefinition.getPartnerRole();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPartnerLinkType() {
		return partnerLinkType;
	}
	public void setPartnerLinkType(String partnerLinkType) {
		this.partnerLinkType = partnerLinkType;
	}
	public String getMyRole() {
		return myRole;
	}
	public void setMyRole(String myRole) {
		this.myRole = myRole;
	}
	public String getPartnerRole() {
		return partnerRole;
	}
	public void setPartnerRole(String partnerRole) {
		this.partnerRole = partnerRole;
	}
}
