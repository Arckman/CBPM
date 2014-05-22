/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
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

package org.jboss.remoting;

public class Remoting
{
   private Remoting() {}
   

   /**
    * Key for the configuration map passed to a Connector to indicate the server
    * socket factory to be used.  This will override the creation of any other socket factory.
    */
   public static final String CUSTOM_SERVER_SOCKET_FACTORY = "customServerSocketFactory";
   

   /**
    * Key for the configuration map passed to a Client to indicate the socket factory to
    * be used.  This will override the creation of any other socket factory.
    */
   public static final String CUSTOM_SOCKET_FACTORY = "customSocketFactory";
   
   /**
    * Key for the configuration map passed to a Client to indicate the classname of
    * the socket factory to be used.
    */
   public static final String SOCKET_FACTORY_NAME = "socketFactory";
   
   /**
    * Key for the configuration map passed to a Client or Connector to indicate
    * a socket creation listener for sockets created by a SocketFactory.
    */
   public static final String SOCKET_CREATION_CLIENT_LISTENER = "socketCreationClientListener";
   
   /**
    * Key for the configuration map passed to a Client or Connector to indicate
    * a socket creation listener for sockets created by a ServerSocket.
    */
   public static final String SOCKET_CREATION_SERVER_LISTENER = "socketCreationServerListener";
}
