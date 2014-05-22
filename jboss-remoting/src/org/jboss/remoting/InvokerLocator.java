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

package org.jboss.remoting;

import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.transport.ClientInvoker;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * InvokerLocator is an object that indentifies a specific Invoker on the network, via a unique
 * locator URI. The locator URI is in the format: <P>
 * <p/>
 * <tt>protocol://host[:port][/path[?param=value&param2=value2]]</tt>     <P>
 * <p/>
 * For example, a http based locator might be: <P>
 * <p/>
 * <tt>http://192.168.10.1:8081</tt>  <P>
 * <p/>
 * An example Socket based locator might be: <P>
 * <p/>
 * <tt>socket://192.168.10.1:9999</tt>  <P>
 * <p/>
 * An example RMI based locator might be: <P>
 * <p/>
 * <tt>rmi://localhost</tt>  <P>
 * <p/>
 * NOTE: the hostname will automatically be resolved to the outside IP address of the local machine
 * if <tt>localhost</tt> or <tt>127.0.0.1</tt> is used as the hostname in the URI.  If it cannot be
 * determined or resolved, it will use what was passed.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 1.22 $
 */
public class InvokerLocator implements Serializable
{
   static final long serialVersionUID;

   protected String protocol;
   protected String host;
   protected int port;
   protected String path;
   protected Map parameters;
   private String uri;
   private String originalURL;


   static
   {
      if(Version.getDefaultVersion() == Version.VERSION_1)
      {
         serialVersionUID = -2909329895029296248L;
      }
      else
      {
         serialVersionUID = -4977622166779282521L;
      }
   }

   /**
    * Indicates should address binding to all network interfaces (i.e. 0.0.0.0)
    */
   private static final String ANY = "0.0.0.0";
   /**
    * Constant value for server bind address system property.  Value is 'jboss.bind.address'.
    */
   private static final String SERVER_BIND_ADDRESS = "jboss.bind.address";

   /**
    * Public key to use when want to specify that locator look up local address by
    * IP or host name.
    */
   public static final String BIND_BY_HOST = "remoting.bind_by_host";


   /**
    * Constant to define the param name to be used when defining the data type.
    */
   public static final String DATATYPE = "datatype";
   public static final String DATATYPE_CASED = "dataType";

   /**
    * Constant to define the param name to be used when defining the serialization type.
    */
   public static final String SERIALIZATIONTYPE = "serializationtype";
   public static final String SERIALIZATIONTYPE_CASED = "serializationType";

   /**
    * Constant to define the param name to be used when defining the marshaller fully qualified classname
    */
   public static final String MARSHALLER = "marshaller";
   /**
    * Constant to define the param name to be used when defining the unmarshaller fully qualified classname
    */
   public static final String UNMARSHALLER = "unmarshaller";

   /**
    * Constant to define what port the marshalling loader port resides on.
    */
   public static final String LOADER_PORT = "loaderport";

   /**
    * Constant to define the param name to be used when defining if marshalling should be by value,
    * which means will be using local client invoker with cloning of payload value.
    */
   public static final String BYVALUE = "byvalue";

   /**
    * Constant to define the param name to be used when defining if marshalling should use
    * remote client invoker instead of using local client invoker (even though both client and
    * server invokers are within same JVM).
    */
   public static final String FORCE_REMOTE = "force_remote";   

   /**
    * Constant to define if client should try to automatically establish a
    * lease with the server.  Value for this parameter key should be either 'true' or 'false'.
    */
   public static final String CLIENT_LEASE = "leasing";

   /**
    * Constant to define what the client lease period should be in the case that
    * server side leasing is turned on.  Value for this parameter key should be the number
    * of milliseconds to wait before each client lease renewal and must be greater than zero
    * in order to be recognized.
    */
   public static final String CLIENT_LEASE_PERIOD = "lease_period";

   /**
    * Constructs the object used to identify a remoting server via simple uri format string (e.g. socket://myhost:7000).
    * Note: the uri passed may not always be the one returned via call to getLocatorURI() as may need to change if
    * port not specified, host is 0.0.0.0, etc.  If need original uri that is passed to this constructor, need to
    * call getOriginalURI().
    * @param uri
    * @throws MalformedURLException
    */
   public InvokerLocator(String uri)
         throws MalformedURLException
   {
      originalURL = uri;
      int i = uri.indexOf("://");
      if(i < 0)
      {
         throw new MalformedURLException("Invalid url " + uri);
      }
      String tmp = uri.substring(i + 3);
      this.protocol = uri.substring(0, i);
      i = tmp.indexOf("/");
      int p = tmp.indexOf(":");
      if(i != -1)
      {
         p = (p < i ? p : -1);
      }
      if(p != -1)
      {
         host = resolveHost(tmp.substring(0, p).trim());
         if(i > -1)
         {
            port = Integer.parseInt(tmp.substring(p + 1, i));
         }
         else
         {
            port = Integer.parseInt(tmp.substring(p + 1));
         }
      }
      else
      {
         if(i > -1)
         {
            host = resolveHost(tmp.substring(0, i).trim());
         }
         else
         {
            host = resolveHost(tmp.substring(0).trim());
         }
         port = -1;
      }

      // now look for any path
      p = tmp.indexOf("?");
      if(p != -1)
      {
         path = tmp.substring(i + 1, p);
         String args = tmp.substring(p + 1);
         StringTokenizer tok = new StringTokenizer(args, "&");
         parameters = new TreeMap();
         while(tok.hasMoreTokens())
         {
            String token = tok.nextToken();
            int eq = token.indexOf("=");
            String name = (eq > -1) ? token.substring(0, eq) : token;
            String value = (eq > -1) ? token.substring(eq + 1) : "";
            parameters.put(name, value);
         }
      }
      else
      {
         p = tmp.indexOf("/");
         if(p != -1)
         {
            path = tmp.substring(p + 1);
         }
         else
         {
            path = "";
         }
      }
      // rebuild it, since the host probably got resolved and the port changed
      this.uri = protocol + "://" + this.host + ((port > -1) ? (":" + port) : "") + "/" + path + ((parameters != null) ? "?" : "");
      if(parameters != null)
      {
         Iterator iter = parameters.keySet().iterator();
         while(iter.hasNext())
         {
            String key = (String) iter.next();
            String val = (String) parameters.get(key);
            this.uri += key + "=" + val;
            if(iter.hasNext())
            {
               this.uri += "&";
            }
         }
      }
   }

