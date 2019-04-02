import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.nio.ByteBuffer;

/** 
* A class which represents a regular file in ext2 file system.
* Its aim is to find a file in the file system given a path.
* Then the file can be read given either an offset of bytes (a point when we want to start reading)
* and length or start from the current position which may be set by the user.
*
* It also contains an information about the file size.
* @author Michal Radecki
*/

public class Ext2File{	
	
	
	private Volume volume;
	private long size;
	private long actualSize;
	private ByteBuffer fileBuffer;	
	
	/**
	* Create an instance of the class given a path and a file system, where the file exists.
	* Automatically get bytes of the file and saves them, so they can be proceed.
	*
	* @param	volume	a file system where the file exists.
	* @param	path	a path in the filesystem to the file.
	*/
	
	public Ext2File(Volume volume, String path){		
		
		this.volume = volume;
		PathHelper finder = new PathHelper();
		finder.getBytes(path);			
	}

	/**
	* Reads at most length bytes starting at byte offset startByte from start of file. Byte 0 is the first byte in the file.
	* If there are fewer than length bytes remaining these will be read and a smaller number of bytes than requested will be returned.
	*
	* @param	startByte	an offset from start of file
	* @param	length		number of bytes to be read
	* @return	bytes		an array of bytes extracted from the file
	*/
	
	public byte[] read(long startByte, long length){
        
        if(startByte >= size || startByte < 0){
			
			System.out.println("StartByte is greater than the file size or is less than 0");
			return null;
		}
		
		byte[] bytes = new byte[(int) length];		
			
		for(int i = 0; i < (length - startByte) && i < (actualSize - startByte); i++){
			
			bytes[i] = fileBuffer.get((int) startByte + i);
		}
		
        return bytes;  
    }
	
	/**
	* Reads at most length bytes starting at current position in the file.
	* If the current position is set beyond the end of the file, and exception should be raised.
	*
	* @param	length	number of bytes we want to read
	* @return	bytes		an array of bytes extracted from the file
	*/	
	
    public byte[] read(int length){

		if(fileBuffer.position() > actualSize){
			
			System.out.println("Position is greater than file size!");
			return null;
		}
		
		byte[] bytes = new byte[length];		
			
		for(int i = 0; i < length && i < actualSize; i++){
			
			bytes[i] = fileBuffer.get();
		}
		
        return bytes; 
	}

	/**
	* Move to byte position in file.
	* Setting position to 0L will move to the start of the file. It is legal to seek beyond the end of the file.
	*
	* @param position	an offset of bytes from the beginning of the file which is a new position.
	*/
	
	public void seek(long position){
		
		fileBuffer.position((int) position);		
	}
	
	/**
	* Returns current position in file, i.e. the byte offset from the start of the file.
	* The file position will be zero when the file is first opened and will advance by the number of bytes read with every call to one of the read( ) routines.
	*
	* @return fileBuffer.position	current position in the file
	*/
	
	public long position(){
		
		return fileBuffer.position();
	}
	
	/**
	* @return size	size of file as specified in filesystem.
	*
	*/
	
	public long size(){
		
		return size;
	}

	/*
	*	A private nested class which helps to read a path of the file.
	*	Its functionality is to get an array of bytes representing the file.
	*
	*/
	
	private class PathHelper{
		
		/*
		*	Read the path given in the constructor - it goes through the file and stop if it reaches the last part of the path.
		*	When stops automatically read bytes of the file (only not-null pointers are returned from Volume).
		*	Bytes are saved in global variable so they can be read.
		*
		*/
		
		public void getBytes(String path){
			
			// Split the path, get rid of "/" and save it as an array.		
			String[] splittedPath = path.split("/");
			
			// Sets an inode to the root by default.
			Integer nextInode =  (int)(long)volume.getRootInode();	
			
			// Sets an inode pointer to the first one by default.
			long pointer = volume.findInodePointer(0);
			
			// find a directory in the root
			long directory = volume.findDirectory(pointer, nextInode);		
			
			// Iterate over the path to go deeper and deeper in the filesystem.
			// Each time the iteration is done inode and directory is replaced.
			for(String name: splittedPath){			
				
				//Create a map which holds a name and inode number of each element
				TreeMap map = volume.readDirectory(directory);
				Set set = map.entrySet();
				Iterator iterator = set.iterator();
				
				// Iterate over the map and check is the name is the same as a part of the path.
				// If name is the same as a part of splitted path the inode is replaced.
				while(iterator.hasNext()){
					
					Map.Entry entry = (Map.Entry)iterator.next();
					if(entry.getKey().equals(name)) nextInode = (Integer) entry.getValue();
				}			
				
				if(nextInode > volume.getInodesInGrup() && nextInode < volume.getInodesInGrup() * 2){
							
					pointer = volume.findInodePointer(1);
					nextInode -= (int)(long) volume.getInodesInGrup();
					directory = volume.findDirectory(pointer, nextInode);				
				}
				
				else if(nextInode > 2 *  volume.getInodesInGrup()){		
						
					pointer = volume.findInodePointer(2);
					nextInode -= (int)(long) volume.getInodesInGrup() * 2;
					directory = volume.findDirectory(pointer, nextInode);
				}			
			}
		
			size = volume.getFileSize(pointer, nextInode);
			fileBuffer = ByteBuffer.allocate((int) ((size / volume.getBlockSize()) * volume.getBlockSize() + volume.getBlockSize()));			
			
			
			//Create an ArrayList of direct pointers then iterate over it to get bytes of the file.
			ArrayList<Long> listOfDirects = volume.findDirect(pointer, nextInode);
			
			if(!(listOfDirects.isEmpty())){				
			
				for(Long block: listOfDirects){					
										
					byte[] fileBytes = volume.getBytes(block * volume.getBlockSize(), volume.getBlockSize());
					
					fileBuffer.put(fileBytes);
					actualSize += volume.getBlockSize();
				}
			}
			
			//Create an ArrayList of single indirect pointers then to get bytes of the last block of the file.
			ArrayList<Long> listOfIndirects = volume.findIndirect(pointer, nextInode);
			
			if(!(listOfIndirects.isEmpty())){
			
				long lastBlock = (long) listOfIndirects.get(listOfIndirects.size() - 1);						
				
				byte[] fileBytes = volume.getBytes(lastBlock * volume.getBlockSize(), volume.getBlockSize());
				fileBuffer.put(fileBytes);
				
				actualSize = volume.getBlockSize();
			}
			
			//Create an ArrayList of double indirect pointers then to get bytes of the last block of the file.
			ArrayList<Long> listOfDoubles = volume.findDoubleIndirect(pointer, nextInode);
			
			if(!(listOfDoubles.isEmpty())){
					
				long lastBlock = (long) listOfDoubles.get(listOfDoubles.size() - 1);			
				
				byte[] fileBytes = volume.getBytes(lastBlock * volume.getBlockSize(), volume.getBlockSize());				
				fileBuffer.put(fileBytes);
				
				actualSize = volume.getBlockSize();
			}				
			
			//Create an ArrayList of triple indirect pointers then to get bytes of the last block of the file.
			ArrayList<Long> listOfTriples = volume.findTripleIndirect(pointer, nextInode);		
			
			if(!(listOfTriples.isEmpty())){				
			
				long lastBlock = (long) listOfTriples.get(listOfTriples.size() - 1);
				
				byte[] fileBytes = volume.getBytes(lastBlock * volume.getBlockSize(), volume.getBlockSize());				
				fileBuffer.put(fileBytes);
				
				actualSize = volume.getBlockSize();				
			}						
		}
	}
}
