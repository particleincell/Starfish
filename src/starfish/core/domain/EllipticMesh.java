/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import starfish.core.boundaries.Spline;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;

/** Constructs a quadrilateral mesh bounded by four prescribed splines */
public class EllipticMesh extends QuadrilateralMesh
{
    protected Spline splines[]; 
    
    /**boundaries are given by 4 splines: 
    * splines[RIGHT]: left right
    * splines[TOP]: bottom top
    * splines[LEFT]: right left
    * splines[BOTTOM]: top bottom*/
    public EllipticMesh (int ni, int nj, Spline splines[], DomainType domain_type)
    {
	super(ni,nj,domain_type);
		
	int u,i,j;
	double pos[], t[];
	double gx[] = new double[ni*nj];
	double gy[] = new double[ni*nj];

	this.splines = splines;
	
	/*left boundary*/
	t = splines[Face.LEFT.value()].splitSpline(nj,false);
	for (j=0;j<nj;j++)
	{
	    u=j*ni;
	    pos = splines[Face.LEFT.value()].pos(t[j]);
	    gx[u]=pos[0];
	    gy[u]=pos[1];
	}
    
	/*right boundary*/
	t = splines[Face.RIGHT.value()].splitSpline(nj,true);
	for (j=0;j<nj;j++)
	{
	    u=j*ni+ni-1;
	    pos = splines[Face.RIGHT.value()].pos(t[j]);
	    gx[u]=pos[0];
	    gy[u]=pos[1];
	}
	
	/*bottom*/
	t = splines[Face.BOTTOM.value()].splitSpline(ni,true);
	for (i=0;i<ni;i++)	
	{
	    u=i;
	    pos = splines[Face.BOTTOM.value()].pos(t[i]);
	    gx[u]=pos[0];
	    gy[u]=pos[1];
	}
	
	/*top*/
	t = splines[Face.TOP.value()].splitSpline(ni,false);
	for (i=0;i<ni;i++)
	{
	    u=(nj-1)*ni+i;
	    pos = splines[Face.TOP.value()].pos(t[i]);
	    gx[u]=pos[0];
	    gy[u]=pos[1];
	}

	/*initial guess - straight lines*/
	for (j=1;j<nj-1;j++)
	    for (i=1;i<ni-1;i++)
	    {
		u=j*ni+i;
		gx[u] = gx[j*ni]+i/(double)(ni-1)*(gx[j*ni+ni-1]-gx[j*ni]);
		gy[u] = gy[i]+j/(double)(nj-1)*(gy[(nj-1)*ni+i]-gy[i]);
	    }
	
	/*call the solver*/
	EllipticSolver(gx,gy);

	/*unwrap*/
	double ipos[][] = new double[ni][nj];
	double jpos[][] = new double[ni][nj];
	for (j=0;j<nj;j++)
	    for (i=0;i<ni;i++)
	    {
		u=j*ni+i;
		ipos[i][j] = gx[u];
		jpos[i][j] = gy[u];
	    }

	/*init mesh with these positions*/
	setPos(ipos,jpos);		 
    }

    /*elliptic solver, based on implementation in
    http://perso.uclouvain.be/vincent.legat/teaching/documents/meca2170-jfr-cours4.pdf*/
    final void EllipticSolver(double gx[], double gy[])
    {
	double A,B,G;
	int it,j,i,u;
	
	/*hardcoded for 50 iterations, seems to be sufficient
	better option would be to add a convergence check*/	
	for (it=0;it<100;it++)
	{
	    /*loop only over internal nodes*/
	    for (j=1;j<nj-1;j++)
		for (i=1;i<ni-1;i++)
		{
		    u=j*ni+i;
		    A = ((gx[u+ni]-gx[u-ni])*(gx[u+ni]-gx[u-ni]) + 
				    (gy[u+ni]-gy[u-ni])*(gy[u+ni]-gy[u-ni]))/4;
		    G = ((gx[u+1]-gx[u-1])*(gx[u+1]-gx[u-1])+
				(gy[u+1]-gy[u-1])*(gy[u+1]-gy[u-1]))/4;
		    B =  ((gx[u+1]-gx[u-1])*(gx[u+ni]-gx[u-ni]))/16+
			       ((gy[u+1]-gy[u-1])*(gy[u+ni]-gy[u-ni]))/16; 

		    gx[u] = (A*(gx[u+1]+gx[u-1])+G*(gx[u+ni]+gx[u-ni])-
			      2*B*(gx[u+ni+1]-gx[u-1+ni]-gx[u+1-ni]+gx[u-1-ni]))/(2*(A+G));

		    gy[u] = (A*(gy[u+1]+gy[u-1])+G*(gy[u+ni]+gy[u-ni])-
				2*B*(gy[u+ni+1]-gy[u-1+ni]-gy[u+1-ni]+gy[u-1-ni]))/(2*(A+G));
		}
	    }	
	}

    /**Returns boundary normal by considering boundary splines*/
    @Override
    public double[] boundaryNormal(Face face, double[] pos)
    {
	/*evaluate intersection position along spline*/
	double t = splines[face.value()].evalT(pos);
	
	if (t<0) 
	{
	    Log.debug("Error for: "+face+" "+pos[0]+" "+pos[1]);
	    Log.error("call to BoundaryNormal for a point not on boundary");
	}
	
	return splines[face.value()].normal(t);
    }

}
