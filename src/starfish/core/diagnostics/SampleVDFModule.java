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
	double speed_min,speed_max;
	double vmin[] = new double[3];
	double vmax[] = new double[3];
	double dv[] = new double[3];
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
		
		/*get limits*/
		if (dirty && !particles.isEmpty())
		{		
		    for (int d=0;d<3;d++)
		    {
			vmin[d] = particles.getFirst().vel[d];
			vmax[d] = vmin[d];
		    }
		    speed_min = Math.sqrt(vmin[0]*vmin[0] + vmin[1]*vmin[1] + vmin[2]*vmin[2]);
		    speed_max = speed_min;	    //since the velocities are the same
		    
		    weight_sum = 0;

		    for (Particle part:particles)
		    {
			for (int d=0;d<3;d++)
			{
			    if (part.vel[d]<vmin[d]) vmin[d] = part.vel[d];
			    if (part.vel[d]>vmax[d]) vmax[d] = part.vel[d];
			}
			double speed = Vector.mag3(part.vel);
			if (speed<speed_min) speed_min = speed;
			if (speed>speed_max) speed_max = speed;
		    }
		
		    for (int d=0;d<3;d++)
			dv[d] = (vmax[d]-vmin[d])/vel_bins[d];
		    ds = (speed_max-speed_min)/speed_bins;

		    /*re-initialize bins, will set to zero*/
		    hist_speed = new double[speed_bins];
		    for (int d=0;d<3;d++)
			hist_vel[d] = new double[vel_bins[d]];
		    
		    dirty = false;
		}   /*if dirty*/
		
		/*sample particles*/
		for (Particle part:particles)
		{
		    for (int d=0;d<3;d++)
		    {
			int vbin = (int)((part.vel[d]-vmin[d])/dv[d]);
			if (vbin<0) vbin=0;
			if (vbin>=vel_bins[d]) vbin=vel_bins[d]-1;
			hist_vel[d][vbin] += part.spwt;
		    }
		    double speed = Vector.mag3(part.vel);
		    if (speed<speed_min) speed_min = speed;
		    if (speed>speed_max) speed_max = speed;
		    int sbin = (int)((speed-speed_min)/ds);
		    if (sbin<0) sbin=0;
		    if (sbin>=speed_bins) sbin = speed_bins-1;
		    hist_speed[sbin] += part.spwt;
		    weight_sum += part.spwt;
		}		
	    }	/*if sample*/
	    
	    /*write data*/
	    if (!dirty &&   //if initialized
		(force || (Starfish.getIt()>start_it && Starfish.getIt()%skip_output==0)))
	    {
		/*write data, each histogram may have different length*/
		
		String name = String.format("%s_%06d.csv", prefix, Starfish.getIt());
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
		    if (b<speed_bins)
			pw.printf("%g,%g", speed_min+(b+0.5)*ds, hist_speed[b]/weight_sum);
		    else
			pw.printf(",");

		    for (int d=0;d<3;d++)
		    {
			if (b<vel_bins[d])
			    pw.printf(",%g,%g", vmin[d]+(b+0.5)*dv[d], hist_vel[d][b]/weight_sum);
			else
			    pw.printf(",,");
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
