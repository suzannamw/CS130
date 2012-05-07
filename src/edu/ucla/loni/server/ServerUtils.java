package edu.ucla.loni.server;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucla.loni.shared.Pipefile;

public class ServerUtils {
	/**
	 * Parse an XML file into a Document
	 */
	public static Document parseXML(File pipe) throws Exception{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(pipe);
		
		return doc;
	}
	
	/** 
	 * Get the text value of an element
	 */
	private static String getElementValue(Element textElement){
		Node textNode = textElement.getFirstChild();
		if (textNode != null){
			String value = textNode.getNodeValue();
			if (value != null){
				return value;
			}
		}
		
		return "";
	}
	
	/** 
	 * Get a comma separated list of the textual value of children within an element
	 * <p>
	 * Example:<br>
	 * {@literal<e><child>a</child><child>b</child></e>}<br>
	 * returns "a, b"
	 */
	private static String getChildValues(Element e, String child){
		NodeList children = e.getChildNodes();
		int length = children.getLength();
		
		String ret = "";
		for (int i = 0; i < length; i++){
			Node childNode = children.item(i);
			
			if (childNode.getNodeType() == Node.ELEMENT_NODE){
				Element childElement = (Element) childNode;
				
				if (child.equals(childElement.getNodeName())){
					String value = getElementValue(childElement);
					
					if (value != ""){
						ret += value + ", ";
					}
				}
			}
		}
		
		// Remove the last comma and space
		if (ret != ""){
			ret = ret.substring(0, ret.length() - 2);
		}
		
		return ret;
	}
	
	/** 
	 * Get a textual value of a child within an element
	 */
	private static String getChildValue(Element e, String child){
		NodeList children = e.getChildNodes();
		int length = children.getLength();
		
		for (int i = 0; i < length; i++){
			Node childNode = children.item(i);
			
			if (childNode.getNodeType() == Node.ELEMENT_NODE){
				Element childElement = (Element) childNode;
				
				if (child.equals(childElement.getNodeName())){
					return getElementValue(childElement);
				}
			}
		}
		
		return "";
	}
	
	/**
	 * Parses a .pipe into a Pipefile
	 */
	public static Pipefile parseFile(File file){
		try {
			Pipefile pipe = new Pipefile();
			
			Document doc = parseXML(file);
			
			NodeList group = doc.getElementsByTagName("moduleGroup");
			NodeList data = doc.getElementsByTagName("dataModule");
			NodeList modules = doc.getElementsByTagName("module");
			
			Node mainNode; // Node which holds the attributes we care out
			
			if (group.getLength() >= 1 && data.getLength() + modules.getLength() > 2){
				mainNode = group.item(0);
				pipe.type = "Workflows";
			} else if (data.getLength() == 1){
				mainNode = data.item(0);
				pipe.type = "Data";
			} else if (modules.getLength() == 1){
				mainNode = modules.item(0);
				pipe.type = "Modules";
			} else {
				return null;
			}
	    
			// Convert to Element
			Element mainElement = (Element) mainNode;
			
			// General properties
			pipe.absolutePath = file.getAbsolutePath();
			
			pipe.name = mainElement.getAttribute("name");
			pipe.packageName = mainElement.getAttribute("package");
			pipe.description = mainElement.getAttribute("description");
			pipe.tags = getChildValues(mainElement, "tag");
			
			// Get type specific properties			
			if (pipe.type == "Data"){
				// TODO, 
				// By the schema dataModule does not have output / input elements
				// Also need to know what we are getting out
			}
			
			if (pipe.type == "Modules"){
				pipe.location = mainElement.getAttribute("location");
			} 
			
			if (pipe.type == "Modules" || pipe.type == "Workflows"){
				pipe.uri = getChildValue(mainElement, "uri");
			}
			
			return pipe;
		} 
		catch (Exception e){
			return null;
		}
	}
	
	/**
	 * Updates a Document (XML file) with all the attributes from a Pipefile
	 */
	public static Document update(Document doc, Pipefile pipe){
		// TODO
		// Update the name
		// Update the packageName
		// Update the description
		// Update the tags
		return null;
	}
	
	/**
	 * Write a document (XML file) to a particular absolute path
	 */
	public static void write(String absolutePath, Document doc){
		//TODO
		return;
	}
}
