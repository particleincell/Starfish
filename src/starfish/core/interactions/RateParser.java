/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.interactions;

import java.util.ArrayList;
import java.util.HashMap;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;

/**
 *
 * @author Lubos Brieda
 */
public class RateParser 
{
    String dep_var_mat;
    
    enum WrapperType {NONE,LOG10,LOG10ENERGY};
    WrapperType wrapper;
    
    private MathParser math_parser;
    
    double c[];		//parser coefficients
    double d[];		//secondary coefficients
    double mult;
    ArrayList<Material> sources = new ArrayList();
    ArrayList<Material> products = new ArrayList();
    
    /**
     *
     * @param el	XML element
     * @param sources	list of source material names for generality
     * @param products	list of product material names for generality
     */
    public RateParser (Element el, String[] sources, String[] products)
    {
	String type = InputParser.getValue("type", el);
	c = InputParser.getDoubleList("coeffs", el);
	d = InputParser.getDoubleList("coeffs_secondary", el);
	mult = InputParser.getDouble("multiplier", el,1);
	math_parser = parsers_list.get(type.toUpperCase());
	if (math_parser==null)
	    Log.error("Unknown rate model "+type);

	Element dep_var = InputParser.getChild("dep_var", el);
	dep_var_mat = InputParser.getValue("mat",dep_var);
	
	String dep_var_type = InputParser.getValue("wrapper",dep_var,"NONE");	
	wrapper = WrapperType.valueOf(dep_var_type.toUpperCase());
	
	/*save sources and products*/
	for (String name:sources)
	{
	    Material mat = Starfish.getMaterial(name);
	    this.sources.add(mat);
	}
	
	for (String name:products)
	{
	    Material mat = Starfish.getMaterial(name);
	    this.products.add(mat);
	}
    }    
    
    public RateParser()
    {
    }

    /**
     *
     * @param var
     * @return
     */
    public double eval(double var)
    {
	double v=var;
	switch (wrapper)
	{
	    case LOG10ENERGY: if (var>0) v=Math.log10(1.5*Constants.KtoEV*var); else return 0; break;
	    case LOG10: if (var>0) v=Math.log10(Constants.KtoEV*var); else return 0; break;
	}
	 
	double rate = math_parser.eval(v,c,d)*mult;
	//rate*=Math.sqrt(2*Constants.K*var/Constants.ME);
	return (rate>0)?rate:0;
    }
    
    /**
     *
     */
    static protected HashMap<String,MathParser> parsers_list = new HashMap<String,MathParser>();

    /**
     *
     * @param name
     * @param parser
     */
    static public void registerMathParser(String name, MathParser parser)
    {
	parsers_list.put(name.toUpperCase(), parser);
    }
    
    /**
     *
     */
    public interface MathParser {

	/**
	 *
	 * @param x input value
	 * @param c main coefficients
	 * @param d array of optional "secondary" coefficients
	 * @return chemical equation rate, generally (sigma*nu)=m^3/s
	 */
	public double eval(double x, double c[], double d[]);
    }
    
    /**
     *
     */
    static public MathParser MathParserPolynomial = new MathParser()
    {
	public double eval (double var, double c[],double d[])
	{
	    int order = c.length;
	    double sum=c[order-1];
	    double v=var;

	    for (int i=order-2;i>=0;i--)
	    {
		sum+=c[i]*v;
		v*=var;
	    }
	    return sum;
	}
    };
    
}
