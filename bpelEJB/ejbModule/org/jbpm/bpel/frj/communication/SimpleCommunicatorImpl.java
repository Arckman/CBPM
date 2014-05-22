package org.jbpm.bpel.frj.communication;

import java.io.Serializable;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.frj.VersionControlManager;
import org.jbpm.bpel.frj.monitor.StaticEdge;

public class SimpleCommunicatorImpl implements Communicator,Serializable{
	protected String source;
	protected String target;
	protected String command;
	protected Object msg;
	public SimpleCommunicatorImpl(){}
	public SimpleCommunicatorImpl(String source,String target){
		this.source=source;
		this.target=target;
	}
	public SimpleCommunicatorImpl(String source,String target,String c,Object msg){
		this.source=source;
		this.target=target;
		this.command=c;
		this.msg=msg;
	}
	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public Object getMsg() {
		return msg;
	}
	public void setMsg(Object msg) {
		this.msg = msg;
	}
	public void send(VersionControlManager vm) {
		// TODO Auto-generated method stub
//		System.out.println(source+" sending command["+command+"]("+msg+")] to "+target);
		Thread t=new Thread(){
			public void run(){
				receive();
			}
		};
		t.start();
	}
	public void receive() {
		// TODO Auto-generated method stub
		JbpmConfiguration.getVersionControlManager().receive(this);
	}
	@Override
	public String toString(){
		StringBuffer s=new StringBuffer();
		s.append("This is a communicator---------------------------------------------\n\t");
		s.append("From "+source+" to "+target+" sending cmd:"+command+"\t\n");
		s.append("Carry message: "+msg+"\t\n");
		s.append("///////////////////////////////////////////////////////////////////");
		return s.toString();
	}
}
