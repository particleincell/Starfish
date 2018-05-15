/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.solver;

import starfish.core.common.Vector;
import java.util.ArrayList;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.Face;
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
    boolean qn; /* quasi-neutral solver? */

    /**
     *
     */
    public double kTe0 = 1; /* reference temperature values for boltzman relationship */

    /**
     *
     */
    public double den0 = 1e15;  /* reference values of ion density along bottom edge */

    /**
     *
     */
    public double phi0 = 0; /* reference values of potential along bottom edge */

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
	public ArrayList<Mesh> mesh = new ArrayList();
	public ArrayList<double[]> lc =new ArrayList();
	public ArrayList<Double> coeff = new ArrayList();
    }
    
    /** Container for data used by the field solver
     * The solvers solves (A*x+Ax_bnd)=b. where Ax_bnd is the part of
     * A*x product that is due to nodes located in a neighbor mesh
     * The gradients matrixes are used to compute the gradient, (Gi*x,Gj*x)
     */
    public class MeshData
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
	public FieldCollection2D fc;	    //little hack to save field collection used for neighbor values
	
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
	int solve(Solver.MeshData mesh_data[], int max_it, double tolerance);
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
	
	for (i = 0; i < ni; i++)
	    for (j = 0; j < nj; j++)
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
	public double[] eval_b(double x[], boolean fixed[]);

	/**
	 *
	 * @param x
	 * @param fixed
	 * @return
	 */
	public double[] eval_prime(double x[], boolean fixed[]);
    }

    /**
     * solves the nonlinear problem Ax-b=0 using the Newton method
     * This method is based on the algorithm on page 614 in Numerical Analysis
     * F(x)=Ax-b
     * J(x)=dF/dx=A-db/dx=A-P
     * solve Jy=F
     * update x=x-y
     * also b=b0+b(x) where b0 is constant
     * @param md
     * @param nl_eval
     * @return 
     */
    public int solveNonLinearNR(MeshData md[], NL_Eval nl_eval)
    {
	int it;
	double norm=-1;

	boolean fixed_node[] = md[0].fixed_node;
	double x[] = md[0].x;
	double b0[] = md[0].b;
	Matrix A = md[0].A;
	
	double y[] = new double[x.length];
	
	MeshData md_nl[] = new MeshData[1];
	md_nl[0] = new MeshData();
	    
	for (it = 0; it < nl_max_it; it++)
	{
	    /*calculate b(x)*/
	    double b_x[] = nl_eval.eval_b(x, fixed_node);

	    /*rhs: b=b0+b_x*/
	    double b[]= Vector.add(b0, b_x);
	    
	    /*calculate P(x) = db/dx*/
	    double P[] = nl_eval.eval_prime(x, fixed_node);

	    /*TEMPORARY*/
	    Mesh mesh = md[0].mesh;
	    /*update boundaries */
	    for (int j=0;j<mesh.nj;j++) 
	    {
		b[mesh.IJtoN(0, j)] = mesh.node[0][j].bc_value;
		b[mesh.IJtoN(mesh.ni-1, j)] = mesh.node[mesh.ni-1][j].bc_value;		
		
		P[mesh.IJtoN(0, j)] = 0;
		P[mesh.IJtoN(mesh.ni-1, j)] = 0;	
	    }
	    
	    for (int i=0;i<mesh.ni;i++) 
	    {
		b[mesh.IJtoN(i, 0)] = mesh.node[i][0].bc_value;
		b[mesh.IJtoN(i,mesh.nj-1)] = mesh.node[i][mesh.nj-1].bc_value;		
		P[mesh.IJtoN(i, 0)] = 0;
		P[mesh.IJtoN(i,mesh.nj-1)] = 0;		
	    }
	    
	    /*calculate F(x)=Ax-b*/
	    double F[] = Vector.subtract(A.mult(x), b);

	    /*calculate J(x) = d/dx(Ax-b) = A-diag(P)*/
	    Matrix J = A.subtractDiag(P);
	    
	    /*solve Jy=F*/   	    
	    /*temporary hack*/	    
	    md_nl[0].A=J;
	    md_nl[0].b=F;
	    md_nl[0].mesh= md[0].mesh;
	    md_nl[0].fixed_node = md[0].fixed_node;
	    md_nl[0].x=y;
	    
	    int lin_it = lin_solver.solve(md_nl, lin_max_it, lin_tol);

	    /*set x=x-y*/
	    Vector.subtractInclusive(x, y);

	    /*check norm(y) for convergence*/
	    norm = Vector.norm(y);
	  //  System.out.println(b0[mesh.IJtoN(5,10)]+" "+ lin_it+" "+norm);
	    
	    Log.log(">>>>NR:" + it + " " + String.format("%.2g", norm));
	    if (norm < nl_tol)
	    {
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
	/*get ghost node values*/
	for (MeshData md:mesh_data)
	{
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
    static double calculateResidue(Matrix A, double x[], double b[])
    {
	/*this is ||Ax-b||*/
	double norm = Vector.norm(Vector.subtract(A.mult(x), b));
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
	if (mesh.isDirichletNode(i,j) ||
	    mesh.isMeshBoundary(i,j))
	{
	    md.A.clearRow(u);
	    md.A.set(u,u, 1);	    /*set one on the diagonal*/
	    md.fixed_node[u] = true;
	    return;
	}
	
	/*boundary nodes when ghost cells not used*/
	if (i==0 || i==ni-1) {md.A.copyRow(md.Gi,u); return;}
	if (j==0 || j==nj-1) {md.A.copyRow(md.Gj,u); return;}
	
	/*not a fixed node*/
	for (Face face : Face.values())
	{
	    /*evaluates face normals and areas*/
	    EdgeData e = ComputeEdgeData(face,i,j,md);

	    Gradient G = getNodeGradient(md, e.im, e.jm);
	    R = G.R;
		
	    /*contribution along each face is grad(phi)* ndA, normal vector given by <dj, -di>*/
	    MultiplyCoeffs(G.Gi, e.ndl_i * R);
	    MultiplyCoeffs(G.Gj, e.ndl_j * R);
	    
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
		double val = G.Gj[v]+G.Gi[v];		
		if (val!=0)
		{
		   if (md.A_neigh[u]==null)
		       md.A_neigh[u] = new NeighborData();
		   
		   NeighborData nb = md.A_neigh[u];
		   nb.mesh.add(G.neighbor_mesh[v]);
		   double lc[] = {G.neighbor_lc[v][0], G.neighbor_lc[v][1]};
		   nb.lc.add(lc);
		   nb.coeff.add(val);
		}		
	    }
	}

	/* scale by volume */
	double V = md.mesh.nodeVol(i, j, true);
	md.A.multRow(u, 1.0 / V);
	node[i][j].bc_value /= V;
    }

    /*
     * Returns coefficients for gradient calculation at location i,j 
	i and j can be half indices     */
    private Gradient getNodeGradient(MeshData md, double i, double j)
    {
	Gradient G = new Gradient();

	for (Face face : Face.values())
	{
	    EdgeData e = ComputeEdgeData(face,i,j,md);

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
			    G.Gi[k] += 0.25 * e.ndl_i;
			    G.Gj[k] += 0.25 * e.ndl_j;
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
	
	//on domain boundaries where the needed node is not in our mesh
	double[][] neighbor_lc = new double[4][];
	Mesh[] neighbor_mesh = new Mesh[4];	//initialized to null
	
    }
    
    /*this function computes the control volume face "area" and the mesh points used in evaluation*/
    private EdgeData ComputeEdgeData(Face face, double i0, double j0, MeshData md)
    {
	EdgeData e = new EdgeData();
	double im,jm;	    /*edge mid point*/
	double i1,j1;
	double i2,j2;
	
	switch (face)
	{
	    case RIGHT:
		im = i0 + 0.5;
		jm = j0;
		i1 = im;
		j1 = jm - 0.5;
		i2 = im;
		j2 = jm + 0.5;
		break;
	    case TOP:
		im = i0;
		jm = j0 + 0.5;
		i1 = im + 0.5;
		j1 = jm;
		i2 = im - 0.5;
		j2 = jm;
		break;
	    case LEFT:
		im = i0 - 0.5;
		jm = j0;
		i1 = im;
		j1 = jm + 0.5;
		i2 = im;
		j2 = jm - 0.5;
		break;
	    case BOTTOM:
		im = i0;
		jm = j0 - 0.5;
		i1 = im - 0.5;
		j1 = jm;
		i2 = im + 0.5;
		j2 = jm;
		break;
	    default: throw new RuntimeException("Unknown face");
	}
	
	//edge end points - will include interpolated position on mesh boundaries
	double x1[] = md.mesh.pos(i1, j1, true);
	double x2[] = md.mesh.pos(i2, j2, true);

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
	    CheckNodeIndex(e,md,n);
	
	e.im = im;
	e.jm = jm;
	
	return e;
    }
    
    /**
     * Checks node indexes for values outside the mesh, indicating mesh boundary
     * neighbor mesh and lc fields are then set
     * @param e edge data to check
     * @param md mesh data structure, just to pass parent mesh
     * @param n [0:3] index indicating which of the four vertices to check
     */
    private void CheckNodeIndex(EdgeData e, MeshData md, int n)
    {
	if (e.ij[n][0]<0 || e.ij[n][1]<0 || e.ij[n][0]>=md.mesh.ni || e.ij[n][1]>=md.mesh.nj)
	{
	    //position of ghost node at ij[n]
	    double pos[] = md.mesh.pos(e.ij[n][0],e.ij[n][1], true);
	    
	    //find the neighbor mesh containing this point
	    Mesh neighbor = Starfish.domain_module.getMesh(pos);
	    
	    /*this node was not found - could happen on physical left boundary along top edge where another mesh starts*/
	    if (neighbor==null)
	    {
		if (e.ij[n][0]<0) e.ij[n][0]=0;
		else if (e.ij[n][0]>md.mesh.ni-1) e.ij[n][0]=md.mesh.ni-1;
		if (e.ij[n][1]<0) e.ij[n][1]=0;
		else if (e.ij[n][1]>md.mesh.nj-1) e.ij[n][1]=md.mesh.nj-1;
		return;
	    }
	    
	    //logical coordinates of the point on the neighbor mesh
	    double lc[] = neighbor.XtoL(pos);
	    e.neighbor_mesh[n] = neighbor;
	    e.neighbor_lc[n] = lc;
	    e.ij[n][0] = -1;	//clear this entry
	    e.ij[n][1] = -1;	    
	}
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
     */
    protected void evaluateGradient(double x[], double gi[], double gj[], MeshData md, double scale)
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
		    gi[u]+=nd.coeff.get(k)*md.fc.gather(nd.mesh.get(k), nd.lc.get(k));
	    }
	}
	
	if (scale!=1.0)	    /*scale data*/
	{
	    Vector.mult(gi, scale, gi);
	    Vector.mult(gj, scale, gj);
	}   
    }
    
    
 
}
