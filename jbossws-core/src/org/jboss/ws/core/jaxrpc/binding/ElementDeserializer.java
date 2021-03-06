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

// $Id: ElementDeserializer.java 3959 2007-07-20 14:44:19Z heiko.braun@jboss.com $

import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.jboss.logging.Logger;
import org.jboss.ws.core.binding.BindingException;
import org.jboss.ws.core.binding.DeserializerSupport;
import org.jboss.ws.core.binding.SerializationContext;
import org.jboss.wsf.common.DOMUtils;
import org.w3c.dom.Element;

/**
 * A deserializer that can handle xsd:anyType
 *
 * @author Thomas.Diesler@jboss.org
 * @since 23-Jun-2005
 */
public class ElementDeserializer extends DeserializerSupport
{
   // provide logging
   private static final Logger log = Logger.getLogger(ElementDeserializer.class);

   public Object deserialize(QName xmlName, QName xmlType, Source xmlFragment, SerializationContext serContext) throws BindingException {
      return deserialize(xmlName, xmlType, sourceToString(xmlFragment), serContext);
   }

   /** Deserialize the given simple xmlString
    */
   private Object deserialize(QName xmlName, QName xmlType, String xmlFragment, SerializationContext serContext) throws BindingException
   {
      if(log.isDebugEnabled()) log.debug("deserialize: [xmlName=" + xmlName + ",xmlType=" + xmlType + "]");
      try
      {
         // TODO: Better way for DOM to source conversion
         Element domElement = DOMUtils.parse(xmlFragment);
         return domElement;
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         throw new BindingException();
      }
   }
}
