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
package org.jboss.wsf.stack.jbws;

// $Id: WSDLFilePublisher.java 4340 2007-08-13 15:24:16Z thomas.diesler@jboss.com $

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.factory.WSDLFactory;

import org.jboss.logging.Logger;
import org.jboss.util.NotImplementedException;
import org.jboss.ws.Constants;
import org.jboss.ws.WSException;
import org.jboss.ws.core.utils.ResourceURL;
import org.jboss.ws.metadata.umdm.ServiceMetaData;
import org.jboss.ws.metadata.umdm.UnifiedMetaData;
import org.jboss.ws.metadata.wsdl.WSDLDefinitions;
import org.jboss.ws.tools.wsdl.WSDLWriter;
import org.jboss.wsf.common.DOMUtils;
import org.jboss.wsf.common.IOUtils;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.management.ServerConfigFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** A helper class that publishes the wsdl files and their imports to the server/data/wsdl directory.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 02-June-2004
 */
public class WSDLFilePublisher
{
   // provide logging
   private static final Logger log = Logger.getLogger(WSDLFilePublisher.class);

   // The deployment info for the web service archive
   private ArchiveDeployment dep;
   // The expected wsdl location in the deployment
   private String expLocation;
   // The server config
   private ServerConfig serverConfig;

   public WSDLFilePublisher(ArchiveDeployment dep)
   {
      this.dep = dep;
      
      SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      serverConfig = spiProvider.getSPI(ServerConfigFactory.class).getServerConfig();
      
      if (dep.getType().toString().endsWith("JSE"))
      {
         expLocation = "WEB-INF/wsdl/";
      }
      else
      {
         expLocation = "META-INF/wsdl/";
      }
   }

   /** Publish the deployed wsdl file to the data directory
    */
   public void publishWsdlFiles(UnifiedMetaData wsMetaData) throws IOException
   {
      String deploymentName = dep.getCanonicalName();

      // For each service
      for (ServiceMetaData serviceMetaData : wsMetaData.getServices())
      {
         File wsdlFile = getPublishLocation(deploymentName, serviceMetaData);
         wsdlFile.getParentFile().mkdirs();

         // Get the wsdl definition and write it to the wsdl publish location
         try
         {
            Writer fWriter = IOUtils.getCharsetFileWriter(wsdlFile, Constants.DEFAULT_XML_CHARSET);
            WSDLDefinitions wsdlDefinitions = serviceMetaData.getWsdlDefinitions();
            new WSDLWriter(wsdlDefinitions).write(fWriter, Constants.DEFAULT_XML_CHARSET);

            URL wsdlPublishURL = wsdlFile.toURL();
            log.info("WSDL published to: " + wsdlPublishURL);

            // udpate the wsdl file location 
            serviceMetaData.setWsdlLocation(wsdlFile.toURL());

            // Process the wsdl imports
            Definition wsdl11Definition = wsdlDefinitions.getWsdlOneOneDefinition();
            if (wsdl11Definition != null)
            {
               publishWsdlImports(wsdlFile.toURL(), wsdl11Definition);

               // Publish XMLSchema imports
               Document document = wsdlDefinitions.getWsdlDocument();
               publishSchemaImports(wsdlFile.toURL(), document.getDocumentElement());
            }
            else
            {
               throw new NotImplementedException("WSDL-2.0 imports");
            }
         }
         catch (RuntimeException rte)
         {
            throw rte;
         }
         catch (Exception e)
         {
            throw new WSException("Cannot publish wsdl to: " + wsdlFile, e);
         }
      }
   }

   /** Publish the wsdl imports for a given wsdl definition
    */
   private void publishWsdlImports(URL parentURL, Definition parentDefinition) throws Exception
   {
      String baseURI = parentURL.toExternalForm();

      Iterator it = parentDefinition.getImports().values().iterator();
      while (it.hasNext())
      {
         for (Import wsdlImport : (List<Import>)it.next())
         {
            String locationURI = wsdlImport.getLocationURI();
            Definition subdef = wsdlImport.getDefinition();

            // its an external import, don't publish locally
            if (locationURI.startsWith("http://") == false)
            {
               URL targetURL = new URL(baseURI.substring(0, baseURI.lastIndexOf("/") + 1) + locationURI);
               File targetFile = new File(targetURL.getPath());
               targetFile.getParentFile().mkdirs();

               WSDLFactory wsdlFactory = WSDLFactory.newInstance();
               javax.wsdl.xml.WSDLWriter wsdlWriter = wsdlFactory.newWSDLWriter();
               FileWriter fw = new FileWriter(targetFile);
               wsdlWriter.writeWSDL(subdef, fw);
               fw.close();

               if (log.isDebugEnabled())
                  log.debug("WSDL import published to: " + targetURL);

               // recursivly publish imports
               publishWsdlImports(targetURL, subdef);

               // Publish XMLSchema imports
               Element subdoc = DOMUtils.parse(targetURL.openStream());
               publishSchemaImports(targetURL, subdoc);
            }
         }
      }
   }

