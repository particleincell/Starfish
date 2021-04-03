/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.materials;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Boundary.BoundaryType;
import starfish.core.boundaries.Segment;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.MeshBoundaryData;
import starfish.core.domain.Mesh.Face;
import starfish.core.domain.Mesh.Node;
import starfish.core.domain.UniformMesh;
import starfish.core.io.InputParser;
import starfish.core.materials.MaterialsModule.MaterialParser;
import starfish.core.common.Vector;
import starfish.core.domain.Mesh.DomainBoundaryType;

/** definition of particle-based material */
public class KineticMaterial extends Material {

	int particle_merge_skip; // number of time steps between particle merges, on -1 to disable
	int vel_grid_dims[]; // number of velocity bins in (u,v,w) spanning the min/max of each
	int last_sort_to_cell_it; // time step of the last sort to cells

	public KineticMaterial(String name, Element element) {
		super(name, element);

		/* kinetic material also need spwt */
		spwt0 = InputParser.getDouble("spwt", element, 1.0);

		/* support for particle merging */
		particle_merge_skip = InputParser.getInt("particle_merge_skip", element, -1);
		if (particle_merge_skip > 0) {
			vel_grid_dims = InputParser.getIntList("vel_grid_dims", element);
			if (vel_grid_dims.length != 3)
				Log.error("vel_grid_dims must specify 3 integers");
		}

		/* log */
		Log.log("Added KINETIC material '" + name + "'");
		Log.log("> charge   = " + charge);
		Log.log("> mass = " + String.format("%.4g (kg)", mass));
		Log.log("> spwt = " + spwt0);

	}
	/* specific weight */

	/**
	 *
	 */

	protected double spwt0;
	int sampling_start_it = 0;

	/**
	 *
	 * @return
	 */
	public double getSpwt0() {
		return spwt0;
	}

	protected int part_id_counter = 0;		//counter for assigning consecutive particle ids, unique for each material

	@Override
	public void init() {
		/* call up */
		super.init();

		/* allocate memory */
		ArrayList<Mesh> mesh_list = Starfish.getMeshList();
		mesh_data = new MeshData[mesh_list.size()];
		for (int m = 0; m < mesh_list.size(); m++) {
			mesh_data[m] = new MeshData(mesh_list.get(m));
		}

		/* fields used to hold sampled data */
		field_manager2d.add_nosync("count-sum", "#", null);
		field_manager2d.add_nosync("u-sum", "m/s", null);
		field_manager2d.add_nosync("v-sum", "m/s", null);
		field_manager2d.add_nosync("w-sum", "m/s", null);
		field_manager2d.add_nosync("uu-sum", "m/s", null);
		field_manager2d.add_nosync("vv-sum", "m/s", null);
		field_manager2d.add_nosync("ww-sum", "m/s", null);

		/* temperature components */
		field_manager2d.add("t1", "K", null);
		field_manager2d.add("t2", "K", null);
		field_manager2d.add("t3", "K", null);

		/* macroparticles per cell */
		field_manager2d.add_nosync("mpc-sum", "#", null);
		field_manager2d.add_nosync("mpc", "#", null); // since cell-based

	}

	boolean first_time = true;

	@Override
	public void updateFields() {
		/* sampling before pushing particles to include contribution from sources */
		/* reset sampling when we reach steady state */
		if (Starfish.steady_state() && !steady_state) {
			clearSamples();
			steady_state = true;
		}

		/* first loop through all particles */
		moveParticles(false);

		/* now move transferred particle */
		// some number of loops so we don't run forever
		int count = 0;
		for (int loop = 0; loop < 10; loop++) {
			moveParticles(true);

			// count number of remaining particles waiting to transfer, ideally none
			count = 0;
			for (MeshData md : mesh_data)
				count += md.getTransferNp();
			if (count == 0)
				break;
		}
		if (count > 0)
			Log.warning("Failed to transfer all particles between domains!");

		/* merge particles if needed, this also sorts particles to cells */
		if (particle_merge_skip > 0 && Starfish.getIt() % particle_merge_skip == 0) {
			Log.log("Performing particle merge on material " + name);
			for (MeshData md : mesh_data)
				mergeParticles(md);
		}

		/* update densities and velocities */
		for (MeshData md : mesh_data) {
			updateFields(md);
		}

		/* update velocity moments, num_samples moved out for multi-domain cases */
		updateGasProperties();

		/* apply boundaries */
		updateBoundaries();

		first_time = false;
	}

	/**
	 * uses final particle positions to update fields
	 */
	protected void updateFields(MeshData md) {
		Field2D Den = getDen(md.mesh);
		Field2D U = getU(md.mesh);
		Field2D V = getV(md.mesh);
		Field2D W = getW(md.mesh);

		/* clear all fields */
		Den.clear();
		U.clear();
		V.clear();
		W.clear();

		Iterator<Particle> iterator = md.getIterator();
		while (iterator.hasNext()) {
			Particle part = iterator.next();

			Den.scatter(part.lc, part.mpw);
			U.scatter(part.lc, part.vel[0] * part.mpw);
			V.scatter(part.lc, part.vel[1] * part.mpw);
			W.scatter(part.lc, part.vel[2] * part.mpw);
		}

		/* first get average velocities */
		U.divideByField(Den);
		V.divideByField(Den);
		W.divideByField(Den);

		/* now turn density actually into density */
		Den.scaleByVol();
	}

	/* updates field on a single mesh */
	void moveParticles(boolean particle_transfer) {
		/* allocate iterators */
		ArrayList<ParticleMover> movers = new ArrayList<>();

		if (!particle_transfer)
		{
			for (MeshData md : mesh_data) 
				for (int block = 0; block < md.particle_block.length; block++) {
					Iterator<Particle> iterator = md.getIterator(block);
				
					/* don't bother adding empty blocks */
					if (iterator.hasNext()) {
						
						ParticleMover mover = new ParticleMover(md, this,  iterator, particle_transfer, "PartMover" + block);
						movers.add(mover);
					}
				} //block
		}
		else  //particle transfer
		{
			for (MeshData md : mesh_data) 
			{
				if (md.transfer_particles.isEmpty()) continue;
				
				//make a local copy so that we can add particles as needed without invalidating iterator
				ArrayList<Particle> tp_copy = new ArrayList<>(md.transfer_particles);				
				md.transfer_particles.clear();	//clear out the original list (this does not touch tp_copy - checked				
				ParticleMover mover = new ParticleMover(md, this, tp_copy.iterator(), particle_transfer, "PartMover_tp" );
				movers.add(mover);				
			}
			
		}


		/* move particles */
		for (ParticleMover mover : movers)
			mover.start();

		/* wait to finish */
		try {
			for (ParticleMover mover : movers)
				mover.join();
		} catch (InterruptedException ex) {
			Log.warning("Particle Mover thread interruption");
		}
				
		/* add up totals */
		if (!particle_transfer) {
			mass_sum = 0;
			Vector.set(momentum_sum, 0);
			energy_sum = 0;

			for (ParticleMover mover : movers) {
				mass_sum += mover.N_sum * mass;
				momentum_sum[0] += mover.P_sum[0] * mass;
				momentum_sum[1] += mover.P_sum[1] * mass;
				momentum_sum[2] += mover.P_sum[2] * mass;
				energy_sum += mover.E_sum * mass;
			}
		}

	}

