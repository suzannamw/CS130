package edu.ucla.loni.client;

import java.util.LinkedHashMap;

import edu.ucla.loni.shared.FileTree;
import edu.ucla.loni.shared.Group;
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
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpHandler;
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
	private String rootDirectoryDefault = "C:\\Users\\charlie\\Desktop\\PipelineRoot12";
	
	/**
	 *   Current Root Directory
	 */
	private String rootDirectory = rootDirectoryDefault;
	
	/**
	 *   Current Packages
	 */
	private final LinkedHashMap<String, String> currentPackages = new LinkedHashMap<String,String>();
	
	/**
	 *   Current Selected Files
	 */
	private String[] currentSelectedFiles;
	
	/**
	 *   Current Pipefile
	 */
	private Pipefile currentPipefile;
	
	/**
	 *   Current Groups
	 */
	private Group[] currentGroups;
	
	/**
	 *   Package Tree
	 */
	private final Tree packageTree = new Tree();
	
	/**
	 *   Module Tree
	 */
	private final Tree moduleTree = new Tree();
	
	/**
	 *   Results Tree
	 */
	private final Tree resultsTree = new Tree();
	
	/**
	 *   Workarea
	 */
	private final VLayout workarea = new VLayout();
	
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
			else if (numSelected == 1 && !folder){
				viewFile(clicked.getAttribute("fullPath"));
			}
			else {
				currentSelectedFiles = new String[selected.length];
				for (int i = 0; i < selected.length; i++){
					currentSelectedFiles[i] = selected[i].getAttribute("fullPath");
				}
				fileOperations(currentSelectedFiles);
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
			    		    currentPackages.put(folder.fullPath, folder.name);
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
			String moduleType = parent.getName();
			TreeNode[] existingTypes = moduleTree.getChildren(moduleTree.getRoot());
			
			boolean createNew = true;
			for (TreeNode type : existingTypes){
				if (type.getName().equals(moduleType)){
					moduleTree.add(branch, type);
					createNew = false;
					break;
				}
			}
			
			if (createNew){
				TreeNode newType = new TreeNode(moduleType);
				moduleTree.add(newType, moduleTree.getRoot());
				moduleTree.add(branch, newType);
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
				    		
				    		resultsTree.add(branch, root);
			        	}
		        	}
		        }
		    }
        );
	}
	
	private void fileOperations(String[] selected){
		clearWorkarea();
		
		// WorkareaTitle
		Label workareaTitle = new Label("File Operations");
		workareaTitle.setHeight(20);
		workareaTitle.setStyleName("workarea-title");
		workarea.addMember(workareaTitle);
		
		Label recordsInfo = new Label("Selecting a folder is synonomous to selecting all pipefiles containted in that folder (at any depth)");
		recordsInfo.setHeight(30);
		recordsInfo.setStyleName("workarea-instructions");
		recordsInfo.setValign(VerticalAlignment.TOP);
		workarea.addMember(recordsInfo);
		
		// Actions
		Button remove = new Button("Remove");
		Button download = new Button("Download");
		Button copy = new Button("Copy");
		Button move = new Button("Move");
		
		HLayout copyMoveButtons = new HLayout(10);
		copyMoveButtons.setAlign(Alignment.CENTER);
		copyMoveButtons.addMember(copy);
		copyMoveButtons.addMember(move);
		
		ComboBoxItem combo = new ComboBoxItem();
		combo.setTitle("To Package"); 
		combo.setValueMap(currentPackages);
		
		DynamicForm form = new DynamicForm();
		form.setItems(combo);		
		
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
		
		// SelectedFiles
		Label recordsTitle = new Label("Selected Files and Folders");
		recordsTitle.setHeight(20);
		recordsTitle.setStyleName("workarea-subtitle");
		workarea.addMember(recordsTitle);
		
		for(String filename : selected){
			Label selectedFile = new Label(filename);
			selectedFile.setHeight(15);
			selectedFile.setStyleName("workarea-selectedFile");
			workarea.addMember(selectedFile);
		}
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
		
		final DynamicForm form = new DynamicForm();
		form.setHeight(40);
		form.setMargin(5);
		form.setAlign(Alignment.CENTER);
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
