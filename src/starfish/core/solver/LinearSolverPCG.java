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
	
	double b[][] = new double[md.length][];
	double x[][] = new double[md.length][];
	Matrix A[]= new Matrix[md.length];
	Matrix Ci[] = new Matrix[md.length];
	Matrix Cit[] = new Matrix[md.length];
	double w[][] = new double[md.length][];
	double v[][] = new double[md.length][];
	double r[][] = new double[md.length][];
	double alpha[] = new double[md.length];
	
	for (int m=0;m<md.length;m++)
	{
	    b[m]= md[m].b;
	    x[m]= md[m].x;
	   
	    A[m] = md[m].A;
	 
	    A[m].print("A");
	    
	    /*diagonal preconditioner*/
	    //Matrix M = Matrix.diag_matrix(A);
	    Matrix C = A[m].diag_matrix();
	    Ci[m] = C.inverse();	    //this is only defined for diagonal matrix
	    Cit[m] = Ci[m].transpose();	    //for generality, identical to inv(C) for C=diag(A)
	    
	    C.print("C");
	    Ci[m].print("inv(C)");
	    Cit[m].print("t(i(C))");
	    
	    
		    
	    /*initialize*/
	    r[m] = Vector.subtract(b[m], A[m].mult(x[m]));  //r=b-Ax
	    w[m] = Ci[m].mult(r[m]);	    // w = inv(C)*r
	    v[m] = Cit[m].mult(w[m]);	    // v = tran(inv(C))*w
	    alpha[m] = Vector.dot(w[m],w[m]);
	    Vector.print(r[m],"R");
	    Vector.print(w[m],"w");
	    Vector.print(v[m],"v");
	    System.out.println(alpha[m]);
	}

	
	/* SOLVER */
	int it = 1;			/*start with one so we don't compute residue on first run*/
	while (it <= max_it)
	{
	     /*** update boundaries**/	    
	   // Solver.updateGhostVector(md, fc);
	    
	    /*iterate over all meshes*/
	    for (int m=0;m<md.length;m++)
	    {
		
		double u[] = A[m].mult(v[m]);	//u=A*v
		
		//t=alpha/dot(v*u), split up to avoid div by zero
		double t_den = Vector.dot(v[m], u);
		if (alpha[m] == 0.0 || t_den==0.0)
		{
		    /*already at exact solution*/
		    break;
		}
		double t = alpha[m]/t_den;
		
		// x = x + t*v
		Vector.addInclusive(x[m], Vector.mult(v[m], t));
		
		// r = r - t*u
		Vector.subtractInclusive(r[m], Vector.mult(u, t));
		
		w[m] = Ci[m].mult(r[m]);    // w = inv(C)*r
		double beta = Vector.dot(w[m],w[m]);	//beta = dot(wm,wm)
		norm = beta;
		if (beta<tolerance)
		{
		    break;
		}
		
		double s  = beta/alpha[m];
		// v = trans(inv(C))*w + s*v
		v[m] = Vector.add( Cit[m].mult(w[m]), Vector.mult(v[m], s));
		alpha[m] = beta;
		
		/* check convergence */
		if (it % 25 == 0)
		{
		    System.out.printf(" alpha = ", alpha[m]);
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
