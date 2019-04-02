public class Driver{
	
	/**
	* A simple example of reading ext2 file system.
	*/
	
	public static void main(String[] args){		
		
		Volume volume = new Volume("ext2fs"); 
		
		Directory d = new Directory(volume);		
		d.fileInfo("/../");	
		
		Ext2File file = new Ext2File(volume, "/two-cities");
		file.seek(10);
		byte[] b = file.read(0L, file.size());
		System.out.format ("%s\n", new String(b));


		//volume.readInode(1720);
		//volume.readGroupDesc(1);
		//volume.readSuperBlock();
		//volume.readBlockGroup(0);		
	}
}
