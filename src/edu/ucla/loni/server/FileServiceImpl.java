package edu.ucla.loni.server;

import java.io.File;

import java.sql.Timestamp;

import java.util.ArrayList;

import edu.ucla.loni.client.FileService;
import edu.ucla.loni.shared.*;

import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.jdom2.Document;

@SuppressWarnings("serial")
public class FileServiceImpl extends RemoteServiceServlet implements FileService {	
	////////////////////////////////////////////////////////////
	// Private Functions
	////////////////////////////////////////////////////////////
	/**
	 *  Recursively get all pipefiles
	 */
	private ArrayList<File> getAllPipefiles(ArrayList<File> files, File dir){
	    if (dir.isDirectory()){
	    	for (File file : dir.listFiles()){
	    		getAllPipefiles(files, file);
	    	}
	    } else {
	    	String name = dir.getName();
	    	if (name.endsWith(".pipe")){
	    		files.add(dir);	
	    	}
	    }
	    
	    return files;
	}
	
	/** 
	 * Gets the directoryID of the root directory by selecting it from the database,
	 * inserts the directory into the database if needed
	 * 
	 * @param absolutePath absolute path of the root directory  
	 * @return directoryID of the root directory
	 */
	private int getDirectoryId(String absolutePath) throws Exception{
		int ret = Database.selectDirectory(absolutePath);
		if(ret == -1){
			Database.insertDirectory(absolutePath);
			ret = Database.selectDirectory(absolutePath);
		}
		return ret;
	}
	
	/**
	 *  Remove files from the database in the case that they were deleted
	 */
	private void cleanDatabase(int dirId) throws Exception{
		Pipefile[] pipes = Database.selectPipefiles(dirId);
		if (pipes != null){
			for(Pipefile pipe : pipes){
				File file = new File(pipe.absolutePath);
				if (file.exists() == false){
					Database.deletePipefile(pipe);
				}
			}
		}
	}
	
	/**
	 *  Update the database for this root folder 
	 *  @param root absolute path of the root directory
	 */
	private void updateDatabase(File rootDir) throws Exception {		
		// Clean the database
		int dirId = getDirectoryId(rootDir.getAbsolutePath());	
		cleanDatabase(dirId);
		
		// Update all files
		ArrayList<File> files = getAllPipefiles(new ArrayList<File>(), rootDir);
				
		// For each pipefile
		for (File file : files){	
			Timestamp db_lastModified = Database.selectPipefileLastModified(file.getAbsolutePath());
		    
			// Determine if the row needs to be updated or inserted
		    boolean update = false;
		    boolean insert = false;
		    
		    Timestamp fs_lastModified = new Timestamp(file.lastModified());
			
		    if (db_lastModified != null){				
				// If file has been modified
				if (db_lastModified.equals(fs_lastModified) == false){
					update = true;
				}
			} 
		    else {
				insert = true;
			}
			
			// If we need to update or insert a row
		    if (update || insert){			    	
		    	Pipefile pipe = ServerUtils.parseFile(file);
				
				if (insert){
					Database.insertPipefile(dirId, pipe, fs_lastModified);
				} else {
					pipe.fileId = Database.selectPipefileId(file.getAbsolutePath());
					Database.updatePipefile(pipe, fs_lastModified);
				}
 		    }
		}
	}
	
	/**
	 *  Removes a file from the server
	 *  @param filename absolute path of the file
	 */
	private void removeFile(String root, Pipefile pipe) throws Exception {		
		File f = new File(pipe.absolutePath);
		if (f.exists()){
			// Delete file on file-system
			boolean success = f.delete();
			if (success == false){
				throw new Exception("Failed to remove file " + pipe.absolutePath);
			}
			
			// Remove parent directory if it is empty
			ServerUtils.recursiveRemoveDir(f.getParentFile());
			
			// Delete file from database
			Database.deletePipefile(pipe);
		}
	}
	
	/**
	 *  Copy a file from the server to the proper package
	 *  @param filename absolute path of the file
	 *  @param packageName absolute path of the package
	 */
	private void copyFile(String root, Pipefile pipe, String packageName) throws Exception {
		// Get old and new absolute path directory
		String oldAbsolutePath = pipe.absolutePath;
		String newAbsolutePath = ServerUtils.newAbsolutePath(pipe.absolutePath, packageName, pipe.type);
		
		File src = new File(oldAbsolutePath);
		File dest = new File(newAbsolutePath);
		
		// If the source does not exist
		if (!src.exists()) return;
		
		// If the destination directory does not exist, create it and necessary parent directories
		File destDir = dest.getParentFile();
		if (destDir.exists() == false){
			boolean success = destDir.mkdirs();
			if (!success){
				throw new Exception("Destination folders could not be created");
			}
		}
		
		// Copy the file
		Files.copy(src, dest);	

		// Update Pipefile
		Pipefile newPipe = pipe;
		newPipe.packageName = packageName;
		newPipe.absolutePath = newAbsolutePath;
		
		// Update XML
		Document doc = ServerUtils.readXML(dest);
		doc = ServerUtils.update(doc, newPipe, true);
		ServerUtils.writeXML(dest, doc);
		
		// Update Database
		int dirId = getDirectoryId(root);
		Timestamp modified = new Timestamp(dest.lastModified());
		Database.insertPipefile(dirId, newPipe, modified);
	}
	
