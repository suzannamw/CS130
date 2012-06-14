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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;

import java.util.Date;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

public class ServerUtils {
	//************************************************************
	// File Operations
	//************************************************************
	
	/**
	 * Returns an unused absolute path built by combining the parameters and checking for conflicts
	 * <p> Possible return values:
	 * <br> root/packageName/type/name.pipe
	 * <br> root/packageName/type/name_(2).pipe 
	 * <br> root/packageName/type/name_(3).pipe
	 * <br> etc
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
	 * Removes directory if it empty and its parent folder if that becomes empty
	 * @param dir a file that is a directory
	 */
	public static void removeEmptyDirectory(File dir){
		if(dir.listFiles().length == 0) {
			dir.delete();
			
			File parent = dir.getParentFile();
			if (parent.listFiles().length == 0){
				parent.delete();
			}
		}
	}
	
	//************************************************************
	// XML
	//************************************************************
	
	//------------------------------------------------------------
	// Read
	//------------------------------------------------------------
	
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
	
	//------------------------------------------------------------
	// Write
	//------------------------------------------------------------
	
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
	
	//------------------------------------------------------------
	// Parse (getChildrenText and getMainElement are helpers)
	//------------------------------------------------------------
	
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
	public static Pipefile parseXML(Document doc){
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
		} 
		catch (Exception e){
			return null;
		}
	}
	
	public static Pipefile parseXML(File file){
		try {
			Document doc = readXML(file);
			Pipefile pipe = parseXML(doc);
			
			if (pipe != null){
				pipe.absolutePath = file.getAbsolutePath();
			}
			
			return pipe;
		} 
		catch (Exception e){
			return null;
		}
	}
	
	//------------------------------------------------------------
	// Update 
	//------------------------------------------------------------
	
	/**
	 * Updates a Document (XML file) with all the attributes from a Pipefile
	 * @throws Exception 
	 */
	public static Document updateXML(Document doc, Pipefile pipe) throws Exception{
		Element main = getMainElement(doc);
		
		// Update name (attribute)
		main.setAttribute("name", pipe.name);
		// Update package (attribute)
		main.setAttribute("package", pipe.packageName);
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
		
		return doc;
	}
	
	//************************************************************
	// Monitor File
	//************************************************************
	
	private static String monitorRelativePath = ".monitorfile";
	
	//------------------------------------------------------------
	// Get 
	//------------------------------------------------------------
	
	private static File getMonitorFile(Directory dir){
		return new File(dir.absolutePath + File.separator + monitorRelativePath);
	}
	
	public static Timestamp getMonitorFileModified(Directory dir){
		Timestamp ret = null;
		
		File monitorFile = ServerUtils.getMonitorFile(dir);
		if (monitorFile.exists()){
			ret = new Timestamp(monitorFile.lastModified());
		}
		
		return ret;
	}
	
	//------------------------------------------------------------
	// Touch 
	//------------------------------------------------------------
	
	public static void touchMonitorFile(Directory dir) throws Exception{
		// Current time
		Date now = new Date();
		
		File monitor = getMonitorFile(dir);
		monitor.setLastModified( now.getTime() );
		
		// Update the database
		dir.monitorModified = new Timestamp(monitor.lastModified());
		Database.updateDirectory(dir);
	}
	
	//************************************************************
	// Access File
	//************************************************************
	
	private static String accessRelativePath = ".access.xml";
	
	//------------------------------------------------------------
	// Get 
	//------------------------------------------------------------
	
	private static File getAccessFile(Directory dir){
		return new File(dir.absolutePath + File.separator + accessRelativePath);
	}
	
	public static Timestamp getAccessFileModified(Directory dir){
		Timestamp ret = null;
		
		File accessFile = ServerUtils.getAccessFile(dir);
		if (accessFile.exists()){
			ret = new Timestamp(accessFile.lastModified());
		}
		
		return ret;
	}
	
	
	//------------------------------------------------------------
	// Read (readAccessFileGroup is a helper)
	//------------------------------------------------------------
	
	public static String readAccessFileGroup(Element parentEle){
		String rs = "";
		String sep = ",";
		
		for (Element agent: parentEle.getChildren()){
			String agentName = agent.getValue();
			String groupAttr = agent.getAttributeValue("group");
			
			boolean isGroup = groupAttr.equals("true");
			
			if (isGroup){
				agentName = GroupSyntax.groupnameToAgent(agentName);
			}
			
			rs += agentName + sep;	
		}
		
		if (!rs.equals("")){
			// Get rid of final separator 
			rs = rs.substring(0, rs.length() - sep.length());
		}
		
		return rs;
	}
	
	/**
	 * Reads the access file for the root directory
	 * Side Effect: Updates accessFileModified in the database
	 */
	public static void readAccessFile(Directory root) throws Exception {
		File accessFile = ServerUtils.getAccessFile(root);
		if (!accessFile.exists()){
			return;
		}
		
		Document doc = readXML(accessFile);
		Element access = doc.getRootElement();
		Element filesRoot = access.getChild("files");
		Element groupsRoot = access.getChild("groups");
		
		if (filesRoot.getChildren() != null) {
			for (Element pipe: filesRoot.getChildren()){
				String packageName = pipe.getAttributeValue("package");
				String type = pipe.getAttributeValue("type");
				String name = pipe.getAttributeValue("name");
				
				Pipefile thisPipe = Database.selectPipefileByHierarchy(root.dirId, packageName, type, name);
				
				if (thisPipe != null){
					thisPipe.access = readAccessFileGroup(pipe);;
					Database.updatePipefile(thisPipe);
				}
			}
		}
		
		if (groupsRoot.getChildren() != null) {
			for (Element group: groupsRoot.getChildren()) {
				String name = group.getAttributeValue("name");
				String userString = readAccessFileGroup(group);
				
				Group thisGroup = Database.selectGroupByName(root.dirId, name);
				
				if (thisGroup == null){
					thisGroup = new Group();
					thisGroup.name = name;
					thisGroup.users = userString;
					Database.insertGroup(root.dirId, thisGroup);
				} 
				else {
					thisGroup.users = userString;
					Database.updateGroup(thisGroup);
				}
			}
		}
		
		// Update when the access file was written
		root.accessModified = new Timestamp( accessFile.lastModified() );
		Database.updateDirectory(root);
	}
	
	//------------------------------------------------------------
	// Write (agentElement is a helper)
	//------------------------------------------------------------
	
	/**
	 *  Creates an agent Element
	 */
	public static Element agentElement(String agent){
		agent = agent.trim();
		
		String value, group;
		
		if (GroupSyntax.isGroup(agent)){
			value = GroupSyntax.agentToGroupname(agent);
			group = "true";
		} else {
			value = agent;
			group = "false";
		}
		
		return new Element("agent").addContent(value).setAttribute("group", group);
	}
	
	/**
	 * Writes the access file for the root directory
	 * Side Effect: Updates accessFileModified in the database
	 */
	public static void writeAccessFile(Directory root) throws Exception{
		File accessFile = getAccessFile(root);
		if (!accessFile.exists()){
			boolean success = accessFile.createNewFile();
			if (!success){
				throw new Exception ("Could not create access file");
			}
		}
				
		// <files>
		Element filesRoot = new Element("files");
		
		// For each pipe, add a <file> child to <files>
		Pipefile[] pipes = Database.selectPipefiles(root.dirId);
		
		if (pipes != null){
			for(Pipefile p : pipes) {
				// Only add the pipefile if access is not empty
				if (!p.access.equals("")){
					Element file = new Element("file");
					file.setAttribute("type", p.type);
					file.setAttribute("name", p.name);
					file.setAttribute("package", p.packageName);
					
					// Add agents
					String[] agents = p.access.split(",");
					for (String agent : agents) {
						if (!agent.equals("")){
							file.addContent(agentElement(agent));
						}
					}
					
					filesRoot.addContent(file);
				}
			}
		}
		
		// <groups>
		Element groupsRoot = new Element("groups");
		
		// For each group, add a <group> child to <groups>
		Group[] groups = Database.selectGroups(root.dirId);
		
		if (groups != null){
			for(Group g : groups) {
				Element group = new Element("group");
				group.setAttribute("name", g.name);
				
				// Add agents
				String[] agents = g.users.split(",");
				for (String agent : agents) {
					if (!agent.equals("")){
						group.addContent(agentElement(agent));
					}
				}
				
				groupsRoot.addContent(group);
			}
		}
		
		// Root Element
		Element access = new Element("access");
		access.addContent(filesRoot);
		access.addContent(groupsRoot);
		
		// Document
		Document doc = new Document();
		doc.addContent(access);
		
		// Write document
		writeXML(accessFile, doc);
		
		// Update when the access file was written
		root.accessModified = new Timestamp( accessFile.lastModified() );
		Database.updateDirectory(root);
	}
}
