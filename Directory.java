import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
* A class which represents a directories in a file system.
* Its aim is to read a path and display contents of a directory in a form suited to being output in Unix like format.
*
* @author Michal Radecki	
*/

public class Directory{	
	
	private Volume volume;
	private Helper help;
	
	/**
	* Create an instance of directory class in a given filesystem.
	*
	* @param volume	filesystem in which we want to check a directory.
	*/

	public Directory(Volume volume){
		
		this.volume = volume;
		
		//Create an instance of Helper which is useful to make output of fileInfo readable.
		help = new Helper();
	}

	/**
	* Prints contents of a directory in a form suited to being output in Unix like format, such as:
	*
    *    
	*   drwxr-xr-x  4 root root   1024 Aug 13 20:20 .
	*   drwxr-xr-x 25 root root   4096 Aug 11 11:15 ..
	*   drwxr-xr-x  3 acs  staff  1024 Aug 13 20:20 home
	*   drwx------  2 root root  12288 Aug 11 11:06 lost+found
	*   -rw-r--r--  1 acs  staff     0 Aug 11 22:17 test
	*/
	
	public void fileInfo(String path){		
		
		// Split the path, get rid of "/" and save it as an array.	
		String[] splittedPath = path.split("/");
		
		// Sets an inode to the root by default.
		Integer nextInode =  (int)(long)volume.getRootInode();
		
		// Sets an inode pointer to the first one by default.		
		long pointer = volume.findInodePointer(0);
		
		// Create ArrayList which will hold direct pointers.
		List<Long> directories = new ArrayList<Long>();
		
		// Set directory to 0 by default.
		long directory = 0;
		
		// An iterator variable which is used to point out the last iteration of next for loop
		int i = 0;
		
		// Iterate over the path to go deeper and deeper in the filesystem.
		// Each time the iteration is done inode and directory is replaced.
		
		for(String name: splittedPath){						

			directory = volume.findDirectory(pointer, nextInode);
			
			//Create a map which holds a name and inode number of each element
			TreeMap map = volume.readDirectory(directory);			
			Set set = map.entrySet();
			Iterator iterator = set.iterator();			
			
			// Iterate over the map and check is the name is the same as a part of the path.
			// If name is the same as a part of splitted path the inode is replaced.
			while(iterator.hasNext()){
				
				Map.Entry entry = (Map.Entry)iterator.next();				
				
				if(entry.getKey().equals(name)){
					
					nextInode = (Integer) entry.getValue();
			
					if(nextInode > volume.getInodesInGrup() && nextInode < volume.getInodesInGrup() * 2 ){				
						
						pointer = volume.findInodePointer(1);
						nextInode -= (int)(long) volume.getInodesInGrup();						
					}
					if(nextInode > 2 *  volume.getInodesInGrup()){		
					
						pointer = volume.findInodePointer(2);
						nextInode -= (int)(long) volume.getInodesInGrup() * 2;
					}
					if(i == splittedPath.length - 1){
						
						ArrayList<Long> newDir = volume.findDirect(pointer, nextInode);
						directories.addAll(newDir);						
					}
				}				
			}
			i++;			
		}

		// After reaching the final destination we iterate over the directory and display the contents of it in a unix style.

		for(long d: directories){
			
			if(d != 0){
		
				TreeMap map = volume.readDirectory(d);			
					
				Set set = map.entrySet();
				Iterator iterator = set.iterator();		
				
				while(iterator.hasNext()){
						
					Map.Entry entry = (Map.Entry)iterator.next();
						
					nextInode = (Integer) entry.getValue();
					pointer = volume.findInodePointer(0);
					
					if(nextInode > volume.getInodesInGrup() && nextInode < volume.getInodesInGrup() * 2 ){
								
						pointer = volume.findInodePointer(1);
						nextInode -= (int)(long) volume.getInodesInGrup();							
					}
					
					else if(nextInode > 2 *  volume.getInodesInGrup()){		
							
						pointer = volume.findInodePointer(2);
						nextInode -= (int)(long) volume.getInodesInGrup() * 2;						
					}
							
					long mode = volume.getFileMode(pointer, nextInode);	
					Date dateMod = volume.getModTime(pointer, nextInode);	
					long size = volume.getFileSize(pointer, nextInode);
					long diskRef = volume.getHardLinks(pointer, nextInode);	
					long userID = volume.getUserID(pointer, nextInode);	
					long groupID = volume.getGroupID(pointer, nextInode);	
					
					System.out.println(help.fileMode(mode) + " " + diskRef + " " + userID + " " + groupID + " " + size + " " + dateMod + " " + entry.getKey());	
				}
			}
		}
	}	
}
            
