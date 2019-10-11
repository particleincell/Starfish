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
import starfish.core.domain.FieldCollection2D;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;

/**
 *
 * @author Lubos Brieda
 */
public class RateParser 
{
    String dep_var_name;
    FieldCollection2D dep_var_fc = null;
    
    enum WrapperType {NONE,CONST,LOG,LOG10,ENERGY,JTOEV,KTOEV};
    static class Wrapper {
	double apply(double input) {
	    switch (type){
		case CONST: return coeff*input;	    //C*input
		case LOG: return Math.log(input);	//log of input
		case LOG10: return Math.log10(input);	//log10 of input
		case ENERGY: return 1.5*Constants.K*input;  //  (3/2)kT
		case JTOEV: return Constants.JtoEV*input;   // converts from Joules to eV
		case KTOEV: return Constants.KtoEV*input;   // converst from Kelvin to eV
		case NONE: 
		default: return input;	
	    }
	}
	Wrapper(WrapperType type, double coeff) {this.type=type;this.coeff=coeff;}	
	WrapperType type;
	double coeff;
	
    };
    
    Sigma rate_model;	    //using Sigma as rate model
    ArrayList<Wrapper> input_wrappers = new ArrayList<>();
    ArrayList<Wrapper> output_wrappers = new ArrayList<>();
    
    double coeffs[];		//parser coefficients
    double mass;
    boolean is_sigma = false;	    //is this actually cross-section?
    Material sources[];
    Material products[];
    ArrayList<Double> sources_coeffs = new ArrayList<>();	    //coefficients for the sources 
    ArrayList<Double> products_coeffs = new ArrayList<>();    //coeficients for products
    
    /**
     *
     * @param el	XML element
     * @param sources	list of source materials for generality
     * @param products	list of product materials for generality
     */
    public RateParser (Element el, Material[] sources, Material[] products)
    {
	String rate_type = InputParser.getValue("type", el);
	coeffs = InputParser.getDoubleList("coeffs", el);
	is_sigma = InputParser.getBoolean("is_sigma", el,false);
	 
	rate_model = InteractionsModule.getSigma(rate_type, coeffs, el);
	
	if (rate_model==null)
	    Log.error("Unknown rate model "+rate_type);

	dep_var_name = InputParser.getValue("dep_var",el);
	String input_wrapper_names[]= InputParser.getList("input_wrappers", el);
	String output_wrapper_names[] = InputParser.getList("output_wrappers",el);
	
	/*check for multipliers*/
	String multiplier = InputParser.getValue("multiplier",el,"");
	if (!multiplier.isEmpty()) 
	    Log.error("<multiplier> has been replaced by <output_wrappers>");
	
	/*convert string wrapper names to data objects*/
	for (String name:input_wrapper_names)
	{
	    WrapperType type=WrapperType.NONE;
	    
	    /*first try to parse a double*/
	    double val=0;
	    try {val=Double.parseDouble(name);type=WrapperType.CONST;}
	    catch (NumberFormatException e) {
		/*pass*/
	    }
	    
	    if (type!=WrapperType.CONST)
	    {
		try {
		    type = WrapperType.valueOf(name.toUpperCase());
		}
		catch (IllegalArgumentException e) {
		    Log.warning("Unknown wrapper type "+name);
		    continue;
		}	    
	    }
	    input_wrappers.add(new Wrapper(type,val));	    
	}

	/*repeat for output wrappers*/
	/*convert string wrapper names to data objects*/
	for (String name:output_wrapper_names)
	{
	    WrapperType type=WrapperType.NONE;	    
	    /*first try to parse a double*/
	    double val=0;
	    try {val=Double.parseDouble(name);type=WrapperType.CONST;}
	    catch (NumberFormatException e) {
		/*pass*/
	    }
	    
	    if (type!=WrapperType.CONST)
	    {
		try {
		    type = WrapperType.valueOf(name.toUpperCase());
		}
		catch (IllegalArgumentException e) {
		    Log.warning("Unknown wrapper type "+name);
		    continue;
		}	    
	    }
	    output_wrappers.add(new Wrapper(type,val));	    
	}
	
	/*compute average mass for computing vmean*/
	this.mass = 0;
	for (Material mat:sources)
	    this.mass+=mat.mass;
	this.mass /= sources.length;
	
	/*save sources and products*/
	this.sources = sources.clone();
	this.products = products.clone();
    }    
    
    public RateParser()
    {
    }

    /**
     *
     * @param var
     * @param T
     * @return
     */
    public double eval(double var)
    {
	for (Wrapper wrapper:input_wrappers) 
	    var = wrapper.apply(var);
	
	double rate = rate_model.eval(var,1);
	
	for (Wrapper wrapper:output_wrappers)
	    rate = wrapper.apply(rate);
	
	return (rate>0)?rate:0;
    }
    
  
 
    
}
