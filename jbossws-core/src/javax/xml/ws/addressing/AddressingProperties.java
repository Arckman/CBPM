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

//$Id: AddressingProperties.java 2897 2007-04-23 06:12:12Z thomas.diesler@jboss.com $

import java.util.Map;

import javax.xml.namespace.QName;

public interface AddressingProperties extends AddressingType, Map<QName, AddressingType>
{

   public AttributedURI getTo();

   public void setTo(AttributedURI iri);

   public AttributedURI getAction();

   public void setAction(AttributedURI iri);

   public AttributedURI getMessageID();

   public void setMessageID(AttributedURI iri);

   public Relationship[] getRelatesTo();

   public void setRelatesTo(Relationship[] relationship);

   public EndpointReference getReplyTo();

   public void setReplyTo(EndpointReference ref);

   public EndpointReference getFaultTo();

   public void setFaultTo(EndpointReference ref);

   public EndpointReference getFrom();

   public void setFrom(EndpointReference ref);

   public ReferenceParameters getReferenceParameters();

   public void initializeAsDestination(EndpointReference ref);

   public void initializeAsReply(AddressingProperties props, boolean isFault);
}
