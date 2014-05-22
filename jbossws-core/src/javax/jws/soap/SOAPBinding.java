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
package javax.jws.soap;

// $Id: SOAPBinding.java 2897 2007-04-23 06:12:12Z thomas.diesler@jboss.com $

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the mapping of the Web Service onto the SOAP message protocol.
 *
 * @author thomas.diesler@jboss.org
 * @since 26-Apr-2005
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
public @interface SOAPBinding
{

   /**
    * The SOAP binding style
    */
   public enum Style
   {
      DOCUMENT, RPC
   };

   /**
    * The SOAP binding use
    */
   public enum Use
   {
      LITERAL, ENCODED
   };

   /**
    * The style of mapping parameters onto SOAP messages
    */
   public enum ParameterStyle
   {
      BARE, WRAPPED
   }

   /**
    * Defines the encoding style for messages send to and from the Web Service.
    */
   Style style() default Style.DOCUMENT;

   /**
    * Defines the formatting style for messages sent to and from the Web Service.
    */
   Use use() default Use.LITERAL;

   /**
    * Determines whether method parameters represent the entire message body, or whether the parameters are elements
    * wrapped inside a top-level element named after the operation
    */
   ParameterStyle parameterStyle() default ParameterStyle.WRAPPED;
}
