/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

/** sources Sources
 * 
 * Sources contain
 * 
 * 
 */
package starfish.core.source;

import starfish.sources.MaxwellianSource;
import starfish.sources.VolumePreloadSource;
import starfish.sources.AmbientSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;
import starfish.core.io.LoggerModule.Level;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.sources.CosineSource;
import starfish.sources.ThermionicEmissionSource;
import starfish.sources.UniformSource;
import starfish.sources.VaporizationSource;
import starfish.sources.VolumeMaxwellianSource;

/** Handles &lt; source\ &gt; command */
public class SourceModule extends CommandModule
{

    /**
     *
     */
    protected ArrayList<VolumeSource> volume_source_list = new ArrayList();


    /** boundary charge for the circuit model    */
    public double boundary_charge = 0;
    
    @Override
    public void init()
    {
	
	/*surface sources*/
	registerSurfaceSource("UNIFORM",UniformSource.uniformSourceFactory);
	registerSurfaceSource("MAXWELLIAN",MaxwellianSource.maxwellianSourceFactory);
	registerSurfaceSource("AMBIENT",AmbientSource.ambientSourceFactory);
	registerSurfaceSource("COSINE",CosineSource.cosineSourceFactory);
	registerSurfaceSource("THERMIONIC",ThermionicEmissionSource.thermionicEmissionSource);
	registerSurfaceSource("VAPORIZATION",VaporizationSource.vaporizationSource);
		
	/*volume sources*/
	registerVolumeSource("PRELOAD",VolumePreloadSource.preloadSourceFactory);
	registerVolumeSource("MAXWELLIAN",VolumeMaxwellianSource.volumeMaxwellianSourceFactory);
    }

    @Override
    public void process(Element element)
    {
	/*process mesh commands*/
	Iterator<Element> iterator = InputParser.iterator(element);

	while (iterator.hasNext())
	{
	    Element el = iterator.next();
	    if (el.getNodeName().equalsIgnoreCase("BOUNDARY_SOURCE"))
	    {
		NewSurfaceSource(el, SurfaceSourceType.BOUNDARY_SOURCE);
	    } else if (el.getNodeName().equalsIgnoreCase("MATERIAL_SOURCE"))
	    {
		NewSurfaceSource(el, SurfaceSourceType.MATERIAL_SOURCE);
	    } else if (el.getNodeName().equalsIgnoreCase("VOLUME_SOURCE"))
	    {
		NewVolumeSource(el);
	    } else
	    {
		Log.warning("Unknown source type " + el.getNodeName());
	    }
	}
    }

    enum SurfaceSourceType {BOUNDARY_SOURCE, MATERIAL_SOURCE};
    
    /**
     *
     * @param name
     * @param fac
     */
    static public void registerSurfaceSource(String name, SurfaceSourceFactory fac)
    {
	surface_source_factories.put(name.toUpperCase(),fac);
	Log.log("Added surface source "+name.toUpperCase());
    }
   
    /**
     *
     * @param name
     * @param fac
     */
    static public void registerVolumeSource(String name, VolumeSourceFactory fac)
    {
	volume_source_factories.put(name.toUpperCase(),fac);
	Log.log("Added volume source "+name.toUpperCase());
    }
   
    /**
     *
     */
    static public HashMap<String,SurfaceSourceFactory> surface_source_factories = new HashMap<String,SurfaceSourceFactory>();

    /**
     *
     */
    static public HashMap<String,VolumeSourceFactory> volume_source_factories = new HashMap<String,VolumeSourceFactory>();
    
    /**interfaces for source factories*/
    public interface SurfaceSourceFactory 
    {

	/**
	 *
	 * @param element
	 * @param name
	 * @param boundary
	 * @param material
	 */
	public void makeSource(Element element, String name, Boundary boundary, Material material);
    }

    /**
     *
     */
    public interface VolumeSourceFactory
    {

	/**
	 *
	 * @param element
	 * @param name
	 * @param material
	 */
	public void makeSource(Element element, String name, Material material);
    }
    
