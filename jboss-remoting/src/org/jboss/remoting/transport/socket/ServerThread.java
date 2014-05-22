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

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvocationResponse;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.Version;
import org.jboss.remoting.Client;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * This Thread object hold a single Socket connection to a client
 * and is kept alive until a timeout happens, or it is aged out of the
 * SocketServerInvoker's LRU cache.
 * <p/>
 * There is also a separate thread pool that is used if the client disconnects.
 * This thread/object is re-used in that scenario and that scenario only.
 * <p/>
 * This is a customization of the same ServerThread class used witht the PookedInvoker.
 * The custimization was made to allow for remoting marshaller/unmarshaller.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version $Revision: 1.29.2.20.2.4 $
 */
public class ServerThread extends Thread
{
   // Constants ------------------------------------------------------------------------------------

   final static private Logger log = Logger.getLogger(ServerThread.class);

   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   private static int idGenerator = 0;

   public static synchronized int nextID()
   {
      return idGenerator++;
   }

   // Attributes -----------------------------------------------------------------------------------

   protected volatile boolean running;
   protected volatile boolean handlingResponse;
   protected volatile boolean shutdown;

   protected LRUPool clientpool;
   protected LinkedList threadpool;

   protected String serverSocketClassName;
   protected Class serverSocketClass;

   private Socket socket;
   private int timeout;
   protected SocketServerInvoker invoker;
   private Constructor serverSocketConstructor;
   protected SocketWrapper socketWrapper;

   protected Marshaller marshaller;
   protected UnMarshaller unmarshaller;

   // the unique identity of the thread, which won't change during the life of the thread. The
   // thread may get associated with different IP addresses though.
   private int id = Integer.MIN_VALUE;

   // Indicates if will check the socket connection when getting from pool by sending byte over the
   // connection to validate is still good.
    private boolean shouldCheckConnection;

   // Will indicate when the last request has been processed (used in determining idle
   // connection/thread timeout)
   private long lastRequestHandledTimestamp = System.currentTimeMillis();

   // Constructors ---------------------------------------------------------------------------------

   public ServerThread(Socket socket, SocketServerInvoker invoker, LRUPool clientpool,
                       LinkedList threadpool, int timeout, String serverSocketClassName)
      throws Exception
   {
      super();

      running = true;
      handlingResponse = true; // start off as true so that nobody can interrupt us

      setName(getWorkerThreadName(socket));

      this.socket = socket;
      this.timeout = timeout;
      this.serverSocketClassName = serverSocketClassName;
      this.invoker = invoker;
      this.clientpool = clientpool;
      this.threadpool = threadpool;
      processNewSocket();

      if (invoker != null)
      {
         Map configMap = invoker.getConfiguration();
         String checkValue = (String)configMap.get(SocketServerInvoker.CHECK_CONNECTION_KEY);
         if (checkValue != null && checkValue.length() > 0)
         {
            shouldCheckConnection = Boolean.valueOf(checkValue).booleanValue();
         }
         else if (Version.getDefaultVersion() == Version.VERSION_1)
         {
            shouldCheckConnection = true;
         }
      }
   }

   // Thread overrides -----------------------------------------------------------------------------

   public void run()
   {
      try
      {
         while (true)
         {
            dorun();

            // The following code has been changed to eliminate a race condition with
            // SocketServerInvoker.cleanup().
            //
            // A ServerThread can shutdown for two reasons:
            // 1. the client shuts down, and
            // 2. the server shuts down.
            //
            // If both occur around the same time, a problem arises.  If a ServerThread starts to
            // shut down because the client shut down, it will test shutdown, and if it gets to the
            // test before SocketServerInvoker.cleanup() calls ServerThread.stop() to set shutdown
            // to true, it will return itself to threadpool.  If it moves from clientpool to
            // threadpool at just the right time, SocketServerInvoker could miss it in both places
            // and never call stop(), leaving it alive, resulting in a memory leak.  The solution is
            // to synchronize parts of ServerThread.run() and SocketServerInvoker.cleanup() so that
            // they interact atomically.

            synchronized (this)
            {
               synchronized (clientpool)
               {
                  synchronized (threadpool)
                  {
                     if (shutdown)
                     {
                        invoker = null;
                        return; // exit thread
                     }
                     else
                     {
                        if(trace) { log.trace(this + " removing itself from clientpool and going to threadpool"); }
                        clientpool.remove(this);
                        threadpool.add(this);
                        Thread.interrupted(); // clear any interruption so that we can be pooled.
                        clientpool.notify();
                     }
                  }
               }

               while (true)
               {
                  try
                  {
                     if(trace) { log.trace(this + " begins to wait"); }

                     wait();

                     if(trace) { log.trace(this + " woke up after wait"); }
                     
                     break;
                  }
                  catch (InterruptedException e)
                  {
                     if (shutdown)
                     {
                        invoker = null;
                        return; // exit thread
                     }
                  }
               }
            }
         }
      }
      catch (Exception e)
      {
         log.debug(this + " exiting run on exception, definitively thrown out of the threadpool", e);
      }
   }

