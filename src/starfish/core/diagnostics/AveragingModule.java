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
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;

/**
 *
 * @author Lubos Brieda
 */
public class AveragingModule extends CommandModule
{
    class AveData
    {
	int frequency=0;
	int start_it=-1;
	int counter=0;
	int last_save=-1;
	String var_list[];
		
	ArrayList<FCPair> fc_list = new ArrayList<FCPair>();
    }
	
    class FCPair
    {
	String var_name;
	FieldCollection2D inst;
	FieldCollection2D ave;
	FCPair(String var_name, FieldCollection2D inst, FieldCollection2D ave) 
	{this.var_name=var_name;this.inst=inst;this.ave=ave;}
    }
	
    ArrayList<AveData> ave_data = new ArrayList<AveData>();
	
    /**
     *
     * @param var_name
     * @return
     */
    public FieldCollection2D getFieldCollection(String var_name) 
    {
	for (AveData ave:ave_data)
	    for (FCPair pair:ave.fc_list)	
	    {
		if (pair.var_name.equalsIgnoreCase(var_name))
		    return pair.ave;
	    }
	return null;
    }
	
    /**
     *
     * @param mesh
     * @param var_name
     * @return
     */
    public Field2D getField(Mesh mesh, String var_name) 
    {
	return getFieldCollection(var_name).getField(mesh);
    }
	
    @Override
    public void process(Element element) 
    {
	/*create new data*/
	AveData data = new AveData();
	
	data.frequency = InputParser.getInt("frequency", element,1);
	data.start_it = InputParser.getInt("start_it", element,-1);	
	data.var_list = InputParser.getList("variables", element);
	
	for (int i=0;i<data.var_list.length;i++)
	    Log.log(" Added: "+data.var_list[i]);
	
	if (data.var_list.length>0) ave_data.add(data);
    }
	
    @Override
    public void start()
    {
	for (AveData data:ave_data)
	    for (String var:data.var_list)
	    {
		FieldCollection2D fc_inst = Starfish.getFieldCollection(var);
		if (fc_inst==null)
		{	
		    Log.warning("unknown variable "+var);
		    continue;
		}
			
		FieldCollection2D fc_ave = new FieldCollection2D(fc_inst);
				
		/*set variable name*/
		String pieces[] = var.split("\\.");
		var = pieces[0]+"-ave";
		if (pieces.length>1) 
		    var+="."+pieces[1];
			
		/*add to list*/
		data.fc_list.add(new FCPair(var,fc_inst,fc_ave));
	    }	
	}

    /**adds a new sample to the averaging*/
    public void sample() 
    {
	sample(false);
    }
    
    /**adds a new sample to the averaging
     @param force_sampling set to true force sampling even if not yet in steady state*/
    public void sample(boolean force_sampling) 
    {
	int it = Starfish.getIt();
	boolean steady_state = Starfish.steady_state();
	
	for (AveData data:ave_data)
	{
	    if (!force_sampling)
	    {
		if ((data.start_it<0 && !steady_state) ||
		    it<data.start_it) return;
	    }
	
	    if (data.last_save<0) 
		data.last_save=it;
			
	    if ((it-data.last_save)%data.frequency==0)
	    {
		for (FCPair pair:data.fc_list)
		{
		    /*iterate over meshes*/
		    FieldCollection2D fc_inst = pair.inst;
		    FieldCollection2D fc_ave = pair.ave;
					
		    for (Mesh mesh:Starfish.getMeshList())
		    {
			/*save average data*/
			double ave[][] = fc_ave.getField(mesh).getData();
			double inst[][] = fc_inst.getField(mesh).getData();
			for (int i=0;i<mesh.ni;i++)
			    for (int j=0;j<mesh.nj;j++)
				ave[i][j] = (inst[i][j] + data.counter*ave[i][j])/(data.counter+1);
		    }
		}
		
		Log.debug(String.format("Performed averaging at it=%d with counter=%d\n",Starfish.getIt(),data.counter));
		data.counter++;
		data.last_save = it;
	    } /*if saving*/
	}		
    }	
}
