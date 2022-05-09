/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starfish.plugins.plasma_dynamics;

import starfish.core.common.Plugin;
//import starfish.core.interactions.RateParser;
import starfish.core.solver.SolverModule;

public class PlasmaDynamicsPlugin implements Plugin
{
    @Override
    public void register()
    {
//RateParser.registerMathParser("BOSCH_HALE",BoschHale.MathParserBoschHale);
		SolverModule.registerSolver("BOYD",BoydSolver.BoydSolverFactory);
		SolverModule.registerSolver("GENG",GengSolver.GengSolverFactory);
		SolverModule.registerSolver("MAG", MagSolver.MagSolverFactory);
		
    }
    
}

