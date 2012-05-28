package edu.ucla.loni.client;

import edu.ucla.loni.shared.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("file")
public interface FileService extends RemoteService {
	Directory	getDirectory(String absolutePath) throws Exception;
	
	Pipefile[]	getFiles(Directory root) throws Exception;
	
	Pipefile[] 	getSearchResults(Directory root, String query) throws Exception;
	
	void		updateFile(Directory root, Pipefile pipe) throws Exception;
	
	void		removeFiles(Directory root, Pipefile[] pipes) throws Exception;	
	void 		copyFiles(Directory root, Pipefile[] pipes, String packageName) throws Exception;
	void 		moveFiles(Directory root, Pipefile[] pipes, String packageName) throws Exception;
	
	Group[]		getGroups(Directory root) throws Exception;
	void		updateGroup(Directory root, Group group) throws Exception;
	void		removeGroups(Directory root, Group[] groups) throws Exception;
}
