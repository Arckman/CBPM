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

package org.jboss.remoting.transport.socket;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.util.TimerUtil;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.util.propertyeditor.PropertyEditors;
import org.jboss.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimerTask;

/**
 * SocketServerInvoker is the server-side of a SOCKET based transport
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version $Revision: 1.30.2.12.2.1 $
 * @jmx:mbean
 */
public class SocketServerInvoker extends ServerInvoker implements Runnable, SocketServerInvokerMBean
{
   private static final Logger log = Logger.getLogger(SocketServerInvoker.class);

   private static boolean trace = log.isTraceEnabled();

   static int clientCount = 0;

   private Properties props = new Properties();

   private static int BACKLOG_DEFAULT = 200;
   protected static int MAX_POOL_SIZE_DEFAULT = 300;

   /**
    * Key for indicating if socket invoker should continue to keep socket connection between
    * client and server open after invocations by sending a ping on the connection
    * before being re-used.  The default for this is false.
    */
   public static final String CHECK_CONNECTION_KEY = "socket.check_connection";

   /**
    * Specifies the fully qualified class name for the custom SocketWrapper implementation to use on the server.
    */
   public static final String SERVER_SOCKET_CLASS_FLAG = "serverSocketClass";
   protected String serverSocketClass = ServerSocketWrapper.class.getName();

   protected ServerSocket serverSocket = null;
   protected boolean running = false;
   protected int backlog = BACKLOG_DEFAULT;
   protected Thread[] acceptThreads;
   protected int numAcceptThreads = 1;
   protected int maxPoolSize = MAX_POOL_SIZE_DEFAULT;
   protected LRUPool clientpool;
   protected LinkedList threadpool;

   protected boolean newServerSocketFactory = false;
   protected Object serverSocketFactoryLock = new Object();

   protected boolean reuseAddress = true;

   // defaults to -1 as to not have idle timeouts
   protected int idleTimeout = -1;
   protected IdleTimerTask idleTimerTask = null;

   public SocketServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public SocketServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   /**
    * after a truststore update use this to
    * set a new ServerSocketFactory to the invoker<br>
    * then a new ServerSocket is created that accepts the new connections
    * @param serverSocketFactory
 	*/
   public void setNewServerSocketFactory(ServerSocketFactory serverSocketFactory)
   {
      log.trace("entering setNewServerSocketFactory()");
      synchronized (serverSocketFactoryLock)
      {
         newServerSocketFactory=true;
         setServerSocketFactory(serverSocketFactory);
         serverSocketFactoryLock.notify();
         log.info("ServerSocketFactory has been updated");
      }
   }

   /**
    * refreshes the serverSocket by closing old one and
    * creating a new ServerSocket from new ServerSocketFactory
    * @throws IOException
    */
   protected void refreshServerSocket() throws IOException
   {
      log.trace("entering refreshServerSocket()");
      synchronized (serverSocketFactoryLock)
      {
         // If release() is able to enter its synchronized block and sees 
         // serverSocket == null, then it knows that something went wrong.
         newServerSocketFactory=false;
         ServerSocket oldServerSocket = serverSocket;
         serverSocket = null;
         oldServerSocket.close();
         InetAddress bindAddress = InetAddress.getByName(getServerBindAddress());
         ServerSocket newServerSocket = createServerSocket(getServerBindPort(), backlog, bindAddress);
         newServerSocket.setReuseAddress(reuseAddress);
         serverSocket = newServerSocket;
         log.info("ServerSocket has been updated");
      }
      log.trace("leavinging refreshServerSocket()");
   }

   protected void setup() throws Exception
   {
      props.putAll(getConfiguration());
      PropertyEditors.mapJavaBeanProperties(this, props, false);

      super.setup();

      String ssclass = props.getProperty(SERVER_SOCKET_CLASS_FLAG);
      if(ssclass != null)
      {
         serverSocketClass = ssclass;
      }
   }

