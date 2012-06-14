/*
 * Copyright 2012 Michael Chang, Tai-Lin Chu, Artin Menachekanian,
 *                Charles Rudolph, Eduard Sedakov, Suzanna Whiteside
 * 
 * This file is part of ServerLibraryManager.
 *
 * ServerLibraryManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ServerLibraryManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ServerLibraryManager.  If not, see <http://www.gnu.org/licenses/>.
 */

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
