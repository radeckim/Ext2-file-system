import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Inode{
	
	private int fileMode;
	private int userId;
	private long fileSize; 
	private int fileSizeUp;
	private int fileSizeLow;
	private int accessTime;
	private int creationTime;
	private int modifiedTime;
	private int deletedTime;
	private int groupId;
	private ArrayList<Integer> directPointers;
	private int indirect;
	private int doubleIndirect;
	private int tripleIndirect;
	private short numOfLinks;
	
	private static final int DIR_POINT_COUNT = 12;
	
	private Volume vol;
	
	public Inode(Volume vol, long inodePointer, long inodeNum){
		
		this.vol = vol;
		
		ByteBuffer buffer = ByteBuffer.wrap(vol.getBytes(vol.getBlockSize() * inodePointer + vol.getInodeSize() * (inodeNum - 1), vol.getInodeSize()));
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		fileMode = buffer.getShort();
		userId = buffer.getShort();
		fileSizeLow =  buffer.getInt();
		accessTime = buffer.getInt(); 
		creationTime = buffer.getInt();
		modifiedTime = buffer.getInt();
		deletedTime = buffer.getInt();
		groupId = buffer.getShort();
		numOfLinks = buffer.getShort();
		
		buffer.position(buffer.position() + 12);
		
		directPointers = new ArrayList<Integer>(DIR_POINT_COUNT);
		
		for(Integer dirPointer: directPointers) dirPointer = (Integer) buffer.getInt();
		
		indirect = buffer.getInt();
		doubleIndirect = buffer.getInt();
		tripleIndirect = buffer.getInt();
		
		buffer.position(buffer.position() + 8);
		
		fileSizeUp = buffer.getInt();		
		
		fileSize = (((long) fileSizeUp) << 32) | (fileSizeLow & 0xffffffffL);
	}
	
	public void readInode(){
		
		System.out.println("File mode: " +	fileMode);
		System.out.println("User ID of owner: " + 	userId);
		System.out.println("File size in bytes: " + fileSize);		
		System.out.println("Last Acces time: " + accessTime);	
		System.out.println("Creation time: " + creationTime);		
		System.out.println("Last modified time: " + modifiedTime);		
		System.out.println("Deleted time: " + deletedTime);		
		System.out.println("Group ID of owner: " + 	groupId);
		System.out.println("Number of hard links referencing file: " + 	numOfLinks);
		
		for(Integer dirPointer: directPointers){			
			
			System.out.println("Pointer: " + dirPointer);				
		}
		
		System.out.println("Indirect pointer: " + indirect);
		System.out.println("Double indirect pointer: " + doubleIndirect);	
		System.out.println("Triple indirect pointer: " + tripleIndirect);				
	}
}