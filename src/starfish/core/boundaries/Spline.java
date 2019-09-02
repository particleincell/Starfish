/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import starfish.core.boundaries.Boundary.BoundaryType;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.solver.Matrix;
import starfish.core.common.Vector;

/** Defines a spline, collection of line segments
 * Segments can be linear or smooth cubic Bezier curves
 * 
 * The parametric position along the spline is t=[0,np] where np is the 
 * number of points. t=2.25 indicates position 1/4 of the way from the start of
 * the third segment*/
public class Spline 
{
    /** general constructor*/
    public Spline ()
    {
	/*do nothing*/
    }
    
    /**constructs a spline made of other spline
     * @param splines*/
    public Spline (ArrayList<Spline>splines)
    {
	for (Spline spline:splines)
	{
	    for (int i=0;i<spline.numSegments();i++)
		segments.add(spline.getSegment(i));
	}

	/*recompute box and length*/
	computeBoundingBox();
    }

    /**constructs a spline composed of a single linear segment
     * @param x1
     * @param x2*/
    public Spline (double x1[], double x2[])
    {
	segments.add(Segment.newLinearSegment(x1, x2));

	/*recompute box and length*/
	computeBoundingBox();
    }

    /**
     *
     */
    protected ArrayList<Segment>segments = new ArrayList<>();

    /*bounding box*/
    protected double box[][];
    double spline_length;   //total spline length
    double spline_area;	    //total spline area
    protected double[] cum_area;	   //cumulative distance to the start of i-th segment

    /** @return bounding box of this spline*/
    public double[][] getBox() 
    {	
	return box;
    }

    /**hook to update spline value
     @return true if value changed*/
    boolean update() {return false;}
    
    /**
     *
     */
    public enum SetPathMode { 

	/**
	 *
	 */
	NONE, 

	/**
	 *
	 */
	MOVE, 

	/**
	 *
	 */
	LINEAR, 

	/**
	 *
	 */
	CUBIC, 

	/**
	 *
	 */
	SMOOTH};

