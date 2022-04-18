/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package starfish.plugins.plasma_dynamics;

import starfish.core.common.Constants;
import starfish.core.interactions.RateParser;

/**
 *
 * @author Lubos Brieda
 */
public class BoschHale
{
    static public RateParser MathParserBoschHale = new RateParser()
	{
	    
	    /*
	    this evaluates <sigma*v>_f from the Bosch Hale relationship used by the fusion community
	    unit(sigm*v)=m^3/s, which is the reaction rate for dn/dt = k*n*n, i.e d+d->n+He3+energy
	    var is the temperature
	    d[0] is Bg, d[1] is mrc2
	    */
	    @Override
	    public double eval (double var, double c[], double d[])
	    {
		double T = var*Constants.KtoEV*1e-3;	//convert to kEv
		double rate;
		double Bg = d[0];
		double mrc2 = d[1];
		//set coefficients, anything not specified is assumed to be zero
		double C1 = c.length>=1?c[0]:0;
		double C2 = c.length>=2?c[1]:0;
		double C3 = c.length>=3?c[2]:0;
		double C4 = c.length>=4?c[3]:0;
		double C5 = c.length>=5?c[4]:0;
		double C6 = c.length>=6?c[5]:0;
		double C7 = c.length>=7?c[6]:0;
		
		double theta = T/(1-T*(C2+T*(C4+T*C6))/(1+T*(C3+T*(C5+T*C7))));
		double xi = Math.pow(Bg*Bg/(4*theta),(1/3.0));
		rate = 1e-6*C1*theta*Math.sqrt(xi/(mrc2*T*T*T))*Math.exp(-3*xi);
		return rate;
		
	    }
	};
	
}
