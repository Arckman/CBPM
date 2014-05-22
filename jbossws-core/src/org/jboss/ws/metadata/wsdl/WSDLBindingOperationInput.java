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
package org.jboss.ws.metadata.wsdl;

import java.io.Serializable;

// $Id: WSDLBindingOperationInput.java 1757 2006-12-22 15:40:24Z thomas.diesler@jboss.com $

/**
 * A Binding Message Reference component describes a concrete binding of a particular message
 * participating in an operation to a particular concrete message format.
 *
 * @author Thomas.Diesler@jboss.org
 * @author <a href="mailto:jason.greene@jboss.com">Jason T. Greene</a>
 * @since 10-Oct-2004
 */
public class WSDLBindingOperationInput extends WSDLBindingMessageReference implements Serializable
{
   private static final long serialVersionUID = -3945310906418557565L;

   public WSDLBindingOperationInput(WSDLBindingOperation wsdlBindingOperation)
   {
      super(wsdlBindingOperation);
   }
}
