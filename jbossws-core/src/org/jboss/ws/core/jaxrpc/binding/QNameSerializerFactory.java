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
package org.jboss.ws.core.jaxrpc.binding;

import org.jboss.ws.core.binding.AbstractSerializerFactory;
import org.jboss.ws.core.binding.SerializerSupport;


// $Id: QNameSerializerFactory.java 3772 2007-07-01 19:29:13Z thomas.diesler@jboss.com $


/**
 * SerializerFactory for QName primitive
 *
 * @author Thomas.Diesler@jboss.org
 * @since 04-Dec-2004
 */
public class QNameSerializerFactory extends AbstractSerializerFactory
{
   public SerializerSupport getSerializer()
   {
      return new QNameSerializer();
   }
}
