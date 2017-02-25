/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.solver;

import starfish.core.solver.Solver.Gradient;

/*this class handles basic multi-column coefficient matrix
 It's sort of like a sparse matrix with a fixed number of entries per line*/
public class Matrix 
{
    /*variables*/
    protected double value[][];		/*data held by the matrix*/
    protected int jx[][];		/*j index to full matrix*/	
    public final int nr;		/*number of rows and values in each row*/
    public final int nv;			/*maximum number of values in each row*/
  	
    /**sparse matrix constructor */
    public Matrix(int nr, int n_values)
    {
	this.nr = nr;
	this.nv = n_values;
	
	value=new double[nr][nv];
	jx=new int[nr][nv];
	
	/*initialize columns to -1*/
	for (int i=0;i<nr;i++)
	    for (int v=0;v<nv;v++)
	    {
		jx[i][v]=-1;
	    }
    }

    /**full matrix constructor*/
    public Matrix(int ni)
    {
	this(ni,ni);
	
	/*init full matrix*/
	for (int i=0;i<nr;i++)
	    clear(i);
    }

    /**copy constructor */
    public static Matrix copy(Matrix A)
    {
	Matrix C = new Matrix(A.nr, A.nv);
	C.value = new double[A.nr][A.nv];
	
	
	for (int i=0;i<A.nr;i++)
	    for (int v=0;v<A.nv;v++)
	    {
		C.value[i][v] = A.value[i][v];
		C.jx[i][v] = A.jx[i][v];
	    } 
	return C;
    }

    /**returns the value held by full matrix at row i and column j*/
    public double val(int i, int j)
    {
	for (int v=0;v<nv;v++)
	    if (jx[i][v]==j)
		return value[i][v];

	return 0;
    }

    /**clears (sets to zero) a single row*/
    public void clear(int r)
    {
	for (int v=0;v<nv;v++)
	{
	    jx[r][v]=-1;
	    value[r][v]=0;
	}
    }

    /**sets value at row r, column c in full matrix*/
    public void set(int r, int c, double val)
    {
	int v=0;
	while (v<nv)
	{
	    if (jx[r][v]==c)
	    {
		value[r][v]=val;
		return;
	    }
	    else if (jx[r][v]<0)
	    {
		/*this column does not yet exist*/
		value[r][v]=val;
		jx[r][v]=c;		
	    }
	    else v++;
	}
	
	throw new IndexOutOfBoundsException();
    }

    /**add value to row r, column c in full matrix*/
    public void add(int r, int c, double val)
    {
	for (int v=0;v<nv;v++)
	    if (jx[r][v]==c)
	    {
		value[r][v]+=val;
		return;
	    }
	    else if (jx[r][v]<0)
	    {
		value[r][v] = val;
		jx[r][v] = c;
		return;
	    }
	   
	throw new IndexOutOfBoundsException();
    }

    /** copies single row between matrixes*/
    public void copyRow(Matrix A, int row)
    {
	assert (A.nv<=nv);
	for (int v=0;v<nv&&A.jx[row][v]>=0;v++)
	    set(row,A.jx[row][v],A.value[row][v]);
    }
    
    /**add value to row r, column c in full matrix*/
    public void subtract(int i, int j,  double val)
    {
	add(i,j,-val);
    }
    
    /**returns A-B, for now defined only for identical matrices (nv is equal)*/
    public Matrix subtract(Matrix B)
    {
	assert(nr==B.nr);
	assert(nv==B.nv);
	
	Matrix R = new Matrix(nr, nv);
		
	for (int i=0;i<nr;i++)
	    for (int v=0;v<nv;v++)
	    {
		R.value[i][v] = value[i][v]-B.value[i][v];
		R.jx[i][v] = jx[i][v];
	    } 
	return R;
    }


    /**returns A-diag(B), for now defined only for identical matrices (nv is equal)*/
    public Matrix subtractDiag(double b[])
    {
	assert(nr==b.length);
	
	Matrix R = Matrix.copy(this);
	
	for (int i=0;i<nr;i++)
	    R.set(i, i, val(i,i)-b[i]);
		 
	return R;
    }


    /** performs matrix matrix multiplication
     * 
     * @param A matrix to multiply by
     * @return R=M*A
     */
    public Matrix mult(Matrix A)
    {
	Matrix R = new Matrix(nr,nv);
	
	/*simple algorithm, not optimized in any way*/
	for (int ri=0;ri<nr;ri++)
	    for (int rj=0;rj<nr;rj++)
	    {
		/*multiply rj in M by ci in A*/
		double prod=0;
		for (int k=0;k<nr;k++)
		{
		    prod += val(ri,k)*A.val(k,rj);
		}
		R.set(ri, rj, prod);
	    }
	   	
	return R;
    }

      /**performs matrix vector multiplication
    @return A*x*/
    public double[] mult(double x[])
    {
	double b[] = new double[this.nr];
	
	for (int r=0;r<nr;r++)
	{
	    double prod=0;
	    for (int v=0;v<nv && jx[r][v]>=0;v++)
	        prod+=value[r][v]*x[jx[r][v]];
	    b[r] = prod;	
	}
    	return b;
    }
    
