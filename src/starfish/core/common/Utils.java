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
	double Rsum=0;
		
	for (int i=0;i<12;i++)
	    Rsum += Starfish.rnd();
		
	double fm = Rsum - 6.0;	/*M/12 = 1, so not included*/	
	double mag = Math.sqrt(0.5)*v_th*fm;
		
	return mag;
    }

    /*samples 3 1d maxwellians
    * TODO: test if this works or if we need to do rotation of 1D speed distribution	 */
    public static double[] SampleMaxw3D(double v_th) 
    {
	double v_max[] = new double[3];
		
/*	v_max[0] = SampleMaxw1D(v_th);
	v_max[1] = SampleMaxw1D(v_th);
	v_max[2] = SampleMaxw1D(v_th);
*/	
	/*pick a random angle*/
	double theta = 2*Math.PI*Math.random();
 
	/*pick a random direction for n[2]*/
	double R = -1.0+2*Math.random();
	double a = Math.sqrt(1-R*R);
 
	/*double mag1 = SampleMaxw1D(v_th);   
	double mag2 = SampleMaxw1D(v_th);   
	double mag3 = SampleMaxw1D(v_th);   
	double mag = Math.sqrt(mag1*mag1+mag2*mag2+mag3*mag3);*/
	
	double mag= SampleMaxw1D(v_th);
	
	v_max[0] = mag * Math.cos(theta)*a;
	v_max[1] = mag * Math.sin(theta)*a;
	v_max[2] = mag *R;
		
	return v_max;
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
