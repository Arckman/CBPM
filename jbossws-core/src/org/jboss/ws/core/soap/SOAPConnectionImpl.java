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

// $Id: SOAPConnectionImpl.java 2787 2007-04-11 10:36:03Z heiko.braun@jboss.com $

import java.io.IOException;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.jboss.logging.Logger;
import org.jboss.ws.core.MessageAbstraction;
import org.jboss.ws.core.client.RemotingConnectionImpl;
import org.jboss.ws.core.client.SOAPRemotingConnection;

/**
 * SOAPConnection implementation
 *
 * @author Thomas.Diesler@jboss.org
 * @author <a href="mailto:jason@stacksmash.com">Jason T. Greene</a>
 *
 * @since 02-Feb-2005
 */
public class SOAPConnectionImpl extends SOAPConnection
{
   // provide logging
   private static Logger log = Logger.getLogger(SOAPConnectionImpl.class);

   private RemotingConnectionImpl remotingConnection;

   public SOAPConnectionImpl()
   {
      remotingConnection = new SOAPRemotingConnection();
   }

   /**
    * Sends the given message to the specified endpoint and blocks until it has
    * returned the response.
    */
   public SOAPMessage call(SOAPMessage reqMessage, Object endpoint) throws SOAPException
   {
      if (reqMessage == null)
         throw new IllegalArgumentException("Given SOAPMessage cannot be null");

      MessageAbstraction resMessage = callInternal(reqMessage, endpoint, false);
      return (SOAPMessage)resMessage;
   }

   /**
    * Sends an HTTP GET request to an endpoint and blocks until a SOAP message is received 
    */
   public SOAPMessage get(Object endpoint) throws SOAPException
   {
      MessageAbstraction resMessage = callInternal(null, endpoint, false);
      return (SOAPMessage)resMessage;
   }

   /**
    * Sends the given message to the specified endpoint. This method is logically
    * non blocking.
    */
   public SOAPMessage callOneWay(SOAPMessage reqMessage, Object endpoint) throws SOAPException
   {
      if (reqMessage == null)
         throw new IllegalArgumentException("Given SOAPMessage cannot be null");

      MessageAbstraction resMessage = callInternal((SOAPMessageImpl)reqMessage, endpoint, true);
      return (SOAPMessage)resMessage;
   }

   /** Closes this SOAPConnection
    */
   public void close() throws SOAPException
   {
      if (remotingConnection.isClosed())
         throw new SOAPException("SOAPConnection is already closed");

      remotingConnection.setClosed(true);
   }

   private MessageAbstraction callInternal(SOAPMessage reqMessage, Object endpoint, boolean oneway) throws SOAPException
   {
      try
      {
         MessageAbstraction resMessage = remotingConnection.invoke((SOAPMessageImpl)reqMessage, endpoint, oneway);
         return resMessage;
      }    
      catch (Exception ex)
      {
         Throwable cause = ex.getCause();
         if (cause instanceof SOAPException)
            throw (SOAPException)cause;

         throw new SOAPException(ex);
      }
   }
}
