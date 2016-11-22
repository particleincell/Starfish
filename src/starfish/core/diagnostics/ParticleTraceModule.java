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
import starfish.core.io.Writer;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;

/**Particle trace command, launched by particle_trace*/
public class ParticleTraceModule extends CommandModule
{
    @Override
    public void init() { /*do nothing*/ }

    @Override
    public void process(Element element) 
    {
	String file_name = InputParser.getValue("file_name", element);
	String format = InputParser.getValue("format", element,"TECPLOT");
	String material = InputParser.getValue("material", element);
	
	Material mat = Starfish.getMaterial(material);
	if (mat==null || !(mat instanceof KineticMaterial))
	    Log.error(String.format("Material %s not found or not a kinetic material"));
	
	int id = InputParser.getInt("id", element);
	int start_it = InputParser.getInt("start_it",element,0);
	
	tracer_list.add(new Tracer(file_name,format,(KineticMaterial) mat, id, start_it));
    }

    ArrayList<Tracer> tracer_list = new ArrayList();
    
    @Override
    public void start() { /*do nothing*/}

    @Override
    public void exit() 
    {
	for (Tracer tracer: tracer_list) tracer.close();
    }

    /** @return trace id associated with particle id, or -1 if no trace*/
    public int getTraceId(long part_id)
    {
	for (int i=0;i<tracer_list.size();i++)
	    if (tracer_list.get(i).id==part_id)
		return i;
	return -1;
    }

    /** saves new trace for the specified particle*/	
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
	
	Tracer(String file_name, String format, KineticMaterial km, int id, int start_it)
	{
	    this.id = id;
	    this.start_it = start_it;
	    writer = new TecplotWriter();
	    writer.openParticles(file_name,null);
	    this.km = km;
	}
		
	/** saves new trace for the specified particle*/
	void addTrace(Particle part)
	{
	 
	    if (part==null || Starfish.getIt()<start_it) return;
	    
	    double data[] = new double [7];
	    data[0] = part.pos[0];
	    data[1] = part.pos[1];
	    data[2] = part.pos[2];
	    data[3] = part.vel[0];
	    data[4] = part.vel[1];
	    data[5] = part.vel[2];
	    data[6] = Starfish.getIt();
	    
	    writer.writeData(data);			
	}
	
	void close() {writer.close();}
    }	
}
