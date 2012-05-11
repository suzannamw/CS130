package edu.ucla.loni.client;

import edu.ucla.loni.shared.*;

import java.util.LinkedHashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.KeyNames;

import com.smartgwt.client.widgets.events.ClickEvent;  
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeClickHandler;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ServerLibraryManager implements EntryPoint {
	////////////////////////////////////////////////////////////
	// Private Variables
	////////////////////////////////////////////////////////////
	
	/**
	 *   Remote Server Proxy to talk to the server-side code
	 */
	private FileServiceAsync fileServer = GWT.create(FileService.class);
	
	/**
	 *   Default Root Directory
	 */
	private String rootDirectoryDefault = "C:\\Users\\charlie\\Desktop\\CraniumLibrary";
	
	/**
	 *   Current Root Directory
	 */
	private String rootDirectory = rootDirectoryDefault;
	
	/**
	 *   Workarea
	 *   <p>
	 *   Updated by a lot of functions
	 */
	private final VLayout workarea = new VLayout();
	
	/**
	 *   Package Tree
	 *   <p>
	 *   Set in: treeRefresh
	 *   <br>
	 *   Used in: onModuleLoad
	 */
	private final Tree packageTree = new Tree();
	
	/**
	 *   Module Tree
	 *   <p>
	 *   Set in: treeRefresh
	 *   <br>
	 *   Used in: onModuleLoad
	 */
	private final Tree moduleTree = new Tree();
	
	/**
	 *   Results Tree
	 *   <p>
	 *   Set in: treeResults
	 *   <br>
	 *   Used in: onModuleLoad
	 */
	private final Tree resultsTree = new Tree();

	/**
	 *  String abosolutePath => Pipefile pipe
	 *  <p>
	 *  Set in: treeRefresh
	 *  <br>
	 *  Used in: viewFile, editFile
	 */
	private final LinkedHashMap<String, Pipefile> pipes = new LinkedHashMap<String, Pipefile>();
	
	/**
	 *   Set in: treeRefresh 
	 *   <br>
	 *   Used in: fileOperations
	 */
	private String[] packages;
	
	/**
	 *   String groupName => Group g
	 *   <p>
	 *   Set in viewGroups
	 *   <br>
	 *   Used in editGroup
	 */
	private final LinkedHashMap<String, Group> groups = new LinkedHashMap<String, Group>();
	
	/**
	 *   NodeClickHandler for when a pipefile is selected within a tree
	 */
	private NodeClickHandler selectPipefileHandler = new NodeClickHandler() {
		public void onNodeClick(NodeClickEvent event){
			TreeGrid grid = event.getViewer();
			Tree tree = grid.getData();
			TreeNode clicked = event.getNode();
			
			boolean folder = tree.isFolder(clicked);
			
			if (folder){
				// Be sure the folder is open			
				tree.openAll(clicked);
				// Deselect the folder
				grid.deselectRecord(clicked);
				// Select all the leaves
				grid.selectRecords(tree.getDescendantLeaves(clicked));
			}
			
			ListGridRecord[] selected = grid.getSelectedRecords();
			int numSelected = selected.length;
			if (numSelected == 0){
				basicInstructions();
			}
			else if (numSelected == 1){
				String absolutePath = selected[0].getAttribute("absolutePath");
				Pipefile pipe = pipes.get(absolutePath);
				viewFile(pipe);
			}
			else {
				Pipefile[] selectedPipes = new Pipefile[selected.length];
				for (int i = 0; i < selected.length; i++){
					String absolutePath = selected[i].getAttribute("absolutePath");
					selectedPipes[i] = pipes.get(absolutePath);
				}
				fileOperations(selectedPipes);
			}
		}
	};

	////////////////////////////////////////////////////////////
	// On Module Load
	////////////////////////////////////////////////////////////
	
	/**
	 * Entry point method (basically main function)
	 */
	public void onModuleLoad() {		
		// Header -- Title
		Label title = new Label ();
		title.setWidth100();
		title.setAlign(Alignment.CENTER);
		title.setHeight(50);
		title.setContents("Loni Pipeline Server Library Manager");
		title.setStyleName("title");
		
		// Header -- Button Row -- Buttons
		Button importButton = new Button("Import");
		importButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				importForm();
			}
		});
		Button groupsButton = new Button("Manage Groups");
		groupsButton.addClickHandler( new ClickHandler() {
			public void onClick(ClickEvent event) {
		    	viewGroups();
		    }
		});
		
		// Header -- Button Row
		HLayout buttonRow = new HLayout(5);
		buttonRow.addMember(importButton);
		buttonRow.addMember(groupsButton);
		buttonRow.setMargin(5);
		
		// Header
		VLayout header = new VLayout();
		header.setHeight(75);
		header.addMember(title);
		header.addMember(buttonRow);
		header.setStyleName("header");
		
		// Workarea
		workarea.setWidth100();
		workarea.setHeight100();
		workarea.setPadding(10);
	    
		basicInstructions();
	    
		// Left -- TreeTabs -- PackageTreeTab
		TreeNode packageRoot = new TreeNode();
		packageTree.setRoot(packageRoot);
		packageTree.setShowRoot(false);
		
		TreeGrid packageTreeGrid = new TreeGrid();
	    packageTreeGrid.setData(packageTree);
	    packageTreeGrid.setShowConnectors(true);
	    packageTreeGrid.addNodeClickHandler(selectPipefileHandler);
	    packageTreeGrid.setShowRollOver(false);
	    
	    Tab packageTreeTab = new Tab("By Package");
	    packageTreeTab.setPane(packageTreeGrid);   
	    
	    // Left -- TreeTabs -- ModuleTreeTab
	    TreeNode moduleRoot = new TreeNode();
	    moduleTree.setRoot(moduleRoot);
	    moduleTree.setShowRoot(false);
	    
	    TreeGrid moduleTreeGrid = new TreeGrid();
	    moduleTreeGrid.setData(moduleTree);
	    moduleTreeGrid.setShowConnectors(true);
	    moduleTreeGrid.addNodeClickHandler(selectPipefileHandler);
	    
	    Tab moduleTreeTab = new Tab("By Module Type");
	    moduleTreeTab.setPane(moduleTreeGrid);
	    
	    // Left -- TreeTabs -- ResultsTreeTab
	    TreeNode resultsRoot = new TreeNode();
	    resultsTree.setRoot(resultsRoot);
	    resultsTree.setShowRoot(false);
	    
	    TreeGrid resultsTreeGrid = new TreeGrid();
	    resultsTreeGrid.setData(resultsTree);
	    resultsTreeGrid.setShowConnectors(false);
	    resultsTreeGrid.addNodeClickHandler(selectPipefileHandler);
	    
	    final TextItem query = new TextItem();
	    query.setShowTitle(false);
	    query.setWidth(290);
	    query.addChangedHandler(new ChangedHandler(){
	    	public void onChanged(ChangedEvent event){
	    		treeResults(query.getValueAsString());
	    	}
	    });

	    DynamicForm searchForm = new DynamicForm();
	    searchForm.setFields(new FormItem[] {query});
	    searchForm.setWidth100();
	    
	    VLayout search = new VLayout(10);
	    search.addMember(searchForm);
	    search.addMember(resultsTreeGrid);
	    
	    Tab resultsTreeTab = new Tab("Search");
	    resultsTreeTab.setPane(search);
	    
	    // Left -- TreeTabs
	    TabSet treeTabs = new TabSet();
	    treeTabs.addTab(packageTreeTab);  
	    treeTabs.addTab(moduleTreeTab);
	    treeTabs.addTab(resultsTreeTab);
	    
	    VLayout left = new VLayout();
	    left.setShowResizeBar(true);
	    left.setCanDragResize(true);  
	    left.setResizeFrom("L", "R"); 
	    left.setWidth(300);
	    left.setMinWidth(300);
	    left.setMaxWidth(600);
	    left.setAlign(Alignment.CENTER);
	    
	    rootDirectoryView(left);
	    left.addMember(treeTabs); 
	    
	    // Main
		HLayout main = new HLayout();
		main.setWidth100();
		
		main.addMember(left);
		main.addMember(workarea);
		
	    VLayout layout = new VLayout();
	    layout.setHeight100();
	    layout.setWidth100();
	    layout.addMember(header);
	    layout.addMember(main);
	    layout.draw();

	    // Tree Initialization
	    treeRefresh();
	    
	    // Group Initialization
	    fileServer.getGroups(
    		new AsyncCallback<Group[]>() {
		        public void onFailure(Throwable caught) {
		        	error("Failed to retrieve groups: " + caught.getMessage());
		        }

		        public void onSuccess(Group[] result) {
		        	if (result != null){
			        	for(Group g : result){
			        		groups.put(g.name, g);
			        	}
		        	}
		        }
		    }
        );
	}
	
	////////////////////////////////////////////////////////////
	// Private Functions
	////////////////////////////////////////////////////////////
	
	/**
	 *  Updates Package Tree and Module Tree based on the rootDirectory
	 */
	private void treeRefresh(){
		// Clear packageTree and moduleTree
		packageTree.removeList(packageTree.getDescendants());
		moduleTree.removeList(moduleTree.getDescendants());
		
	    // Update Trees
		fileServer.getFiles(
            rootDirectory, 
            new AsyncCallback<Pipefile[]>() {
		        public void onFailure(Throwable caught) {
		        	error("Failed to retrieve files: " + caught.getMessage());
		        }

		        public void onSuccess(Pipefile[] result) {
		        	if (result != null) {
		        		LinkedHashMap<String, TreeNode> packageMap = new LinkedHashMap<String, TreeNode>();
		        		LinkedHashMap<String, TreeNode> packageTypeMap = new LinkedHashMap<String, TreeNode>();
		        	
		        		LinkedHashMap<String, TreeNode> typeMap = new LinkedHashMap<String, TreeNode>();
		        		LinkedHashMap<String, TreeNode> typePackageMap = new LinkedHashMap<String, TreeNode>();
		        	
			        	for (Pipefile p : result){
			        		pipes.put(p.absolutePath, p);
			        		
			        		TreeNode pipe = new TreeNode(p.name);
			        		pipe.setAttribute("absolutePath", p.absolutePath);
			        		
			        		// Package Tree
			        		TreeNode packageTreeGrandParent;
			        		
			        		if (packageMap.containsKey(p.packageName)){
			        			packageTreeGrandParent = packageMap.get(p.packageName);
			        		} else {
			        			packageTreeGrandParent = new TreeNode(p.packageName);
			        			packageTree.add(packageTreeGrandParent, packageTree.getRoot());
			        			packageMap.put(p.packageName, packageTreeGrandParent);
			        		}
			        		
			        		TreeNode packageTreeParent;
			        		String package_type = p.packageName + p.type;
			        		
			        		if (packageTypeMap.containsKey(package_type)){
			        			packageTreeParent = packageTypeMap.get(package_type);
			        		} else {
			        			packageTreeParent = new TreeNode(p.type);
			        			packageTree.add(packageTreeParent, packageTreeGrandParent);
			        			packageTypeMap.put(package_type, packageTreeParent);
			        		}
			        		
			        		packageTree.add(pipe, packageTreeParent);
			        		
			        		// Module Tree
			        		TreeNode moduleTreeGrandParent;
			        		
			        		if (typeMap.containsKey(p.type)){
			        			moduleTreeGrandParent = typeMap.get(p.type);
			        		} else {
			        			moduleTreeGrandParent = new TreeNode(p.type);
			        			moduleTree.add(moduleTreeGrandParent, moduleTree.getRoot());
			        			typeMap.put(p.type, moduleTreeGrandParent);
			        		}
			        		
			        		TreeNode moduleTreeParent;
			        		String type_package = p.type + p.packageName ;
			        		
			        		if (typePackageMap.containsKey(type_package)){
			        			moduleTreeParent = typePackageMap.get(type_package);
			        		} else {
			        			moduleTreeParent = new TreeNode(p.packageName);
			        			moduleTree.add(moduleTreeParent, moduleTreeGrandParent);
			        			typePackageMap.put(type_package, moduleTreeParent);
			        		}
			        		
			        		moduleTree.add(pipe, moduleTreeParent);
			        	}
		        	
			        	packages = new String[packageMap.size()];
			        	packages = packageMap.keySet().toArray(packages);
		        	}
		        }
		    }
        );
	}
	
	/**
	 *  Updates ResultsTree based on what query is returned by the server
	 */
	private void treeResults(final String query){
		if (query != null && query.length() >= 2){
			fileServer.getSearchResults(
	            rootDirectory,
	            query,
	            new AsyncCallback<Pipefile[]>() {
			        public void onFailure(Throwable caught) {
			        	error("Failed to retrieve search results: "+ caught.getMessage());
			        }
	
			        public void onSuccess(Pipefile[] result) {
			        	// Clear the ResultsTree
			        	resultsTree.removeList(resultsTree.getDescendants());
			        	
			        	if (result != null){
			        		for (Pipefile p : result){
			        			if (pipes.containsKey(p.absolutePath) == false){
			        				pipes.put(p.absolutePath, p);
			        			}
				        		
				        		TreeNode pipe = new TreeNode(p.name);
				        		pipe.setAttribute("absolutePath", p.absolutePath);
				        		
				        		resultsTree.add(pipe, resultsTree.getRoot());
			        		}
			        	}
			        }
			    }
	        );
		} else {
			resultsTree.removeList(resultsTree.getDescendants());
		}
	}
	
	private void fileOperationsActions(final Pipefile[] selected){
		// Title
		Label workareaTitle = new Label("File Operations");
		workareaTitle.setHeight(20);
		workareaTitle.setStyleName("workarea-title");
		workarea.addMember(workareaTitle);
		
		// Actions
		final ComboBoxItem combo = new ComboBoxItem();
		combo.setTitle("To Package"); 
		combo.setValueMap(packages);
		
		DynamicForm form = new DynamicForm();
		form.setItems(combo);		
		
		Button remove = new Button("Remove");
		Button download = new Button("Download");
		Button copy = new Button("Copy");
		Button move = new Button("Move");
		
		remove.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event){
				fileServer.removeFiles(selected, new AsyncCallback<Void>() {
		        	public void onFailure(Throwable caught) {
				        error("Failed to remove file(s): " + caught.getMessage());
				    }
		
				    public void onSuccess(Void result){
				    	basicInstructions();
				    	treeRefresh();
				    }
				});
			}
		});
		
		download.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event){
				int length = selected.length;
				
				String url = "download?n=" + length;
				for(int i = 0; i < length; i++){
					String filename = selected[i].absolutePath;
					url += "&filename_" + i + "=" + URL.encode(filename);
				}
				
				Window.open(url, "downloadWindow", "");
			}
		});
		
		copy.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event){
				fileServer.copyFiles(selected, combo.getDisplayValue(), new AsyncCallback<Void>() {
		        	public void onFailure(Throwable caught) {
				        error("Failed to copy file(s): " + caught.getMessage());
				    }
		
				    public void onSuccess(Void result){
				    	basicInstructions();
				    	treeRefresh(); 
				    }
				});
			}
		});
		
		move.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event){
				fileServer.moveFiles(selected, combo.getDisplayValue(), new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
				        error("Failed to move file(s): " + caught.getMessage());
				    }
		
				    public void onSuccess(Void result){
				    	basicInstructions();
				    	treeRefresh();  
				    }
				});
			}
		});
		
		
		HLayout copyMoveButtons = new HLayout(10);
		copyMoveButtons.setAlign(Alignment.CENTER);
		copyMoveButtons.addMember(copy);
		copyMoveButtons.addMember(move);
		
		VLayout copyMoveLayout = new VLayout(5);
		copyMoveLayout.setPadding(5);
		copyMoveLayout.setWidth(250);
		copyMoveLayout.setHeight(80);
		copyMoveLayout.setShowEdges(true);
		copyMoveLayout.setDefaultLayoutAlign(Alignment.CENTER);
		copyMoveLayout.addMember(copyMoveButtons);
		copyMoveLayout.addMember(form);
		
		VLayout downloadLayout = new VLayout();
		downloadLayout.setPadding(5);
		downloadLayout.setWidth(110);
		downloadLayout.setHeight(30);
		downloadLayout.setShowEdges(true);
		downloadLayout.addMember(download);
		
		VLayout removeLayout = new VLayout();
		removeLayout.setPadding(5);
		removeLayout.setWidth(110);
		removeLayout.setHeight(30);
		removeLayout.setShowEdges(true);
		removeLayout.addMember(remove);
		
		final HLayout actions = new HLayout(10);
		actions.setHeight(100);
		actions.addMember(downloadLayout);
		actions.addMember(removeLayout);
		actions.addMember(copyMoveLayout);
		
		workarea.addMember(actions);
	}
	
	private void fileOperationsSelectedFiles(final Pipefile[] selected){
		// Title
		Label selectedTitle = new Label("Selected Files");
		selectedTitle.setHeight(20);
		selectedTitle.setStyleName("workarea-title");
		workarea.addMember(selectedTitle);
		
		// Selected Files
		ListGrid grid = new ListGrid();
		grid.setWidth(600);
		ListGridField nField = new ListGridField("name", "Name");  
        ListGridField pField = new ListGridField("packageName", "Package");  
        ListGridField tField = new ListGridField("type", "Type");
        grid.setFields(nField, pField, tField);
        
		ListGridRecord[] records = new ListGridRecord[selected.length];
		
		for(int i = 0; i < selected.length; i++){
			Pipefile pipe = selected[i];
			
			ListGridRecord record = new ListGridRecord();
			record.setAttribute("name", pipe.name);
			record.setAttribute("packageName", pipe.packageName);
			record.setAttribute("type", pipe.type);
			
			records[i] = record;
		}
		
		grid.setData(records);
		
		workarea.addMember(grid);
	}
	
	private void fileOperations(final Pipefile[] selected){
		clearWorkarea();
		
		fileOperationsActions(selected);
		fileOperationsSelectedFiles(selected);
	}
	
	/**
	 *  Updates the workarea with information about the file and
	 *  buttons to edit it, copy it to another package, move it to another
	 *  package, and to remove it
	 *  <p>
	 *  @param pipe Pipefile 
	 */
	private void viewFile(final Pipefile pipe){		
		clearWorkarea();
		
		// File Operations
		fileOperationsActions(new Pipefile[] {pipe});
		
		// Title
		Label editFileTitle = new Label("Edit File");
		editFileTitle.setHeight(20);
		editFileTitle.setStyleName("workarea-title");
		workarea.addMember(editFileTitle);
		
		// Edit File
		DynamicForm form = new DynamicForm();
		form.setCanEdit(true);
        form.setPadding(10);
        form.setWidth(800);
        form.setStyleName("edit-form");
		
		TextItem name = new TextItem();
		name.setTitle("Name");
		name.setName("name");
		name.setWidth(600);
		
		TextItem packageName = new TextItem();
		packageName.setTitle("Package");
		packageName.setName("package");
		packageName.setWidth(600);
		
		StaticTextItem type = new StaticTextItem();
		type.setTitle("Type");
		type.setName("type");
		
		TextAreaItem description = new TextAreaItem();
		description.setTitle("Description");
		description.setName("description");
		description.setWidth(600);
	
		TextItem input = new TextItem();
		input.setTitle("Input");
		input.setName("input");
		
		TextItem output = new TextItem();
		output.setTitle("Output");
		output.setName("output");
		
		TextItem location = new TextItem();
		location.setTitle("Location");
		location.setName("location");
		location.setWidth(600);
		
		TextItem uri = new TextItem();
		uri.setTitle("URI");
		uri.setName("uri");
		uri.setWidth(600);
		
		TextAreaItem access = new TextAreaItem();
		access.setTitle("Access");
		access.setName("access");
		access.setWidth(600);
		
		form.setFields(name, packageName, type, description, location, uri, input, output, access);
		form.setValue("name", pipe.name);
		form.setValue("package", pipe.packageName);
		form.setValue("type", pipe.type);
		form.setValue("description", pipe.description);
		
		if(pipe.type.equals("Data"))
			;//TODO fill in later with the input and output data
		else{
			form.hideItem("output");
			form.hideItem("input");
		}
		
		if(pipe.type.equals("Modules"))
			form.setValue("location", pipe.location);
		else
			form.hideItem("location");
		
		if(pipe.type.equals("Modules") || pipe.type.equals("Workflows"))
			form.setValue("uri", pipe.uri);
		else
			form.hideItem("uri");
		
		// Update Button
		Button update = new Button("Update");
		update.addClickHandler( new ClickHandler() {
			public void onClick(ClickEvent event) {
		    	// TODO
				// Update pipefile
				// Call fileServer.update
		    }
		});
		
		workarea.addMember(form);
		workarea.addMember(update);
	}
	
	/**
	 *  Updates workarea with a list of the groups 
	 */
	private void viewGroups(){
		// TODO		
		// Create a table
		
		// For each group
		//   Create a row in a table
		//   First column = name
		//   Second column = user list
		//   Third column = Edit Button
		//      OnClick - call groupEdit
		//   Fourth = Remove Button
		//      Disabled if the group is in use
		//      OnClick - call removeGroup, then groupViewSummary
	}

	/**
	 *  Updates workarea with a form to edit a group 
	 *  @param groupIndex is an index into the currentGroups
	 */
	private void editGroup(String groupName){
		// TODO
		
		// Find the group in the private variable
		
		// Create a form
		// Textarea to edit the users 
	}
	
	/**
	 *  Updates the workarea with an import form
	 */
	private void importForm(){
		// TODO
		
		// Allow user to select files/folders
		//   Checkbox for recursive
		// Allow user to supply a url
		// Import button
		//   On click, import the files, go back to basic instructions
	}
	
	
	/**
	 *  Updates the page so the user can view the root directory
	 *  @param container canvas to place the objects in
	 */
	private void rootDirectoryView(final VLayout container){		
		final Label current = new Label(rootDirectory);
		current.setHeight(40);
	    current.setAlign(Alignment.CENTER);
	    
	    current.addClickHandler(new ClickHandler() {
	    	public void onClick(ClickEvent event){
	    		// Update the view
	    		container.removeMember(current);
	    		rootDirectoyEdit(container);
	    	}
	    });
	    
	    container.addMember(current, 0);
	}
	
	/**
	 *  Updates the page so the user can edit the root directory
	 *  @param container canvas to place the objects in
	 */
	private void rootDirectoyEdit(final VLayout container){
		final TextItem newDir = new TextItem();
		newDir.setDefaultValue(rootDirectory);
		newDir.setShowTitle(false);
		newDir.setWidth(289);
		
		final DynamicForm form = new DynamicForm();
		form.setHeight(40);
		form.setMargin(5);
		form.setAlign(Alignment.CENTER);
		form.setFields(new FormItem[] {newDir});
		
		newDir.addKeyUpHandler(new KeyUpHandler() {  
            public void onKeyUp(KeyUpEvent event) {
            	String pressed = event.getKeyName();
            	if (pressed.equals(KeyNames.ENTER)){
            		// Determine if the tree needs to be updated, set the rot directory
	            	String newRoot = newDir.getValueAsString();
	            	Boolean updateTree = rootDirectory.equals(newRoot) == false;
	            	rootDirectory = newRoot;
            		
            		// Update the view
            		container.removeMember(form);
        	        rootDirectoryView(container);
        	        
        	        // Update the tree if need be
        	        if (updateTree){
        	        	treeRefresh();
        	        }
	            }
            }  
        });
		
		container.addMember(form, 0);
		form.focusInItem(newDir);
	}
	
	/**
	 *  Updates workarea with the basic instructions
	 */
	private void basicInstructions(){
		clearWorkarea();
		// TODO, add actual basic instructions
		workarea.addMember(new Label("Basic Instructions"));
	}
	
	/**
	 *  Clears the workarea
	 */
	private void clearWorkarea(){
		workarea.removeMembers( workarea.getMembers() );
	}
	
	private void error(String message){
		clearWorkarea();
		workarea.addMember(new Label("Error: " + message));
	}
}
