/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.solver;

import starfish.core.common.Vector;
import java.util.ArrayList;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.Face;
import starfish.core.domain.Mesh.DomainBoundaryType;
import starfish.core.domain.Mesh.Node;

/** Finite volume Matrix solver for Ax=b using the Gauss-Seidel method (for now)
 */
public abstract class Solver
{

    /** constructor
     * 
     * @param lin_solver solver method to use for the linear Ax=b equation
     */
    public void setLinearSolver(LinearSolver lin_solver)
    {
	this.lin_solver = lin_solver;
    }
    
    /**
     *
     */
    protected int lin_max_it;

    /**
     *
     */
    protected double lin_tol;

    /**
     *
     */
    protected int nl_max_it;

    /**
     *
     */
    protected double nl_tol;
   

    boolean first = true;
    boolean initial_only = false;

    /* gradient class used to store gradient coefficients at control volume 
     * edge midpoints (i+1/2,j) (i,j+1/2) and so on*/

    class Gradient
    {
	double Gi[] = new double[6]; /* i derivative */
	double Gj[] = new double[6]; /* j derivative */
	double R;		    /* radius of calculation*/
	
	int[] u = {-1, -1, -1, -1, -1, -1}; /*unknown index for matrix assembly*/
	
	//support for multiple domains, these complement the "u" array
	Mesh neighbor_mesh[] = new Mesh[6];
	double neighbor_lc[][] = new double[6][];
	double neighbor_Gi[] = new double[6];
	double neighbor_Gj[] = new double[6];
    }

         /** Container for values and coefficients for the matrix solver
     * defines values needed to include value at mesh.pos(lc) in the
     * Ax = b - (Bx)_neighbor RHS term
     */
    public static class MeshNeighborCoeffs
    {
	public Mesh mesh;  //mesh containing this point
	public double lc[];	//logical coordiate
	public double value;
	public double coeff;
	
	/** updates the value of this value
	 * 
	 * @param fc data to use
	 */
	public void updateValue(FieldCollection2D fc)
	{
	    Field2D field = fc.getField(mesh);
	    value = field.gather(lc);	    
	}
	
    }
    
    /**data to evaluate sum(k*mesh.eval(lc)) on a node n*/
    public class NeighborData
    {
	public ArrayList<Mesh> mesh = new ArrayList<>();
	public ArrayList<double[]> lc =new ArrayList<>();
	public ArrayList<Double> coeff = new ArrayList<>();
    }
    
    /** Container for data used by the field solver
     * The solvers solves (A*x+Ax_bnd)=b. where Ax_bnd is the part of
     * A*x product that is due to nodes located in a neighbor mesh
     * The gradients matrixes are used to compute the gradient, (Gi*x,Gj*x)
     */
    static public class MeshData
    {

	public Mesh mesh;	/*associated mesh*/
	public Matrix Gi;	/*gradient matrix in i direction*/
	public Matrix Gj;	/*gradient matrix in j direction*/
	public Matrix A;
	public Matrix L;	//LU decomposition of matrix A, if available
	public Matrix U;
	public boolean fixed_node[];
	public double b[];
	public double x[];	    /*solution vector*/
	
	//support for multiple domains
	public double Ax_neigh[];	//part of Ax due to nodes beyond the mesh boundary
	
	//coefficient and node locations needed to evaluate the terms from neighbor points
	public NeighborData A_neigh[];
	public NeighborData Gi_neigh[];
	public NeighborData Gj_neigh[];	
    }

    public MeshData mesh_data[];

    void setLinParams(int lin_max_it, double lin_tol)
    {
	/*some defaults*/
	this.lin_max_it = lin_max_it;
	this.lin_tol = lin_tol;
    }

    void setNLParams(int nl_max_it, double nl_tol)
    {
	/*some defaults*/
	this.nl_max_it = nl_max_it;
	this.nl_tol = nl_tol;
    }

    public LinearSolver lin_solver;
    /*init*/

