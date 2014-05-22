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

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Version
{
   // possible remoting versions
   public static final byte VERSION_1 = 1;
   public static final byte VERSION_2 = 2;
   public static final byte VERSION_2_2 = 22;

   public static final String VERSION = "2.2.2.SP1 (Bluto)";
   private static final byte byteVersion = VERSION_2_2;
   private static byte defaultByteVersion = byteVersion;
   private static boolean performVersioning = true;


   public static final String PRE_2_0_COMPATIBLE = "jboss.remoting.pre_2_0_compatible";
   //TODO: -TME Is this the best system property key to use?  May want to use something that
   // is more decscriptive that is user defined version.  However, may want to make available
   // to users via system property the version of remoting?
   public static final String REMOTING_VERSION_TO_USE = "jboss.remoting.version";

   // have a static block to load the user defined version to use
   static
   {
      boolean precompatibleFlag = false;
      String precompatible = System.getProperty(PRE_2_0_COMPATIBLE);
      if(precompatible != null && precompatible.length() > 0)
      {
         precompatibleFlag = Boolean.valueOf(precompatible).booleanValue();
      }
      // if is precompatible, no need to look for custom version, as there is only 1 precompatible version
      if(precompatibleFlag)
      {
         defaultByteVersion = 1;
         performVersioning = false;
      }
      else
      {
         String userDefinedVersion = System.getProperty(REMOTING_VERSION_TO_USE);
         if(userDefinedVersion != null && userDefinedVersion.length() > 0)
         {
            byte userByteVersion = new Byte(userDefinedVersion).byteValue();
            if(userByteVersion > 0)
            {
               defaultByteVersion = userByteVersion;
               if(defaultByteVersion < 2)
               {
                  performVersioning = false;
               }
            }
            else
            {
               System.err.println("Can not set remoting version to value less than 1.  " +
                                  "System property value set for '" + REMOTING_VERSION_TO_USE + "' was " + userDefinedVersion);
            }
         }
         else
         {
            System.setProperty(REMOTING_VERSION_TO_USE, new Byte(defaultByteVersion).toString());
         }
      }
   }

   public static void main(String arg[])
   {
      System.out.println("JBossRemoting Version " + VERSION);
   }

   public static int getDefaultVersion()
   {
      return defaultByteVersion;
   }

   public static boolean performVersioning()
   {
      return performVersioning;
   }
}