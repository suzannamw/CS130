package edu.ucla.loni.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class Download extends HttpServlet{
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		// TODO
		// Create API for how this works
		boolean single = true;
		
		// If one file
		if (single){
			String filename = request.getParameter("filename");
			if (filename.endsWith(".pipe")){
				// Set the Content Type to XML and the name of the file
				response.setContentType("text/xml"); 
				response.setHeader("Content-Disposition", "attachment; filename=" + filename); 			
            
	            OutputStream out = response.getOutputStream();
	            FileInputStream in = new FileInputStream(filename);
	            
	            // Use buffered streams for faster speed 
	            BufferedInputStream buf_in = new BufferedInputStream(in);
	            BufferedOutputStream buf_out = new BufferedOutputStream(out);
	            
	            int length = 0;
	            byte[] buffer = new byte[8192];
	            
	            while ((length = buf_in.read(buffer)) != -1){
	            	buf_out.write(buffer, 0, length);
	            }
	            
	            // Close and flush the streams
	            buf_in.close();
	            buf_out.flush();
	            buf_out.close();
	            
	            in.close();
	            out.flush();
	            out.close();
			}
		}
		else {
			// TODO: Create and download zip
		}
	}
}
