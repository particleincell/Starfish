/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.materials;

import java.io.DataInputStream;
import java.io.IOException;
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.materials.MaterialsModule.MaterialParser;

/** solid material, does not change in density*/
public class SolidMaterial extends Material
{

    /**
     *
     * @param name
     */
    public SolidMaterial(String name, Element element)
    {
	super(name,element);
	Starfish.Log.log("Added SOLID material '"+name+"'");	    
    }
	
    @Override
    public void updateFields() 
    {
	/*do nothing*/
    }
    
    /**
     *
     */
    public static MaterialParser SolidMaterialParser = new MaterialParser() {
	@Override
	public  Material addMaterial(String name, Element element)
	{

		return new SolidMaterial(name,element);
	}
    }; 
    
    /**
     *
     * @param in
     * @throws IOException
     */
    @Override 
    public void loadRestartData(DataInputStream in)throws IOException {}

}
