package org.jboss.remoting.transport.socket;

import org.jboss.logging.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.Version;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.util.propertyeditor.PropertyEditors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.rmi.MarshalException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

/**
 * SocketClientInvoker uses Sockets to remotely connect to the a remote ServerInvoker, which must be
 * a SocketServerInvoker.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version $Revision: 1.16.2.26.2.4 $
 */
public class MicroSocketClientInvoker extends RemoteClientInvoker
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(MicroSocketClientInvoker.class);

   /**
    * Can be either true or false and will indicate if client socket should have TCP_NODELAY turned
    * on or off. TCP_NODELAY is for a specific purpose; to disable the Nagle buffering algorithm.
    * It should only be set for applications that send frequent small bursts of information without
    * getting an immediate response; where timely delivery of data is required (the canonical
    * example is mouse movements). The default is false.
    */
   public static final String TCP_NODELAY_FLAG = "enableTcpNoDelay";

   /**
    * The client side maximum number of threads. The default is MAX_POOL_SIZE.
    */
   public static final String MAX_POOL_SIZE_FLAG = "clientMaxPoolSize";

   /**
    * Specifies the fully qualified class name for the custom SocketWrapper implementation to use on
    * the client. Note, will need to make sure this is marked as a client parameter (using the
    * 'isParam' attribute). Making this change will not affect the marshaller/unmarshaller that is
    * used, which may also be a requirement.
    */
   public static final String CLIENT_SOCKET_CLASS_FLAG = "clientSocketClass";

   /**
    * Default value for enable TCP nodelay. Value is false.
    */
   public static final boolean TCP_NODELAY_DEFAULT = false;

   /**
    * Default maximum number of retries to get a valid socket from the* socket pool. This also
    * translates to number of seconds will wait for connection to be returned to connection pool
    * before erroring. Default is 30.
    */
   public static final int MAX_RETRIES = 30;

   /**
    * Default maximum number of times a invocation will be made when it gets a SocketException.
    * Default is 3.
    */
   public static final int MAX_CALL_RETRIES = 3;

   /**
    * Default maximum number of socket connections allowed at any point in time. Default is 50.
    */
   public static final int MAX_POOL_SIZE = 50;


   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   /**
    * Used for debugging (tracing) connections leaks
    */
   static int counter = 0;

   protected static final Map connectionPools = new HashMap();

   // Performance measurements
   public static long getSocketTime = 0;
   public static long readTime = 0;
   public static long writeTime = 0;
   public static long serializeTime = 0;
   public static long deserializeTime = 0;

   /**
    * Close all sockets in a specific pool.
    */
   public static void clearPool(LinkedList thepool)
   {
      try
      {
         if (thepool == null)
         {
            return;
         }
         synchronized (thepool)
         {
            int size = thepool.size();
            for (int i = 0; i < size; i++)
            {
               SocketWrapper socketWrapper = (SocketWrapper)thepool.removeFirst();
               try
               {
                  socketWrapper.close();
                  socketWrapper = null;
               }
               catch (Exception ignored)
               {
               }
            }
         }
      }
      catch (Exception ex)
      {
         log.debug("Failure", ex);
      }
   }

   /**
    * Close all sockets in all pools.
    */
   public static void clearPools()
   {
      synchronized (connectionPools)
      {
         for(Iterator i = connectionPools.keySet().iterator(); i.hasNext();)
         {
            ServerAddress sa = (ServerAddress) i.next();

            if (trace) { log.trace("clearing pool for " + sa); }
            clearPool((LinkedList) connectionPools.get(sa));
            i.remove();
         }
      }
   }

   // Attributes -----------------------------------------------------------------------------------

   private Constructor clientSocketConstructor;
   private boolean reuseAddress;

   protected InetAddress addr;
   protected int port;

   // flag being set on true by a disconnect request. If trying to create a connection goes on in a
   // loop and a disconnect request arrives, this flag will be used to sent this information into
   // the connect loop
   private volatile boolean bailOut;

   /**
    * Indicates if will check the socket connection when getting from pool by sending byte over the
    * connection to validate is still good.
    */
   protected boolean shouldCheckConnection;

   /**
    * If the TcpNoDelay option should be used on the socket.
    */
   protected boolean enableTcpNoDelay;

   protected String clientSocketClassName;
   protected Class clientSocketClass;
   protected int numberOfRetries;
   protected int numberOfCallRetries;
   protected int maxPoolSize;

   /**
    * Pool for this invoker. This is shared between all instances of proxies attached to a specific
    * invoker.
    */
   protected LinkedList pool;

   /**
    * connection information
    */
   protected ServerAddress address;

   public long usedPooled;
   public Object usedPoolLock;

   // Constructors ---------------------------------------------------------------------------------

   public MicroSocketClientInvoker(InvokerLocator locator)
   {
      this(locator, null);
   }

   public MicroSocketClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);

      clientSocketConstructor = null;
      reuseAddress = true;
      shouldCheckConnection = false;
      enableTcpNoDelay = TCP_NODELAY_DEFAULT;
      clientSocketClassName = ClientSocketWrapper.class.getName();
      clientSocketClass = null;
      numberOfRetries = MAX_RETRIES;
      numberOfCallRetries = MAX_CALL_RETRIES;
      pool = null;
      maxPoolSize = MAX_POOL_SIZE;
      usedPooled = 0;
      usedPoolLock = new Object();

      try
      {
         setup();
      }
      catch (Exception ex)
      {
         log.error("Error setting up " + this, ex);
         throw new RuntimeException(ex.getMessage());
      }

      log.debug(this + " constructed");
   }

   // Public ---------------------------------------------------------------------------------------

   /**
    * Indicates if will check socket connection when returning from pool by sending byte to the
    * server. Default value will be false.
    */
   public boolean checkingConnection()
   {
      return shouldCheckConnection;
   }

   /**
    * Returns if newly created sockets will have SO_REUSEADDR enabled. Default is for this to be
    * true.
    */
   public boolean getReuseAddress()
   {
      return reuseAddress;
   }

   /**
    * Sets if newly created socket should have SO_REUSEADDR enable. Default is true.
    */
   public void setReuseAddress(boolean reuse)
   {
      reuseAddress = reuse;
   }

   public synchronized void disconnect()
   {
      log.debug(this + " disconnecting ...");
      bailOut = true;
      super.disconnect();
   }

   public void flushConnectionPool()
   {
      synchronized (pool)
      {
         while (pool != null && pool.size() > 0)
         {
            SocketWrapper socketWrapper = (SocketWrapper)pool.removeFirst();
            try
            {
               socketWrapper.close();
            }
            catch (IOException e)
            {
               log.debug("Failed to close socket wrapper", e);
            }
         }
      }
   }

   /**
    * Sets the number of times an invocation will retry based on getting SocketException.
    */
   public void setNumberOfCallRetries(int numberOfCallRetries)
   {
      if (numberOfCallRetries < 1)
      {
         this.numberOfCallRetries = MAX_CALL_RETRIES;
      }
      else
      {
         this.numberOfCallRetries = numberOfCallRetries;
      }
   }

   public int getNumberOfCallRetries()
   {
      return numberOfCallRetries;
   }

   /**
    * Sets the number of retries to get a socket connection.
    *
    * @param numberOfRetries Must be a number greater than 0.
    */
   public void setNumberOfRetries(int numberOfRetries)
   {
      if (numberOfRetries < 1)
      {
         this.numberOfRetries = MAX_RETRIES;
      }
      else
      {
         this.numberOfRetries = numberOfRetries;
      }
   }

   public int getNumberOfRetries()
   {
      return numberOfRetries;
   }

   /**
    * The name of of the server.
    */
   public String getServerHostName() throws Exception
   {
      return address.address;
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected void setup() throws Exception
   {
      addr = InetAddress.getByName(locator.getHost());
      port = locator.getPort();

      Properties props = new Properties();
      props.putAll(configuration);
      PropertyEditors.mapJavaBeanProperties(this, props, false);

      configureParameters();

      address = createServerAddress();
   }

   protected void configureParameters()
   {
      Map params = configuration;

      if (params == null)
      {
         return;
      }

      // look for enableTcpNoDelay param
      Object val = params.get(TCP_NODELAY_FLAG);
      if (val != null)
      {
         try
         {
            enableTcpNoDelay = Boolean.valueOf((String)val).booleanValue();
            log.debug(this + " setting enableTcpNoDelay to " + enableTcpNoDelay);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + TCP_NODELAY_FLAG + " value of " +
                     val + " to a boolean value.");
         }
      }

      // look for maxPoolSize param
      val = params.get(MAX_POOL_SIZE_FLAG);
      if (val != null)
      {
         try
         {
            maxPoolSize = Integer.valueOf((String)val).intValue();
            log.debug(this + " setting maxPoolSize to " + maxPoolSize);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + MAX_POOL_SIZE_FLAG + " value of " +
                     val + " to a int value");
         }
      }

      // look for client socket class name
      val = params.get(CLIENT_SOCKET_CLASS_FLAG);
      if (val != null)
      {
         String value = (String)val;
         if (value.length() > 0)
         {
            clientSocketClassName = value;
            log.debug(this + " setting client socket wrapper class name to " + clientSocketClassName);
         }
      }

      val = params.get(SocketServerInvoker.CHECK_CONNECTION_KEY);
      if (val != null && ((String)val).length() > 0)
      {
         String value = (String) val;
         shouldCheckConnection = Boolean.valueOf(value).booleanValue();
         log.debug(this + " setting shouldCheckConnection to " + shouldCheckConnection);
      }
      else if (Version.getDefaultVersion() == Version.VERSION_1)
      {
         shouldCheckConnection = true;
         log.debug(this + " setting shouldCheckConnection to " + shouldCheckConnection);
      }
   }

   protected ServerAddress createServerAddress()
   {
      return new ServerAddress(addr.getHostAddress(), port, enableTcpNoDelay, -1);
   }

   protected void finalize() throws Throwable
   {
      disconnect();
      super.finalize();
   }

   protected synchronized void handleConnect() throws ConnectionFailedException
   {
      initPool();
   }

   protected synchronized void handleDisconnect()
   {
      clearPools();
      clearPool(pool);
   }

   /**
    * Each implementation of the remote client invoker should have a default data type that is used
    * in the case it is not specified in the invoker locator URI.
    */
   protected String getDefaultDataType()
   {
      return SerializableMarshaller.DATATYPE;
   }

   protected Object transport(String sessionID, Object invocation, Map metadata,
                              Marshaller marshaller, UnMarshaller unmarshaller)
         throws IOException, ConnectionFailedException, ClassNotFoundException
   {
      long start = System.currentTimeMillis();
      SocketWrapper socketWrapper = null;
      Object response = null;
      boolean oneway = false;

      // tempTimeout < 0 will indicate there is no per invocation timeout.
      int tempTimeout = -1;
      int savedTimeout = -1;

      if(metadata != null)
      {
         // check to see if is one way invocation and return after writing invocation if is
         Object val = metadata.get(org.jboss.remoting.Client.ONEWAY_FLAG);
         if(val != null && val instanceof String && Boolean.valueOf((String)val).booleanValue())
         {
            oneway = true;
         }

         // look for temporary timeout values
         String tempTimeoutString = (String) metadata.get(ServerInvoker.TIMEOUT);
         {
            if (tempTimeoutString != null)
            {
               try
               {
                  tempTimeout = Integer.valueOf(tempTimeoutString).intValue();
                  log.debug(this + " setting timeout to " + tempTimeout + " for this invocation");
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + ServerInvoker.TIMEOUT + " value of " +
                           tempTimeoutString + " to an integer value.");
               }
            }
         }
      }

      int retryCount = 0;
      SocketException sockEx = null;

      for (; retryCount < numberOfCallRetries; retryCount++)
      {
         // timeLeft < 0 will indicate that there is no per invocation timeout.
         int timeLeft = -1;
         if (0 < tempTimeout)
         {
            // If a per invocation timeout has been set, the time spent retrying
            // should count toward the elapsed time.
            timeLeft = (int) (tempTimeout - (System.currentTimeMillis() - start));
            if (timeLeft <= 0)
               break;
         }

         try
         {
            socketWrapper = getConnection(marshaller, unmarshaller, timeLeft);
         }
         catch (Exception e)
         {
//            if (bailOut)
//               return null;
            
            throw new CannotConnectException(
               "Can not get connection to server. Problem establishing " +
               "socket connection for " + locator, e);
         }

         if (tempTimeout >= 0)
         {
            savedTimeout = socketWrapper.getTimeout();
            socketWrapper.setTimeout((int) (tempTimeout - (System.currentTimeMillis() - start)));
         }

         long end = System.currentTimeMillis() - start;
         getSocketTime += end;

         try
         {
            int version = Version.getDefaultVersion();
            boolean performVersioning = Version.performVersioning();

            OutputStream outputStream = socketWrapper.getOutputStream();

            if (performVersioning)
            {
               writeVersion(outputStream, version);
            }

            //TODO: -TME so this is messed up as now ties remoting versioning to using a marshaller type
            versionedWrite(outputStream, marshaller, invocation, version);

            end = System.currentTimeMillis() - start;
            writeTime += end;
            start = System.currentTimeMillis();

            if (oneway)
            {
               if(trace) { log.trace(this + " sent oneway invocation, so not waiting for response, returning null"); }
            }
            else
            {
               InputStream inputStream = socketWrapper.getInputStream();
               if (performVersioning)
               {
                  version = readVersion(inputStream);
                  if (version == -1)
                  {
                     throw new SocketException("end of file");
                  }
                  if (version == SocketWrapper.CLOSING)
                  {
                     log.info("Received version 254: treating as end of file");
                     throw new SocketException("end of file");
                  }
               }

               response = versionedRead(inputStream, unmarshaller, version);
            }

            end = System.currentTimeMillis() - start;
            readTime += end;

            // Note that resetting the timeout value after closing the socket results
            // in an exception, so the reset is not done in a finally clause.  However,
            // if a catch clause is ever added that does not close the socket, care
            // must be taken to reset the timeout in that case.
            if (tempTimeout >= 0)
            {
               socketWrapper.setTimeout(savedTimeout);
            }
         }
         catch (SocketException sex)
         {
            log.debug(this + " got SocketException " + sex);

            try
            {
               socketWrapper.close();
               synchronized (usedPoolLock)
               {
                  usedPooled--;
               }
            }
            catch (Exception ex)
            {
               if (trace) { log.trace(this + " couldn't successfully close its socketWrapper", ex); }
            }

            /**
             * About to run out of retries and
             * pool may be full of timed out sockets,
             * so want to flush the pool and try with
             * fresh socket as a last effort.
             */
            if (retryCount == (numberOfCallRetries - 2))
            {
               flushConnectionPool();
            }
            sockEx = sex;
            continue;
         }
         catch (Exception ex)
         {
            log.debug(this + " got exception " + ex);

            try
            {
               socketWrapper.close();
               synchronized (usedPoolLock)
               {
                  usedPooled--;
               }
            }
            catch (Exception ignored)
            {
            }
            return handleException(ex, socketWrapper);
         }

         // call worked, so no need to retry
         break;
      }

      // need to check if ran out of retries
      if (retryCount >= numberOfCallRetries)
      {
         handleException(sockEx, socketWrapper);
      }

      // Put socket back in pool for reuse
      synchronized (pool)
      {
         if (pool.size() < maxPoolSize)
         {
            pool.add(socketWrapper);
            synchronized(usedPoolLock)
            {
               usedPooled--;
            }
            if (trace) { log.trace(this + " returned " + socketWrapper + " to pool"); }
         }
         else
         {
            if (trace) { log.trace(this + "'s pool is full, will close the connection"); }
            try
            {
               socketWrapper.close();
            }
            catch (Exception ignored)
            {
            }
         }
      }

      if (trace && !oneway) { log.trace(this + " received response " + response);  }
      return response;
   }

   protected Object handleException(Exception ex, SocketWrapper socketWrapper)
      throws ClassNotFoundException, MarshalException
   {
      log.error(this + " got marshalling exception, exiting ...", ex);

      if (ex instanceof ClassNotFoundException)
      {
         //TODO: -TME Add better exception handling for class not found exception
         log.error("Error loading classes from remote call result.", ex);
         throw (ClassNotFoundException)ex;
      }

      throw new MarshalException(
         "Failed to communicate. Problem during marshalling/unmarshalling.", ex);
   }

   protected void initPool()
   {
      synchronized (connectionPools)
      {
         pool = (LinkedList)connectionPools.get(address);
         if (pool == null)
         {
            pool = new LinkedList();
            connectionPools.put(address, pool);
            if (trace)
            {
               synchronized (pool)
               {
                  log.trace(this + " added new pool (" + pool + ") as " + address);
               }
            }
         }
         else
         {
            if (trace)
            {
               synchronized (pool)
               {
                  log.trace(this + " using pool (" + pool + ") already defined for " + address);
               }
            }
         }
      }
   }

   protected SocketWrapper getConnection(Marshaller marshaller,
                                         UnMarshaller unmarshaller,
                                         int timeAllowed)
      throws Exception
   {
      SocketWrapper pooled = null;

      // Need to retry a few times on socket connection because, at least on Windoze, if too many
      // concurrent threads try to connect at same time, you get ConnectionRefused. Retrying seems
      // to be the most performant. This problem always happens with RMI and seems to have nothing
      // to do with backlog or number of threads waiting in accept() on the server.

      // If a per socket invocation has been set, the time spent looking for a connection
      // should count toward the elapsed time.
      long start = System.currentTimeMillis();

      for (int i = 0; i < numberOfRetries; i++)
      {
         if (bailOut)
         {
            log.debug(this + " has been concurrently disconnected, " +
               "bailing out from trying to create a new connection");
            break;
         }

         // timeAllowed < 0 indicates no per invocation timeout has been set.
         int timeRemaining = -1;
         if (0 <= timeAllowed)
         {
            timeRemaining = (int) (timeAllowed - (System.currentTimeMillis() - start));
            if (timeRemaining <= 0)
               break;
         }

         synchronized (pool)
         {
            // if connection within pool, use it
            if (pool.size() > 0)
            {
               pooled = getPooledConnection();
               if (trace) log.trace(this + " reusing pooled connection: " + pooled);
            }
         }

         boolean retry = false;
         synchronized(usedPoolLock)
         {
            if (pooled != null)
            {
               usedPooled++;
               if (trace) log.trace(this + " got a socket, usedPooled: " + usedPooled);
               break;
            }
            if (usedPooled < maxPoolSize)
            {
               // Try to get a socket.
               if (trace) log.trace(this + " getting a socket, usedPooled: " + usedPooled);
               usedPooled++;
            }
            else
            {
               retry = true;
               if (trace) log.trace(this + " will try again to get a socket");
            }
         }

         if (retry)
         {
            Thread.sleep(1000);
            continue;
         }

         // If no connection in pool and all pooled connections not in use, then need create
         // a new connection which will be later returned to the pool (thus filling out the
         // pool, since starts out empty).

         Socket socket = null;
         long timestamp = System.currentTimeMillis();
         try
         {
            if (trace) { log.trace(this + " creating socket " + (counter++) + ", attempt " + (i + 1)); }
            socket = createSocket(address.address, address.port, timeRemaining);
            if (trace) log.trace(this + " created socket: " + socket);
         }
         catch (Exception ex)
         {
            log.debug(this + " got Exception " + ex + ", creation attempt took " +
                  (System.currentTimeMillis() - timestamp) + " ms");

            synchronized(usedPoolLock)
            {
               usedPooled--;
            }
            if (i + 1 < numberOfRetries)
            {
               Thread.sleep(1);
               continue;
            }
            throw ex;
         }
         socket.setTcpNoDelay(address.enableTcpNoDelay);

         Map metadata = getLocator().getParameters();
         if (metadata == null)
         {
            metadata = new HashMap(2);
         }
         else
         {
            metadata = new HashMap(metadata);
         }
         metadata.put(SocketWrapper.MARSHALLER, marshaller);
         metadata.put(SocketWrapper.UNMARSHALLER, unmarshaller);

         if (timeAllowed > 0)
         {
            timeRemaining = (int) (timeAllowed - (System.currentTimeMillis() - start));
            if (timeRemaining <= 0)
               break;
            metadata.put(SocketWrapper.TEMP_TIMEOUT, new Integer(timeRemaining));
         }

         pooled = createClientSocket(socket, address.timeout, metadata);
         break;
      }

      if (pooled == null)
      {
         throw new SocketException("Can not obtain client socket connection from pool. " +
            "Have waited " + (System.currentTimeMillis() - start) +
            " milliseconds for available connection (" + usedPooled + "in use)");
      }
      else
      {
         return pooled;
      }
   }

   protected SocketWrapper createClientSocket(Socket socket, int timeout, Map metadata)
      throws Exception
   {
      if (clientSocketConstructor == null)
      {
         if(clientSocketClass == null)
         {
            clientSocketClass = ClassLoaderUtility.loadClass(clientSocketClassName, getClass());
         }

         Class[] args = new Class[]{Socket.class, Map.class, Integer.class};
         clientSocketConstructor = clientSocketClass.getConstructor(args);
      }

      SocketWrapper clientSocketWrapper = null;
      clientSocketWrapper = (SocketWrapper)clientSocketConstructor.
         newInstance(new Object[]{socket, metadata, new Integer(timeout)});

      return clientSocketWrapper;
   }

   protected Socket createSocket(String address, int port, int timeout) throws IOException
   {
      Socket s = new Socket();
      s.setReuseAddress(getReuseAddress());
      InetSocketAddress inetAddr = new InetSocketAddress(address, port);
      s.connect(inetAddr);
      return s;
   }

   protected SocketWrapper getPooledConnection()
   {
      SocketWrapper socketWrapper = null;
      while (pool.size() > 0)
      {
         socketWrapper = (SocketWrapper)pool.removeFirst();
         try
         {
            if (socketWrapper != null)
            {
               if (socketWrapper instanceof OpenConnectionChecker)
               {
                  ((OpenConnectionChecker) socketWrapper).checkOpenConnection();
               }
               if (shouldCheckConnection)
               {
                  socketWrapper.checkConnection();
                  return socketWrapper;
               }
               else
               {
                  return socketWrapper;
               }
            }
         }
         catch (Exception ex)
         {
            if (trace) { log.trace(this + " couldn't reuse connection from pool"); }
            try
            {
               socketWrapper.close();
            }
            catch (Exception e)
            {
               log.debug("Failed to close socket wrapper", e);
            }
         }
      }
      return null;
   }

   // Private --------------------------------------------------------------------------------------

   private Object versionedRead(InputStream inputStream, UnMarshaller unmarshaller, int version)
      throws IOException, ClassNotFoundException
   {
      //TODO: -TME - is switch required?
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            if (trace) { log.trace(this + " reading response from unmarshaller"); }
            if (unmarshaller instanceof VersionedUnMarshaller)
               return((VersionedUnMarshaller)unmarshaller).read(inputStream, null, version);
            else
               return unmarshaller.read(inputStream, null);
         }
         default:
         {
            throw new IOException("Can not read data for version " + version + ". " +
               "Supported versions: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);
         }
      }
   }

   private void versionedWrite(OutputStream outputStream, Marshaller marshaller,
                               Object invocation, int version) throws IOException
   {
      //TODO: -TME Should I worry about checking the version here?  Only one way to do it at this point
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            if (trace) { log.trace(this + " writing invocation to marshaller"); }
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(invocation, outputStream, version);
            else
               marshaller.write(invocation, outputStream);
            if (trace) { log.trace(this + " done writing invocation to marshaller"); }

            return;
         }
         default:
         {
            throw new IOException("Can not write data for version " + version + ".  " +
               "Supported versions: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);
         }
      }
   }

   //TODO: -TME Exact same method in ServerThread
   private int readVersion(InputStream inputStream) throws IOException
   {
      if (trace) { log.trace(this + " reading version from input stream"); }
      int version = inputStream.read();
      if (trace) { log.trace(this + " read version " + version + " from input stream"); }
      return version;
   }

   //TODO: -TME Exact same method in ServerThread
   private void writeVersion(OutputStream outputStream, int version) throws IOException
   {
      if (trace) { log.trace(this + " writing version " + version + " on output stream"); }
      outputStream.write(version);
   }

   // Inner classes --------------------------------------------------------------------------------

}
