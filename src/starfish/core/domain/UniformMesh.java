/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.io.InputParser;

/**
 *
 * @author Lubos Brieda
 */
public class UniformMesh extends Mesh
{
    /*variables*/

    /**
     *
     */

    public double x0[]=new double[2]; /*node origin*/

    /**
     *
     */
    public double dh[]=new double[2]; /*node spacing*/

    /**
     *
     */
    public double xd[]=new double[2]; /*diagonal point*/
	
    /*methods*/

    /** 
     * Creates a structured rectilinear mesh with uniform spacing in each direction
     * @param nn number of nodes
     * @param element XML element
     * @param name mesh name
     * @param domain_type
     */
    public UniformMesh (int nn[], Element element, String name, DomainType domain_type)
    {
	super(nn, name,domain_type);
	
	String origin[] = InputParser.getList("origin", element);
	String spacing[] = InputParser.getList("spacing", element);
	double x0[] = {Double.parseDouble(origin[0]), Double.parseDouble(origin[1])};
	double dh[] = {Double.parseDouble(spacing[0]), Double.parseDouble(spacing[1])};	    
	
	setMetrics(x0,dh);
	
	/*log*/
	Starfish.Log.log("Added UNIFORM_MESH");
	Starfish.Log.log("> nodes   = "+nn[0]+" : "+nn[1]);
	Starfish.Log.log("> origin  = "+origin[0]+" : "+origin[1]);
	Starfish.Log.log("> spacing = "+spacing[0]+" : "+spacing[1]);	
    }
    
    /*
    
    	///check for axisymmetric domains, can't have ghosts on r=0 plane
	if (domain_type==DomainType.RZ && x0[0]<0)
	{
	    while(x0[0]<0)
	    {
		x0[0] += dh[0];
		ghost_layers[Face.LEFT.val()]--;
	    }
	}
	else if (domain_type==DomainType.ZR && x0[1]<0)
	{
	    while(x0[1]<0)
	    {
		x0[1] += dh[1];
		ghost_layers[Face.BOTTOM.val()]--;
	    }
	}

    */
    
    /**
     *
     * @return
     */
    public double getDi() {return dh[0];}

    /**
     *
     * @return
     */
    public double getDj() {return dh[1];}
	
    /** Sets mesh origin and spacing
     *
     * @param x1
     * @param x2
     */
    public final void setMetrics(double x0[], double dh[])
    {
	this.x0[0]=x0[0];
	this.x0[1]=x0[1];
	this.dh[0] = dh[0];
	this.dh[1] = dh[1];
	setXd();
    }
    
  	
    /**computes the diagonal point*/
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

    /**
     *
     * @param d1
     * @param d2
     * @return
     */
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
    public double[] faceNormal(Face face, double[] pos)
    {
	double n[] = new double[3];
	
	switch (face)
	{
	    case LEFT: n[0]=1;break;
	    case RIGHT: n[0]=-1; break;
	    case BOTTOM: n[1]=1;break;
	    case TOP: n[1]=-1;break;
	    default: throw new UnsupportedOperationException("Bad Face in a call to faceNormal");
	}
	return n;
    }
}
