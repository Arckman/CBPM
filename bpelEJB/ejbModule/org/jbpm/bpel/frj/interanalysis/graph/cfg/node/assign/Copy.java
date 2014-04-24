/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph.cfg.node.assign;

/**
 * @author frj
 *
 */
public class Copy extends AssignOperation {
	private From from=null;
	private To to=null;
	
	public From getFrom() {
		return from;
	}
	public void setFrom(From from) {
		this.from = from;
	}
	public To getTo() {
		return to;
	}
	public void setTo(To to) {
		this.to = to;
	}
	
}