    /**Sets the spline using a SVG-like path syntax:
     M x y : moves the current position to x y
     L x y : creates a linear segment from current position to (x,y)
     C k1x k1y k2x k2y x y : creates a cubic segment from current position to (x,y) using knots k1 and k2
     S x y : creates a smooth segment from current position to (x,y)
     Z : adds discontinuity into the spline

     Unlike SVG, the cubic path is specified only by the final position. The control
     points P1 and P2 and calculated automatically such that the path passes through all knots.

     See: http://www.particleincell.com/2012/bezier-splines/ for more info
     * @param path path definition in SVG format, also supports boundary:[first|last]
     * @param T tranformation matrix
     * @param flip_normals true if connectivity should be reversed (flip normals)
     */
    public void setPath(String path, Matrix T, boolean flip_normals)
    {
	SetPathMode mode = SetPathMode.MOVE;
	ArrayList<double[]> points = new ArrayList<>();

	Scanner sc = new Scanner(path);
	sc.useDelimiter("[,\\s\\n\\t]+");
	sc.useLocale(Locale.English)

	/*need [0,0,1] so we can apply translation via transformation matrix*/
	double x1[]={0,0,1};
	double x2[]={0,0,1};
	double k1[] = {0,0,1};
	double k2[] = {0,0,1};

	boolean relative = false;
	
	Log.debug("PATH: "+path);	

	while (sc.hasNext())
	{
	    String str = sc.next();

	    /*process points in the smooth spline if we are changing mode*/
	    if (points.size()>0 && 
		(str.equalsIgnoreCase("M") || str.equalsIgnoreCase("L") ||
		 str.equalsIgnoreCase("C")))
	    {
		if (points.size()>0) 
		    addSmoothSegments(points,flip_normals);
		points.clear();
	    }

	    /*first check if this is one of the special characters*/	    
	    if (str.equalsIgnoreCase("M"))
	    {
		mode = SetPathMode.MOVE;
		relative = str.equals("m");    //lowercase is relative
	    }
	    else if (str.equalsIgnoreCase("L"))
	    {
		mode = SetPathMode.LINEAR;
		relative = str.equals("l");    //lowercase is relative
	    }
	    else if (str.equalsIgnoreCase("C"))
	    {
		mode = SetPathMode.CUBIC;
		relative = str.equals("c");    //lowercase is relative		
	    }
	    else if (str.equalsIgnoreCase("S"))
	    {
		mode = SetPathMode.SMOOTH;
		points.add(x1.clone());
		relative = (str.equals("s"))?true:false;    //lowercase is relative
	    }
	    else if (str.equalsIgnoreCase("Z"))
	    {
		/*simply ignore, M will move to next point as needed*/
	    }
	    else
	    {
		/*point, can be either x,y or spline connector*/

		/*try to parse Double*/
		try{
		    x2[0] = Double.parseDouble(str);	/*may throw exception*/
		    x2[1] = sc.nextDouble();	

		    //TODO: tranformation not working properly
		    /*perform transformation on x2, x1 is already transformed*/
		    x2 = T.mult(x2);
		
		    if (relative) {
			x2[0]+=x1[0];
			x2[1]+=x1[1];			
		    }
		}
		catch (NumberFormatException e)
		{
		    /*not a number, must be spline:first/last*/
		    String pieces[] = str.split(":");
		    Boundary con_spline = Starfish.getBoundary(pieces[0]);
		    if (con_spline==null)
		    	Log.error("connecting boundary "+pieces[0]+" not found");

		    if (pieces[1].equalsIgnoreCase("FIRST"))
		    {
			/*our x2 is [3], return point may be different dimension*/
			double xp[] = con_spline.firstPoint();
			if (flip_normals) xp = con_spline.lastPoint();
			
			x2[0] = xp[0];
			x2[1] = xp[1];			
		    }
		    else if (pieces[1].equalsIgnoreCase("LAST"))
		    {
			double xp[] = con_spline.lastPoint();
			if (flip_normals) xp = con_spline.firstPoint();
			x2[0] = xp[0];
			x2[1] = xp[1];
		    }
		    else
			Log.error("unknown boundary conntector type "+pieces[1]);
		}				
		
		/*if this is a cubic spline we need to capture the 2 knots*/
		if (mode == SetPathMode.CUBIC)
		{
		    k1[0] = x2[0];
		    k1[1] = x2[1];
		    
		    try{
		    k2[0] = sc.nextDouble();
		    k2[1] = sc.nextDouble();
		    x2[0] = sc.nextDouble();
		    x2[1] = sc.nextDouble();
		    }    
		    catch (Exception e)
		    {
			Log.error ("Error parsing cubic spline, syntax: k1x k1y k2x k2y x2 y2");
		    }
		    
		    /*transform k2 and x2, k1 is actually previous x2*/
		    k2 = T.mult(k2);
		    x2 = T.mult(x2);
	
		    if (relative)
		    	for (int i=0;i<2;i++)
			{
			    //k1 and x1 are already offset
			    k2[i]+=x1[i];
			    x2[i]+=x1[i];
			}
	    		   		    
		    addCubicSegment(x1,k1,k2,x2,flip_normals);
		}
		else if (mode == SetPathMode.LINEAR)
			addLinearSegment(x1,x2,flip_normals);
		else if (mode == SetPathMode.SMOOTH)
		    	points.add(x2.clone());	    /*add to collection of points making up the smooth curve*/
		else if (mode == SetPathMode.NONE || mode == SetPathMode.MOVE)
			mode = SetPathMode.LINEAR;
		
		/*copy current position down*/
		x1[0] = x2[0];
		x1[1] = x2[1];
	    }
	}

	/*add any remaining smooth points if any*/
	if (points.size()>0)
	    addSmoothSegments(points,flip_normals);

	/*if reverse, we need to also flip the segment ordering for continuity*/
	if (flip_normals)
	    reverseSegmentOrdering();
	
	if (segments.isEmpty()) Log.error("Failed to add points for spline "+path);
	computeBoundingBox();
	computeGeometry();
    }

