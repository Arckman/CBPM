package org.jboss.remoting;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * This class is used on the server side to notify any connection listeners when a client connection
 * has been terminated (either by loss of lease or by normal disconnect).
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class ConnectionNotifier
{
   private List listeners = new ArrayList();

   private static final Logger log = Logger.getLogger(ConnectionNotifier.class);

   public void addListener(ConnectionListener listener)
   {
      listeners.add(listener);
   }

   public void removeListener(ConnectionListener listener)
   {
      listeners.remove(listener);
   }

   public int size()
   {
      return listeners.size();
   }

   public void connectionLost(String locatorurl, String clientSessionId, Map requestPayload)
   {
      try
      {
         log.debug("Server connection lost to client (session id = " + clientSessionId);
         Client client = new Client(new InvokerLocator(locatorurl), requestPayload);
         client.setSessionId(clientSessionId);
         ConnectionListener[] list = (ConnectionListener[])listeners.toArray(new ConnectionListener[listeners.size()]);
         for(int x = 0; x < list.length; x++)
         {
            list[x].handleConnectionException(null, client);
         }
      }
      catch(Exception e)
      {
         log.error("Error notifying connection listeners of lost client connection.", e);
      }
   }

   public void connectionTerminated(String locatorURL, String clientSessionId, Map requestPayload)
   {
      try
      {
         if(log.isTraceEnabled())
         {
            log.trace("Client disconnected (session id = " + clientSessionId);
         }
         Client client = new Client(new InvokerLocator(locatorURL), requestPayload);
         client.setSessionId(clientSessionId);
         ClientDisconnectedException ex = new ClientDisconnectedException();

         ConnectionListener[] list = (ConnectionListener[])listeners.toArray(new ConnectionListener[listeners.size()]);
         for(int x = 0; x < list.length; x++)
         {
            list[x].handleConnectionException(ex, client);
         }
      }
      catch(Exception e)
      {
         log.error("Error notifying connection listeners of disconnected client connection.", e);
      }
   }
}
