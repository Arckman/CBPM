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
package javax.xml.ws.addressing;

//$Id: EndpointUnavailableException.java 4812 2007-10-19 21:26:39Z thomas.diesler@jboss.com $

import javax.xml.namespace.QName;

public class EndpointUnavailableException extends AddressingException
{
   private static final long serialVersionUID = 4098776568071868541L;

   static
   {
      fMessage = ac.getEndpointUnavailableText();
   }

   public EndpointUnavailableException()
   {
   }

   public EndpointUnavailableException(int retryAfter, String problemIRI)
   {
      super(fMessage + ": [retry=" + retryAfter + ",iri=" + problemIRI + "]");
   }

   public QName getSubcode()
   {
      return ac.getEndpointUnavailableQName();
   }
}
