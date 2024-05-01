package starfish.plugins.surface_processing;

import java.util.NoSuchElementException;

import org.w3c.dom.Element;

import starfish.collisions.MCC;
import starfish.collisions.MCC.MCCModel;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Vector;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Mesh;
import starfish.core.interactions.InteractionsModule;
import starfish.core.interactions.InteractionsModule.InteractionFactory;
import starfish.core.interactions.MaterialInteraction.SurfaceImpactHandler;
import starfish.core.interactions.MaterialInteraction;
import starfish.core.interactions.Sigma;
import starfish.core.interactions.SurfaceInteraction;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;
import starfish.core.materials.KineticMaterial.Particle;

public class Sputtering extends MaterialInteraction
{
    Material target;
    Material source;
    
    Sputtering(String target, String source) 
    {
    	this.target = Starfish.getMaterial(target);
		this.source = Starfish.getMaterial(source);
    }

    

    /**Parses &lt;sputtering&gt; element*/
    public static InteractionFactory sputteringFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
		// get source, target, and product
	    String source_name = InputParser.getValue("source", element);
	    String target_name = InputParser.getValue("target", element);
	    
	    // if product is not specified, assume it's the same as target
	    String product_name = InputParser.getValue("product", element,target_name);
	    	   
	    // get surface impact handler
	    String model_type = InputParser.getValue("model", element);
	    SurfaceImpactHandler surface_impact_handler = SurfaceInteraction.getSurfaceImpactModel(model_type);

	    // grab material handles
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
	    material_interaction.setSurfaceImpactHandler(surface_impact_handler);
	    target.target_interactions.addInteraction(material_interaction);

	    // log
	    Log.log("Added SPUTTERING "+source_name+" + " +target_name + " -> "+product_name);
	    
	}
    };

    
    // sputtering model
    
    static abstract class SputterModel 
    {
    	/**returns cross-section for the given relative velocity*/
    	public abstract void perform(Particle source);
	
		protected SputterModel() {}
    }
	
    static class ModelConst extends SputterModel
    {
		@Override
		public void perform(Particle source) 
		{			
			/* ... */
	    }
    }
	

}
