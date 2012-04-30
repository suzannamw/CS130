package edu.ucla.loni.client;

import edu.ucla.loni.shared.FileTree;
import edu.ucla.loni.shared.Group;
import edu.ucla.loni.shared.Pipefile;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface FileServiceAsync {
	void getPackageTree(String root, AsyncCallback<FileTree> callback);
	void getSearchResults(String root, String query, AsyncCallback<FileTree> callback);
	
	void getFile(String filename, AsyncCallback<Pipefile> callback);
	
	void updateFile(Pipefile pipe, AsyncCallback<Void> callback);
	void removeFile(String[] filenames, AsyncCallback<Void> callback);
	void copyFile(String[] filenames, String folder, AsyncCallback<Void> callback);
	void moveFile(String[] filenames, String folder, AsyncCallback<Void> callback);
	
	void getGroups(AsyncCallback<Group[]> callback);
	void updateGroup(Group g, AsyncCallback<Void> callback);
}
