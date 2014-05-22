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
package javax.xml.ws;

// $Id: Response.java 2897 2007-04-23 06:12:12Z thomas.diesler@jboss.com $

import java.util.Map;
import java.util.concurrent.Future;

/** The <code>Response</code> interface provides methods used to obtain the  
 *  payload and context of a message sent in response to an operation
 *  invocation.
 *
 *  <p>For asynchronous operation invocations it provides additional methods
 *  to check the status of the request. The <code>get(...)</code> methods may
 *  throw the standard
 *  set of exceptions and their cause may be a RemoteException or a  
 *  WebServiceException that represents the error that occured during the
 *  asynchronous method invocation.</p>
 *
 *  @since JAX-WS 2.0
 **/
public interface Response<T> extends Future<T>
{
   /** Gets the contained response context.
    *
    * @return The contained response context. May be <code>null</code> if a
    * response is not yet available.
    *
    **/
   Map<String, Object> getContext();
}
