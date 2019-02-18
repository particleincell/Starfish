/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.source;

import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;
import starfish.core.materials.FluidMaterial;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.materials.SolidMaterial;

/** Base class for particle sources*/
public abstract class Source
{
    /*variables*/


    protected Material source_mat;

    protected double mdot0;

    protected Boundary boundary;

    protected String name;
    public int start_it;
    public int stop_it;
    
    double mass_generated;	//total mass generated
    double mass_generated_inst; //instantenous mass generated
    
    public double getMassGenerated() {return mass_generated;}
    public double getMassGeneratedInst() {return mass_generated_inst;}
    public void clearMassGeneratedInst() {mass_generated += mass_generated_inst;mass_generated_inst=0;} //called from stats

    
    /*temporary implementation of a circuit model*/

    /**
     *
     */

    public boolean circuit_model;
   
    /**
     * constructor
     * @param name
     * @param source_mat
     * @param boundary
     * @param element
     */   
    public Source(String name, Material source_mat, Boundary boundary, Element element)
    {
	this(name,source_mat,boundary);
	start_it = InputParser.getInt("start_it", element, 0);
	stop_it = InputParser.getInt("stop_it",element, -1);
	/*for backwards compatibility*/
	stop_it = InputParser.getInt("end_it",element,stop_it);
    }
    /**
     * constructor without xml element
     * @param name
     */
    public Source(String name, Material source_mat, Boundary boundary)
    {
	this.source_mat = source_mat;
	this.boundary = boundary;
	this.name = name; 	
    }
    
    public void start()
    {
	/*do nothing*/
    }
    
    /**
     *
     */
    protected int num_mp;	/*number of macroparticles to sample*/

    /**
     *
     */
    protected double mp_rem;


    /**
     * returns true if there are more particles to sample
     * @return 
     */
    public boolean hasParticles()
    {
	if (num_mp > 0)
	{
	    return true;
	}
	return false;
    }

    /*returns mass flow rate at the given time, to be overridden as needed*/
    public double mdot(double time)
    {
//	int p=(int)(time/400e-6);
//	double t=(time-p*400e-6)/1e-6;	/*in us*/
//	return mdot0*60*Math.exp(-0.013*t)/11.47 ;
	return mdot0;
    }
    
    /**
     * default function to reset particle sample size, called prior to sampling source
     * this can be overridden, but the default hook should be update()
     */
    public void regenerate()
    {
	if (source_mat instanceof KineticMaterial)
	{
	    KineticMaterial km = (KineticMaterial) source_mat;
	    double mp =  (mdot(Starfish.time_module.getTime()) * Starfish.getDt()) / (km.getMass() * km.getSpwt0()) + mp_rem;
	    num_mp = (int) mp;
	    mp_rem = mp-num_mp;
	} else
	{
	    num_mp = 0;		/*for now?*/
	    mp_rem = 0;
	}
    }
    
    /**called prior to regenerate, allows sources to update mass flow rates, etc..*/
    public void update()
    {
	/*do nothing*/
    }

    /**
     * returns a new particle
     * @return 
     */
    public abstract Particle sampleParticle();

    /** samples the source for either particles or fluid*/
    final void sampleAll()
    {	
	/*kinetic material*/
	if (source_mat instanceof KineticMaterial)
	{
	    sampleKinetic();
	} else if (source_mat instanceof FluidMaterial)
	{
	    sampleFluid();
	} else if (!(source_mat instanceof SolidMaterial))
	{
	//    Log.error("Attempt to sample material of type " + source_mat.getClass().getName());
	}
    }

    /**
     * samples particles from a source, declared as final so that sources can't override 
     * (instead should override sampleParticle and hasParticle)
     */
    final void sampleKinetic()
    {
	/*source material*/
	KineticMaterial ks = (KineticMaterial) source_mat;

	/*do we have any particles?*/
	if (hasParticles() == false)
	{
	    return;
	}

	int count = 0;
	while (hasParticles())
	{
	    Particle part = sampleParticle();

	    /*only add particles located within our domain*/
	    if (part!=null && ks.addParticle(part))
	    {
		count++;
		mass_generated_inst+=part.spwt*source_mat.mass;
	    }
	}

	
	Log.log_low("Added " + count + " " + ks.getName() + " particles from " + getName());
    }

    /**
     * updates boundaries for fluid-based species
     */
    public abstract void sampleFluid();

    /**
     *
     * @return
     */
    public Material getMaterial()
    {
	return source_mat;
    }
    
    /**
     *
     * @return
     */
    public String getName() {return name;}
}
