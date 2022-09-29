package net.boerwi.convexhull;
/**
 * A basic 2D point implementation.
 */
public class CHPoint{
	int x, y;
	/**
	 * Creates a new point.
	 * @param x X Coordinate.
	 * @param y Y Coordinate.
	 */
	public CHPoint(int x, int y){
		setPosition(x,y);
	}
	/**
	 * Changes the point's position.
	 * @param x X Coordinate.
	 * @param y Y Coordinate.
	 */
	public void setPosition(int x, int y){
		this.x = x;
		this.y = y;
	}
	/**
	 * Sets this point's position to that of the given point.
	 * @param pt The point to copy the position from.
	 */
	public void setPosition(CHPoint pt){
		x = pt.x;
		y = pt.y;
	}
	/**
	 * Gets the point's X coordinate.
	 * @return X Coordinate.
	 */
	public int getX(){
		return x;
	}
	/**
	 * Gets the point's Y coordinate.
	 * @return Y Coordinate.
	 */
	public int getY(){
		return y;
	}
	/**
	 * Human-readable point representation.
	 * @return String describing this point.
	 */
	@Override
	public String toString(){
		return String.format("[%s (%d, %d)]", super.toString(), x, y);
	}
}
