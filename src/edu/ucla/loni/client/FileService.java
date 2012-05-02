package edu.ucla.loni.client;

import edu.ucla.loni.shared.FileTree;
import edu.ucla.loni.shared.Group;
import edu.ucla.loni.shared.Pipefile;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("file")
public interface FileService extends RemoteService {
	FileTree	getPackageTree(String root);
	FileTree 	getSearchResults(String root, String query);
	
	Pipefile 	getFile(String filename);
	
	void 		updateFile(Pipefile pipe);
	
	void		removeFile(String filename);
	void		removeFiles(String filenames[]);
	
	void 		copyFile(String filename, String packageName);
	void 		copyFiles(String[] filenames, String packageName);
	
	void 		moveFile(String filename, String packageName);
	void 		moveFiles(String[] filenames, String packageName);
	
	Group[]		getGroups();
	void		updateGroup(Group g);	
}
