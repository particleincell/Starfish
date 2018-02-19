/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

/* QuadrilateralMesh
 * 
 * Topologically structured mesh (ni x nj) with quadrilateral cells
 */
package starfish.core.domain;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;

/**
 *
 * @author Lubos Brieda
 */
public class QuadrilateralMesh extends Mesh
{
    /*variables*/

    /**
     *
     */

    protected double IPOS[][];

    /**
     *
     */
    protected double JPOS[][];
	
    /*coefficients*/
    double alpha[][][];
    double beta[][][];
	
    /**
     *
     * @return
     */
    public double[][] getIPos() {return IPOS;}

    /**
     *
     * @return
     */
    public double[][] getJPos() {return JPOS;}

    /*methods*/

    /**
     *
     * @param ni
     * @param nj
     * @param ipos
     * @param jpos
     * @param domain_type
     */

    public QuadrilateralMesh (int ni, int nj, double ipos[][], double jpos[][], DomainType domain_type)
    {
	super(ni,nj, domain_type);  
	  
	setPos(ipos,jpos);
    }
	
    /**
     *
     * @param ni
     * @param nj
     * @param domain_type
     */
    public QuadrilateralMesh (int ni, int nj, DomainType domain_type)
    {
	super(ni,nj,domain_type);
    }
  
    /**
     *
     * @param ipos
     * @param jpos
     */
    public final void setPos(double ipos[][], double jpos[][])
    {
	/*make sure we have the right size*/
	if (ipos.length != ni || jpos.length!=ni ||
	    ipos[0].length !=nj || jpos[0].length !=nj)
	    throw new UnsupportedOperationException("wrong data size");
	  
	/*make a copy of data*/
	IPOS = new double[ni][nj];
	JPOS = new double[ni][nj];
	for (int i=0;i<ni;i++)
	{
	    System.arraycopy(ipos[i],0, IPOS[i], 0, nj);
	    System.arraycopy(jpos[i],0, JPOS[i], 0, nj);
	}

	/*allocate memory for coefficients*/
	alpha = new double[ni-1][nj-1][4];
	beta = new double[ni-1][nj-1][4];

	/*compute coefficients*/
	for (int i=0;i<ni-1;i++)
	    for (int j=0;j<nj-1;j++)
		ComputeCoeffs(i,j);
		
	/*call init on parent mesh to recompute node volumes, etc...*/
	//init();
    }
    
    /**computes alphas and betas for interpolation, see
    * http://www.particleincell.com/2012/quad-interpolation/
     * @param i
     * @param j
    */
    protected final void ComputeCoeffs(int i, int j)
    {
	/*vertices*/
	double x[] = new double[4];
	double y[] = new double[4];
		
	x[0] = IPOS[i][j];
	x[1] = IPOS[i+1][j];
	x[2] = IPOS[i+1][j+1];
	x[3] = IPOS[i][j+1];
		
	y[0] = JPOS[i][j];
	y[1] = JPOS[i+1][j];
	y[2] = JPOS[i+1][j+1];
	y[3] = JPOS[i][j+1];
		
	/*compute coeffs, this is alpha = M*x, where
	* M=[1 0 0 0; -1 1 0 0; -1 0 0 1; 1 -1 1 -1]
	* corresponds to the inverse of logical coordinates on the vertices*/
	double a[] = alpha[i][j];
	double b[] = beta[i][j];
		
	a[0] = x[0];
	a[1] = -x[0]+x[1];
	a[2] = -x[0]+x[3];
	a[3] = x[0]-x[1]+x[2]-x[3];
		
	b[0] = y[0];
	b[1] = -y[0]+y[1];
	b[2] = -y[0]+y[3];
	b[3] = y[0]-y[1]+y[2]-y[3];
    }
    
    @Override
    /*returns position*/
    public double[] pos(double fi, double fj)
    {
	int i = (int)fi;
	int j = (int)fj;
		
	double l=fi-i;
	double m=fj-j;
		
	if (fi>=ni-1) {i=ni-2;l=1;}
	if (fj>=nj-1) {j=nj-2;m=1;}
			
	double x[]=new double[2];
	double a[] = alpha[i][j];
	double b[] = beta[i][j];
		
	x[0] = a[0] + a[1]*l + a[2]*m + a[3]*l*m;
	x[1] = b[0] + b[1]*l + b[2]*m + b[3]*l*m;
		
	return x;
    }

    /*visited array*/

    /**
     *
     */

    protected boolean visited[][];
			
    /**
     *
     * @param xi
     * @param xj
     * @return
     */
    @Override
    public double[] XtoL(double xi, double xj)
    {
	int i = (int)(ni/2.0);
	int j = (int)(nj/2.0);
	return XtoL(xi,xj,i,j);
    }
	