   // Public ---------------------------------------------------------------------------------------

   public synchronized void wakeup(Socket socket, int timeout, SocketServerInvoker invoker)
      throws Exception
   {
      // rename the worker thread to reflect the new socket it is handling
      setName(getWorkerThreadName(socket));

      this.socket = socket;
      this.timeout = timeout;
      this.invoker = invoker;

      running = true;
      handlingResponse = true;
      processNewSocket();
      notify();

      if(trace) { log.trace(this + " has notified on mutex"); }
   }

   public long getLastRequestTimestamp()
   {
      return lastRequestHandledTimestamp;
   }

   public void shutdown()
   {
      shutdown = true;
      running = false;

      // This is a race and there is a chance that a invocation is going on at the time of the
      // interrupt.  But I see no way right now to protect for this.

      // NOTE ALSO!: Shutdown should never be synchronized. We don't want to hold up accept()
      // thread! (via LRUpool)

      if (!handlingResponse)
      {
         try
         {
            this.interrupt();
            Thread.interrupted(); // clear
         }
         catch (Exception ignored)
         {
         }
      }
   }

   /**
    * Sets if server thread should check connection before continue to process on next invocation
    * request.  If is set to true, will send an ACK to client to verify client is still connected
    * on same socket.
    */
   public void shouldCheckConnection(boolean checkConnection)
   {
      this.shouldCheckConnection = checkConnection;
   }

   /**
    * Indicates if server will check with client (via an ACK) to see if is still there.
    */
   public boolean getCheckingConnection()
   {
      return this.shouldCheckConnection;
   }

   public void evict()
   {
      running = false;

      // This is a race and there is a chance that a invocation is going on at the time of the
      // interrupt.  But I see no way right now to protect for this. There may not be a problem
      // because interrupt only effects threads blocking on IO.

      // NOTE ALSO!: Shutdown should never be synchronized. We don't want to hold up accept()
      // thread! (via LRUpool)

      if (!handlingResponse)
      {
         try
         {
            this.interrupt();
            Thread.interrupted(); // clear
         }
         catch (Exception ignored)
         {
         }
      }
   }

   /**
    * This method is intended to be used when need to unblock I/O read, which the thread will
    * automatically loop back to do after processing a request.
    */
   public void unblock()
   {
      try
      {
         socket.close();
      }
      catch (IOException e)
      {
         log.warn("Error closing socket when attempting to unblock I/O", e);
      }
   }

