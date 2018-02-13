package starfish.collisions;

import starfish.core.common.Plugin;
import starfish.core.interactions.InteractionsModule;

/** Registers MCC and DSMC collision handlers. Also acts as a demo for writing plugins.
 *
 * @author Lubos Brieda
 */
public class CollisionsPlugin implements Plugin
{
    
    /** 
     *
     */
    @Override
    public void register()
    {		
	/*add new interactions*/
	InteractionsModule.registerInteraction("DSMC",DSMC.DSMCFactory);
	InteractionsModule.registerInteraction("MCC", MCC.MCCFactory);
	
	/*add cross-section*/
	InteractionsModule.registerSigma("Bird463",SigmaPlus.makeSigmaBird463);	
    }
    
}
