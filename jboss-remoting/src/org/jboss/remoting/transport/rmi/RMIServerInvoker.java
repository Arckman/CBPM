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

package org.jboss.remoting.transport.rmi;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.UnMarshallerDecorator;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.logging.Logger;

import javax.net.SocketFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

/**
 * RMIServerInvoker
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @version $Revision: 1.17.4.3 $
 */
public class RMIServerInvoker extends ServerInvoker implements RMIServerInvokerInf
{
   private static final Logger log = Logger.getLogger(RMIServerInvoker.class);

   private Remote stub;

   /**
    * Default for how many connections are queued.  Value is 200.
    */
   public static final int BACKLOG_DEFAULT = 200;

   /**
    * Default port on which rmi registry will be started.  Value is 3455.
    */
   public static final int DEFAULT_REGISTRY_PORT = 3455;

   /**
    * Key for port on which rmi registry should be started on.
    */
   public static final String REGISTRY_PORT_KEY = "registryPort";

   private Marshaller marshaller = null;
   private UnMarshaller unmarshaller = null;

   public RMIServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public void start() throws IOException
   {
      super.start();

      int bindPort = getServerBindPort();
      Registry registry = null;
      
      try
      {
         registry = getRegistry();
      }
      catch (Exception e)
      {
         throw new IOException(e.getMessage());
      }
//      stub = UnicastRemoteObject.exportObject(this, bindPort)

      String bindHost = getServerBindAddress();
      String clientConnectHost = getClientConnectAddress();

      if(clientConnectHost == null)
      {
         clientConnectHost = bindHost;
      }

      RMIServerSocketFactory ssf = new RemotingRMIServerSocketFactory(getServerSocketFactory(), BACKLOG_DEFAULT, bindHost, getTimeout());
      RMIClientSocketFactory csf = getRMIClientSocketFactory(clientConnectHost);
      stub = UnicastRemoteObject.exportObject(this, bindPort, csf, ssf);

      log.debug("Binding registry to " + "remoting/RMIServerInvoker/" + bindPort);
      registry.rebind("remoting/RMIServerInvoker/" + bindPort, this);

      unmarshaller = MarshalFactory.getUnMarshaller(getLocator(), this.getClass().getClassLoader());
      marshaller = MarshalFactory.getMarshaller(getLocator(), this.getClass().getClassLoader());
   }


   protected RMIClientSocketFactory getRMIClientSocketFactory(String clientConnectHost)
   {
      // Remove server side socket creation listeners.
      HashMap remoteConfig = new HashMap(configuration);
      remoteConfig.remove(Remoting.CUSTOM_SERVER_SOCKET_FACTORY);
      remoteConfig.remove(Remoting.SOCKET_CREATION_CLIENT_LISTENER);
      remoteConfig.remove(Remoting.SOCKET_CREATION_SERVER_LISTENER);
      return new RemotingRMIClientSocketFactory(locator, clientConnectHost, getTimeout(), remoteConfig);
   }
   
   protected SocketFactory getDefaultSocketFactory()
   {
//      return SocketFactory.getDefault();
      /**
       * Returning null because by default, this socket factory
       * will be need to be serialized when exported.  Since the
       * default factory implementation returned from SocketFactory.getDefault()
       * is not serializable, it will not work.  Therefore, if return null,
       * will delay the creation of the socket factory until the RMIClientSocketFactory is
       * exported.
       */
      return null;
   }


   public RMIServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   private Registry getRegistry() throws Exception
   {
      Registry registry = null;

      int port = DEFAULT_REGISTRY_PORT;

      // See if locator contains a specific registry port
      Map params = getConfiguration();
      if(params != null)
      {
         String value = (String) params.get(REGISTRY_PORT_KEY);
         if(value != null)
         {
            try
            {
               port = Integer.parseInt(value);
               log.debug("Using port " + port + " for rmi registry.");
            }
            catch(NumberFormatException e)
            {
               throw new Exception("Can not set the RMIServerInvoker RMI registry to port " + value + ".  This is not a valid port number.");
            }
         }
      }

      try
      {
         log.debug("Creating registry for " + port);

         registry = LocateRegistry.createRegistry(port);
      }
      catch(ExportException exportEx)
      {
         log.debug("Locating registry for " + port);

         // Probably means that the registry already exists, so just get it.
         registry = LocateRegistry.getRegistry(port);
      }
      if(log.isTraceEnabled())
      {
         log.trace("Got registry: " + registry);
      }
      return registry;
   }

   protected String getDefaultDataType()
   {
      return SerializableMarshaller.DATATYPE;
   }

   /**
    * destroy the RMI Server Invoker, which will unexport the RMI server
    */
   public void destroy()
   {
      super.destroy();
      try
      {
         try
         {
            log.debug("unbinding " + "remoting/RMIServerInvoker/" + locator.getPort() + " from registry running on port " + (locator.getPort() + 1));
            Registry registry = getRegistry();
            registry.unbind("remoting/RMIServerInvoker/" + locator.getPort());
         }
         catch(Exception e)
         {
            log.error("Error unbinding RMIServerInvoker from RMI registry.", e);
         }

         UnicastRemoteObject.unexportObject(this, true);

      }
      catch(java.rmi.NoSuchObjectException e)
      {

      }
   }

   protected void finalize() throws Throwable
   {
      destroy();
      super.finalize();
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

   public final Remote getStub()
   {
      return stub;
   }

   public Object transport(Object invocation)
         throws RemoteException, IOException
   {

      Object payload = invocation;
      if(unmarshaller != null)
      {
         if(unmarshaller instanceof UnMarshallerDecorator)
         {
            payload = ((UnMarshallerDecorator) unmarshaller).removeDecoration(payload);
         }
         else
         {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(byteOut);
            oos.writeObject(payload);
            oos.flush();

            ByteArrayInputStream is = new ByteArrayInputStream(byteOut.toByteArray());

            try
            {
               oos.close();
               payload = unmarshaller.read(is, null);
               is.close();
            }
            catch(ClassNotFoundException e)
            {
               log.error("Could not unmarshall invocation request" + payload, e);
               throw new IOException(e.getMessage());
            }
         }

      }

      Object response = invoke(payload);

      /* Jira Issue: JBREM-167
      if(marshaller != null)
      {
         PipedOutputStream pos = new PipedOutputStream();
         PipedInputStream pis = new PipedInputStream();
         pos.connect(pis);
         ObjectOutputStream oos = new ObjectOutputStream(pos);
         marshaller.write(response, oos);
         pos.flush();
         pos.write(new byte[0]);
         pos.flush();
         ObjectInputStream ois = new ObjectInputStream(pis);
         try
         {
            oos.close();
            response = ois.readObject();
            ois.close();
         }
         catch(ClassNotFoundException e)
         {
            log.error("Could not marshall invocation response object " + response, e);
            throw new IOException(e.getMessage());
         }
      }
      */

      return response;
   }
}
