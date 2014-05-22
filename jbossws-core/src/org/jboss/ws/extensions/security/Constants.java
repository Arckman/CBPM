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

import javax.xml.namespace.QName;

import org.apache.xml.security.utils.EncryptionConstants;


/**
 * @author Jason T. Greene
 * @version $Id: Constants.java 2629 2007-03-16 07:19:47Z jason.greene@jboss.com $
 */
public class Constants
{
   public static final String WSS_SOAP_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0";

   public static final String JBOSS_WSSE_NS = "http://www.jboss.com/jbossws/ws-security";

   public static final String JBOSS_WSSE_PREFIX = "jboss-wsse";

   public static final String WSSE_PREFIX = "wsse";

   public static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

   public static final String WSU_PREFIX = "wsu";

   public static final String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

   public static final String XML_SIGNATURE_NS = org.apache.xml.security.utils.Constants.SignatureSpecNS;

   public static final String XML_ENCRYPTION_NS = EncryptionConstants.EncryptionSpecNS;

   public static final String XML_ENCRYPTION_PREFIX = "xenc";

   public static final String ID = "Id";

   public static final String WSU_ID = WSU_PREFIX + ":" + ID;

   public static final String BASE64_ENCODING_TYPE = WSS_SOAP_NS + "#Base64Binary";

   public static final String WSSE_HEADER = WSSE_PREFIX + ":Security";

   public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";

   public static final String XENC_DATAREFERENCE = "DataReference";

   public static final String XENC_REFERENCELIST = "ReferenceList";

   public static final String XENC_ELEMENT_TYPE = EncryptionConstants.TYPE_ELEMENT;

   public static final String XENC_CONTENT_TYPE = EncryptionConstants.TYPE_CONTENT;
   
   public static final QName WSSE_HEADER_QNAME = new QName(WSSE_NS, "Security");
}
