/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * Implements Schottsky thermionic emission of electrons per https://en.wikipedia.org/wiki/Thermionic_emission
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
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;

import starfish.core.source.Source;
import starfish.core.source.SourceModule;

/**
 * Implements Schottsky thermionic emission of electrons per https://en.wikipedia.org/wiki/Thermionic_emission
 * @author Lubos Brieda
 */
public class ThermionicEmissionSource extends Source
{
    public ThermionicEmissionSource(String name, Material source_mat, Boundary boundary, Element element)
    {
	super(name, source_mat, boundary, element);
	A0 = 4*Constants.PI*Constants.ME*Constants.K*Constants.K*Constants.QE/(Constants.H*Constants.H*Constants.H);
	AG = 0.5*A0;
	
	double x0[] = boundary.pos(0.5);    /*midpoint location*/
	mesh_x0 = Starfish.domain_module.getMesh(x0);
	if (mesh_x0==null)
	    Log.error("Source boundary "+boundary.getName()+" is outside the computational domain");
	lc_x0 = mesh_x0.XtoL(x0);
	normal_x0 = boundary.normal(0.5);	
    }
    
    final double A0;
    final double AG;
    double v_th;
    final Mesh mesh_x0;	//mesh containing the spline midpoint
    final double lc_x0[];	//logical coordinate of the spline midpoint
    final double normal_x0[];	//surface normal at the spline midpoint
    private double J_te;	//emission current density at present time, updated by regenerate()

    @Override
    /**
     * Updates mdot
     */
    public double mdot(double time)
    {
	/*mdot is (J/e)*A*mass */
	double ndot = (J_te/Constants.QE)*(boundary.area());	
	return ndot*source_mat.mass;	
    }
    
    @Override
    public Particle sampleParticle()
    {
	Particle part = new Particle((KineticMaterial) source_mat);
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
	double va_e =0.5*Math.sqrt(8*Constants.K*boundary.getTemp()/(Constants.PI*source_mat.mass));
	double nd_e = J_te/(Constants.QE*va_e);
	
	/*set density and velocity on inlet nodes*/	
    }
    
    @Override
    /**
     * computes new emission current density
     */
    public void update()
    {
	/*update surface temperature and thermal velocity*/
	double T_surf = boundary.getTemp();
	v_th = boundary.getVth(source_mat);
	
	double ef[] = {Starfish.domain_module.getEfi(mesh_x0).gather(lc_x0), 
	               Starfish.domain_module.getEfj(mesh_x0).gather(lc_x0),0};
    	
	/*magnitude of normal electric field at the surface*/
	double E_mag = -Vector.dot(normal_x0, ef);	//negative sign since negative field needed to pull electrons
	double W = boundary.getMaterial().getWorkFunction();
	double DW = Math.sqrt(Constants.QE*Constants.QE*Constants.QE*E_mag/(4*Constants.PI*Constants.EPS0));
	
	/*emission current density*/
	J_te = AG*T_surf*T_surf*Math.exp(-(W - DW)/(Constants.K*T_surf));
	if (J_te<0) J_te = 0;	
    }
    
    static public SourceModule.SurfaceSourceFactory thermionicEmissionSource = new SourceModule.SurfaceSourceFactory()
    {
	@Override
	public void makeSource(Element element, String name, Boundary boundary, Material material)
	{
	    ThermionicEmissionSource source = new ThermionicEmissionSource(name, material, boundary, element);
	    
	    boundary.addSource(source);
	}
	
    };
}
