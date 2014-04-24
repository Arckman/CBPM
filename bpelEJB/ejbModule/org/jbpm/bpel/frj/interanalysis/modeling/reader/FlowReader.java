/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.Iterator;
import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Flow;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Flow.LinkDefinition;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class FlowReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public FlowReader(Parser p) {
		super(p);
		// TODO Auto-generated constructor stub
	}
	@Override
	public Activity read(Element element, CompositeActivity parent) {
		// TODO Auto-generated method stub
		String name;
		if(element.hasAttribute(BpelConstants.ATTR_NAME))
			name=element.getAttribute(BpelConstants.ATTR_NAME);
		else
			name="Flow#"+(++num);
		//new a activity
		Flow flow=new Flow(name);
		//set property
		setProperty(element,flow,parent);
		//set links
		setConnection(element,flow,parent);
		//set activites of this activity
		readFlow(element,flow);
		return flow;
	}
	
	private void readFlow(Element element,Flow parent){
		// links
		Element linksElem=this.getParser().getTheElement(element,BpelConstants.ElEM_LINKS);
		if(linksElem!=null){
			List list=this.getParser().getTheElements(linksElem,BpelConstants.ElEM_LINK);
			for(int i=0;i<list.size();i++){
				Element linkElem=(Element)list.get(i);
				String linkName=linkElem.getAttribute(BpelConstants.ATTR_NAME);
				LinkDefinition ld=parent.new LinkDefinition(linkName);
				parent.addLink(ld);
			}
		}
		
		//activities
		List nodes=this.getParser().getActivityElements(element);
		Iterator iterator=nodes.iterator();
		while(iterator.hasNext()){
			Element child=(Element)iterator.next();
			this.getParser().readActivity(child, parent);
			}
	}

}
