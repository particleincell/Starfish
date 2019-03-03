/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.util.ArrayList;
import java.util.Arrays;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;

/** handles &lt; output &gt; commands*/
public class OutputModule extends CommandModule
{
    @Override
    public void init()
    {
	/*do nothing*/
    }

    @Override
    public void process(Element element) 
    {
	/*make sure that mesh and boundary modules have started*/
	Starfish.boundary_module.start();
	Starfish.domain_module.start();
	
	Writer writer = createWriter(element);
	writer.write();
	writer.close();
    }

    /**
     *
     * @return
     */
    public static Writer createWriter1D()
    {
	throw new UnsupportedOperationException("Not yet implemented");
    }
    
    /**
     *
     * @param element
     * @return
     */
    public static Writer createWriter(Element element)
    {
	String type;
	String file_name;
	String format;
		
	try{
	    type = InputParser.getValue("type",element);
	    file_name = InputParser.getValue("file_name",element);
	    format = InputParser.getValue("format",element,"TECPLOT");
	}
	catch (Exception e)
	{
	    Log.error("Syntax <output type=\"2D/1D/BOUNDARIES/PARTICLES\" file_name=\"...\" [format=\"TECPLOT\"]>");
	    return null;
	}
		
	/*grab variables*/
	String variables[] = InputParser.getList("variables",element);
	String scalars[] = InputParser.getList("scalars",element);  //alternate name of variables		
	String cell_data[] = InputParser.getList("cell_data",element);
	ArrayList<String[]> vectors = InputParser.getListOfPairs("vectors",element);
	
	//combine "scalars" and "variables", keeping both for backwards compatibility
	ArrayList<String> vars = new ArrayList();
	vars.addAll(Arrays.asList(variables));
	vars.addAll(Arrays.asList(scalars));
	variables = vars.toArray(new String[0]);
	
	/*make writer*/
	Writer writer;
	if (format.equalsIgnoreCase("TECPLOT")) writer = new TecplotWriter(element);
	else if (format.equalsIgnoreCase("VTK")) writer = new VTKWriter(element);
	else {Log.error("Unknown output format "+format);return null;}
		 
	
	/*TODO: replace this with "factories"*/
	/*output data*/
	if (type.equalsIgnoreCase("2D"))
	    writer.init2D(variables,vectors,cell_data,element);
	else if (type.equalsIgnoreCase("1D"))
	    writer.init1D(variables,vectors,cell_data,element);
	else if (type.equalsIgnoreCase("3D"))
	    writer.init3D(variables,vectors,cell_data,element);
	else if (type.equalsIgnoreCase("BOUNDARIES"))
	    writer.initBoundaries(variables,element);
	else if (type.equalsIgnoreCase("PARTICLES"))
	    writer.initParticles(element);
	else
	    Log.error("Unknown output type "+type);

	return writer;	
}

    /*TODO: this doesn't work properly for averaged values*/
    /** checks if variable exists and if not, prints an error message and terminate
     * @param var
     * @return s*/
    public boolean validateVar(String var)
    {
	String pieces[] = var.split("\\.");

	String base = pieces[0];
	String species = null;

	if (pieces.length>1)
	    species = pieces[1];

	if (species!=null)
	{	
	    if (Starfish.materials_module.hasMaterial(species)==false)
	    {Log.warning("unrecognized species "+species+" while processing output variable "+var);return false;}
	    if (Starfish.getMaterial(species).getFieldManager1d().hasField(base)==false && 
		Starfish.getMaterial(species).getFieldManager2d().hasField(base)==false)
	    {Log.warning("unrecognized variable "+base+" in "+var);return false;}
	    return true;
	}
	else	/*not a species var*/
	{
	    if (Starfish.boundary_module.getFieldManager().hasField(var)==false && 
		Starfish.domain_module.getFieldManager().hasField(var)==false)
	    {Log.warning("unrecognized variable "+var);return false;}
	    return true;
	}	
    }

    @Override
    public void exit() 
    {
	/*close files?*/
    }

    @Override
    public void start() 
    {
	/**/
    }
}
