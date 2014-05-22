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

// $Id: AsyncHandler.java 2897 2007-04-23 06:12:12Z thomas.diesler@jboss.com $

/** The <code>AsyncHandler</code> interface is implemented by
 * clients that wish to receive callback notification of the completion of
 * service endpoint operations invoked asynchronously.
 *
 *  @since JAX-WS 2.0
 **/
public interface AsyncHandler<T>
{

   /** Called when the response to an asynchronous operation is available.
    *
    * @param res The response to the operation invocation.
    *
    **/
   void handleResponse(Response<T> res);
}