    /**starts searching in specified cel
     * @param xi
     * @param xjl
     * @param i
     * @param j
     * @return */
    public double[] XtoL(double xi, double xj, int i, int j)
    {
	/*start searching in the center
	* TODO: implement quads of bounding boxes*/
	
	/*allocated visited array*/
	visited = new boolean[ni-1][nj-1];

	double lc[] = new double[2];
	lc[0]=-1;
	lc[1]=-1;
		
	/*start searching*/
	XtoLrecursive(xi,xj,i,j,lc);
		
	/*TODO: Had to add this rigorous search since the code was
	* not finding points along centerpole of CHT, fix */
	/*do a rigorous search*/
	
	if (lc[0]<0)
	{
	    for (i=0;i<ni-1;i++)
		for (j=0;j<nj-1;j++)
		    if (!visited[i][j])
		    {
			if (XtoLrecursive(xi,xj,i,j,lc))
			    return lc;
		    }
	}
	
	return lc;
    }
	
    /*recursively searches for cell containinging (xi,xj)*/

    /**
     *
     * @param xi
     * @param xj
     * @param i
     * @param j
     * @param lc
     * @return
     */

    protected boolean XtoLrecursive(double xi, double xj, int i, int j, double lc[])
    {
	visited[i][j] = true;
		
	double a[] = alpha[i][j];
	double b[] = beta[i][j];
		
	/*quadratic equation coeffs, aa*mm^2+bb*m+cc=0*/
	double aa = a[3]*b[2] - a[2]*b[3];
	double bb = a[3]*b[0] -a[0]*b[3] + a[1]*b[2] - a[2]*b[1] + xi*b[3] - xj*a[3];
	double cc = a[1]*b[0] -a[0]*b[1] + xi*b[1] - xj*a[1];

	if (Math.abs(aa)<1e-16) aa=0;	//take care of numerical errors
	
	/*compute m = (-b+sqrt(b^2-4ac))/(2a)*/
	double det = bb*bb - 4*aa*cc;
	if (det<0 && det>-1e-7) det=0;
		
	/*this should never happen*/
	if (det<0)
	    Log.error("QuadrilateralMesh: Det < TOL, "+det);
	    
	double m;
	/*zero aa means we have orthogonal mesh and a linear equation to solve*/
	if (aa!=0)
	    m = (-bb+Math.sqrt(det))/(2*aa);
	else
	    m = -cc/bb;

	/*is m in range?*/
	if (m<0 && j>0 && !visited[i][j-1]) {
	    if (XtoLrecursive(xi,xj,i,j-1,lc)) 
		return true;
	}
	else if (m>1.0 && j<nj-2 && !visited[i][j+1]) {
	    if (XtoLrecursive(xi,xj,i,j+1,lc)) 
		return true;
	}

	//we now have the correct m so next check if l is in rage
	
	/*compute l*/
	double ln = xi-a[0]-a[2]*Math.abs(m);
	double ld = a[1]+a[3]*Math.abs(m); 
	double l;
		
	if (ld!=0) l=ln/ld;
	else 
	{
	    /*recompute by inverting order (l first, m second)*/
	    aa = a[3]*b[1]-a[1]*b[3];
	    bb = a[3]*b[0]-a[0]*b[3] + a[2]*b[1]-a[1]*b[2] - a[3]*xj  + b[3]*xi;
	    cc = a[2]*b[0]-a[0]*b[2]-a[2]*xj+b[2]*xi;

	    /*compute m = (-b+sqrt(b^2-4ac))/(2a)*/
	    det = bb*bb - 4*aa*cc;
	    if (det<0 && det>-1e-7) det=0;

	    /*this should never happen*/
	    if (det<0)
		Log.error("QuadrilateralMesh: Det < TOL, "+det);

	    /*check for linear case*/
	    if (aa!=0)
		l = (-bb-Math.sqrt(det))/(2*aa);
	    else
		l = -cc/bb;

	    /*compute m*/
	    ln = xi-a[0]-a[1]*Math.abs(l);
	    ld = a[2]+a[3]*Math.abs(l); 
	    m=ln/ld;
			
	    /*is m in range?*/
	    if (m<0 && j>0 && !visited[i][j-1]) {
		if (XtoLrecursive(xi,xj,i,j-1,lc)) 
		    return true;
	    }
	    else if (m>1.0 && j<nj-2 && !visited[i][j+1]) {
		if (XtoLrecursive(xi,xj,i,j+1,lc)) 
		    return true;
	    }
	}
			
	if (l<0 && i>0 && !visited[i-1][j]) {
	    if (XtoLrecursive(xi,xj,i-1,j,lc)) 
		return true;
	}
	else if (l>1.0 && i<ni-2 && !visited[i+1][j]) {
	    if (XtoLrecursive(xi,xj,i+1,j,lc)) 
		return true;
	}
	
	if (l>=0 && m>=0 && l<=1.0000001 && m<=1.0000001)
	{
	    /*found*/
	    lc[0]=i+l;
	    lc[1]=j+m;
	    return true;
	}
	else
	{
	    /*not found*/
	    return false;
	}
    }

    @Override
    public boolean containsPosStrict(double x[]) 
    {
	double lc[] = XtoL(x);
	if (lc[0]<0 || lc[1]<0 || lc[0]>=ni-1 || lc[1]>=nj-1) 
		return false;
	return true;
    }

    @Override
    public double[] boundaryNormal(Face face, double[] pos)
    {
	throw new UnsupportedOperationException("Not supported yet."); 
    }

}
