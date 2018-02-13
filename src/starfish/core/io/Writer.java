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
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.Mesh;

/**
 *
 * @author Lubos Brieda
 */
public abstract class Writer 
{
    static enum OutputType {FIELD,ONED,BOUNDARIES,PARTICLES};
    static enum Dim {I,J};
	
    PrintWriter pw = null;
    OutputType output_type;
    String scalars[];
    ArrayList <String[]> vectors;
    String cell_data[];	    //cell centered variables
    Mesh output_mesh;
    Dim dim;
    int index;
    String file_name;
    
    /*for particles*/
    int particle_count;
	
    /**
     *
     * @param file_name
     * @param variables
     * @param element
     */
    public void open1D(String file_name,String[] variables, Element element)
    {
	String mesh_name = InputParser.getValue("mesh",element);
	String index_str = InputParser.getValue("index",element);
	this.file_name = file_name;
	    
	output_type=OutputType.ONED;
	ArrayList<String[]> vectors = new ArrayList<String[]>();
	String cell_data[] = {};	
	open(file_name,variables,vectors,cell_data,mesh_name,index_str);
    }
	

    /** open function for 1D output
     * @param file_name
     * @param scalars
     * @param vectors
     * @param cell_data
     * @param index
     * @param mesh_name*/
    protected void open(String file_name, String[] scalars, ArrayList<String[]> vectors, String[] cell_data, String mesh_name, String index)
    {
	/*grab the mesh*/
	output_mesh =Starfish.domain_module.getMesh(mesh_name);
	if (output_mesh==null)
	{
	    Log.error("Mesh "+mesh_name+" does not exist");
	    return;
	}
	this.file_name = file_name;
	
	/*parse index*/
	String pieces[]=index.split("\\s*=\\s*");
	if (pieces.length!=2)
	    Log.error(String.format("couldn't parse index string %s, syntax [I/J]=value",index));
		
	if (pieces[0].equalsIgnoreCase("I"))
	    dim = Dim.I;
	else if (pieces[0].equalsIgnoreCase("J"))
	    dim = Dim.J;
	else Log.error("Unknown dimension "+pieces[0]);
    
	this.index=Integer.parseInt(pieces[1]);		
		
	/*call main open function*/
	open2D(file_name, scalars, vectors, cell_data);	
    }
	
    /** open function for 2D field dat
     * @param file_name
     * @param scalarsa
     * @param vectors
     * @param cell_data*/
    protected void open2D(String file_name, String[] scalars, ArrayList<String[]> vectors, String[] cell_data)
    {
	output_type=OutputType.FIELD;
	this.file_name = file_name;
	
	try {
	    pw = new PrintWriter(new FileWriter(file_name));
	} catch (IOException ex) 
	{
	    Log.error("error opening file "+file_name);
	}
		
	String vars_temp[] = new String[scalars.length];
	int temp_length=0;
		
	Mesh mesh = Starfish.getMeshList().get(0);
	for (int v=0;v<scalars.length;v++)
	{
	    try{
		Starfish.getField(mesh, scalars[v]);
		vars_temp[temp_length++] = scalars[v];
	    }
	    catch(Exception e)
	    {
		Log.warning("Skipping unrecognized variable "+scalars[v]);
	    }
	}
		
	/*save vars*/
	this.scalars = new String[temp_length];
	System.arraycopy(vars_temp, 0, this.scalars, 0, temp_length);
				
	/*now repeat for cell variables*/
	String cell_data_temp[] = new String[cell_data.length];
	int cell_data_temp_length=0;
		
	for (int v=0;v<cell_data.length;v++)
	{
	    try{
		Starfish.getField(mesh, cell_data[v]);
		cell_data_temp[cell_data_temp_length++]=cell_data[v];
	    }
	    catch(Exception e)
	    {
		Log.warning("Skipping unrecognized variable "+cell_data[v]);
	    }
	}		
	/*save vars*/
	this.cell_data = new String[cell_data_temp_length];
	System.arraycopy(cell_data_temp, 0, this.cell_data, 0, cell_data_temp_length);
	
	this.vectors = new ArrayList<String[]>();		
	for (String[] pair:vectors)
	{
	    try{
		Starfish.getField(mesh, pair[0]);
		Starfish.getField(mesh, pair[1]);	
		this.vectors.add(pair);
	    }
	    catch(Exception e)
	    {
		Log.warning("Skipping unrecognized vector pair "+pair[0]+":"+pair[1]);
	    }
	}		
	
	/*write header*/
	writeHeader();
    }
	
