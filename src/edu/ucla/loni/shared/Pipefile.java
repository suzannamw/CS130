package edu.ucla.loni.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Pipefile implements Serializable {
	// Identifiers
	public int fileId;
	public String absolutePath;
	
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
	
	public boolean packageUpdated; 	// Easy way to tell if the package changed
	
	public Pipefile(){
		fileId = 0;
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
		
		packageUpdated = false;
	}
}
