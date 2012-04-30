package edu.ucla.loni.server;

import java.io.File;
import java.io.FileFilter;

import edu.ucla.loni.client.FileService;
import edu.ucla.loni.shared.FileTree;
import edu.ucla.loni.shared.Group;
import edu.ucla.loni.shared.ModuleType;
import edu.ucla.loni.shared.Pipefile;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class FileServiceImpl extends RemoteServiceServlet implements FileService {
	/**
	 *  Tree operations
	 */
	public FileTree getPackageTree(String root) {
		File rootFolder = new File(root);
		
		if (rootFolder.exists() && rootFolder.isDirectory()){
			return buildFileTree(rootFolder);
		} else {
			FileTree ret = new FileTree();
			ret.name = "Root directory does not exist or is not a directory";
			return ret;
		}
	}
	
	public FileTree getSearchResults(String root, String query){
		// TODO
		return null;
	}
	
	/**
	 *  File operations
	 */
	public Pipefile getFile(String filename){
		// TODO
		return null;
	}
	
	public void updateFile(Pipefile pipe){
		// TODO
		return;
	}
	
	public void removeFile(String filenames[]){
		// TODO
		return;
	}
	
	public void copyFile(String[] filenames, String folder){
		// TODO
		return;
	}
	
	public void moveFile(String[] filenames, String folder){
		// TODO
		return;
	}
	
	/**
	 *  Group operations
	 */
	public Group[] getGroups(){
		// TODO
		return null;
	}
	
	public void	updateGroup(Group g){
		// TODO
		return;
	}
	
	/**
	 *  Private methods
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
				// If a row exists and its data is valid (compare date modified)
				//   set the moduleType to what is in the database
				// Else 
				//   Read the file, determine the module type
				//   Update the database
				//   Set the moduleType
				child.type = ModuleType.DATA; 
				
				ret.children[index] = child;
			}
			
			index++;
		}
		
		return ret;
	}
}
