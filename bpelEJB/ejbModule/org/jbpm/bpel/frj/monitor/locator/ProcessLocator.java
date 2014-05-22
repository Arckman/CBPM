/**
 * 
 */
package org.jbpm.bpel.frj.monitor.locator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.apache.xerces.dom.DeferredElementNSImpl;
import org.jbpm.bpel.frj.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author frj
 *
 */
public class ProcessLocator {
	private String path=".."+File.separator+"server"+File.separator+"default"+
File.separator;
	private String fileName="ProcessLocator.xml";
	private Map<String,LocatorUnit>locators=new HashMap<String,LocatorUnit>();
	public ProcessLocator(){locateProcesses();}
	public ProcessLocator(String path,String file){locateProcesses(path,file);}
	private void locateProcesses(){
		File file=new File(path+fileName);
		System.out.println("Parsing "+file.getAbsolutePath()+"...");
		DocumentBuilder db=XMLUtil.getDocumentBuilder();
		try {
			Document d=db.parse(file);
			NodeList processes=d.getFirstChild().getChildNodes();
			for(int a=0;a<processes.getLength();a++){
				Node process=processes.item(a);
				if(process instanceof DeferredElementNSImpl){
					Node nameNode=process.getFirstChild().getNextSibling();
					Node ipNode=nameNode.getNextSibling().getNextSibling();
					LocatorUnit lu=new LocatorUnit(nameNode.getTextContent(),
							ipNode.getTextContent());
					locators.put(lu.getProcessName(), lu);
					NodeList incomes=((DeferredElementNSImpl) process).getElementsByTagName("income");
					for(int b=0;b<incomes.getLength();b++){
						Node income=incomes.item(b);
						if(income instanceof DeferredElementNSImpl){
							lu.addIncome(income.getTextContent(),
									((DeferredElementNSImpl) income).getAttribute("plt"));
						}
					}
					NodeList outgoes=((DeferredElementNSImpl) process).getElementsByTagName("outgo");
					for(int b=0;b<outgoes.getLength();b++){
						Node outgo=outgoes.item(b);
						if(outgo instanceof DeferredElementNSImpl){
							lu.addOutgo(outgo.getTextContent(),
									((DeferredElementNSImpl) outgo).getAttribute("plt"));
						}
					}
//					System.out.println(lu);
				}
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//for test
	private void locateProcesses(String p,String f){
		path=p;
		fileName=f;
		locateProcesses();
	}
	public LocatorUnit getLocatorUnit(String processName){
		return locators.get(processName);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path="/home/conup/jboss-4.2.2.GA/server/default/";
		String file="ProcessLocator.xml";
		ProcessLocator pl=new ProcessLocator(path,file);
		pl.locateProcesses(path, file);
		
	}

}
