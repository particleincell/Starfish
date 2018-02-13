/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.pic;

import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;
import starfish.core.common.Vector;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.domain.UniformMesh;

/**
 *
 * @author Lubos Brieda
 */
public class PoissonSolver extends PotentialSolver
{
    boolean linear_mode;
    double eps;	    /*permittivity*/
    enum Method {DIRECT, GS, PCG, MULTIGRID};
    Method method;
	
    /**
     *
     * @param element
     */
    public PoissonSolver (Element element)
    {
	super();
		
	linear_mode = InputParser.getBoolean("linear", element, false);
	if (!linear_mode)
	{
	    den0=InputParser.getDouble("n0", element);
	    kTe0=InputParser.getDouble("Te0", element);
	    phi0=InputParser.getDouble("phi0", element);
	     /*log*/
	    Log.log("Added NONLINEAR POISSON solver");
	    Log.log("> n0: " + den0 + " (#/m^3)");
	    Log.log("> T0: " + kTe0 + " (eV)");
	    Log.log("> phi0: " + phi0 + " (V)");
	}
	else
	{
	    den0=InputParser.getDouble("n0", element,1e15);
	    kTe0=InputParser.getDouble("Te0", element,1);
	    /*log*/
	    Log.log("Added LINEAR POISSON solver");
	}
	
	double eps_r = InputParser.getDouble("eps_r",element,1);	/*relative permittivity*/	    
	eps = Constants.EPS0*eps_r;
	Log.log("> eps_r: "+eps_r);

	String sm = (InputParser.getValue("method", element, "direct")).toUpperCase();
	if (sm.equals("DIRECT")) method = Method.DIRECT;
	else if (sm.equals("GS")) method= Method.GS;
	else if (sm.equals("PCG")) method = Method.PCG;
	else if (sm.equals("MULTIGRID")) method = Method.MULTIGRID;
	else Log.error("Unknown method "+sm);
	Log.log("> method: "+sm);
	   
	
	/*output debye length*/
	double lambda_d = Math.sqrt(eps*kTe0/(Constants.QE*den0));
	Log.log(String.format("> Debye length: %.3g (m)",lambda_d));
    }

    @Override
    public void update() 
    {
	for (MeshData md:mesh_data)
	{
	    Mesh mesh = md.mesh;

	    /*flatten data*/
	    md.x = Vector.deflate(Starfish.domain_module.getPhi(mesh).getData());
	    md.b = Vector.deflate(Starfish.domain_module.getRho(mesh).getData());	    
	    md.b = Vector.mult(md.b, -1/eps);

	    /*update boundaries */
	    for (int j=0;j<md.mesh.nj;j++) 
	    {
		md.b[md.mesh.IJtoN(0, j)] = md.mesh.node[0][j].bc_value;
		md.b[md.mesh.IJtoN(md.mesh.ni-1, j)] = md.mesh.node[md.mesh.ni-1][j].bc_value;		
	    }
	    
	    for (int i=0;i<md.mesh.ni;i++) 
	    {
		md.b[md.mesh.IJtoN(i, 0)] = md.mesh.node[i][0].bc_value;
		md.b[md.mesh.IJtoN(i,md.mesh.nj-1)] = md.mesh.node[i][md.mesh.nj-1].bc_value;		
	    }
	    
	    /*add objects*/
	    md.b = Vector.merge(md.fixed_node, md.x, md.b);
	}
		
	/* solve potential */
	
	if (linear_mode)
		solvePotentialLin();
	else
		solvePotentialNL();
	
	/*inflate and update electric field*/
	for (MeshData md:mesh_data)
	    Vector.inflate(md.x, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getPhi(md.mesh).getData());
    }
        
