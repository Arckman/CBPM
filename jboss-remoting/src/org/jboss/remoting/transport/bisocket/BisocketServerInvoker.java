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

package org.jboss.remoting.transport.bisocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.invocation.InternalInvocation;
import org.jboss.remoting.socketfactory.CreationListenerServerSocket;
import org.jboss.remoting.socketfactory.CreationListenerSocketFactory;
import org.jboss.remoting.socketfactory.SocketCreationListener;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.logging.Logger;


/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1.2.17.2.8 $
 * <p>
 * Copyright Nov 23, 2006
 * </p>
 */
public class BisocketServerInvoker extends SocketServerInvoker
{
   private static final Logger log = Logger.getLogger(BisocketServerInvoker.class);

   private static Map listenerIdToServerInvokerMap = Collections.synchronizedMap(new HashMap());
   private static Timer timer;
   private static Object timerLock = new Object();

   private Map listenerIdToInvokerLocatorMap = Collections.synchronizedMap(new HashMap());
   private ServerSocket secondaryServerSocket;
   private InvokerLocator secondaryLocator;
   private SecondaryServerSocketThread secondaryServerSocketThread;
   private Map controlConnectionThreadMap = new HashMap();
   private Map controlConnectionRestartsMap = Collections.synchronizedMap(new HashMap());
   private int pingFrequency = Bisocket.PING_FREQUENCY_DEFAULT;
   private int pingWindowFactor = Bisocket.PING_WINDOW_FACTOR_DEFAULT;
   private int pingWindow = pingWindowFactor * pingFrequency;
   private int socketCreationRetries = Bisocket.MAX_RETRIES_DEFAULT;
   private int controlConnectionRestarts = Bisocket.MAX_CONTROL_CONNECTION_RESTARTS_DEFAULT;
   private ControlMonitorTimerTask controlMonitorTimerTask;
   protected boolean isCallbackServer = false;
   protected int secondaryBindPort = -1;
   protected int secondaryConnectPort = -1;


   public static BisocketServerInvoker getBisocketServerInvoker(String listenerId)
   {
      return (BisocketServerInvoker) listenerIdToServerInvokerMap.get(listenerId);
   }


