/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.boundaries;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.LinearList;
import starfish.core.common.Starfish;
import starfish.core.io.InputParser;
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
    /** boundary type (linear or cubic)*/
    protected final BoundaryType type;

    /** name of this boundary*/
    protected String name;

    /** boundary material*/
    protected Material material;

    /** boundary material index*/
    protected int mat_index;

    /** boundary temperature in K*/
    protected LinearList temp_list;	
    protected double temp;	//last evaluated temperature, saved so we don't need to re-evaluate for every particles
    
    /** Dirichlet value associated with this boundary*/
    protected LinearList value_list;
    protected double value;

    /**
     *
     * @param name
     * @param type
     * @param mat
     */
    public Boundary (String name, BoundaryType type, Material mat)
    {
	this.name = name;
	this.type = type;
	this.value_list = new LinearList();
	this.temp_list = new LinearList();	
	this.material = mat;
    }
    
    public Boundary (Element element) {
	/*get name and type*/
	name = InputParser.getValue("name", element);
	String type_name = InputParser.getValue("type", element,"solid");
		
	/*b.c.*/
	value_list = InputParser.getLinearList("value", "time",element,0);
			
	/*set boundary type*/
	if (type_name.equalsIgnoreCase("OPEN")) type = BoundaryType.OPEN;
	else if (type_name.equalsIgnoreCase("SOLID")) type = BoundaryType.DIRICHLET;
	//legacy support, symmetry should be set on meshes
	else if (type_name.equalsIgnoreCase("SYMMETRY")) type = BoundaryType.VIRTUAL;	
	else if (type_name.equalsIgnoreCase("VIRTUAL")) type = BoundaryType.VIRTUAL;
	else if (type_name.equalsIgnoreCase("SINK")) type = BoundaryType.SINK;
	else {Starfish.Log.error("Unknown boundary type "+type_name);type=null;}
		
	/*try to grab material, only require for dirichlet*/
	material = null;
	String mat_name = InputParser.getValue("material", element,"");
	try{
	    material = Starfish.getMaterial(mat_name);	
	}
	catch (Exception e) {
	    if (type == BoundaryType.DIRICHLET)
		Starfish.Log.error(e.getMessage());
	}
		
	/*also try to grab temperature*/
	temp_list = InputParser.getLinearList("temperature","time",element,273.15);
	
	update();	//set current value and temperature

	/*spline split?*/
	//int split = InputParser.getInt("split",element,1);
	
		
	/*log*/
	Starfish.Log.log("Added " + type_name + " boundary '"+name+"'");
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
	computeGeometry();
	
	/*set boundaries for segments*/
	for (int i=0;i<numSegments();i++)
	    this.getSegment(i).setParentInfo(this,i);	   
    }

    void setValues(LinearList values)
    {
	this.value_list = values;
    }
    /*sets boundary temperature*/
    void setTemperatures(LinearList temp) 
    {
	this.temp_list = temp;    	
    }

    /**updates boundary temperature and value
     * 
     * @return true if value changed. This then causes mesh information to be updated as well.
     */
    @Override
    final boolean update() {
	temp = temp_list.eval(Starfish.getTime());
	double value_old = value;
	value = value_list.eval(Starfish.getTime());
	return !(value==value_old);
    }
    
    /**@return boundary temperature at current simulation time*/
    public double getTemp() {return temp;}
    
    /**return thermal velocity
     * @param material
     * @return thermal velocity of the given material using surface temperature at current time*/
    public double getVth(Material material) {
	return Math.sqrt(2*Constants.K*getTemp()/material.getMass());
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
    public double getValue() {
	return value;}
 
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
    			
    
    
    

}
