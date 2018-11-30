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
import starfish.core.diagnostics.DiagnosticsModule.Diagnostic;
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
    class Averaging implements Diagnostic
    {
	int frequency=0;
	int start_it=-1;
	int counter=0;
	int last_save=-1;
	String var_list[];
		
	ArrayList<FCPair> fc_list = new ArrayList<FCPair>();

	

	@Override
	public void exit()
	{
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	class FCPair
	{
	    String var_name;
	    FieldCollection2D inst;
	    FieldCollection2D ave;
	    FCPair(String var_name, FieldCollection2D inst, FieldCollection2D ave) 
	    {this.var_name=var_name;this.inst=inst;this.ave=ave;}
	}
	    
	Averaging(Element element)
	{
	    frequency = InputParser.getInt("frequency", element,1);
	    start_it = InputParser.getInt("start_it", element,-1);	
	    var_list = InputParser.getList("variables", element);
	
	    for (String var : var_list)
	    {
		Log.log(" Added: " + var);
	    }
	}
	/**
	*
	* @param var_name
	* @return
	*/
       public FieldCollection2D getFieldCollection(String var_name) 
       {

	       for (FCPair pair:fc_list)	
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
	
    /*called by sample on first call*/
    public void init()
    {
	for (String var:var_list)
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
	    fc_list.add(new FCPair(var,fc_inst,fc_ave));
	}
    }
    
    /**adds a new sample to the averaging
     @param force_sampling set to true force sampling even if not yet in steady state*/
    public void sample(boolean force) 
    {
	int it = Starfish.getIt();
	boolean steady_state = Starfish.steady_state();
	
	if (!force)
	{
	    if ((start_it<0 && !steady_state) ||
		it<start_it) return;
	}
	
	if (last_save<0) 
	    last_save=it;
			
	if (fc_list.isEmpty())
	    init();
	
	if ((it-last_save)%frequency==0)
	{
	    for (FCPair pair:fc_list)
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
				ave[i][j] = (inst[i][j] + counter*ave[i][j])/(counter+1);
		    }
		}
		
		Log.debug(String.format("Performed averaging at it=%d with counter=%d\n",Starfish.getIt(),counter));
		counter++;
		last_save = it;
	    } /*if saving*/
	}		
    }	
	
 
	
    @Override
    public void process(Element element) 
    {
	/*create new data*/
	Averaging averaging = new Averaging(element);
	
	if (averaging.var_list.length>0) Starfish.diagnostics_module.addDiagnostic(averaging);
    }
	

}
