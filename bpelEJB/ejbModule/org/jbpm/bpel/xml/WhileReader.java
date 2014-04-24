/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the JBPM BPEL PUBLIC LICENSE AGREEMENT as
 * published by JBoss Inc.; either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jbpm.bpel.xml;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.graph.def.Activity;
import org.jbpm.bpel.graph.def.CompositeActivity;
import org.jbpm.bpel.graph.struct.While;
import org.jbpm.bpel.sublang.def.Expression;
import org.jbpm.bpel.xml.util.XmlUtil;
import org.w3c.dom.Element;

/**
 * Encapsulates the logic to create and connect process elements that constitute
 * the <i>while</i> structure.
 * @author Juan Cantu
 * @version $Revision: 1.7 $ $Date: 2007/05/31 12:55:12 $
 */
public class WhileReader extends ActivityReader {

  public Activity read(Element activityElem, CompositeActivity parent) {
    While _while = new While();
    readStandardProperties(activityElem, _while, parent);
    readWhile(activityElem, _while);
    return _while;
  }

  public void readWhile(Element whileElem, While _while) {
    validateNonInitial(whileElem, _while);
    
    // condition
    Element conditionElem = XmlUtil.getElement(whileElem,
        BpelConstants.NS_BPEL, BpelConstants.ELEM_CONDITION);
    Expression condition=bpelReader.readExpression(conditionElem, _while);
    _while.setCondition(condition);

    // activity
    Element activityElem = bpelReader.getActivityElement(whileElem);
    bpelReader.readActivity(activityElem, _while);
    
  //=======================================================================================================
//    JbpmConfiguration.addExpression(condition);
  }
}
