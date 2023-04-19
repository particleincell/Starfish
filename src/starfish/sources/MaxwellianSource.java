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
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.common.Vector;
import starfish.core.common.Starfish.Log;
import starfish.core.source.Source;
import starfish.core.source.SourceModule;

/** Source that samples Maxwellian VDF */
public class MaxwellianSource extends Source {

	final double den0;
	final double v_drift;
	final double v_az; // azimuthal velocity
	final double v_th; // thermal velocity

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
	public MaxwellianSource(String name, Material source_mat, Boundary boundary, Element element) {
		super(name, source_mat, boundary, element);

		mdot0 = InputParser.getDouble("mdot", element, 0);
		double current = InputParser.getDouble("current", element, 0);
		double den = InputParser.getDouble("den", element,0);
		int num = 0;
		if (mdot0>0) num++; if (current>0) num++; if (den>0) num++;
		if (num!=1)
			Log.error("only one of <mdot>, <current>, and <den> can be specified");
		if (current != 0.0) {
			if (source_mat.charge == 0.0)
				Log.error("Cannot use <current> with neutral materials");
			mdot0 = Math.abs(current / source_mat.charge * source_mat.mass);
		}

		/* drift velocity and temperature */
		v_drift = InputParser.getDouble("v_drift", element);
		v_az = InputParser.getDouble("v_az", element,0.0);
		
		double temp = Double.parseDouble(InputParser.getValue("temperature", element));
		circuit_model = InputParser.getBoolean("circuit_model", element, false);

		/* calculate density */
		double A = boundary.area();
		if (den>0) {	// compute mdot if density specified
			mdot0 = den*(A*v_drift*source_mat.getMass());
		}
		
		den0 = mdot0 / (A * v_drift * source_mat.getMass());
		v_th = Utils.computeVth(temp, source_mat.getMass());
		
		Starfish.Log.log("Added MAXWELLIAN source '" + name + "'");
		Starfish.Log.log("> mdot   = " + mdot0 + "(kg/s)");
		Starfish.Log.log(String.format("> den0 = %.5g (#/m^3)", den0));
		Starfish.Log.log(String.format("> flux = %.5g (kg/s/m^2)", mdot0 / A));
		Starfish.Log.log("> spline  = " + boundary.getName());
		Starfish.Log.log("> temp  = " + temp);
		Starfish.Log.log("> v_drift  = " + v_drift);
		Starfish.Log.log("> v_th  = " + v_th);
		Starfish.Log.log("> v_az  = " + v_az);  //azimuthal velocity
		
		Starfish.Log.log("> start_it  = " + start_it);
		Starfish.Log.log("> end_it  = " + stop_it);
	}

	@Override
	public Particle sampleParticle() {
		Particle part = new Particle((KineticMaterial) source_mat);
		double t = boundary.randomT();

		double x[] = boundary.pos(t);
		double n[] = boundary.normal(t);

		/* copy values */
		part.pos[0] = x[0];
		part.pos[1] = x[1];
		part.pos[2] = 0;
		
		if (Starfish.getDomainType()==DomainType.RZ ||
			Starfish.getDomainType()==DomainType.ZR) {
			part.pos[2] = Starfish.rnd()*2*Math.PI;	//assign arbitrary theta angle
		}

		/* TODO: Move this to a wrapper! */
		do {
			double v_max[] = Utils.SampleMaxw3D(v_th);

			/* add drift */
			part.vel[0] = v_max[0] + n[0] * v_drift;
			part.vel[1] = v_max[1] + n[1] * v_drift;
			part.vel[2] = v_max[2] +v_az;

			part.dt = Starfish.rnd() * Starfish.getDt();
		} while (Vector.dot2(n, part.vel) <= 0);

		num_mp--;

		return part;
	}

	@Override
	public void sampleFluid() {
		/* TODO: this is hardcoded, generalize */

		// for (Mesh mesh:Starfish.getMeshList())
		Mesh mesh = Starfish.getMeshList().get(0);

		double den[][] = source_mat.getDen(mesh).getData();
		double U[][] = source_mat.getU(mesh).getData();
		double V[][] = source_mat.getV(mesh).getData();

		int i = 0;
		for (int j = 5; j < 9; j++) {
			den[i][j] = den0;
			U[i][j] = 0.5 * v_drift;
			V[i][j] = 0.5 * v_drift;
		}
	}

	/**
	 *
	 */
	static public SourceModule.SurfaceSourceFactory maxwellianSourceFactory = new SourceModule.SurfaceSourceFactory() {
		@Override
		public void makeSource(Element element, String name, Boundary boundary, Material material) {
			MaxwellianSource source = new MaxwellianSource(name, material, boundary, element);
			boundary.addSource(source);

		}

	};

}