   protected void finalize() throws Throwable
   {
      stop();
      super.finalize();
   }

   /**
    * Starts the invoker.
    *
    * @jmx.managed-operation description = "Start sets up the ServerInvoker we are wrapping."
    * impact      = "ACTION"
    */
   public synchronized void start() throws IOException
   {
      if(!running)
      {
         log.debug(this + " starting");

         InetAddress bindAddress = InetAddress.getByName(getServerBindAddress());

         if(maxPoolSize <= 0)
         {
            //need to reset to default
            maxPoolSize = MAX_POOL_SIZE_DEFAULT;
         }
         try
         {
            serverSocket = createServerSocket(getServerBindPort(), backlog, bindAddress);
            serverSocket.setReuseAddress(reuseAddress);
         }
         catch(IOException e)
         {
            log.error("Error starting ServerSocket.  Bind port: " + getServerBindPort() +
               ", bind address: " + bindAddress);
            throw e;
         }

         clientpool = new LRUPool(2, maxPoolSize);
         clientpool.create();
         threadpool = new LinkedList();

         acceptThreads = new Thread[numAcceptThreads];

         for(int i = 0; i < numAcceptThreads; i++)
         {
            if(trace) { log.trace(this + " creating another AcceptThread"); }

            String name = getThreadName(i);
            acceptThreads[i] = new Thread(this, name);

            if(trace) { log.trace(this + " created and registered " + acceptThreads[i]); }
         }
      }

      try
      {
         super.start();
      }
      catch(IOException e)
      {
         log.error("Error starting SocketServerInvoker.", e);
         cleanup();
      }
      if(!running)
      {
         running = true;

         for(int i = 0; i < numAcceptThreads; i++)
         {
            acceptThreads[i].start();
         }
      }

      if(idleTimeout > 0)
      {
         if(idleTimerTask != null)
         {
            idleTimerTask.cancel();
         }
         idleTimerTask = new IdleTimerTask();
         TimerUtil.schedule(idleTimerTask, idleTimeout * 1000);
      }
      else
      {
         if(idleTimerTask != null)
         {
            idleTimerTask.cancel();
         }
      }

      log.debug(this + " started");

   }

   protected ServerSocket createServerSocket(int serverBindPort,
                                             int backlog,
                                             InetAddress bindAddress) throws IOException
   {
      return getServerSocketFactory().createServerSocket(serverBindPort, backlog, bindAddress);
   }

   protected String getThreadName(int i)
   {
      return "AcceptorThread#" + i + ":" + getServerBindPort();
   }

   public void destroy()
   {
      if(clientpool != null)
      {
         clientpool.destroy();
      }
      super.destroy();
   }

   /**
    * Stops the invoker.
    *
    * @jmx.managed-operation description = "Stops the invoker."
    * impact      = "ACTION"
    */
   public synchronized void stop()
   {
      if(running)
      {
         cleanup();
      }
      super.stop();
   }

   protected void cleanup()
   {
      running = false;

      maxPoolSize = 0; // so ServerThreads don't reinsert themselves
      if(acceptThreads != null)
      {
         for(int i = 0; i < acceptThreads.length; i++)
         {
            try
            {
               acceptThreads[i].interrupt();
            }
            catch(Exception ignored)
            {
            }
         }
      }

      // The following code has been changed to avoid a race condition with ServerThread.run() which
      // can result in leaving ServerThreads alive, which causes a memory leak.
      if (clientpool != null)
      {
         synchronized (clientpool)
         {
            Set svrThreads = clientpool.getContents();
            Iterator itr = svrThreads.iterator();

            while(itr.hasNext())
            {
               Object o = itr.next();
               ServerThread st = (ServerThread) o;
               st.shutdown();
            }

            clientpool.flush();
            clientpool.stop();

            if (threadpool != null)
            {
               synchronized(threadpool)
               {
                  int threadsToShutdown = threadpool.size();
                  for(int i = 0; i < threadsToShutdown; i++)
                  {
                     ServerThread thread = (ServerThread) threadpool.removeFirst();
                     thread.shutdown();
                  }
               }
            }
         }
      }

      try
      {
         serverSocket.close();
      }
      catch(Exception e)
      {
      }
   }

