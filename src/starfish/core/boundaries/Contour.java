/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import java.io.FileOutputStream;
import java.io.PrintStream;
import starfish.core.common.Starfish;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.Face;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Mesh.MeshBoundaryType;

/**spline which defines a contour line*/
public class Contour 
{

    /**
     *
     */
    protected double value;
	
    /**Creates a contour of field collection at value bounded by start and end splines
     * @param value 
     * @param field_collection
     * @param start
     * @param end
     * @param num_points*/
    public Contour (double value, FieldCollection2D field_collection, Spline start, Spline end, int num_points)
    {		
	int i,j;
		
	this.value = value;
	
	//point = new ContourPoint[np];
	point = new ContourPoint[max_segments];
	for (i=0;i<max_segments;i++) point[i]=new ContourPoint();
		
	this.np = 0;
		
	int edge;

	/*** 1) Find the first cell ********************************/
	/* TODO: this is not a very efficient algorithm, revise*/
	boolean found = false;
	double max_t = start.numPoints();
	double t=0;
	Mesh mesh=null;
	Field2D field=null;
		
	mesh_loop:
	for (int m=0;m<Starfish.getMeshList().size();m++)
	{
	    mesh = Starfish.getMeshList().get(m);	   
	    field = field_collection.getField(mesh);
			
	    /*set tolerance*/
	    double range[] = field.getRange();
	    double tolerance = Math.abs(1e-5*(range[1]-range[0]));
	    if (tolerance < Float.MIN_VALUE)
		Log.warning("No variation in field value");
			
	    /*loop through segments, this could probably be sped up using some better search algorithm*/
			
	    /*find first point it the mesh*/
	    double dt=0.1;
			
	    t = 0;
	    double x1[]= start.pos(0);
	    while(mesh.containsPos(x1)==false && t<max_t)
	    {
		t+=dt;
		x1 = start.pos(t);
	    }
			
	    if (t>=max_t) continue;	/*not found*/
			
	    double v1 = field.eval(x1);
	    if (Math.abs(v1-value)<tolerance) {found=true; break mesh_loop;}
			
	    do
	    {
		t+=dt;
				
		double x2[] = start.pos(t);	
		if (!mesh.containsPos(x2))
		    break;		/*this will not catch value near boundaries!*/
				
		double v2 = field.eval(x2);
				
		/*have we found the value?*/
		if (Math.abs(v2-value)<tolerance) {found=true; break mesh_loop;}
				
		/*check if the value is between the current and the last position
		check for v2<v1 to capture maximum lambda line beyond which lambda=0
		*/		
		if ((v1-value)*(v2-value)<0 || (v2<v1 && value==0.0))
		{
		    //double f = (value-v1)/(v2-v1);	    
		    //t-=(1-f)*dt;
		    t -= dt;
		    dt=0.5*dt;
		    continue;
		}
		
		//so we don't run forever
		if (dt<1e-6) break;
				
		/*else copy down and continue*/
		v1=v2;
	    		
	    } while(t<start.numSegments()+1);	
	}
		
	if (mesh==null || !found)
	{
	    Log.warning(String.format("Failed to find contour line for %g",value));
	    return;
	}

		
	/*2) create the contour from the starting point by intersecting cell edges,
	* this will result in a number of control points different from the 
	* prescribed value */
		
	double x[] = start.pos(t);
	double lc[] = mesh.XtoL(x);
	i = (int)lc[0];
	j = (int)lc[1];
		
	/*add the first point*/
	AddPoint(mesh,x,lc);
		
	Mesh mesh_old = mesh;	/*used to keep track of mesh changes*/
	edge=0;
	
	/*loop over rest*/
	while (true)
	{
	    if (mesh != mesh_old)
	    {
		field=field_collection.getField(mesh);
		
		lc=mesh.XtoL(point[np-1].x);
		i = (int)lc[0];
		j = (int)lc[1];			
		mesh_old=mesh;
	    }
		
	    /*make sure field is set*/
	    if (field==null) continue;
		
	    edge=SetCell(field,i,j,edge,end);
	    if (edge<0) break;	
    		
	    /*move to next cell, taking into account multiple meshes?*/
	    if (edge==0)	/*bottom*/
	    {
		edge=2;
		if (j==0 && mesh.boundaryType(Face.BOTTOM,i)==MeshBoundaryType.MESH)
		{
		    mesh = mesh.boundaryData(Face.BOTTOM, i).neighbor[0];
		    continue;
		}
		j--;
	    }
	    else if (edge==1)	/*right*/
	    {
		edge=3;
		if (i==field.getNi()-2 && mesh.boundaryType(Face.RIGHT,j)==MeshBoundaryType.MESH)
		{
		    mesh = mesh.boundaryData(Face.RIGHT, j).neighbor[0];
		    continue;
		}
		i++;
	    }
	    else if (edge==2)	/*top*/
	    {
		edge=0;
		if (j==field.getNj()-2 && mesh.boundaryType(Face.TOP,i)==MeshBoundaryType.MESH)
		{
		    mesh = mesh.boundaryData(Face.TOP, i).neighbor[0];
		    continue;
		}
		j++;
	    }
	    else if (edge==3)	/*left*/
	    {
		edge=1;
		if (i==0 && mesh.boundaryType(Face.LEFT,j)==MeshBoundaryType.MESH)
		{
		    mesh = mesh.boundaryData(Face.LEFT, j).neighbor[0];
		    continue;
		}
		i--;
	    }
    		
	}
		
	if (np<2) 
	{
	    Log.warning("Contour with less than 2 data points");
	    return;
	}
			
	//this.save("contour-full.csv");

	/*3) describe the contour by num_points equidistant points*/
	/*compute length*/
	double length = 0;
	for (i=0;i<np-1;i++)
	{
	    double dx = point[i+1].x[0]-point[i].x[0];
	    double dy = point[i+1].x[1]-point[i].x[1];
	    point[i].ds = Math.sqrt(dx*dx + dy*dy);
	    if (point[i].ds<=0) 
	    {
		/*elimininate the next point*/
		for (j=i+1;j<np-2;j++)
		    point[j]=point[j+1];
		i--; //to recompute
		np--;	//reduce point count
		continue;
	    }		
	    length+=point[i].ds;
	}

	double ds = length/(num_points-1);
		
	/*create a new temporary array for filling positions*/
	ContourPoint up[] = new ContourPoint[num_points];
	
	/*copy first and last points*/
	up[0] = point[0].clone();
	up[num_points-1]=point[np-1].clone();
		
	/*set internal points*/
	double accum=0;
	j=0;		/*point index*/
	    
	for (i=1;i<num_points-1;i++)
	{
	    double s=i*ds;
	    while((accum+point[j].ds)<=s)
	    {
		accum+=point[j].ds;
		j++;
		if (j>=np)
		    Log.error("Error parsing contour");
	    }
	    double f=(s-accum)/point[j].ds;
	    up[i] = new ContourPoint();
	    up[i].x[0] = point[j].x[0]+f*(point[j+1].x[0]-point[j].x[0]);
	    up[i].x[1] = point[j].x[1]+f*(point[j+1].x[1]-point[j].x[1]);
	    up[i].mesh = point[j].mesh;
	    if (!up[i].mesh.containsPos(up[i].x))
		    up[i].mesh = point[j+1].mesh;		/*assuming that the point must be in one of the two possible meshes*/				    
	}

	point = up;
	np = num_points;

	/*recompute ds for all points*/
	for (i=0;i<np-1;i++)
	{
	    double dx = point[i+1].x[0]-point[i].x[0];
	    double dy = point[i+1].x[1]-point[i].x[1];
	    point[i].ds = Math.sqrt(dx*dx + dy*dy);
	}
	
	
	


    }