	/* returns a particle iterator that iterates over all blocks */

	/**
	 *
	 * @param mesh
	 * @return
	 */

	public Iterator<Particle> getIterator(Mesh mesh) {
		return getMeshData(mesh).getIterator();
	}

	/* updates particles on a single block */
	class ParticleMover extends Thread {

		protected MeshData md;
		protected Iterator<Particle> iterator;
		protected boolean particle_transfer;
		double N_sum; // total number of physical particles
		double P_sum[] = new double[3]; // total momentum
		double E_sum; // total energy
		protected KineticMaterial km;	// the associated km

		private ParticleMover(MeshData md, KineticMaterial km, Iterator<Particle> iterator, boolean particle_transfer, String thread_name) {
			super(thread_name);

			this.md = md;
			this.iterator = iterator;
			this.particle_transfer = particle_transfer;
			this.km = km;
			N_sum = 0; // clear sums
			Vector.set(P_sum, 0);
			E_sum = 0;
		}

		@Override
		public void run() {
			final int max_bounces = 10; /* maximum number of surface bounces per step */
			double old[] = new double[2]; /* old physical coordinate */
			double old_lc[] = new double[2]; /* old logical coordinate */
			double ef[] = new double[3];
			double bf[] = new double[3];

			Mesh mesh = md.mesh;
			Field2D Efi = md.Efi;
			Field2D Efj = md.Efj;

			Field2D Bfi = md.Bfi;
			Field2D Bfj = md.Bfj;
			
			int p=0;
			while (iterator.hasNext()) {
			
				//periodically yield to prevent lock up
				if (++p%25==0) Thread.yield();
				
				Particle part = iterator.next();

				/* increment particle time and velocity */
				if (!particle_transfer) {
					part.dt += Starfish.getDt();

					/* update velocity */
					ef[0] = Efi.gather(part.lc);
					ef[1] = Efj.gather(part.lc);

					/* update velocity */
					bf[0] = Bfi.gather(part.lc);
					bf[1] = Bfj.gather(part.lc);

					/* update velocity */
					if (bf[0] == 0 && bf[1] == 0) {
						part.vel[0] += q_over_m * ef[0] * part.dt;
						part.vel[1] += q_over_m * ef[1] * part.dt;
					} else {
						UpdateVelocityBoris(part, ef, bf);
					}
				}

				int bounces = 0;
				boolean alive = true;

				/* iterate while we have time remaining */
				while (part.dt > 0 && bounces++ < max_bounces) {
					/* save old position */
					old[0] = part.pos[0];
					old[1] = part.pos[1];

					old_lc[0] = part.lc[0];
					old_lc[1] = part.lc[1];

					/* update position */
					part.pos[0] += part.vel[0] * part.dt;
					part.pos[1] += part.vel[1] * part.dt;

					switch (Starfish.getDomainType()) {
					case RZ:
						rotateToRZ(part);
						break;
					case ZR:
						rotateToZR(part);
						break;
					default:
						part.pos[2] += part.vel[2] * part.dt;
						break;
					}

					part.lc = mesh.XtoL(part.pos);

					/* check if particle hit anything or left the domain */
					alive = ProcessBoundary(part, mesh, old, old_lc);

					/* add post push/surface impact position to trace */
					if (part.has_trace)
						Starfish.particle_trace_module.addTrace(km, part);

					if (!alive) {
						iterator.remove();
						break;
					}

				} /* dt */

				/*
				 * compute total mass, momentum, energy for this block of particles note, here I
				 * am also including particles eventually going to the transfer bin. If we ever
				 * need particle counts on a mesh-by-mesh basis, we may need to perform this
				 * calculation post-transfer.
				 */
				if (alive) {
					/* save momentum for diagnostics, will be multiplied by mass in updatefields */
					N_sum += part.mpw;
					P_sum[0] += part.mpw * part.vel[0];
					P_sum[1] += part.mpw * part.vel[1];
					P_sum[2] += part.mpw * part.vel[2];
					E_sum += part.mpw * Vector.mag3(part.vel);
				}

				//add the particle to the main population if it is in the particle_transfer list
				if (alive && particle_transfer) {
					md.addParticle(part);
				}

			} /* end of particle loop */

		}

		private void rotateToRZ(Particle part) {
			/* movement in R plane */
			double A = part.vel[2] * part.dt; // theta is in -z direction
			double B = part.pos[0]; /* new position in R plane */
			double R = Math.sqrt(A * A + B * B); /* new radius */

			double cos = B / R;
			double sin = A / R;

			/* update particle theta, only used for visualization */
			part.pos[2] -= Math.asin(sin);

			/* rotate velocity through theta */
			double v1 = part.vel[0];
			double v2 = part.vel[2];
			part.pos[0] = R;
			part.vel[0] = cos * v1 + sin * v2;
			part.vel[2] = -sin * v1 + cos * v2;
		}

		private void rotateToZR(Particle part) {
			/* movement in R plane */
			double A = part.vel[2] * part.dt;
			double B = part.pos[1];
			double R = Math.sqrt(A * A + B * B); /* new radius */

			double cos = B / R;
			double sin = A / R;

			/* update particle theta, only used for visualization */
			part.pos[2] += Math.acos(cos);

			/* rotate velocity through theta */
			double v1 = part.vel[1];
			double v2 = part.vel[2];
			part.pos[1] = R;
			part.vel[1] = cos * v1 + sin * v2;
			part.vel[2] = -sin * v1 + cos * v2;
		}
	}

