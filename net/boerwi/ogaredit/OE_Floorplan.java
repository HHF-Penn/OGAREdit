package net.boerwi.ogaredit;

import java.util.*;
import java.awt.Polygon;

import org.json.JSONString;
import org.json.JSONObject;
import org.json.JSONArray;

import net.boerwi.linemiter.LineMiter;

// This class makes use of non-public classes in GridView (Denoted by GV_ prefix).
// This data should really be decoupled from the rendering classes. But this is faster.
import net.boerwi.ogaredit.GridView;

public class OE_Floorplan extends OE_Resource {
	final ArrayList<GV_Hull> regions = new ArrayList<>();
	final ArrayList<GV_Line> walls = new ArrayList<>();
	private final GV_Vec avatar;
	private boolean thickWalls = false;
	private int cmWallHeight = 300;
	private int cmEyeHeight = 160;
	int wallThickness = 150;
	public OE_Floorplan() {
		avatar = new GV_Vec(5000, 5000, 0);
		avatar.setZIndex(30);
		// Populate with a demo region
		ArrayList<GV_Point> demoRegionPoints = new ArrayList<>();
		demoRegionPoints.add(new GV_Point(2500, 2500));
		demoRegionPoints.add(new GV_Point(2500, 7500));
		demoRegionPoints.add(new GV_Point(7500, 7500));
		demoRegionPoints.add(new GV_Point(7500, 2500));
		addRegion("Center of Room", new GV_Hull(demoRegionPoints));
		// Populate with some demo walls
		addWall(new GV_Line(0,0,10000, 0));
		addWall(new GV_Line(10000,0,10000, 10000));
		addWall(new GV_Line(10000, 10000,0,10000));
		addWall(new GV_Line(0,10000,0,0));
	}
	public OE_Floorplan(String name, long uid, JSONObject dat){
		super(name, uid);
		JSONArray jwalls = dat.getJSONArray("walls");
		JSONArray jregions = dat.getJSONArray("regions");
		JSONObject javatar = dat.getJSONObject("avatar");
		avatar = new GV_Vec(javatar.getInt("mmX"), javatar.getInt("mmY"), javatar.getInt("ddAngle"));
		cmWallHeight = dat.getInt("cmWallHeight");
		cmEyeHeight = dat.getInt("cmEyeHeight");
		thickWalls = dat.optBoolean("thickWalls", false);
		for(int wIdx = 0; wIdx < jwalls.length(); wIdx++){
			JSONArray jw = jwalls.getJSONArray(wIdx);
			walls.add(new GV_Line(jw.getInt(0), jw.getInt(1), jw.getInt(2), jw.getInt(3)));
		}
		for(int rIdx = 0; rIdx < jregions.length(); rIdx++){
			JSONObject jr = jregions.getJSONObject(rIdx);
			GV_Hull h = new GV_Hull();
			h.setName(jr.getString("name"));
			JSONArray jrpts = jr.getJSONArray("ptPairs");
			for(int rPtIdx = 0; rPtIdx < jrpts.length(); rPtIdx+=2){
				h.addPoint(jrpts.getInt(rPtIdx), jrpts.getInt(rPtIdx+1));
			}
			regions.add(h);
		}
	}
	public OE_Resource duplicate(){
		OE_Floorplan ret = new OE_Floorplan();
		ret.cmWallHeight = cmWallHeight;
		ret.cmEyeHeight = cmEyeHeight;
		ret.avatar.setPosition(avatar.getX(), avatar.getY());
		ret.avatar.ddAngle = avatar.ddAngle;
		ret.regions.clear();
		ret.walls.clear();
		ret.thickWalls = thickWalls;
		for(GV_Line w : walls){
			ret.addWall(new GV_Line(w.p1[0], w.p1[1], w.p2[0], w.p2[1]));
		}
		for(GV_Hull h : regions){
			GV_Hull dupH = new GV_Hull();
			for(int idx = 0; idx < h.pts.size(); idx++){
				GV_Point pt = h.pts.get(idx);
				dupH.addPoint(pt.getX(), pt.getY());
			}
			ret.addRegion(h.getName(), dupH);
		}
		commonDuplicateTasks(ret);
		return ret;
	}
	public OE_ResType getType(){ return OE_ResType.FLOORPLAN; }
	public void setEyeHeight(int cm){
		cmEyeHeight = cm;
		signalUnsavedChanges();
	}
	public void setWallHeight(int cm){
		cmWallHeight = cm;
		signalUnsavedChanges();
	}
	public void setAvatarDeciAngle(int ddAngle){
		avatar.ddAngle = ddAngle;
		signalUnsavedChanges();
	}
	public void setAvatarPosition(int mmX, int mmY){
		avatar.setPosition(mmX, mmY);
		signalUnsavedChanges();
	}
	public void setThickWalls(boolean thickWalls){
		this.thickWalls = thickWalls;
		signalUnsavedChanges();
	}
	public boolean isThickWalls(){
		return thickWalls;
	}
	public int getEyeHeight(){
		return cmEyeHeight;
	}
	public int getWallHeight(){
		return cmWallHeight;
	}
	public int getAvatarDeciAngle(){
		return avatar.ddAngle;
	}
	public GV_Vec getAvatar(){
		return avatar;
	}
	public void addRegion(String name, GV_Hull hull){
		hull.setName(name);
		regions.add(hull);
		hull.setZIndex(10);
		signalUnsavedChanges();
	}
	public void removeRegion(GV_Hull hull){
		regions.remove(hull);
		signalUnsavedChanges();
	}
	public void clearRegions(){
		regions.clear();
		signalUnsavedChanges();
	}
	public int getRegionCount(){
		return regions.size();
	}
	public void addRegionPoint(GV_Hull t, int mmX, int mmY){
		t.addPoint(mmX, mmY);
		signalUnsavedChanges();
	}
	public void addWall(GV_Line wall){
		walls.add(wall);
		wall.setZIndex(20);
		signalUnsavedChanges();
	}
	public void clearWalls(){
		walls.clear();
		signalUnsavedChanges();
	}
	public void deleteWall(GV_Line wall){
		walls.remove(wall);
		signalUnsavedChanges();
	}
	public int getWallIndex(GV_Line wall){
		return walls.indexOf(wall);
	}
	public GV_Line getWallAtIndex(int idx){
		return walls.get(idx);
	}
	/** Convert walls to their 'thick' version.
	 * @return An array of lines, where each thick wall is composed of two lines.
	 */
	public GV_Line[] getThickWalls(){
		LineMiter rdat = new LineMiter(wallThickness);
		for(GV_Line w : walls){
			rdat.addLine(w.p1[0], w.p1[1], w.p2[0], w.p2[1]);
		}
		int[][] wdat = rdat.getWalls();
		ArrayList<GV_Line> ret = new ArrayList<>();
		for(int[] wseg : wdat){
			GV_Line w = new GV_Line(wseg[0], wseg[1], wseg[2], wseg[3]);
			ret.add(w);
		}
		return ret.toArray(new GV_Line[0]);
	}
	// Uses prims algorithm to construct a maze
	public void generateMaze(int mmPassageSize, int passageWidth, int extraJoins){
		byte[] maze = new byte[passageWidth*passageWidth];
		final byte up = 1, down = 2, left = 4, right = 8;
		Random rnd = new Random(this.getId());
		LinkedList<Integer> backtrack = new LinkedList<>();
		backtrack.push(0);
		byte[] dirSeq = new byte[]{1, 2, 4, 8};
		// Create a basic maze
		while(backtrack.size() != 0){
			int loc = backtrack.pop();
			int x = loc % passageWidth;
			int y = loc / passageWidth;
			// Continue as long as we keep moving
			boolean moved = true;
			while(moved){
				moved = false;
				int rnum = rnd.nextInt();
				int locidx = y*passageWidth+x;
				// There are only 24 options, but this is easier to code
				for(int idx = 0; idx < 15; idx++){
					int idx1 = rnum&0b11;
					rnum = rnum >>> 2;
					int idx2 = rnum&0b11;
					rnum = rnum >>> 2;
					byte t = dirSeq[idx2];
					dirSeq[idx2] = dirSeq[idx1];
					dirSeq[idx1] = t;
				}
				for(int dirIdx = 0; dirIdx < 4; dirIdx++){
					int dir = dirSeq[dirIdx];
					int newX = x - (dir>>2&1) + (dir>>3&1);
					int newY = y - (dir>>0&1) + (dir>>1&1);
					if(newX < 0 || newY < 0 || newX >= passageWidth || newY >= passageWidth){
						continue;
					}
					int newlocidx = newY*passageWidth+newX;
					if(maze[newlocidx] == 0){
						// We haven't been here before
						moved = true;
						maze[locidx] |= dir;
						maze[newlocidx] = (byte)((dir > 2) ? (dir == 4 ? 8 : 4) : dir%2 + 1);
						x = newX;
						y = newY;
						backtrack.push(newlocidx);
						break;
					}
				}
			}
		}
		// Add in extra joins
		while(extraJoins > 0){
			int x = rnd.nextInt(passageWidth-2) + 1;
			int y = rnd.nextInt(passageWidth-2) + 1;
			int idx = y*passageWidth+x;
			int dir = 1 << rnd.nextInt(4);
			if((dir & maze[idx]) == 0){
				// This passage doesn't exist
				int newX = x - (dir>>2&1) + (dir>>3&1);
				int newY = y - (dir>>0&1) + (dir>>1&1);
				int newlocidx = newY*passageWidth+newX;
				maze[idx] |= dir;
				maze[newlocidx] |= (byte)((dir > 2) ? (dir == 4 ? 8 : 4) : dir%2 + 1);
				extraJoins--;
			}
		}
		//System.out.println(Arrays.toString(maze));
		// Create left and upper walls
		for(int idx = 0; idx < passageWidth; idx++){
			// Add Left wall
			addWall(new GV_Line(0, idx*mmPassageSize, 0, (idx+1)*mmPassageSize));
			// Add Upper wall
			addWall(new GV_Line(idx*mmPassageSize, 0, (idx+1)*mmPassageSize, 0));
		}
		// Create all needed bottom and right walls
		for(int idx = 0; idx < passageWidth*passageWidth; idx++){
			int x = idx % passageWidth;
			int y = idx / passageWidth;
			if((maze[idx] & down) == 0){
				addWall(new GV_Line(x*mmPassageSize, (y+1)*mmPassageSize, (x+1)*mmPassageSize, (y+1)*mmPassageSize));
			}
			if((maze[idx] & right) == 0){
				addWall(new GV_Line((x+1)*mmPassageSize, y*mmPassageSize, (x+1)*mmPassageSize, (y+1)*mmPassageSize));
			}
		}
	}
	public String toJSONString(){
		JSONObject ret = getCoreJson();
		JSONObject dat = (JSONObject)(ret.get("dat"));
		JSONArray jwalls = new JSONArray();
		for(GV_Line w : walls){
			JSONArray jwall = new JSONArray();
			jwalls.put(jwall);
			jwall.put(w.p1[0]);
			jwall.put(w.p1[1]);
			jwall.put(w.p2[0]);
			jwall.put(w.p2[1]);
		}
		JSONArray jregions = new JSONArray();
		for(GV_Hull r : regions){
			JSONObject jreg = new JSONObject();
			jreg.put("name", r.getName());
			JSONArray jregpts = new JSONArray();
			for(GV_Point pt : r.pts){
				jregpts.put(pt.mmCX);
				jregpts.put(pt.mmCY);
			}
			jreg.put("ptPairs", jregpts);
			jregions.put(jreg);
		}
		dat.put("thickWalls", thickWalls);
		dat.put("cmEyeHeight", cmEyeHeight);
		dat.put("cmWallHeight", cmWallHeight);
		JSONObject javatar = new JSONObject();
		javatar.put("mmX", avatar.mmCX);
		javatar.put("mmY", avatar.mmCY);
		javatar.put("ddAngle", avatar.ddAngle);
		dat.put("avatar", javatar);
		dat.put("walls", jwalls);
		dat.put("regions", jregions);
		return ret.toString();
	}
}
