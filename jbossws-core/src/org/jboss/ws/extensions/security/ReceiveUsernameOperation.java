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
package org.jboss.ws.extensions.security;

// $Id: ReceiveUsernameOperation.java 4340 2007-08-13 15:24:16Z thomas.diesler@jboss.com $

import org.jboss.ws.extensions.security.element.SecurityHeader;
import org.jboss.ws.extensions.security.element.Token;
import org.jboss.ws.extensions.security.element.UsernameToken;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.invocation.SecurityAdaptor;
import org.jboss.wsf.spi.invocation.SecurityAdaptorFactory;
import org.w3c.dom.Document;

public class ReceiveUsernameOperation implements TokenOperation
{
   private SecurityHeader header;
   private SecurityStore store;
   
   private SecurityAdaptorFactory secAdapterfactory;

   public ReceiveUsernameOperation(SecurityHeader header, SecurityStore store)
   {
      this.header = header;
      this.store = store;

      SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      secAdapterfactory = spiProvider.getSPI(SecurityAdaptorFactory.class);
   }

   public void process(Document message, Token token) throws WSSecurityException
   {
      UsernameToken user = (UsernameToken)token;
      SecurityAdaptor securityAdaptor = secAdapterfactory.newSecurityAdapter();
      securityAdaptor.setPrincipal(new SimplePrincipal(user.getUsername()));
      securityAdaptor.setCredential(user.getPassword());
   }
}
