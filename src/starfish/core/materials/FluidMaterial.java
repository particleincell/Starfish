/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.materials;

import starfish.core.common.Starfish;
import starfish.core.domain.Mesh;

/** abstract definition of a fluid material*/
public abstract class FluidMaterial extends Material
{
    /*constructor*/

    public FluidMaterial(String name, double mass, double charge)
    {
	super(name, mass, charge);
    }

    @Override
    public void init()
    {
	super.init();
	
	for (Mesh mesh:Starfish.getMeshList())
	    getDen(mesh).fill(1e17);
    }
   
}
