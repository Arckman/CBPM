package org.jbpm.bpel.frj.util;
/**
 * for communication constants
 * @author frj
 *
 */
public class ComConstants {

	//static edge
	public final static String STATICEDGE_ADD="staticEdge_add";
	public final static String STATICEDGE_REMOVE="staticEdge_remove";
	
	//scope
	public final static String SCOPE_REQUEST="scope_request";
	public final static String SCOPE_ACK="scope_ack";
	
	//setup
	public final static String SETUP_REQUEST="setup_request";
	public final static String SETUP_ACK="setup_ack";
	
	//notify
	public final static String FUTURE_NOTIFY="future_notify";
	public final static String PAST_NOTIFY="past_notify";
	public final static String SUB_FUTURE_NOTIFY="sub_future_notify";
	public final static String SUB_PAST_NOTIFY="sub_past_notify";
	public final static String FUTURE_ACK="future_ack";
	public final static String PAST_ACK="past_ack";
	public final static String SUB_FUTURE_ACK="sub_future_ack";
	public final static String SUB_PAST_ACK="sub_past_ack";
	
	//on-going notify
	public final static String FUTURE_CREATE_NOTIFY="future_create_notify";
	public final static String FUTURE_REMOVE_NOTIFY="future_remove_notify";
	public final static String SUBTX_INIT_ACK="subtx_init_ack";
	public final static String SUBTX_END_NOTIFY="subtx_end_notify";
	public final static String PAST_CREATE_NOTIFY="past_create_notify";
	//cleanup
	public final static String CLEANUP_REQUEST="cleanup_request";
	
	//quiescence
	public final static String PASSIVE_REQUEST="passive_request";
	public final static String PASSIVE_ACK="passive_ack";
}
