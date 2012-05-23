package edu.ucla.loni.server;

import java.sql.Timestamp;

public class Directory {
	public int dirId;
	public String absolutePath;
	public Timestamp monitorModified;
	public Timestamp accessModified;
	
	public Directory(){
		absolutePath = "";
		monitorModified = null;
		accessModified = null;
	}
}
