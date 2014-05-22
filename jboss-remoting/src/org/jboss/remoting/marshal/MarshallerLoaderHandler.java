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

package org.jboss.remoting.marshal;

import java.util.Map;
import javax.management.MBeanServer;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.loading.ClassBytes;
import org.jboss.remoting.loading.ClassUtil;

/**
 * The invocation handler that receives requests for getting marshallers/unmarshallers and
 * loading of these and any related classes to remoting client.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class MarshallerLoaderHandler implements ServerInvocationHandler, MarshallerLoaderConstants
{
   private ServerInvoker invoker = null;
   private MBeanServer server = null;

   protected final static Logger log = Logger.getLogger(MarshallerLoaderHandler.class);


   /**
    * set the mbean server that the handler can reference
    *
    * @param server
    */
   public void setMBeanServer(MBeanServer server)
   {
      this.server = server;
   }

   /**
    * set the invoker that owns this handler
    *
    * @param invoker
    */
   public void setInvoker(ServerInvoker invoker)
   {
      this.invoker = invoker;
   }

   /**
    * called to handle a specific invocation.  Please take care to make sure implementations are thread safe and can,
    * and often will, receive concurrent calls on this method.
    *
    * @param invocation
    * @return
    * @throws Throwable
    */
   public Object invoke(InvocationRequest invocation)
         throws Throwable
   {
      Object ret = null;

      Object param = invocation.getParameter();
      Map metadMap = invocation.getRequestPayload();
      if(metadMap == null)
      {
         throw new RuntimeException("Can not load class as invocation request metadat map is null.");
      }

      String dataType = (String) metadMap.get(InvokerLocator.DATATYPE);

      log.debug("MarshallerLoaderHandler received invocation with param of " + param + " and data type of " + dataType);

      if(GET_MARSHALLER_METHOD.equals(param))
      {
         ret = MarshalFactory.getMarshaller(dataType, invoker.getSerializationType());
      }
      else if(GET_UNMARSHALLER_METHOD.equals(param))
      {
         ret = MarshalFactory.getUnMarshaller(dataType, invoker.getSerializationType());
      }
      else if(LOAD_CLASS_METHOD.equals(param))
      {
         String className = (String) metadMap.get(CLASSNAME);
         if(className != null)
         {
            ret = loadClassBytes(className, invoker.getClassLoader());
         }
         else
         {
            log.error("Received invocation " + param + " to load class, but metadata map key " + CLASSNAME +
                      " contains a null value for the class name to load.");
         }
      }
      else if(LOAD_MARSHALLER_METHOD.equals(param))
      {
         // load based on data type
         Marshaller marshaller = MarshalFactory.getMarshaller(dataType, invoker.getSerializationType());
         if(marshaller != null)
         {
            String className = marshaller.getClass().getName();
            ret = loadClassBytes(className, invoker.getClassLoader());
         }
         else
         {
            log.warn("Could not find registered marshaller for data type: " + dataType);
         }
      }
      else if(LOAD_UNMARSHALLER_METHOD.equals(param))
      {
         UnMarshaller unmarshaller = MarshalFactory.getUnMarshaller(dataType, invoker.getSerializationType());
         if(unmarshaller != null)
         {
            String className = unmarshaller.getClass().getName();
            ret = loadClassBytes(className, invoker.getClassLoader());
         }
         else
         {
            log.warn("Could not find registered unmarshaller for data type: " + dataType);
         }
      }
      else
      {
         log.warn("Received invocation with unknown parameter request: " + param);
      }


      return ret;
   }

   private Object loadClassBytes(String className, ClassLoader classLoader)
   {
      ClassBytes classBytes = null;

      if(className != null)
      {
         byte[] classDefinition = ClassUtil.getClassBytes(className, classLoader);
         classBytes = new ClassBytes(className, classDefinition);
      }
      return classBytes;
   }

   /**
    * Adds a callback handler that will listen for callbacks from the server invoker handler.
    *
    * @param callbackHandler
    */
   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      //NO OP as don't won't allow listeners
   }

   /**
    * Removes the callback handler that was listening for callbacks from the server invoker handler.
    *
    * @param callbackHandler
    */
   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      //NO OP as don't won't allow listeners
   }
}
