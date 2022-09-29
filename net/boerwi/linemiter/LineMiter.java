package net.boerwi.linemiter;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Calculates mitering geometry for graphs of connected line segments.
 */
public class LineMiter{
	final int thickness;
	final LinkedHashMap<Long, Point> points = new LinkedHashMap<>();
	/**
	 * Creates a new Line-Mitering instance.
	 * @param thickness The thickness of the mitered walls.
	 */
	public LineMiter(int thickness){
		this.thickness = thickness;
	}
	/**
	 * Adds a new line to the mitering input.
	 * @param x1 X-Coordinate of the first point.
	 * @param y1 Y-Coordinate of the first point.
	 * @param x2 X-Coordinate of the second point.
	 * @param y2 Y-Coordinate of the second point.
	 */
	public void addLine(int x1, int y1, int x2, int y2){
		// Ignore 0-length lines
		if(x1 == x2 && y1 == y2) return;
		Point pt1 = getPoint(x1, y1);
		Point pt2 = getPoint(x2, y2);
		Line nl = new Line(pt1, pt2, thickness);
		pt1.addMember(nl);
		pt2.addMember(nl);
	}
	/**
	 * Computes the mitering for whatever lines have been added to this LineMiter instance.
	 * @return An array of length-4 arrays representing line segments. Each length-4 array is represented as [X1, Y1, X2, Y2].
	 */
	public int[][] getWalls(){
		ArrayList<int[]> ret = new ArrayList<>();
		for(Point pt : points.values()){
			// Establish all of the points that will appear in the final result
			pt.precompute();
		}
		for(Point pt : points.values()){
			// has this point already handled all of its outbound lines?
			if(pt.missing.size() == 0) continue;
			getWalls(pt, ret);
		}
		return ret.toArray(new int[][]{});
	}
	
	
	void getWalls(Point srcpt, ArrayList<int[]> ret){
		while( !srcpt.missing.isEmpty() ){
			Line curr = srcpt.missing.get(srcpt.missing.size()-1);
			Point pt = srcpt;
			while(true){
				// exit condition :  we are asked to draw a line that we have already drawn.
				if(!pt.missing.contains(curr)) break;
				// Mark line as drawn
				pt.missing.remove(curr);
				int prepointIdx = (pt.sequence.indexOf(curr)-1+pt.sequence.size() ) % pt.sequence.size();
				int[] prepoint = (int[]) pt.sequence.get(prepointIdx);
				Point o = curr.getOther(pt);
				int postpointIdx = (o.sequence.indexOf(curr)+1) % o.sequence.size();
				int[] postpoint = (int[]) o.sequence.get(postpointIdx);
				ret.add(new int[]{prepoint[0], prepoint[1], postpoint[0], postpoint[1]});
				int[] lastPoint = postpoint;
				postpointIdx = (postpointIdx+1)%o.sequence.size();
				// While the next sequence item is an int array (aka, a point, and not a line), add the point to the return array
				while(o.sequence.get(postpointIdx) instanceof int[]){
					// Draw connecting lines
					int[] newPoint = (int[])(o.sequence.get(postpointIdx));
					ret.add(new int[]{lastPoint[0], lastPoint[1], newPoint[0], newPoint[1]});
					lastPoint = newPoint;
					postpointIdx = (postpointIdx+1)%o.sequence.size();
				}
				// The next item is a line. So process the line.
				curr = (Line)o.sequence.get(postpointIdx);
				pt = o;
			}
		}
	}
	/**
	 * Creates a point, or returns an existing point if one already exists at that location.
	 * @param x X Coordinate.
	 * @param y Y Coordinate.
	 * @return The requested point.
	 */
	Point getPoint(int x, int y){
		long key = (((long)x)&0xFFFFFFFFl) | (((long)y) << 32);
		Point pt = points.get(key);
		if(pt == null){
			pt = new Point(x, y, thickness);
			points.put(key, pt);
		}
		return pt;
	}
}

