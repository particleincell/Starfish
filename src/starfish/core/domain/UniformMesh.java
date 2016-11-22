/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import starfish.core.boundaries.Boundary;
import starfish.core.domain.DomainModule.DomainType;

public class UniformMesh extends Mesh
{
    /*variables*/
    public double x0[]=new double[2]; /*node origin*/
    public double dh[]=new double[2]; /*node spacing*/
    public double xd[]=new double[2]; /*diagonal point*/
	
    /*methods*/
    public UniformMesh (int ni, int nj, DomainType domain_type)
    {
	super(ni,nj, domain_type);  
    }
    
    public double getDi() {return dh[0];}
    public double getDj() {return dh[1];}
	
    /*sets mesh origin*/
    public void setOrigin(double x1, double x2)
    {
	x0[0]=x1;
	x0[1]=x2;
	setXd();
    }
    
    /*sets mesh spacing*/
    public void setSpacing(double d1, double d2)
    {
	dh[0]=d1;
	dh[1]=d2;
	setXd();
    }
	
    /*computes diagonal point*/
    protected void setXd()
    {
	xd[0]=x0[0]+(ni-1)*dh[0];
	xd[1]=x0[1]+(nj-1)*dh[1];
    }
    
    @Override
    /*returns position*/
    public double[] pos(double i, double j)
    {
	double x[]=new double[2];
	x[0] = x0[0]+i*dh[0];
	x[1] = x0[1]+j*dh[1];
	return x;
    }

    @Override
    public double[] XtoL(double d1, double d2)
    {
	double lc[] = new double[2];
	
	lc[0] = (d1-x0[0])/dh[0];
	lc[1] = (d2-x0[1])/dh[1];
	return lc;
    }

    @Override
    public boolean containsPosStrict(double x[]) 
    {
	if (x[0]>=x0[0] && x[0]<xd[0] &&
	    x[1]>=x0[1] && x[1]<xd[1])
	    return true;
		
	return false;
    }

    @Override
    public double[] boundaryNormal(Face face, double[] pos)
    {
	double n[] = new double[3];
	
	switch (face)
	{
	    case LEFT: n[0]=1;break;
	    case RIGHT: n[0]=-1; break;
	    case BOTTOM: n[1]=1;break;
	    case TOP: n[1]=-1;break;
	    default: throw new UnsupportedOperationException("Bad Face in a call to boundaryNormal");
	}
	return n;
    }
}
