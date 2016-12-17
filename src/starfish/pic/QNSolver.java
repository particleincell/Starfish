/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.pic;

import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Vector;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.io.InputParser;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;

public class QNSolver extends PotentialSolver
{
    public QNSolver (double den0, double phi0,double Te0)
    {
	/*reference values*/
	this.den0 = den0;
	this.kTe0 = Te0;	    /* in eV */
	this.phi0 = phi0;
    }

    @Override
    public void update() 
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    int ni = mesh.ni;
	    int nj = mesh.nj;
	    double phi[][] = Starfish.domain_module.getPhi(mesh).getData();
	    double rho[][] = Starfish.domain_module.getRho(mesh).getData();
			
	    for (int i=0;i<ni;i++)
		for (int j=0;j<nj;j++)
		{
		    if (mesh.nodeType(i, j) == NodeType.DIRICHLET)
			continue;
					
		    double ion_den = rho[i][j]/Constants.QE;
		    if (ion_den>0)
			phi[i][j] = phi0 + kTe0*Math.log(ion_den/den0);
		    else
			phi[i][j] = phi0 + kTe0*Math.log(1e-10);	/*background O(10) less than den0*/						
		}
	    }
    }
    
    public static SolverModule.SolverFactory boltzmannSolverFactory = new SolverModule.SolverFactory()
    {
	@Override
	public Solver makeSolver(Element element)
	{
	    double n0=InputParser.getDouble("n0", element);
	    double Te0=InputParser.getDouble("Te0", element);
	    double phi0=InputParser.getDouble("phi0", element);
	    Solver solver=new QNSolver(n0, phi0, Te0);

	    /*log*/
	    Starfish.Log.log("Added BOLTZMANN solver");
	    Starfish.Log.log("> n0  =" + n0 + " (#/m^3)");
	    Starfish.Log.log("> T0 =" + Te0 + " (eV)");
	    Starfish.Log.log("> phi0 =" + phi0 + " (V)");
	    
	    return solver;
	}	
    };


}
