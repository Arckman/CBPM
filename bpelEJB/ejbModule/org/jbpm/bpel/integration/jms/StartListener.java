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
package org.jbpm.bpel.integration.jms;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.QueueReceiver;
import javax.jms.Session;
import javax.jms.TopicSubscriber;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.bpel.frj.monitor.InstanceMonitor;
import org.jbpm.bpel.frj.monitor.ProcessMonitor;
import org.jbpm.bpel.frj.util.MonitorConstants;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;
import org.jbpm.bpel.graph.exe.BpelFaultException;
import org.jbpm.bpel.integration.def.ReceiveAction;
import org.jbpm.bpel.persistence.db.BpelGraphSession;
import org.jbpm.bpel.persistence.db.IntegrationSession;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * @author Alejandro Guizar
 * @version $Revision: 1.22 $ $Date: 2008/06/12 08:18:54 $
 */
public class StartListener implements MessageListener {

  private final long processDefinitionId;
  private final long receiveActionId;

  private final IntegrationControl integrationControl;

  private final Session jmsSession;
  private final MessageConsumer messageConsumer;

  private static final Log log = LogFactory.getLog(StartListener.class);

  StartListener(BpelProcessDefinition processDefinition, ReceiveAction receiveAction,
      IntegrationControl integrationControl) throws JMSException {
    // initialize entity identifiers
    processDefinitionId = processDefinition.getId();
    receiveActionId = receiveAction.getId();

    // save integration control
    this.integrationControl = integrationControl;

    jmsSession = integrationControl.createJmsSession();

    // create message consumer
    Destination destination = integrationControl.getPartnerLinkEntry(receiveAction.getPartnerLink())
        .getDestination();
    String selector = formatSelector(receiveAction);
    messageConsumer = jmsSession.createConsumer(destination, selector);

    integrationControl.addStartListener(this);
    log.debug("created start listener: processDefinition="
        + processDefinition
        + ", receiveAction="
        + receiveAction);
  }

  private static String formatSelector(ReceiveAction receiveAction) {
    return IntegrationConstants.PARTNER_LINK_ID_PROP
        + '='
        + receiveAction.getPartnerLink().getId()
        + " AND "
        + IntegrationConstants.OPERATION_NAME_PROP
        + "='"
        + receiveAction.getOperation().getName()
        + '\'';
  }

  StartListener(StartListener other) throws JMSException {
    this.processDefinitionId = other.processDefinitionId;
    this.receiveActionId = other.receiveActionId;

    this.integrationControl = other.integrationControl;

    jmsSession = integrationControl.createJmsSession();
    messageConsumer = jmsSession.createConsumer(getDestination(other.messageConsumer),
        other.messageConsumer.getMessageSelector());

    integrationControl.addStartListener(this);
    log.debug("created start listener: processDefinition="
        + processDefinitionId
        + ", receiveAction="
        + receiveActionId);
  }

  private static Destination getDestination(MessageConsumer messageConsumer) throws JMSException {
    if (messageConsumer instanceof QueueReceiver) {
      QueueReceiver queueReceiver = (QueueReceiver) messageConsumer;
      return queueReceiver.getQueue();
    }
    if (messageConsumer instanceof TopicSubscriber) {
      TopicSubscriber topicSubscriber = (TopicSubscriber) messageConsumer;
      return topicSubscriber.getTopic();
    }
    throw new JMSException("unknown message consumer type: " + messageConsumer.getClass());
  }

  public long getReceiveActionId() {
    return receiveActionId;
  }

  public MessageConsumer getMessageConsumer() {
    return messageConsumer;
  }

  public void open() throws JMSException {
    /*
     * jms may deliver a message immediately after setting the message listener; make sure this
     * listener is fully initialized at this point
     */
    messageConsumer.setMessageListener(this);
    log.debug("opened start listener: processDefinition="
        + processDefinitionId
        + ", receiveAction="
        + receiveActionId);
  }

