package starfish.plugins.plasma_dynamics;

import org.w3c.dom.Element;

import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Vector;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.UniformMesh;
import starfish.core.io.InputParser;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;
import starfish.core.solver.Solver.LinearSolver;
import starfish.core.solver.Solver.MeshData;

public class MagSolver implements LinearSolver {

	public MagSolver(int solver_it, double solver_tol) {
		super();
		
		this.solver_it = solver_it;
		this.solver_tol = solver_tol;
		
		phi_m = Starfish.domain_module.getFieldManager().add("phi_m","",null);
		// b field
		bfi = Starfish.domain_module.getBfi();
		bfj = Starfish.domain_module.getBfj();

	}
	
	int solver_it;
	double solver_tol;
	FieldCollection2D phi_m;
	FieldCollection2D bfi;
	FieldCollection2D bfj;
	
	
	@Override
	public int solve(MeshData[] mesh_data, FieldCollection2D fc, int max_it, double tolerance) {
		boolean converged = false;
		
		for (MeshData md:mesh_data) {
			
			Mesh mesh = md.mesh;
			
			/*this only works for uniform mesh*/
			UniformMesh umesh = (UniformMesh) mesh;
			
			double idx2 = 1.0/(umesh.dh[0]*umesh.dh[0]);
			double idy2 = 1.0/(umesh.dh[1]*umesh.dh[1]);
			
			double phi_m[][] = this.phi_m.getField(umesh).getData();
			double rho[][] = Starfish.domain_module.getRho(mesh).data;
			
			int solver_it; 
			converged = false;
			
			/*these are hardcoded since LinearSolver doesn't have access to Solver*/
			double n0 = 1e12;
			double phi0 = 0;
			double kTe0 = 1.5;
			
			for (solver_it = 0; solver_it<max_it; solver_it++)
			{
			    for (int i=0;i<mesh.ni;i++)
				for (int j=0;j<mesh.nj;j++)
				{
				    if (mesh.isDirichletNode(i, j)) 
				    {
					phi_m[i][j] = mesh.getNode(i, j).bc_value;
					continue;			
				    }
				    
				    /*else assume neumann boundaries*/
				    if (i==0) phi_m[i][j] = phi_m[i+1][j];
				    else if (i==mesh.ni-1) phi_m[i][j] = phi_m[i-1][j];
				    else if (j==0) phi_m[i][j] = phi_m[i][j+1];
				    else if (j==mesh.nj-1) phi_m[i][j] = phi_m[i][j-1];
				    else
				    {
					double b = rho[i][j] - Constants.QE*n0*Math.exp((phi_m[i][j]-phi0)/kTe0);
					
					double phi_new = (b/Constants.EPS0 + 
							idx2*(phi_m[i-1][j] + phi_m[i+1][j]) +
							idy2*(phi_m[i][j-1] + phi_m[i][j+1])) / (2*(idx2+idy2));
		                
		            		/*SOR*/
					phi_m[i][j] = phi_m[i][j] + 1.4*(phi_new-phi_m[i][j]);
				    }  
				}

			    //convergence check
			    if (solver_it%25==0)
			    {
				double sum = 0;
	
				for (int i=0;i<mesh.ni;i++)
				    for (int j=0;j<mesh.nj;j++)				
				    {
					if (mesh.isDirichletNode(i, j)) continue;
					 
					if (i==0) phi_m[i][j] = phi_m[i+1][j];
					else if (i==mesh.ni-1) phi_m[i][j] = phi_m[i-1][j];
					else if (j==0) phi_m[i][j] = phi_m[i][j+1];
					else if (j==mesh.nj-1) phi_m[i][j] = phi_m[i][j-1];
					else			
					{
					    double b = rho[i][j] - Constants.QE*n0*Math.exp((phi_m[i][j]-phi0)/kTe0);			
					    double R = b/Constants.EPS0 + 
							idx2*(phi_m[i-1][j]-2*phi_m[i][j]+phi_m[i+1][j])+
							idy2*(phi_m[i][j-1]-2*phi_m[i][j]+phi_m[i][j+1]);
	
					    sum += R*R;
					}
				    }
	
				double L2 = Math.sqrt(sum/(mesh.ni*mesh.nj));
				if (L2<tolerance) {converged=true;break;}
				//else System.out.printf("L2 = %g\n",L2);
			    }	/*convergence check*/
			}
			
		}
		// set bfi
		setGradient();
		
		return converged?solver_it:-1;
	}

	// set field
	void setGradient() {
		
		for (Mesh mesh:Starfish.domain_module.getMeshList()) {
						
			/*this only works for uniform mesh*/
			UniformMesh umesh = (UniformMesh) mesh;
			
			double dx = (umesh.dh[0]);
			double dy = (umesh.dh[1]);
			
			double phi_m[][] = this.phi_m.getField(umesh).getData();
			double bfi[][] = this.bfi.getField(umesh).data;
			double bfj[][] = this.bfj.getField(umesh).data;
			
			for (int i=1;i<umesh.ni-1;i++) 
				for (int j=1;j<umesh.nj-1;j++) {
					bfi[i][j] = (phi_m[i-1][j]-phi_m[i+1][j])/(2*dx);
					bfj[j][j] = (phi_m[i][j-1]-phi_m[i][j+1])/(2*dy);
					
				}
		}
	}
		
    /*SOLVER FACTORY*/
    public static SolverModule.SolverFactory MagSolverFactory = new SolverModule.SolverFactory()
    {
		@Override
		public Solver makeSolver(Element element)
		{
		    Solver solver;
		    
		    int solver_it = InputParser.getInt("solver_it",element);
		    double solver_tol = InputParser.getDouble("solver_tol",element);
		    solver= new MagSolver(solver_it,solver_tol);
		
		    /*log*/
		    Starfish.Log.log("Added Magnetostatic (Mag) solver");
		    Starfish.Log.log("> Electron Temperature: " + Te + " (eV)" );
		    Starfish.Log.log("> Solver Max Iterations: " + solver_it);
		    Starfish.Log.log("> Solver Tolerance: " + solver_tol);
		    return solver;
		}
    };

}
