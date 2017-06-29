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
import starfish.core.io.InputParser;
import starfish.core.materials.MaterialsModule.MaterialParser;

/** solid material, does not change in density*/
public class SolidMaterial extends Material
{
    public SolidMaterial(String name, double mass)
    {
	super(name,mass,0, true);
    }
	
    @Override
    public void updateFields() 
    {
	/*do nothing*/
    }
    
    public static MaterialParser SolidMaterialParser = new MaterialParser() {
    @Override
    public  Material addMaterial(String name, Element element)
    {
	double molwt = Double.parseDouble(InputParser.getValue("molwt", element));
	    
	    Material material = new SolidMaterial(name,molwt);
	    
	    /*log*/
	    Starfish.Log.log("Added SOLID material '"+name+"'");
	    Starfish.Log.log("> molwt  = "+molwt);
	    return material;
    }
    }; 
    
    @Override 
    public void loadRestartData(DataInputStream in)throws IOException {}

}
