package org.jboss.remoting.transport.multiplex;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.ServerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TransportServerFactory implements ServerFactory
{
   public ServerInvoker createServerInvoker(InvokerLocator locator, Map config)
         throws IOException
   {
      return new MultiplexServerInvoker(locator, config);
   }

   public boolean supportsSSL()
   {
      return false;
   }
}
