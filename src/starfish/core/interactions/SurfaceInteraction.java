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
import starfish.core.source.ParticleListSource;

/** particle-surface interaction handler*/
public class SurfaceInteraction
{
    static void registerModels()
    {
	registerSurfaceModel("NONE",SurfaceEmissionNone);
	registerSurfaceModel("ABSORB",SurfaceEmissionNone);
	registerSurfaceModel("SPECULAR",SurfaceEmissionSpecular);
	registerSurfaceModel("DIFFUSE",SurfaceEmissionCosine);
	registerSurfaceModel("COSINE",SurfaceEmissionCosine);
    }
    
    static public HashMap<String,SurfaceImpactHandler> surface_model_factories = new HashMap();
    static public void registerSurfaceModel(String name, SurfaceImpactHandler fac)
    {
	surface_model_factories.put(name.toUpperCase(),fac);
	Log.log("Added surface impact model "+name.toUpperCase());
    }
  
    public interface SurfaceModelFactory 
    {
	public SurfaceImpactHandler makeModel();
    }
   
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
	
    /** absorbs particles*/
    public static SurfaceImpactHandler SurfaceEmissionNone = new SurfaceImpactHandler() 
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
	    double v_diff = Utils.SampleMaxwSpeed(boundary.getVth(mat_int.target_mat));
	    double v_mag = v_refl + mat_int.c_accom*(v_diff-v_refl);
	    
	    //did the particle stick?
	    /*TODO: make user input.*/
	    if (v_mag<1e-4)
		return false;				
				   		
	    /*cosine emission*/		
	    double dir_diff[] = Vector.lambertianVector(segment.normal(t_int),segment.tangent(t_int));
	 
	    for (int i=0;i<3;i++)
		vel[i]=v_mag*dir_diff[i];
	    
	    /*if we are producing a new material, kill the source and
	    * create new one*/
	    if (mat_int.product_mat_index !=
		mat_int.source_mat_index)
	    {
		ParticleListSource source = boundary.getParticleListSource(mat_int.product_mat_index);
		source.spawnParticles(segment.pos(t_int),vel,mat_int.source_mat_index);
		return false;
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
