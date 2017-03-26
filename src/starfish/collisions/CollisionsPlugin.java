package starfish.collisions;

import starfish.core.common.Plugin;
import starfish.core.interactions.InteractionsModule;

public class CollisionsPlugin implements Plugin
{
    
    @Override
    public void register()
    {
		
	/*add new interactions*/
	InteractionsModule.registerInteraction("DSMC",DSMC.DSMCFactory);
	
	/*add cross-section*/
	InteractionsModule.registerSigma("Bird463",SigmaPlus.makeSigmaBird463);
	
    }
    
}
