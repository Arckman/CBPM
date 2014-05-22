package org.jbpm.bpel.frj.monitor.locator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocatorUnit {
	private String name;//process name
	private String ip;
	private Set<LinkUnit> incomes=new HashSet<LinkUnit>(10);//process name,plt
	private Set<LinkUnit> outgoes=new HashSet<LinkUnit>(10);//process name,plt
	public LocatorUnit(String name){
		this.name=name;
	}
	public LocatorUnit(String name,String ip){
		this.name=name;
		this.ip=ip;
	}
	public void addIncome(String income,String plt){
		incomes.add(new LinkUnit(income,plt));
	}
	public void addOutgo(String outgo,String plt){outgoes.add(new LinkUnit(outgo,plt));}
	public Set<LinkUnit> getIncomes(){return incomes;}
	public Set<LinkUnit> getOutgoes(){return outgoes;}
	public String getProcessName(){return name;}
	public String getIP(){return ip;}
	@Override
	public String toString(){
		StringBuffer s=new StringBuffer();
		s.append("(LocatorUnit)"+name);
		s.append(":"+ip+"\t\n");
		s.append("Income:"+incomes.toString()+"\t\n");
		s.append("Outgoes:"+outgoes.toString()+"\t\n");
		return s.toString();
	}
	
	public class LinkUnit{
		private String processName;
		private String plt;
		public LinkUnit(String processName,String plt){
			this.plt=plt;
			this.processName=processName;
		}
		public String getProcessName() {
			return processName;
		}
		public String getPlt() {
			return plt;
		}
		@Override
		public String toString(){
			StringBuffer s=new StringBuffer();
			s.append(processName+"("+plt+")");
			return s.toString();
		}
	}
}
