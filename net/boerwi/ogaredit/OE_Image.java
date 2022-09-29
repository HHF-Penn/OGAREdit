package net.boerwi.ogaredit;

import java.util.TreeMap;
import java.util.Arrays;
import java.util.List;
import java.util.Base64;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.AffineTransformOp;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;

import org.json.JSONString;
import org.json.JSONObject;
import org.json.JSONArray;

public class OE_Image extends OE_Resource {
	public static BufferedImage imageFromBytes(byte[] dat){
		BufferedImage ret = null;
		ImageFormat imgFormat;
		try{
			imgFormat = Imaging.getImageInfo(dat).getFormat();
			if(imgFormat == ImageFormats.PNG || imgFormat == ImageFormats.TIFF){
				ret = Imaging.getBufferedImage(dat);
			}
		}catch(ImageReadException e){
		}catch(IOException e){
			assert false : "IOException while reading image: "+e;
		}
		if(ret == null){
			System.out.println("Commons Imaging failed to read file");
			try{
				ret = ImageIO.read(new ByteArrayInputStream(dat));
			}catch(IllegalArgumentException e){
				assert false : "Commons Imaging and ImageIO both failed to read file: " + e;
			}catch(IOException e){
				assert false : "IOException while reading image: "+e;
			}
		}
		System.out.println("Image: "+ret);
		return ret;
	}
	public static byte[] encodeImage(BufferedImage src, String fmt){
		try{
			ByteArrayOutputStream o = new ByteArrayOutputStream();
			if(!ImageIO.write(src, fmt, o)){
				System.out.println("No writer for \""+fmt+"\"!");
			}
			return o.toByteArray();
		}catch(IOException e){
			assert false : "Failed to encode image as \""+fmt+"\": "+e;
		}
		return new byte[0];
	}
	public static String b64EncodeImage(BufferedImage target, String fmt){
		String ret = null;
		ByteArrayOutputStream imgStream = new ByteArrayOutputStream();
		try{
			boolean success = ImageIO.write(target, fmt, imgStream);
			if(success){
				ret = Base64.getEncoder().encodeToString(imgStream.toByteArray());
			}else{
				System.out.println("ImageIO failed to write image (fmt:\""+fmt+"\"");
				for(String fname : ImageIO.getWriterFormatNames()){
					System.out.println(fname);
				}
			}
		}catch(IOException e){
			System.out.println("Failed to write thumbnail to string: "+e);
		}
		return ret;
	}
	public static void drawRandom(BufferedImage target){
		java.util.Random rnd = new java.util.Random();
		Graphics2D g = target.createGraphics();
		Color[] palette = new Color[5];
		for(int pidx = 0; pidx < 5; pidx++){
			palette[pidx] = OE_Color.random().toAwtColor();
		}
		int x = target.getWidth();
		int y = target.getHeight();
		g.setColor(palette[0]);
		g.fillRect(0, 0, x, y);
		for(int op = 0; op < 10; op++){
			g.setColor(palette[rnd.nextInt(5)]);
			if(rnd.nextBoolean()){
				g.drawLine(rnd.nextInt(x), rnd.nextInt(y), rnd.nextInt(x), rnd.nextInt(y));
			}else{
				int cx = rnd.nextInt(x);
				int cy = rnd.nextInt(y);
				int sx = rnd.nextInt(x/4);
				int sy = rnd.nextInt(y/4);
				g.fillOval(cx-sx/2, cy-sy/2, sx, sy);
			}
		}
	}
	
