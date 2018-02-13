/* *****************************************************
*(c) 2012 Particle In Cell Consulting LLC
*
*This document is subject to the license specified in 
*Starfish.java and the LICENSE file
******************************************************/
package starfish.core.solver;

import java.util.HashMap;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;
import starfish.pic.ConstantEF;
import starfish.pic.FluidElectronsMaterial;
import starfish.pic.PoissonSolver;
import starfish.pic.QNSolver;

/**
 *
 * @author Lubos Brieda
 */
public class SolverModule extends CommandModule
{
    /*set default solver to no-field*/
    Solver solver=new ConstantEF(0, 0);

    /**
     *
     * @param solver
     */
    public void addSolver(Solver solver)
    {
	this.solver=solver;
    }

    /**
     *
     * @return
     */
    public Solver getSolver()
    {
	return solver;
    }

    @Override
    public void init()
    {
	registerSolver("CONSTANT-EF",ConstantEF.constantEFSolverFactory);
	registerSolver("QN",QNSolver.boltzmannSolverFactory);
	registerSolver("POISSON",PoissonSolver.poissonSolverFactory);
    }

    @Override
    public void process(Element element)
    {
	String type=null;

	try
	{
	    type=InputParser.getValue("type", element);
	} catch (NoSuchElementException e)
	{
	    Log.error("Syntax <solver type=SOLVER_TYPE>");
	}

	SolverFactory fac = solver_factories.get(type.toUpperCase());
	if (fac!=null)
	    solver = fac.makeSolver(element);
	else
	{
	    Log.error("Unrecognized solver type " + type);
	}

	//how often do we update potential, <=0 implies only initial field?
	boolean initial_only = InputParser.getBoolean("initial_only", element, false);
	
	/*get solver parameters*/
	int lin_max_it=InputParser.getInt("max_it", element, 5000);
	double lin_tol=InputParser.getDouble("tol", element, 1e-6);
	int nl_max_it=InputParser.getInt("nl_max_it", element, 50);
	double nl_tol=InputParser.getDouble("nl_tol", element, 1e-4);
	Log.log("> max_it (linear) = " + lin_max_it);
	Log.log("> tol (linear) = " + lin_tol);
	Log.log("> nl_max_it (non-linear) = " + nl_max_it);
	Log.log("> nl_tol (non-linear) = " + nl_tol);
	Log.log("> initial only = " + initial_only);
	solver.setLinParams(lin_max_it, lin_tol);
	solver.setNLParams(nl_max_it, nl_tol);
	solver.initial_only = initial_only;
    }

    /**
     *
     */
    public interface SolverFactory 
    {

	/**
	 *
	 * @param element
	 * @return
	 */
	public Solver makeSolver(Element element);
    }
    
    static HashMap<String,SolverFactory> solver_factories = new HashMap<String,SolverFactory>();

    /**
     *
     * @param name
     * @param fac
     */
    public static void registerSolver(String name, SolverFactory fac)
    {
	solver_factories.put(name.toUpperCase(),fac);
	Log.log("Added solver "+name.toUpperCase());
    }
    
    @Override
    public void exit()
    {
	solver.exit();
    }

    /**
     *
     */
    public void updateFields()
    {
	if (solver.initial_only && !solver.first) return;
	
	/*update rho*/
	updateRho();

	/*call solver*/
	solver.update();
	
	/*update electric field*/
	solver.updateGradientField();
	
	solver.first = false;
    }

    /**
    *updates charge density
     */
    void updateRho()
    {
	for (Mesh mesh : Starfish.getMeshList())
	{
	    double rho[][]=Starfish.domain_module.getRho(mesh).getData();

	    /*reset values*/
	    for (int i=0; i<mesh.ni; i++)
	    {
		for (int j=0; j<mesh.nj; j++)
		{
		    rho[i][j]=0;
		}
	    }

	    /*loop over species and add densities*/
	    for (Material mat : Starfish.getMaterialsList())
	    {
		if (mat.getCharge() == 0 || (mat instanceof FluidElectronsMaterial))
		{
		    continue;
		}

		double den[][]=mat.getDen(mesh).getData();
		double charge=mat.getCharge();

		for (int i=0; i<mesh.ni; i++)
		    for (int j=0; j<mesh.nj; j++)
		    {
			rho[i][j]+=den[i][j]*charge;
		    }
	    }
	    
	}
    }

    @Override
    public void start()
    {
	solver.init();
    }
}
