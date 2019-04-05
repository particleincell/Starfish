/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.solver;

import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Vector;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.FieldCollection2D;
import starfish.core.solver.Solver.LinearSolver;

public class LinearSolverPCG implements LinearSolver
{
    boolean first_time = true;
    
    @Override
    public int solve(Solver.MeshData[] md, FieldCollection2D fc, int max_it, double tolerance)
    {
	double norm = tolerance;
	
	if (first_time && Starfish.getDomainType()!=DomainType.XY)
	    Log.warning("PCG solver may not converge on axi-symmetric domains!!");
	
	double b[][] = new double[md.length][];
	double x[][] = new double[md.length][];
	Matrix A[]= new Matrix[md.length];
	Matrix Mi[] = new Matrix[md.length];
	double r[][] = new double[md.length][];
	double z[][] = new double[md.length][];
	double p[][] = new double[md.length][];
	
	for (int m=0;m<md.length;m++)
	{
	    /*pcg needs a non-zero initial guess (otherwise diverges), 
	      setting to b seems to do the trick   */
	    if (first_time) 
	    	md[m].x = Vector.copy(md[m].b);
	    
	    b[m]= md[m].b;
	    x[m]= md[m].x;
	    
	    
	    A[m] = md[m].A;
	 
	    //A[m].print("A");
	    
	    /*diagonal preconditioner*/
	    //Matrix M = Matrix.diag_matrix(A);
	    Matrix M = A[m].diag_matrix();
	    Mi[m] = M.inverse();	    //this is only defined for diagonal matrix
	    
	    //M.print("M");
	    //Mi[m].print("inv(M)");
	    
	    /*initialize*/
	    r[m] = Vector.subtract(b[m], A[m].mult(x[m]));  //r=b-Ax
	    z[m] = Mi[m].mult(r[m]);	    // z = Mi*r
	    p[m] = Vector.copy(z[m]);
	}
	
	first_time = false;
	
	
	/* SOLVER */
	int it = 1;			/*start with one so we don't compute residue on first run*/
	while (it <= max_it)
	{
	     /*** update boundaries**/	    
	   // Solver.updateGhostVector(md, fc);
	    
	    norm = 0;
	    
	    /*iterate over all meshes*/
	    for (int m=0;m<md.length;m++)
	    {
		
		//compute the maximum norm accross the domains
		double  norm_m = Vector.norm(r[m]);
		if (norm_m>norm) norm = norm_m;
		if (norm<tolerance) break;
		
		//alpha = dot(r,z) / dot(p,A*p)
		double alpha = Vector.dot(r[m], z[m]) / 
			       Vector.dot(p[m], A[m].mult(p[m]));
		
		//x = x + alpha*p
		Vector.addInclusive(x[m],Vector.mult(p[m], alpha));

		//save dot(z,r) for later use
		double zr_dot = Vector.dot(z[m],r[m]);
        
		//r = r - alpha*(A*p)
		Vector.subtractInclusive(r[m], Vector.mult(A[m].mult(p[m]),alpha));

		//z = Mi*r
		z[m] = Mi[m].mult(r[m]);
        
		// beta = dot(z,r)/ dot(z[k-1],r[k-1]))
		double beta = Vector.dot(z[m],r[m])/zr_dot;
		
		// p = z + beta*p
		p[m] = Vector.add(z[m], Vector.mult(p[m], beta));
		
	    }
	    
	    /* check convergence */
	    if (it % 25 == 0)
	    {
		System.out.printf("it: %d, norm = %.3g\n", it, norm);
	    }
	    	    
	    if (norm < tolerance)
	    {
		break;
	    }
	    it++;
	}

	it--;
	if (it >= max_it)
	{
	    Starfish.Log.warning(" !! PCG failed to converge in " + it + " iteration, norm = " + norm);
	}

	return it;
    }

    
}