    public void init()
    {
	ArrayList<Mesh> mesh_list = Starfish.getMeshList();

	/*allocate memory*/
	mesh_data = new MeshData[mesh_list.size()];

	/*for now*/
	for (int m = 0; m < mesh_list.size(); m++)
	{
	    Mesh mesh = Starfish.getMeshList().get(m);
	    MeshData md = new MeshData();
	    mesh_data[m] = md;
	    md.mesh = mesh;

	    /* setup up coefficient matrix */
	    int ni = mesh.ni;
	    int nj = mesh.nj;
	    int nu = ni*nj;

	    /* initialize coefficient matrix */
	    md.A = new Matrix(nu);

	    /* initialize array of fixed (dirichlet) nodes*/
	    md.fixed_node = new boolean[nu];
	    	    
	    for (int i = 0; i<nu;i++)
		md.fixed_node[i] = false;
	    
	    //initialize structures for multi-domains simulations
	    md.Ax_neigh = new double[nu];
	    md.A_neigh = new NeighborData[nu];
	    md.Gi_neigh = new NeighborData[nu];
	    md.Gj_neigh = new NeighborData[nu]; 

	    /*init coefficients*/
	    initCoefficients(md);
	}
    }

    /**
     */
    public void exit()
    {
    }
    
     /**
     * interface implemented by linear solvers
     */
    public interface LinearSolver
    {
	int solve(Solver.MeshData mesh_data[], FieldCollection2D fc, int max_it, double tolerance);
    }
    

    /* initializes matrix coefficients */
    private void initCoefficients(MeshData md)
    {
	int i, j;
	int ni = md.mesh.ni;
	int nj = md.mesh.nj;
	int nu = ni*nj;
	
	/* *** 1) set the gradient coefficients ******/
	md.Gi = new Matrix(nu);
	md.Gj = new Matrix(nu);
	
	for (j = 0; j < nj; j++)
	    for (i = 0; i < ni; i++)
	    {
		Gradient G = getNodeGradient(md, i, j);
		
		/*assemble non-zero values into gradient matrix*/
		int u = md.mesh.IJtoN(i,j);
		for (int v = 0; v < 6 && G.u[v]>=0; v++)
		{
		    if (G.Gi[v]!=0) md.Gi.add(u, G.u[v], G.Gi[v]);
		    if (G.Gj[v]!=0) md.Gj.add(u, G.u[v], G.Gj[v]);
		}
		
		//non-local data
		for (int v = 0; v < 6 && G.neighbor_mesh[v]!=null; v++)
		{
		    if (G.neighbor_Gi[v]!=0) 
		    {
			if (md.Gi_neigh[u]==null)
			    md.Gi_neigh[u] = new NeighborData();
			NeighborData nb = md.Gi_neigh[u];
			nb.coeff.add(G.neighbor_Gi[v]);
			nb.lc.add(G.neighbor_lc[v]);
			nb.mesh.add(G.neighbor_mesh[v]);			
		    }
		    if (G.neighbor_Gj[v]!=0) 
		    {
			if (md.Gj_neigh[u]==null)
			    md.Gj_neigh[u] = new NeighborData();
			NeighborData nb = md.Gj_neigh[u];
			nb.coeff.add(G.neighbor_Gj[v]);
			nb.lc.add(G.neighbor_lc[v]);
			nb.mesh.add(G.neighbor_mesh[v]);			
		    }
		}
	    }
	
	/***** 2) set coefficients for potential solver **************/
	for (i = 0; i < ni; i++)
	    for (j = 0; j < nj; j++)
	    {
		setNodeFVMCoefficients(md, i, j);		    
	    }
	
//	for (int u=0;u<ni*nj;u++)
//	    	md.A.println(u,md.mesh.nj);
    }

    /**updates potential and calculates new electric field*/
    abstract public void update();

    /**
     *
     */
    public interface NL_Eval
    {

	/**
	 *
	 * @param x
	 * @param fixed
	 * @return
	 */
	public double[] eval_bx(double x[], boolean fixed[]);

	/**
	 *
	 * @param x
	 * @param fixed
	 * @return
	 */
	public double[] eval_bx_prime(double x[], boolean fixed[]);
    }

