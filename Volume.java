import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Date;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
*	A class represents an ext2 file system. 
*	It cointains all the constances and parameters hold in group descriptors/super blocks/inodes which are useful to read a file in the file system.
*	Its purpose is to create every single object like an super block, group descriptor or an inode and returns these values, so they may be proceed to read a file.
*	It implements a functionality of reading constances and parameters to debug.
*
*	@author Michal Radecki
*/

public class Volume{
	
	private RandomAccessFile file;

	private final long BLOCK_SIZE = 1024;		//A size of each block in the file system
	
	private final long ROOT_INODE = 2;			//A number of the root inode
	private final int MAX_POINTERS = 256;		//An iterate variable which is used to find indirect pointers			
	
	private short magicNumber;
	private int inodesInSys;
	private int blocksInSys;
	private int blocksInGroup;
	private int inodesInGroup;
	private int inodeSize;
	private String volLabel;
	private SuperBlock superBlock;
	private GroupDescriptor[] groupDesc;
	private Inode[] inodes;
	
	private Helper help;

	/**
	*	Opens the Volume represented by the host Windows/ Linux file filename.
	*	@param fileName path to a file to be opened
	*/
 
	public Volume(String fileName){
		
		//Try to open a file - if the file doesn't exist, rise an exception		
		try{							

			file = new RandomAccessFile(fileName, "r");			
					
		}catch(IOException e){
			
			System.out.println("File doesn't exist!");
		}
		
		//Create Helper		
		help = new Helper();
		
		//Create an instance of SuperBlock
		//it's created only once as every single super block in the volume is exactly the same
		superBlock = new SuperBlock();
		
		//Create a group descriptor for each group block
		
		groupDesc = new GroupDescriptor[3];		
		
		for(int i = 0; i < 3; i++) groupDesc[i] = new GroupDescriptor(i);				
		
		//Create all of the inodes
		
		inodes = new Inode[inodesInSys];
		
		for(int i = 0; i < inodesInSys; i++) inodes[i] = new Inode(i);		
	}
	
	/**
	*	Looking for a bytes in a file - if a startByte is greater than file size it rises an exception.
	*	@param	startBytes	an offset in the file
	*	@param	length		number of bytes to be returned
	*	@return	bytes		an array of bytes	
	*/
	
	public byte[] getBytes(long startByte, long length){
        
        byte[] bytes = new byte[(int) length];      
		
		try{			
			
			file.seek(startByte);        
			file.read(bytes);
			 
		}catch(IOException e){
			
			System.out.println("StartByte is greater than file size!");
		} 		
        return bytes;		
    }

	/**
	* Find direct pointers in an inode given an inode pointer and inode number in the table pointed
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @returns dataBlocks	numbers of data blocks which hold a data
	*/

	public ArrayList<Long> findDirect(long inodePointer, long inodeNum){
		
		ArrayList<Long> dataBlocks = new ArrayList<Long>();
		
		for(int i = 0; i < 12; i++){			
					
			long x = help.toLong(getBytes(inodePointer * BLOCK_SIZE + inodeSize * (inodeNum - 1) + 40 + 4 * i, 4));
			if(x != 0 ) dataBlocks.add(x);		
		}
		
		return dataBlocks;
	}
	
	/**
	* Find single indirect pointers in an inode given an inode pointer and inode number in the table pointed
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @returns dataBlocks	numbers of data blocks which hold a data
	*/
	
	public ArrayList<Long> findIndirect(long inodePointer, long inodeNum){
		
		ArrayList<Long> dataBlocks = new ArrayList<Long>();
		
		
		long pointer = help.toLong(getBytes(inodePointer * BLOCK_SIZE + inodeSize * (inodeNum - 1) + 88, 4));		
		
		for(int i = 0; i < MAX_POINTERS && pointer != 0; i ++){			
			
			long x = help.toLong(getBytes(pointer * BLOCK_SIZE + i * 4, 4));
			if(x != 0 ) dataBlocks.add(x);			
		}
		
		return dataBlocks;
	}
	
	/**
	* Find triple indirect pointers in an inode given an inode pointer and inode number in the table pointed
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @returns dataBlocks	numbers of data blocks which hold a data
	*/
	
	public ArrayList<Long> findDoubleIndirect(long inodePointer, long inodeNum){
		
		ArrayList<Long> dataBlocks = new ArrayList<Long>();

		long pointer = help.toLong(getBytes(inodePointer * BLOCK_SIZE + inodeSize * (inodeNum - 1) + 92, 4));		

		int z = 0;
		
		for(int i = 0; i < MAX_POINTERS && pointer != 0; i++){		
				
			long pointerDouble = help.toLong(getBytes(pointer * BLOCK_SIZE + i * 4, 4));						
				
			for(int j = 0; j < MAX_POINTERS && pointerDouble != 0; j++){
				
				long x = help.toLong(getBytes(pointerDouble * BLOCK_SIZE + j * 4, 4));
				if(x != 0 ) dataBlocks.add(x);
			}
		}
				
		return dataBlocks;
	}
	
