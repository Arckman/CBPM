package org.jbpm.bpel.frj.monitor;

import java.io.Serializable;

public class StaticEdge implements Serializable{
//	private ProcessMonitor sourceProcessMonitor=null;
//	private ProcessMonitor targetProcessMonitor=null;
	private String source;
	private String target;
	
	public StaticEdge(){}
	public StaticEdge(String source,String target){
		this.source=source;
		this.target=target;
	}
	public StaticEdge(StaticEdge edge){
		this.source=edge.source;
		this.target=edge.target;
	}
//	public ProcessMonitor getSourceProcessMonitor() {
//		return sourceProcessMonitor;
//	}
//	public void setSourceProcessMonitor(ProcessMonitor sourceProcessMonitor) {
//		this.sourceProcessMonitor = sourceProcessMonitor;
//	}
//	public ProcessMonitor getTargetProcessMonitor() {
//		return targetProcessMonitor;
//	}
//	public void setTargetProcessMonitor(ProcessMonitor targetProcessMonitor) {
//		this.targetProcessMonitor = targetProcessMonitor;
//	}
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
	@Override
	public boolean equals(Object o){
		if(o instanceof StaticEdge){
			StaticEdge edge=(StaticEdge)o;
			return this.source.equals(edge.source)&&target.equals(edge.target);
		}else
			return false;
	}
	@Override
	public int hashCode(){
		return source.hashCode()^target.hashCode();
	}
	@Override
	public StaticEdge clone(){return new StaticEdge(this);}
	@Override
	public String toString(){return "Static edge: "+source+" ----> "+target;}
}
