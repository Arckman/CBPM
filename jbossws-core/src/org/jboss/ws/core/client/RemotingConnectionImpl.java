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
package org.jboss.ws.core.client;

// $Id: RemotingConnectionImpl.java 3882 2007-07-13 13:48:43Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.rpc.Stub;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.addressing.EndpointReference;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Version;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.ws.core.CommonMessageContext;
import org.jboss.ws.core.MessageAbstraction;
import org.jboss.ws.core.MessageTrace;
import org.jboss.ws.core.StubExt;
import org.jboss.ws.core.WSTimeoutException;
import org.jboss.ws.core.soap.MessageContextAssociation;
import org.jboss.ws.core.soap.SOAPBodyElementDoc;
import org.jboss.ws.core.soap.SOAPMessageImpl;
import org.jboss.ws.metadata.config.EndpointProperty;

/**
 * SOAPConnection implementation.
 * <p/>
 *
 * Per default HTTP 1.1 chunked encoding is used.
 * This may be ovverriden through {@link org.jboss.ws.metadata.config.EndpointProperty#CHUNKED_ENCODING_SIZE}.
 * A chunksize value of zero disables chunked encoding.
 *
 * @author Thomas.Diesler@jboss.org
 * @author <a href="mailto:jason@stacksmash.com">Jason T. Greene</a>
 *
 * @since 02-Feb-2005
 */
public abstract class RemotingConnectionImpl implements RemotingConnection
{
   // provide logging
   private static Logger log = Logger.getLogger(RemotingConnectionImpl.class);

   private Map<String, Object> clientConfig = new HashMap<String, Object>();

   private static Map<String, String> metadataMap = new HashMap<String, String>();
   static
   {
      metadataMap.put(Stub.USERNAME_PROPERTY, "http.basic.username");
      metadataMap.put(Stub.PASSWORD_PROPERTY, "http.basic.password");
      metadataMap.put(BindingProvider.USERNAME_PROPERTY, "http.basic.username");
      metadataMap.put(BindingProvider.PASSWORD_PROPERTY, "http.basic.password");
   }
   private static Map<String, String> configMap = new HashMap<String, String>();
   static
   {
      // Remoting constants since 2.0.0.GA
      //configMap.put(StubExt.PROPERTY_KEY_STORE, SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH);
      //configMap.put(StubExt.PROPERTY_KEY_STORE_PASSWORD, SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD);
      //configMap.put(StubExt.PROPERTY_KEY_STORE_TYPE, SSLSocketBuilder.REMOTING_KEY_STORE_TYPE);
      //configMap.put(StubExt.PROPERTY_TRUST_STORE, SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH);
      //configMap.put(StubExt.PROPERTY_TRUST_STORE_PASSWORD, SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD);
      //configMap.put(StubExt.PROPERTY_TRUST_STORE_TYPE, SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE);

      configMap.put(StubExt.PROPERTY_KEY_STORE, "org.jboss.remoting.keyStore");
      configMap.put(StubExt.PROPERTY_KEY_STORE_PASSWORD, "org.jboss.remoting.keyStorePassword");
      configMap.put(StubExt.PROPERTY_KEY_STORE_TYPE, "org.jboss.remoting.keyStoreType");
      configMap.put(StubExt.PROPERTY_TRUST_STORE, "org.jboss.remoting.trustStore");
      configMap.put(StubExt.PROPERTY_TRUST_STORE_PASSWORD, "org.jboss.remoting.trustStorePassword");
      configMap.put(StubExt.PROPERTY_TRUST_STORE_TYPE, "org.jboss.remoting.trustStoreType");
   }

   private boolean closed;

   public RemotingConnectionImpl()
   {
      // HTTPClientInvoker conect sends gratuitous POST
      // http://jira.jboss.com/jira/browse/JBWS-711
      clientConfig.put(Client.ENABLE_LEASE, false);
   }

   public boolean isClosed()
   {
      return closed;
   }

   public void setClosed(boolean closed)
   {
      this.closed = closed;
   }

