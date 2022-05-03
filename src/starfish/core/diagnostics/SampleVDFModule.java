/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.diagnostics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Vector;
import starfish.core.diagnostics.DiagnosticsModule.Diagnostic;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;

/** This class computes a velocity distribution histogram for particles of a given
 * kinetic material within specified bounds
 *
 * @author Lubos Brieda
 */
public class SampleVDFModule extends CommandModule
{
    class SampleVDF implements Diagnostic
    {
	KineticMaterial km; /*material to sample*/
	double xmin[];	    /*sampling region lower bound*/
	double xmax[];	    /*sampling region upper bound*/
	int skip_sample;     /*frequency of sampling*/
	int skip_output;    /*frequency of output*/
	int start_it;	    /*starting iteration*/
	String prefix;	    /*file name prefix*/
	int speed_bins;	    /*number of speed bins*/
	int vel_bins[];	    /*number of velocity bins for the three dimensions*/
	
	private boolean dirty;	/*will re-initialize sampling if set true*/
	boolean auto_vel_range;	//should velocity bins be recomputed	
	double vel_min[] = null;	//initialized to null to distinguish user input 
	double vel_max[] = null;
	double dv[] = new double[3];
	
	boolean auto_speed_range;
	double speed_range[] = null;
	double ds;
	
	double weight_sum;
	double hist_speed[];
	double hist_vel[][] = new double[3][];
			
	
	SampleVDF(Element element)
	{
	    String mat_name = InputParser.getValue("material", element);
	    km = Starfish.getKineticMaterial(mat_name);
	    if (km==null)
		Log.error("SampleVDF only works with kinetic materials");
	    xmin = InputParser.getDoubleList("xmin", element);
	    xmax = InputParser.getDoubleList("xmax", element);
	    if (xmin.length!=2)
		Log.error("xmin must specify two values");
	    if (xmax.length!=2)
		Log.error("xmax must specify two values");
	    speed_bins = InputParser.getInt("speed_bins",element,20);
	    vel_bins = new int[]{20,20,20};
	    vel_bins = InputParser.getIntList("vel_bins", element, vel_bins);	  
	    if (vel_bins.length!=3)
		Log.error("vel_bins must contain three integers");
	    
	    //try to read in velocity limits
	    vel_min = null;
	    vel_max = null;
	    auto_vel_range = false;
	    vel_min = InputParser.getDoubleList("vel_min", element, vel_min);
	    vel_max = InputParser.getDoubleList("vel_max", element, vel_max);
	    if (vel_min==null || vel_max==null || vel_min.length!=3 || vel_max.length!=3) {
	    	auto_vel_range = true;
	    	vel_min = new double[3];
	    	vel_max = new double[3];
	    }
	    
	    auto_speed_range = false;
	    speed_range = InputParser.getDoubleList("speed_range", element, speed_range);
	    if (speed_range.length!=2) {
	    	auto_speed_range = true;
	    	speed_range = new double[2];	    		    	
	    }
	    	    
	    start_it = InputParser.getInt("start_it", element,-1);
	    skip_sample = InputParser.getInt("skip_sample",element,100);
	    skip_output = InputParser.getInt("skip_output",element,1000);
	    prefix = InputParser.getValue("file_name",element);
	    dirty = true;
	}