	/**
	* Find triple indirect pointers in an inode given an inode pointer and inode number in the table pointed
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @returns dataBlocks	numbers of data blocks which hold a data
	*/
	
	public ArrayList<Long> findTripleIndirect(long inodePointer, long inodeNum){
		
		ArrayList<Long> dataBlocks = new ArrayList<Long>();

		long pointer = help.toLong(getBytes(inodePointer * BLOCK_SIZE + inodeSize * (inodeNum - 1) + 96, 4));			
		
		for(int i = 0; i < MAX_POINTERS && pointer != 0; i++){		
				
			long pointerDouble = help.toLong(getBytes(pointer* BLOCK_SIZE + i * 4, 4));			

			for(int j = 0; j < MAX_POINTERS && pointerDouble != 0; j++){					
					
				long pointerTriple = help.toLong(getBytes(pointerDouble * BLOCK_SIZE + j * 4, 4));	
							
				for(int y = 0; y < MAX_POINTERS && pointerTriple != 0; y++){							
					
					long x = help.toLong(getBytes(pointerTriple * BLOCK_SIZE + y * 4, 4));
					if(x != 0) dataBlocks.add(x);			
				}
			}
		}		
		
		return dataBlocks;
	}
	
	/** 
	* Returns inode pointer given block group number
	*
	* @param 	blockGroupNum block group number
	* @returns	groupDesc[blockGroupNum].getTablePointer() a pointer to a inode table
	*/
	
	public long findInodePointer(int blockGroupNum){	
		
		return 	groupDesc[blockGroupNum].getTablePointer();
	}

	/** 
	* Returns first direct pointer which may be used to traverse a file system.
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @returns	help.toLong(getBytes(inodePointer * BLOCK_SIZE + inodeSize * (inodeNum - 1) + 40, 4)) first direct pointer
	*/
	
	public long findDirectory(long inodePointer, long inodeNum){
		
		return 	help.toLong(getBytes(inodePointer * BLOCK_SIZE + inodeSize * (inodeNum - 1) + 40, 4));
	}
	
	/** 
	* Returns a TreeMap which holds two values - name of the file and inode number
	*
	* @param 	dirPointer	a first direct pointer in the file
	* @returns	help.toDirectory(getBytes(dirPointer * BLOCK_SIZE, BLOCK_SIZE)) a TreeMap
	*/
	
	public TreeMap readDirectory(long dirPointer){
		
		return help.toDirectory(getBytes(dirPointer * BLOCK_SIZE, BLOCK_SIZE));		
	}
	
	/**
	* Returns a file mode of the file
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @return fileMode 		file mode of the file
	*/
	
	public long getFileMode(long inodePointer, long inodeNum){			
		
		return help.toLong(getBytes(BLOCK_SIZE * inodePointer + inodeSize * (inodeNum - 1), 2));		
	}
	
	/**
	* Returns a number of hard links to the file
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @return hardLinks 	file mode of the file
	*/	
	
	public long getHardLinks(long inodePointer, long inodeNum){
		
		return help.toLong(getBytes(BLOCK_SIZE * inodePointer + inodeSize * (inodeNum - 1) + 26, 2));	
	}
	
	/**
	* Returns a date when the file was last modified
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @return timeMod 	file mode of the file
	*/
	
	public Date getModTime(long inodePointer, long inodeNum){
	
		return help.readDate(getBytes(BLOCK_SIZE * inodePointer + inodeSize * (inodeNum - 1) + 16, 4));
	}
	
	/**
	* Returns an user id
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @return userId 		ID of the user
	*/
	
	public long getUserID(long inodePointer, long inodeNum){
		
		return help.toLong(getBytes(BLOCK_SIZE * inodePointer + inodeSize * (inodeNum - 1) + 2, 2));
	}
	
	/**
	* Returns an user id
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @return groupID		ID of the group
	*/
	
	public long getGroupID(long inodePointer, long inodeNum){
		
		return help.toLong(getBytes(BLOCK_SIZE * inodePointer + inodeSize * (inodeNum - 1) + 20, 2));
	}
	
	/**
	* Returns a file size
	*
	* @param inodePointer	pointer to the inode table
	* @param inodeNum		inode number in the table pointed
	* @return fileSize 		size of the file
	*/
	
