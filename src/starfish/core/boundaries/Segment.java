/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import starfish.core.domain.Mesh;
import static starfish.core.domain.Mesh.Face.BOTTOM;
import static starfish.core.domain.Mesh.Face.LEFT;
import static starfish.core.domain.Mesh.Face.RIGHT;
import static starfish.core.domain.Mesh.Face.TOP;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.common.Vector;


/********** SEGMENT IMPLEMENTATIONS ****************************/



/** Segment is the basic building block of a spline. A segment connects two points
 * either linearly or as a smooth cubic bezier curve*/
public abstract class Segment
{

    /**
     *
     */
    public Boundary boundary;		/*parent boundary*/
    int id;							/*segment id in the parent structure*/
	
    boolean smooth = false;
	
    /**
     *
     */
    protected Segment() {}
	
    /**
     *
     * @param boundary
     * @param id
     */
    public void setParentInfo(Boundary boundary, int id) {this.boundary=boundary;this.id=id;}

    /**
     *
     * @return
     */
    public Boundary getBoundary() {return boundary;}

    /**
     *
     * @return
     */
    public NodeType getBoundaryType() {return boundary.type;}

    /**
     *
     * @return
     */
    public int id() {return id;}
	
    /**
     *
     * @param x1
     * @param x2
     * @return
     */
    public static LinearSegment newLinearSegment(double x1[], double x2[])
    {
	return new LinearSegment(x1,x2);
    }
    	
    /**
     *
     * @return
     */
    public boolean isSmooth() {return smooth;}
	
    double x1[] = new double[3];
    double x2[] = new double[3];

    double centroid[] = new double[3];
    double length;

    /**returns normal vector at segment position t=[0,1
     * @return ]*/
    public final double length() {return length;}

    /**
     *
     * @return
     */
    public final double[] centroid() {return centroid;}

    /**
     *
     * @return
     */
    public final double[][] getBox() 
    {
	double box[][] = new double[2][2];
	box[0][0] = Math.min(x1[0],x2[0]);
	box[0][1] = Math.min(x1[1],x2[1]);
	box[1][0] = Math.max(x1[0],x2[0]);
	box[1][1] = Math.max(x1[1],x2[1]);
	return box;
    }
			    
    /**returns normal vector at segment position t=[0,1
     * @param t]
     * @return */
    public abstract double[] normal(double t);
    
    /**returns tangent vector at segment position t=[0,1
     * @param t]
     * @return */
    public abstract double[] tangent(double t);

    /**returns position at t=[0,1
     * @param t
     * @return ]*/
    public abstract double[] pos(double t);

    /**returns parametric t for position or -1 if not containin
     * @param pos
     * @return g*/
    public abstract double evalT(double pos[]);
    
    /**
     *
     * @return
     */
    public double[] firstPoint() {return x1.clone();}

    /**
     *
     * @return
     */
    public double[] lastPoint() {return x2.clone();}

    /**
     *
     * @param s2
     * @return
     */
    public double intersect0(Segment s2)
    {
	return intersect(s2)[0];
    }

    /**intersects line segments,
     * @param s2
    * @return t[0]  parametric intersection along parent segment
    * @return t[1]  parametric intersection along segment s2*/
    public abstract double[] intersect (Segment s2);

    /**
     *
     * @param x1
     * @param x2
     * @return
     */
    public abstract double[] intersect (double x1[], double x2[]);
    

    /** only defined for linear segments*/
    boolean colinearWith(Segment segment)
    {
	if (this.smooth || segment.smooth) return false;
	
	/*make first ray*/
	double r2[] = new double[2];
	r2[0] = segment.x2[0]-segment.x1[0];
	r2[1] = segment.x2[1]-segment.x1[1];
	
	double r1[] = new double[2];
	r1[0] = x2[0]-x1[0];
	r1[1] = x2[1]-x1[1];
	
	double acos = Vector.dot2(r1, r2)/(Vector.mag2(r1)*Vector.mag2(r2));
	if (acos<0) acos=-acos;
	
	if (Math.abs(acos-1.0)<1e-6) return true;
	return false;	
    }
    
    /**
     * @param b1 * @return a list of segments either intersecting or fully located in a box
     * @param b2
     */
    public boolean segmentInBox(double b1[], double b2[])
    {
	/*first check if the first or last node are in the box*/
	if (inBox(x1,b1,b2) || inBox(x2,b1,b2))
	    return true;
	
	/*check for cuts of box faces*/
	for (Mesh.Face face:Mesh.Face.values())
	{
	    /*face end points*/
	    double f1[] = new double[2];
	    double f2[] = new double[2];

	    switch (face)
	    {
		case BOTTOM:f1[0]=b1[0];f1[1]=b1[1];
			    f2[0]=b2[0];f2[1]=b1[1];
			    break;
		case RIGHT: f1[0]=b2[0];f1[1]=b1[1];
			    f2[0]=b2[0];f2[1]=b2[1];						
			    break;
		case TOP:   f1[0]=b2[0];f1[1]=b2[1];
			    f2[0]=b1[0];f2[1]=b2[1];
			    break;
		case LEFT: f1[0]=b1[0];f1[1]=b2[1];
			    f2[0]=b1[0];f2[1]=b1[1];
			    break;
	    }
	    
	    double tbox[] = intersect(f1,f2);
	    if (tbox[0]<0) continue;
	    return true;
	}
	return false;
    }

    /*returns true if p is in the box given by diagonal points b1 and b2*/

    /**
     *
     * @param p
     * @param b1
     * @param b2
     * @return
     */

    public static boolean inBox(double p[], double b1[], double b2[])
    {
	if (p[0]>=b1[0] && p[0]<=b2[0] &&
		p[1]>=b1[1] && p[1]<=b2[1])
	    return true;
	return false;
    }

    /**
     *
     * @param xp
     * @return
     */
    public abstract double closestPos(double[] xp);
    
}

