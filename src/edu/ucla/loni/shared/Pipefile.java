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
