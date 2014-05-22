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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMIClientSocketFactory;
import java.util.HashMap;
import java.util.Map;

import javax.net.SocketFactory;

import org.jboss.logging.Logger;
import org.jboss.remoting.AbstractInvoker;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.Version;


/**
 * <code>RemotingRMIClientSocketFactory</code> provides two services to <code>RMIServerInvoker</code>.
 * <ol>
 * <li>It can be parameterized by a host name, allowing <code>RMIServerInvoker</code> to supply RMI
 * with a factory which creates sockets connected to a specified host name.
 * <p/>
 * <li>It can be parameterized by a <code>SocketFactory</code> allowing <code>RMIServerInvoker</code>
 * to supply RMI with a factory facility which creates specialized sockets.
 * </ol>
 * <p/>
 * if the <code>SocketFactory</code> parameter is specified, then the <code>RemotingRMIClientSocketFactory</code>
 * should be used with a matching instance of <code>RemotingRMIServerSocketFactory</code> with a compatible
 * <code>ServerSocketFactory</code>.
 * <p/>
 * If the <code>SocketFactory</code> parameter is not specified, an instance of <code>java.net.Socket</code>
 * will be created by default.
 * <p/>
 * Although there is no apparent need for the host name parameter, since the <code>createSocket()</code>
 * method receives a host name, it seems that for a server object bound to localhost, the RMI runtime will
 * pass to <code>createSocket()</code> one of the IP addresses for which the host is configured (other than
 * 127.0.0.1), resulting in a failure to retrieve the object from the Registry.  If a host name is passed to
 * a <code>RemotingRMIClientFactory</code> constructor, it will override the host name passed to
 * <code>createSocket()</code>  In particular, parameterizing <code>RemotingRMIClientSocketFactory</code>
 * with localhost will allow the retrieval of objects bound to localhost.
 *
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 1.9.4.4.4.1 $
 *          <p/>
 *          Copyright (c) 2005
 *          </p>
 */

public class RemotingRMIClientSocketFactory implements RMIClientSocketFactory, Serializable
{
   static final long serialVersionUID;

   protected static Logger log = Logger.getLogger(RemotingRMIClientSocketFactory.class);
   protected static HashMap configMaps = new HashMap();
   protected static Map socketFactories = new HashMap();

   protected Map configuration;
   protected InvokerLocator invokerLocator;
   
   transient protected SocketFactory socketFactory;
   private int timeout = 60000;

   // The commented code below is from an attempt to incorporate a <code>java.lang.reflect.Constructor</code>
   // parameter to provide a very general way to create sockets.  The problem is that
   // <code>Constructor</code> does not implement <code>Serializable</code>, which is necessary to
   // allow the <code>RemotingRMIClientSocketFactory</code> to be transmitted to the client.  The
   // code is left in place because it could be resurrected by passing in a class name and parameter
   // types to specify a constructor.  Fortunately, <code>java.lang.Class</code> does implement
   // <code>Serializable</code>.

//   private Constructor constructor;
//   private Object[] args;
//   private int hostPosition;
//   private int portPosition;

   protected String hostName;

   static
   {
      if(Version.getDefaultVersion() == Version.VERSION_1)
      {
         serialVersionUID = -7491556589517716155L;
      }
      else
      {
         serialVersionUID = -3039839695840773968L;
      }
   }

   
   public static void addLocalConfiguration(InvokerLocator invokerLocator, Map localConfig)
   {
      log.debug("adding local configuration for: " + invokerLocator);
      configMaps.put(new ComparableHolder(invokerLocator), localConfig);
   }


   /**
    * @param locator
    * @param hostName
    * @param timeout
    * @param config
    */
   public RemotingRMIClientSocketFactory(InvokerLocator locator,
                                         String hostName,
                                         int timeout,
                                         Map config)
   {
      this.invokerLocator = locator;
      this.hostName = hostName;
      this.timeout = timeout;
      this.configuration = new HashMap(config);
   }


//   public RemotingRMISocketFactory(Constructor constructor, Object[] args, int hostPosition, int portPosition)
//   throws ClassNotFoundException, NoSuchMethodException
//   {
//      this.constructor = constructor;
//      this.args = args;
//      this.portPosition = portPosition;
//   }


