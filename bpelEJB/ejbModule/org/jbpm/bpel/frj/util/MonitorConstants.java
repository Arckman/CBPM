package org.jbpm.bpel.frj.util;

public class MonitorConstants {
	//setup state
	public final static String STATE_NORMAL="normal";
	public final static String STATE_ONDEMAND="ondemand";
	public final static String STATE_SETUP="setup";
	public final static String STATE_VALID="valid";
	public final static String STATE_UPDATE="update";
	
	//update strategy
	public final static String STRATEGY_WAIT="wait";
	public final static String STRATEGY_CONCURRENT="concurrent";
	public final static String STRATEGY_BLOCK="block";
	//dynamic dependency type
	public final static String DYNAMICDEPENDENCY_FUTURE="future";
	public final static String DYNAMICDEPENDENCY_PAST="past";
	//method
	public final static String METHOD_VC="VersionConsistency";
	public final static String METHOD_VERSION="versioning";
	public final static String METHOD_QUIESCENCE="quiescence";
}
