package edu.ucla.loni.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class Download extends HttpServlet{
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException { 
		String nFiles = request.getParameter("n");
		int n = Integer.parseInt(nFiles);
		
		// If one file
		if (n == 1){
			String filename = request.getParameter("filename_0");
			File file = new File(filename);
			
			// Set the Content Type to XML and the name of the file
			String fShortName = file.getName();
			
			response.setContentType("text/xml"); 
			response.setHeader("Content-Disposition", "attachment; filename=" + fShortName); 			
        
			// Get the input and output streams
            OutputStream out = response.getOutputStream();
            FileInputStream in = new FileInputStream(file);
            
            // Read from input, write to output
            int length = 0;
            byte[] buffer = new byte[8192];
            
            while ((length = in.read(buffer)) != -1){
            	out.write(buffer, 0, length);
            }
           
            // Close the input, flush and close the output
            in.close();
            out.flush();
            out.close();
		}
		else {
			Date today = new Date();
			Timestamp now = new Timestamp(today.getTime());
			String nowStr = new SimpleDateFormat("yyyy-MM-dd_hhmmss").format(now);
			
			// Set the Content Type to XML and the name of the file
			response.setContentType("application/zip"); 
			response.setHeader("Content-Disposition", "attachment; filename=" + "pipefiles_" + nowStr + ".zip"); 
			
			// Create output stream
			OutputStream out = response.getOutputStream();
			ZipOutputStream zip = new ZipOutputStream(out);
			
			// For each file
			for (int i = 0; i < n; i++){
				// Get the filename, get the input stream
				String filename = request.getParameter("filename_" + i);
				File file = new File(filename);
				
				// Add an entry to the zip 
				String fShortName = file.getName();				
				zip.putNextEntry(new ZipEntry(fShortName));
				
				// Write the data to the zip
				FileInputStream in = new FileInputStream(file);
				
	            int length = 0;
	            byte[] buffer = new byte[8192];
	            
	            while ((length = in.read(buffer)) != -1){
	            	zip.write(buffer, 0, length);
	            }
	            
	            // Close the file, flush the zip
	            in.close();
	            zip.flush();
			}
	
			// Close the zip, flush and close the output
			zip.close();
			out.flush();
			out.close();
		}
	}
}