   /** Publish the schema imports for a given wsdl definition
    */
   private void publishSchemaImports(URL parentURL, Element element) throws Exception
   {
      String baseURI = parentURL.toExternalForm();

      Iterator it = DOMUtils.getChildElements(element);
      while (it.hasNext())
      {
         Element childElement = (Element)it.next();
         if ("import".equals(childElement.getLocalName()) || "include".equals(childElement.getLocalName()))
         {
            String schemaLocation = childElement.getAttribute("schemaLocation");
            if (schemaLocation.length() > 0)
            {
               if (schemaLocation.startsWith("http://") == false)
               {
                  URL xsdURL = new URL(baseURI.substring(0, baseURI.lastIndexOf("/") + 1) + schemaLocation);
                  File targetFile = new File(xsdURL.getPath());
                  targetFile.getParentFile().mkdirs();

                  String deploymentName = dep.getCanonicalName();

                  // get the resource path
                  int index = baseURI.indexOf(deploymentName);
                  String resourcePath = baseURI.substring(index + deploymentName.length());
                  resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf("/"));
                  if (resourcePath.length() > 0)
                     resourcePath = resourcePath + "/";

                  resourcePath = expLocation + resourcePath + schemaLocation;
                  URL resourceURL = dep.getMetaDataFileURL(resourcePath);
                  InputStream is = new ResourceURL(resourceURL).openStream();
                  if (is == null)
                     throw new IllegalArgumentException("Cannot find schema import in deployment: " + resourcePath);

                  FileOutputStream fos = new FileOutputStream(targetFile);
                  IOUtils.copyStream(fos, is);
                  fos.close();
                  is.close();

                  if (log.isDebugEnabled())
                     log.debug("XMLSchema import published to: " + xsdURL);

                  // recursivly publish imports
                  Element subdoc = DOMUtils.parse(xsdURL.openStream());
                  publishSchemaImports(xsdURL, subdoc);
               }
            }
         }
         else
         {
            publishSchemaImports(parentURL, childElement);
         }
      }
   }

   /**
    * Delete the published wsdl
    */
   public void unpublishWsdlFiles() throws IOException
   {
      String deploymentDir = (dep.getParent() != null ? dep.getParent().getSimpleName() : dep.getSimpleName());

      File serviceDir = new File(serverConfig.getServerDataDir().getCanonicalPath() + "/wsdl/" + deploymentDir);
      deleteWsdlPublishDirectory(serviceDir);
   }

   /**
    * Delete the published wsdl document, traversing down the dir structure
    */
   private void deleteWsdlPublishDirectory(File dir) throws IOException
   {
      String[] files = dir.list();
      for (int i = 0; files != null && i < files.length; i++)
      {
         String fileName = files[i];
         File file = new File(dir + "/" + fileName);
         if (file.isDirectory())
         {
            deleteWsdlPublishDirectory(file);
         }
         else
         {
            if (file.delete() == false)
               log.warn("Cannot delete published wsdl document: " + file.toURL());
         }
      }

      // delete the directory as well
      dir.delete();
   }

   /**
    * Get the file publish location
    */
   private File getPublishLocation(String archiveName, ServiceMetaData serviceMetaData) throws IOException
   {
      String wsdlLocation = null;
      if (serviceMetaData.getWsdlLocation() != null)
         wsdlLocation = serviceMetaData.getWsdlLocation().toExternalForm();
      else if (serviceMetaData.getWsdlFile() != null)
         wsdlLocation = serviceMetaData.getWsdlFile();

      if (wsdlLocation == null)
         throw new IllegalStateException("Cannot obtain wsdl location for: " + serviceMetaData.getServiceName());

      if (log.isDebugEnabled())
         log.debug("Publish WSDL file: " + wsdlLocation);

      // Only file URLs are supported in <wsdl-publish-location>
      String publishLocation = serviceMetaData.getWsdlPublishLocation();
      boolean predefinedLocation = publishLocation != null && publishLocation.startsWith("file:");

      File locationFile = null;
      if (predefinedLocation == false)
      {
         locationFile = new File(serverConfig.getServerDataDir().getCanonicalPath() + "/wsdl/" + archiveName);
      }
      else
      {
         try
         {
            locationFile = new File(new URL(publishLocation).getPath());
         }
         catch (MalformedURLException e)
         {
            throw new IllegalArgumentException("Invalid publish location: " + e.getMessage());
         }
      }

      File wsdlFile;
      if (wsdlLocation.indexOf(expLocation) >= 0)
      {
         wsdlLocation = wsdlLocation.substring(wsdlLocation.indexOf(expLocation) + expLocation.length());
         wsdlFile = new File(locationFile + "/" + wsdlLocation);
      }
      else if (wsdlLocation.startsWith("vfsfile:") || wsdlLocation.startsWith("file:") || wsdlLocation.startsWith("jar:"))
      {
         wsdlLocation = wsdlLocation.substring(wsdlLocation.lastIndexOf("/") + 1);
         wsdlFile = new File(locationFile + "/" + wsdlLocation);
      }
      else
      {
         throw new WSException("Invalid wsdlFile '" + wsdlLocation + "', expected in: " + expLocation);
      }

      return wsdlFile;
   }
}
