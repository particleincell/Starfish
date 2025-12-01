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
import starfish.core.common.Utils;
import starfish.core.common.Vec;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.DomainBoundaryType;
import starfish.core.domain.Mesh.Face;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.io.InputParser;
import starfish.core.solver.LinearSolverADI;
import starfish.core.solver.LinearSolverDirect1D;
import starfish.core.solver.LinearSolverGS;
import starfish.core.solver.LinearSolverGSsimple;
import starfish.core.solver.LinearSolverLU;
import starfish.core.solver.LinearSolverMG;
import starfish.core.solver.LinearSolverPCG;
import starfish.core.solver.Matrix;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;

/**
 *
 * @author Lubos Brieda
 */
public class PoissonSolver extends PotentialSolver
{
    boolean linear_mode;
    double eps;	    /*permittivity*/
    enum Method {DIRECT, GS, PCG, MULTIGRID, ADI};
    Method method;
    int skip;
    boolean qn_switch;	    //nodes with dh>lambda_d fixed per Boltzmann inversion
    double qn_den0,qn_kTe0,qn_phi0;
	
    /**
     * @param element
     */
    public PoissonSolver (Element element)
    {
		linear_mode = InputParser.getBoolean("linear", element, false);
		if (!linear_mode)
		{
		    den0=InputParser.getDouble("n0", element, -1);
		    den0_pos = InputParser.getDoublePairs("n0_pos", element);
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
		    //these are only used to compute the debye length
		    den0=InputParser.getDouble("n0", element,1e15);
		    kTe0=InputParser.getDouble("Te0", element,1);
		    
		    //get "rho moat" width
		    rho_moat = InputParser.getInt("rho_moat", element,0);
		    /*log*/
		    Log.log("Added LINEAR POISSON solver");
		}
		
		//check for qn_switch
		qn_switch = InputParser.getBoolean("qn_switch",element,false);
		if (qn_switch) {
		    qn_den0 = InputParser.getDouble("qn_n0", element,den0);
		    qn_kTe0 = InputParser.getDouble("qn_Te0", element,kTe0);
		    qn_phi0 = InputParser.getDouble("qn_phi0", element,phi0);
		}
		
		double eps_r = InputParser.getDouble("eps_r",element,1);	/*relative permittivity*/	    
		eps = Constants.EPS0*eps_r;
		Log.log("> eps_r: "+eps_r);
	
		String sm = (InputParser.getValue("method", element, "GS")).toUpperCase();
		if (sm.equals("DIRECT")) lin_solver = new LinearSolverLU();
		else if (sm.equals("GS")) lin_solver = new LinearSolverGS();
		else if (sm.equals("GS_SIMPLE")) lin_solver = new LinearSolverGSsimple();
		else if (sm.equals("PCG")) lin_solver = new LinearSolverPCG();
		else if (sm.equals("MULTIGRID")) lin_solver = new LinearSolverMG();
		else if (sm.equals("ADI")) lin_solver = new LinearSolverADI();
		else if (sm.equals("DIRECT1D")) lin_solver = new LinearSolverDirect1D();
		else Log.error("Unknown method "+sm);
		Log.log("> method: "+sm);
		   
		//frequency
		skip = InputParser.getInt("skip",element,1);
		
		/*output debye length*/
		double lambda_d = Math.sqrt(eps*kTe0/(Constants.QE*den0));
		Log.log(String.format("> Debye length: %.3g (m)",lambda_d));
	
    }