    /**
     * solves the nonlinear problem Ax-b=0 using the Newton method
     * This method is based on the algorithm on page 614 in Numerical Analysis
     * F(x)=Ax + (Ax)_neigh - b
     * J(x)=dF/dx=A+A_neigh-db/dx=(A+A_neigh)-P
     * solve Jy=F
     * update x=x-y
     * also b=b0+b(x) where b0 is constant
     * @param md
     * @param nl_eval
     * @return 
     */
    public int solveNonLinearNR(MeshData mesh_data[], NL_Eval nl_eval, FieldCollection2D fc)
    {
	int it;
	double norm=-1;

	MeshData md_nl[] = new MeshData[mesh_data.length];
	for (int k=0;k<mesh_data.length;k++)	    
	{
	    md_nl[k] = new MeshData();
	    md_nl[k].mesh = mesh_data[k].mesh;
	    md_nl[k].x = new double[mesh_data[k].x.length];
	    md_nl[k].Ax_neigh = new double[mesh_data[k].x.length];
	}
	
	/*create a new field collection to store y*/
	FieldCollection2D fc_y = new FieldCollection2D(Starfish.getMeshList(),null);
	
	//main newton-rhapson loop
	for (it = 0; it < nl_max_it; it++)
	{
	    //recompute Ax_neigh;
	    updateGhostVector(mesh_data, fc);
	    
	    for (int k=0;k<mesh_data.length;k++)
	    {
		Mesh mesh = md_nl[k].mesh;
		
		/*calculate b(x)*/
		double b_x[] = nl_eval.eval_bx(mesh_data[k].x, mesh_data[k].fixed_node);

		/*rhs: b=b0+b_x*/
		double b[]= Vector.add(mesh_data[k].b, b_x);
		
		/*calculate P(x) = db/dx*/
		double P[] = nl_eval.eval_bx_prime(mesh_data[k].x, mesh_data[k].fixed_node);
	    
		for (int i=0;i<mesh.ni;i++)
		    for (int j=0;j<mesh.nj;j++)
		    {
			int n = mesh.IJtoN(i,j);
			if (mesh.isDirichletNode(i, j)) continue;
			
			if ((i==0 && mesh.boundaryType(Face.LEFT,j)!=DomainBoundaryType.MESH
			 	  && mesh.boundaryType(Face.LEFT,j)!=DomainBoundaryType.DIRICHLET) ||					  
			    (i==mesh.ni-1 && mesh.boundaryType(Face.RIGHT,j)!=DomainBoundaryType.MESH
				 && mesh.boundaryType(Face.RIGHT,j)!=DomainBoundaryType.DIRICHLET) ||
			    (j==0 && mesh.boundaryType(Face.BOTTOM,i)!=DomainBoundaryType.MESH
				 && mesh.boundaryType(Face.BOTTOM,i)!=DomainBoundaryType.DIRICHLET) ||
			    (j==mesh.nj-1 && mesh.boundaryType(Face.TOP,i)!=DomainBoundaryType.MESH
				 && mesh.boundaryType(Face.TOP,i)!=DomainBoundaryType.DIRICHLET))				
			{
			    b[n] = 0;
			    P[n] = 0;		    
			}
		    }

		/*calculate F(x)=Ax + (Ax)_neigh - b*/
		double lhs[] = mesh_data[k].A.mult(mesh_data[k].x);
		lhs = Vector.add(lhs, mesh_data[k].Ax_neigh);
		double F[] = Vector.subtract(lhs, b);
		
		/*calculate J(x) = d/dx(Ax-b) = A-diag(P)*/
		/*The A_neigh matrix contributes only non-diagonal terms hence 
		  doesn't need to be included directly. Contribution will be taken care of by
		  updateGhostVector in the linear solver*/
		Matrix J = mesh_data[k].A.subtractDiag(P);
	    
		/*solve Jy=F*/   	    
		md_nl[k].A=J;
		md_nl[k].b=F;
		md_nl[k].A_neigh = mesh_data[k].A_neigh;
	    }

	    int lin_it = lin_solver.solve(md_nl, fc_y, lin_max_it, lin_tol);

	    /*md_nl.x is the "y", update solution from x=x-y*/
	    for (int k=0;k<mesh_data.length;k++)
	    {
		Vector.subtractInclusive(mesh_data[k].x, md_nl[k].x);
	    }

	    /*check norm(y) for convergence*/
	    norm = 0;
	    for (MeshData md:md_nl)
		norm += Vector.norm(md.x);
	    
	  //  System.out.println(b0[mesh.IJtoN(5,10)]+" "+ lin_it+" "+norm);
	    
	    Log.log(">>>>NR:" + it + " " + String.format("%.2g", norm));
	    if (norm < nl_tol)
	    {
		Log.debug(String.format("NR converged in %d iterations with norm=%g",it,norm));
		break;
	    }
	}

	if (it==nl_max_it)
	    Log.warning("!! NR solver failed to converge, norm = "+norm);
	return it;
    }


