/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.interactions;

import java.util.HashMap;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Segment;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.interactions.MaterialInteraction.SurfaceImpactHandler;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;
import starfish.core.common.Vector;
import starfish.core.materials.KineticMaterial;
import starfish.core.source.ParticleListSource;

/** particle-surface interaction handler*/
public class SurfaceInteraction
{
    static void registerModels()
    {
	registerSurfaceModel("NONE",SurfaceEmissionNone);
	registerSurfaceModel("ABSORB",SurfaceEmissionAbsorb);
	registerSurfaceModel("SPECULAR",SurfaceEmissionSpecular);
	registerSurfaceModel("DIFFUSE",SurfaceEmissionDiffuse);
	registerSurfaceModel("COSINE",SurfaceEmissionCosine);
    }
    
    /**
     *
     */
    static public HashMap<String,SurfaceImpactHandler> surface_model_factories = new HashMap<String,SurfaceImpactHandler>();

    /**
     *
     * @param name
     * @param fac
     */
    static public void registerSurfaceModel(String name, SurfaceImpactHandler fac)
    {
	surface_model_factories.put(name.toUpperCase(),fac);
	Log.log("Added surface impact model "+name.toUpperCase());
    }
  
    /**
     *
     */
    public interface SurfaceModelFactory 
    {

	/**
	 *
	 * @return
	 */
	public SurfaceImpactHandler makeModel();
    }
   
    /**
     *
     * @param handler_name
     * @return
     */
    public static SurfaceImpactHandler getSurfaceImpactModel(String handler_name)
    {
	try {
	    SurfaceImpactHandler handler = surface_model_factories.get(handler_name.toUpperCase());
	    return handler;
	}
	catch (Exception e)
	{
	    throw new NoSuchElementException("Unknown surface handler name "+handler_name);
	}
    }
	
    /** doesn't do anything - particles will pass*/
    public static SurfaceImpactHandler SurfaceEmissionNone = new SurfaceImpactHandler() 
    {
	@Override
	public boolean perform(double[] vel, Segment segment, double t_int, MaterialInteraction mat_int) 
	{	
	    return true;
	}		
    };

        /** absorbs particles*/
    public static SurfaceImpactHandler SurfaceEmissionAbsorb = new SurfaceImpactHandler() 
    {
	@Override
	public boolean perform(double[] vel, Segment segment, double t_int, MaterialInteraction mat_int) 
	{	
	    return false;
	}		
    };

    /** specularly reflects all particles*/
    public static SurfaceImpactHandler SurfaceEmissionSpecular = new SurfaceImpactHandler()
    {
	@Override
	public boolean perform(double vel[], Segment segment, double t_int, MaterialInteraction mat_int) 
	{
	    double n[] = segment.normal(t_int);
			
	    /*from geometry, v2/|v2| = n*sqrt(2) + v1/|v1|
	    * since elastic, |v2|=|v1], and v2 = n*sqrt(2)*|v1| + v1;*/
	    double mag = Vector.mag2(vel)*Constants.SQRT2;
			
	    vel[0] += n[0]*mag;
	    vel[1] += n[1]*mag;

/*    	    double norm[] = segment.normal(t_int);
	    double dir[] = Vector.copy(vel);
	    Vector.unit3(dir);
	    //component along normal
	    double dir_perp[] = Vector.mult(norm, Vector.dot(dir, norm));
	    double dir_tang[] = Vector.subtract(dir, dir_perp);
	    //v_spec = v_tang - v_perp
	    double dir_spec[] = Vector.subtract(dir_tang,dir_perp);
*/
	    return true;
	}				
    };
    
    /** Reflects particles using Bird's diffuse reflection model*/
    public static SurfaceImpactHandler SurfaceEmissionDiffuse = new SurfaceImpactHandler()
    {
	@Override
	public boolean perform(double vel[], Segment segment, double t_int, MaterialInteraction mat_int) 
	{
	    /*based on REFLECT2 in DSMC2.f*/
	    
	    /*most probable speed, eqns 4.1 and 4.7*/
	    double vmp = segment.boundary.getVth(mat_int.product_mat);	
	    double ref_vel[] = Utils.diffuseReflVel(vmp, segment.normal(t_int),segment.tangent(t_int));
	   
	    /*need to actually set the value, saying vel=ref_vel won't work*/
	    for (int i=0;i<3;i++) vel[i] = ref_vel[i];
	    return true;
	}
    };
		
