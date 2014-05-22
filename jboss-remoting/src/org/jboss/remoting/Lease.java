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
import org.jboss.remoting.util.TimerUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

/**
 * This class is used on the remoting server to maintain lease information
 * for remoting clients.  Will generate callback to ConnectionListener interface
 * if determined that client no longer available.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Lease
{

   private ConnectionNotifier notifier = null;
   private String clientSessionId = null;
   private long leasePeriod = -1;
   private String locatorURL = null;
   private Map requestPayload = null;
   private LeaseTimerTask leaseTimerTask = null;
   private long leaseWindow = -1;
   private long pingStart = -1;
   private Map clientLeases = null;

   private boolean leaseUpdated = false;

   private static final Logger log = Logger.getLogger(Lease.class);
   private static final boolean isTraceEnabled = log.isTraceEnabled();

   public Lease(String clientSessionId, long leasePeriod, String locatorurl, Map requestPayload,
                ConnectionNotifier notifier, Map clientLeases)
   {
      this.clientSessionId = clientSessionId;
      this.leasePeriod = leasePeriod;
      this.notifier = notifier;
      this.locatorURL = locatorurl;
      if(requestPayload != null)
      {
         this.requestPayload = (Map)requestPayload.get(ClientHolder.CLIENT_HOLDER_KEY);
      }
      this.leaseWindow = leasePeriod * 2;
      this.clientLeases = clientLeases;
   }


   public void startLease()
   {
      if(isTraceEnabled)
      {
         log.trace("Starting lease for client invoker (session id = " + clientSessionId + ") with lease window time of " + leaseWindow);
      }
      leaseTimerTask = new LeaseTimerTask();
      TimerUtil.schedule(leaseTimerTask, leaseWindow);
   }

   public void updateLease(long leasePeriod, Map requestMap)
   {
      if(requestMap != null)
      {
         this.requestPayload = (Map)requestMap.get(ClientHolder.CLIENT_HOLDER_KEY);
      }
      updateLease(leasePeriod);
   }

   public void updateLease(long leasePeriod)
   {
      leaseUpdated = true;
      if (leasePeriod != this.leasePeriod)
      {
         this.leasePeriod = leasePeriod;
         this.leaseWindow = leasePeriod * 2;
         stopLease();
         startLease();
         if(isTraceEnabled)
         {
            log.trace("Lease for client invoker (session id = " + clientSessionId + ") updated with new lease window of " + leaseWindow + ".  Resetting timer.");
         }
      }
      else
      {
         if (pingStart != -1)
         {
            long pingDuration = System.currentTimeMillis() - pingStart;
            if (pingDuration > 0.75 * leaseWindow)
            {
               leaseWindow = pingDuration * 2;

               stopLease();
               leaseTimerTask = new LeaseTimerTask();
               TimerUtil.schedule(leaseTimerTask, leaseWindow);
            }
         }

      }
      pingStart = System.currentTimeMillis();
   }

   public void terminateLease(String sessionId)
   {
      if(isTraceEnabled)
      {
         log.trace("Terminating lease for session id " + sessionId);
      }
      // is this terminate for all clients
      if (clientSessionId.equals(sessionId))
      {
         stopLease();
         // should be ok to call this will null as all the client should have
         // already been disconnected and there been a notification for each
         // of these client disconnections (which would remove the client from
         // the lease, thus leaving the collection empty
         notifyClientTermination(null);
      }
      else
      {
         notifyClientTermination(sessionId);
      }
   }

   private void notifyClientTermination(String sessionId)
   {
      // is for a particular client, so need to inspect request payload for client
      if (requestPayload != null)
      {
         // should notify for one client or all?
         if (sessionId != null)
         {
            Object clientHolderObj = requestPayload.remove(sessionId);
            if (clientHolderObj != null && clientHolderObj instanceof ClientHolder)
            {
               ClientHolder clientHolder = (ClientHolder) clientHolderObj;
               notifier.connectionTerminated(locatorURL, clientHolder.getSessionId(), clientHolder.getConfig());
               if(isTraceEnabled)
               {
                  log.trace("Notified connection listener of lease termination due to disconnect from client (client session id = " + clientHolder.getSessionId());
               }
            }
         }
         else
         {
            // loop through and notify for all clients
            Collection clientHoldersCol = requestPayload.values();
            if (clientHoldersCol != null && clientHoldersCol.size() > 0)
            {
               Iterator itr = clientHoldersCol.iterator();
               while (itr.hasNext())
               {
                  Object val = itr.next();
                  if (val != null && val instanceof ClientHolder)
                  {
                     ClientHolder clientHolder = (ClientHolder) val;
                     notifier.connectionTerminated(locatorURL, clientHolder.getSessionId(), clientHolder.getConfig());
                     if(isTraceEnabled)
                     {
                        log.trace("Notified connection listener of lease termination due to disconnect from client (client session id = " + clientHolder.getSessionId());
                     }
                  }
               }
            }
         }
      }
      else
      {
         log.warn("Tried to terminate lease for session id " + sessionId + ", but no collection of clients have been set.");
      }
   }

   private void notifyClientLost()
   {
      // is not for a particular client (but all clients associated with client invoker), so need to inspect request payload for client
      if (requestPayload != null)
      {
         // loop through and notify for all clients
         Collection clientHoldersCol = requestPayload.values();
         if (clientHoldersCol != null && clientHoldersCol.size() > 0)
         {
            Iterator itr = clientHoldersCol.iterator();
            while (itr.hasNext())
            {
               Object val = itr.next();
               if (val != null && val instanceof ClientHolder)
               {
                  ClientHolder clientHolder = (ClientHolder) val;
                  notifier.connectionLost(locatorURL, clientHolder.getSessionId(), clientHolder.getConfig());
                  if(isTraceEnabled)
                  {
                     log.trace("Notified connection listener of lease expired due to lost connection from client (client session id = " + clientHolder.getSessionId());
                  }
               }
            }
         }
      }
      else
      {
         notifier.connectionTerminated(locatorURL, clientSessionId, null);
      }
   }

   private void stopLease()
   {
      leaseTimerTask.cancel();
   }

   private class LeaseTimerTask extends TimerTask
   {

      /**
       * The action to be performed by this timer task.
       */
      public void run()
      {
         if (leaseUpdated)
         {
            leaseUpdated = false;
         }
         else
         {
            try
            {
               if (log.isTraceEnabled()) log.trace("did not receive ping: " + clientSessionId);
               stopLease();
               notifyClientLost();
               if (clientLeases != null)
               {
                  clientLeases.remove(clientSessionId);
               }
               if (log.isTraceEnabled()) log.trace("removed lease:" + clientSessionId);
            }
            catch (Throwable thr)
            {
               log.error("Error terminating client lease and sending notification of lost client.", thr);
            }
         }
      }
   }
}