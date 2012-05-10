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
	public static String extractFileName(String s)
	{
		String res = "";
		for( int i = s.length() - 1; i >= 0; i-- )
		{
			if( s.charAt(i) == File.separatorChar ) //http://docs.oracle.com/javase/1.4.2/docs/api/java/io/File.html#separatorChar
				//File.pathSeparatorChar = ';' and File.separatorChar = '/' OR '\' depending on OS
			{
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
	public static String extractDirName(String s)
	{
		String res = "";
		for( int i = s.length() - 1; i >= 0; i-- )
		{
			if( s.charAt(i) == File.separatorChar ) //http://docs.oracle.com/javase/1.4.2/docs/api/java/io/File.html#separatorChar
				//File.pathSeparatorChar = ';' and File.separatorChar = '/' OR '\' depending on OS
			{
				res = s.substring(0, i);
				break;
			}
		}
		return res;
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
	 * Get the text value of children separated by a common and space
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
				pipe.type = "Workflows";
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
			
			if (pipe.type == "Modules" || pipe.type == "Workflows"){
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
			while (tags != ""){
				String tag = "";
				
				// Find a comma, Set tag to substring, set tags to remainder			
				int commaIndex = tags.indexOf(",");
				if (commaIndex == -1){
					tag = tags;
					tags = "";
				} else {
					tag = tags.substring(0, commaIndex);
					tags = tags.substring(commaIndex);
				}
				
				// Create and add child				
				Element child = new Element("uri");
				child.setText(tag);
				main.addContent(child);
			}
		
			// Update input/output (children)
			if (pipe.type == "Data"){	
				// TODO
			}
			
			// Update location (attribute)
			if (pipe.type == "Modules"){
				main.setAttribute("location", pipe.location);
			}
			
			// Update uri (child)
			if (pipe.type == "Modules" || pipe.type == "Workflows"){
				Element child = main.getChild("uri");
				
				// If child not present, create
				if (child == null){
					child = new Element("uri");
					main.addContent(child);
				}
				
				child.setText(pipe.uri);
			}
		}
		
		return doc;
	}
}
