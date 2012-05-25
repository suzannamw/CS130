package edu.ucla.loni.server;

import edu.ucla.loni.shared.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;

public class Database {
	////////////////////////////////////////////////////////////
	// Private Variables
	////////////////////////////////////////////////////////////
	private static Connection db_connection;
	private static String db_name = "jdbc:hsqldb:hsql://localhost:9002/xdb1";
	private static String db_username = "SA";
	private static String db_password = "";
	
	/**
	 *  Returns a connection to the database
	 */
	private static Connection getDatabaseConnection() throws Exception {
		if (db_connection == null){
			Class.forName("org.hsqldb.jdbcDriver");
			db_connection = DriverManager.getConnection(db_name, db_username, db_password);
		}
		
		return db_connection;
	}
	
	////////////////////////////////////////////////////////////
	// Directory
	////////////////////////////////////////////////////////////
	
	public static Directory selectDirectory(String absolutePath) throws Exception{
		Connection con = getDatabaseConnection();
		
		PreparedStatement stmt = con.prepareStatement(
			"SELECT * " +
			"FROM directories " +		
			"WHERE absolutePath = ?"		
		);
		stmt.setString(1, absolutePath);
		ResultSet rs = stmt.executeQuery();
		
		if (rs.next()){
			Directory dir = new Directory();
			
			dir.dirId = rs.getInt(1);
			dir.absolutePath = rs.getString(2);
			dir.monitorModified = rs.getTimestamp(3);
			dir.accessModified = rs.getTimestamp(4);
			
			return dir;
		} else {
			return null;
		}
	}
	
