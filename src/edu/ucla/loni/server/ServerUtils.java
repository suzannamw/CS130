package edu.ucla.loni.server;

import edu.ucla.loni.shared.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

public class ServerUtils {
	/**
	* essentially a duplicate of function below (i.e. extractFileName) with exception that 
	* this function extracts name from string that could have window's or unix separators.
	* It became esuful for getting name of urls
	*/
	public static String extractNameFromURL(String s){
		String res = "";
		for( int i = s.length() - 1; i >= 0; i-- ){
			if( (s.charAt(i) == File.separatorChar) || (s.charAt(i) == '/') ) {
				res = s.substring(i + 1, s.length());
				break;
			}
		}
		return res;
	}
	
	/**  
	 * given an absolute path string, this function attempts to isolate actual name of file
	 * @param s - absolute path of file
	 */
	public static String extractFileName(String s){
		String res = "";
		for( int i = s.length() - 1; i >= 0; i-- ){
			if( s.charAt(i) == File.separatorChar ) {
				res = s.substring(i + 1, s.length());
				break;
			}
		}
		return res;
	}
	
	/**
	 *  given an absolute path string, this function isolates the directory absolute address where
	 *  current file is placed
	 *  @param s - absolute path of file
	 */
	public static String extractDirName(String s){
		String res = "";
		for( int i = s.length() - 1; i >= 0; i-- ){
			if( s.charAt(i) == File.separatorChar ) {
				res = s.substring(0, i);
				break;
			}
		}
		return res;
	}
	
	public static String extractRootDir(String abspath) {
		// Absolute Path = root / package / type / filename
		return extractDirName(extractDirName(extractDirName(abspath)));
	}
	
	/**
	 * Returns an unused absolute path for a file
	 */
	public static String newAbsolutePath(String root, String packageName, String type, String name){		
		String newAbsolutePath = root +
			File.separatorChar + packageName.replace(" " , "_") +
			File.separatorChar + type.replace(" " , "_") +
			File.separatorChar + name.replace(" " , "_") + ".pipe";
		
		File file = new File(newAbsolutePath);
		
		if (!file.exists()){
			return newAbsolutePath;
		}
		else {
			/*
			 * The code above will try:
			 * <root>/<pkg>/<mod>/my.pipe
			 * <root>/<pkg>/<mod>/my_(2).pipe
			 * <root>/<pkg>/<mod>/my_(3).pipe
			 * ...
			 */
			
			String testPathPart = newAbsolutePath.substring(0, newAbsolutePath.lastIndexOf(".pipe"));
			String ext = ".pipe";
			
			for (int i = 2 ;; i++){
				newAbsolutePath = testPathPart + "_(" + i + ")" + ext;
				file = new File(newAbsolutePath);
				
				if (!file.exists()){
					return newAbsolutePath;
				}
			}			
		}
	}
	
	/**
	 * Parse an XML file into a Document
	 */
	public static Document readXML(File pipe) throws Exception{
		SAXBuilder builder = new SAXBuilder();
		Document doc = (Document) builder.build(pipe);
		
		return doc;
	}
	
	/**
	 * Parse an XML file into a Document
	 */
	public static Document readXML(InputStream stream) throws Exception{
		SAXBuilder builder = new SAXBuilder();
		Document doc = (Document) builder.build(stream);
		
		return doc;
	}
	
	/**
	 * Write a document (XML file) to a particular absolute path
	 * @throws Exception 
	 */
	public static void writeXML(File file, Document doc) throws Exception{
		XMLOutputter xmlOut = new XMLOutputter();
		FileOutputStream fileOut = new FileOutputStream(file);
		
		xmlOut.output(doc, fileOut);
		
		fileOut.flush();
		fileOut.close();
	}
	
	/** 
	 * Get the text value of children joined by separator
	 */
	private static String getChildrenText(Element element, String childName, String separator){
		List<Element> children = element.getChildren(childName);
		int length = children.size();
		
		String ret = "";
		for (int i = 0; i < length; i++){
			Element child = children.get(i);
			
			String text = child.getText();
			if (text != ""){
				ret += text + separator;
			}
		}
		
		// Remove the last separator
		if (ret != ""){
			ret = ret.substring(0, ret.length() - separator.length());
		}
		
		return ret;
	}
	
