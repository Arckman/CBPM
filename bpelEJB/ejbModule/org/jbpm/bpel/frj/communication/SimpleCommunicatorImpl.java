package org.jbpm.bpel.frj.communication;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.frj.monitor.ProcessMonitor;

public class SimpleCommunicatorImpl implements Communicator{
	private String source;
	private String target;
	private String command;
	private Object msg;
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
	public void send() {
		// TODO Auto-generated method stub
		System.out.println("Sending msg: "+source+" to "+target+" using cmd ["+command+"]("+msg+")");
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
}
