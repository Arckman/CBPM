package org.jbpm.bpel.frj.monitor;

/**
 * information of parent can be removed, using root information instead anywhere
 * @author frj
 *
 */
public class DynamicDependency {
	private String sourceMonitorName;
	private String targetMonitorName;
	private String rootMonitorName=null;
	private long rootInstanceId=-1;
//	private String parentMonitorName=null;
//	private long parentInstanceId=-1;
	private String type;
	public DynamicDependency(){}
	public DynamicDependency(String source,String target,String type){
		setSourceMonitorName(source);
		setTargetMonitorName(target);
		setType(type);
	}
	public DynamicDependency(String source,String target,String root,long rootId,String type){
		setSourceMonitorName(source);
		setTargetMonitorName(target);
		setRootMonitorName(root);
		setRootInstanceId(rootId);
		setType(type);
	}
	public DynamicDependency(DynamicDependency dd){
		sourceMonitorName=dd.sourceMonitorName;
		targetMonitorName=dd.targetMonitorName;
		rootInstanceId=dd.rootInstanceId;
		rootMonitorName=dd.rootMonitorName;
		type=dd.type;
	}
	public String getSourceMonitorName(){
		return sourceMonitorName;
	}
	public void setSourceMonitorName(String sourceMonitorName) {
		this.sourceMonitorName = sourceMonitorName;
	}
	public String getTargetMonitorName() {
		return targetMonitorName;
	}
	public void setTargetMonitorName(String targetMonitorName) {
		this.targetMonitorName = targetMonitorName;
	}
	public String getRootMonitorName() {
		return rootMonitorName;
	}
	public void setRootMonitorName(String rootMonitorName) {
		this.rootMonitorName = rootMonitorName;
	}
	public long getRootInstanceId() {
		return rootInstanceId;
	}
	public void setRootInstanceId(long rootInstanceId) {
		this.rootInstanceId = rootInstanceId;
	}
	
//	public String getParentMonitorName() {
//		return parentMonitorName;
//	}
//	public void setParentMonitorName(String parentMonitorName) {
//		this.parentMonitorName = parentMonitorName;
//	}
//	public long getParentInstanceId() {
//		return parentInstanceId;
//	}
//	public void setParentInstanceId(long parentInstanceId) {
//		this.parentInstanceId = parentInstanceId;
//	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public boolean equalRootTX(DynamicDependency dd){
		if(rootMonitorName==null||dd.rootMonitorName==null)
			return rootMonitorName==null&&dd.rootMonitorName==null&&rootInstanceId==-1&&dd.rootInstanceId==-1;
		else
			return rootMonitorName.equals(dd.rootMonitorName)&&rootInstanceId==dd.rootInstanceId;
	}
	@Override
	public boolean equals(Object o){
		if(o instanceof DynamicDependency){
			DynamicDependency dd=(DynamicDependency)o;
			boolean b=sourceMonitorName.equals(dd.sourceMonitorName)&&targetMonitorName.equals(dd.targetMonitorName)&&
					(type==null?dd.type==null:type.equals(dd.type));
//			if(!(rootMonitorName==null||dd.rootMonitorName==null))
				b&=rootMonitorName.equals(dd.rootMonitorName)&&rootInstanceId==dd.rootInstanceId;
			return b;
		}
		return false;
	}
	@Override
	public int hashCode(){
		int code=sourceMonitorName.hashCode()^targetMonitorName.hashCode()^rootMonitorName.hashCode()^(int)rootInstanceId;
		if(type==null)
			return code;
		else
			return code^type.hashCode();
	}
	@Override
	public DynamicDependency clone(){return new DynamicDependency(this);}
	
	@Override
	public String toString(){return sourceMonitorName+"==["+type+":"+rootMonitorName+"("+rootInstanceId+")]==>>"+targetMonitorName;}
	
}
