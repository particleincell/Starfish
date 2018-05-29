/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import starfish.core.boundaries.Boundary;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.Mesh;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;

/** writer for ASCII VTK files*/
public class VTKWriter extends Writer 
{

    public VTKWriter(String file_name)
    {
	super(file_name);
    }
    
    @Override
    public void write3D(boolean animation)
    {
	if (Starfish.getDomainType()==DomainType.XY)
	    Log.warning("Write3D is not (yet) supported for DomainType XY");
	
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

	    pw.println("<VTKFile type=\"UnstructuredGrid\">");
	    pw.println("<UnstructuredGrid>");
	    pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfCells=\"%d\">\n", 
		    mesh.n_nodes*(resolution),
		    mesh.n_cells*(resolution-1));
	    	    
	    pw.println("<Points>");
	    pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	    
	    for (int m=0;m<resolution;m++)
	    {
		//first and last slice is duplicated to simplify cell writing, hopefully
		//Paraview can deal with this fine
		double theta = m*Math.PI/(resolution-1);
		for (int j=0;j<mesh.nj;j++)
		    for (int i=0;i<mesh.ni;i++)
		    {
			double x[] = mesh.pos(i,j);			
			if (Starfish.getDomainType()==DomainType.RZ)
			    pw.printf("%g %g %g ", Math.cos(theta)*x[0], x[1], Math.sin(theta)*x[0]);
			else
			    pw.printf("%g %g %g ", x[0], Math.cos(theta)*x[1], Math.sin(theta)*x[1]);
			pw.println();
		    }
	    }
	    pw.println("\n</DataArray>");
	    pw.println("</Points>");
	    
	    pw.println("<Cells>");
	    pw.println("<DataArray type=\"Int32\" Name=\"connectivity\">");
	    for (int m=0;m<resolution-1;m++)
	    {
		for (int j=0;j<mesh.nj-1;j++)
		    for (int i=0;i<mesh.ni-1;i++)
		    {
			int d1 = m*mesh.n_nodes;
			int d2 = (m+1)*mesh.n_nodes;
			pw.printf("%d %d %d %d ", d1+mesh.IJtoN(i, j), d1+mesh.IJtoN(i+1,j),
			    		      d1+mesh.IJtoN(i+1, j+1), d1+mesh.IJtoN(i, j+1));
			pw.printf("%d %d %d %d ", d2+mesh.IJtoN(i, j), d2+mesh.IJtoN(i+1,j),
			    		      d2+mesh.IJtoN(i+1, j+1), d2+mesh.IJtoN(i, j+1));
		    }
	    }
	    pw.println("\n</DataArray>");

	    pw.println("<DataArray type=\"Int32\" Name=\"offsets\">");
	    for (int c=0;c<(resolution-1)*mesh.n_cells;c++)
		pw.printf("%d ",(c+1)*8);		    
	    pw.println("\n</DataArray>");
	    
	    pw.println("<DataArray type=\"Int32\" Name=\"types\">");
	    for (int c=0;c<(resolution-1)*mesh.n_cells;c++)
		pw.printf("12 ");   //VTK_HEXAHEDRON
	    pw.println("\n</DataArray>");
	    
	    pw.println("</Cells>");

