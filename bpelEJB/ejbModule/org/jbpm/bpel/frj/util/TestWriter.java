package org.jbpm.bpel.frj.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestWriter {
	private static String path=System.getProperty("user.home")+"/Desktop/result";
	private static List<Long> times=new ArrayList<Long>(3);
	public static void writeResult(long time){
		if(times.size()<3){
			times.add(time);
			if(times.size()==3){
				FileWriter w=null;
				File file=null;
				try {
					file=new File(path);
					w=new FileWriter(file,true);
					w.write("Update time: "+(times.get(1)-times.get(0))+"\t");
					w.write("Clean time: "+(times.get(2)-times.get(0))+"\t\n");
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
		}
	}
	public static void clear(){
		times.clear();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(System.getProperty("user.home"));
	}

}
