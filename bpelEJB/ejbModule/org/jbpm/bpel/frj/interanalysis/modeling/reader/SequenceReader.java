/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;

import java.util.Iterator;
import java.util.List;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.structure.Sequence;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * @author frj
 * 2012-3-12
 */
public class SequenceReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public SequenceReader(Parser p) {
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
			name="Sequence#"+(++num);
		//new a activity
		Sequence sequence=new Sequence(name);
		//set property
		setProperty(element,sequence,parent);
		//set links
		setConnection(element,sequence,parent);
		//set activites of this activity
		readSequence(element,sequence);
		return sequence;
	}
	
	private void readSequence(Element element,CompositeActivity parent){
		List nodes=this.getParser().getActivityElements(element);
		Iterator iterator=nodes.iterator();
		while(iterator.hasNext()){
			Element child=(Element)iterator.next();
			this.getParser().readActivity(child, parent);
			}
	}

}