    /** SetCell
     * this function adds control point on cell specified by
     * i,j, skipping over ignore edge (counterclockwise from XMAX)
     * function returns the edge that was set or -1 if not found
     * @param field
     * @param i
     * @param j
     * @param ignore
     * @param end
     * @return */
    protected final int SetCell (Field2D field, int i, int j, int ignore, Spline end)
    {
	/*are we in domain?*/
	if (i<0 || j<0 ||
	    i>=field.getNi()-1 || j>=field.getNj()-1) return -1;

	/* go through edges in order, skipping over set edge
	* if edge is set to -1 (or anything other than 0-3), we will
	* check all edges
	*/

	int edge;
	double delta_left,delta_right;
	double left, right;
	double L,px[];		/*using capital L to distinguish easier from 1*/

	double data[][] = field.getData();

	/*loop over edges, starting with the last one*/
	edge= ignore;
	for (int e=0;e<3;e++)
	{
	    edge++;
	    if (edge>3) edge=0;
	    
	    switch (edge)
	    {
		case 0: left=data[i][j];right=data[i+1][j];break;
		case 1: left=data[i+1][j];right=data[i+1][j+1];break;
		case 2: left=data[i][j+1];right=data[i+1][j+1];break;
		case 3: left=data[i][j];right=data[i][j+1];break;
		default: return -1;
	    }

	    delta_left=left-value;
	    delta_right=right-value;
	    
	    if (delta_left*delta_right>0)
		continue;  /*not in here*/

	    if (Math.abs(right-left)<Float.MIN_VALUE)
		return -1; //Log.error("algorithm error in Contour");

	    /*get logical coordinate of control point*/ 
	    L = (value-left)/(right-left);

	    double f[] = new double[2];

	    /*now convert to physical*/
	    switch(edge)
	    {
	    case 0: f[0]=i+L;f[1]=j;break;
	    case 1: f[0]=i+1;f[1]=j+L;break;
	    case 2: f[0]=i+L;f[1]=j+1;break;
	    case 3: f[0]=i;f[1]=j+L;break;
	    }

	    px=field.pos(f[0],f[1]);
	    
	    /*skip points at lower radial position than prior,
	    this is to prevent the contouring below the starting spline   */
	    if (px[1]<point[numPoints()-1].x[1]) 
		continue;

	    /*do we already have this point?*/
	    /*TODO: implement*/

	    /*test for intersection with the end spline*/
	    double t = end.intersect0(new Spline(point[np-1].x,px));
	    if (t>0)
		px = end.pos(t);

	    /*save just the intersections with j grid lines*/
	    if (AddPoint(field.getMesh(),px,f))
		return edge;
	    else return -1; //if we ran out of points
	}

	/*not found*/
	return -1;
    }

