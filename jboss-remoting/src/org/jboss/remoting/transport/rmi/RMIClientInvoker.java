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

import org.jboss.logging.Logger;
import org.jboss.remoting.AbstractInvoker;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.Version;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.MarshallerDecorator;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.rmi.RMIMarshaller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

/**
 * RMIClientInvoker
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@vocalocity.net">Tom Elrod</a>
 * @version $Revision: 1.9.2.1.4.1 $
 */
public class RMIClientInvoker extends RemoteClientInvoker
{
   private static final Logger log = Logger.getLogger(RMIClientInvoker.class);
   private static final boolean isTraceEnabled = log.isTraceEnabled();
   
   private RMIServerInvokerInf server;

   /**
    * Need flag to indicate if have been able to lookup registry and set stub.
    * Can't do this in the constructor, as need to throw CannotConnectException so
    * for clustering capability.
    *
    * @param locator
    */
   private boolean connected = false;

   public RMIClientInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public RMIClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }


   private int getRegistryPort(InvokerLocator locator)
   {
      int port = RMIServerInvoker.DEFAULT_REGISTRY_PORT;

      // See if locator contains a specific registry port
      Map params = locator.getParameters();
      if(params != null)
      {
         String value = (String) params.get(RMIServerInvoker.REGISTRY_PORT_KEY);
         if(value != null)
         {
            try
            {
               port = Integer.parseInt(value);
               log.debug("Using port " + port + " for rmi registry.");
            }
            catch(NumberFormatException e)
            {
               log.error("Can not set the RMIServerInvoker RMI registry to port " + value + ".  This is not a valid port number.");
            }
         }
      }
      return port;
   }

   /**
    * get the server stub
    *
    * @param server
    */
   public void setServerStub(RMIServerInvokerInf server)
   {
      this.server = server;
      log.trace(this.server);
   }

   /**
    * return the RMI server stub
    *
    * @return
    */
   public RMIServerInvokerInf getServerStub()
   {
      return this.server;
   }

   /**
    * subclasses must implement this method to provide a hook to connect to the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    *
    * @throws ConnectionFailedException
    */
   protected void handleConnect()
         throws ConnectionFailedException
   {
      //TODO: -TME Need to figure this out a little better as am now dealing with
      // with 2 ports, the rmi server and the registry.
      try
      {
         storeLocalConfig(configuration);
         String host = locator.getHost();
         int port = getRegistryPort(locator);
         Registry regsitry = LocateRegistry.getRegistry(host, port);
         Remote remoteObj = regsitry.lookup("remoting/RMIServerInvoker/" + locator.getPort());
         log.debug("Remote RMI Stub: " + remoteObj);
         setServerStub((RMIServerInvokerInf) remoteObj);
         connected = true;
      }
      catch(Exception e)
      {
         connected = false;
         log.debug("Error connecting RMI invoker client.", e);
         throw new CannotConnectException("Error connecting RMI invoker client", e);
      }
   }

   /**
    * subclasses must implement this method to provide a hook to disconnect from the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    */
   protected void handleDisconnect()
   {
   }

   protected String getDefaultDataType()
   {
      return RMIMarshaller.DATATYPE;
   }

   protected void storeLocalConfig(Map config)
   {
      HashMap localConfig = new HashMap(config);

      // If a specific SocketFactory was passed in, use it.  If a SocketFactory was
      // generated from SSL parameters, discard it.  It will be recreated later by
      // SerializableSSLClientSocketFactory with any additional parameters sent
      // from server.
      if (socketFactory != null &&
            !socketFactoryCreatedFromSSLParameters &&
            AbstractInvoker.isCompleteSocketFactory(socketFactory))
         localConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, socketFactory);

      // Save configuration for SerializableSSLClientSocketFactory.
      RemotingRMIClientSocketFactory.addLocalConfiguration(locator, localConfig);
   }
   
   protected Object transport(String sessionId, Object invocation, Map metadata, Marshaller marshaller, UnMarshaller unmarshaller)
         throws IOException, ConnectionFailedException
   {
      if(this.server == null)
      {
         log.error("Server stub has not been set in RMI invoker client.  See previous errors for details.");
         //throw new IOException("Server stub hasn't been set!");
         throw new CannotConnectException("Server stub has not been set.");
      }
      try
      {

         Object payload = invocation;
         if(marshaller != null && !(marshaller instanceof RMIMarshaller))
         {
            if(marshaller instanceof MarshallerDecorator)
            {
               payload = ((MarshallerDecorator) marshaller).addDecoration(payload);
            }
            else
            {
               ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
               if (marshaller instanceof VersionedMarshaller)
                  ((VersionedMarshaller) marshaller).write(payload, byteOut, Version.getDefaultVersion());
               else
                  marshaller.write(payload, byteOut);
               ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
               ObjectInputStream ois = new ObjectInputStream(byteIn);

               try
               {
                  byteOut.close();
                  payload = ois.readObject();
                  ois.close();
               }
               catch(ClassNotFoundException e)
               {
                  log.error("Could not marshall invocation payload object " + payload, e);
                  throw new IOException(e.getMessage());
               }
            }

         }

         Object response = server.transport(payload);

         /* Jira Issue: JBREM-167
         if(unmarshaller != null && !(unmarshaller instanceof RMIUnMarshaller))
         {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream();
            pos.connect(pis);
            ObjectOutputStream oos = new ObjectOutputStream(pos);
            oos.writeObject(response);
            oos.flush();
            oos.reset();
            oos.writeObject(Boolean.TRUE);
            oos.flush();
            oos.reset();
            try
            {
               oos.close();
               response = unmarshaller.read(pis, metadata);
               pis.close();
            }
            catch(ClassNotFoundException e)
            {
               log.error("Could not unmarshall invocation response" + response, e);
               throw new IOException(e.getMessage());
            }

         }
         */

         return response;
      }
      catch(RemoteException e)
      {
         log.error(e);
         throw new CannotConnectException("Error making invocation in RMI client invoker.", e);
      }
   }
}