    /**
     *
     */
    protected void reverseSegmentOrdering()
    {
	ArrayList<Segment> list = new ArrayList<>();
	int ns=segments.size();
	
	for (int i=0;i<ns;i++)
	    list.add(segments.get(ns-i-1));
	
	segments = list;
    }
    
    /**
     *
     * @param x1
     * @param x2
     * @param reverse
     */
    protected void addLinearSegment(double x1[], double x2[],boolean reverse)
    {
	if (!reverse)
	    segments.add(Segment.newLinearSegment(x1, x2));
	else
	    segments.add(Segment.newLinearSegment(x2, x1));
    }

    /**
     *
     * @param x1
     * @param p1
     * @param p2
     * @param x2
     * @param reverse
     */
    protected void addCubicSegment(double x1[], double p1[], double p2[], double x2[],boolean reverse)
    {
	/*don't add degenerate cubics, these get exported by Gimp*/
	double mag1 = Vector.dist2(x1,p1);
	double mag2 = Vector.dist2(x2,p2);
	
	if (mag1==0 && mag2==0)
	{
	    addLinearSegment(x1,x2,reverse);
	    return;
	}
	
	if (!reverse)
	    segments.add(CubicSegment.newCubicSegment(x1, p1, p2, x2));
	else
	    segments.add(CubicSegment.newCubicSegment(x2, p2, p1, x1));
    }

    /**
     *
     * @param points
     * @param reverse
     */
    protected void addSmoothSegments(ArrayList<double[]> points, boolean reverse)
    {
	int np = points.size();
	double p1[][] = new double[np-1][2];
	double p2[][] = new double[np-1][2];

	/*compute control points*/
	double Kx[] = new double[np];
	double Ky[] = new double[np];
	for (int i=0;i<np;i++)
	{
	    double x[] = points.get(i);
	    Kx[i] = x[0];
	    Ky[i] = x[1];
	}

	double px[][] = computeControlPoints(Kx);
	double py[][] = computeControlPoints(Ky);

	/*repack*/
	for (int i=0;i<np-1;i++)
	{
	    p1[i][0] = px[0][i];
	    p2[i][0] = px[1][i];

	    p1[i][1] = py[0][i];
	    p2[i][1] = py[1][i];
	}

	/*add segments*/
	for (int i=0;i<np-1;i++)
	    addCubicSegment(points.get(i), p1[i], p2[i],points.get(i+1),reverse);
    }

    /*computes control points along a single direction given knots K, code from
     http://www.particleincell.com/2012/bezier-splines/ */
    double[][] computeControlPoints(double K[])
    {
	int n=K.length-1;

	double p[][] = new double[2][n];

	/*matrix*/
	double a[] = new double[n];
	double b[] = new double[n];
	double c[] = new double[n];
	double r[] = new double[n];

	/*left most segment*/
	a[0]=0;
	b[0]=2;
	c[0]=1;
	r[0] = K[0]+2*K[1];

	/*internal segments*/
	for (int i = 1; i < n - 1; i++)
	{
	    a[i]=1;
	    b[i]=4;
	    c[i]=1;
	    r[i] = 4 * K[i] + 2 * K[i+1];
	}

	/*right segment*/
	a[n-1]=2;
	b[n-1]=7;
	c[n-1]=0;
	r[n-1] = 8*K[n-1]+K[n];

	/*solves Ax=b with the Thomas algorithm (from Wikipedia)*/
	for (int i = 1; i < n; i++)
	{
	    double m = a[i]/b[i-1];
	    b[i] = b[i] - m * c[i - 1];
	    r[i] = r[i] - m*r[i-1];
	}

	p[0][n-1] = r[n-1]/b[n-1];
	for (int i = n - 2; i >= 0; --i)
	    p[0][i] = (r[i] - c[i] * p[0][i+1]) / b[i];

	/*we have p1, now compute p2*/
	for (int i=0;i<n-1;i++)
	    p[1][i]=2*K[i+1]-p[0][i+1];

	p[1][n-1]=0.5*(K[n]+p[0][n-1]);

	return p;
    }

