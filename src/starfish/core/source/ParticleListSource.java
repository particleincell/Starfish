/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.source;

import java.util.ArrayList;
import starfish.core.boundaries.Boundary;
import starfish.core.common.Starfish;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;

/**
 * Source which can accumulate a list of particles which it then samples
 */
public class ParticleListSource extends Source
{

    ArrayList<Particle> particle_list = new ArrayList<Particle>();

    /**
     *
     * @param source_mat
     * @param boundary
     */
    public ParticleListSource(Material source_mat, Boundary boundary)
    {
	super("PartList " + source_mat.getName() + ":" + boundary.getName(), source_mat, boundary, 0);
    }

    @Override
    public Particle sampleParticle()
    {
	Particle part = particle_list.get(particle_list.size() - 1);
	particle_list.remove(particle_list.size() - 1);
	return part;
    }

    /**
     * adds a new particle to the list
     * @param part
     */
    public void addParticle(Particle part)
    {
	particle_list.add(part);
    }

    @Override
    public boolean hasParticles()
    {
	if (particle_list.size() > 0)
	{
	    return true;
	} else
	{
	    return false;
	}
    }

    @Override
    public void sampleFluid()
    {
	/*do nothing*/
    }

    /**
     *
     * @param pos
     * @param vel
     * @param orig_mat_index
     */
    public void spawnParticles(double[] pos, double[] vel, int orig_mat_index)
    {
	KineticMaterial orig_mat = (KineticMaterial) Starfish.getMaterial(orig_mat_index);
	double spwt_orig = orig_mat.getSpwt0();
	double spwt_source = ((KineticMaterial) source_mat).getSpwt0();
	double ratio = spwt_orig / spwt_source;

	int count = (int) (ratio + Starfish.rnd());

	for (int i = 0; i < count; i++)
	{
	    addParticle(new Particle(pos, vel, spwt_source, orig_mat));
	}
    }
}