   public String toString()
   {
      return getName();
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   /**
    * This is needed because Object*Streams leak
    */
   protected void dorun()
   {
      if(trace) { log.trace("beginning dorun()"); }

      running = true;
      handlingResponse = true;

      // lazy initialize the socketWrapper on the worker thread itself. We do this to avoid to have
      // it done on the acceptor thread (prone to lockup)
      try
      {
         if(trace) { log.trace("creating the socket wrapper"); }

         socketWrapper =
            createServerSocketWrapper(socket, timeout, invoker.getLocator().getParameters());

         // Always do first one without an ACK because its not needed
         if(trace) { log.trace("processing first invocation without acknowledging"); }
         processInvocation(socketWrapper);
      }
      catch (Exception ex)
      {
         log.error("Worker thread initialization failure", ex);
         running = false;
      }

      // Re-use loop
      while (running)
      {
         try
         {
            acknowledge(socketWrapper);
            processInvocation(socketWrapper);
         }
         catch (AcknowledgeFailure e)
         {
            if (!shutdown && trace)
            {
               log.trace("keep alive acknowledge failed!");
            }
            running = false;
         }
         catch(SocketTimeoutException ste)
         {
            if(!shutdown)
            {
               if(trace)
               {
                  log.trace(ste);
               }
            }
            running = false;
         }
         catch (InterruptedIOException e)
         {
            if (!shutdown)
            {
               log.error("Socket IO interrupted", e);
            }
            running = false;

         }
         catch (InterruptedException e)
         {
            if(trace)
            {
               log.trace(e);
            }
            if (!shutdown)
            {
               log.error("interrupted", e);
            }
         }
         catch (EOFException eof)
         {
            if (!shutdown && trace)
            {
               log.trace("EOFException received. This is likely due to client finishing communication.", eof);
            }
            running = false;
         }
         catch (SocketException sex)
         {
            if (!shutdown && trace)
            {
               log.trace("SocketException received. This is likely due to client disconnecting and resetting connection.", sex);
            }
            running = false;
         }
         catch (Exception ex)
         {
            if (!shutdown)
            {
               log.error("failed", ex);
               running = false;
            }
         }
         // clear any interruption so that thread can be pooled.
         handlingResponse = false;
         Thread.interrupted();
      }

      // Ok, we've been shutdown.  Do appropriate cleanups.
      // The stream close code has been moved to SocketWrapper.close().
//      try
//      {
//         if (socketWrapper != null)
//         {
//            InputStream in = socketWrapper.getInputStream();
//            if (in != null)
//            {
//               in.close();
//            }
//            OutputStream out = socketWrapper.getOutputStream();
//            if (out != null)
//            {
//               out.close();
//            }
//         }
//      }
//      catch (Exception ex)
//      {
//         log.debug("failed to close in/out", ex);
//      }

      try
      {
         if (socketWrapper != null)
         {
            log.debug(this + " closing socketWrapper: " + socketWrapper);
            socketWrapper.close();
         }
      }
      catch (Exception ex)
      {
         log.error("failed to close socket wrapper", ex);
      }
      socketWrapper = null;
   }

   protected void processInvocation(SocketWrapper socketWrapper) throws Exception
   {
      if(trace) { log.trace("preparing to process next invocation invocation"); }

      handlingResponse = true;

      // Ok, now read invocation and invoke

      //TODO: -TME This needs to be done by ServerInvoker
      int version = Version.getDefaultVersion();
      boolean performVersioning = Version.performVersioning();
      InputStream inputStream = socketWrapper.getInputStream();

      if (performVersioning)
      {
         version = readVersion(inputStream);

         //TODO: -TME Should I be checking for -1?

         // This is a best attempt to determine if is old version.  Typically, the first byte will
         // be -1, so if is, will reset stream and process as though is older version.

         // Originally this code (now uncommented) and the other commented code was to try to make
         // so could automatically detect older version that would not be sending a byte for the
         // version.  However, due to the way the serialization stream manager handles the stream,
         // resetting it does not work, so will probably have to throw away that idea. However, for
         // now, am uncommenting this section because if are using the flag to turn off connection
         // checking (ack back to client), then will get a -1 when the client closes connection.
         // Then when stream passed onto the versionedRead, will get EOFException thrown and will
         // process normally (as though came from the acknowledge, as would have happened if
         // connection checking was turned on).  Am hoping this is not a mistake...

         if(version == -1)
         {
//            version = Version.VERSION_1;
            throw new EOFException();
         }
      }

      Object obj = versionedRead(inputStream, invoker, getClass().getClassLoader(), version);

      // setting timestamp since about to start processing
      lastRequestHandledTimestamp = System.currentTimeMillis();

      InvocationRequest req = null;
      boolean createdInvocationRequest = false;
      boolean isError = false;

      if(obj instanceof InvocationRequest)
      {
         req = (InvocationRequest)obj;
      }
      else
      {
         req = createInvocationRequest(obj, socketWrapper);
         createdInvocationRequest = true;
         performVersioning = false;
      }

      Object resp = null;

      try
      {
         // Make absolutely sure thread interrupted is cleared.
         Thread.interrupted();

         if(trace) { log.trace("about to call " + invoker + ".invoke()"); }

         // handle socket-specific invocations
         if ("$GET_CLIENT_LOCAL_ADDRESS$".equals(req.getParameter()))
         {
            Socket s = socketWrapper.getSocket();
            InetAddress a = s.getInetAddress();
            resp = new InvocationResponse(req.getSessionId(), a, false, null);
         }
         else
         {
             // call transport on the subclass, get the result to handback
             resp = invoker.invoke(req);
         }

         if(trace) { log.trace(invoker + ".invoke() returned " + resp); }
      }
      catch (Throwable ex)
      {
         resp = ex;
         isError = true;
         if (trace) log.trace(invoker + ".invoke() call failed", ex);
      }

      Thread.interrupted(); // clear interrupted state so we don't fail on socket writes

      if(isOneway(req.getRequestPayload()))
      {
         if(trace) { log.trace("oneway request, writing no reply on the wire"); }
      }
      else
      {
         if(!createdInvocationRequest)
         {
            // need to return invocation response
            if(trace) { log.trace("creating response instance"); }
            resp = new InvocationResponse(req.getSessionId(), resp, isError, req.getReturnPayload());
         }

         OutputStream outputStream = socketWrapper.getOutputStream();
         if (performVersioning)
         {
            writeVersion(outputStream, version);
         }

         versionedWrite(outputStream, invoker, this.getClass().getClassLoader(), resp, version);
      }

      handlingResponse = false;

      // set the timestamp for last successful processed request
      lastRequestHandledTimestamp = System.currentTimeMillis();
   }

   protected void acknowledge(SocketWrapper socketWrapper) throws Exception
   {
      if (shouldCheckConnection)
      {
         // HERE IS THE RACE between ACK received and handlingResponse = true. We can't synchronize
         // because readByte blocks and client is expecting a response and we don't want to hang
         // client. See shutdown and evict for more details. There may not be a problem because
         // interrupt only effects threads blocking on IO. and this thread will just continue.

         handlingResponse = true;

         try
         {
            if(trace) { log.trace("checking connection"); }
            socketWrapper.checkConnection();
         }
         catch (EOFException e)
         {
            throw new AcknowledgeFailure();
         }
         catch (SocketException se)
         {
            throw new AcknowledgeFailure();
         }
         catch (IOException ioe)
         {
            throw new AcknowledgeFailure();
         }

         handlingResponse = false;
      }
   }

   protected Object versionedRead(InputStream inputStream, ServerInvoker invoker,
                                  ClassLoader classLoader, int version)
      throws IOException, ClassNotFoundException
   {
      //TODO: -TME - Should I even botther to check for version here?  Only one way to do processing
      //             at this point, regardless of version.
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            if(trace) { log.trace("blocking to read invocation from unmarshaller"); }

            Object o = null;
            if (unmarshaller instanceof VersionedUnMarshaller)
               o = ((VersionedUnMarshaller)unmarshaller).read(inputStream, null, version);
            else
               o = unmarshaller.read(inputStream, null);

            if(trace) { log.trace("read " + o + " from unmarshaller"); }

            return o;
         }
         default:
         {
            throw new IOException("Can not read data for version " + version +
               ".  Supported versions: " + Version.VERSION_1 + "," + Version.VERSION_2 + "," + Version.VERSION_2_2);
         }
      }
   }

   // Private --------------------------------------------------------------------------------------

   private SocketWrapper createServerSocketWrapper(Socket socket, int timeout, Map metadata)
      throws Exception
   {
      if (serverSocketConstructor == null)
      {
         if(serverSocketClass == null)
         {
            serverSocketClass = ClassLoaderUtility.loadClass(serverSocketClassName, getClass());
         }

         try
         {
            serverSocketConstructor = serverSocketClass.
               getConstructor(new Class[]{Socket.class, Map.class, Integer.class});
         }
         catch (NoSuchMethodException e)
         {
            serverSocketConstructor = serverSocketClass.getConstructor(new Class[]{Socket.class});
         }

      }

      SocketWrapper serverSocketWrapper = null;

      if (serverSocketConstructor.getParameterTypes().length == 3)
      {
         Map localMetadata = null;
         if (metadata == null)
         {
            localMetadata = new HashMap(2);
         }
         else
         {
            localMetadata = new HashMap(metadata);
         }
         localMetadata.put(SocketWrapper.MARSHALLER, marshaller);
         localMetadata.put(SocketWrapper.UNMARSHALLER, unmarshaller);

         serverSocketWrapper = (SocketWrapper)serverSocketConstructor.
            newInstance(new Object[]{socket, localMetadata, new Integer(timeout)});
      }
      else
      {
         serverSocketWrapper =
            (SocketWrapper)serverSocketConstructor.newInstance(new Object[]{socket});

         serverSocketWrapper.setTimeout(timeout);
      }
      return serverSocketWrapper;
   }

   private boolean isOneway(Map metadata)
   {
      boolean isOneway = false;

      if (metadata != null)
      {
         Object val = metadata.get(Client.ONEWAY_FLAG);
         if (val != null && val instanceof String && Boolean.valueOf((String) val).booleanValue())
         {
            isOneway = true;
         }
      }
      return isOneway;
   }

   private InvocationRequest createInvocationRequest(Object obj, SocketWrapper socketWrapper)
   {
      if(obj instanceof InvocationRequest)
      {
         return (InvocationRequest)obj;
      }
      else
      {
         // need to wrap request with invocation request
         SocketAddress remoteAddress = socketWrapper.getSocket().getRemoteSocketAddress();

         return new InvocationRequest(remoteAddress.toString(),
                                      invoker.getSupportedSubsystems()[0],
                                      obj, null, null, null);
      }
   }

   private void processNewSocket()
   {
      InvokerLocator locator = invoker.getLocator();
      ClassLoader classLoader = getClass().getClassLoader();
      String dataType = invoker.getDataType();
      String serializationType = invoker.getSerializationType();

      //TODO: -TME Need better way to get the unmarshaller (via config)

      if (unmarshaller == null)
      {
         unmarshaller = MarshalFactory.getUnMarshaller(locator, classLoader);
      }
      if (unmarshaller == null)
      {
         unmarshaller = MarshalFactory.getUnMarshaller(dataType, serializationType);
      }

      if (marshaller == null)
      {
         marshaller = MarshalFactory.getMarshaller(locator, classLoader);
      }
      if (marshaller == null)
      {
         marshaller = MarshalFactory.getMarshaller(dataType, serializationType);
      }


   }

   private void versionedWrite(OutputStream outputStream, SocketServerInvoker invoker,
                               ClassLoader classLoader, Object resp, int version) throws IOException
   {
      //TODO: -TME - Should I ever worry about checking version here?  Only one way to send data at this point.
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(resp, outputStream, version);
            else
               marshaller.write(resp, outputStream);
            if (trace) { log.trace("wrote response to the output stream"); }
            return;
         }
         default:
         {
            throw new IOException("Can not write data for version " + version +
               ".  Supported version: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);
         }
      }
   }

   private int readVersion(InputStream inputStream) throws IOException
   {
      if(trace) { log.trace("blocking to read version from input stream"); }

      int version = inputStream.read();

      if(trace) { log.trace("read version " + version + " from input stream"); }

      return version;
   }

   private void writeVersion(OutputStream outputStream, int version) throws IOException
   {
      outputStream.write(version);
   }

   private String getWorkerThreadName(Socket currentSocket)
   {
      if (id == Integer.MIN_VALUE)
      {
         id = nextID();
      }

      StringBuffer sb = new StringBuffer("WorkerThread#");
      sb.append(id).append('[');
      sb.append(currentSocket.getInetAddress().getHostAddress());
      sb.append(':');
      sb.append(currentSocket.getPort());
      sb.append(']');

      return sb.toString();
   }

   // Inner classes --------------------------------------------------------------------------------

   public static class AcknowledgeFailure extends Exception
   {
   }
}
