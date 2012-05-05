package edu.ucla.loni.client;

import edu.ucla.loni.shared.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("file")
public interface FileService extends RemoteService {
	Pipefile[]	getFiles(String root) throws Exception;
	
	Pipefile[] 	getSearchResults(String root, String query) throws Exception;
	
	void 		updateFile(Pipefile pipe) throws Exception;
	
	void		removeFiles(String filenames[]) throws Exception;	
	void 		copyFiles(String[] filenames, String packageName) throws Exception;
	void 		moveFiles(String[] filenames, String packageName) throws Exception;
	
	Group[]		getGroups() throws Exception;
	void		updateGroup(Group g) throws Exception;	
}
