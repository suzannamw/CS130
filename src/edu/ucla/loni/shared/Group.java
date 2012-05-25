package edu.ucla.loni.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Group implements Serializable {
	public int groupId;
	
	public String name;
	public String users;
	
	public boolean dependencies;
	
	public Group() {
		groupId = -1;
		name = "";
		users = "";
		dependencies = false;
	}
}

