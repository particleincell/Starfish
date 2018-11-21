/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * implements the ADI method
 * multidomain not currently supported
 */
package starfish.core.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.FieldCollection2D;
import starfish.core.solver.Solver.LinearSolver;
import starfish.core.solver.Solver.MeshData;

/**
 *
 * @author Lubos Brieda
 */
public class LinearSolverADI implements LinearSolver
{
    class ParLinearGS<K> implements Callable
    {
	int i_min,i_max;
	Matrix A;
	double x[],b[];
	double Ax_neigh[];
	protected int id;
	
	ParLinearGS(int id, int i_min, int i_max, MeshData md)
	{
	    this.id=id;
	    this.A=md.A;
	    this.x=md.x;
	    this.b=md.b;
	    this.Ax_neigh = md.Ax_neigh;
	    this.i_min=i_min;
	    this.i_max=i_max<A.nr?i_max:A.nr;
	}
	
	@Override
	public String call() throws Exception
	{
	    for (int u=i_min;u<i_max;u++)
	    {		
		/* tau = [A-D]x */
		double tau = A.multRowNonDiag(x, u);
		double g = (b[u] - Ax_neigh[u] - tau) / A.get(u,u);

		 x[u] = x[u] + 1.4*(g-x[u]); /*SOR*/
	    }
	    
	    return "OK";
	}
	
    }

    /**
     * solves Ax=b for x using the ADI method
     * multidomain support not implemented since not really clear where to add the neighbor data
     * 
     * Given a 5 point stencil, we can rewrite a A*phi=b as
     * X*phi + Y*phi = b 
     * where X is d^2/dx^2 
     * Then, using some initial guess for phi, we can write
     * Y*phi1 = b-X*phi0 
     * which can be solved with a tri-diag algorithm
     * We then use this solution into
     * X*phi2 = b-Y*phi1
     * and so in. This is iterated until convergence
     * 
     * @param mesh_data
     * @return 
     */
    public int solve(MeshData mesh_data[], FieldCollection2D fc, int max_it, double tolerance)
    {
	if (mesh_data.length>1)
	    Log.error("ADI multi-domain support not yet implemented");
	
	MeshData md = mesh_data[0];
	
	/* construct the X and Y vectors*/
	int nu = md.x.length;
	int ni = md.mesh.ni;
	double x[] = md.x;
	double Xa[] = new double[nu];
	double Xb[] = new double[nu];
	double Xc[] = new double[nu];
	double Ya[] = new double[nu];
	double Yb[] = new double[nu];
	double Yc[] = new double[nu];
	double d[] = new double[nu];
	
	/* SOLVER */
	int it = 1;			/*start with one so we don't compute residue on first run*/
	double norm=1e66;
	while (it <= max_it)
	{
	    /*** update boundaries**/	    
	    Solver.updateGhostVector(mesh_data, fc);

	    //reset diagonals
	    for (int u=0;u<nu;u++)
	    {
		Xb[u] = md.A.get(u, u);
		Yb[u] = Xb[u];
		d[u] = md.b[u];

		if (u>=1)
		    Xa[u] = md.A.get(u,u-1);
		if (u<nu-1)
		    Xc[u] = md.A.get(u,u+1);
		if (u>=ni)
		    Ya[u] = md.A.get(u,u-ni);
		if (u<nu-ni)
		    Yc[u] = md.A.get(u,u+ni);	    
	    }
	
	    //compute RHS = b-X*phi or b-Y*phi
	    if (it%2==0)
	    {
		//RHS = b-X*phi
		for (int u=0;u<nu;u++)
		    d[u] = md.b[u] - Xa[u]*(u>0?x[u-1]:0) 
			    -Xb[u]*x[u] - Xc[u]*(u<nu-1?x[u+1]:0);
		
		//solve Y*phi = RHS with Thomas alg
		Yc[0] = Yc[0]/Yb[0];
		d[0] = d[0]/Yb[0];
		for (int u=1;u<nu;u++)
		{
		    Yc[u] = Yc[u]/(Yb[u]-Ya[u]*Yc[u-1]);
		    d[u] = (d[u]-Ya[u]*d[u-1])/(Yb[u]-Ya[u]*Yc[u-1]);
		}
		    
		x[nu-1] = d[nu-1];
		for (int u=nu-2;u>=0;u--)
		    x[u] = d[u] - Yc[u]*x[u+1];
	    }
	    else
	    {
		//RHS = b-Y*phi
		for (int u=0;u<nu;u++)
		    d[u] = md.b[u] - Ya[u]*(u>ni-1?x[u-ni]:0) 
			    -Yb[u]*x[u] - Yc[u]*(u<nu-ni?x[u+ni]:0);	
		
		//solve X*phi = RHS with Thomas alg
		Xc[0] = Xc[0]/Xb[0];
		d[0] = d[0]/Xb[0];
		for (int u=1;u<nu;u++)
		{
		    Xc[u] = Xc[u]/(Xb[u]-Xa[u]*Xc[u-1]);
		    d[u] = (d[u]-Xa[u]*d[u-1])/(Xb[u]-Xa[u]*Yc[u-1]);
		}
		    
		x[nu-1] = d[nu-1];
		for (int u=nu-2;u>=0;u--)
		    x[u] = d[u] - Xc[u]*x[u+1];
	    }
	    
	    
	    /* check convergence */
	    if (it % 25 == 0)
	    {
		norm=0;
		int nn=0;
		
		norm += Solver.calculateResidue(md.A, md.Ax_neigh, md.x, md.b);
		nn += md.x.length;
		
		norm/=nn;
		
		//System.out.println(norm);
		if (norm < tolerance)
		{
		    Log.debug(String.format("ADI converged in %d iterations with norm=%g",it,norm));
		    break;
		}
	    }
		    
	    it++;
	}
 	it--;
	if (it >= max_it)
	{
	    Starfish.Log.warning(" !! ADI failed to converge in " + it + " iteration, norm = " + norm);
	}

	return it;
    }

    
}
