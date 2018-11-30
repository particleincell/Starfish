/*
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.diagnostics;

import java.util.ArrayList;
import starfish.core.common.Starfish;

/** storage container for various probes and other diagnostics
 *
 * @author Lubos Brieda
 */
public class DiagnosticsModule
{
    public interface Diagnostic
    {
	public void sample(boolean force);	//called each time step
	public void exit();	//called after main loop ends
    }
    
    ArrayList<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
    
    /** runs all registered diagnostics*/
    public void sampleAll(boolean force)
    {
	for (Diagnostic diag:diagnostics)
	    diag.sample(force);
    }
    
    public void exitAll()
    {
	for (Diagnostic diag:diagnostics)
	    diag.exit();
    }
    
    /*adds diagnostic to the list if the simulation has not yet started,
    otherwise calls sample*/
    public void addDiagnostic(Diagnostic diag)
    {
	if (!Starfish.time_module.hasFinished())
	    diagnostics.add(diag);
	else
	    diag.sample(true);
    }
}



