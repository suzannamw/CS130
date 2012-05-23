package edu.ucla.loni.client;

import edu.ucla.loni.shared.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("file")
public interface FileService extends RemoteService {
	Pipefile[]	getFiles(String root, boolean useMonitorFile) throws Exception;
	
	Pipefile[] 	getSearchResults(String root, String query) throws Exception;
	
	void 		updateFile(String root, Pipefile pipe) throws Exception;
	
	void		removeFiles(String root, Pipefile[] pipes) throws Exception;	
	void 		copyFiles(String root, Pipefile[] pipes, String packageName) throws Exception;
	void 		moveFiles(String root, Pipefile[] pipes, String packageName) throws Exception;
	
	Group[]		getGroups(String root) throws Exception;
	void		updateGroup(String root, Group group) throws Exception;
	void		removeGroups(String root, Group[] groups) throws Exception;
}
