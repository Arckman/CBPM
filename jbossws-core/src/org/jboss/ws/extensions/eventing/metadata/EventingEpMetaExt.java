package org.jboss.ws.extensions.eventing.metadata;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.ws.metadata.umdm.MetaDataExtension;

/**
 * Eventing specific endpoint meta data extensions.
 *
 * @author Heiko Braun, <heiko@openj.net>
 * @since 21-Mar-2006
 */
public class EventingEpMetaExt extends MetaDataExtension {

   private boolean isEventSource = true;
   private String eventSourceNS;

   private String[] notificationSchema;
   private String notificationRootElementNS;

   public EventingEpMetaExt(String extensionNameSpace) {
      super(extensionNameSpace);
   }

   public boolean isEventSource() {
      return isEventSource;
   }

   public void setEventSource(boolean eventSource) {
      isEventSource = eventSource;
   }

   public String getEventSourceNS() {
      return eventSourceNS;
   }

   public void setEventSourceNS(String eventSourceNS) {
      this.eventSourceNS = eventSourceNS;
   }

   public URI getEventSourceURI()
   {
      try
      {
         return new URI(eventSourceNS);
      }
      catch (URISyntaxException e)
      {
         throw new IllegalArgumentException("Illegal event source URI: " + eventSourceNS);
      }
   }

   public String[] getNotificationSchema() {
      return this.notificationSchema;
   }

   public void setNotificationSchema(String[] notificationSchema) {
      this.notificationSchema = notificationSchema;
   }

   public String getNotificationRootElementNS() {
      return notificationRootElementNS;
   }

   public void setNotificationRootElementNS(String notificationRootElementNS) {
      this.notificationRootElementNS = notificationRootElementNS;
   }
}
