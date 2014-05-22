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

package org.jboss.remoting.transport.sslbisocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.socketfactory.SocketFactoryWrapper;
import org.jboss.remoting.transport.bisocket.BisocketClientInvoker;
import org.jboss.remoting.util.socket.HandshakeRepeater;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 *  * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketClientInvoker extends BisocketClientInvoker
{
   private static final Logger log = Logger.getLogger(SSLBisocketClientInvoker.class);

   public SSLBisocketClientInvoker(InvokerLocator locator) throws IOException
   {
      super(locator);
      try
      {
         setup();
      }
      catch (Exception ex)
      {
         log.error("Error setting up ssl bisocket client invoker.", ex);
         throw new RuntimeException(ex.getMessage());
      }
   }

   public SSLBisocketClientInvoker(InvokerLocator locator, Map configuration) throws IOException
   {
      super(locator, configuration);
      try
      {
         setup();
      }
      catch (Exception ex)
      {
         log.error("Error setting up ssl bisocket client invoker.", ex);
         throw new RuntimeException(ex.getMessage());
      }
   }

   protected SocketFactory createSocketFactory(Map configuration)
   {
      SocketFactory sf = super.createSocketFactory(configuration);

      if (isCompleteSocketFactory(sf))
         return sf;
      
      SocketFactory wrapper = sf;

      try
      {
         SSLSocketBuilder server = new SSLSocketBuilder(configuration);
         sf = server.createSSLSocketFactory();
      }
      catch (Exception e)
      {
         log.error("Error creating SSL Socket Factory for client invoker.", e);
      }

      if (wrapper != null)
      {
         ((SocketFactoryWrapper) wrapper).setSocketFactory(sf);
         return wrapper;
      }
      
      return sf;
   }

   
   
   protected Socket createSocket(String address, int port, int timeout) throws IOException
   {
      if (isCallbackInvoker)
         return super.createSocket(address, port, timeout);
      
      SocketFactory sf = getSocketFactory();

      if (sf == null)
         sf = createSocketFactory(configuration);

      Socket s = sf.createSocket();

      s.setReuseAddress(getReuseAddress());
      InetSocketAddress inetAddr = new InetSocketAddress(address, port);
      
      if (timeout < 0)
      {
         timeout = getTimeout();
         if (timeout < 0)
            timeout = 0;
      }
      
      s.connect(inetAddr, timeout);
      
      if (s instanceof SSLSocket)
      {
         // need to check for handshake listener and add them if there is one
         Object obj = configuration.get(Client.HANDSHAKE_COMPLETED_LISTENER);
         if (obj != null && obj instanceof HandshakeCompletedListener)
         {
            SSLSocket sslSocket = (SSLSocket) s;
            HandshakeCompletedListener listener = (HandshakeCompletedListener) obj;
            establishHandshake(sslSocket, listener);
         }
      }

      return s;
   }

   private void establishHandshake(SSLSocket sslSocket, HandshakeCompletedListener listener)
         throws IOException
   {
      HandshakeRepeater repeater = new HandshakeRepeater(listener);
      sslSocket.addHandshakeCompletedListener(repeater);
      sslSocket.getSession();
      repeater.waitForHandshake();
   }
}