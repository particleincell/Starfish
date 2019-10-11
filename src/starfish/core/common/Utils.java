/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.common;

import java.util.ArrayList;
import java.util.Collections;
import starfish.core.common.Starfish.Log;

/** Common utilities */
public class Utils 
{
    /**
     * @param v_th * @return Samples 1D Maxwellian DF using Birdsall's method, needs be multiplied by v_th to get velocity
    *
    * fM = (R_1+R_2+...+R_M - M/2)*(M/12)^(-1/2)
    * 
    * for M=12, the second term drops out
    * 
    * Due to difference in Birdsall's v_th, need to multiply by sqrt(1/2)
    */

    public static double SampleMaxw1D(double v_th)
    {
	double sum=0;
	final int M=12;
	for (int i=0;i<M;i++)
	    sum += Starfish.rnd();
		
	double fm = (sum - 0.5*M)*Math.sqrt(12.0/M);	/*M/12 = 1, so not included*/	
	return Math.sqrt(0.5)*v_th*fm;
    }

    /*samples 3 1d maxwellians
    * TODO: this works, but perhaps sampling from 1D speed and rotation works better */

    /**
     *
     * @param v_th
     * @return
     */

    public static double[] SampleMaxw3D(double v_th) 
    {
	double vel[] = new double[3];
	vel[0] = SampleMaxw1D(v_th);
	vel[1] = SampleMaxw1D(v_th);
	vel[2] = SampleMaxw1D(v_th);
	return vel;	
    }
    
    /*samples speed from Maxwellian
    f(v) = 4/(sqrt(pi)*v_th^3)*v^2*exp(-v^2/v_th^2)
    */

    /**
     *
     * @param v_th
     * @return
     */

    public static double SampleMaxwSpeed(double v_th)
    {
	double bin_max = 6*v_th;

	double a = 4/(Math.sqrt(Math.PI)*v_th*v_th*v_th);
    
	//this is fm(v_th)
	double fm_max = 4/(Math.sqrt(Math.PI)*v_th)*Math.exp(-1);
    
	while(true)
	{
	    //pick random velocity between bin_min and bin_max
	    double v = Starfish.rnd()*bin_max;	

	    //compare against distribution function
	    double fm = a * v*v * Math.exp(-v*v/(v_th*v_th));
	    if ((fm/fm_max)>Starfish.rnd()) return v;
	}
    }

    /**
     *
     * @param mag
     * @param norm
     * @param tang1
     * @return returns velocity corresponding to a diffuse reflection from surface per Bird's alg
     */

    public static double[] diffuseReflVel(double mag, double norm[], double tang1[])
    {
	/*Bird uses RF(0) to generate (0,1), can't take log(0)*/
	double v_norm = Math.sqrt(-Math.log(Starfish.rndEx0()))*mag;
	  
	/*based on REFLECT2 in DSMC2.f*/
	double A = Math.sqrt(-Math.log(Starfish.rndEx0()));
	double B = 2*Math.PI*Starfish.rnd();
	double v_tang1 = A*Math.sin(B)*mag;
	double v_tang2 = A*Math.cos(B)*mag;
          
	double tang2[] = {0,0,1};
	    
	double vel[] = new double[3];
	for (int i=0;i<3;i++)
	    vel[i] = v_norm*norm[i] + v_tang1*tang1[i] + v_tang2*tang2[i];    	
	return vel;
    }
    
    /**
     *
     * @param mag
     * @return
     */
    public static double[] isotropicVel(double mag)
    {
	/*pick a random angle*/
	double theta = 2*Math.PI*Starfish.rnd();
 
	/*pick a random direction for n[2]*/
	double R = -1.0+2*Starfish.rnd();
	double a = Math.sqrt(1-R*R);
 
	double amag = (mag>0)?mag:-mag;
	double v[] = new double[3];
	v[0] = Math.cos(theta)*a*amag;
	v[1] = Math.sin(theta)*a*amag;
	v[2] = R*amag;
	return v;
    }
    
    /** computes thermal velocit
     * @param tempy
     * @param mass
     * @return */
    public static double computeVth(double temp, double mass)
    {
	return Math.sqrt(2*Constants.K*temp/mass);
    }

    /** parses integer with exception catchin
     * @param string
     * @return g*/
    public static int parseInt(String string) 
    {
	try {
	    return Integer.parseInt(string);
	}
	catch (NumberFormatException e)
	{
	    Log.error("Expected integer but found "+string);
	}
	
	/*this will not get executed*/
	return 0;
    }
	
    /** parses double with exception catchin
     * @param string
     * @return g*/
    public static double parseDouble(String string) 
    {
	try {
	    return Double.parseDouble(string);
	}
	catch (NumberFormatException e)
	{
	    Log.error("Expected double but found "+string);
	}
	
	/*this will not get executed*/
	return 0;
    }	
    
    /** returns val if between limits, otherwise returns one of the limits
     * 
     * @param val value to check
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return value or one of the limits
     */
    public static double minmax(double val, double min, double max)
    {
	return ((val<min?min:val)>max?max:val);
    }
    
    /** returns val if between limits, otherwise returns one of the limits
     * 
     * @param val value to check
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return value or one of the limits
     */
    public static int minmax(int val, int min, int max)
    {
    	if (val<min) return min;
    	else if (val>max) return max;
    	return val;
    }
    
    
    /*class for storing 1D list and interpolating*/

    /**
     *
     */

    

    
    //calculates the Gamma function of X per Bird's algorithm
    static public double gamma(double X)
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
    
    /*Gamma function from http://rosettacode.org/wiki/Gamma_function*/
    static double gamma_alt(double x)
    {
	double[] p = {0.99999999999980993, 676.5203681218851, -1259.1392167224028,
		    771.32342877765313, -176.61502916214059, 12.507343278686905,
		    -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
	int g = 7;
	if(x < 0.5) return Math.PI / (Math.sin(Math.PI * x)*gamma(1-x));

	x -= 1;
	double a = p[0];
	double t = x+g+0.5;
	for(int i = 1; i < p.length; i++){
		a += p[i]/(x+i);
	}

	return Math.sqrt(2*Math.PI)*Math.pow(t, x+0.5)*Math.exp(-t)*a;
    }
    
    /**
     * @param kTe Electron temperature in eV
     * @param ne electron density
     * @return Debye length
     */
    public static double debyeLength(double kTe, double ne) {
	return Math.sqrt(Constants.EPS0*kTe/(ne*Constants.QE));
    }
    
    /**
     * @param kTe Electron temperature in eV
     * @param ne electron density
     * @return Volume of a Debye sphere
     */
    public static double debyeVolume(double kTe, double ne) {
	double lambda_d = Utils.debyeLength(kTe,ne);
	return (4/3.0)*Math.PI*lambda_d*lambda_d*lambda_d;
    }
}
