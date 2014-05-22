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

package org.jboss.remoting.transport.http;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpParser;
import org.apache.commons.httpclient.NameValuePair;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Version;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.transport.web.WebServerInvoker;
import org.jboss.remoting.transport.web.WebUtil;
import org.jboss.util.threadpool.BasicThreadPool;
import org.jboss.util.threadpool.BlockingMode;
import org.jboss.util.threadpool.ThreadPool;
import org.jboss.util.threadpool.ThreadPoolMBean;
import org.jboss.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is the stand alone http server invoker which acts basically as a web server.
 *
 * Server invoker implementation based on http protocol.  Is basically a stand alone http server whose request are
 * forwared to the invocation handler and responses from invocation handler are sent back to caller as http response.
 * @deprecated This class has been replaced by org.jboss.remoting.transport.coyote.CoyoteInvoker, which is used
 * as the default server invoker for the http and https transport on the server side.  This class will be removed
 * from remoting distribution in a future release.
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class HTTPServerInvoker extends WebServerInvoker implements Runnable
{
   private static final Logger log = Logger.getLogger(HTTPServerInvoker.class);

   public static final String MAX_NUM_HTTP_THREADS_KEY = "maxNumThreadsHTTP";
   public static final String HTTP_THREAD_POOL_CLASS_KEY = "HTTPThreadPool";
   public static final String HTTP_KEEP_ALIVE_TIMEOUT_KEY = "keepAliveTimeout";

   private String httpThreadPoolClass = null;

   private static int BACKLOG_DEFAULT = 1000;
   private static int MAX_POOL_SIZE_DEFAULT = 100;

   private ServerSocket serverSocket = null;

   private boolean running = false;

   private ThreadPool httpThreadPool;
   private int maxPoolSize = MAX_POOL_SIZE_DEFAULT;

   protected int backlog = BACKLOG_DEFAULT;

   protected int keepAliveTimeout = 15000; // default to 15 (same as apache web server)

   private boolean newServerSocketFactory = false;

   // list of content types
   public static String HTML = "text/html";
   public static String PLAIN = "text/plain";
   public static String SOAP = "application/soap+xml";

   public HTTPServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public HTTPServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   public int getKeepAliveTimeout()
   {
      return keepAliveTimeout;
   }

   public void setKeepAliveTimeout(int keepAliveTimeout)
   {
      this.keepAliveTimeout = keepAliveTimeout;
   }

   public void setNewServerSocketFactory(ServerSocketFactory serverSocketFactory){
	   newServerSocketFactory=true;
	   setServerSocketFactory(serverSocketFactory);
   }

   private void refreshServerSocket() throws IOException{
	   newServerSocketFactory=false;
	   serverSocket.close();
	   serverSocket=null;
	   InetAddress bindAddress = InetAddress.getByName(getServerBindAddress());
	   serverSocket = createServerSocket(getServerBindPort(), backlog, bindAddress);
   }

   protected void setup() throws Exception
   {
      super.setup();

      Map config = getConfiguration();
      String maxNumOfThreads = (String) config.get(MAX_NUM_HTTP_THREADS_KEY);
      if(maxNumOfThreads != null && maxNumOfThreads.length() > 0)
      {
         try
         {
            maxPoolSize = Integer.parseInt(maxNumOfThreads);
         }
         catch(NumberFormatException e)
         {
            log.error("Can not convert max number of threads value (" + maxNumOfThreads + ") into a number.");
         }
      }
      httpThreadPoolClass = (String) config.get(HTTP_THREAD_POOL_CLASS_KEY);

      String keepAliveTimeoutValue = (String) config.get(HTTP_KEEP_ALIVE_TIMEOUT_KEY);
      if(keepAliveTimeoutValue != null && keepAliveTimeoutValue.length() > 0)
      {
         try
         {
            keepAliveTimeout = Integer.parseInt(keepAliveTimeoutValue);
         }
         catch(NumberFormatException e)
         {
            log.error("Can not convert keep alive timeout value (" + keepAliveTimeoutValue + ") into a number.");
         }
      }

   }

   public void setMaxNumberOfHTTPThreads(int numOfThreads)
   {
      this.maxPoolSize = numOfThreads;
   }

   public int getMaxNumberOfHTTPThreads()
   {
      return this.maxPoolSize;
   }

   public ThreadPool getHTTPThreadPool()
   {
      if(httpThreadPool == null)
      {
         // if no thread pool class set, then use default BasicThreadPool
         if(httpThreadPoolClass == null || httpThreadPoolClass.length() == 0)
         {
            BasicThreadPool basicthreadpool = new BasicThreadPool("JBossRemoting - HTTP Server Invoker");
            basicthreadpool.setBlockingMode(BlockingMode.RUN);
            basicthreadpool.setMaximumPoolSize(maxPoolSize);
            httpThreadPool = basicthreadpool;
         }
         else
         {
            //first check to see if this is an ObjectName
            boolean isObjName = false;
            try
            {
               ObjectName objName = new ObjectName(httpThreadPoolClass);
               httpThreadPool = createThreadPoolProxy(objName);
               isObjName = true;
            }
            catch(MalformedObjectNameException e)
            {
               log.debug("Thread pool class supplied is not an object name.");
            }

            if(!isObjName)
            {
               try
               {
                  httpThreadPool = (ThreadPool) Class.forName(httpThreadPoolClass, false, getClassLoader()).newInstance();
               }
               catch(Exception e)
               {
                  throw new RuntimeException("Error loading instance of ThreadPool based on class name: " + httpThreadPoolClass);
               }
            }
         }
      }
      return httpThreadPool;
   }

   private ThreadPool createThreadPoolProxy(ObjectName objName)
   {
      ThreadPool pool;
      MBeanServer server = getMBeanServer();
      if(server != null)
      {
         ThreadPoolMBean poolMBean = (ThreadPoolMBean)
               MBeanServerInvocationHandler.newProxyInstance(server,
                                                             objName,
                                                             ThreadPoolMBean.class,
                                                             false);
         pool = poolMBean.getInstance();
      }
      else
      {
         throw new RuntimeException("Can not register MBean ThreadPool as the ServerInvoker has not been registered with a MBeanServer.");
      }
      return pool;
   }


   public void setHTTPThreadPool(ThreadPool pool)
   {
      this.httpThreadPool = pool;
   }


   public void start() throws IOException
   {
      if(!running)
      {
         try
         {
            ThreadPool httpThreadPool = getHTTPThreadPool();
            InetAddress bindAddress = InetAddress.getByName(getServerBindAddress());
            serverSocket = createServerSocket(getServerBindPort(), backlog, bindAddress);

            // prime the pool by starting up max
            for(int t = 0; t < maxPoolSize; t++)
            {
               httpThreadPool.run(this);
            }

            running = true;

         }
         catch(IOException e)
         {
            log.error("Error starting ServerSocket.  Bind port: " + getServerBindPort() + ", bind address: " + getServerBindAddress());
            throw e;
         }
      }
      super.start();
   }

   protected ServerSocket createServerSocket(int serverBindPort, int backlog, InetAddress bindAddress) throws IOException
   {
      return new ServerSocket(serverBindPort, backlog, bindAddress);
   }

   public void run()
   {
      try
      {
    	  if(newServerSocketFactory){
			  log.debug("got notice about new ServerSocketFactory");
			  try {
				  log.debug("refreshing server socket");
				  refreshServerSocket();
			  } catch (IOException e) {
				  log.debug("could not refresh server socket");
				  log.debug("message is: "+e.getMessage());
			  }
			  log.debug("server socket refreshed");

		  }
    	 Socket socket = serverSocket.accept();
         BufferedInputStream dataInput = null;
         BufferedOutputStream dataOutput = null;

         if(socket != null)
         {
            // tell the pool to start another thread to listen for more socket requests.
            httpThreadPool.run(this);

            try
            {

               boolean keepAlive = true;

               // start processing incoming request from client.  will try to keep current connection alive
               socket.setKeepAlive(true);
               socket.setSoTimeout(keepAliveTimeout);

               DataInputStream realdataInput = new DataInputStream(socket.getInputStream());
               DataOutputStream realdataOutput = new DataOutputStream(socket.getOutputStream());

               while(keepAlive)
               {
                  dataOutput = new BufferedOutputStream(realdataOutput, 512);
                  dataInput = new BufferedInputStream(realdataInput, 512);
                  keepAlive = processRequest(dataInput, dataOutput);
               }

            }
            catch(Throwable thr)
            {
               if(running)
               {
                  log.error("Error processing incoming request.", thr);
               }
            }
            finally
            {
               if(dataInput != null)
               {
                  try
                  {
                     dataInput.close();
                  }
                  catch(Exception e)
                  {
                     log.warn("Error closing resource.", e);
                  }
               }
               if(dataOutput != null)
               {
                  try
                  {
                     dataOutput.close();
                  }
                  catch(Exception e)
                  {
                     log.warn("Error closing resource.", e);
                  }
               }
               try
               {
                  socket.close();
               }
               catch(Exception e)
               {
                  log.warn("Error closing resource.", e);
               }
            }
         }
      }
      catch(Throwable thr)
      {
         if(running)
         {
            log.error("Error processing incoming request.", thr);
         }
      }
   }

   public void stop()
   {
      if(running)
      {
         running = false;

         maxPoolSize = 0; // so ServerThreads don't reinsert themselves

         try
         {
            httpThreadPool.stop(false);
            httpThreadPool.waitForTasks(2000);
         }
         catch(InterruptedException e)
         {
            log.error(e.getMessage(), e);
         }

         try
         {
            if(serverSocket != null && !serverSocket.isClosed())
            {
               serverSocket.close();
            }
            serverSocket = null;
         }
         catch(Exception e)
         {
         }
      }
      super.stop();

      log.debug("HTTPServerInvoker stopped.");
   }

   private boolean processRequest(FilterInputStream dataInput, FilterOutputStream dataOutput)
   {
      boolean keepAlive = true;
      try
      {
         Object response = null;
         boolean isError = false;
         String requestContentType = null;
         String methodType = null;
         String path = null;
         String httpVersion = null;

         InvocationRequest request = null;

         try
         {

            // Need to parse the header to find Content-Type

            /**
             * Read the first line, as this will be the POST or GET, path, and HTTP version.
             * Then next comes the headers.  (key value seperated by a ': '
             * Then followed by an empty \r\n, which will be followed by the payload
             */
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int ch;
            while((ch = dataInput.read()) >= 0)
            {
               buffer.write(ch);
               if(ch == '\n')
               {
                  break;
               }
            }

            byte[] firstLineRaw = buffer.toByteArray();
            buffer.close();
            // now need to backup and make sure the character before the \n was a \r
            if(firstLineRaw[firstLineRaw.length - 2] == '\r')
            {
               //Got our first line, now to set the variables
               String firstLine = new String(firstLineRaw).trim();
               int startIndex = 0;
               int endIndex = firstLine.indexOf(' ');
               methodType = firstLine.substring(startIndex, endIndex);
               startIndex = endIndex + 1;
               endIndex = firstLine.indexOf(' ', startIndex);
               path = firstLine.substring(startIndex, endIndex);
               startIndex = endIndex + 1;
               httpVersion = firstLine.substring(startIndex);
            }
            else
            {
               log.error("Error processing first line.  Should have ended in \r\n, but did not");
               throw new RuntimeException("Error processing HTTP request type.  First line of request is invalid.");
            }

            Map metadata = new HashMap();
            Header[] headers = HttpParser.parseHeaders(dataInput);
            for(int x = 0; x < headers.length; x++)
            {
               String headerName = headers[x].getName();
               String headerValue = headers[x].getValue();
               metadata.put(headerName, headerValue);
               // doing this instead of getting from map since ignores case
               if("Content-Type".equalsIgnoreCase(headerName))
               {
                  requestContentType = headers[x].getValue();
               }
            }

            metadata.put(HTTPMetadataConstants.METHODTYPE, methodType);
            metadata.put(HTTPMetadataConstants.PATH, path);
            metadata.put(HTTPMetadataConstants.HTTPVERSION, httpVersion);

            // checks to see if is Connection: close, which will deactivate keep alive.
            keepAlive = checkForConnecctionClose(headers);


            if(methodType.equals("OPTIONS"))
            {
               request = createNewInvocationRequest(metadata, null);
               response = invoke(request);

               Map responseMap = request.getReturnPayload();

               dataOutput.write("HTTP/1.1 ".getBytes());
               String status = "200 OK";
               dataOutput.write(status.getBytes());
               String server = "\r\n" + "Server: JBoss Remoting HTTP Server/" + Version.VERSION;
               dataOutput.write(server.getBytes());
               String date = "\r\n" + "Date: " + new Date();
               dataOutput.write(date.getBytes());
               String contentLength = "\r\n" + "Content-Length: 0";
               dataOutput.write(contentLength.getBytes());

               if(responseMap != null)
               {
                  Set entries = responseMap.entrySet();
                  Iterator itr = entries.iterator();
                  while(itr.hasNext())
                  {
                     Map.Entry entry = (Map.Entry) itr.next();
                     String entryString = "\r\n" + entry.getKey() + ": " + entry.getValue();
                     dataOutput.write(entryString.getBytes());
                  }
               }

               String close = "\r\n" + "Connection: close";
               dataOutput.write(close.getBytes());

               // content seperator
               dataOutput.write(("\r\n" + "\r\n").getBytes());

               dataOutput.flush();

               //nothing more to do since do not need to call handler for this one
               return keepAlive;

            }
            else if(methodType.equals("GET") || methodType.equals("HEAD"))
            {
               request = createNewInvocationRequest(metadata, null);
            }
            else // must be POST or PUT
            {
               UnMarshaller unmarshaller = getUnMarshaller();
               Object obj = unmarshaller.read(dataInput, metadata);


               if(obj instanceof InvocationRequest)
               {
                  request = (InvocationRequest) obj;
               }
               else
               {
                  if(WebUtil.isBinary(requestContentType))
                  {
                     request = getInvocationRequest(metadata, obj);
                  }
                  else
                  {
                     request = createNewInvocationRequest(metadata, obj);
                  }
               }
            }

            try
            {
               // call transport on the subclass, get the result to handback
               response = invoke(request);
            }
            catch(Throwable ex)
            {
               log.debug("Error thrown calling invoke on server invoker.", ex);
               response = ex;
               isError = true;
            }
         }
         catch(Throwable thr)
         {
            log.debug("Error thrown processing request.  Probably error with processing headers.", thr);
            if(thr instanceof SocketException)
            {
               log.error("Error processing on socket.", thr);
               keepAlive = false;
               return keepAlive;
            }
            else if(thr instanceof Exception)
            {
               response = (Exception) thr;
            }
            else
            {
               response = new Exception(thr);
            }
            isError = true;
         }

         if(dataOutput != null)
         {

            Map responseMap = null;

            if(request != null)
            {
               responseMap = request.getReturnPayload();
            }

            if(response == null)
            {
               dataOutput.write("HTTP/1.1 ".getBytes());
               String status = "204 No Content";
               if(responseMap != null)
               {
                  String handlerStatus = (String) responseMap.get(HTTPMetadataConstants.RESPONSE_CODE);
                  if(handlerStatus != null)
                  {
                     status = handlerStatus;
                  }
               }
               dataOutput.write(status.getBytes());
               String contentType = "\r\n" + "Content-Type" + ": " + "text/html";
               dataOutput.write(contentType.getBytes());
               String contentLength = "\r\n" + "Content-Length" + ": " + 0;
               dataOutput.write(contentLength.getBytes());
               if(responseMap != null)
               {
                  Set entries = responseMap.entrySet();
                  Iterator itr = entries.iterator();
                  while(itr.hasNext())
                  {
                     Map.Entry entry = (Map.Entry) itr.next();
                     String entryString = "\r\n" + entry.getKey() + ": " + entry.getValue();
                     dataOutput.write(entryString.getBytes());
                  }
               }

               dataOutput.write(("\r\n" + "\r\n").getBytes());
               //dataOutput.write("NULL return".getBytes());
               dataOutput.flush();
            }
            else
            {
               dataOutput.write("HTTP/1.1 ".getBytes());
               String status = null;
               if(isError)
               {
                  status = "500 JBoss Remoting: Error occurred within target application.";
               }
               else
               {
                  status = "200 OK";
                  if(responseMap != null)
                  {
                     String handlerStatus = (String) responseMap.get(HTTPMetadataConstants.RESPONSE_CODE);
                     if(handlerStatus != null)
                     {
                        status = handlerStatus;
                     }
                  }
               }

               dataOutput.write(status.getBytes());

               // write return headers
               String contentType = "\r\n" + "Content-Type" + ": " + requestContentType;
               dataOutput.write(contentType.getBytes());
               int iContentLength = getContentLength(response);
               String contentLength = "\r\n" + "Content-Length" + ": " + iContentLength;
               dataOutput.write(contentLength.getBytes());

               if(responseMap != null)
               {
                  Set entries = responseMap.entrySet();
                  Iterator itr = entries.iterator();
                  while(itr.hasNext())
                  {
                     Map.Entry entry = (Map.Entry) itr.next();
                     String entryString = "\r\n" + entry.getKey() + ": " + entry.getValue();
                     dataOutput.write(entryString.getBytes());
                  }
               }

               // content seperator
               dataOutput.write(("\r\n" + "\r\n").getBytes());

               if(methodType != null && !methodType.equals("HEAD"))
               {
                  // write response
                  Marshaller marshaller = getMarshaller();
                  marshaller.write(response, dataOutput);
               }
            }
         }
         else
         {
            if(isError)
            {
               log.warn("Can not send error response due to output stream being null (due to previous error).");
            }
            else
            {
               log.error("Can not send response due to output stream being null (even though there was not a previous error encountered).");
            }
         }
      }
      catch(Exception e)
      {
         log.error("Error processing client request.", e);
         keepAlive = false;
      }

      return keepAlive;
   }

   private boolean checkForConnecctionClose(Header[] headers)
   {
      boolean keepAlive = true;

      if(headers != null)
      {
         for(int x = 0; x < headers.length; x++)
         {
            String name = headers[x].getName();
            if("Connection".equals(name))
            {
               String value = headers[x].getValue();
               if(value != null)
               {
                  if("close".equalsIgnoreCase(value))
                  {
                     keepAlive = false;
                     break;
                  }
               }
               else
               {
                  try
                  {
                     HeaderElement[] hdrElements = headers[x].getValues();
                     if(hdrElements != null)
                     {
                        for(int i = 0; i < hdrElements.length; i++)
                        {
                           NameValuePair pair = hdrElements[i].getParameterByName("Connection");
                           if("close".equalsIgnoreCase(pair.getValue()))
                           {
                              keepAlive = false;
                              break;
                           }
                        }
                     }
                  }
                  catch(HttpException e)
                  {
                     log.error(e.getMessage(), e);
                  }
               }
            }
         }
      }

      return keepAlive;
   }
}
