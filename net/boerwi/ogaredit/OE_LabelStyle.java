package net.boerwi.ogaredit;

import java.util.*;

import org.json.JSONString;
import org.json.JSONObject;
import org.json.JSONArray;

public class OE_LabelStyle extends OE_Resource {
	public enum HAlignment{
		CENTER    ("CENTER"),
		LEFT      ("LEFT"),
		RIGHT     ("RIGHT"),
		JUSTIFIED ("JUSTIFIED");
		private final String name;
		HAlignment(String name){
			this.name = name;
		}
		public String toString(){
			return name;
		}
		public static HAlignment fromString(String s){
			switch(s){
				case "CENTER": return CENTER;
				case "LEFT": return LEFT;
				case "RIGHT": return RIGHT;
				case "JUSTIFIED": return JUSTIFIED;
				default:
					assert false : "invalid name";
					return null;
			}
		}
	}
	public enum VAlignment{
		TOP       ("TOP"),
		CENTER    ("CENTER"),
		BOTTOM    ("BOTTOM");
		private final String name;
		VAlignment(String name){
			this.name = name;
		}
		public String toString(){
			return name;
		}
		public static VAlignment fromString(String s){
			switch(s){
				case "TOP": return TOP;
				case "CENTER": return CENTER;
				case "BOTTOM": return BOTTOM;
				default:
					assert false : "invalid name";
					return null;
			}
		}
	}
	public enum Cardinal{
		NORTH ("NORTH"),
		SOUTH ("SOUTH"),
		EAST  ("EAST"),
		WEST  ("WEST");
		private final String name;
		Cardinal(String name){
			this.name = name;
		}
		public String toString(){
			return name;
		}
		public static Cardinal fromString(String s){
			switch(s){
				case "NORTH": return NORTH;
				case "SOUTH": return SOUTH;
				case "EAST": return EAST;
				case "WEST": return WEST;
				default:
					assert false : "invalid name";
					return null;
			}
		}
	}

	private Cardinal textSide;
	private VAlignment vAlign;
	private OE_Color bgColor;
	private final LinkedHashMap<String, TextStyle> styles = new LinkedHashMap<>();
	