   /**
    * Indicates if SO_REUSEADDR is enabled on server sockets
    * Default is true.
    */
   public boolean getReuseAddress()
   {
      return reuseAddress;
   }

   /**
    * Sets if SO_REUSEADDR is enabled on server sockets.
    * Default is true.
    *
    * @param reuse
    */
   public void setReuseAddress(boolean reuse)
   {
      this.reuseAddress = reuse;
   }

   /**
    * @return Value of property serverBindPort.
    * @jmx:managed-attribute
    */
   public int getCurrentThreadPoolSize()
   {
      return threadpool.size();
   }

   /**
    * @return Value of property serverBindPort.
    * @jmx:managed-attribute
    */
   public int getCurrentClientPoolSize()
   {
      return clientpool.size();
   }

   /**
    * Getter for property numAcceptThreads
    *
    * @return The number of threads that exist for accepting client connections
    * @jmx:managed-attribute
    */
   public int getNumAcceptThreads()
   {
      return numAcceptThreads;
   }

   /**
    * Setter for property numAcceptThreads
    *
    * @param size The number of threads that exist for accepting client connections
    * @jmx:managed-attribute
    */
   public void setNumAcceptThreads(int size)
   {
      this.numAcceptThreads = size;
   }

   /**
    * Setter for max pool size.
    * The number of server threads for processing client. The default is 300.
    *
    * @return
    * @jmx:managed-attribute
    */
   public int getMaxPoolSize()
   {
      return maxPoolSize;
   }

   /**
    * The number of server threads for processing client. The default is 300.
    *
    * @param maxPoolSize
    * @jmx:managed-attribute
    */
   public void setMaxPoolSize(int maxPoolSize)
   {
      this.maxPoolSize = maxPoolSize;
   }

   /**
    * @jmx:managed-attribute
    */
   public int getBacklog()
   {
      return backlog;
   }

   /**
    * @jmx:managed-attribute
    */
   public void setBacklog(int backlog)
   {
      if(backlog < 0)
      {
         this.backlog = BACKLOG_DEFAULT;
      }
      else
      {
         this.backlog = backlog;
      }
   }

   public int getIdleTimeout()
   {
      return idleTimeout;
   }

   /**
    * Sets the timeout for idle threads to be removed from pool.
    * If the value is greater than 0, then idle timeout will be
    * activated, otherwise no idle timeouts will occur.  By default,
    * this value is -1.
    *
    * @param idleTimeout number of seconds before a idle thread is timed out.
    */
   public void setIdleTimeout(int idleTimeout)
   {
      this.idleTimeout = idleTimeout;

      if(isStarted())
      {
         if(idleTimeout > 0)
         {
            if(idleTimerTask != null)
            {
               idleTimerTask.cancel();
            }
            idleTimerTask = new IdleTimerTask();
            TimerUtil.schedule(idleTimerTask, idleTimeout * 1000);
         }
         else
         {
            if(idleTimerTask != null)
            {
               idleTimerTask.cancel();
            }
         }
      }
   }

