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
	
	// shape information
	enum Shape {
		RECT, CIRCLE
	};

	Shape shape;
	double x0[], x1[]; // for RECT
	double radius; // for CIRCLE
	double xc[]; // shape center, used for volume calculation
	double volume;

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

		String shape_name = InputParser.getValue("shape", element);

		xc = new double[2];

		if (shape_name.equalsIgnoreCase("RECTANGLE") || shape_name.equalsIgnoreCase("RECT")) {
			shape = Shape.RECT;
			x0 = InputParser.getDoubleList("x0", element);
			x1 = InputParser.getDoubleList("x1", element);
			if (x0.length < 2 || x1.length < 2)
				Log.error("x0/x1 need to specify two values");
			xc[0] = x0[0] + 0.5 * (x1[0] - x0[0]);
			xc[1] = x0[1] + 0.5 * (x1[1] - x0[1]);

		} else if (shape_name.equalsIgnoreCase("CIRCLE")) {
			shape = Shape.CIRCLE;
			x0 = InputParser.getDoubleList("x0", element);
			radius = InputParser.getDouble("radius", element);
			if (x0.length < 2)
				Log.error("x0 needs to specify two values");

			xc[0] = x0[0];
			xc[1] = x0[1];
		} else
			Log.error("Unrecognized shape " + shape_name);

		volume = getVolume();
				
		/* log */
		Starfish.Log.log("Added MAXWELLIAN_VOLUME source '" + name + "'");
	}

	protected boolean first_time = true;

	// checks if the specified point is inside the shape
	protected boolean inShape(double x[]) {
		switch (shape) {
		case RECT:
			return (x[0] >= x0[0] && x[0] <= x1[0] && x[1] >= x0[1] && x[1] <= x1[1]);
		case CIRCLE:
			double dx = x[0] - x0[0];
			double dy = x[1] - x0[1];
			return (dx * dx + dy * dy) <= radius * radius;
		default:
			return false;
		}
	}

	// samples random position in a rectangle
	protected static double[] samplePosRect(double x1[], double x2[]) {
		double pos[] = new double[2];

		if (Starfish.getDomainType() == DomainType.XY) {
			pos[0] = x1[0] + Starfish.rnd() * (x2[0] - x1[0]);
			pos[1] = x1[1] + Starfish.rnd() * (x2[1] - x1[1]);
		} else if (Starfish.getDomainType() == DomainType.RZ) {
			double r1 = x1[0];  if (r1<0) r1=0;
			double r2 = x2[0];
			pos[0] = Math.sqrt(Starfish.rnd() * (r2 * r2 - r1 * r1) + r1 * r1);
			pos[1] = x1[1] + Starfish.rnd() * (x2[1] - x1[1]);
		} else if (Starfish.getDomainType() == DomainType.ZR) {
			double r1 = x1[1];  if (r1<0) r1=0;
			double r2 = x2[1];  
			pos[0] = x1[0] + Starfish.rnd() * (x2[0] - x1[0]);
			pos[1] = Math.sqrt(Starfish.rnd() * (r2 * r2 - r1 * r1) + r1 * r1);
		} else
			Log.error("Unsupported domain type");

		return pos;

	}

	// samples random position in the shape
	protected double[] samplePos() {
		double pos[] = new double[2];

		switch (shape) {
		case RECT:
			pos = samplePosRect(x0, x1);
			break;
		case CIRCLE:
			
			double theta =  Starfish.rnd()*2*Math.PI;
			double r = Math.sqrt(Starfish.rnd())*radius;
			pos[0] = x0[0]+Math.cos(theta)*r;
			pos[1] = x0[1]+Math.sin(theta)*r;
			break;
		}
		return pos;
	}

	/*
	 * returns the shape volume TODO: this needs a correction for fluid sampling
	 * since we end up sampling a sugarcubed region
	 */
	protected final double getVolume() {
		double perim = 1;

		if (Starfish.getDomainType() == DomainType.RZ)
			perim = 2 * Math.PI * xc[0];
		else if (Starfish.getDomainType() == DomainType.ZR)
			perim = 2 * Math.PI * xc[1];

		switch (shape) {
		case RECT:
			return (x1[0] - x0[0]) * (x1[1] - x0[1]) * perim;
		case CIRCLE:
			return Math.PI * radius * radius * perim;
		default:
			return 0;
		}

		// need something similar to below to compute volume for fluid material sampling
		/*
		 * if (first_time) { double vol=0; for (Mesh mesh:Starfish.getMeshList()) {
		 * dn.getField(mesh).setValue(den0); getTemp(mesh).setValue(temp0);
		 * 
		 * for (int i=0;i<mesh.ni-1;i++) for (int j=0;j<mesh.nj-1;j++)
		 * vol+=mesh.cellVol(i, j); }
		 */
	}

	@Override
	public void update() {
		// update mdot if specifying density
		if (density>0 && mdot0==0) {
			if (end_it<0 || (end_it-start_it)<=0) Log.error("density can be specified only for a non-zero start_it:end_it interval");
			double mass_gen = density*volume*source_mat.mass;   // total mass to generate
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
		double pos[] = samplePos();
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
