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

package edu.ucla.loni.shared;

import java.io.Serializable;
import java.sql.Timestamp;

@SuppressWarnings("serial")
public class Pipefile implements Serializable {
	// Identifiers
	public int fileId;
	public int directoryId;
	public String absolutePath;
	public Timestamp lastModified;
	
	// General Properties
	public String name;
	public String type;				// "Data", "Modules", or "Groups"
	public String packageName;
	public String description;
	public String tags;
	public String access;
	
	// Type specific properties
	public String values;			// For Data
	public String valuesPrefix;		// For Data
	public String formatType;		// For Data
	public String location;			// For Modules
	public String locationPrefix; 	// For Modules
	public String uri;				// For Modules and Workflows
	
	public boolean nameUpdated;  // Easy way to tall if the name changed, if true change filename
	public boolean packageUpdated; 	// Easy way to tell if the package changed, if true move file
	
	public Pipefile(){
		fileId = -1;
		directoryId = -1;
		absolutePath = "";
		
		name = "";
		type = "";
		packageName = "";
		description = "";
		tags = "";
		access = "";
		
		values = "";
		valuesPrefix = "";
		formatType = "";
		location = "";
		locationPrefix = "";
		uri = "";
		
		nameUpdated = false;
		packageUpdated = false;
	}
}