	/** 
	 * Get the main element (who has the attributes and children we care about)
	 * from a document
	 */
	private static Element getMainElement(Document doc) throws Exception{
		Element pipeline = doc.getRootElement();
		
		Element moduleGroup = pipeline.getChild("moduleGroup");
		if (moduleGroup == null){
			throw new Exception("Pipefile does not have moduleGroup");
		}
		else {
			List<Element> data = moduleGroup.getChildren("dataModule");
			List<Element> module = moduleGroup.getChildren("module");
			
			if (data.size() == 1 && module.size() == 0){
				return data.get(0);
			} else if (module.size() == 1 && data.size() == 0) {
				return module.get(0);
			} else {
				return moduleGroup;
			}
		}
	}
	
	/**
	 * Parses a .pipe into a Pipefile
	 */
	public static Pipefile parse(Document doc){
		try {
			Pipefile pipe = new Pipefile();
			Element main = getMainElement(doc);
			
			String mainName = main.getName();
			if (mainName.equals("dataModule")){
				pipe.type = "Data";
			} else if (mainName.equals("module")){
				pipe.type = "Modules";
			} else if (mainName.equals("moduleGroup")){
				pipe.type = "Groups";
			} else {
				throw new Exception("Pipefile has unknown type");
			}
			
			// General Properties
			pipe.name = main.getAttributeValue("name", "");
			pipe.packageName = main.getAttributeValue("package", "");
			pipe.description = main.getAttributeValue("description", "");
			pipe.tags = getChildrenText(main, "tag", ",");
			
			// Get type specific properties			
			if (pipe.type.equals("Data")){
				Element values = main.getChild("values");
				if (values != null){
					pipe.values = getChildrenText(values, "value", "\n");
				}
				
				Element output = main.getChild("output");
				if (output != null){
					Element format = output.getChild("format");
					if (format != null){
						pipe.formatType = format.getAttributeValue("type");
					}
				}
			}
			
			if (pipe.type.equals("Modules")){
				pipe.location = main.getAttributeValue("location", "");
			} 
			
			if (pipe.type.equals("Modules") || pipe.type.equals("Groups")){
				pipe.uri = main.getChildText("uri");
			}
			
			return pipe;
		} catch (Exception e){
			return null;
		}
	}
	
	public static Pipefile parse(File file){
		try {
			Document doc = readXML(file);
			Pipefile pipe = parse(doc);
			
			if (pipe != null){
				pipe.absolutePath = file.getAbsolutePath();
			}
			return pipe;
		} catch (Exception e){
			return null;
		}
	}
	
	/**
	 * Updates a Document (XML file) with all the attributes from a Pipefile
	 * @throws Exception 
	 */
	public static Document update(Document doc, Pipefile pipe, boolean packageOnly) throws Exception{
		Element main = getMainElement(doc);
		
		main.setAttribute("package", pipe.packageName);
		
		if (!packageOnly){
			// Update name (attribute)
			main.setAttribute("name", pipe.name);
			// Update description (attribute)
			main.setAttribute("description", pipe.description);
			
			// Update the tags (children)
			main.removeChildren("tag"); // Remove all old tags
			String tags = pipe.tags;
			if (tags != null && tags.length() > 0){
				String[] tagArray = tags.split(",");
				for (String tag : tagArray){
					Element child = new Element("tag");
					child.setText(tag);
					main.addContent(child);
				}
			}
		
			if (pipe.type.equals("Data")){
				// Update values (values child => children)
				Element valuesElement = main.getChild("values");
				if (valuesElement == null){
					valuesElement = new Element("values");
					main.addContent(valuesElement);
				}
				
				valuesElement.removeChildren("value"); // Remove all old values
				
				String values = pipe.values;
				if (values != null && values.length() > 0){
					String[] valueArray = values.split("\n");
					for (String value : valueArray){
						Element valueElement = new Element("value");
						valueElement.setText(value);
						valuesElement.addContent(valueElement);
					}
				}
				
				// Update formatType (output child => format child => attribute)
				Element output = main.getChild("output");
				if (output == null){
					output = new Element("output");
					main.addContent(output);
				}
				
				Element format = output.getChild("format");
				if (format == null){
					format = new Element("format");
					main.addContent(format);
				}
				
				format.setAttribute("type", pipe.formatType);
			}
			
			
			if (pipe.type.equals("Modules")){
				// Update location (attribute)
				main.setAttribute("location", pipe.location);
			}
			
			
			if (pipe.type.equals("Modules") || pipe.type.equals("Groups")){
				// Update uri (child)
				Element uri = main.getChild("uri");
				
				// If child not present, create
				if (uri == null){
					uri = new Element("uri");
					main.addContent(uri);
				}
				
				uri.setText(pipe.uri);
			}
		}
		
		return doc;
	}
	
