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
package org.jboss.ws.core.jaxws.client;

// $Id: DispatchImpl.java 3772 2007-07-01 19:29:13Z thomas.diesler@jboss.com $

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBContext;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.jboss.logging.Logger;
import org.jboss.util.NotImplementedException;
import org.jboss.ws.core.MessageAbstraction;
import org.jboss.ws.core.ConfigProvider;
import org.jboss.ws.core.client.HTTPRemotingConnection;
import org.jboss.ws.core.client.RemotingConnection;
import org.jboss.ws.core.client.SOAPRemotingConnection;
import org.jboss.ws.core.jaxws.binding.BindingProviderImpl;
import org.jboss.ws.metadata.umdm.EndpointMetaData;
import org.jboss.ws.metadata.config.ConfigurationProvider;

/**
 * The Dispatch interface provides support for the dynamic invocation of a service endpoint operations. 
 * The javax.xml.ws.Service interface acts as a factory for the creation of Dispatch  instances.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 04-Jul-2006
 */
public class DispatchImpl<T> implements Dispatch<T>
{
   // provide logging
   private final Logger log = Logger.getLogger(DispatchImpl.class);

   private BindingProvider bindingProvider;
   private EndpointMetaData epMetaData;
   private JAXBContext jaxbContext;
   private ExecutorService executor;
   private Class type;
   private Mode mode;

   public DispatchImpl(ExecutorService executor, EndpointMetaData epMetaData, Class<T> type, Mode mode)
   {
      this.bindingProvider = new BindingProviderImpl(epMetaData);
      this.epMetaData = epMetaData;
      this.executor = executor;
      this.type = type;
      this.mode = mode;
      initDispatch();
   }

   public DispatchImpl(ExecutorService executor, EndpointMetaData epMetaData, JAXBContext jbc, Mode mode)
   {
      this.bindingProvider = new BindingProviderImpl(epMetaData);
      this.epMetaData = epMetaData;
      this.executor = executor;
      this.type = Object.class;
      this.jaxbContext = jbc;
      this.mode = mode;
      initDispatch();
   }

   public T invoke(T obj)
   {
      T retObj = null;
      try
      {
         retObj = (T)invokeInternal(obj, getResponseContext());
      }
      catch (Exception ex)
      {
         handleInvokeException(ex);
      }
      return retObj;
   }

   private Object invokeInternal(Object obj, Map<String, Object> resContext) throws IOException
   {
      MessageAbstraction reqMsg = getRequestMessage(obj);
      String targetAddress = epMetaData.getEndpointAddress();
      
      // R2744 A HTTP request MESSAGE MUST contain a SOAPAction HTTP header field
      // with a quoted value equal to the value of the soapAction attribute of
      // soapbind:operation, if present in the corresponding WSDL description.

      // R2745 A HTTP request MESSAGE MUST contain a SOAPAction HTTP header field
      // with a quoted empty string value, if in the corresponding WSDL description,
      // the soapAction attribute of soapbind:operation is either not present, or
      // present with an empty string as its value.
      String bindingID = bindingProvider.getBinding().getBindingID();
      if (bindingID.indexOf("soap") > 0)
      {
         String soapAction = null; 
         Map<String, Object> reqContext = getRequestContext();
         Boolean useSOAPAction = (Boolean)reqContext.get(BindingProvider.SOAPACTION_USE_PROPERTY);
         if (Boolean.TRUE.equals(useSOAPAction))
         {
            soapAction = (String)reqContext.get(BindingProvider.SOAPACTION_URI_PROPERTY);
            if (soapAction == null)
               throw new IllegalStateException("Cannot obtain: " + BindingProvider.SOAPACTION_URI_PROPERTY);
         }
         MimeHeaders mimeHeaders = reqMsg.getMimeHeaders();
         mimeHeaders.addHeader("SOAPAction", soapAction != null ? soapAction : "");
      }
      
      MessageAbstraction resMsg = getRemotingConnection().invoke(reqMsg, targetAddress, false);
      Object retObj = getReturnObject(resMsg);
      return retObj;
   }

   private RemotingConnection getRemotingConnection()
   {
      String bindingID = bindingProvider.getBinding().getBindingID();
      if (EndpointMetaData.SUPPORTED_BINDINGS.contains(bindingID) == false)
         throw new IllegalStateException("Unsupported binding: " + bindingID);

      RemotingConnection remotingConnection;
      if (HTTPBinding.HTTP_BINDING.equals(bindingID))
      {
         remotingConnection = new HTTPRemotingConnection();
      }
      else
      {
         remotingConnection = new SOAPRemotingConnection();
      }
      return remotingConnection;
   }

   public Response<T> invokeAsync(T msg)
   {
      ResponseImpl response = new ResponseImpl();
      Runnable task = new AsyncRunnable(response, null, msg);
      Future future = executor.submit(task);
      response.setFuture(future);
      return response;
   }

   public Future invokeAsync(T obj, AsyncHandler<T> handler)
   {
      ResponseImpl response = new ResponseImpl();
      Runnable task = new AsyncRunnable(response, handler, obj);
      Future future = executor.submit(task);
      response.setFuture(future);
      return response;
   }

   public void invokeOneWay(T msg)
   {
      try
      {
         MessageAbstraction reqMsg = getRequestMessage(msg);
         String targetAddress = epMetaData.getEndpointAddress();
         getRemotingConnection().invoke(reqMsg, targetAddress, true);
      }
      catch (Exception ex)
      {
         handleInvokeException(ex);
      }
   }

