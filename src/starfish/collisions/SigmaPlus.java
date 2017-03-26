/* *****************************************************
 * (c) 2015 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.collisions;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.interactions.Sigma;
import starfish.core.materials.KineticMaterial;

public abstract class SigmaPlus
{
    public static Sigma.SigmaFactory makeSigmaBird463 = new Sigma.SigmaFactory() {
	@Override
	public Sigma makeSigma(double[] c) {
	    return new SigmaBird463(c);
	}
    };   
 
    /*equation 4.63 in Bird*/
    public static class SigmaBird463 extends Sigma
    {
	private double sigma0;
	private double ref_temp;
	private double visc_temp_index;
	private double reduced_mass;
	private double gamma;
    
	SigmaBird463(double c[]) {super(c);}
	
	public void init(KineticMaterial mat1, KineticMaterial mat2) 
	{
	    /*set collision data*/
	    sigma0=0.25*Constants.PI*(mat1.diam*mat1.diam+mat2.diam*mat2.diam);
	
	    if (sigma0<=0) Starfish.Log.error("Material diameter <diam> not defined");
	
	    /*TODO: these are needed only for bird's sigma model!*/
	    ref_temp=0.5*(mat1.ref_temp+mat2.ref_temp);
	    visc_temp_index=0.5*(mat1.visc_temp_index+mat2.visc_temp_index);
	    reduced_mass=mat1.mass*mat2.mass/(mat1.mass+mat2.mass);
	    gamma=GAM(2.5-visc_temp_index);
	}
	
	@Override
	public double eval(double g) {
	    double sigma = sigma0*
		    Math.pow(2.*Constants.K*ref_temp/(reduced_mass*g*g),
			    visc_temp_index-0.5)/gamma;
	    return sigma;
		}
	
	//calculates the Gamma function of X per Bird's algorithm
	double GAM(double X)
	{
	    double A=1.;
	    double Y=X;
	    if (Y<1.0)
		A=A/Y;
	    else
	    {
		do {
		    Y=Y-1;
		    if (Y>=1.)
			A=A*Y;	
		} while (Y>=1.);
	    }

	    double GAM=A*(1.-0.5748646*Y+0.9512363*Y*Y-0.6998588*Y*Y*Y+
			    0.4245549*Y*Y*Y*Y-0.1010678*Y*Y*Y*Y*Y);
	    return GAM;
	    }
	}
}