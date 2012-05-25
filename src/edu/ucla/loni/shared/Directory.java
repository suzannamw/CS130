package edu.ucla.loni.shared;

import java.io.Serializable;
import java.sql.Timestamp;

@SuppressWarnings("serial")
public class Directory implements Serializable {
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
