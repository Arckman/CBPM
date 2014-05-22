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

package org.jboss.remoting.transport.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.coyote.RequestMap;
import org.jboss.remoting.transport.coyote.ResponseMap;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.http.HTTPMarshaller;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class WebServerInvoker extends ServerInvoker
{
   // header constants
   public static String HEADER_SESSION_ID = "sessionId";
   public static String HEADER_SUBSYSTEM = "subsystem";


   public WebServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public WebServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   /**
    * returns true if the transport is bi-directional in nature, for example, HTTP in unidirectional and SOCKETs are
    * bi-directional (unless behind a firewall for example).
    *
    * @return false (HTTP is unidirrectional)
    */
   public boolean isTransportBiDirectional()
   {
      return false;
   }

   protected String getDefaultDataType()
   {
      return HTTPMarshaller.DATATYPE;
   }

   protected InvocationRequest getInvocationRequest(Map metadata, Object obj)
   {
      InvocationRequest request = null;

      if(obj instanceof InvocationRequest)
      {
         request = (InvocationRequest) obj;
         if(request.getRequestPayload() == null)
         {
            request.setRequestPayload(metadata);
         }
         else
         {
            request.getRequestPayload().putAll(metadata);
         }
      }
      else
      {
         request = createNewInvocationRequest(metadata, obj);
      }
      return request;
   }

   public UnMarshaller getUnMarshaller()
   {
      UnMarshaller unmarshaller = MarshalFactory.getUnMarshaller(getLocator(), this.getClass().getClassLoader());
      if(unmarshaller == null)
      {
         unmarshaller = MarshalFactory.getUnMarshaller(getDataType(), getSerializationType());
      }
      return unmarshaller;
   }

   public Marshaller getMarshaller()
   {
      Marshaller marshaller = MarshalFactory.getMarshaller(getLocator(), this.getClass().getClassLoader());

      if(marshaller == null)
      {
         marshaller = MarshalFactory.getMarshaller(getDataType(), getSerializationType());
      }
      return marshaller;
   }


   protected InvocationRequest createNewInvocationRequest(Map metadata, Object payload)
   {
      // will try to use the same session id if possible to track
      String sessionId = getSessionId(metadata);
      String subSystem = (String) metadata.get(HEADER_SUBSYSTEM);

      InvocationRequest request = null;
      Map responseMap = new HashMap();
      boolean isLeasQuery = checkForLeaseQuery(metadata);
      if(isLeasQuery)
      {
         addLeaseInfo(responseMap);
         request = new InvocationRequest(sessionId, subSystem, "$PING$", null, responseMap, null);
      }
      else
      {
         request = new InvocationRequest(sessionId, subSystem, payload, metadata, null, null);
      }
      request.setReturnPayload(responseMap);
      return request;
   }

   private boolean checkForLeaseQuery(Map headers)
   {
      boolean isLeaseQuery = false;

         if(headers != null)
         {
            Object val = headers.get(HTTPMetadataConstants.REMOTING_LEASE_QUERY);
            if(val != null && val instanceof String)
            {
               isLeaseQuery = Boolean.valueOf((String)val).booleanValue();
            }
            else
            {
               val = headers.get(HTTPMetadataConstants.REMOTING_LEASE_QUERY_LOWER_CASE);
               if(val != null && val instanceof String)
               {
                  isLeaseQuery = Boolean.valueOf((String)val).booleanValue();
               }
            }
         }
      return isLeaseQuery;
   }

   private void addLeaseInfo(Map response)
   {
      boolean leaseManagement = isLeaseActivated();
      response.put("LEASING_ENABLED", new Boolean(leaseManagement));

      if(leaseManagement)
      {
         long leasePeriod = getLeasePeriod();
         response.put("LEASE_PERIOD", new Long(leasePeriod));
      }
   }



   protected String getSessionId(Map metadata)
   {
      String sessionId = (String) metadata.get(HEADER_SESSION_ID);

      if(sessionId == null || sessionId.length() == 0)
      {
         String userAgent = (String) metadata.get("User-Agent");
         String host = (String) metadata.get("Host");
         String idSeed = userAgent + ":" + host;
         sessionId = Integer.toString(idSeed.hashCode());
      }

      return sessionId;
   }

   /**
    * Will write out the object to byte array and check size of byte array. This is VERY expensive, but need for the
    * content length.
    *
    * @param response
    * @return
    */
   protected int getContentLength(Object response) throws IOException

   {
      if(response != null)
      {
         /**
          * Am checking to see if type String because:
          * 1. faster to just get the length compared to doing serialization
          * 2. doing serialization adds extra bytes, so the value calculated is larger
          * than the actual number of characters (and causes the client to wait for the
          * extra characters that it will never get).
          */
         if(response instanceof String)
         {
            return ((String) response).length();
         }
         else
         {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(response);
            oos.flush();
            bos.flush();
            byte buffer[] = bos.toByteArray();
            return buffer.length;
         }
      }
      else
      {
         return 0;
      }
   }

}