	/**
	 *  Move a file from the server to the proper package
	 *  @param filename absolute path of the file = source path of file
	 *  @param packageName is the name of the package as it appears in the Database in column PACKAGENAME
	 *  @throws Exception 
	 */
	public void moveFile(String root, Pipefile pipe, String packageName) throws Exception{		
		// Get old and new absolute path directory
		String oldAbsolutePath = pipe.absolutePath;
		String newAbsolutePath = ServerUtils.newAbsolutePath(pipe.absolutePath, packageName, pipe.type);
		
		File src = new File(oldAbsolutePath);
		File dest = new File(newAbsolutePath);
		
		// If the source does not exist
		if (!src.exists()) return;
		
		// If the destination directory does not exist, create it and necessary parent directories
		File destDir = dest.getParentFile();
		if (destDir.exists() == false){
			boolean success = destDir.mkdirs();
			if (!success){
				throw new Exception("Destination folders could not be created");
			}
		}
		
		// Move the file
		boolean success = src.renameTo(dest);
		if(success == false) {
			throw new Exception("File could not be moved");
		}
		
		// Remove parent directory if it is empty
		ServerUtils.recursiveRemoveDir(src.getParentFile());
		
		// Update Pipefile
		pipe.packageName = packageName;
		pipe.absolutePath = newAbsolutePath;
		
		// Update XML
		Document doc = ServerUtils.readXML(dest);
		doc = ServerUtils.update(doc, pipe, true);
		ServerUtils.writeXML(dest, doc);
		
		// Update Database
		Timestamp modified = new Timestamp(dest.lastModified());
		Database.updatePipefile(pipe, modified);
	}
	
	////////////////////////////////////////////////////////////
	// Public Functions
	////////////////////////////////////////////////////////////
	
	/**
	 *  Returns a FileTree that represents the root directory
	 *  <br>
	 *  Thus the children are the packages
	 *  @param root the absolute path of the root directory
	 */
	public Pipefile[] getFiles(String root) throws Exception {
		try {
			File rootDir = new File(root);
			if (rootDir.exists() && rootDir.isDirectory()){
				updateDatabase(rootDir);
				
				int dirId = getDirectoryId(root);
				
				return Database.selectPipefiles(dirId);
			} else {
				return null;
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 *  Returns a FileTree where the children are all files and are the search results
	 *  @param root the absolute path of the root directory
	 *  @param query what the user is searching for
	 */
	public Pipefile[] getSearchResults(String root, String query) throws Exception{
		try {
			int dirId = getDirectoryId(root); 
			return Database.selectPipefilesSearch(dirId, query);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 *  Updates the file on the server
	 *  @param pipe Pipefile representing the updated file
	 */
	public void updateFile(String root, Pipefile pipe) throws Exception{
		try {
			// Update the XML
			File file = new File(pipe.absolutePath);
			if (!file.exists() || !file.canRead())
				return;
			
			Document doc = ServerUtils.readXML(file);
			doc = ServerUtils.update(doc, pipe, false);
			ServerUtils.writeXML(file, doc);
			
			// TODO if packageChanged, move file
			
			// Update the database
			Timestamp modified = new Timestamp(file.lastModified());
			Database.updatePipefile(pipe, modified);
			
			// Write the access file
			ServerUtils.writeAccessFile(root);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}

	}
	
	/**
	 *  Removes files from the server
	 *  @param filenames absolute paths of the files
	 * @throws SQLException 
	 */
	public void removeFiles(String root, Pipefile[] pipes) throws Exception {
		try {
			// Remove each file
			for (Pipefile pipe : pipes) {
				removeFile(root, pipe);
			}
			
			// Write the access file
			ServerUtils.writeAccessFile(root);	
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 *  Copies files from the server to the proper package
	 *  @param filenames absolute paths of the files
	 *  @param packageName absolute path of the package
	 */
	public void copyFiles(String root, Pipefile[] pipes, String packageName) throws Exception {		
		try {
			// Copy each file
			for (Pipefile pipe : pipes) {
				copyFile(root, pipe, packageName);
			}

			// Write the access file
			ServerUtils.writeAccessFile(root);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 *  Moves files from the server to the proper package
	 *  @param filenames absolute paths of the files
	 *  @param packageName absolute path of the package
	 *  @throws Exception 
	 */
	public void moveFiles(String root, Pipefile[] pipes, String packageName) throws Exception{
		try {
			// Move each file
			for (Pipefile pipe : pipes) {
				moveFile(root, pipe, packageName);
			}
			
			// Write the access file
			ServerUtils.writeAccessFile(root);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 *  Returns an array of all the groups
	 */
	public Group[] getGroups(String root) throws Exception {
		try {
			return Database.selectGroups();
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 *  Inserts or Updates a group on the server (also used for creating groups)
	 *  @param group group to be updated
	 */
	public void	updateGroup(String root, Group group) throws Exception{
		try {
			// Insert or update the group
			if (group.groupId == -1){
				Database.insertGroup(group);
			} else {
				Database.updateGroup(group);
			}
			
			// Write the access file
			ServerUtils.writeAccessFile(root);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 *  Deletes groups on the server (also used for creating groups)
	 *  @param group group to be updated
	 */
	public void	removeGroups(String root, Group[] groups) throws Exception{
		try {
			// Delete each group
			for (Group group: groups){
				Database.deleteGroup(group);
			}
			
			// Write the access file
			ServerUtils.writeAccessFile(root);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
}

