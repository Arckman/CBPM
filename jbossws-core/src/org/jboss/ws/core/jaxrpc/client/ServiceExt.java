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
package org.jboss.ws.core.jaxrpc.client;

//$Id: ServiceExt.java 2209 2007-01-31 09:33:09Z thomas.diesler@jboss.com $

import javax.xml.rpc.Service;
import javax.xml.rpc.handler.HandlerRegistry;

/**
 * Extends the JAXRPC Service with JBoss propriatary behaviour
 *
 * @author Thomas.Diesler@jboss.org
 * @since 27-Jan-2005
 */
public interface ServiceExt extends Service
{
   /** 
    * Get a HandlerRegistry that can be used to dynamically 
    * change the client side handler chain
    */
   HandlerRegistry getDynamicHandlerRegistry();
}
