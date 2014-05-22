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
package org.jboss.ws.core;

// $Id: MessageTrace.java 3959 2007-07-20 14:44:19Z heiko.braun@jboss.com $

import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.jboss.logging.Logger;
import org.jboss.ws.core.soap.SOAPElementImpl;
import org.jboss.ws.core.soap.SOAPElementWriter;
import org.jboss.wsf.common.DOMWriter;
import org.w3c.dom.Element;

/**
 * Trace incomming/outgoing messages
 *
 * @author Thomas.Diesler@jboss.org
 * @since 04-Apr-2007
 */
public final class MessageTrace
{
   private static Logger msgLog = Logger.getLogger(MessageTrace.class);

   // Hide ctor
   private MessageTrace()
   {
   }

   public static void traceMessage(String messagePrefix, MessageAbstraction message)
   {
      if (msgLog.isTraceEnabled())
      {
         if (message instanceof SOAPMessage)
         {
            try
            {
               SOAPEnvelope soapEnv = ((SOAPMessage)message).getSOAPPart().getEnvelope();
               if (soapEnv != null)
               {
                  String envStr = SOAPElementWriter.writeElement((SOAPElementImpl)soapEnv, true);
                  msgLog.trace(messagePrefix + "\n" + envStr);
               }
            }
            catch (SOAPException ex)
            {
               msgLog.error("Cannot trace SOAPMessage", ex);
            }
         }
         else if (message instanceof HTTPMessageImpl)
         {
            HTTPMessageImpl httpMessage = (HTTPMessageImpl)message;
            Element root = httpMessage.getXmlFragment().toElement();
            String xmlString = DOMWriter.printNode(root, true);
            msgLog.trace(messagePrefix + "\n" + xmlString);
         }
         else
         {
            msgLog.warn("Unsupported message type: " + message);
         }
      }
   }
}