	/**
	 * checks for particle surface hits and/or domain escape
	 *
	 * @param id return value, contains info about impact location
	 * @return remaining dt, or -1 if absorbed
	 */
	boolean ProcessBoundary(Particle part, Mesh mesh, double old[], double lc_old[]) {
		Face exit_face;
		boolean alive = true;

		double dt0 = part.dt; /* initial time step */
		part.dt = 0; /* default, used up all time */

		/*
		 * capture bounding box of particle motion and particle position before pushing
		 * particle into domain
		 */
		double lc_min[] = new double[2];
		double lc_max[] = new double[2];
		lc_min[0] = Math.min(part.lc[0], lc_old[0]);
		lc_min[1] = Math.min(part.lc[1], lc_old[1]);
		lc_max[0] = Math.max(part.lc[0], lc_old[0]);
		lc_max[1] = Math.max(part.lc[1], lc_old[1]);

		int i_min = (int) lc_min[0];
		int i_max = (int) lc_max[0];
		int j_min = (int) lc_min[1];
		int j_max = (int) lc_max[1];

		/* verify above min/max are in range */
		if (i_min < 0)
			i_min = 0;
		if (j_min < 0)
			j_min = 0;
		if (i_max >= mesh.ni)
			i_max = mesh.ni - 1;
		if (j_max >= mesh.nj)
			j_max = mesh.nj - 1;

		/* assemble a list of segments in this block, using a set to avoid duplicates */
		Set<Segment> segments = new HashSet();

		/* make a set of all segments in the bounding box */
		for (int i = i_min; i <= i_max; i++)
			for (int j = j_min; j <= j_max; j++) {
				Node node = mesh.getNode(i, j);

				for (Segment seg : node.segments)
					if (seg.getBoundaryType() == BoundaryType.DIRICHLET ||
					// seg.getBoundaryType() == BoundaryType.VIRTUAL || /*9/2019 disabled virtual
					// here, not sure why being added, causes particle leaks
							seg.getBoundaryType() == BoundaryType.SINK)
						segments.add(seg);
			}

		/* iterate over the segments and find the first one to be hit */
		double tp_min = 2.0, tsurf_min = 0;
		Segment seg_min = null;
		for (Segment seg : segments) {
			/*
			 * t[0] is the location along the surface, t[1] is location along particle
			 * vector
			 */
			double t[] = seg.intersect(old, part.pos);
			double t_part = t[1];

			/* do we have an intersection, excluding starting point? */
			/*
			 * todo: need to consider velocity direction, only makes sense if moving away
			 * from surface
			 */
			if (t_part > 0) {
				/*
				 * skip over particles that collide with surface at the beginning of their time
				 * step, as long as they are moving away from the surface
				 */
				double acos = Vector.dot2(seg.normal(t[0]), part.vel) / Vector.mag2(part.vel);
				if (t_part < Constants.FLT_EPS && // ignore direction for virtual walls since particles can pass through
						(acos > 0 || seg.getBoundaryType() == BoundaryType.VIRTUAL))
					continue;

				/* is this a new minimum? */
				if (t_part < tp_min) {
					tp_min = t_part;
					tsurf_min = t[0];
					seg_min = seg;
				}
			}
		}

		/* TODO: 11/2018: why is virtual being added in the first place? */
		if (seg_min != null && seg_min.getBoundaryType() == BoundaryType.VIRTUAL)
			seg_min = null;

		/* perform intersection */
		if (seg_min != null) {
			/* don't go all the way to the surface to avoid numerical errors */
			tp_min *= 0.9999;

			/* move to surface (almost) */
			part.pos[0] = old[0] + tp_min * (part.pos[0] - old[0]);
			part.pos[1] = old[1] + tp_min * (part.pos[1] - old[1]);
			part.lc = mesh.XtoL(part.pos);

			/* set dt_rem */
			part.dt = dt0 * (1 - tp_min);

			// check for math errors, the issue here is that a particle could truly
			// first leave the mesh if the boundary is outside the domain
			if (part.lc[0] < 0 && part.lc[0] > -Constants.FLT_EPS)
				part.lc[0] = 0;
			if (part.lc[1] < 0 && part.lc[1] > -Constants.FLT_EPS)
				part.lc[1] = 0;

			/* call handler */
			Boundary boundary_hit = seg_min.getBoundary();
			Material target_mat = boundary_hit.getMaterial(tsurf_min);
			double boundary_t = seg_min.id() + tsurf_min;

			/* perform surface interaction */
			if (target_mat != null)
				alive = target_mat.performSurfaceInteraction(part.vel, part.mpw, mat_index, seg_min, tsurf_min);

			// track boundary charge for use with the circuit model
			if (!alive)
				Starfish.source_module.boundary_charge += part.mpw * charge;

			if (boundary_hit.getType() == BoundaryType.SINK)
				alive = false;

			/* deposit flux and deposit, if stuck */
			addSurfaceMomentum(boundary_hit, boundary_t, part.vel, part.mpw);

			if (!alive) {
				/* we will multiply by mass in "finish" */
				addSurfaceMassDeposit(boundary_hit, boundary_t, part.mpw);
				return alive;
			}
		}

		/* particle still alive, did it leave the domain */
		if (part.lc[0] < 0 || part.lc[1] < 0 || part.lc[0] >= mesh.ni - 1 || part.lc[1] >= mesh.nj - 1) {
			/* determine exit face */
			double t_right = 99, t_top = 99, t_left = 99, t_bottom = 99;

			if (part.lc[0] >= mesh.ni - 1) {
				/* using >1.0 to place particle inside domain */
				t_right = (mesh.ni - 1.0 - lc_old[0]) / (part.lc[0] - lc_old[0]);
			}
			if (part.lc[1] >= mesh.nj - 1) {
				t_top = (mesh.nj - 1.0 - lc_old[1]) / (part.lc[1] - lc_old[1]);
			}
			if (part.lc[0] < 0) {
				t_left = lc_old[0] / (lc_old[0] - part.lc[0]);
			}
			if (part.lc[1] < 0) {
				t_bottom = lc_old[1] / (lc_old[1] - part.lc[1]);
			}

			exit_face = Face.RIGHT;
			double t = t_right;

			if (t_top < t) {
				exit_face = Face.TOP;
				t = t_top;
			}
			if (t_left < t) {
				exit_face = Face.LEFT;
				t = t_left;
			}
			if (t_bottom < t) {
				exit_face = Face.BOTTOM;
				t = t_bottom;
			}

			/*
			 * find boundary position, cannot use linear expression on position since
			 * mapping from physical to logical may not be linear
			 */
			part.lc[0] = lc_old[0] + t * (part.lc[0] - lc_old[0]);
			part.lc[1] = lc_old[1] + t * (part.lc[1] - lc_old[1]);

			// take care of round off errors, particle should be on mesh boundary
			if (part.lc[0] < 0)
				part.lc[0] = 0;
			else if (part.lc[0] > mesh.ni - 1)
				part.lc[0] = mesh.ni - 1;
			if (part.lc[1] < 0)
				part.lc[1] = 0;
			else if (part.lc[1] > mesh.nj - 1)
				part.lc[1] = mesh.nj - 1;

			double x[] = mesh.pos(part.lc);

			/* update particle position, part.pos is double[3], wall pos is double[2] */
			part.pos[0] = x[0];
			part.pos[1] = x[1];

			part.dt = dt0 * (1 - t); /* update remaining dt */

			int i = (int) part.lc[0];
			int j = (int) part.lc[1];
			if (exit_face == Face.TOP)
				j++;
			if (exit_face == Face.RIGHT)
				i++;

			if (i < 0)
				i = 0;
			if (j < 0)
				j = 0;
			if (i >= mesh.ni - 1)
				i = mesh.ni - 1;
			if (j >= mesh.nj - 1)
				j = mesh.nj - 1;

			/* process boundary */
			DomainBoundaryType type;

			if (exit_face == Face.LEFT || exit_face == Face.RIGHT)
				type = mesh.boundaryType(exit_face, j);
			else
				type = mesh.boundaryType(exit_face, i);

			switch (type) {
			case OPEN:
				return false;
			case SYMMETRY:
				/* grab normal vector */
				double n[] = mesh.faceNormal(exit_face, part.pos);
				part.vel = Vector.mirror(part.vel, n);
				return true;
			case PERIODIC: /* TODO: implemented only for single mesh */
				UniformMesh um = (UniformMesh) mesh;
				if (exit_face == Face.LEFT)
					part.pos[0] += (um.xd[0] - um.x0[0]);
				else if (exit_face == Face.RIGHT)
					part.pos[0] -= (um.xd[0] - um.x0[0]);
				else if (exit_face == Face.BOTTOM)
					part.pos[1] += (um.xd[1] - um.x0[1]);
				else
					part.pos[1] -= (um.xd[1] - um.x0[1]);
				return true;
			case MESH: /* add to neighbor */
				int index;
				if (exit_face == Face.LEFT || exit_face == Face.RIGHT)
					index = (int) part.lc[1];
				else
					index = (int) part.lc[0];
				MeshBoundaryData bc = mesh.boundaryData(exit_face, index);
				for (int m = 0; m < 2; m++) {
					if (bc.neighbor[m] != null && bc.neighbor[m].containsPos(part.pos)) {
						Mesh next = bc.neighbor[m];
						part.lc = next.XtoL(part.pos);
						getMeshData(next).addTransferParticle(part);
					}
				}
				return false;
			case CIRCUIT: // energy boundary for electrons
				if (charge >= 0)
					return false;
				// double T = this.getT(mesh).gather(part.lc);
				double T = 1 * Constants.EVtoK;
				double vth = Math.sqrt(2 * Constants.K * T / mass);
				double phi_b = Starfish.domain_module.getPhi(mesh).gather(part.lc);
				double v = Vector.mag3(part.vel);
				double KE = 0.5 * mass * v * v;
				double PE = Constants.QE * (phi_b - 0);
				// if (Vector.mag3(part.vel)<=vth)

				// absorb particle if ions collected on the wall
				if (Starfish.source_module.boundary_charge / (-charge) > part.mpw) {
					Starfish.source_module.boundary_charge += part.mpw * charge;
					return false;
				} else {
					part.vel = Vector.mult(part.vel, -1); // flip
					return true;
				}

			default:
				return false;
			}
		} // left mesh

		return true;
	}

