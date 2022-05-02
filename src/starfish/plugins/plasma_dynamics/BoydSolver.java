/*
Detailed electron potential solver based on Boyd's
'Modeling of the near field plume of a Hall thruster'
for now, polytropic model from
'Far field modeling of the plasma plume of a Hall thruster'
is used instead of the temperature solver
 */
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
import starfish.pic.PotentialSolver;

public class BoydSolver extends PotentialSolver
{	
    public BoydSolver (double den0, double kTe, double gamma)
    {
		super();
			
		/*reference values for polytropic model*/
		this.den0 = den0;
		this.kTe0 = kTe;	    /* in eV */
		this.gamma = gamma;
	
		psi = Starfish.domain_module.getFieldManager().add("psi","", null);
		te = Starfish.domain_module.getFieldManager().add("Te", "K", null);
		ue = Starfish.domain_module.getFieldManager().add("ue", "m/s", null);
		ve = Starfish.domain_module.getFieldManager().add("ve", "m/s", null);
		sigma = Starfish.domain_module.getFieldManager().add("sigma", "", null);
		RHS = Starfish.domain_module.getFieldManager().add("RHS", "", null);
		Jx = Starfish.domain_module.getFieldManager().add("Jx", "A/m^2", null);
		Jy = Starfish.domain_module.getFieldManager().add("Jy", "A/n^2", null);
    }
    
    FieldCollection2D psi;
    FieldCollection2D te;
    FieldCollection2D ue;
    FieldCollection2D ve;
    FieldCollection2D sigma;
    FieldCollection2D RHS;
    FieldCollection2D Jx;
    FieldCollection2D Jy;
    FieldCollection2D ne;
    
    @Override
    public void update() 
    {
		//testing
		ne = Starfish.materials_module.getIonDensity();
	//	ne.mult(1e7);
		
		
	//	updateElectronVelocity();
		
		updateElectronTemperaturePolytropic();
		te.setValue(1*Constants.EVtoK);
		
		evalConductivity();
		updatePotential();
		evalCurrentDensity();
		
		updateGradientField();
    }
    
    /*obtains electron velocity by solving nabla^2(psi)=0
      (RHS is actually ne*ni*Ci, but ignoring plume ionization)
      grad(psi) = ne*ue    
    */
    protected void updateElectronVelocity()
    {
		//need to get ion den and ion vel
		//first solve nabla(psi) = 0, where grad(psi)=n*u
		for (Solver.MeshData md:mesh_data)
		{
		    Mesh mesh = md.mesh;
	
		    /*flatten data*/
		    md.x = Vector.deflate(psi.getField(mesh).getData());
		    md.b = Vector.zeros(md.x.length); 	    
		    
		    /*update boundaries */
		    for (int j=0;j<5;j++) 
		    {
		    	md.b[md.mesh.IJtoN(0, j)] = -1e17*1500;		
		    }
		    /*
		    for (int i=0;i<md.mesh.ni;i++) 
		    {
			md.b[md.mesh.IJtoN(i, 0)] = md.mesh.node[i][0].bc_value;
			md.b[md.mesh.IJtoN(i,md.mesh.nj-1)] = md.mesh.node[i][md.mesh.nj-1].bc_value;		
		    }
		    */
		    /*add objects*/
		    //md.b = Vector.merge(md.fixed_node, md.x, md.b);
		}
			
		/* solve potential */
		//solvePotentialNL();
		//solveLinearGS(mesh_data);
		
		for (Solver.MeshData md:mesh_data)
		{
		    Mesh mesh = md.mesh;
		    double ue[] = new double[md.x.length];
		    double ve[] = new double[md.x.length];
	
		   // evaluateGradient(md.x,ue,ve,md,1);
		    
		}
		
		/*inflate and update electric field*/
		for (Solver.MeshData md:mesh_data)
		    Vector.inflate(md.x, md.mesh.ni, md.mesh.nj, psi.getField(md.mesh).getData());
		
		
	    }
	    
