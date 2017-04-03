/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.interactions;

import starfish.core.boundaries.Segment;
import starfish.core.common.Starfish;
import starfish.core.common.Utils;
import starfish.core.common.Vector;
import starfish.core.materials.Material;

/** material interaction */
public class MaterialInteraction
{
    int target_mat_index;		/*target*/
    int source_mat_index;
    int product_mat_index;
	
    double probability;
	
    Material target_mat;
    Material source_mat;
    Material product_mat;

    /**constructor*/
    public MaterialInteraction()
    {
	target_mat_index=-1;
	source_mat_index=-1;
	product_mat_index=-1;
	probability = 0.0;		/*default is zero*/
	surface_impact_handler = SurfaceInteraction.getSurfaceImpactModel("ABSORB");
    }

    /**sets probability*/
    public void setProbability(double prob) 
    {
	probability = prob;
    }
	
    public double getProbability() {return probability;}
	
    /*interaction interface*/
    public interface SurfaceImpactHandler 
    {
	/**
	* @param vel		incident velocity
	* @param segment	intersection spline
	* @param t_int		parametric  intersection location
	* @return false if particle is absorbed
	*/
	public boolean perform(double vel[], Segment segment, double t_int, MaterialInteraction mat_int);
    }
	
    SurfaceImpactHandler surface_impact_handler;
	
    public void setSurfaceImpactHandler(SurfaceImpactHandler surface_impact_handler) {this.surface_impact_handler = surface_impact_handler;}
    public boolean callSurfaceImpactHandler(double vel[], Segment segment, double t_int) 
    {
	return surface_impact_handler.perform(vel, segment, t_int, this);
    }

    public void setTargetMatIndex(int mat_index) {target_mat_index=mat_index;target_mat=Starfish.getMaterial(mat_index);}
    public int getTargetMatIndex() {return target_mat_index;}

    public void setSourceMatIndex(int mat_index) {source_mat_index=mat_index;source_mat=Starfish.getMaterial(mat_index);}
    public int getSourceMatIndex() {return source_mat_index;}

    public void setProductMatIndex(int mat_index) {product_mat_index=mat_index;product_mat=Starfish.getMaterial(mat_index);}
    public int getProductMatIndex() {return product_mat_index;}

    double c_rest;			/*coefficient of restitution*/
    double c_accom;			/*thermal accomodation coefficient*/

    public void setRestitutionCoefficient(double c_rest) {this.c_rest = c_rest;}
    public void setAccomodationCoefficient(double c_accom) {this.c_accom = c_accom;}

    public double getRestitutionCoefficient() {return c_rest;}
    public double getAccomodationCoefficient() {return c_accom;}
};
