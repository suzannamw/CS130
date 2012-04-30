package edu.ucla.loni.client;

import edu.ucla.loni.shared.FileTree;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;

import com.smartgwt.client.widgets.events.ClickEvent;  
import com.smartgwt.client.widgets.events.ClickHandler;

import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ServerLibraryManager implements EntryPoint {
	//  Remote Server Proxy to talk to the server-side code
	private FileServiceAsync fileServer = GWT.create(FileService.class);
	
	// Preferences Variables
	private String rootFileDirectoryDefault = "C:\\Users\\charlie\\Desktop\\PipelineRoot1";
	private String rootFileDirectory = rootFileDirectoryDefault;
	
	// Parts of the page that will need to be updated
	private final Tree packageTree = new Tree();
	private final Tree moduleTree = new Tree();
	private final Tree resultsTree = new Tree();
	private final VLayout workarea = new VLayout();
	
	// Other variables we need
	// When viewing a pipefile, store it as an XML document, used if they click "edit"
	// When viewing groups, store them as a Group[], used if they click edit on one of them

	/**
	 * Entry point method (basically main function)
	 */
	public void onModuleLoad() {
		/**
		 *  Header
		 */
		Button importButton = new Button("Import");
		importButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				importForm();
			}
		});
		Button groupsButton = new Button("Manage Groups");
		groupsButton.addClickHandler( new ClickHandler() {
			public void onClick(ClickEvent event) {
		    	groupViewSummary();
		    }
		});
		Button prefsButton = new Button("Preferences");
		prefsButton.addClickHandler(new ClickHandler() {
		    public void onClick(ClickEvent event) {
		    	preferencesForm();
		    }
		});
		
		HLayout buttonRow = new HLayout();
		buttonRow.addMember(importButton);
		buttonRow.addMember(groupsButton);
		buttonRow.addMember(prefsButton);
		
		VLayout header = new VLayout();
		header.setHeight(100);
		header.addMember(new HTML ("<h1>Loni Pipeline Server Library Manager</h1>"));
		header.addMember(buttonRow);
		
		/**
		 *  Workarea
		 */
		workarea.setShowEdges(true);
		workarea.setWidth100();
		workarea.setHeight100();
	    
		workarea.addMember(new Label("Basic Instructions"));
		
		/**
		 *  Tree
		 */
	    // Package Tree
		TreeGrid packageTreeGrid = new TreeGrid();
	    packageTreeGrid.setData(packageTree);
	    packageTreeGrid.setShowConnectors(true);
	    
	    Tab packageTreeTab = new Tab("By Package");
	    packageTreeTab.setPane(packageTreeGrid);
	    
	    // Module Tree
	    TreeGrid moduleTreeGrid = new TreeGrid();
	    moduleTreeGrid.setData(moduleTree);
	    
	    Tab moduleTreeTab = new Tab("By Module Type");
	    moduleTreeTab.setPane(moduleTreeGrid);
	    
	    // Search
	    TreeGrid resultsTreeGrid = new TreeGrid();
	    resultsTreeGrid.setData(resultsTree);
	    
	    VLayout search = new VLayout(10);
	    DynamicForm searchForm = new DynamicForm();
	    TextItem query = new TextItem();
	    query.setTitle("Search");
	    searchForm.setFields(new FormItem[] {query});
	    search.addMember(searchForm);
	    search.addMember(resultsTreeGrid);
	    
	    Tab searchTab = new Tab("Search");
	    searchTab.setPane(search);
	    
	    // Tabs
	    TabSet treeTabs = new TabSet();
	    treeTabs.addTab(packageTreeTab);  
	    treeTabs.addTab(moduleTreeTab);
	    treeTabs.addTab(searchTab);
		
	    VLayout tabs = new VLayout();
	    tabs.setShowEdges(true);
	    tabs.setCanDragResize(true);  
	    tabs.setResizeFrom("L", "R"); 
	    tabs.setWidth(300);
	    tabs.setMinWidth(300);
	    tabs.setMaxWidth(600);
	    
	    tabs.addMember(treeTabs); 
	    
	    /**
		 *  Overall Layout
		 */
		HLayout main = new HLayout(10);
		main.setWidth100();
		
		main.addMember(tabs);
		main.addMember(workarea);
		
	    VLayout layout = new VLayout(10);
	    layout.setHeight100();
	    layout.setWidth100();
	    layout.addMember(header);
	    layout.addMember(main);
	    layout.draw();

	    // Refresh the Trees
	    treeRefresh();
	}
	
	/**
	 *  Tree Operations
	 */
	private void treeRefresh(){
	    // Update packageTree
		fileServer.getPackageTree(rootFileDirectory, new AsyncCallback<FileTree>() {
		      public void onFailure(Throwable caught) {
		    	  packageTree.add( new TreeNode("Error fetching " + rootFileDirectory), packageTree.getRoot());
		      }

		      public void onSuccess(FileTree result) {
		    	  for(FileTree folder: result.children){
		    		  treeParse(folder, packageTree.getRoot(), packageTree);
		    	  }
		      }
		 });
	}
	
	private void treeResults(String query){
		// Update resultsTree
	}
	
	// Adds Filetree "file" to Tree "t" under TreeNode "parent"
	private void treeParse(FileTree file, TreeNode parent, Tree t){
		TreeNode branch = new TreeNode(file.name);
		t.add(branch, parent);
		
		if (file.folder){
			for(FileTree child : file.children){
				treeParse(child, branch, t);
			}
		}
	}
	
	/**
	 *  File Operations
	 */
	private void fileView(String file){
		// Call getFile, store result in private variable
		// Parse XML
		// Display in workarea
	}
	
	private void fileEdit(){
		// Update workarea
	}
	
	/**
	 *  Group Operations
	 */
	private void groupViewSummary(){
		// Update workarea
	}
	
	private void groupEdit(String group){
		// Update workarea
	}
	
	/**
	 *  Other Operations
	 */
	private void importForm(){
		// Allow user to select files/folders
		//   Checkbox for recursive
		// Allow user to supply a url
		// Import button
		//   On click, import the files, go back to basic instructions
	}
	
	private void preferencesForm(){
		// Textbox to specify root directory 
		// Update button
		//   On click, update the root directory, go back to basic instructions
	}
}
