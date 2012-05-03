package edu.ucla.loni.server;

import java.io.File;

import edu.ucla.loni.client.FileService;
import edu.ucla.loni.shared.FileTree;
import edu.ucla.loni.shared.Group;
import edu.ucla.loni.shared.Pipefile;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class FileServiceImpl extends RemoteServiceServlet implements FileService {
	/**
	 *  Returns a FileTree that represents the root directory
	 *  <br>
	 *  Thus the children are the packages
	 *  @param root the absolute path of the root directory
	 */
	public FileTree getPackageTree(String root) {
		File rootFolder = new File(root);
		
		if (rootFolder.exists() && rootFolder.isDirectory()){
			return buildFileTree(rootFolder);
		} else {
			return null;
		}
	}
	
	/**
	 *  Returns a FileTree where the children are all files and are the search results
	 *  @param root the absolute path of the root directory
	 *  @param query what the user is searching for
	 */
	public FileTree getSearchResults(String root, String query){
		// TODO
		// Call database to get results
		// Change to proper format
		return null;
	}
	
	/**
	 *  Retrieves a file from the serv stores it as a pipefile
	 *  @param filename absolute path of the file
	 *  @return a Pipefile if the file is found or null
	 */
	public Pipefile getFile(String filename){
		// TODO
		// If file exists
		//    create pipefile
		//    set filename
		//    set xml to contents of the file
		//    set the accessRestrictions to those retrieved from the database
		//    return the pipefile
		// Else
		//    return null
		return null;
	}
	
	/**
	 *  Updates the file on the server
	 *  @param pipe Pipefile representing the updated file
	 */
	public void updateFile(Pipefile pipe){
		// TODO
		// Read the filename
		//   If the file exists
		//      set the contents of the file to xml
		//      if the package changed update the name, call move file
		//      update the row corresponding to this file in the database
		//      rewrite the restictions file
		//   Else
		//      return
		return;
	}
	
	/**
	 *  Removes a file from the server
	 *  @param filename absolute path of the file
	 */
	public void removeFile(String Filename) {
		// TODO
		// If the file exists
		//   delete the file
		//   delete the row corresponding to this file in the database
		//   update access restrictions file
		return;
	}
	
	/**
	 *  Removes files from the server
	 *  @param filenames absolute paths of the files
	 */
	public void removeFiles(String filenames[]){
		// TODO
		// For each filename
		//   Call removeFile
		return;
	}
	
	/**
	 *  Copy a file from the server to the proper package
	 *  @param filename absolute path of the file
	 *  @param packageName absolute path of the package
	 */
	public void copyFile(String filename, String packageName){
		// TODO
		// If the file exists
		//   Copy the file to the new destination (use updateFilenameForPackage)
		//   Modify the file at its new destination to update the package
		//   Insert a row corresponding to this file in the database
		return;	
	}
	
	/**
	 *  Copies files from the server to the proper package
	 *  @param filenames absolute paths of the files
	 *  @param packageName absolute path of the package
	 */
	public void copyFiles(String[] filenames, String packageName){
		// TODO
		// For each filename
		//   Call copyFile
		return;
	}
	
	/**
	 *  Move a file from the server to the proper package
	 *  @param filename absolute path of the file
	 *  @param packageName absolute path of the package
	 */
	public void moveFile(String filenames, String packageName){
		// TODO
		// Call copyFile
		// Call removeFile
		return;
	}
	
	/**
	 *  Moves files from the server to the proper package
	 *  @param filenames absolute paths of the files
	 *  @param packageName absolute path of the package
	 */
	public void moveFiles(String[] filenames, String packageName){
		// TODO
		// For each filename
		//   Call moveFile
		return;
	}
	
	/**
	 *  Returns an array of all the groups
	 */
	public Group[] getGroups(){
		// TODO
		// Call the database for a list of groups
		// Convert to proper format
		return null;
	}
	
	/**
	 *  Updates a group on the server
	 *  @param group group to be updated
	 */
	public void	updateGroup(Group group){
		// TODO
		// Update the row in the database corresponding to this group
		return;
	}
	
	////////////////////////////////////////////////////////////
	// Private Methods
	////////////////////////////////////////////////////////////
	
	/**
	 *  Builds a Filetree from a specific folder
	 *  @param folder
	 *  @return FileTree for the particular folder
	 */
	private FileTree buildFileTree(File folder){
		// Get the children
		File[] files = folder.listFiles();
		
		// Create the return value
		FileTree ret = new FileTree();
		ret.name = folder.getName();
		ret.fullPath = folder.getPath();
		ret.folder = true;
		ret.children = new FileTree[files.length];
		
		int index = 0;
		for (File f : files){			
			// If its a directory, make a recursive call
			if (f.isDirectory()){
				ret.children[index] = buildFileTree(f);
			} else {
				FileTree child = new FileTree();
				child.name = f.getName();
				child.fullPath = f.getPath();
				child.folder = false;
				
				// TODO
				// Query the database for the row about this file
				// If a does not exist or the data is invalid (compare date modified)
				//   Read the file, determine the module type
				//   Update the database
				
				ret.children[index] = child;
			}
			
			index++;
		}
		
		return ret;
	}
	
	private String updateFilenameForPackage(String filename, String Package){
		// Ex: Filename = C:\Users\charlie\Desktop\PipelineRoot1\AAL\Data\AAL_Atlas.data
		//     Package = AFNI
		// 	   Return value = C:\Users\charlie\Desktop\PipelineRoot1\AFNI\Data\AAL_Atlas.data
		return "";
	}
}