    /** updates values of ghost nodes on mesh boundaries
     * @param fc field collection containing data
    */
    static void updateGhostVector(MeshData mesh_data[], FieldCollection2D fc)
    {
	//can't do anything if fc not provided
	if (fc==null) return;
	
	/*inflate data since using field collection gather to interpolate*/
	for (MeshData md:mesh_data)
		Vector.inflate(md.x, md.mesh.ni, md.mesh.nj, fc.getField(md.mesh).getData());
	
	/*get ghost node values*/
	for (MeshData md:mesh_data)
	{
	    if (md.A_neigh==null)
		continue;
	    
	    for (int u=0;u<md.A_neigh.length;u++)
	    {
		md.Ax_neigh[u] = 0;
		NeighborData nd = md.A_neigh[u];
		if (nd==null) continue;
		
		for (int k=0;k<nd.coeff.size();k++)
		{
		    md.Ax_neigh[u] += nd.coeff.get(k)*fc.gather(nd.mesh.get(k),nd.lc.get(k));
		}
	    }
	    
	}   //for md
	

    }
    
    /**
     * returns L2 norm of R=|Ax-b|
     * @param A
     * @param x
     * @param b
     * @return 
     */
    static double calculateResidue(Matrix A, double Ax_neigh[], double x[], double b[])
    {
	/*this is ||Ax-b||*/
	double lhs[] = A.mult(x);
	if (Ax_neigh!=null)
	    lhs = Vector.add(lhs, Ax_neigh);
	double norm = Vector.norm(Vector.subtract(lhs, b));
	if (Double.isInfinite(norm)
		|| Double.isNaN(norm))
	{
	    Log.error("Solver diverged, aborting");
	}

	return norm;
    }