class Point{
	final int[] c;
	final int thickness;
	final ArrayList<Line> members = new ArrayList<>();
	final ArrayList<Line> missing = new ArrayList<>();
	//A list of Lines and int[2]. Each line represents when the perimeter goes to a different point. Each int[2] represents a vertex.
	final ArrayList<Object> sequence = new ArrayList<>();
	Point(int x, int y, int thickness){
		c = new int[]{x, y};
		this.thickness = thickness;
	}
	void addMember(Line t){
		members.add(t);
		missing.add(t);
	}
	double distanceTo(Point o){
		long dx = c[0]-o.c[0];
		long dy = c[1]-o.c[1];
		return Math.sqrt(dx*dx + dy*dy);
	}
	void precompute(){
		members.sort(new LineAngleSorter(this));
		sequence.clear();
		// Butt-end
		if(members.size() == 1){
			sequence.add(members.get(0));
			sequence.add(members.get(0).getPoint(this, true));
			sequence.add(members.get(0).getPoint(this, false));
		// Some sort of join
		}else{
			for(int idx = 0; idx < members.size(); idx++){
				Line l1 = members.get(idx);
				Line l2 = members.get((idx+1)%members.size());
				sequence.add(l1);
				double lineAngle = l1.angleToLine(l2);
				// Angle is too big, so we need two joining points
				if(lineAngle > Math.PI * 1.55){
					sequence.add(l1.getPoint(this, true));
					sequence.add(l2.getPoint(this, false));
				// Angle is small enough that we need to join with a projected point based on the angle.
				}else{
					int[] pt = l1.getPoint(this, true);
					// Angle along which pt should be shifted
					double angle = l1.angleFrom(this);
					// The amount of distance change needed.
					double t = Math.min(Math.min(l1.distance, l2.distance), 0.5*thickness * Math.tan((Math.PI - lineAngle)/2.0));
					pt[0] -= Math.cos(angle) * t;
					pt[1] -= Math.sin(angle) * t;
					sequence.add(pt);
				}
			}
		}
	}
	public String toString(){
		return String.format("Point[(%d, %d), %d conn]", c[0], c[1], members.size());
	}
}
class Line{
	final Point[] p;
	final double aAngle, bAngle;
	final int thickness;
	final double distance;
	Line(Point a, Point b, int thickness){
		p = new Point[]{a, b};
		this.thickness = thickness;
		aAngle = Math.atan2(b.c[1]-a.c[1], b.c[0]-a.c[0]) + Math.PI;
		bAngle = (aAngle + Math.PI) % (2.0*Math.PI);
		distance = p[0].distanceTo(p[1]);
	}
	Point getOther(Point than){
		assert than == p[0] || than == p[1];
		return (than == p[0]) ? p[1] : p[0];
	}
	double angleFrom(Point s){
		assert s == p[0] || s == p[1];
		return (s == p[0]) ? aAngle : bAngle;
	}
	double angle(){
		return aAngle;
	}
	int[] getPoint(Point t, boolean leftSide){
		int[] ret = new int[2];
		double oa = aAngle+Math.PI/2.0;
		int offX = (int)(Math.cos(oa) * thickness/2.0);
		int offY = (int)(Math.sin(oa) * thickness/2.0);
		if(t == p[1] ^ leftSide){
			offX *= -1;
			offY *= -1;
		}
		ret[0] = t.c[0] + offX;
		ret[1] = t.c[1] + offY;
		return ret;
	}
	// This gives the angle between two lines which share an endpoint.
	// The return value is found by starting at the non-shared point of this line, then going to the shared point, then turning (the returned number of degrees -180), then traveling to the non-shared point of 'o'
	double angleToLine(Line o){
		double ret = 0.0;
		if(p[0] == o.p[0]){
			ret = o.angle() - angle() + Math.PI;
		}else if(p[0] == o.p[1]){
			ret = o.angle() - angle();
		}else if(p[1] == o.p[0]){
			ret = o.angle() - angle();
		}else if(p[1] == o.p[1]){
			ret = o.angle() - angle() + Math.PI;
		}else{
			assert false : "angleTo requires two lines which share an endpoint";
		}
		// This is used to be more useful to me, but it doesn't make the result easier to understand.
		ret += Math.PI;
		// Put in 0 - +2pi range
		while(ret < 0){
			ret += 2.0*Math.PI;
		}
		ret = ret % (2.0*Math.PI);
		return ret;
	}
}
/**
 * A line comparator that sorts lines based on their angle. The given point must be an endpoint for every sorted line.
 */
class LineAngleSorter implements Comparator<Line>{
	final Point pt;
	LineAngleSorter(Point pt){
		this.pt = pt;
	}
	public int compare(Line o1, Line o2){
		double a1 = o1.angleFrom(pt);
		double a2 = o2.angleFrom(pt);
		double d = a1-a2;
		if(d < 0){
			return -1;
		}else if(d > 0){
			return 1;
		}else{
			return 0;
		}
	}
}

