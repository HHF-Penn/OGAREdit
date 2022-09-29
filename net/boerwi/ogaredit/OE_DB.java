package net.boerwi.ogaredit;

import org.json.*;

import java.util.*;

import java.sql.*;
import org.sqlite.SQLiteConfig;


import javax.swing.tree.*;
import javax.swing.event.*;
import java.nio.file.*;
import java.io.IOException;

import java.nio.charset.Charset;

/**
* @author martin@boerwi.net
*/
public class OE_DB extends Dir implements TreeModel{
	final Path savePath;
	boolean unsavedChanges = true;
	Dir imageRoot, textRoot, labelStyleRoot, matteStyleRoot, audioRoot, floorplanRoot, galleryRoot;
	OE_BlobMgr blobMgr;
	TreeMap<Long, OE_Resource> uidMap = new TreeMap<>();
	// When loading, references may not exist yet. So we put the idx of the loaded stuff in this array, and then we call 'deferredLoadOperations' when we are done loading.
	ArrayList<long[]> deferredDependencies = new ArrayList<>();
	Connection dbConn;
	public OE_DB(Path p, boolean isNew) {
		super("", null, false, OE_ResType.ALL);
		savePath = p;
		assert p != null;
		imageRoot = new Dir("Image", this, false, OE_ResType.IMAGE);
		textRoot = new Dir("Text", this, false, OE_ResType.TEXT);
		labelStyleRoot = new Dir("Label Style", this, false, OE_ResType.LABELSTYLE);
		matteStyleRoot = new Dir("Matte Style", this, false, OE_ResType.MATTESTYLE);
		audioRoot = new Dir("Audio", this, false, OE_ResType.AUDIO);
		floorplanRoot = new Dir("Floorplan", this, false, OE_ResType.FLOORPLAN);
		galleryRoot = new Dir("Gallery", this, false, OE_ResType.GALLERY);
		addChild(imageRoot);
		addChild(textRoot);
		addChild(labelStyleRoot);
		addChild(matteStyleRoot);
		addChild(audioRoot);
		addChild(floorplanRoot);
		addChild(galleryRoot);
		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.setEncoding(SQLiteConfig.Encoding.getEncoding("UTF-8"));
		try{
			System.out.println("Instantiating DB Connection");
			dbConn = DriverManager.getConnection("jdbc:sqlite:"+savePath, sqliteConfig.toProperties());
			dbConn.setAutoCommit(true);
		}catch(SQLException e){
			assert false : "SQL Exception: "+e;
		}
		executeSQL("PRAGMA foreign_keys = ON;");
		// blobMgr needs to be set up after instantiateDB but before loadDB, depending on which is used.
		if(isNew){
			instantiateDB();
		}
		executeSQL("SAVEPOINT usersave;");
		blobMgr = new OE_DBBlobMgr(dbConn);
		if(!isNew){
			loadDB();
		}
		setUnsavedChanges(false);
	}
	private void executeSQL(String query){
		try{
			System.out.println("About to try query: \""+query+"\"");
			Statement dbStmt = dbConn.createStatement();
			dbStmt.executeUpdate(query);
			dbStmt.close();
		}catch(SQLException e){
			assert false : "SQL Exception: "+e;
		}
	}
	public void cleanup(){
		blobMgr.cleanup();
		executeSQL("ROLLBACK TRANSACTION TO SAVEPOINT usersave;");
		executeSQL("RELEASE SAVEPOINT usersave;");
		try{
			dbConn.close();
		}catch(SQLException e){
			assert false : "Failed to close database: "+e;
		}
	}
	public Path getSavePath(){
		return savePath;
	}
	public void setUnsavedChanges(boolean unsaved){
		if(unsavedChanges != unsaved){
			unsavedChanges = unsaved;
			TMNodeChanged(this);
		}
	}
	public void save(){
		// Delete everything
		executeSQL("DELETE FROM oeresource;");
		executeSQL("DELETE FROM oedirectory;");
		// Iterate through everything
		try{
			PreparedStatement addDir, addRes;
			addDir = dbConn.prepareStatement("INSERT INTO oedirectory (dpath, rtype) VALUES (?, ?);");
			addRes = dbConn.prepareStatement("INSERT INTO oeresource (id, parent, rtype, jsondata) VALUES (?,?,?,?);");
			saveRec(this, "", addDir, addRes);
		}catch(SQLException e){
			assert false : "Error in recsave:"+e;
		}
		blobMgr.vacuumUnused();
		executeSQL("RELEASE SAVEPOINT usersave;");
		executeSQL("SAVEPOINT usersave;");
		setUnsavedChanges(false);
	}
	private void saveRec(Dir c, String p, PreparedStatement addDir, PreparedStatement addRes) throws SQLException{
		System.out.println(c.toString());
		for(int cidx = 0; cidx < c.getChildCount(); cidx++){
			Node child = c.getChild(cidx);
			if(child instanceof Dir){
				String cPath = p+"/"+child.toString();
				addDir.setString(1, cPath);
				addDir.setString(2, ((Dir)child).getType().toString());
				addDir.execute();
				saveRec((Dir)child, cPath, addDir, addRes);
			}else{
				OE_Resource res = ((Entry)child).getData();
				// id
				addRes.setLong(1, res.getId());
				// parent string
				addRes.setString(2, p);
				// rtype
				addRes.setString(3, child.getType().toString());
				// data
				addRes.setString(4, res.toJSONString());
				addRes.execute();
			}
		}
	}
	private int getDBVersion(){
		try{
			Statement stmt = dbConn.createStatement();
			stmt.execute("CREATE TABLE IF NOT EXISTS oeversion (version INTEGER);");
			ResultSet versionResult = stmt.executeQuery("SELECT version FROM oeversion;");
			if(versionResult.next()){
				return versionResult.getInt("version");
			}else{
				// Version 1 didn't have a oeversion table.
				return 1;
			}
		}catch(SQLException e){
			assert false : "Failed to get db version";
		}
		return -1;
	}
	@SuppressWarnings("fallthrough")
	private boolean upgradeDBVersion(){
		final int oeversion = 3;
		int dbversion = getDBVersion();
		try{
			Statement stmt = dbConn.createStatement();
			// This switch statement relies on switch behavior to proceed to subsequent cases. So, if it matches case 1, it will execute the case 1 upgrade, followed by the case 2 upgrade, etc...
			switch(dbversion){
				case 1:
					// Add "Label Style" to table oetype
					stmt.execute("INSERT INTO oetype (rtype) VALUES ('Label Style');");
				case 2:// 2022-07-09; add Matte Styles
					// Set Version to '3'
					stmt.execute("INSERT INTO oetype (rtype) VALUES ('Matte Style');");
					
					stmt.execute("DELETE FROM oeversion;");
					stmt.execute("INSERT INTO oeversion (version) VALUES ("+oeversion+");");
				case 3:
					// Up to date, do nothing
					break;
				default:
					assert false : "Invalid database version number for this software! Maybe your version of OGAREdit is out of date? (Max supported is \""+oeversion+"\", database is version \""+dbversion+"\")";
					return false;
			}
			return true;
		}catch(SQLException e){
			assert false : "Failed to upgrade database";
		}
		return false;
	}
	private void loadDB(){
		if( !upgradeDBVersion() ){
			return;
		}
		try{
			PreparedStatement getDir, getRes;
			getDir = dbConn.prepareStatement("SELECT dpath, rtype FROM oedirectory ORDER BY dpath;");
			getRes = dbConn.prepareStatement("SELECT id, parent, rtype, jsondata FROM oeresource;");
			
			// Populate all the saved directories
			ResultSet dRes = getDir.executeQuery();
			while(dRes.next()){
				String[] dpath = dRes.getString("dpath").split("/");
				//System.out.println(Arrays.toString(dpath));
				OE_ResType rtype = OE_ResType.fromString(dRes.getString("rtype"));
				//dpaths of length 2 (the minimum) and above are custom type directories
				if(dpath.length > 2){
					Dir curr = this;
					for(int idx = 1; idx < dpath.length-1; idx++){
						// This is a safe cast, because no resources have been loaded yet
						curr = (Dir)curr.getChildByName(dpath[idx]);
						assert curr != null : "Invalid directory string: "+Arrays.toString(dpath);
					}
					curr.addChild(new Dir(dpath[dpath.length-1], curr, true, rtype));
				}
			}
			dRes.close();
			
			// Populate all the saved resources
			ResultSet rRes = getRes.executeQuery();
			while(rRes.next()){
				long id = rRes.getLong("id");
				String[] parentPath = rRes.getString("parent").split("/");
				OE_ResType rtype = OE_ResType.fromString(rRes.getString("rtype"));
				String jsonData = rRes.getString("jsondata");
				Dir curr = this;
				for(int idx = 1; idx < parentPath.length; idx++){
					curr = (Dir)curr.getChildByName(parentPath[idx]);
					assert curr != null : "Invalid directory string: "+Arrays.toString(parentPath);
				}
				OE_Resource loaded = OE_Resource.loadFromJson(id, jsonData, rtype, blobMgr, this);
				curr.addChild(new Entry(loaded, rtype));
			}
			OE_Resource.deferredLoadOperations(this);
			rRes.close();
			// Cleanup
			getDir.close();
			getRes.close();
		}catch(SQLException e){
			assert false : "Error loading from file: "+e;
		}
	}
	private void instantiateDB(){
		String dbDefSql = null;
		try{
			dbDefSql = new String(this.getClass().getClassLoader().getResourceAsStream("resource/dbDef.sql").readAllBytes(), Charset.forName("UTF-8"));
		}catch(IOException e){
			assert false : "Couldn't load embedded resource";
		}
		for(String query : dbDefSql.split(";")){
			executeSQL(query+";");
		}
	}
	public void TMAddChild(Node element){
		TreeModelEvent e = createTMEvent(element);
		for(TreeModelListener l : tmListeners){
			l.treeNodesInserted(e);
		}
	}
	public OE_BlobMgr getBlobMgr(){
		return blobMgr;
	}
	public Dir getRootForType(OE_ResType type){
		switch(type){
			case ALL:
				return this;
			case IMAGE:
				return imageRoot;
			case TEXT:
				return textRoot;
			case LABELSTYLE:
				return labelStyleRoot;
			case MATTESTYLE:
				return matteStyleRoot;
			case AUDIO:
				return audioRoot;
			case FLOORPLAN:
				return floorplanRoot;
			case GALLERY:
				return galleryRoot;
			default:
				assert false : "Unimplemented resource selector for OE_ResType";
		}
		return null;
	}
	public void createResource(OE_ResType type, Dir target){
		if(!target.canAddChildren()) return;
		OE_Resource res = null;
		switch(type){
			case IMAGE: res = new OE_Image();
				break;
			case TEXT: res = new OE_Text();
				break;
			case LABELSTYLE: res = new OE_LabelStyle();
				break;
			case MATTESTYLE: res = new OE_MatteStyle();
				break;
			case AUDIO: res = new OE_Audio();
				break;
			case FLOORPLAN: res = new OE_Floorplan();
				break;
			case GALLERY: res = new OE_Gallery();
				break;
			case ALL:
			default:
				assert false : "invalid res type";
		}
		res.setOwnerDB(this);
		Entry added = new Entry(res, type);
		added.setName(target.uniqifyChildName("Unnamed "+type.toString(), null));
		target.addChild(added);
		TMAddChild(added);
		setUnsavedChanges(true);
	}
	public void createSubDir(Dir target, String name){
		assert target.canAddChildren();
		Dir child = new Dir(target.uniqifyChildName(name, null), target, true, target.type);
		target.addChild(child);
		TMAddChild(child);
		setUnsavedChanges(true);
	}
	public void addResource(OE_Resource res, Dir parent){
		res.setName(parent.uniqifyChildName(res.toString(), null));
		Entry child = new Entry(res, res.getType());
		parent.addChild(child);
		res.setOwnerDB(this);
		TMAddChild(child);
		setUnsavedChanges(true);
	}
	public void duplicate(Entry target){
		Entry dup = new Entry(target.getData().duplicate(), target.getType());
		dup.setName(target.getParent().uniqifyChildName(target.toString(), null));
		target.getParent().addChild(dup);
		TMAddChild(dup);
		setUnsavedChanges(true);
	}
	public void moveNode(Node target, Dir destination){
		TreeModelEvent evt = createTMEvent(target);
		target.getParent().dereferenceChild(target);
		TMNodeRemoved(evt);
		target.setName(destination.uniqifyChildName(target.toString(), null));
		destination.addChild(target);
		TMAddChild(target);
		setUnsavedChanges(true);
	}
	TreeModelEvent createTMEvent(Node target){
		TreeModelEvent ret;
		assert target != null;
		Dir parent = target.getParent();
		System.out.println(parent);
		ArrayList<Node> pPath = new ArrayList<Node>();
		int[] cIdx;
		if(parent != null){
			pPath.addAll(parent.getPath());
			cIdx = new int[]{parent.getIndexOfChild(target)};
			Object[] targetArray = new Object[]{target};
			ret = new TreeModelEvent(this, new TreePath(pPath.toArray()), cIdx, targetArray);
		}else{ //special case for root node (us)
			ret = new TreeModelEvent(this, (TreePath)null, null, null);
		}
		System.out.println(ret);
		return ret;
	}
	void TMNodeChanged(Node target){
		TreeModelEvent e = createTMEvent(target);
		for(TreeModelListener l : tmListeners){
			l.treeNodesChanged(e);
		}
	}
	void TMNodeRemoved(TreeModelEvent evt){ //You need to generate the event before removing the node, then pass it to me
		for(TreeModelListener l : tmListeners){
			l.treeNodesRemoved(evt);
		}
	}
	boolean remove(Collection<Node> targets){
		System.out.println("Removing: "+targets);
		//check all of our items are mutable. This assumes that all sub-nodes are editable too.
		for(Node t : targets){
			if( !t.isEditable() ){
				return false;
			}
		}
		for(Node t : targets){
			Dir parent = t.parent;
			if(parent == null) continue; //we probably already removed this guy's parents.
			TreeModelEvent evt = createTMEvent(t);
			parent.removeChild(t);
			TMNodeRemoved(evt);
		}
		setUnsavedChanges(true);
		return true;
	}
	// INode Stuff
	public boolean isEditable(){ return false; }
	public boolean canAddChildren(){ return false; }
	public void setName(String newName){ }
	public void removeChild(Node child){ }
	public void remove(){ assert false : "Attempting to remove root db node"; }
	// TreeModel Stuff
	ArrayList<TreeModelListener> tmListeners = new ArrayList<>();
	public void addTreeModelListener(TreeModelListener l){
		tmListeners.add(l);
	}
	public Object getChild(Object parent, int index){
		return ((Dir)parent).getChild(index);
	}
	public int getChildCount(Object parent){
		if(parent instanceof Dir){
			return ((Dir)parent).getChildCount();
		}else{
			return 0;
		}
	}
	public int getIndexOfChild(Object parent, Object child){
		return ((Dir)parent).getIndexOfChild((Node)child);
	}
	public Object getRoot(){
		return this;
	}
	public boolean isLeaf(Object node){
		return ((Node)node).isLeaf();
	}
	public void removeTreeModelListener(TreeModelListener l){
		tmListeners.remove(l);
	}
	public void valueForPathChanged(TreePath path, Object newValue){
		assert newValue instanceof String;
		Node target = (Node)path.getLastPathComponent();
		if(target.isEditable()){
			target.setName(target.getParent().uniqifyChildName((String)newValue, target));
			TMNodeChanged(target);
			setUnsavedChanges(true);
		}
	}
	public String toString(){
		Path p = getSavePath();
		String ret;
		if(p != null){
			ret = p.getFileName().toString();
		}else{
			ret = "Unnamed Database";
		}
		ret += (unsavedChanges ? "*" : "");
		return ret;
	}
}
abstract class Node {
	OE_ResType type;
	Dir parent;
	public Node(OE_ResType type){
		this.type = type;
	}
	abstract void setName(String name);
	void setParent(Dir parent){
		if(isEditable()){
			this.parent = parent;
		}
	}
	Dir getParent(){
		return parent;
	}
	OE_ResType getType(){
		return type;
	}
	abstract boolean isLeaf();
	abstract boolean isEditable();
	public ArrayList<Node> getPath(){
		ArrayList<Node> ret = new ArrayList<>();
		Node p = this;
		while(p != null){
			ret.add(0, p);
			p = p.getParent();
		}
		return ret;
	}
}