    /**performs matrix vector multiplication and stores it in result vector
    @return A*x*/
    public void mult(double x[], double result[])
    {
	for (int r=0;r<nr;r++)
	{
	    double prod=0;
	    for (int v=0;v<nv && jx[r][v]>=0;v++)
	        prod+=value[r][v]*x[jx[r][v]];
	    result[r] = prod;	
	}    	
    }

    /**multiplies value held in row r, column c, by value*/
    public void mult(int i, int j,  double val)
    {
	for (int v=0;v<nv;v++)
	    if (jx[i][v]==j)
	    {
		value[i][v]*=val;
		return;
	    }

	throw new IndexOutOfBoundsException();
    }

    /**multiplies entire row by val*/
    public void multRow(int row, double val)
    {
	int v=0;
	while (v<nv && jx[row][v]>=0)
	    value[row][v++]*=val;
    }

    /**multiplies one row of the matrix by a vector but
     * excludes the diagonal element  */
    public double multRowNonDiag(double x[], int i)
    {
	double prod=0;
	int v=0;
	while (v<nv)
	{
	    int cj = jx[i][v];
	    if (cj<0) break;		/*no more data*/

	    if (cj!=i)
		prod+=value[i][v]*x[cj];
	    v++;
	}
	
	return prod;
    }
	
    /**returns the identity matrix of size of A*/
    public Matrix identity()
    {
	Matrix I = new Matrix(nr,1);		/*diagonal matrix*/
        
	for (int i=0;i<nr;i++)
	    I.set(i, i, 1);

	return I;
    }

    /**returns a new matrix which is the diagonal of the specified one*/
    public Matrix diag_matrix()
    {
	Matrix D = new Matrix(nr,1);		/*diagonal matrix*/

	for (int i=0;i<D.nr;i++)
	    D.set(i, i, val(i,i));

	return D;
    }

    /**returns a vector containing the diagonal*/
    public double[]diag(Matrix A)
    {
	double D[]= new double[A.nr];		/*diagonal matrix*/

	for (int i=0;i<A.nr;i++)
	    D[i]=A.val(i,i);

	return D;
    }

    /**returns the inverse
     * NOTE: this is currently defined only for diagonal matrix!
     */
    public Matrix inverse()
    {
	assert(nv==1);		/*diagonal matrix*/

	Matrix I = new Matrix(nr,1);		/*diagonal matrix*/

	for (int i=0;i<I.nr;i++)
	    I.set(i,i, 1.0/val(i,i));	/*inverse of a diagonal is 1/D[i,i]*/

	return I;
    }

    /**returns the transpose
     */
    public Matrix transpose()
    {
	Matrix T = new Matrix(nr,nv);		/*diagonal matrix*/

	for (int i=0;i<nr;i++)
	    for (int v=0;v<nv;v++)
		T.set(i, jx[i][v],val(jx[i][v],i));	/*TODO: check this!*/

	return T;		
    }
	
 

    /**prints self*/
    public void print()
    {
	for (int i=0;i<nr;i++)
	{
	    for (int j=0;j<nr;j++)
	    {
		System.out.printf("%8.2g", val(i,j));
	    }
	    System.out.println();
	}
	System.out.println();
    }	
    
    /**creates 2D transformation matrix
     * M = T*R*S 
     */
    public static Matrix makeTransformationMatrix(double scaling[], double theta, double translation[])
    {
	/*convert to radians*/
	theta *= Math.PI/180.0;
	
	/*rotation matrix
	 * 
	 * R = [C -S  0]
	 *     [S  C  0]
	 *     [0  0  1]
	 */
	Matrix R = new Matrix(3,3);
	R.set(0, 0, Math.cos(theta));
	R.set(1, 0, -Math.sin(theta));
	R.set(0, 1, Math.sin(theta));
	R.set(1, 1, Math.cos(theta));
	R.set(2, 2, 1);
	
	/*scaling matrix
	 *
	 * S = [sx 0  0]
	 *     [0  sy 0]
	 *     [0  0  1]
	 */
	Matrix S = new Matrix(3,3);
	S.set(0,0,scaling[0]);
	S.set(1,1,scaling[1]);
	S.set(2,2,1);
	
	/*translation matrix
	 *
	 * T = [1 0 vx]
	 *     [0 1 vy]
	 *     [0 0  0]
	 */
	Matrix T = new Matrix(3,3);
	T.set(0, 0, 1);
	T.set(0, 2, translation[0]);
	T.set(1, 1, 1);
	T.set(1, 2, translation[1]);
	T.set(2, 2, 1);

	/*apply transformation, rotation->scaling->translation*/
	return T.mult(R.mult(S));
    }

    void println(int u, int nj)
    {
	boolean output = false;
	for (int v=0;v<nv && jx[u][v]>=0;v++)
	{
	    int i = jx[u][v] / nj;
	    int j = jx[u][v] % nj;
		System.out.printf("[%d,%d]*%g ",i,j,value[u][v]);
	    output=true;
	}
	if (output) System.out.println();
    }
}
