package edu.ucla.loni.client;

import edu.ucla.loni.shared.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("file")
public interface FileService extends RemoteService {
	Pipefile[]	getFiles(String root) throws Exception;
	
	Pipefile[] 	getSearchResults(String root, String query) throws Exception;
	
	void 		updateFile(Pipefile pipe) throws Exception;
	
	void		removeFiles(Pipefile[] pipes) throws Exception;	
	void 		copyFiles(Pipefile[] pipes, String packageName) throws Exception;
	void 		moveFiles(Pipefile[] pipes, String packageName) throws Exception;
	
	Group[]		getGroups() throws Exception;
	void		updateGroup(Group group) throws Exception;
	void		removeGroups(Group[] groups) throws Exception;
}
