package edu.ucla.loni.server;

import java.io.File;

import java.sql.Timestamp;

import java.util.ArrayList;

import edu.ucla.loni.client.FileService;
import edu.ucla.loni.shared.*;

import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.jdom2.Document;
import java.io.OutputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

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
	private void removeFiles(int dirId) throws Exception{
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
	private void updateFiles(File rootDir) throws Exception {		
		// Clean the database
		int dirId = getDirectoryId(rootDir.getAbsolutePath());	
		removeFiles(dirId);
		
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
	private void removeFile(Pipefile pipe) throws Exception {		
		File f = new File(pipe.absolutePath);
		if (f.exists()){
			f.delete(); // TODO: check return status
			
			Database.deletePipefile(pipe);
			
			// remove parent if this directory is empty
			ServerUtils.recursiveRemoveDir(f.getParentFile());
			
			//TODO: update access restrictions file
		}
	}
	
	/**
	 *  Copy a file from the server to the proper package
	 *  @param filename absolute path of the file
	 *  @param packageName absolute path of the package
	 */
	private void copyFile(Pipefile pipe, String packageName) throws Exception {
		// If the file exists
		//   Copy the file to the new destination
		//     File must be changed update the package
		//   Insert a row corresponding to this file in the database
		
		
		
		// Get old and new absolute path directory
		String oldAbsolutePath = pipe.absolutePath;
		File src = new File(oldAbsolutePath);
		
		if (!src.exists()) return;
		
		String newAbsolutePath = ServerUtils.newAbsolutePath(pipe.absolutePath, packageName, pipe.type);
		File dest = new File(newAbsolutePath);
		Files.copy(src, dest);
		
		
		Pipefile newPipe = pipe;
		
		// Update Pipefile
		newPipe.packageName = packageName;
		newPipe.absolutePath = newAbsolutePath;
		
		// Update XML
		Document doc = ServerUtils.readXML(dest);
		doc = ServerUtils.update(doc, newPipe, true);
		ServerUtils.writeXML(dest, doc);
		
		// Update Database
		Database.insertPipefile(
				getDirectoryId(ServerUtils.extractRootDir(newAbsolutePath)),
				newPipe, new Timestamp(dest.lastModified()));
	}
	
	/**
	 *  Move a file from the server to the proper package
	 *  @param filename absolute path of the file = source path of file
	 *  @param packageName is the name of the package as it appears in the Database in column PACKAGENAME
	 *  @throws Exception 
	 */
	public void moveFile(Pipefile pipe, String packageName) throws Exception{		
		// Get old and new absolute path directory
		String oldAbsolutePath = pipe.absolutePath;
		String newAbsolutePath = ServerUtils.newAbsolutePath(pipe.absolutePath, packageName, pipe.type);
		
		File src = new File(oldAbsolutePath);
		File dest = new File(newAbsolutePath);
		
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
		
		// Update Pipefile
		pipe.packageName = packageName;
		pipe.absolutePath = newAbsolutePath;
		
		// Update XML
		Document doc = ServerUtils.readXML(dest);
		doc = ServerUtils.update(doc, pipe, true);
		ServerUtils.writeXML(dest, doc);
		
		// Update Database
		Database.updatePipefile(pipe, new Timestamp(dest.lastModified()));
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
				updateFiles(rootDir);
				
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
	public void updateFile(Pipefile pipe) throws Exception{
		try {
			// Update the XML
			File file = new File(pipe.absolutePath);
			if (!file.exists() || !file.canRead())
				return;
			
			Document doc = ServerUtils.readXML(file);
			doc = ServerUtils.update(doc, pipe, false);
			ServerUtils.writeXML(file, doc);
			
			// TODO if packageChanged, move file
			// TODO update the database
			// TODO rewrite access file
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
	public void removeFiles(Pipefile[] pipes) throws Exception {
		try {
			for (Pipefile pipe : pipes) {
				removeFile(pipe);
			}
			// TODO rewrite access file	
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
	public void copyFiles(Pipefile[] pipes, String packageName) throws Exception {		
		try {
			for (Pipefile pipe : pipes) {
				copyFile(pipe, packageName);
			}
			// TODO rewrite access file	
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
	public void moveFiles(Pipefile[] pipes, String packageName) throws Exception{
		try {
			for (Pipefile pipe : pipes) {
				moveFile(pipe, packageName);
			}
			// TODO rewrite access file	
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 *  Returns an array of all the groups
	 */
	public Group[] getGroups() throws Exception {
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
	public void	updateGroup(Group group) throws Exception{
		try {
			if (group.groupId == -1){
				Database.insertGroup(group);
			} else {
				Database.updateGroup(group);
			}
			
			//update groups.xml
			File f = new File("groups.xml");
			if (!f.exists()) {
				byte[] buf = "<groups></groups>".getBytes();
				OutputStream out = new FileOutputStream(f);
				out.write(buf);
			}
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(f);
			
			boolean groupExist = false;
			NodeList groups = doc.getElementsByTagName("group");
			for (int i = 0; i < groups.getLength(); i++) {
				Node g = groups.item(i);
				NamedNodeMap attributes = g.getAttributes();
				int groupId = Integer.parseInt(attributes.getNamedItem("groupId").getNodeValue());
				if (groupId == group.groupId) {
					groupExist = true;
					attributes.getNamedItem("name").setTextContent(group.name);
					attributes.getNamedItem("numUsers").setTextContent(Integer.toString(group.numUsers));
					//remove previous users
					NodeList users = g.getChildNodes();
					int users_len = users.getLength();
					for (int j = 0; j < users_len; j++) 
						g.removeChild(users.item(0));
					
					//add users
					String users_string = group.users;
					while (users_string.contains(", ")) {
						Element e = doc.createElement("user");
						e.setTextContent(users_string.substring(0, users_string.indexOf(", ")));
						users_string = users_string.substring(users_string.indexOf(", ") + 2);
						g.appendChild(e);
						
					}
					Element e = doc.createElement("user");
					e.setTextContent(users_string);
					g.appendChild(e);
					
					writeXmlFile(doc, "groups.xml");
				}
			}
			if (groupExist) return;
			//create new group
			Element g = doc.createElement("group");
			g.setAttribute("name", group.name);
			g.setAttribute("groupId", Integer.toString(group.groupId));
			g.setAttribute("numUsers", Integer.toString(group.numUsers));
			String users_string = group.users;
			while (users_string.contains(", ")) {
				Element e = doc.createElement("user");
				e.setTextContent(users_string.substring(0, users_string.indexOf(", ")));
				users_string = users_string.substring(users_string.indexOf(", ") + 2);
				g.appendChild(e);
			}
			Element e = doc.createElement("user");
			e.setTextContent(users_string);
			g.appendChild(e);
			doc.getFirstChild().appendChild(g);
			writeXmlFile(doc, "groups.xml");
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
	public void	removeGroups(Group[] groups) throws Exception{
		try {
			for (Group group: groups){
				Database.deleteGroup(group);
			}
			File f = new File("groups.xml");
			//groups file does not exist
			if (!f.exists()) 
				return;
			//change groups.xml
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(f);
			
			Node e = doc.getFirstChild();
			NodeList filegroups = doc.getElementsByTagName("group");
			int numgroups = filegroups.getLength();
			int[] removeGroupNums = new int[groups.length];
			int index = 0;
			for (int j = 0; j < groups.length; j++) {
				removeGroupNums[j] = -1;
			}

			for (int i = 0; i < numgroups; i++) {
				Node g = filegroups.item(i);
				NamedNodeMap attributes = g.getAttributes();
				int groupId = Integer.parseInt(attributes.getNamedItem("groupId").getNodeValue());
				for (int j = 0; j < groups.length; j++) {
					if (groupId == groups[j].groupId) {
						removeGroupNums[index] = i;
						index++;
					}
				}

			}
			for (int i = 0; i < groups.length; i++) {
				if (removeGroupNums[i] == -1) break;
				e.removeChild(filegroups.item(removeGroupNums[i]-i));
				System.out.println(removeGroupNums[i]);
			}
			writeXmlFile(doc, "groups.xml");
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
}

