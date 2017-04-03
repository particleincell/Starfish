/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import starfish.core.common.Starfish.Log;

/** Common utilities */
public class Utils 
{
    /**@return Samples 1D Maxwellian DF using Birdsall's method, needs be multiplied by v_th to get velocity
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

    /*returns velocity corresponding to a diffuse reflection from surface per Bird's alg*/
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
    
    /** computes thermal velocity*/
    public static double computeVth(double temp, double mass)
    {
	return Math.sqrt(2*Constants.K*temp/mass);
    }

    /** parses integer with exception catching*/
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
	
    /** parses double with exception catching*/
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
    
    
    /*class for storing 1D list and interpolating*/
    public static class LinearList
    {
	public ArrayList<XYData> data = new ArrayList();
	
	public void insert(double x, double y)
	{
	    data.add(new XYData(x,y));
	    dirty = true;
	}
	
	private boolean dirty = true;
	public double eval(double x)
	{
		if (data.isEmpty()) return 0;
		if (dirty) {
		    Collections.sort(data);
		    dirty=false;
		}
		
		int data_index = 0; /*this can be made a global if we know it will be called in sequence*/
		while(true)
		{
		    XYData current = data.get(data_index);
		    if (data_index<data.size()-1)
		    {
			XYData next = data.get(data_index+1);
			if (x>next.x)
			    data_index++;
			else
			{
			    double t = (x-current.x)/(next.x-current.x);
			    if (t<0) t=0;
					
			    /*interpolate*/
			    return current.y+t*(next.y-current.y);
			}			
		    }
		    else return current.y;	/*reached end of data*/
		}
	}

	public class XYData implements Comparable
	{
	    public double x;
	    public double y;
	    XYData(double x, double y) {this.x = x; this.y=y;}

	    @Override
	    public int compareTo(Object o)
	    {
		XYData t2 = (XYData)o;
		
		if (x<t2.x) return -1;
		else if (x==t2.x) return 0;
		else return 1;
	    }
	}

    }

    
}