  public void onMessage(Message message) {
    if (!(message instanceof ObjectMessage)) {
      log.error("received non-object jms message: " + message);
      return;
    }
    try {
      ObjectMessage request = (ObjectMessage) message;
      log.debug("delivering request: " + RequestListener.messageToString(request));

      /*
       * CODE ORDER NOTE. Removing this listener early in the process prevents any other thread from
       * closing it. This effect is desirable because the orderly shutdown mechanism of JMS never
       * stops a running listener anyway. Furthermore, the mechanism is specified to *block* the
       * other thread until this listener returns.
       */
      integrationControl.removeStartListener(this);

      // BPEL-282 create new start listener to improve concurrency
      StartListener startListener = new StartListener(this);
      startListener.open();
      
      deliverRequest((Map) request.getObject(), request.getJMSReplyTo(), request.getJMSMessageID());
      request.acknowledge();

      close();
    }
    catch (JMSException e) {
      log.error("request delivery failed due to jms exception, giving up", e);
    }
    catch (RuntimeException e) {
      if (RequestListener.isRecoverable(e)) {
        log.warn("request delivery failed due to recoverable exception, attempting recovery");
        try {
          // recover the session manually
          jmsSession.recover();
        }
        catch (JMSException je) {
          log.error("request recovery failed, giving up", je);
        }
      }
      else
        log.error("request delivery failed due to non-recoverable exception, giving up", e);
    }
  }

  private void deliverRequest(Map parts, Destination replyTo, String requestId) {
    JbpmContext jbpmContext = integrationControl.getIntegrationServiceFactory()
        .getJbpmConfiguration()
        .createJbpmContext();
    try {
      // load process definition
      BpelGraphSession graphSession = BpelGraphSession.getContextInstance(jbpmContext);
      BpelProcessDefinition processDefinition = graphSession.loadProcessDefinition(processDefinitionId);
      // load receive action
      ReceiveAction receiveAction = IntegrationSession.getContextInstance(jbpmContext)
          .loadReceiveAction(receiveActionId);

      // instantiate the process
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      // XXX root token is not assigned an identifier at creation time
      jbpmContext.getServices().getPersistenceService().assignId(processInstance.getRootToken());

      // build initial runtime structures
      Token receivingToken = receiveAction.initializeProcessInstance(processInstance);

      //==================================================================================================
      //add instance to version control manager
      ProcessMonitor pm=JbpmConfiguration.getVersionControlManager().getProcessMonitor(processDefinition.getName());
      InstanceMonitor im=pm.addInstanceMonitor(processDefinition, processInstance);
      if(parts.get("rootMonitorName")!=null){
    	  im.setRoot(false);
    	  im.setRootMonitorName((String)parts.get("rootMonitorName"));
    	  im.setRootInstanceId(Long.valueOf((String)parts.get("rootId")));
    	  im.setParentMonitorName((String)parts.get("parentMonitorName"));
    	  im.setParentInstanceId(Long.valueOf((String)parts.get("parentId")));
    	  parts.remove("rootMonitorName");
    	  parts.remove("rootId");
    	  parts.remove("parentMonitorName");
    	  parts.remove("parentId");
      }else{
    	  im.setRootMonitorName(processDefinition.getName());
    	  im.setRootInstanceId(processInstance.getId());
      }
//      System.out.println(im.toString());
//      if(pm.getSetupState().equals(MonitorConstants.STATE_ONDEMAND)||pm.getSetupState().equals(MonitorConstants.STATE_SETUP)){
//    	  //setting up,need suspend
//    	  if(pm.isSuspend()){
//    		  synchronized(im){
//    			  try {
//					im.wait();
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//    		  }
//    	  }
//      }
      if(pm.getSetupState().equals(MonitorConstants.STATE_VALID)){// valid state,create future path
    	  im.newTXInit();
      }
      
      try {
        // file outstanding request, in case operation has output
        if (receiveAction.getOperation().getOutput() != null) {
          // encapsulate the fields needed to reply
          OutstandingRequest outRequest = new OutstandingRequest(replyTo, requestId);
          // register the request in the integration control
          integrationControl.addOutstandingRequest(receiveAction, receivingToken, outRequest);
        }

        // pass control to start activity
        receiveAction.deliverMessage(receivingToken, parts);
      }
      catch (BpelFaultException e) {
        log.debug("request delivery caused a fault", e);
        processDefinition.getGlobalScope().raiseException(e, new ExecutionContext(receivingToken));
      }
      // save changes to instance
      jbpmContext.save(processInstance);
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();
      throw e;
    }
    finally {
      // end transaction, close all services
      jbpmContext.close();
    }
  }

  public void close() throws JMSException {
    messageConsumer.close();
    jmsSession.close();
    log.debug("closed start listener: processDefinition="
        + processDefinitionId
        + ", receiveAction="
        + receiveActionId);
  }

  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    try {
      QueueReceiver queueReceiver = (QueueReceiver) messageConsumer;
      builder.append("queue", queueReceiver.getQueue()).append("selector",
          queueReceiver.getMessageSelector());
    }
    catch (JMSException e) {
      log.debug("could not fill request listener fields", e);
    }
    return builder.toString();
  }
}