	/**
	 * ads a new particle
	 * 
	 * @param md
	 * @param part
	 * @return
	 */
	public boolean addParticle(MeshData md, Particle part) {
		if (part.lc == null) {
			Mesh mesh = md.mesh;
			part.lc = mesh.XtoL(part.pos);
			
			

			/*
			 * particles could be added on the plus edge by a source, make sure LC is in
			 * range
			 */
			if (part.lc[0] >= mesh.ni)
				part.lc[0] = mesh.ni - 1;
			if (part.lc[1] >= mesh.nj)
				part.lc[1] = mesh.nj - 1;
		}

		/* rewind velocity by -0.5dt */
		part.dt = -0.5 * Starfish.getDt();

		double ef[] = new double[3];
		double bf[] = new double[3];

		ef[0] = md.Efi.gather(part.lc);
		ef[1] = md.Efj.gather(part.lc);

		bf[0] = md.Bfi.gather(part.lc);
		bf[1] = md.Bfj.gather(part.lc);

		/* update velocity */
		if (bf[0] == 0 && bf[1] == 0) {
			part.vel[0] += q_over_m * ef[0] * part.dt;
			part.vel[1] += q_over_m * ef[1] * part.dt;
		} else {
			UpdateVelocityBoris(part, ef, bf);
		}

		part.dt = 0;
		part.id = part_id_counter++;

		/* add particle trace, returns false if not traced */
		part.has_trace = Starfish.particle_trace_module.addTrace(this,part);

		md.addParticle(part);
		return true;
	}

	/**
	 * ads a new particle at the specified position and velocity
	 * 
	 * @param part
	 * @return
	 */
	public boolean addParticle(Particle part) {
		Mesh mesh = Starfish.domain_module.getMesh(part.pos);
		if (mesh == null) {
			return false;
		}
		MeshData md = getMeshData(mesh);
		return addParticle(md, part);
	}

	/**
	 * ads a new particle at the specified position and velocity
	 * 
	 * @param pos
	 * @param vel
	 * @return
	 */
	public boolean addParticle(double[] pos, double[] vel) {
		Mesh mesh = Starfish.domain_module.getMesh(pos);
		if (mesh == null) {
			return false;
		}

		MeshData md = getMeshData(mesh);

		return addParticle(md, new Particle(pos, vel, spwt0, this));
	}

