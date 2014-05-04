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
package org.jbpm.bpel.integration.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.wsdl.Fault;
import javax.wsdl.Port;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.frj.VersionControlManager;
import org.jbpm.bpel.frj.monitor.ProcessMonitor;
import org.jbpm.bpel.frj.util.MonitorConstants;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.graph.exe.FaultInstance;
import org.jbpm.bpel.integration.soap.MessageDirection;
import org.jbpm.bpel.integration.soap.SoapFormatter;
import org.jbpm.bpel.variable.def.MessageType;
import org.jbpm.bpel.variable.exe.MessageValue;
import org.jbpm.bpel.wsdl.xml.WsdlUtil;
import org.jbpm.bpel.xml.BpelConstants;

import com.ibm.wsdl.extensions.soap.SOAPConstants;

/**
 * Provides support for the dynamic invocation of a service endpoint bound to SOAP.
 * @author Alejandro Guizar
 * @version $Revision: 1.1 $ $Date: 2008/01/30 07:18:22 $
 */
public class SoapCaller implements Caller {

  private final SoapFormatter formatter;
  private URL address;
  private final SOAPConnection soapConnection;

  private static final Log log = LogFactory.getLog(SoapCaller.class);

  private static MessageFactory messageFactory;
  private static SOAPConnectionFactory soapConnectionFactory;

  public SoapCaller(Port port) {
    formatter = new SoapFormatter(port.getBinding());

    // exclude non-soap ports
    SOAPAddress soapAddress = (SOAPAddress) WsdlUtil.getExtension(port.getExtensibilityElements(),
        SOAPConstants.Q_ELEM_SOAP_ADDRESS);
    if (soapAddress == null)
      throw new BpelException("not a soap-bound port: " + port);

    // exclude malformed locations
    String location = soapAddress.getLocationURI();
    try {
      address = new URL(location);
    }
    catch (MalformedURLException e) {
      throw new BpelException("invalid address location: " + location, e);
    }

    try {
      soapConnection = soapConnectionFactory.createConnection();
    }
    catch (SOAPException e) {
      throw new BpelException("could not create soap connection", e);
    }
  }

  public SoapFormatter getFormatter() {
    return formatter;
  }

  public URL getAddress() {
    return address;
  }

  public Map call(String operation, Map inputParts) {
    try {
      SOAPMessage soapOutput = callImpl(operation, inputParts);
      HashMap outputParts = new HashMap();

      if (!formatter.hasFault(soapOutput)) {
        formatter.readMessage(operation, soapOutput, outputParts, MessageDirection.OUTPUT);
        return outputParts;
      }

      Fault fault = formatter.readFault(operation, soapOutput, outputParts);
      /*
       * WS-BPEL 2.0 section 6.1: each WSDL fault is identified in WS-BPEL by a qualified name
       * formed by the target namespace of the WSDL document in which the relevant port type and
       * fault are defined, and the NCName of the fault
       */
      String targetNamespace = formatter.getBinding().getPortType().getQName().getNamespaceURI();
      QName faultQName = new QName(targetNamespace, fault.getName());

      MessageValue faultMessage = new MessageValue(new MessageType(fault.getMessage()));
      faultMessage.setParts(outputParts);

      FaultInstance faultInstance = new FaultInstance(faultQName, faultMessage);
      throw new BpelFaultException(faultInstance);
    }
    catch (SOAPException e) {
      // BPEL-286 raise SOAP communication exception as BPEL fault
      log.error("endpoint call failed: " + address, e);
      throw new BpelFaultException(BpelConstants.FAULT_INVOCATION_FAILURE);
    }
  }

  public void callOneWay(String operation, Map inputParts) {
    try {
      callImpl(operation, inputParts);
    }
    catch (SOAPException e) {
      // BPEL-286 raise SOAP communication exception as BPEL fault
      log.error("endpoint call failed: " + address, e);
      throw new BpelFaultException(BpelConstants.FAULT_INVOCATION_FAILURE);
    }
  }