	public long getFileSize(long inodePointer, long inodeNum){
		
		long lower = help.toLong(getBytes(BLOCK_SIZE * inodePointer + inodeSize * (inodeNum - 1) + 4, 4));	
		long upper = help.toLong(getBytes(BLOCK_SIZE * inodePointer + inodeSize * (inodeNum - 1) + 108, 4));
		
		return (((long) upper) << 32) | (lower & 0xffffffffL);
	}

	/**
	*	Returns the constant ROOT_INODE
	*	@return	ROOT_INODE	number of the root inode
	*/
	
	public long getRootInode(){
		
		return ROOT_INODE;		
	}
	
	/**
	*	Returns the constant BLOCK_SIZE
	*	@return	BLOCK_SIZE	size of each block in the file (in bytes)
	*/
	
	public long getBlockSize(){
		
		return BLOCK_SIZE;
	}
	
	/**
	*	Returns the inode size defined in a super block
	*	@return	inodeSize	size of each inode in the file (in bytes)
	*/
	
	public long getInodeSize(){
		
		return inodeSize;
	}
	
	/**
	*	Returns the number of inodes in a group defined in a super block
	*	@return	inodeSize	number of inodes in a group in the file(in bytes)
	*/
	
	public long getInodesInGrup(){
		
		return inodesInGroup;
	}
	
	/**
	*	Reads useful data defined in a super block
	*/
	
	public void readSuperBlock(){
		
		System.out.println("Magic number: " + help.toHex(magicNumber));		
		System.out.println("Number of inodes in the filesystem: " + inodesInSys);
		System.out.println("Number of blocks in the filesystem: " + blocksInSys);	
		System.out.println("Number of blocks per group: " + blocksInGroup);
		System.out.println("Number of inodes per group: " + inodesInGroup);	
		System.out.println("Size of each inode: " + inodeSize);	
		System.out.println("Volume label (disk name): " + volLabel);		
	}
	
	/**
	*	Reads useful data defined in a super block
	*	@param inodeNum number of an inode counted from 0
	*/

	public void readInode(int inodeNum){
		
		inodes[inodeNum].readInode();
		
	}
	
	/**
	*	Reads useful data defined in a group desc
	*	@param descNum number of an group descriptor counted from 0
	*/

	public void readGroupDesc(int descNum){
		
		groupDesc[descNum].readDesc();
	}
	
	/**
	*	Reads a whole block group.
	*
	*/
	
	public void readBlockGroup(int blockGroupNum){	
		
		for(int i = 1 * blockGroupNum; i < inodesInGroup * (blockGroupNum + 1); i++) inodes[i].readInode();
		groupDesc[blockGroupNum].readDesc();
		readSuperBlock();
	}
	
	/*
	*	A private nested class which represents a single super block in the file.
	*/

	private class SuperBlock{	
		
		private static final int MGC_NR_OFFSET = 56;
		private static final int BLKS_GRP_OFFSET = 32;
		private static final int INDS_GRP_OFFSET = 40;
		private static final int IND_SIZE_OFFSET = 88;
		private static final int VOL_NAME_OFFSET = 120;
		private static final int VOL_NAME_LNGTH = 16;	
		
		private Helper help;

		/*
		*	Create a super block.
		*/
		
		private SuperBlock(){			
		
			ByteBuffer buffer = ByteBuffer.wrap(getBytes(BLOCK_SIZE, BLOCK_SIZE));
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			magicNumber = buffer.getShort(MGC_NR_OFFSET);
			inodesInSys = buffer.getInt();
			blocksInSys = buffer.getInt();
			blocksInGroup = buffer.getInt(BLKS_GRP_OFFSET);
			inodesInGroup = buffer.getInt(INDS_GRP_OFFSET);
			inodeSize = buffer.getInt(IND_SIZE_OFFSET);		
			volLabel = new String();
			
			for(int i = 0; i < VOL_NAME_LNGTH; i++){
				
				char ch = (char) buffer.get(VOL_NAME_OFFSET + i);
				volLabel += ch;
			}			
		}	
	}
	
	/*
	*	A private nested class which represents a single group descriptor in the file.
	*/

	private class GroupDescriptor{		
		
		private final int DESC_SIZE = 32;
		private final int POINTER_OFFSET = 8;
		private final int blockGroupNum;
		private int tablePointer;
		
		/*
		*	Create a group descriptor given the number of a block group.
		*	@param blockGroupNum a number of the block group.
		*/
		