   /**
    * Creates a new socket.  If a <code>SocketFactory</code> was passed to the constructor, it will be used.
    * Otherwise, a <code>java.net.Socket</code> will be created by default.
    *
    * @param host host to which socket should be connected
    * @param port port to which socket should be connected
    * @return new socket
    * @throws IOException if there is a problem creating a socket
    */
   public Socket createSocket(String host, int port) throws IOException
   {
      // If invokerLocator isn't in configMaps, an RMICLientInvoker has not been created
      // yet.  This call was probably made by an RMI thread, and is premature.  Best attempt
      // is to return a vanilla socket.
      
      // Note: the previous observation might be incorrect.  The phenomenon of missing
      // entries in configMaps might simply have been an error in the definition of
      // ComparableHolder.equals.  See JBREM-697.

      if (invokerLocator != null)
      {
         ComparableHolder holder = new ComparableHolder(invokerLocator);
         if (!configMaps.containsKey(holder))
         {
            log.warn("unrecognized invoker locator: " + invokerLocator);
            log.warn("unable to retrieve socket factory: returning plain socket");
            return new Socket(host, port);
         }
         socketFactory = retrieveSocketFactory(holder);
      }

      String effectiveHost = hostName != null ? hostName : host;
      Socket socket = null;
      
      if(socketFactory != null)
      {
         socket = socketFactory.createSocket(effectiveHost, port);
      }

//      if (constructor != null)
//      {
//         if (hostPosition != -1)
//            args[hostPosition] = host;
//
//         if (portPosition != -1)
//            args[portPosition] = new Integer(port);
//
//         try
//         {
//            return (Socket) constructor.newInstance(args);
//         }
//         catch (Exception e)
//         {
//            throw new IOException(e.getMessage());
//         }
//      }

      else
      {
         socket = new Socket(effectiveHost, port);
      }

      socket.setSoTimeout(timeout);
      socketFactory = null;
      return socket;
   }


   public SocketFactory retrieveSocketFactory(ComparableHolder holder)
      throws IOException
   {
      SocketFactory sf = (SocketFactory) socketFactories.get(this);
      if (sf == null)
      {
         // We want to keep the local configuration map, which might contain a
         // SocketFactory, separate from the configuration map, which is meant
         // to contain only serializable objects.
         Map tempConfig = new HashMap(configuration);
         Map localConfig = (Map) configMaps.get(holder);
         if (localConfig != null)
            tempConfig.putAll(localConfig);

         if (tempConfig.containsKey(Remoting.CUSTOM_SOCKET_FACTORY))
         {
            sf = (SocketFactory) tempConfig.get(Remoting.CUSTOM_SOCKET_FACTORY);
         }
         
         if (sf == null)
         {
            sf = SocketFactory.getDefault();
            sf = AbstractInvoker.wrapSocketFactory(sf, tempConfig);
         }
         
         socketFactories.put(this, sf);
      }

      return sf;
   }
   

   protected static class ComparableHolder
   {
      private String protocol;
      private InetAddress host;
      private int port;
      private int hashCode;

      public ComparableHolder(InvokerLocator invokerLocator)
      {
         protocol = invokerLocator.getProtocol().toLowerCase();
         
         try
         {
            host = InetAddress.getByName(invokerLocator.getHost());
         }
         catch (UnknownHostException e)
         {
            log.error("unable to resolve host: " + invokerLocator.getHost());
         }
         
         port = invokerLocator.getPort();
         hashCode = protocol.hashCode() * host.hashCode() * port;
      }

      public boolean equals(Object obj)
      {
         if (obj == null || !(obj instanceof ComparableHolder))
            return false;

         ComparableHolder holder = (ComparableHolder) obj;

         return protocol.equals(holder.protocol.toLowerCase())
                && host.equals(holder.host)
                && port == holder.port;
      }

      public int hashCode()
      {
         return hashCode;
      }
   }
}