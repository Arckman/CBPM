/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Axis" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.jboss.ws.core.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

import javax.xml.namespace.QName;
import javax.xml.rpc.holders.BigDecimalHolder;
import javax.xml.rpc.holders.BigIntegerHolder;
import javax.xml.rpc.holders.BooleanHolder;
import javax.xml.rpc.holders.BooleanWrapperHolder;
import javax.xml.rpc.holders.ByteArrayHolder;
import javax.xml.rpc.holders.ByteHolder;
import javax.xml.rpc.holders.ByteWrapperHolder;
import javax.xml.rpc.holders.CalendarHolder;
import javax.xml.rpc.holders.DoubleHolder;
import javax.xml.rpc.holders.DoubleWrapperHolder;
import javax.xml.rpc.holders.FloatHolder;
import javax.xml.rpc.holders.FloatWrapperHolder;
import javax.xml.rpc.holders.IntHolder;
import javax.xml.rpc.holders.IntegerWrapperHolder;
import javax.xml.rpc.holders.LongHolder;
import javax.xml.rpc.holders.LongWrapperHolder;
import javax.xml.rpc.holders.ObjectHolder;
import javax.xml.rpc.holders.QNameHolder;
import javax.xml.rpc.holders.ShortHolder;
import javax.xml.rpc.holders.ShortWrapperHolder;
import javax.xml.rpc.holders.StringHolder;

import org.jboss.logging.Logger;
import org.jboss.wsf.common.JavaUtils;

/**
 * HolderUtils provides static utility functions for both JAX-RPC
 * and JAX-WS holders.
 *
 * @author Thomas.Diesler@jboss.org
 * @author <a href="jason.greene@jboss.com">Jason T. Greene</a>
 * @since 22-Dec-2004
 */
public class HolderUtils
{
   private static final Logger log = Logger.getLogger(HolderUtils.class);

   /** True if the given type is a holder. */
   public static boolean isHolderType(Class javaType)
   {
      return javax.xml.rpc.holders.Holder.class.isAssignableFrom(javaType) || javax.xml.ws.Holder.class.isAssignableFrom(javaType);
   }

   /** True if the given type is a holder. */
   public static boolean isHolderType(Type javaType)
   {
      return isHolderType(JavaUtils.erasure(javaType));
   }

   /**
    * Gets the JAX-RPC holder for a specific value type.
    *
    * @param valueType the value
    * @return the holder, or null if there is no match
    */
   public static Class getJAXRPCHolderType(Class valueType)
   {
      if (valueType == null)
         throw new IllegalArgumentException("Illegal null parameter");

      if (javax.xml.rpc.holders.Holder.class.isAssignableFrom(valueType))
         throw new IllegalArgumentException("Is already a holder: " + valueType.getName());

      if (valueType == BigDecimal.class)
         return BigDecimalHolder.class;
      if (valueType == BigInteger.class)
         return BigIntegerHolder.class;
      if (valueType == boolean.class)
         return BooleanHolder.class;
      if (valueType == Boolean.class)
         return BooleanWrapperHolder.class;
      if (valueType == byte[].class)
         return ByteArrayHolder.class;
      if (valueType == byte.class)
         return ByteHolder.class;
      if (valueType == Byte.class)
         return ByteWrapperHolder.class;
      if (valueType == Calendar.class)
         return CalendarHolder.class;
      if (valueType == double.class)
         return DoubleHolder.class;
      if (valueType == Double.class)
         return DoubleWrapperHolder.class;
      if (valueType == float.class)
         return FloatHolder.class;
      if (valueType == Float.class)
         return FloatWrapperHolder.class;
      if (valueType == int.class)
         return IntHolder.class;
      if (valueType == Integer.class)
         return IntegerWrapperHolder.class;
      if (valueType == long.class)
         return LongHolder.class;
      if (valueType == Long.class)
         return LongWrapperHolder.class;
      if (valueType == QName.class)
         return QNameHolder.class;
      if (valueType == short.class)
         return ShortHolder.class;
      if (valueType == Short.class)
         return ShortWrapperHolder.class;
      if (valueType == String.class)
         return StringHolder.class;
      if (valueType == Object.class)
         return ObjectHolder.class;

      log.warn("Cannot get holder type for: " + valueType);

      return null;
   }

