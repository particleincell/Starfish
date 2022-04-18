package starfish.plugins.surface_processing;

/* *****************************************************
 * (c) 2019 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/


import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.common.Starfish;
import starfish.core.common.Utils;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.common.Vector;
import starfish.core.common.Starfish.Log;
import starfish.core.source.Source;
import starfish.core.source.SourceModule;

/** Source that samples Maxwellian VDF*/
public class DropletSource extends Source
{

    final double v_drift;
    final double v_th;			/*thermal velocity*/
    final double diam_range[];

    /**
     *
     * @param name
     * @param source_mat
     * @param boundary
     * @param mdot
     * @param v_drift
     * @param temp
     * @param start_it
     * @param end_it
     */
    public DropletSource(String name, Material source_mat, Boundary boundary, Element element)
    {
		super(name, source_mat, boundary, element);
	
		mdot0 = Double.parseDouble(InputParser.getValue("mdot", element));
	
		/*drift velocity and temperature*/
		v_drift = Double.parseDouble(InputParser.getValue("v_drift", element));
		double temp = Double.parseDouble(InputParser.getValue("temperature", element));
		
		//get diameter range
		diam_range = InputParser.getDoubleList("diam_range", element);
		if (diam_range.length!=2) 
			Log.error("Exactly two values need to be specified for diam_range");
	
		/*calculate density*/
		double A = boundary.area();
		v_th = Utils.computeVth(temp, source_mat.getMass());	
		
		if (!(source_mat instanceof GrainMaterial))
			Log.error("Source material must of type 'grain'");
		
		//make sure we have source density
		if (source_mat.density<=0) Log.error("Density not defined for material "+name);
		
		Starfish.Log.log("Added DROPLET source '" + name + "'");
		Starfish.Log.log("> mdot   = " + mdot0 + "(kg/s)");
		Starfish.Log.log("> spline  = " + boundary.getName());
		Starfish.Log.log("> temp  = " + temp);
		Starfish.Log.log("> v_drift  = " + v_drift);
		Starfish.Log.log("> v_th  = " + v_th);
		Starfish.Log.log("> start_it  = " + start_it);
		Starfish.Log.log("> end_it  = " + stop_it);
    }

    @Override
    public Particle sampleParticle()
    {
		Particle part = new Particle((KineticMaterial)source_mat);
		double t = boundary.randomT();
		
		double x[] = boundary.pos(t);
		double n[] = boundary.normal(t);
	
		/*copy values*/
		part.pos[0] = x[0];
		part.pos[1] = x[1];
		part.pos[2] = 0;
	
		do
		{
		    double v_max[] = Utils.SampleMaxw3D(v_th);
	
		    /*add drift*/
		    part.vel[0] = v_max[0] + n[0] * v_drift;
		    part.vel[1] = v_max[1] + n[1] * v_drift;
		    part.vel[2] = v_max[2];
		    
		    part.dt=Starfish.rnd()*Starfish.getDt();
		} while (Vector.dot2(n, part.vel) <= 0);
	
		//pick random size
		part.radius = 0.5*(diam_range[0]+Starfish.rnd()*(diam_range[1]-diam_range[0]));
		
		//reduce density
		mass_to_sample -= (4/3.0)*Math.PI*part.radius*part.radius*part.radius*source_mat.density;
		
		return part;
    }

    @Override
    public void sampleFluid()
    {
    	/*TODO: this is hardcoded, generalize*/
	

    }
    
    double mass_to_sample =0;
    @Override
    public void update() {
    	mass_to_sample += mdot0 * Starfish.getDt();
    }
    
    @Override
    public boolean hasParticles()
    {
    	return mass_to_sample>0;
    }
    
    /**
     */
    static public SourceModule.SurfaceSourceFactory dropletSourceFactory = new SourceModule.SurfaceSourceFactory()
    {
	@Override
	public void makeSource(Element element, String name, Boundary boundary, Material material)
	{
	    DropletSource source = new DropletSource(name, material, boundary, element);
	    boundary.addSource(source);

	    
	}
	
    };

}
