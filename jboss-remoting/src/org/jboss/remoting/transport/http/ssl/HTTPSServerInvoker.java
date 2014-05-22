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

package org.jboss.remoting.transport.http.ssl;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.ServerSocketFactoryMBean;
import org.jboss.remoting.transport.http.HTTPServerInvoker;
import org.jboss.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Extension of the HTTPserverInvoker which uses a custom server socket
 * which is created using custom server socket factory that can support SSL.
 *
 * @deprecated This class has been replaced by org.jboss.remoting.transport.coyote.CoyoteInvoker, which is used
 * as the default server invoker for the http and https transport on the server side.  This class will be removed
 * from remoting distribution in a future release.
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPSServerInvoker extends HTTPServerInvoker
{
   private static final Logger log = Logger.getLogger(HTTPSServerInvoker.class);
   
   protected ServerSocketFactory serverSocketFactory = null;

   public HTTPSServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public HTTPSServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }
   
   protected ServerSocket createServerSocket(int serverBindPort, int backlog, InetAddress bindAddress) throws IOException
   {
      ServerSocket svrSocket = null;

      Map props = getConfiguration();

      /**
       * At this point in time, this can ONLY be set programmatically (via setServerSocketFactory),
       * so if it is set, then will use this.  Otherwise, will try config and then just use new ServerSocket
       * if all else failse.  This is due to JMX based DI (see more below for more).
       */
      if(serverSocketFactory != null)
      {
         svrSocket = serverSocketFactory.createServerSocket(serverBindPort, backlog, bindAddress);
      }
      else
      {

         /**
          * TODO: -TME This is another big hack because of dependancy on JMX within configuration.
          * Have to wait till the mbean server is set before can actually set the server socket
          * factory since it is an mbean (new server's DI will fix all this).  Would prefer this
          * to be in the setup() method...
          * Also, I can't cast the mbean proxy directly to ServerSocketFactory because it is not
          * an interface.  Therefore, have to require that ServerSocketFactoryMBean is used.
          */
         if(props.get("serverSocketFactory") != null)
         {
            try
            {
               String serverSocketFactoryString = (String) props.get("serverSocketFactory");
               if(serverSocketFactoryString != null)
               {
                  MBeanServer server = getMBeanServer();
                  ObjectName serverSocketFactoryObjName = new ObjectName(serverSocketFactoryString);
                  if(server != null)
                  {
                     try
                     {
                        ServerSocketFactoryMBean serverSocketFactory = (ServerSocketFactoryMBean) MBeanServerInvocationHandler.newProxyInstance(server,
                                                                                                                                                serverSocketFactoryObjName,
                                                                                                                                                ServerSocketFactoryMBean.class,
                                                                                                                                                false);
                        svrSocket = serverSocketFactory.createServerSocket(serverBindPort, backlog, bindAddress);
                     }
                     catch(Exception e)
                     {
                        log.error("Error creating mbean proxy for server socket factory for object name: " + serverSocketFactoryObjName, e);
                        throw new IOException("Error createing custom server socket factory.");
                     }
                  }
                  else
                  {
                     log.error("The 'serverSocketFactory' attribute was set with a value, but the MBeanServer reference is null.");
                     throw new IOException("Error creating custom server socket factory.  The invoker does not have a reference to the mbean server.");
                  }
               }
            }
            catch(MalformedObjectNameException e)
            {
               log.error("Error setting the server socket factory due to the attibute value passed not being a valid ObjectName.", e);
               throw new IOException("Error creating custom server socket factory.  The attributed value passed is not a valid object name.");
            }
            catch(NullPointerException e)
            {
               log.error("Error setting the server socket factory due to null pointer exception.", e);
               throw new IOException("Error creating custom server socket factory.");
            }
         }
      }

      if(svrSocket == null)
      {
         log.debug("Creating default server socket.");
         serverSocketFactory = SSLServerSocketFactory.getDefault();
         svrSocket = serverSocketFactory.createServerSocket(serverBindPort, backlog, bindAddress);
      }

      log.debug("Created server socket: " + svrSocket);

      return svrSocket;

   }

   /**
    * Sets the server socket factory for the SocketServerInvoker to use.
    * Can produce normal server socket or ssl server socket, depending
    * on implementation passed.
    *
    * @param serverSocketFactory
    */
   public void setServerSocketFactory(ServerSocketFactory serverSocketFactory)
   {
      this.serverSocketFactory = serverSocketFactory;
   }

}