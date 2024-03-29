
/*
Potential solver based on Geng, et.al, J. Appl. Physics, Vol. 114, No 103305, 2013
 */

package starfish.plugins.plasma_dynamics;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.UniformMesh;
import starfish.core.io.InputParser;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;
import starfish.pic.PotentialSolver;

public class GengSolver extends PotentialSolver
{	
    public GengSolver (double Te0, int solver_it,double solver_tol)
    {
		super();
			
		this.Te0 = Te0;
		this.solver_it = solver_it;
		this.solver_tol = solver_tol;
		
		// this solver is currently working only for Z-R configuration
		if (Starfish.getDomainType()!=DomainType.ZR)
			Starfish.Log.error("Geng solver requires ZR computational domain");
		
		// need to set electron temperature somehow
		te = Starfish.domain_module.getFieldManager().add("Te", "K", null);
		
		// get e field
		efi = Starfish.domain_module.getEfi();
		efj = Starfish.domain_module.getEfj();
		
		// b field
		bfi = Starfish.domain_module.getBfi();
		bfj = Starfish.domain_module.getBfj();
		
		// this should probably be moved over to the material module
		mu = Starfish.domain_module.getFieldManager().add("mu", "", null);
		
		mu11 = Starfish.domain_module.getFieldManager().add("mu11", "", null);
		mu12 = Starfish.domain_module.getFieldManager().add("mu12", "", null);
		mu21 = Starfish.domain_module.getFieldManager().add("mu21", "", null);
		mu22 = Starfish.domain_module.getFieldManager().add("mu22", "", null);
		
		// Zs and Rs
		Z1 = Starfish.domain_module.getFieldManager().add("Z1","",null);
		Z2 = Starfish.domain_module.getFieldManager().add("Z2","",null);
		Z3 = Starfish.domain_module.getFieldManager().add("Z3","",null);
		R1 = Starfish.domain_module.getFieldManager().add("R1","",null);
		R2 = Starfish.domain_module.getFieldManager().add("R2","",null);
		R3 = Starfish.domain_module.getFieldManager().add("R3","",null);
		
		// jz and jr for visualization
		jz = Starfish.domain_module.getFieldManager().add("jz","",null);
		jr = Starfish.domain_module.getFieldManager().add("jr","",null);
    }
    
    FieldCollection2D efi,efj;
    FieldCollection2D bfi,bfj;
    FieldCollection2D mu;
    FieldCollection2D mu11,mu12,mu21,mu22;
    FieldCollection2D te;
    FieldCollection2D na;
    FieldCollection2D ne;
    FieldCollection2D Z1,Z2,Z3;
    FieldCollection2D R1,R2,R3;
    FieldCollection2D phi0;
    FieldCollection2D jz,jr;
    
 
    double Te0;		// electron temperature to use in eV
    int solver_it;	// max number of iterations
    double solver_tol;	
    
    final double  H       =   0.015;		// not sure what this is
    final double ne_min = 1e4;		// minimum electron density

    
    @Override
    public void update() 
    {
	
    	// plasma and neutral densities
    	ne = Starfish.materials_module.getIonDensity();
    	na = Starfish.materials_module.getNeutralDensity();
    			
    	setCollisionFrequency();
    	evalMobility();
    	evalCoeffs();
    		
		te.setValue(Te0*Constants.EVtoK);
		
		updatePotential();
		
		updateGradientField();
		
		setCurrent();
    }

