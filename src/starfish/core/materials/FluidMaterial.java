/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.materials;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.domain.Mesh;

/** abstract definition of a fluid material*/
public abstract class FluidMaterial extends Material
{
    /*constructor*/

    /**
     *
     * @param name

     */
    public FluidMaterial(String name, Element element)
    {
	super(name, element);
    }

    @Override
    public void init()
    {
	super.init();
	
	for (Mesh mesh:Starfish.getMeshList())
	    getDen(mesh).fill(1e17);
    }
   
}
