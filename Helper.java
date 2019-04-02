import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.TreeMap;

/** 
* A class which helps in debuging and displaying values.
* It can change a filemode given in long to unix style String, change bytes to: long, date, hex
* and directory in ext2 file system in the following format - "Name of the file, inode number".
*/

public class Helper{	
	
	
	private static final int IFDIR = 0x4000;      // Directory
	
	private static final int IRUSR = 0x0100;      // User getBytes
	private static final int IWUSR = 0x0080;      // User write
	private static final int IXUSR = 0x0040;      // User execute

	private static final int IRGRP = 0x0020;      // Group getBytes
	private static final int IWGRP = 0x0010;      // Group write
	private static final int IXGRP = 0x0008;      // Group execute

	private static final int IROTH = 0x0004;      // Others getBytes
	private static final int IWOTH = 0x0002;      // Others wite
	private static final int IXOTH = 0x0001;      // Others execute	

	/**
	* Build a string representing a file mode in unix style format.
	* It is done by using a bitwise operation AND
	*
	* @param modeLong a file mode represented as a long value
	* @return mode 	  a properly formated file mode representation
	*/

	public static StringBuilder fileMode(long modeLong){
		
		StringBuilder mode = new StringBuilder();
		
		if((IFDIR & modeLong) == IFDIR) mode.append("d");
		else mode.append("-");
		
		if((IRUSR & modeLong) == IRUSR) mode.append("r");
		else mode.append("-");
		
		if((IWUSR & modeLong) == IWUSR) mode.append("w");
		else mode.append("-");
		
		if((IXUSR & modeLong) == IXUSR) mode.append("x");
		else mode.append("-");
		
		if((IRGRP & modeLong) == IRGRP) mode.append("r");
		else mode.append("-");
		
		if((IWGRP & modeLong) == IWGRP) mode.append("w");
		else mode.append("-");
		
		if((IXGRP & modeLong) == IXGRP) mode.append("x");
		else mode.append("-");
		
		if((IROTH & modeLong) == IROTH) mode.append("r");
		else mode.append("-");
		
		if((IWOTH & modeLong) == IWOTH) mode.append("w");
		else mode.append("-");
		
		if((IXOTH & modeLong) == IXOTH) mode.append("x");
		else mode.append("-");	
		
		return mode;
	}
	
	/**
	* Convert an array of bytes to long value
	*
	* @param bytes		an array of bytes
	* @return bytesLong	long representation of given array
	*/
	
	public static long toLong(byte[] bytes){		
		
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		long bytesLong = 0xFFFFFFFF & (long)bb.getShort();			
		return bytesLong;	
	}	
	
	/**
	* Convert an array of bytes to String	*
	* @param bytes		an array of bytes
	* @return bytesLong	String representation of given array
	*/
	
	public static String toString(byte[] bytes){
		try{
			return new String(bytes, "ASCII");
		}catch(UnsupportedEncodingException e){
			return null;
		}
	}
	
	/**
	* Convert an array of bytes to Date	*
	* @param bytes		an array of bytes
	* @return bytesLong	Date representation of given array
	*/
	
	public static Date readDate(byte[] bytes){
		
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);		
		
		long miliseconds = 0xFFFFFFFF & (long)bb.getInt();			
			
		Date d1 = new Date(miliseconds * 1000);		
		
		return d1;
	}
	
	/**
	* Convert an array of bytes to hex	*
	* @param bytes		an array of bytes
	* @return bytesLong	hex representation of given array
	*/
	
	public static String toHex(short value){				
		
		return String.format("0x%02X", value);		
	}
	
	/**
	* Iterate over the directory (represented by an array of bytes) and saves each file name and inode in a map
	* @param bytes		an array of bytes
	* @return map       map which holds a name and inode of each file in the directory
	*/

	public static TreeMap toDirectory(byte[] bytes){		
	
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		TreeMap<String, Integer> map = new TreeMap<String, Integer>(); 
		
		while(bb.remaining() > 0){		
			
			int inode = bb.getInt();			
			short length = bb.getShort();
			byte nameLength = bb.get();				
			byte fileType = bb.get();			
		
			//Set a new position to skip to the next line
			int newPosition = bb.position() - 8 + length;				
			
			char[] charsName = new char[nameLength];			
			for(int j = 0; j < nameLength; j++){

				charsName[j] = (char)bb.get();		
			}	
			
			map.put(String.valueOf(charsName), inode);
			//Skip to the next line
			bb.position(newPosition);
		}
		
		return map;
	}
}