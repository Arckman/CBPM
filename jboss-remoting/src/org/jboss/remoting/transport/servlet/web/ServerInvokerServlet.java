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

package org.jboss.remoting.transport.servlet.web;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.servlet.ServletServerInvokerMBean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * The servlet that receives the inital http request for the ServletServerInvoker.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServerInvokerServlet extends HttpServlet
{
   private static Logger log = Logger.getLogger(ServerInvokerServlet.class);
   private ServletServerInvokerMBean servletInvoker;
   private static final long serialVersionUID = 8796224225710165263L;

   /**
    * Initializes the servlet.
    */
   public void init(ServletConfig config) throws ServletException
   {
      super.init(config);

      // first see if the invoker is specified by its URL; if not, then see if the invoker was specified by name
      servletInvoker = getInvokerFromInvokerUrl(config);

      if (servletInvoker == null)
      {
         servletInvoker = getInvokerFromInvokerName(config);

         if (servletInvoker == null)
         {
            throw new ServletException("Could not find init parameter for 'locatorUrl' or 'locatorName' - one of which must be supplied for ServerInvokerServlet to function.");
         }
      }
   }

   /**
    * Destroys the servlet.
    */
   public void destroy()
   {

   }

   /**
    * Read a MarshalledInvocation and dispatch it to the target JMX object
    * invoke(Invocation) object.
    *
    * @param request  servlet request
    * @param response servlet response
    */
   protected void processRequest(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      boolean trace = log.isTraceEnabled();
      if (trace)
      {
         log.trace("processRequest, ContentLength: " + request.getContentLength());
         log.trace("processRequest, ContentType: " + request.getContentType());
      }

      int bufferSize = 1024;
      byte[] byteBuffer = new byte[bufferSize];
      ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

      int pointer = 0;
      int contentLength = request.getContentLength();
      ServletInputStream inputStream = request.getInputStream();
      int amtRead = inputStream.read(byteBuffer);

      while (amtRead > 0)
      {
         byteOutputStream.write(byteBuffer, pointer, amtRead);
         //pointer+=amtRead;
         if (amtRead < bufferSize && byteOutputStream.size() >= contentLength)
         {
            //done reading, so process
            break;
         }
         amtRead = inputStream.read(byteBuffer);
      }
      byteOutputStream.flush();
      byte[] totalByteArray = byteOutputStream.toByteArray();

      byte[] out = servletInvoker.processRequest(request, totalByteArray, response);
      ServletOutputStream outStream = response.getOutputStream();
      outStream.write(out);
      outStream.flush();
      outStream.close();
      //response.setContentLength(out.length);
   }

   /**
    * Handles the HTTP <code>GET</code> method.
    *
    * @param request  servlet request
    * @param response servlet response
    */
   protected void doGet(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      processRequest(request, response);
   }

   /**
    * Handles the HTTP <code>POST</code> method.
    *
    * @param request  servlet request
    * @param response servlet response
    */
   protected void doPost(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      processRequest(request, response);
   }

   /**
    * Returns a short description of the servlet.
    */
   public String getServletInfo()
   {
      return "Servlet front to JBossRemoting servlet server invoker.";
   }

   /**
    * Returns the servlet server invoker but only if it was specified via the
    * "invokerUrl" init parameter.
    *
    * @param config the servlet configuration
    * @return the servlet server invoker as specified by the "invokerUrl", or
    *         <code>null</code> if "invokerUrl" init parameter was not specified
    * @throws ServletException
    */
   protected ServletServerInvokerMBean getInvokerFromInvokerUrl(ServletConfig config)
         throws ServletException
   {
      String locatorUrl = config.getInitParameter("locatorUrl");
      if (locatorUrl == null)
      {
         return null;
      }

      ServerInvoker[] serverInvokers = InvokerRegistry.getServerInvokers();
      if (serverInvokers != null && serverInvokers.length > 0)
      {
         for (int x = 0; x < serverInvokers.length; x++)
         {
            ServerInvoker svrInvoker = serverInvokers[x];
            InvokerLocator locator = svrInvoker.getLocator();
            if (locatorUrl.equalsIgnoreCase(locator.getOriginalURI()))
            {
               return (ServletServerInvokerMBean) svrInvoker;
            }
         }

         throw new ServletException("Can not find servlet server invoker with same locator as specified (" + locatorUrl + ")");
      }

      throw new ServletException("Can not find any server invokers registered.  " +
                                 "Could be that servlet server invoker not registered or " +
                                 "has been created using different classloader.");
   }

   /**
    * Returns the servlet server invoker but only if it was specified via the
    * "invokerName" init parameter.
    *
    * @param config the servlet configuration
    * @return the servlet server invoker as specified by the "invokerName", or
    *         <code>null</code> if "invokerName" init parameter was not specified
    * @throws ServletException
    */
   protected ServletServerInvokerMBean getInvokerFromInvokerName(ServletConfig config)
         throws ServletException
   {
      ObjectName localInvokerName = null;

      String name = config.getInitParameter("invokerName");
      if (name == null)
      {
         return null;
      }

      try
      {
         localInvokerName = new ObjectName(name);
         log.debug("localInvokerName=" + localInvokerName);
      }
      catch (MalformedObjectNameException e)
      {
         throw new ServletException("Failed to build invokerName", e);
      }

      // Lookup the MBeanServer
      MBeanServer mbeanServer = getMBeanServer();
      if (mbeanServer == null)
      {
         throw new ServletException("Failed to locate the MBeanServer");
      }

      return (ServletServerInvokerMBean)
            MBeanServerInvocationHandler.newProxyInstance(mbeanServer,
                                                          localInvokerName,
                                                          ServletServerInvokerMBean.class,
                                                          false);
   }

   /**
    * Returns the MBeanServer where the server invoker should be found.  This should only be
    * used if the "invokerName" init parameter is specified (where that invoker name is the
    * object name registered in the returned MBeanServer).
    *
    * @return MBeanServer where the invoker is supposed to be registered
    */
   protected MBeanServer getMBeanServer()
   {
      // the intention of having this as a separate protected method is for subclasses to override
      // it in case this servlet is not running in JBossAS and thus needs to find an non-JBoss
      // MBeanServer.  This design won't work however since when this servlet is loaded, it will
      // still need to load in this JBoss specific MBeanServerLocator.  But, this servlet also
      // requires JBoss logging too so its not like this is the only place that breaks if not running
      // in JBossAS.  To complete this design, we must make this parent servlet an abstract class,
      // which this method abstract.  Then we need to create a JBoss-specific subclass with this
      // method's code in its getMBeanServer().
      for (Iterator i = MBeanServerFactory.findMBeanServer(null).iterator(); i.hasNext();)
      {
         MBeanServer server = (MBeanServer) i.next();
         if (server.getDefaultDomain().equals("jboss"))
         {
            return server;
         }
      }
      return null;
   }
}