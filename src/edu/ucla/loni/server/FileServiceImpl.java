package edu.ucla.loni.server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.ucla.loni.client.FileService;
import edu.ucla.loni.shared.*;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class FileServiceImpl extends RemoteServiceServlet implements FileService {
	////////////////////////////////////////////////////////////
	// Private Variables
	////////////////////////////////////////////////////////////
	private Connection db_connection;
	private String db_name = "jdbc:hsqldb:hsql://localhost/xdb";
	private String db_username = "SA";
	private String db_password = "";
	
	////////////////////////////////////////////////////////////
	// Private Database Functions
	////////////////////////////////////////////////////////////
	/**
	 *  Returns a connection to the database
	 */
	private Connection getDatabaseConnection() throws SQLException {
		if (db_connection == null){
			db_connection = DriverManager.getConnection(db_name, db_username, db_password);
		}
		
		return db_connection;
	}
	
	/**
	 *  Recursively get all pipefiles
	 */
	private ArrayList<File> getAllPipefiles(ArrayList<File> files, File dir){
	    if (dir.isDirectory()){
	    	for (File file : dir.listFiles()){
	    		getAllPipefiles(files, file);
	    	}
	    } else {
	    	String name = dir.getName();
	    	if (name.endsWith(".pipe")){
	    		files.add(dir);	
	    	}
	    }
	    
	    return files;
	}
	
	/** 
	 * Gets the directoryID of the root directory by selecting it from the database,
	 * inserts the directory into the database if needed
	 * 
	 * @param absolutePath absolute path of the root directory  
	 * @return directoryID of the root directory
	 */
	private int getDirectoryId(String absolutePath) throws SQLException{
		int ret = selectDirectoryId(absolutePath);
		if(ret == -1){
			insertDirectoryId(absolutePath);
			ret = selectDirectoryId(absolutePath);
		}
		return ret;
	}
	
	/** 
	 * @param absolutePath absolute path of the root directory  
	 * @return directoryID of the root directory, or -1 if not found
	 */
	private int selectDirectoryId(String absolutePath) throws SQLException{
		Connection con = getDatabaseConnection();
		
		PreparedStatement stmt = con.prepareStatement(
			"SELECT directoryID " +
			"FROM directory " +		
			"WHERE absolutePath = ?"		
		);
		stmt.setString(1, absolutePath);
		ResultSet rs = stmt.executeQuery();
		
		if (rs.next()){
			return rs.getInt(1);
		} else {
			return -1;
		}
	}
	
	
	/** 
	 * Inserts directory into the database, directoryID is automatically generated
	 * 
	 * @param absolutePath absolute path of the root directory  
	 */
	private void insertDirectoryId(String absolutePath) throws SQLException{
		Connection con = getDatabaseConnection();
		
		PreparedStatement stmt = con.prepareStatement(
			"INSERT INTO directory (absolutePath) " +
			"VALUES (?)" 		
		);
		stmt.setString(1, absolutePath);
		stmt.executeUpdate();
	}
	
	private Document parseXML(File pipe) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(pipe);
		
		return doc;
	}
	
	private String getChildElementValue(Element e, String child){
		// TODO
		return "";
	}
	
	/**
	 *  Update the database for this root folder 
	 *  @param root absolute path of the root directory
	 *  @throws SQLException
	 *  @throws ParserConfigurationException 
	 *  @throws IOException 
	 *  @throws SAXException 
	 */
	private void updateDatabase(File rootDir) throws SQLException, ParserConfigurationException, SAXException, IOException {
		// Get all pipefiles recursively under this folder
		ArrayList<File> pipes = getAllPipefiles(new ArrayList<File>(), rootDir);
		
		if (pipes.size() > 0){
			Connection con = getDatabaseConnection();
			 
			int dirID = getDirectoryId(rootDir.getAbsolutePath());
			
			// For each pipefile
			for (File pipe : pipes){			    
			    // Get the dateModified of this pipefile
			    PreparedStatement stmt = con.prepareStatement(
			    	"SELECT lastModified " +
					"FROM pipefile " +
					"WHERE absolutePath = ?" 		
				);
			    stmt.setString(1, pipe.getAbsolutePath());
				ResultSet rs = stmt.executeQuery();
			    
				// Determine if the row needs to be updated or inserted
			    boolean update = false;
			    boolean insert = true;
			    
				if (rs.next()){
					insert = false;
					
					Timestamp db_lastModified = rs.getTimestamp(1);
					Timestamp fs_lastModified = new Timestamp(pipe.lastModified());
					
					// If file has been modified
					if (db_lastModified.equals(fs_lastModified) == false){
						update = true;
					}
				}
				
				// If we need to update or insert a row
			    if (update || insert){
			    	// Parse the file
					Document doc = parseXML(pipe);
					
					NodeList group = doc.getElementsByTagName("moduleGroup");
					NodeList data = doc.getElementsByTagName("dataModule");
					NodeList modules = doc.getElementsByTagName("module");
					
					Node mainNode; // Node which holds the attributes we care out
					String type = "";
					
					if (group.getLength() >= 1 && data.getLength() + modules.getLength() > 2){
						mainNode = group.item(0);
						type = "Workflows";
					} else if (data.getLength() == 1){
						mainNode = data.item(0);
						type = "Data";
					} else if (modules.getLength() == 1){
						mainNode = modules.item(0);
						type = "Modules";
					} else {
						continue;
					}
			    
					// Convert to Element
					Element mainElement = (Element) mainNode;
					
					// General properties
					String absolutePath = pipe.getAbsolutePath();
					Timestamp modified = new Timestamp(pipe.lastModified());
					String access = ""; // Default access is empty
					
					String name = mainElement.getAttribute("name");
					String packageName = mainElement.getAttribute("package");
					String description = mainElement.getAttribute("description");
					String tags = getChildElementValue(mainElement, "tag");
					
					String searchableText = name + " " + packageName + " " + description + " " + tags; 
					
					// Get Type specific properties
					String input = "";
					String output = "";
					String location = "";
					String uri = "";
					
					if (type == "Data"){
						// TODO, 
						// By the schema dataModule does not have output / input elements
						// Also need to know what we are getting out
						input = "";
						output = "";
					}
					
					if (type == "Modules"){
						location = mainElement.getAttribute("location");
					} 
					
					if (type == "Modules" || type == "Workflows"){
						uri = getChildElementValue(mainElement, "uri");
					}
					
					if (insert){
						/*
						 * database schema for pipefile
						 */
						
						stmt = con.prepareStatement(
							"INSERT INTO pipefile (directoryID, absolutePath, name, type, packageName, description, tags, access, " +
								"location, uri, searchableText, lastModified) " +
							"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
						);
						stmt.setInt(1, dirID);
						stmt.setString(2, absolutePath);
						stmt.setString(3, name);
						stmt.setString(4, type);
						stmt.setString(5, packageName);
						stmt.setString(6, description);
						stmt.setString(7, tags);
						stmt.setString(8, access);
						stmt.setString(9, location);
						stmt.setString(10, uri);
						stmt.setString(11, searchableText);
						stmt.setTimestamp(12, modified);
					} else {
						// directoryID and access are not based on the file in the system
						stmt = con.prepareStatement(
							"UPDATE pipefile " +
						    "SET name = ?, type = ?, packageName = ?, description = ?, tags = ?, " +
						    "location = ?, uri = ?, searchableText = ?, lastModified = ? " +
							"WHERE absolutePath = ?"
						);
						stmt.setString(1, name);
						stmt.setString(2, type);
						stmt.setString(3, packageName);
						stmt.setString(4, description);
						stmt.setString(5, tags);
						stmt.setString(6, location);
						stmt.setString(7, uri);
						stmt.setString(8, searchableText);
						stmt.setTimestamp(9, modified);
						stmt.setString(10, absolutePath);
					}
					stmt.executeUpdate();
	 		    }
			}
		}
	}
	
	////////////////////////////////////////////////////////////
	// Public Functions
	////////////////////////////////////////////////////////////
	
	/**
	 *  Returns a FileTree that represents the root directory
	 *  <br>
	 *  Thus the children are the packages
	 *  @param root the absolute path of the root directory
	 */
	public Pipefile[] getFiles(String root) {
		try {
			File rootDir = new File(root);
			if (rootDir.exists() && rootDir.isDirectory()){
				updateDatabase(rootDir);
				
				Connection con = getDatabaseConnection();
				Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery(
				    "SELECT name, type, packageName, absolutePath " +
				    "FROM pipefile as P, directory AS d " +
				    "WHERE p.directoryID = d.directoryID"
				);
				
				ArrayList<Pipefile> list = new ArrayList<Pipefile>();
				while (rs.next()) {
					Pipefile p = new Pipefile();
					p.name = rs.getString(1);
					p.type = rs.getString(2);
					p.packageName = rs.getString(3);
					p.absolutePath = rs.getString(4);
					
					list.add(p);
				}
				
				Pipefile[] ret = new Pipefile[list.size()];
				ret = list.toArray(ret);
				
				return ret;
			}
			else {
				return null;
			}
		} catch (SQLException e){
			Pipefile[] ret = new Pipefile[1];
			ret[0] = new Pipefile();
			ret[0].name = "SQL Exception: " + e.getMessage();
			ret[0].packageName = "Error";
			ret[0].type = "Error";
			e.printStackTrace();
			return ret;
    	} catch (Exception e){
    		Pipefile[] ret = new Pipefile[1];
			ret[0] = new Pipefile();
			ret[0].name = "Other Exception" + e.getMessage();
			ret[0].packageName = "Error";
			ret[0].type = "Error";
			return ret;
    	}
	}
	
	/**
	 *  Returns a FileTree where the children are all files and are the search results
	 *  @param root the absolute path of the root directory
	 *  @param query what the user is searching for
	 */
	public Pipefile[] getSearchResults(String root, String query){
		// TODO
		// Call database to get results
		// Change to proper format
		return null;
	}
	
	/**
	 *  Updates the file on the server
	 *  @param pipe Pipefile representing the updated file
	 */
	public void updateFile(Pipefile pipe){
		// TODO
		// Read the filename
		//   If the file exists
		//      read, parse the file, update the needed fields
		//      if the package changed update the name, move the file
		//      update the row corresponding to this file in the database
		//      rewrite the access file
		//   Else
		//      return
		return;
	}
	
	/**
	 *  Removes a file from the server
	 *  @param filename absolute path of the file
	 */
	public void removeFile(String Filename) throws SQLException {
		// TODO
		// If the file exists
		//   delete the file
		//   delete the row corresponding to this file in the database
		//   update access restrictions file
		
		File f = new File(Filename);
		if (f.exists()){
			f.delete(); // check return status
			Connection con = getDatabaseConnection();
			
			PreparedStatement stmt = con.prepareStatement(
				"DELETE FROM pipefile " +
				"WHERE absolutePath = ?" 		
			);
			stmt.setString(1, Filename);
			stmt.executeUpdate();
			//todo: update access restrictions file
		}
	}
	
	/**
	 *  Removes files from the server
	 *  @param filenames absolute paths of the files
	 */
	public void removeFiles(String filenames[]){
		// TODO
		// For each filename
		//   Call removeFile
		return;
	}
	
	/**
	 *  Copy a file from the server to the proper package
	 *  @param filename absolute path of the file
	 *  @param packageName absolute path of the package
	 */
	public void copyFile(String filename, String packageName){
		// TODO
		// If the file exists
		//   Copy the file to the new destination
		//     File must be changed update the package
		//   Insert a row corresponding to this file in the database
		return;	
	}
	
	/**
	 *  Copies files from the server to the proper package
	 *  @param filenames absolute paths of the files
	 *  @param packageName absolute path of the package
	 */
	public void copyFiles(String[] filenames, String packageName){
		// TODO
		// For each filename
		//   Call copyFile
		return;
	}
	
	/**
	 *  Move a file from the server to the proper package
	 *  @param filename absolute path of the file
	 *  @param packageName absolute path of the package
	 */
	public void moveFile(String filenames, String packageName){
		// TODO
		// If the file exists
		//   Move the file to the new destination
		//     File must be changed to update the package
		//   Update the row corresponding to this file in the database
		return;
	}
	
	/**
	 *  Moves files from the server to the proper package
	 *  @param filenames absolute paths of the files
	 *  @param packageName absolute path of the package
	 */
	public void moveFiles(String[] filenames, String packageName){
		// TODO
		// For each filename
		//   Call moveFile
		return;
	}
	
	/**
	 *  Returns an array of all the groups
	 */
	public Group[] getGroups(){
		// TODO
		// Call the database for a list of groups
		// Convert to proper format
		return null;
	}
	
	/**
	 *  Updates a group on the server (also used for creating groups)
	 *  @param group group to be updated
	 */
	public void	updateGroup(Group group){
		// TODO
		// Update the row in the database corresponding to this group
		return;
	}
}
