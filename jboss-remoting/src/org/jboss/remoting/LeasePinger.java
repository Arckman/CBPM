package org.jboss.remoting;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.ClientInvoker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * Internal agent class to ping the remote server to keep lease alive.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@ejboss.org">Ovidiu Feodorov</a>
 */
public class LeasePinger
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(LeasePinger.class);

   public static final long DEFAULT_LEASE_PERIOD = 5000;
   public static final int DEFAULT_DISCONNECT_TIMEOUT = -1;

   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   private static Timer timer = new Timer(true);

   // Attributes -----------------------------------------------------------------------------------

   private long defaultPingPeriod = -1;

   private ClientInvoker invoker = null;
   private String invokerSessionID = null;

   private Map clients = new ConcurrentHashMap();
   private TimerTask timerTask = null;

   private long pingPeriod = -1;
   private int disconnectTimeout = DEFAULT_DISCONNECT_TIMEOUT;

   // Constructors ---------------------------------------------------------------------------------

   public LeasePinger(ClientInvoker invoker, String invokerSessionID, long defaultLeasePeriod)
   {
      this.invoker = invoker;
      this.invokerSessionID = invokerSessionID;
      this.pingPeriod = defaultLeasePeriod;
      this.defaultPingPeriod = defaultLeasePeriod;
   }

   // Public ---------------------------------------------------------------------------------------

   public void startPing()
   {
      if(trace) { log.trace(this + " starting lease timer with ping period of " + pingPeriod); }

      timerTask = new LeaseTimerTask(this);
      timer.schedule(timerTask, pingPeriod, pingPeriod);
   }

   public void stopPing()
   {
      if(trace) { log.trace(this + " stopping lease timer"); }

      if (timerTask != null)
      {
         timerTask.cancel();
         timerTask = null;
         
         try
         {
            // sending request map with no ClientHolders will indicate to server
            // that is full disconnect (for client invoker)
            HashMap metadata = null;
            
            // If disconnectTimeout == 0, skip network i/o.
            if (disconnectTimeout != 0)
            {
               if (disconnectTimeout > 0)
               {
                  metadata = new HashMap(1);
                  metadata.put(ServerInvoker.TIMEOUT, Integer.toString(disconnectTimeout));
               }
               InvocationRequest ir =
                  new InvocationRequest(invokerSessionID, null, "$DISCONNECT$", metadata, null, null);
               invoker.invoke(ir);
            }
         }
         catch (Throwable throwable)
         {
            RuntimeException e = new RuntimeException("Error tearing down lease with server.");
            e.initCause(throwable);
            throw e;
         }
      }
   }

   public void addClient(String sessionID, Map configuration, long leasePeriod)
   {
      if (leasePeriod <= 0)
      {
         leasePeriod = defaultPingPeriod;
      }

      if(trace) { log.trace(this + " adding new client with session ID " + sessionID + " and lease period " + leasePeriod); }

      ClientHolder newClient = new ClientHolder(sessionID, configuration, leasePeriod);
      clients.put(sessionID, newClient);

      sendClientPing();

      // if new client lease period is less than the current ping period, need to refresh to new one
      if (leasePeriod < pingPeriod)
      {
         pingPeriod = leasePeriod;

         // don't want to call stopPing() as that will send disconnect for client invoker
         if (timerTask != null)
         {
            timerTask.cancel();
            timerTask = null;
            startPing();
         }
      }
   }

   public boolean removeClient(String sessionID)
   {
      boolean isLastClientLease = false;

      if(trace) { log.trace(this + " removing client with session ID " + sessionID); }

      ClientHolder holder = (ClientHolder)clients.remove(sessionID);
      
      if (holder != null)
      {
         // send disconnect for this client
         try
         {
            Map clientMap = new HashMap();
            clientMap.put(ClientHolder.CLIENT_HOLDER_KEY, holder);
            
            // If disconnectTimeout == 0, skip network i/o.
            if (disconnectTimeout != 0)
            {
               if (disconnectTimeout > 0)
                  clientMap.put(ServerInvoker.TIMEOUT, Integer.toString(disconnectTimeout));
               
               InvocationRequest ir = new InvocationRequest(invokerSessionID, null, "$DISCONNECT$",
                     clientMap, null, null);
               invoker.invoke(ir);
               
               if(trace) { log.trace(this + " sent out disconnect message to server for lease tied to client with session ID " + sessionID); }
            }
         }
         catch (Throwable throwable)
         {
            log.warn(this + " failed sending disconnect for client lease for " +
                  "client with session ID " + sessionID);
         }
      }
      else
      {
         log.warn(this + " tried to remove lease for client with session ID " + sessionID +
         ", but no such lease was found");
      }
      
      if (clients.isEmpty())
      {
         isLastClientLease = true;
         if(trace) { log.trace(this + " has no more client leases"); }
      }
      else
      {
         // now need to see if any of the other client holders have a lower lease period than
         // default

         long tempPingPeriod = defaultPingPeriod;

         for (Iterator i = clients.values().iterator(); i.hasNext(); )
         {
            ClientHolder clientHolder = (ClientHolder)i.next();
            long clientHolderLeasePeriod = clientHolder.getLeasePeriod();
            if (clientHolderLeasePeriod > 0 && clientHolderLeasePeriod < tempPingPeriod)
            {
               tempPingPeriod = clientHolderLeasePeriod;
            }
         }

         // was there a change in lease period?
         if (tempPingPeriod != pingPeriod)
         {
            // need to update to new ping period and reset timer
            pingPeriod = tempPingPeriod;

            if (timerTask != null)
            {
               timerTask.cancel();
               timerTask = null;
            }
            startPing();
         }

      }
      return isLastClientLease;
   }

   public long getLeasePeriod(String sessionID)
   {
      if (timerTask == null)
      {
         return -1;
      }

      // look to see if the client is still amont those serviced by this lease pinger
      if (clients.containsKey(sessionID))
      {
         return pingPeriod;
      }
      else
      {
         return -1;
      }
   }

   public String toString()
   {
      return "LeasePinger[" + invoker + "(" + invokerSessionID + ")]";
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   
   protected int getDisconnectTimeout()
   {
      return disconnectTimeout;
   }
   
   protected void setDisconnectTimeout(int disconnectTimeout)
   {
      this.disconnectTimeout = disconnectTimeout;
      log.debug(this + " setting disconnect timeout to: " + disconnectTimeout);
   }
   
   // Private --------------------------------------------------------------------------------------

   private void sendClientPing()
   {
      try
      {
         if(trace)
         {
            StringBuffer sb = new StringBuffer();
            if(clients != null)
            {
               for(Iterator i = clients.values().iterator(); i.hasNext(); )
               {
                  ClientHolder h = (ClientHolder)i.next();
                  sb.append("    ").append(h.getSessionId()).append('\n');
               }
            }

            log.trace(this + " sending ping to server. Currently managing lease " +
               "for following clients:\n" + sb.toString());
         }

         Map clientsClone = new ConcurrentHashMap(clients);
         Map requestClients = new ConcurrentHashMap();
         requestClients.put(ClientHolder.CLIENT_HOLDER_KEY, clientsClone);

         InvocationRequest ir =
            new InvocationRequest(invokerSessionID, null, "$PING$", requestClients, null, null);

         invoker.invoke(ir);

         if(trace) { log.trace(this + " successfully pinged the server"); }
      }
      catch (Throwable t)
      {
         log.debug(this + " failed to ping to server", t);
         log.warn(this + " failed to ping to server: " + t.getMessage());
      }
   }

   // Inner classes --------------------------------------------------------------------------------

   static private class LeaseTimerTask extends TimerTask
   {
      private LeasePinger pinger;

      LeaseTimerTask(final LeasePinger pinger)
      {
          this.pinger = pinger;
      }

      public void run()
      {
         final LeasePinger currentPinger;
         synchronized(this)
         {
             currentPinger = pinger;
         }

         if (currentPinger != null)
         {
             currentPinger.sendClientPing();
         }
      }

      public boolean cancel()
      {
          synchronized(this)
          {
              pinger = null;
          }
          return super.cancel();
      }
   }
}