  private SOAPMessage callImpl(String operation, Map inputParts) throws SOAPException {
    // create message
    SOAPMessage soapInput = messageFactory.createMessage();
    String rootMonitorName = null;
    rootMonitorName=(String)inputParts.get("rootMonitorName");
    inputParts.remove("rootMonitorName");
    long rootId = 0;
    rootId=(Long) inputParts.get("rootId");
    inputParts.remove("rootId");
    String parentMonitorName=null;
    parentMonitorName=(String)inputParts.get("parentMonitorName");
    inputParts.remove("parentMonitorName");
    long parentId =0;
    parentId=(Long) inputParts.get("parentId");
    inputParts.remove("parentId");
    
    // write message
    formatter.writeMessage(operation, soapInput, inputParts, MessageDirection.INPUT);
    //===========================add root and parent information into soap call input=========================
    SOAPPart soapPart=soapInput.getSOAPPart();
    SOAPEnvelope envelop=soapPart.getEnvelope();
    SOAPBody body=envelop.getBody();
    if(rootMonitorName!=null){
	    Name soapName=envelop.createName("rootMonitorName");
	    SOAPBodyElement bodyElement=body.addBodyElement(soapName);
	    bodyElement.addTextNode(rootMonitorName);
	    soapName=envelop.createName("rootId");
	    bodyElement=body.addBodyElement(soapName);
	    bodyElement.addTextNode((String.valueOf(rootId)));
	    soapName=envelop.createName("parentMonitorName");
	    bodyElement=body.addBodyElement(soapName);
	    bodyElement.addTextNode(parentMonitorName);
	    soapName=envelop.createName("parentId");
	    bodyElement=body.addBodyElement(soapName);
	    bodyElement.addTextNode((String.valueOf(parentId)));
    }

    // call endpoint
    log.debug("calling endpoint at: " + address);
    //======================before call, try to configure address========================
    VersionControlManager vm=JbpmConfiguration.getVersionControlManager();
    ProcessMonitor target=vm.getPMFromURL(address);
//    if(vm.getStrategy().equals(MonitorConstants.STRATEGY_CONCURRENT))
//    	address=target.getNewURL();
    if(target!=null&&target.isNeedUpdate()){//target need update
    	//TODO if need suspend during target updating, add here! This thread should not wait on target object, because of distribution.
    	if(target.getSetupState().equals(MonitorConstants.STATE_UPDATE)){
    		System.out.println("target is updating");
    		synchronized(target){
    			if(target.getSetupState().equals(MonitorConstants.STATE_UPDATE)){//double check
		    		try {
						target.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			//TODO if updated,resume should wait for finishing of updating for target
	    			try {
	    				Thread.currentThread().sleep(1000);
	    			} catch (InterruptedException e) {
	    				// TODO Auto-generated catch block
	    				e.printStackTrace();
	    			}
    			}
    		}
    	}
    	//consider strategy
    	if(vm.getStrategy().equals(MonitorConstants.STRATEGY_WAIT)){
    			
    	}else if(vm.getStrategy().equals(MonitorConstants.STRATEGY_CONCURRENT)){
    		address=target.getOldURL();
//    		if(target.checkHasPast(rootMonitorName, rootId)){//has past use
//    			address=target.getOldURL();
//    		}else{//using new version
//    			if(target.getSetupState().equals(MonitorConstants.STATE_UPDATE)){
//    	    		synchronized(target){
//    		    		try {
//    						target.wait();
//    					} catch (InterruptedException e) {
//    						// TODO Auto-generated catch block
//    						e.printStackTrace();
//    					}
//    	    		}
//    	    	}
//    			try {
//					Thread.currentThread().sleep(3000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//    			address=target.getNewURL();
//    		}
    		System.out.println("Calling "+address.toString());
    	}
    	else if(vm.getStrategy().equals(MonitorConstants.STRATEGY_BLOCK)){
    		if(target.getSetupState().equals(MonitorConstants.STATE_VALID))
    		if(!target.checkHasPast(rootMonitorName, rootId)){//has no past use
    			synchronized(target){
	    			try {
						target.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    			//TODO if updated,resume should wait for finishing of updating for target
				try {
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    }
    return soapConnection.call(soapInput, address);
  }

  public void close() {
    try {
      soapConnection.close();
    }
    catch (SOAPException e) {
      log.warn("could not close soap connection", e);
    }
  }

  static {
    /*
     * Static creation of SAAJ factories is a moot question. Whereas he specification does not
     * indicate their concurrency, typical implementations simply instantiate objects of appropriate
     * concrete class and are totally thread safe.
     */
    try {
      messageFactory = MessageFactory.newInstance();
      soapConnectionFactory = SOAPConnectionFactory.newInstance();
    }
    catch (SOAPException e) {
      // should not happen
      throw new AssertionError(e);
    }
  }

  public String toString() {
    return new ToStringBuilder(this).append("formatter", formatter)
        .append("address", address)
        .toString();
  }
}