    /**handlers for boundary sources*/
    void NewSurfaceSource(Element element, SurfaceSourceType source_type)
    {
	/*get name*/
	String name = InputParser.getValue("name", element);

	/*get type*/
	String type = InputParser.getValue("type", element);

	/*read material, spline, mdot, must be defined for all*/
	Material material = null;

	try
	{
	    material = Starfish.getMaterial(InputParser.getValue("material", element));
	} catch (NoSuchElementException e)
	{
	    Log.error("Unknown flying material for source " + name);
	}

	/*get boundary for boundary source*/
	String boundary_name;
	Boundary boundary;
	if (source_type == SurfaceSourceType.BOUNDARY_SOURCE)
	{
	    boundary_name = InputParser.getValue("boundary", element);
	    boundary = Starfish.getBoundary(boundary_name);
	} else
	{
	    /*TODO: Need to add the source for each boundary*/
	    return;
	}

	/*create source*/
	SurfaceSourceFactory fac = surface_source_factories.get(type.toUpperCase());
	
	if (fac!=null)
	    fac.makeSource(element, name, boundary, material);
	 else
	    Log.log(Level.ERROR, "Unrecognized source type " + type);
	
    }

    /*handler for volume sources*/
    void NewVolumeSource(Element element)
    {
	/*get name*/
	String name = InputParser.getValue("name", element);

	/*get type*/
	String type = InputParser.getValue("type", element);

	/*read material, spline, mdot, must be defined for all*/
	Material material = null;

	try
	{
	    material = Starfish.getMaterial(InputParser.getValue("material", element));
	} catch (NoSuchElementException e)
	{
	    Log.error("Unknown flying material for source " + name);
	}
	
	/*create source*/
	VolumeSourceFactory fac = volume_source_factories.get(type.toUpperCase());
	
	if (fac!=null)
	    fac.makeSource(element, name, material);
	 else
	    Log.log(Level.ERROR, "Unrecognized source type " + type);
    }

    /**
     * samples sources and adds particles or updates boundaries
     */
    public void sampleSources()
    {
	/*surface sources*/
	for (Boundary boundary : Starfish.getBoundaryList())
	{
	    for (Source source : boundary.getSourceList())
	    {
		if (source.source_mat.frozen) continue;
		
		//if (source instanceof ParticleListSource) continue;
		if (Starfish.getIt()<source.start_it ||
		    source.stop_it>=0 && Starfish.getIt()>source.stop_it) continue;
		
		//circuit to model to inject electrons lost to the wall
		int num_mp_delta = 0;
				
		if (source.circuit_model && boundary_charge<0 && source.getMaterial().charge<0)
		{
		    KineticMaterial km = (KineticMaterial)source.getMaterial();
		    double spwt = km.getSpwt0();
			
		    //negative since electrons contribute negative flux
		    num_mp_delta = (int)(boundary_charge/(km.charge*spwt));
		    if (num_mp_delta>0)
		    {
		//	System.out.printf("Reinjecting %d electrons\n",num_mp_delta);
		    }
		    boundary_charge-=num_mp_delta*km.charge*spwt;
		}
		
		source.update();
		source.regenerate();
		
		//part of the above circuit model
		source.num_mp += num_mp_delta;
		
		source.sampleAll();
		
		
	    }/*for source*/
	}

	/*volume sources*/
	for (Source source : volume_source_list)
	{
	    if (source.source_mat.frozen) continue;

	    source.update();
	    source.regenerate();
	    source.sampleAll();
	}/*for volume source*/

    }

    /**
     * adds new volume source
     * @param vol_source
     */
    public void addVolumeSource(VolumeSource vol_source)
    {
	volume_source_list.add(vol_source);
    }

    @Override
    public void exit()
    {
	Log.log("Source Summary: ");
	/*initialize surface sources*/
	for (Boundary boundary:Starfish.getBoundaryList())
	{
	    for (Source source:boundary.getSourceList())
	    {
		double delta_t = Starfish.time_module.getTimeElapsed();
		double mdot = source.getMassGenerated()/delta_t;
		double flux = mdot / boundary.area();
		double current = mdot * source.source_mat.charge / source.source_mat.mass;
		double current_den = current / boundary.area();
		Log.log(String.format("%s: mdot: %.3g (kg/s), mass flux: %.3g (kg/s/m^2), "
			+ "current: %.3g (A), current density: %.3g (A/m^2)",source.name,mdot,flux,current,current_den));
	    }
	}
	
	/*init volume sources*/
	for (Source source:volume_source_list)
	    source.start();
    }

    @Override
    public void start()
    {
	/*initialize surface sources*/
	for (Boundary boundary:Starfish.getBoundaryList())
	{
	    for (Source source:boundary.getSourceList())
		source.start();
	}
	
	/*init volume sources*/
	for (Source source:volume_source_list)
	    source.start();
	    
    }
}
