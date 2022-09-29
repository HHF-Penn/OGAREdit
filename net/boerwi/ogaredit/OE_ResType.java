package net.boerwi.ogaredit;

public enum OE_ResType{
	ALL          ("Resource"),
	IMAGE        ("Image"),
	TEXT         ("Text"),
	LABELSTYLE   ("Label Style"),
	MATTESTYLE   ("Matte Style"),
	AUDIO        ("Audio"),
	FLOORPLAN    ("Floorplan"),
	GALLERY      ("Gallery");
	private final String name;
	OE_ResType(String name){
		this.name = name;
	}
	String getName(){
		return name;
	}
	public String toString(){
		return getName();
	}
	public static OE_ResType fromString(String s){
		switch(s.toLowerCase()){
			case "resource": return ALL;
			case "image": return IMAGE;
			case "text": return TEXT;
			case "label style": return LABELSTYLE;
			case "matte style": return MATTESTYLE;
			case "audio": return AUDIO;
			case "floorplan": return FLOORPLAN;
			case "gallery": return GALLERY;
			default:
				assert false;
				return null;
		}
	}
	boolean compatible(OE_ResType child){
		return compatible(this, child);
	}
	static boolean compatible(OE_ResType parent, OE_ResType child){
		if(parent == null) return false;
		return parent == child || parent == ALL;
	}
}
