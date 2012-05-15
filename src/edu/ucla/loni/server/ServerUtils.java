package edu.ucla.loni.server;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

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
	 */
	public static void writeAccessFile(String rootDirectory){
		// TODO
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
		
		// Code copied over from Michael for writing groups.xml

//		//update groups.xml
//		File f = new File("groups.xml");
//		if (!f.exists()) {
//			byte[] buf = "<groups></groups>".getBytes();
//			OutputStream out = new FileOutputStream(f);
//			out.write(buf);
//		}
//		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//		Document doc = dBuilder.parse(f);
//		
//		boolean groupExist = false;
//		NodeList groups = doc.getElementsByTagName("group");
//		for (int i = 0; i < groups.getLength(); i++) {
//			Node g = groups.item(i);
//			NamedNodeMap attributes = g.getAttributes();
//			int groupId = Integer.parseInt(attributes.getNamedItem("groupId").getNodeValue());
//			if (groupId == group.groupId) {
//				groupExist = true;
//				attributes.getNamedItem("name").setTextContent(group.name);
//				attributes.getNamedItem("numUsers").setTextContent(Integer.toString(group.numUsers));
//				//remove previous users
//				NodeList users = g.getChildNodes();
//				int users_len = users.getLength();
//				for (int j = 0; j < users_len; j++) 
//					g.removeChild(users.item(0));
//				
//				//add users
//				String users_string = group.users;
//				while (users_string.contains(", ")) {
//					Element e = doc.createElement("user");
//					e.setTextContent(users_string.substring(0, users_string.indexOf(", ")));
//					users_string = users_string.substring(users_string.indexOf(", ") + 2);
//					g.appendChild(e);
//					
//				}
//				Element e = doc.createElement("user");
//				e.setTextContent(users_string);
//				g.appendChild(e);
//				
//				writeXmlFile(doc, "groups.xml");
//			}
//		}
//		if (groupExist) return;
//		//create new group
//		Element g = doc.createElement("group");
//		g.setAttribute("name", group.name);
//		g.setAttribute("groupId", Integer.toString(group.groupId));
//		g.setAttribute("numUsers", Integer.toString(group.numUsers));
//		String users_string = group.users;
//		while (users_string.contains(", ")) {
//			Element e = doc.createElement("user");
//			e.setTextContent(users_string.substring(0, users_string.indexOf(", ")));
//			users_string = users_string.substring(users_string.indexOf(", ") + 2);
//			g.appendChild(e);
//		}
//		Element e = doc.createElement("user");
//		e.setTextContent(users_string);
//		g.appendChild(e);
//		doc.getFirstChild().appendChild(g);
//		writeXmlFile(doc, "groups.xml");
		
		
//		File f = new File("groups.xml");
//		//groups file does not exist
//		if (!f.exists()) 
//			return;
//		//change groups.xml
//		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//		Document doc = dBuilder.parse(f);
//		
//		Node e = doc.getFirstChild();
//		NodeList filegroups = doc.getElementsByTagName("group");
//		int numgroups = filegroups.getLength();
//		int[] removeGroupNums = new int[groups.length];
//		int index = 0;
//		for (int j = 0; j < groups.length; j++) {
//			removeGroupNums[j] = -1;
//		}
//
//		for (int i = 0; i < numgroups; i++) {
//			Node g = filegroups.item(i);
//			NamedNodeMap attributes = g.getAttributes();
//			int groupId = Integer.parseInt(attributes.getNamedItem("groupId").getNodeValue());
//			for (int j = 0; j < groups.length; j++) {
//				if (groupId == groups[j].groupId) {
//					removeGroupNums[index] = i;
//					index++;
//				}
//			}
//
//		}
//		for (int i = 0; i < groups.length; i++) {
//			if (removeGroupNums[i] == -1) break;
//			e.removeChild(filegroups.item(removeGroupNums[i]-i));
//			System.out.println(removeGroupNums[i]);
//		}
//		writeXmlFile(doc, "groups.xml");
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
}
