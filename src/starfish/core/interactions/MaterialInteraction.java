/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.interactions;

import starfish.core.boundaries.Segment;
import starfish.core.common.Starfish;
import starfish.core.materials.KineticMaterial;
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
    
    KineticMaterial target_km_mat;
    KineticMaterial source_km_mat;
    KineticMaterial product_km_mat;

    /**constructor*/
    public MaterialInteraction()
    {
	target_mat_index=-1;
	source_mat_index=-1;
	product_mat_index=-1;
	probability = 0.0;		/*default is zero*/
	surface_impact_handler = SurfaceInteraction.getSurfaceImpactModel("ABSORB");
    }

    /**sets probability
     * @param prob*/
    public void setProbability(double prob) 
    {
	probability = prob;
    }
	
    /**
     *
     * @return
     */
    public double getProbability() {return probability;}
	
    /*interaction interface*/

    /**
     *
     */

    public interface SurfaceImpactHandler 
    {
	/**
	* @param vel		incident velocity
	* @param segment	intersection spline
	* @param spwt		specific weight of the impacting particle
	* @param t_int		parametric  intersection location
	 * @param mat_int       material interaction structure
	* @return false if particle is absorbed
	*/
	public boolean perform(double vel[], double spwt, Segment segment, double t_int, MaterialInteraction mat_int);
    }
	
    SurfaceImpactHandler surface_impact_handler;
	
    /**
     *
     * @param surface_impact_handler
     */
    public void setSurfaceImpactHandler(SurfaceImpactHandler surface_impact_handler) {this.surface_impact_handler = surface_impact_handler;}

    /**
     *
     * @param vel	velocity of the impacting particle
     * @param spwt      weight of the impacting particle
     * @param segment
     * @param t_int
     * @return
     */
    public boolean callSurfaceImpactHandler(double vel[], double spwt, Segment segment, double t_int) 
    {
	return surface_impact_handler.perform(vel, spwt, segment, t_int, this);
    }

    /**
     *
     * @param mat_index
     */
    public void setTargetMatIndex(int mat_index) {
	target_mat_index=mat_index;
	target_mat=Starfish.getMaterial(mat_index);
	target_km_mat=Starfish.getKineticMaterial(mat_index);	
    }

    /**
     *
     * @return
     */
    public int getTargetMatIndex() {return target_mat_index;}

    /**
     *
     * @param mat_index
     */
    public void setSourceMatIndex(int mat_index) {
	source_mat_index=mat_index;
	source_mat=Starfish.getMaterial(mat_index);
	source_km_mat=Starfish.getKineticMaterial(mat_index);
    }

    /**
     *
     * @return
     */
    public int getSourceMatIndex() {return source_mat_index;}

    /**
     *
     * @param mat_index
     */
    public void setProductMatIndex(int mat_index) {
	product_mat_index=mat_index;
	product_mat=Starfish.getMaterial(mat_index);
	product_km_mat=Starfish.getKineticMaterial(mat_index);
    }

    /**
     *
     * @return
     */
    public int getProductMatIndex() {return product_mat_index;}

    double c_rest;			/*coefficient of restitution*/
    double c_accom;			/*thermal accomodation coefficient*/

    /**
     *
     * @param c_rest
     */
    public void setRestitutionCoefficient(double c_rest) {this.c_rest = c_rest;}

    /**
     *
     * @param c_accom
     */
    public void setAccomodationCoefficient(double c_accom) {this.c_accom = c_accom;}

    /**
     *
     * @return
     */
    public double getRestitutionCoefficient() {return c_rest;}

    /**
     *
     * @return
     */
    public double getAccomodationCoefficient() {return c_accom;}
};
