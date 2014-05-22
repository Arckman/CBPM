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
package org.jboss.ws.metadata.acessor;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.jboss.ws.WSException;
import org.jboss.ws.core.jaxws.JAXBContextFactory;
import org.jboss.ws.metadata.umdm.Accessor;
import org.jboss.ws.metadata.umdm.AccessorFactory;
import org.jboss.ws.metadata.umdm.AccessorFactoryCreator;
import org.jboss.ws.metadata.umdm.FaultMetaData;
import org.jboss.ws.metadata.umdm.ParameterMetaData;
import org.jboss.ws.metadata.umdm.WrappedParameter;

import com.sun.xml.bind.api.AccessorException;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.api.RawAccessor;

/**
 * A JAXB object accessor.
 *
 * @author <a href="jason.greene@jboss.com">Jason T. Greene</a>
 * @version $Revision: 3828 $
 */
@SuppressWarnings("unchecked")
public class JAXBAccessor implements Accessor
{
   private RawAccessor accessor;

   public static AccessorFactoryCreator FACTORY_CREATOR = new AccessorFactoryCreator() {

      public AccessorFactory create(ParameterMetaData parameter)
      {
         return create(parameter.getJavaType());
      }

      public AccessorFactory create(FaultMetaData fault)
      {
         return create(fault.getFaultBean());
      }

      private AccessorFactory create(final Class clazz)
      {         
         final JAXBRIContext ctx = (JAXBRIContext) JAXBContextFactory.newInstance().createContext(clazz);

         return new AccessorFactory()
         {
            public Accessor create(WrappedParameter parameter)
            {
               RawAccessor<Object, Object> accessor;
               try
               {
                  QName name = parameter.getName();
                  accessor = ctx.getElementPropertyAccessor(
                     clazz,
                     name.getNamespaceURI().intern(), // JAXB internally optimizes String usage towards intern()
                     name.getLocalPart().intern()     // see com.sun.xml.bind.v2.util.QNameMap;
                  );
               }
               catch (Throwable t)
               {
                  WSException ex = new WSException(t.getMessage());
                  ex.setStackTrace(t.getStackTrace());
                  throw ex;
               }

               if (accessor == null)
                  throw new IllegalStateException("Could not obtain accessor for parameter: " + parameter);

               return new JAXBAccessor(accessor);
            }
         };
      }
   };

   private JAXBAccessor(RawAccessor accessor)
   {
      this.accessor = accessor;
   }

   public Object get(Object bean)
   {
      try
      {
         return accessor.get(bean);
      }
      catch (AccessorException a)
      {
         WSException ex = new WSException(a.getMessage());
         ex.setStackTrace(a.getStackTrace());
         throw ex;
      }
   }

   public void set(Object bean, Object value)
   {
      try
      {
         accessor.set(bean, value);
      }
      catch (AccessorException a)
      {
         WSException ex = new WSException(a.getMessage());
         ex.setStackTrace(a.getStackTrace());
         throw ex;
      }
   }
}