/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starfish.plugins.het;

import starfish.core.common.Plugin;
import starfish.core.common.Starfish;
import starfish.core.diagnostics.AveragingModule;
import starfish.core.io.LoadFieldModule;
import starfish.core.solver.SolverModule;


/**
 *
 * @author lbrieda
 */
public class HETPlugin implements Plugin
{   
    @Override
    public void register()
    {
		
    	// register new command
    	Starfish.register("compute_lambda", new ComputeLambdaModule());
    	
    	// add solvers
		SolverModule.registerSolver("TPM",TPMSolver.TPMSolverFactory);
		SolverModule.registerSolver("HETFluid",FluidHETSolver.SolverFactory);
		
			
		// add rate parsers
		//RateParser.registerMathParser("fife",IonizationFife.MathParserFife);
		
		LoadFieldModule.registerReader("hphall", HPHallReader.hphallReaderFactory);
    } 
    
}