	/**
	 * Writes the access file for the root directory
	 * @throws Exception 
	 */
	public static void writeAccessFile(Directory root) throws Exception{
		String accessFilePath = root.absolutePath + "/.access.xml";
		File f = new File(accessFilePath);
		
		// If the path doesn't exist return
		if (!f.exists()){
			boolean success = f.createNewFile();
			if (!success){
				throw new Exception ("Could not create access file");
			}
		}
		
		// Create blank document
		Document doc = new Document();
		
		// Add root
		Element access = new Element("access");
		doc.addContent(access);
		
		// Add files child to root
		Element filesRoot = new Element("files");
		access.addContent(filesRoot);
		
		// For each pipe, add a file child to files
		Pipefile[] pipes = Database.selectPipefiles(root.dirId);
		
		if (pipes != null){
			for(Pipefile p : pipes) {
				// OnlY add the pipefile if access is not empty
				if (!p.access.equals("")){
					// File Element
					Element file = new Element("file");
					
					// Set attributes
					file.setAttribute("type", p.type); // for now
					file.setAttribute("name", p.name);
					file.setAttribute("package", p.packageName);
					
					// Add agents
					String[] agents = p.access.split(",");
					for (String agent : agents) {
						file.addContent(agentElement(agent));
					}
					
					// Add to file element to files
					filesRoot.addContent(file);
				}
			}
		}
		
		// Add groups child to root
		Element groupsRoot = new Element("groups");
		access.addContent(groupsRoot);
		
		// For each group, add a group child to groups
		Group[] groups = Database.selectGroups(root.dirId);
		
		if (groups != null){
			for(Group g : groups) {
				// Group Element
				Element group = new Element("group");
				
				// Set attribute
				group.setAttribute("name", g.name); // for now
				
				// Add agents
				String[] agents = g.users.split(",");
				for (String agent : agents) {
					group.addContent(agentElement(agent));
				}
				
				// Add group element to groups
				groupsRoot.addContent(group);
			}
		}
		
		// Write document
		writeXML(f, doc);
		
		// Update when the access file was written
		root.accessModified = new Timestamp( f.lastModified() );
		Database.updateDirectory(root);
	}
	
	/**
	 *  Creates an agent Element
	 */
	public static Element agentElement(String agent){
		agent = agent.trim();
		
		String value, group;
		
		if (GroupSyntax.isGroup(agent)){
			value = GroupSyntax.agentToGroup(agent);
			group = "true";
		} else {
			value = agent;
			group = "false";
		}
		
		return new Element("agent").addContent(value).setAttribute("group", group);
	}
	
	/**
	 * Remove empty directory
	 * @param dir, parent folder that held the pipefile that was removed / moved
	 */
	public static void removeEmptyDirectory(File dir){
		// Only try two levels
		// Level == 2, dir == type
		// Level == 1, dir == package
		// Level == 0, dir == root (stop here)
		
		removeEmptyDirectoryRecursive(dir, 2);
	}
	
	/**
	 * Remove directory if it is empty, calls recursively on parent
	 */
	private static void removeEmptyDirectoryRecursive(File dir, int levels){
		// Don't go any higher
		if (levels == 0){
			return;
		} 
		
		// If empty, delete, call on parent
		if(dir.listFiles().length == 0) {
			dir.delete();
			removeEmptyDirectoryRecursive(dir.getParentFile(), levels - 1);
		}
	}
}
