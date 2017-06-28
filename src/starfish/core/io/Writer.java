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
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.Mesh;

public abstract class Writer 
{
    static enum OutputType {FIELD,ONED,BOUNDARIES,PARTICLES};
    static enum Dim {I,J};
	
    PrintWriter pw = null;
    OutputType output_type;
    String variables[];
    String cell_data[];	    //cell centered variables
    Mesh output_mesh;
    Dim dim;
    int index;
    
    /*for particles*/
    int particle_count;
	
    public void open1D(String file_name,String[] variables, Element element)
    {
	String mesh_name = InputParser.getValue("mesh",element);
	String index_str = InputParser.getValue("index",element);
	    
	output_type=OutputType.ONED;
	String cell_data[] = {};	
	open(file_name,variables,cell_data,mesh_name,index_str);
    }
	

    /** open function for 1D output*/
    protected void open(String file_name, String[] variables, String[] cell_data, String mesh_name, String index)
    {
	/*grab the mesh*/
	output_mesh =Starfish.domain_module.getMesh(mesh_name);
	if (output_mesh==null)
	{
	    Log.error("Mesh "+mesh_name+" does not exist");
	    return;
	}
		
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
	open2D(file_name, variables, cell_data);	
    }
	
    /** open function for 2D field data*/
    protected void open2D(String file_name, String[] variables, String[] cell_data)
    {
	output_type=OutputType.FIELD;
	
	
	try {
	    pw = new PrintWriter(new FileWriter(file_name));
	} catch (IOException ex) 
	{
	    Log.error("error opening file "+file_name);
	}
		
	String vars_temp[] = new String[variables.length];
	int temp_length=0;
		
	Mesh mesh = Starfish.getMeshList().get(0);
	for (int v=0;v<variables.length;v++)
	{
	    try{
		Starfish.getField(mesh, variables[v]);
		vars_temp[temp_length++]=variables[v];
	    }
	    catch(Exception e)
	    {
		Log.warning("Skipping unrecognized variable "+variables[v]);
	    }
	}
		
	/*save vars*/
	this.variables = new String[temp_length];
	System.arraycopy(vars_temp, 0, this.variables, 0, temp_length);
				
	/*now repeate for cell variables*/
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
		
	/*write header*/
	writeHeader();
    }
	
    /** open function for boundary data*/
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
	this.variables = new String[temp_length];
	System.arraycopy(vars_temp, 0, this.variables, 0, temp_length);
				
	/*write header*/
	writeHeader();
    }

    /** open function for particle output*/
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
	variables = new String[5];
	
	if (Starfish.domain_module.getDomainType()==DomainType.XY)
	{
	    variables[0] = "z (m)";
	    variables[1] = "u";
	    variables[2] = "v";
	    variables[3] = "w";
	}
	else if (Starfish.domain_module.getDomainType()==DomainType.RZ)
	{
	    variables[0] = "theta (rad)";
	    variables[1] = "ur";
	    variables[2] = "uz";
	    variables[3] = "utheta";
	}
	else if (Starfish.domain_module.getDomainType()==DomainType.ZR)
	{
	    variables[0] = "theta (rad)";
	    variables[1] = "uz";
	    variables[2] = "ur";
	    variables[3] = "utheta";
	}
	    
	variables[4] = "id";
	
	/*write header*/
	writeHeader();
    }

    public final void writeZone()
    {
	/*update average data, this is needed mainly to capture anything if not yet at steady state,
	 true parameter to force sampling even if not yet in steady state*/
	Starfish.averaging_module.sample(true);
	
	switch (output_type)
	{
	    case FIELD: writeZone2D();break;
	    case ONED: writeZone1D();break;
	    case BOUNDARIES: writeZoneBoundaries();break;
	    case PARTICLES: writeParticles();break;
		
	}
	pw.flush();
    }
	
    protected abstract void writeHeader();
    protected abstract void writeZone2D();
    protected abstract void writeZone1D();
    protected abstract void writeZoneBoundaries();
    protected abstract void writeParticles();
    public abstract void writeData(double data[]);
    
    /** closes the file*/
    public void close()
    {
	pw.close();
    }
	   	
    /** convenience method for one stop writing*/
    public final void saveBoundaries(String file_name, String[] variables)
    {
	openBoundaries(file_name,variables);
	writeZone();
	close();
    }
}
