package starfish.plugins.surface_processing;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.interactions.InteractionsModule;
import starfish.core.interactions.InteractionsModule.InteractionFactory;
import starfish.core.interactions.Sigma;
import starfish.core.interactions.VolumeInteraction;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;

public class Sputtering extends VolumeInteraction
{
    Material target;
    Material source;
    
    Sputtering(String target, String source) 
    {
	this.target = Starfish.getMaterial(target);
	this.source = Starfish.getMaterial(source);
    }

    @Override
    public void perform()
    {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void init()
    {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    /**Parses &lt;DSMC&gt; element*/
    public static InteractionFactory SputteringFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
	    String pair[] = InputParser.getList("pair", element);
	    if (pair.length!=2)
		Log.error("Must specify collision pair, pair=\"mat1,mat2\"");
	    String model_name = InputParser.getValue("model", element);

	    Sigma sigma = InteractionsModule.parseSigma(element);
	    
	    Starfish.interactions_module.addInteraction(new Sputtering(pair[0],pair[1]));
	}
    };

	@Override
	public void clearSamples() {
		// TODO Auto-generated method stub
		
	}

}
