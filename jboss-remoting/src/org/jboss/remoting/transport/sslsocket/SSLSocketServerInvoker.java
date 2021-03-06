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

package org.jboss.remoting.transport.sslsocket;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.socket.SocketServerInvoker;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLSocketServerInvoker extends SocketServerInvoker implements SSLSocketServerInvokerMBean
{

   public SSLSocketServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public SSLSocketServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   protected ServerSocketFactory getDefaultServerSocketFactory()
   {
      return SSLServerSocketFactory.getDefault();
   }

   protected ServerSocket createServerSocket(int serverBindPort, int backlog, InetAddress bindAddress) throws IOException
   {
      ServerSocket svrSocket = getServerSocketFactory().createServerSocket(serverBindPort, backlog, bindAddress);
      return svrSocket;
   }

}