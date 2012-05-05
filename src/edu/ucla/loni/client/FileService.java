package edu.ucla.loni.client;

import java.sql.SQLException;

import edu.ucla.loni.shared.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("file")
public interface FileService extends RemoteService {
	Pipefile[]	getFiles(String root);
	
	Pipefile[] 	getSearchResults(String root, String query);
	
	void 		updateFile(Pipefile pipe);
	
	void		removeFile(String filename) throws SQLException;
	void		removeFiles(String filenames[]);
	
	void 		copyFile(String filename, String packageName);
	void 		copyFiles(String[] filenames, String packageName);
	
	void 		moveFile(String filename, String packageName);
	void 		moveFiles(String[] filenames, String packageName);
	
	Group[]		getGroups();
	void		updateGroup(Group g);	
}
