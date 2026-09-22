package starfish.plugins.het;

import org.w3c.dom.Element;
import static starfish.core.common.Constants.*;
import starfish.core.common.Starfish;
import starfish.core.domain.Mesh;

/**
 *
 * @author Lubos Brieda
 */
public class MobilityBohm extends Mobility
{
    public MobilityBohm(Element element)
    {
	super(element);
    }
    
    /*analytical model based on Bohm anomalosu diffusion*/
    @Override
    public void update()
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    double mu[][] = Starfish.domain_module.getField(mesh, "mu").getData();
	    double bfi[][] = Starfish.domain_module.getField(mesh,"bfi").getData();
	    double bfj[][] = Starfish.domain_module.getField(mesh,"bfj").getData();
	    double na[][] = Starfish.materials_module.getNeutralDensity().getField(mesh).getData();
	    double nu;

	    /*TODO: get collision rate from interactions*/
	    /*	
	    for (VolumeInteraction interaction:Starfish.getInteractionsList())
	    {
		if (!(interaction instanceof ChemicalReaction))
		    continue;

		for (Mesh mesh:Starfish.getMeshList())
		{
		    NU.interp(interaction.getDn());
		}
	    }
	    */	
	    double Kb=0.15;
	    for (int i=0;i<mesh.ni;i++)
		for (int j=0;j<mesh.nj;j++)
		{	
		    

		    /*from Kooj, 2.48*/
		    nu = 2.5e-13*na[i][j];
		    if (nu<1e6) nu=1e6;

		    double b = Math.sqrt(bfi[i][j]*bfi[i][j] + bfj[i][j]*bfj[i][j]);	

		    double mu0 = QE/(nu*ME);
		    double beta = QE*b/(ME*nu); 
		    mu[i][j] = mu0/(1+beta*beta);
		    if (b!=0) 			
			mu[i][j] += Kb/(16*b);	
		    
		}
	}
    }
    
    public static MobilityFactory mobilityBohmFactory = new MobilityFactory() {
	@Override
	public Mobility makeMobility(Element element)
	{
	    return new MobilityBohm(element); 
	}
    };
}
