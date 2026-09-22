/*
Starfish, a general 2D plasma simulation code
(c) 2012, Particle In Cell Consulting LLC
Contact: lubos.brieda@particleincell.com
*/
package starfish.plugins.het;

import starfish.core.common.Constants;
import starfish.core.common.LinearList;
import starfish.interactions.RateParser;

public class IonizationFife 
{
	final public static double EI = 12.17*Constants.QE;	/*ionization energy in eV*/
	
   	
	static public RateParser MathParserFife = new RateParser()
	{
	    LinearList varI = null;
	    @Override
	    public double eval(double var, double c[], double d[])
	    {
			if (varI==null)
			{
			    /*init*/
			    varI = new LinearList();
			    for (double Te=0.01*Constants.EVtoK;Te<100*Constants.EVtoK;Te+=0.05*Constants.EVtoK)
			    {
			    	double theta = Constants.K*Te/EI;
			    	varI.insert(theta, var_I(theta));
			    }
			}
			return var_sig(var,varI);			
	    }
	};
	
	/*var_sig, equation 2.101 in Fife*/
	/*uses precomputed list*/
	static public double var_sig(double Te,LinearList varI)		/*in K*/
	{
	    double theta = Constants.K*Te/EI;
	    final double beta1 = 1;
	    final double Q = 4.13e-13;
		
	    
	    return Q*beta1*varI.eval(theta)/Math.pow(theta,1.5);
	}
	/*var_sig, equation 2.101 in Fife*/

	/*version without pre-computed list*/
	static public double var_sig(double Te)		/*in K*/
	{
	    double theta = Constants.K*Te/EI;
	    final double beta1 = 1;
	    final double Q = 4.13e-13;
		
	    
	    return Q*beta1*var_I(theta)/Math.pow(theta,1.5);
	}
	
	/*evaluates 2.99 in Fife*/
	static public double var_I(double theta)
	{
	    final double beta2=0.80; //table 2.3
		
	    double du=0.01;
	    int n=1000;
	    double f[] = new double[n];
		
	    /*first terms evaluates to zero*/
	    f[0] = 0;
		
	    for (int i=1;i<n;i++)
	    {
		double u = 1.0+i*du;
		f[i] = Math.exp(-u/theta)*(u-1)/(u)*Math.log(1.25*beta2*u);
			
		/*the curve has an inflection point, need to capture it*/
		if (f[i-1]>f[i] && f[i]<1e-5)
		{
		    n=i;
		    break;
		}
	    }
		
	    double sum=0;
	    for (int i=0;i<n-1;i++)
	    {
		    sum+=0.5*(f[i]+f[i+1])*du;
	    }
	    return sum;
	}

}
