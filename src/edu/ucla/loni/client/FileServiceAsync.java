package edu.ucla.loni.client;

import edu.ucla.loni.shared.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface FileServiceAsync {
	void getFiles(String root, boolean useMonitorFile, AsyncCallback<Pipefile[]> callback);
	
	void getSearchResults(String root, String query, AsyncCallback<Pipefile[]> callback);
	
	void updateFile(String root, Pipefile pipe, AsyncCallback<Void> callback);
	
	void removeFiles(String root, Pipefile[] pipes, AsyncCallback<Void> callback);
	void copyFiles(String root, Pipefile[] pipes, String packageName, AsyncCallback<Void> callback);
	void moveFiles(String root, Pipefile[] pipes, String packageName, AsyncCallback<Void> callback);
	
	void getGroups(String root, AsyncCallback<Group[]> callback);
	
	void updateGroup(String root, Group g, AsyncCallback<Void> callback);
	void removeGroups(String root, Group[] groups, AsyncCallback<Void> callback);
}
