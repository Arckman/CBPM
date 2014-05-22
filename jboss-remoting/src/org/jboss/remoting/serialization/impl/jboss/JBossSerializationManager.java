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


package org.jboss.remoting.serialization.impl.jboss;

import org.jboss.logging.Logger;
import org.jboss.remoting.loading.ObjectInputStreamWithClassLoader;
import org.jboss.remoting.serialization.IMarshalledValue;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.serial.io.JBossObjectInputStream;
import org.jboss.serial.io.JBossObjectOutputStream;
import org.jboss.serial.util.StringUtilBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Instantiates the Streamings according to JbossObjectOutputStream and JBossObjectInputStream.
 * Also, it uses a different approach for MarshallValues as we don't need to convert objects in bytes.
 * $Id: JBossSerializationManager.java,v 1.11.4.1.4.1 2007/05/09 08:35:28 rsigal Exp $
 *
 * @author <a href="mailto:tclebert.suconic@jboss.com">Clebert Suconic</a>
 */
public class JBossSerializationManager extends SerializationManager
{
   protected static final Logger log = Logger.getLogger(JBossSerializationManager.class);

   private static boolean trace = log.isTraceEnabled();

   public ObjectInputStream createInput(InputStream input, ClassLoader loader) throws IOException
   {
      if (trace) { log.trace(this + " creating JBossObjectInputStream"); }
      return new JBossObjectInputStream(input, loader, new StringUtilBuffer(10024, 10024));
   }

   public ObjectOutputStream createOutput(OutputStream output) throws IOException
   {
      if (trace) { log.trace(this + " creating JBossObjectOutputStream"); }
      return new JBossObjectOutputStream(output, new StringUtilBuffer(10024, 10024));
   }

   /**
    * Creates a MarshalledValue that does lazy serialization.
    */
   public IMarshalledValue createdMarshalledValue(Object source) throws IOException
   {
      if (source instanceof IMarshalledValue)
      {
         return (IMarshalledValue) source;
      }
      else
      {
         return new MarshalledValue(source);
      }
   }

   public IMarshalledValue createMarshalledValueForClone(Object original) throws IOException
   {
      return new SmartCloningMarshalledValue(original);
   }


   public void sendObject(ObjectOutputStream oos, Object dataObject, int version) throws IOException
   {
      oos.writeObject(dataObject);
      oos.flush();
   }

   public Object receiveObject(InputStream inputStream, ClassLoader customClassLoader, int version)
   throws IOException, ClassNotFoundException
   {
      ObjectInputStream objInputStream = null;
      Object obj = null;
      if (inputStream instanceof ObjectInputStreamWithClassLoader)
      {
         if (((ObjectInputStreamWithClassLoader) inputStream).getClassLoader() == null)
         {
            ((ObjectInputStreamWithClassLoader) inputStream).setClassLoader(customClassLoader);
         }
         objInputStream = (ObjectInputStream) inputStream;
      }
      else if (inputStream instanceof JBossObjectInputStream)
      {
         if (((JBossObjectInputStream) inputStream).getClassLoader() == null)
         {
            ((JBossObjectInputStream) inputStream).setClassLoader(customClassLoader);
         }
         objInputStream = (ObjectInputStream) inputStream;
      }
      else if (inputStream instanceof ObjectInputStream)
      {
         objInputStream = (ObjectInputStream) inputStream;
      }
      else
      {
         if (customClassLoader != null)
         {
            objInputStream = SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JBOSS).createInput(inputStream, customClassLoader);
         }
         else
         {
            objInputStream = SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JBOSS).createRegularInput(inputStream);
         }
      }

      obj = objInputStream.readObject();
      return obj;
   }

   public String toString()
   {
      return "JBossSerializationManager[" + Integer.toHexString(hashCode()) + "]";
   }

}
