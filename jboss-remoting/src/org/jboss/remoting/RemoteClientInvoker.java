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

package org.jboss.remoting;


import java.util.Map;

/**
 * This class extends the MicroRemoteClientInvoker and adds extra
 * functionality that can not be included in a J2ME envrionment, such as
 * setting socket factories.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 1.24 $
 */
public abstract class RemoteClientInvoker extends MicroRemoteClientInvoker
{
   public RemoteClientInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public RemoteClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
      socketFactory = createSocketFactory(configuration);
   }
}
