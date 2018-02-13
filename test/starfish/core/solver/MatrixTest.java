/*
sets up a dummy sparse 4x4 matrix multiplication

[-1  1  0  0]
[ 1 -2  1  0]
[ 0  1 -2  1]
[ 0  0  1 -1]



 */
package starfish.core.solver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Lubos Brieda
 */
public class MatrixTest
{
    
    public MatrixTest()
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
     * Test of copy method, of class Matrix.
     */
    @Test
    public void testCopy()
    {
	System.out.println("copy");
	Matrix A = null;
	Matrix expResult = null;
	Matrix result = Matrix.copy(A);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of val method, of class Matrix.
     */
    @Test
    public void testVal()
    {
	System.out.println("val");
	int i = 0;
	int j = 0;
	Matrix instance = null;
	double expResult = 0.0;
	double result = instance.val(i, j);
	assertEquals(expResult, result, 0.0);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of clear method, of class Matrix.
     */
    @Test
    public void testClear()
    {
	System.out.println("clear");
	int i = 0;
	int j = 0;
	Matrix instance = null;
	instance.clear(i, j);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of set method, of class Matrix.
     */
    @Test
    public void testSet()
    {
	System.out.println("set");
	int i = 0;
	int j = 0;
	double val = 0.0;
	Matrix instance = null;
	instance.set(i, j, val);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of add method, of class Matrix.
     */
    @Test
    public void testAdd()
    {
	System.out.println("add");
	int i = 0;
	int j = 0;
	int ci = 0;
	int cj = 0;
	double val = 0.0;
	Matrix instance = null;
	instance.add(i, j, ci, cj, val);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of subtract method, of class Matrix.
     */
    @Test
    public void testSubtract_5args()
    {
	System.out.println("subtract");
	int i = 0;
	int j = 0;
	int ci = 0;
	int cj = 0;
	double val = 0.0;
	Matrix instance = null;
	instance.subtract(i, j, ci, cj, val);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of subtract method, of class Matrix.
     */
    @Test
    public void testSubtract_Matrix()
    {
	System.out.println("subtract");
	Matrix B = null;
	Matrix instance = null;
	Matrix expResult = null;
	Matrix result = instance.subtract(B);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of subtractDiag method, of class Matrix.
     */
    @Test
    public void testSubtractDiag()
    {
	System.out.println("subtractDiag");
	double[][] b = null;
	Matrix instance = null;
	Matrix expResult = null;
	Matrix result = instance.subtractDiag(b);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of mult method, of class Matrix.
     */
    @Test
    public void testMult_Matrix_doubleArrArr()
    {
	System.out.println("mult");
	Matrix A = null;
	double[][] x = null;
	double[][] expResult = null;
	double[][] result = Matrix.mult(A, x);
	assertArrayEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of mult method, of class Matrix.
     */
    @Test
    public void testMult_Matrix()
    {
	System.out.println("mult");
	Matrix A = null;
	Matrix instance = null;
	Matrix expResult = null;
	Matrix result = instance.mult(A);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of mult method, of class Matrix.
     */
    @Test
    public void testMult_doubleArr()
    {
	System.out.println("mult");
	double[] x = null;
	Matrix instance = null;
	double[] expResult = null;
	double[] result = instance.mult(x);
	assertArrayEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of multSingle method, of class Matrix.
     */
    @Test
    public void testMultSingle()
    {
	System.out.println("multSingle");
	int i = 0;
	int j = 0;
	int ci = 0;
	int cj = 0;
	double val = 0.0;
	Matrix instance = null;
	instance.multSingle(i, j, ci, cj, val);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of multRow method, of class Matrix.
     */
    @Test
    public void testMultRow()
    {
	System.out.println("multRow");
	int i = 0;
	int j = 0;
	double val = 0.0;
	Matrix instance = null;
	instance.multRow(i, j, val);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of multRowNonDiag method, of class Matrix.
     */
    @Test
    public void testMultRowNonDiag()
    {
	System.out.println("multRowNonDiag");
	Matrix A = null;
	double[][] x = null;
	int i = 0;
	int j = 0;
	double expResult = 0.0;
	double result = Matrix.multRowNonDiag(A, x, i, j);
	assertEquals(expResult, result, 0.0);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of identity method, of class Matrix.
     */
    @Test
    public void testIdentity()
    {
	System.out.println("identity");
	Matrix A = null;
	Matrix expResult = null;
	Matrix result = Matrix.identity(A);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of diag_matrix method, of class Matrix.
     */
    @Test
    public void testDiag_matrix()
    {
	System.out.println("diag_matrix");
	Matrix A = null;
	Matrix expResult = null;
	Matrix result = Matrix.diag_matrix(A);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of diag method, of class Matrix.
     */
    @Test
    public void testDiag()
    {
	System.out.println("diag");
	Matrix A = null;
	double[][] expResult = null;
	double[][] result = Matrix.diag(A);
	assertArrayEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of inverse method, of class Matrix.
     */
    @Test
    public void testInverse()
    {
	System.out.println("inverse");
	Matrix A = null;
	Matrix expResult = null;
	Matrix result = Matrix.inverse(A);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of transpose method, of class Matrix.
     */
    @Test
    public void testTranspose()
    {
	System.out.println("transpose");
	Matrix A = null;
	Matrix expResult = null;
	Matrix result = Matrix.transpose(A);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of addGradient method, of class Matrix.
     */
    @Test
    public void testAddGradient()
    {
	System.out.println("addGradient");
	int i = 0;
	int j = 0;
	Solver.Gradient G = null;
	Matrix instance = null;
	instance.addGradient(i, j, G);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of print method, of class Matrix.
     */
    @Test
    public void testPrint()
    {
	System.out.println("print");
	Matrix instance = null;
	instance.print();
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }

    /**
     * Test of makeTransformationMatrix method, of class Matrix.
     */
    @Test
    public void testMakeTransformationMatrix()
    {
	System.out.println("makeTransformationMatrix");
	double[] scaling = null;
	double theta = 0.0;
	double[] translation = null;
	Matrix expResult = null;
	Matrix result = Matrix.makeTransformationMatrix(scaling, theta, translation);
	assertEquals(expResult, result);
	// TODO review the generated test code and remove the default call to fail.
	fail("The test case is a prototype.");
    }
    
}
