package starfish.plugins.surface_processing;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.interactions.InteractionsModule;
import starfish.core.interactions.InteractionsModule.InteractionFactory;
import starfish.core.interactions.Sigma;
import starfish.core.interactions.SurfaceInteraction;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;

public class SurfaceEmission extends SurfaceInteraction
{
    Material target;
    Material source;
    
    SurfaceEmission(String target, String source) 
    {
    	this.target = Starfish.getMaterial(target);
    	this.source = Starfish.getMaterial(source);
    }
   
    /**Parses surface emission element*/
    public static InteractionFactory SurfaceEmissionFactory = new InteractionFactory()
    {
		@Override
		public void getInteraction(Element element)
		{
		    String pair[] = InputParser.getList("pair", element);
		    if (pair.length!=2)
			Log.error("Must specify collision pair, pair=\"mat1,mat2\"");
		    String model_name = InputParser.getValue("model", element);
	
		    Sigma sigma = InteractionsModule.parseSigma(element);
		    
		    //Starfish.interactions_module.addInteraction(new Sputtering(pair[0],pair[1]));
		}
    };

}
