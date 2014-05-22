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
package org.jboss.ws.core.jaxws;

import org.jboss.ws.WSException;
import org.jboss.ws.core.CommonMessageContext;
import org.jboss.ws.core.soap.MessageContextAssociation;
import org.jboss.wsf.spi.binding.BindingCustomization;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.Iterator;

/**
 * The default factory checks if a {@link JAXBBindingCustomization} exists
 * and uses it to customize the JAXBContext that will be created.
 *
 * @see org.jboss.wsf.spi.deployment.Endpoint
 * @see org.jboss.wsf.spi.binding.BindingCustomization
 * @see JAXBBindingCustomization
 *
 * @see JAXBContext#newInstance(Class...)
 * @see JAXBContext#newInstance(String, ClassLoader, java.util.Map<java.lang.String,?>)
 *
 * @author Heiko.Braun@jboss.com
 *         Created: Jun 26, 2007
 */
public class CustomizableJAXBContextFactory extends JAXBContextFactory
{
   public JAXBContext createContext(Class[] clazzes) throws WSException
   {
      try
      {
         BindingCustomization customization = getCustomization();
         if(null == customization)
            return JAXBContext.newInstance(clazzes);
         else
            return JAXBContext.newInstance(clazzes, customization);
      }
      catch (JAXBException e) {
         throw new WSException("Failed to create JAXBContext", e);
      }
   }

   public JAXBContext createContext(Class clazz) throws WSException
   {
      return createContext(new Class[] {clazz});
   }

   private BindingCustomization getCustomization()
   {
      BindingCustomization customization = null;

      CommonMessageContext msgContext = MessageContextAssociation.peekMessageContext();
      if(msgContext!=null) // may not be available anytime
      {
         Iterator<BindingCustomization> it = msgContext.getEndpointMetaData().getBindingCustomizations().iterator();
         while(it.hasNext())
         {
            BindingCustomization current = it.next();
            if(current instanceof JAXBBindingCustomization)
            {
               customization = current;
               break;
            }
         }
      }

      return customization;
   }
}
