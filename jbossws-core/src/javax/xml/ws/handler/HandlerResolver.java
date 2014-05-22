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
package javax.xml.ws.handler;

// $Id: HandlerResolver.java 2897 2007-04-23 06:12:12Z thomas.diesler@jboss.com $

/** 
 *  <code>HandlerResolver</code> is an interface implemented
 *  by an application to get control over the handler chain
 *  set on proxy/dispatch objects at the time of their creation.
 *  <p>
 *  A <code>HandlerResolver</code> may be set on a <code>Service</code>
 *  using the <code>setHandlerResolver</code> method.
 * <p>
 *  When the runtime invokes a <code>HandlerResolver</code>, it will
 *  pass it a <code>PortInfo</code> object containing information
 *  about the port that the proxy/dispatch object will be accessing.
 *
 *  @see javax.xml.ws.Service#setHandlerResolver
 *
 *  @since JAX-WS 2.0
**/
public interface HandlerResolver {

  /** 
   *  Gets the handler chain for the specified port.
   *
   *  @param portInfo Contains information about the port being accessed.
   *  @return java.util.List Handler chain
  **/
  public java.util.List<Handler> getHandlerChain(PortInfo portInfo);
}
