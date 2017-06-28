/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.interactions;

import starfish.core.materials.Material;

/** Collision cross-sections */
public abstract class Sigma 
{
    /**returns cross-section for the given relative velocity*/
    public abstract double eval(double g);
    protected double c[];
 
    //by default doesn't do anything but some sigmas may need to initialize
    public void init(Material mat1, Material mat2) {};
    
    /*default empty init*/
    protected Sigma () {c=new double[1];c[0]=0;}
    
    protected Sigma (double c[]) 
    {
	this.c=c;
    }
     
    public interface SigmaFactory
    {
	public Sigma makeSigma(double c[]);
    }
    

    static SigmaFactory sigmaConstFactory = new SigmaFactory() {
	@Override
	public Sigma makeSigma(double[] c) {
	    return new SigmaConst(c);
	}
    };

    static SigmaFactory sigmaInvFactory = new SigmaFactory() {
	@Override
	public Sigma makeSigma(double[] c) {
	    return new SigmaInv(c);
	}
    };
    
    /** sigma = [c0] */
    public static class SigmaConst extends Sigma
    {
	SigmaConst(double c[]){super(c);}
	@Override
	public double eval(double g) {return c[0];}
    }
 	
    /** sigma = c[0]/g */
    public static class SigmaInv extends Sigma
    {
	SigmaInv(double c[]){super(c);}
	@Override
	public double eval(double g) 
	{
	    double val = 0;
	    if (g>0)
		val = c[0]/g;
    		
	    //if (val>1e-14) val=1e-14;
	    return val;
	}
    }
}
