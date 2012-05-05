package edu.ucla.loni.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Pipefile implements Serializable {
	public String name;
	public String type;		// "Data", "Modules", or "Workflows"
	public String packageName;
	public String absolutePath;
	public String accessResrictions;
	
	// TODO input / output 		- For Data
	// TODO location			- For Modules
	// TODO server address		- For Modules and Workflows
}
