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
package org.jboss.ws.metadata.umdm;

// $Id: ClientEndpointMetaData.java 4016 2007-07-27 06:00:11Z thomas.diesler@jboss.com $

import org.jboss.ws.metadata.config.ConfigurationProvider;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData.HandlerType;

import javax.xml.namespace.QName;

/**
 * Client side endpoint meta data.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2005
 */
public class ClientEndpointMetaData extends EndpointMetaData
{
   // The endpoint address
   private String endpointAddress;
   
   public ClientEndpointMetaData(ServiceMetaData service, QName qname, QName portTypeName, Type type)
   {
      super(service, qname, portTypeName, type);
      configName = ConfigurationProvider.DEFAULT_CLIENT_CONFIG_NAME;

      if (type == Type.JAXRPC)
         configFile = ConfigurationProvider.DEFAULT_JAXRPC_CLIENT_CONFIG_FILE;
      else
         configFile = ConfigurationProvider.DEFAULT_JAXWS_CLIENT_CONFIG_FILE;
   }

   public String getEndpointAddress()
   {
      return endpointAddress;
   }

   public void setEndpointAddress(String endpointAddress)
   {
      this.endpointAddress = endpointAddress;
   }

   public String toString()
   {
      StringBuilder buffer = new StringBuilder("\nClientEndpointMetaData:");
      buffer.append("\n type=").append(getType());
      buffer.append("\n qname=").append(getPortName());
      buffer.append("\n address=").append(getEndpointAddress());
      buffer.append("\n binding=").append(getBindingId());
      buffer.append("\n seiName=").append(getServiceEndpointInterfaceName());
      buffer.append("\n configFile=").append(getConfigFile());
      buffer.append("\n configName=").append(getConfigName());
      buffer.append("\n authMethod=").append(getAuthMethod());
      buffer.append("\n properties=").append(getProperties());

      for (OperationMetaData opMetaData : getOperations())
      {
         buffer.append("\n").append(opMetaData);
      }
      for (HandlerMetaData hdlMetaData : getHandlerMetaData(HandlerType.ALL))
      {
         buffer.append("\n").append(hdlMetaData);
      }
      return buffer.toString();
   }
}
