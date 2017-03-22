/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.sources;

import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Spline;
import starfish.core.common.Starfish;
import starfish.core.common.Utils;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.common.Vector;
import starfish.core.source.Source;
import starfish.core.source.SourceModule;

/** Source that samples Maxwellian VDF*/
public class MaxwellianSource extends Source
{

    final double den0;
    final double v_drift;
    final double v_th;			/*thermal velocity*/


    public MaxwellianSource(String name, Material source_mat, Spline spline,
	    double mdot, double v_drift, double temp, int start_it)
    {
	super(name, source_mat, spline, mdot,start_it);

	/*calculate density*/
	double A = spline.area();
	den0 = mdot / (A * v_drift * source_mat.getMass());

	this.v_drift = v_drift;

	v_th = Utils.computeVth(temp, source_mat.getMass());
    }

    @Override
    public Particle sampleParticle()
    {
	Particle part = new Particle((KineticMaterial) source_mat);
	double t = spline.randomT();
	double x[] = spline.pos(t);
	double n[] = spline.normal(t);

	/*copy values*/
	part.pos[0] = x[0];
	part.pos[1] = x[1];
	part.pos[2] = 0;

	/*TODO: Move this to a wrapper!*/
	do
	{
	    double v_max[] = Utils.SampleMaxw3D(v_th);

	    /*add drift*/
	    part.vel[0] = v_max[0] + n[0] * v_drift;
	    part.vel[1] = v_max[1] + n[1] * v_drift;
	    part.vel[2] = v_max[2];
	    
	    part.dt=0.5*Starfish.getDt();
	} while (Vector.dot2(n, part.vel) <= 0);

	num_mp--;
	
	return part;
    }

    @Override
    public void sampleFluid()
    {
	/*TODO: this is hardcoded, generalize*/
	
	//for (Mesh mesh:Starfish.getMeshList())
	Mesh mesh = Starfish.getMeshList().get(0);

	double den[][] = source_mat.getDen(mesh).getData();
	double U[][] = source_mat.getU(mesh).getData();
	double V[][] = source_mat.getV(mesh).getData();

	int i = 0;
	for (int j = 5; j < 9; j++)
	{
	    den[i][j] = den0;
	    U[i][j] = 0.5 * v_drift;
	    V[i][j] = 0.5 * v_drift;
	}
    }
    
    static public SourceModule.SurfaceSourceFactory maxwellianSourceFactory = new SourceModule.SurfaceSourceFactory()
    {
	@Override
	public void makeSource(Element element, String name, Boundary boundary, Material material)
	{
	    double mdot = Double.parseDouble(InputParser.getValue("mdot", element));

	    /*drift velocity and temperature*/
	    double v_drift = Double.parseDouble(InputParser.getValue("v_drift", element));
	    double temp = Double.parseDouble(InputParser.getValue("temp", element));
	    int start_it = InputParser.getInt("start_it",element,0);
	    
	    MaxwellianSource source = new MaxwellianSource(name, material, boundary, mdot, v_drift, temp, start_it);
	    boundary.addSource(source);

	    /*log*/
	    Starfish.Log.log("Added MAXWELLIAN source '" + name + "'");
	    Starfish.Log.log("> mdot   = " + mdot + "(kg/s)");
	    Starfish.Log.log("> flux = " + mdot/boundary.area() + "(kg/s/m^2)");
	    Starfish.Log.log("> spline  = " + boundary.getName());
	    Starfish.Log.log("> v_drift  = " + v_drift);
	    Starfish.Log.log("> temp  = " + temp);
	}
	
    };

}
