/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.solver;

import starfish.core.common.Starfish;
import starfish.core.common.Vector;
import starfish.core.domain.FieldCollection2D;
import starfish.core.solver.Solver.LinearSolver;

public class LinearSolverPCG implements LinearSolver
{
    @Override
    public int solve(Solver.MeshData[] md, FieldCollection2D fc, int max_it, double tolerance)
    {
	double norm = tolerance;
	double alpha;
	double beta;
	
	double b[][] = new double[md.length][];
	double x[][] = new double[md.length][];
	Matrix A[]= new Matrix[md.length];
	Matrix Pi[] = new Matrix[md.length];
	double r[][] = new double[md.length][];
	double d[][] = new double[md.length][];
	double del_new[] = new double[md.length];
	
	for (int i=0;i<md.length;i++)
	{
	    b[i]= md[i].b;
	    x[i]= md[i].x;
	    A[i] = md[i].A;
	 
	    /*diagonal preconditioner*/
	    b[i] = Vector.mult(b[i], -1);
	    for (int j = 0; j < A[i].nr; j++)
		A[i].multRow(j, -1);

	    //Matrix M = Matrix.diag_matrix(A);
	    Matrix P = A[i].identity();
	    Pi[i] = P.inverse();
	    //	double Mi[] = Matrix.diag(Matrix.inverse(M)); 

	    /*initialize*/
	    r[i] = Vector.subtract(b[i], A[i].mult(x[i]));
	    d[i] = Pi[i].mult(r[i]);
	    del_new[i] = Vector.dot(r[i], d[i]);
	}

	/* SOLVER */
	int it = 1;			/*start with one so we don't compute residue on first run*/
	while (it <= max_it)
	{
	    /*reset norm*/
	    norm = -1;
	    
	     /*** update boundaries**/	    
	    Solver.updateGhostVector(md, fc);
	    
	    /*iterate over all meshes*/
	    for (int i=0;i<md.length;i++)
	    {
		double q[] = A[i].mult(d[i]);

		double t = Vector.dot(d[i], q);
		if (t == 0.0)
		{
		    /*already at exact solution*/
		    continue;
		}
		alpha = del_new[i] / t;

		Vector.addInclusive(x[i], Vector.mult(d[i], alpha));
		Vector.subtractInclusive(r[i], Vector.mult(q, alpha));
		//r = Vector.subtract(b, Matrix.mult(A,x));		

		double s[] = Pi[i].mult(r[i]);
		double del_old = del_new[i];
		del_new[i] = Vector.dot(r[i], s);
		beta = del_new[i] / del_old;
		d[i] = Vector.add(s, Vector.mult(d[i], beta));
		
		/* check convergence */
		if (it % 25 == 0)
		{
		    double norm_l = Vector.norm(r[i]);
		    if (norm_l>norm) norm=norm_l;
		}
		
	    }
	    
	    //	System.out.println(norm);
	    if (norm>0 && norm < tolerance)
	    {
		break;
	    }
	    it++;
	    System.out.printf("it: %d, norm: %g\n",it,norm);
	}

	it--;
	if (it >= max_it)
	{
	    Starfish.Log.warning(" !! PCG failed to converge in " + it + " iteration, norm = " + norm);
	}

	return it;
    }

    
}