	/**
	 * Kills particle by setting its weight to zero, will be actually removed on
	 * subsequent pass
	 * 
	 * @param part particle to remove
	 */
	public void removeParticle(Particle part) {
		part.mpw = 0;
	}

	private void UpdateVelocityBoris(Particle part, double[] E, double[] B) {
		double v_minus[] = new double[3];
		double v_prime[] = new double[3];
		double v_plus[] = new double[3];

		double t[] = new double[3];
		double s[] = new double[3];
		double t_mag2;

		int dim;

		/* t vector */
		for (dim = 0; dim < 3; dim++) {
			t[dim] = q_over_m * B[dim] * 0.5 * part.dt;
		}

		/* magnitude of t, squared */
		t_mag2 = t[0] * t[0] + t[1] * t[1] + t[2] * t[2];

		/* s vector */
		for (dim = 0; dim < 3; dim++) {
			s[dim] = 2 * t[dim] / (1 + t_mag2);
		}

		/* v minus */
		for (dim = 0; dim < 3; dim++) {
			v_minus[dim] = part.vel[dim] + q_over_m * E[dim] * 0.5 * part.dt;
		}

		/* v prime */
		double v_minus_cross_t[] = Vector.CrossProduct3(v_minus, t);
		for (dim = 0; dim < 3; dim++) {
			v_prime[dim] = v_minus[dim] + v_minus_cross_t[dim];
		}

		/* v plus */
		double v_prime_cross_s[] = Vector.CrossProduct3(v_prime, s);
		for (dim = 0; dim < 3; dim++) {
			v_plus[dim] = v_minus[dim] + v_prime_cross_s[dim];
		}

		/* v n+1/2 */
		for (dim = 0; dim < 3; dim++) {
			part.vel[dim] = v_plus[dim] + q_over_m * E[dim] * 0.5 * part.dt;
		}

	}

	/* saves restart data */

	/**
	 *
	 * @param out
	 * @throws IOException
	 */

	@Override
	public void saveRestartData(DataOutputStream out) throws IOException {
		out.writeInt(num_samples);

		for (Mesh mesh : Starfish.getMeshList()) {
			/* save particles */
			Iterator<KineticMaterial.Particle> iter = getIterator(mesh);
			out.writeLong(getMeshData(mesh).getNp());

			while (iter.hasNext()) {
				Particle part = iter.next();
				for (int i = 0; i < 3; i++) {
					out.writeDouble(part.pos[i]);
					out.writeDouble(part.vel[i]);
				}

				for (int i = 0; i < 2; i++)
					out.writeDouble(part.lc[i]);

				out.writeDouble(part.dt);
				out.writeDouble(part.mpw);
				out.writeDouble(part.mass);
				out.writeInt(part.born_it);
				out.writeInt(part.id);
			}

			/* next save fields */
			getDen(mesh).binaryWrite(out);
			getDenAve(mesh).binaryWrite(out);
			getT(mesh).binaryWrite(out);
			getU(mesh).binaryWrite(out);
			getV(mesh).binaryWrite(out);
			getW(mesh).binaryWrite(out);
			getUAve(mesh).binaryWrite(out);
			getVAve(mesh).binaryWrite(out);
			getWAve(mesh).binaryWrite(out);

		}
	}

	/* saves restart data */

	/**
	 * reads data from restart file TODO: currently this will crash if mesh size has
	 * changed due to the read of the sums
	 * 
	 * @param in
	 * @throws IOException
	 */
	@Override
	public void loadRestartData(DataInputStream in) throws IOException {
		num_samples = in.readInt();

		/* load in particles */
		for (Mesh mesh : Starfish.getMeshList()) {
			long np = in.readLong();
			MeshData md = getMeshData(mesh);

			for (long p = 0; p < np; p++) {
				Particle part = new Particle(this);
				for (int i = 0; i < 3; i++) {
					part.pos[i] = in.readDouble();
					part.vel[i] = in.readDouble();
				}

				part.lc = new double[2];
				for (int i = 0; i < 2; i++)
					part.lc[i] = in.readDouble();

				part.dt = in.readDouble();
				part.mpw = in.readDouble();
				part.mass = in.readDouble();
				part.born_it = in.readInt();
				part.id = in.readInt();

				addParticle(md, part);
			}

			/* next load fields */
			getDen(mesh).binaryRead(in);
			getDenAve(mesh).binaryRead(in);
			getT(mesh).binaryRead(in);
			getU(mesh).binaryRead(in);
			getV(mesh).binaryRead(in);
			getW(mesh).binaryRead(in);
			getUAve(mesh).binaryRead(in);
			getVAve(mesh).binaryRead(in);
			getWAve(mesh).binaryRead(in);

			/* set pressure */
			Field2D p = getP(mesh);
			Field2D nd_ave = getDenAve(mesh);
			Field2D T = getT(mesh);
			for (int i = 0; i < md.mesh.ni; i++)
				for (int j = 0; j < md.mesh.nj; j++)
					p.data[i][j] = nd_ave.at(i, j) * Constants.K * T.at(i, j);
		}
	}

	/**
	 * uses the algorithm from Justin Fox' dissertation to merge particles
	 * 
	 * @param md mesh to apply the merge to
	 * 
	 */
	void mergeParticles(MeshData md) {
		/* first sort particles to physical cells */
		sortParticlesToCells(md);
		for (int i = 0; i < md.mesh.ni; i++)
			for (int j = 0; j < md.mesh.nj; j++) {
				/* don't sort if less than 10 particles */
				if (md.cell_data[i][j].parts_in_cell.size() >= 10)
					mergeParticlesInCell(md, i, j);
			}

		// invalidate sort since now have new particles
		last_sort_to_cell_it = -1;

	}

