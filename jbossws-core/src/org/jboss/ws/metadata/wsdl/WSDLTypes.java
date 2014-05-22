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
package org.jboss.ws.metadata.wsdl;

//$Id: WSDLTypes.java 1757 2006-12-22 15:40:24Z thomas.diesler@jboss.com $

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.jboss.logging.Logger;

/**
 * WSDL types.
 *
 * @author Thomas.Diesler@jboss.org
 * @author Anil.Saldhana@jboss.org
 * @since 10-Oct-2004
 */
public abstract class WSDLTypes implements Serializable
{
   private static final long serialVersionUID = 7919937323521372194L;

   // provide logging
   static private final Logger log = Logger.getLogger(WSDLTypes.class);

   private WSDLDefinitions wsdlDefinitions;
   private String namespace;

   public abstract QName getXMLType(QName name);

   public WSDLDefinitions getWsdlDefinitions()
   {
      return wsdlDefinitions;
   }

   void setWSDLDefintiions(WSDLDefinitions parent)
   {
      wsdlDefinitions = parent;
   }

   /**
    * Gets the namespace associate with this types declaration. Currently this is used to filter
    * which WSDL file receives this types definition. Null means all files.
    *
    * @return the namespace associated with this type definition
    */
   public String getNamespace()
   {
      return namespace;
   }

   public void setNamespace(String namespace)
   {
      this.namespace = namespace;
   }
}
