/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.sources;

import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.common.Vector;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.source.Source;
import starfish.core.source.SourceModule;

/**
 *
 * @author Lubos Brieda
 */
public class VaporizationSource extends Source
{
    
    public VaporizationSource(String name, Material source_mat, Boundary boundary, Element element)
    {
	super(name,source_mat,boundary,element);
	
	/*make sure vaporization coefficients are defined*/
	if (boundary.getMaterial().p_vap_coeffs.length<3) 
	    Log.error("<p_vap_coeffs> need to be defined for material "+boundary.getMaterial().name);
	
	A = boundary.getMaterial().p_vap_coeffs[0];
	B = boundary.getMaterial().p_vap_coeffs[1];
	C = boundary.getMaterial().p_vap_coeffs[2];

	if (A == 0.0 && B == 0.0 && C == 0.0)
	    Log.error("<p_vap_coeffs> are all zero for material "+boundary.getMaterial().name);
	
	Log.log("Added VAPORIZATION_SOURCE");
	Log.log(String.format("log10(Pvap) = %g + %g/T + %g*log10(T)", A,B,C));
	Log.log(String.format("Boundary surface = %s",boundary.getMaterial().getName()));
    }
    
    @Override
    public KineticMaterial.Particle sampleParticle()
    {
	KineticMaterial.Particle part = new KineticMaterial.Particle((KineticMaterial) source_mat);
	double t = boundary.randomT();
	
	double x[] = boundary.pos(t);
	double normal[] = boundary.normal(t);
	double tang[] = boundary.tangent(t);

	/*copy values*/
	part.pos[0] = x[0];
	part.pos[1] = x[1];
	part.pos[2] = 0;

	double v_mag = Utils.SampleMaxwSpeed(v_th);
	/*TODO: Move this to a wrapper!*/
	do
	{    
	    part.vel = Utils.diffuseReflVel(v_mag, normal,tang);

	    part.dt=Starfish.rnd()*Starfish.getDt();
	} while (Vector.dot2(normal, part.vel) <= 0);

	num_mp--;
	
	return part;
    }

    @Override
    public void sampleFluid()
    {
	throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void update()
    {
	/*number density of titanium*/
	T_surf = boundary.getTemp();
	double f = A + B/T_surf + C*Math.log10(T_surf);
	double p_vap = Math.pow(10,f);	//liquid from https://en.wikipedia.org/wiki/Vapor_pressures_of_the_elements_(data_page)
	num_den = p_vap/(2*Constants.K*T_surf);		//eq. 1 in Benilov
	
	v_th = boundary.getVth(source_mat);
	
    }
    
    public double mdot(double time)
    {
	/*mdot is (J/e)*A*mass */
	double va_e =0.5*Math.sqrt(8*Constants.K*T_surf/(Constants.PI*source_mat.mass));
	double Ndot = num_den * boundary.area() * va_e;
	return Ndot * source_mat.mass;	 
    }
	
    double num_den;
    double T_surf;
    double v_th;
    double A, B, C;
    
    static public SourceModule.SurfaceSourceFactory vaporizationSource = new SourceModule.SurfaceSourceFactory()
    {
	@Override
	public void makeSource(Element element, String name, Boundary boundary, Material material)
	{
	    VaporizationSource source = new VaporizationSource(name, material, boundary, element);
	    
	    boundary.addSource(source);
	}
	
    };
}
