/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

/*
 * Physical constants used by the simulation * 
 */
package starfish.core.common;

public class Constants 
{
    static public final double EPS0 = 8.85418782e-12;	    /*permittivity of free space*/
    static public final double K =  1.3806503e-23;	    /*boltzmann constant*/
    static public final double QE = 1.60217646e-19;	    /*elementary charge*/
    static public final double AMU = 1.66053886e-27;	/*atomic mass unit*/
    static public final double ME = 9.10938e-31;	    /*electron mass*/
    static public final double FLT_EPS = 1e-6;		    /*epsilon for floating comparisons*/
    static public final double PI = Math.PI;
    static public final double KtoEV = K/QE;
    static public final double EVtoK = 1.0/KtoEV;
    static public final double SQRT2 = Math.sqrt(2);
    static public final double BOHR = 5.29e-11;	    /*bohr radius*/
}
