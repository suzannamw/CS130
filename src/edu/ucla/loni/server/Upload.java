package edu.ucla.loni.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class Upload extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO
		// Everything comes in as a form that has been POSTED
		
		// If uploading from a URL
		  // Get the file from the URL
		  // Add it to the filesystem
		  // Update the database
		// If uploading a folder or files
		  // For each file
		    // Add it to the filesystem
		    // Update the database
	}
}
