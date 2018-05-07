/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Field1D;
import starfish.core.boundaries.Segment;
import starfish.core.boundaries.Spline;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;

/**abstract implementation of a mesh with a structured topology*/
public abstract class Mesh 
{
       /** constructor
     *
     * @param ni
     * @param nj
     * @param domain_type
     */
    public Mesh (int ni, int nj, DomainType domain_type)
    {
	this.ni=ni;
        this.nj=nj;
	this.domain_type = domain_type;
		
	this.index=0;
	if (Starfish.domain_module!=null)
	    this.index = Starfish.getMeshList().size();
		
	n_nodes = ni*nj;
        n_cells = (ni-1)*(nj-1);
		
	/*allocate nodes*/      
        node = new Node[ni][nj];
	
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		node[i][j]=new Node();
		node[i][j].type=NodeType.UNKNOWN;
	    }
	
	/*initialize boundary mesh data*/
	boundary_data[Face.RIGHT.value()] = new MeshBoundaryData[nj];
	boundary_data[Face.LEFT.value()] = new MeshBoundaryData[nj];
	boundary_data[Face.TOP.value()] = new MeshBoundaryData[ni];
	boundary_data[Face.BOTTOM.value()] = new MeshBoundaryData[ni];	
	
	for (int j=0;j<nj;j++)
	{
	    boundary_data[Face.LEFT.value()][j]= new MeshBoundaryData();
	    boundary_data[Face.RIGHT.value()][j]= new MeshBoundaryData();	    
	}
	for (int i=0;i<ni;i++)
	{
	    boundary_data[Face.BOTTOM.value()][i]= new MeshBoundaryData();
	    boundary_data[Face.TOP.value()][i]= new MeshBoundaryData();	    
	}

    }
	
    /** called by DomainModule during initialization*/
    public void init()
    {
	if (!virtual) setMeshNeighbors();
    }
    
    /*mesh index corresponds to the DomainModule list, meshes not in the main mesh_list
    * will have duplicate index*/
    protected int index;

     /** @return index identifying this mesh
     */
    public int getIndex() {return index;}

    /** Named constants for mesh faces*/
    public enum Face {

	RIGHT (0),
	TOP(1),
	LEFT(2),
	BOTTOM(3);
	private int val;
	Face(int value) {val=value;}

	/** @return associated value */
	public int value() {return val;}
    }

    /** data structure defined on each node*/
    public static class Node
    {
	public NodeType type;

	public double bc_value;
		
	/**list of splines in this control volume*/
	public ArrayList<Segment>segments = new ArrayList<Segment>();
    }
    
    Field2D node_vol;
    Field2D getNodeVol() {return node_vol;}

    /*data type to specify the type of mesh boundary*/
    static public enum MeshBoundaryType    {
	OPEN(-1),
	DIRICHLET(0),
	NEUMANN(1), 
	PERIODIC(2), 
	SYMMETRY(3), 
	MESH(4), 
	SINK(5), 
	CIRCUIT(6); 
	
	protected int val;
	MeshBoundaryType(int val) {this.val=val;}

	/** @return associated value
	 */
	public int value() {return val;}	
    }
    /** Data structure for storing information on mesh neighbors on boundary nodes
     */
    public static class MeshBoundaryData
    {
	public Mesh neighbor[] = null;
	MeshBoundaryType type;
	public int num_neighbors = 0;
	public double buffer;
	double value;	    //optional value for Dirichlet/Neumann boundaries
    }
	
    MeshBoundaryData boundary_data[][] = new MeshBoundaryData[4][];	/*[face][node_index]*/
	
    /**
     */
    static public enum NodeType {
	BAD(-99), 
	UNKNOWN(-2), 
	OPEN(-1),
	DIRICHLET(0); 
	
	protected int val;
	NodeType(int val) {this.val=val;}

	/** @return associated value
	 */
	public int value() {return val;}
    };
    
    
    /**
     *
     * @param face
     * @param type
     * @param value
     */
    public void setMeshBCType(Face face, MeshBoundaryType type, double value)
    {
	if (face==Face.LEFT || face==Face.RIGHT)
	    for (int j=0;j<nj;j++)
	    {
		boundary_data[face.value()][j].type = type;
		boundary_data[face.value()][j].value = value;
	    }
	else
	    for (int i=0;i<ni;i++)
	    {
		boundary_data[face.value()][i].type = type;
		boundary_data[face.value()][i].value = value;
	    }
    }
    
    /** Returns mesh boundary type at the node of the specified face
     * @param face mesh face
     * @param index node index
     * @return boundary type
     */
    public MeshBoundaryType boundaryType(Face face, int index)
    {
	return boundary_data[face.value()][index].type;
    }
    	
    /*mesh definition*/
    public final int ni,

    /**
     *
     */
    nj;       /*number of nodes*/
    public final int n_nodes,

    /**
     *
     */
    n_cells;

    /**
     *
     */
    public final Node node[][];    

    /*geometry*/
    private DomainType domain_type;

    boolean virtual = false;

    /**
     *
     */
    public void makeVirtual() {virtual=true;}
    
    /*name*/
    String name;

    /**
     *
     * @param name
     */
    public void setName(String name) {this.name = name;}

    /**
     *
     * @return
     */
    public String getName() {return name;}
	
    /**
     *
     * @return
     */
    public Node[][] getNodeArray() {return node;}

    /*accessors*/

    /**
     *
     * @param i
     * @param j
     * @return
     */

    public Node getNode(int i, int j) {return node[i][j];}

    /**
     *
     * @param i
     * @param j
     * @return
     */
    public NodeType nodeType(int i, int j) {return node[i][j].type;}
    
    /** Returns mesh boundary normal vector at corresponding face and position
     * @param face mesh face of interest
     * @param pos  position, must be along the boundary
     * @return boundary normal vector, pointing into the domain
     */
    abstract public double[] boundaryNormal(Face face, double pos[]);
	
    /**
     *
     * @param face
     * @param index
     * @return
     */
    public MeshBoundaryData boundaryData(Face face, int index) {return boundary_data[face.value()][index];}

    /**
     *
     * @param i
     * @param j
     * @return
     */
    public boolean isFarFromSurfNode(int i, int j) 
    { 
	if (node[i][j].segments==null ||
	    node[i][j].segments.isEmpty()) 
	    return true;
	else 
	    return false;
    }
    
    /** @return true if node i,j is a dirichlet node
     * @param i
     * @param j
     */
    public boolean isDirichletNode(int i, int j) {return nodeType(i,j)==NodeType.DIRICHLET;}
    
    /** @return true if node i,j is on a mesh boundary and is of the specified type
     * @param i
     * @param j
     */
    public boolean isMeshBoundaryType(int i, int j,MeshBoundaryType type) {
	if (i==0) return this.boundaryType(Face.LEFT,j)==type;
	else if (i==ni-1) return this.boundaryType(Face.RIGHT,j)==type;
	else if (j==0) return this.boundaryType(Face.BOTTOM,i)==type;
	else if (j==nj-1) return this.boundaryType(Face.TOP,i)==type;
	else return false;
    }
  
    /** @return true if node i,j is a boundary node shared by meshes
     * @param i
     * @param j
     */
    public boolean isMeshBoundary(int i, int j) {
	return isMeshBoundaryType(i,j,MeshBoundaryType.MESH);
    }

    /*constructor*/

 
	    
    /**allocates memory, computes node volumes, and sets boundary cells*/
    void initNodes()
    {
	int i,j;
	
	node_vol = Starfish.getFieldCollection("NodeVol").getField(this);
		
	/*compute node volumes on open nodes*/
	for (i=0;i<ni;i++)
	    for (j=0;j<nj;j++ )
		node_vol.set(i,j,nodeVol(i,j));     
	
	/*now repeat, but use monte carlo on interface nodes to correct volumes*/
	for (i=0;i<ni;i++)
	    for (j=0;j<nj;j++)
		computeInterfaceNodeVol(i,j);
	
    }
   
    /**sets neighbors boundary cells*/
    public void setMeshNeighbors()
    {
	int i,j;
    	
		
	/*set defaults*/
	for (j=0;j<nj;j++)
	{
	    boundary_data[Face.RIGHT.value()][j] = new MeshBoundaryData();
	    boundary_data[Face.LEFT.value()][j] = new MeshBoundaryData();
	}

	for (i=0;i<ni;i++)
	{
	    boundary_data[Face.TOP.value()][i] = new MeshBoundaryData();
	    boundary_data[Face.BOTTOM.value()][i] = new MeshBoundaryData();	
	}
		
	/*modify cells with neighbor meshes*/
	for (Mesh mesh:Starfish.getMeshList())
	{
	    /*skip self*/
	    if (mesh==this) continue;
			
	    for (j=0;j<nj;j++)
	    {
		if (mesh.containsPos(pos(0,j)))				
		    addMeshToBoundary(Face.LEFT,j,mesh);
		if (mesh.containsPos(pos(ni-1,j)))				
		    addMeshToBoundary(Face.RIGHT,j,mesh);
	    }
		
	    for (i=0;i<ni;i++)
	    {
		if (mesh.containsPos(pos(i,0)))				
		    addMeshToBoundary(Face.BOTTOM,i,mesh);
	    		
		if (mesh.containsPos(pos(i,nj-1)))				
		    addMeshToBoundary(Face.TOP,i,mesh);
	    }	    
	}	
    }
	
    /**
     *
     * @param face
     * @param index
     * @param mesh
     */
    protected void addMeshToBoundary(Face face, int  index, Mesh mesh)
    {
	MeshBoundaryData bc=boundary_data[face.value()][index];
	
	if (bc.num_neighbors==0)
	    bc.neighbor = new Mesh[Starfish.getMeshList().size()];
		
	bc.type = MeshBoundaryType.MESH;
	bc.neighbor[bc.num_neighbors]=mesh;
	bc.num_neighbors++;
    }

    /**evaluates position at topological coordinate i,
     * @param ij
     * @param j
     * @return */
    abstract public double[] pos(double i, double j);
	
    /**
     *
     * @param lc
     * @return
     */
    public double[] pos(double lc[] ) 
    {
	return pos(lc[0], lc[1]);
    }

    /**
     * @param i1
     * @param j1 *  @return distance between two points
     * @param i2
     * @param j2*/
    public double dist(double i1,double j1,double i2,double j2)
    {
	double x1[] = pos(i1, j1);
	double x2[] = pos(i2, j2);
	double dx = x1[0]-x2[0];
	double dy = x1[1]-x2[1];
	return Math.sqrt(dx*dx+dy*dy);	
    }
	
    /**
     * @param i * @return random position in cell i,j
     * @param j
     */
    public double[] randomPosInCell(int i, int j) 
    {
	double lc[]=new double[2];
	int c=0;
	do {
	   lc[0] = i+Starfish.rnd();
	   lc[1] = j+Starfish.rnd();
	} while (++c<10 && isInternalPoint(lc));
	
	if (c>=10) Log.error("Failed to find external point in cell "
		+i+" "+j+" ("+pos1(i, j)+", "+pos2(i,j)+")");
	return pos(lc);
    }

    /**returns true if mesh contains the poin
     * @param x
     * @return t*/
    abstract public boolean containsPosStrict(double x[]);
	
    /*returns first component of position*/

    /**
     *
     * @param i
     * @param j
     * @return
     */

    public double pos1(double i, double j)
    {
	double x[] = pos(i,j);
	return x[0];
    }
 
    /*return second component of position*/

    /**
     *
     * @param i
     * @param j
     * @return
     */

    public double pos2(double i, double j)
    {
	double x[] = pos(i,j);
	return x[1];
    }
 
    /**returns radius, taking into account domain type, will be 1 for X
     * @param i
     * @param j
     * @return Y*/
    public double R(double i, double j)
    {
	if (domain_type==DomainType.RZ)
	    return pos1(i,j);
	if (domain_type==DomainType.ZR)
	    return pos2(i,j);
	else
	    return 1;
    }

    
    /*/*evaluates logical coordinates at spatial x1,x2
     *
     * @param x1 
     * @param x2
     * @return
     */

    abstract public double[] XtoL(double x1, double x2);
    
    /** Returns logical coordinate of point x
     *
     * @param x physical coordinate
     * @return
     */
    public double[] XtoL(double x[]) 
    {
	return XtoL(x[0],x[1]);
    }
    
    /*returns integral logical coordinate at spatial location d1,d2*/

    /**
     *
     * @param d1
     * @param d2
     * @return
     */

    public int[] XtoI(double d1, double d2)
    {
	int i[] = new int[2];
	double l[] = XtoL(d1,d2);
	i[0] = (int)l[0];
	i[1] = (int)l[1];
	return i;
    }
    
    /*returns integral logical coordinate at spatial location d[]*/

    /**
     *
     * @param d
     * @return
     */

    public int[] XtoI(double d[])
    {
	return XtoI(d[0],d[1]);
    }
    
    /*converts i,j to node index*/

    /**
     *
     * @param i
     * @param j
     * @return
     */

    public int IJtoN(int i, int j) 
    {
	if (i>=0 && i<ni && j>=0 && j<nj)
	    return i*nj + j;
	return -1;
    }
	
    /**
     *
     * @param i
     * @param j
     * @return
     */
    public int IJtoN(double i, double j) 
    {
	return IJtoN((int)i,(int)j);
    }
    
    /**
     *
     * @param i
     * @param j
     * @return
     */
    public int IJtoC(int i, int j) 
    {
	if (i>=0 && i<ni-1 && j>=0 && j<nj-1)
	    return i*(nj-1) + j;
	return -1;
    }

    /**
     *
     * @param i
     * @param j
     * @return
     */
    public int IJtoC(double i, double j) 
    {
	return IJtoC((int)i,(int)j);
    }
	
    /**returns area for node/cell centered at i,
     * @param i0
     * @param j0
     * @return j*/
    public double area(double i0,double j0)
    {
	return area(i0,j0,0.5);
    }
    
    /**returns area around a node/cell centered at i,j
     * @param i0
     * @param j0
    *@param delta indicates the width in each direction, 0.5 will give a node volume
     * @return */
    public double area(double i0,double j0, double delta)
    {
	int e;
	double V[][]=new double[4][];
	double i,j;
	
	/*first set the four corner positions, these may not be nodes */
	for (e=0;e<4;e++)
	{
	    switch (e)
	    {
	    case 0: i=i0-delta;j=j0-delta;break; /*bottom left corner*/
	    case 1: i=i0+delta;j=j0-delta;break;
	    case 2: i=i0+delta;j=j0+delta;break;
	    default:
	    case 3: i=i0-delta;j=j0+delta;break;
	    }

	    if (i<0) i=0;
	    else if (i>ni-1) i=ni-1;
			
	    if (j<0) j=0;
	    else if (j>nj-1) j=nj-1;
			
	    V[e] = pos(i,j);
	}

	/* compute cell area */
	double A;
	A=0.5*Math.abs(V[0][0]*V[1][1] - V[0][0]*V[2][1] +
			V[1][0]*V[2][1] - V[1][0]*V[0][1] +
			V[2][0]*V[0][1] - V[2][0]*V[1][1]);

	A+=0.5*Math.abs(V[0][0]*V[2][1] - V[0][0]*V[3][1] +
			V[2][0]*V[3][1] - V[2][0]*V[0][1] +
			V[3][0]*V[0][1] - V[3][0]*V[2][1]);
	return A;
    }

    /**
     * @param i0 *  @return volume for node i,j
     * @param j0*/
    public double nodeVol(double i0,double j0)
    {
	if (domain_type==DomainType.XY)
		return 1e-3*area(i0,j0);    //TODO: need better way to set domain width to simplify comparison of RZ to XY
	
	/*axisymmetric mesh*/
	
	double r = R(i0,j0);
	//on centerline, we revolve (half area) through circle at r+0.25dr
	if (domain_type==DomainType.RZ)
	{
	    if (i0==0)
		r = R(i0+0.25,j0);
	    else if (i0==ni-1)
		r = R(i0-0.25,j0);	    
	}
	else if (domain_type==DomainType.ZR)
	{
	    if (j0==0)
		r = R(i0,j0+0.25);
	    else if (j0==nj-1)
		r = R(i0,j0-0.25);
	}

	return 2*Math.PI*area(i0,j0)*r;
    }
	
    /**
     * @param i * @return volume for cell i,j
     * @param j*/
    public double cellVol(int i, int j)
    {
	double lc[] = {i+0.5,j+0.5};
	return node_vol.gather(lc);
    }

    /*uses monte carlo approach to compute node volumes in interface node control volumes*/

    /**
     *
     * @param i
     * @param j
     */

    public void computeInterfaceNodeVol(int i, int j)
    {
	//only process nodes with surface elements
	if (node[i][j].segments.isEmpty()) return;
	
	int inside = 0;
	int good = 0;
	for (int d=0;d<1000;d++)
	{
	    /*pick random position from [i-0.5,j-0.5] to [i+0.5,j+0.5]*/
	    double lc[] = {i+0.5*Starfish.rnd2(), j+0.5*Starfish.rnd2()};
	    
	    //is point inside the mesh?
	    if (lc[0]<0 || lc[1]<0 || lc[0]>=ni-1 || lc[1]>=nj-1) continue;
	    
	    inside++;
	    
	    if (!isInternalPoint(lc)) good++;

	}
	

	//scale node volume, but only on interface nodes (fully internal are left alone so can visualize leaks)
	if (good>0)
	    node_vol.data[i][j]*=good/(double)(inside);
	
    }
    
    
    /*returns the two nodes making up edge number e,
     *ordering is counter clockwise from "Right" (R->T->L->B)*/

    /**
     *
     * @param i
     * @param j
     * @param face
     * @param first
     * @return
     */

    public double[] edge(double i, double j, Face face, boolean first) 
    {
	double fi=0,fj=0;	/*first node*/
	double si=0,sj=0;	/*second node*/
	double ri,rj;

	switch (face)
	{
	    case RIGHT: fi=i+0.5;fj=j-0.5;si=i+0.5;sj=j+0.5;break;
	    case TOP: fi=i+0.5;fj=j+0.5;si=i-0.5;sj=j+0.5;break;
	    case LEFT: fi=i-0.5;fj=j+0.5;si=i-0.5;sj=j-0.5;break;
	    case BOTTOM: fi=i-0.5;fj=j-0.5;si=i+0.5;sj=j-0.5;break;
	    default: throw new UnsupportedOperationException("Wrong edge in call to Edge");
	}

	if (first)
	{
	    ri=fi;rj=fj;
	}
	else
	{
	    ri=si;rj=sj;
	}

	if (ri<0) ri=0;
	else if (ri>ni-1) ri=ni-1;
	if (rj<0) rj=0;
	else if (rj>=nj-1) rj=nj-1;
	return pos(ri,rj);
    }

    /**returns index of node offset by di,dj from im,j
     * @param im
     * @param jm
     * @param dim
     * @param dj
     * @return */
    public int[] NodeIndexOffset(double im, double jm, double di, double dj) 
    {
	double i,j;
	double fi,fj;
	int ii[] = new int[2];

	i=im+di;
	j=jm+dj;

	/*is this a fractional index? if so, keep original*/
	fi=i-(int)i;
	fj=j-(int)j;

	if (fi!=0) 
	    ii[0] = (int)im;
	else ii[0] = (int)i;

	if (fj!=0) 
	    ii[1] = (int)jm;
	else ii[1] = (int)j;

	/*make sure we are in bounds*/
	if (ii[0]<0) ii[0]=0;
	if (ii[1]<0) ii[1]=0;
	if (ii[0]>=ni) ii[0]=ni-1;
	if (ii[1]>=nj) ii[1]=nj-1;	
	
	return ii;
    }

    /** computes node cuts and performs flood fill
     * @param boundary_list
     */
    public void setBoundaries(ArrayList<Boundary>boundary_list)
    {
	if (!boundary_list.isEmpty())
	{
	    setNodeControlVolumes(boundary_list);
	    setInterfaceNodeLocation();		
	    performFloodFill();
	}
    }
	
    /** marks boundaries located in a volume centered about each node
     * @param boundary_list
     */
    protected void setNodeControlVolumes(ArrayList<Boundary>boundary_list)
    {
	int i,j;
		
	/*set node control volumes*/
	for (Boundary boundary:boundary_list)
	{
	    for (Segment segment:boundary.getSegments())
	    {
		/*get spline range*/
		double box[][] = segment.getBox();

		/*convert to logical coordinates*/
		int lcm[] = XtoI(box[0]);
		int lcp[] = XtoI(box[1]);

		/*expand*/
		lcm[0]--;
		lcm[1]--;

		lcp[0]+=2;
		lcp[1]+=2;

		/*make sure we are in domain*/
		if (lcm[0]<0) lcm[0]=0;
		if (lcm[1]<0) lcm[1]=0;

		if (lcp[0]>=ni-1) lcp[0]=ni-1;
		if (lcp[1]>=nj-1) lcp[1]=nj-1;

		/*TODO: this algorithm will not detect a boudaries in neighbor
		* meshes, need some post set "all reduce" operation or some way to
		* grow the mesh into the neighbor one		 */

		/*loop through all nodes and set volumes*/
		for (j=lcm[1];j<=lcp[1];j++)
		    for (i=lcm[0];i<=lcp[0];i++)
		    {				
			/*number of cells the box will grow in each direction,
			 * want this to be >1.0 so that we capture elements terminating
			 * at the cell boundary */
			final double bsize=1.01;

			/*node to bottom left*/
			double i2=i-bsize;
			double j2=j-bsize;

			double ncv_m[] = pos(i2,j2);

			i2=i+bsize;j2=j+bsize;
			double ncv_p[] = pos(i2,j2);

			if (!segment.segmentInBox(ncv_m,ncv_p))
			    continue;

			boolean found = false;

			/*see if we already have this boundary*/
			for (Segment seg:node[i][j].segments)
			    {
				if (seg.getBoundary()==boundary &&
				    seg.id()==segment.id())
				{found = true; break;}
			    }

			/*not found, add*/
			if (!found)
			{
			    if (node[i][j].segments==null)
				   node[i][j].segments = new ArrayList<Segment>();
			    node[i][j].segments.add(segment);
			}
					 
		    } /*node loop*/
		}   /*segment*/
	    } /*boundary*/
	}
	
    /*return if point is located inside or outside a surface in interface cell*/

    /**
     *
     * @param lc
     * @return
     */

    public boolean isInternalPoint(double lc[])
    {
	/*don't remember anymore how segments are set so check all four cell vertices*/
	int i1 = (int)lc[0];
	int j1 = (int)lc[1];
	
	int i2 = i1+1;
	int j2 = j1+1;
	if (i2>=ni) i2=ni-1;
	if (j2>=nj) j2=nj-1;
	
	double x[] = pos(lc);
	
	for (int i=i1;i<=i2;i++)
	    for (int j=j1;j<=j2;j++)
	    {	
		ArrayList<Segment> blist = node[i][j].segments;
		if (blist.isEmpty()) continue;
	
		Segment seg = Spline.visibleSegment(x, blist);
		if (seg==null)
		    continue;
				
		return Spline.isInternal(x,seg);
	    }
	return false;
    }
    
	/*uses boundaries located in a node control volume to set node locations*/

    /**
     *
     */

	protected void setInterfaceNodeLocation()
	{
	    int i,j;
		
	    /*now loop through all nodes and set the ones with cuts*/
	    for (i=0;i<ni;i++)
		for (j=0;j<nj;j++)
		{
		    ArrayList<Segment> blist = node[i][j].segments;
		    if (blist.isEmpty()) continue;
	
		    Segment seg = Spline.visibleSegment(pos(i,j), blist);
		    if (seg==null)
		    {
			/*no internal Dirichlet splines*/
			continue;
		    }
				
		    if (Spline.isInternal(pos(i,j),seg))
		    {
			node[i][j].type = NodeType.DIRICHLET;
			node[i][j].bc_value = seg.boundary.getValue();
		    }		    
		    else if (node[i][j].type==NodeType.UNKNOWN) //do not overwrite MESH nodes
		    {
			node[i][j].type = NodeType.OPEN;
			node[i][j].bc_value = 0;
		    }
		}
	}
	
    /**
     *
     */
    protected void performFloodFill()
	{
	    int i,j;
		
	    /*perform flood fill, set some maximum number of passes to avoid infinite loops*/
	    int count=0;
	    for (int pass=0; pass<20*ni*nj;pass++)
	    {
		count = 0;

		for (i=0;i<ni;i++)
		    for (j=0;j<nj;j++)
		    {
			if (node[i][j].type!=NodeType.UNKNOWN)
			    continue;

			if (i>0 && okToCopy(i-1,j))
			{
			    node[i][j].type = node[i-1][j].type;
			    node[i][j].bc_value = node[i-1][j].bc_value;
			    count++;
			}
			else if (i<ni-1 && okToCopy(i+1,j))
			{
			    node[i][j].type = node[i+1][j].type;
			    node[i][j].bc_value = node[i+1][j].bc_value;
			    count++;
			}
			else if (j>0 && okToCopy(i,j-1))
			{
			    node[i][j].type = node[i][j-1].type;
			    node[i][j].bc_value = node[i][j-1].bc_value;
			    count++;
			}
			else if (j<nj-1 && okToCopy(i,j+1))
			{
			    node[i][j].type = node[i][j+1].type;
			    node[i][j].bc_value = node[i][j+1].bc_value;
			    count++;
			}
		    } /*j*/

		    /*this indicates that we did not set any more nodes*/
		    if (count==0) 
			break;
		} /*pass*/

		if (count>0)
		    throw (new RuntimeException("Failed to set all nodes"));
	}
	
    /**
     * Check if mesh contains a point, inclusive of all boundaries
     * @param x
     * @return
     */
    public boolean containsPos(double x[]) 
	{
	    double lc[] = XtoL(x);
	    if (lc[0]<-Constants.FLT_EPS || lc[1]<-Constants.FLT_EPS ||
		lc[0]>(ni-1+Constants.FLT_EPS) || lc[1]>(nj-1+Constants.FLT_EPS))
	    return false;
		
	    return true;
	}
	
    /**
     *
     * @param i
     * @param j
     * @return
     */
    protected boolean okToCopy(int i, int j)
	{
	    if (node[i][j].type==NodeType.OPEN ||
		node[i][j].type==NodeType.DIRICHLET) return true;
		
	    return false;
	}
	
	/**saves the mesh, useful for debugging
     * @param file_name*/
	public void save(String file_name)
	{
	    /*create an empty map*/
	    LinkedHashMap<String,Field2D> fields = new LinkedHashMap<String,Field2D>();
	    save(file_name,fields);
	}
	
    /**
     * Saves the mesh along with the specified 2D data
     * @param file_name
     * @param fields
     */
    public void save(String file_name, HashMap<String,Field2D> fields) 
    {
	PrintWriter pw = null;
	try {
	    pw = new PrintWriter(new FileWriter(file_name));
	} catch (IOException ex) 
	{
	    Log.error("error opening file "+file_name);
	}
	
	pw.println("<?xml version=\"1.0\"?>");
	pw.println("<VTKFile type=\"StructuredGrid\">");
	pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", ni-1,nj-1);
	pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n",ni-1,nj-1);
	   
	pw.println("<Points>");
	pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	for (int j=0;j<nj;j++)
	    for	(int i=0;i<ni;i++)
	    {
		double x[] = pos(i,j);			
		pw.printf("%g %g 0 ", x[0], x[1]);
	    }
	pw.println("\n</DataArray>");
	pw.println("</Points>");
	
	pw.println("<PointData>");
	for (Entry<String,Field2D> pair:fields.entrySet())
	{
	    String var = pair.getKey();
	    Field2D field = pair.getValue();
	    
	    double data[][] = field.getData();
	    
	    pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (int j=0;j<nj;j++)
	  	for (int i=0;i<ni;i++)
		    pw.printf("%g ",(data[i][j]));
	    pw.println("\n</DataArray>");
	}
	
	pw.println("</PointData>");	
	 
	 
	 pw.println("</Piece>");
	 pw.println("</StructuredGrid>");
	 pw.println("</VTKFile>");
	/*save output file*/
        pw.close();
	  
  	}
    
    /**
     * Saves the mesh along with the specified 2D data and 1D (constant along j index) data
     * This function saves in Paraview format
     * @param file_name file name
     * @param fields2d list of 2D fields to save
     * @param fields1d list of 1D fields to save
     */
    public void save(String file_name, LinkedHashMap<String,Field2D> fields2d, 
	    LinkedHashMap<String,Field1D> fields1d) 
	{
	    /*open file*/
	    PrintWriter pw = null;
	    try 
	    {
		pw = new PrintWriter(new FileWriter(file_name));
	    } 
	    catch (IOException ex) 
	    {
		Log.error("Failed to open input file "+file_name);
	    }
	    
	    pw.println("<?xml version=\"1.0\"?>");
	    pw.println("<VTKFile type=\"StructuredGrid\">");
	    pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", ni-1,nj-1);
	    pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n",ni-1,nj-1);
	   
	    pw.println("<Points>");
	    pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	    for (int j=0;j<nj;j++)
		for	(int i=0;i<ni;i++)
		{
		    double x[] = pos(i,j);			
		    pw.printf("%g %g 0 ", x[0], x[1]);
		}
	    pw.println("\n</DataArray>");
	    pw.println("</Points>");
	
	    pw.println("<PointData>");
	    pw.println("<DataArray Name=\"type\" type=\"Int32\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (int j=0;j<nj;j++)
		for	(int i=0;i<ni;i++)
		    pw.printf("%d ",getNode(i,j).type.value());
	    pw.println("\n</DataArray>");
	
	    /*2d data*/
	    for (Map.Entry<String, Field2D> pair : fields2d.entrySet()) 
	    {
		String var = pair.getKey();
		Field2D field = pair.getValue();	    
		double data[][] = field.getData();
	    
		pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
		for (int j=0;j<nj;j++)
		    for (int i=0;i<ni;i++)
			pw.printf("%g ",(data[i][j]));
		pw.println("\n</DataArray>");
	    }
	    
	    /*1d data*/
	    for (Map.Entry<String, Field1D> pair : fields1d.entrySet()) 
	    {
		String var = pair.getKey();
		Field1D field = pair.getValue();	    
		double data[]= field.getData();
	    
		pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
		for (int j=0;j<nj;j++)
		    for (int i=0;i<ni;i++)
			pw.printf("%g ",(data[i]));
		pw.println("\n</DataArray>");
	    }
		
	pw.println("</PointData>");		 
	pw.println("</Piece>");
	pw.println("</StructuredGrid>");
	pw.println("</VTKFile>");
	/*save output file*/
        pw.close();
   
    }


}
