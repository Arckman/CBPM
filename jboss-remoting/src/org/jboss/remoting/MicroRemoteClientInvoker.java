package org.jboss.remoting;

import org.jboss.logging.Logger;
import org.jboss.remoting.loading.ClassByteClassLoader;
import org.jboss.remoting.loading.RemotingClassLoader;
import org.jboss.remoting.marshal.InvalidMarshallingResource;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.util.id.GUID;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MicroRemoteClientInvoker is an abstract client part handler that implements the bulk of the heavy
 * lifting to process a remote method and dispatch it to a remote ServerInvoker and handle the result. <P>
 * <p/>
 * Specialized Client/Server Invokers might add additional functionality as part of the invocation - such as
 * delivering queued notifcations from a remote server by adding the notification objects during each invocation
 * to the invocation result payload and then having the client re-dispatch the notifications locally upon
 * receiving the return invocation result.
 *
 * The reason for the name micro is that this class contains only api that can be run within a J2ME envrionment.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 1.7.2.14.4.6 $
 */
public abstract class MicroRemoteClientInvoker extends AbstractInvoker implements ClientInvoker
{
   private static final Logger log = Logger.getLogger(MicroRemoteClientInvoker.class);
   private boolean trace = log.isTraceEnabled();

   protected boolean connected = false;
   private Marshaller marshaller;
   private UnMarshaller unmarshaller;
   private String dataType;
   private final Object clientLeaseLock = new Object();
   private LeasePinger leasePinger = null;
   private String invokerSessionID = new GUID().toString();

   public MicroRemoteClientInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public MicroRemoteClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   /**
    * Transport a request against a remote ServerInvoker.
    */
   public Object invoke(InvocationRequest invocationReq) throws Throwable
   {
      Object returnValue = null;
      int invokeCount = 0;

      if (trace) { log.trace(this + "(" + (++invokeCount) + ") invoking " + invocationReq); }

      Marshaller marshaller = getMarshaller();
      UnMarshaller unmarshaller = getUnMarshaller();

      if (marshaller == null)
      {
         // try by locator (in case marshaller class name specified)
         marshaller = MarshalFactory.getMarshaller(getLocator(), getClassLoader());
         if (marshaller == null)
         {
            // need to have a marshaller, so create a default one
            marshaller = MarshalFactory.getMarshaller(getDataType(), getSerializationType());
            if (marshaller == null)
            {
               // went as far as possible to find a marshaller, will have to give up
               throw new InvalidMarshallingResource(
                  "Can not find a valid marshaller for data type: " + getDataType());
            }
            setMarshaller(marshaller);
         }
      }

      if (unmarshaller == null)
      {
         // creating a new classloader containing the remoting class loader (for remote classloading)
         // and the current thread's class loader.  This allows to load remoting classes as well as
         // user's classes.
         ClassLoader remotingClassLoader =
            new RemotingClassLoader(getClassLoader(), Thread.currentThread().getContextClassLoader());
         
         // try by locator (in case unmarshaller class name specified)
         unmarshaller = MarshalFactory.getUnMarshaller(getLocator(), getClassLoader());
         if (unmarshaller == null)
         {
            unmarshaller = MarshalFactory.getUnMarshaller(getDataType(), getSerializationType());
            if (unmarshaller == null)
            {
               // went as far as possible to find a unmarshaller, will have to give up
               throw new InvalidMarshallingResource(
                  "Can not find a valid unmarshaller for data type: " + getDataType());
            }
            setUnMarshaller(unmarshaller);
         }
         unmarshaller.setClassLoader(remotingClassLoader);
      }

      // if raw, then send only param of invocation request
      Object payload = null;
      Map metadata = invocationReq.getRequestPayload();
      if (metadata != null && metadata.get(Client.RAW) != null)
      {
         payload = invocationReq.getParameter();
      }
      else
      {
         payload = invocationReq;
      }

      returnValue =
         transport(invocationReq.getSessionId(), payload, metadata, marshaller, unmarshaller);

      // Now check if is remoting response and process
      if (returnValue instanceof InvocationResponse)
      {
         InvocationResponse response = (InvocationResponse)returnValue;
         returnValue = response.getResult();

         // if is a server side exception, throw it
         if (response.isException())
         {
            Throwable e = (Throwable)returnValue;

            if (trace) { log.trace(this + " received a server-side exception as response to the invocation: " + e); }

            StackTraceElement[] serverStackTrace;
            if (e.getCause() != null)
            {
               serverStackTrace = e.getCause().getStackTrace();
               if (serverStackTrace == null || serverStackTrace.length == 0)
               {
                  serverStackTrace = e.getStackTrace();
               }
            }
            else
            {
               serverStackTrace = e.getStackTrace();
            }

            // need to check that there is a server stack trace.  If there is not, need to log
            // warning here so caller knows that error happened on server side and to look there,
            // as stack trace is just going to lead them to here, giving the impression that is
            // a client side exception from this point within remoting client.
            if (serverStackTrace == null || serverStackTrace.length == 0)
            {
               log.warn("An exception occurred on the server side when making remote invocation.  " +
                        "The exception returned from server does not include a stack trace.  " +
                        "Original server side exception message is " + e.getMessage(), e);
            }

            Exception clientException = new Exception();
            StackTraceElement[] clientStackTrace = clientException.getStackTrace();
            StackTraceElement[] completeStackTrace = new StackTraceElement[serverStackTrace.length + clientStackTrace.length];
            System.arraycopy(serverStackTrace, 0, completeStackTrace, 0, serverStackTrace.length);
            System.arraycopy(clientStackTrace, 0, completeStackTrace, serverStackTrace.length, clientStackTrace.length);

            if (e.getCause() != null)
            {
               e.getCause().setStackTrace(completeStackTrace);
            }
            else
            {
               e.setStackTrace(completeStackTrace);
            }

            throw e;
         }

         if (trace) { log.trace(this + " received InvocationResponse so going to return response's return value of " + returnValue);}

      }

      return returnValue;
   }

