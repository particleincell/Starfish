/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package starfish.plugins.het;

import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;

/**
 *
 * @author Lubos Brieda
 */
public class MobilityLynx extends Mobility
{
     	/**calls Lynx to update mobility*/
	boolean first_time = true;

	public MobilityLynx(Element element)
	{
	    super(element);
	}
	
	@Override
	public void update()
	{
	  //  if (first_time) {updateMobilityNWB();first_time=false;return;}
	   	
	    lambda_mesh.domainToLambda();
	    
	    double te[][]=lambda_mesh.TE.getData();
	    for (int i=0;i<lambda_mesh.ni;i++)
		for (int j=0;j<lambda_mesh.nj;j++)
		    te[i][j]/=Constants.EVtoK;
	
	    
	    //lambda_mesh.saveLambdaMesh();
		
		
	    lynx.Reader reader = new lynx.Reader(lambda_mesh.ni,lambda_mesh.nj,lambda_mesh.Z.getData(),lambda_mesh.R.getData(), 
				     lambda_mesh.LAMBDA.getData(),lambda_mesh.BF.getData(),lambda_mesh.NA.getData(),lambda_mesh.NE.getData(),
				     lambda_mesh.UPERP.getData(),lambda_mesh.EPERP.getData(),lambda_mesh.TE.getData());

	    //read settings file
	    lynx.Settings settings = new lynx.Settings();	
	    settings.it_orbit = 150;
	    settings.max_it = 10000;
	    settings.it_collisions=4;
	    settings.solve_potential=true;
	    settings.wall_model=lynx.Domain.WallModelType.DUNAEVSKY;
	    settings.out_trace=-1;
	    settings.electrons_per_cell = 200;
	    settings.min_it_ss=2000;
	    settings.delta_averaging=1000;
	    settings.ld_per_dh=2;
	    settings.out_fields=2000;
	    settings.conductor=true;	/*dirichlet walls*/
	    settings.cylindrical=false;	/*cylindrical solution not right, fix!*/
	    settings.file_output=true;
	    settings.max_rl=1.0;
	    
	    lynx.Main.RunSims(reader,settings);
	    reader.Save2D("results2d.dat");
	    //get mobility
	    lambda_mesh.MU.setDataShallow(reader.getMobility());
	    FieldCollection2D mu_fc = Starfish.domain_module.getFieldCollection("mu");
	    
	    /*LB: 2/2018: commnented out during Mobility refactor, need to figure out how to call this
	    probably make an instance of MobilityNWB	    */
	    
	    /*use classical model to "prefill" the nodes*/
	   // updateMobilityNWB();	
	    
	    /*replace data where we have values from lambda mesh*/
	    for (Mesh mesh:Starfish.getMeshList())
	    {
		Field2D mu2d = mu_fc.getField(mesh);
		mu2d.interpWithOverwrite(lambda_mesh.MU);
		for (int i=0;i<mesh.ni;i++)
		    for (int j=0;j<mesh.nj;j++)
		    {
			mu2d.data[i][j] = Math.abs(mu2d.data[i][j]);
			if (mu2d.data[i][j]>100) mu2d.data[i][j]=100;
		    }
	    }
	}

    public static MobilityFactory mobilityLynxFactory = new MobilityFactory() {
	@Override
	public Mobility makeMobility(Element element)
	{
	    return new MobilityLynx(element); 
	}
    };
}
