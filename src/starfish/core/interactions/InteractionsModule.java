/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.interactions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;

/** base for material interactions handling*/
public class InteractionsModule extends CommandModule
{
    @Override
    public void init()
    {
	/*register sigmas*/
	registerSigma("CONST", Sigma.sigmaConstFactory);
	registerSigma("INV", Sigma.sigmaInvFactory);
	
	/*register interactions*/
	registerInteraction("SURFACE_HIT",SurfaceInteraction.surfaceHitFactory);
	registerInteraction("SURFACE_IMPACT",SurfaceInteraction.surfaceHitFactory);	
	registerInteraction("CHEMISTRY",ChemicalReaction.chemicalReactionFactory);
	
	/*register surface impact models*/
	SurfaceInteraction.registerModels();
	
	/*register rate parser*/
	RateParser.registerMathParser("POLYNOMIAL",RateParser.MathParserPolynomial);
    }
	
    static public void registerInteraction(String type, InteractionFactory fac)
    {
	interactions_types.put(type.toUpperCase(), fac);
	Log.log("Added interaction "+type.toUpperCase());
    }
    
    public interface InteractionFactory
    {
	public void  getInteraction(Element element);
    }
    
    static HashMap<String,InteractionFactory> interactions_types = new HashMap();
    
    @Override
    public void process(Element element) 
    {
	Iterator<Element> iterator = InputParser.iterator(element);
		
	while(iterator.hasNext())
	{
	    Element el = iterator.next();
	    InteractionFactory fac = interactions_types.get(el.getNodeName().toUpperCase());
	    if (fac!=null)
		fac.getInteraction(el);
	    else			
		Log.warning("Unknown interactions element "+el.getNodeName().toUpperCase());
	}
    }
	
    
    ArrayList<VolumeInteraction> interactions_list = new ArrayList<VolumeInteraction>();
    public ArrayList<VolumeInteraction> getInteractionsList() {return interactions_list;}

    /*adds material interaction*/
    public void addInteraction(VolumeInteraction handler)
    {
	interactions_list.add(handler);
    }

    /*performs material interactions*/
    public void performInteractions()
    {
	for (VolumeInteraction vint:interactions_list)
		vint.perform();
    }

    @Override
    public void exit() {}

    @Override
    public void start() 
    {
	for (VolumeInteraction vint:interactions_list)
		vint.init();
    }

    
    /** cross-sections*/
    static public Sigma parseSigma(Element element)
    {
	String sigma_name = InputParser.getValue("sigma", element);
	double coeffs[] = InputParser.getDoubleList("sigma_coeffs", element);

	/*make the cross-section*/
	return getSigma(sigma_name,coeffs);
    }
    
    static HashMap<String,Sigma.SigmaFactory> sigma_list = new HashMap();
    public static void registerSigma(String name, Sigma.SigmaFactory fac)
    {
	sigma_list.put(name.toUpperCase(),fac);
	Log.log("Added sigma "+name.toUpperCase());
    }

    public static Sigma getSigma(String type, double c[])
    {
	Sigma.SigmaFactory fac = sigma_list.get(type.toUpperCase());
	if (fac!= null) return fac.makeSigma(c);
	
	throw new UnsupportedOperationException("Collision cross-section "+type+" undefined");
    }
   
}
