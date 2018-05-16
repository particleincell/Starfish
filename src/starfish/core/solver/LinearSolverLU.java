/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.solver;

import starfish.core.common.Starfish;
import starfish.core.domain.FieldCollection2D;
import starfish.core.solver.Solver.LinearSolver;

public class LinearSolverLU implements LinearSolver
{

    @Override
    public int solve(Solver.MeshData[] mesh_data, FieldCollection2D fc, int max_it, double tolerance)
    {
  	for (Solver.MeshData md:mesh_data)
	{
	    if (md.L==null || md.U==null)
	    {
		try {
		Matrix ret[] = md.A.decomposeLU();
		md.L = ret[0]; md.U = ret[1];
		}
		catch (UnsupportedOperationException e)
		{
		    Starfish.Log.error("LU decomposition failed");
		}
	    }
	    
	    double b[] = md.b;
	    double x[] = md.x;
	    Matrix L = md.L;
	    Matrix U = md.U;
	    
	    //first solve Ly=b using forward subsitution
	    int n = md.A.nr;
	    double y[] = new double[n];
	    y[0] = b[0] / L.get(0,0);
	    for (int i=1;i<n;i++)
	    {
		double s = b[i];
		for (int j=0;j<i;j++)
		    s -= L.get(i,j) * y[j];
		y[i] = s/L.get(i, i);		
	    }	   
	    
	    //now solve Ux=y
	    x[n-1] = y[n-1]/U.get(n-1,n-1);
	    for (int i=n-2;i>=0;i--)
	    {
		double s = y[i];
		for (int j=i+1;j<n;j++)
		    s -= U.get(i,j) * x[j];
		x[i] = s/U.get(i,i);
	    }
	}
	
	return 0;
    }

    
}
