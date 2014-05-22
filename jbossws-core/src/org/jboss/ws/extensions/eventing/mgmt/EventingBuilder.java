package org.jboss.ws.extensions.eventing.mgmt;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.ws.WSException;
import org.jboss.ws.extensions.eventing.EventingConstants;
import org.jboss.ws.extensions.eventing.deployment.EventingEndpointDeployment;

/**
 * @author Heiko Braun, <heiko@openj.net>
 * @since 24-Jan-2006
 */
public class EventingBuilder
{

   private EventingBuilder()
   {
   }

   public static EventingBuilder createEventingBuilder()
   {
      return new EventingBuilder();
   }

   public EventSource newEventSource(EventingEndpointDeployment desc)
   {
      URI eventSourceNS = newEventSourceURI(desc.getName());
      EventSource eventSource = new EventSource(desc.getName(), eventSourceNS, desc.getSchema(), desc.getNotificationRootElementNS());
      eventSource.getSupportedFilterDialects().add(EventingConstants.getDefaultFilterDialect());
      return eventSource;
   }

   public URI newEventSourceURI(String name)
   {
      try
      {
         return new URI(name);
      }
      catch (URISyntaxException e)
      {
         throw new WSException("Failed to create eventsource URI: " + e.getMessage());
      }
   }

}
