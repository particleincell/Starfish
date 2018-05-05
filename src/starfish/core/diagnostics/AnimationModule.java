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

/** animation support */
public class AnimationModule extends CommandModule 
{
    class AnimData
    {
	int start_it=0;
	int frequency=0;
	ArrayList<Writer> writer_list = new ArrayList<Writer>();
	Element output_list[];
	boolean first_time = true;
    }
	
    ArrayList<AnimData> anim_data = new ArrayList<AnimData>();
	
    @Override
    public void process(Element element) 
    {
	/*create new data*/
	AnimData data = new AnimData();
		
	data.start_it = InputParser.getInt("start_it", element);
	data.frequency = InputParser.getInt("frequency", element);
		
	 data.output_list = InputParser.getChildren("output", element);
	 anim_data.add(data);
    }

    /**
     *
     */
    public void save() 
    {
	int it = Starfish.getIt();
		
	for (AnimData data:anim_data)
	{
	    if (it>= data.start_it && (it-data.start_it)%data.frequency==0)
	    {
		if (data.first_time)
		{
		    for (Element output:data.output_list)
			data.writer_list.add(OutputModule.createWriter(output));
		    data.first_time=false;
		}
		
		for (Writer writer:data.writer_list)
		    writer.write(true);
	    }
	}		
    }

    @Override 
    public void exit()
    {
	for (AnimData data:anim_data)
	    for (Writer writer:data.writer_list)
		writer.close();
    }
	
}
