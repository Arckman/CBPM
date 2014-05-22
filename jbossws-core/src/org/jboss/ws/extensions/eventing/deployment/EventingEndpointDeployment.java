package org.jboss.ws.extensions.eventing.deployment;

import javax.xml.namespace.QName;

/**
 * Eventsource endpoint deployment info.
 *
 * @author Heiko Braun, <heiko@openj.net>
 * @since 18-Jan-2006
 */
public class EventingEndpointDeployment
{
   /* event source URI */
   private String name;

   private QName portName;

   // event source endpoint address
   private String endpointAddress;

   /* notification schema */
   private String[] schema;

   private String notificationRootElementNS;

   public EventingEndpointDeployment(String name, String[] schema, String notificationRootElementNS)
   {
      this.name = name;
      this.schema = schema;
      this.notificationRootElementNS = notificationRootElementNS;
   }

   public QName getPortName()
   {
      return portName;
   }

   public void setPortName(QName portName)
   {
      this.portName = portName;
   }

   public String getName()
   {
      return name;
   }

   public String[] getSchema()
   {
      return schema;
   }

   public String getEndpointAddress()
   {
      return endpointAddress;
   }

   public void setEndpointAddress(String endpointAddress)
   {
      this.endpointAddress = endpointAddress;
   }

   public String getNotificationRootElementNS()
   {
      return notificationRootElementNS;
   }

   public void setNotificationRootElementNS(String notificationRootElementNS)
   {
      this.notificationRootElementNS = notificationRootElementNS;
   }

}
