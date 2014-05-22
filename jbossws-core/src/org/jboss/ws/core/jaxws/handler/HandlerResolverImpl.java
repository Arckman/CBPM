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
package org.jboss.ws.core.jaxws.handler;

// $Id: HandlerResolverImpl.java 3959 2007-07-20 14:44:19Z heiko.braun@jboss.com $

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;

import org.jboss.logging.Logger;
import org.jboss.util.NotImplementedException;
import org.jboss.ws.WSException;
import org.jboss.ws.metadata.umdm.EndpointMetaData;
import org.jboss.ws.metadata.umdm.HandlerMetaData;
import org.jboss.ws.metadata.umdm.HandlerMetaDataJAXWS;
import org.jboss.ws.metadata.umdm.ServiceMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData.HandlerType;

/**
 * HandlerResolver is an interface implemented by an application to get control over 
 * the handler chain set on proxy/dispatch objects at the time of their creation.
 * 
 * A HandlerResolver may be set on a Service using the setHandlerResolver method.
 * 
 * When the runtime invokes a HandlerResolver, it will pass it a PortInfo object 
 * containing information about the port that the proxy/dispatch object will be accessing.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 08-Aug-2006
 */
public class HandlerResolverImpl implements HandlerResolver
{
   private static Logger log = Logger.getLogger(HandlerResolverImpl.class);

   private static final Map<String, String> protocolMap = new HashMap<String, String>();
   static
   {
      protocolMap.put("##SOAP11_HTTP", SOAPBinding.SOAP11HTTP_BINDING);
      protocolMap.put("##SOAP11_HTTP_MTOM", SOAPBinding.SOAP11HTTP_MTOM_BINDING);
      protocolMap.put("##SOAP12_HTTP", SOAPBinding.SOAP12HTTP_BINDING);
      protocolMap.put("##SOAP12_HTTP_MTOM", SOAPBinding.SOAP12HTTP_MTOM_BINDING);
      protocolMap.put("##XML_HTTP", HTTPBinding.HTTP_BINDING);
   }

   private List<ScopedHandler> preHandlers = new ArrayList<ScopedHandler>();
   private List<ScopedHandler> jaxwsHandlers = new ArrayList<ScopedHandler>();
   private List<ScopedHandler> postHandlers = new ArrayList<ScopedHandler>();

   // understood headers
   Set<QName> headers = new HashSet<QName>();

   public Set<QName> getHeaders()
   {
      return headers;
   }

   public List<Handler> getHandlerChain(PortInfo info)
   {
      return getHandlerChain(info, HandlerType.ENDPOINT);
   }

   public List<Handler> getHandlerChain(PortInfo info, HandlerType type)
   {
      log.debug("getHandlerChain: [type=" + type + ",info=" + info + "]");

      List<Handler> handlers = new ArrayList<Handler>();
      for (ScopedHandler scopedHandler : getHandlerMap(type))
      {
         if (scopedHandler.matches(info))
            handlers.add(scopedHandler.handler);
      }
      return handlers;
   }

   public void initServiceHandlerChain(ServiceMetaData serviceMetaData)
   {
      log.debug("initServiceHandlerChain: " + serviceMetaData.getServiceName());

      // clear all exisisting handler to avoid double registration
      List<ScopedHandler> handlerMap = getHandlerMap(HandlerType.ENDPOINT);
      handlerMap.clear();

      ClassLoader classLoader = serviceMetaData.getUnifiedMetaData().getClassLoader();
      for (HandlerMetaData handlerMetaData : serviceMetaData.getHandlerMetaData())
         addHandler(classLoader, HandlerType.ENDPOINT, handlerMetaData);
   }

   public void initHandlerChain(EndpointMetaData epMetaData, HandlerType type, boolean clearExistingHandlers)
   {
      log.debug("initHandlerChain: " + type);

      List<ScopedHandler> handlerMap = getHandlerMap(type);

      if (clearExistingHandlers)
         handlerMap.clear();

      ClassLoader classLoader = epMetaData.getClassLoader();
      for (HandlerMetaData handlerMetaData : epMetaData.getHandlerMetaData(type))
         addHandler(classLoader, type, handlerMetaData);
   }