    // evaluates nu
    protected void setCollisionFrequency() {
    	for (Solver.MeshData md:mesh_data)
		{
    		double te[][] = this.te.getField(md.mesh).getData();
    		double na[][] = this.na.getField(md.mesh).getData();
    		double bfi[][] = this.bfi.getField(md.mesh).getData();
    		double bfj[][] = this.bfj.getField(md.mesh).getData();
    		double mu[][] = this.mu.getField(md.mesh).getData();
    		
    		for (int i=0;i<md.mesh.ni;i++)
    			for (int j=0;j<md.mesh.nj;j++)
    			{
    				// using equations from Geng's code, check these
    				double TeEv = te[i][j]/Constants.KtoEV;
    				double veth = 4.19e5*Math.sqrt(TeEv);  // electron thermal velocity
    				double nuen = na[i][j]*2.e-20*veth;
    				
    				double B = Math.sqrt(bfi[i][j]*bfi[i][j] + bfj[i][j]*bfj[i][j]);
    		        double nueb = 1./44.*Constants.QE*B/Constants.ME;
    		        double nuew = veth/H/(2.e6/(4*2.e9*0.015)); // double-check this to see how to solve the literature
    		        double nu = nuen + nueb + nuew;
    		        
    		        mu[i][j] = 0;
    		        if (nu>0)  mu[i][j] = Constants.QE/(Constants.ME*nu);    		        
    			}
		}
    }
    
    
    /* this functions sets values of mu11-mu22 using nu
     */
    protected void evalMobility() {
    	for (Solver.MeshData md:mesh_data)
		{
    		
    		double Bz2d[][], Br2d[][];
    		if (Starfish.getDomainType()==DomainType.RZ) {
    			Br2d = this.bfi.getField(md.mesh).getData();
    			Bz2d = this.bfj.getField(md.mesh).getData();
    		}
    		else {
    			Bz2d = this.bfi.getField(md.mesh).getData();
    			Br2d = this.bfj.getField(md.mesh).getData();    			
    		}
    		
    		double mu2d[][] = this.mu.getField(md.mesh).getData();
    		double mu11[][] = this.mu11.getField(md.mesh).getData();
    		double mu12[][] = this.mu12.getField(md.mesh).getData();
    		double mu21[][] = this.mu21.getField(md.mesh).getData();
    		double mu22[][] = this.mu22.getField(md.mesh).getData();

    		for (int i=0;i<md.mesh.ni;i++)
    			for (int j=0;j<md.mesh.nj;j++)
    			{
    				double mu = mu2d[i][j];
    				double Bz = Bz2d[i][j];
    				double Br = Br2d[i][j];
    				double Btheta = 0;
    				double B = Math.sqrt(Bz*Bz + Br*Br + Btheta*Btheta);
    				
    				mu11[i][j] = (1+mu*mu*Bz*Bz)/(1+mu*mu*B*B);
    				mu12[i][j] = (mu*Btheta + mu*mu*Bz*Br)/(1+mu*mu*B*B);
    				mu21[i][j] = (-mu*Btheta + mu*mu*Bz*Br)/(1+mu*mu*B*B);
    				mu22[i][j] = (1+mu*mu*Br*Br)/(1+mu*mu*B*B);    				
    			}    		
		}
     }
   
    //sets Z and R coefficients
    protected void evalCoeffs() {
    	for (MeshData md:mesh_data) {
		    UniformMesh mesh = (UniformMesh)md.mesh;
		    double dz = mesh.dh[0];
		    double dr = mesh.dh[1];
		    
    		double Z1[][] = this.Z1.getField(md.mesh).getData();
    		double Z2[][] = this.Z2.getField(md.mesh).getData();
    		double Z3[][] = this.Z3.getField(md.mesh).getData();
    		double R1[][] = this.R1.getField(md.mesh).getData();
    		double R2[][] = this.R2.getField(md.mesh).getData();
    		double R3[][] = this.R3.getField(md.mesh).getData();
    		double mu11[][] = this.mu11.getField(md.mesh).getData();
    		double mu12[][] = this.mu12.getField(md.mesh).getData();
    		double mu21[][] = this.mu21.getField(md.mesh).getData();
    		double mu22[][] = this.mu22.getField(md.mesh).getData();
    		double mu[][] = this.mu.getField(md.mesh).getData();
    		double ne[][] = this.ne.getField(md.mesh).getData();
    		double te[][] = this.te.getField(md.mesh).getData();
    		
    		for (int i=0;i<md.mesh.ni;i++)
    			for (int j=0;j<md.mesh.nj;j++) {
    				double sigma = mu[i][j]*ne[i][j]*Constants.QE;
    				
    				//gradients
    				int im=i-1,ip=i+1;
    				int jm=j-1,jp=j+1;
    				
    				if (im<0) im = 0;
    				if (ip>md.mesh.ni-1) ip=md.mesh.ni-1;
    				if (jm<0) jm = 0;
    				if (jp>md.mesh.nj-1) jp=md.mesh.nj-1;
    				
    				double nepz = ne[ip][j];
    				double nemz = ne[im][j];
    				double nepr = ne[i][jp];
    				double nemr = ne[i][jm];
    				if (nepz<ne_min) nepz=ne_min;
    				if (nemz<ne_min) nemz=ne_min;
    				if (nepr<ne_min) nepr=ne_min;
    				if (nemr<ne_min) nemr=ne_min;
    				
    				
    				double gradz_te = (te[ip][j]-te[im][j])/((ip-im)*dz);
    				double gradz_lnn = (Math.log(nepz)-Math.log(nemz))/((ip-im)*dz);
    				double gradr_te = (te[i][jp]-te[i][jm])/((jp-jm)*dr);
    				double gradr_lnn = (Math.log(nepr)-Math.log(nemr))/((jp-jm)*dr);
    				
    				//terms    				
    				Z1[i][j] = mu11[i][j]*sigma;
    				Z2[i][j] = mu12[i][j]*sigma;
    				Z3[i][j] = mu11[i][j]*sigma*(gradz_te + te[i][j]*gradz_lnn) + 
    						   mu12[i][j]*sigma*(gradr_te + te[i][j]*gradr_lnn);
    				
    				R1[i][j] = mu21[i][j]*sigma;
    				R2[i][j] = mu22[i][j]*sigma;
    				R3[i][j] = mu21[i][j]*sigma*(gradz_te+te[i][j]*gradz_lnn) + 
    						   mu22[i][j]*sigma*(gradr_te+te[i][j]*gradr_lnn);   		
    			}
    		
    	}
    }
    
