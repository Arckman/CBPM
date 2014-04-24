/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.basic.Throw;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.other.VariableDefinition;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class ThrowReader extends ActivityReader {
	private static int num=0;
	/**
	 * @param p
	 */
	public ThrowReader(Parser p) {
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
			name="Throw#"+(++num);
		Throw _throw=new Throw(name);
		//set property
		setProperty(element,_throw,parent);
		//set links
		setConnection(element,_throw,parent);
		//set activites of this activity
	
		//fault name
		String faultName=element.getAttribute(BpelConstants.ATTR_FAULTNAME);
		_throw.setFaultName(faultName);
		//fault variable optional
		if(element.hasAttribute(BpelConstants.ATTR_FAULTVARIABLE)){
			String variableName=element.getAttribute(BpelConstants.ATTR_FAULTVARIABLE);
			_throw.setVariableName(variableName);
			VariableDefinition variable=parent.findVariable(variableName);
			_throw.setFaultVariable(variable);
		}
		return _throw;
	}
}
