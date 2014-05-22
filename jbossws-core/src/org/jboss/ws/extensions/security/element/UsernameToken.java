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
package org.jboss.ws.extensions.security.element;

import org.apache.xml.security.utils.XMLUtils;
import org.jboss.ws.extensions.security.Constants;
import org.jboss.ws.extensions.security.Util;
import org.jboss.ws.extensions.security.WSSecurityException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * @author Jason T. Greene
 * @version $Id: UsernameToken.java 1757 2006-12-22 15:40:24Z thomas.diesler@jboss.com $
 */
public class UsernameToken implements Token
{
   private String username;

   private String password;

   private Document doc;

   private String id;

   private Element cachedElement;

   public UsernameToken(String username, String password, Document doc)
   {
      this.username = username;
      this.password = password;
      this.doc = doc;
   }

   public UsernameToken(Element element) throws WSSecurityException
   {
      this.doc = element.getOwnerDocument();
      String id = element.getAttributeNS(Constants.WSU_NS, Constants.ID);
      if (id == null || id.length() == 0)
         throw new WSSecurityException("Invalid message, UsernameToken is missing an id");

      setId(id);

      Element child = Util.getFirstChildElement(element);
      if (child == null || ! Constants.WSSE_NS.equals(child.getNamespaceURI()) || ! "Username".equals(child.getLocalName()))
         throw new WSSecurityException("Username child expected in UsernameToken element");

      this.username = XMLUtils.getFullTextChildrenFromElement(child);

      child = Util.getNextSiblingElement(child);
      if (child == null || ! Constants.WSSE_NS.equals(child.getNamespaceURI()) || ! "Password".equals(child.getLocalName()))
         throw new WSSecurityException("Password child expected in UsernameToken element");

      this.password = XMLUtils.getFullTextChildrenFromElement(child);
   }

   public String getId()
   {
      if (id == null)
         id = Util.generateId("token");

      return id;
   }

   public void setId(String id)
   {
      this.id = id;
   }


   public String getPassword()
   {
      return password;
   }


   public void setPassword(String password)
   {
      this.password = password;
   }

   public String getUsername()
   {
      return username;
   }

   public void setUsername(String username)
   {
      this.username = username;
   }

   public Element getElement()
   {
      if (cachedElement != null)
         return cachedElement;

      Element element = doc.createElementNS(Constants.WSSE_NS, Constants.WSSE_PREFIX + ":" + "UsernameToken");
      element.setAttributeNS(Constants.WSU_NS, Constants.WSU_ID, getId());
      Element child = doc.createElementNS(Constants.WSSE_NS, Constants.WSSE_PREFIX + ":" + "Username");
      child.appendChild(doc.createTextNode(username));
      element.appendChild(child);
      child = doc.createElementNS(Constants.WSSE_NS, Constants.WSSE_PREFIX + ":" + "Password");
      child.appendChild(doc.createTextNode(password));
      element.appendChild(child);

      cachedElement = element;
      return cachedElement;
   }

   public Object getUniqueContent()
   {
      return null;
   }
}