	    /*hard coded for now until I get some more robust way to output cell and vector data*/
	    pw.println("<CellData>");
	    pw.println("<DataArray Name=\"CellVol\" type=\"Float32\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (int m=0;m<resolution-1;m++)
		for (int j=0;j<mesh.nj-1;j++)
		    for (int i=0;i<mesh.ni-1;i++)
			pw.printf("%g ",mesh.cellVol(i, j));
	    pw.println("\n</DataArray>");

	    pw.println("<DataArray Name=\"CellId\" type=\"Int32\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (int c=0;c<(resolution-1)*mesh.n_cells;c++)
		pw.printf("%d ",c);
	    pw.println("\n</DataArray>");

	    for (String var:cell_data)
	    {
		double data[][] = Starfish.domain_module.getField(mesh, var).getData();

		pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
		for (int m=0;m<resolution-1;m++)
		    for (int j=0;j<mesh.nj-1;j++)
			for (int i=0;i<mesh.ni-1;i++)
			    pw.printf("%g ",(data[i][j]));
		pw.println("\n</DataArray>");
	    }	
	    pw.println("</CellData>");

	    pw.println("<PointData>");
	    pw.println("<DataArray Name=\"type\" type=\"Int32\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (int m=0;m<resolution;m++)
		for (int j=0;j<mesh.nj;j++)
		    for (int i=0;i<mesh.ni;i++)
			pw.printf("%d ",mesh.getNode(i,j).type.value());
	    pw.println("\n</DataArray>");

	    for (String var:scalars)
	    {
		/*make sure we have this variable*/
	      //  if (!Starfish.output_module.validateVar(var)) continue;
		double data[][] = Starfish.domain_module.getField(mesh, var).getData();

		pw.println("<DataArray Name=\""+var+"\" type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
		for (int m=0;m<resolution;m++)
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
		for (int m=0;m<resolution;m++)
		    for (int j=0;j<mesh.nj;j++)
			for (int i=0;i<mesh.ni;i++)
			    pw.printf("%g %g 0 ",data1[i][j], data2[i][j]);
		pw.println("\n</DataArray>");
	    }

	    pw.println("</PointData>");	
	 	 
	    pw.println("</Piece>");
	
	    pw.println("</UnstructuredGrid>");
	    pw.println("</VTKFile>");
	    /*save output file*/
	    pw.close();
	    
	    part++;
	}
	
	/*write the collection file*/
	writeCollection(animation);

    }
	
    /**saves 2D data in VTK ASCII format, supports .vts, .vtr, and .vtp
     * @param animation if true, will open new file for each save*/
    protected enum VTK_Type {RECT, STRUCT, POLY};

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
	    
	    VTK_Type vtk_type;
	    
	    if (substr[1].equalsIgnoreCase(".vts")) vtk_type = VTK_Type.STRUCT;
	    else if (substr[1].equalsIgnoreCase(".vtr")) vtk_type = VTK_Type.RECT;
	    else {Log.warning("Unrecognized VTK data type "+substr[1]+", assuming VTS");vtk_type=VTK_Type.STRUCT;}
	    	    
	    //add to collection but remove path since relative to pvd file
	    substr = splitFileName(name);
	    //collection.add(new CollectionData(time_step,part,substr[3]+substr[1]));
	    collection.add(new CollectionData(Starfish.getIt(),part,substr[3]+substr[1]));
	    	    
	    PrintWriter pw = open(name);
	    
	    pw.println("<?xml version=\"1.0\"?>");
	    
	    String close_tag="";
	    
	    if (vtk_type==VTK_Type.STRUCT)
	    {
		pw.println("<VTKFile type=\"StructuredGrid\">");
		pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni-1,mesh.nj-1);
		pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n",mesh.ni-1,mesh.nj-1);
		close_tag = "</StructuredGrid>";
	    }
	    else if (vtk_type==VTK_Type.RECT)
	    {
		pw.println("<VTKFile type=\"RectilinearGrid\">");
	    	pw.printf("<RectilinearGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni-1,mesh.nj-1);
		pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n",mesh.ni-1,mesh.nj-1);
		close_tag = "</RectilinearGrid>";
	    }
	    
	    if (vtk_type==VTK_Type.STRUCT)
	    {
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
	    }	    
	    else if (vtk_type==VTK_Type.RECT)
	    {
		pw.println("<Coordinates>");
		pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
		for (int i=0;i<mesh.ni;i++)
		    	pw.printf("%g ",mesh.pos1(i, 0));			
		pw.println("\n</DataArray>");
		
		pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
		for (int j=0;j<mesh.nj;j++)
		    	pw.printf("%g ",mesh.pos2(0, j));			
		pw.println("\n</DataArray>");
		
		pw.println("<DataArray type=\"Float64\" NumberOfComponents=\"1\" format=\"ascii\">");
		pw.printf("0");
		pw.println("\n</DataArray>");
		
		pw.println("</Coordinates>");
	    }
	    
	    /*hard coded for now until I get some more robust way to output cell and vector data*/
	    pw.println("<CellData>");
	    pw.println("<DataArray Name=\"CellVol\" type=\"Float32\" NumberOfComponents=\"1\" format=\"ascii\">");
	    for (int j=0;j<mesh.nj-1;j++)
		for (int i=0;i<mesh.ni-1;i++)
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
		for (int i=0;i<mesh.ni;i++)
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
	
	    pw.println(close_tag);
	    pw.println("</VTKFile>");
	    /*save output file*/
	    pw.close();
	    
	    part++;
	}
	
	/*write the collection file*/
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
    public void write1D(boolean animation) 
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
	    for	(int j=0;j<mesh.nj;j++)
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
    public void writeBoundaries(boolean animation) 
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
    protected void writeParticles(boolean animation)
    {
	ArrayList<Particle> parts = new ArrayList();
	KineticMaterial mat = Starfish.getKineticMaterial(mat_name);
	if (mat==null)
	{
	    Log.warning("Material "+mat_name+" is not a kinetic material");
	    return;
	}
	
	double prob = (double)particle_count/mat.getNp();
	for (Mesh mesh:Starfish.getMeshList())
	{
	    Iterator<Particle> it = mat.getIterator(mesh);
	    while(it.hasNext())
	    {
		Particle part = it.next();
		if (Starfish.rnd()<prob) parts.add(part);
	    }
	}
	
	String substr[] = splitFileName(file_name);
	String name = substr[0];
	if (animation)
	    name += String.format("_%06d", Starfish.getIt());
	name += substr[1];    
	PrintWriter pw = open(name);
	
	pw.println("<?xml version=\"1.0\"?>");
	pw.println("<VTKFile type=\"PolyData\">");
	pw.println("<PolyData>");
	pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfVerts=\"0\" "
		+ "NumberOfLines=\"0\" NumberOfStrips=\"0\" NumberOfPolys=\"0\">\n",
		parts.size());
	
	pw.println("<Points>");
	pw.println("<DataArray type=\"Float32\" NumberOfComponents=\"3\" format=\"ascii\">");
	for (int i=0;i<parts.size();i++)
	{
	    Particle part = parts.get(i);
	    double pos[] = {part.pos[0], part.pos[1], 0};
	    
	    if (rotate)
	    {
		
		switch (Starfish.getDomainType())
		{
		    case RZ:    pos[0]=part.pos[0]*Math.cos(part.pos[2]);
				pos[2]=-part.pos[0]*Math.sin(part.pos[2]);
				break;
		    case ZR:	pos[1]=part.pos[1]*Math.cos(part.pos[2]);
				pos[2]=part.pos[1]*Math.sin(part.pos[2]);
				break;
		}		
	    }	  		   
	    pw.printf("%g %g %g\n", pos[0], pos[1], pos[2]);
	}
	pw.println("</DataArray>");
	pw.println("</Points>");
	
	/*data*/
	pw.println("<PointData>");
	
	/*first save node area*/
	pw.println("<DataArray Name=\"velocity\" type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	for (int i=0;i<parts.size();i++)
	{
	    Particle part = parts.get(i);
	    pw.printf("%g %g %g\n", part.vel[0], part.vel[1], part.vel[2]);
	}
	pw.println("\n</DataArray>");

	
	pw.println("</PointData>");	
	 
	pw.println("</Piece>");
	pw.println("</PolyData>");
	pw.println("</VTKFile>");

	/*save output file*/
        pw.flush();
    }

    /** writes particle trace*/
    public void writeTrace(ArrayList<Particle> particles, ArrayList<Integer>time_steps)
    {
	PrintWriter pw = open(file_name);
	pw.println("<?xml version=\"1.0\"?>");
	pw.println("<VTKFile type=\"PolyData\">");
	pw.println("<PolyData>");
	pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfVerts=\"0\" "
		+ "NumberOfLines=\"%d\" NumberOfStrips=\"0\" NumberOfPolys=\"0\">\n",
		particles.size(),particles.size()-1);
	
	pw.println("<Points>");
	pw.println("<DataArray type=\"Float32\" NumberOfComponents=\"3\" format=\"ascii\">");
	for (int i=0;i<particles.size();i++)
	{
	    Particle part = particles.get(i);
	    switch (Starfish.getDomainType())
	    {
		   case RZ: pw.printf("%g %g %g\n", part.pos[0]*Math.cos(part.pos[2]), part.pos[1], -part.pos[0]*Math.sin(part.pos[2]));break; 
		   case ZR: pw.printf("%g %g %g\n", part.pos[0], part.pos[1]*Math.cos(part.pos[2]), part.pos[1]*Math.sin(part.pos[2]));break; 		    
		   default: pw.printf("%g %g %g\n", part.pos[0], part.pos[1], part.pos[2]);break;	    
	    }
	}
	pw.println("</DataArray>");
	pw.println("</Points>");
	
	pw.println("<Lines>");
	pw.println("<DataArray type=\"Int32\" Name=\"connectivity\" format=\"ascii\">");
	for (int i=0;i<particles.size()-1;i++)
	    pw.printf("%d %d ",i, i+1);
	pw.println("\n</DataArray>");
	pw.println("<DataArray type=\"Int32\" Name=\"offsets\" format=\"ascii\">");
	for (int i=0;i<particles.size()-1;i++)
	    pw.printf("%d ",i*2);
	pw.println("\n</DataArray>");
	pw.println("</Lines>");
	
	/*data*/
	pw.println("<PointData>");
	
	/*velocities*/
	pw.println("<DataArray Name=\"vel\" type=\"Float64\" NumberOfComponents=\"3\" format=\"ascii\">");
	for (int i=0;i<particles.size();i++)
	{
	    Particle part = particles.get(i);
	    pw.printf("%g %g %g ", part.vel[0],part.vel[1],part.vel[2]);
	}
	pw.println("\n</DataArray>");

	pw.println("<DataArray Name=\"time_step\" type=\"Int32\" NumberOfComponents=\"1\" format=\"ascii\">");
	for (int i=0;i<particles.size();i++)
	{
	    pw.printf("%d ", time_steps.get(i));
	}
	pw.println("\n</DataArray>");

	pw.println("<DataArray Name=\"part_id\" type=\"Int32\" NumberOfComponents=\"1\" format=\"ascii\">");
	for (int i=0;i<particles.size();i++)
	{
	    Particle part = particles.get(i);	 
	    pw.printf("%d ", part.id);
	}
	pw.println("\n</DataArray>");
	pw.println("</PointData>");	
	 
	pw.println("</Piece>");
	pw.println("</PolyData>");
	pw.println("</VTKFile>");

	/*save output file*/
        pw.flush();
    }


}
