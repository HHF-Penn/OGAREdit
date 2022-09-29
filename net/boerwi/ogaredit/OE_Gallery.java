package net.boerwi.ogaredit;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.*;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;


import org.json.JSONString;
import org.json.JSONObject;
import org.json.JSONArray;

import net.boerwi.convexhull.CHPoint;

public class OE_Gallery extends OE_Resource {
	long loadFinalize_fpId;
	JSONObject loadFinalize_art; //TODO put loadFinalize_* not as properties of OE_Gallery
	OE_Floorplan fp = null;
	boolean clickToExamine = false;
	boolean autoplayAudio = false;
	double pxPerMMExport = 1.2;
	final TreeMap<String, OE_Color> colorMap = new TreeMap<>(defaultColors);
	final LinkedHashMap<GV_Line, ArrayList<ArtPlacement>> art = new LinkedHashMap<>();
	static final TreeMap<String, OE_Color> defaultColors = new TreeMap<>(Map.of("Floor", new OE_Color("A3A3B1"), "Walls", new OE_Color("E2E2D1"), "Ceiling", new OE_Color("262626")));
	public OE_Gallery(){
		super();
	}
	public OE_Gallery(String name, long uid, JSONObject dat){
		super(name, uid);
		// Load colors
		JSONObject jColorMap = dat.getJSONObject("colors");
		for(String colorKey : jColorMap.keySet()){
			colorMap.put(colorKey, new OE_Color(jColorMap.getString(colorKey)));
		}
		clickToExamine = dat.optBoolean("clickExamine", false);
		autoplayAudio = dat.optBoolean("autoplayAudio", false);
		pxPerMMExport = dat.optDouble("pxPerMM", 1.2);
		loadFinalize_fpId = dat.optLong("floorplan", -1l);
		loadFinalize_art = dat.getJSONObject("art");
	}
	public OE_Resource duplicate(){
		OE_Gallery ret = new OE_Gallery();
		// Copy colors
		for(String colorKey : colorMap.keySet()){
			ret.colorMap.put(colorKey, colorMap.get(colorKey));
		}
		// Copy Floorplan
		ret.fp = fp;
		// Copy Click-To-Examine
		ret.clickToExamine = clickToExamine;
		ret.autoplayAudio = autoplayAudio;
		// Copy Art placement
		for(Map.Entry<GV_Line, ArrayList<ArtPlacement>> aWallEntry : art.entrySet()){
			ArrayList<ArtPlacement> dupPlacement = new ArrayList<>();
			ret.art.put(aWallEntry.getKey(), dupPlacement);
			for(ArtPlacement aPlace : aWallEntry.getValue()){
				dupPlacement.add(new ArtPlacement(aPlace));
			}
		}
		commonDuplicateTasks(ret);
		return ret;
	}
	public OE_ResType getType(){ return OE_ResType.GALLERY; }
	public void loadFinalize(){
		if(loadFinalize_fpId != -1){
			fp = (OE_Floorplan)ownerdb.uidMap.get(loadFinalize_fpId);
		}
		for(String artWallIdxStr : loadFinalize_art.keySet()){
			GV_Line aWall = fp.getWallAtIndex(Integer.parseInt(artWallIdxStr));
			ArrayList<ArtPlacement> artOnWall = new ArrayList<>();
			art.put(aWall, artOnWall);
			JSONArray artOnWallArray = loadFinalize_art.getJSONArray(artWallIdxStr);
			for(int idx = 0; idx < artOnWallArray.length(); idx++){
				artOnWall.add(new ArtPlacement(artOnWallArray.getJSONObject(idx), ownerdb));
			}
		}
		rebuildDependencies();
	}
	public Set<String> getColorNames(){
		return colorMap.keySet();
	}
	public void setColor(String name, String hex){
		OE_Color newColor = new OE_Color(hex);
		if(!newColor.equals(colorMap.get(name))){
			colorMap.put(name, new OE_Color(hex));
			signalUnsavedChanges();
		}
	}
	public OE_Color getColor(String name){
		return colorMap.get(name);
	}
	public boolean getCanClickToExamine(){
		return clickToExamine;
	}
	public void setCanClickToExamine(boolean c){
		if(c != clickToExamine){
			clickToExamine = c;
			if(clickToExamine == false){
				setAutoplayAudio(false);
			}
			signalUnsavedChanges();
		}
	}
	public boolean getAutoplayAudio(){
		return autoplayAudio;
	}
	public void setAutoplayAudio(boolean a){
		if(a != autoplayAudio){
			autoplayAudio = a;
			signalUnsavedChanges();
		}
	}
	public double getPxPerMM(){
		return pxPerMMExport;
	}
	public void setPxPerMM(double v){
		if(pxPerMMExport != v){
			pxPerMMExport = v;
			signalUnsavedChanges();
		}
	}
	public void setFloorplan(OE_Floorplan fp){
		if(fp != this.fp){
			art.clear();
			if(this.fp != null){
				this.fp.removeDependent(this);
			}
			if(fp != null){
				fp.addDependent(this);
			}
			this.fp = fp;
			signalUnsavedChanges();
		}
	}
	public OE_Floorplan getFloorplan(){
		return fp;
	}
	public ArtPlacement addArt(GV_Line wall, boolean onLeft, OE_Image img){
		ArrayList<ArtPlacement> artArray = art.get(wall);
		if(artArray == null){
			artArray = new ArrayList<ArtPlacement>();
			art.put(wall, artArray);
		}
		ArtPlacement newArt = new ArtPlacement(img, onLeft);
		artArray.add(newArt);
		img.addDependent(this);
		signalUnsavedChanges();
		return newArt;
	}
	public void delArt(GV_Line wall, ArtPlacement artp){
		art.get(wall).remove(artp);
		rebuildDependencies();
		signalUnsavedChanges();
	}
	public void relocateArt(ArtPlacement a, double cx, double cy, double scale){
		a.scale = scale;
		relocateArt(a, cx, cy);
	}
	public void relocateArt(ArtPlacement a, double cx, double cy){
		if(cx > 1.0) cx = 1.0;
		if(cy > 1.0) cy = 1.0;
		if(cx < 0.0) cx = 0.0;
		if(cy < 0.0) cy = 0.0;
		a.cx = cx;
		a.cy = cy;
		signalUnsavedChanges();
	}
	public void associateRes(ArtPlacement a, OE_Text targ, OE_LabelStyle style){
		if(targ == a.text && style == a.labelStyle) return;
		a.text = targ;
		a.labelStyle = style;
		rebuildDependencies();
		signalUnsavedChanges();
	}
	public void associateRes(ArtPlacement a, OE_Audio targ){
		if(targ == a.audio) return;
		a.audio = targ;
		rebuildDependencies();
		signalUnsavedChanges();
	}
	public void associateRes(ArtPlacement a, OE_MatteStyle style){
		if(style == a.matteStyle) return;
		a.matteStyle = style;
		rebuildDependencies();
		signalUnsavedChanges();
	}
	public ArtPlacement[] getAllArtPlacements(){
		ArtPlacement[] ret = new ArtPlacement[0];
		ArrayList<ArtPlacement> artArray = new ArrayList<>();
		for(ArrayList<ArtPlacement> aWallArt : art.values()){
			artArray.addAll(aWallArt);
		}
		// System.out.println("Art Placement Count: "+artArray.size());
		return artArray.toArray(ret);
	}
	public ArtPlacement[] getArtOnWall(GV_Line wall){
		ArtPlacement[] ret = new ArtPlacement[0];
		ArrayList<ArtPlacement> artArray = art.get(wall);
		if(artArray != null){
			return artArray.toArray(ret);
		}
		return ret;
	}
	JSONObject generateGalleryJson(String prefix, TextureAtlas atlas, HashMap<OE_Color, Integer> texcolormap){
		if(fp == null) return null;
		JSONObject ret = new JSONObject();
		JSONObject patron = new JSONObject();
		JSONArray wallGroups = new JSONArray();
		JSONArray regions = new JSONArray();
		JSONObject artdef = new JSONObject();
		JSONArray artplace = new JSONArray();
		JSONArray matte = new JSONArray();
		ret.put("galleryID", getId());
		ret.put("patron", patron);
		ret.put("wallHeight", fp.getWallHeight()/100.0);
		ret.put("walls", wallGroups);
		ret.put("regions", regions);
		ret.put("art", artdef);
		ret.put("artPlacement", artplace);
		ret.put("matte", matte);
		ret.put("texture", prefix+"tex.png");
		ret.put("clickExamine", getCanClickToExamine());
		if(getCanClickToExamine()){
			ret.put("autoplayAudio", getAutoplayAudio());
		}
		ret.put("atlasCellSize", atlas.getCellSize());
		// Populate Patron
		patron.put("height", fp.getEyeHeight()/100.0);
		patron.put("start", new double[]{fp.getAvatar().getX()/1000.0, fp.getAvatar().getY()/1000.0});
		patron.put("dir", fp.getAvatar().ddAngle/10.0);
		// Fixed, for now.
		patron.put("readingDistance", 2.0);
		
		// Populate regions
		for(GV_Hull reg : fp.regions){
			if(reg.hull.getHull().size() < 3){
				continue;
			}
			JSONArray hull = new JSONArray();
			for(CHPoint hullPt : reg.hull.getHull()){
				hull.put(hullPt.getX()/1000.0);
				hull.put(hullPt.getY()/1000.0);
			}
			JSONObject jreg = new JSONObject();
			regions.put(jreg);
			jreg.put("name", reg.getName().replaceAll("[,;]", "_"));
			jreg.put("hull", hull);
		}
		
		// Populate walls
		//  Walls represents our remaining walls to add.
		ArrayList<GV_Line> walls = new ArrayList<>();
		if(fp.isThickWalls()){
			walls.addAll(Arrays.asList(fp.getThickWalls()));
		}else{
			walls.addAll(fp.walls);
		}
		// FIXME This algorithm can be improved by preferentially starting with walls that aren't connected on one end, and by allowing opposite-direction walls to chain together. But this is good enough for now.
		while( !walls.isEmpty() ){
			ArrayList<Double> wallSeg = new ArrayList<>();
			int x, y;
			GV_Line cWall = walls.get(0);
			wallSeg.add(cWall.p1[0]/1000.0);
			wallSeg.add(cWall.p1[1]/1000.0);
			while(cWall != null){
				x = cWall.p2[0];
				y = cWall.p2[1];
				wallSeg.add(cWall.p2[0]/1000.0);
				wallSeg.add(cWall.p2[1]/1000.0);
				walls.remove(cWall);
				// Search for a wall that shares an endpoint
				cWall = null;
				for(int wIdx = 0; wIdx < walls.size(); wIdx++){
					GV_Line testWall = walls.get(wIdx);
					if(testWall.p1[0] == x && testWall.p1[1] == y){
						cWall = testWall;
						break;
					}
				}
			}
			wallGroups.put(wallSeg.toArray(new Double[0]));
		}
		walls = null;
		
		// Populate art
		LinkedHashSet<OE_Image> imgSet = new LinkedHashSet<>();
		for(ArtPlacement aPlace : getAllArtPlacements()){
			imgSet.add(aPlace.img);
		}
		for(OE_Image img : imgSet){
			JSONObject artDefEntry = new JSONObject();
			artdef.put(img.getUniqueName(), artDefEntry);
			artDefEntry.put("size", new double[]{img.mmWidth/1000.0, img.mmHeight/1000.0});
			artDefEntry.put("texture", prefix+"img/"+img.getExportFileName());
			artDefEntry.put("atlas", atlas.getLocation(img).getCoord());
		}
		
		// Populate artPlacement
		ArrayList<OE_MatteStyle> mattes = new ArrayList<>();
		OE_MatteStyle defaultMatte = OE_MatteStyle.GenerateDefaultMatteStyle();
		for(Map.Entry<GV_Line, ArrayList<ArtPlacement>> aWallEntry : art.entrySet()){
			GV_Line w = aWallEntry.getKey();
			double[] wVec = w.getVec();
			double artdir = 180.0/Math.PI * Math.atan2(wVec[0], -wVec[1]);
			// Place all the artworks on each wall (one wall at a time)
			for(ArtPlacement aPlace : aWallEntry.getValue()){
				JSONObject artPlaceEntry = new JSONObject();
				artplace.put(artPlaceEntry);
				artPlaceEntry.put("art", aPlace.img.getUniqueName());
				// Images that are not on the left should get flipped 180 deg
				double dir = aPlace.onLeft ? artdir : (artdir+180.0)%360.0;
				artPlaceEntry.put("dir", dir);
				double cx = aPlace.cx;
				// Images that are on the left should start from the opposite end of the wall, relatively
				if(aPlace.onLeft){
					cx = 1.0-cx;
				}
				// Both of the dimensions of loc use 'cx', since cy is the vertical height off the ground
				double[] loc = new double[]{(w.p1[0]+wVec[0]*w.distance()*cx), (w.p1[1]+wVec[1]*w.distance()*cx)};
				// Handle thick wall offsets
				if(fp.isThickWalls()){
					loc[0] += fp.wallThickness * 0.5 * Math.cos(dir * Math.PI/180.0);
					loc[1] += fp.wallThickness * 0.5 * Math.sin(dir * Math.PI/180.0);
				}
				loc[0] /= 1000.0;
				loc[1] /= 1000.0;
				artPlaceEntry.put("loc", loc);
				artPlaceEntry.put("height", aPlace.cy * fp.getWallHeight() / 100.0);
				if(aPlace.scale != 1.0){
					artPlaceEntry.put("scale", aPlace.scale);
				}
				// Handle Text
				if(aPlace.text != null){
					artPlaceEntry.put("label", aPlace.text.getSubstitutedData(aPlace.img));
					if(aPlace.labelStyle != null){
						artPlaceEntry.put("labelStyle", aPlace.labelStyle.toGalleryDefJson(null));
					}
				}
				// Handle Audio
				if(aPlace.audio != null){
					artPlaceEntry.put("audio", prefix+"audio/"+aPlace.audio.getExportFileName());
				}
				// Handle Matte
				OE_MatteStyle ms = defaultMatte;
				if(aPlace.matteStyle != null) ms = aPlace.matteStyle;
				int matteIdx = mattes.indexOf(ms);
				if(matteIdx == -1){
					matteIdx = mattes.size();
					mattes.add(ms);
				}
				artPlaceEntry.put("matte", matteIdx);
			}
		}
		// Populate matte
		for(OE_MatteStyle ms : mattes){
			JSONObject jmatte = new JSONObject();
			matte.put(jmatte);
			jmatte.put("edgeColor", texcolormap.get(ms.edgeColor));
			JSONArray mlayers = new JSONArray();
			for(int lidx = 0; lidx < ms.LAYERCOUNT; lidx++){
				OE_MatteStyle.MatteLayer ml = ms.layers[lidx];
				if(!ml.isNullPresentation()){
					JSONObject l = new JSONObject();
					mlayers.put(l);
					l.put("color", texcolormap.get(ml.color));
					if(ml.mmWidth != 0){
						l.put("width", ml.mmWidth);
					}
					if(ml.mmHeight != 0){
						l.put("height", ml.mmHeight);
					}
					if(ml.mmStandoff != 0){
						l.put("standoff", ml.mmStandoff);
					}
					// We only need to record bevel angle if there is some combination of standoff with either width or height
					if(ml.mmStandoff != 0 && (ml.mmWidth != 0 || ml.mmHeight != 0)){
						l.put("bevel", ml.cdegBevelAngle);
					}
				}
			}
			if(mlayers.length() > 0){
				jmatte.put("layers", mlayers);
			}
		}
		return ret;
	}
	byte[] generateTexture(HashMap<OE_Color, Integer> texcolormap){
		LinkedHashSet<OE_Color> texcolors = new LinkedHashSet<>();
		// wall, floor, ceiling
		texcolors.add(colorMap.get("Walls"));
		texcolors.add(colorMap.get("Floor"));
		texcolors.add(colorMap.get("Ceiling"));
		// add colors for each of the matte types we might encounter
		OE_MatteStyle defMatte = OE_MatteStyle.GenerateDefaultMatteStyle();
		for(ArtPlacement aPlace : getAllArtPlacements()){
			OE_MatteStyle m = defMatte;
			if(aPlace.matteStyle != null){
				m = aPlace.matteStyle;
			}
			for(int lidx = 0; lidx < m.LAYERCOUNT; lidx++){
				if(!m.layers[lidx].isNullPresentation()){
					texcolors.add(m.layers[lidx].color);
				}
			}
			texcolors.add(m.edgeColor);
		}
		System.out.println("Tex Color Palette Count: "+texcolors.size());
		int imgdim = 2;
		while(imgdim*imgdim < texcolors.size()) imgdim *= 2;
		BufferedImage tex = new BufferedImage(imgdim, imgdim, BufferedImage.TYPE_INT_ARGB);
		int coloridx = 0;
		for(OE_Color c : texcolors){
			texcolormap.put(c, Integer.valueOf(coloridx));
			tex.setRGB(coloridx%imgdim, coloridx/imgdim, c.asInt());
			coloridx++;
		}
		return OE_Image.encodeImage(tex, "png");
	}
	byte[] generateGalleryCSV(){
		JSONArray ret = new JSONArray();
		//art label
		// Populate artPlacement
		for(Map.Entry<GV_Line, ArrayList<ArtPlacement>> aWallEntry : art.entrySet()){
			for(ArtPlacement aPlace : aWallEntry.getValue()){
				JSONObject artEntry = new JSONObject();
				ret.put(artEntry);
				artEntry.put("common_name", aPlace.img.toString());
				artEntry.put("image", aPlace.img.getExportFileName());
				if(aPlace.text != null){
					artEntry.put("label", aPlace.text.getSubstitutedData(aPlace.img));
				}
				if(aPlace.audio != null){
					artEntry.put("audio", aPlace.audio.getExportFileName());
				}
				double[] dimensions = aPlace.getDimensions();
				artEntry.put("meters_width", dimensions[0]);
				artEntry.put("meters_height", dimensions[1]);
			}
		}
		return CSVUtils.jsonToCSV(ret, null);
	}
	public synchronized String exportPackaged(Path exportPath, boolean[] running){
		String prefix = getUniqueName()+"/";
		// This is a set of all images we are using. The Double value is the maximum scale it is used at, which affects the output resolution
		LinkedHashMap<OE_Image, Double> imgSet = new LinkedHashMap<>();
		LinkedHashSet<OE_Audio> audioSet = new LinkedHashSet<>();
		for(ArrayList<ArtPlacement> aWall : art.values()){
			for(ArtPlacement aPlace : aWall){
				if(!running[0]) return "User Cancelled";
				Double scale = aPlace.scale;
				if(aPlace.audio != null){
					audioSet.add(aPlace.audio);
				}
				// Even if this entry already exists, if this placement is a larger scale, update the scale with this value.
				if(imgSet.containsKey(aPlace.img)){
					Double oldScale = imgSet.get(aPlace.img);
					if(oldScale < scale){
						imgSet.put(aPlace.img, scale);
					}
				}else{
					imgSet.put(aPlace.img, scale);
				}
			}
		}
		TextureAtlas atlas = TextureAtlas.forImages(List.copyOf(imgSet.keySet()), 4096);
		if(!running[0]) return "User Cancelled";
		HashMap<OE_Color, Integer> texcolormap = new HashMap<>();
		byte[] colorTexture = generateTexture(texcolormap);
		if(!running[0]) return "User Cancelled";
		JSONObject galleryJson = generateGalleryJson("uploads/"+prefix, atlas, texcolormap);
		if(galleryJson == null){
			return "No floorplan associated with this gallery";
		}
		byte[] galleryCSV = generateGalleryCSV();
		byte[] artDefCSV = CSVUtils.jsonToCSV(galleryJson.getJSONObject("art"), "uniqueIdentifier", null);
		byte[] artPlacementCSV = CSVUtils.jsonToCSV(galleryJson.getJSONArray("artPlacement"), new String[]{"art", "label", "scale"});
		ZipOutputStream z;
		if(!running[0]) return "User Cancelled";
		try{
			OutputStream writeStream = Files.newOutputStream(exportPath);
			z = new ZipOutputStream(writeStream);
		}catch(IOException e){
			return "IOException while opening zip stream: "+e;
		}
		if(!running[0]) return "User Cancelled";
		try{
			z.setMethod(ZipOutputStream.DEFLATED);
			z.putNextEntry(new ZipEntry(prefix+"gallery.json"));
			String galleryJsonString = galleryJson.toString();
			if(galleryJsonString == null){
				return "Invalid gallery json";
			}
			z.write(galleryJsonString.getBytes(Charset.forName("UTF-8")));
			System.out.println("Finished gallery.json");
			z.putNextEntry(new ZipEntry(prefix+"gallery.csv"));
			z.write(galleryCSV);
			System.out.println("Finished gallery.csv");
			z.putNextEntry(new ZipEntry(prefix+"tex.png"));
			z.write(colorTexture);
			System.out.println("Finished tex.png");
			z.putNextEntry(new ZipEntry(prefix+"img/__default.jpg"));
			z.write(this.getClass().getClassLoader().getResourceAsStream("resource/__default.jpg").readAllBytes());
			System.out.println("Finished __default.jpg");
			z.putNextEntry(new ZipEntry(prefix+"audio/__default.mp3"));
			z.write(this.getClass().getClassLoader().getResourceAsStream("resource/__default.mp3").readAllBytes());
			System.out.println("Finished __default.mp3");
			z.putNextEntry(new ZipEntry(prefix+"img/__atlas.png"));
			z.write(OE_Image.encodeImage(atlas.getImage(), "png"));
			System.out.println("Finished __atlas.png");
			for(Map.Entry<OE_Image, Double> imgMapEntry : imgSet.entrySet()){
				if(!running[0]) return "User Cancelled";
				OE_Image img = imgMapEntry.getKey();
				Double scale = imgMapEntry.getValue();
				z.putNextEntry(new ZipEntry(prefix+"img/"+img.getExportFileName()));
				ByteArrayOutputStream croppedImgStream = new ByteArrayOutputStream();
				// scale affects the output resolution because we need super-resolution export for large-scale image placements
				BufferedImage cImage = img.getCroppedImage(getPxPerMM() * scale, 4096);
				boolean success = ImageIO.write(cImage, "jpg", croppedImgStream);
				z.write(croppedImgStream.toByteArray());
			}
			for(OE_Audio aud : audioSet){
				if(!running[0]) return "User Cancelled";
				z.putNextEntry(new ZipEntry(prefix+"audio/"+aud.getExportFileName()));
				z.write(aud.getExportEncodedAudio());
			}
			z.close();
		}catch(IOException e){
			return "IOException while writing to zip stream: "+e;
		}
		return null;
	}
	void rebuildDependencies(){
		// Remove all dependencies
		for(OE_Resource dep : dependency.toArray(new OE_Resource[0])){
			dep.removeDependent(this);
		}
		// And rebuild them from scratch
		if(fp != null) fp.addDependent(this);
		for(ArrayList<ArtPlacement> aWall : art.values()){
			for(ArtPlacement aPlace : aWall){
				aPlace.img.addDependent(this);
				if(aPlace.text != null) aPlace.text.addDependent(this);
				if(aPlace.labelStyle != null) aPlace.labelStyle.addDependent(this);
				if(aPlace.audio != null) aPlace.audio.addDependent(this);
				if(aPlace.matteStyle != null) aPlace.matteStyle.addDependent(this);
			}
		}
	}
	public void informDependencyChanged(OE_Resource dep){
		// If our floorplan changed, delete any orphaned art placements.
		if(dep == fp){
			for(GV_Line wall : art.keySet().toArray(new GV_Line[0])){
				if(!fp.walls.contains(wall)){
					art.remove(wall);
				}
			}
		}
	}
	public String toJSONString(){
		JSONObject ret = getCoreJson();
		JSONObject dat = (JSONObject)(ret.get("dat"));
		JSONObject jcolors = new JSONObject();
		for(String ckey : colorMap.keySet()){
			jcolors.put(ckey, colorMap.get(ckey).getHex(false));
		}
		dat.put("colors", jcolors);
		dat.put("clickExamine", getCanClickToExamine());
		if(getCanClickToExamine()){
			dat.put("autoplayAudio", getAutoplayAudio());
		}
		dat.put("pxPerMM", getPxPerMM());
		if(fp != null){
			dat.put("floorplan", fp.getId());
		}else{
			dat.put("floorplan", JSONObject.NULL);
		}
		JSONObject jArt = new JSONObject();
		dat.put("art", jArt);
		for(GV_Line w : art.keySet()){
			ArrayList<ArtPlacement> artOnWall = art.get(w);
			if(artOnWall.size() == 0) continue;
			JSONArray jArtOnWall = new JSONArray();
			jArt.put(Integer.toString(fp.getWallIndex(w)), jArtOnWall);
			for(ArtPlacement p : artOnWall){
				jArtOnWall.put(p);
			}
		}
		return ret.toString();
	}
}