    /**
     *
     */
    public final void computeBoundingBox()
    {
	box = new double[2][];
	if (segments.isEmpty()) Log.error("Error computing bounding box");
	box[0] = segments.get(0).firstPoint();
	box[1] = segments.get(0).firstPoint();

	for (int i=0; i<numSegments(); i++)
	{
	    double x[] = segments.get(i).lastPoint();

	    /*bottom left (min) corner*/
	    box[0][0] = Math.min(box[0][0], x[0]);
	    box[0][1] = Math.min(box[0][1], x[1]);

	    /*top right (max) corner*/
	    box[1][0] = Math.max(box[1][0], x[0]);
	    box[1][1] = Math.max(box[1][1], x[1]);
	}
    }

    /** @return first point*/
    public double[] firstPoint()
    {
	return segments.get(0).x1.clone();
    }

    /** @return last point*/
    public double[] lastPoint()
    {
	return segments.get(segments.size()-1).x2.clone();
    }

    /**
     * @param i *  @return i-th point*/
    public double [] getPoint(int i)
    {
	if (i<segments.size())
	    return segments.get(i).x1.clone();
	else 
	    return segments.get(segments.size()-1).x2.clone();
}
    /**
     * @param i *  @return i-th segment*/
    public Segment getSegment(int i) {return segments.get(i);}

    /**
     *
     * @return
     */
    public ArrayList<Segment> getSegments() {return segments;}


    /** @return number of segments*/
    public int numSegments()
    {
	return segments.size();
    }

    /** @return number of points*/  
    public int numPoints()
    {
	return numSegments()+1;
    }

    /**
     * Sets global length and revolved area. This gets called from BoundaryModule.start, since
     * we need to have domain type to compute segment areas
     */
    public final void computeGeometry()
    {
	spline_length = 0;
	spline_area = 0;
	cum_area = new double[numSegments()+1];
	
	/*initialize segment areas now that we know domain type we have*/
	for (Segment seg:segments)
	    seg.computeArea();
		
	cum_area[0] = 0;	 //not really needed but to be explicit
	for (int i=0;i<numSegments();i++)
	{
	    Segment seg = segments.get(i);
	    double L = seg.length();
	    spline_length += L;
	    spline_area += seg.area();
	    cum_area[i+1] = cum_area[i] + seg.area();
	}
    }

    /**
     * Computes surface area centered on i-th node. This is the half of surface
     * areas on both sides. On end points, we have half area.
     * @return node area, area centered around the node
     */
    public double nodeArea(int i)
    {
	if (i>0 && i<numSegments())
	    return 0.5*(segmentArea(i)+segmentArea(i-1));
	else if (i==0)
	    return 0.5*segmentArea(i);
	else
	    return 0.5*segmentArea(numSegments()-1);
    }
    
    /**
     * @param i *  @return area of the segment, analogous to cell area in mesh*/
    public double segmentArea(int i)
    {
	return segments.get(i).area();
    }
    
    /**
     * @param t1
     * @param t2 *  @return area of a strip between two endpoints, assumes line connection  */
    public double stripArea(double t1, double t2)
    {
	double pos1[] = pos(t1);
	double pos2[] = pos(t2);
	double l = Vector.dist2(pos1, pos2);	/*arc length*/
	if (Starfish.domain_module.getDomainType()==DomainType.XY) return l;
	
	/*in axisymmetric case, the strip area is the difference between two cones
	    A = pi* ( r2*(l2+r2) - r1*(l1+r1) )
	*/
	double r1,z1,r2,z2;
	switch (Starfish.domain_module.getDomainType()) {
	    case RZ: r1=pos1[0]; r2=pos2[0]; z1=pos1[1]; z2=pos2[1]; break;
	    case ZR: z1=pos1[0]; z2=pos2[0]; r1=pos1[1]; r2=pos2[1]; break;
	    default: Log.error("Unknown domain type in stripArea");return 0;	
	}
	
	return Math.PI*(r2*r2 - r1*r1 + r2*z2 - r1*z1);
    }

    /** @return "surface" area of the spline*/
    /*TODO: why is this being recomputed?*/
    public double area()
    {
	return spline_area;
    }

