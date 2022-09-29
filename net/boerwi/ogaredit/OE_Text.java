package net.boerwi.ogaredit;

import java.util.*;

import org.json.JSONString;
import org.json.JSONObject;
import org.json.JSONArray;

public class OE_Text extends OE_Resource {
	private String data;
	public OE_Text() {
		super();
		data = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. [Delta]Δ [e grave]è [A umlaut]Ä [Eszett]ß [Person, Jp]人 [D, Cyrillic]Д";
	}
	public OE_Text(String name, long uid, JSONObject dat){
		super(name, uid);
		data = dat.getString("text");
	}
	public OE_Resource duplicate(){
		OE_Text ret = new OE_Text();
		ret.data = data;
		commonDuplicateTasks(ret);
		return ret;
	}
	public String getData() {
		return data;
	}
	// Returns the data with certain tags substituted based on the target image
	public String getSubstitutedData(OE_Image target){
		String ret = data;
		for(Map.Entry<String, String> itag : target.tags.entrySet()){
			String patt = String.format("(?i)<%s>", itag.getKey());
			ret = ret.replaceAll(patt, itag.getValue());
		}
		return ret;
	}
	public void setData(String val) {
		if(val.equals(data)) return;
		data = val;
		signalUnsavedChanges();
	}
	public OE_ResType getType(){ return OE_ResType.TEXT; }
	public String toJSONString(){
		JSONObject ret = getCoreJson();
		JSONObject dat = (JSONObject)(ret.get("dat"));
		dat.put("text", data);
		return ret.toString();
	}
}
