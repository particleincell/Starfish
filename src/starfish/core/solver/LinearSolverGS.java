/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
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
public class LinearSolverGS implements LinearSolver
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
     * solves Ax=b for x using the GS method
     * @param mesh_data
     * @return 
     */
    public int solve(MeshData mesh_data[], FieldCollection2D fc, int max_it, double tolerance)
    {
	/*create threads*/
	int np = Starfish.getNumProcessors();
	
	/*total number of unknowns*/
	int nu_total = 0;
	for (MeshData md:mesh_data)
	    nu_total+=md.x.length;

	Collection<Callable<String>> workers = new ArrayList<Callable<String>>();
	ExecutorService executor = Executors.newFixedThreadPool(np);
	
	for (MeshData md:mesh_data)
	{
	    int nu = md.x.length;
	    
	    /*number of nodes in each chunk*/
	    int chunk_size = (int)((double)nu_total/np)+1;
	    
	    for (int i1=0, id=0;i1<nu;i1+=chunk_size)
	    {
		/*avoid creating new threads for pieces with less than 100 unknowns*/
		if ((nu-(i1+chunk_size))<100) chunk_size = nu-i1;		
	    	workers.add(new ParLinearGS(id,i1,i1+chunk_size,md));
	    }
	}
	
	/* SOLVER */
	int it = 1;			/*start with one so we don't compute residue on first run*/
	double norm=1e66;
	while (it <= max_it)
	{
	    /*** update boundaries**/	    
	    Solver.updateGhostVector(mesh_data, fc);
	    
	    /* check convergence */
	    if (it % 25 == 0)
	    {
		norm=0;
		int nn=0;
		
		for (MeshData md:mesh_data)
		{
		    norm += Solver.calculateResidue(md.A, md.Ax_neigh, md.x, md.b);
		    nn += md.x.length;
		}
		
		norm/=nn;
		
		//System.out.println(norm);
		if (norm < tolerance)
		{
		    Log.debug(String.format("GS converged in %d iterations with norm=%g",it,norm));
		    break;
		}
	    }
		    
	    try
	    {
		executor.invokeAll(workers);
	    } catch (InterruptedException ex)
	    {
		Logger.getLogger(Solver.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    it++;
	}
 	it--;
	if (it >= max_it)
	{
	    Starfish.Log.warning(" !! GS failed to converge in " + it + " iteration, norm = " + norm);
	}

	executor.shutdown();
	
	/*hack for testing*/
	
	return it;
    }

    
}
