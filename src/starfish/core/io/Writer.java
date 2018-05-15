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
import starfish.core.materials.KineticMaterial.Particle;

/**
 *
 * @author Lubos Brieda
 */
public abstract class Writer 
{
    static enum OutputType {FIELD,ONED,BOUNDARIES,PARTICLES,TRACE};
    static enum Dim {I,J};
	
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
    String mat_name;
	
    /*general constructor*/
    public Writer (String file_name)
    {
	this.file_name = file_name;	
    }
    
    protected PrintWriter open(String file_name)
    {
	PrintWriter pw = null;
	
	try {
	    pw = new PrintWriter(new FileWriter(file_name));
	} catch (IOException ex) 
	{
	    Log.error("error opening file "+file_name);
	}
	
	return pw;
    }
    
    /** Outputs data along a specified I or J index in a single mesh
     *
     * @param scalars
     * @param vectors
     * @param cell_data
     * @param element XML element
     */
    public void init1D(String[] scalars, ArrayList<String[]> vectors, String[] cell_data, 
					    Element element)
    {
	String mesh_name = InputParser.getValue("mesh",element);
	String str_index = InputParser.getValue("index",element);
	    
	/*grab the mesh*/
	output_mesh =Starfish.domain_module.getMesh(mesh_name);
	if (output_mesh==null)
	{
	    Log.error("Mesh "+mesh_name+" does not exist");
	    return;
	}
	
	/*parse index*/
	String pieces[]=str_index.split("\\s*=\\s*");
	if (pieces.length!=2)
	    Log.error(String.format("couldn't parse index string %s, syntax [I/J]=value",index));
		
	if (pieces[0].equalsIgnoreCase("I"))
	    dim = Dim.I;
	else if (pieces[0].equalsIgnoreCase("J"))
	    dim = Dim.J;
	else Log.error("Unknown dimension "+pieces[0]);
    
	this.index=Integer.parseInt(pieces[1]);		
		
	/*call main open function*/
	init2D(scalars, vectors, cell_data,element);	
	
	/*set output type - after init2D*/
	output_type=OutputType.ONED;
    }
	    	
    /** open function for 2D field data
     * @param scalars
     * @param vectors
     * @param cell_data
     * @param element xml data element*/ 
    protected void init2D(String[] scalars, ArrayList<String[]> vectors, 
	    String[] cell_data, Element element)
    {
	    output_type=OutputType.FIELD;

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
    }
	
    /** open function for boundary data
     * @param variables
     * @param element xml element*/
    protected void initBoundaries(String[] variables, Element element)
    {
	    output_type = OutputType.BOUNDARIES;

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
    }

    /** open function for particle output
     * @param element*/
    public void initParticles(Element element)
    {
	    output_type=OutputType.PARTICLES;

	    particle_count = InputParser.getInt("count", element,1000);
	    mat_name = InputParser.getValue("material", element);

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
    }

     /** open function for particle output
     * @param element*/
    public void initTrace()
    {
	output_type = OutputType.TRACE;
    }

    /** Wrapper for default parameter
     *
     */
    public final void write() {write(false);}

    /** Writes latest data to a file
     *
     * @param animation
     */
    public final void write(boolean animation)
    {
	/*update average data, this is needed mainly to capture anything if not yet at steady state,
	 true parameter to force sampling even if not yet in steady state*/
	Starfish.averaging_module.sample(true);
	
	switch (output_type)
	{
	    case FIELD: write2D(animation);break;
	    case ONED: write1D(animation);break;
	    case BOUNDARIES: writeBoundaries(animation);break;
	    case PARTICLES: writeParticles(animation);break;
	    case TRACE: Log.warning("write not supported for TRACE");break;		
	}	
    }

    /**
     *
     * @param animation
     */
    protected abstract void write2D(boolean animation);

    /**
     *
     */
    protected abstract void write1D(boolean animation);

    /**
     *
     */
    protected abstract void writeBoundaries(boolean animation);

    /**
     *
     */
    protected abstract void writeParticles(boolean animation);
    
    /**
     *
     */
    public abstract void writeTrace(ArrayList<Particle> particles, ArrayList<Integer>time_steps);
    

    /** placeholder for file close out operations to be overriden as needed*/
    public void close()
    {
	
    }
	   	    
    /** convenience method for one stop writing
    * @param file_name
     * @param scalars
     * @param vectors
     * @param cell_data
    * */
    public final void saveMesh(String[] scalars, ArrayList<String[]> vectors, String[] cell_data)
    {
	    init2D(scalars,vectors,cell_data,null);
	    write();
	    close();
    }
}
