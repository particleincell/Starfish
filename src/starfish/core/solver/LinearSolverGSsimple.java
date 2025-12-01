/*
 * (c) 2012-2019 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.solver;

import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Vec;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.UniformMesh;
import starfish.core.solver.Solver.LinearSolver;
import starfish.core.solver.Solver.MeshData;

/**
 * This a simple "direct" implementation of the Gauss Seidel solver with inline Boltzmann term, mainly for testing
 * It operates directly on 2d data sets to avoid errors introduced by flattening
 * Should be run with "linear=true" to skip the NR solver part
 * @author Lubos Brieda
 */
public class LinearSolverGSsimple implements LinearSolver
{
    @Override
    public int solve(MeshData[] mesh_data, FieldCollection2D fc, int max_it, double tolerance)
    {
	/*this works only on the first mesh*/
	MeshData md = mesh_data[0];
	Mesh mesh = md.mesh;
	
	/*this only works for uniform mesh*/
	UniformMesh umesh = (UniformMesh) mesh;
	
	double idx2 = 1.0/(umesh.dh[0]*umesh.dh[0]);
	double idy2 = 1.0/(umesh.dh[1]*umesh.dh[1]);
	
	double phi[][] = Starfish.domain_module.getPhi(mesh).data;
	double rho[][] = Starfish.domain_module.getRho(mesh).data;
	
	int solver_it; 
	boolean converged = false;
	
	/*these are hardcoded since LinearSolver doesn't have access to Solver*/
	double n0 = 1e12;
	double phi0 = 0;
	double kTe0 = 1.5;
	
	for (solver_it = 0; solver_it<max_it; solver_it++)
	{
	    for (int i=0;i<mesh.ni;i++)
		for (int j=0;j<mesh.nj;j++)
		{
		    if (mesh.isDirichletNode(i, j)) 
		    {
			phi[i][j] = mesh.getNode(i, j).bc_value;
			continue;			
		    }
		    
		    /*else assume neumann boundaries*/
		    if (i==0) phi[i][j] = phi[i+1][j];
		    else if (i==mesh.ni-1) phi[i][j] = phi[i-1][j];
		    else if (j==0) phi[i][j] = phi[i][j+1];
		    else if (j==mesh.nj-1) phi[i][j] = phi[i][j-1];
		    else
		    {
			double b = rho[i][j] - Constants.QE*n0*Math.exp((phi[i][j]-phi0)/kTe0);
			
			double phi_new = (b/Constants.EPS0 + 
					idx2*(phi[i-1][j] + phi[i+1][j]) +
					idy2*(phi[i][j-1] + phi[i][j+1])) / (2*(idx2+idy2));
                
            		/*SOR*/
			phi[i][j] = phi[i][j] + 1.4*(phi_new-phi[i][j]);
		    }  
		}

	    //convergence check
	    if (solver_it%25==0)
	    {
		double sum = 0;

		for (int i=0;i<mesh.ni;i++)
		    for (int j=0;j<mesh.nj;j++)				
		    {
			if (mesh.isDirichletNode(i, j)) continue;
			 
			if (i==0) phi[i][j] = phi[i+1][j];
			else if (i==mesh.ni-1) phi[i][j] = phi[i-1][j];
			else if (j==0) phi[i][j] = phi[i][j+1];
			else if (j==mesh.nj-1) phi[i][j] = phi[i][j-1];
			else			
			{
			    double b = rho[i][j] - Constants.QE*n0*Math.exp((phi[i][j]-phi0)/kTe0);			
			    double R = b/Constants.EPS0 + 
					idx2*(phi[i-1][j]-2*phi[i][j]+phi[i+1][j])+
					idy2*(phi[i][j-1]-2*phi[i][j]+phi[i][j+1]);

			    sum += R*R;
			}
		    }

		double L2 = Math.sqrt(sum/(mesh.ni*mesh.nj));
		if (L2<tolerance) {converged=true;break;}
		//else System.out.printf("L2 = %g\n",L2);
	    }	/*convergence check*/
	}
	
	/*the calling function assumes the solution is in md.x so pack it*/
	md.x = Vec.deflate(phi);

	return converged?solver_it:-1;
    }
    
}
