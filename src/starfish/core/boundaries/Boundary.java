/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.boundaries;

import java.util.ArrayList;
import starfish.core.common.Constants;
import starfish.core.materials.Material;
import starfish.core.source.Source;

/*Boundary
 * 
 * Spline with additional information, such as boundary behavior 
 * and particle sources
 */

/**
 *
 * @author Lubos Brieda
 */

public class Boundary extends Spline
{

    /**
     *
     * @param name
     * @param type
     * @param value
     * @param mat
     */
    public Boundary (String name, BoundaryType type, double value, Material mat)
    {
	this.name = name;
	this.type = type;
	this.value = value;	
	this.material = mat;
    }

    /**named constants for boundary types*/
    static public enum BoundaryType    {
	DIRICHLET(0),
	OPEN(1),	
	SINK(2),
	VIRTUAL(3)
	; 
	
	protected int val;
	BoundaryType(int val) {this.val=val;}

	/** @return associated value */
	public int value() {return val;}	
    }
    
    /**allocates sources*/
    protected void init()
    {
	/*set boundaries for segments*/
	for (int i=0;i<numSegments();i++)
	    this.getSegment(i).setParentInfo(this,i);	   
    }

    /*sets boundary temperature*/
    void setTemp(double temp) 
    {
	this.temp = temp;    	
    }

    /**@return boundary temperature*/
    public double getTemp() {return temp;}
    
    /**return thermal velocity
     * @param material
     * @return thermal velocity of the given material using surface temperature*/
    public double getVth(Material material) {
	return Math.sqrt(2*Constants.K*temp/material.getMass());
    }
   
    /*accessors*/

    /**
     *
     * @return
     */

    public String getName() {return name;}

    /**
     *
     * @return
     */
    public BoundaryType getType() {return type;}

    /**
     *
     * @return
     */
    public double getValue() {return value;}
 
    /**@return boundary material*/
    public Material getMaterial() {return material;}
	
    /** returns material at spline position
     * @param t parametric position
     * @return */
    public Material getMaterial (double t) {return material;}	/*TODO: for now this assumes uniform mat*/
    
    /** returns material index
     * @return */
    public int getMaterialIndex() {return mat_index;}

    /** source list*/
    protected ArrayList<Source> source_list = new ArrayList<Source>();  

    /**
     *
     * @return
     */
    public ArrayList<Source> getSourceList() {return source_list;}

    /**
     *
     * @param source
     */
    public void addSource(Source source){source_list.add(source);}
 
    /**
     *
     * @param mat_index
     * @return
     */
    			
    /*variables*/

    /**
     *
     */

    protected final BoundaryType type;

    /**
     *
     */
    protected double value;

    /**
     *
     */
    protected String name;

    /**
     *
     */
    protected Material material;

    /**
     *
     */
    protected int mat_index;

    /**
     *
     */
    protected double temp;		/*temparture in Kelvin*/
    
    

}