	/**
	 * performs the actual merge in a single physical cell
	 * 
	 * @param md mesh to apply the merge to
	 * @param i  cell i-index
	 * @param j  cell j-index
	 */
	void mergeParticlesInCell(MeshData md, int i, int j) {
		/* create velocity grid */
		Mesh mesh = md.mesh;

		/* don't do anything if we have fewer than 10 particles */
		LinkedList<Particle> parts_in_cell = md.cell_data[i][j].parts_in_cell;

		double vel_min[] = new double[3];
		double vel_max[] = new double[3];

		/* initialize limits to first particle */
		Particle part0 = parts_in_cell.get(0);
		for (int d = 0; d < 3; d++) {
			vel_min[d] = part0.vel[d];
			vel_max[d] = part0.vel[d];
		}

		/* allocate velocity grid */
		int nu = vel_grid_dims[0]; // to save on typing
		int nv = vel_grid_dims[1];
		int nw = vel_grid_dims[2];

		CellData vel_cell_data[][][] = new CellData[nu][nv][nw];
		for (int iu = 0; iu < nu; iu++)
			for (int iv = 0; iv < nv; iv++)
				for (int iw = 0; iw < nw; iw++) {
					vel_cell_data[iu][iv][iw] = new CellData();
				}

		/* get velocity limits */
		for (Particle part : parts_in_cell) {
			for (int d = 0; d < 3; d++) {
				if (part.vel[d] < vel_min[d])
					vel_min[d] = part.vel[d];
				if (part.vel[d] > vel_max[d])
					vel_max[d] = part.vel[d];
			}
		}
		double du[] = new double[3];
		for (int d = 0; d < 3; d++)
			du[d] = (vel_max[d] - vel_min[d]) / vel_grid_dims[d];

		/* sort particles to velocity grid */
		for (Particle part : parts_in_cell) {
			int ui[] = new int[3];
			for (int d = 0; d < 3; d++) {
				ui[d] = (int) ((part.vel[d] - vel_min[d]) / du[d]);
				if (ui[d] < 0)
					ui[d] = 0;
				if (ui[d] >= vel_grid_dims[d])
					ui[d] = vel_grid_dims[d] - 1;
			}

			vel_cell_data[ui[0]][ui[1]][ui[2]].parts_in_cell.add(part);
		}

		/* now loop through velocity grid, replacing particles */
		for (int iu = 0; iu < nu; iu++)
			for (int iv = 0; iv < nv; iv++)
				for (int iw = 0; iw < nw; iw++) {
					LinkedList<Particle> vel_parts_in_cell = vel_cell_data[iu][iv][iw].parts_in_cell;
					if (vel_parts_in_cell.size() <= 2)
						continue; // need at least two particles

					/* compute total weight, average velocity, and variance per Fox' disseration */
					double n0 = 0; // total weight;
					double p0[] = new double[3]; // average velocity
					double t0[] = new double[3]; // variance
					double x0[] = new double[3]; // average position

					/* first compute n0 and accumulate data for p0 and t0 */
					for (Particle part : vel_parts_in_cell) {
						n0 += part.mpw;
						for (int d = 0; d < 3; d++) {
							p0[d] += part.mpw * part.vel[d];
							t0[d] += part.mpw * part.vel[d] * part.vel[d];
							x0[d] += part.mpw * part.pos[d];
						}
					}

					/* finish computations, based on my AdvPIC Lesson 2 slides */
					for (int d = 0; d < 3; d++) {
						p0[d] /= n0; // average velocity
						t0[d] = t0[d] / n0 - p0[d] * p0[d];
						if (t0[d] < 0)
							t0[d] = 0;
						x0[d] /= n0; // average position;
					}

					/* add two new particles corresponding to the vel cell population */
					double w = 0.5 * n0; // each particle gets half weight;
					double vel1[] = new double[3];
					double vel2[] = new double[3];
					for (int d = 0; d < 3; d++) {
						// assign random sign to each dimension
						int sign = (Starfish.rnd() < 0.5) ? 1 : -1;
						vel1[d] = p0[d] + sign * Math.sqrt(t0[d]);
						vel2[d] = p0[d] - sign * Math.sqrt(t0[d]);
					}
					Particle part1 = new Particle(x0, vel1, w, this);
					Particle part2 = new Particle(x0, vel2, w, this);

					this.addParticle(part1);
					this.addParticle(part2);

					/* destroy old particles in the vel cell */
					for (Particle part : vel_parts_in_cell)
						removeParticle(part);
				}

	}

	/**
	 * sorts particles to cell
	 * 
	 * @param md mesh to apply to
	 */
	public void sortParticlesToCells(MeshData md) {
		Mesh mesh = md.mesh;
		last_sort_to_cell_it = Starfish.getIt();

		/* allocate data structure on first call */
		if (md.cell_data == null) {
			md.cell_data = new CellData[mesh.ni][mesh.nj];
			for (int i = 0; i < mesh.ni; i++)
				for (int j = 0; j < mesh.nj; j++)
					md.cell_data[i][j] = new CellData();
		}

		/* cleanup */
		for (int i = 0; i < md.mesh.ni - 1; i++)
			for (int j = 0; j < mesh.nj - 1; j++) {
				md.cell_data[i][j].parts_in_cell.clear();
			}

		/* sort particles into cell */
		Iterator<Particle> src_iterator = getIterator(mesh);
		while (src_iterator.hasNext()) {
			Particle part = src_iterator.next();
			int i = (int) part.lc[0];
			int j = (int) part.lc[1];
			if (i >= mesh.ni - 1 || j >= mesh.nj - 1)
				continue; // boundary source can create particles on mesh edge
			md.cell_data[i][j].parts_in_cell.add(part);

		}
	}

	/**
	 * returns particle with i
	 * 
	 * @param id
	 * @return d
	 */
	public Particle getParticle(long id) {
		for (MeshData md : mesh_data)
			for (int block = 0; block < md.particle_block.length; block++) {

				Iterator<Particle> iterator = md.getIterator(block);
				while (iterator.hasNext()) {
					Particle part = iterator.next();
					if (part.id == id)
						return part;
				}
			}
		return null;
	}

	/* particle definition */

	/**
	 *
	 */

	static public class Particle {
		public double pos[] = new double[3]; //the 3rd dimension is the "depth", xy: (x,y,z), rz: (r,z,theta), zr (z,r,theta)
		public double vel[] = new double[3]; //cartesian system velocities, 
		public double mpw = 0;	//macroparticle weight: number of real particles represented by this sim particle
		public double mass = 0; // mass of the physical particle
		public double lc[] = new double[2]; // logical coordinate of current position
		public double dt = 0;  // remaining dt to move through 
		public double radius = 0; // particle radius, currently used only by droplets
		public int id = -1; 	// particle id for plotting
		public int born_it = -1;	// time step born for possible diagnostics
		public boolean has_trace = false; // indicates whether the particle is being traced
		public boolean attached = false; // temporary, used by grain particles on surface layer

