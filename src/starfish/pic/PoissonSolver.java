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
import starfish.core.solver.LinearSolverGS;
import starfish.core.solver.LinearSolverLU;
import starfish.core.solver.LinearSolverMG;
import starfish.core.solver.LinearSolverPCG;

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
     * @param element
     */
    public PoissonSolver (Element element)
    {
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

	String sm = (InputParser.getValue("method", element, "GS")).toUpperCase();
	if (sm.equals("DIRECT")) lin_solver = new LinearSolverLU();
	else if (sm.equals("GS")) lin_solver = new LinearSolverGS();
	else if (sm.equals("PCG")) lin_solver = new LinearSolverPCG();
	else if (sm.equals("MULTIGRID")) lin_solver = new LinearSolverMG();
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
		lin_solver.solve(mesh_data, lin_max_it, lin_tol);
	else
		solvePotentialNL();
	
	/*inflate and update electric field*/
	for (MeshData md:mesh_data)
	    Vector.inflate(md.x, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getPhi(md.mesh).getData());
    }
        
    
    /** Nonlinear solver using the Newthon Rhapson method
     * 
     * @return number of iterations
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
    
    /**SOLVER FACTORY*/
    public static SolverModule.SolverFactory poissonSolverFactory = new SolverModule.SolverFactory()
    {
	@Override
	public Solver makeSolver(Element element)
	{	    
	    return new PoissonSolver(element);
	}
    };


 
    
}