    @Override
    public void update() 
    {
    	//is it time to update?
    	if ((Starfish.getIt()-1)%skip!=0) return;
	
		//re-evalute den0 if sampling from points
		updateDen0();
		
		for (MeshData md:mesh_data)
		{
		    Mesh mesh = md.mesh;
	
		    /*flatten data*/
		    md.x = Vec.deflate(Starfish.domain_module.getPhi(mesh).getData());
		    md.b = Vec.deflate(Starfish.domain_module.getRho(mesh).getData());	    
		    md.b = Vec.mult(md.b, -1/eps);
		   
		    /*update boundaries, looping over all nodes to avoid code reuse */
		    for (int i=0;i<mesh.ni;i++) 
				for (int j=0;j<mesh.nj;j++) 
				{
				    //skip dirichlet nodes
				    if (mesh.isDirichletNode(i, j))  {
				    	md.b[mesh.IJtoN(i,j)] = mesh.node[i][j].bc_value;
				    	continue;
				    }
				    
				    if (mesh.nodeType(i, j)==NodeType.INSULATOR) {
				    	md.b[mesh.IJtoN(i, j)]=0;
				    	continue;
				    }
				    
				    //skip internal nodes
				    if (i!=0 && i!=mesh.ni-1 && j!=0 && j!=mesh.nj-1) continue;
				    
				    //on corner nodes where one face is neumann and another is mesh, we still set b=bc
				    if (i==0 && mesh.boundaryType(Face.LEFT, j)!=DomainBoundaryType.MESH)
					md.b[mesh.IJtoN(0,j)] = mesh.node[0][j].bc_value;
				    if (i==mesh.ni-1 && mesh.boundaryType(Face.RIGHT, j)!=DomainBoundaryType.MESH)
					md.b[mesh.IJtoN(mesh.ni-1,j)] = mesh.node[mesh.ni-1][j].bc_value;
				    if (j==0 && mesh.boundaryType(Face.BOTTOM, i)!=DomainBoundaryType.MESH)
					md.b[mesh.IJtoN(i,0)] = mesh.node[i][0].bc_value;
				    if (j==mesh.ni-1 && mesh.boundaryType(Face.TOP, i)!=DomainBoundaryType.MESH)
					md.b[mesh.IJtoN(i,mesh.nj-1)] = mesh.node[i][mesh.nj-1].bc_value;		
				}
			    
		    /*add objects*/
		    // md.b = Vector.mergeBC(md.fixed_node, mesh, md.b);
		}
			
		MeshData md_bu[] = mesh_data.clone();   //back up
		/*apply QN switch, if enabled*/
		if (qn_switch) {
		    
		    for (int m=0;m<mesh_data.length;m++) {
			md_bu[m].A = Matrix.copy(mesh_data[m].A);
			md_bu[m].b = mesh_data[m].b.clone();
			md_bu[m].fixed_node = mesh_data[m].fixed_node.clone();
			
			Mesh mesh = mesh_data[m].mesh;
			
			double rho[][] = Starfish.domain_module.getRho(mesh).getData();
			
			for (int i=0;i<mesh.ni;i++)
			    for (int j=0;j<mesh.nj;j++) {
				if (mesh.isDirichletNode(i, j)) continue;
				
				//compute local Debye length and compare to node volume
				double ion_den = rho[i][j]/Constants.QE;
				if (ion_den<=0) ion_den=1e4;	//apply floor
				
				double debye_vol = Utils.debyeVolume(kTe0,ion_den);
				if (mesh.nodeVol(i,j)>debye_vol) {
				    int u = mesh.IJtoN(i, j);
				    double phi_qn = qn_phi0 + qn_kTe0*Math.log(ion_den/qn_den0);
				    mesh_data[m].A.clearRow(u);
				    mesh_data[m].A.set(u, u, 1);
				    mesh_data[m].b[u] = phi_qn;
				    mesh_data[m].fixed_node[u] = true;
				}
			    }
		    }
		    
		}
			
		/* solve potential */	
		if (linear_mode)
			lin_solver.solve(mesh_data, Starfish.domain_module.getPhi(), lin_max_it, lin_tol);
		else
			solvePotentialNL();
		  
		/*restore matrix if QN switch is used*/
		if (qn_switch)	{
		    for (int m=0;m<mesh_data.length;m++) {
			mesh_data[m].A = md_bu[m].A;
			mesh_data[m].fixed_node = md_bu[m].fixed_node;
			mesh_data[m].b = md_bu[m].b;	//probably not needed
		    }
		}
		    
		/*inflate and update electric field*/
		for (MeshData md:mesh_data)
		    Vec.inflate(md.x, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getPhi(md.mesh).getData());
	
    }
        
    
    /** Nonlinear solver using the Newton Rhapson method
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
	    public double[] eval_bx(double[] x, boolean fixed[]) 
	    {
		    double b[] = new double[x.length];
		    for (int i=0;i<x.length;i++) 
			b[i] = fixed[i] ? 0 : C * den0*Math.exp((x[i]-phi0)/kTe0);
		    return b;
	    }

	    @Override
	    public double[] eval_bx_prime(double x[], boolean fixed[]) 
	    {
		    double b_prime[] = new double[x.length];
		    for (int i=0;i<x.length;i++) 
			b_prime[i] = fixed[i] ? 0 : C * den0*Math.exp((x[i]-phi0)/kTe0)/kTe0;
		    return b_prime;
	    }

	};

	/*call nonlinear solver*/
	int it = solveNonLinearNR(mesh_data, pot_boltzmann, Starfish.domain_module.getPhi());

	return it;
    }
    
    /**tests the linear solver by loading a sinusoidal form for charge density and
     * comparing results with theory
     */
    void solverTest()
    {
	MeshData md = mesh_data[0];
	Mesh mesh = md.mesh;
	
	
	md.x = Vec.deflate(Starfish.domain_module.getPhi(mesh).getData());
	md.b = Vec.deflate(Starfish.domain_module.getRho(mesh).getData());	    
	md.b = Vec.mult(md.b, -1/eps);
	lin_solver.solve(mesh_data, Starfish.domain_module.getPhi(), lin_max_it, lin_tol);
	Vec.inflate(md.x, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getPhi(md.mesh).getData());
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
