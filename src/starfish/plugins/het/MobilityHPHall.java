/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package starfish.plugins.het;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.io.InputParser;

/**
 *
 * @author Lubos Brieda
 */
public class MobilityHPHall extends Mobility
{
    
        /*user params*/
    double Q_E_N_ELASTIC;      /* total elastic cross section of electrons in Xe. Mitchner, P.103 */
    double lambda_end_insulator_1;
    double lambda_bohm_1;
    double lambda_bohm_2;
    double lambda_bohm_3;
    double lambda_bohm_4;
    double BOHM_DC_K;
    double BOHM_EXIT_K;
    double BOHM_PLUME_K;
    double EN_K;
    double EI_K;
    double NWC_K;
    boolean use_xenon_nu_en;

    public MobilityHPHall(Element element)
    {
	super(element);
	Q_E_N_ELASTIC = InputParser.getDouble("q_e_n_elastic", element, 3.0e-19);
	lambda_end_insulator_1 = InputParser.getDouble("lambda_end_insulator",element); //Lambda value for where insulator ends and NWC is no longer present
	double lambdas[] = InputParser.getDoubleList("lambda_bohm", element);
	if (lambdas.length<4) Log.error("Four values expected for lambda_bohm: anode, dc, exit, plume");
	
	for (int i=0;i<3;i++)
	    if (lambdas[i+1]<=lambdas[i]) Log.error("lambda_bohm values must be unique and increasing");
	
	lambda_bohm_1 = lambdas[0];
	lambda_bohm_2 = lambdas[1];
	lambda_bohm_3 = lambdas[2];
	lambda_bohm_4 = lambdas[3];
	
	BOHM_DC_K = InputParser.getDouble("bohm_dc_k",element);
	BOHM_EXIT_K = InputParser.getDouble("bohm_exit_k",element);
	BOHM_PLUME_K = InputParser.getDouble("bohm_plume_k",element);
	EN_K = InputParser.getDouble("en_k",element,1.0);
	EI_K = InputParser.getDouble("ei_k",element,1.0);
	NWC_K = InputParser.getDouble("nwc_K",element,1.0);
	use_xenon_nu_en = InputParser.getBoolean("use_xenon_nu_en", element,true); //Flag whether specialized xenon model should be used for e-n collision rate
    }