class Dir extends Node{
	ArrayList<Node> children;
	boolean editable = true;
	String name;
	public Dir(String name, Dir parent, boolean editable, OE_ResType type){
		super(type);
		setName(name);
		setParent(parent);
		this.editable = editable;
		children = new ArrayList<>();
	}
	void setName(String name){
		if(isEditable()){
			this.name = name;
		}
	}
	String uniqifyChildName(String test, Node except){
		// Names can't contain many characters
		test = test.replaceAll("[^a-zA-Z0-9_\\.\\-() ]", "_");
		TreeSet<String> names = new TreeSet<>();
		for(Node child : children){
			if(child == except) continue;
			names.add(child.toString());
		}
		// This name is not taken
		if(!names.contains(test)) return test;
		// This name is taken
		int suffix = 1;
		while(names.contains(test+"_"+suffix)){
			suffix++;
		}
		return test+"_"+suffix;
	}
	boolean isEditable(){ return editable; }
	boolean canAddChildren(){ return true; }
	Node getChild(int index){
		return children.get(index);
	}
	Node getChildByName(String name){
		for(Node c : children){
			if(c.toString().equals(name)){
				return c;
			}
		}
		return null;
	}
	LinkedHashSet<Node> getAllChildren(){
		LinkedHashSet<Node> ret = new LinkedHashSet<>(children);
		for(Node child : children){
			if(child instanceof Dir){
				ret.addAll(((Dir)child).getAllChildren());
			}
		}
		return ret;
	}
	void removeChild(Node child){
		if(child instanceof Dir){
			for(int idx = 0; idx < ((Dir)child).getChildCount(); idx++){
				((Dir)child).removeChild(idx);
			}
		}else{
			((Entry)child).getData().cleanup();
		}
		child.setParent(null);
		children.remove(child);
	}
	void removeChild(int childIdx){
		removeChild(children.get(childIdx));
	}
	void dereferenceChild(Node child){
		child.setParent(null);
		children.remove(child);
	}
	int getChildCount(){
		return children.size();
	}
	int getIndexOfChild(Node child){
		int ret = children.indexOf(child);
		assert ret >= 0 : "getIndexOfChild on non-existant child";
		return ret;
	}
	boolean addChild(Node child){
		if(!this.type.compatible(child.type)) return false;
		child.setParent(this);
		children.add(child);
		return true;
	}
	public boolean isLeaf(){ return false; }
	public String toString(){
		if(name == null){
			return "Unnamed Directory";
		}else{
			return name;
		}
	}
}
class Entry extends Node{
	OE_Resource data;
	public Entry(OE_Resource data, OE_ResType type){
		super(type);
		this.data = data;
	}
	void setName(String name){
		data.setName(name);
	}
	OE_Resource getData(){ return data; }
	boolean isLeaf(){ return true; }
	boolean isEditable(){ return true; }
	public String toString(){
		assert data != null;
		return data.toString();
	}
}
