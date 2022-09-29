package net.boerwi.convexhull;

import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Computes a 2D convex hull from a collection of points. Based on GRAHAM-SCAN from Introduction to Algorithms, 3rd, CHP 33.
 */
public class ConvexHull{
	ArrayList<CHPoint> points = new ArrayList<>();
	ArrayList<CHPoint> hull = null;
	final CHPoint barycenter = new CHPoint(0,0);
	/** Creates a convex hull with no points.
	 * @see add
	 * @see addAll
	 */
	public ConvexHull(){}
	/**
	 * Adds multiple points to the hull.
	 * @param p Collection of points to add.
	 */
	public void addAll(Collection<CHPoint> p){
		points.addAll(p);
		invalidate();
	}
	/**
	 * Adds a point to the hull.
	 * @param p The point to add.
	 */
	public void add(CHPoint p){
		points.add(p);
		invalidate();
	}
	/**
	 * Removes all points from the hull.
	 */
	public void clear(){
		points.clear();
		invalidate();
	}
	/**
	 * Invalidates the cached hull solution. Recomputation will automatically occur the next time it is needed (e.g. from a call to getHull() or getBarycenter()).
	 * @see getHull
	 */
	public void invalidate(){
		hull = null;
		barycenter.setPosition(0,0);
	}
	void revalidate(){
		if(hull == null){
			computeHull();
			computeBarycenter();
		}
	}
	/**
	 * Computes the convex hull. Caches results, so subsequent calls are fast.
	 * @return Points forming the hull in counter-clockwise order.
	 */
	public ArrayList<CHPoint> getHull(){
		revalidate();
		return hull;
	}
	/**
	 * Computes the barycenter of the convex hull. Caches results, so subsequent calls are fast.
	 * @return The convex-hull polygon's barycenter (or centroid).
	 */
	public CHPoint getBarycenter(){
		revalidate();
		return barycenter;
	}
	public long[] getBounds(){
		revalidate();
		long[] bb = new long[]{Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
		if(hull != null){
			for(CHPoint pt : hull){
				long x = pt.getX();
				long y = pt.getY();
				if(bb[0] > x) bb[0] = x;
				if(bb[1] > y) bb[1] = y;
				if(bb[2] < x) bb[2] = x;
				if(bb[3] < y) bb[3] = y;
			}
		}
		return bb;
	}
	void computeHull(){
		if(points.size() < 3) return;
		ArrayList<CHPoint> radPoints = new ArrayList<>(points);
		radPoints.sort(new PtRadialComparator(startingPoint()));
		//let S be an empty stack, PUSH(p[0],S), PUSH(p[1],S), PUSH(p[2],S)
		hull = new ArrayList<>(radPoints.subList(0, 3));
		//for i = 3 to m
		for(int i = 3; i < radPoints.size(); i++){
			//while the angle formed by points NEXT-TO-TOP(S), TOP(S) and p[i] makes a nonleft turn
			CHPoint curr = radPoints.get(i);
			while(nonleft(hull.get(hull.size()-2), hull.get(hull.size()-1), curr)){
				//POP(S)
				hull.remove(hull.size()-1);
			}
			//PUSH(p[i], S)
			hull.add(curr);
		}
		//Return S
	}
	/**
	 * Computes the area of the hull using the Shoelace formula.
	 * @return The area of the hull.
	 */
	long area(){
		long a = 0;
		if(hull != null){
			for(int ptIdx = 0; ptIdx < hull.size(); ptIdx++){
				CHPoint pt1 = hull.get(ptIdx);
				CHPoint pt2 = hull.get((ptIdx+1)%hull.size());
				a += pt1.getX() * pt2.getY();
				a -= pt2.getX() * pt1.getY();
			}
			a /= 2l;
		}
		return a;
	}
	/**
	 * Computes the barycenter of the hull.
	 * @see <a href=https://en.wikipedia.org/wiki/Centroid#Of_a_polygon>Wikipedia Centroid of a Polygon</a>
	 */
	void computeBarycenter(){
		long area = area();
		if(area == 0){
			if(points.size() > 0){
				// There aren't enough points to form a convex hull, but there are still input points
				barycenter.setPosition(points.get(0));
			}
			return;
		}
		long cx = 0;
		long cy = 0;
		for(int ptIdx = 0; ptIdx < hull.size(); ptIdx++){
			CHPoint pt1 = hull.get(ptIdx);
			CHPoint pt2 = hull.get((ptIdx+1)%hull.size());
			double mult = (pt1.getX()*pt2.getY() - pt2.getX()*pt1.getY());
			cx += (pt1.getX()+pt2.getX()) * mult;
			cy += (pt1.getY()+pt2.getY()) * mult;
		}
		cx /= 6l*area;
		cy /= 6l*area;
		barycenter.setPosition((int)cx, (int)cy);
	}
	/**
	 * Gets the least Y (least X as tie-breaker) point in the hull.
	 * @return The least Y then X point.
	 */
	CHPoint startingPoint(){
		CHPoint ret = points.get(0);
		for(int idx = points.size()-1; idx > 0; idx--){
			CHPoint test = points.get(idx);
			int dy = test.y-ret.y;
			if(dy <= 0){
				if(dy == 0){
					if(test.x-ret.x < 0){
						ret = test;
					}
				}else{ // assume dy < 0
					ret = test;
				}
			}
		}
		return ret;
	}
	static boolean nonleft(CHPoint a, CHPoint b, CHPoint c){
		int x1 = b.x-a.x;
		int y1 = b.y-a.y;
		int x2 = c.x-a.x;
		int y2 = c.y-a.y;
		return x1*y2-x2*y1 < 0;
	}
}
class PtRadialComparator implements Comparator<CHPoint>{
	CHPoint origin;
	public PtRadialComparator(CHPoint origin){
		this.origin = origin;
	}
	public int compare(CHPoint o1, CHPoint o2){
		int dX1 = o1.x-origin.x;
		int dY1 = o1.y-origin.y;
		int dX2 = o2.x-origin.x;
		int dY2 = o2.y-origin.y;
		if(dX1 == dX2 && dY1 == dY2){
			return 0;
		}
		if(dX1 == 0 && dY1 == 0) return -1;
		if(dX2 == 0 && dY2 == 0) return 1;
		double a1 = Math.atan2(dY1, dX1);
		double a2 = Math.atan2(dY2, dX2);
		double deltaA = a1-a2;
		if(deltaA == 0){
			double distDelta = Math.sqrt(dX1*dX1 + dY1*dY1) - Math.sqrt(dX2*dX2 + dY2*dY2);
			if(distDelta > 0){
				return 1;
			}else if(distDelta < 0){
				return -1;
			}
			return 0;
		}
		return (deltaA > 0) ? 1 : ((deltaA < 0) ? -1 : 0);
	}
}