   /**
    * this method is called prior to making the remote invocation to allow the subclass the ability
    * to provide additional data or modify the invocation
    *
    * @param sessionId
    * @param param
    * @param sendPayload
    * @param receivedPayload
    */
   protected void preProcess(String sessionId, Object param, Map sendPayload, Map receivedPayload)
   {
   }

   /**
    * this method is called prior to returning the result for the invocation to allow the subclass the ability
    * to modify the result result
    *
    * @param sessionId
    * @param param
    * @param sendPayload
    * @param receivedPayload
    */
   protected void postProcess(String sessionId, Object param, Map sendPayload,
                              Map receivedPayload)
   {

   }

   protected abstract Object transport(String sessionId, Object invocation, Map metadata,
                                       Marshaller marshaller, UnMarshaller unmarshaller)
      throws IOException, ConnectionFailedException, ClassNotFoundException;

   /**
    * Subclasses must provide this method to return true if their remote connection is connected and
    * false if disconnected.  in some transports, such as SOAP, this method may always return true,
    * since the remote connectivity is done on demand and not kept persistent like other transports
    * (such as socket-based transport).
    *
    * @return boolean true if connected, false if not
    */
   public boolean isConnected()
   {
      return connected;
   }

   /**
    * Connect to the remote invoker.
    */
   public synchronized void connect() throws ConnectionFailedException
   {
      if (!connected)
      {
         log.debug(this + " connecting");

         handleConnect();
         connected = true;

         log.debug(this + " connected");
      }
   }

   /**
    * Subclasses must implement this method to provide a hook to connect to the remote server, if
    * this applies to the specific transport. However, in some transport implementations, this may
    * not make must difference since the connection is not persistent among invocations, such as
    * SOAP.  In these cases, the method should silently return without any processing.
    *
    * @throws ConnectionFailedException
    *
    */
   protected abstract void handleConnect() throws ConnectionFailedException;

   /**
    * Subclasses must implement this method to provide a hook to disconnect from the remote server,
    * if this applies to the specific transport. However, in some transport implementations, this
    * may not make must difference since the connection is not persistent among invocations, such as
    * SOAP. In these cases, the method should silently return without any processing.
    */
   protected abstract void handleDisconnect();

   /**
    * disconnect from the remote invokere
    */
   public synchronized void disconnect()
   {
      if (trace) { log.trace(this + " disconnecting ..."); }

      if (connected)
      {
         connected = false;
         handleDisconnect();
         ClassLoader classLoader = getClassLoader();
         if (classLoader != null && classLoader instanceof ClassByteClassLoader)
         {
            ((ClassByteClassLoader) classbyteloader).destroy();
         }
         if (trace) { log.trace(this + " disconnected"); }
      }
      else
      {
         if (trace) { log.trace(this + " is not connected!"); }
      }
   }

   public void setMarshaller(Marshaller marshaller)
   {
      this.marshaller = marshaller;
   }

   public Marshaller getMarshaller()
   {
      return this.marshaller;
   }

   public void setUnMarshaller(UnMarshaller unmarshaller)
   {
      this.unmarshaller = unmarshaller;
   }

   public UnMarshaller getUnMarshaller()
   {
      return this.unmarshaller;
   }

   public void terminateLease(String sessionId, int disconnectTimeout)
   {
      synchronized(clientLeaseLock)
      {
         if(leasePinger != null)
         {
            leasePinger.setDisconnectTimeout(disconnectTimeout);
            boolean isLastClientLease = leasePinger.removeClient(sessionId);
            if(isLastClientLease)
            {
               try
               {
                  leasePinger.stopPing();
               }
               catch (Exception e)
               {
                  log.error("error shutting down lease pinger");
               }
               leasePinger = null;
            }
         }
      }
   }

   public long getLeasePeriod(String sessionID)
   {
      synchronized(clientLeaseLock)
      {
         if(leasePinger == null)
         {
            return -1;
         }

         return leasePinger.getLeasePeriod(sessionID);
      }
   }