    /**
     *
     * @return
     */
    protected int solvePotentialLin() 
    {
	int it = 0;
	
	/*call linear solver*/
	if (method==Method.DIRECT)
	    it = solveLU(mesh_data);
	else if (method==Method.GS)
	    it = solveLinearGS(mesh_data);
	else if (method==Method.PCG)
	    it = solveLinearPCG(mesh_data);
	
	return it;
    }
    
    
    /*HACK FOR IEPC*/
    int solveLinearGSsimple()
    {
	for (int it=0;it<10000;it++)
	for (MeshData md:mesh_data)
	   
	{
	    UniformMesh mesh = (UniformMesh)md.mesh;
	    
	    double phi[][] = Starfish.domain_module.getPhi(mesh).data;
	    double rho[][] = Starfish.domain_module.getRho(mesh).data;
	    
	    for (int i=0;i<mesh.ni;i++)
		for (int j=0;j<mesh.nj;j++)
		{
		    if (mesh.node[i][j].type==NodeType.DIRICHLET) continue;
		    
		    double phiL,phiR,phiT,phiB;
		    double dx=mesh.dh[0],dy=mesh.dh[1];
		    /*if internal*/
		    if (i>0 && j>0 && i<mesh.ni-1 && j<mesh.nj-1)
		    {
			phiL=phi[i-1][j];phiR=phi[i+1][j];
			phiB=phi[i][j-1];phiT=phi[i][j+1];
		    }
		    else if (j==0 && mesh.nodeType(i, j)!=NodeType.MESH)
		    {
			phi[i][j]=phi[i][j+1];
			continue;
		    }
		    else if (j==mesh.nj-1 && mesh.nodeType(i, j)!=NodeType.MESH)
		    {
			phi[i][j]=phi[i][j-1];
			continue;
		    }
		    else if (i==0 && mesh.nodeType(i, j)!=NodeType.MESH)
		    {
			phi[i][j]=phi[i+1][j];
			continue;
		    }
		    else if (i==mesh.ni-1 && mesh.nodeType(i, j)!=NodeType.MESH)
		    {
			phi[i][j]=phi[i-1][j];
			continue;
		    }
		    else if (i==0 && mesh.nodeType(i,j)==NodeType.MESH)
		    {
			Mesh nm=mesh.boundaryData(Mesh.Face.LEFT, j).neighbor[0];
			double x[] = mesh.pos(i-1,j);
			phiL = Starfish.domain_module.getPhi(nm).eval(x);
			phiR=phi[i+1][j];
			phiB=phi[i][j-1];phiT=phi[i][j+1];
			
		    }
		    else if (i==mesh.ni-1 && mesh.nodeType(i,j)==NodeType.MESH)
		    {
			Mesh nm=mesh.boundaryData(Mesh.Face.RIGHT, j).neighbor[0];
			double x[] = mesh.pos(i+1,j);
			phiR = Starfish.domain_module.getPhi(nm).eval(x);
			phiL=phi[i-1][j];
			phiB=phi[i][j-1];phiT=phi[i][j+1];
		    }
		    else if (j==0 && mesh.nodeType(i,j)==NodeType.MESH)
		    {
			Mesh nm=mesh.boundaryData(Mesh.Face.BOTTOM, i).neighbor[0];
			double x[] = mesh.pos(i,j-1);
			phiL=phi[i-1][j];phiR=phi[i+1][j];
			phiB=Starfish.domain_module.getPhi(nm).eval(x);
			phiT=phi[i][j+1];
		    }
		    else if (j==mesh.nj-1 && mesh.nodeType(i,j)==NodeType.MESH)
		    {
			Mesh nm=mesh.boundaryData(Mesh.Face.TOP, j).neighbor[0];
			double x[] = mesh.pos(i+1,j);
			phiL=phi[i-1][j];phiR=phi[i+1][j];
			phiB=phi[i][j-1];phiT=Starfish.domain_module.getPhi(nm).eval(x);
		    }
		    else
		    {
			phiL=0;phiR=0;
			phiT=0;phiB=0;
			dx=mesh.dh[0];
			dy=mesh.dh[1];
		    }

		    double dx2=dx*dx;
		    double dy2=dy*dy;
			
		    phi[i][j] = (rho[i][j]/eps+(phiL+phiR)/dx2 + (phiB+phiT)/dy2)/(2/dx2+2/dy2);
		    
		}
	    
	}
	
	return 100;
    }
    
    
    

    /* solves potential using Gauss-Seidel method */

    /**
     *
     * @return
     */

    protected int solvePotentialNL() 
    {
	final double C=Constants.QE/eps;
	
	/*non linear part*/
	NL_Eval pot_boltzmann = new NL_Eval() 
	{
	    @Override
	    public double[] eval_b(double[] x, boolean fixed[]) 
	    {
		    double b[] = new double[x.length];
		    for (int i=0;i<x.length;i++) 
			b[i] = fixed[i] ? 0 : C * den0*Math.exp((x[i]-phi0)/kTe0);
		    return b;
	    }

	    @Override
	    public double[] eval_prime(double x[], boolean fixed[]) 
	    {
		    double b_prime[] = new double[x.length];
		    for (int i=0;i<x.length;i++) 
			b_prime[i] = fixed[i] ? 0 : C * den0*Math.exp((x[i]-phi0)/kTe0)/kTe0;
		    return b_prime;
	    }

	};

	/*call nonlinear solver*/
	int it = solveNonLinearNR(mesh_data, pot_boltzmann);

	return it;
    }
    
    /*SOLVER FACTORY*/

    /**
     *
     */

    public static SolverModule.SolverFactory poissonSolverFactory = new SolverModule.SolverFactory()
    {
	@Override
	public Solver makeSolver(Element element)
	{	    
	    return new PoissonSolver(element);
	}
    };


 
    
}
