/**
 * 
 */
package org.jbpm.bpel.frj.interanalysis.modeling.model.other;

/**
 * @author frj
 *
 */
public class AlarmAction {

	private String _for=null;
	private String _for_lang=null;
	private String until=null;
	private String until_lang=null;
	
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
	public String getUntil() {
		return until;
	}
	public void setUntil(String until) {
		this.until = until;
	}
	public String getUntil_lang() {
		return until_lang;
	}
	public void setUntil_lang(String until_lang) {
		this.until_lang = until_lang;
	}
	
}
