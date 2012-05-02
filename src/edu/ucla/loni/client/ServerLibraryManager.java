package edu.ucla.loni.client;

import edu.ucla.loni.shared.FileTree;
import edu.ucla.loni.shared.Group;
import edu.ucla.loni.shared.ModuleType;
import edu.ucla.loni.shared.Pipefile;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.KeyNames;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.events.ClickEvent;  
import com.smartgwt.client.widgets.events.ClickHandler;

import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpHandler;
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
	private String rootDirectoryDefault = "C:\\Users\\charlie\\Desktop\\PipelineRoot12";
	
	/**
	 *   Current Root Directory
	 */
	private String rootDirectory = rootDirectoryDefault;
	
	/**
	 *   NodeClickHandler for when a pipefile is selected within a tree
	 */
	private NodeClickHandler selectPipefileHandler = new NodeClickHandler() {
		public void onNodeClick(NodeClickEvent event){
			TreeNode clicked = event.getNode();
			viewFile(clicked.getAttribute("fullPath"));
		}
	};
	
	// Trees Variables
	private final Tree packageTree = new Tree();
	
	private final Tree moduleTree = new Tree();
	private final TreeNode moduleTree_data = new TreeNode("Data");
	private final TreeNode moduleTree_modules = new TreeNode("Modules");
	private final TreeNode moduleTree_workflows = new TreeNode("Workflows");
	
	private final Tree resultsTree = new Tree();
	
	// Workarea that will be updated
	private final VLayout workarea = new VLayout();
	
	// Current Pipefile
	private Pipefile currentPipefile;
	
	// Groups Array
	private Group[] currentGroups;

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
	    
	    moduleTree_data.setIsFolder(true);
	    moduleTree_modules.setIsFolder(true);
	    moduleTree_workflows.setIsFolder(true);
	    
	    moduleTree.add(moduleTree_data, moduleRoot);
	    moduleTree.add(moduleTree_modules, moduleRoot);
	    moduleTree.add(moduleTree_workflows, moduleRoot);
	    
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
	    left.setLayoutAlign(Alignment.CENTER);
	    
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
		moduleTree.removeList(moduleTree.getDescendants(moduleTree_data));
		moduleTree.removeList(moduleTree.getDescendants(moduleTree_modules));
		moduleTree.removeList(moduleTree.getDescendants(moduleTree_workflows));
		
	    // Update Trees
		fileServer.getPackageTree(
            rootDirectory, 
            new AsyncCallback<FileTree>() {
		        public void onFailure(Throwable caught) {
		    	    packageTree.add( new TreeNode("Error fetching " + rootDirectory), packageTree.getRoot());
		        }

		        public void onSuccess(FileTree result) {
		    	    if (result != null && result.children != null){
			    	    for(FileTree folder: result.children){
			    		    treeParse(folder, packageTree.getRoot());
			    	    }
		    	    }
		        }
		    }
        );
	}
	
	/**
	 *  Adds each file to its proper place on the Package Tree and Module Tree
	 */
	private void treeParse(FileTree file, TreeNode parent){
		TreeNode branch = new TreeNode(file.name);
		branch.setAttribute("fullPath", file.fullPath);
		branch.setAttribute("moduleType", file.type);
		
		// Add to packageTree
		packageTree.add(branch, parent);
		
		// If folder, recurse
		if (file.folder){
			if (file.children != null){
				for(FileTree child : file.children){
					treeParse(child, branch);
				}
			}
		} 
		// Else add to moduleTree
		else {
			if (file.type == ModuleType.DATA){
				moduleTree.add(branch, moduleTree_data);
			} else if (file.type == ModuleType.EXECUTABLE){
				moduleTree.add(branch, moduleTree_modules);
			} else {
				moduleTree.add(branch, moduleTree_workflows);
			}
		}
	}
	
	/**
	 *  Updates ResultsTree based on what query is returned by the server
	 */
	private void treeResults(final String query){
		fileServer.getSearchResults(
            rootDirectory,
            query,
            new AsyncCallback<FileTree>() {
		        public void onFailure(Throwable caught) {
		        	clearWorkarea();
		    	    workarea.addMember(new Label("Error fetching pipefile: " + query));
		        }

		        public void onSuccess(FileTree result) {
		        	// Clear the resultsTree
		        	resultsTree.removeList(resultsTree.getDescendants());
		        	
		        	// Add in all the search results
		        	TreeNode root = resultsTree.getRoot();
		        	if (result != null && result.children != null){
			        	for(FileTree file: result.children){
			        		TreeNode branch = new TreeNode(file.name);
				    		branch.setAttribute("fullPath", file.fullPath);
				    		branch.setAttribute("moduleType", file.type);
				    		
				    		resultsTree.add(branch, root);
			        	}
		        	}
		        }
		    }
        );
	}
	
	/**
	 *  Updates the workarea with information about the file and
	 *  buttons to edit it, copy it to another package, move it to another
	 *  package, and to remove it
	 *  <p>
	 *  Saves the Pipefile to the private variable currentPipefile
	 *  @param pathname full pathname for the file
	 */
	private void viewFile(final String pathname){
		clearWorkarea();
		
		fileServer.getFile(
            pathname, 
            new AsyncCallback<Pipefile>() {
		        public void onFailure(Throwable caught) {
		    	    workarea.addMember(new Label("Error fetching pipefile: " + pathname));
		        }

		        public void onSuccess(Pipefile result) {
		        	currentPipefile = result;
        			// TODO
		        	// Parse XML
        			// Display properties
		        	clearWorkarea();
		        	workarea.addMember(new Label(pathname));
		        }
		    }
        );
	}
	
	/**
	 *  Updates the workarea with a form to edit the file
	 */
	private void editFile(){
		// TODO
	}
	
	/**
	 *  Updates workarea with a list of the groups 
	 */
	private void viewGroups(){
		// TODO
		// Call getGroups, save into private variable currentGroups
		
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
	private void editGroup(int groupIndex){
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
		newDir.setAlign(Alignment.CENTER);
		newDir.setVAlign(VerticalAlignment.CENTER);
		
		final DynamicForm form = new DynamicForm();
		form.setHeight(40);
		form.setMargin(5);
		form.setLayoutAlign(Alignment.CENTER);
		form.setLayoutAlign(VerticalAlignment.CENTER);
		form.setFields(new FormItem[] {newDir});
		
		newDir.addKeyUpHandler(new KeyUpHandler() {  
            public void onKeyUp(KeyUpEvent event) {
            	String pressed = event.getKeyName();
            	if (pressed.equals(KeyNames.ENTER)){
	            	container.removeMember(form);
	            	rootDirectory = newDir.getValueAsString();
	            	rootDirectoryView(container);
	            	treeRefresh();
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
}