	    //computed electron temperature from the polytropic relationship
	    protected void updateElectronTemperaturePolytropic()
	    {
		FieldCollection2D rho_fc = Starfish.domain_module.getRho();
		for (Solver.MeshData md:mesh_data)
		{
		    Mesh mesh = md.mesh;
		    
		    double ne[][] = this.ne.getField(mesh).getData();
		    double te[][] = this.te.getField(mesh).getData();
		    
		    for (int i=0;i<mesh.ni;i++)
			for (int j=0;j<mesh.nj;j++)
			{
			    te[i][j] = kTe0 * Math.pow(ne[i][j]/(den0), gamma-1);
			    if (te[i][j]<0.1*Constants.EVtoK) te[i][j] = 0.1*Constants.EVtoK;
			}
		}
    }
    
    protected void evalConductivity()
    {
		for (Solver.MeshData md:mesh_data)
		{
		    Mesh mesh = md.mesh;
		    double te[][] = this.te.getField(md.mesh).getData();
		    double sigma[][] = this.sigma.getField(md.mesh).getData();
		    for (int i=0;i<mesh.ni;i++)
			for (int j=0;j<mesh.nj;j++)
			{
			    //floors to avoid division by zero
			    if (te[i][j]<0.01*Constants.EVtoK) te[i][j] = 0.01*Constants.EVtoK;
			    
			    double v = Math.sqrt(Constants.K*te[i][j]/Constants.ME);	//v_rms also from (1/2)m*v^2=(3/2)kTe
			    sigma[i][j] = 16*Math.PI*Constants.EPS0*Constants.EPS0*Constants.ME*v*v*v/(Constants.QE*Constants.QE);  
			}	    
		    
		    //perform smoothing passes
		    for (int p=0;p<0;p++)
			for (int i=1;i<mesh.ni-1;i++)
			    for (int j=1;j<mesh.nj-1;j++)
			    {
			    	sigma[i][j] = (1/8.0)*(4*sigma[i][j]+
					sigma[i-1][j]+sigma[i+1][j]+
					sigma[i][j-1]+sigma[i][j+1]);
			    }	       	
		}
    }
    