	public static void insertDirectory(String absolutePath, Timestamp monitorModified, Timestamp accessModified) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"INSERT INTO directories (absolutePath, monitorModified, accessModified) " +
			"VALUES (?, ?, ?)" 		
		);
		stmt.setString(1, absolutePath);
		stmt.setTimestamp(2, monitorModified);
		stmt.setTimestamp(3, accessModified);
		int updated = stmt.executeUpdate();
		
		if (updated != 1){
			throw new Exception("Failed to insert row into 'directory'");
		}
	}
	
	public static void updateDirectory(Directory dir) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"UPDATE directories " +
			"SET monitorModified = ?, accessModified = ? " +
			"WHERE directoryId = ?" 		
		);
		stmt.setTimestamp(1, dir.monitorModified);
		stmt.setTimestamp(2, dir.accessModified);
		stmt.setInt(3, dir.dirId);
		int updated = stmt.executeUpdate();
		
		if (updated != 1){
			throw new Exception("Failed to update row in 'directory'");
		}
	}
	
	////////////////////////////////////////////////////////////
	// Pipefile
	////////////////////////////////////////////////////////////
	
	/**
	 * ResultSet is from a query with the following form 
	 *   SELECT * FROM pipefile WHERE ...
	 */
	private static Pipefile[] resultSetToPipefileArray(ResultSet rs) throws Exception{
		ArrayList<Pipefile> list = new ArrayList<Pipefile>();
		
		while (rs.next()) {
			Pipefile p = new Pipefile();
			
			p.fileId = rs.getInt(1);
			// directoryId at index 2
			p.absolutePath = rs.getString(3);
			// lastModified at index 4
			
			p.name = rs.getString(5);
			p.type = rs.getString(6);
			p.packageName = rs.getString(7);
			p.description = rs.getString(8);
			p.tags = rs.getString(9);
			p.access = rs.getString(10);
			
			p.values = rs.getString(11);
			p.formatType = rs.getString(12);
			p.location = rs.getString(13);
			p.uri = rs.getString(14);
			
			list.add(p);
		}
		
		if (list.size() > 0){
			Pipefile[] ret = new Pipefile[list.size()];
			return list.toArray(ret);
		} else {
			return null;
		}
	}
	
	public static Pipefile[] selectPipefiles(int dirId) throws Exception {
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
	    	"SELECT * " +
			"FROM pipefiles " +
			"WHERE directoryID = ? " +
			"ORDER BY absolutePath"
		);
	    stmt.setInt(1, dirId);
		ResultSet rs = stmt.executeQuery();
		
		return resultSetToPipefileArray(rs);
	}
	
	public static Pipefile[] selectPipefilesSearch(int dirId, String query) throws Exception {
		query = "%" + query.toLowerCase() + "%";
		
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"SELECT * " +
			"FROM pipefiles " +
			"WHERE directoryID = ? " +
				"AND (LCASE(name) LIKE ? " +
				     "OR LCASE(packageName) LIKE ? " +
				     "OR LCASE(description) LIKE ? " +
				     "OR LCASE(tags) LIKE ? )" 
		);
		stmt.setInt(1, dirId);
		stmt.setString(2, query);
		stmt.setString(3, query);
		stmt.setString(4, query);
		stmt.setString(5, query);
		ResultSet rs = stmt.executeQuery();
		
		return resultSetToPipefileArray(rs);
	}
	
	public static int selectPipefileId(String absolutePath) throws Exception {
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
	    	"SELECT fileId " +
			"FROM pipefiles " +
			"WHERE absolutePath = ? "
		);
	    stmt.setString(1, absolutePath);
		ResultSet rs = stmt.executeQuery();
		
		if (rs.next()){
			return rs.getInt(1);
		} else {
			return -1;
		}
	}
	
	public static Timestamp selectPipefileLastModified(String absolutePath) throws Exception {
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
	    	"SELECT lastModified " +
			"FROM pipefiles " +
			"WHERE absolutePath = ? "
		);
	    stmt.setString(1, absolutePath);
		ResultSet rs = stmt.executeQuery();
		
		if (rs.next()){
			return rs.getTimestamp(1);
		} else {
			return null;
		}
	}
	
	public static void insertPipefile(int dirId, Pipefile pipe, Timestamp modified) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"INSERT INTO pipefiles (" +
				"directoryID, absolutePath, lastModified, " +
				"name, type, packageName, description, tags, access, " +
				"dataValues, formatType, location, uri) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
		);
		stmt.setInt(1, dirId);
		stmt.setString(2, pipe.absolutePath);
		stmt.setTimestamp(3, modified);
		
		stmt.setString(4, pipe.name);
		stmt.setString(5, pipe.type);
		stmt.setString(6, pipe.packageName);
		stmt.setString(7, pipe.description);
		stmt.setString(8, pipe.tags);
		stmt.setString(9, pipe.access);
		
		stmt.setString(10, pipe.values);
		stmt.setString(11, pipe.formatType);
		stmt.setString(12, pipe.location);
		stmt.setString(13, pipe.uri);
		
		int inserted = stmt.executeUpdate();
		
		if (inserted != 1){
			throw new Exception("Failed to insert row into database");
		}
	}
	
	public static void updatePipefile(Pipefile pipe, Timestamp modified) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"UPDATE pipefiles " +
		    "SET name = ?, type = ?, packageName = ?, description = ?, tags = ?, access = ?, " +
		    "dataValues = ?, formatType = ?, location = ?, uri = ?, " +
		    "absolutePath = ?, lastModified = ? " +
			"WHERE fileId = ?"
		);
		
		stmt.setString(1, pipe.name);
		stmt.setString(2, pipe.type);
		stmt.setString(3, pipe.packageName);
		stmt.setString(4, pipe.description);
		stmt.setString(5, pipe.tags);
		stmt.setString(6, pipe.access);
		
		stmt.setString(7, pipe.values);
		stmt.setString(8, pipe.formatType);
		stmt.setString(9, pipe.location);
		stmt.setString(10, pipe.uri);
		
		stmt.setString(11, pipe.absolutePath);
		stmt.setTimestamp(12, modified);
		
		stmt.setInt(13, pipe.fileId);
		
		int updated = stmt.executeUpdate();
		
		if (updated != 1){
			throw new Exception("Failed to insert row into database");
		}
	}
	
	public static void deletePipefile(Pipefile pipe) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"DELETE FROM pipefiles " +
			"WHERE fileId = ?" 		
		);
		stmt.setInt(1, pipe.fileId);
		
		int deleted = stmt.executeUpdate();
		
		if (deleted != 1){
			throw new Exception("Failed to delete row from 'pipefile' table");
		}
	}
	
	////////////////////////////////////////////////////////////
	// Groups
	////////////////////////////////////////////////////////////
	
	/**
	 * ResultSet is from a query with the following form 
	 *   SELECT groupId, name, users, COUNT(fileId) 
	 *   FROM groups AS g JOIN groupPipefileConnections AS gpc ON g.gorupId = gpc.groupId
	 *   WHERE ...
	 *   GROUP BY groupId
	 */
	private static Group[] resultSetToGroupArray(ResultSet rs) throws Exception{
		ArrayList<Group> list = new ArrayList<Group>();
		
		while (rs.next()) {
			Group g = new Group();
			
			g.groupId = rs.getInt(1);
			g.name = rs.getString(2);
			g.users = rs.getString(3);
			g.canRemove = ( rs.getInt(4) == 0 );
			
			list.add(g);
		}
		
		if (list.size() > 0){
			Group[] ret = new Group[list.size()];
			return list.toArray(ret);
		} else {
			return null;
		}
	}
	
	/**
	 *  Select all groups
	 */
	public static Group[] selectGroups(int dirId) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"SELECT groupId, name, users, COUNT(depId) " +
			"FROM groups AS g LEFT JOIN groupDependencies AS gd ON g.groupId = gd.groupId " +
			"WHERE directoryId = ?" +
			"GROUP BY groupId"
		);
		stmt.setInt(1, dirId);
		ResultSet rs = stmt.executeQuery();
		
		return resultSetToGroupArray(rs);
	}
	
	/**
	 *  Select a group's id by name
	 */
	public static int selectGroupId(String name) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"SELECT groupId " +
			"FROM groups " +
			"WHERE name = ?"
		);
		stmt.setString(1, name);
		ResultSet rs = stmt.executeQuery();
		
		if (rs.next()){
			return rs.getInt(1);
		} else {
			return -1;
		}
	}
	
	/**
	 *  Insert a group
	 */
	public static void insertGroup(int dirId, Group group) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"INSERT INTO groups (directoryId, name, users) " +
			"VALUES (?, ?, ?)"
		);
		stmt.setInt(1, dirId);
		stmt.setString(2, group.name);
		stmt.setString(3, group.users);
		
		int inserted = stmt.executeUpdate();
		
		if (inserted != 1){
			throw new Exception("Failed to insert row into 'groups' table");
		}
	}
	
	/**
	 *  Update a group
	 */
	public static void updateGroup(Group group) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"UPDATE groups " +
		    "SET name = ?, users = ? " +
			"WHERE groupId = ?"
		);
		
		stmt.setString(1, group.name);
		stmt.setString(2, group.users);
		stmt.setInt(3, group.groupId);
		
		int updated = stmt.executeUpdate();
		
		if (updated != 1){
			throw new Exception("Failed to update row in 'groups' table");
		}
	}
	
	/**
	 *  Delete a group
	 */
	public static void deleteGroup(Group group) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"DELETE FROM groups " +
			"WHERE groupId = ?" 		
		);
		stmt.setInt(1, group.groupId);
		
		int deleted = stmt.executeUpdate();
		
		if (deleted != 1){
			throw new Exception("Failed to delete row from 'groups' table");
		}
	}
	
	////////////////////////////////////////////////////////////
	// GroupPipefileCOnnections
	////////////////////////////////////////////////////////////
	
	/**
	 * Inserts a group dependency such that the 
	 * object specified by depType and depId is dependent 
	 * on the definition of the group specified by groupId
	 * 
	 * @param groupId 
	 * @param depType 0 for group, 1 for file
	 * @param depId
	 * @throws Exception
	 */
	public static void insertGroupDependency(int groupId, int depType, int depId) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"INSERT INTO groupDependencies (groupId, depType, depId) " +
			"VALUES (?, ?, ?)"
		);
		stmt.setInt(1, groupId);
		stmt.setInt(2, depType);
		stmt.setInt(3, depId);
		
		int inserted = stmt.executeUpdate();
		
		if (inserted != 1){
			throw new Exception("Failed to insert row into 'groupDependencies' table");
		}
	}
	
	/**
	 * Deletes all group dependencies for the 
	 * object specified by depType and depId
	 * 
	 * @param depTyp 0 for group, 1 for file
	 * @param depId
	 * @throws Exception
	 */
	public static void deleteGroupDependency(int depType, int depId) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"DELETE FROM groupDependencies " +
			"WHERE depType = ? AND depId = ?" 		
		);
		stmt.setInt(1, depType);
		stmt.setInt(2, depId);
		
		stmt.executeUpdate();
	}
}