   public void run()
   {
      if(trace) { log.trace(this + " started execution of method run()"); }

      ServerSocketRefresh thread = new ServerSocketRefresh();
      thread.setDaemon(true);
      thread.start();

      try
      {
         while(running)
         {
            try
            {
               thread.release(); //goes on if serversocket refresh is completed

               if(trace) { log.trace(this + " is going to wait on serverSocket.accept()"); }

               Socket socket = serverSocket.accept();

               if(trace) { log.trace(this + " accepted " + socket); }

               // the acceptor thread should spend as little time as possbile doing any kind of
               // operation, and under no circumstances should perform IO on the new socket, which
               // can potentially block and lock up the server. For this reason, the acceptor thread
               // should grab a worker thread and delegate all subsequent work to it. This is what
               // processInvocation() does.

               processInvocation(socket);
            }
            catch (SSLException e)
            {
               log.error("SSLServerSocket error", e);
               return;
            }
            catch (InvalidStateException e)
            {
               log.error("Cannot proceed without functioning server socket.  Shutting down");
               stop();
            }
            catch(Throwable ex)
            {  
               if(running)
               {
                  log.error(this + " failed to handle socket", ex);
               }
               else
               {
                  log.debug(this + " caught exception in run()", ex);     
               }
            }
         }
      }
      finally
      {
         thread.shutdown();
      }
   }


   /**
    * The acceptor thread should spend as little time as possbile doing any kind of operation, and
    * under no circumstances should perform IO on the new socket, which can potentially block and
    * lock up the server. For this reason, the acceptor thread should grab a worker thread and
    * delegate all subsequent work to it.
    */
   protected void processInvocation(Socket socket) throws Exception
   {
      ServerThread worker = null;
      boolean newThread = false;

      while(worker == null)
      {
         if(trace) { log.trace(this + " trying to get a worker thread from threadpool for processing"); }

         synchronized(threadpool)
         {
            if(threadpool.size() > 0)
            {
               worker = (ServerThread)threadpool.removeFirst();

               if(trace) { log.trace(this + (worker == null ? " found NO threads in threadpool" : " got " + worker + " from threadpool")); }
            }
            else if (trace) { { log.trace(this + " has an empty threadpool"); } }
         }

         if(worker == null)
         {
            synchronized(clientpool)
            {
               if(clientpool.size() < maxPoolSize)
               {
                  if(trace) { log.trace(this + " creating new worker thread"); }

                  worker = new ServerThread(socket, this, clientpool, threadpool,
                                            getTimeout(), serverSocketClass);

                  if(trace) { log.trace(this + " created " + worker); }

                  newThread = true;
               }

               if(worker == null)
               {
                  if(trace) {log.trace(this + " trying to evict a thread from clientpool"); }

                  clientpool.evict();

                  if(trace) {log.trace(this + " waiting for a thread from clientpool"); }

                  clientpool.wait();

                  if(trace) { log.trace(this + " notified of clientpool thread availability"); }
               }
            }
         }
      }

      synchronized(clientpool)
      {
         clientpool.insert(worker, worker);
      }

      if(newThread)
      {
         if(trace) {log.trace(this + " starting " + worker); }
         worker.start();
      }
      else
      {
         if(trace) { log.trace(this + " reusing " + worker); }
         worker.wakeup(socket, getTimeout(), this);
      }
   }

   /**
    * returns true if the transport is bi-directional in nature, for example,
    * SOAP in unidirectional and SOCKETs are bi-directional (unless behind a firewall
    * for example).
    */
   public boolean isTransportBiDirectional()
   {
      return true;
   }

   public String toString()
   {
      return "SocketServerInvoker[" +
         (serverSocket == null ?
            "UNINITIALIZED" :
            serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort()) +
         "]";
   }

   /**
    * Each implementation of the remote client invoker should have
    * a default data type that is uses in the case it is not specified
    * in the invoker locator uri.
    */
   protected String getDefaultDataType()
   {
      return SerializableMarshaller.DATATYPE;
   }

   /**
    * this thread checks if a new ServerSocketFactory was set,<br>
    * if so initializes a serversocket refresh
    * @author Michael Voss
    *
    */
   public class ServerSocketRefresh extends Thread
   {  
      private boolean running = true;
      
      public ServerSocketRefresh()
      {
         super("ServerSocketRefresh");
      }
      
