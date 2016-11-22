/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.common;

import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import starfish.core.common.Starfish.Log;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;

/** Module for maintaining current simulation time*/
public class TimeModule extends CommandModule
{
    protected int it;
    protected double dt;
    protected int num_it;
    protected double time;
	
    public boolean steady_state;
    int it_steady_state=-1;		/*user set it for steady state*/
    protected double ss_time;
	
    public double getDt() {return dt;}
    public int getNumIt() {return num_it;}
    public int getIt() {return it;}
    public double getTime() {return it*dt;}
	
    public void setIt(int it) {this.it=it;}
    public void setNumIt(int num_it) {this.num_it=num_it;}
    public void setDt(double dt) {this.dt=dt;}
	
    public TimeModule() 
    {
	super();
		
	/*set defaults*/
	dt = 1e-6;
	num_it = 100;
	time = 0;
	it = 0;
    }
	
    public boolean hasTime()
    {
	if (it<num_it) return true;
	return false;
    }
	
    /**advances time to next time step*/
    protected double momentum_old=0;
    protected int ss_countdown=5;

    private boolean first = true;
    public void advance()
    {
	/*check if we have reached steady state*/
	if (!steady_state)
	{
	    if (it_steady_state>=0)
	    {
		if (it>=it_steady_state) steady_state=true;
	    }
	    else
	    {
		double momentum_new = 0;
			
		/*iterate over all materials*/
		for (Material mat: Starfish.getMaterialsList())
		    momentum_new += mat.getTotalMomentum();
		
		/*The achievable tolerance depends on number of particles, 
		 * hard to reach anything less than 1e-3 with under 10,000 particles*/
		if (Math.abs((momentum_new-momentum_old)/momentum_new)<1e-2)
		    ss_countdown--;
		else
		    ss_countdown=5;
		
		if (ss_countdown<=0)
		    steady_state=true;
				
		momentum_old=momentum_new;
	    }
			
	    if (steady_state)
	    {
		ss_time = time;
		Log.message("** Reached steady state **");
	    }
	} /*if not steady state*/
	
	it++;
	time += dt;	
    }
	
    @Override
    public void init()
    {
	/*do nothing*/
    }

    @Override
    public void process(Element element) 
    {
	try {
	    dt = InputParser.getDouble("dt", element);
	    num_it = InputParser.getInt("num_it", element);
	}
	catch (NoSuchElementException e)
	{
	    Log.log("<dt> and/or <num_it> not specified, using default values");
	}
		
	/*check for steady state*/
	String ss;
	ss = InputParser.getValue("steady_state", element,"auto");
				
	if (ss.equalsIgnoreCase("auto"))
	    it_steady_state=-1;
	else
	    it_steady_state = Utils.parseInt(ss);
		
	Log.log("Time:");
	Log.log(">dt: "+dt);
	Log.log(">max_it: "+num_it);
	Log.log(">steady_sate: "+it_steady_state);	
    }

    @Override
    public void exit() 
    {
    }

    @Override
    public void start() 
    {
	/*todo: implement*/
    }

    public double getSteadyStateTime() 
    {
	return time-this.ss_time;
    }
}
