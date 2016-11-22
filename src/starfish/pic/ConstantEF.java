/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.pic;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;

/*Solver that does nothing*/
public class ConstantEF extends Solver
{
    double ei0,ej0;
	
    public ConstantEF(double ei, double ej)
    {
	    super();
	    ei0=ei;
	    ej0=ej;
    }
	
    @Override
    public void update() 
    {
	/*clear potential*/
	for (Mesh mesh:Starfish.getMeshList())
	    Starfish.domain_module.getPhi(mesh).clear();	
    }
	
    @Override
    public void updateGradientField()
    {
	for (MeshData md:mesh_data)
	{
	    Mesh mesh = md.mesh;
	    double[][] ei = Starfish.domain_module.getEfi(mesh).getData();
	    double[][] ej = Starfish.domain_module.getEfj(mesh).getData();

	    for (int i = 0; i < mesh.ni; i++) 
		for (int j = 0; j<mesh.nj;j++)
		{
		    ei[i][j] = ei0;
		    ej[i][j] = ej0;
		}
	}
    }
    
    public static SolverModule.SolverFactory constantEFSolverFactory = new SolverModule.SolverFactory()
    {
	@Override
	public Solver makeSolver(Element element)
	{
	    String comps[];
	    double ei=0, ej=0;
	    try
	    {
		comps=InputParser.getList("comps", element);
		ei=Double.parseDouble(comps[0]);
		ej=Double.parseDouble(comps[1]);
	    } catch (Exception e)
	    {
		Starfish.Log.warning("<comps>ei,ej</comps> not found, using zero field default");
	    }

	    Solver solver=new ConstantEF(ei, ej);
	    return solver;
	}
    };

}
