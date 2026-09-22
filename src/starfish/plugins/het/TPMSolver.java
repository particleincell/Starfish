/* ThermalizedSolver
 * 
 * This solvers solves electron energy equation across magnetic field
 * It follows the work of Dr. Michael Fife
 * 
 * Electrons are assumed to be magnetized along the magnetic field,
 * with tangential potential given by the thermalized potential relationship
 * 
 */
package starfish.plugins.het;

import starfish.plugins.het.IonizationFife;
import starfish.plugins.het.Mobility;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import starfish.core.boundaries.Spline;
import static starfish.core.common.Constants.*;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.io.InputParser;
import starfish.core.materials.BoltzmannElectronsMaterial.ElectronModel;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;
import starfish.pic.PotentialSolver;
		
public class TPMSolver extends PotentialSolver
{	
	/*variables*/
	protected int ni,nj;		/*number of nodes*/
		
	/*[lambda,t] interpolated results*/
	protected double d_lambda;
	double anode_lambda, cathode_lambda;
	double Mi;
	Spline anode_spline;
		
	protected LambdaMesh lambda_mesh;
	Mobility mobility;
	
	/*accessors*/
	
	String bottom_list[], top_list[];
	
	Params params;
	ElectronSolver solver;
	
	PrintWriter solver_out;
	
	/*constructor*/
	public TPMSolver(Element element)
	{
	    
	    /*lambda mesh parameters*/
	    Element lme = InputParser.getChild("lambda_mesh", element);	    
	    if (lme==null) Log.error("<lambda_mesh> not found");
	    
	    int nn[] = InputParser.getIntList("nodes", lme);
	    if (nn.length<2) Log.error("need 2 integers for nodes");
	    this.ni = nn[0];
	    this.nj = nn[1];	    	    
	    bottom_list= InputParser.getList("bottom", lme);
	    top_list= InputParser.getList("top", lme);	
	    anode_lambda = InputParser.getDouble("anode_lambda",lme);
	    cathode_lambda = InputParser.getDouble("cathode_lambda",lme);
	
	    String ion_mat_name = InputParser.getValue("ion_mat", element, "xe+");
	    Material ion_mat = Starfish.getMaterial(ion_mat_name);
	    if (ion_mat!=null)
		Mi = ion_mat.getMass();
	    else
		Log.error("Ion material "+ion_mat_name+" not found");
	    
	    //anode spline (for area)
	    String anode_spline_name = InputParser.getValue("anode_spline", element);
	    anode_spline = Starfish.boundary_module.getBoundary(anode_spline_name);
	    if (anode_spline==null) Log.error("Anode spline "+anode_spline_name+" not found");
	    
	    Element mob_element = InputParser.getChild("mobility", element);
	    if (mob_element==null) Log.error("<mobility> not found");
	    mobility = Mobility.makeMobility(mob_element);
	    
	    params = new Params(element);
	    
	    solver = new ElectronSolver(element);
	    
	}
	
	@Override
	public void init()
	{
	    super.init();
		
	    /*make sure we have fluid electrons*/
	    Material emat = Starfish.getMaterial("e-");
	    if ((emat instanceof KineticMaterial)) Log.error("TPM cannot be run with kinetic electrons");
		
	    /*make sure the density is not being updated*/
	    //((BoltzmannElectronModel)emat).setElectronModelNone();
				
	    lambda_mesh = new LambdaMesh(ni,nj,anode_lambda,cathode_lambda,bottom_list,top_list);
	    lambda_mesh.saveLambdaMesh();
	    
	    mobility.setLambdaMesh(lambda_mesh);
	    params.setLambdaMesh(lambda_mesh);
	    solver.init(lambda_mesh, params, anode_spline.area(), Mi);
	   
	    try {
		solver_out = new PrintWriter(new FileWriter("solver1d.csv"));
		solver_out.println("time, Ia, Vd, W");
	    } catch (IOException ex) {
		Logger.getLogger(TPMSolver.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
	
	@Override
	public void exit()
	{
		mobility.update();
		solver_out.close();
		lambda_mesh.saveLambdaMesh();
	}
	
	boolean first_time = true;
	double Ia_old;
	
	/*** THERMAL SOLVER*****************************************
	 * */
	@Override
	public void update()
	{	
	    /*interpolate data onto the lambda mesh*/
	    lambda_mesh.domainToLambda();

	    /*compute mobility on lambda mesh*/
	    mobility.update();

	    params.update();

	    solver.update();

	    lambda_mesh.lambdaToDomain(solver);
	}
		
	/*returns distance between two points*/
	double dist(double i1,double j1,double i2,double j2)
	{
		double x1[] = lambda_mesh.mesh.pos(i1, j1);
		double x2[] = lambda_mesh.mesh.pos(i2, j2);
		double dx = x1[0]-x2[0];
		double dy = x1[1]-x2[1];
		return Math.sqrt(dx*dx+dy*dy);	
	}



    public static SolverModule.SolverFactory TPMSolverFactory = new SolverModule.SolverFactory()
    {
	@Override
	public Solver makeSolver(Element element)
	{
	    TPMSolver solver=new TPMSolver(element);
					
	    /*log*/
	    Log.log("Added Thermalized Potential solver");
	    //Log.log("> n0=" + n0 + " (#/m^3)");
	    return solver;
	}
    };
}
