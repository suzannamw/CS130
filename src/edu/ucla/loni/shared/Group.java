package edu.ucla.loni.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Group implements Serializable {
	public int groupId;
	public int directoryId;
	
	public String name;
	public String users;
	
	public Group() {
		groupId = -1;
		directoryId = -1;
		name = "";
		users = "";
	}
}