		public GroupDescriptor(int blockGroupNum){
		
			this.blockGroupNum = blockGroupNum;
			ByteBuffer buffer = ByteBuffer.wrap(getBytes(BLOCK_SIZE * 2, BLOCK_SIZE));
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			tablePointer =  buffer.getInt(DESC_SIZE * blockGroupNum  + POINTER_OFFSET);				
		}
		
		/*
		*	Get a table pointer of the group descriptor.
		*	@return tablePointer	a pointer to a table of inodes.
		*/

		private int getTablePointer(){
			
			return tablePointer;
		}
		
		/*
		*	Display useful data in a group descriptor (inode table pointer)
		*/
		
		private void readDesc(){
			
			System.out.println("Group descriptor nr " + blockGroupNum + " table pointer is " + tablePointer);
		}
	}
	
	/*
	* A private nested class which represents a single inode in the file system.
	* It holds all the useful information which may be useful to read the filesystem.
	*
	*/

	private class Inode{
		
		private int fileMode;
		private int userId;
		private long fileSize; 
		private int fileSizeUp;
		private int fileSizeLow;
		private Date access;
		private Date creation;	
		private	Date modified;	
		private	Date deleted;
		private int groupId;
		private int[] directPointers;
		private int indirect;
		private int doubleIndirect;
		private int tripleIndirect;
		private short numOfLinks;
		
		private static final int DIR_POINT_COUNT = 12;	
		
		/*
		* Create an instance of inode class and read all of the useful information
		*
		* @param inodeNum number of the inode in the filesystem
		*/
		
		public Inode(long inodeNum){
			
			//Decide to which inodeTable the inode belongs
			
			long pointer = pointer = groupDesc[0].getTablePointer();;
			
			if(inodeNum > inodesInGroup && inodeNum < inodesInGroup * 2){
							
				pointer = groupDesc[1].getTablePointer();
				inodeNum -= (int)(long) inodesInGroup;				
			}
				
			else if(inodeNum > 2 * inodesInGroup){		
						
				pointer = groupDesc[2].getTablePointer();
				inodeNum -= (int)(long) inodesInGroup * 2;				
			}	
			
			//Read the information
			
			ByteBuffer buffer = ByteBuffer.wrap(getBytes(BLOCK_SIZE * pointer + inodeSize * (inodeNum - 1), inodeSize));
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			fileMode = buffer.getShort();
			userId = buffer.getShort();
			fileSizeLow =  buffer.getInt();
			long accessTime = 0xFFFFFFFF & (long)buffer.getInt(); 
			long creationTime = 0xFFFFFFFF & (long)buffer.getInt();
			long modifiedTime = 0xFFFFFFFF & (long)buffer.getInt();
			long deletedTime = 0xFFFFFFFF & (long)buffer.getInt();
			groupId = buffer.getShort();
			numOfLinks = buffer.getShort();			
			
			if(accessTime != 0)	access = new Date(accessTime * 1000);
			else access = null;
			if(creationTime != 0) creation = new Date(creationTime * 1000);
			else creation = null;			
			if(modifiedTime != 0) modified = new Date(modifiedTime * 1000);	
			else modified = null;
			if(deletedTime != 0) deleted = new Date(deletedTime * 1000);	
			else deleted = null;
			
			buffer.position(buffer.position() + 12);
			
			directPointers = new int[DIR_POINT_COUNT];
			
			for(int i = 0; i < DIR_POINT_COUNT; i++) directPointers[i] = buffer.getInt();
			
			indirect = buffer.getInt();
			doubleIndirect = buffer.getInt();
			tripleIndirect = buffer.getInt();
			
			buffer.position(buffer.position() + 8);
			
			fileSizeUp = buffer.getInt();		
			
			// Merge to int values which describe a size of the file the inode holds
			fileSize = (((long) fileSizeUp) << 32) | (fileSizeLow & 0xffffffffL);
		}
		
		/*
		*	Reads useful data defined in the inode
		*/
	
		private void readInode(){
			
			System.out.println("File mode: " +	fileMode);
			System.out.println("User ID of owner: " + userId);
			System.out.println("File size in bytes: " + fileSize);		
			System.out.println("Last Acces time: " + access);	
			System.out.println("Creation time: " + creation);		
			System.out.println("Last modified time: " + modified);		
			System.out.println("Deleted time: " + deleted);		
			System.out.println("Group ID of owner: " + groupId);
			System.out.println("Number of hard links referencing file: " + numOfLinks);
			
			for(Integer dirPointer: directPointers){			
				
				System.out.println("Pointer: " + dirPointer);				
			}
			
			System.out.println("Indirect pointer: " + indirect);
			System.out.println("Double indirect pointer: " + doubleIndirect);	
			System.out.println("Triple indirect pointer: " + tripleIndirect);				
		}
		
	}
}
