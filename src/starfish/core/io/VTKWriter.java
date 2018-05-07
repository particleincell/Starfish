/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.io.PrintWriter;
import java.util.ArrayList;
import starfish.core.boundaries.Boundary;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Mesh;

/** writer for ASCII VTK files*/
public class VTKWriter extends Writer 
{

    public VTKWriter(String file_name)
    {
	super(file_name);
    }

    /** splits file name into directory, prefix, and extension
     * @param name     
     * @return Array containing [file name, extension, path, prefix], 
     * for instance "results/field.vts" returns [results/field,.vts,results,field]
     */
    protected String[] splitFileName(String name)
    {
	int p1,p2;
	
	/*find the last back or forward slash*/
	for (p1=name.length()-1;p1>=0;p1--)
	    if (name.charAt(p1)=='/' || name.charAt(p1)=='\\') break;
	
	for (p2=name.length()-1;p2>=0;p2--) if (name.charAt(p2)=='.') break;
	
	//if extension not found, set to end
	if (p2==0) p2 = name.length()-1;
	
	//if no path specified, set to extension position
	if (p1==0) p1=p2;	
	
	String substr[] = {name.substring(0,p2),
			   name.substring(p2),
			   name.substring(0,p1), 
			   name.substring((p1>0)?(p1+1):0,p2)};
   	return substr;    
    }
    
    /**saves 2D data in VTK ASCII forma
     * @param animation if true, will open new file for each save*/
    @Override
    public void write2D(boolean animation)
    {
	int part = 0;	
	for (Mesh mesh:Starfish.getMeshList())
	{
	    //split out extension from the file name
	    
	    String substr[] = splitFileName(file_name);
	    String name = substr[0]+"_"+mesh.getName();
	    if (animation)
		name += String.format("_%06d", Starfish.getIt());
	    name += substr[1];    
	    
	    //add to collection but remove path since relative to pvd file
	    substr = splitFileName(name);
	    //collection.add(new CollectionData(time_step,part,substr[3]+substr[1]));
	    collection.add(new CollectionData(Starfish.getIt(),part,substr[3]+substr[1]));
	    	    
	    PrintWriter pw = open(name);
	    
	    pw.println("<?xml version=\"1.0\"?>");
	    pw.println("<VTKFile type=\"StructuredGrid\">");
	    pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni-1,mesh.nj-1);
	
	    pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n",mesh.ni-1,mesh.nj-1);

	    pw.println("<Points>");
	    pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	    for (int j=0;j<mesh.nj;j++)
		for (int i=0;i<mesh.ni;i++)
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
		    pw.printf("%d ",mesh.getNode(i,j).type.value());
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
	    
	    part++;
	}
	
