/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.graph;

import org.jbpm.bpel.frj.interanalysis.modeling.BpelModel;

/**
 * @author frj
 * 2012-5-14
 */
public interface GraphBuilder {

	public Graph buildGraph(BpelModel bpelModel);
}
