/*

 */
package starfish.plugins.het;

import java.util.HashMap;
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;

public abstract class Mobility
{    	
	public interface MobilityFactory
	{
	/** creates a mobility evaluation function
	 *
	 * @param element XML parent element
	 * @return new mobility
	 */
	   public Mobility makeMobility(Element element);
	}
     
	public abstract void update();
	
	static HashMap<String,MobilityFactory> mobility_factories = new HashMap();
	
	public static void registerMobility(String type, MobilityFactory fac)
	{
	    mobility_factories.put(type.toUpperCase(), fac);
	    Log.debug("Registered mobility model "+type.toUpperCase());
	}

	
	public static Mobility makeMobility(Element element)
	{
	    
	    /*make sure models are registered*/
	    if (mobility_factories.isEmpty())
	    {
		registerMobility("NWB", MobilityNWB.mobilityNWBFactory);
		registerMobility("LYNX", MobilityLynx.mobilityLynxFactory);
		registerMobility("BOHM", MobilityBohm.mobilityBohmFactory);
		registerMobility("HPHALL", MobilityHPHall.mobilityHPHallFactory);
	    }
	    
	    String model = InputParser.getValue("model", element);
	    MobilityFactory fac = mobility_factories.get(model.toUpperCase());
	    if (fac!=null)
	    {
		Log.log("Mobility model: "+model.toUpperCase());
		return fac.makeMobility(element);
	    }
	    else
		Log.error("Unknown mobility model "+model);
	    
	    return null;	    
	}
		
	
	Mobility(Element element) {	    
	    Starfish.domain_module.getFieldManager().add("mu", null, null);	    	    
	}
	
		
	LambdaMesh  lambda_mesh;
	/*creates lambda mesh used for Lynx computations*/
	public void setLambdaMesh(LambdaMesh lambda_mesh)
	{
	    this.lambda_mesh = lambda_mesh;
	}

    }
