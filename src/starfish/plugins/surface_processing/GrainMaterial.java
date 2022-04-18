/*
 * implementation of a grain particle for the DEM method
 */
package starfish.plugins.surface_processing;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.materials.MaterialsModule.MaterialParser;

/*9.2.1 in Tanehill*/
public class GrainMaterial extends KineticMaterial 
{
  public GrainMaterial(String name, Element element)
    {
		super(name, element);
		
		    
		/*log*/
		Log.log("Added GRAIN material '"+name+"'");
		Log.log("> charge   = "+charge);
    }

    @Override
    public void init()
    {
		super.init();
		
    }

    
    @Override
    public void updateFields() {

    	MeshData md = mesh_data[0];
    	
    	KineticMaterial km_ions = Starfish.getKineticMaterial("cu+");
    	double dt = Starfish.getDt();
    	 
    	/*add gravity*/
    	ParticleBlock pb_gs[] = md.particle_block;
    	ParticleBlock pb_is[] = km_ions.mesh_data[0].particle_block;
    	
    	/*
    	for (ParticleBlock pb_g:pb_gs)
    	{
    		for (Particle particle : pb_g.particle_list)
    		{
    			particle.vel[1] -= 9.81*Starfish.getDt();	//add acceleration in -y direction
    		}
    	}
    	*/
    	
    	//kinetic ion impact
    	if (false)
		for (ParticleBlock pb_g:pb_gs)
    		for (Particle grain : pb_g.particle_list)
	
    			for (ParticleBlock pb_i:pb_is)
    			{
    				for (Particle ion:pb_i.particle_list)
    				{
    					double x_old[] = {ion.pos[0]-ion.vel[0]*dt,ion.pos[1]-ion.vel[1]*dt};
    			
    					//check for collisions with granules
    					//does not check for particles crossing the granules
    					{
	    				double dx = ion.pos[0]-grain.pos[0];
	    				double dy = ion.pos[1]-grain.pos[1];
	    				double r2 = Math.sqrt(dx*dx+dy*dy);
	    				if (r2>(0.5*km_ions.diam+grain.radius)) continue;
    				
	    				//	collision
	    				dx = x_old[0]-grain.pos[0];
	    				dy = x_old[1]-grain.pos[1];
	    				double r1 = Math.sqrt(dx*dx+dy*dy);
	    				if (r2<=r1) continue;
    				
	    				double t = (grain.radius-r1)/(r2-r1);
	    				double xp[] = new double[2];		//intersection point;
	    				xp[0] = x_old[0]+t*(ion.pos[0]-x_old[0]);
	    				xp[1] = x_old[1]+t*(ion.pos[1]-x_old[1]);
    				
    					//	ray to intersection
	    				double n[] = new double[2];
	    				n[0] = xp[0]-grain.pos[0];
	    				n[1] = xp[1]-grain.pos[1];
	    				double n_mag = Math.sqrt(n[0]*n[0]+n[1]*n[1]);
	    				if (n_mag<=0) continue;
	    				n[0] /=n_mag;
	    				n[1] /=n_mag;
    				
	    				//	decompose particle velocity
	    				double v_norm_mag = ion.vel[0]*n[0]+ion.vel[1]*n[1];
	    				double v_norm[] = {n[0]*v_norm_mag,n[1]*v_norm_mag};	//moving away
	    				double v_tang[] = {ion.vel[0]-v_norm[0],ion.vel[1]-v_norm[1]};
    				
	    				double f_ion = grain.mass/(grain.mass+ion.mass);
	    				double f_gran = ion.mpw*ion.mass/(grain.mass+ion.mass);	//actually hit by ion.spwt ions
	    				f_ion=1;
	    				f_gran=1e-3;
	    				
    					//cout<<f_ion<<" "<<f_gran<<endl;
	    				ion.vel[0] = f_ion*(-v_norm[0]+v_tang[0]);
	    				ion.vel[1] = f_ion*(-v_norm[1]+v_tang[1]);
	    				grain.vel[0] += f_gran*(v_tang[0]);
	    				grain.vel[1] += f_gran*(v_tang[1]);
	    				double grain_vel = Math.sqrt(grain.vel[0]*grain.vel[0]+grain.vel[1]*grain.vel[1]);
	    			//	grain.vel[0] = grain_vel*v_tang[0];
	    			//	grain.vel[1] = grain_vel*v_tang[1];
    								
	    				ion.pos[0] = xp[0];
	    				ion.pos[1] = xp[1];					
    	    		}
    			}  //for ion
			} //for ion partblock
    	
    	
    	//use MCC like approach for ions
    	if (false)
    		//move over ions
    		for (ParticleBlock pb_g:pb_gs)
        		for (Particle grain : pb_g.particle_list)
    			{
        			int gi = (int)grain.lc[0];
        			int gj = (int)grain.lc[1];
        			//pick random samples;
        			int num_samples=5;
        			for (int s=0;s<num_samples;s++)
        			{
        				double lc[] = {gi+Starfish.rnd(),gj+Starfish.rnd()};
        				double ion_pos[] = md.mesh.pos(lc);
        				
    					double dx = ion_pos[0]-grain.pos[0];
	    				double dy = ion_pos[1]-grain.pos[1];
	    				double r2 = Math.sqrt(dx*dx+dy*dy);
	    				if (r2>(0.5*km_ions.diam+grain.radius)) continue;
    				
	    			
    					//	ray to intersection
	    				double n[] = new double[2];
	    				n[0] = ion_pos[0]-grain.pos[0];
	    				n[1] = ion_pos[1]-grain.pos[1];
	    				double n_mag = Math.sqrt(n[0]*n[0]+n[1]*n[1]);
	    				if (n_mag<=0) continue;
	    				n[0] /=n_mag;
	    				n[1] /=n_mag;
    				
	    				//sample velocity
	    				double ion_vel[] = km_ions.sampleVelocity(md.mesh,lc);
	    				
	    				//	decompose particle velocity
	    				double v_norm_mag = ion_vel[0]*n[0]+ion_vel[1]*n[1];
	    				double v_norm[] = {n[0]*v_norm_mag,n[1]*v_norm_mag};	//moving away
	    				double v_tang[] = {ion_vel[0]-v_norm[0], ion_vel[1]-v_norm[1]};
    				
	    				double ion_den = km_ions.getDen(md.mesh).gather(lc);
	    				double num_ions = ion_den*md.mesh.cellVol(gi, gj);
	    				double ion_mass = num_ions*km_ions.mass;
	    				
	    				double f_gran = ion_mass/(grain.mass+ion_mass);	//actually hit by ion.spwt ions
	    				f_gran /= num_samples;
	    				
	    				f_gran *= 1e0;		//scaling fudge factor
    					//cout<<f_ion<<" "<<f_gran<<endl;
	    				grain.vel[0] += f_gran*(v_tang[0]);
	    				grain.vel[1] += f_gran*(v_tang[1]);
	    							
    	    			}
        			}  //for ion
    		
		//apply granule forces
       	if (true)
    	for (ParticleBlock pb_g:pb_gs)
    		for (Particle grain : pb_g.particle_list)    		
    		{
    			//total force
    			double F[] = {0,0};
    			
    			for (ParticleBlock pb_g2:pb_gs)
    	    		for (Particle grain2 : pb_g2.particle_list)    	    		
    	    		{
    	    			//not sure if this works
    	    			if (grain2==grain) continue;
    	    			
    	    			//compute distance between the granules
    	    			double dx = grain.pos[0]-grain2.pos[0];
    	    			double dy = grain.pos[1]-grain2.pos[1];
    	    			double r = Math.sqrt(dx*dx+dy*dy);
    	    			if (r==0.0) continue;
    				
    	    			//break bond if distance greater than some stretch times actual distance between molecules
    	    			double rad_sum = grain.radius+grain2.radius;
    	    			if (r>1.0*rad_sum) continue;
    	    			
    	    			//repulsive force, modeled as parabola with zero at r=0.8				
    	    			double r_ratio = r/grain.radius;
    	    			
    	    			double F_rep_mag = (1-(r_ratio*r_ratio/0.64));
    	    			if (F_rep_mag<0) F_rep_mag=0;
    	    			F_rep_mag=0;
    				
    	    			//attractive force, modeled as F=kx
    	    			double F_att_mag = 0.05*r_ratio*100;
    	    						
    	    			//apply mass scaling, not sure if this right... "2" so if same radius it gives 1
    	    			//F_rep_mag*=2*grain2.radius/(grain.radius+grain2.radius);
    	    			//F_att_mag*=2*grain2.radius/(grain.radius+grain2.radius);
    	    			
    	    			double dir[] = {grain2.pos[0]-grain.pos[0],grain2.pos[1]-grain.pos[1]};
    	    			double mag = Math.sqrt(dir[0]*dir[0]+dir[1]*dir[1]);
    	    			dir[0] /= mag;
    	    			dir[1] /= mag;
    	    			F[0] += dir[0]*(F_att_mag-F_rep_mag);
    					F[1] += dir[1]*(F_att_mag-F_rep_mag);
    	    		}
    			

    	    	//force scaling factor
    	    	double F0 = 1e-16;
    			
    			double dv[] = {dt*F0*(F[0]/grain.mass), dt*F0*(F[1]/grain.mass)};
    			
    			grain.vel[0]+=dv[0];
    			grain.vel[1]+=dv[1];
    			
    		}    
    	super.updateFields();		
    }    
    
    public static MaterialParser GrainMaterialParser = new MaterialParser() {
	@Override
	public Material addMaterial(String name, Element element)
	{
	    Material material = new GrainMaterial(name,element);   
	    return material;
        }
    };
}
