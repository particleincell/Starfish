/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.pic;

import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.materials.Material;

/** electrons from ne=n0*exp((phi-phi0)/kTe)*/
public class FluidElectronsMaterial extends Material
{
    public FluidElectronsMaterial(int index, String name) 
    {
	super(name, Constants.ME/Constants.AMU, -1);
    }

    @Override
    public void init()
    {
	super.init();
	//this.getDenCollection().setValue(Starfish.solver_module.getSolver().den0);
	this.getDenCollection().setValue(2e16);
	
	this.getTempCollection().setValue(Starfish.solver_module.getSolver().kTe0/Constants.KtoEV);
    }
	
    public interface ElectronModel {
	public void update(Material material);
    }
    
    /*TODO: specify model from input file*/
    protected ElectronModel model = electronModelNone;
    public void setElectronModel (ElectronModel model) {this.model=model;}
    
    @Override
    public void updateFields() 
    {
	model.update(this);	
    }
    
    
    public static ElectronModel electronModelNone = new ElectronModel()	    
    {
	@Override
	public void update(Material material) 
	{
	    /*do nothing*/
	}
    };
    
    /*sets electron density equal to ion density*/
    public static ElectronModel electronModelQN = new ElectronModel()	    
    {
	@Override
	public void update(Material material) 
	{
	    FieldCollection2D ndi = Starfish.materials_module.getIonDensity();
	    material.getDenCollection().clear();
	    material.getDenCollection().addData(ndi);
	}
    };
    
    public static ElectronModel electronModelBoltzmann = new ElectronModel()	    
    {
	@Override
	public void update(Material material) 
	{
	    /*TODO: need to specify electron model independently of solver*/
	    double phi0 = Starfish.solver_module.getSolver().phi0;
	    double kTe0 = Starfish.solver_module.getSolver().kTe0;
	    double den0 = Starfish.solver_module.getSolver().den0;
	    	
	    for (Mesh mesh:Starfish.getMeshList())
	    {
		/*reset values*/
		material.getDen(mesh).clear();
		double den[][] = material.getDen(mesh).getData();

		double phi[][] = Starfish.domain_module.getPhi(mesh).getData();
		for (int i=0;i<mesh.ni;i++)
		    for (int j=0;j<mesh.nj;j++)
			den[i][j]=den0*Math.exp((phi[i][j]-phi0)/kTe0);			
	    }
	}
    };
}
