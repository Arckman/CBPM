package org.jboss.ws.extensions.eventing.mgmt;

import java.net.URI;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

import org.jboss.ws.WSException;
import org.w3c.dom.Element;

/**
 * Event dispatching delegate that will be bound to JNDI.
 *
 * @see DispatcherFactory
 * 
 * @author Heiko Braun, <heiko@openj.net>
 * @since 11-Jan-2006
 */
public class DispatcherDelegate implements EventDispatcher, Referenceable
{
   private String hostname;
   public final static String MANAGER_HOSTNAME = "manager.hostname";
   private SubscriptionManagerMBean subscriptionManager = null;

   public DispatcherDelegate()
   {
   }

   public DispatcherDelegate(String hostname)
   {
      setHostname(hostname);
   }

   public void dispatch(URI eventSourceNS, Element payload)
   {
      getSubscriptionManager().dispatch(eventSourceNS, payload);
   }

   public Reference getReference() throws NamingException
   {

      Reference myRef = new Reference(DispatcherDelegate.class.getName(), DispatcherFactory.class.getName(), null);

      // let the delegate now where to find the subscription manager
      myRef.add(new StringRefAddr(MANAGER_HOSTNAME, hostname));

      return myRef;
   }

   private SubscriptionManagerMBean getSubscriptionManager()
   {
      if (null == subscriptionManager)
      {
         try
         {
            ObjectName objectName = SubscriptionManager.OBJECT_NAME;
            subscriptionManager = (SubscriptionManagerMBean)MBeanServerInvocationHandler.newProxyInstance(getServer(), objectName, SubscriptionManagerMBean.class,
                  false);
         }
         catch (Exception e)
         {
            throw new WSException("Failed to access subscription manager: " + e.getMessage());
         }
      }

      return subscriptionManager;
   }

   private MBeanServerConnection getServer() throws NamingException
   {
      // todo: bypass rmi adapter when used locally
      InitialContext iniCtx = new InitialContext();
      MBeanServerConnection server = (MBeanServerConnection)iniCtx.lookup("jmx/invoker/RMIAdaptor");
      return server;
   }

   void setHostname(String hostname)
   {
      if (null == hostname)
         throw new IllegalArgumentException("Hostname may not be null");
      this.hostname = hostname;
   }
}
