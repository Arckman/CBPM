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
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.jboss.ws.extensions.security.element.EncryptedKey;
import org.jboss.ws.extensions.security.element.SecurityHeader;
import org.jboss.ws.extensions.security.element.SecurityProcess;
import org.jboss.ws.extensions.security.element.Signature;
import org.jboss.ws.extensions.security.element.Timestamp;
import org.jboss.ws.extensions.security.element.Token;
import org.jboss.ws.extensions.security.element.UsernameToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:jason.greene@jboss.com">Jason T. Greene</a>
 * @version $Revision: 1757 $
 */
public class SecurityDecoder
{
   private Element headerElement;

   private Calendar now =  null;

   private SecurityHeader header;

   private Document message;

   private SecurityStore store;

   private HashSet<String> signedIds = new HashSet<String>();

   private HashSet<String> encryptedIds = new HashSet<String>();

   public SecurityDecoder(SecurityStore store)
   {
      org.apache.xml.security.Init.init();
      this.store = store;
   }

   /**
    * A special constructor that allows you to use a different value when validating the message.
    * DO NOT USE THIS UNLESS YOU REALLY KNOW WHAT YOU ARE DOING!.
    *
    * @param SecurityStore the security store that contains key and trust information
    * @param now The timestamp to use as the current time when validating a message expiration
    */
   public SecurityDecoder(SecurityStore store, Calendar now)
   {
      this(store);
      this.now = now;
   }

   private Element getHeader(Document message) throws WSSecurityException
   {
      Element header = Util.findElement(message.getDocumentElement(), "Security", Constants.WSSE_NS);
      if (header == null)
         throw new WSSecurityException("Expected security header was not found");

      return header;
   }

   private void detachHeader()
   {
      headerElement.getParentNode().removeChild(headerElement);
   }


   private void decode() throws WSSecurityException
   {
      // Validate a timestamp if it is present
      Timestamp timestamp = header.getTimestamp();

      if (timestamp != null)
      {
         TimestampVerificationOperation operation =
            (now == null) ? new TimestampVerificationOperation() : new TimestampVerificationOperation(now);
         operation.process(message, timestamp);
      }

      for (Token token : header.getTokens())
      {
         if (token instanceof UsernameToken)
            new ReceiveUsernameOperation(header, store).process(message, token);
      }

      signedIds.clear();
      encryptedIds.clear();

      SignatureVerificationOperation signatureVerifier = new SignatureVerificationOperation(header, store);
      DecryptionOperation decrypter = new DecryptionOperation(header, store);

      for (SecurityProcess process : header.getSecurityProcesses())
      {
         // If this list gets much larger it should probably be a hash lookup
         if (process instanceof Signature)
         {
            Collection<String> ids = signatureVerifier.process(message, process);
            if (ids != null)
              signedIds.addAll(ids);
         }
         else if (process instanceof EncryptedKey)
         {
            Collection<String> ids = decrypter.process(message, process);
            if (ids != null)
               encryptedIds.addAll(ids);
         }
      }
   }

   public void verify(List<OperationDescription<RequireOperation>> requireOperations) throws WSSecurityException
   {
      if (requireOperations == null)
         return;

      for (OperationDescription<RequireOperation> o : requireOperations)
      {
         Class<? extends RequireOperation> operation = o.getOperation();
         RequireOperation op;
         Collection<String> processedIds = null;

         if (operation.equals(RequireSignatureOperation.class))
         {
            op = new RequireSignatureOperation(header, store);
            processedIds = signedIds;
         }
         else if (operation.equals(RequireEncryptionOperation.class))
         {
            op = new RequireEncryptionOperation(header, store);
            processedIds = encryptedIds;
         }
         else
         {
            try
            {
               Constructor<? extends RequireOperation> c = operation.getConstructor(SecurityHeader.class, SecurityStore.class);
               op = c.newInstance(header, store);
            }
            catch (Exception e)
            {
               throw new WSSecurityException("Error constructing operation: " + operation);
            }
         }

         op.process(message, o.getTargets(), o.getCertificateAlias(), o.getCredential(), processedIds);
      }
   }

   public void decode(Document message) throws WSSecurityException
   {
      decode(message, getHeader(message));
   }

   public void decode(Document message, Element headerElement) throws WSSecurityException
   {
      this.headerElement = headerElement;
      this.header = new SecurityHeader(this.headerElement, store);
      this.message = message;

      decode();
   }

   public void complete()
   {
      // On completion we must remove the header so that no one else can process this
      // message (required by the specification)
      detachHeader();
   }
}
