/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.interactions;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Utils.LinearList;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;

/** Collision cross-sections */
public abstract class Sigma 
{
    /**returns cross-section for the given relative velocit
     * @param g relative velocity
     * @param mass mass of colliding particle(s) for computing energy, if needed
     * @return y*/
    public abstract double eval(double g, double mass);

    /**
     *
     */
    protected double c[];
 
    //by default doesn't do anything but some sigmas may need to initialize

    /**
     *
     * @param mat1
     * @param mat2
     */
    public void init(Material mat1, Material mat2) {};
    
    /*default empty init*/

    /**
     *
     */

    protected Sigma () {c=new double[1];c[0]=0;}
    
    /**
     *
     * @param c
     */
    protected Sigma (double c[]) 
    {
	this.c=c;
    }
     
    /**
     *
     */
    public interface SigmaFactory
    {
	/** creates a sigma handler of this type
	 *
	 * @param coeffs coefficients
	 * @param element XML element in case additional data is needed
	 * @return new sigma method
	 */
	public Sigma makeSigma(double[] coeffs, Element element);
    }
    

    static SigmaFactory sigmaConstFactory = new SigmaFactory() {
	@Override
	public Sigma makeSigma(double[] c, Element element) {
	    return new SigmaConst(c);
	}
    };

    static SigmaFactory sigmaInvFactory = new SigmaFactory() {
	@Override
	public Sigma makeSigma(double[] c, Element element) {
	    return new SigmaInv(c);
	}
    };
    
    static SigmaFactory sigmaTableFactory = new SigmaFactory() {
	@Override
	public Sigma makeSigma(double[] c, Element element) {
	    return new SigmaTable(element);
	}
    };
    
    /** sigma = [c0] */
    public static class SigmaConst extends Sigma
    {
	SigmaConst(double c[]){super(c);}
	@Override
	public double eval(double g, double mass) {return c[0];}
    }
 	
    /** sigma = c[0]/g */
    public static class SigmaInv extends Sigma
    {
	SigmaInv(double c[]){super(c);}
	@Override
	public double eval(double g, double mass) 
	{
	    double val = 0;
	    if (g>0)
		val = c[0]/g;
    		
	    //if (val>1e-14) val=1e-14;
	    return val;
	}
    }
    
    /** linear interpolation from tabular data */
    public static class SigmaTable extends Sigma
    {
	protected LinearList table;
	protected boolean dep_var_energy;
	
	SigmaTable(Element element){
	    super();
	    ArrayList<double[]> data = InputParser.getDoublePairs("sigma_tabulated", element);
	    table = new LinearList(data);
	    String dep_var = InputParser.getValue("sigma_dep_var", element,"velocity");    
	    if (dep_var.equalsIgnoreCase("ENERGY"))
		dep_var_energy = true;
	    else
		dep_var_energy = false;
	    }
	@Override
	public double eval(double g, double mass) 
	{
	    double x = g;
	    
	    /*convert to eV as needed*/
	    if (dep_var_energy)
		x=0.5*mass*g*g/Constants.QE;
		
	    return table.eval(x);
	}
    }
}
