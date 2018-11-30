/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.diagnostics;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.io.InputParser;
import starfish.core.io.OutputModule;
import starfish.core.io.Writer;
import starfish.core.materials.Material;

/** animation support */
public class AnimationModule extends CommandModule 
{
    public class Animation implements DiagnosticsModule.Diagnostic
    {
	int start_it=0;
	int frequency=0;
	boolean clear_samples;
	ArrayList<Writer> writer_list = new ArrayList<Writer>();
	Element output_list[];
	boolean first_time = true;
	
	public Animation(Element element)
	{
	    start_it = InputParser.getInt("start_it", element);
	    frequency = InputParser.getInt("frequency", element);		
	    output_list = InputParser.getChildren("output", element);   
	    clear_samples = InputParser.getBoolean("clear_samples",element,true);   /*resets collected data post write*/
	}
	
	/*writes out new animation file*/
	@Override
	public void sample(boolean force) 
	{
	    int it = Starfish.getIt();

	    if (force || (it>= start_it && (it-start_it)%frequency==0))
	    {
		if (first_time)
		{
		    for (Element output:output_list)
			writer_list.add(OutputModule.createWriter(output));
		    first_time=false;
		}
		
		for (Writer writer:writer_list)
		{
		    writer.write(true);						    
		}
		
		if (clear_samples)
		{
		    /*reset collected velocity moments*/
		    for (Material mat:Starfish.getMaterialsList())
			mat.clearSamples();
		}
		    
	    }
	}	

	/*closes files*/
	@Override
	public void exit()
	{
	    for (Writer writer:writer_list)
		writer.close();
	}
    };	//animation

    	
    @Override
    public void process(Element element) 
    {
	/*create new animation*/
	Animation anim = new Animation(element);
	Starfish.diagnostics_module.addDiagnostic(anim);
    }
	
}
