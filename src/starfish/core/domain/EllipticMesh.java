/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import starfish.core.boundaries.Spline;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.io.InputParser;

/** Constructs a quadrilateral mesh bounded by four prescribed splines */
public class EllipticMesh extends QuadrilateralMesh
{

    /**
     *
     */
    protected Spline splines[]; 
    
    /**boundaries are given by 4 splines: 
     * @param nn number of nodes
     * @param element XML element
     * @param name mesh name
     * @param domain_type XY/RZ*/
    public EllipticMesh (int nn[], Element element, String name, DomainType domain_type)
    {
	super(nn, name,domain_type);
		
	String left[] = InputParser.getList("left",element);
	String bottom[] = InputParser.getList("bottom",element);
	String right[] = InputParser.getList("right",element);
	String top[] = InputParser.getList("top",element);

	splines = new Spline[4];
	ArrayList<Spline> list = new ArrayList<Spline>();

	try{
	    /*left*/
	    for (String str:left)
		list.add(Starfish.getBoundary(str));
	    splines[Face.LEFT.val()] = new Spline(list);

	    /*bottom*/
	    list.clear();
	    for (String str:bottom)
		list.add(Starfish.getBoundary(str));
	    splines[Face.BOTTOM.val()] = new Spline(list);

	    /*right*/
	    list.clear();
	    for (String str:right)
		list.add(Starfish.getBoundary(str));
	    splines[Face.RIGHT.val()] = new Spline(list);

	    /*top*/
	    list.clear();
	    for (String str:top)
		list.add(Starfish.getBoundary(str));
	    splines[Face.TOP.val()] = new Spline(list);
	}
	catch (NoSuchElementException e)
	{
	    Log.error(e.getMessage());
	}

	/*log*/
	Log.log("Added ELLIPTIC_MESH");
	Log.log("> nodes   = "+ni+" : "+nj);
	Log.log("> left  = "+InputParser.getValue("left", element));
	Log.log("> bottom = "+InputParser.getValue("bottom",element));
	Log.log("> right = "+InputParser.getValue("right",element));
	Log.log("> top = "+InputParser.getValue("top",element));			
   
	int u,i,j;
	double pos[], t[];
	double gx[] = new double[ni*nj];
	double gy[] = new double[ni*nj];

	/*left boundary*/
	t = splines[Face.LEFT.val()].splitSpline(nj,false);
	for (j=0;j<nj;j++)
	{
	    u=j*ni;
	    pos = splines[Face.LEFT.val()].pos(t[j]);
	    gx[u]=pos[0];
	    gy[u]=pos[1];
	}
    
	/*right boundary*/
	t = splines[Face.RIGHT.val()].splitSpline(nj,true);
	for (j=0;j<nj;j++)
	{
	    u=j*ni+ni-1;
	    pos = splines[Face.RIGHT.val()].pos(t[j]);
	    gx[u]=pos[0];
	    gy[u]=pos[1];
	}
	
	/*bottom*/
	t = splines[Face.BOTTOM.val()].splitSpline(ni,true);
	for (i=0;i<ni;i++)	
	{
	    u=i;
	    pos = splines[Face.BOTTOM.val()].pos(t[i]);
	    gx[u]=pos[0];
	    gy[u]=pos[1];
	}
	
	/*top*/
	t = splines[Face.TOP.val()].splitSpline(ni,false);
	for (i=0;i<ni;i++)
	{
	    u=(nj-1)*ni+i;
	    pos = splines[Face.TOP.val()].pos(t[i]);
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

    /**Returns boundary normal by considering boundary spline
     * @return s*/
    @Override
    public double[] faceNormal(Face face, double[] pos)
    {
	/*evaluate intersection position along spline*/
	double t = splines[face.val()].evalT(pos);
	
	if (t<0) 
	{
	    Log.debug("Error for: "+face+" "+pos[0]+" "+pos[1]);
	    Log.error("call to BoundaryNormal for a point not on boundary");
	}
	
	return splines[face.val()].normal(t);
    }

}
