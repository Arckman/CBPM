/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Wait;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class WaitReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public WaitReader(Parser p) {
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
			name="Wait#"+(++num);
		Wait wait=new Wait(name);
		//set property
		setProperty(element,wait,parent);
		//set links
		setConnection(element,wait,parent);
		//set activites of this activity
		return wait;
	}

}
