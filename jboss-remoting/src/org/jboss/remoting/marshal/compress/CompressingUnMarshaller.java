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

package org.jboss.remoting.marshal.compress;

import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;
import org.jboss.remoting.serialization.SerializationStreamFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;


/**
 * <code>CompressingMarshaller</code> and <code>CompressingUnMarshaller</code> are a general
 * purpose compressing marshaller / decompressing unmarshaller pair based on Java's GZIP facilities.
 * <p/>
 * <code>CompressingUnMarshaller</code> is subclassed from <code>SerializableUnMarshaller</code>,
 * and by default it uses <code>super.read()</code> to deserialize an object, once the object has been
 * uncompressed.  Optionally, it can wrap any other unmarshaller and use that instead of
 * <code>SerializableUnMarshaller</code> to unmarshall an uncompressed input stream.  For example,
 * <p/>
 * <center><code>new CompressingUnMarshaller(new HTTPUnMarshaller())</code></center
 * <p/>
 * will create an umarshaller that
 * uses an <code>HTTPUnMarshaller</code> to restore an uncompressed input stream.
 *
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 1.6.8.1 $
 *          <p/>
 *          Copyright (c) 2005
 *          </p>
 */

public class CompressingUnMarshaller extends SerializableUnMarshaller
{
   public final static String DATATYPE = "compressible";

   private UnMarshaller wrappedUnMarshaller;
   private static final long serialVersionUID = 3843451434770746776L;


   /**
    * Create a new CompressingUnMarshaller.
    */
   public CompressingUnMarshaller()
   {
   }


   /**
    * Create a new CompressingUnMarshaller.
    *
    * @param unMarshaller unmarshaller to be used to restore uncompressed byte stream to original object
    */
   public CompressingUnMarshaller(UnMarshaller unMarshaller)
   {
      wrappedUnMarshaller = unMarshaller;
   }

   /**
    * Restores a compressed, marshalled form of an object to its original state.
    *
    * @param inputStream <code>InputStream</code> from which marshalled form is to be retrieved
    * @param metadata    can be any transport specific metadata (such as headers from http transport).
    *                    This can be null, depending on if transport supports metadata.
    * @param version     wire format version
    * @return restored object
    * @throws IOException            if there is a problem reading from <code>inputStream</code>
    * @throws ClassNotFoundException if there is a problem finding a class needed for unmarshalling
    */
   public Object read(InputStream inputStream, Map metadata, int version) throws IOException, ClassNotFoundException
   {
      GZIPInputStream gzis = new GZIPInputStream(inputStream);
      ObjectInputStream ois = SerializationStreamFactory.getManagerInstance(getSerializationType()).createRegularInput(gzis);

      if(wrappedUnMarshaller != null)
      {
         if (wrappedUnMarshaller instanceof VersionedUnMarshaller)
            return ((VersionedUnMarshaller)wrappedUnMarshaller).read(ois, metadata, version);
         else
            return wrappedUnMarshaller.read(ois, metadata);
      }
      else
      {
         return super.read(ois, metadata, version);
      }
   }


   /**
    * Returns a new <code>CompressingUnMarshaller</code>
    *
    * @return a new <code>CompressingUnMarshaller</code>
    * @throws CloneNotSupportedException In practice no exceptions are thrown.
    */
   public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException
   {
      return new CompressingUnMarshaller(wrappedUnMarshaller);
   }

}

