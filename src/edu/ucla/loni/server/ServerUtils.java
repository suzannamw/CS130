package edu.ucla.loni.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import edu.ucla.loni.shared.*;

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
	
	public static String newAbsolutePath(String root, String packageName, String type, String filename){		
		String newAbsolutePath = root +
			File.separatorChar + packageName.replace(" " , "_") +
			File.separatorChar + type +
			File.separatorChar + filename;
		
		return newAbsolutePath;
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
	public static void writeAccessFile(String rootDirectory) throws Exception{	
		Directory dir = Database.selectDirectory(rootDirectory);
		String accessFilePath = rootDirectory + "/.access.xml";
		File f = new File(accessFilePath);
		
		// If the path doesn't exist return
		if (!f.exists()){
			return;
		}
		
		// Create blank document
		Document doc = new Document();
		
		// Add root
		Element root = new Element("access");
		doc.addContent(root);
		
		// Add files child to root
		Element filesRoot = new Element("files");
		root.addContent(filesRoot);
		
		// For each pipe, add a file child to files
		Pipefile[] pipes = Database.selectPipefiles(dir.dirId);
		
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
		root.addContent(groupsRoot);
		
		// For each group, add a group child to groups
		Group[] groups = Database.selectGroups(dir.dirId);
		
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
		dir.accessModified = new Timestamp( f.lastModified() );
		Database.updateDirectory(dir);
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
	 * recursively remove dir if it is empty
	 */
	public static void recursiveRemoveDir(File dir)
	{
		if (!dir.isDirectory())
			return;
		
		if(dir.listFiles().length == 0) {
			dir.delete();
			recursiveRemoveDir(dir.getParentFile());
		}
	}
	
	/**
	 * 
	 * @param root
	 * @param oldFilename
	 * @return abspath for newFile, 
	 * null if it fails, and the new file should not be added to the system
	 */
	
	public static String newFilename(String root, String oldFilename)
	{
		File f = new File(oldFilename);
		if (!f.exists() || !f.canRead())
			return null;
		Pipefile p = parse(f);
		if (p == null)
			return null; // A junk file, cannot be parse
		String testPath = newAbsolutePath(root, p.packageName, p.type, f.getName());
		if (testPath.substring(testPath.lastIndexOf(".")) != ".pipe")
			return null; // The file can be parse, but it has wrong extension

		File readyFile = new File(testPath);
		if (!readyFile.exists())
			return testPath; // <root>/<pkg>/<mod>/my.pipe
		else
		{
			// <root>/<pkg>/<mod>/my = testPart
			String testPathPart = testPath.substring(0, testPath.lastIndexOf("."));
			String ext = ".pipe";
			
			for (int i = 2 ;; i++) // trying <root>/<pkg>/<mod>/my_<lucky number>.pipe
			{
				testPath = testPathPart + "_(" + i + ")" + ext;
				readyFile = new File(testPath);
				if (!readyFile.exists())
					return testPath;
			}
			
			/*
			 * The code above will try:
			 * <root>/<pkg>/<mod>/my.pipe
			 * <root>/<pkg>/<mod>/my_(2).pipe
			 * <root>/<pkg>/<mod>/my_(3).pipe
			 * ...
			 */
			
		}
	}
}