    //updates potential by solving ohm's law
    protected void updatePotential()
    {	
		//compute k/(e*ne)*nabla^2(ne*te)
		for (MeshData md:mesh_data)
		{
		    UniformMesh mesh = (UniformMesh)md.mesh;
		    double phi[][] = Starfish.domain_module.getPhi(mesh).getData();
		    double ne[][] = this.ne.getField(md.mesh).getData();
		    double te[][] = this.te.getField(md.mesh).getData();
		    double sigma[][] = this.sigma.getField(md.mesh).getData();
		    
		    double nab_ne[][] = new double[mesh.ni][mesh.nj];
		    double grad_grad_2[][] = new double[mesh.ni][mesh.nj];
		    	    
		    double dx = mesh.dh[0];
		    double dy = mesh.dh[1];
		    double dx2 = dx*dx;
		    double dy2 = dy*dy;
		    
		    //evaluate terms independent of phi
		    for (int i=1;i<mesh.ni-1;i++)
			for (int j=1;j<mesh.nj-1;j++)
			{
			    if (ne[i][j]==0) continue;	    //shouldn't happen as floor is applied in getIonDensity
			    
			    //term 3, (k/(e*ne))*laplacian(ne*Te)
			    nab_ne[i][j] = Constants.K/(Constants.QE*ne[i][j])*(
					   (ne[i+1][j]*te[i+1][j]-2*ne[i][j]*te[i][j]+ne[i-1][j]*te[i-1][j])/(dx2) +
					   (ne[i][j+1]*te[i][j+1]-2*ne[i][j]*te[i][j]+ne[i][j-1]*te[i][j-1])/(dy2)
					   );
			    
			    //term 2, k/(sigma*e)*grad(sigma/ne).grad(ne*Te)
			    grad_grad_2[i][j] = Constants.K/(sigma[i][j]*Constants.QE) * (
				                ((sigma[i+1][j]/ne[i+1][j]) - (sigma[i-1][j]/ne[i-1][j]))*(ne[i+1][j]*te[i+1][j]-ne[i-1][j]*te[i-1][j])/(4*dx2) + 
						((sigma[i][j+1]/ne[i][j+1]) - (sigma[i][j-1]/ne[i][j-1]))*(ne[i][j+1]*te[i][j+1]-ne[i][j-1]*te[i][j-1])/(4*dy2)
						); 				    
			}
		
		    //ev
		    //solve lap(phi)=(1/sigma)*((grad_sigma*(-grad_phi)+k/(e*ne)*lap(ne*te)
		    
		    //for (int i=0;i<mesh.ni;i++)
		//	for (int j=0;j<mesh.nj;j++) phi[i][j]=0;
		    
		    //double RHS[][] = new double[mesh.ni][mesh.nj];
		    double RHS[][] = this.RHS.getField(mesh).getData();
			
		    double norm = 0;
		    
		    //eval RHS = (1/sigma)*((grad_sigma*(-grad_phi)+k/(e*ne)*lap(ne*te)
		    for (int outer_it=0;outer_it<10;outer_it++)
		    {
			for (int i=1;i<mesh.ni-1;i++)
			    for (int j=1;j<mesh.nj-1;j++)
			    {
				    //update grad(sigma)*grad(phi)
				    double grad_grad = (-1/sigma[i][j])*(
						       (sigma[i+1][j]-sigma[i-1][j])*(phi[i+1][j]-phi[i-1][j])/(4*dx2)+
						       (sigma[i][j+1]-sigma[i][j-1])*(phi[i][j+1]-phi[i][j-1])/(4*dy2)
						       );
				    RHS[i][j] = grad_grad + grad_grad_2[i][j] + nab_ne[i][j];
	
				 //   System.out.printf("%g\n",grad_grad);
				}
			
			   //perform smoothing passes
			    for (int p=0;p<10;p++)
				for (int i=1;i<mesh.ni-1;i++)
				    for (int j=1;j<mesh.nj-1;j++)
				    {
					RHS[i][j] = (1/8.0)*(4*RHS[i][j]+
						RHS[i-1][j]+RHS[i+1][j]+
						RHS[i][j-1]+RHS[i][j+1]);
				    }	
	
			 //converge
			 int it;
			for ( it=0;it<500;it++)
			{
			    for (int i=0;i<mesh.ni;i++)
				for (int j=0;j<mesh.nj;j++)
				{
				    if (i==0) {
					    phi[i][j] = 0;	//500V/m
				    }
				    else if (i==mesh.ni-1) phi[i][j] = 0;	    //chamber wall
				    else if (j==0) phi[i][j] = phi[i][j+1];	    //symmetry
				    else if (j==mesh.nj-1) phi[i][j] = 0;	    //chamber wall
				    else
				    {			
					double a = -RHS[i][j] + 
						   (phi[i-1][j] + phi[i+1][j])/dx2 +
						   (phi[i][j+1] + phi[i][j-1])/dy2;
					double g = a / (2/dx2 + 2/dy2);
					phi[i][j] += 1.4*(g-phi[i][j]);
				    }
	
				}
	
			    //compute residue
	
			    if (it%100==0)
			    {
				double sum = 0;
				for (int i=1;i<mesh.ni-1;i++)
				for (int j=1;j<mesh.nj-1;j++)
				{
				    double r = -RHS[i][j] + (
					    (phi[i-1][j]-2*phi[i][j]+phi[i+1][j])/dx2 +
					    (phi[i][j-1]-2*phi[i][j]+phi[i][j+1])/dy2);
				    sum += r*r;			
				}
	
				norm = Math.sqrt(sum/((mesh.ni-2)*(mesh.nj-2)));
				if (norm<1e-3) break;		    
			    }
			}
	
			System.out.printf("outer_it %d, Norm: %g in it:%d\n",outer_it, norm,it);
		    } //outer it		    
		}
    }
    
