package edu.ucla.loni.client;

import edu.ucla.loni.shared.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface FileServiceAsync {
	void getDirectory(String absolutePath, AsyncCallback<Directory> callback);
	
	void getFiles(Directory root, AsyncCallback<Pipefile[]> callback);
	
	void getSearchResults(Directory root, String query, AsyncCallback<Pipefile[]> callback);
	
	void updateFile(Directory root, Pipefile pipe, AsyncCallback<Void> callback);
	
	void removeFiles(Directory root, Pipefile[] pipes, AsyncCallback<Void> callback);
	void copyFiles(Directory root, Pipefile[] pipes, String packageName, AsyncCallback<int[]> callback);
	void moveFiles(Directory root, Pipefile[] pipes, String packageName, AsyncCallback<int[]> callback);
	
	void getGroups(Directory root, AsyncCallback<Group[]> callback);
	
	void updateGroup(Directory root, Group g, AsyncCallback<Void> callback);
	void removeGroups(Directory root, Group[] groups, AsyncCallback<Void> callback);
}