      public void run()
      {
         while(running)
         {
            synchronized (serverSocketFactoryLock)
            {  
               if(newServerSocketFactory)
               {
                  log.debug("got notice about new ServerSocketFactory");
                  try
                  {
                     log.debug("refreshing server socket");
                     refreshServerSocket();
                  } catch (IOException e)
                  {
                     log.debug("could not refresh server socket");
                     log.debug("message is: "+e.getMessage());
                  }
                  log.debug("server socket refreshed");
               }
               
               try
               {
                  serverSocketFactoryLock.wait();
                  log.trace("ServerSocketRefresh thread woke up");
               }
               catch (InterruptedException e)
               {
               }
            }
         }
      }

      /**
       * Let SocketServerInvoker.run() resume when refresh is completed
       */
      public void release() throws InvalidStateException
      {
         synchronized (serverSocketFactoryLock)
         {
            if (serverSocket == null)
            {
               throw new InvalidStateException("error refreshing ServerSocket");
            }
            log.trace("passed through ServerSocketRefresh.release()");
         }
      }
      
      public void shutdown()
      {
         running = false;
         
         synchronized (serverSocketFactoryLock)
         {
            serverSocketFactoryLock.notify();
         }
      }
   }

   /**
    * The IdleTimerTask is used to periodically check the server threads to
    * see if any have been idle for a specified amount of time, and if so,
    * release those threads and their connections and clear from the server
    * thread pool.
    */
   public class IdleTimerTask extends TimerTask
   {
      public void run()
      {
         Object[] svrThreadArray = null;

         synchronized(clientpool)
         {
            Set svrThreads = clientpool.getContents();
            svrThreadArray = svrThreads.toArray();
         }
         if(trace)
         {
            if(svrThreadArray != null)
               {
                  log.trace("Idle timer task fired.  Number of ServerThreads = " + svrThreadArray.length);
               }
         }

         // iterate through pooled server threads and evict idle ones
         if(svrThreadArray != null)
         {
            long currentTime = System.currentTimeMillis();

            for(int x = 0; x < svrThreadArray.length; x++)
            {
               ServerThread svrThread = (ServerThread)svrThreadArray[x];

               // check the idle time and evict
               long idleTime = currentTime - svrThread.getLastRequestTimestamp();

               if(trace)
               {
                  log.trace("Idle time for ServerThread (" + svrThread + ") is " + idleTime);
               }

               long idleTimeout = getIdleTimeout() * 1000;
               if(idleTime > idleTimeout)
               {
                  if(trace)
                  {
                     log.trace("Idle timeout reached for ServerThread (" + svrThread + ") and will be evicted.");
                  }
                  clientpool.remove(svrThread);
                  svrThread.shutdown();
                  svrThread.unblock();
               }
            }
         }

         // now check idle server threads in the thread pool
         svrThreadArray = null;
         synchronized(threadpool)
         {
            if(threadpool.size() > 0)
            {
               // now need to check the tread pool to remove threads
               svrThreadArray = threadpool.toArray();
            }
         }

         if(trace)
         {
            if(svrThreadArray != null)
            {
               log.trace("Number of ServerThread in thead pool = " + svrThreadArray.length);
            }
         }

         if(svrThreadArray != null)
         {
            long currentTime = System.currentTimeMillis();

            for(int x = 0; x < svrThreadArray.length; x++)
            {
               ServerThread svrThread = (ServerThread)svrThreadArray[x];
               long idleTime = currentTime - svrThread.getLastRequestTimestamp();

               if(trace)
               {
                  log.trace("Idle time for ServerThread (" + svrThread + ") is " + idleTime);
               }

               long idleTimeout = getIdleTimeout() * 1000;
               if(idleTime > idleTimeout)
               {
                  if(trace)
                  {
                     log.trace("Idle timeout reached for ServerThread (" + svrThread + ") and will be removed from thread pool.");
                  }
                  threadpool.remove(svrThread);
                  svrThread.shutdown();
               }
            }
         }
      }
   }

}
