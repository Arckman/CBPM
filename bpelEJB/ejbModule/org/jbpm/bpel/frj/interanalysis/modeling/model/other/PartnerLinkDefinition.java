/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.other;

/**
 * @author frj
 *
 */
public class PartnerLinkDefinition {

	private String name;
	private String partnerLinkType;
	private String myRole=null;
	private String partnerRole=null;
	public PartnerLinkDefinition(){};
	public PartnerLinkDefinition(String name){
		this.name=name;
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