    /** sets FVM coefficients at node i,j
     * @param md associated mesh data
     * @param i node index
     * @param j node index
     */
    void setNodeFVMCoefficients(MeshData md, int i, int j)
    {
	double R;
	Mesh mesh = md.mesh;
	
	int ni = mesh.ni;
	int nj = mesh.nj;

	Node node[][] = mesh.getNodeArray();

	int u = mesh.IJtoN(i, j);
	
	/*check for fixed nodes*/
	if (mesh.isDirichletNode(i,j))
	{
	    md.A.clearRow(u);
	    md.A.set(u,u, 1);	    /*set one on the diagonal*/
	    md.fixed_node[u] = true;
	    return;
	}
	
	/*check for Neumann boundary*/
	if ( (i==0 && md.mesh.boundaryType(Face.LEFT, j)!=DomainBoundaryType.MESH) ||
	     (i==ni-1 && md.mesh.boundaryType(Face.RIGHT, j)!=DomainBoundaryType.MESH))
		{md.A.copyRow(md.Gi,u); return;}
	
	if ( (j==0 && md.mesh.boundaryType(Face.BOTTOM, i)!=DomainBoundaryType.MESH) ||
	     (j==nj-1 && md.mesh.boundaryType(Face.TOP, i)!=DomainBoundaryType.MESH))
		{md.A.copyRow(md.Gj,u); 
		return;}

	/*not a fixed or boundary node*/
	
	/*evaluates face normals and areas*/
	EdgeData e[] = ComputeEdgeData(i,j,md);

	//loop over faces
	for (int f=0;f<4;f++)
	{	    
	    Gradient G = getNodeGradient(md, e[f].im, e[f].jm);
	    R = G.R;
		
	    /*contribution along each face is grad(phi)* ndA, normal vector given by <dj, -di>*/
	    MultiplyCoeffs(G.Gi, e[f].ndl_i * R);
	    MultiplyCoeffs(G.Gj, e[f].ndl_j * R);
	    MultiplyCoeffs(G.neighbor_Gi, e[f].ndl_i * R);
	    MultiplyCoeffs(G.neighbor_Gj, e[f].ndl_j * R);
	    
	    //local node contribution
	    for (int v = 0; v < 6 && G.u[v]>=0; v++)
	    {
		double val = G.Gj[v]+G.Gi[v];		
		if (val!=0)	//don't bother adding zero
		    md.A.add(u,G.u[v],val);
	    }
	    
	    //contribution for mesh neighbors
	    for (int v=0; v<6 && G.neighbor_mesh[v]!=null; v++)
	    {
		double val = G.neighbor_Gj[v]+G.neighbor_Gi[v];		
		if (val!=0)
		{
		   if (md.A_neigh[u]==null)
		       md.A_neigh[u] = new NeighborData();
		   
		   NeighborData nb = md.A_neigh[u];
		   nb.mesh.add(G.neighbor_mesh[v]);
		   double lc[] = {G.neighbor_lc[v][0], G.neighbor_lc[v][1]};
		   nb.lc.add(lc);
		   nb.coeff.add(val);
		   //sanity check - shouldn't be adding local nodes
		   if (G.neighbor_mesh[v]==mesh)
			Log.warning("local mesh being added to neighbor_mesh data structure");
		}		
	    }
	}

	/* scale by volume */
	double V = md.mesh.nodeVol(i, j, true);
	md.A.multRow(u, 1.0 / V);
	if (md.A_neigh[u]!=null)
	{
	    for(int v=0;v<md.A_neigh[u].coeff.size();v++)
		md.A_neigh[u].coeff.set(v, md.A_neigh[u].coeff.get(v)/V);
	}
	node[i][j].bc_value /= V;
    }

    /*
     * Returns coefficients for gradient calculation at location i,j 
	i and j can be half indices     */
    private Gradient getNodeGradient(MeshData md, double i, double j)
    {
	Gradient G = new Gradient();
	
	EdgeData edge_data[] = ComputeEdgeData(i,j,md);

	for (int f=0;f<4;f++)
	{
	    EdgeData e = edge_data[f];

	    /* assemble the four coefficients into the 6-coefficient gradient table */
	    for (int n = 0; n < 4; n++)
	    {
		/*is this is a local node?*/
		if (e.neighbor_mesh[n]==null)
		{
		    //loop through our six slots and look for either this node or an empty slot
		    for (int k = 0; k < 6; k++)
		    {
			//unknown index
			int u = md.mesh.IJtoN(e.ij[n][0], e.ij[n][1]);
			
			if ((u==G.u[k]) || G.u[k] < 0)
			{
			    G.u[k] = u;
			    G.Gi[k] += 0.25 * e.ndl_i;	/*if all four nodes are the same, then we add 1*/
			    G.Gj[k] += 0.25 * e.ndl_j;

			    break; /* break out of the k loop, go to the next N value*/
			}
		    }
		}   //local mesh
		else
		{
		    for (int k=0;k<6;k++)
		    {
			if ((e.neighbor_mesh[n]==G.neighbor_mesh[k] &&
			     e.neighbor_lc[n][0]==G.neighbor_lc[k][0] &&
			     e.neighbor_lc[n][1]==G.neighbor_lc[k][1]) || G.neighbor_mesh[k]==null)
			{
			    G.neighbor_lc[k] = e.neighbor_lc[n];
			    G.neighbor_mesh[k] = e.neighbor_mesh[n];
			    G.neighbor_Gi[k] += 0.25 * e.ndl_i;
			    G.neighbor_Gj[k] += 0.25 * e.ndl_j;
			    break;  //go to next n value
			}
		    }
		} //neighbor mesh		
	    }	//n
	}   //face

	/*scale by area*/
	double A = md.mesh.area(i, j, true);
	MultiplyCoeffs(G.Gj, 1 / A);
	MultiplyCoeffs(G.Gi, 1 / A);
	MultiplyCoeffs(G.neighbor_Gi, 1/A);
	MultiplyCoeffs(G.neighbor_Gj, 1/A);

	/*radius at which gradient was calculated*/
	G.R = md.mesh.R(i, j);

	return G;
    }

