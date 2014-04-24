/**
 * 
 */
package org.jbpm.bpel.frj.util;

/**
 * contain some constants of bpel
 * @author frj
 * 2012-3-20
 */
public class BpelConstants {

	public static final String bpel_NS="http://docs.oasis-open.org/wsbpel/2.0/process/executable";
	
	public static final String ATTR_NAME="name";
	public static final String ATTR_TARGETNAMESPACE="targetNamespace";
	
	public static final String ELEM_JOINCONDITION="joinCondition";
	public static final String ELEM_TRANSITIONCONDITION="transitionCondition";
	public static final String ElEM_TARGETS="targets";
	public static final String ElEM_TARGET="target";
	public static final String ATTR_LINKNAME="linkName";
	public static final String ElEM_SOURCES="sources";
	public static final String ElEM_SOURCE="source";
	
	public static final String ElEM_CONDITION="condition";
	public static final String ATTR_CONDITION="condition";
	
	
	//<if>
	public static final String ELEM_ELSEIF="elseif";
	public static final String ELEM_ELSE="else";
	
	//<flow>
	public static final String ElEM_LINKS="links";
	public static final String ElEM_LINK="link";
	
	//<pick>
	public static final String ElEM_PICK_ONMESSAGE="OnMessage";
	public static final String ELEM_PICK_ONALARM="onAlarm";
	
	//<switch>
	public static final String ELEM_CASE="case";
	public static final String ELEM_OTHERWISE="otherwise";
	
	//<partnerLink>
	public static final String ELEM_PARTNERLINKS="partnerLinks";
	public static final String ELEM_PARTNERLINK="partnerLink";
	public static final String ATTR_PARTNERLINKTYPE="partnerLinkType";
	public static final String ATTR_PARTNERLINK_MYROLE="myRole";
	public static final String ATTR_PARTNERLINK_PARTNERROLE="partnerRole";
	
	//<variable>
	public static final String ELEM_VARIABLES="variables";
	public static final String ELEM_VARIABLE="variable";
	public static final String ATTR_VARIABLE_TYPE="type";
	public static final String ATTR_VARIABLE_MSGTYPE="messageType";
	
	//<correlation>
	public static final String ELEM_CORRELATIONSETS="correlationSets";
	public static final String ELEM_CORRELATIONSET="correlationSet";
	public static final String ATTR_CORRELATIONSET_PROPERTIES="properties";
	
	
	//<faultHandler>
	public static final String ELEM_FAULTHANDLERS="faultHandlers";
	public static final String ELEM_CATCH="catch";
	public static final String ELEM_CATCHALL="catchAll";
	public static final String ATTRI_CATCH_FAULTNAME="faultName";
	public static final String ELEM_COMPENSATIONHANDLER="compensationHandler";
	public static final String ELEM_TERMINATIONNHANDLER="terminationHandler";
	public static final String ELEM_EVENTHANDLERS="eventHandlers";
	public static final String ELEM_ONEVENT="onEvent";
	public static final String ELEM_ONALARM="onAlarm";
	
	
	//<compensate>
	public static final String ELEM_COMPENSATE="compensate";
	public static final String ATTR_COMPENSATE_SCOPE="scope";
	
	//<compensateScope>
	public static final String ELEM_COMPENSATESCOPE="compensateScope";
	public static final String ATTR_COMPENSATESCOPE_TARGET="target";
	
	
	//<assign>
	public static final String ELEM_ASSIGN_COPY="copy";
	public static final String ELEM_ASSIGN_COPY_FROM="from";
	public static final String ELEM_ASSGIN_COPY_TO="to";
	public static final String ATTR_ASSIGN_PROPERTY="property";
	public static final String ATTR_ASSIGN_VARIABLE="variable";
	public static final String ATTR_ASSIGN_VARIABLE_PART="part";
	public static final String ELEM_ASSIGN_VARIABLE_QUERY="query";
	public static final String ATTR_ASSIGN_VARIABLE_QUERY_LANGUAGE="queryLanguage";
	public static final String ATTR_ASSIGN_PARTNERLINK="partnerLink";
	public static final String ATTR_ASSIGN_PARTNERLINK_ENDPOINTREFERENCE="endpointReference";
	public static final String ELEM_ASSIGN_LITERAL="literal";
	public static final String ATTR_ASSIGN_EXPRESSION="expression";
	public static final String ATTR_ASSIGN_EXPRESSIONLANGUAGE="expressionLanguage";
	
	
	//<invoke>
	public static final String ATTR_PARTNERLINK="partnerLink";
	public static final String ATTR_PORTTYPE="portType";
	public static final String ATTR_OPERATION="operation";
	public static final String ATTR_INPUTVARIABLE="inputVariable";
	public static final String ATTR_OUTPUTVARIABLE="outputVariable";
	public static final String ELEM_OTPARTS="toParts";
	public static final String ELEM_OTPART="toPart";
	public static final String ATTR_TOVARIABLE="toVariable";
	public static final String ELEM_FROMPARTS="fromParts";
	public static final String ELEM_FROMPART="fromPart";
	public static final String ATTR_FROMVARIABLE="fromVariable";
	
	//<receive>
	public static final String ATTR_CREATEINSTANCE="createInstance";
	public static final String ATTR_MSGEXCHANGE="messageExchange";
	
	//<reply>
	public static final String ATTR_FAULTNAME="faultName";
	
	//<throw>
	public static final String ATTR_FAULTVARIABLE="faultVariable";
	
	//<eventHandler>
	public static final String ATTR_VARIABLE="variable";
	public static final String ATTR_ELEMENT="element";
	public static final String ATTR_PART="part";
	public static final String ELEM_FOR="for";
	public static final String ELEM_UNTIL="until";
	public static final String ELEM_REPEATEVERY="repeatEvery";
	public static final String ATTR_EXPRESSIONLANGUAGE="expressionLanguage";
}
