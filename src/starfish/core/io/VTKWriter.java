/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.util.ArrayList;
import starfish.core.boundaries.Boundary;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Mesh;

/** writer for ASCII VTK files*/
public class VTKWriter extends Writer 
{
    @Override
    public void writeHeader()
    {
	/*print header*/
	pw.println("# vtk DataFile Version 2.0");
	pw.println("(note)");
	pw.println("ASCII");
    
	/*this format supports only one mesh so for now otput just one*/
	if (Starfish.getMeshList().size()>1)
	    Log.warning("VTK file format supports only one mesh, only the first mesh will be outputted");		
    }
	
    /**saves 2D data in VTK ASCII format*/
    @Override
    public void writeZone2D()
    {
	/*open file*/
			
	Mesh mesh = Starfish.getMeshList().get(0);
		
	pw.println("DATASET STRUCTURED_GRID");
	pw.println("DIMENSIONS "+mesh.ni+" "+mesh.nj+" 1");
	pw.println("POINTS "+mesh.ni*mesh.nj+ " float");
		
	for (int j=0;j<mesh.nj;j++)
	    for	(int i=0;i<mesh.ni;i++)
	    {
		double x[] = mesh.pos(i,j);			
		pw.printf("%g %g 0\n", x[0], x[1]);
	    }
		
	pw.println();
	pw.println("POINT_DATA "+mesh.ni*mesh.nj);
	pw.println("SCALARS type int 1");
	pw.println("LOOKUP_TABLE default");
	for (int j=0;j<mesh.nj;j++)
	{
	    for	(int i=0;i<mesh.ni;i++)
		pw.printf("%d ",mesh.getNode(i,j).type.ordinal());
	    pw.println();
	}
	
	for (String var:variables)
	{
	    /*make sure we have this variable*/
	    Starfish.output_module.validateVar(var);

	    /*print var name*/
	    pw.println("SCALARS "+var+" float 1");
	    pw.println("LOOKUP_TABLE default");
	    double data[][] = Starfish.domain_module.getField(mesh, var).getData();

	    for (int j=0;j<mesh.nj;j++)
	    {
		for (int i=0;i<mesh.ni;i++)
		    pw.printf("%8.6g ",(data[i][j]));
		pw.println();
	    }
	}
		
	/*save output file*/
        pw.flush();
    }

    @Override
    public void writeZone1D() 
    {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeZoneBoundaries() 
    {
	/*count number of points*/
	int num_points=0;
	ArrayList<Boundary> bl = Starfish.getBoundaryList();
	
	for (Boundary boundary:bl)
	{
	   num_points+=boundary.numPoints(); 
	}
		
	pw.println("DATASET POLYDATA");
	pw.println("POINTS "+num_points+ " float");
		
	for (Boundary boundary:bl)
	    for (int i=0;i<boundary.numPoints();i++)
	    {
		double x[] = boundary.getPoint(i);
		pw.printf("%.6g %.6g 0\n", x[0], x[1]);
	    }
	
	int p=0;
	pw.printf("LINES %d %d\n",bl.size(),num_points+bl.size());
	for (Boundary boundary:bl)
	{
	    pw.printf("%d ",boundary.numPoints());
	    for	(int i=0;i<boundary.numPoints();i++)
	    {
		pw.printf("%d ",p);
		p++;
	    }
	    pw.println();
	}
	
		
	/*save output file*/
        pw.flush();
    }	

    @Override
    protected void writeParticles()
    {
	throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeData(double[] data)
    {
	throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
