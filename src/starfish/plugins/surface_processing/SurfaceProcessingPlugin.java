/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starfish.plugins.surface_processing;

import starfish.core.common.Plugin;
import starfish.core.interactions.InteractionsModule;
import starfish.core.materials.MaterialsModule;
import starfish.core.source.SourceModule;

/**
 *
 * @author lbrieda
 */
public class SurfaceProcessingPlugin implements Plugin
{

    @Override
    public void register()
    {
	MaterialsModule.registerMaterialType("GRAIN",GrainMaterial.GrainMaterialParser);	
	SourceModule.registerSurfaceSource("DROPLET",DropletSource.dropletSourceFactory);
	
    }
    
}

