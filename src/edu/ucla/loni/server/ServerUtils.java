package edu.ucla.loni.server;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import edu.ucla.loni.shared.Group;
import edu.ucla.loni.shared.Pipefile;

public class ServerUtils {
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
	
	public static String newAbsolutePath(String oldAbsolutePath, String packageName, String type){
		String root = extractRootDir(oldAbsolutePath);	
		String filename = extractFileName(oldAbsolutePath);
		
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
	public static Pipefile parseFile(File file){
		try {
			Pipefile pipe = new Pipefile();
			
			Document doc = readXML(file);
			Element main = getMainElement(doc);
			
			String mainName = main.getName();
			if (mainName == "dataModule"){
				pipe.type = "Data";
			} else if (mainName == "module"){
				pipe.type = "Modules";
			} else if (mainName == "moduleGroup"){
				pipe.type = "Groups";
			} else {
				throw new Exception("Pipefile has unknown type");
			}
			
			// General properties
			pipe.absolutePath = file.getAbsolutePath();
			
			pipe.name = main.getAttributeValue("name", "");
			pipe.packageName = main.getAttributeValue("package", "");
			pipe.description = main.getAttributeValue("description", "");
			pipe.tags = getChildrenText(main, "tag", ",");
			
			// Get type specific properties			
			if (pipe.type == "Data"){
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
			
			if (pipe.type == "Modules"){
				pipe.location = main.getAttributeValue("location", "");
			} 
			
			if (pipe.type == "Modules" || pipe.type == "Groups"){
				pipe.uri = main.getChildText("uri");
			}
			
			return pipe;
		} 
		catch (Exception e){
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
		
			if (pipe.type == "Data"){
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
			
			
			if (pipe.type == "Modules"){
				// Update location (attribute)
				main.setAttribute("location", pipe.location);
			}
			
			
			if (pipe.type == "Modules" || pipe.type == "Groups"){
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
		
		
		// Create a blank document (use syntax similar as found in readXML)
		// Initialize with root element "access" with children "files" and "groups"
		// See Access Restrictions XML document, option 4
		
		// Query database, get all pipefiles for the directory where access is not blank
		// For each file
		//   Create element in document
		
		// Query database, get all groups
		// For each group
		//   Create element in document
		
		// Write XML document with writeXML function

		String accessFilePath = rootDirectory + "/.access.xml";
		File f = new File(accessFilePath);

		Element rootEle = new Element("access");
		Document doc = new Document(rootEle);
		
		Element filesRoot = new Element("files");
		Element groupsRoot = new Element("groups");
		
		Group[] groups = Database.selectGroups();
		Pipefile[] pipes = Database.selectPipefiles(Database.selectDirectory(rootDirectory));
		for(Pipefile p : pipes) {
			Element file = new Element("file");
			file.setAttribute("type", p.type); // for now
			file.setAttribute("name", p.name);
			file.setAttribute("package", p.packageName);
			String[] agents = p.access.split(",");
			for (String agent : agents) {
				if (isGroup(agent)) {
					//group
					//do we need to strip []?
					file.addContent(new Element("agent").addContent(trimAgent(agent)).setAttribute("group", "true"));
				} else {
					//not a group
					file.addContent(new Element("agent").addContent(trimAgent(agent)).setAttribute("group", "false"));
			
				}
			}
			filesRoot.addContent(file);
			
		}
		
		for(Group g : groups) {
			Element group = new Element("group");
			group.setAttribute("name", g.name); // for now
			String[] agents = g.users.split(",");
			for (String agent : agents) {
				if (isGroup(agent)) {
					//group
					group.addContent(new Element("agent").addContent(trimAgent(agent)).setAttribute("group", "true"));
				} else {
					//not a group
					
					group.addContent(new Element("agent").addContent(trimAgent(agent)).setAttribute("group", "false"));
				}
			}
			groupsRoot.addContent(group);
		}
		doc.addContent(filesRoot);
		doc.addContent(groupsRoot);
		writeXML(f, doc);
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
	
	/*
	 * input: a string that might be a user or a group
	*/
	public static boolean isGroup(String agent)
	{
		return agent.startsWith("[") && agent.endsWith("]");
	}
	
	/*
	 * given an agent, remove space and []
	 */
	public static String trimAgent(String agent)
	{
		return agent.replace("[", "").replace("]", "").trim();
	}
}
