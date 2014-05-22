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

import java.lang.reflect.Constructor;
import java.util.List;

import org.jboss.ws.extensions.security.element.SecurityHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * <code>SecurityEncoder</code> is responsible for transforming a SOAP message
 * into a WS-Security encoded message.
 *
 * @author <a href="mailto:jason.greene@jboss.com">Jason T. Greene</a>
 * @version $Revision: 3992 $
 */
public class SecurityEncoder
{
   private List<OperationDescription<EncodingOperation>> operations;

   private SecurityStore store;

   public SecurityEncoder(List<OperationDescription<EncodingOperation>> operations, SecurityStore store)
   {
      org.apache.xml.security.Init.init();
      this.operations = operations;
      this.store = store;
   }

   private void attachHeader(SecurityHeader header, Document message)
   {
      Element soapHeader = Util.findOrCreateSoapHeader(message.getDocumentElement());
      try
      {
         Element wsse = header.getElement();
         wsse.setAttributeNS(soapHeader.getNamespaceURI(), soapHeader.getPrefix() + ":mustUnderstand", "1");
         soapHeader.insertBefore(wsse, soapHeader.getFirstChild());
      }
      catch (Exception e) {}

   }

   public void encode(Document message) throws WSSecurityException
   {
      SecurityHeader header = new SecurityHeader(message);
      for (OperationDescription<EncodingOperation> op : operations)
      {
         EncodingOperation operation;

         try
         {
            Constructor<? extends EncodingOperation> constructor = op.getOperation().getConstructor(SecurityHeader.class, SecurityStore.class);
            operation = constructor.newInstance(header, store);
         }
         catch (Exception e)
         {
            throw new WSSecurityException("Error constructing operation: " + op.getOperation());
         }

         operation.process(message, op.getTargets(), op.getCertificateAlias(), op.getCredential(), op.getAlgorithm());
      }
      attachHeader(header, message);
   }
}
