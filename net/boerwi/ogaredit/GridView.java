package net.boerwi.ogaredit;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import net.boerwi.convexhull.*;

@SuppressWarnings("serial") // We don't support serialization
public class GridView extends JComponent implements MouseWheelListener, MouseInputListener, KeyListener{
	// Center of view in mm
	private int mmCenterX = 0, mmCenterY = 0;
	private int mmMouseX = 0, mmMouseY = 0;
	private int rotation = 0;//view rotation, 0-3 inclusive, ccw.
	private int mmMousePressX = 0, mmMousePressY = 0;
	private boolean mouseClickEnabled = true;
	private boolean fitContentsFlag = true;
	private boolean isMPressed = false, isMDragging = false;
	private int[] xbounds = new int[]{0,1}, ybounds = new int[]{0,1};
	AffineTransform pxTransform, realTransform, realPxTransform, invRealTransform;
	GridTool tool = null;
	// Drawn grid pitch in mm. -1 means draw no grid
	private int mmGrid = 100;
	// px_DrawnSize = mm_RealSize * zscale(scaleIdx)
	private int scaleIdx = 15, scaleMin = 0, scaleMax = 27;
	ArrayList<GridViewComponent> components = new ArrayList<>();
	void zoomOut(){ if(scaleIdx > scaleMin) scaleIdx--; }
	void zoomIn(){ if(scaleIdx < scaleMax) scaleIdx++; }
	double zScale(int idx){ return 0.001 * Math.pow(1.2, idx); }
	double zScale(){ return zScale(scaleIdx); }
	public GridView(){
		super();
		setFocusable(true);
		System.out.println(java.awt.Toolkit.getDefaultToolkit().getScreenResolution());
		addMouseWheelListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}
	public void setRotation(int rot){
		rotation = rot;
		repaint();
	}
	public void addComponent(GridViewComponent c){
		components.add(c);
		components.sort(new GridViewDrawComparator());
		repaint();
	}
	void fitContents(){
		if(components.size() == 0) return;
		//minx, miny, maxx, maxy
		long[] bb = new long[]{Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
		for(GridViewComponent comp : components){
			long[] cbb = new long[]{Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
			comp.getBounds(cbb);
			for(int idx = 0; idx < 2; idx++){
				if(bb[idx] > cbb[idx]) bb[idx] = cbb[idx];
				if(bb[idx+2] < cbb[idx+2]) bb[idx+2] = cbb[idx+2];
			}
		}
		// Zoom out until everything can be shown
		for(scaleIdx = scaleMax; scaleIdx >= scaleMin; scaleIdx--){
			if(getWidth() < (bb[2]-bb[0]) * zScale()){
				continue;
			}
			if(getHeight() < (bb[3]-bb[1]) * zScale()){
				continue;
			}
			break;
		}
		// Center content
		mmCenterX = (int)(bb[2]+bb[0])/2;
		mmCenterY = (int)(bb[3]+bb[1])/2;
		//System.out.println("Bounds: "+Arrays.toString(bb) + " "+getWidth());
	}
	public int getMMGrid(){
		return mmGrid;
	}
	public void removeComponent(GridViewComponent c){
		components.remove(c);
		repaint();
	}
	public void clearComponents(){
		components.clear();
		repaint();
	}
	public void setGridTool(GridTool t){
		if(tool != null){
			removeMouseListener(tool);
			removeMouseMotionListener(tool);
			removeKeyListener(tool);
		}
		tool = t;
		if(tool != null){
			addMouseListener(tool);
			addMouseMotionListener(tool);
			addKeyListener(tool);
		}
	}
	public void enableMouseClick(boolean enabled){
		mouseClickEnabled = enabled;
	}
	public void mouseWheelMoved(MouseWheelEvent evt){
		int mmMouseOffsetX = mmMouseX - mmCenterX;
		int mmMouseOffsetY = mmMouseY - mmCenterY;
		double preScale = zScale(scaleIdx);
		int idx = evt.getWheelRotation();
		boolean zoomingIn = (idx < 0);
		for(; idx != 0; idx += (zoomingIn ? 1 : -1)){
			if(zoomingIn){
				zoomIn();
			}else{
				zoomOut();
			}
		}
		// Mouse-following scroll zoom requires the mouse is in the same real location after the zoom.
		double scaleRatio = preScale/zScale(scaleIdx);
		// This subtracts the difference between the scale-Ratioed offset and the original offset to keep the mouse location unchanged.
		mmCenterX -= (scaleRatio-1.0) * mmMouseOffsetX;
		mmCenterY -= (scaleRatio-1.0) * mmMouseOffsetY;
		repaint();
	}
	Point2D getMouseLocRes = new Point2D.Double();
	public int[] snapToGrid(int[] loc){
		int[] ret = new int[2];
		for(int idx = 0; idx < 2; idx++){
			ret[idx] = ((int)Math.round(((double)loc[idx])/mmGrid)) * mmGrid;
		}
		return ret;
	}
	public int[] getMouseLoc(MouseEvent evt, boolean gridSnapped){
		int x = evt.getX();
		int y = evt.getY();
		invRealTransform.transform(new Point2D.Double(x, y), getMouseLocRes);
		int[] ret = new int[]{(int)getMouseLocRes.getX(), (int)getMouseLocRes.getY()};
		if(gridSnapped){
			ret = snapToGrid(ret);
		}
		return ret;
	}
	void setMouseLoc(MouseEvent evt){
		int[] p = getMouseLoc(evt, false);
		mmMouseX = p[0];
		mmMouseY = p[1];
	}
	void setMousePressLoc(MouseEvent evt){
		int[] p = getMouseLoc(evt, false);
		mmMousePressX = p[0];
		mmMousePressY = p[1];
	}
	public void mouseClicked(MouseEvent evt){}
	public void mouseEntered(MouseEvent evt){}
	public void mouseExited(MouseEvent evt){}
	public void mousePressed(MouseEvent evt){
		requestFocusInWindow();
		if(!mouseClickEnabled) return;
		if(evt.getButton() != MouseEvent.BUTTON1) return;
		isMPressed = true;
		setMousePressLoc(evt);
		repaint();
	}
	public void mouseReleased(MouseEvent evt){
		if(!mouseClickEnabled) return;
		isMPressed = false;
		isMDragging = false;
		repaint();
	}
	public void mouseDragged(MouseEvent evt){
		setMouseLoc(evt);
		if(!mouseClickEnabled) return;
		if(!isMPressed) return;
		isMDragging = true;
		int dX = mmMouseX - mmMousePressX;
		int dY = mmMouseY - mmMousePressY;
		mmCenterX -= dX;
		mmCenterY -= dY;
		repaint();
		setMouseLoc(evt);
	}
	public void mouseMoved(MouseEvent evt){
		isMDragging = false;
		setMouseLoc(evt);
	}
	public void keyPressed(KeyEvent e){
		int code = e.getKeyCode();
		java.util.List<Integer> arrowKeys = Arrays.asList(KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT);
		if(arrowKeys.contains(code)){
			double[] dirX = new double[]{0, -1, 0, 1};
			double[] dirY = new double[]{1, 0, -1, 0};
			int idx = arrowKeys.indexOf(code);
			double[] offset = new double[]{dirX[idx], dirY[idx]};
			AffineTransform.getRotateInstance(-Math.PI * 0.5 * rotation).transform(offset, 0, offset, 0, 1);
			final int pxMove = 20;
			mmCenterX += pxMove * offset[0] / zScale(scaleIdx);
			mmCenterY += pxMove * offset[1] / zScale(scaleIdx);
		}
		repaint();
	}
	public void keyReleased(KeyEvent e){}
	public void keyTyped(KeyEvent e){}
	
	public void paintComponent(Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		int width = getWidth();
		int height = getHeight();
		if(fitContentsFlag){
			fitContentsFlag = false;
			fitContents();
		}
		double scale = zScale(scaleIdx);
		pxTransform = g2.getTransform();
		realTransform = new AffineTransform();
		realTransform.translate((double)width/2.0, (double)height/2.0);
		realTransform.rotate(-Math.PI * 0.5 * rotation);
		realTransform.scale(scale, -scale);
		realTransform.translate(-mmCenterX, -mmCenterY);
		try {
		invRealTransform = realTransform.createInverse();
		}catch(NoninvertibleTransformException e){
			System.out.println("Cannot find inverse transform: "+e);
		}
		realPxTransform = (AffineTransform)pxTransform.clone();
		realPxTransform.concatenate(realTransform);
		g2.setTransform(realPxTransform);
		// System.out.println(g2.getTransform());
		Point2D pt1 = new Point2D.Double(), pt2 = new Point2D.Double();
		invRealTransform.transform(new Point(0, 0), pt1);
		invRealTransform.transform(new Point(width, height), pt2);
		xbounds = new int[]{(int)Math.min(pt1.getX(), pt2.getX()), (int)Math.max(pt1.getX(), pt2.getX())};
		ybounds = new int[]{(int)Math.min(pt1.getY(), pt2.getY()), (int)Math.max(pt1.getY(), pt2.getY())};
		// Draw Axis lines
		//g.drawRect(xbounds[0], ybounds[0], xbounds[1]-xbounds[0], ybounds[1]-ybounds[0]);
		g.drawLine(xbounds[0], 0, xbounds[1], 0);
		g.drawLine(0, ybounds[0], 0, ybounds[1]);
		// Draw Grid Points
		g2.setTransform(pxTransform);
		if(mmGrid*scale >= 2 && !isMDragging){ // Don't draw the grid if the points are too close.
			int pxGridRad = (int)(scale*mmGrid/8.0);
			Point2D placement = new Point2D.Double();
			for(int x = xbounds[0]/mmGrid+1; x <= xbounds[1]/mmGrid-1; x++){
				for(int y = ybounds[0]/mmGrid+1; y <= ybounds[1]/mmGrid-1; y++){
					realTransform.transform(new Point(x*mmGrid, y*mmGrid), placement);
					int px = (int)placement.getX();
					int py = (int)placement.getY();
					g.drawLine(px-pxGridRad, py, px+pxGridRad, py);
					g.drawLine(px, py-pxGridRad, px, py+pxGridRad);
				}
			}
		}
		// Draw components
		// Make lines slightly thicker, and at least 2px
		double minStrokeWidth = Math.max(2.0/scale, mmGrid/2.0);
		g2.setStroke(new BasicStroke((float)minStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.setTransform(realPxTransform);
		for(GridViewComponent comp : components){
			comp.draw(g2, pxTransform, realTransform);
			g2.setTransform(realPxTransform);
		}
		// Draw tool
		if(tool != null){
			tool.draw(g2);
		}
	}
}
abstract class GridTool implements MouseInputListener, KeyListener{
	GridView owner;
	public GridTool(GridView owner){
		this.owner = owner;
	}
	public void mouseClicked(MouseEvent evt){}
	public void mouseEntered(MouseEvent evt){}
	public void mouseExited(MouseEvent evt){}
	public void mousePressed(MouseEvent evt){}
	public void mouseReleased(MouseEvent evt){}
	public void mouseDragged(MouseEvent evt){}
	public void mouseMoved(MouseEvent evt){}
	public void keyPressed(KeyEvent e){}
	public void keyReleased(KeyEvent e){}
	public void keyTyped(KeyEvent e){}
	public void draw(Graphics2D g){};
	void drawTextAt(Graphics2D g, String text, int mmX, int mmY){
		String[] str = text.split("\n");
		g.setTransform(owner.pxTransform);
		g.setColor(Color.BLACK);
		int fontsize = 15;
		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, fontsize));
		for(int line = 0; line < str.length; line++){
			Point2D pxLoc = owner.realTransform.transform(new Point2D.Double(mmX, mmY), null);
			g.drawString(str[line], (int)pxLoc.getX(), (int)(pxLoc.getY() + line*fontsize*1.4));
		}
		g.setTransform(owner.realPxTransform);
	}
}
abstract class GridViewComponent {
	protected Color baseColor = new Color(0,0,255);
	// draw in increasing z order
	protected int zIdx = 0;
	// Center of component
	protected int mmCX, mmCY;
	GridViewComponent(int x, int y){
		this.mmCX = x;
		this.mmCY = y;
	}
	public int getX(){
		return mmCX;
	}
	public int getY(){
		return mmCY;
	}
	public void setX(int x){
		setPosition(x, mmCY);
	}
	public void setY(int y){
		setPosition(mmCX, y);
	}
	public void setZIndex(int z){
		zIdx = z;
	}
	public int getZIndex(){ return zIdx; }
	public void setPosition(int x, int y){
		mmCX = x;
		mmCY = y;
	}
	public void setColor(Color c){
		baseColor = c;
	}
	/**
	 * degHue - 0.0-1.0;
	 * saturation 0.0-1.0
	 * value 0.0-1.0
	 * from https://en.wikipedia.org/wiki/HSL_and_HSV#HSV_to_RGB
	 */
	 //FIXME dedup to OE_Color functionality
	public void setColor(float hue, float saturation, float value){
		double chroma = saturation * value;
		//H', which is from 0-6.
		double hueP = hue*6.0;
		// Second-largest component
		double X = chroma * (1.0-Math.abs(hueP % 2.0 - 1.0));
		// This is wack, but results in the correct component indices
		int XIdx = 2+((-1 - ((int)hueP)) % 3);
		int chromaIdx = ((((int)hueP)+1)/2)%3;
		int base = (int)(255*(value - chroma));
		int[] rgb = new int[]{base,base,base};
		rgb[XIdx] += 255*X;
		rgb[chromaIdx] += 255*chroma;
		setColor(new Color(rgb[0], rgb[1], rgb[2]));
	}
	/** Sets to a nicely-visible color based on a hash.
	 */
	public void setColor(int hash){
		Random randomizer = new Random(hash);
		randomizer.nextFloat(); // Without this, "Room2".hashCode(), "Room3".hashCode(), and "Room4".hashCode() all return *very* similar colors.
		float hue = randomizer.nextFloat();
		setColor(hue, 1.0f, 1.0f);
	}
	Color getColor(){
		return baseColor;
	}
	Color getBoldColor(){
		return getColor().brighter();
	}
	Color getMellowColor(){
		Color darker = getColor().darker();
		return new Color(darker.getRed(), darker.getGreen(), darker.getBlue(), darker.getAlpha()/2);
	}
	abstract void draw(Graphics2D g, AffineTransform pxT, AffineTransform realT);
	abstract void getBounds(long[] ret);
}
class GV_Image extends GridViewComponent{
	RenderedImage img;
	int mmw, mmh;
	int pxW, pxH;
	public GV_Image(int mmx, int mmy, int mmw, int mmh, RenderedImage img){
		super(mmx, mmy);
		this.mmw = mmw;
		this.mmh = mmh;
		this.img = img;
		pxW = img.getWidth();
		pxH = img.getHeight();
	}
	void draw(Graphics2D g, AffineTransform pxT, AffineTransform realT){
		AffineTransform t = new AffineTransform();
		t.translate(getX(), getY());
		t.scale((double)mmw/(double)pxW, (double)mmh/(double)pxH);
		g.drawRenderedImage(img, t);
	}
	void getBounds(long[] ret){
		if(mmw > 0){
			ret[0] = mmCX;
			ret[2] = mmCX+mmw;
		}else{
			ret[0] = mmCX+mmw;
			ret[2] = mmCX;
		}
		if(mmh > 0){
			ret[1] = mmCY;
			ret[3] = mmCY+mmh;
		}else{
			ret[1] = mmCY+mmh;
			ret[3] = mmCY;
		}
	}
	/** returns two doubles in range 0-1
	 */
	double[] getImageCoords(int mmTx, int mmTy){
		return new double[]{(mmTx-mmCX)/(double)mmw, (mmTy-mmCY)/(double)mmh};
	}
	/** x and y are in range 0-1
	 */
	int[] getRealCoords(double x, double y){
		return new int[]{(int)(mmCX+x*mmw), (int)(mmCY+y*mmh)};
	}
}
class GV_Vec extends GridViewComponent{
	//Angle in deci-degrees
	int ddAngle;
	public GV_Vec(int mmx, int mmy, int deciDegrees){
		super(mmx, mmy);
		ddAngle = deciDegrees;
	}
	void draw(Graphics2D g, AffineTransform pxT, AffineTransform realT){
		g.setColor(getMellowColor());
		g.fillOval(getX()-100, getY()-100, 200, 200);
		double rad = ddAngle*(Math.PI/1800.0);
		int mmlength = 500;
		g.setColor(getBoldColor());
		g.drawLine(getX(), getY(), getX()+(int)(mmlength*Math.cos(rad)), getY()+(int)(mmlength*Math.sin(rad)));
	}
	void getBounds(long[] ret){
		ret[0] = mmCX-150;
		ret[1] = mmCY-150;
		ret[2] = mmCX+150;
		ret[3] = mmCY+150;
	}
}
class GV_Line extends GridViewComponent{
	int[] p1;
	int[] p2;
	public GV_Line(int x1, int y1, int x2, int y2){
		super((x1+x2)/2, (y1+y2)/2);
		p1 = new int[]{x1, y1};
		p2 = new int[]{x2, y2};
	}
	void draw(Graphics2D g, AffineTransform pxT, AffineTransform realT){
		g.setColor(getBoldColor());
		g.drawLine(p1[0], p1[1], p2[0], p2[1]);
	}
	void getBounds(long[] ret){
		if(p1[0] < p2[0]){
			ret[0] = p1[0];
			ret[2] = p2[0];
		}else{
			ret[0] = p2[0];
			ret[2] = p1[0];
		}
		if(p1[1] < p2[1]){
			ret[1] = p1[1];
			ret[3] = p2[1];
		}else{
			ret[1] = p2[1];
			ret[3] = p1[1];
		}
	}
	double distance(){
		long dx = p2[0] - p1[0];
		long dy = p2[1] - p1[1];
		return Math.sqrt(dx*dx+dy*dy);
	}
	public double clickScore(int mmX, int mmY){
		double goal = distance();
		long dx1 = mmX - p1[0];
		long dx2 = mmX - p2[0];
		long dy1 = mmY - p1[1];
		long dy2 = mmY - p2[1];
		double score = Math.sqrt(dx1*dx1+dy1*dy1) + Math.sqrt(dx2*dx2+dy2*dy2) - goal;
		return score;
	}
	public double distanceToPoint(int mmX, int mmY){
		long dx1 = mmX - p1[0];
		long dx2 = mmX - p2[0];
		long dy1 = mmY - p1[1];
		long dy2 = mmY - p2[1];
		double[] v = getVec();
		double[] v1 = norm(new double[]{dx1, dy1});
		double[] v2 = norm(new double[]{dx2, dy2});
		double a1 = Math.acos(v1[0]*v[0] + v1[1]*v[1]);
		double a2 = Math.acos(v2[0]*v[0] + v2[1]*v[1]);
		boolean canProject = (a1 <= Math.PI/2.0) && (a2 >= Math.PI/2.0);
		if(canProject){
			// We can project onto the line, so the distance is the rightangle distance.
			double dist = Math.sqrt(dx1*dx1 + dy1*dy1);
			// distance * dot product of the vector to the point and a vector orthogonal to the line
			return dist * Math.abs(v1[0]*(-v[1]) + v1[1]*v[0]);
		}else{
			// We can't project onto the line, so the distance is the distance to the closest end point.
			return Math.min(Math.sqrt(dx1*dx1+dy1*dy1), Math.sqrt(dx2*dx2+dy2*dy2));
		}
	}
	public boolean onLeft(int mmX, int mmY){//FIXME cleanup
		long dx1 = mmX - p1[0];
		long dx2 = mmX - p2[0];
		long dy1 = mmY - p1[1];
		long dy2 = mmY - p2[1];
		double[] v = getVec();
		double[] v1 = norm(new double[]{dx1, dy1});
		return 0.0 > (v[0] * -v1[1] + v[1] * v1[0]);
	}
	double[] getVec(){
		return norm(new double[]{p2[0]-p1[0], p2[1]-p1[1]});
	}
	// FIXME put in standalone geom pack
	double[] norm(double[] v){
		double dist = 0.0;
		for(double c : v){
			dist += c*c;
		}
		dist = Math.sqrt(dist);
		double[] ret = new double[v.length];
		for(int idx = 0; idx < v.length; idx++){
			ret[idx] = v[idx]/dist;
		}
		return ret;
	}
}
class GV_Point extends GridViewComponent{
	CHPoint pt;
	public GV_Point(int x, int y){
		super(x, y);
		pt = new CHPoint(x, y);
	}
	public void setPosition(int x, int y){
		super.setPosition(x,y);
		pt.setPosition(x,y);
	}
	void draw(Graphics2D g, AffineTransform pxT, AffineTransform realT){
		g.setColor(getBoldColor());
		//Draw 20CM
		g.fillOval(getX()-100, getY()-100, 200, 200);
	}
	void getBounds(long[] ret){
		long x = pt.getX();
		long y = pt.getY();
		ret[0] = x;
		ret[1] = y;
		ret[2] = x;
		ret[3] = y;
	}
}
class GV_Hull extends GridViewComponent{
	//FIXME make super.mmx/mmy reflect the barycenter of GV_Hull
	ArrayList<GV_Point> pts = new ArrayList<>();
	final ConvexHull hull = new ConvexHull();
	String name;
	public GV_Hull(){
		super(0,0);
		setName("");
	}
	GV_Hull(GV_Point seed){
		super(seed.getX(), seed.getY());
		pts.add(seed);
		setName("");
	}
	GV_Hull(java.util.List<GV_Point> seed){
		super(seed.get(0).getX(), seed.get(0).getY());
		pts.addAll(seed);
		rebuildHull();
		setName("");
	}
	public void addPoint(int x, int y){
		GV_Point newPt = new GV_Point(x, y);
		hull.add(new CHPoint(x, y));
		newPt.setColor(getColor());
		pts.add(newPt);
	}
	public void removePoint(GV_Point t){
		pts.remove(t);
		rebuildHull();
	}
	public void setName(String name){
		this.name = name;
		setColor(name.hashCode());
		System.out.println("Name: "+name+" Hash: "+name.hashCode()+" Color: "+baseColor);
	}
	public String getName(){ return name; }
	public String toString(){
		return getName();
	}
	public void setColor(Color c){
		for(GV_Point p : pts){
			p.setColor(c);
		}
		super.setColor(c);
	}
	public void setZIndex(int z){
		super.setZIndex(z);
		//Subpoint Z index isn't respected, but we set it so that it will return accurately.
		for(GV_Point p : pts){
			p.setZIndex(z);
		}
	}
	void drawPoints(Graphics2D g, AffineTransform pxT, AffineTransform realT){
		for(GV_Point p : pts){
			p.draw(g, pxT, realT);
		}
	}
	void rebuildHull(){
		hull.clear();
		for(GV_Point p : pts){
			hull.add(new CHPoint(p.getX(), p.getY()));
		}
	}
	void draw(Graphics2D g, AffineTransform pxT, AffineTransform realT){
		ArrayList<CHPoint> poly = hull.getHull();
		if(poly != null){
			int[] polyX = new int[poly.size()];
			int[] polyY = new int[poly.size()];
			for(int idx = 0; idx < poly.size(); idx++){
				polyX[idx] = poly.get(idx).getX();
				polyY[idx] = poly.get(idx).getY();
			}
			g.setColor(getMellowColor());
			g.fillPolygon(polyX, polyY, poly.size());
		}
		CHPoint center = hull.getBarycenter();
		// FIXME unify this and gridviewtool's drawtext
		//System.out.println("Center: "+center);
		g.setTransform(pxT);
		g.setColor(Color.BLACK);
		int fontsize = 15;
		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, fontsize));
		Point2D pxLoc = realT.transform(new Point2D.Double(center.getX(), center.getY()), null);
		g.drawString(getName(), (int)pxLoc.getX(), (int)pxLoc.getY());
	}
	void getBounds(long[] ret){
		long[] bb = hull.getBounds();
		for(int idx = 0; idx < 4; idx++){
			ret[idx] = bb[idx];
		}
	}
}
class GridViewDrawComparator implements Comparator<GridViewComponent>{
	public int compare(GridViewComponent o1, GridViewComponent o2){
		return o1.getZIndex() - o2.getZIndex();
	}
}
