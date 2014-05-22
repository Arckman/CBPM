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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jboss.ws.WSException;
import org.jboss.ws.metadata.umdm.Accessor;
import org.jboss.ws.metadata.umdm.AccessorFactory;
import org.jboss.ws.metadata.umdm.AccessorFactoryCreator;
import org.jboss.ws.metadata.umdm.FaultMetaData;
import org.jboss.ws.metadata.umdm.ParameterMetaData;
import org.jboss.ws.metadata.umdm.WrappedParameter;

/**
 * A simple Java field accessor that uses ordinary reflection.
 *
 * @author <a href="jason.greene@jboss.com">Jason T. Greene</a>
 * @version $Revision: 1760 $
 */
@SuppressWarnings("unchecked")
public class ReflectiveFieldAccessor implements Accessor
{
   private Field field;

   public static AccessorFactoryCreator FACTORY_CREATOR = new AccessorFactoryCreator()
   {
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
         return new AccessorFactory()
         {
            public Accessor create(WrappedParameter parameter)
            {
               String fieldName = parameter.getVariable();
               try
               {
                  Field field;

                  try
                  {
                     field = clazz.getField(fieldName);
                  }
                  catch (NoSuchFieldException e)
                  {
                     // Search for a private field
                     field = clazz.getDeclaredField(fieldName);
                     field.setAccessible(true);
                  }

                  if (Modifier.isStatic(field.getModifiers()))
                     throw new WSException("Field can not be static: " + fieldName);

                  return new ReflectiveFieldAccessor(field);
               }
               catch (Throwable t)
               {
                  WSException ex = new WSException("Error accessing field: " + fieldName + t.getClass().getSimpleName() + ": " + t.getMessage());
                  ex.setStackTrace(t.getStackTrace());
                  throw ex;
               }
            }
         };
      }
   };

   private ReflectiveFieldAccessor(Field field)
   {
      this.field = field;
   }

   public Object get(Object bean)
   {
      try
      {
         return field.get(bean);
      }
      catch (Throwable e)
      {
         WSException ex = new WSException(e.getMessage());
         ex.setStackTrace(ex.getStackTrace());
         throw ex;
      }
   }

   public void set(Object bean, Object value)
   {
      try
      {
         field.set(bean, value);
      }
      catch (Throwable e)
      {
         WSException ex = new WSException(e.getMessage());
         ex.setStackTrace(ex.getStackTrace());
         throw ex;
      }
   }
}