   private static final String resolveHost(String host)
   {
      if(host.indexOf("0.0.0.0") != -1)
      {
         if(System.getProperty(SERVER_BIND_ADDRESS, "0.0.0.0").equals("0.0.0.0"))
         {
            host = fixRemoteAddress(host);
         }
         else
         {
            host = host.replaceAll("0\\.0\\.0\\.0", System.getProperty(SERVER_BIND_ADDRESS));
         }
      }
         return host;
   }

   private static String fixRemoteAddress(String address)
   {
      try
      {
         if(address == null || ANY.equals(address))
         {
            boolean byHost = true;
            String bindByHost = System.getProperty(BIND_BY_HOST, "True");
            try
            {
               byHost = Boolean.getBoolean(bindByHost);
            }
            catch(Exception e)
            {
            }
            if(byHost)
            {
               return InetAddress.getLocalHost().getHostName();
            }
            else
            {
               return InetAddress.getLocalHost().getHostAddress();
            }
         }
      }
      catch(UnknownHostException ignored)
      {
      }
      return address;
   }

   /**
    * Constructs the object used to identify a remoting server.
    * @param protocol
    * @param host
    * @param port
    * @param path
    * @param parameters
    */
   public InvokerLocator(String protocol, String host, int port, String path, Map parameters)
   {
      this.protocol = protocol;
      this.host = resolveHost(host);
      this.port = port;
      this.path = path;
      this.parameters = parameters;

      this.uri = protocol + "://" + this.host + ((port > -1) ? (":" + port) : "") + "/" + path + ((parameters != null) ? "?" : "");
      if(parameters != null)
      {
         Iterator iter = parameters.keySet().iterator();
         while(iter.hasNext())
         {
            String key = (String) iter.next();
            String val = (String) parameters.get(key);
            this.uri += key + "=" + val;
            if(iter.hasNext())
            {
               this.uri += "&";
            }
         }
      }
      originalURL = uri;
   }

   public int hashCode()
   {
      return uri.hashCode();
   }

   /**
    * Compares to see if Object passed is of type InvokerLocator and
    * it's internal locator uri hashcode is same as this one.  Note, this
    * means is testing to see if not only the host, protocol, and port are the
    * same, but the path and parameters as well.  Therefore 'socket://localhost:9000'
    * and 'socket://localhost:9000/foobar' would NOT be considered equal.
    * @param obj
    * @return
    */
   public boolean equals(Object obj)
   {
      return obj != null && obj instanceof InvokerLocator && uri.equals(((InvokerLocator)obj).getLocatorURI());
   }

   /**
    * Compares to see if InvokerLocator passed represents the same physical remoting server
    * endpoint as this one.  Unlike the equals() method, this just tests to see if protocol, host,
    * and port are the same and ignores the path and parameters.
    * @param compareMe
    * @return
    */
   public boolean isSameEndpoint(InvokerLocator compareMe)
   {
      return compareMe != null && getProtocol().equalsIgnoreCase(compareMe.getProtocol()) &&
             getHost().equalsIgnoreCase(compareMe.getHost()) && getPort() == compareMe.getPort();
   }

   /**
    * return the locator URI, in the format: <P>
    * <p/>
    * <tt>protocol://host[:port][/path[?param=value&param2=value2]]</tt>
    * Note, this may not be the same as the original uri passed as parameter to the constructor.
    * @return
    */
   public String getLocatorURI()
   {
      return uri;
   }

   public String getProtocol()
   {
      return protocol;
   }

   public String getHost()
   {
      return host;
   }

   public int getPort()
   {
      return port;
   }

   public String getPath()
   {
      return path;
   }

   public Map getParameters()
   {
      return parameters;
   }

   public String toString()
   {
      return "InvokerLocator [" + uri + "]";
   }

   /**
    * Gets the original uri passed to constructor (if there was one).
    * @return
    */
   public String getOriginalURI()
   {
      return originalURL;
   }

   /**
    * narrow this invoker to a specific RemoteClientInvoker instance
    *
    * @return
    * @throws Exception
    */
   public ClientInvoker narrow() throws Exception
   {
      return InvokerRegistry.createClientInvoker(this);
   }


   public String findSerializationType()
   {
      String serializationTypeLocal = SerializationStreamFactory.JAVA;
      if(parameters != null)
      {
         serializationTypeLocal = (String) parameters.get(SERIALIZATIONTYPE);
         if(serializationTypeLocal == null)
         {
            serializationTypeLocal = (String) parameters.get(InvokerLocator.SERIALIZATIONTYPE_CASED);
            if(serializationTypeLocal == null)
            {
               serializationTypeLocal = SerializationStreamFactory.JAVA;
            }
         }
      }

      return serializationTypeLocal;
   }


}
