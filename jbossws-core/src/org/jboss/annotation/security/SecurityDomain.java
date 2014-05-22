/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.annotation.security;

// $Id: SecurityDomain.java 2385 2007-02-16 14:02:27Z thomas.diesler@jboss.com $

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying the JBoss security domain for an EJB
 * 
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 **/
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface SecurityDomain
{
   /**
    * The required name for the security domain.
    * 
    * Do not use the JNDI name
    * 
    *    Good: "MyDomain"
    *    Bad:  "java:/jaas/MyDomain"
    */
   String value();
   
   /**
    * The name for the unauthenticated pricipal
    */
   String unauthenticatedPrincipal() default "";
}
