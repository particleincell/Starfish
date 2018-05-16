/**
Parent class for solvers computing potential to avoid re-using the method
to compute electric field
 */
package starfish.pic;

import starfish.core.common.Starfish;
import starfish.core.common.Vector;
import starfish.core.solver.Solver;

/**
 *
 * @author Lubos Brieda
 */
abstract public class PotentialSolver extends Solver
{
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
	    double phi1[] = Vector.deflate(Starfish.domain_module.getPhi(md.mesh).getData());
	    
	    evaluateGradient(phi1, efi, efj,md, -1, Starfish.domain_module.getPhi());
	    
	    Vector.inflate(efi, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getEfi(md.mesh).getData());
	    Vector.inflate(efj, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getEfj(md.mesh).getData());
	}
    }   
}
