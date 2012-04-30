package edu.ucla.loni.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class FileTree implements Serializable{	
	public String name;				// Filename
	public String fullPath;			// Full Server Address 
	public boolean folder;			// True if folder, false if file
	public FileTree[] children;		// If folder, fileTree for all the children
	public ModuleType type;			// If file, this is the moduleType 
}