    /**
     * data used in gradient calculation
     */
    class EdgeData
    {
	double im, jm;	/*midpoint location*/
	double ndl_i;	/*dj * ni, area * normal vector*/
	double ndl_j;	/*di * nj, area * normal vector*/
	int[][] ij = new int[4][]; //four nodes used to compute the value at im,jm
	boolean collapsed;	//used only to find collapsed node
	
	//on domain boundaries where the needed node is not in our mesh
	double[][] neighbor_lc = new double[4][];
	Mesh[] neighbor_mesh = new Mesh[4];	//initialized to null
	
    }
    
    /** this function computes (d/dn)*l for the four faces of a control volume along with the four nodes 
     * used to interpolate the face value. Special care is taken on multi-domain corner nodes to use only
     * the half control volume.
     @return edge data or null if computation failed due to a non existing node
     */
    private EdgeData[] ComputeEdgeData(double i0, double j0, MeshData md)
    {
	EdgeData edge_data[] = new EdgeData[4];
	
	int v1=0,v2=0;
	double im=0,jm=0;
	
	//control volume over which we are integrating
	double lcs[][] = md.mesh.controlVolumeLCs(i0, j0, 0.5);
	
	//loop over the four faces
	for (Face face:Face.values())
	{	    
	    switch (face)
	    {
		case BOTTOM: v1=0; im = i0; jm = j0-0.5; break;
		case RIGHT: v1=1; im = i0+0.5; jm=j0; break;
		case TOP: v1=2; im = i0; jm=j0+0.5; break;
		case LEFT: v1=3; im = i0-0.5; jm = j0; break;
	    }
	    v2 = v1+1;
	    if (v2>3) v2=0;
	    
	    //make sure the center point (where value is computed) is in CV bounds
	    if (im<lcs[0][0]) im=lcs[0][0]; //left bottom
	    if (im<lcs[3][0]) im=lcs[3][0]; //left top
	    if (im>lcs[1][0]) im=lcs[1][0]; //right bottom
	    if (im>lcs[2][0]) im=lcs[2][0]; //right top
	    
	    if (jm<lcs[0][1]) jm=lcs[0][1]; //left bottom
	    if (jm<lcs[1][1]) jm=lcs[1][1]; //left top
	    if (jm>lcs[2][1]) jm=lcs[2][1]; //right bottom
	    if (jm>lcs[3][1]) jm=lcs[3][1]; //right top
	    
	    EdgeData e = new EdgeData();
	
	    //edge end points - will include interpolated position on mesh boundaries
	    double x1[] = md.mesh.pos(lcs[v1], true);
	    double x2[] = md.mesh.pos(lcs[v2], true);
	    
	    if (x1==null || x2==null)
	    {
		    lcs = md.mesh.controlVolumeLCs(i0, j0, 0.5);
	    }
	
	    /* compute n*dl = (dy/dl,-dx/dl)*dl=(dy,-dx)	*/	
	    e.ndl_i =   x2[1] - x1[1];	// dy
	    e.ndl_j = -(x2[0] - x1[0]);	// -dx
	
	    /* set logical coordinates for the four nodes surrounding im,jm */
	    e.ij[0] = md.mesh.NodeIndexOffset(im, jm, -0.5, -0.5);
	    e.ij[1] = md.mesh.NodeIndexOffset(im, jm, 0.5, -0.5);
	    e.ij[2] = md.mesh.NodeIndexOffset(im, jm, 0.5, 0.5);
	    e.ij[3] = md.mesh.NodeIndexOffset(im, jm, -0.5, 0.5);
	
	    //on mesh neighbors, we may end up with nodes outside the domain
	    //this function will replace these indexes with lookups on a neighbor mesh
	    for (int n=0;n<4;n++)
		if (!CheckNodeIndex(e,md,n)) return null;

	    e.im = im;
	    e.jm = jm;
	    
	    //save in the array
	    edge_data[face.val()] = e;
	}
	
	return edge_data;
    }
    
