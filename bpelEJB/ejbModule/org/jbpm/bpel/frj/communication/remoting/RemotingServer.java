package org.jbpm.bpel.frj.communication.remoting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import javax.management.MBeanServer;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jbpm.JbpmConfiguration;

public class RemotingServer {
	private static String host="localhost";
	private static String transport="socket";
	private static String port="54003";
	public RemotingServer(){
		start();
	}
	private void start(){
		String path=".."+File.separator+"server"+File.separator+"default"+
				File.separator;
		String f="vm.properties";
		File file=new File(path+f);
		Properties properties=new Properties();
		try {
			properties.load(new FileReader(file));
			host=properties.getProperty("localIP");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String locatorURL=transport+"://"+host+":"+port;
		Connector connector=null;
		try {
			InvokerLocator invokerLocator=new InvokerLocator(locatorURL);
			connector=new Connector(invokerLocator);
			connector.create();
			RemotingInvocationHandler handler=new RemotingInvocationHandler();
			connector.addInvocationHandler("remoting", handler);
			connector.start();
			System.out.println("Jboss Remoting start listening on "+locatorURL);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
//			if(connector!=null)
//				connector.stop();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RemotingServer server=new RemotingServer();
//		server.start();
	}
	public class RemotingInvocationHandler implements ServerInvocationHandler{

		public void setMBeanServer(MBeanServer server) {
			// TODO Auto-generated method stub
			
		}

		public void setInvoker(ServerInvoker invoker) {
			// TODO Auto-generated method stub
			
		}

		public Object invoke(InvocationRequest invocation) throws Throwable {
			// TODO Auto-generated method stub
			RemotingCommunicator c=(RemotingCommunicator)invocation.getParameter();
//			System.out.println("We get "+c);
			c.receive();
			return null;
		}

		public void addListener(InvokerCallbackHandler callbackHandler) {
			// TODO Auto-generated method stub
			
		}

		public void removeListener(InvokerCallbackHandler callbackHandler) {
			// TODO Auto-generated method stub
			
		}
		
	}

}
