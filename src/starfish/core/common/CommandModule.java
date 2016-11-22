/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

/** Base class for all input-file callable modules*/
package starfish.core.common;

import org.w3c.dom.Element;

public abstract class CommandModule 
{
    protected boolean has_started = false;	/*used by some modules to prevent repeated start*/
    
    /**Constructor for level 1 module*/
    public CommandModule()
    {
	/*to nothing*/
    }
	
    /** function called prior to start of processing input file*/
    public void init()
    {
	/*do nothing*/
    }

    /** reads element data and performs appropriate action
    * @param element XML element containing data objects*/
    public abstract void process(Element element);
		
    /** function called prior to main loop*/
    public void start()
    {
	/*do nothing*/
    }

    /** function called after end of main loop*/
    public void finish() 
    {
	/*do nothing*/
    }
	
    /** function called prior to program exit*/
    public void exit()
    {
	/*do nothing*/
    }
}