		/**
		 * copy constructor
		 * 
		 * @param part
		 */
		public Particle(Particle part) {
			pos = new double[3];
			vel = new double[3];
			lc = new double[2];
			for (int i = 0; i < 3; i++) {
				pos[i] = part.pos[i];
				vel[i] = part.vel[i];
			}
			for (int i = 0; i < 2; i++) {
				lc[i] = part.lc[i];
			}
			mpw = part.mpw;
			mass = part.mass;
			dt = part.dt;
			id = part.id;
			born_it = part.born_it;
			has_trace = part.has_trace;
		}

		/**
		 *
		 * @param mat
		 */
		public Particle(KineticMaterial mat) {
			mpw = mat.spwt0;
			mass = mat.mass;
			born_it = Starfish.getIt();
			radius = mat.diam * 0.5;
			lc = null;		//to force recompute
		}

		/**
		 *
		 * @param mpw macroparticle weight
		 * @param mat
		 */
		public Particle(double mpw, KineticMaterial mat) {
			this(mat);
			this.mpw = mpw;
		}

		/**
		 *
		 * @param pos
		 * @param vel
		 * @param spwt
		 * @param mat
		 */
		public Particle(double pos[], double vel[], double spwt, KineticMaterial mat) {
			this(spwt, mat);

			this.pos[0] = pos[0];
			this.pos[1] = pos[1];
			this.pos[2] = pos[2];

			this.vel[0] = vel[0];
			this.vel[1] = vel[1];
			this.vel[2] = vel[2];
		}
	}

	/**
	 *
	 * @param mesh
	 * @param block
	 * @return
	 */
	public Iterator<Particle> getIterator(Mesh mesh, int block) {
		return getMeshData(mesh).getIterator(block);
	}

	/**
	 * @return number of particles across all domains
	 */
	public long getNp() {
		long np = 0;
		for (int m = 0; m < mesh_data.length; m++) {
			np += mesh_data[m].getNp();
		}
		return np;
	}

	/* list of particles in each cell */
	public class CellData // class to avoid generic array creation
	{
		LinkedList<Particle> parts_in_cell = new LinkedList<Particle>();
	}

	/**
	 * particle data structure
	 */
	public class MeshData {
		public MeshData(Mesh mesh) {
			this.mesh = mesh;

			/* save references */
			Efi = Starfish.domain_module.getEfi(mesh);
			Efj = Starfish.domain_module.getEfj(mesh);
			Bfi = Starfish.domain_module.getBfi(mesh);
			Bfj = Starfish.domain_module.getBfj(mesh);

			num_blocks = Starfish.getNumProcessors();

			particle_block = new ParticleBlock[num_blocks];
			transfer_particles = new ArrayList<>();

			/* init particle lists */
			for (int i = 0; i < particle_block.length; i++) {
				particle_block[i] = new ParticleBlock();				
			}
		}

		/**
		 *
		 */
		public int num_blocks;
		public Mesh mesh;
		public Field2D Efi, Efj;
		public Field2D Bfi, Bfj;

		public CellData[][] cell_data;

		public ParticleBlock particle_block[];
		public ArrayList<Particle> transfer_particles; /*
												 * particles transferred into this mesh from a neighboring one during
												 * the transfer
												 */

		/**
		 * add particle to the list, attempting to keep block sizes equal
		 * 
		 * @param part
		 */
		public void addParticle(Particle part) {
			if (!Vector.isFinite(part.vel)) {
				/* not sure why this can happen sometimes... */
				Log.warning("Infinite vel");
				return;
			}

			/* find particle block with fewest particles */
			int block = 0;
			int min_count = particle_block[block].particle_list.size();

			for (int i = 1; i < particle_block.length; i++)
				if (particle_block[i].particle_list.size() < min_count) {
					min_count = particle_block[i].particle_list.size();
					block = i;
				}

			particle_block[block].particle_list.add(part);
		}

		/** add particle to the transfers list, attempting to keep block sizes equal */
		void addTransferParticle(Particle part) {
			/* call copy constructor since original particle may be deleted */
			transfer_particles.add(new Particle(part));
		}

		/**
		 * @return number of particles
		 */
		public long getNp() {
			long count = 0;
			for (int i = 0; i < particle_block.length; i++)
				count += particle_block[i].particle_list.size();
			return count;
		}

		/**
		 * @return number of particles waiting to be transferred between domains
		 */
		public long getTransferNp() {
			return transfer_particles.size();			
		}

		/**
		 * returns particle iterator for the given block
		 * 
		 * @param block
		 * @return
		 */
		public Iterator<Particle> getIterator(int block) {
			return particle_block[block].particle_list.iterator();
		}

		/**
		 * returns iterator that iterates over all particles in all block
		 * 
		 * @return s
		 */
		public Iterator<Particle> getIterator() {
			return new BlockIterator(particle_block);
		}

		/**
		 * returns transfer particle iterators for the given bloc
		 * 
		 * @param block
		 * @return k
		 */
		public Iterator<Particle> getTransferIterator() {
			return transfer_particles.iterator();
		}
	}

	/**
	 *
	 */
	public MeshData mesh_data[];

	/**
	 * Particles are stored in linked-list array blocks	 *
	 */
	public class ParticleBlock {

		public ArrayList<Particle> particle_list = new ArrayList<Particle>();
	}

	/**
	 *
	 */
	public class BlockIterator implements Iterator<Particle> {

		ParticleBlock blocks[];
		int b = 0;
		final int num_blocks;

		/**
		 *
		 */
		protected Iterator<Particle> iterator;

		BlockIterator(ParticleBlock blocks[]) {
			this.blocks = blocks;
			num_blocks = blocks.length;
			iterator = blocks[b].particle_list.iterator();
		}

		@Override
		public boolean hasNext() {
			if (iterator.hasNext())
				return true;

			/* any more blocks? */
			while (b < num_blocks - 1) {
				b++;
				iterator = blocks[b].particle_list.iterator();
				if (iterator.hasNext())
					return true;
			}
			return false;
		}

		@Override
		public Particle next() {
			return iterator.next();
		}

		@Override
		public void remove() {
			iterator.remove();
		}

	}

	/**
	 *
	 * @param mesh
	 * @return
	 */
	public MeshData getMeshData(Mesh mesh) {
		for (int m = 0; m < mesh_data.length; m++) {
			if (mesh_data[m].mesh == mesh) {
				return mesh_data[m];
			}
		}

		Log.warning("Failed to find mesh_data for the specified mesh " + mesh);
		return null;
	}

	/**
	 * clears collected velocity moments
	 */
	@Override
	public void clearSamples() {
		for (MeshData md : mesh_data)
			clearSamples(md);

		num_samples = 0;
		Log.log("Cleared samples in Material " + name);
	}