    /** @return random parametric position*/
    public double randomT() 
    {
	/*sample uniformly from total spline area*/
	double A1 = Starfish.rnd()*spline_area;
	int i = Vector.binarySearch(cum_area, A1); //map distance to segment
	
	/*compute parametric position along this segment*/
	double seg_area = segments.get(i).area;
	double frac = (A1-cum_area[i])/seg_area;
	
	//area of a conical frustrum 
	
	if (Starfish.getDomainType()!=DomainType.XY)
	{
	    /*for axisymmetric domain we need to search for "t" that will produce
	    the desired fractional area. My attempt for analytical solution has so far
	    been unfruitful as this gives an ugly equation containing sqrt(t) terms
	    */
	    
	    Segment seg = segments.get(i);
	    	    
	    /*
	    this is my attempt at searching for the solution. This is probably the same
	    as Newton's method.	    
	    */
	    final int max_steps = 10;
	    final double tol = 1e-6;
	    double x[] = new double[max_steps];
	    double f[] = new double[max_steps];
	    double f_goal = frac*seg_area;   //what we are looking
	    
	    //initialize. With assume that t ~ area fraction 
	    x[0] = frac;
	    f[0] = seg.area(x[0]);
	    
	    double diff = Math.abs(f[0]-f_goal);
	    
	    int k=1;
	    //set x[1] since this is the final that is used if initial guess is right
	    x[1] = x[0] + (f_goal-f[0]);    
	    	
	    if (diff>tol)
	    	f[1] = seg.area(x[1]);		
	    	    
	    while (diff>tol && k<max_steps-1)
	    {
		x[k+1] = (x[k]-x[k-1])*(f_goal-f[k-1])/(f[k]-f[k-1]) + x[k-1];
		f[k+1] = seg.area(x[k+1]);
		diff = Math.abs(f[k+1]-f_goal);	 
		k++;		
	    }
	    
	   frac = x[k];
	}
	
	/*add random distance along the segment*/
	return i+frac;
    }

    /** @return random parametric position for uniform sampling on RZ mesh*/
    protected double randomTforRZ()
    {
	double t=-1;
	double A;
	double Atot = area();
	double rp, rm;
	
	do {
	    /*pick random segment*/
	    int seg = 0;
	    
	    if (numSegments()>0) seg=(int)(numSegments()*Starfish.rnd());

	    /*add random distance along the segment*/
	    t = seg+Starfish.rnd();
	    
	    /*compute area of a strip centered around the sampled point*/
	    double tp=t+0.01;
	    double tm=t-0.01;
	    if (tp>numSegments()) tp=numSegments();
	    if (tm<0) tm=0;

	    /*r compoment of position*/ 
	    switch (Starfish.getDomainType())
	    {
	    	case RZ:
		    rp = pos(tp)[0]; 		
		    rm = pos(tm)[0];
		    break;
	    	case ZR:
		    rp = pos(tp)[1];
		    rm = pos(tm)[1];
		    break;
	    	default:
		    throw new UnsupportedOperationException("Unknown domain type in RandomT");
	    }
	
	    //swap if needed
	    if (rm>rp) {double s=rp;rp=rm;rm=s;}
	    /*note, A will be zero if source is parallel with r-axis*/
	    A = Math.PI*(rp*rp-rm*rm);
	  
	} while (A>0 && (A/Atot)<Starfish.rnd());
	
	return t;
    }
    
    
    /** @return random position on the spline*/
    public double[] randomPos()
    {
	return pos(randomT());
    }

    /** Evaluates position given by parametric position
     * @param t		parametric position, whole part indicates segment
     * @return		position*/
    public double[] pos(double t)
    {
	int si = (int) t;
	double seg_t = t-si;

	if (si>segments.size()-1) {si=segments.size()-1;seg_t=1.0;}
	return segments.get(si).pos(seg_t);
    }

    /** Returns parametric t for pos if on line
     * 
     * @param pos point position to evaluate
     * @return segment_id.fractional parametric position or -1 if not on the line
     */
    public double evalT(double[] pos)
    {
	for (Segment segment:segments)
	{
	    double t = segment.evalT(pos);
	    if (t>=0.0 && t<=1.0) return segment.id+t;
	}
	return -1;
    }