class ArtPlacement implements JSONString{
	double scale = 1.0;
	boolean onLeft;
	// Center coordinates from 0.0-1.0.
	double cx = 0.5, cy = 0.5;
	OE_Image img;
	OE_Text text = null;
	OE_LabelStyle labelStyle = null;
	OE_Audio audio = null;
	OE_MatteStyle matteStyle = null;
	ArtPlacement(ArtPlacement target){
		scale = target.scale;
		onLeft = target.onLeft;
		cx = target.cx;
		cy = target.cy;
		img = target.img;
		labelStyle = target.labelStyle;
		matteStyle = target.matteStyle;
		text = target.text;
		audio = target.audio;
	}
	ArtPlacement(OE_Image img, boolean onLeft){
		this.onLeft = onLeft;
		this.img = img;
		assert img != null;
	}
	ArtPlacement(JSONObject jrep, OE_DB db){
		cx = jrep.getDouble("cx");
		cy = jrep.getDouble("cy");
		onLeft = jrep.getBoolean("onLeft");
		scale = jrep.getDouble("scale");
		img = (OE_Image)db.uidMap.get(jrep.getLong("img"));
		if(jrep.has("text")){
			text = (OE_Text)db.uidMap.get(jrep.getLong("text"));
			if(jrep.has("labelStyle")){
				labelStyle = (OE_LabelStyle)db.uidMap.get(jrep.getLong("labelStyle"));
			}
		}
		if(jrep.has("audio")){
			audio = (OE_Audio)db.uidMap.get(jrep.getLong("audio"));
		}
		if(jrep.has("matteStyle")){
			matteStyle = (OE_MatteStyle)db.uidMap.get(jrep.getLong("matteStyle"));
		}
	}
	public double[] getDimensions(){
		return new double[]{scale * img.getMMWidth()/1000.0, scale * img.getMMHeight()/1000.0};
	}
	public String toString(){
		return String.format("ArtPlacement[%s, %f, %f]", img.toString(), cx, cy);
	}
	public String toJSONString(){
		JSONObject ret = new JSONObject();
		ret.put("onLeft", onLeft);
		ret.put("scale", scale);
		ret.put("cx", cx);
		ret.put("cy", cy);
		ret.put("img", img.getId());
		if(text != null){
			ret.put("text", text.getId());
			if(labelStyle != null){
				ret.put("labelStyle", labelStyle.getId());
			}
		}
		if(audio != null){
			ret.put("audio", audio.getId());
		}
		if(matteStyle != null){
			ret.put("matteStyle", matteStyle.getId());
		}
		return ret.toString();
	}
}
