package org.jbpm.bpel.frj.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestWriter {
	private static String path="/home/frj/桌面/result";
	
	public static void writeResult(String str){
		FileWriter w=null;
		File file=null;
		try {
			file=new File(path);
			w=new FileWriter(file,true);
			w.write(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				w.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
