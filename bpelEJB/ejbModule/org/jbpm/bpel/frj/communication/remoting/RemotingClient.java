package org.jbpm.bpel.frj.communication.remoting;

import java.net.MalformedURLException;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jbpm.bpel.frj.monitor.StaticEdge;

public class RemotingClient{
	private String ip="localhost";
	private final static String transport="socket";
	private final static String port="54003";
	public void makeInvocation(RemotingCommunicator c){
		String locatorURL=transport+"://"+ip+":"+port;
		InvokerLocator invokerLocator;
		try {
			invokerLocator = new InvokerLocator(locatorURL);
			Client client=new Client(invokerLocator);
			client.connect();
			client.invokeOneway(c);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void makeInvocation(RemotingCommunicator c,String ip){
		this.ip=ip;
		makeInvocation(c);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RemotingClient c=new RemotingClient();
		RemotingCommunicator o=new RemotingCommunicator("a","b");
		o.setCommand("hello");o.setMsg(new StaticEdge("c","d"));
		c.makeInvocation(o, "114.212.189.157");
	}

}
