package edu.ucla.loni.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Pipefile implements Serializable {
	// General Properties
	public String absolutePath;
	public String name;
	public String type;		// "Data", "Modules", or "Workflows"
	public String packageName;
	public String description;
	public String tags;
	public String access;
	
	// Type specific properties
	// TODO input / output 		- For Data
	public String location;		// - For Modules
	public String uri;			// - For Modules and Workflows
	
	public Pipefile(){
		absolutePath = "";
		name = "";
		type = "";
		packageName = "";
		description = "";
		tags = "";
		access = "";
		location = "";
		uri = "";
	}
}
