/*
* 
 */
package starfish.plugins.het;

import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.interactions.Sigma;

/**
 *
 * @author Lubos Brieda
 */
public class MobilityNWB extends Mobility
{
    public MobilityNWB(Element element)
    {
	super(element);
	/*grab few other parameters for the analytical model*/
	alpha_n = InputParser.getDouble("alpha_n", element,1.0);
	alpha_b= InputParser.getDouble("alpha_b", element,0.15);
	alpha_w = InputParser.getDouble("alpha_w", element,1.0);
	h_w = InputParser.getDouble("h_w",element,0.015);
	see_s = InputParser.getDouble("see_s",element,0.95);
	V_0 = InputParser.getDouble("v_0",element,1000);
    }
    
    double alpha_n;	    /*classical coefficient*/
    double alpha_b;	    /*bohm coefficient*/
    double alpha_w;	    /*near wall coefficient*/
    double h_w;	    /*channel height for near wall model*/
    double see_s;	    /*secondary electron emission*/
    double V_0;	    /*bohm velocity for ions*/
	
    /*analytical model from paper by Keidar, considering wall effects
    consists of three terms: classical, bohm, and near wall conductivity	*/
    @Override
    public void update()
    {
	SigmaSzaboScatter sigma_scatter = new SigmaSzaboScatter();
	for (Mesh mesh: Starfish.getMeshList())
	{
	    double mu[][] = Starfish.domain_module.getField(mesh, "mu").getData();
	    double bfi[][] = Starfish.domain_module.getField(mesh,"bfi").getData();
	    double bfj[][] = Starfish.domain_module.getField(mesh,"bfj").getData();
	    double na[][] = Starfish.materials_module.getNeutralDensity().getField(mesh).getData();
	    double te[][] = Starfish.materials_module.getMaterial("e-").getT(mesh).getData();

	    double veth0 = Math.sqrt(2*Constants.K/Constants.ME);

	    for (int i=0;i<mesh.ni;i++)
		for (int j=0;j<mesh.nj;j++)
		{
		    if (mesh.isDirichletNode(i, j)) {mu[i][j]=0;continue;}

		    double veth = veth0*Math.sqrt(te[i][j]);  //electron thermal velocity

		    double b = Math.sqrt(bfi[i][j]*bfi[i][j] + bfj[i][j]*bfj[i][j]);
		    if (b<0.001) b=0.001;

		    double TeEv = te[i][j]*Constants.KtoEV;
		    /*from Keidar's nwc paper, the term under sqrt evaluates to s/m*/
		    double phi_w = TeEv*Math.log((1-see_s)/(V_0*Math.sqrt(2*Math.PI*Constants.ME/(Constants.K*te[i][j]))));
		    /*collision frequency*/
		    double sigma_n = sigma_scatter.eval(veth, Constants.ME);
		    double nu_n = na[i][j]*sigma_n*veth;

		    double nu_w=veth/h_w*Math.exp(-phi_w/TeEv); //double-check this, look at the literature is how to solve
		    //nuew = veth/h_w/(2.e6/(4*2.e9*0.015)); // double-check this to see how to solve the literature
		    double nu_total =alpha_n*nu_n + alpha_w*nu_w;

		    mu[i][j] = 0;
		    if (nu_total>0)
		    {
			double mu0=Constants.QE/(Constants.ME*nu_total);	    /*mobility, e/(ME*nu)*/
			double beta = Constants.QE*b/(Constants.ME*nu_total); 
			mu[i][j] = mu0/(1+beta*beta);
		    }

		    /*add bohm term*/
		     mu[i][j] += alpha_b/(16*b);
		}
	}
    }
    
    /* These functions are from Szabo's Thesis, modified to return sigma in m^2*/
    class SigmaSzaboScatter extends Sigma 
    {
	@Override
	public double eval(double g, double mass)
	{	
	    double W=0.5*Constants.ME*g*g/Constants.QE;
	    double rootE;
	    double ans;

	    rootE=Math.sqrt(W);
	    if (W<=.1592)
		    ans=1.699e-15; // not exact -- data is at 1.703, function evaluates to 1.695 
	    else if (W<=2.8)
		    ans=1.0e-13 *(
			    0.07588072747894*W*W
			    -0.34475940259139*W*rootE
			    +0.58473840309059*W
			    -0.42726069455393*rootE
			    +0.11430271021684);
	    else if (W<=24.7)
		    ans =
			    1.0e-13 *(
			    -0.00199145459640*W*W
			    +0.02974653588357*W*rootE
			    -0.16550787909579*W
			    +0.40171310068942*rootE
			    -0.31727871240879);
	    else if (W<=50)
		    ans =
			    1.0e-13 *(
			    -0.00217736834537*W*rootE
			    +0.04302155076778*W
			    -0.28567311384223*rootE
			    +0.65180228051047);
	    else if (W<=500)
		    ans =
			    1.0e-14 *(
			    -0.00002249610521*W*rootE
			    + 0.00109930275788*W
			    -0.02071463195923*rootE
			    + 0.22876772390428);
	    else
		    ans=6.400000000000000e-16;
	    return ans/1e4;
	}
    };

    
    public static MobilityFactory mobilityNWBFactory = new MobilityFactory() {
	@Override
	public Mobility makeMobility(Element element)
	{
	    return new MobilityNWB(element); 
	}
    };
}
