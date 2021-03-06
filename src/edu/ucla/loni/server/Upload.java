/*
 * Copyright 2012 Michael Chang, Tai-Lin Chu, Artin Menachekanian,
 *                Charles Rudolph, Eduard Sedakov, Suzanna Whiteside
 * 
 * This file is part of ServerLibraryManager.
 *
 * ServerLibraryManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ServerLibraryManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ServerLibraryManager.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.loni.server;

import edu.ucla.loni.shared.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jdom2.Document;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;

@SuppressWarnings("serial")
public class Upload extends HttpServlet {
	/**
	 *  Writes a file
	 */
	private void writeFile(Directory root, String packageName, InputStream in) throws Exception{
		Document doc = ServerUtils.readXML(in);  
		Pipefile pipe = ServerUtils.parseXML(doc);
		
		// Update the packageName
		if (packageName != null && packageName.length() > 0){
			pipe.packageName = packageName;
			doc = ServerUtils.updateXML(doc, pipe);
		}
		
		// Write the document
		String destPath = ServerUtils.newAbsolutePath(root.absolutePath, pipe.packageName, pipe.type, pipe.name);
		File dest = new File(destPath);
		
		// Create parent folders if needed
		File destDir = dest.getParentFile();
		if (destDir.exists() == false){
			boolean success = destDir.mkdirs();
			if (!success){
				throw new Exception("Destination folders could not be created");
			}
		}
		
		ServerUtils.writeXML(dest, doc);
		pipe.absolutePath = destPath;
		pipe.lastModified = new Timestamp(dest.lastModified());
		Database.insertPipefile(root.dirId, pipe);
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if ( ServletFileUpload.isMultipartContent( req ) ){
			// Create a factory for disk-based file items
			FileItemFactory factory = new DiskFileItemFactory();
			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload( factory );
			
			try	{
				@SuppressWarnings("unchecked")
				List<FileItem> items = upload.parseRequest( req );
				
				// Go through the items and get the root and package
				String rootPath = "";
				String packageName = "";
				
				for ( FileItem item : items ){
					if ( item.isFormField() ){
						if (item.getFieldName().equals("root")){
							rootPath = item.getString();
						} else if (item.getFieldName().equals("packageName")){
							packageName = item.getString();
						}
					}
				}
				
				if (rootPath.equals("")){
					res.getWriter().println("error :: Root directory has not been found.");
					return;
				}
				
				Directory root = Database.selectDirectory(rootPath);
				
				// Go through items and process urls and files
				for ( FileItem item : items ){
					// Uploaded File
					if (item.isFormField() == false){
						String filename = item.getName();;
						if( filename.equals("") == true )
							continue;
						try {
							InputStream in = item.getInputStream();
					//determine if it is pipefile or zipfile
					//if it is pipefile => feed directly to writeFile function
					//otherwise, uncompresse it first and then one-by-one feed to writeFile
					if( filename.endsWith(".zip") )
					{
						ZipInputStream zip = new ZipInputStream(in);
						ZipEntry entry = zip.getNextEntry();
						while(entry != null)
						{
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							if( baos != null )
							{
								byte[] arr = new byte[4096];
								int index = 0;
								while((index = zip.read(arr, 0, 4096)) != -1)
								{
								baos.write(arr, 0, index);
								}
							}
							InputStream is = new ByteArrayInputStream(baos.toByteArray());
							writeFile(root, packageName, is);
							entry = zip.getNextEntry();
						}
						zip.close();
					}
					else
					{
						writeFile(root, packageName, in);
					
					}
							
							
							in.close();
						} 
						catch (Exception e) {
							res.getWriter().println("Failed to upload " + filename + ". " + e.getMessage() );
						}
					}
					// URLs					
					if (item.isFormField() && item.getFieldName().equals("urls") && item.getString().equals("") == false){
						String urlListAsStr = item.getString();
						String[] urlList = urlListAsStr.split("\n");
						
						for (String urlStr : urlList){
	                        try{
			                	URL url = new URL(urlStr);
			                    URLConnection urlc = url.openConnection();
			                    
			                    InputStream in = urlc.getInputStream();
			                    
			                    writeFile(root, packageName, in);
			                    
			                    in.close();
			                }
			                catch(Exception e){
			                	res.getWriter().println("Failed to upload " + urlStr);
			                    return;
			                }
						}
					}
				}
			}
			catch ( Exception e ){
				res.getWriter().println("Error occurred while creating file. Error Message : " + e.getMessage());
			}
		}
	}
}