    /** Reflects particles in direction following cosine law*/
    public static SurfaceImpactHandler SurfaceEmissionCosine = new SurfaceImpactHandler()
    {
	@Override
	public boolean perform(double vel[], Segment segment, double t_int, MaterialInteraction mat_int) 
	{
	    Boundary boundary = segment.getBoundary();
	
	    /*magnitude of post-impact velocity due to coefficient of restitution*/
	    double v_refl = Vector.mag3(vel)*mat_int.c_rest;
	    
	    /*magnitude due to thermal accomodation*/
	    double v_diff = Utils.SampleMaxwSpeed(boundary.getVth(mat_int.product_mat));
	    double v_mag = v_refl + mat_int.c_accom*(v_diff-v_refl);
	    
	    //did the particle stick?
	    /*TODO: make user input.*/
	    if (v_mag<1e-4)
		return false;				
				   			    
	    /*if we are producing a new material, kill the source and create new ones
	    this used to call SpawnParticles but moved the code here so we can re-avaluate the Maxwellian
	    */
	    if (mat_int.source_mat != mat_int.product_mat && mat_int.product_km_mat!=null)
	    {
		ParticleListSource source = mat_int.product_mat.getParticleListSource();
		
		double spwt_orig = mat_int.source_km_mat.getSpwt0();
		double spwt_source = source.spwt0;
	
		double ratio = spwt_orig / spwt_source;

		int count = (int) (ratio + Starfish.rnd());
		double pos[] = segment.pos(t_int);
		double normal[] = segment.normal(t_int);
		double tang[] = segment.tangent(t_int);
	
		for (int i = 0; i < count; i++)
		{
		    /*cosine emission*/		
		    double dir_diff[] = Vector.lambertianVector(normal, tang);
		    v_diff = Utils.SampleMaxwSpeed(boundary.getVth(mat_int.product_mat));
		    v_mag = v_refl + mat_int.c_accom*(v_diff-v_refl);
		    vel = Vector.mult(dir_diff,v_mag);
		    source.addParticle(new KineticMaterial.Particle(pos, vel, spwt_source, mat_int.source_km_mat));
		}		
		return false;
	    }
	    else
	    {
		/*cosine emission*/		
		double dir_diff[] = Vector.lambertianVector(segment.normal(t_int),segment.tangent(t_int));
	 
		for (int i=0;i<3;i++)
		    vel[i]=v_mag*dir_diff[i];
	    }

	    return true;
	}				
    };
    
    /**Parses &lt;surface_impact&gt; element*/
    static InteractionsModule.InteractionFactory surfaceHitFactory = new InteractionsModule.InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
	    /*get source, target, and product*/
	    String source_name = InputParser.getValue("source", element);
	    String target_name = InputParser.getValue("target", element);
	    
	    /*if product is not specified, assume it's the same as source*/
	    String product_name = InputParser.getValue("product", element,source_name);
	    
	    /*read accomodation, and restitution coefficients*/
	    double c_rest=0, c_accom=0;		/*meaningless values to initialize the variable*/
	    double prob=0;
	    try{
		prob = InputParser.getDouble("prob",element,1.0);
		c_rest = InputParser.getDouble("c_rest", element,1.0);
		c_accom = InputParser.getDouble("c_accom", element,0.0);
	    }
	    catch (NumberFormatException e)
	    {
		Log.error("Non-numeric value for c_rest, c_accom, or prob");
	    }

	    /*get surface impact handler*/
	    String model_type = InputParser.getValue("model", element);
	    SurfaceImpactHandler surface_impact_handler = SurfaceInteraction.getSurfaceImpactModel(model_type);

	    /*grab material handles*/
	    Material target=null,product=null,source=null;
	    try
	    {
		target = Starfish.getMaterial(target_name);
		product = Starfish.getMaterial(product_name);
		source = Starfish.getMaterial(source_name);
	    }
	    catch (NoSuchElementException e)
	    {
		Log.error(e.getMessage());
	    }

	    MaterialInteraction material_interaction = new MaterialInteraction();
	    material_interaction.setTargetMatIndex(target.getIndex());
	    material_interaction.setSourceMatIndex(source.getIndex());
	    material_interaction.setProductMatIndex(product.getIndex());
	    material_interaction.setAccomodationCoefficient(c_accom);
	    material_interaction.setProbability(prob);
	    material_interaction.setRestitutionCoefficient(c_rest);
	    material_interaction.setSurfaceImpactHandler(surface_impact_handler);
	    target.source_interactions.addInteraction(material_interaction);

	    /*log*/
	    Log.log("Added SURFACE_IMPACT "+source_name+" + " +target_name + " -> "+product_name);
	    Log.log("> probability   = "+prob);
	    Log.log("> restitution  = "+c_rest);
	    Log.log("> thermal accomodation = "+c_accom);		
	}
    };
    		
}
