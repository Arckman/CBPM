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
package org.jboss.ws.core.server;

// $Id: WSDLRequestHandler.java 3959 2007-07-20 14:44:19Z heiko.braun@jboss.com $

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.logging.Logger;
import org.jboss.ws.metadata.umdm.EndpointMetaData;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.common.DOMUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles the delivery of the WSDL and its included artifacts.
 * It rewrites the include URL's.
 *
 * http://www.jboss.org/index.html?module=bb&op=viewtopic&p=3871263#3871263
 *
 * For a discussion of this topic.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 23-Mar-2005
 */
public class WSDLRequestHandler
{
   // provide logging
   private Logger log = Logger.getLogger(WSDLRequestHandler.class);

   private EndpointMetaData epMetaData;

   public WSDLRequestHandler(EndpointMetaData epMetaData)
   {
      this.epMetaData = epMetaData;
   }

   /**
    * Get the WSDL resource for a given resource path
    * <p/>
    * Use path value of null to get the root document
    *
    * @param resPath The wsdl resource to get, can be null for the top level wsdl
    * @return A wsdl document, or null if it cannot be found
    */
   public Document getDocumentForPath(URL reqURL, String wsdlHost, String resPath) throws IOException
   {
      Document wsdlDoc;

      // The WSDLFilePublisher should set the location to an URL 
      URL wsdlLocation = epMetaData.getServiceMetaData().getWsdlLocation();
      if (wsdlLocation == null)
         throw new IllegalStateException("Cannot obtain wsdl location");

      // get the root wsdl
      if (resPath == null)
      {
         Element wsdlElement = DOMUtils.parse(wsdlLocation.openStream());
         wsdlDoc = wsdlElement.getOwnerDocument();
      }

      // get some imported resource
      else
      {
         String impResourcePath = new File(wsdlLocation.getPath()).getParent() + File.separatorChar + resPath;
         File impResourceFile = new File(impResourcePath);

         Element wsdlElement = DOMUtils.parse(impResourceFile.toURL().openStream());
         wsdlDoc = wsdlElement.getOwnerDocument();
      }

      modifyAddressReferences(reqURL, wsdlHost, resPath, wsdlDoc.getDocumentElement());
      return wsdlDoc;
   }

   /**
    * Modify the location of wsdl and schema imports
    */
   private void modifyAddressReferences(URL reqURL, String wsdlHost, String resPath, Element element) throws MalformedURLException
   {
      // map wsdl definition imports
      NodeList nlist = element.getChildNodes();
      for (int i = 0; i < nlist.getLength(); i++)
      {
         Node childNode = nlist.item(i);
         if (childNode.getNodeType() == Node.ELEMENT_NODE)
         {
            Element childElement = (Element)childNode;
            String nodeName = childElement.getLocalName();

            // Replace xsd:import and xsd:include location attributes
            if ("import".equals(nodeName) || "include".equals(nodeName))
            {
               Attr locationAttr = childElement.getAttributeNode("schemaLocation");
               if (locationAttr == null)
                  locationAttr = childElement.getAttributeNode("location");

               if (locationAttr != null)
               {
                  String orgLocation = locationAttr.getNodeValue();
                  boolean isAbsolute = orgLocation.startsWith("http://") || orgLocation.startsWith("https://");
                  if (isAbsolute == false && orgLocation.startsWith(reqURL.getPath()) == false)
                  {
                     String newResourcePath = orgLocation;

                     if (resPath != null && resPath.indexOf("/") > 0)
                        newResourcePath = resPath.substring(0, resPath.lastIndexOf("/") + 1) + orgLocation;

                     String reqPath = reqURL.getPath();
                     String completeHost = wsdlHost;

                     if (!(wsdlHost.startsWith("http://") || wsdlHost.startsWith("https://")))
                     {
                        String reqProtocol = reqURL.getProtocol();
                        int reqPort = reqURL.getPort();
                        String hostAndPort = wsdlHost + (reqPort > 0 ? ":" + reqPort : "");
                        completeHost = reqProtocol + "://" + hostAndPort;
                     }

                     String newLocation = completeHost + reqPath + "?wsdl&resource=" + newResourcePath;
                     locationAttr.setNodeValue(newLocation);

                     log.trace("Mapping import from '" + orgLocation + "' to '" + newLocation + "'");
                  }
               }
            }

            // Replace the soap:address location attribute
            else if ("address".equals(nodeName))
            {
               Attr locationAttr = childElement.getAttributeNode("location");
               if (locationAttr != null)
               {
                  String orgLocation = locationAttr.getNodeValue();

                  URL orgURL = new URL(orgLocation);
                  String orgProtocol = orgURL.getProtocol();
                  String orgHost = orgURL.getHost();
                  int orgPort = orgURL.getPort();
                  String orgPath = orgURL.getPath();

                  if (ServerConfig.UNDEFINED_HOSTNAME.equals(orgHost))
                  {
                     URL newURL = new URL(wsdlHost); 
                     String newHost = newURL.getHost();
                     int newPort = newURL.getPort();
                     
                     String newLocation = orgProtocol + "://" + newHost;
                     if (orgPort != -1)
                        newLocation += ":" + orgPort;
                     else if (newPort != -1)
                        newLocation += ":" + newPort;
                     
                     newLocation += orgPath;
                     locationAttr.setNodeValue(newLocation);

                     log.trace("Mapping address from '" + orgLocation + "' to '" + newLocation + "'");
                  }
               }
            }
            else
            {
               modifyAddressReferences(reqURL, wsdlHost, resPath, childElement);
            }
         }
      }
   }

}
