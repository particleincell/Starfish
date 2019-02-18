/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package starfish.core.diagnostics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import static starfish.core.common.Starfish.getIt;
import static starfish.core.common.Starfish.getMaterialsList;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.source.Source;

/**
 *
 * @author Lubos Brieda
 */
public class StatsModule extends CommandModule
{

    /**
     *
     */
    protected int stats_skip = 1;	    //frequency of file saves;
    String file_name = "starfish_stats.csv";
    PrintWriter pw;
	    
    @Override
    public void process(Element element)
    {
	stats_skip = InputParser.getInt("skip", element, stats_skip);
	file_name = InputParser.getValue("file_name",element,file_name);
    }
    
    @Override
    public void start()
    {
	try
	{
	    pw = new PrintWriter(new FileWriter(file_name));
	} catch (IOException ex)
	{
	    Log.error("Failed to open stats file "+file_name);
	}
	
	//for now only saving particle counts
	pw.printf("it,time");	
	for (Material mat:getMaterialsList())
	{
	    pw.printf(",mass.%s,mom.%s.x,mom.%s.y,mom.%s.z,energy.%s",
		    mat.name, mat.name, mat.name, mat.name, mat.name);
	    
	    //add number of macroparticles for kinetic materials
	    if (mat instanceof KineticMaterial)
	    	pw.printf(",mp.%s",mat.name);	    
	}
	
	//write out sources
	for (Boundary boundary : Starfish.getBoundaryList())
	{
	    for (Source source: boundary.getSourceList())
	    {
		pw.printf(",source.%s (kg/s)",source.getName());
		pw.printf(",source.%s (A/m^2/s)",source.getName());
	    }
	}

	pw.printf("\n");
    }

    @Override
    public void finish()
    {
	pw.close();
    }
        
    //generates screen and file output

    /**
     *
     */
    public void printStats()
    {
	if (stats_skip>0 && getIt()%stats_skip != 0)
	    return;
	    
	String msg = String.format("it: %d\t",getIt());
		
	for (Material mat:getMaterialsList())
	{
	    if (mat instanceof KineticMaterial)
	    {
		KineticMaterial km = (KineticMaterial)mat;
		msg += String.format("%s: %d ", km.getName(),km.getNp());
	    }
	}
	
	Starfish.Log.message(msg);
	
	//now save to the file
	saveStats();
    }
    
    //saves stats to the file

    /**
     *
     */
    protected int lines = 0;

    /** 
     * writes files to a log file
     */
    protected void saveStats()
    {
	pw.printf("%d,%g",getIt(),Starfish.time_module.getTime());	
	for (Material mat:getMaterialsList())
	{
	     pw.printf(",%.4g,%.4g,%.4g,%.4g,%.4g",
		    mat.getMassSum(),mat.getMomentumSum()[0],mat.getMomentumSum()[1],
		    mat.getMomentumSum()[2],mat.getEnergySum());
	    if (mat instanceof KineticMaterial)
	    {
		KineticMaterial km = (KineticMaterial)mat;
		pw.printf(",%d",km.getNp());
	    }
	}
	
		//write out sources
	for (Boundary boundary : Starfish.getBoundaryList())
	{
	    for (Source source: boundary.getSourceList())
	    {
		double mass_gen = source.getMassGeneratedInst();
		pw.printf(",%.4g",mass_gen); //mass		
		pw.printf(",%.4g",mass_gen*source.getMaterial().charge/(source.getMaterial().mass*boundary.area()*stats_skip*Starfish.getDt())); //current den
		source.clearMassGeneratedInst();
		
	    }
	}
	pw.printf("\n");
	lines++;
	if (lines>10) {pw.flush();lines=0;}
    }

}
