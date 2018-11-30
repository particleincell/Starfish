/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.diagnostics;

import java.io.PrintWriter;
import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;
import starfish.core.io.TecplotWriter;
import starfish.core.io.VTKWriter;
import starfish.core.io.Writer;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;

/**Particle trace command, launched by particle_trace*/
public class ParticleTracer extends CommandModule
{
    @Override
    public void init() { /*do nothing*/ }

    @Override
    public void process(Element element) 
    {
	String file_name = InputParser.getValue("file_name", element);
	String format = InputParser.getValue("format", element,"VTK");
	String material = InputParser.getValue("material", element);
	
	Material mat = Starfish.getMaterial(material);
	if (mat==null || !(mat instanceof KineticMaterial))
	    Log.error(String.format("Material %s not found or not a kinetic material"));
	
	int id = InputParser.getInt("id", element);
	int start_it = InputParser.getInt("start_it",element,0);
	
	tracer_list.add(new Tracer(file_name,format,(KineticMaterial) mat, id, start_it));
    }

    ArrayList<Tracer> tracer_list = new ArrayList<Tracer>();
    
    @Override
    public void start() { /*do nothing*/}

    @Override
    public void exit() 
    {
	for (Tracer tracer: tracer_list) tracer.close();
    }

    /**
     * @param part_id *  
     * @return trace id associated with particle id, or -1 if no trace*/
    public int getTraceId(long part_id)
    {
	for (int i=0;i<tracer_list.size();i++)
	    if (tracer_list.get(i).id==part_id)
		return i;
	return -1;
    }

    /** saves new trace for the specified particle
     * @param part*/	
    public void addTrace(Particle part)
    {
	if (part.trace_id>=0)
	    tracer_list.get(part.trace_id).addTrace(part);
    }
	
    class Tracer
    {
	int id;		/** particle id*/
	int start_it;	/** iteration to start sampling at */
	PrintWriter pw = null;
	Writer writer;
	KineticMaterial km;	
	ArrayList<Particle> particles = new ArrayList();
	ArrayList<Integer> time_steps = new ArrayList();
	
	Tracer(String file_name, String format, KineticMaterial km, int id, int start_it)
	{
	    this.id = id;
	    this.start_it = start_it;
	    if (format.equalsIgnoreCase("VTK")) 
		writer = new VTKWriter(file_name);
	    else if (format.equalsIgnoreCase("TECPLOT"))
		writer = new TecplotWriter(file_name);
	    else
		Log.error("Unsuported writer format "+format);
	    writer.initTrace();
	    this.km = km;	    
	}
		
	/** saves new trace for the specified particle*/
	void addTrace(Particle part)
	{	 	    
	    if (part==null || Starfish.getIt()<start_it) return;
	    particles.add(new Particle(part));
	    time_steps.add(Starfish.getIt());	
	    
	    //write every 25 time steps, this needs to be a user input
	    if (Starfish.getIt()%25==0)
		writer.writeTrace(particles,time_steps);
	}
	
	void close() {writer.writeTrace(particles, time_steps);writer.close();}
    }	
}