    /**
     * Checks node indexes for values outside the mesh, indicating mesh boundary
     * neighbor mesh and lc fields are then set
     * @param e edge data to check
     * @param md mesh data structure, just to pass parent mesh
     * @param n [0:3] index indicating which of the four vertices to check
     */
    private boolean CheckNodeIndex(EdgeData e, MeshData md, int n)
    {
	if (e.ij[n][0]<0 || e.ij[n][1]<0 || e.ij[n][0]>=md.mesh.ni || e.ij[n][1]>=md.mesh.nj)
	{
	    //position of ghost node at ij[n]
	    double pos[] = md.mesh.pos(e.ij[n][0],e.ij[n][1], true);
	    
	    //find the neighbor mesh containing this point
	    Mesh neighbor = Starfish.domain_module.getMesh(pos);
	    
	     //sanity check - shouldn't be adding local nodes
	    if (neighbor==md.mesh)
		Log.warning("local mesh being added to neighbor_mesh data structure");
					
	    /*this node was not found - could happen on physical left boundary along top edge where another mesh starts*/
	    if (neighbor==null)
	    {
		return false;
	    }
	    
	    //logical coordinates of the point on the neighbor mesh
	    double lc[] = neighbor.XtoL(pos);
	    e.neighbor_mesh[n] = neighbor;
	    e.neighbor_lc[n] = lc;
	    e.ij[n][0] = -1;	//clear this entry
	    e.ij[n][1] = -1;	    
	}
	return true;
    }
    /* This functions multiplies all stencil coefficients by the given value */
    private void MultiplyCoeffs(double[] C, double val)
    {
	for (int i = 0; i < 6; i++)
	    C[i] *= val;
    }
    
    /** function used to compute, for instance, the electric field
     *
     */
    public abstract void updateGradientField();

    /** evaluates gradient of flattened data "x" times scale and stores it into fi and fj
     *
     * @param x
     * @param gi
     * @param gj
     * @param md
     * @param scale
     * @param fc    field collection for interpolation of neighbor data
     */
    protected void evaluateGradient(double x[], double gi[], double gj[], MeshData md, double scale,
	    FieldCollection2D fc)
    {
	md.Gi.mult(x,gi);	/*gi = Gi*x*/
	md.Gj.mult(x,gj);
	
	/*add contribution for mesh neighbors*/
	for (int u=0;u<md.Gi_neigh.length;u++)
	{
	    if (md.Gi_neigh[u]!=null)
	    {
		NeighborData nd = md.Gi_neigh[u];		
		for (int k=0;k<nd.coeff.size();k++)
		    gi[u]+=nd.coeff.get(k)*fc.gather(nd.mesh.get(k), nd.lc.get(k));
	    }

	    if (md.Gj_neigh[u]!=null)
	    {
		NeighborData nd = md.Gj_neigh[u];		
		for (int k=0;k<nd.coeff.size();k++)
		    gj[u]+=nd.coeff.get(k)*fc.gather(nd.mesh.get(k), nd.lc.get(k));
	    }
	}
	
	if (scale!=1.0)	    /*scale data*/
	{
	    Vector.mult(gi, scale, gi);
	    Vector.mult(gj, scale, gj);
	}   
    }
    
    
 
}