	/*write the collection file*/
	if (animation || Starfish.getMeshList().size()>1)
	    writeCollection(animation);
    }

     /*colletor for generated files to add to collection*/
    class CollectionData
    {
	int time_step;
	int part;
	String file_name;
	CollectionData(int time_step, int part, String file_name)
	{
	    this.time_step = time_step;
	    this.part = part;
	    this.file_name = file_name;
	}	
    }    
    protected ArrayList<CollectionData> collection = new ArrayList();

    /**
     * Writes the .pvd file for paraview
     * @param animation 
     */
    protected void writeCollection(boolean animation)
    {
	String substr[] = splitFileName(file_name);
	String pvd_name = substr[0];
	if (animation)
		pvd_name += "_anim";
	pvd_name += ".pvd";    
	
	PrintWriter pw = open(pvd_name);
	pw.println("<?xml version=\"1.0\"?>");
	pw.println("<VTKFile type=\"Collection\" version=\"0.1\">");
	//byte_order = "LittleEndian" compressor="vtkZLibDataCompresssor"
	pw.println("<Collection>");
	for (CollectionData cd: collection)
	{
	    pw.printf("<DataSet timestep=\"%d\" group=\"\" part=\"%d\" file=\"%s\" />\n",
			    cd.time_step,cd.part,cd.file_name);
	}
	pw.println("</Collection>");
	pw.println("</VTKFile>");
	pw.close();	
    }
    
    /**
     * Saves data along a single I/J grid line on a single mesh
     */
    @Override
    public void write1D() 
    {
	int part = 0;	
	//split out extension from the file name
	    
	PrintWriter pw = open(file_name);
	Mesh mesh = output_mesh;
	
	pw.println("<?xml version=\"1.0\"?>");
	pw.println("<VTKFile type=\"StructuredGrid\">");
	if (dim==Dim.I)
	{
	    pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 0 0 0\">\n", mesh.ni-1);
	    pw.printf("<Piece Extent=\"0 %d 0 0 0 0\">\n",mesh.ni-1);
	}
	else if (dim==Dim.J)
	{
	    pw.printf("<StructuredGrid WholeExtent=\"0 0 0 %d 0 0\">\n", mesh.nj-1);
	    pw.printf("<Piece Extent=\"0 0 0 %d 0 0\">\n",mesh.nj-1);
	}
	else
	    Log.error("Unsupported dimension in write1D");
	
	pw.println("<Points>");
	pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	if (dim == Dim.I)
	{
	    for (int j=0;j<mesh.nj;j++)
	    {
		double x[] = mesh.pos(index,j);			
		pw.printf("%g %g 0 ", x[0], x[1]);
	    }
	}
	else
	{
	    for (int i=0;i<mesh.ni;i++)
	    {
		double x[] = mesh.pos(i,index);			
		pw.printf("%g %g 0 ", x[0], x[1]);
	    }	    
	}
	
	pw.println("\n</DataArray>");
	pw.println("</Points>");

	/*cell data*/
	pw.println("<CellData>");
	   
	for (String var:cell_data)
	{
	    double data[][] = Starfish.domain_module.getField(mesh, var).getData();

	    pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
	    if (dim == Dim.I)
	    {
		for (int j=0;j<mesh.nj-1;j++)
		    pw.printf("%g ",(data[index][j]));
	    }
	    else 
	    {
		for (int i=0;i<mesh.ni-1;i++)
		    pw.printf("%g ",(data[i][index]));
	    }
	    pw.println("\n</DataArray>");
	}	
	pw.println("</CellData>");

	/*point data*/
	pw.println("<PointData>");
	pw.println("<DataArray Name=\"type\" type=\"Int32\" NumberOfComponents=\"1\" format=\"ascii\">");
	if (dim == Dim.I)
	{
	    for	(int j=0;j<mesh.ni;j++)
		pw.printf("%d ",mesh.getNode(index,j).type.value());
	}
	else
	{
	    for	(int i=0;i<mesh.ni;i++)
		pw.printf("%d ",mesh.getNode(i,index).type.value());	    
	}
	pw.println("\n</DataArray>");

	for (String var:scalars)
	{
	    /*make sure we have this variable*/
	  //  if (!Starfish.output_module.validateVar(var)) continue;
	    double data[][] = Starfish.domain_module.getField(mesh, var).getData();

	    pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
	    if (dim == Dim.I)
	    {
		for (int j=0;j<mesh.nj;j++)
		    pw.printf("%g ",(data[index][j]));
	    }
	    else
	    {
		for (int i=0;i<mesh.ni;i++)
		    pw.printf("%g ",(data[i][index]));
	    }
	    	
	    pw.println("\n</DataArray>");
	}

	for (String[] vars:vectors)
	{
	    /*make sure we have this variable*/
	  //  if (!Starfish.output_module.validateVar(var)) continue;
	    double data1[][] = Starfish.domain_module.getField(mesh, vars[0]).getData();
	    double data2[][] = Starfish.domain_module.getField(mesh, vars[1]).getData();

	    pw.println("<DataArray Name=\""+vars[0]+"\" type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	    if (dim == Dim.I)
	    {
		for (int j=0;j<mesh.nj;j++)
		    pw.printf("%g %g 0 ",data1[index][j], data2[index][j]);
	    }
	    else
	    {
		for (int i=0;i<mesh.ni;i++)
		    pw.printf("%g %g 0 ",data1[i][index], data2[i][index]);
	    }

	    pw.println("\n</DataArray>");
	}

	pw.println("</PointData>");	

	pw.println("</Piece>");

	pw.println("</StructuredGrid>");
	pw.println("</VTKFile>");
	/*save output file*/
	pw.close();
    }

    /** Writes the "surface" file
     *
     */
    @Override
    public void writeBoundaries() 
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
	
	PrintWriter pw = open(file_name);
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
		pw.printf("%d ", boundary.getType().value());
	    }
	pw.println("\n</DataArray>");
	
	pw.println("<DataArray type=\"Int32\" NumberOfComponents=\"1\" Name=\"boundary_id\" format=\"ascii\">");
	for (int b=0;b<bl.size();b++)	 
	{
	    Boundary boundary = bl.get(b);
	    for (int i=0;i<boundary.numPoints()-1;i++)
	    {
		pw.printf("%d ", b);
	    }
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

    /**
     *
     */
    @Override
    protected void writeParticles()
    {
	Log.warning("writeParticles not yet implemented for VTK"); //To change body of generated methods, choose Tools | Templates.
    }


}
