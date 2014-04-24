/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.composite.handlers;

import org.jbpm.bpel.frj.interanalysis.modeling.model.Activity;

/**
 * @author frj
 *
 */
public class OnAlarm extends Handler {

	private String _for=null;
	private String _for_lang=null;
	private String _until=null;
	private String _until_lang=null;
	private String repeatEvery=null;
	private String repeatEvery_lang=null;
	
	/**
	 * @param str
	 */
	public OnAlarm(String str) {
		super(str);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param str
	 * @param a
	 * @param b
	 */
	public OnAlarm(String str, Activity a, Activity b) {
		super(str, a, b);
		// TODO Auto-generated constructor stub
	}

	public String get_for() {
		return _for;
	}

	public void set_for(String _for) {
		this._for = _for;
	}

	public String get_for_lang() {
		return _for_lang;
	}

	public void set_for_lang(String _for_lang) {
		this._for_lang = _for_lang;
	}

	public String get_until() {
		return _until;
	}

	public void set_until(String _until) {
		this._until = _until;
	}

	public String get_until_lang() {
		return _until_lang;
	}

	public void set_until_lang(String _until_lang) {
		this._until_lang = _until_lang;
	}

	public String getRepeatEvery() {
		return repeatEvery;
	}

	public void setRepeatEvery(String repeatEvery) {
		this.repeatEvery = repeatEvery;
	}

	public String getRepeatEvery_lang() {
		return repeatEvery_lang;
	}

	public void setRepeatEvery_lang(String repeatEvery_lang) {
		this.repeatEvery_lang = repeatEvery_lang;
	}
}
