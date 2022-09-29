package net.boerwi.ogaredit;

import java.util.*;

import org.json.JSONString;
import org.json.JSONObject;
import org.json.JSONArray;

public class OE_MatteStyle extends OE_Resource {
	public int LAYERCOUNT;
	OE_Color edgeColor = new OE_Color("#000000");
	static class MatteLayer{
		int mmWidth = 0;
		int mmHeight = 0;
		int cdegBevelAngle = 450;
		int mmStandoff = 0;
		OE_Color color;
		public MatteLayer(OE_Color c){
			this.color = c;
		}
		public MatteLayer(JSONObject j){
			this.mmWidth = j.optInt("mmWidth", 0);
			this.mmHeight = j.optInt("mmHeight", 0);
			this.cdegBevelAngle = j.optInt("cdegBevelAngle", 450);
			this.mmStandoff = j.optInt("mmStandoff", 0);
			this.color = new OE_Color(j.optString("color", "#ffffff"));
		}
		public JSONObject toJSON(){
			JSONObject ret = new JSONObject();
			ret.put("mmWidth", mmWidth);
			ret.put("mmHeight", mmHeight);
			ret.put("cdegBevelAngle", cdegBevelAngle);
			ret.put("mmStandoff", mmStandoff);
			ret.put("color", color.getHex(false));
			return ret;
		}
		/**
		 * Predicate. Is this layer configured such that it has no effect on the final matte? This is typically if all dimensions are zero.
		 * @return Is this layer null-presenting?
		 */
		public boolean isNullPresentation(){
			if(mmWidth != 0 || mmHeight != 0 || mmStandoff != 0){
				return false;
			}
			return true;
		}
		public String toJSONString(){
			return toJSON().toString();
		}
		MatteLayer duplicate(){
			return new MatteLayer(this.toJSON());
		}
	}
	public MatteLayer[] layers;
	
	// This is used to provide a minimal default behavior to images in gallery exports without an assigned matte.
	public static OE_MatteStyle GenerateDefaultMatteStyle(){
		OE_MatteStyle ret = new OE_MatteStyle(0);
		ret.edgeColor = new OE_Color("#000000");
		return ret;
	}
	public OE_MatteStyle(){
		this(3);
	}
	public OE_MatteStyle(int layercount){
		super();
		LAYERCOUNT = layercount;
		layers = new MatteLayer[LAYERCOUNT];
		for(int mlidx = 0; mlidx < LAYERCOUNT; mlidx++){
			int bright = 255-(mlidx*(255/LAYERCOUNT)); // Interior (0 index) matte is brighter
			layers[mlidx] = new MatteLayer(new OE_Color(bright,bright,bright,255));
		}
		if(layers.length > 0){
			layers[0].mmWidth = 50;
			layers[0].mmHeight = 50;
			layers[0].mmStandoff = 3;
			layers[0].cdegBevelAngle = 450;
		}
	}
	public OE_MatteStyle(String name, long uid, JSONObject dat){
		super(name, uid);
		JSONArray jslayers = dat.getJSONArray("layers");
		LAYERCOUNT = jslayers.length();
		layers = new MatteLayer[LAYERCOUNT];
		for(int mlidx = 0; mlidx < LAYERCOUNT; mlidx++){
			layers[mlidx] = new MatteLayer(jslayers.getJSONObject(mlidx));
		}
		edgeColor = new OE_Color(dat.optString("edgeColor", "#000000"));
	}
	public void setWidth(int layeridx, int mmWidth){
		if(mmWidth != layers[layeridx].mmWidth){
			signalUnsavedChanges();
			layers[layeridx].mmWidth = mmWidth;
		}
	}
	public void setHeight(int layeridx, int mmHeight){
		if(mmHeight != layers[layeridx].mmHeight){
			signalUnsavedChanges();
			layers[layeridx].mmHeight = mmHeight;
		}
	}
	public void setStandoff(int layeridx, int mmStandoff){
		if(mmStandoff != layers[layeridx].mmStandoff){
			signalUnsavedChanges();
			layers[layeridx].mmStandoff = mmStandoff;
		}
	}
	public void setColor(int layeridx, OE_Color color){
		if(!(color.equals(layers[layeridx].color))){
			signalUnsavedChanges();
			layers[layeridx].color = color;
		}
	}
	public void setEdgeColor(OE_Color c){
		if(!(c.equals(edgeColor))){
			signalUnsavedChanges();
			edgeColor = c;
		}
	}
	public void setBevel(int layeridx, int cdegBevelAngle){
		if(cdegBevelAngle != layers[layeridx].cdegBevelAngle){
			signalUnsavedChanges();
			layers[layeridx].cdegBevelAngle = cdegBevelAngle;
		}
	}
	public OE_Resource duplicate(){
		OE_MatteStyle ret = new OE_MatteStyle(LAYERCOUNT);
		ret.edgeColor = this.edgeColor;
		for(int lidx = 0; lidx < LAYERCOUNT; lidx++){
			ret.layers[lidx] = this.layers[lidx].duplicate();
		}
		commonDuplicateTasks(ret);
		return ret;
	}
	public OE_ResType getType(){ return OE_ResType.MATTESTYLE; }
	public String toJSONString(){
		JSONObject ret = getCoreJson();
		JSONObject dat = (JSONObject)(ret.get("dat"));
		dat.put("edgeColor", edgeColor.getHex(false));
		JSONArray jslayers = new JSONArray();
		for(int lidx = 0; lidx < LAYERCOUNT; lidx++){
			jslayers.put(layers[lidx].toJSON());
		}
		dat.put("layers", jslayers);
		return ret.toString();
	}
}
