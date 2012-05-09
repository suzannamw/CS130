package edu.ucla.loni.client;

import edu.ucla.loni.shared.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface FileServiceAsync {
	void getFiles(String root, AsyncCallback<Pipefile[]> callback);
	
	void getSearchResults(String root, String query, AsyncCallback<Pipefile[]> callback);
	
	void updateFile(Pipefile pipe, AsyncCallback<Void> callback);
	
	void removeFiles(Pipefile[] pipes, AsyncCallback<Void> callback);
	void copyFiles(Pipefile[] pipes, String packageName, AsyncCallback<Void> callback);
	void moveFiles(Pipefile[] pipes, String packageName, AsyncCallback<Void> callback);
	
	void getGroups(AsyncCallback<Group[]> callback);
	void updateGroup(Group g, AsyncCallback<Void> callback);
}
