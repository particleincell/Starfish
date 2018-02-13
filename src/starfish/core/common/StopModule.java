/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.common;

import org.w3c.dom.Element;
import starfish.core.common.Starfish.Log;

/** implements lt; stop &gt; command*/
public class StopModule extends CommandModule
{
    @Override
    public void process(Element element) {
	    Log.error("<stop> command: stopping input file parsing");
    }
}
