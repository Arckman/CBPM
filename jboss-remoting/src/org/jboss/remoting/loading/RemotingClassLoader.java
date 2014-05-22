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

package org.jboss.remoting.loading;

import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RemotingClassLoader extends ClassLoader
{
   private ClassLoader userClassLoader = null;

   protected static final Logger log = Logger.getLogger(RemotingClassLoader.class);
   protected static final boolean isTrace = log.isTraceEnabled();

   public RemotingClassLoader(ClassLoader remotingClassLoader, ClassLoader userClassLoader)
   {
      super(remotingClassLoader);
      this.userClassLoader = userClassLoader;
   }

   public Class loadClass(String name) throws ClassNotFoundException
   {
      Class loadedClass = null;

      try
      {
         loadedClass = Class.forName(name, false, getParent());
      }
      catch(ClassNotFoundException e)
      {
         if(isTrace)
         {
            log.trace("Could not load class (" + name + ") using parent remoting class loader (" + getParent() + ")");
         }
         if(userClassLoader != null)
         {
            try
            {
               loadedClass = Class.forName(name, false, userClassLoader);
            }
            catch (ClassNotFoundException e1)
            {
               if(isTrace)
               {
                  log.trace("Could not load class (" + name + ") using parent remoting class loader (" + getParent() + ")");
               }
            }
         }
      }

      if(loadedClass == null)
      {
         loadedClass = ClassLoaderUtility.loadClass(name, getClass());
      }

      return loadedClass;
   }

}