/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starfish.plugins.het;

import starfish.core.common.Plugin;
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
		// add solvers
		SolverModule.registerSolver("TPM",TPMSolver.TPMSolverFactory);
		SolverModule.registerSolver("HETFluid",FluidHETSolver.SolverFactory);
			
		// add rate parsers
		//RateParser.registerMathParser("fife",IonizationFife.MathParserFife);
		
		LoadFieldModule.registerReader("hphall", HPHallReader.hphallReaderFactory);
    } 
    
}
