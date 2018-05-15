/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.solver;

import starfish.core.solver.Solver.LinearSolver;
import starfish.core.solver.Solver.MeshData;

/** Placehold for a multigrid solver, not yet implmented
 * 
 * @author Lubos Brieda 
 */
public class LinearSolverMG implements LinearSolver
{
    /**
     * solves Ax=b for x using the Multigrid method
     * @return 
     */
    public int solve(MeshData mesh_data[], int num_it, double tolerance)
    {
	/*TODO: Implement*/
	throw new UnsupportedOperationException("Not yet implemented");	
    }
    
}
