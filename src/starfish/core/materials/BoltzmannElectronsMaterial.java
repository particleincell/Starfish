/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.materials;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.solver.Solver;
import starfish.pic.PotentialSolver;

/** electrons from ne=n0*exp((phi-phi0)/kTe)*/
public class BoltzmannElectronsMaterial extends Material
{

    /**
     *
     * @param element xml file element
     * @param name
     */
    public BoltzmannElectronsMaterial(Element element, String name) 
    {
	super(name);
	charge = -Constants.QE;
	mass = Constants.ME;
	frozen = false;
	
	/*read model*/
	String model_name = InputParser.getValue("model",element,"BOLTZMANN");
	switch (model_name.toUpperCase())
	{
	    case "BOLTZMANN": model = electronModelBoltzmann;break;
	    case "QN": model = electronModelQN;break;
	    default: Log.error("Unsupported electron model "+model_name);
	}
	
	/*read in reference values*/
	phi0 = InputParser.getDouble("phi0", element, 0);		    
	kTe0 = InputParser.getDouble("kTe0",element, -1);
	den0 = InputParser.getDouble("n0",element, -1);	//negative density used as a flag to get values from solver
	
	/*read fixed region settings*/
	Element fixed_els[] = InputParser.getChildren("fixed_region", element);
	for (Element fixed_el:fixed_els)
	{
	    FixedRegion fr = new FixedRegion();
	    
	    fr.x0 = InputParser.getDoubleList("x0", fixed_el);
	    fr.xm = InputParser.getDoubleList("xm", fixed_el);
	    fr.density = InputParser.getDouble("density",fixed_el,-1);
	    fr.temperature = InputParser.getDouble("temperature",fixed_el,-1);
	    if (fr.density>=0 || fr.temperature>=0)
		fixed_regions.add(fr);
	}	
    }

    @Override
    public void init()
    {
	super.init();
	Solver solver = Starfish.solver_module.getSolver();
	if (solver instanceof PotentialSolver) {
	    PotentialSolver ps = (PotentialSolver) solver;
	    this.getDenCollection().setValue(ps.den0);
	    this.getTempCollection().setValue(ps.kTe0/Constants.KtoEV);
	}
    }
	
    /**
     *
     */
    public interface ElectronModel {

	/**
	 *
	 * @param material
	 */
	public void update(Material material);
    }
    
    /*reference values for Boltzmann model*/
    double phi0;
    double den0;
    double kTe0;
    
    protected ElectronModel model = electronModelNone;
    /**
     * @param model
     */
    public void setElectronModel (ElectronModel model) {this.model=model;}
    
    @Override
    public void updateFields() 
    {
	model.update(this);	
	applyFixedRegion();
    }
    
    protected class FixedRegion {
	double x0[];
	double xm[];
	double density = -1;
	double temperature = -1;
    }
    ArrayList<FixedRegion> fixed_regions = new ArrayList<>();
    
    /*sets fixed density and temperature within a small region*/
    protected void applyFixedRegion()
    {
	for (FixedRegion fr:fixed_regions)
	{
	    for (Mesh mesh:Starfish.getMeshList())
	    {
		double den[][] = this.getDen(mesh).getData();
		double temp[][] = this.getT(mesh).getData();
		
		for (int i=0;i<mesh.ni;i++)
		    for (int j=0;j<mesh.nj;j++)
		    {
			double pos[] = mesh.pos(i,j);
			if (pos[0]>=fr.x0[0] && pos[0]<=fr.xm[0] &&
			    pos[1]>=fr.x0[1] && pos[1]<=fr.xm[1])
			{
			    if (fr.density>=0) 
				den[i][j] = fr.density;
			    if (fr.temperature>=0)
				temp[i][j] = fr.temperature;
			}
		    }
	    }
	}
    }
    
    /**
     *
     */
    public static ElectronModel electronModelNone = new ElectronModel()	    
    {
	@Override
	public void update(Material material) 
	{
	    /*do nothing*/
	}
    };
    
    /*sets electron density equal to ion density*/

    /**
     *
     */

    public ElectronModel electronModelQN = new ElectronModel()	    
    {
	@Override
	public void update(Material material) 
	{
	    FieldCollection2D ndi = Starfish.materials_module.getIonDensity();
	    material.getDenCollection().clear();
	    material.getDenCollection().addData(ndi);
	    
	    if (kTe0<=0)
		Log.error("Non zero temperature <kTe0> needs to be specified for QN electron fluid");
	    material.getTempCollection().setValue(kTe0*Constants.EVtoK);
	}
    };
    
    /**
     *
     */
    public ElectronModel electronModelBoltzmann = new ElectronModel()	    
    {
	@Override
	public void update(Material material) 
	{
	    /*get values from solver if not specified*/
	    if (den0<=0 || kTe0<=0)
	    {
		Solver  solver = Starfish.solver_module.getSolver();
		if (solver instanceof PotentialSolver) {
		    PotentialSolver ps = (PotentialSolver) solver;	
		    phi0 = ps.phi0;
		    den0 = ps.den0;
		    kTe0 = ps.kTe0;		 
		}
		if (kTe0<=0)
		    Log.error("Non zero temperature <kTe0> needs to be specified for QN electron fluid");
	    }
	    	
	    for (Mesh mesh:Starfish.getMeshList())
	    {
		/*reset values*/
		material.getDen(mesh).clear();
		double den[][] = material.getDen(mesh).getData();

		double phi[][] = Starfish.domain_module.getPhi(mesh).getData();
		for (int i=0;i<mesh.ni;i++)
		    for (int j=0;j<mesh.nj;j++)
		    {
			den[i][j]=den0*Math.exp((phi[i][j]-phi0)/kTe0);			
			if (den[i][j]<1) den[i][j] = 1;	    //the exponential model can give tiny numbers
		    }
		
		material.getT(mesh).setValue(kTe0*Constants.EVtoK);
	    }
	}
    };
    
    public static MaterialsModule.MaterialParser BoltzmannElectronsMaterialParser = 
	    new MaterialsModule.MaterialParser() 
    {
	@Override
	public Material addMaterial(String name, Element element)
	{
	    Material material = new BoltzmannElectronsMaterial(element, name);
	    return material;
        }
    };
}