	int num_samples = 0;

	void clearSamples(MeshData md) {
		field_manager2d.get(md.mesh, "count-sum").clear();
		field_manager2d.get(md.mesh, "u-sum").clear();
		field_manager2d.get(md.mesh, "v-sum").clear();
		field_manager2d.get(md.mesh, "w-sum").clear();
		field_manager2d.get(md.mesh, "uu-sum").clear();
		field_manager2d.get(md.mesh, "vv-sum").clear();
		field_manager2d.get(md.mesh, "ww-sum").clear();
		field_manager2d.get(md.mesh, "mpc-sum").clear();
	}

	/**
	 * updates velocity samples and recomputes average density, temperature and
	 * pressure fields
	 * 
	 */
	protected void updateGasProperties() {
		for (MeshData md : mesh_data)
			updateSamples(md);

		num_samples++;

		// compute temperature and average densities and velocities
		if (first_time || Starfish.getIt() % 10 == 0) {
			computeFields();
		}
	}

	/**
	 * updates velocity samples
	 * 
	 * @param md
	 */
	protected void updateSamples(MeshData md) {
		Field2D count_sum = this.field_manager2d.get(md.mesh, "count-sum");
		Field2D u_sum = this.field_manager2d.get(md.mesh, "u-sum");
		Field2D v_sum = this.field_manager2d.get(md.mesh, "v-sum");
		Field2D w_sum = this.field_manager2d.get(md.mesh, "w-sum");
		Field2D uu_sum = this.field_manager2d.get(md.mesh, "uu-sum");
		Field2D vv_sum = this.field_manager2d.get(md.mesh, "vv-sum");
		Field2D ww_sum = this.field_manager2d.get(md.mesh, "ww-sum");
		Field2D mpc_sum = this.field_manager2d.get(md.mesh, "mpc-sum");

		Iterator<Particle> iterator = md.getIterator();
		while (iterator.hasNext()) {
			Particle part = iterator.next();

			u_sum.scatter(part.lc, part.mpw * part.vel[0]);
			v_sum.scatter(part.lc, part.mpw * part.vel[1]);
			w_sum.scatter(part.lc, part.mpw * part.vel[2]);
			uu_sum.scatter(part.lc, part.mpw * part.vel[0] * part.vel[0]);
			vv_sum.scatter(part.lc, part.mpw * part.vel[1] * part.vel[1]);
			ww_sum.scatter(part.lc, part.mpw * part.vel[2] * part.vel[2]);
			count_sum.scatter(part.lc, part.mpw);

			// mpc is cell data
			mpc_sum.add((int) part.lc[0], (int) part.lc[1], 1);
		}
	}

	/** uses collected sampled data to compute bulk gas properties */
	public void computeFields() {
		for (MeshData md : mesh_data)
			computeFields(md);
	}

	/**
	 * uses velocity samples to compute average density, velocity, and temperature
	 */
	protected void computeFields(MeshData md) {
		Field2D count_sum = this.field_manager2d.get(md.mesh, "count-sum");
		Field2D u_sum = this.field_manager2d.get(md.mesh, "u-sum");
		Field2D v_sum = this.field_manager2d.get(md.mesh, "v-sum");
		Field2D w_sum = this.field_manager2d.get(md.mesh, "w-sum");
		Field2D uu_sum = this.field_manager2d.get(md.mesh, "uu-sum");
		Field2D vv_sum = this.field_manager2d.get(md.mesh, "vv-sum");
		Field2D ww_sum = this.field_manager2d.get(md.mesh, "ww-sum");
		Field2D mpc_sum = this.field_manager2d.get(md.mesh, "mpc-sum");
		Field2D nd_ave = this.field_manager2d.get(md.mesh, "nd-ave");
		Field2D u_ave = this.field_manager2d.get(md.mesh, "u-ave");
		Field2D v_ave = this.field_manager2d.get(md.mesh, "v-ave");
		Field2D w_ave = this.field_manager2d.get(md.mesh, "w-ave");
		Field2D p = this.getP(md.mesh);
		Field2D T = this.getT(md.mesh);
		Field2D T1 = this.field_manager2d.get(md.mesh, "t1");
		Field2D T2 = this.field_manager2d.get(md.mesh, "t2");
		Field2D T3 = this.field_manager2d.get(md.mesh, "t3");

		/* compute temperatures */
		for (int i = 0; i < md.mesh.ni; i++)
			for (int j = 0; j < md.mesh.nj; j++) {
				double count = count_sum.at(i, j);
				if (count > 0) {
					double u = u_sum.at(i, j) / count;
					double v = v_sum.at(i, j) / count;
					double w = w_sum.at(i, j) / count;
					double uu = uu_sum.at(i, j) / count - u * u;
					double vv = vv_sum.at(i, j) / count - v * v;
					double ww = ww_sum.at(i, j) / count - w * w;
					double f = mass / (Constants.K);
					T.data[i][j] = (uu + vv + ww) * f / 3.0;
					T1.data[i][j] = uu * f;
					T2.data[i][j] = vv * f;
					T3.data[i][j] = ww * f;
				} else {
					T.data[i][j] = init_vals.T;
					T1.data[i][j] = init_vals.T;
					T2.data[i][j] = init_vals.T;
					T3.data[i][j] = init_vals.T;
				}
			}

		/* set average density and velocities */
		nd_ave.copy(count_sum);
		nd_ave.mult(1.0 / num_samples);
		nd_ave.scaleByVol();

		u_ave.copy(u_sum);
		u_ave.divideByField(count_sum);
		v_ave.copy(v_sum);
		v_ave.divideByField(count_sum);
		w_ave.copy(w_sum);
		w_ave.divideByField(count_sum);

		/* set pressure */
		for (int i = 0; i < md.mesh.ni; i++)
			for (int j = 0; j < md.mesh.nj; j++)
				p.data[i][j] = nd_ave.at(i, j) * Constants.K * T.at(i, j);

		/* macroparticles per cell */
		Field2D mpc = this.field_manager2d.get(md.mesh, "mpc");
		mpc.copy(mpc_sum);
		mpc.mult(1. / num_samples);

	}

	/** parser */
	public static MaterialParser KineticMaterialParser = new MaterialParser() {
		@Override
		public Material addMaterial(String name, Element element) {

			Material material = new KineticMaterial(name, element);

			return material;
		}
	};

}