   // 4.17. Conformance (Failed Dispatch.invoke): When an operation is invoked using an invoke method, an
   // implementation MUST throw a WebServiceException if there is any error in the configuration of the
   // Dispatch instance or a ProtocolException if an error occurs during the remote operation invocation.
   //
   // 4.19  Conformance (Failed Dispatch.invokeOneWay): When an operation is invoked using an invoke-
   // OneWay method, an implementation MUST throw a WebServiceException if there is any error in the
   // configuration of the Dispatch instance or if an error is detected1 during the remote operation invocation.
   private void handleInvokeException(Exception ex)
   {
      if (ex instanceof WebServiceException)
      {
         throw (WebServiceException)ex;
      }

      String msg = "Cannot dispatch message";
      log.error(msg, ex);
      throw new WebServiceException(msg, ex);
   }

   public Map<String, Object> getRequestContext()
   {
      return bindingProvider.getRequestContext();
   }

   public Map<String, Object> getResponseContext()
   {
      return bindingProvider.getResponseContext();
   }

   public Binding getBinding()
   {
      return bindingProvider.getBinding();
   }

   private void initDispatch()
   {
      if (SOAPMessage.class.isAssignableFrom(type) && mode == Mode.MESSAGE)
      {
         // accepted
      }
      else if (Source.class.isAssignableFrom(type))
      {
         // accepted
      }
      else if (jaxbContext != null && mode == Mode.PAYLOAD)
      {
         // accepted
      }
      else
      {
         throw new WebServiceException("Illegal argument combination [type=" + (type != null ? type.getName() : null) + ",mode=" + mode + "]");
      }
   }

   private MessageAbstraction getRequestMessage(Object obj)
   {
      // jaxws/api/javax_xml_ws/Dispatch/Client.java#invokeTestJAXBNull
      if (obj == null)
         throw new SOAPFaultException("Request object cannot be null");

      String bindingID = bindingProvider.getBinding().getBindingID();
      if (EndpointMetaData.SUPPORTED_BINDINGS.contains(bindingID) == false)
         throw new IllegalStateException("Unsupported binding: " + bindingID);

      MessageAbstraction message;
      if (HTTPBinding.HTTP_BINDING.equals(bindingID))
      {
         DispatchHTTPBinding helper = new DispatchHTTPBinding(mode, type, jaxbContext);
         ((ConfigurationProvider)epMetaData).configure(helper);
         message = helper.getRequestMessage(obj);
      }
      else
      {
         DispatchSOAPBinding helper = new DispatchSOAPBinding(mode, type, jaxbContext);
         ((ConfigurationProvider)epMetaData).configure(helper);
         message = helper.getRequestMessage(obj);
      }
      return message;
   }

   private Object getReturnObject(MessageAbstraction resMsg)
   {
      String bindingID = bindingProvider.getBinding().getBindingID();
      if (EndpointMetaData.SUPPORTED_BINDINGS.contains(bindingID) == false)
         throw new IllegalStateException("Unsupported binding: " + bindingID);

      Object retObj = null;
      if (HTTPBinding.HTTP_BINDING.equals(bindingID))
      {
         DispatchHTTPBinding helper = new DispatchHTTPBinding(mode, type, jaxbContext);
         retObj = helper.getReturnObject(resMsg);
      }
      else
      {
         DispatchSOAPBinding helper = new DispatchSOAPBinding(mode, type, jaxbContext);
         retObj = helper.getReturnObject(resMsg);
      }
      return retObj;
   }

   class AsyncRunnable implements Runnable
   {
      private ResponseImpl response;
      private AsyncHandler handler;
      private Object payload;

      public AsyncRunnable(ResponseImpl response, AsyncHandler handler, Object payload)
      {
         if (response == null)
            throw new IllegalArgumentException("Async response cannot be null");
         if (payload == null)
            throw new IllegalArgumentException("Async payload cannot be null");
         
         this.response = response;
         this.handler = handler;
         this.payload = payload;
      }

      public void run()
      {
         try
         {
            Map<String, Object> resContext = response.getContext();
            Object result = invokeInternal(payload, resContext);
            response.set(result);
         }
         catch (Exception ex)
         {
            handleAsynInvokeException(ex);
         }
         finally
         {
            // Call the handler if available
            if (handler != null)
               handler.handleResponse(response);
         }
      }

      // 4.18 Conformance (Failed Dispatch.invokeAsync): When an operation is invoked using an invokeAsync
      // method, an implementation MUST throw a WebServiceException if there is any error in the configuration 
      // of the Dispatch instance. Errors that occur during the invocation are reported when the client
      // attempts to retrieve the results of the operation.
      private void handleAsynInvokeException(Exception ex)
      {
         String msg = "Cannot dispatch message";
         log.error(msg, ex);

         WebServiceException wsex;
         if (ex instanceof WebServiceException)
         {
            wsex = (WebServiceException)ex;
         }
         else
         {
            wsex = new WebServiceException(msg, ex);
         }
         response.setException(wsex);
      }
   }

   public EndpointReference getEndpointReference()
   {
      throw new NotImplementedException();
   }

   public <T extends EndpointReference> T getEndpointReference(Class<T> clazz)
   {
      throw new NotImplementedException();
   }
}
