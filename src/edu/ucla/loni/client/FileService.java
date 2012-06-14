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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("file")
public interface FileService extends RemoteService {
	Directory	getDirectory(String absolutePath) throws Exception;
	
	Pipefile[]	getFiles(Directory root) throws Exception;
	
	Pipefile[] 	getSearchResults(Directory root, String query) throws Exception;
	
	void		updateFile(Directory root, Pipefile pipe) throws Exception;
	
	void		removeFiles(Directory root, Pipefile[] pipes) throws Exception;	
	int[] 		copyFiles(Directory root, Pipefile[] pipes, String packageName) throws Exception;
	int[] 		moveFiles(Directory root, Pipefile[] pipes, String packageName) throws Exception;
	
	Group[]		getGroups(Directory root) throws Exception;
	void		updateGroup(Directory root, Group group) throws Exception;
	void		removeGroups(Directory root, Group[] groups) throws Exception;
}
