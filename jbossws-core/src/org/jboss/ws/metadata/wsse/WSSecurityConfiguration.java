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
package org.jboss.ws.metadata.wsse;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;

/**
 * Root configuration class, represents the "jboss-ws-security" tag.
 *
 * @author <a href="mailto:jason.greene@jboss.com">Jason T. Greene</a>
 * @version $Revision: 3772 $
 */
public class WSSecurityConfiguration implements Serializable
{
   private static final long serialVersionUID = 1022564645797303076L;

   private Config defaultConfig;
   private HashMap<String, Port> ports = new HashMap<String, Port>();
   private String keyStoreFile;
   private URL keyStoreURL;
   private String keyStoreType;
   private String keyStorePassword;
   private String trustStoreFile;
   private URL trustStoreURL;
   private String trustStoreType;
   private String trustStorePassword;
   private HashMap<String, String> keyPasswords = new HashMap<String, String>();

   public WSSecurityConfiguration()
   {
   }

   public Config getDefaultConfig()
   {
      return defaultConfig;
   }

   public void setDefaultConfig(Config defaultConfig)
   {
      this.defaultConfig = defaultConfig;
   }

   public HashMap<String, Port> getPorts()
   {
      return ports;
   }

   public void setPorts(HashMap<String, Port> ports)
   {
      this.ports = ports;
   }

   public String getKeyStoreFile()
   {
      return keyStoreFile;
   }

   public void setKeyStoreFile(String keyStoreFile)
   {
      this.keyStoreFile = keyStoreFile;
   }

   public URL getKeyStoreURL()
   {
      return keyStoreURL;
   }

   public void setKeyStoreURL(URL keyStoreURL)
   {
      this.keyStoreURL = keyStoreURL;
   }

   public void addPort(Port port)
   {
      this.ports.put(port.getName(), port);
   }

   public String getKeyStorePassword()
   {
      return keyStorePassword;
   }

   public void setKeyStorePassword(String keyStorePassword)
   {
      this.keyStorePassword = keyStorePassword;
   }

   public String getKeyStoreType()
   {
      return keyStoreType;
   }

   public void setKeyStoreType(String keyStoreType)
   {
      this.keyStoreType = keyStoreType;
   }

   public String getTrustStorePassword()
   {
      return trustStorePassword;
   }

   public void setTrustStorePassword(String trustStorePassword)
   {
      this.trustStorePassword = trustStorePassword;
   }

   public String getTrustStoreType()
   {
      return trustStoreType;
   }

   public void setTrustStoreType(String trustStoreType)
   {
      this.trustStoreType = trustStoreType;
   }

   public URL getTrustStoreURL()
   {
      return trustStoreURL;
   }

   public void setTrustStoreURL(URL trustStoreURL)
   {
      this.trustStoreURL = trustStoreURL;
   }

   public String getTrustStoreFile()
   {
      return trustStoreFile;
   }

   public void setTrustStoreFile(String trustStoreFile)
   {
      this.trustStoreFile = trustStoreFile;
   }

   public HashMap<String, String> getKeyPasswords()
   {
      return keyPasswords;
   }

   public void setKeyPasswords(HashMap<String, String> keyPasswords)
   {
      this.keyPasswords = keyPasswords;
   }
}
