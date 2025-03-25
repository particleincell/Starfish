/* *****************************************************
 * (c) 2015 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.collisions;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Utils;
import starfish.core.materials.Material;
import starfish.interactions.Sigma;

/**
 *
 * @author Lubos Brieda
 */
public abstract class SigmaPlus
{

    /**
     *
     */
    public static Sigma.SigmaFactory makeSigmaBird463 = new Sigma.SigmaFactory() {
	@Override
	public Sigma makeSigma(double[] c, Element element) {
	    return new SigmaBird463(c);
	}
    };   
 
    /*equation 4.63 in Bird*/

    /**
     *
     */
    public static class SigmaBird463 extends Sigma
    {
	private double sigma0;
	private double ref_temp;
	private double visc_temp_index;
	private double reduced_mass;
	private double gamma;
    
	SigmaBird463(double c[]) {super(c);}
	
	/**
	 *
	 * @param mat1
	 * @param mat2
	 */
	@Override
	public void init(Material mat1, Material mat2) 
	{
	    
	    /*set collision data*/
	    double d_ref = (mat1.diam+mat2.diam);
	    sigma0 = 0.25*Constants.PI*d_ref*d_ref; //1.35
	   	    
	    if (sigma0<=0) Starfish.Log.error("Material diameter <diam> not defined");
	
	    /*TODO: these are needed only for bird's sigma model!*/
	    ref_temp=0.5*(mat1.ref_temp+mat2.ref_temp);
	    visc_temp_index=0.5*(mat1.visc_temp_index+mat2.visc_temp_index);
	    reduced_mass=mat1.mass*mat2.mass/(mat1.mass+mat2.mass);
	    gamma=Utils.gamma(2.5-visc_temp_index);
	}
	
	@Override
	public double eval(double g, double mass) {
	    double sigma = sigma0*
		    Math.pow(2.*Constants.K*ref_temp/(reduced_mass*g*g),
			    visc_temp_index-0.5)/gamma;
	    return sigma;
		}
    }
	
}