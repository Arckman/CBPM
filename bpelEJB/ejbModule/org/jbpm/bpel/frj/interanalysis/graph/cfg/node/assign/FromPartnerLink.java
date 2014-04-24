/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign;

/**
 * @author frj
 * 2012-5-17
 */
public class FromPartnerLink extends From {

	private String partnerLink;
	private String endpointReference;
	/**
	 * 
	 */
	public FromPartnerLink() {
		// TODO Auto-generated constructor stub
	}
	
	public FromPartnerLink(org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign.FromPartnerLink fromPartnerLink) {
		this.partnerLink=fromPartnerLink.getPartnerLink();
		this.endpointReference=fromPartnerLink.getEndpointReference();
	}

	public String getPartnerLink() {
		return partnerLink;
	}
	public void setPartnerLink(String partnerLink) {
		this.partnerLink = partnerLink;
	}
	public String getEndpointReference() {
		return endpointReference;
	}
	public void setEndpointReference(String endpointReference) {
		this.endpointReference = endpointReference;
	}
}
