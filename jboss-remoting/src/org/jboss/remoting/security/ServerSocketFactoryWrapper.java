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
package org.jboss.remoting.security;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServerSocketFactoryWrapper extends ServerSocketFactory
{
   private ServerSocketFactoryMBean serverSocketFactory = null;

   public ServerSocketFactoryWrapper(ServerSocketFactoryMBean serverSocketFactory)
   {
      this.serverSocketFactory = serverSocketFactory;
   }

   public ServerSocket createServerSocket(int i) throws IOException
   {
      return serverSocketFactory.createServerSocket(i);
   }

   public ServerSocket createServerSocket(int i, int i1) throws IOException
   {
      return serverSocketFactory.createServerSocket(i, i1);
   }

   public ServerSocket createServerSocket(int i, int i1, InetAddress inetAddress) throws IOException
   {
      return serverSocketFactory.createServerSocket(i, i1, inetAddress);
   }

   public ServerSocket createServerSocket() throws IOException
   {
      return serverSocketFactory.createServerSocket();
   }

   public ServerSocketFactoryMBean getDelegate()
   {
      return serverSocketFactory;
   }

}