package net.boerwi.ogaredit;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.Dimension;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.swing.filechooser.FileFilter;

import net.boerwi.extrawidgets.HelpIcon;
import net.boerwi.snowfluke.Snowfluke;

import org.json.*;

import net.boerwi.ogaredit.OE_DB;//FIXME this was a quick hack, but Node, Dir, and Entry should probably be public
/** Swing-based interface. */
public class OE_ViewControl_Swing extends OE_ViewControl implements WindowListener{
	private JFrame frame;
	private JPanel viewPanel;
	private JMenu editMenu, projectMenu;
	private JPopupMenu assetPopupSingle, assetPopupMulti;
	private JTree assetTree;
	private ResEditor currentEditor = null;
	JSplitPane splitpane;
	//JScrollPane viewPanel;
	// File actions
	private OEAction newAction, openAction, saveAction, closeAction, quitAction;
	// Edit actions
	private OEAction deleteAction, exportAction, ogarGalleryExportAction, createImageAction, createTextAction, createLabelStyleAction, createAudioAction, createFloorplanAction, createGalleryAction, createAction, duplicateAction, renameAction, bulkCreateAction, moveAction, createFolderAction, helpAction, aboutAction, createStressTestGallery, infoAction;
	/** Populates the main window with swing elements */
	private void populateFrame() {
		// File actions
		newAction = new OEAction("✧ New");
		openAction = new OEAction("✉ Open");
		saveAction = new OEAction("🖫 Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		closeAction = new OEAction("✘ Close");
		quitAction = new OEAction("⏻ Quit");
		// Edit actions
		deleteAction = new OEAction("Delete Selected");
		exportAction = new OEAction("Export Selected");
		ogarGalleryExportAction = new OEAction("OGAR Gallery Export");
		createAction = new OEAction("Create");
		bulkCreateAction = new OEAction("Bulk Create");
		moveAction = new OEAction("Move Selected");
		duplicateAction = new OEAction("Duplicate");
		renameAction = new OEAction("Rename");
		createImageAction = new OEAction("Create Image");
		createTextAction = new OEAction("Create Text");
		createLabelStyleAction = new OEAction("Create Label Style");
		createAudioAction = new OEAction("Create Audio");
		createFloorplanAction = new OEAction("Create Floorplan");
		createGalleryAction = new OEAction("Create Gallery");
		createFolderAction = new OEAction("Create Folder");
		createStressTestGallery = new OEAction("Create Stress-Test Gallery");
		infoAction = new OEAction("Info");
		helpAction = new OEAction("Help");
		aboutAction = new OEAction("About");
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		fileMenu.add(new JMenuItem(newAction));
		fileMenu.add(new JMenuItem(openAction));
		fileMenu.add(new JMenuItem(saveAction));
		fileMenu.add(new JMenuItem(closeAction));
		fileMenu.add(new JMenuItem(quitAction));
		
		editMenu = new JMenu("Edit");
		menuBar.add(editMenu);
		JMenu editCreate = new JMenu("Create");
		editCreate.add(new JMenuItem(createImageAction));
		editCreate.add(new JMenuItem(createTextAction));
		editCreate.add(new JMenuItem(createLabelStyleAction));
		editCreate.add(new JMenuItem(createAudioAction));
		editCreate.add(new JMenuItem(createFloorplanAction));
		editCreate.add(new JMenuItem(createGalleryAction));
		editCreate.add(new JMenuItem(createStressTestGallery));
		editMenu.add(editCreate);
		editMenu.add(new JMenuItem(deleteAction));
		
		projectMenu = new JMenu("Project");
		menuBar.add(projectMenu);
		projectMenu.add(ogarGalleryExportAction);
		
		//JMenu helpMenu = new JMenu("Help");
		//menuBar.add(helpMenu);
		//helpMenu.add(helpAction);
		//helpMenu.add(aboutAction);
		
		viewPanel = new JPanel(new BorderLayout());
		assetTree = new JTree((TreeModel) null);
		// Delete is not an accelerator because it should not be universally available, rather, it should be available only when the tree is selected to prevent user confusion
		assetTree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
		assetTree.getActionMap().put("deleteSelected", deleteAction);
		assetTree.setEditable(true);
		assetTree.addTreeSelectionListener(new TreeSelectionListener() { public void valueChanged(TreeSelectionEvent e) {
			updateView();
		}});
		// Right-click Menu Generation
		assetPopupSingle = new JPopupMenu("Single");
		assetPopupSingle.add(new JSeparator());
		assetPopupSingle.add(new JMenuItem(createAction));
		assetPopupSingle.add(new JMenuItem(bulkCreateAction));
		assetPopupSingle.add(new JMenuItem(createFolderAction));
		assetPopupSingle.add(new JMenuItem(moveAction));
		assetPopupSingle.add(new JMenuItem(duplicateAction));
		assetPopupSingle.add(new JMenuItem(renameAction));
		assetPopupSingle.add(new JMenuItem(infoAction));
		//assetPopupSingle.add(new JMenuItem(exportAction));
		assetPopupSingle.add(new JSeparator());
		assetPopupSingle.add(new JMenuItem(deleteAction));
		assetPopupMulti = new JPopupMenu("Multi");
		assetPopupMulti.add(new JSeparator());
		assetPopupMulti.add(new JMenuItem(moveAction));
		//assetPopupMulti.add(new JMenuItem(exportAction));
		assetPopupMulti.add(new JSeparator());
		assetPopupMulti.add(new JMenuItem(deleteAction));
		assetTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				tryPopup(e);
			}
			public void mouseReleased(MouseEvent e) {
				tryPopup(e);
			}
			void tryPopup(MouseEvent e) {
				int pidx = assetTree.getRowForLocation(e.getX(), e.getY());
				if(e.isPopupTrigger() && pidx != -1){
					int[] selRows = assetTree.getSelectionRows();
					Arrays.sort(selRows);
					if(Arrays.binarySearch(selRows, pidx) >= 0){
						//This doesn't change the selection
					}else{
						assetTree.clearSelection();
						assetTree.addSelectionRow(pidx);
					}
					int selCount = assetTree.getSelectionCount();
					assert selCount > 0 : "nothing selected for right click";
					if(selCount == 1){
						assetPopupSingle.show(assetTree, e.getX(), e.getY());
					}else{
						assetPopupMulti.show(assetTree, e.getX(), e.getY());
					}
				}
			}
		});
		JScrollPane treeContainer = new JScrollPane(assetTree);
		treeContainer.setPreferredSize(new Dimension(300, 600));
		splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, treeContainer, viewPanel);
		frame.getContentPane().add(splitpane);
		frame.getContentPane().setPreferredSize(new Dimension(800, 600));
		frame.pack();
	}
	/** 
	 * FIXME
	 */
	private void launchGui() {
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);
		/*try{
			System.out.println("System: "+UIManager.getSystemLookAndFeelClassName()+" Available: "+Arrays.toString(UIManager.getInstalledLookAndFeels()));
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		}catch(Exception e){
			System.out.println("Failed to get look and feel");
		}*/
		frame = new JFrame("OGAREdit");
		// Close will be handled by a window listener which will cleanly shutdown other resources.
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		// Add windowevent handler
		frame.addWindowListener(this);
		// Add in frame elements
		populateFrame();
		// Set initial state
		newAction.setEnabled(true);
		openAction.setEnabled(true);
		saveAction.setEnabled(false);
		closeAction.setEnabled(false);
		editMenu.setEnabled(false);
		// Make the window appear
		frame.setVisible(true);
	}
	/** called by super to prep for redrawing. (avoid super having to come first)
	 */
	void init(){
		try{
			javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					launchGui();
				}
			});
		}catch(InterruptedException e){
			assert false : "Interrupted while launching GUI: "+e.toString();
		}catch(InvocationTargetException e){
			assert false : "Exception while launching GUI: "+e.toString();
		}
	}
	/** Creates a OE_ViewControl_Swing instance.
	 * @param openPath Path to a .ogr file to open on startup, or NULL.
	 */
	public OE_ViewControl_Swing(String openPath) {
		super(openPath);
	}
	private void openExistingDialog(){
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(ResEditor.oeFileFilter);
		int ret = fc.showOpenDialog(frame);
		if(ret == JFileChooser.APPROVE_OPTION){
			Path path = fc.getSelectedFile().toPath();
			openDB(path, false);
		}
	}
	private void openNewDialog(){
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(ResEditor.oeFileFilter);
		fc.setAcceptAllFileFilterUsed(false);
		int ret = fc.showSaveDialog(frame);
		if(ret == JFileChooser.APPROVE_OPTION){
			String existingFilePath = fc.getSelectedFile().getAbsolutePath();
			if(!existingFilePath.endsWith(".ogr")){
				existingFilePath = existingFilePath+".ogr";
			}
			File existingFile = new File(existingFilePath);
			if(existingFile.exists()){
				int overwrite = JOptionPane.showConfirmDialog(null, "Overwrite and clear existing file \""+existingFilePath+"\"?", "Overwrite File", JOptionPane.YES_NO_OPTION);
				if(overwrite == JOptionPane.NO_OPTION){
					return;
				}
				existingFile.delete();
			}
			openDB(existingFile.toPath(), true);
		}
	}
	class OEAction implements Action{
		ArrayList<PropertyChangeListener> listeners = new ArrayList<>();
		TreeMap<String, Object> properties = new TreeMap<>();
		String name;
		String commName;
		
		static final String ENABLED_KEY = "enabled";
		public OEAction(String name){
			this(name, null);
		}
		public OEAction(String name, KeyStroke accel){
			this.name = name;
			commName = name.toUpperCase().replace(' ', '_');
			properties.put(ENABLED_KEY, true);
			properties.put(SHORT_DESCRIPTION, null);
			properties.put(ACTION_COMMAND_KEY, commName);
			properties.put(MNEMONIC_KEY, null);
			properties.put(NAME, name);
			properties.put(DISPLAYED_MNEMONIC_INDEX_KEY, null);
			properties.put(SMALL_ICON, null);
			properties.put(LARGE_ICON_KEY, null);//this stays null. use small icon only
			properties.put(ACCELERATOR_KEY, accel);
			properties.put(SELECTED_KEY, true); //null disables it from changing state
		}
		public void addPropertyChangeListener(PropertyChangeListener listener){
			listeners.add(listener);
		}
		public Object getValue(String key){
			return properties.get(key);
		}
		public boolean isEnabled(){ return (boolean)getValue(ENABLED_KEY); }

		public void putValue(String key, Object value){
			Object oldValue = getValue(key);
			properties.put(key, value);
			if(oldValue != value){
				PropertyChangeEvent evt = new PropertyChangeEvent(this, key, oldValue, value);
				for(PropertyChangeListener l : listeners){
					l.propertyChange(evt);
				}
			}
		}
		public void removePropertyChangeListener(PropertyChangeListener listener){
			listeners.remove(listener);
		}
		public void setEnabled(boolean b){ putValue(ENABLED_KEY, b); }
		public void actionPerformed(ActionEvent evt){
			String command = evt.getActionCommand();
			int modkeys = evt.getModifiers();
			Object source = evt.getSource();
			TreePath[] selRows = assetTree.getSelectionPaths();
			if(selRows == null) selRows = new TreePath[0];
			ArrayList<Node> targets = new ArrayList<>();
			for(int idx = 0; idx < selRows.length; idx++){
				targets.add((Node)(selRows[idx].getLastPathComponent()));
			}
			switch(command) {
				case "⏻_QUIT":
					if(!closeDB()) break;
					cleanup();
					break;
				case "✧_NEW": openNewDialog();
					break;
				case "✉_OPEN": openExistingDialog();
					break;
				case "🖫_SAVE": saveDB();
					break;
				case "✘_CLOSE": 
					closeDB();
					break;
				case "CREATE_IMAGE": createResource(OE_ResType.IMAGE, db.imageRoot);
					break;
				case "CREATE_TEXT": createResource(OE_ResType.TEXT, db.textRoot);
					break;
				case "CREATE_LABEL_STYLE": createResource(OE_ResType.LABELSTYLE, db.labelStyleRoot);
					break;
				case "CREATE_AUDIO": createResource(OE_ResType.AUDIO, db.audioRoot);
					break;
				case "CREATE_FLOORPLAN": createResource(OE_ResType.FLOORPLAN, db.floorplanRoot);
					break;
				case "CREATE_GALLERY": createResource(OE_ResType.GALLERY, db.galleryRoot);
					break;
				case "CREATE_STRESS-TEST_GALLERY": createStressTestGallery();
					break;
				case "INFO": openInfoDialog(targets.get(0));
					break;
				case "CREATE":
					Dir tp = (Dir) targets.get(0);
					createResource(tp.type, tp);
					break;
				case "BULK_CREATE":
					bulkCreateDialog((Dir)(targets.get(0)));
					break;
				case "MOVE_SELECTED":
					moveNodesDialog(targets);
					break;
				case "DUPLICATE":
					duplicate((Entry)(targets.get(0)));
					break;
				case "RENAME":
					String ret = (String)JOptionPane.showInputDialog(frame, "New Name:", "Rename", JOptionPane.PLAIN_MESSAGE, (Icon)null, (Object[])null, targets.get(0).toString());
					if(ret != null){
						//TreeModel rename
						db.valueForPathChanged(selRows[0], ret);
					}
					break;
				case "EXPORT_SELECTED":
					exportNodesDialog(targets);
					break;
				case "OGAR_GALLERY_EXPORT":
					if(targets.size() == 1 && targets.get(0) instanceof Entry && ((Entry)targets.get(0)).getData() instanceof OE_Gallery){
						ogarGalleryExportDialog((OE_Gallery)((Entry)(targets.get(0))).getData());
					}else{
						ogarGalleryExportDialog(null);
					}
					break;
				case "DELETE_SELECTED":
					LinkedHashSet<Node> delTarg = new LinkedHashSet<>(targets);
					// Construct view of every tree node (Dirs and Entries) getting deleted
					for(Node n : targets){
						if(n instanceof Dir){
							 delTarg.addAll(((Dir)n).getAllChildren());
						}
					}
					// Gather all of the OE_Resources we are going to delete
					LinkedHashSet<OE_Resource> delResTarg = new LinkedHashSet<>();
					for(Node n : delTarg){
						if(n instanceof Entry){
							delResTarg.add(((Entry)n).getData());
						}
					}
					// Make sure all Nodes are editable
					for(Node n : delTarg){
						if( !n.isEditable() ){
							JOptionPane.showMessageDialog(frame, "Object \""+n+"\" cannot be deleted because it is not editable.");
							return;
						}
					}
					// Make sure no OE_Resources have dependents which are not also getting deleted
					for(OE_Resource r : delResTarg){
						// "Does r.dependents have anything which is not in delResTarg?"
						if( !delResTarg.containsAll(r.dependents) ){
							JOptionPane.showMessageDialog(frame, "Resource \""+r+"\" cannot be deleted because other resources depend on it.");
							return;
						}
					}
					String confirmDeleteMsg;
					if(delTarg.size() == 1){
						// If there is only one deletion target, name it.
						confirmDeleteMsg = "Delete \""+delTarg.toArray()[0].toString()+"\"? This cannot be undone.";
					}else{
						confirmDeleteMsg = "Delete "+delTarg.size()+" items? This cannot be undone.";
					}
					int userChoice = JOptionPane.showConfirmDialog(null, confirmDeleteMsg, "Confirm Delete", JOptionPane.YES_NO_OPTION);
					if(userChoice == JOptionPane.YES_OPTION){
						removeNodes(delTarg);
					}
					break;
				case "CREATE_FOLDER":
					System.out.println("Creating folder");
					assert assetTree.getSelectionCount() == 1;
					createSubDir((Dir)(assetTree.getSelectionPath().getLastPathComponent()), "New Folder");
					break;
				default: System.out.println("Unknown command: "+command);
					break;
			}
		}
	}
	void openInfoDialog(Node target){
		assert target != null;
		final JDialog infoDialog = new JDialog(frame, target.toString(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		infoDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		Container content = infoDialog.getContentPane();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.add(new JLabel(target.toString()));
		if(target instanceof Entry){
			// Resource
			Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
			OE_Resource res = ((Entry)target).getData();
			content.add(new JLabel(res.getType().toString()));
			content.add(new JLabel(Snowfluke.toString(res.getId())));
			content.add(HelpIcon.wrapString("Dependents:", "<html><p>Dependents are resources that rely on, or reference, this resource.</p><p>For example, a Floorplan may have a Gallery as a dependent.</p>"));
			JTextArea dependents = new JTextArea(8, 40);
			dependents.setEditable(false);
			dependents.setFont(mono);
			for(OE_Resource dep : res.getDependents()){
				dependents.append(String.format("%d : %12s : %s\n", dep.getId(), dep.getType(), dep.toString()));
			}
			content.add(new JScrollPane(dependents));
			content.add(HelpIcon.wrapString("Dependencies:", "<html><p>Dependencies are resources that this resource references, or relies on.</p><p>For example, a Gallery may depend on a Floorplan.</p>"));
			JTextArea dependencies = new JTextArea(8, 40);
			dependencies.setEditable(false);
			dependencies.setFont(mono);
			for(OE_Resource dep : res.getDependencies()){
				dependencies.append(String.format("%d : %12s : %s\n", dep.getId(), dep.getType(), dep.toString()));
			}
			content.add(new JScrollPane(dependencies));
		}else{
			// Directory
			content.add(new JLabel("Directory"));
			content.add(new JLabel("- Editable: "+target.isEditable()));
			content.add(new JLabel("- Content Type: "+target.getType()));
		}
		JButton btExit = new JButton("Close");
		btExit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				infoDialog.dispose();
			}
		});
		content.add(btExit);
		infoDialog.pack();
		infoDialog.setVisible(true);
	}
	void bulkCreateDialog(Dir destination){
		OE_ResType type = destination.getType();
		assert type != OE_ResType.ALL;
		assert db != null;
		// Determine which files we can bulk-load from
		FileFilter typeFileFilter;
		if(type == OE_ResType.IMAGE){
			typeFileFilter = ResEditor.imageFileFilter;
		}else if(type == OE_ResType.TEXT){
			typeFileFilter = ResEditor.textFileFilter;
		}else if(type == OE_ResType.AUDIO){
			typeFileFilter = ResEditor.audioFileFilter;
		}else{
			JOptionPane.showMessageDialog(frame, String.format("This type (%s) cannot be loaded directly from a file.", type));
			return;
		}
		if(type == OE_ResType.IMAGE){
			// Images get special treatment to optionally load their tags from a JSON file.
			final String jsonHelpString = String.format(
				"""
				<html>\
				<p>You can specify a JSON file to load image definitions from.</p>\
				<p>The file must contain a JSON Object in the following format:</p>\
				<pre>
				
				{
				   "imageName1":{
				      "Filepath": "&lt;ABSOLUTE_FILE_PATH_TO_IMAGE&gt;",
				      "Width": 0.53,
				      "Height": 0.77,
				      "Title": "Mona Lisa",
				      "Artist": "Leonardo da Vinci",
				      ...
				   },
				   "imageName2":{
				      ...
				   },
				   ...
				}
				
				</pre>
				<p>"Filepath", "Width", and "Height" are required for each image.</p>\
				<p>The other valid, optional, keys are:</p>\
				<pre>%s</pre>\
				</html>\
				""",
				Arrays.toString(OE_Image.getBaseTags())
			);
			int jsonUserChoice = JOptionPane.showConfirmDialog(frame, HelpIcon.wrapString("Would you like to load image definitions from a JSON file? (Press 'No' if you don't know.)", jsonHelpString), "Image Bulk JSON Load?", JOptionPane.YES_NO_CANCEL_OPTION);
			if(jsonUserChoice == JOptionPane.CANCEL_OPTION || jsonUserChoice == JOptionPane.CLOSED_OPTION){
				// User cancel while choosing about json
				return;
			}
			if(jsonUserChoice == JOptionPane.YES_OPTION){
				// User committed to loading images from JSON
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(ResEditor.jsonFileFilter);
				int fcUserChoice = fc.showOpenDialog(frame);
				File jsonSource;
				if(fcUserChoice == JFileChooser.APPROVE_OPTION){
					jsonSource = fc.getSelectedFile().getAbsoluteFile();
				}else{
					// User cancel
					return;
				}
				assert jsonSource != null : "User appeared to approve file choice, but no file chosen";
				// We now have our json source file selected
				System.out.println("JSON Source: "+jsonSource);
				String jsonData;
				try{
					jsonData = new String(new FileInputStream(jsonSource).readAllBytes(), "UTF-8");
				}catch(Exception exc){
					assert false : exc;
					return;
				}
				JSONObject imgJson;
				try{
					imgJson = new JSONObject(jsonData);
				}catch(JSONException exc){
					JOptionPane.showMessageDialog(frame, "File does not contain a valid JSON Object (or has duplicate keys)");
					return;
				}
				// We now have valid JSON
				String[] artNames = JSONObject.getNames(imgJson);
				if(artNames == null) return;
				for(String artName : artNames){
					JSONObject artEntry = imgJson.getJSONObject(artName);
					String entryPath = artEntry.optString("Filepath");
					if(entryPath.equals("")){
						JOptionPane.showMessageDialog(frame, "JSON entry \""+artName+"\" does not have a 'Filepath' attribute");
						continue;
					}
					double iWidth = artEntry.optDouble("Width");
					double iHeight = artEntry.optDouble("Height");
					if(Double.isNaN(iWidth) || Double.isNaN(iHeight)){
						JOptionPane.showMessageDialog(frame, "JSON entry \""+artName+"\" is missing 'Width' and/or 'Height' attributes");
						continue;
					}
					OE_Image retImg = null;
					try{
						FileInputStream imgDataStream = new FileInputStream(entryPath);
						retImg = (OE_Image)(OE_Resource.createFromData(artName, imgDataStream, type, db.blobMgr));
					}catch(Exception exc){
						JOptionPane.showMessageDialog(frame, "Failed to load image \""+artName+"\" at \""+entryPath+"\"");
					}
					if(retImg == null){
						JOptionPane.showMessageDialog(frame, String.format("Image \"%s\" failed to load", artName));
					}else{
						retImg.setDimensions((int)(iWidth*1000.0), (int)(iHeight*1000.0));
						// Add any tags that are specified in the json
						for(String tKey : OE_Image.getBaseTags()){
							String tEntry = artEntry.optString(tKey, null);
							if(tEntry != null){
								retImg.setTag(tKey, tEntry);
							}
						}
						db.addResource(retImg, destination);
					}
				}
				// We finished loading from JSON. Do not proceed to user-choice selection method
				JOptionPane.showMessageDialog(frame, "Done bulk loading images from "+jsonSource);
				return;
			}
			assert jsonUserChoice == JOptionPane.NO_OPTION : "unexpected choice while asking user about json import: "+jsonUserChoice;
		}
		// Figure out what files the user wants
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(typeFileFilter);
		fc.setMultiSelectionEnabled(true);
		int fcUserChoice = fc.showOpenDialog(frame);
		File[] selFiles;
		if(fcUserChoice == JFileChooser.APPROVE_OPTION){
			selFiles = fc.getSelectedFiles();
		}else{
			// User cancel
			return;
		}
		System.out.println(Arrays.toString(selFiles));
		// Add them one at a time
		for(File item : selFiles){
			FileInputStream dataStream = null;
			try{
				 dataStream = new FileInputStream(item);
			}catch(Exception exc){
				assert false : exc;
				return;
			}
			OE_Resource ret = OE_Resource.createFromData(item.getName(), dataStream, type, db.blobMgr);
			if(ret == null){
				JOptionPane.showMessageDialog(frame, String.format("Item \"%s\" failed to load", item.getName()));
			}else{
				db.addResource(ret, destination);
			}
		}
		JOptionPane.showMessageDialog(frame, "Done bulk loading");
	}
	void exportNodesDialog(ArrayList<Node> targets){
		System.out.println("TODO");//TODO
	}
	void ogarGalleryExportDialog(OE_Gallery target){
		if(target == null){
			OE_ResourceSelector selector = new OE_ResourceSelector(db);
			target = (OE_Gallery)selector.singleSelect(OE_ResType.GALLERY);
			// User cancelled selection
			if(target == null) return;
		}
		if(target.fp == null){
			JOptionPane.showMessageDialog(frame, "Cannot export - This gallery does not have an associated floorplan.");
			return;
		}
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setFileHidingEnabled(true);
		fc.setMultiSelectionEnabled(false);
		int userResp = fc.showSaveDialog(null);
		// User cancelled save destination selection
		if(userResp != JFileChooser.APPROVE_OPTION) return;
		File sel = fc.getSelectedFile();
		assert sel.isDirectory() : "Selected destination appears to not be a directory";
		Path exportPath = Path.of(sel.getAbsolutePath(), String.format("OEG_%s.zip", target.getUniqueName().replaceAll("\\W+", "_")));
		File exportFile = exportPath.toFile();
		if(exportFile.exists()){
			// No and Cancel behave the same
			int userChoiceOverwrite = JOptionPane.showConfirmDialog(frame, "File \""+exportPath+"\" already exists. Overwrite it?", "Export - File Exists", JOptionPane.YES_NO_CANCEL_OPTION);
			if(userChoiceOverwrite != JOptionPane.YES_OPTION){
				// User cancel on overwrite prompt
				return;
			}
		}

		class ExportTask extends Thread{
			private JDialog cancelDialog;
			private OE_Gallery target;
			private String status = null;
			private final boolean[] running = new boolean[]{true};
			public ExportTask(OE_Gallery target){
				this.target = target;
				cancelDialog = new JDialog(frame, "Exporting", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
				cancelDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				Container pane = cancelDialog.getContentPane();
				pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
				pane.add(new JLabel("Exporting \""+target+"\". Please wait..."));
				JButton btCancel = new JButton("Cancel Export");
				btCancel.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent evt){
						running[0] = false;
						try{
							join();
						}catch(InterruptedException e){
							assert false : "ModalExportCancel-er interrupted while joining cancel target";
						}
						cancelDialog.dispose();
					}
				});
				pane.add(btCancel);
				cancelDialog.pack();
				cancelDialog.setResizable(false);
			}
			public void run(){
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						show();
					}
				});
				status = this.target.exportPackaged(exportPath, running);
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						hide();
						if(status == null){
							System.out.println("User selected: "+exportPath);
							JOptionPane.showMessageDialog(frame, "Finished export to \""+exportPath+"\"");
						}else if(status.equals("User Cancelled")){
							System.out.println("User Cancelled export");
							exportFile.delete();
						}else{
							System.out.println("Export failed");
							JOptionPane.showMessageDialog(frame, "Failure to export to \""+exportPath+"\".\nExport message was: \""+status+"\"");
						}
					}
				});
			}
			public void show(){
				cancelDialog.setVisible(true);
			}
			public void hide(){
				cancelDialog.setVisible(false);
			}
		}
		ExportTask exportTask = new ExportTask(target);
		exportTask.start();
	}
	void moveNodesDialog(ArrayList<Node> targets){
		OE_ResType t = targets.get(0).getType();
		for(Node target : targets){
			// Check for top-level directories
			if(target instanceof Dir && !((Dir)target).isEditable()){
				JOptionPane.showMessageDialog(null, "Cannot move top-level directories.");
				return;
			}
			// Check everything is the same type
			if(target.getType() != t){
				JOptionPane.showMessageDialog(null, "All move targets must be of the same type (\""+target+"\" is not type \""+t+"\").");
				return;
			}
		}
		
		// Remove all selected items of selected directories
		for(int idx = targets.size()-1; idx >= 0; idx--){
			Node target = targets.get(idx);
			// Get target path, and remove the item (so it doesn't check for itself)
			ArrayList<Node> targPath = target.getPath();
			targPath.remove(targPath.size()-1);
			for(Node testTarget : targets){
				if(testTarget instanceof Dir){
					if(targPath.contains(testTarget)){
						targets.remove(idx);
						break;
					}
				}
			}
		}
		OE_ResourceSelector selector = new OE_ResourceSelector(db);
		Dir destDir = selector.selectDirectory(t);
		if(destDir == null) return; // User pressed cancel on destination selection
		//Verify that the destination isn't a subdirectory of a selection
		ArrayList<Node> destPath = destDir.getPath();
		for(Node destPathItem : destPath){
			if(targets.contains(destPathItem)){
				JOptionPane.showMessageDialog(null, "You cannot move a directory into its subdirectory (See \""+destPathItem+"\")");
				return;
			}
		}
		System.out.println("Moving "+targets+" to "+destDir);
		//TODO Tell the DB to move it
		for(Node target : targets){
			db.moveNode(target, destDir);
		}
	}
	void viewPath(Object[] path){
		assetTree.expandPath(new TreePath(path));
	}
	OE_Resource editTarget = null;
	void updateView(){
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			TreeModel currModel = assetTree.getModel();
			if(db != currModel){
				assetTree.setModel(db);
				assert db == assetTree.getModel();
			}
			OE_Resource newEditTarget = null;
			duplicateAction.setEnabled(false);
			if(isDBOpen()){
				int[] selRows = assetTree.getSelectionRows();
				TreePath[] selections = assetTree.getSelectionPaths();
				newAction.setEnabled(false);
				openAction.setEnabled(false);
				saveAction.setEnabled(true);
				closeAction.setEnabled(true);
				editMenu.setEnabled(true);
				projectMenu.setEnabled(true);
				moveAction.setEnabled(true);
				deleteAction.setEnabled(selRows.length > 0);
				//FIXME
				boolean isAddCandidate = selRows.length == 1 && selections[0].getLastPathComponent() instanceof Dir && ((Dir)(selections[0].getLastPathComponent())).canAddChildren();
				createFolderAction.setEnabled(isAddCandidate);
				createAction.setEnabled(isAddCandidate);
				bulkCreateAction.setEnabled(isAddCandidate);
				boolean isRenameCandidate = selRows.length == 1 && ((Node)selections[0].getLastPathComponent()).isEditable();
				renameAction.setEnabled(isRenameCandidate);
				if(selRows.length == 1) { //Open editor for that selected item
					Node elem = (Node)selections[0].getLastPathComponent();
					if(elem instanceof Entry) { //this is not a directory, so open an editor
						newEditTarget = ((Entry)elem).getData();
						duplicateAction.setEnabled(true);
					}
					if(!elem.isEditable()){
						deleteAction.setEnabled(false);
						moveAction.setEnabled(false);
					}
				}
				if(newEditTarget != editTarget) {
					if(currentEditor != null){
						currentEditor.cleanup();
						currentEditor = null;
					}
					viewPanel.removeAll();
					if(newEditTarget != null) {
						currentEditor = ResEditor.createEditor(newEditTarget, db, previewAsyncBlobServer);
						viewPanel.add(currentEditor.getPanel(), BorderLayout.CENTER);
					}
					viewPanel.revalidate();
					viewPanel.repaint();
				}
			} else {
				newAction.setEnabled(true);
				openAction.setEnabled(true);
				saveAction.setEnabled(false);
				closeAction.setEnabled(false);
				editMenu.setEnabled(false);
				projectMenu.setEnabled(false);
				deleteAction.setEnabled(false);
				createFolderAction.setEnabled(false);
				createAction.setEnabled(false);
				bulkCreateAction.setEnabled(false);
			}
			editTarget = newEditTarget;
		}});
	}
	boolean closeDB(){
		if(db != null && db.unsavedChanges){
			int save = JOptionPane.showConfirmDialog(null, "Save unsaved changes?", "Close with Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
			if(save == JOptionPane.CANCEL_OPTION || save == JOptionPane.CLOSED_OPTION){
				return false;
			}else if(save == JOptionPane.YES_OPTION){
				saveDB();
			}
		}
		if(currentEditor != null){
			currentEditor.cleanup();
			viewPanel.removeAll();
			viewPanel.revalidate();
			viewPanel.repaint();
		}
		super.closeDB();
		return true;
	}
	/**
	 * Null function @see java.awt.event.WindowListener
	 */
	public void windowActivated(WindowEvent e) { }
	/**
	 * Null function @see java.awt.event.WindowListener
	 */
	public void windowClosed(WindowEvent e) { }
	/**
	 * Performs cleanup tasks and exits @see java.awt.event.WindowListener
	 */
	public void windowClosing(WindowEvent e) {
		if(!closeDB()) return;
		cleanup();
	}
	/**
	 * Null function @see java.awt.event.WindowListener
	 */
	public void windowDeactivated(WindowEvent e) { }
	/**
	 * Null function @see java.awt.event.WindowListener
	 */
	public void windowDeiconified(WindowEvent e) { }
	/**
	 * Null function @see java.awt.event.WindowListener
	 */
	public void windowIconified(WindowEvent e) { }
	/**
	 * Null function @see java.awt.event.WindowListener
	 */
	public void windowOpened(WindowEvent e) { }
}
