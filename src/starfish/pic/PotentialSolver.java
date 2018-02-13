/*
Parent class for solver that compute potential, defines 
method for ocmputing electric field
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

    /**
     *
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
	    
	    computeGradient(phi1, efi, efj,md, -1);
	    
	    Vector.inflate(efi, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getEfi(md.mesh).getData());
	    Vector.inflate(efj, md.mesh.ni, md.mesh.nj, Starfish.domain_module.getEfj(md.mesh).getData());
	}
    }   
}
