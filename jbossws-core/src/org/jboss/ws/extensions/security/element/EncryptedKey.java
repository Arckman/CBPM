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

import java.security.PrivateKey;

import javax.crypto.SecretKey;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.jboss.ws.extensions.security.Constants;
import org.jboss.ws.extensions.security.InvalidSecurityHeaderException;
import org.jboss.ws.extensions.security.KeyResolver;
import org.jboss.ws.extensions.security.Util;
import org.jboss.ws.extensions.security.WSSecurityException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <code>EncryptedKey</code> represents the am XMLSecurity encrypted key.
 *
 * @author Jason T. Greene
 * @version $Id: EncryptedKey.java 1757 2006-12-22 15:40:24Z thomas.diesler@jboss.com $
 */
public class EncryptedKey implements SecurityProcess
{
   private Document document;

   private SecretKey secretKey;

   private X509Token token;

   private ReferenceList list;

   private Element cachedElement;

   public EncryptedKey(Document document, SecretKey secretKey, X509Token token)
   {
      this(document, secretKey, token, new ReferenceList());
   }

   public EncryptedKey(Document document, SecretKey secretKey, X509Token token, ReferenceList list)
   {
      this.document = document;
      this.secretKey = secretKey;
      this.token = token;
      this.list = list;
   }

   public EncryptedKey(Element element, KeyResolver resolver) throws WSSecurityException
   {
      org.apache.xml.security.encryption.EncryptedKey key;
      XMLCipher cipher;

      try
      {
         cipher = XMLCipher.getInstance();
         key = cipher.loadEncryptedKey(element);
      }
      catch (XMLSecurityException e)
      {
         throw new WSSecurityException("Could not parse encrypted key: " + e.getMessage(), e);
      }

      KeyInfo info = key.getKeyInfo();

      if (info == null)
         throw new WSSecurityException("EncryptedKey element did not contain KeyInfo");

      PrivateKey privateKey = resolver.resolvePrivateKey(info);

      // Locate the reference list. We have to manually parse this because xml security doesn't handle
      // shorthand xpointer references (URI="#fooid")

      Element referenceList = Util.findElement(element, Constants.XENC_REFERENCELIST, Constants.XML_ENCRYPTION_NS);
      if (referenceList == null)
         throw new WSSecurityException("Encrypted key did not contain a reference list");

      this.list = new ReferenceList(referenceList);

      // Now use the element list to determine the encryption alg
      String alg = getKeyAlgorithm(element);
      if (alg == null)
         throw new WSSecurityException("Could not determine encrypted key algorithm!");

      try
      {
         cipher.init(XMLCipher.UNWRAP_MODE, privateKey);
         this.secretKey = (SecretKey) cipher.decryptKey(key, alg);
      }
      catch (XMLSecurityException e)
      {
         throw new WSSecurityException("Could not parse encrypted key: " + e.getMessage(), e);
      }

      this.document = element.getOwnerDocument();
      this.token = new X509Token(resolver.resolveCertificate(info), this.document);
   }

   private String getKeyAlgorithm(Element element) throws WSSecurityException
   {
      // We obtain the keys algorithm by looking at the first data element in our reference list
      String id = this.list.getAllReferences().iterator().next();
      if (id == null)
         return null;

      Element dataElement = Util.findElementByWsuId(element.getOwnerDocument().getDocumentElement(), id);
      if (dataElement == null)
         return null;

      return getEncryptionAlgorithm(dataElement);
   }

   private String getEncryptionAlgorithm(Element element) throws WSSecurityException
   {
      element = Util.findElement(element, "EncryptionMethod", Constants.XML_ENCRYPTION_NS);
      if (element == null)
         throw new InvalidSecurityHeaderException("Encrypted element corrupted, no encryption method");

      String alg = element.getAttribute("Algorithm");
      if (alg == null || alg.length() == 0)
         throw new InvalidSecurityHeaderException("Encrypted element corrupted, no algorithm specified");

      return alg;
   }

   public Element getElement() throws WSSecurityException
   {
      if (cachedElement != null)
         return cachedElement;

      XMLCipher cipher;
      org.apache.xml.security.encryption.EncryptedKey key;

      try
      {
         cipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
         cipher.init(XMLCipher.WRAP_MODE, token.getCert().getPublicKey());
         key = cipher.encryptKey(document, secretKey);
      }
      catch (XMLSecurityException e)
      {
         throw new WSSecurityException("Error encrypting key: " + e.getMessage(), e);
      }

      SecurityTokenReference reference = new SecurityTokenReference(new DirectReference(document, token));
      KeyInfo keyInfo = new KeyInfo(document);
      keyInfo.addUnknownElement(reference.getElement());
      key.setKeyInfo(keyInfo);

      key.setReferenceList(cipher.createReferenceList(org.apache.xml.security.encryption.ReferenceList.DATA_REFERENCE));
      list.populateRealReferenceList(key.getReferenceList());

      cachedElement = cipher.martial(key);
      return cachedElement;
   }

   public void addReference(String id)
   {
      list.add(id);
   }

   public SecretKey getSecretKey()
   {
      return secretKey;
   }

   public ReferenceList getReferenceList()
   {
      return list;
   }
}