/**
 * 
 */
package org.jbpm.bpel.frj.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author frj
 * 2012-3-12
 */
public class XMLUtil {

	public static DocumentBuilder getDocumentBuilder(){
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setIgnoringComments(true);
		DocumentBuilder db;
		try {
			db = factory.newDocumentBuilder();
			return db;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