    protected void evalCurrentDensity()
    {
		for (MeshData md:mesh_data)
		{
		    UniformMesh mesh = (UniformMesh)md.mesh;
		    
		    double dx = mesh.dh[0];
		    double dy = mesh.dh[1];
		    
		    double jx[][] = Jx.getField(mesh).getData();
		    double jy[][] = Jy.getField(mesh).getData();
		    double sigma[][] = this.sigma.getField(mesh).getData();
		    double ne[][] = this.ne.getField(mesh).getData();
		    double te[][] = this.te.getField(mesh).getData();
		    double phi[][] = Starfish.domain_module.getPhi(mesh).getData();
		    
		    for (int i=0;i<mesh.ni;i++)
			for (int j=0;j<mesh.nj;j++)
			{
			    double grad_phi_x, grad_phi_y;
			    double grad_ne_te_x,grad_ne_te_y;
			    
			    if (i==0)
			    {
			    	grad_phi_x = (phi[i+1][j]-phi[i][j])/(dx);
			    	grad_ne_te_x = (ne[i+1][j]*te[i+1][j] - ne[i][j]*te[i][j])/(2*dx);
			    }
			    else if (i==mesh.ni-1)
			    {
			    	grad_phi_x = (phi[i][j]-phi[i-1][j])/(dx);
			    	grad_ne_te_x = (ne[i][j]*te[i][j] - ne[i-1][j]*te[i-1][j])/(dx);
			    }
			    else
			    {
				grad_phi_x = (phi[i+1][j]-phi[i-1][j])/(2*dx);
				grad_ne_te_x = (ne[i+1][j]*te[i+1][j] - ne[i-1][j]*te[i-1][j])/(2*dx);
			    }
		
			    if (j==0)
			    {
				grad_phi_y = (phi[i][j+1]-phi[i][j])/(dy);
				grad_ne_te_y = (ne[i][j+1]*te[i][j+1] - ne[i][j]*te[i][j])/(dy);
			    }
			    else if (j==mesh.nj-1)
			    {
				grad_phi_y = (phi[i][j]-phi[i][j-1])/(dy);
				grad_ne_te_y = (ne[i][j]*te[i][j] - ne[i][j-1]*te[i][j-1])/(dy);
			    }
			    else
			    {
				grad_phi_y = (phi[i][j+1]-phi[i][j-1])/(2*dy);
				grad_ne_te_y = (ne[i][j+1]*te[i][j+1] - ne[i][j-1]*te[i][j-1])/(2*dy);
			    }
				    
			    jx[i][j] = sigma[i][j]*(-grad_phi_x + Constants.K/(Constants.QE*ne[i][j])*grad_ne_te_x);
			    jy[i][j] = sigma[i][j]*(-grad_phi_y + Constants.K/(Constants.QE*ne[i][j])*grad_ne_te_y);
			    
			}
		}
    }
    
    protected int solvePotentialLin() 
    {
		/*call linear solver*/
		int it = 0;// solveLinearGS(mesh_data);
		
		/*TODO: hack for IEPC*/
		//int it = solveLinearGSsimple();
		for (Solver.MeshData md:mesh_data)
		{
		    Mesh mesh = md.mesh;
	
		    /*flatten data*/
		    md.x = Vector.deflate(Starfish.domain_module.getPhi(mesh).getData());
		}
		
		return it;
    }
   
    /*SOLVER FACTORY*/
    public static SolverModule.SolverFactory BoydSolverFactory = new SolverModule.SolverFactory()
    {
		@Override
		public Solver makeSolver(Element element)
		{
		    Solver solver;
		    
		    double n0=InputParser.getDouble("n0", element);
		    double Te0=InputParser.getDouble("Te0", element)*Constants.EVtoK;
		    double gamma = InputParser.getDouble("gamma",element,5.0/3.0);
			
		    solver=new BoydSolver(n0, Te0, gamma);
		
		    /*log*/
		    Starfish.Log.log("Added Boyd Detailed Electron solver");
		    Starfish.Log.log("> n0: " + n0 + " (#/m^3)");
		    Starfish.Log.log("> T0: " + Te0 + " (eV)");
		    Starfish.Log.log("> gamma: " + gamma );
		    return solver;
		}
    };

    //variables
    
    double gamma;	//gamma for polytropic model
    
}
