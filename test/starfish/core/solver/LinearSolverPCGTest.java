/*
 * (c) 2012-2019 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.solver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import starfish.core.common.Vector;
import starfish.core.domain.FieldCollection2D;
import starfish.core.solver.Solver.MeshData;

/**
 *
 * @author lubos
 */
public class LinearSolverPCGTest
{
    
    public LinearSolverPCGTest()
    {
    }
    
    @BeforeClass
    public static void setUpClass()
    {
    }
    
    @AfterClass
    public static void tearDownClass()
    {
    }
    
    @Before
    public void setUp()
    {
    }
    
    @After
    public void tearDown()
    {
    }

    /**
     * Test of solve method, of class LinearSolverPCG.
     */
    @Test
    public void testSolve()
    {
	System.out.println("solve");
	LinearSolverPCG solver = new LinearSolverPCG();
	MeshData md[] = new MeshData[1];
	md[0] = new MeshData();
	
	double dx = 0.001;
	double dy = 0.001;
	
	int ni=10;
	int nj=10;
	int nu=ni*nj;
	
	Matrix A = new Matrix(nu);
	double b[] = new double[nu];
	
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		int u = j*ni+i;
		if (i==0)
		{
		    A.set(u,u,1);
		    b[u] = 1;
		}
		else if (j==0)
		{
		    A.set(u,u,1);
		    b[u] = 2;
		}
		else if (i==ni-1)
		{
		    A.set(u,u,1);
		    A.set(u,u-1,-1);
		    b[u] = 0;
		}
		else if (j==nj-1)
		{
		    A.set(u,u,1);
		    A.set(u,u-ni,-1);
		    b[u] = 0;
		}
		else 
		{
		    double idx2 = 1/(dx*dx);
		    double idy2 = 1/(dy*dy);
		    A.set(u,u,-2*idx2 -2*idy2);
		    A.set(u,u-1,idx2);
		    A.set(u,u-ni,idy2);
		    A.set(u,u+1,idx2);
		    A.set(u,u+ni,idy2);
		    b[u] = 0.01;
		}
	}
	    
	    
	System.out.println("solve2");

	md[0].A = A;
	md[0].b = b;
	md[0].x = Vector.copy(b);
	
	FieldCollection2D fc =null;
	
	int max_it = 100;
	double tol = 1e-4;
	
	solver.solve(md, fc, max_it, tol);
		

	int expResult = 0;
	//int result = instance.solve(md, fc, max_it, tolerance);
	
	//assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	
    }
    
}