   /**
    * Gets the value type of a JAX-WS or JAX-RPC holder.
    *
    * @param holderType the generic type for JAX-WS, a standard class for JAX-RPC
    * @return the value type
    */
   public static Class getValueType(Type holderType)
   {
      Class holderClass = JavaUtils.erasure(holderType);

      boolean jaxrpcHolder = javax.xml.rpc.holders.Holder.class.isAssignableFrom(holderClass);
      boolean jaxwsHolder = javax.xml.ws.Holder.class.isAssignableFrom(holderClass);
      if (!jaxrpcHolder && !jaxwsHolder)
         throw new IllegalArgumentException("Is not a holder: " + holderClass.getName());

      if (jaxwsHolder)
         return JavaUtils.erasure(getGenericValueType(holderType));

      // Holder is supposed to have a public value field.
      Field field;
      try
      {
         field = holderClass.getField("value");
      }
      catch (NoSuchFieldException e)
      {
         throw new IllegalArgumentException("Cannot find public value field: " + holderClass);
      }

      return field.getType();
   }

   /**
    * Gets the value type of a JAX-RPC holder. Note this method should not be used 
    * for JAX-WS, as a JAX-WS holder requires generic info. Instead, use the Type 
    * version.
    *
    * @param holderType the generic type for JAX-WS, a standard class for JAX-RPC
    * @return the value type
    */
   public static Class getValueType(Class holderClass)
   {
      boolean jaxrpcHolder = javax.xml.rpc.holders.Holder.class.isAssignableFrom(holderClass);
      boolean jaxwsHolder = javax.xml.ws.Holder.class.isAssignableFrom(holderClass);
      if (!jaxrpcHolder && !jaxwsHolder)
         throw new IllegalArgumentException("Is not a holder: " + holderClass.getName());

      // No generic info
      if (jaxwsHolder)
         return Object.class;

      // Holder is supposed to have a public value field.
      Field field;
      try
      {
         field = holderClass.getField("value");
      }
      catch (NoSuchFieldException e)
      {
         throw new IllegalArgumentException("Cannot find public value field: " + holderClass);
      }

      return field.getType();
   }

   /**
    * Gets the value object of a JAX-WS or JAX-RPC holder instance.
    *
    * @param holder the holder object instance
    * @return the value object instance
    */
   public static Object getHolderValue(Object holder)
   {
      if (holder == null)
         throw new IllegalArgumentException("Illegal null parameter");

      if (!javax.xml.rpc.holders.Holder.class.isInstance(holder) && !javax.xml.ws.Holder.class.isInstance(holder))
         throw new IllegalArgumentException("Is not a holder: " + holder);

      try
      {
         Field valueField = holder.getClass().getField("value");
         Object obj = valueField.get(holder);
         return obj;
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception e)
      {
         throw new IllegalArgumentException("Cannot access public value field: " + holder);
      }
   }

   /**
    * Sets the value object of a JAX-WS or JAX-RPC holder instance. This method
    * will also dynamically convert primitive and wrapper arrays to match the
    * target array type.
    *
    * @param holder the holder instance
    * @param value the value, can be null
    */
   public static void setHolderValue(Object holder, Object value)
   {
      if (holder == null)
         throw new IllegalArgumentException("Holder instance was null");

      if (!javax.xml.rpc.holders.Holder.class.isInstance(holder) && !javax.xml.ws.Holder.class.isInstance(holder))
         throw new IllegalArgumentException("Is not a holder: " + holder);

      Class valueType = getValueType(holder.getClass());

      if (value != null && JavaUtils.isAssignableFrom(valueType, value.getClass()) == false)
         throw new IllegalArgumentException("Holder [" + holder.getClass().getName() + "] value not assignable: " + value);

      if (valueType.isArray())
         value = JavaUtils.syncArray(value, valueType);

      try
      {
         Field valueField = holder.getClass().getField("value");
         if (value != null || valueType.isPrimitive() == false)
            valueField.set(holder, value);
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception e)
      {
         throw new IllegalArgumentException("Cannot access public value field: " + holder);
      }
   }

   /**
    * Gets the generic value type of a JAX-WS Holder.
    * If there is no generic information, Object.class will be returned
    *
    * @param holder JAX-WS holder type
    * @return generic value type
    */
   public static Type getGenericValueType(Type holder)
   {
      // For some reason the JDK 4 bytecode verifier trips up on this function if you use the ternary operator
      // The only difference between it and the working form here is the use of a goto instruction. JDK bug perhaps?
      if (holder instanceof ParameterizedType)
        return ((ParameterizedType)holder).getActualTypeArguments()[0];

      return Object.class;
   }


   /**
    * Creates a JAX-WS or JAX-RPC holder instance.
    *
    * @param value the value instance
    * @param holderType the holder type
    * @return a new holder
    */
   public static Object createHolderInstance(Object value, Class<?> holderType)
   {
      if (! isHolderType(holderType))
         throw new IllegalArgumentException("Not a holder type:" + holderType.getName());

      Object holder;

      try
      {
         holder = holderType.newInstance();
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception e)
      {
         throw new IllegalArgumentException("Cannot instanciate holder: " + holderType);
      }

      setHolderValue(holder, value);

      return holder;
   }
}