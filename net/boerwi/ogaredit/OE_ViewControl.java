package net.boerwi.ogaredit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.ByteArrayInputStream;
import java.util.*;

import java.awt.image.BufferedImage;

abstract class OE_ViewControl{
	/**
	 * The database we are connected to
	 */
	OE_DB db;
	AsyncBlobServer previewAsyncBlobServer;
	abstract void init();
	abstract void updateView();
	public OE_ViewControl(String openPath) {
		init();
		//closeDB needs to set all values to their default state, so we call that when we begin to avoid code dupe
		closeDB();
		if(openPath != null){
			openDB(Paths.get(openPath), false);
		}
	}
	boolean isDBOpen(){
		return db != null;
	}
	void cleanup() {
		System.out.println("Exiting normally");
		// Cleanup Tasks
		assert db == null;
		/*if(db != null){
			db.cleanup();
		}*/
		// Sysexit is preferable to trying to cleanup all awt components
		System.exit(0);
	}
	boolean removeNodes(Collection<Node> nodes){
		assert db != null;
		return db.remove(nodes);
	}
	void duplicate(Entry t){
		db.duplicate(t);
	}
	abstract void viewPath(Object[] path);
	void createResource(OE_ResType type, Dir parent){
		assert db != null;
		db.createResource(type, parent);
		viewPath(parent.getPath().toArray());
	}
	void createSubDir(Dir target, String name){
		db.createSubDir(target, name);
		viewPath(target.getPath().toArray());
	}
	void createStressTestGallery(){
		Random rnd = new Random();
		OE_Floorplan floor = new OE_Floorplan();
		floor.setName("Stress Test Floorplan");
		floor.setThickWalls(true);
		floor.clearWalls();
		floor.clearRegions();
		floor.generateMaze(2500, 100, 150);
		floor.setAvatarPosition(1250, 1250);
		int wallCount = floor.walls.size();
		db.addResource(floor, db.floorplanRoot);
		OE_Gallery gal = new OE_Gallery();
		gal.setFloorplan(floor);
		gal.setName("Stress Test Gallery");
		db.addResource(gal, db.galleryRoot);
		ArrayList<OE_Image> imgs = new ArrayList<>();
		for(int imgIdx = 0; imgIdx < 100; imgIdx++){
			OE_Image img = new OE_Image();
			db.addResource(img, db.imageRoot);
			BufferedImage imgSrc = new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_BGR);
			OE_Image.drawRandom(imgSrc);
			byte[] imgDat = OE_Image.encodeImage(imgSrc, "jpg");
			OE_BlobMgr blobMgr = db.getBlobMgr();
			long bId = blobMgr.addBlob(new ByteArrayInputStream(imgDat), "stresstest"+imgIdx+".jpg");
			img.setBlob(blobMgr.getBlob(bId));
			img.setDimensions(200 + rnd.nextInt(2000), 200 + rnd.nextInt(2000));
			for(int instanceIdx = 0; instanceIdx < 3; instanceIdx++){
				gal.addArt(floor.getWallAtIndex(rnd.nextInt(wallCount)), rnd.nextBoolean(), img);
			}
		}
	}
	void openNewDB(Path p){
		openDB(p, true);
		System.out.println("Saved As: " + p.toString());
		saveDB();
	}
	void openDB(Path p, boolean isNew){
		assert db == null;
		assert p != null;
		db = new OE_DB(p, isNew);
		previewAsyncBlobServer = db.getBlobMgr().getAsyncBlobServer();
		System.out.println("Opened: " + p.toString());
		updateView();
	}
	void saveDB(){
		db.save();
		updateView();
	}
	boolean closeDB(){
		if(db != null) {
			db.cleanup();
		}
		previewAsyncBlobServer = null;
		db = null;
		updateView();
		return true;
	}
}
