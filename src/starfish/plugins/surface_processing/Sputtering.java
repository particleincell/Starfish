package starfish.plugins.surface_processing;

import java.util.NoSuchElementException;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.interactions.InteractionsModule;
import starfish.core.interactions.InteractionsModule.InteractionFactory;
import starfish.core.interactions.MaterialInteraction.SurfaceImpactHandler;
import starfish.core.interactions.MaterialInteraction;
import starfish.core.interactions.Sigma;
import starfish.core.interactions.SurfaceInteraction;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;

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
    public static InteractionFactory SputteringFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
		  /*get source, target, and product*/
	    String source_name = InputParser.getValue("source", element);
	    String target_name = InputParser.getValue("target", element);
	    
	    /*if product is not specified, assume it's the same as target*/
	    String product_name = InputParser.getValue("product", element,target_name);
	    
	    /*read accomodation, and restitution coefficients*/
/*
	    double c_rest=0, c_accom=0;		
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
*/
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
	  //  material_interaction.setAccomodationCoefficient(c_accom);
	  //  material_interaction.setProbability(prob);
	  //  material_interaction.setRestitutionCoefficient(c_rest);
	    material_interaction.setSurfaceImpactHandler(surface_impact_handler);
	    target.target_interactions.addInteraction(material_interaction);

	    /*log*/
	    Log.log("Added SPUTTERING "+source_name+" + " +target_name + " -> "+product_name);
	    
	}
    };


}
