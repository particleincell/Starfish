/**
Parent class for solvers computing potential to avoid re-using the method
to compute electric field
 */
package starfish.pic;

import java.util.ArrayList;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Vec;
import starfish.core.domain.FieldCollection2D;
import starfish.core.solver.Solver;

/**
 *
 * @author Lubos Brieda
 */
abstract public class PotentialSolver extends Solver
{
     boolean qn; /* quasi-neutral solver? */

    /**
     *
     */
    public double kTe0 = 1; /* reference temperature values for boltzman relationship */

    /**
     *
     */
    public double den0 = 1e15;  /* reference values of ion density along bottom edge */
    
    protected int rho_moat;		//if positive, number of cells over which rho should decay quadratically
    public ArrayList<double[]> den0_pos = null;	/*positions for sampling reference density */
    
    /** updates den0 if sampling positions are specified*/
    public void updateDen0()
    {
		if (den0_pos==null || den0_pos.isEmpty()) return;
		
		FieldCollection2D rho_fc = Starfish.domain_module.getRho();
		double rho = 0;
		for (double[] pos : den0_pos)
		    rho+=rho_fc.eval(pos);
		
		den0 = rho/(den0_pos.size()*Constants.QE);	
		Starfish.Log.log("Reference plasma density: "+den0);
    }

    /**
     *
     */
    public double phi0 = 0; /* reference values of potential along bottom edge */
    /** Computes E=-grad(phi)
     */
    @Override
    public void updateGradientField()
    {
		/*inflate and update electric field*/
		for (Solver.MeshData md:mesh_data)
		{
		    double efi[] = new double[md.mesh.n_nodes];
		    double efj[] = new double[md.mesh.n_nodes];
		    double phi1[] = Vec.deflate(Starfish.domain_module.getPhi(md.mesh).getData());
		    
		    evaluateGradient(phi1, efi, efj,md, -1, Starfish.domain_module.getPhi());
		    
		    Vec.inflate(efi, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getEfi(md.mesh).getData());
		    Vec.inflate(efj, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getEfj(md.mesh).getData());
		}
    }   
}
