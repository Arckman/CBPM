package org.jbpm.bpel.frj.communication.remoting;

import java.io.Serializable;

import org.jbpm.bpel.frj.VersionControlManager;
import org.jbpm.bpel.frj.communication.SimpleCommunicatorImpl;

public class RemotingCommunicator extends SimpleCommunicatorImpl implements Serializable {
	private transient String targetIP;
	public RemotingCommunicator(){
		super();
	}
	public RemotingCommunicator(String source, String target) {
		// TODO Auto-generated constructor stub
		super(source,target);
	}
	@Override
	public void send(VersionControlManager vm){
		targetIP=vm.getLocatorUnit(target).getIP();
//		String targetIP="114.212.189.157";
		RemotingClient client=new RemotingClient();
		client.makeInvocation(this,targetIP);
	}
	
	@Override
	public String toString(){
		StringBuffer s=new StringBuffer();
		s.append("This is a communicator---------------------------------------------\n\t");
		s.append("From "+source+" to "+target+"("+targetIP+") sending cmd:"+command+"\t\n");
		s.append("Carry message: "+msg+"\t\n");
		s.append("///////////////////////////////////////////////////////////////////");
		return s.toString();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RemotingCommunicator c=new RemotingCommunicator();
		c.send(null);
	}

}
