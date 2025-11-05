package starfish.interactions;

import java.util.HashMap;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;

import starfish.core.boundaries.Segment;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Vector;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.source.ParticleListSource;
import starfish.interactions.InteractionsModule.InteractionFactory;
import starfish.interactions.MaterialInteraction.SurfaceImpactHandler;

public class Sputtering extends MaterialInteraction
{
    Material target;
    Material source;
    
    Sputtering(Element element) 
    {
    	String source_name = InputParser.getValue("source", element);
  	    String target_name = InputParser.getValue("target", element);
  	    
  	    // if product is not specified, assume it's the same as target
  	    String product_name = InputParser.getValue("product", element,target_name);
  	    	   
  	    // grab material handles
  	    Material target=null, product=null, source=null;
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
  	    
  	    // get surface impact handler
  	    String model_type = InputParser.getValue("model", element);
  	    SurfaceImpactHandler sputter_handler = getSputteringModel(model_type);
  	    
  	    setTargetMatIndex(target.getIndex());
  	    setSourceMatIndex(source.getIndex());
  	    setProductMatIndex(product.getIndex());
  	    setSurfaceImpactHandler(sputter_handler);
  	    target.target_interactions.addInteraction(this);

  	    // log
  	    Log.log("Added SPUTTERING "+source_name+" + " +target_name + " -> "+product_name);
  	    
    }

    static InteractionFactory sputteringFactory = new InteractionFactory()
    {
		@Override
		public void getInteraction(Element element)
		{
		  
		    Sputtering sputtering = new Sputtering(element);
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

    static void registerModels()
    {
		registerSputterModel("CONST",SputteringModelConst);
	}

  
	static public HashMap<String,SurfaceImpactHandler> sputtering_model_factories = new HashMap<String,SurfaceImpactHandler>();
   
	static public void registerSputterModel(String name, SurfaceImpactHandler fac)
	{
		sputtering_model_factories.put(name.toUpperCase(),fac);
		Log.log("Added sputtering model "+name.toUpperCase());
	}
  
	public static SurfaceImpactHandler getSputteringModel(String model_name)
	{
    	
		try {
			SurfaceImpactHandler handler = sputtering_model_factories.get(model_name.toUpperCase());
			return handler;
		}
		catch (Exception e)
		{
			throw new NoSuchElementException("Unknown sputtering model "+model_name);
		}
	}
    
    // ****************** SPUTTERING MODELS ****************************************************
    /** specularly reflects all particles*/
    public static SurfaceImpactHandler SputteringModelConst = new SurfaceImpactHandler()
    {
		@Override
		public boolean perform(double vel[], double spwt_source, Segment segment, double t_int, MaterialInteraction mat_int) 
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
		    /*species change?*/
		    if (mat_int.source_mat != mat_int.product_mat && mat_int.product_km_mat!=null)
		    {
			ParticleListSource source = mat_int.product_mat.getParticleListSource();
			
			double spwt_orig = mat_int.source_km_mat.getSpwt0();
		
			double ratio = spwt_orig / spwt_source;
	
			int count = (int) (ratio + Starfish.rnd());
			double pos[] = segment.pos(t_int);
				
			for (int i = 0; i < count; i++)
			{
			    /*specular emission*/		
			    source.addParticle(new KineticMaterial.Particle(pos, vel, spwt_source, mat_int.product_km_mat));
			}		
			return false;
		    }
		    
		    /*otherwise keep particle*/
		    return true;
		}				
    };
	

}
