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
package org.jboss.ws.core.jaxws.spi;

// $Id: ProviderImpl.java 2732 2007-03-30 17:33:56Z thomas.diesler@jboss.com $

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.spi.Provider;
import javax.xml.ws.spi.ServiceDelegate;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.jboss.util.NotImplementedException;
import org.w3c.dom.Element;

/**
 * Service provider for ServiceDelegate and Endpoint objects.
 *  
 * @author Thomas.Diesler@jboss.com
 * @since 03-May-2006
 */
public class ProviderImpl extends Provider
{
   // 6.2 Conformance (Concrete javax.xml.ws.spi.Provider required): An implementation MUST provide
   // a concrete class that extends javax.xml.ws.spi.Provider. Such a class MUST have a public constructor
   // which takes no arguments.
   public ProviderImpl()
   {
   }

   @Override
   public ServiceDelegate createServiceDelegate(URL wsdlLocation, QName serviceName, Class serviceClass)
   {
      ServiceDelegateImpl delegate = new ServiceDelegateImpl(wsdlLocation, serviceName, serviceClass);
      return delegate;
   }

   @Override
   public Endpoint createEndpoint(String bindingId, Object implementor)
   {
      EndpointImpl endpoint = new EndpointImpl(bindingId, implementor);
      return endpoint;
   }

   @Override
   public Endpoint createAndPublishEndpoint(String address, Object implementor)
   {
      // 6.3 Conformance (Provider createAndPublishEndpoint Method): The effect of invoking the createAnd-
      // PublishEndpoint method on a Provider MUST be the same as first invoking the createEndpoint
      // method with the binding ID appropriate to the URL scheme used by the address, then invoking the 
      // publish(String address) method on the resulting endpoint.
      
      String bindingId = getBindingFromAddress(address);
      EndpointImpl endpoint = (EndpointImpl)createEndpoint(bindingId, implementor);
      endpoint.publish(address);
      return endpoint;
   }

   private String getBindingFromAddress(String address)
   {
      String bindingId;
      try
      {
         URL url = new URL(address);
         String protocol = url.getProtocol();
         if (protocol.startsWith("http"))
         {
            bindingId = SOAPBinding.SOAP11HTTP_BINDING;
         }
         else
         {
            throw new IllegalArgumentException("Unsupported protocol: " + address);
         }
      }
      catch (MalformedURLException e)
      {
         throw new IllegalArgumentException("Invalid endpoint address: " + address);
      }
      return bindingId;
   }
   
   @Override
   public <T extends EndpointReference> T createEndpointReference(Class<T> clazz, QName serviceName, QName portName, Source wsdlDocumentLocation, Element... referenceParameters)
   {
      throw new NotImplementedException();
   }

   @Override
   public W3CEndpointReference createW3CEndpointReference(String address, QName serviceName, QName portName, List<Element> metadata, String wsdlDocumentLocation, List<Element> referenceParameters)
   {
      throw new NotImplementedException();
   }

   @Override
   public <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface, WebServiceFeature... features)
   {
      throw new NotImplementedException();
   }

   @Override
   public EndpointReference readEndpointReference(Source eprInfoset)
   {
      throw new NotImplementedException();
   }
}