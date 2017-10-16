/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.source;

import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;

/** creates particles in the simulation volume*/
public class VolumeSource extends Source
{
    protected FieldCollection2D dn;	/*change in density*/
    protected FieldCollection2D temp;	/*temperature*/
    protected int i_sample,j_sample;	/*current sampling cell index*/
    protected Mesh sample_mesh;
    protected double num_rem;		/*number of particles to sample in current cell*/
    double spwt0;

    public FieldCollection2D getDn() {return dn;}
    public Field2D getDn(Mesh mesh) {return dn.getField(mesh);}
    
    public Field2D getTemp(Mesh mesh) {return temp.getField(mesh);}

    /*constructor*/
    public VolumeSource(String name, Material source_mat)
    {
	super(name, source_mat);

	dn = new FieldCollection2D(Starfish.getMeshList(),null);
	temp = new FieldCollection2D(Starfish.getMeshList(),null);
	
	if (source_mat instanceof KineticMaterial)
	    spwt0 = ((KineticMaterial)source_mat).getSpwt0();
	
    }

    @Override
    public Particle sampleParticle() 
    {
	/*sanity check, should not happen*/
	if (num_rem<spwt0) 
	{
	    Log.warning("sampleParticle called when there are no particles left to sample");
	    return null;
	}
	
	Particle part = new Particle((KineticMaterial)source_mat);

	/*position*/
	double ip=i_sample+1.0*Starfish.rnd();
	double jp=j_sample+1.0*Starfish.rnd();
	double p[] = sample_mesh.pos(ip,jp);
	part.pos[0] = p[0];
	part.pos[1] = p[1];
	part.pos[2] = 0;

	/*grab temperature in the cell*/
	double temp_p = getTemp(sample_mesh).gather(ip,jp);
	//negative temperature indicates we are in a boundary cell that has neighbors set to -1
	if (temp_p<50) temp_p = 50;	//todo: for now added limit
	
	double v_th=Utils.computeVth(temp_p, source_mat.mass);
	
	/*velocity*/
	part.vel = Utils.SampleMaxw3D(v_th);

	num_rem -= spwt0;

	/*move to next cell if we are done with this one*/
	if (num_rem<=spwt0)
	{
	    /*set remaining dn/dt*/
	    double v = dn_f.gather(i_sample+0.5, j_sample+0.5);
	    dn_f.scatter(i_sample+0.5, j_sample+0.5,num_rem/sample_mesh.cellVol(i_sample,j_sample) - v); 	
	    findNextCell(i_sample,j_sample+1);
	}

	/* do not return internal particles!*/
	if (Starfish.boundary_module.isInternal(part.pos))
	    return null;
		    
	return part;
    }

    @Override
    public boolean hasParticles()
    {
	if (num_rem>=spwt0) return true;
	return false;
    }
    
    @Override
    public void regenerate()
    {
	sample_mesh=Starfish.getMeshList().get(0);		
	//System.out.printf("%g\n",getDn(sample_mesh).data[24][8]);

	findNextCell(0,0);
    }
    
    Field2D dn_f;
    
    /**finds next cell with a finite density change*/
    void findNextCell(int i0, int j0)
    {
	dn_f = dn.getField(sample_mesh);

	num_rem=0;
	/*set initial i,j*/
	i_sample=i0;
	j_sample=j0;

	while(true)
	{
	    if (j_sample>sample_mesh.nj-2)
	    {
		j_sample=0;
		i_sample++;
		if (i_sample>sample_mesh.ni-2)
		    break;
	    }

	    num_rem = dn_f.gather(i_sample+0.5, j_sample+0.5)*sample_mesh.cellVol(i_sample,j_sample);//
	    
	    /*add random fraction*/
	    num_rem += Starfish.rnd()*spwt0;
	    
	    if (num_rem>=spwt0)
		return;
	    j_sample++;		
	}
	
	int m = sample_mesh.getIndex()+1;
	if (m<Starfish.getMeshList().size())
	{
	    sample_mesh = Starfish.getMeshList().get(m);
	    findNextCell(0,0);
	}
    }

    @Override
    public void sampleFluid() 
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    int ni = mesh.ni;
	    int nj = mesh.nj;
	    double den[][] = source_mat.getDen(mesh).getData();
	    double dn_local[][] = dn.getField(mesh).getData();
	    for (i_sample=0;i_sample<ni;i_sample++)
		for (j_sample=0;j_sample<nj;j_sample++)
		    den[i_sample][j_sample]+=dn_local[i_sample][j_sample];
	}
    }

    public void clearDn() 
    {
	dn.clear();
    }

}