    // sets jz and jr terms for visualization
    void setCurrent() {
    	for (MeshData md:mesh_data)
		{
    		double Z1[][] = this.Z1.getField(md.mesh).getData();
    		double Z2[][] = this.Z2.getField(md.mesh).getData();
    		double Z3[][] = this.Z3.getField(md.mesh).getData();
    		double R1[][] = this.R1.getField(md.mesh).getData();
    		double R2[][] = this.R2.getField(md.mesh).getData();
    		double R3[][] = this.R3.getField(md.mesh).getData();
    		double ez[][] = this.efi.getField(md.mesh).getData();
    		double er[][] = this.efj.getField(md.mesh).getData();		  
    		double jz[][] = this.jz.getField(md.mesh).getData();
    		double jr[][] = this.jr.getField(md.mesh).getData();
    		for (int i=0;i<md.mesh.ni;i++) 
    			for (int j=0;j<md.mesh.nj;j++) {
    				jz[i][j] = Z1[i][j]*ez[i][j] + Z2[i][j]*er[i][j] + Z3[i][j];
    				jr[i][j] = R1[i][j]*ez[i][j] + R2[i][j]*er[i][j] + R3[i][j];    				
    			}
    		
		}

    }
    //updates potential by solving ohm's law
    protected void updatePotential()
    {
    	for (MeshData md:mesh_data)
		{
		    double phi[][] = Starfish.domain_module.getPhi(md.mesh).getData();

    		for (int i=0;i<md.mesh.ni;i++) 
    			for (int j=0;j<md.mesh.nj;j++) {
    				if (md.mesh.isDirichletNode(i, j)) 
    					phi[i][j] = md.mesh.nodeBCValue(i,j);
    				else phi[i][j] = 0;
    			}
		}
		
		
		for (int it=0;it<solver_it;it++) {
			
			double R_sum = 0;
			int node_count = 0;
						
			for (MeshData md:mesh_data)
			{
			    UniformMesh mesh = (UniformMesh)md.mesh;
			    double phi[][] = Starfish.domain_module.getPhi(mesh).getData();
			    
	    		double Z1[][] = this.Z1.getField(md.mesh).getData();
	    		double Z2[][] = this.Z2.getField(md.mesh).getData();
	    		double Z3[][] = this.Z3.getField(md.mesh).getData();
	    		double R1[][] = this.R1.getField(md.mesh).getData();
	    		double R2[][] = this.R2.getField(md.mesh).getData();
	    		double R3[][] = this.R3.getField(md.mesh).getData();
	    		double ez[][] = this.efi.getField(md.mesh).getData();
	    		double er[][] = this.efj.getField(md.mesh).getData();
	    		
			    double dz = mesh.dh[0];
			    double dr = mesh.dh[1];
			
			    // recompute electric field
			    for (int i=0;i<mesh.ni;i++) 
			    	for (int j=0;j<mesh.nj;j++) {
			    		if (i==0) ez[i][j] = -(phi[i+1][j]-phi[i][j])/dz;
			    		else if (i==mesh.ni-1) ez[i][j] = -(phi[i][j]-phi[i-1][j])/dz;
			    		else ez[i][j] = -(phi[i+1][j]-phi[i-1][j])/(2*dz);
			    		
			    		if (j==0) er[i][j] = -(phi[i][j+1]-phi[i][j])/dr;
			    		else if (j==mesh.nj-1) er[i][j] = -(phi[i][j]-phi[i][j-1])/dr;
			    		else er[i][j] = -(phi[i][j+1]-phi[i][j-1])/(2*dr);			    		
			    	}
							
			    
			    for (int i=0;i<mesh.ni;i++) { 
			    	
			    	for (int j=0;j<mesh.nj;j++) {
			    
			    		// skip over dirichlet nodes
			    		if (mesh.isDirichletNode(i, j)) {
			    			continue;
			    		}
			    	 	
			    		// symmetry on r 0
			    		if (i==0) {
			    			phi[i][j] = phi[i+1][j];
			    			continue;
			    		} 
			    		else if (i==mesh.ni-1) {
			    			phi[i][j] = phi[i-1][j];
			    			continue;
			    		}
			    		else if (j==0) {
			    			phi[i][j] = phi[i][j+1];	
			    			continue;
			    		}	
			    		else if (j==mesh.nj-1) {
			    			phi[i][j] = phi[i][j-1];
			    			continue;
			    		}
			    					    		
			    		double r = mesh.R(i,j);
			    					    		
			    		double Ezn = 0.5*(ez[i][j]+ez[i][j+1]);
			    		double Ezs = 0.5*(ez[i][j]+ez[i][j-1]);
			    		double Ere = 0.5*(er[i][j]+er[i+1][j]);
			    		double Erw = 0.5*(er[i][j]+er[i-1][j]);
			    		
			    		double R1n = 0.5*(R1[i][j]+R1[i][j+1]);
			    		double R1s = 0.5*(R1[i][j]+R1[i][j-1]);
			    		
			    		double R3n = 0.5*(R3[i][j]+R3[i][j+1]);
			    		double R3s = 0.5*(R3[i][j]+R3[i][j-1]);
			    		
			    		double Z1e = 0.5*(Z1[i][j]+Z1[i+1][j]);
			    		double Z1w = 0.5*(Z1[i][j]+Z1[i-1][j]);
			    		
			    		double Z2e = 0.5*(Z2[i][j]+Z2[i+1][j]);
			    		double Z2w = 0.5*(Z2[i][j]+Z2[i-1][j]);
			    		
			    		double Z3e = 0.5*(Z3[i][j]+Z3[i+1][j]);
			    		double Z3w = 0.5*(Z3[i][j]+Z3[i-1][j]);
			    		
			    		double R2n = 0.5*(R2[i][j]+R2[i][j+1]);
			    		double R2s = 0.5*(R2[i][j]+R2[i][j-1]);
			    		
			    		double a = Z1e/(dz*dz);
			    		double b = Z1w/(dz*dz);
			    		double c = R2n/(dr*dr);
			    		double d = R2s/(dr*dr);
			    			
			    		double S = -((Z2e*Ere+Z3e) - (Z2w*Erw+Z3w))/dz
			    				   -((R1n*Ezn+R3n) - (R1s*Ezs+R3s))/dr
			    				   -((R1n*Ezn+R3n) + (R1s*Ezs+R3s))/(2*r);
			    		
			    		
			    		double phi_s = phi[i][j];			    		
			    		double denom = (a+b+c*(1+dr)+d*(1-dr));
			    		double term = (S + a*phi[i+1][j] + b*phi[i-1][j] + c*(1+dr)*phi[i][j+1] + d*(1-dr)*phi[i][j-1]);
			    		if (denom!=0) 
			    			phi_s = (1/denom) * term;
			    						    		
			    		phi[i][j] = phi[i][j] + 1.4*(phi_s-phi[i][j]);
			    		
			    		// add to residue sum
			    		double R = phi[i][j]*denom - term;
			    		R_sum += R*R;
			    		node_count++;			// number of nodes used in the residue calculation
			    	}
			    }
		
			}  // mesh data
			
			double L2 = Math.sqrt(R_sum/node_count);
			if (it%50==0)
				System.out.printf("L2 = %g\n", L2);
			if (L2<solver_tol) break;
		}
	
    }
    
   
    /*SOLVER FACTORY*/
    public static SolverModule.SolverFactory GengSolverFactory = new SolverModule.SolverFactory()
    {
		@Override
		public Solver makeSolver(Element element)
		{
		    Solver solver;
		    
		    double Te = InputParser.getDouble("Te", element);
		    int solver_it = InputParser.getInt("solver_it",element);
		    double solver_tol = InputParser.getDouble("solver_tol",element);
		    solver=new GengSolver(Te,solver_it,solver_tol);
		
		    /*log*/
		    Starfish.Log.log("Added Geng Potential solver");
		    Starfish.Log.log("> Electron Temperature: " + Te + " (eV)" );
		    Starfish.Log.log("> Solver Max Iterations: " + solver_it);
		    Starfish.Log.log("> Solver Tolerance: " + solver_tol);
		    return solver;
		}
    };

    
}