	public OE_LabelStyle(){
		super();
		textSide = Cardinal.EAST;
		vAlign = VAlignment.CENTER;
		bgColor = new OE_Color("#FAFAFA");
		setDefaultStyles();
	}
	public OE_LabelStyle(String name, long uid, JSONObject dat){
		super(name, uid);
		textSide = Cardinal.fromString(dat.getString("textSide"));
		vAlign = VAlignment.fromString(dat.getString("vAlign"));
		bgColor = new OE_Color(dat.getString("bgColor"));
		setDefaultStyles();
		JSONObject jStyles = dat.getJSONObject("styles");
		for(String styleKey : jStyles.keySet()){
			styles.put(styleKey, new TextStyle(jStyles.getJSONObject(styleKey)));
		}
	}
	private void setDefaultStyles(){
		styles.put("<h1>", new TextStyle(new OE_Color("#202060"), HAlignment.CENTER, 18, true, false, true));
		styles.put("<h2>", new TextStyle(new OE_Color("#202020"), HAlignment.LEFT, 14, false, true, false));
		styles.put("DEFAULT", new TextStyle(new OE_Color("#202020"), HAlignment.JUSTIFIED, 12, false, false, false));
		styles.put("<small>", new TextStyle(new OE_Color("#202020"), HAlignment.LEFT, 9, false, false, false));
	}
	public Set<Map.Entry<String, TextStyle>> getStyles(){
		return styles.entrySet();
	}
	public void setStyle(String name, TextStyle s){
		if(!s.equals(styles.get(name))){
			styles.put(name, s);
			signalUnsavedChanges();
		}
	}
	public TextStyle getStyle(String name){
		return styles.get(name);
	}
	public void setVAlign(VAlignment a){
		if(vAlign != a){
			vAlign = a;
			signalUnsavedChanges();
		}
	}
	public VAlignment getVAlign(){
		return vAlign;
	}
	public void setTextSide(Cardinal c){
		if(textSide != c){
			textSide = c;
			signalUnsavedChanges();
		}
	}
	public Cardinal getTextSide(){
		return textSide;
	}
	public void setBGColor(OE_Color c){
		if(!c.equals(bgColor)){
			bgColor = c;
			signalUnsavedChanges();
		}
	}
	public OE_Color getBGColor(){
		return bgColor;
	}
	public OE_Resource duplicate(){
		OE_LabelStyle ret = new OE_LabelStyle();
		ret.textSide = textSide;
		ret.vAlign = vAlign;
		ret.bgColor = bgColor;
		ret.styles.clear();
		for(Map.Entry<String, TextStyle> entry : styles.entrySet()){
			ret.styles.put(entry.getKey(), entry.getValue());
		}
		commonDuplicateTasks(ret);
		return ret;
	}
	public OE_ResType getType(){ return OE_ResType.LABELSTYLE; }
	public JSONObject toGalleryDefJson(JSONObject dat){
		if(dat == null){
			dat = new JSONObject();
		}
		dat.put("textSide", textSide.toString());
		dat.put("vAlign", vAlign.toString());
		dat.put("bgColor", bgColor.getHex(false));
		JSONObject jStyles = new JSONObject();
		dat.put("styles", jStyles);
		for(Map.Entry<String, TextStyle> entry : styles.entrySet()){
			jStyles.put(entry.getKey(), entry.getValue());
		}
		return dat;
	}
	public String toJSONString(){
		JSONObject ret = getCoreJson();
		JSONObject dat = (JSONObject)(ret.get("dat"));
		toGalleryDefJson(dat);
		return ret.toString();
	}
	public class TextStyle implements JSONString{
		public static class RichStyle{
			private final int s;
			RichStyle(int s){
				this.s = s;
			}
			public static RichStyle getStyle(boolean underline, boolean italic, boolean bold){
				int ret = 0;
				if(underline) ret |= 1;
				if(italic) ret |= 2;
				if(bold) ret |= 4;
				return new RichStyle(ret);
			}
			public static RichStyle unionStyles(RichStyle s1, RichStyle s2){
				return new RichStyle(s1.s | s2.s);
			}
			public static RichStyle diffStyles(RichStyle s1, RichStyle s2){
				return new RichStyle(s1.s - s2.s);
			}
			public boolean isUnderline(){
				return (1 & s) > 0;
			}
			public boolean isItalic(){
				return (2 & s) > 0;
			}
			public boolean isBold(){
				return (4 & s) > 0;
			}
			@Override
			public boolean equals(Object other){
				if(other instanceof RichStyle){
					return s == ((RichStyle)other).s;
				}
				return false;
			}
			public int hashCode(){
				return s;
			}
		}
		private final OE_Color color;
		private final HAlignment hAlign;
		private final double ptFont;
		private final RichStyle style;
		
		public TextStyle(OE_Color c, HAlignment hAlign, double ptFont, boolean underline, boolean italic, boolean bold){
			color = c;
			this.ptFont = ptFont;
			this.hAlign = hAlign;
			style = RichStyle.getStyle(underline, italic, bold);
		}
		public TextStyle(JSONObject dat){
			hAlign = HAlignment.fromString(dat.getString("hAlign"));
			ptFont = dat.getInt("ptFont");
			color = new OE_Color(dat.getString("color"));
			style = RichStyle.getStyle(dat.optBoolean("underline", false),
				dat.optBoolean("italic", false),
				dat.optBoolean("bold", false));
		}
		public OE_Color getColor(){
			return color;
		}
		public HAlignment getHAlign(){
			return hAlign;
		}
		public double getPtSize(){
			return ptFont;
		}
		public RichStyle getStyle(){
			return style;
		}
		public String toJSONString(){
			JSONObject ret = new JSONObject();
			ret.put("hAlign", hAlign.toString());
			ret.put("ptFont", ptFont);
			ret.put("color", color.getHex(false));
			if(style.isBold()) ret.put("bold", true);
			if(style.isItalic()) ret.put("italic", true);
			if(style.isUnderline()) ret.put("underline", true);
			return ret.toString();
		}
		@Override
		public boolean equals(Object other){
			if(other instanceof TextStyle){
				TextStyle o = (TextStyle)other;
				return (
					color.equals(o.color) &&
					hAlign == o.hAlign &&
					ptFont == o.ptFont &&
					style.equals(o.style)
				);
			}
			return false;
		}
		public int hashCode(){
			return color.hashCode()^style.hashCode()^hAlign.hashCode()^(int)(ptFont*10);
		}
	}
}
