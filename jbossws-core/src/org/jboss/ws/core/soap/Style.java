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
package org.jboss.ws.core.soap;

// $Id: Style.java 3400 2007-06-03 10:11:51Z thomas.diesler@jboss.com $

/** A type-safe enumeration for encoding style.
 *  
 * @author Thomas.Diesler@jboss.org
 * @since 14-Oct-2004
 */
public class Style
{
   private String style;

   public static final Style RPC = new Style("rpc");
   public static final Style DOCUMENT = new Style("document");

   private Style(String style)
   {
      this.style = style;
   }

   public static Style getDefaultStyle()
   {
      return DOCUMENT;
   }

   public static Style valueOf(String style)
   {
      if (RPC.style.equals(style))
         return RPC;
      if (DOCUMENT.style.equals(style))
         return DOCUMENT;
      
      throw new IllegalArgumentException("Unsupported style: " + style);
   }

   public String toString()
   {
      return style;
   }
}
