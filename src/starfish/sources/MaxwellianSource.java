/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.sources;

import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
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

    /**
     *
     * @param name
     * @param source_mat
     * @param boundary
     * @param mdot
     * @param v_drift
     * @param temp
     * @param start_it
     * @param end_it
     */
    public MaxwellianSource(String name, Material source_mat, Boundary boundary,
	    double mdot, double v_drift, double temp, int start_it, int end_it)
    {
	super(name, source_mat, boundary, mdot,start_it,end_it);

	/*calculate density*/
	double A = boundary.area();
	den0 = mdot / (A * v_drift * source_mat.getMass());

	this.v_drift = v_drift;

	v_th = Utils.computeVth(temp, source_mat.getMass());	
	
	Starfish.Log.log("Added MAXWELLIAN source '" + name + "'");
	Starfish.Log.log("> mdot   = " + mdot + "(kg/s)");
	Starfish.Log.log(String.format("> den0 = %.5g (#/m^3)",den0));
	Starfish.Log.log(String.format("> flux = %.5g (kg/s/m^2)",mdot/A));
	Starfish.Log.log("> spline  = " + boundary.getName());
	Starfish.Log.log("> temp  = " + temp);
	Starfish.Log.log("> v_drift  = " + v_drift);
	Starfish.Log.log("> v_th  = " + v_th);
	Starfish.Log.log("> start_it  = " + start_it);
	Starfish.Log.log("> end_it  = " + end_it);
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
	    
	    part.dt=Starfish.rnd()*Starfish.getDt();
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
    
    /**
     *
     */
    static public SourceModule.SurfaceSourceFactory maxwellianSourceFactory = new SourceModule.SurfaceSourceFactory()
    {
	@Override
	public void makeSource(Element element, String name, Boundary boundary, Material material)
	{
	    double mdot = Double.parseDouble(InputParser.getValue("mdot", element));

	    /*drift velocity and temperature*/
	    double v_drift = Double.parseDouble(InputParser.getValue("v_drift", element));
	    double temp = Double.parseDouble(InputParser.getValue("temperature", element));
	    int start_it = InputParser.getInt("start_it",element,0);
	    int end_it = InputParser.getInt("end_it",element,-1);
	    	    
	    MaxwellianSource source = new MaxwellianSource(name, material, boundary, mdot, v_drift, temp, start_it, end_it);
	    
	    /*use this source with the circuit model?*/
    	    source.circuit_model = InputParser.getBoolean("circuit_model", element, false);
	    
	    boundary.addSource(source);

	    
	}
	
    };

}
