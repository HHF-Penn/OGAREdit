package net.boerwi.ogaredit;

import java.util.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class TextureAtlas {
	private BufferedImage atlas;
	private int cellSize = 4, width = 4, height = 4;
	private HashMap<OE_Image, AtlasCoord> mapping = new HashMap<>();
	public BufferedImage getImage(){
		return atlas;
	}
	public AtlasCoord getLocation(OE_Image img){
		return mapping.get(img);
	}
	public double[] getCellSize(){
		return new double[]{cellSize/(double)width, cellSize/(double)height};
	}
	public static TextureAtlas forImages(List<OE_Image> images, int maxDim){
		long imgCount = images.size();
		TextureAtlas ret = new TextureAtlas();
		if(imgCount == 0){
			ret.atlas = new BufferedImage(4, 4, BufferedImage.TYPE_INT_BGR);
			return ret;
		}
		// What is our maximum area?
		long dpow = 0;
		while(1l<<dpow < maxDim){
			dpow++;
		}
		long dim = 1l<<dpow;
		ret.width = (int)dim;
		ret.height = (int)dim;
		
		long area = dim*dim;
		
		long ppow = 0;
		while((1l<<ppow) * (1l<<ppow) * imgCount <= area){
			ppow++;
		}
		ppow--;
		long idim = 1l<<ppow;
		ret.cellSize = (int)idim;
		long iarea = idim*idim;
		long siarea = iarea * imgCount;
		if(siarea <= area/2){
			ret.height /= 2;
		}
		int tileWidth = (int)(dim/idim);
		ret.atlas = new BufferedImage(ret.width, ret.height, BufferedImage.TYPE_INT_BGR);
		Graphics2D g = ret.atlas.createGraphics();
		for(int imgIdx = 0; imgIdx < imgCount; imgIdx++){
			int xcoord = (int)(idim * (imgIdx % tileWidth));
			int ycoord = (int)(idim * (imgIdx / tileWidth));
			OE_Image img = images.get(imgIdx);
			BufferedImage imgDat = img.getCroppedImage();
			AffineTransform trans = new AffineTransform();
			trans.translate(xcoord, ycoord);
			trans.scale((double)idim/(double)imgDat.getWidth(), (double)idim/(double)imgDat.getHeight());
			ret.mapping.put(img, ret.new AtlasCoord(xcoord, ycoord));
			g.drawImage(imgDat, trans, null);
		}
		g.dispose();
		return ret;
	}
	public class AtlasCoord {
		private final double[] c;
		public AtlasCoord(int x, int y){
			c = new double[]{x/(double)width, y/(double)height};
		}
		public double[] getCoord(){
			return c;
		}
	}
}
