/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish.Log;

/** handles &lt;note &gt;*/
public class NoteModule extends CommandModule 
{
    @Override
    public void init()
    {
	/*do nothing*/
    }

    @Override
    public void process(Element element) 
    {
	/*print the note*/
	Log.message("**"+InputParser.getFirstChild(element)+"**");
    }

    @Override
    public void exit() 
    {
	/*nothing to do*/
    }

    @Override
    public void start() 
    {
	/*nothing to do*/
    }
}