    /**Intersects the spline with another and returns intersection parametric t
     * only considers segments between start_p and end_p, inclusive
     * @param s2
     * @param start_p
     * @param end_p
     @return parametric position for the "source" spline*/
    public double[] intersect(Spline s2, int start_p, int end_p)
    {
	int i1,i2;
	double t[] = new double[2];

	for (i1=start_p; i1<end_p; i1++)
	{
	    Segment seg1 = getSegment(i1);

	    /*loop over other spline*/
	    for (i2=0; i2<s2.numSegments();i2++)
	    {
		Segment seg2 = s2.getSegment(i2);

		t = seg1.intersect(seg2);

		if (t[0]<0 || t[0]>1) continue;

		/*we have intersection, update t to include segment offset*/
		t[0]+=i1;

		return t;
	    }
	} /*i1*/

	/*did not find an intersection*/
	t[0]=-1;
	t[1]=-1;
	return t;
    }

    /**Intersects two splines and returns intersection parametric t
     * Considers entire spline rang
     * @param s2e
     * @return */
    public double[] intersect(Spline s2)
    {
	return intersect(s2,0,numSegments());
    }

    /**Intersects two splines and returns intersection parametric t for this spline
     * Considers entire spline rang
     * @param s2e
     * @return */
    public double intersect0(Spline s2)
    {
	return intersect(s2)[0];
    }    

    /*returns true if the node is the positive halfspace of the spline*/

    /**
     *
     * @param xp
     * @return
     */

    public boolean isInternal(double xp[])
    {
	/*find the first visible segment*/
	Segment segment = visibleSegment(xp);

	if (segment==null) return false;

	return isInternal(xp,segment);
    }

    /*returns true if the node is the positive halfspace of the spline*/

    /**
     *
     * @param xp
     * @param segment
     * @return
     */

    static public boolean isInternal(double xp[], Segment segment)
    {
	/*internal if we are on a line*/
	if (Spline.isOnLine(xp,segment))
	    return true;		

	double t = segment.closestPos(xp);
	
	/*segment normal vector*/
	double n[] = segment.normal(t);
	
	/*ray to midpoint*/
	double m[] = segment.pos(t);
	
	double r[] = new double[2];
	r[0] = xp[0]-m[0];
	r[1] = xp[1]-m[1];
	double r_mag=Math.sqrt(r[0]*r[0]+r[1]*r[1]);
	
	double cos_theta;
	cos_theta = (r[0]*n[0]+r[1]*n[1])/r_mag;
	if (cos_theta<=0) return true;

	return false;
    }

    //@return true if point xp lies on the segment*/
    private static boolean isOnLine(double xp[], Segment segment) 
    {
	/*grab the end points*/
	double x1[] = segment.firstPoint();
	double x2[] = segment.lastPoint();

	double t;	
	if (x2[0]!=x1[0])
	{
	    t = (xp[0] - x1[0]) / (x2[0]-x1[0]);	
	    if (t<-Constants.FLT_EPS || t>(1.0+Constants.FLT_EPS)) return false;
	}
	else		/*vertical line*/
	{
	    t = (xp[1] - x1[1]) / (x2[1]-x1[1]);	
	    if (t<-Constants.FLT_EPS || t>(1.0+Constants.FLT_EPS)) return false;
	}

	double pos[] = new double[2];
	pos[0] = x1[0] + t*(x2[0]-x1[0]);
	pos[1] = x1[1] + t*(x2[1]-x1[1]);

	double d1= pos[0]-xp[0];
	double d2=pos[1]-xp[1];
	double l=segment.length;
	double d=(d1*d1+d2*d2)/(l*l);
	
	if (d>Constants.FLT_EPS) return false;

	return true;
    }

    /*returns index to first segment that can see the point*/

    /**
     *
     * @param xp
     * @return
     */

    public Segment visibleSegment(double xp[])
    {
	return visibleSegment(xp,this.segments);
    }

