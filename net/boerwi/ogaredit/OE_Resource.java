package net.boerwi.ogaredit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.HashSet;
import javax.swing.JPanel;

import java.io.InputStream;

import org.json.JSONString;
import org.json.JSONObject;
import org.json.JSONArray;

import net.boerwi.snowfluke.Snowfluke;

public abstract class OE_Resource implements JSONString{
	String name;
	long uid;
	static Iterator<Long> uidGenerator = Snowfluke.getGenerator();
	HashSet<OE_Resource> dependents = new HashSet<>();
	HashSet<OE_Resource> dependency = new HashSet<>();
	//Used for signalling unsaved changes.
	OE_DB ownerdb = null;
	public OE_Resource() {
		this(null, null);
	}
	public OE_Resource(String name, Long uid) {
		if(name == null){
			this.name = "Unnamed Resource";
		}else{
			this.name = name;
		}
		if(uid == null){
			this.uid = uidGenerator.next();
		}else{
			this.uid = uid;
		}
	}
	abstract public OE_Resource duplicate();
	void commonDuplicateTasks(OE_Resource dup){
		for(OE_Resource dep : dependency){
			dep.addDependent(dup);
		}
		dup.setOwnerDB(ownerdb);
	}
	public void cleanup(){
		ownerdb.uidMap.remove(uid);
		for(OE_Resource parent : getDependencies()){
			parent.removeDependent(this);
		}
		for(OE_Resource child : getDependents()){
			//FIXME this can create some invalid state, but it is assumed that that invalid state will only occur temporarily when performing a bulk delete. - BUT we should check it, somehow. Maybe some static OE_Resource 'marked for deletion' set?
			removeDependent(child);
		}
	}
	public long getId(){
		return this.uid;
	}
	public String getUniqueName(){
		return toString()+"_"+getId();
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public String toString() {
		return getName();
	}
	abstract public OE_ResType getType();
	public void setOwnerDB(OE_DB ownerdb){
		this.ownerdb = ownerdb;
		ownerdb.uidMap.put(uid, this);
	}
	public void informDependencyChanged(OE_Resource dep){}
	public int getDependentCount() {
		return dependents.size();
	}
	public OE_Resource[] getDependents(){
		return dependents.toArray(new OE_Resource[0]);
	}
	public OE_Resource[] getDependencies(){
		return dependency.toArray(new OE_Resource[0]);
	}
	public void addDependent(OE_Resource dep){
		dependents.add(dep);
		dep.dependency.add(this);
	}
	public void removeDependent(OE_Resource dep){
		dependents.remove(dep);
		dep.dependency.remove(this);
	}
	public static void deferredLoadOperations(OE_DB db){
		// Fix deferred dependencies
		for(long[] def : db.deferredDependencies){
			assert def.length == 2 : "each dep must be an array [dependency, dependent]";
			db.uidMap.get(def[0]).addDependent(db.uidMap.get(def[1]));
		}
		db.deferredDependencies.clear();
		for(OE_Resource r : db.uidMap.values()){
			r.loadFinalize();
		}
	}
	// Operations that require other resources to be loaded can be overridden into this function
	public void loadFinalize(){}
	void signalUnsavedChanges(){
		if(ownerdb != null){
			ownerdb.setUnsavedChanges(true);//FIXME put myself in a list of things that need changes saved
			// Thread.dumpStack();
		}
		for(OE_Resource dep : dependents){
			dep.informDependencyChanged(this);
		}
	}
	JSONObject getCoreJson(){
		JSONObject ret = new JSONObject();
		ret.put("name", name);
		ret.put("uid", uid);
		JSONArray dep = new JSONArray();
		for(OE_Resource d : dependents){
			dep.put(d.uid);
		}
		ret.put("dep", dep);
		ret.put("dat", new JSONObject());
		return ret;
	}
	public static OE_Resource createFromData(String name, InputStream data, OE_ResType type, OE_BlobMgr blobMgr){
		if(data == null) return null;
		OE_Resource retRes = null;
		if(type == OE_ResType.IMAGE){
			OE_Blob dataBlob = blobMgr.getBlob(blobMgr.addBlob(data, "BULKIMG__"+name));
			OE_Image ret = new OE_Image();
			ret.setBlob(dataBlob);
			retRes = ret;
		}else if(type == OE_ResType.TEXT){
			String dataString;
			try{
				dataString = new String(data.readAllBytes(), "UTF-8");
			}catch(Exception e){
				return null;
			}
			OE_Text ret = new OE_Text();
			ret.setData(dataString);
			retRes = ret;
		}else if(type == OE_ResType.AUDIO){
			OE_Blob dataBlob = blobMgr.getBlob(blobMgr.addBlob(data, "BULKAUD__"+name));
			OE_Audio ret = new OE_Audio();
			ret.setBlob(dataBlob);
			retRes = ret;
		}
		if(retRes != null){
			retRes.setName(name);
		}
		return retRes;
	}
	public static OE_Resource loadFromJson(Long id, String jsonStr, OE_ResType type, OE_BlobMgr blobMgr, OE_DB ownerdb){
		if(id == null){
			id = uidGenerator.next();
		}
		assert jsonStr != null : "Cannot load from null json string";
		JSONObject j = new JSONObject(jsonStr);
		String name = j.getString("name");
		assert j.getLong("uid") == id;
		JSONArray dep = j.getJSONArray("dep");
		JSONObject dat = j.getJSONObject("dat");
		OE_Resource ret = null;
		switch(type){
			case IMAGE: ret = new OE_Image(name, id, dat, blobMgr);
				break;
			case TEXT: ret = new OE_Text(name, id, dat);
				break;
			case LABELSTYLE: ret = new OE_LabelStyle(name, id, dat);
				break;
			case MATTESTYLE: ret = new OE_MatteStyle(name, id, dat);
				break;
			case AUDIO: ret = new OE_Audio(name, id, dat, blobMgr);
				break;
			case FLOORPLAN: ret = new OE_Floorplan(name, id, dat);
				break;
			case GALLERY: ret = new OE_Gallery(name, id, dat);
				break;
			default:
				assert false : "Invalid type";
		}
		if(ret != null) ret.setOwnerDB(ownerdb);
		for(int depIdx = 0; depIdx < dep.length(); depIdx++){
			ownerdb.deferredDependencies.add(new long[]{id, dep.getLong(depIdx)});
		}
		return ret;
	}
}