   public BisocketServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }


   public BisocketServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }


   public void start() throws IOException
   {
      if (isCallbackServer)
      {
         Object val = configuration.get(Bisocket.MAX_RETRIES);
         if (val != null)
         {
            try
            {
               int nVal = Integer.valueOf((String) val).intValue();
               socketCreationRetries = nVal;
               log.debug("Setting socket creation retry limit: " + socketCreationRetries);
            }
            catch (Exception e)
            {
               log.warn("Could not convert " + Bisocket.MAX_RETRIES +
                     " value of " + val + " to an int value.");
            }
         }
         
         val = configuration.get(Bisocket.MAX_CONTROL_CONNECTION_RESTARTS);
         if (val != null)
         {
            try
            {
               int nVal = Integer.valueOf((String) val).intValue();
               controlConnectionRestarts = nVal;
               log.debug("Setting control connection restart limit: " + controlConnectionRestarts);
            }
            catch (Exception e)
            {
               log.warn("Could not convert " + Bisocket.MAX_CONTROL_CONNECTION_RESTARTS +
                     " value of " + val + " to an int value.");
            }
         }
         
         if(maxPoolSize <= 0)
         {
            maxPoolSize = MAX_POOL_SIZE_DEFAULT;
         }
         clientpool = new LRUPool(2, maxPoolSize);
         clientpool.create();
         threadpool = new LinkedList();
         checkSocketFactoryWrapper();

         controlMonitorTimerTask = new ControlMonitorTimerTask(this);
         synchronized (timerLock)
         {
            if (timer == null)
            {
               timer = new Timer(true);
            }
            try
            {
               timer.schedule(controlMonitorTimerTask, pingFrequency, pingFrequency);
            }
            catch (IllegalStateException e)
            {
               log.debug("Unable to schedule TimerTask on existing Timer", e);
               timer = new Timer(true);
               timer.schedule(controlMonitorTimerTask, pingFrequency, pingFrequency);
            }
         }
         
         running = true;
         started = true;
      }
      else
      {
         super.start();
         InetAddress host = getServerSocket().getInetAddress();
         if (secondaryBindPort < 0)
         {
            secondaryBindPort = PortUtil.findFreePort(host.getHostAddress());
         }
         if (serverSocketFactory != null)
         {
            secondaryServerSocket = serverSocketFactory.createServerSocket(secondaryBindPort, 0, host);
         }
         else
         {
            secondaryServerSocket = new ServerSocket(secondaryBindPort, 0, host);
         }
         checkSecondaryServerSocketWrapper();
         
         String connectAddress = getLocator().getHost();
         int connectPort = secondaryConnectPort > 0 ? secondaryConnectPort : secondaryBindPort;
         secondaryLocator = new InvokerLocator(null, connectAddress, connectPort, null, null);
         secondaryServerSocketThread = new SecondaryServerSocketThread(secondaryServerSocket);
         secondaryServerSocketThread.setName("secondaryServerSocketThread");
         secondaryServerSocketThread.setDaemon(true);
         secondaryServerSocketThread.start();
         log.debug("started secondary port: " + host + ":" + secondaryBindPort);
      }
   }


   public boolean isTransportBiDirectional()
   {
      return true;
   }


   public void createControlConnection(String listenerId, boolean firstConnection)
   throws IOException
   {
      BisocketClientInvoker clientInvoker = BisocketClientInvoker.getBisocketClientInvoker(listenerId);
      
      if (clientInvoker == null)
      {
         log.debug("Unable to retrieve client invoker: must have disconnected");
         throw new ClientUnavailableException();
      }
      
      InvokerLocator oldLocator = (InvokerLocator) listenerIdToInvokerLocatorMap.get(listenerId);
      InvokerLocator newLocator = null;
      
      try
      {
         newLocator = clientInvoker.getSecondaryLocator();
      }
      catch (Throwable t)
      {
         log.error("unable to get secondary locator", t);
         throw new IOException("unable to get secondary locator: " + t.getMessage());
      }
      

      // If a server restarts, it is likely that it creates a new secondary server socket on
      // a different port.  It will possible to recreate the control connection, but if
      // there is no PingTimerTask running in the new server to keep it alive, it will just
      // die again.  Once a new secondary server socket address is detected, a count is kept
      // of the number of times the control connection is restarted, and when it hits a
      // configured maximum, it is allowed to die.  See JBREM-731.
      
      boolean locatorChanged = !newLocator.equals(oldLocator);
      listenerIdToInvokerLocatorMap.put(listenerId, newLocator);
      log.debug("creating control connection: " + newLocator);

      Socket socket = null;
      IOException savedException = null;
      
      for (int i = 0; i < socketCreationRetries; i++)
      {
         try
         {
            if (socketFactory != null)
               socket = socketFactory.createSocket(newLocator.getHost(), newLocator.getPort());
            else
               socket = new Socket(newLocator.getHost(), newLocator.getPort());
         }
         catch (IOException e)
         {
            log.debug("Error creating a control socket", e);
            savedException = e;
         }
         
         if (socket != null)
            break;
         
         try
         {
            Thread.sleep(1000);
         }
         catch (InterruptedException e)
         {
            log.debug("received interrupt");
         }
      }

      if (socket == null)
      {
         log.error("unable to create control connection after "
                   + socketCreationRetries + " retries", savedException);
         throw savedException;
      }
      
      DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
      if (firstConnection)
      {
         dos.write(Bisocket.CREATE_CONTROL_SOCKET);
      }
      else
      {
         dos.write(Bisocket.RECREATE_CONTROL_SOCKET);
      }
      dos.writeUTF(listenerId);
      
      Thread thread = new ControlConnectionThread(socket, listenerId);
      thread.setName("control: " + socket.toString());
      thread.setDaemon(true);

      synchronized (controlConnectionThreadMap)
      {
         controlConnectionThreadMap.put(listenerId, thread);
      }
      
      Object o = controlConnectionRestartsMap.get(listenerId);
      if (o != null)
      {
         int restarts = ((Integer) o).intValue();
         if (locatorChanged || restarts > 0)
         controlConnectionRestartsMap.put(listenerId, new Integer(++restarts));
      }
      else
      {
         controlConnectionRestartsMap.put(listenerId, new Integer(0));
      }

      thread.start();
      log.debug(this + " created control connection (" + listenerId + "): " + socket.toString());
   }


   public void destroyControlConnection(String listenerId)
   {
      Thread t = null;
      
      synchronized (controlConnectionThreadMap)
      {
         t = (Thread) controlConnectionThreadMap.remove(listenerId);
      }
      
      if (t != null)
      {
         ((ControlConnectionThread)t).shutdown();
         log.debug(this + " shutting down control connection: " + listenerId);
      }
      else
      {
         log.debug("unrecognized listener ID: " + listenerId);
      }
      
      listenerIdToInvokerLocatorMap.remove(listenerId);
      controlConnectionRestartsMap.remove(listenerId);
   }
   
   
   public int getControlConnectionRestarts()
   {
      return controlConnectionRestarts;
   }
   
   
   public void setControlConnectionRestarts(int controlConnectionRestarts)
   {
      this.controlConnectionRestarts = controlConnectionRestarts;
   }
   
   
   public int getPingFrequency()
   {
      return pingFrequency;
   }


   public void setPingFrequency(int pingFrequency)
   {
      this.pingFrequency = pingFrequency;
      pingWindow = pingWindowFactor * pingFrequency;
   }
   
   
   public int getPingWindowFactor()
   {
      return pingWindowFactor;
   }
   
   
   public void setPingWindowFactor(int pingWindowFactor)
   {
      this.pingWindowFactor = pingWindowFactor;
      pingWindow = pingWindowFactor * pingFrequency;
   }
   
   
   public int getSecondaryBindPort()
   {
      return secondaryBindPort;
   }
   
   
   public void setSecondaryBindPort(int secondaryPort)
   {
      this.secondaryBindPort = secondaryPort;
   }
   
   
   public int getSecondaryConnectPort()
   {
      return secondaryConnectPort;
   }
   
   
   public void setSecondaryConnectPort(int secondaryConnectPort)
   {
      this.secondaryConnectPort = secondaryConnectPort;
   }
   
   
   public int getSocketCreationRetries()
   {
      return socketCreationRetries;
   }
   
   
   public void setSocketCreationRetries(int socketCreationRetries)
   {
      this.socketCreationRetries = socketCreationRetries;
   }
   
   
   protected void setup() throws Exception
   {
      Object o = configuration.get(Bisocket.IS_CALLBACK_SERVER);

      if (o != null)
      {
         if (o instanceof String)
            isCallbackServer = Boolean.valueOf((String) o).booleanValue();
         else if (o instanceof Boolean)
            isCallbackServer = ((Boolean) o).booleanValue();
         else
            log.error("unrecognized value for configuration key \"" +
                  Bisocket.IS_CALLBACK_SERVER + "\": " + o);
      }
      
      o = configuration.get(Bisocket.PING_FREQUENCY);
      if (o instanceof String && ((String) o).length() > 0)
      {
            try
            {
               pingFrequency = Integer.valueOf(((String) o)).intValue();
               log.debug(this + " setting pingFrequency to " + pingFrequency);
            }
            catch (NumberFormatException e)
            {
               log.warn("Invalid format for " + "\"" + Bisocket.PING_FREQUENCY + "\": " + o);
            }
      }
      else if (o != null)
      {
         log.warn("\"" + Bisocket.PING_FREQUENCY + "\" must be specified as a String");
      }
      
      o = configuration.get(Bisocket.PING_WINDOW_FACTOR);
      if (o instanceof String && ((String) o).length() > 0)
      {
            try
            {
               pingWindowFactor = Integer.valueOf(((String) o)).intValue();
               log.debug(this + " setting pingWindowFactor to " + pingWindowFactor);
            }
            catch (NumberFormatException e)
            {
               log.warn("Invalid format for " + "\"" + Bisocket.PING_WINDOW_FACTOR + "\": " + o);
            }
      }
      else if (o != null)
      {
         log.warn("\"" + Bisocket.PING_WINDOW_FACTOR + "\" must be specified as a String");
      }
      
      pingWindow = pingWindowFactor * pingFrequency;
      
      o = configuration.get(Bisocket.SECONDARY_BIND_PORT);
      if (o instanceof String && ((String) o).length() > 0)
      {
            try
            {
               secondaryBindPort = Integer.valueOf(((String) o)).intValue();
               log.debug(this + " setting secondaryBindPort to " + secondaryBindPort);
            }
            catch (NumberFormatException e)
            {
               log.warn("Invalid format for " + "\"" + Bisocket.SECONDARY_BIND_PORT + "\": " + o);
            }
      }
      else if (o != null)
      {
         log.warn("\"" + Bisocket.SECONDARY_BIND_PORT + "\" must be specified as a String");
      }
      
      o = configuration.get(Bisocket.SECONDARY_CONNECT_PORT);
      if (o instanceof String && ((String) o).length() > 0)
      {
            try
            {
               secondaryConnectPort = Integer.valueOf(((String) o)).intValue();
               log.debug(this + " setting secondaryConnectPort to " + secondaryConnectPort);
            }
            catch (NumberFormatException e)
            {
               log.warn("Invalid format for " + "\"" + Bisocket.SECONDARY_CONNECT_PORT + "\": " + o);
            }
      }
      else if (o != null)
      {
         log.warn("\"" + Bisocket.SECONDARY_CONNECT_PORT + "\" must be specified as a String");
      }
      
      super.setup();
   }


   protected void cleanup()
   {
      super.cleanup();

      if (controlMonitorTimerTask != null)
         controlMonitorTimerTask.shutdown();

      synchronized (controlConnectionThreadMap)
      {
         Iterator it = controlConnectionThreadMap.values().iterator();
         while (it.hasNext())
         {
            ControlConnectionThread t = (ControlConnectionThread) it.next();
            it.remove();
            t.shutdown();
         }
      }
      
      if (secondaryServerSocketThread != null)
         secondaryServerSocketThread.shutdown();

      if (secondaryServerSocket != null)
      {
         try
         {
            secondaryServerSocket.close();
         }
         catch (IOException e)
         {
            log.info("Error closing secondary server socket: " + e.getMessage());
         }
      }
   }


   protected InvokerLocator getSecondaryLocator()
   {
      return secondaryLocator;
   }


   protected ServerSocket getServerSocket()
   {
      return serverSocket;
   }


   protected void checkSocketFactoryWrapper() throws IOException
   {

      Object o = configuration.get(Remoting.SOCKET_CREATION_SERVER_LISTENER);
      if (o != null)
      {
         if (o instanceof SocketCreationListener)
         {
            SocketCreationListener listener = (SocketCreationListener) o;
            if (socketFactory instanceof CreationListenerSocketFactory)
            {
               CreationListenerSocketFactory clsf = (CreationListenerSocketFactory) socketFactory;
               clsf.setListener(listener);
            }
            else
            {
               socketFactory = new CreationListenerSocketFactory(socketFactory, listener);
            }
         }
         else
         {
            log.error("socket creation listener of invalid type: " + o);
         }
      }
      else
      {
         if (socketFactory instanceof CreationListenerSocketFactory)
         {
            CreationListenerSocketFactory clsf = (CreationListenerSocketFactory) socketFactory;
            socketFactory = clsf.getFactory();
         }
      }
   }


   protected void checkSecondaryServerSocketWrapper() throws IOException
   {

      Object o = configuration.get(Remoting.SOCKET_CREATION_CLIENT_LISTENER);
      if (o != null)
      {
         if (o instanceof SocketCreationListener)
         {
            SocketCreationListener listener = (SocketCreationListener) o;
            if (secondaryServerSocket instanceof CreationListenerServerSocket)
            {
               CreationListenerServerSocket clss = (CreationListenerServerSocket) secondaryServerSocket;
               clss.setListener(listener);
            }
            else
            {
               secondaryServerSocket = new CreationListenerServerSocket(secondaryServerSocket, listener);
            }
         }
         else
         {
            log.error("socket creation listener of invalid type: " + o);
         }
      }
      else
      {
         if (secondaryServerSocket instanceof CreationListenerServerSocket)
         {
            CreationListenerServerSocket clss = (CreationListenerServerSocket) secondaryServerSocket;
            secondaryServerSocket = clss.getServerSocket();
         }
      }
   }


   protected Object handleInternalInvocation(InternalInvocation ii,
                                             InvocationRequest ir,
                                             ServerInvocationHandler handler)
   throws Throwable
   {
      if(Bisocket.GET_SECONDARY_INVOKER_LOCATOR.equals(ii.getMethodName()))
      {
         return secondaryLocator;
      }

      Object response = super.handleInternalInvocation(ii, ir, handler);

      if(InternalInvocation.ADDCLIENTLISTENER.equals(ii.getMethodName()))
      {
         Map metadata = ir.getRequestPayload();
         if(metadata != null)
         {
            String listenerId = (String) metadata.get(Client.LISTENER_ID_KEY);
            if (listenerId != null)
            {
               listenerIdToServerInvokerMap.put(listenerId, this);
            }
         }
      }
      else if(InternalInvocation.REMOVECLIENTLISTENER.equals(ii.getMethodName()))
      {
         Map metadata = ir.getRequestPayload();
         if(metadata != null)
         {
            String listenerId = (String) metadata.get(Client.LISTENER_ID_KEY);
            if (listenerId != null)
            {
               listenerIdToServerInvokerMap.remove(listenerId);
               BisocketClientInvoker.removeBisocketClientInvoker(listenerId);
               destroyControlConnection(listenerId);
            }
         }
      }

      return response;
   }


   class ControlConnectionThread extends Thread
   {
      private static final int MAX_INITIAL_ATTEMPTS = 5;
      private Socket controlSocket;
      private String listenerId;
      private DataInputStream dis;
      private boolean running;
      private int errorCount;
      private long lastPing = -1;
      private int initialAttempts;

      ControlConnectionThread(Socket socket, String listenerId) throws IOException
      {
         controlSocket = socket;
         this.listenerId = listenerId;
         dis = new DataInputStream(socket.getInputStream());
      }

      void shutdown()
      {
         running = false;

         try
         {
            controlSocket.close();
         }
         catch (IOException e)
         {
            log.warn("unable to close controlSocket");
         }
         interrupt();
      }

      boolean checkConnection()
      {
         if (lastPing < 0 && initialAttempts++ < MAX_INITIAL_ATTEMPTS)
         {
            return true;
         }
         else if (lastPing < 0)
         {
            return false;
         }
         
         long currentTime = System.currentTimeMillis();

         if (log.isTraceEnabled())
         {
            log.trace("elapsed: " + (currentTime - lastPing));
         }
         return (currentTime - lastPing <= pingWindow);
      }

      String getListenerId()
      {
         return listenerId;
      }

      public void run()
      {
         running = true;
         while (running)
         {
            Socket socket = null;

            try
            {
               int action = dis.read();
               lastPing = System.currentTimeMillis();

               switch (action)
               {
                  case Bisocket.CREATE_ORDINARY_SOCKET:
                     InvokerLocator locator = (InvokerLocator) listenerIdToInvokerLocatorMap.get(listenerId);
                     
                     IOException savedException = null;
                     
                     for (int i = 0; i < socketCreationRetries; i++)
                     {
                        try
                        {
                           if (socketFactory != null)
                              socket = socketFactory.createSocket(locator.getHost(), locator.getPort());
                           else
                              socket = new Socket(locator.getHost(), locator.getPort());
                        }
                        catch (IOException e)
                        {
                           log.debug("Error creating a socket", e);
                           savedException = e;
                        }
                        
                        if (socket != null)
                           break;
                        
                        try
                        {
                           Thread.sleep(1000);
                        }
                        catch (InterruptedException e)
                        {
                           if (running)
                           {
                              log.debug("received unexpected interrupt");
                              continue;
                           }
                           else
                           {
                              return;
                           }
                        }
                     }
                     
                     if (socket == null)
                     {
                        log.error("Unable to create socket after " + socketCreationRetries 
                                  + " retries", savedException);
                        continue;
                     }
                     
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                     dos.write(Bisocket.CREATE_ORDINARY_SOCKET);
                     dos.writeUTF(listenerId);
                     break;

                  case Bisocket.PING:
                     continue;

                  case -1:
                     shutdown();
                     return;

                  default:
                     log.error("unrecognized action on ControlConnectionThread (" +
                               listenerId + "): " +  action);
                     continue;
               }
            }
            catch (IOException e)
            {
               if (running)
               {
                  if ("Socket closed".equalsIgnoreCase(e.getMessage()) ||
                      "Socket is closed".equalsIgnoreCase(e.getMessage()) ||
                      "Connection reset".equalsIgnoreCase(e.getMessage()))
                  {
                     shutdown();
                     return;
                  }
                  log.error("Unable to process control connection: " + e.getMessage(), e);
                  if (++errorCount > 5)
                  {
                     shutdown();
                     return;
                  }
                  continue;
               }

               return;
            }

            synchronized (clientpool)
            {
               if(clientpool.size() < maxPoolSize)
               {
                  Thread thread = null;
                  try
                  {
                     thread = new ServerThread(socket, BisocketServerInvoker.this,
                                               clientpool, threadpool,
                                               getTimeout(), serverSocketClass);
                     thread.start();

                     if (log.isDebugEnabled())
                        log.debug("created: " + thread);
                  }
                  catch (Exception e)
                  {
                     log.error("Unable to create new ServerThread: " + e.getMessage());
                     e.printStackTrace();
                  }

                  synchronized (threadpool)
                  {
                     threadpool.add(thread);
                  }
               }
            }
         }
      }
   }


   class SecondaryServerSocketThread extends Thread
   {
      private ServerSocket secondaryServerSocket;
      boolean running = true;

      SecondaryServerSocketThread(ServerSocket secondaryServerSocket) throws IOException
      {
         this.secondaryServerSocket = secondaryServerSocket;
      }

      void shutdown()
      {
         running = false;
         interrupt();
      }

      public void run()
      {
         while (running)
         {
            try
            {
               Socket socket = secondaryServerSocket.accept();
               if (log.isTraceEnabled()) log.trace("accepted: " + socket);
               DataInputStream dis = new DataInputStream(socket.getInputStream());
               int action = dis.read();
               String listenerId = dis.readUTF();

               switch (action)
               {
                  case Bisocket.CREATE_CONTROL_SOCKET:
                     BisocketClientInvoker.transferSocket(listenerId, socket, true);
                     if (log.isTraceEnabled()) 
                        log.trace("SecondaryServerSocketThread: created control socket: (" + socket + ")"+ listenerId);
                     break;
                     
                  case Bisocket.RECREATE_CONTROL_SOCKET:
                     BisocketClientInvoker invoker =  BisocketClientInvoker.getBisocketCallbackClientInvoker(listenerId);
                     if (invoker == null)
                     {
                        log.error("received new control socket for unrecognized listenerId: " + listenerId);
                     }
                     else
                     {
                        invoker.replaceControlSocket(socket);
                        if (log.isTraceEnabled())
                           log.trace("SecondaryServerSocketThread: recreated control socket: " + listenerId);
                     }
                     break;

                  case Bisocket.CREATE_ORDINARY_SOCKET:
                     BisocketClientInvoker.transferSocket(listenerId, socket, false);
                     if (log.isTraceEnabled())
                        log.trace("SecondaryServerSocketThread: transferred socket: " + listenerId);
                     break;

                  default:
                     log.error("unrecognized action on SecondaryServerSocketThread: " + action);
               }
            }
            catch (IOException e)
            {
               if (running)
                  log.error("Failed to accept socket connection", e);
               else
                  return;

            }
         }
      }

      ServerSocket getServerSocket()
      {
         return secondaryServerSocket;
      }
   }


   static class ControlMonitorTimerTask extends TimerTask
   {
      private boolean running = true;
      private BisocketServerInvoker invoker;
      private Map listenerIdToInvokerLocatorMap;
      private Map controlConnectionThreadMap;
      private Map controlConnectionRestartsMap;
      private int controlConnectionRestarts;
      
      ControlMonitorTimerTask(BisocketServerInvoker invoker)
      {
         this.invoker = invoker;
         listenerIdToInvokerLocatorMap = invoker.listenerIdToInvokerLocatorMap;
         controlConnectionThreadMap = invoker.controlConnectionThreadMap;
         controlConnectionRestartsMap = invoker.controlConnectionRestartsMap;
         controlConnectionRestarts = invoker.controlConnectionRestarts;
      }

      synchronized void shutdown()
      {
         // Note that there is a race between shutdown() and run().  But if run()
         // were synchronized, then shutdown() could be held up waiting on network
         // i/o, including invocations on a server that no longer is accessible.
         // So only minimal synchronization is imposed on run(), enough to avoid
         // NullPointerExceptions.
         
         running = false;
         invoker = null;
         listenerIdToInvokerLocatorMap = null;
         controlConnectionThreadMap = null;
         cancel();
      }

      public void run()
      {
         if (!running)
            return;
         
         if (log.isTraceEnabled())
            log.trace("checking connections");

         Collection controlConnectionThreads = null;
         synchronized (this)
         {
            if (!running)
               return;
            
            controlConnectionThreads = new HashSet(controlConnectionThreadMap.values());
         }
         
         Iterator it = controlConnectionThreads.iterator();
         while (it.hasNext())
         {
            final ControlConnectionThread t = (ControlConnectionThread) it.next();
            final String listenerId = t.getListenerId();
            final Object locator;
            
            synchronized (this)
            {
               if (!running)
                  return;
                  
               locator = listenerIdToInvokerLocatorMap.get(listenerId);
            }
            
            if (!t.checkConnection())
            {
               t.shutdown();
               
               synchronized (this)
               {
                  if (!running)
                     return;
                  
                  controlConnectionThreadMap.remove(listenerId);
                  Object o = controlConnectionRestartsMap.get(listenerId);
                  int restarts = ((Integer)o).intValue();
                  
                  if (restarts + 1 > controlConnectionRestarts)
                  {
                     log.warn(this + ": detected failure on control connection " + t);
                     log.warn("Control connection " + listenerId + " has been recreated " + restarts + " times.");
                     log.warn("Assuming it is a connection to an old server, and will not restart");
                     controlConnectionRestartsMap.remove(listenerId);
                     continue;
                  }
                  
                  log.warn(this + ": detected failure on control connection " + t + 
                                  " (" + listenerId + 
                                  ": requesting new control connection");
               }
               
               Thread t2 = new Thread()
               {
                  public void run()
                  {
                     if (!running)
                        return;
                     
                     try
                     {
                        invoker.createControlConnection(listenerId, false);
                     }
                     catch (ClientUnavailableException e)
                     {
                        log.debug("Unable to recreate control connection: " + locator, e);
                     }
                     catch (IOException e)
                     {
                        if (running)
                           log.error("Unable to recreate control connection: " + locator, e);
                        else
                           log.debug("Unable to recreate control connection: " + locator, e);
                     }
                  }
               };
               t2.setName("controlConnectionRecreate:" + t.getName());
               t2.start();
            }
         }
      }
   }
   
   static class ClientUnavailableException extends IOException
   {
      private static final long serialVersionUID = 2846502029152028732L;
   }
}