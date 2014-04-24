/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Rethrow;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class RethrowReader extends ActivityReader {
	private static int num;
	/**
	 * @param p
	 */
	public RethrowReader(Parser p) {
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
			name="Rethrow#"+(++num);
		Rethrow rethrow=new Rethrow(name);
		//set property
		setProperty(element,rethrow,parent);
		//set links
		setConnection(element,rethrow,parent);
		//set activites of this activity
		return rethrow;
	}

	

}
