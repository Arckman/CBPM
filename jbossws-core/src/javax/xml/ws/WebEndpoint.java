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

// $Id: WebEndpoint.java 2897 2007-04-23 06:12:12Z thomas.diesler@jboss.com $

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 
 *  Used to annotate the <code>get<em>PortName</em>()</code>
 *  methods of a generated service interface.
 *
 *  <p>The information specified in this annotation is sufficient
 *  to uniquely identify a <code>wsdl:port</code> element
 *  inside a <code>wsdl:service</code>. The latter is
 *  determined based on the value of the <code>WebServiceClient</code>
 *  annotation on the generated service interface itself.
 *
 *  @since JAX-WS 2.0
 *
 *  @see javax.xml.ws.WebServiceClient
 **/
@Target( { ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebEndpoint {
   /**
    *  The local name of the endpoint.
    **/
   String name() default "";
}
