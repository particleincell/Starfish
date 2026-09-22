/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.io.InputParser;
import starfish.core.io.VTKWriter;
import starfish.core.io.VTKWriter.VTK_Type;
import starfish.core.io.Writer;

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
		Starfish.Log.log("> origin  = "+x0[0]+" : "+x0[1]);
		Starfish.Log.log("> spacing = "+dh[0]+" : "+dh[1]);
    }
    
    //another constructor that doesn't read element
    public UniformMesh(int nn[], double x0[], double dh[], String name, DomainType domain_type) {
    	super(nn, name,domain_type);
		setMetrics(x0,dh);
		/*log*/
		Starfish.Log.log("Added UNIFORM_MESH");
		Starfish.Log.log("> nodes   = "+nn[0]+" : "+nn[1]);
		Starfish.Log.log("> origin  = "+x0[0]+" : "+x0[1]);
		Starfish.Log.log("> spacing = "+dh[0]+" : "+dh[1]);
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
    
    @Override
    public String getVTKExtension() {return ".vti";}
    /**
     * Starts VTK output for this mesh
     */
    @Override
    public void startVTKFile(PrintWriter pw, String endianess, VTKWriter writer) {

    		pw.println("<?xml version=\"1.0\"?>");

    		pw.println("<VTKFile type=\"ImageData\"" + endianess + ">");
			pw.printf("<ImageData Origin=\"%g %g 0\" ",x0[0], x0[1]);
			pw.printf("Spacing=\"%g %g 0\" ",dh[0], dh[1]);
			pw.printf("WholeExtent=\"0 %d 0 %d 0 0\">\n", ni - 1, nj - 1);
			
			VTKWriter.writeFieldData(pw);

			pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n", ni - 1, nj - 1);
			
    	/*	if (vtk_type == VTK_Type.STRUCT) {
    			pw.println("<VTKFile type=\"StructuredGrid\"" + endianess + ">");
    			pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, mesh.nj - 1);
    			pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, mesh.nj - 1);
    			close_tag = "</StructuredGrid>";
    		} else if (vtk_type == VTK_Type.RECT) {
    			pw.println("<VTKFile type=\"RectilinearGrid\"" + endianess + ">");
    			pw.printf("<RectilinearGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, mesh.nj - 1);
    			pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, mesh.nj - 1);
    			close_tag = "</RectilinearGrid>";
    		}*/

/*
    		int a = 0;
    		if (vtk_type == VTK_Type.STRUCT) {
    			pw.println("<Points>");

    			double pos[] = new double[mesh.ni * mesh.nj * 3];
    			a = 0;
    			for (int j = 0; j < mesh.nj; j++) {
    				for (int i = 0; i < mesh.ni; i++) {
    					double x[] = mesh.pos(i, j);
    					pos[a++] = x[0];
    					pos[a++] = x[1];
    					pos[a++] = 0;
    				}
    			}
    			outputDataArrayVec(pw, "pos", pos);
    			pw.println("</Points>");
    		} else if (vtk_type == VTK_Type.RECT) {
    			pw.println("<Coordinates>");
    			double pos_x[] = new double[mesh.ni];
    			double pos_y[] = new double[mesh.nj];
    			double pos_z[] = new double[1];
    			for (int i = 0; i < mesh.ni; i++) {
    				pos_x[i] = mesh.pos1(i, 0);
    			}
    			for (int j = 0; j < mesh.nj; j++) {
    				pos_y[j] = mesh.pos1(j, 0);
    			}
    			pos_z[0] = 0;
    			outputDataArrayScalar(pw, "x", pos_x);
    			outputDataArrayScalar(pw, "y", pos_y);
    			outputDataArrayScalar(pw, "z", pos_z);
    			pw.println("</Coordinates>");
    		}
    		*/

    		
    }
    
    @Override
    public void endVTKFile(PrintWriter pw) {

		pw.println("</Piece>");
		pw.println("</ImageData>");
    }
}