	/**
	 * samples particle data every skip_sample time steps and then outputs the 
	 * sampled data every skip_output steps. Velocity/speed limits on the first sample
	 * post write are used to set the histogram bins
	 * */
	@Override
	public void sample(boolean force)
	{
	    if (force || (Starfish.getIt()>=start_it && Starfish.getIt()%skip_sample==0))
	    {
			LinkedList<Particle> particles = new LinkedList();
			
			/*loop through all particles and get a list of those in bounds*/
			for (Mesh mesh:Starfish.getMeshList())
			{
			    Iterator<Particle> it = km.getIterator(mesh);
			    while (it.hasNext())
			    {
				Particle part = it.next();
				if (part.pos[0]>=xmin[0] && part.pos[0]<=xmax[0] &&
				    part.pos[1]>=xmin[1] && part.pos[1]<=xmax[1])
				    particles.add(part);			
			    }
			}
		
			/*set limits*/
			if (dirty && !particles.isEmpty())
			{	
			
				// rescale velocity range
				if (auto_vel_range) {
				    for (int d=0;d<3;d++)
				    {
						vel_min[d] = particles.getFirst().vel[d];
						vel_max[d] = vel_min[d];
				    }
				    
				    for (Particle part:particles)
				    {
						for (int d=0;d<3;d++)
						{
						    if (part.vel[d]<vel_min[d]) vel_min[d] = part.vel[d];
						    if (part.vel[d]>vel_max[d]) vel_max[d] = part.vel[d];
						}
					}
				}
				
				if (auto_speed_range) {
					speed_range[0] = 1e66;
					speed_range[1] = 0;
					
					for (Particle part:particles)
				    {
						double speed = Math.sqrt(Vector.mag3(part.vel));
						if (speed<speed_range[0]) speed_range[0] = speed;
						if (speed>speed_range[1]) speed_range[1] = speed;
				    }
				}
			
		       for (int d=0;d<3;d++)
		    	   dv[d] = (vel_max[d]-vel_min[d])/vel_bins[d];
		       ds = (speed_range[1]-speed_range[0])/speed_bins;
			
		       /*re-initialize bins, will set to zero*/		      
		        hist_speed = new double[speed_bins];
			    for (int d=0;d<3;d++)
				hist_vel[d] = new double[vel_bins[d]];
			    weight_sum = 0;
			    dirty = false;
			}   /*if dirty*/
			
			/*sample particles*/
			for (Particle part:particles)
			{
			    for (int d=0;d<3;d++)
			    {
					int vbin = (int)((part.vel[d]-vel_min[d])/dv[d]);
					if (vbin<0) vbin=0;
					if (vbin>=vel_bins[d]) vbin=vel_bins[d]-1;
					hist_vel[d][vbin] += part.mpw;
			    }
		
			    double speed = Vector.mag3(part.vel);
			    int sbin = (int)((speed-speed_range[0])/ds);
			    if (sbin<0) sbin=0;
			    if (sbin>=speed_bins) sbin = speed_bins-1;
			    hist_speed[sbin] += part.mpw;
			    weight_sum += part.mpw;
			}		
	    }	/*if sample*/
		    
	    /*write data*/
	    if (!dirty &&  (force || (Starfish.getIt()>start_it && Starfish.getIt()%skip_output==0)))
	    {
			/*write data, each histogram may have different length*/
			
			String name = Starfish.options.wd+String.format("%s_%06d.csv", prefix, Starfish.getIt());
			PrintWriter pw = null;
			try
			{
			    pw = new PrintWriter(new FileWriter(name));
			} catch (IOException ex)
			{
			    Log.error("Failed to open file "+name);
			}
			
			pw.println("speed,f_speed,u,f_u,v,f_v,w,f_w");
			int max_bins = speed_bins;
			for (int d=0;d<3;d++)
			    if (vel_bins[d]>max_bins) max_bins = vel_bins[d];
			for (int b=0;b<max_bins;b++)
			{
				int bp = Math.min(b, speed_bins-1);
		    	pw.printf("%g,%g", speed_range[0]+(bp+0.5)*ds, hist_speed[bp]/weight_sum);
			
			    for (int d=0;d<3;d++)
			    {
			    	bp = Math.min(b,vel_bins[d]-1);		// bin to print
		    		pw.printf(",%g,%g", vel_min[d]+(bp+0.5)*dv[d], hist_vel[d][bp]/weight_sum);
			    }
			    pw.printf("\n");
			    
			}
			pw.close();
			dirty = true;
	    }
	  
	}

	@Override
	public void exit()
	{
	    
	}
    }
    
    @Override
    public void process(Element element)
    {
	SampleVDF sample_vdf = new SampleVDF(element);
	Starfish.diagnostics_module.addDiagnostic(sample_vdf);	
    }
    
    public void sample()
    {
	
    }
}
