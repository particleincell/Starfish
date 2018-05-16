/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.solver;

import starfish.core.domain.FieldCollection2D;
import starfish.core.solver.Solver.LinearSolver;
import starfish.core.solver.Solver.MeshData;

/** Placeholder for a multigrid solver, not yet implemented
 * 
 * @author Lubos Brieda 
 */
public class LinearSolverMG implements LinearSolver
{
    /**
     * solves Ax=b for x using the Multigrid method
     * @return 
     */
    public int solve(MeshData mesh_data[], FieldCollection2D fc, int num_it, double tolerance)
    {
	/*TODO: Implement*/
	throw new UnsupportedOperationException("Not yet implemented");	
    }
    
}