    /**
     * @param xp * @return first Boundary segment from the list visible from a point or null
     * @param segments*/
    public static Segment visibleSegment(double xp[], ArrayList<Segment>segments)
    {
	for (int pass=0;pass<2;pass++)
	{
	    for (Segment segment:segments)
	    {
		if (segment.boundary.type!=BoundaryType.DIRICHLET)
		    continue;

		/*during first pass we look only for linear segments*/
		if (pass==0 && segment.isSmooth()) continue;
		else if (pass==1 && !segment.isSmooth()) continue;
		
		/*first check if this point is on this segment*/
		if (Spline.isOnLine(xp,segment))
		    return segment;					

		/*create spline from point to segment centroid*/
		double xc[] = segment.pos(segment.closestPos(xp));
		LinearSegment p2c = new LinearSegment(xp,xc);

		/*clear flag*/
		boolean intersect = false;

		/*loop throught the rest*/
		section2_loop:
		for (Segment segment2:segments)
		{
		    if (segment2==segment) continue;	/*skip self*/
		    if (segment2.boundary.type!=BoundaryType.DIRICHLET)
			continue;

		    /*TODO: this algorithm does not work properly, it is tripped by
		     * lines in series, while it should only handle lines sitting on top of each other*/
		    /*if we have two lines on top of each other, keep the one that will make the point internal*/
		    /*
		    if (segment2.colinearWith(segment) &&
			Spline.isInternal(xp,segment)) continue;
		    */
		    if (segment2.intersect0(p2c)>=0)
		    {
			intersect=true; 
			break section2_loop;
		    }
		} /*section2*/

		if (!intersect)
		    return segment;
	    }	/*for segment*/
	} /*for pass*/

	/*did not find anything*/
	return null;
    }

    /**
     * @param segment * @return centroid of a line segment*/
    public double[] centroid(int segment)
    {
	return segments.get(segment).centroid().clone();
    }

    /**
     * @param t * @return normal vector*/
    public double[] normal(double t)
    {
	int si = (int)t;

	if (si>numSegments()-1) si = numSegments()-1;

	return segments.get(si).normal(t-si).clone();
    }
    
    /**
     * @param t * @return tangent vector*/
    public double[] tangent(double t)
    {
	int si = (int)t;

	if (si>numSegments()-1) si = numSegments()-1;

	return segments.get(si).tangent(t-si).clone();
    }
    

    /**this function returns an array of parametric position [0,1] that split
    a multisegment spline into nn-1 segments. The function enforces segments
    starting at each control point (assuming  nn %gt; num_points). Node spacing will
    be only approximately uniform, since segments must pass through the control points
     * @param nn
     * @param reverse.
     * @return */
    public double[] splitSpline(int nn, boolean reverse)
    {
	double t[] = new double[nn];

	/*set number of divisions for each segment*/
	final int ns = numSegments();
	int tot=0;
	int divs[] = new int[ns];
	double dt[] = new double[ns];

	for (int s=0;s<ns;s++)
	{
	    divs[s] = (int)Math.floor((nn-1)*getSegment(s).length()/spline_length+0.5);
	    dt[s] = getSegment(s).length()/divs[s];
	    tot += divs[s];
	}

	/*we may have leftover segments, assign these to the segments with
	    largest deltas*/
	while(tot<nn-1)
	{
	    double dt_max=-1;
	    int max_s=0;
	    for (int s=0;s<ns;s++)
		if (dt[s]>dt_max) {dt_max=dt[s];max_s=s;}
	
	    divs[max_s]++;
	    dt[max_s]=getSegment(max_s).length/divs[max_s];
	    tot++;
	}

	/*split each segment into the precomputed number of divisions*/
	for (int s=0, i=0;s<ns;s++)
	{
	    /*add start*/
	    t[i++]=s;

	    for (int k=1;k<divs[s];k++)
		t[i++]=s+k/(double)divs[s];
	}
	t[nn-1]=ns;

	/*flip if reverse*/
	if (reverse)
	{
	    double tr[] = new double[nn];
	    for (int i=0;i<nn;i++)
		tr[nn-i-1]=t[i];
	    return tr;
	}
	
	return t;
    }
}

