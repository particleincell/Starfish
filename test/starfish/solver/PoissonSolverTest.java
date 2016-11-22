package starfish.solver;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lbrieda
 */
public class PoissonSolverTest {
	
	public PoissonSolverTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}

	/**
	 * Test of solvePotential method, of class PoissonSolver.
	 */
	@Test
	public void testSolvePotential() 
	{
		System.out.println("solvePotential");
		PoissonSolver ps = new PoissonSolver();
		int expResult = 0;
	
		/*!!!!!!!!!!!!!!HACK!!!!!!!!!!!!!!!!*/
		final double C=-Constants.Q/Constants.EPS0*1.6623e-4*1.6623e-4;
				
		/*testing*/	
		this.den0=1e16;
		this.phi0=0;
		this.kTe=5;
	
		int nr=7;
		M=new Matrix(nr,3);
		fixed_node = new boolean[nr];
		b0=new double[nr];
	
		M.set(0, 0, 1);
		b0[0]=-5;
		fixed_node[0]=true;
	
		M.set(nr-1, nr-1, 1);
		b0[nr-1]=5;
		fixed_node[nr-1]=true;

		for (int r=1;r<nr-1;r++)
		{
			M.set(r, r-1, 1);
			M.set(r, r, -2);
			M.set(r, r+1, 1);
			fixed_node[r]=false;
			b0[r]=C*den0;
		}
	
		phi1D=new double[nr];
		
		/*call linear solver*/
		solveNonLinearNR(this.M, phi1D, b0, pot_boltzmann);
		//solveLinearGS(this.M,phi1D,b0);

	}
}