    @Override
    public void update()
    {
	Field2D MU = lambda_mesh.MU;
	Field2D BF = lambda_mesh.BF;
	Field2D NA = lambda_mesh.NA;
	Field2D NE = lambda_mesh.NE;
	Field2D TE = lambda_mesh.TE;
	Field2D LAMBDA = lambda_mesh.LAMBDA;

	for (int i=0;i<lambda_mesh.ni;i++)
	    for (int j=0;j<lambda_mesh.nj;j++)
	    {		    
		double nu_en;
		double nu_wm;
		double nu_ei;
		double nu_b=0;

		/*evaluate various properties at this node*/
		double t_e = TE.at(i,j);
		double t_e_ev = t_e*Constants.KtoEV;
		double n_n = NA.at(i,j);
		double n_e = NE.at(i,j);
		double lambda = LAMBDA.at(i,j);
		double b_mag = BF.at(i,j);

		/*electron neutral momentum transfer*/
		if (use_xenon_nu_en)
		{
		    nu_en = EN_K*n_n*Math.sqrt(8.0*Constants.K*t_e/Constants.PI/Constants.ME);

		    // McEachan data (default) ... actually a theory based cross-section
		    if(t_e_ev < 1) // set coll freq to value at 1 eV since this fit goes negative at low energy.
			nu_en *= 1e-20 * (94 * ((1/2.21)-0.364)/(1+ Math.pow((1/2.21),1.7) )+18*1/(18+1));
		    else
			nu_en *= 1e-20 * (94 * ((t_e_ev/2.21)-0.364)/(1+ Math.pow((t_e_ev/2.21),1.7) )+18*t_e_ev/(18+t_e_ev));
		}
		else // any other gas except Xenon ... hphall.h
		    nu_en = EN_K*n_n*Q_E_N_ELASTIC*Math.sqrt(8.0*Constants.K*t_e/Constants.PI/Constants.ME); // Fife's version of nu_en .. we keep this in case we want to run non-xenon cases

		/* Compute wall collision frequency. Set to zero if downstream of insulator (see above). */
		if ( lambda <= lambda_end_insulator_1 ) // this is not properly generalized for offset walls.
		{
		    //commented out since I need to figure out what is da_vec and dv_vec
		    /*
			double mu_wm = (*i_e_w_1d)(l)/((*e_field_2_1d)(l)*(*da_vec)(l)*n_e)/MKS_e;
			nu_wm = mu_wm*MKS_e*(*b_w)(l,i)*(*b_w)(l,i)/MKS_me;
			nu_wm += gamma_p_1*delta_eff*(*area_1_vec)(l); // This is the second-half of the computation.  See above.
			nu_wm /= (*dv_vec)(l)*n_e;
			nu_wm *= NWC_K;*/

		     /*for now*/
			nu_wm=0;

			if (nu_wm < 0)
			    nu_wm = 0;

		}
		else
		{
		    nu_wm = 0;
		}

		/*  Compute electron-ion collision frequency */
		/* Expression neglects doubly-charged effects in Coulomb logarithm, which are negligible. */
		double coulomb_log;
		if (t_e_ev <= 10) // See NRL Plasma Formulary. Formulas are in CGS, hence the conversions.
		{
		    coulomb_log = 23 - Math.log( Math.sqrt(n_e/1e6) * Math.pow((t_e_ev),-1.5) );
		}
		else
		{
		    coulomb_log = 24 - Math.log(Math.sqrt(n_e/1e6) / (t_e_ev) );
		}
		nu_ei = EI_K * 2.91e-6 * (n_e/1e6) * Math.pow((t_e_ev),-1.5) * coulomb_log;

		/* Compute Bohm collision frequency */
		/* Apply different Bohm factors depending on our location wrt some axial position (e.g., exit plane) */
		/* Here, lambda_bohm_X are being used to specify a smoothing region */

		if ( lambda < lambda_bohm_1 )
		{
		    nu_b = BOHM_DC_K / 16.0 * (Constants.QE*b_mag/Constants.ME);
		}
		else if ( lambda >= lambda_bohm_1 && lambda <= lambda_bohm_2 )
		{
		    double frac_2 = (lambda - lambda_bohm_1) / (lambda_bohm_2 - lambda_bohm_1);
		    double frac_1 = 1 - frac_2;
		    double frac_1_tot = frac_1*BOHM_DC_K;
		    double frac_2_tot = frac_2*BOHM_EXIT_K;
		    nu_b = ( frac_1_tot + frac_2_tot ) / 16.0 * (Constants.QE*b_mag/Constants.ME);
		}
		else if ( lambda > lambda_bohm_2 && lambda < lambda_bohm_3)
		{
		    nu_b = BOHM_EXIT_K / 16.0 * (Constants.QE*b_mag/Constants.ME);
		}
		else if ( lambda >= lambda_bohm_3 && lambda <= lambda_bohm_4 )
		{
		    double frac_2 = (lambda - lambda_bohm_3) / (lambda_bohm_4 - lambda_bohm_3);
		    double frac_1 = 1 - frac_2;
		    double frac_1_tot = frac_1*BOHM_EXIT_K;
		    double frac_2_tot = frac_2*BOHM_PLUME_K;
		    nu_b = ( frac_1_tot + frac_2_tot ) / 16.0 * (Constants.QE*b_mag/Constants.ME);
		}
		else
		{
		    nu_b = BOHM_PLUME_K / 16.0 * (Constants.QE*b_mag/Constants.ME);
		}
	    
	    /* Compute the total effective electron collision frequency, Hall parameter, and mobility. */
	    double nu_eff = nu_en + nu_ei + nu_wm + nu_b; //total effective electron collision frequency
	    double mu_eff = 0;
	    if (nu_eff>0) 
	    {
		double hall_p = Constants.QE/Constants.ME*b_mag/nu_eff;
		mu_eff = Constants.QE/Constants.ME/nu_eff/(1+hall_p*hall_p); // total electron cross-field mobility
	    }

	    MU.set(i, j, mu_eff);
	}
    }
     
    public static MobilityFactory mobilityHPHallFactory = new MobilityFactory() {
	@Override
	public Mobility makeMobility(Element element)
	{
	    return new MobilityHPHall(element); 
	}
    };
}
