/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

/** \page sources Sources
 * 
 * Sources contain
 * \subpage MaxwellianSource
 * 
 */
package starfish.core.source;

import starfish.sources.MaxwellianSource;
import starfish.sources.AmbientDensitySource;
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
import starfish.core.materials.Material;
import starfish.sources.CosineSource;
import starfish.sources.UniformSource;

/** Handles \<source\> command */
public class SourceModule extends CommandModule
{
    protected ArrayList<VolumeSource> volume_source_list = new ArrayList();

    @Override
    public void init()
    {
	
	/*surface sources*/
	registerSurfaceSource("UNIFORM",UniformSource.uniformSourceFactory);
	registerSurfaceSource("MAXWELLIAN",MaxwellianSource.maxwellianSourceFactory);
	registerSurfaceSource("AMBIENT",AmbientSource.ambientSourceFactory);
	registerSurfaceSource("AMBIENT_DENSITY",AmbientDensitySource.ambientDensitySourceFactory);
	registerSurfaceSource("COSINE",CosineSource.cosineSourceFactory);
	
	/*volume sources*/
	registerVolumeSource("PRELOAD",VolumePreloadSource.preloadSourceFactory);
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
    
    static public void registerSurfaceSource(String name, SurfaceSourceFactory fac)
    {
	surface_source_factories.put(name.toUpperCase(),fac);
	Log.log("Added surface source "+name.toUpperCase());
    }
   
    static public void registerVolumeSource(String name, VolumeSourceFactory fac)
    {
	volume_source_factories.put(name.toUpperCase(),fac);
	Log.log("Added volume source "+name.toUpperCase());
    }
   
    static public HashMap<String,SurfaceSourceFactory> surface_source_factories = new HashMap();
    static public HashMap<String,VolumeSourceFactory> volume_source_factories = new HashMap();
    
    /**interfaces for source factories*/
    public interface SurfaceSourceFactory 
    {
	public void makeSource(Element element, String name, Boundary boundary, Material material);
    }

    public interface VolumeSourceFactory
    {
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

	/*get boundary for boundary source*/
	String boundary_name = null;
	
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
    /*TODO move this to flying material*/
    public void sampleSources()
    {
	/*surface sources*/
	for (Boundary boundary : Starfish.getBoundaryList())
	{
	    for (Source source : boundary.getSourceList())
	    {
		//if (source instanceof ParticleListSource) continue;
		if (Starfish.getIt()<source.start_it) continue;
		source.update();
		source.regenerate();
		source.sampleAll();
	    }/*for source*/
	}

	/*volume sources*/
	for (Source source : volume_source_list)
	{
	    source.update();
	    source.regenerate();
	    source.sampleAll();
	}/*for volume source*/

    }

    /**
     * adds new volume source
     */
    public void addVolumeSource(VolumeSource vol_source)
    {
	volume_source_list.add(vol_source);
    }

    @Override
    public void exit()
    {
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
