/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.reader;


import org.jbpm.bpel.frj.interanalysis.modeling.Parser;
import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.CompensateScope;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.CompositeActivity;
import org.jbpm.bpel.frj.interanalysis.modeling.model.composite.scope.Scope;
import org.jbpm.bpel.frj.util.BpelConstants;
import org.w3c.dom.Element;


/**
 * @author frj
 * 2012-3-12
 */
public class CompensateScopeReader extends ActivityReader {

	private static int num=0;
	/**
	 * @param p
	 */
	public CompensateScopeReader(Parser p) {
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
			name="CompensateScope#"+(++num);
		CompensateScope compensateScope=new CompensateScope(name);
		setProperty(element, compensateScope, parent);
		setConnection(element, compensateScope, parent);
		
		readCompensateScope(element, compensateScope);
		return compensateScope;
	}

	private void readCompensateScope(Element element,CompensateScope compensateScope){
		String targetScopeName=element.getAttribute(BpelConstants.ATTR_COMPENSATESCOPE_TARGET);
		Scope taregetScope=null;
		if(targetScopeName!=null){
			taregetScope=(Scope)compensateScope.getBpelModel().getTheActivity(targetScopeName);
			compensateScope.setScope(taregetScope);
		}else
			System.out.println("error in readCompensateScope");
	}
}