   private void addHandler(ClassLoader classLoader, HandlerType type, HandlerMetaData handlerMetaData)
   {
      HandlerMetaDataJAXWS jaxwsMetaData = (HandlerMetaDataJAXWS)handlerMetaData;
      String handlerName = jaxwsMetaData.getHandlerName();
      String className = jaxwsMetaData.getHandlerClassName();
      Set<QName> soapHeaders = jaxwsMetaData.getSoapHeaders();

      try
      {
         // Load the handler class using the deployments top level CL
         Class hClass = classLoader.loadClass(className);
         Handler handler = (Handler)hClass.newInstance();

         if (handler instanceof GenericHandler)
            ((GenericHandler)handler).setHandlerName(handlerName);

         if (handler instanceof GenericSOAPHandler)
            ((GenericSOAPHandler)handler).setHeaders(soapHeaders);

         // Inject resources 
         injectResources(handler);

         // Call @PostConstruct
         callPostConstruct(handler);

         addHandler(jaxwsMetaData, handler, type);
      }
      catch (RuntimeException rte)
      {
         throw rte;
      }
      catch (Exception ex)
      {
         throw new WSException("Cannot load handler: " + className, ex);
      }
   }

   private void injectResources(Handler handler)
   {
      Class<? extends Handler> handlerClass = handler.getClass();
      for (Field field : handlerClass.getFields())
      {
         if (field.isAnnotationPresent(Resource.class))
            throw new NotImplementedException("@Resource not implemented for handler: " + handlerClass.getName());
      }
      for (Method method : handlerClass.getMethods())
      {
         if (method.isAnnotationPresent(Resource.class))
            throw new NotImplementedException("@Resource not implemented for handler: " + handlerClass.getName());
      }
   }

   private void callPostConstruct(Handler handler) throws Exception
   {
      Class<? extends Handler> handlerClass = handler.getClass();
      for (Method method : handlerClass.getMethods())
      {
         if (method.isAnnotationPresent(PostConstruct.class))
         {
            method.invoke(handler, new Object[] {});
         }
      }
   }

   private boolean addHandler(HandlerMetaDataJAXWS hmd, Handler handler, HandlerType type)
   {
      log.debug("addHandler: " + hmd);

      List<ScopedHandler> handlerMap = getHandlerMap(type);
      ScopedHandler scopedHandler = new ScopedHandler(handler);
      scopedHandler.servicePattern = hmd.getServiceNamePattern();
      scopedHandler.portPattern = hmd.getPortNamePattern();
      scopedHandler.protocols = hmd.getProtocolBindings();
      handlerMap.add(scopedHandler);

      // Ask all initialized handlers for what headers they understand
      if (handler instanceof SOAPHandler)
      {
         Set handlerHeaders = ((SOAPHandler)handler).getHeaders();
         if (handlerHeaders != null)
            headers.addAll(handlerHeaders);
      }

      return true;
   }

   private List<ScopedHandler> getHandlerMap(HandlerType type)
   {
      List<ScopedHandler> handlers = null;
      if (type == HandlerType.PRE)
         handlers = preHandlers;
      else if (type == HandlerType.ENDPOINT)
         handlers = jaxwsHandlers;
      else if (type == HandlerType.POST)
         handlers = postHandlers;
      else throw new IllegalArgumentException("Illegal handler type: " + type);

      return handlers;
   }

   private class ScopedHandler
   {
      Handler handler;
      QName servicePattern;
      QName portPattern;
      String protocols;

      Set<String> bindings;

      ScopedHandler(Handler handler)
      {
         this.handler = handler;
      }

      boolean matches(PortInfo info)
      {
         boolean match = true;
         if (servicePattern != null)
         {
            QName serviceName = info.getServiceName();
            match = matchQNamePattern(servicePattern, serviceName);
         }
         if (match && portPattern != null)
         {
            QName portName = info.getPortName();
            match = matchQNamePattern(portPattern, portName);
         }
         if (match && protocols != null)
         {
            boolean bindingMatch = false;
            String bindingID = info.getBindingID();
            for (String protocol : protocols.split("\\s"))
            {
               String aux = protocolMap.get(protocol);
               if (aux != null && aux.equals(bindingID))
               {
                  bindingMatch = true;
                  break;
               }
            }
            match = bindingMatch;
         }
         return match;
      }

      boolean matchQNamePattern(QName pattern, QName qname)
      {
         boolean match = true;
         String nsURI = pattern.getNamespaceURI();
         String localPart = pattern.getLocalPart();
         if (localPart.equals("*") == false)
         {
            if (localPart.endsWith("*"))
               localPart = localPart.substring(0, localPart.length() - 1);

            String qnameStr = qname.toString();
            String patternStr = new QName(nsURI, localPart).toString();
            match = qnameStr.startsWith(patternStr);
         }
         return match;
      }
   }
}
