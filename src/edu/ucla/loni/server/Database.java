package edu.ucla.loni.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;

import edu.ucla.loni.shared.Pipefile;

public class Database {
	////////////////////////////////////////////////////////////
	// Private Variables
	////////////////////////////////////////////////////////////
	private static Connection db_connection;
	private static String db_name = "jdbc:hsqldb:hsql://localhost/xdb";
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
	
	public static int selectDirectory(String absolutePath) throws Exception{
		Connection con = getDatabaseConnection();
		
		PreparedStatement stmt = con.prepareStatement(
			"SELECT directoryID " +
			"FROM directories " +		
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
	
	public static void insertDirectory(String absolutePath) throws Exception{
		Connection con = getDatabaseConnection();
		PreparedStatement stmt = con.prepareStatement(
			"INSERT INTO directories (absolutePath) " +
			"VALUES (?)" 		
		);
		stmt.setString(1, absolutePath);
		int updated = stmt.executeUpdate();
		
		if (updated != 1){
			throw new Exception("Failed to insert row into 'directory'");
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
		    "dataValues = ?, formatType = ?, location = ?, uri = ?, lastModified = ? " +
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
		stmt.setTimestamp(11, modified);
		
		stmt.setInt(12, pipe.fileId);
		
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
}
