/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.basic.assign;

/**
 * @author frj
 * 2012-5-17
 */
public class ToPartnerLink extends To {

	private String partnerLink;
	private String endpointReference;
	/**
	 * 
	 */
	public ToPartnerLink() {
		// TODO Auto-generated constructor stub
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
