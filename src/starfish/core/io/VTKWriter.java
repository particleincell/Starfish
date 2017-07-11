/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
    
	/*this format supports only one mesh so for now otput just one*/
	if (Starfish.getMeshList().size()>1)
	    Log.warning("VTK file format supports only one mesh, only the first mesh will be outputted");		
    }
	
    /**saves 2D data in VTK ASCII forma
     * @param animation if true, will open new file for each save*/
    @Override
    public void writeZone2D(boolean animation)
    {
	//hacked in for now, need to rewrite file output to eliminate opening and header writing by Writer
	if (animation)
	{
	    pw.close();
	    //split out extension from the file name
	    int i;
	    for (i=file_name.length()-1;i>=0;i--) if (file_name.charAt(i)=='.') break;
	    if (i==0) i=file_name.length()-1;	//if not found, set to end
	    String substr[] = {file_name.substring(0,i),file_name.substring(i)};
   	    
	    String name = substr[0]+String.format("_%06d", Starfish.getIt())+substr[1];    
	    	    
	    try {
		pw = new PrintWriter(new FileWriter(name));
	    } catch (IOException ex) 
	    {
		Log.error("error opening file "+name);
	    }
	}
	
	Mesh mesh = Starfish.getMeshList().get(0);
	pw.println("<?xml version=\"1.0\"?>");
	pw.println("<VTKFile type=\"StructuredGrid\">");
	pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni-1,mesh.nj-1);
	pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n",mesh.ni-1,mesh.nj-1);
	   
	pw.println("<Points>");
	pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	for (int j=0;j<mesh.nj;j++)
	    for	(int i=0;i<mesh.ni;i++)
	    {
		double x[] = mesh.pos(i,j);			
		pw.printf("%g %g 0 ", x[0], x[1]);
	    }
	pw.println("\n</DataArray>");
	pw.println("</Points>");
	
	/*hard coded for now until I get some more robust way to output cell and vector data*/
	pw.println("<CellData>");
	pw.println("<DataArray Name=\"CellVol\" type=\"Float32\" NumberOfComponents=\"1\" format=\"ascii\">");
	for (int j=0;j<mesh.nj-1;j++)
	    for	(int i=0;i<mesh.ni-1;i++)
		pw.printf("%g ",mesh.cellVol(i, j));
	pw.println("\n</DataArray>");
	
	for (String var:cell_data)
	{
	    double data[][] = Starfish.domain_module.getField(mesh, var).getData();
	    
	    pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (int j=0;j<mesh.nj-1;j++)
	  	for (int i=0;i<mesh.ni-1;i++)
		    pw.printf("%g ",(data[i][j]));
	    pw.println("\n</DataArray>");
	}	
	pw.println("</CellData>");
	
	pw.println("<PointData>");
	pw.println("<DataArray Name=\"type\" type=\"Int32\" NumberOfComponents=\"1\" format=\"ascii\">");
	for (int j=0;j<mesh.nj;j++)
	    for	(int i=0;i<mesh.ni;i++)
		pw.printf("%d ",mesh.getNode(i,j).type.ordinal());
	pw.println("\n</DataArray>");
	
	for (String var:scalars)
	{
	    /*make sure we have this variable*/
	  //  if (!Starfish.output_module.validateVar(var)) continue;
	    double data[][] = Starfish.domain_module.getField(mesh, var).getData();
	    
	    pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (int j=0;j<mesh.nj;j++)
	  	for (int i=0;i<mesh.ni;i++)
		    pw.printf("%g ",(data[i][j]));
	    pw.println("\n</DataArray>");
	}
	
	for (String[] vars:vectors)
	{
	    /*make sure we have this variable*/
	  //  if (!Starfish.output_module.validateVar(var)) continue;
	    double data1[][] = Starfish.domain_module.getField(mesh, vars[0]).getData();
	    double data2[][] = Starfish.domain_module.getField(mesh, vars[1]).getData();
	    
	    pw.println("<DataArray Name=\""+vars[0]+"\" type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	    for (int j=0;j<mesh.nj;j++)
	  	for (int i=0;i<mesh.ni;i++)
		    pw.printf("%g %g 0 ",data1[i][j], data2[i][j]);
	    pw.println("\n</DataArray>");
	}
		
	pw.println("</PointData>");	
	 
	 
	 pw.println("</Piece>");
	 pw.println("</StructuredGrid>");
	 pw.println("</VTKFile>");
	/*save output file*/
        pw.close();
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
	int num_points = 0;
	int num_lines = 0;
	ArrayList<Boundary> bl = Starfish.getBoundaryList();
	
	for (Boundary boundary:bl)
	{
	   num_points+=boundary.numPoints(); 
	   num_lines+=boundary.numPoints()-1;
	}
	
	pw.println("<?xml version=\"1.0\"?>");
	pw.println("<VTKFile type=\"PolyData\">");
	pw.println("<PolyData>");
	pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfVerts=\"0\" "
		+ "NumberOfLines=\"%d\" NumberOfStrips=\"0\" NumberOfPolys=\"0\">\n",
		num_points,num_lines);
	
	pw.println("<Points>");
	 pw.println("<DataArray type=\"Float32\" NumberOfComponents=\"3\" format=\"ascii\">");
	 for (Boundary boundary:bl)
	    for (int i=0;i<boundary.numPoints();i++)
	    {
		double x[] = boundary.getPoint(i);
		pw.printf("%g %g 0\n", x[0], x[1]);
	    }
	pw.println("</DataArray>");
	pw.println("</Points>");
	
	pw.println("<Lines>");
	pw.println("<DataArray type=\"Int32\" Name=\"connectivity\" format=\"ascii\">");
	int p0=0;
	for (Boundary boundary:bl)
	{
	    for (int i=0;i<boundary.numPoints()-1;i++)
		pw.printf("%d %d ",p0+i, p0+i+1);
	    p0+=boundary.numPoints();
	}
	pw.println("\n</DataArray>");
	pw.println("<DataArray type=\"Int32\" Name=\"offsets\" format=\"ascii\">");
	p0=2;
	for (Boundary boundary:bl)
	{
	    for (int i=0;i<boundary.numPoints()-1;i++,p0+=2)
		pw.printf("%d ",p0);
	}
	pw.println("\n</DataArray>");
	pw.println("</Lines>");
	
	/*normals*/
	pw.println("<CellData>");
	pw.println("<DataArray type=\"Float32\" NumberOfComponents=\"3\" Name=\"normals\" format=\"ascii\">");
	for (Boundary boundary:bl)
	    for (int i=0;i<boundary.numPoints()-1;i++)
	    {
		double norm[] = boundary.normal(i+0.5);
		pw.printf("%g %g 0 ", norm[0],norm[1]);
	    }
	pw.println("\n</DataArray>");
	
	pw.println("<DataArray type=\"Int32\" NumberOfComponents=\"1\" Name=\"type\" format=\"ascii\">");
	for (Boundary boundary:bl)
	    for (int i=0;i<boundary.numPoints()-1;i++)
	    {
		pw.printf("%d ", boundary.getType().ordinal());
	    }
	pw.println("\n</DataArray>");
	
	pw.println("</CellData>");
	
	/*data*/
	pw.println("<PointData>");
	
	/*first save node area*/
	pw.println("<DataArray Name=\"area\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
	for (Boundary boundary:bl)
	{
	    for (int i=0;i<boundary.numPoints();i++)
	        pw.printf("%g ", boundary.nodeArea(i));
	}
	pw.println("\n</DataArray>");

	for (String var:scalars)
	{
	    /*make sure we have this variable*/
	    if (!Starfish.output_module.validateVar(var)) continue;
	    
	    pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (Boundary boundary:bl)
	    {
		double data[] = Starfish.boundary_module.getField(boundary,var).getData();
		for (int i=0;i<boundary.numPoints();i++)
		    pw.printf("%g ", data[i]);
	    }
	    pw.println("\n</DataArray>");
	}
	
	 pw.println("</PointData>");	
	 
	pw.println("</Piece>");
	pw.println("</PolyData>");
	pw.println("</VTKFile>");

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
