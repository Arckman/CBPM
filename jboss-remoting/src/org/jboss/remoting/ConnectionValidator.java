/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.jboss.remoting;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.util.StoppableTimerTask;
import org.jboss.remoting.util.TimerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimerTask;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 */
public class ConnectionValidator extends TimerTask implements StoppableTimerTask
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(ConnectionValidator.class.getName());

   /** Configuration map key for ping period. */
   public static final String VALIDATOR_PING_PERIOD = "validatorPingPeriod";
   
   /** Default ping period. Value is 2 seconds. */
   public static final long DEFAULT_PING_PERIOD = 2000;
   
   /** Configuration map key for ping timeout. */
   public static final String VALIDATOR_PING_TIMEOUT = "validatorPingTimeout";
   
   /** Default ping timeout period.  Value is 1 second. */
   public static final String DEFAULT_PING_TIMEOUT = "1000";
   
   /**
    * Default number of ping retries.  Value is 1.
    * Currently implemented only on socket transport family.
    */
   public static final String DEFAULT_NUMBER_OF_PING_RETRIES = "1";
   
   /**
    * Default number of connection acquisition retries.  Value is 1.
    * Currently implemented only on socket transport family.
    */
   public static final String DEFAULT_NUMBER_OF_CONNECTION_RETRIES = "1";

   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   /**
    * Will make $PING$ invocation on server. If sucessful, will return true. Otherwise, will throw
    * an exception.
    *
    * @param locator - locator for the server to ping
    * @param config  - any configuration needed for server
    * @return true if alive, false if not
    */
   public static boolean checkConnection(InvokerLocator locator, Map config) throws Throwable
   {
      boolean pingWorked = false;
      Map configMap = createPingConfig(config, null);
      ClientInvoker innerClientInvoker = null;

      try
      {
         innerClientInvoker = InvokerRegistry.createClientInvoker(locator, configMap);

         if (!innerClientInvoker.isConnected())
         {
            if (trace) { log.trace("inner client invoker not connected, connecting ..."); }
            innerClientInvoker.connect();
         }

         pingWorked = doCheckConnection(innerClientInvoker);
      }
      catch (Throwable throwable)
      {
         log.debug("ConnectionValidator to connect to server " +
            innerClientInvoker.getLocator().getProtocol() + "://" +
            innerClientInvoker.getLocator().getHost() + ":" +
            innerClientInvoker.getLocator().getPort(), throwable);
      }
      finally
      {
         if (innerClientInvoker != null)
         {
            InvokerRegistry.destroyClientInvoker(locator, configMap);
         }
      }

      return pingWorked;
   }

   private static boolean doCheckConnection(ClientInvoker clientInvoker) throws Throwable
   {
      boolean pingWorked = false;

      try
      {
         // Sending null client id as don't want to trigger lease on server side. This also means
         // that client connection validator will NOT impact client lease, so can not depend on it
         // to maintain client lease with the server.
         InvocationRequest ir =
            new InvocationRequest(null, Subsystem.SELF, "$PING$", null, null, null);

         if (trace) { log.trace("pinging, sending " + ir + " over " + clientInvoker); }

         clientInvoker.invoke(ir);

         if (trace) { log.trace("ConnectionValidator got successful ping using " + clientInvoker);}

         pingWorked = true;
      }
      catch (Throwable t)
      {
         log.debug("ConnectionValidator failed to ping via " + clientInvoker, t);
      }

      return pingWorked;
   }
   
   private static Map createPingConfig(Map config, Map metadata)
   {
      Map localConfig = new HashMap();
      localConfig.put("connection_checker", "true");

      if (config != null)
      {
         Object o = config.get(VALIDATOR_PING_TIMEOUT);
         log.trace("config timeout: " + o);
         if (o != null)
            localConfig.put(ServerInvoker.TIMEOUT, o);
         
         o = config.get("NumberOfCallRetries");
         if (o != null)
            localConfig.put("NumberOfCallRetries", o);
         
         o = config.get("NumberOfRetries");
         if (o != null)
            localConfig.put("NumberOfRetries", o);
      }
      
      if (metadata != null)
      {
         metadata.remove(ServerInvoker.TIMEOUT);
         localConfig.putAll(metadata);
         Object o = metadata.get(VALIDATOR_PING_TIMEOUT);
         if (o != null)
            localConfig.put(ServerInvoker.TIMEOUT, o);
      }
      
      if (localConfig.get(ServerInvoker.TIMEOUT) == null)
         localConfig.put(ServerInvoker.TIMEOUT, DEFAULT_PING_TIMEOUT);
      
      if (localConfig.get("NumberOfCallRetries") == null)
         localConfig.put("NumberOfCallRetries", DEFAULT_NUMBER_OF_PING_RETRIES);
      
      if (localConfig.get("NumberOfRetries") == null)
         localConfig.put("NumberOfRetries", DEFAULT_NUMBER_OF_CONNECTION_RETRIES);
      
      return localConfig;
   }

   // Attributes -----------------------------------------------------------------------------------

   private Client client;
   private long pingPeriod;
   private Map metadata;
   private InvokerLocator locator;
   private Map configMap;
   private List listeners;
   private ClientInvoker clientInvoker;
   private Object lock = new Object();
   private volatile boolean stopped;

   // Constructors ---------------------------------------------------------------------------------

   public ConnectionValidator(Client client)
   {
      this(client, DEFAULT_PING_PERIOD);
   }

   public ConnectionValidator(Client client, long pingPeriod)
   {
      this.client = client;
      this.pingPeriod = pingPeriod;
      this.listeners = new ArrayList();
      this.stopped = false;

      log.debug(this + " created");
   }
   
   public ConnectionValidator(Client client, Map metadata)
   {
      this.client = client;
      this.pingPeriod = DEFAULT_PING_PERIOD;
      this.listeners = new ArrayList();
      this.stopped = false;
      
      Map config = client.getConfiguration();
      if (config != null)
      {  
         Object o = config.get(VALIDATOR_PING_PERIOD);
         if (o != null)
         {
            if (o instanceof String)
            {
               try 
               {
                  pingPeriod = Long.parseLong((String)o);
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + VALIDATOR_PING_PERIOD +
                           " value of " + o + " to a long value");
               }
            }
            else
            {
               log.warn(this + " could not convert " + VALIDATOR_PING_PERIOD +
                        " value of " + o + " to a long value: must be a String");
            }
         }
      }

      if (metadata != null)
      {
         this.metadata = new HashMap(metadata);

         Object o = metadata.get(VALIDATOR_PING_PERIOD);
         if (o != null)
         {
            if (o instanceof String)
            {
               try 
               {
                  pingPeriod = Long.parseLong((String)o);
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + VALIDATOR_PING_PERIOD +
                           " value of " + o + " to a long value");
               }
            }
            else
            {
               log.warn(this + " could not convert " + VALIDATOR_PING_PERIOD +
                        " value of " + o + " to a long value: must be a String");
            }
         }
      }

      log.debug(this + " created");
   }

   // StoppableTimerTask implementation ------------------------------------------------------------

   public void stop()
   {
      if (stopped)
      {
         return;
      }

      doStop();
   }

   // TimerTask overrides --------------------------------------------------------------------------

   /**
    * The action to be performed by this timer task.
    */
   public void run()
   {
      synchronized(lock)
      {
         if(!stopped)
         {
            try
            {
               if (trace) { log.trace(this + " pinging ..."); }

               boolean isValid = doCheckConnection(clientInvoker);

               if (!isValid)
               {
                  log.debug(this + "'s connections is invalid");

                  notifyListeners(new Exception("Could not connect to server!"));
               }
            }
            catch (Throwable thr)
            {
               log.debug(this + " got throwable while pinging", thr);
               notifyListeners(thr);
            }
         }
      }
   }

   public boolean cancel()
   {
      return doStop();
   }

   // Public ---------------------------------------------------------------------------------------

   public void addConnectionListener(ConnectionListener listener)
   {
      if (listener != null)
      {
         synchronized (listeners)
         {
            if (listeners.size() == 0)
            {
               start();
            }
            listeners.add(listener);
         }
      }
   }

   public boolean removeConnectionListener(ConnectionListener listener)
   {
      boolean isRemoved = false;
      if (listener != null)
      {
         synchronized (listeners)
         {
            isRemoved = listeners.remove(listener);
            if (listeners.size() == 0)
            {
               stop();
            }
         }
      }
      return isRemoved;
   }

   public long getPingPeriod()
   {
      if (stopped)
      {
         return -1;
      }

      return pingPeriod;
   }

   public String toString()
   {
      return "ConnectionValidator[" + clientInvoker + ", pingPeriod=" + pingPeriod + " ms]";
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   private void start()
   {
      configMap = createPingConfig(client.getConfiguration(), metadata);
      log.debug(this + " timeout: " + configMap.get(ServerInvoker.TIMEOUT));
      log.debug(this + " ping retries: " + configMap.get("NumberOfCallRetries"));
      log.debug(this + " connection retries: " + configMap.get("NumberOfRetries"));
      locator = client.getInvoker().getLocator();

      try
      {
         clientInvoker = InvokerRegistry.createClientInvoker(locator, configMap);
      }
      catch (Exception e)
      {
         log.error("Unable to create client invoker for locator: " + locator);
         throw new RuntimeException("Unable to create client invoker for locator: " + locator, e);
      }

      if (!clientInvoker.isConnected())
      {
         if (trace) { log.trace("inner client invoker not connected, connecting ..."); }
         clientInvoker.connect();
      }

      TimerUtil.schedule(this, pingPeriod);
      stopped = false;

      log.debug(this + " started");
   }

   private boolean doStop()
   {
      synchronized(lock)
      {
         if (!listeners.isEmpty())
         {
            listeners.clear();
         }
         stopped = true;
      }

      if (clientInvoker != null)
      {
         InvokerRegistry.destroyClientInvoker(locator, configMap);
      }

      TimerUtil.unschedule(this);

      boolean result = super.cancel();
      log.debug(this + " stopped, returning " + result);
      return result;
   }

   private void notifyListeners(Throwable thr)
   {
      final Throwable t = thr;
      synchronized (listeners)
      {
         ListIterator itr = listeners.listIterator();
         while (itr.hasNext())
         {
            final ConnectionListener listener = (ConnectionListener) itr.next();
            new Thread()
            {
               public void run()
               {
                  listener.handleConnectionException(t, client);
               }
            }.start();
         }
      }
      stop();
      listeners.clear();
   }

   // Inner classes --------------------------------------------------------------------------------

}