    /** open function for boundary dat
     * @param file_namea
     * @param variables*/
    protected void openBoundaries(String file_name, String[] variables)
    {
	output_type = OutputType.BOUNDARIES;
	
	try {
	    pw = new PrintWriter(new FileWriter(file_name));
	} catch (IOException ex) 
	{
	    Log.error("error opening file "+file_name);
	}
		
	String vars_temp[] = new String[variables.length];
	int temp_length=0;
	
	/*validate variables*/
	for (int v=0;v<variables.length;v++)
	{
	    try{
		Starfish.boundary_module.getFieldCollection(variables[v]);
		vars_temp[temp_length++]=variables[v];
	    }
	    catch(Exception e)
	    {
		Log.warning("Skipping unrecognized variable "+variables[v]);
	    }
	}
		
	/*save vars*/
	this.scalars = new String[temp_length];
	System.arraycopy(vars_temp, 0, this.scalars, 0, temp_length);
				
	/*write header*/
	writeHeader();
    }

    /** open function for particle outpu
     * @param file_name
     * @param elementt*/
    public void openParticles(String file_name,Element element)
    {
	output_type=OutputType.PARTICLES;

	try {
	    pw = new PrintWriter(new FileWriter(file_name));
	} catch (IOException ex) 
	{
	    Log.error("error opening file "+file_name);
	}
		
	/*get count*/
	if (element!=null)
	    particle_count = InputParser.getInt("count", element,1000);
	else
	    particle_count = 1000;
	
	/*save vars*/
	scalars = new String[5];
	
	if (Starfish.domain_module.getDomainType()==DomainType.XY)
	{
	    scalars[0] = "z (m)";
	    scalars[1] = "u";
	    scalars[2] = "v";
	    scalars[3] = "w";
	}
	else if (Starfish.domain_module.getDomainType()==DomainType.RZ)
	{
	    scalars[0] = "theta (rad)";
	    scalars[1] = "ur";
	    scalars[2] = "uz";
	    scalars[3] = "utheta";
	}
	else if (Starfish.domain_module.getDomainType()==DomainType.ZR)
	{
	    scalars[0] = "theta (rad)";
	    scalars[1] = "uz";
	    scalars[2] = "ur";
	    scalars[3] = "utheta";
	}
	    
	scalars[4] = "id";
	
	/*write header*/
	writeHeader();
    }

    /**
     *
     */
    public final void writeZone() {writeZone(false);}	   //wrapper for default param

    /**
     *
     * @param animation
     */
    public final void writeZone(boolean animation)
    {
	/*update average data, this is needed mainly to capture anything if not yet at steady state,
	 true parameter to force sampling even if not yet in steady state*/
	Starfish.averaging_module.sample(true);
	
	switch (output_type)
	{
	    case FIELD: writeZone2D(animation);break;
	    case ONED: writeZone1D();break;
	    case BOUNDARIES: writeZoneBoundaries();break;
	    case PARTICLES: writeParticles();break;
		
	}
	pw.flush();
    }
	
    /**
     *
     */
    protected abstract void writeHeader();

    /**
     *
     * @param animation
     */
    protected abstract void writeZone2D(boolean animation);

    /**
     *
     */
    protected abstract void writeZone1D();

    /**
     *
     */
    protected abstract void writeZoneBoundaries();

    /**
     *
     */
    protected abstract void writeParticles();

    /**
     *
     * @param data
     */
    public abstract void writeData(double data[]);
    
    /** closes the file*/
    public void close()
    {
	pw.close();
    }
	   	
    /** convenience method for one stop writin
     * @param file_name
     * @param variablesg*/
    public final void saveBoundaries(String file_name, String[] variables)
    {
	openBoundaries(file_name,variables);
	writeZone();
	close();
    }
}
