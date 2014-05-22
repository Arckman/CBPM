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

// $Id: EndpointImpl.java 4025 2007-07-30 12:49:32Z thomas.diesler@jboss.com $

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;

import javax.xml.transform.Source;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServicePermission;

import org.jboss.logging.Logger;
import org.jboss.util.NotImplementedException;
import org.jboss.ws.core.jaxws.binding.BindingProviderImpl;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.http.HttpContext;
import org.jboss.wsf.spi.http.HttpContextFactory;
import org.jboss.wsf.spi.http.HttpServer;
import org.jboss.wsf.spi.http.HttpServerFactory;
import org.jboss.wsf.spi.management.ServerConfigFactory;
import org.w3c.dom.Element;

/**
 * A Web service endpoint implementation.
 *  
 * @author Thomas.Diesler@jboss.com
 * @since 07-Jul-2006
 */
public class EndpointImpl extends Endpoint
{
   // provide logging
   private final Logger log = Logger.getLogger(EndpointImpl.class);

   // The permission to publish an endpoint
   private static final WebServicePermission ENDPOINT_PUBLISH_PERMISSION = new WebServicePermission("publishEndpoint");

   private Object implementor;
   private Executor executor;
   private List<Source> metadata;
   private BindingProvider bindingProvider;
   private Map<String, Object> properties = new HashMap<String, Object>();
   private HttpContext serverContext;
   private boolean isPublished;
   private boolean isDestroyed;

   public EndpointImpl(String bindingId, Object implementor)
   {
      log.debug("new EndpointImpl(bindingId=" + bindingId + ",implementor=" + implementor + ")");

      if (implementor == null)
         throw new IllegalArgumentException("Implementor cannot be null");

      this.implementor = implementor;
      this.bindingProvider = new BindingProviderImpl(bindingId);
   }

   @Override
   public Binding getBinding()
   {
      return bindingProvider.getBinding();
   }

   @Override
   public Object getImplementor()
   {
      return implementor;
   }

   /**
    * Publishes this endpoint at the given address. The necessary server infrastructure will be created and configured by the JAX-WS
    * implementation using some default configuration. In order to get more control over the server configuration,
    * please use the javax.xml.ws.Endpoint#publish(Object) method instead.
    *
    * @param URI specifying the address to use. The address must be compatible with the binding specified at the time the endpoint was created.
    */
   @Override
   public void publish(String address)
   {
      log.debug("publish: " + address);

      URI addrURI;
      try
      {
         addrURI = new URI(address);
      }
      catch (URISyntaxException e)
      {
         throw new IllegalArgumentException("Invalid address: " + address);
      }

      // Check with the security manger
      checkPublishEndpointPermission();

      // Create and start the HTTP server
      SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      HttpServer httpServer = spiProvider.getSPI(HttpServerFactory.class).getHttpServer();
      httpServer.setProperties(properties);
      httpServer.start();

      String path = addrURI.getPath();
      String contextRoot = "/" + new StringTokenizer(path, "/").nextToken();
      HttpContext context = httpServer.createContext(contextRoot);

      publish(context);
   }

   /**
    * Publishes this endpoint at the provided server context.
    * A server context encapsulates the server infrastructure and addressing information for a particular transport.
    * For a call to this method to succeed, the server context passed as an argument to it must be compatible with the endpoint's binding.
    *
    * @param serverContext An object representing a server context to be used for publishing the endpoint.
    */
   @Override
   public void publish(Object context)
   {
      log.debug("publish: " + context);

      if (isDestroyed)
         throw new IllegalStateException("Endpoint already destroyed");

      // Check with the security manger
      checkPublishEndpointPermission();

      /* Check if we are standalone
      boolean isStandalone;
      try
      {
         SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
         spiProvider.getSPI(ServerConfigFactory.class).getServerConfig();
         isStandalone = false;
      }
      catch (Exception ex)
      {
         // ignore, there should be no ServerConfigFactory in VM
         isStandalone = true;
      }

      if (isStandalone == false)
         throw new IllegalStateException("Cannot publish endpoint from within server");
      */

      if (context instanceof HttpContext)
      {
         serverContext = (HttpContext)context;
         HttpServer httpServer = serverContext.getHttpServer();
         httpServer.publish(serverContext, this);
         isPublished = true;
      }
   }

   @Override
   public void stop()
   {
      log.debug("stop");

      if (serverContext == null || isPublished == false)
         log.error("Endpoint not published");

      try
      {
         if (serverContext != null)
         {
            HttpServer httpServer = serverContext.getHttpServer();
            httpServer.destroy(serverContext, this);
         }
      }
      catch (Exception ex)
      {
         log.error("Cannot stop endpoint", ex);
      }

      isPublished = false;
      isDestroyed = true;
   }

   @Override
   public boolean isPublished()
   {
      return isPublished;
   }

   @Override
   public List<Source> getMetadata()
   {
      return metadata;
   }

   @Override
   public void setMetadata(List<Source> list)
   {
      log.info("Ignore metadata, not implemented");
      this.metadata = list;
   }

   @Override
   public Executor getExecutor()
   {
      return executor;
   }

   @Override
   public void setExecutor(Executor executor)
   {
      log.info("Ignore executor, not implemented");
      this.executor = executor;
   }

   @Override
   public Map<String, Object> getProperties()
   {
      return properties;
   }

   @Override
   public void setProperties(Map<String, Object> map)
   {
      properties = map;
   }

   private void checkPublishEndpointPermission()
   {
      // 5.10 Conformance (Checking publishEndpoint Permission): When any of the publish methods defined
      // by the Endpoint class are invoked, an implementation MUST check whether a SecurityManager is
      // installed with the application. If it is, implementations MUST verify that the application has the 
      // WebServicePermission identified by the target name publishEndpoint before proceeding. If the permission
      // is not granted, implementations MUST NOT publish the endpoint and they MUST throw a 
      // java.lang.SecurityException.
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         sm.checkPermission(ENDPOINT_PUBLISH_PERMISSION);
      }
   }

   @Override
   public EndpointReference getEndpointReference(Element... referenceParameters)
   {
      throw new NotImplementedException();
   }

   @Override
   public <T extends EndpointReference> T getEndpointReference(Class<T> clazz, Element... referenceParameters)
   {
      throw new NotImplementedException();
   }
}