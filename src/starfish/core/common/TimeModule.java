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

    /**
     *
     */
    protected int it;

    /**
     *
     */
    protected double dt;

    /**
     *
     */
    protected int num_it;

    /**
     *
     */
    protected double time;
	
    double time_initial = 0;	    //time at the start of the simulation, TODO: update by restart
    /**
     *
     */
    public boolean steady_state;
    int it_steady_state=-1;		/*user set it for steady state*/

    /**
     *
     */
    protected double ss_time;
	
    /**
     *
     * @return
     */
    public double getDt() {return dt;}

    /**
     *
     * @return
     */
    public int getNumIt() {return num_it;}

    /**
     *
     * @return
     */
    public int getIt() {return it;}

    /**
     *
     * @return
     */
    public double getTime() {return it*dt;}
    
    /**
     * 
     * @param it time step
     * @return time for a given time step
     */
    public double getTime(int it) {return it*dt;}
    
    /** @return physical simulated time*/
    public double getTimeElapsed() {return getTime()-time_initial;}
	
    /**
     *
     * @param it
     */
    public void setIt(int it) {this.it=it;}

    /**
     *
     * @param num_it
     */
    public void setNumIt(int num_it) {this.num_it=num_it;}

    /**
     *
     * @param dt
     */
    public void setDt(double dt) {this.dt=dt;}
	
    /**
     *
     */
    public TimeModule() 
    {
	super();
		
	/*set defaults*/
	dt = 1e-6;
	num_it = 100;
	time = 0;
	it = 0;
    }
	
    /**
     *
     * @return
     */
    public boolean hasTime()
    {
	if (it<num_it) return true;
	return false;
    }
    
    /*support for determining if the simulation has finished*/
    boolean finished = false;
    public boolean hasFinished() {return finished;}
    @Override
    public void finish() {finished=true;}
	
    /**advances time to next time step*/
    protected double energy_old=0;

    /**
     *
     */
    protected int ss_countdown=5;

    private boolean first = true;

    /**
     *
     */
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
		double energy_new = 0;
			
		/*iterate over all materials*/
		for (Material mat: Starfish.getMaterialsList())
		    energy_new += mat.getEnergySum();
		
		/*right now using change in total energy to set steady state*/
		if (Math.abs((energy_new-energy_old)/energy_new)<1e-3)
		    ss_countdown--;
		else
		    ss_countdown=5;
		
		if (ss_countdown<=0)
		    steady_state=true;
				
		energy_old=energy_new;
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

    /**
     *
     * @return
     */
    public double getSteadyStateTime() 
    {
	return time-this.ss_time;
    }
}
