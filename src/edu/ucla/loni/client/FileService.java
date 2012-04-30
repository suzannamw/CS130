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
	void		removeFile(String filenames[]);
	void 		copyFile(String[] filenames, String folder);
	void 		moveFile(String[] filenames, String folder);
	
	Group[]		getGroups();
	void		updateGroup(Group g);
}