   /** 
    * Sends the given message to the specified endpoint. 
    * 
    * A null reqMessage signifies a HTTP GET request.
    */
   public MessageAbstraction invoke(MessageAbstraction reqMessage, Object endpoint, boolean oneway) throws IOException
   {
      if (endpoint == null)
         throw new IllegalArgumentException("Given endpoint cannot be null");

      if (closed)
         throw new IOException("Connection is already closed");

      Object timeout = null;
      String targetAddress;
      Map<String, Object> callProps = new HashMap<String, Object>();

      if (endpoint instanceof EndpointInfo)
      {
         EndpointInfo epInfo = (EndpointInfo)endpoint;
         targetAddress = epInfo.getTargetAddress();
         callProps = epInfo.getProperties();

         if (callProps.containsKey(StubExt.PROPERTY_CLIENT_TIMEOUT))
         {
            timeout = callProps.get(StubExt.PROPERTY_CLIENT_TIMEOUT);
            targetAddress = addURLParameter(targetAddress, "timeout", timeout.toString());
         }

      }
      else if (endpoint instanceof EndpointReference)
      {
         EndpointReference epr = (EndpointReference)endpoint;
         targetAddress = epr.getAddress().toString();
      }
      else
      {
         targetAddress = endpoint.toString();
      }

      // setup remoting client            
      Map<String, Object> metadata = createRemotingMetaData(reqMessage, callProps);
      //we add header here==========================================================
      Properties header=(Properties)metadata.get("HEADER");
      SOAPMessageImpl soapMessage=(SOAPMessageImpl)reqMessage;
      SOAPPart soapPart=soapMessage.getSOAPPart();
      SOAPEnvelope soapEnvelop;
	try {
		soapEnvelop = soapPart.getEnvelope();
		SOAPBody soapBody=soapEnvelop.getBody();
		Iterator i=soapBody.getChildElements(soapEnvelop.createName("rootMonitorName"));
		while(i.hasNext()){
			SOAPBodyElementDoc element=(SOAPBodyElementDoc) i.next();
			header.put("rootMonitorName", element.getValue());
		}
		i=soapBody.getChildElements(soapEnvelop.createName("rootId"));
		while(i.hasNext()){
			SOAPBodyElementDoc element=(SOAPBodyElementDoc) i.next();
			header.put("rootId", element.getValue());
		}
		i=soapBody.getChildElements(soapEnvelop.createName("parentMonitorName"));
		while(i.hasNext()){
			SOAPBodyElementDoc element=(SOAPBodyElementDoc) i.next();
			header.put("parentMonitorName", element.getValue());
		}
		i=soapBody.getChildElements(soapEnvelop.createName("parentId"));
		while(i.hasNext()){
			SOAPBodyElementDoc element=(SOAPBodyElementDoc) i.next();
			header.put("parentId", element.getValue());
		}
	} catch (SOAPException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      //================================================================
      Client client = createRemotingClient(endpoint, targetAddress, oneway);

      try
      {
         if (log.isDebugEnabled())
            log.debug("Remoting metadata: " + metadata);

         // debug the outgoing message
         MessageTrace.traceMessage("Outgoing Request Message", reqMessage);

         MessageAbstraction resMessage = null;

         if (oneway == true)
         {
            client.invokeOneway(reqMessage, metadata, false);
         }
         else
         {
            resMessage = (MessageAbstraction)client.invoke(reqMessage, metadata);
         }

         // Disconnect the remoting client
         client.disconnect();

         callProps.clear();
         callProps.putAll(metadata);

         // trace the incomming response message
         MessageTrace.traceMessage("Incoming Response Message", resMessage);

         return resMessage;
      }
      catch (Throwable th)
      {
         if (timeout != null && (th.getCause() instanceof SocketTimeoutException))
         {
            throw new WSTimeoutException("Timeout after: " + timeout + "ms", new Long(timeout.toString()));
         }

         IOException io = new IOException("Could not transmit message");
         io.initCause(th);
         throw io;
      }
   }

   private String addURLParameter(String url, String key, String value)
   {
      int qmIndex = url.indexOf("?");
      url += (qmIndex < 0 ? "?" : "&") + key + "=" + value;
      return url;
   }

   private Client createRemotingClient(Object endpoint, String targetAddress, boolean oneway)
   {
      Client client;
      try
      {
         // Get the invoker from Remoting for a given endpoint address
         log.debug("Get locator for: " + endpoint);
         
         Marshaller marshaller = getMarshaller();
         UnMarshaller unmarshaller = getUnmarshaller();
         
         /** 
          * [JBWS-1704] The Use Of Remoting Causes An Additional 'datatype' Parameter To Be Sent On All Requests
          * 
          * An HTTPClientInvoker may disconnect from the server and recreated by the remoting layer.
          * In that case the new invoker does not inherit the marshaller/unmarshaller from the disconnected invoker.
          * We therefore explicitly specify the invoker locator datatype and register the SOAP marshaller/unmarshaller
          * with the MarshalFactory. 
          * 
          * This applies to remoting-1.4.5 and less
          */
         String version = getRemotingVersion();
         if (version.startsWith("1.4"))
         {
            targetAddress = addURLParameter(targetAddress, InvokerLocator.DATATYPE, "JBossWSMessage");
            MarshalFactory.addMarshaller("JBossWSMessage", marshaller, unmarshaller);
         }

         InvokerLocator locator = new InvokerLocator(targetAddress);
         client = new Client(locator, "jbossws", clientConfig);
         client.connect();

         client.setMarshaller(marshaller);

         if (oneway == false)
            client.setUnMarshaller(unmarshaller);
      }
      catch (MalformedURLException e)
      {
         throw new IllegalArgumentException("Malformed endpoint address", e);
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Could not setup remoting client", e);
      }
      return client;
   }

   private String getRemotingVersion()
   {
      String version = null; 
      try
      {
         // Access the constant dynamically, otherwise it will be the compile time value
         Field field = Version.class.getDeclaredField("VERSION");
         version = (String)field.get(null);
      }
      catch (Exception ex)
      {
         throw new RuntimeException("Cannot obtain remoting version", ex);
      }
      
      if (version == null)
      {
         URL codeURL = Version.class.getProtectionDomain().getCodeSource().getLocation();
         throw new RuntimeException("Cannot obtain remoting version from: " + codeURL);
      }
      return version;
   }

   protected abstract UnMarshaller getUnmarshaller();

   protected abstract Marshaller getMarshaller();

   private Map<String, Object> createRemotingMetaData(MessageAbstraction reqMessage, Map callProps)
   {
      CommonMessageContext msgContext = MessageContextAssociation.peekMessageContext();

      Map<String, Object> metadata = new HashMap<String, Object>();

      // We need to unmarshall faults (HTTP 500)
      // metadata.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "true"); // since 2.0.0.GA
      metadata.put("NoThrowOnError", "true");

      if (reqMessage != null)
      {
         populateHeaders(reqMessage, metadata);

         // Enable chunked encoding. This is the default size. 
         clientConfig.put("chunkedLength", "1024");
         
         // May be overridden through endpoint config
         if (msgContext != null)
         {
            Properties epmdProps = msgContext.getEndpointMetaData().getProperties();

            // chunksize settings
            String chunkSizeValue = epmdProps.getProperty(EndpointProperty.CHUNKED_ENCODING_SIZE);
            int chunkSize = chunkSizeValue != null ? Integer.valueOf(chunkSizeValue) : -1;
            if (chunkSize > 0)
            {
               clientConfig.put(EndpointProperty.CHUNKED_ENCODING_SIZE, chunkSizeValue);
            }
            else
            {
               clientConfig.remove("chunkedLength");
            }
         }
      }
      else
      {
         metadata.put("TYPE", "GET");
      }

      if (callProps != null)
      {
         Iterator it = callProps.entrySet().iterator();

         // Get authentication type, default to BASIC authetication
         String authType = (String)callProps.get(StubExt.PROPERTY_AUTH_TYPE);
         if (authType == null)
            authType = StubExt.PROPERTY_AUTH_TYPE_BASIC;

         while (it.hasNext())
         {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            Object val = entry.getValue();

            // pass properties to remoting meta data
            if (metadataMap.containsKey(key))
            {
               String remotingKey = metadataMap.get(key);
               if ("http.basic.username".equals(remotingKey) || "http.basic.password".equals(remotingKey))
               {
                  if (authType.equals(StubExt.PROPERTY_AUTH_TYPE_BASIC))
                  {
                     metadata.put(remotingKey, val);
                  }
                  else
                  {
                     log.warn("Ignore '" + key + "' with auth typy: " + authType);
                  }
               }
               else
               {
                  metadata.put(remotingKey, val);
               }
            }

            // pass properties to remoting client config
            if (configMap.containsKey(key))
            {
               String remotingKey = configMap.get(key);
               clientConfig.put(remotingKey, val);
            }
         }
      }

      return metadata;
   }

   protected void populateHeaders(MessageAbstraction reqMessage, Map<String, Object> metadata)
   {
      MimeHeaders mimeHeaders = reqMessage.getMimeHeaders();

      Properties props = new Properties();
      metadata.put("HEADER", props);

      Iterator i = mimeHeaders.getAllHeaders();
      while (i.hasNext())
      {
         MimeHeader header = (MimeHeader)i.next();
         String currentValue = props.getProperty(header.getName());

         /*
          * Coalesce multiple headers into one
          *
          * From HTTP/1.1 RFC 2616:
          *
          * Multiple message-header fields with the same field-name MAY be
          * present in a message if and only if the entire field-value for that
          * header field is defined as a comma-separated list [i.e., #(values)].
          * It MUST be possible to combine the multiple header fields into one
          * "field-name: field-value" pair, without changing the semantics of
          * the message, by appending each subsequent field-value to the first,
          * each separated by a comma.
          */
         if (currentValue != null)
         {
            props.put(header.getName(), currentValue + "," + header.getValue());
         }
         else
         {
            props.put(header.getName(), header.getValue());
         }
      }
   }
}
