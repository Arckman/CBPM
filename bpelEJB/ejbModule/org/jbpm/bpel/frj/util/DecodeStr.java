package org.jbpm.bpel.frj.util;

import java.util.HashMap;
import java.util.Map;

public class DecodeStr {

	private static char[][] matrix={
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
		{' ','!','\"','#','$','%','&','\'','(',')','*','+',',','-','.','/'},
		{'0','1','2','3','4','5','6','7','8','9',':',';','<','=','>','?'},
		{'@','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O'},
		{'P','Q','R','S','T','U','V','W','X','Y','Z','[','\\',']','^','_'},
		{'`','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o'},
		{'p','q','r','s','t','u','v','w','x','y','z','{','|','}','~',0},
		
};
private static Map<Character,Integer>mapper=new HashMap<Character,Integer>();
static{
	mapper.put(' ', 32);mapper.put('!', 33);mapper.put('\"', 34);mapper.put('#', 35);
	mapper.put('$', 36);mapper.put('%', 37);mapper.put('&', 38);mapper.put('\'', 39);
	mapper.put('(', 40);mapper.put(')', 41);mapper.put('*', 42);mapper.put('+', 43);
	mapper.put(',', 44);mapper.put('-', 45);mapper.put('.', 46);mapper.put('/', 47);
	mapper.put('0', 48);mapper.put('1', 49);mapper.put('2', 50);mapper.put('3', 51);
	mapper.put('4', 52);mapper.put('5', 53);mapper.put('6', 54);mapper.put('7', 55);
	mapper.put('8', 56);mapper.put('9', 57);mapper.put(':', 58);mapper.put(';', 59);
	mapper.put('<', 60);mapper.put('=', 61);mapper.put('>', 62);mapper.put('?', 63);
	mapper.put('@', 64);mapper.put('A', 65);mapper.put('B', 66);mapper.put('C', 67);
	mapper.put('D', 68);mapper.put('E', 69);mapper.put('F', 70);mapper.put('G', 71);
	mapper.put('H', 72);mapper.put('I', 73);mapper.put('J', 74);mapper.put('K', 75);
	mapper.put('L', 76);mapper.put('M', 77);mapper.put('N', 78);mapper.put('O', 79);
	mapper.put('P', 80);mapper.put('Q', 81);mapper.put('R', 82);mapper.put('S', 83);
	mapper.put('T', 84);mapper.put('U', 85);mapper.put('V', 86);mapper.put('W', 87);
	mapper.put('X', 88);mapper.put('Y', 89);mapper.put('Z', 90);mapper.put('[', 91);
	mapper.put('\\', 92);mapper.put(']', 93);mapper.put('^', 94);mapper.put('_', 95);
	mapper.put('`', 96);mapper.put('a', 97);mapper.put('b', 98);mapper.put('c', 99);
	mapper.put('d', 100);mapper.put('e', 101);mapper.put('f', 102);mapper.put('g', 103);
	mapper.put('h', 104);mapper.put('i', 105);mapper.put('j', 106);mapper.put('k', 107);
	mapper.put('l', 108);mapper.put('m', 109);mapper.put('n', 110);mapper.put('o', 111);
	mapper.put('p', 112);mapper.put('q', 113);mapper.put('r', 114);mapper.put('s', 115);
	mapper.put('t', 116);mapper.put('u', 117);mapper.put('v', 118);mapper.put('w', 119);
	mapper.put('x', 120);mapper.put('y', 121);mapper.put('z', 122);mapper.put('{', 123);
	mapper.put('|', 124);mapper.put('}', 125);mapper.put('~', 126);
}
public static String byteToStr(byte[] bytes){
	return byteToStr(bytes,0,bytes.length);
}
public static String byteToStr(byte[] bytes,int start,int end){
	StringBuffer s=new StringBuffer();
	for(int i=start;i<end;i++){
		if(bytes[i]<0)return null;
		int num=(int)bytes[i];
		int x=num/16;
		int y=num%16;
		s.append(matrix[x][y]);
	}
	return s.toString();
}
public static byte[] strToByte(String str){
	byte[] bytes=new byte[100];
	for(int i=0;i<str.length();i++){
//		System.out.println(str.charAt(i));
		bytes[i]=(byte)((int)mapper.get(str.charAt(i)));
//		System.out.print(bytes[i]+",");
	}
	return bytes;
}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		byte[] b={80, 79, 83, 84, 32, 47, 104, 111, 116, 101, 108, 112, 97, 114, 116, 110, 101, 114, 47, 104, 111, 116, 101, 108, 112, 97, 114, 116, 110, 101, 114, 80, 114, 111, 118, 105, 100, 101, 114, 32, 72, 84, 84, 80, 47, 49, 46, 49, 13, 10, 97, 99, 99, 101, 112, 116, 45, 101, 110, 99, 111, 100, 105, 110, 103, 58, 103, 122, 105, 112, 44, 100, 101, 102, 108, 97, 116, 101, 101, 13, 10, 99, 111, 110, 116, 101, 110, 116, 45, 116, 121, 112, 101, 58, 116, 101, 120, 116, 47, 120, 109, 108, 59, 99, 104, 97, 114, 115, 101, 116, 61, 85, 84, 70, 45, 56, 56, 13, 10, 115, 111, 97, 112, 97, 99, 116, 105, 111, 110, 58, 34, 111, 114, 103, 46, 110, 106, 117, 46, 104, 111, 116, 101, 108, 112, 97, 114, 116, 110, 101, 114, 35, 105, 110, 118, 111, 107, 101, 72, 111, 116, 101, 108, 34, 34, 13, 10, 99, 111, 110, 116, 101, 110, 116, 45, 108, 101, 110, 103, 116, 104, 58, 50, 54, 54, 54, 13, 10, 104, 111, 115, 116, 58, 49, 50, 55, 46, 48, 46, 48, 46, 49, 58, 56, 48, 56, 48, 48, 13, 10, 99, 111, 110, 110, 101, 99, 116, 105, 111, 110, 58, 75, 101, 101, 112, 45, 65, 108, 105, 118, 101, 101, 13, 10, 117, 115, 101, 114, 45, 97, 103, 101, 110, 116, 58, 65, 112, 97, 99, 104, 101, 45, 72, 116, 116, 112, 67, 108, 105, 101, 110, 116, 47, 52, 46, 49, 46, 49, 32, 40, 106, 97, 118, 97, 32, 49, 46, 53, 41, 41, 13, 10, 13, 10, 60, 115, 111, 97, 112, 101, 110, 118, 58, 69, 110, 118, 101, 108, 111, 112, 101, 32, 120, 109, 108, 110, 115, 58, 115, 111, 97, 112, 101, 110, 118, 61, 34, 104, 116, 116, 112, 58, 47, 47, 115, 99, 104, 101, 109, 97, 115, 46, 120, 109, 108, 115, 111, 97, 112, 46, 111, 114, 103, 47, 115, 111, 97, 112, 47, 101, 110, 118, 101, 108, 111, 112, 101, 47, 34, 32, 120, 109, 108, 110, 115, 58, 111, 114, 103, 61, 34, 111, 114, 103, 46, 110, 106, 117, 46, 104, 111, 116, 101, 108, 112, 97, 114, 116, 110, 101, 114, 34, 62, 10, 32, 32, 32, 60, 115, 111, 97, 112, 101, 110, 118, 58, 72, 101, 97, 100, 101, 114, 47, 62, 10, 32, 32, 32, 60, 115, 111, 97, 112, 101, 110, 118, 58, 66, 111, 100, 121, 62, 10, 32, 32, 32, 32, 32, 32, 60, 111, 114, 103, 58, 104, 111, 116, 101, 108, 95, 114, 101, 113, 117, 101, 115, 116, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 32, 60, 105, 110, 112, 117, 116, 62, 97, 60, 47, 105, 110, 112, 117, 116, 62, 10, 32, 32, 32, 32, 32, 32, 60, 47, 111, 114, 103, 58, 104, 111, 116, 101, 108, 95, 114, 101, 113, 117, 101, 115, 116, 62, 10, 32, 32, 32, 60, 47, 115, 111, 97, 112, 101, 110, 118, 58, 66, 111, 100, 121, 62, 10, 60, 47, 115, 111, 97, 112, 101, 110, 118, 58, 69, 110, 118, 101, 108, 111, 112, 101, 62}; 
		String str=DecodeStr.byteToStr(b);
		System.out.println(str);
	}

}
