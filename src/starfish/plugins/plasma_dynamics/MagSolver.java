package starfish.plugins.plasma_dynamics;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.UniformMesh;
import starfish.core.io.InputParser;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;
import starfish.pic.PotentialSolver;


public class MagSolver extends PotentialSolver {

	public MagSolver(int solver_it, double solver_tol) {
		super();
		
		this.solver_it = solver_it;
		this.solver_tol = solver_tol;
		
		phi_m = Starfish.domain_module.getFieldManager().add("phi_m","",null);
		rho_m = Starfish.domain_module.getFieldManager().add("rho_m","",null);

		magMi = Starfish.domain_module.getFieldManager().add("magMi","",null);
		magMj = Starfish.domain_module.getFieldManager().add("magMj","",null);
		magMk = Starfish.domain_module.getFieldManager().add("magMk","",null);

		// b field
		bfi = Starfish.domain_module.getBfi();
		bfj = Starfish.domain_module.getBfj();
	}
	
	int solver_it;
	double solver_tol;
	boolean first_time = true;
	
	FieldCollection2D phi_m;
	FieldCollection2D rho_m;
	FieldCollection2D magMi;
	FieldCollection2D magMj;
	FieldCollection2D magMk;
	FieldCollection2D bfi;
	FieldCollection2D bfj;
	
	@Override
	public void update() {
		
		if (!first_time) return;		// runs only once
		first_time = false;
		
		setRhoM();
		
		for (MeshData md:mesh_data) {
			
			Mesh mesh = md.mesh;
			
			/*this only works for uniform mesh*/
			UniformMesh umesh = (UniformMesh) mesh;
			
			double idx2 = 1.0/(umesh.dh[0]*umesh.dh[0]);
			double idy2 = 1.0/(umesh.dh[1]*umesh.dh[1]);
			
			double phi_m[][] = this.phi_m.getField(umesh).getData();
			double rho_m[][] = this.rho_m.getField(umesh).getData();
							
			for (int it = 0; it<solver_it; it++)
			{
				for (int i=0;i<mesh.ni;i++)
					for (int j=0;j<mesh.nj;j++)
					{	
						if (i==0 && j==0) phi_m[i][j] = 0;
						
					    /*else assume neumann boundaries*/
					    if (i==0) phi_m[i][j] = phi_m[i+1][j];
					    else if (i==mesh.ni-1) phi_m[i][j] = phi_m[i-1][j];
					    else if (j==0) phi_m[i][j] = phi_m[i][j+1];
					    else if (j==mesh.nj-1) phi_m[i][j] = phi_m[i][j-1];
					    else
					    {
							double b = -rho_m[i][j];
							
							double phi_new = (b + 
									idx2*(phi_m[i-1][j] + phi_m[i+1][j]) +
									idy2*(phi_m[i][j-1] + phi_m[i][j+1])) / (2*(idx2+idy2));
				                
				            		/*SOR*/
							phi_m[i][j] = phi_m[i][j] + 1.4*(phi_new-phi_m[i][j]);
					    }  
					}

			    	//convergence check
			    	if (it%25==0)
			    	{
			    		double sum = 0;
	
			    		for (int i=0;i<mesh.ni;i++)
			    			for (int j=0;j<mesh.nj;j++)				
			    			{			
			    				if (i==0 && j==0) continue;
								
								if (i==0) phi_m[i][j] = phi_m[i+1][j];
								else if (i==mesh.ni-1) phi_m[i][j] = phi_m[i-1][j];
								else if (j==0) phi_m[i][j] = phi_m[i][j+1];
								else if (j==mesh.nj-1) phi_m[i][j] = phi_m[i][j-1];
								else			
								{
									double b = -rho_m[i][j];			
									double R = b + 
											idx2*(phi_m[i-1][j]-2*phi_m[i][j]+phi_m[i+1][j])+
											idy2*(phi_m[i][j-1]-2*phi_m[i][j]+phi_m[i][j+1]);
		
									sum += R*R;
								}
			    			}
	
			    		double L2 = Math.sqrt(sum/(mesh.ni*mesh.nj));
			    		if (L2<solver_tol) {System.out.printf(" MagSolver converged\n");break;}
			    		//else System.out.printf("L2 = %g\n",L2);
			    	}	/*convergence check*/
				}
			
			}
		// set bfi
		setGradient();
		
	}

	// computes divergence of M, rho_m = -div(M);
	void setRhoM() {
		
		for (Mesh mesh:Starfish.domain_module.getMeshList()) {
			/*this only works for uniform mesh*/
			UniformMesh umesh = (UniformMesh) mesh;
			double[][] magMi = this.magMi.getField(umesh).getData();
			double[][] magMj = this.magMj.getField(umesh).getData();
			double[][] magMk = this.magMk.getField(umesh).getData();
					
			for (int i=0;i<umesh.ni;i++) 
				for (int j=0;j<umesh.nj;j++) {
					double[] magM = umesh.getNode(i, j).mag_M;
					magMi[i][j] = magM[0];
					magMj[i][j] = magM[1];
					magMk[i][j] = magM[2];								
				}
		}
		
		for (Mesh mesh:Starfish.domain_module.getMeshList()) {
			
			/*this only works for uniform mesh*/
			UniformMesh umesh = (UniformMesh) mesh;
			
			double[][] magMi = this.magMi.getField(umesh).getData();
			double[][] magMj = this.magMj.getField(umesh).getData();
			//double[][] magMk = this.magMk.getField(umesh).getData();
			double[][] rho_m = this.rho_m.getField(umesh).getData();
			
			double dx = (umesh.dh[0]);
			double dy = (umesh.dh[1]);
			
			for (int i=0;i<umesh.ni;i++) 
				for (int j=0;j<umesh.nj;j++) {
					
					double divX = 0;
					double divY = 0;
					double divZ = 0;
					
					if (i==0) divX = (magMi[i+1][j]-magMi[i][j])/dx;
					else if (i==umesh.ni-1) divX = (magMi[i][j]-magMi[i-1][j])/dx;
					else divX = (magMi[i+1][j]-magMi[i-1][j])/dx;
					
					if (j==0) divY = (magMj[i][j+1]-magMj[i][j])/dy;
					else if (j==umesh.nj-1) divY = (magMj[i][j]-magMj[i][j-1])/dy;
					else divY = (magMj[i][j+1]-magMj[i][j-1])/dy;
					
					rho_m[i][j] = - (divX+divY+divZ);
					
				}
		}
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
			
			for (int i=0;i<umesh.ni;i++) 
				for (int j=0;j<umesh.nj;j++) {
					int im = Math.max(0,i-1); int ip = Math.min(umesh.ni-1,i+1);
					int jm = Math.max(0,j-1); int jp = Math.min(umesh.nj-1,j+1);
					
					bfi[i][j] = (phi_m[im][j]-phi_m[ip][j])/((ip-im)*dx);
					bfj[i][j] = (phi_m[i][jm]-phi_m[i][jp])/((jp-jm)*dy);
					
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
		    
		    int solver_it = InputParser.getInt("max_it",element);
		    double solver_tol = InputParser.getDouble("solver_tol",element,1e-4);
		    solver= new MagSolver(solver_it,solver_tol);
		
		    /*log*/
		    Starfish.Log.log("Added Magnetostatic (Mag) solver");
		    Starfish.Log.log("> Solver Max Iterations: " + solver_it);
		    Starfish.Log.log("> Solver Tolerance: " + solver_tol);
		    return solver;
		}
    };


}
