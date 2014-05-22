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
package org.jboss.ws.core.jaxrpc.binding;

// $Id: BufferedStreamSource.java 4319 2007-08-10 15:48:41Z darran.lofthouse@jboss.com $

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.stream.StreamSource;

import org.jboss.ws.WSException;
import org.jboss.wsf.common.IOUtils;

/**
 * A StreamSource that can be read repeatedly. 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 29-Mar-2007
 */
public class BufferedStreamSource extends StreamSource
{
   private byte[] bytes;
   private char[] chars;

   public BufferedStreamSource(StreamSource source)
   {
      try
      {
         InputStream ins = source.getInputStream();
         if (ins != null)
         {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            IOUtils.copyStream(baos, ins);
            bytes = baos.toByteArray();
         }

         Reader rd = source.getReader();
         if (ins == null && rd != null)
         {
            char[] auxbuf = new char[1024];
            CharArrayWriter wr = new CharArrayWriter(auxbuf.length);
            int r = rd.read(auxbuf);
            while (r > 0)
            {
               wr.write(auxbuf, 0, r);
               r = rd.read(auxbuf);
            }
            chars = wr.toCharArray();
         }
      }
      catch (IOException ex)
      {
         WSException.rethrow(ex);
      }
   }

   public BufferedStreamSource(byte[] bytes)
   {
      this.bytes = bytes;
   }

   @Override
   public InputStream getInputStream()
   {
      return (bytes != null ? new ByteArrayInputStream(bytes) : null);
   }

   @Override
   public Reader getReader()
   {
      return (chars != null ? new CharArrayReader(chars) : null);
   }

   @Override
   public void setInputStream(InputStream inputStream)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setReader(Reader reader)
   {
      throw new UnsupportedOperationException();
   }

   public String toString()
   {
      String retStr = null;
      if (bytes != null)
      {
         try
         {
            retStr = new String(bytes, "UTF-8");
         }
         catch (UnsupportedEncodingException e)
         {
            WSException.rethrow(e);
         }
      }
      else if (chars != null)
         retStr = new String(chars);
      return "" + retStr;
   }
}