   public void establishLease(String clientSessionID, Map configuration, long leasePeriod)
      throws Throwable
   {
      synchronized (clientLeaseLock)
      {
         // if already have a lease pinger, then already have a client with an established
         // lease and just need to update the lease pinger
         if (leasePinger != null)
         {
            leasePinger.addClient(clientSessionID, configuration, leasePeriod);
            log.debug(this + " added client with session ID " + clientSessionID + " to the lease pinger");
            return;
         }

         try
         {
            if(trace) { log.trace(this + " sending initial lease ping to server to determine if server has leasing enabled."); }

            // configuration should NOT be passed as want ping to be specific to client invoker
            // and NOT to the client.

            InvocationRequest ir =
               new InvocationRequest(invokerSessionID, null, "$PING$", null, new HashMap(), null);

            Object ret = invoke(ir);

            if (ret instanceof InvocationResponse)
            {
               InvocationResponse resp = (InvocationResponse) ret;
               Boolean shouldLease = (Boolean)resp.getResult();

               if (shouldLease.booleanValue())
               {
                  long defaultLeasePeriod = LeasePinger.DEFAULT_LEASE_PERIOD;
                  Map respMap = resp.getPayload();

                  if (respMap != null)
                  {
                     Long leaseTimeoutValue = (Long)respMap.get("clientLeasePeriod");
                     long serverDefaultLeasePeriod = leaseTimeoutValue.longValue();
                     if(serverDefaultLeasePeriod > 0)
                     {
                        defaultLeasePeriod = serverDefaultLeasePeriod;
                     }
                  }

                  if(trace) { log.trace("server does have leasing enabled (with default lease period of " + defaultLeasePeriod + ") and will start a new lease pinger."); }

                  leasePinger = new LeasePinger(this, invokerSessionID, defaultLeasePeriod);
                  leasePinger.addClient(clientSessionID, configuration, leasePeriod);
                  leasePinger.startPing();
               }
            }
         }
         catch (Throwable throwable)
         {
            Exception e = new Exception("Error setting up client lease");
            e.initCause(throwable);
            throw e;
         }
      }
   }

   /**
    * Will get the data type for the marshaller factory so know which marshaller to
    * get to marshal the data.  Will first check the locator uri for a 'datatype'
    * parameter and take that value if it exists.  Otherwise, will use the
    * default datatype for the client invoker, based on transport.
    */
   private String getDataType()
   {
      if (dataType == null)
      {
         dataType = getDataType(getLocator());
         if (dataType == null)
         {
            dataType = getDefaultDataType();
         }
      }
      return dataType;
   }

   private String getDataType(InvokerLocator locator)
   {
      String type = null;

      if (locator != null)
      {
         Map params = locator.getParameters();
         if (params != null)
         {
            type = (String) params.get(InvokerLocator.DATATYPE);
            if (type == null)
            {
               type = (String) params.get(InvokerLocator.DATATYPE_CASED);
            }
         }
      }
      return type;
   }

   /**
    * Each implementation of the remote client invoker should have
    * a default data type that is uses in the case it is not specified
    * in the invoker locator uri.
    */
   protected abstract String getDefaultDataType();


   /**
    * Called by the garbage collector on an object when garbage collection
    * determines that there are no more references to the object.
    * A subclass overrides the <code>finalize</code> method to dispose of
    * system resources or to perform other cleanup.
    * <p/>
    * The general contract of <tt>finalize</tt> is that it is invoked
    * if and when the Java<font size="-2"><sup>TM</sup></font> virtual
    * machine has determined that there is no longer any
    * means by which this object can be accessed by any thread that has
    * not yet died, except as a result of an action taken by the
    * finalization of some other object or class which is ready to be
    * finalized. The <tt>finalize</tt> method may take any action, including
    * making this object available again to other threads; the usual purpose
    * of <tt>finalize</tt>, however, is to perform cleanup actions before
    * the object is irrevocably discarded. For example, the finalize method
    * for an object that represents an input/output connection might perform
    * explicit I/O transactions to break the connection before the object is
    * permanently discarded.
    * <p/>
    * The <tt>finalize</tt> method of class <tt>Object</tt> performs no
    * special action; it simply returns normally. Subclasses of
    * <tt>Object</tt> may override this definition.
    * <p/>
    * The Java programming language does not guarantee which thread will
    * transport the <tt>finalize</tt> method for any given object. It is
    * guaranteed, however, that the thread that invokes finalize will not
    * be holding any user-visible synchronization locks when finalize is
    * invoked. If an uncaught exception is thrown by the finalize method,
    * the exception is ignored and finalization of that object terminates.
    * <p/>
    * After the <tt>finalize</tt> method has been invoked for an object, no
    * further action is taken until the Java virtual machine has again
    * determined that there is no longer any means by which this object can
    * be accessed by any thread that has not yet died, including possible
    * actions by other objects or classes which are ready to be finalized,
    * at which point the object may be discarded.
    * <p/>
    * The <tt>finalize</tt> method is never invoked more than once by a Java
    * virtual machine for any given object.
    * <p/>
    * Any exception thrown by the <code>finalize</code> method causes
    * the finalization of this object to be halted, but is otherwise
    * ignored.
    *
    * @throws Throwable the <code>Exception</code> raised by this method
    */
   protected void finalize() throws Throwable
   {
      disconnect();
      super.finalize();
   }

}
