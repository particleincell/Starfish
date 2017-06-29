/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.core.common.Starfish.Log;
import static starfish.core.common.Starfish.solver_module;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.MeshData;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;

public  class RestartModule extends CommandModule
{
    int it_save;
    int nt_add;
    boolean load_restart;
    boolean save_restart;
	
    int it_last_save;
	
    @Override
    public void init()
    {
	    /*do nothing*/
    }

    @Override
    public void process(Element element) 
    {
	it_save = InputParser.getInt("it_save",element,500);
	nt_add = InputParser.getInt("nt_add", element,-1);
		
	load_restart = InputParser.getBoolean("load",element,false);
	save_restart = InputParser.getBoolean("save",element,false);
    }

    @Override
    public void start() {
	/*do nothing*/
    }

    @Override
    public void exit() 
    {
	/*call save to capture latest data*/
	if (it_last_save != Starfish.getIt())
	    save(true);
    }
	
	
    public void save()
    {
	save(false);
    }
	
    public void save(boolean ignore_it)
    {
	if (!save_restart || it_last_save==Starfish.getIt())
	    return;
				
	if (Starfish.getIt()%it_save!=0 && ignore_it==false) return;
		
	Log.message("Saving restart data");
	it_last_save=Starfish.getIt();
		
	try{
	    saveRestartData();
	} catch (IOException e) 
	{
	    Log.error("Failed to save restart data to restart.bin");
	}
    }
	
    public void load()
    {
	if (!load_restart) return;
			
	Log.message("Loading restart data");
	try{
		loadRestartData();
	    } catch (IOException e) 
	{
	    Log.error("Failed to load restart data from restart.bin");
	}
		
	/*update number of times steps*/
	if (nt_add>0)
	    Starfish.time_module.setNumIt(Starfish.getIt()+nt_add);
    }

    /**saves restart data
    * At present only saves particle positions
    * TODO: add save for fields and fluid materials
    * @throws IOException 
    */
    protected void saveRestartData() throws IOException  
    {
	DataOutputStream out=null;
	out = new DataOutputStream(new FileOutputStream("restart.bin"));
	
	out.writeInt(Starfish.getIt());
		
	/*TODO: replace with a system checksum*/
	out.writeInt(getCheckSum());	/*number of materials*/
		
	for (Material mat:Starfish.getMaterialsList())
	{
	    mat.saveRestartData(out);		
	}
	out.close();
    }
	
    /**loads restart data
    * At present only loads particle positions
    * TODO: non-kinetic mats
    * @throws IOException 
    */
    protected void loadRestartData() throws IOException  
    {
	DataInputStream in=null;
	in = new DataInputStream(new FileInputStream("restart.bin"));
	
	Starfish.time_module.setIt(in.readInt());
		
	/*TODO: replace with a system checksum*/
	if (in.readInt() != getCheckSum())
		Log.error("Incompatible restart file");
		
	for (Material mat:Starfish.getMaterialsList())
	{
	    mat.loadRestartData(in);
	}
	
	in.close();
    }

    /*system checksum*/
    int getCheckSum()
    {
	/*TODO: implement a more rigorous checksum*/
	return Starfish.getMaterialsList().size();
    }	
}
