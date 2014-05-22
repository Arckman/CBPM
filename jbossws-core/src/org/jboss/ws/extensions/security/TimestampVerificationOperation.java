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

import java.util.Calendar;

import org.jboss.ws.extensions.security.element.Timestamp;
import org.w3c.dom.Document;


public class TimestampVerificationOperation
{
   private Calendar now = null;

   public TimestampVerificationOperation()
   {
   }

   /**
    * A special constructor that allows you to use a different value when validating the message.
    * DO NOT USE THIS UNLESS YOU REALLY KNOW WHAT YOU ARE DOING!.
    *
    * @param now The timestamp to use as the current time when validating a message expiration
    */
   public TimestampVerificationOperation(Calendar now)
   {
      this.now = now;
   }

   public void process(Document message, Timestamp timestamp) throws WSSecurityException
   {
      Calendar expired = timestamp.getExpires();
      Calendar created = timestamp.getCreated();
      Calendar now = (this.now == null) ? Calendar.getInstance() : this.now;

      if (created.after(now))
         throw new WSSecurityException("Invalid timestamp, message claimed to be created after now");

      if (expired != null && ! now.before(expired))
         throw new FailedCheckException("Expired message.");
   }
}
