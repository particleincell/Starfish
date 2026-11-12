/* Implements a volume source that loads particles within a specified 
 * region described by a square or a circle
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.sources;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.Shape;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.source.SourceModule;
import starfish.core.source.VolumeSource;

/**
 *
 * @author Lubos Brieda
 */
public class VolumeMaxwellianSource extends VolumeSource {

	protected double v_th;
	protected double v_drift[];
	protected int start_it;
	protected int end_it;
	protected int mp_count = 0;
	protected double density; 
	
	Shape shape;
	/* constructor */

	/**
	 *
	 * @param name       source name
	 * @param source_mat source material
	 * @param element    XML element containing source information
	 */

	public VolumeMaxwellianSource(String name, Material source_mat, Element element) {
		super(name, source_mat);

		mdot0 = InputParser.getDouble("mdot", element, 0);
		double current = InputParser.getDouble("current", element, 0);
		mp_count = InputParser.getInt("mp_count", element,0);
		density = InputParser.getDouble("density", element, 0); // mdot is computed in update since dt() may not be set correctly yet
		
		if (mdot0 != 0.0 && current != 0.0 && mp_count != 0.0 && density!=0)
			Log.error("only one of <mdot>, <current>, <mp_count>, and <density> can be specified");
		if (current != 0.0) {
			if (source_mat.charge == 0.0)
				Log.error("Cannot use <current> with neutral materials");
			mdot0 = Math.abs(current / source_mat.charge * source_mat.mass);
		}

		/* drift velocity and temperature */
		v_drift = InputParser.getDoubleList("v_drift", element, new double[] { 0.0, 0.0 });
		if (v_drift.length != 2)
			Log.error("v_drift needs to be a 2D list");

		double T = Double.parseDouble(InputParser.getValue("temperature", element));
		start_it = InputParser.getInt("start_it", element, 0);
		end_it = InputParser.getInt("end_it", element, -1);

		v_th = Utils.computeVth(T, source_mat.getMass());

		shape = new Shape(InputParser.getChild("shape",element));
		
		/* log */
		Starfish.Log.log("Added MAXWELLIAN_VOLUME source '" + name + "'");
	}

	protected boolean first_time = true;

	@Override
	public void update() {
		// update mdot if specifying density
		if (density>0 && mdot0==0) {
			if (end_it<0 || (end_it-start_it)<=0) Log.error("density can be specified only for a non-zero start_it:end_it interval");
			double mass_gen = density*shape.getVolume()*source_mat.mass;   // total mass to generate
			mdot0 = mass_gen/(Starfish.getDt()*(end_it-start_it));
			Log.log("Assigning mdot = "+String.format("%.3g", mdot0)+" (kg/s)");
		}
	}
	@Override
	public void regenerate() {
		/* check for injection interval */
		if (Starfish.getIt() < start_it || (end_it > 0 && Starfish.getIt() > end_it)) {
			num_mp = 0;
			num_rem = 0;
			return;
		}

		if (source_mat instanceof KineticMaterial) {
			KineticMaterial km = (KineticMaterial) source_mat;
			double mp = (mdot(Starfish.time_module.getTime()) * Starfish.getDt()) / (km.getMass() * km.getSpwt0())
					+ mp_rem;
			if (mp_count>0) mp = mp_count;		//override if the actual number of macroparticles is specified
			
			num_mp = (int) mp;
			mp_rem = mp - num_mp;
		} else {
			num_mp = 0; /* for now? */
			mp_rem = 0;
		}
		//Log.message("Generating "+num_mp+" particles");
	}

	@Override
	public boolean hasParticles() {
		return num_mp > 0;
	}

	@Override
	public KineticMaterial.Particle sampleParticle() {
		KineticMaterial.Particle part = new KineticMaterial.Particle((KineticMaterial) source_mat);

		/* position */
		double pos[] = shape.samplePos();
		part.pos[0] = pos[0];
		part.pos[1] = pos[1];
		
		if (Starfish.getDomainType()==DomainType.XY)
			part.pos[2] = 0;
		else
			part.pos[2] = 0*Starfish.rnd()*2*Math.PI;		//random theta angle

		/* velocity */
		part.vel = Utils.SampleMaxw3D(v_th);

		/* add drifting velocity */
		part.vel[0] += v_drift[0];
		part.vel[1] += v_drift[1];

		num_mp -= 1;

		return part;
	}

	@Override
	public void sampleFluid() {
		throw new UnsupportedOperationException("Not yet implemented");
		/*
		 * for (Mesh mesh:Starfish.getMeshList()) { int ni = mesh.ni; int nj = mesh.nj;
		 * double den_data[][] = source_mat.getDen(mesh).getData(); double dn_local[][]
		 * = dn.getField(mesh).getData(); for (i_sample=0;i_sample<ni;i_sample++) for
		 * (j_sample=0;j_sample<nj;j_sample++)
		 * den_data[i_sample][j_sample]+=dn_local[i_sample][j_sample];
		 * 
		 * }
		 */
	}

	/**
	 * 
	 *
	 */
	static public SourceModule.VolumeSourceFactory volumeMaxwellianSourceFactory = new SourceModule.VolumeSourceFactory() {
		@Override
		public void makeSource(Element element, String name, Material material) {
			VolumeMaxwellianSource source = new VolumeMaxwellianSource(name, material, element);
			Starfish.source_module.addVolumeSource(source);

		}
	};

}