	final TreeMap<String, String> tags = new TreeMap<>();
	static final int thumbSize = 128;
	int mmWidth=1, mmHeight=1;
	boolean aspectLocked = false;
	//0-3 inclusive rotate source image 90deg*rotation CCW
	private int rotation = 0;
	// upper left, lower left, lower right, upper right
	private double[] cropCornerX = new double[]{0.0, 0.0, 1.0, 1.0};
	private double[] cropCornerY = new double[]{0.0, 1.0, 1.0, 0.0};
	int srcWidth = 1, srcHeight = 1; // Pixel size of source image
	private BufferedImage thumbnail = new BufferedImage(thumbSize, thumbSize, BufferedImage.TYPE_INT_BGR);
	private BufferedImage croppedThumbnail = null;
	private double croppedAspect = -1;
	private OE_Blob blob = null;
	public OE_Image(){
		this.blob = null;
		populateBaseTags();
	}
	public OE_Image(OE_Blob blob){
		setBlob(blob);
		populateBaseTags();
	}
	public OE_Image(String name, long uid, JSONObject dat, OE_BlobMgr blobMgr){
		super(name, uid);
		populateBaseTags();
		JSONObject jTags = dat.getJSONObject("tags");
		for(String tagname : jTags.keySet()){
			tags.put(tagname, jTags.getString(tagname));
		}
		JSONArray jcropX = dat.getJSONArray("cropX"), jcropY = dat.getJSONArray("cropY");
		for(int idx = 0; idx < 4; idx++){
			cropCornerX[idx] = jcropX.getDouble(idx);
			cropCornerY[idx] = jcropY.getDouble(idx);
		}
		JSONArray srcDim = dat.getJSONArray("srcDim");
		srcWidth = srcDim.getInt(0);
		srcHeight = srcDim.getInt(1);
		JSONArray mmDim = dat.getJSONArray("mmDim");
		mmWidth = mmDim.getInt(0);
		mmHeight = mmDim.getInt(1);
		rotation = dat.getInt("rotation");
		aspectLocked = dat.getBoolean("aspectLocked");
		// We don't use setBlob here because that would retrigger expensive file processing operations that we don't need since they are saved
		blob = blobMgr.getBlob(dat.getLong("blobId"));
		try{
			thumbnail = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(dat.getString("thumbnail"))));
			String croppedThumbnailEncoded = dat.optString("croppedThumbnail", null);
			if(croppedThumbnailEncoded == null){
				croppedThumbnail = null;
			}else{
				croppedThumbnail = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(croppedThumbnailEncoded)));
			}
		}catch(IOException e){
			assert false : "Failed to load thumbnail:"+e;
		}
	}
	public static String[] getBaseTags(){
		return new String[]{
			"Title",
			"Artist",
			"Date",
			"Medium",
			"Description",
			"Style",
			"Geography",
			"Notes"
		};
	}
	private void populateBaseTags(){
		for(String t : getBaseTags()){
			tags.put(t, String.format("[NO %s]", t.toUpperCase()));
		}
	}
	public OE_ResType getType(){ return OE_ResType.IMAGE; }
	public OE_Resource duplicate(){
		OE_Image ret = new OE_Image(this.blob);
		for(String tagkey : tags.keySet()){
			ret.tags.put(tagkey, tags.get(tagkey));
		}
		ret.rotation = rotation;
		ret.mmWidth = mmWidth;
		ret.mmHeight = mmHeight;
		ret.aspectLocked = aspectLocked;
		ret.cropCornerX = Arrays.copyOf(cropCornerX, 4);
		ret.cropCornerY = Arrays.copyOf(cropCornerY, 4);
		ret.srcWidth = srcWidth;
		ret.srcHeight = srcHeight;
		commonDuplicateTasks(ret);
		return ret;
	}
	/**-1 is ccw, 1 is cw.
	 */
	public void rotate(int dir){
		while(dir < 0) dir += 4;
		rotation = (rotation+dir)%4;
		System.out.println("Rotation: "+rotation);
		resetCropCorners();
		setWidth(mmWidth);
		signalUnsavedChanges();
	}
	public int getRotation(){
		return rotation;
	}
	public int getMMWidth(){
		return mmWidth;
	}
	public int getMMHeight(){
		return mmHeight;
	}
	public boolean getAspectLocked(){
		return aspectLocked;
	}
	public void setAspectLocked(boolean locked){
		if(locked == aspectLocked) return;
		aspectLocked = locked;
		if(aspectLocked){
			// This triggers the aspect locking based on width's value
			setWidth(mmWidth);
		}
		signalUnsavedChanges();
	}
	public double getSrcAspectRatio(){
		if(rotation % 2 == 1){
			return (double)srcHeight/(double)srcWidth;
		}else{
			return (double)srcWidth/(double)srcHeight;
		}
	}
	public double getCroppedAspectRatio(){
		// upper left, lower left, lower right, upper right
		double[] pxPosX = new double[4];
		double[] pxPosY = new double[4];
		for(int idx = 0; idx < 4; idx++){
			pxPosX[idx] = cropCornerX[idx]*srcWidth;
			pxPosY[idx] = cropCornerY[idx]*srcHeight;
		}
		double topLen = Math.hypot(pxPosX[3]-pxPosX[0], pxPosY[3]-pxPosY[0]);
		double bottomLen = Math.hypot(pxPosX[2]-pxPosX[1], pxPosY[2]-pxPosY[1]);
		double leftLen = Math.hypot(pxPosX[1]-pxPosX[0], pxPosY[1]-pxPosY[0]);
		double rightLen = Math.hypot(pxPosX[3]-pxPosX[2], pxPosY[3]-pxPosY[2]);
		return (topLen+bottomLen)/(leftLen+rightLen);
	}
	public void setWidth(int w){
		mmWidth = w;
		if(aspectLocked){
			mmHeight = Math.max(1, (int)Math.round(mmWidth * (1.0/getCroppedAspectRatio())));
		}
		signalUnsavedChanges();
	}
	public void setHeight(int h){
		mmHeight = h;
		if(aspectLocked){
			mmWidth = Math.max(1, (int)Math.round(mmHeight * getCroppedAspectRatio()));
		}
		signalUnsavedChanges();
	}
	public void setDimensions(int mmX, int mmY){
		mmWidth = mmX;
		mmHeight = mmY;
		signalUnsavedChanges();
	}
	public void resetCropCorners(){
		double[] rawCropCornerX = new double[]{0.0, 0.0, 1.0, 1.0};
		double[] rawCropCornerY = new double[]{0.0, 1.0, 1.0, 0.0};
		for(int idx = 0; idx < 4; idx++){
			cropCornerX[idx] = rawCropCornerX[(idx+4-rotation)%4];
			cropCornerY[idx] = rawCropCornerY[(idx+4-rotation)%4];
		}
		setWidth(mmWidth);
		signalUnsavedChanges();
	}
	public BufferedImage getSourceImage(){
		BufferedImage src = null;
		if(blob != null){
			byte[] blobData = null;
			try{
				blobData = blob.getData().readAllBytes();
			}catch(IOException e){
				assert false : "Failed to get image data: "+e;
				return null;
			}
			src = imageFromBytes(blobData);
			if(src != null){
				srcWidth = src.getWidth();
				srcHeight = src.getHeight();
			}
		}
		return src;
	}
	//FIXME put in a custom geometry utility static class
	static double distance(double[] p1, double[] p2){
		double dx = p2[0]-p1[0];
		double dy = p2[1]-p1[1];
		return Math.sqrt(dx*dx + dy*dy);
	}
	static double[] toBary(double x, double y){
		return new double[]{y, 1.0-(x+y), x};
	}
	public BufferedImage getCroppedImage(){
		return getCroppedImage(Double.POSITIVE_INFINITY, 4096);
	}
	public BufferedImage getCroppedImage(double maxPixPerMM, int maxDim){
		BufferedImage ret = null;
		BufferedImage src = getSourceImage();
		if(src == null){
			return new BufferedImage(thumbSize, thumbSize, BufferedImage.TYPE_INT_BGR);
		}
		double[]
			pxUL = new double[]{cropCornerX[0]*srcWidth, cropCornerY[0]*srcHeight},
			pxLL = new double[]{cropCornerX[1]*srcWidth, cropCornerY[1]*srcHeight},
			pxLR = new double[]{cropCornerX[2]*srcWidth, cropCornerY[2]*srcHeight},
			pxUR = new double[]{cropCornerX[3]*srcWidth, cropCornerY[3]*srcHeight};
			System.out.println(pxLL[0]+" "+pxLL[1]);
		double topDist = distance(pxUL, pxUR);
		double botDist = distance(pxLL, pxLR);
		double lefDist = distance(pxUL, pxLL);
		double rigDist = distance(pxUR, pxLR);
		// Because of how it is rendered in the client, we could have different horizontal and vertical pixel densities. But that is complex and requires a lot of extra work for minimal benefit.
		double pixelPerMMVert = ((lefDist + rigDist)/2.0) / mmHeight;
		double pixelPerMMHori = ((topDist + botDist)/2.0) / mmWidth;
		System.out.println("dimensional pixel density ratio: "+(pixelPerMMVert/pixelPerMMHori));
		double pxPerMM = Math.min(maxPixPerMM, (pixelPerMMVert+pixelPerMMHori)/2.0);
		System.out.println("pxPerMM: "+pxPerMM);
		int retWidth = (int)Math.round(mmWidth * pxPerMM);
		int retHeight = (int)Math.round(mmHeight * pxPerMM);
		if(retWidth > maxDim || retHeight > maxDim){
			System.out.println("Over maxDim");
			double aspectRatio = (double)retWidth/(double)retHeight;
			if(aspectRatio > 1.0){
				// Wide image
				retWidth = maxDim;
				retHeight = (int) (maxDim / aspectRatio);
			}else{
				// Tall image
				retWidth = (int)(maxDim * aspectRatio);
				retHeight = maxDim;
			}
		}
		System.out.println("Final dim: "+retWidth+" x "+retHeight);
		assert retWidth <= maxDim && retHeight <= maxDim && retWidth > 0 && retHeight > 0 : "Invalid image dimensions: "+retWidth+"x"+retHeight;
		ret = new BufferedImage(retWidth, retHeight, BufferedImage.TYPE_INT_BGR);
		
		//Copy over pixel data
		double[] bary; // Barycentric Coords
		double[] p1,p2,p3;
		for(int x = 0; x < retWidth; x++){
			double xp = x/(double)retWidth;
			for(int y = 0; y < retHeight; y++){
				double yp = y/(double)retHeight;
				if(xp+yp > 1.0){ //Which triangle half are we in?
					bary = toBary(1.0-xp, 1.0-yp);
					p1 = pxUR;
					p2 = pxLR;
					p3 = pxLL;
				}else{
					bary = toBary(xp, yp);
					p1 = pxLL;
					p2 = pxUL;
					p3 = pxUR;
				}
				int rx = (int)((bary[0]*p1[0] + bary[1]*p2[0] + bary[2]*p3[0]));
				int ry = (int)((bary[0]*p1[1] + bary[1]*p2[1] + bary[2]*p3[1]));
				if(rx < 0) rx = 0;
				if(rx >= srcWidth) rx = srcWidth-1;
				if(ry < 0) ry = 0;
				if(ry >= srcHeight) ry = srcHeight-1;
				int rgb = src.getRGB(rx, ry);
				//System.out.println("writing: "+rgb);
				ret.setRGB(x, y, rgb);
			}
		}
		return ret;
	}

	public BufferedImage getThumbnail(){
		return thumbnail;
	}
	public void invalidateCroppedThumbnail(){
		croppedThumbnail = null;
		signalUnsavedChanges();
	}
	public BufferedImage getCroppedThumbnail(){
		if(croppedThumbnail == null){
			generateCroppedThumbnail();
		}
		return croppedThumbnail;
	}
	void generateCroppedThumbnail(){
		croppedThumbnail = getCroppedImage(Double.POSITIVE_INFINITY, thumbSize);
		signalUnsavedChanges();
	}
	void generateThumbnail(){
		BufferedImage src = getSourceImage();
		if(src == null) return;
		float scale = (float)Math.min((double)thumbSize/(double)srcHeight, (double)thumbSize/(double)srcWidth);
		AffineTransformOp thumbRescale = new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), AffineTransformOp.TYPE_BILINEAR);
		BufferedImage thumbnailinter = thumbRescale.createCompatibleDestImage(src, null);
		thumbRescale.filter(src, thumbnailinter);
		int width = thumbnailinter.getWidth();
		int height = thumbnailinter.getHeight();
		thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				thumbnail.setRGB(x, y, thumbnailinter.getRGB(x, y));
			}
		}
	}
	public void setTag(String tag, String content){
		String curr = tags.get(tag);
		if(curr != null && content.equals(curr)) return;
		tags.put(tag, content);
		signalUnsavedChanges();
	}
	public void setBlob(OE_Blob blob){
		if(this.blob != null){
			this.blob.remDep(this);
		}
		this.blob = blob;
		if(this.blob != null){
			blob.addDep(this);
		}
		System.out.println("Set blob: "+blob);
		resetCropCorners();
		generateThumbnail();
		croppedThumbnail = thumbnail;
		setDimensions(2000, (int)(2000.0 * srcHeight / (double)srcWidth));
		signalUnsavedChanges();
	}
	public OE_Blob getBlob(){
		return this.blob;
	}
	public void setCropCorner(int idx, double x, double y){
		if(cropCornerX[idx] != x || cropCornerY[idx] != y){
			cropCornerX[idx] = x;
			cropCornerY[idx] = y;
			invalidateCroppedThumbnail();
			setWidth(mmWidth);
			signalUnsavedChanges();
		}
	}
	public double[] getCropCorner(int idx){
		return new double[]{cropCornerX[idx], cropCornerY[idx]};
	}
	public void cleanup(){
		setBlob(null);
		super.cleanup();
	}
	public String getExportFileName(){
		if(blob == null){
			return "__default.jpg";
		}else{
			return getId()+"_"+toString()+".jpg";
		}
	}
	public String toJSONString(){
		JSONObject ret = getCoreJson();
		JSONObject dat = (JSONObject)(ret.get("dat"));
		JSONObject jsonTags = new JSONObject();
		for(String tagname : tags.keySet()){
			jsonTags.put(tagname, tags.get(tagname));
		}
		dat.put("tags", jsonTags);
		dat.put("cropX", new JSONArray(cropCornerX));
		dat.put("cropY", new JSONArray(cropCornerY));
		dat.put("srcDim", new JSONArray(Arrays.asList(srcWidth, srcHeight)));
		dat.put("mmDim", new JSONArray(Arrays.asList(mmWidth, mmHeight)));
		dat.put("rotation", rotation);
		dat.put("aspectLocked", aspectLocked);
		long blobId = -1;
		if(blob != null){
			blobId = blob.getId();
		}
		dat.put("blobId", blobId);
		if(croppedThumbnail != null){
			dat.put("croppedThumbnail", b64EncodeImage(getCroppedThumbnail(), "jpg"));
		}
		dat.put("thumbnail", b64EncodeImage(getThumbnail(), "jpg"));
		return ret.toString();
	}
}