    /**
     *
     * @param xi
     * @param xj
     */
    public void pos(double[] xi, double[] xj) 
    {
	for (int i=0;i<xi.length;i++)
	{
	    xi[i]=point[i].x[0];
	    xj[i]=point[i].x[1];
	}
    }

    /**
     *
     * @param ds
     */
    public void ds(double[] ds) 
    {
	for (int i=0;i<ds.length;i++)
	{
	    ds[i]=point[i].ds;
	}
    }

    class ContourPoint
    {
	double x[] = new double[2];		/*position*/
	double lc[] = new double[2];	/*logical coordinates*/
	Mesh mesh;					/*mesh owning this point*/
	double ds;					/*linear distance to next point*/

	@Override
	public ContourPoint clone()
	{
	    ContourPoint n = new ContourPoint();
	    n.x = x.clone();
	    n.lc = lc.clone();
	    n.mesh = mesh;
	    n.ds = ds;
	    return n;
	}
    }
    ContourPoint point[];
    int np;

    /**
     *
     * @param i
     * @return
     */
    public double getFi(int i) {return point[i].lc[0];}

    /**
     *
     * @param i
     * @return
     */
    public double getFj(int i) {return point[i].lc[1];}

    /**
     *
     * @param i
     * @return
     */
    public Mesh getMesh(int i) {return point[i].mesh;}

    /**
     *
     * @return
     */
    public int numPoints() {return np;}

    /**
     *
     */
    protected final int max_segments=1000;

    /**
     *
     */
    protected final int segments_in_box=500;		/*number of line segments in a box*/

    /* AddPoint
     * Adds new point (segment) and also updates bounding boxes*/

    /**
     *
     * @param mesh
     * @param x
     * @param lc
     * @return
     */

    public final boolean AddPoint(Mesh mesh, double x[], double lc[])
    {
    //	System.out.printf("%d: %g, %g\n", np,x[0],x[1]);
	point[np].x=x.clone();
	point[np].lc=lc.clone();
	point[np].mesh = mesh;
	np++;
	if (np>=max_segments) return false;
	
	return true;
    }

    /*returns array of distances to the next point*/

    /**
     *
     * @return
     */

    public double[] getDs()
    {
	double ds[] = new double[np];

	for (int i=0;i<np-1;i++)
	{
	    double dz = point[i+1].x[0]-point[i].x[0];
	    double dr = point[i+1].x[1]-point[i].x[1];
	    ds[i] = Math.sqrt(dz*dz+dr*dr);
	}

	/*ds[ns-1] will be zero*/
	return ds;
    }
    
    /*Save
     * Saves spline to a file in tecplot format
     */

    /**
     *
     * @param filename
     */

    public final void save(String filename)
    {
	FileOutputStream out; 
	PrintStream p = null; 
	int i;

	if (np<1)
	    return;

	try
	{
	    out = new FileOutputStream(filename);
	    p = new PrintStream( out );

	    p.println ("x,y");
	}
	catch (Exception e)
	{
	    System.err.println ("Error writing results");
	}

	/*iterate over the field*/
	for (i=0;i<np;i++)
	    p.printf("%g, %g\n", point[i].x[0],point[i].x[1]);

	p.close();
    }
}
