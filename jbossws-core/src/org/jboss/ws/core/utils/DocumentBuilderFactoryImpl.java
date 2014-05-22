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
package org.jboss.ws.core.utils;

// $Id: DocumentBuilderFactoryImpl.java 3471 2007-06-06 13:41:06Z heiko.braun@jboss.com $

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.logging.Logger;

/**
 * A DocumentBuilderFactory that delegates to Xerces and is namespace aware by default.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 11-Apr-2007
 */
public class DocumentBuilderFactoryImpl extends DocumentBuilderFactory
{
   private static Logger log = Logger.getLogger(DocumentBuilderFactoryImpl.class);

   public static final String XERCES_DOCUMENT_BUILDER_FACTORY = "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl";

   private DocumentBuilderFactory delegate;

   public DocumentBuilderFactoryImpl()
   {
      try
      {
         ClassLoader classLoader = getClass().getClassLoader();
         Class clazz = classLoader.loadClass(XERCES_DOCUMENT_BUILDER_FACTORY);
         delegate = (DocumentBuilderFactory)clazz.newInstance();

         // namespace aware by default
         delegate.setNamespaceAware(true);			
		}
      catch (Exception ex)
      {
         throw new IllegalStateException("Cannot create delegate document builder factory: " + XERCES_DOCUMENT_BUILDER_FACTORY, ex);
      }
   }

   public Object getAttribute(String name) throws IllegalArgumentException
   {
      return delegate.getAttribute(name);
   }

   public boolean getFeature(String name) throws ParserConfigurationException
   {
      return delegate.getFeature(name);
   }

   public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException
   {
      DocumentBuilder builder = delegate.newDocumentBuilder();
		builder.setEntityResolver( new JBossWSEntityResolver() );
		return builder;
   }

   public void setAttribute(String name, Object value) throws IllegalArgumentException
   {
      delegate.setAttribute(name, value);
   }

   public void setFeature(String name, boolean value) throws ParserConfigurationException
   {
      delegate.setFeature(name, value);
   }
}
