package edu.ucla.loni.server;

import gwtupload.server.UploadAction;
import gwtupload.server.exceptions.UploadActionException;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;

@SuppressWarnings("serial")
public class Upload extends UploadAction {
	@Override
	  public String executeAction(HttpServletRequest request, List<FileItem> sessionFiles) throws UploadActionException {
	    	String root = request.getParameter("root");
		String response = "";
	    	for (FileItem item : sessionFiles) {
	    		if (false == item.isFormField()) {
	    			try {
	    				/// Create a new file based on the remote file name in the client
	    				// String saveName = item.getName().replaceAll("[\\\\/><\\|\\s\"'{}()\\[\\]]+", "_");
	    				// File file =new File("/tmp/" + saveName);
	          
	    				/// Create a temporary file placed in /tmp (only works in unix)
	    				// File file = File.createTempFile("upload-", ".bin", new File("/tmp"));
	          
	    				/// Create a temporary file placed in the default system temp folder
	    				String name = root + File.separatorChar + item.getName();
	    				File file = new File(name);
	    				item.write(file);
	          
	    				/// Send a customized message to the client.
	    				response += "File saved as " + file.getAbsolutePath();

	    			} catch (Exception e) {
	    				throw new UploadActionException(e);
	    			}
	    		}
	    	}
	    
	    	/// Remove files from session because we have a copy of them
	    	removeSessionFileItems(request);
	    
	    	/// Send your customized message to the client.
	    	return response;
	  }
}
