package starfish.plugins.het;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import starfish.core.boundaries.Field1D;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;

class ElectronSolver
{
    Integrals integrals;
    
    ElectronSolver(Element element)
    {
    // Inputs
	t_e_ratio = InputParser.getInt("t_e_ratio",element);
	
	String val = InputParser.getValue("ppu_type", element);
	switch (val.toUpperCase())
	{
	    case "CONSTANT_VOLTAGE": ppu_type = PPU_type.ConstantVoltage;break;
	    case "CONSTANT_CURRENT": ppu_type = PPU_type.ConstantCurrent;break;
	    default: Log.error("PPU_TYPE must be CONSTANT_VOLTAGE or CONSTANT_CURRENT");
	}
	
	ppu_i_max = InputParser.getDouble("ppu_i_max",element,100);
	ppu_p_max = InputParser.getDouble("ppu_p_max",element,15e3);
	anode_current = InputParser.getDouble("anode_current",element,0.01);   //Ia for Constant Current PPU
	v_discharge = InputParser.getDouble("v_discharge",element, 300);
	t_e_cathode = InputParser.getDouble("t_e_cathode",element,25*Constants.EVtoK);
	t_e_ground = InputParser.getDouble("t_e_ground",element,1*Constants.EVtoK);
	
	//cathode potential
	phi_cathode = InputParser.getDouble("phi_cathode",element,20.0);
	
	//phi star at ground
	phi_ground = InputParser.getDouble("phi_ground",element,0.0);
	
	TE_MIN = 0.01*Constants.EVtoK;        //same as in HPHall
	TE_MAX = 100*Constants.EVtoK;
    }
    
    void init(LambdaMesh lambda_mesh, Params params, double anode_area, double Mi)
    {
	this.lm = lambda_mesh;
	lm.te_vec.setValue(1*Constants.EVtoK);
	lm.Ia = anode_current;
	this.anode_area = anode_area;
	this.Mi = Mi;
	
	t_e_store = new Field1D(lm.ni);
	
	integrals = new Integrals(lambda_mesh, params);
	
    }
    
    /*implements logic from hphall.c for "normal" mode*/
    void update()
    {
	/* ===== INTEGRATE ELECTRON EQUATIONS ===== */
	Field1D t_e_0_vec = new Field1D(lm.ni);
	
	/*electron_mark_1, seems to store value of t_e before integration probably*/
	for (int l = 0; l < lm.ni; ++l)
	    t_e_0_vec.data[l]  = lm.te_vec.data[l];    /*electron_mark_1*/
    

	if (ppu_type==PPU_type.ConstantVoltage)  // this is usually the default ... converge_phi_anode has electron_integrate in it	  
	    converge_phi_anode();
	else if (ppu_type==PPU_type.ConstantCurrent)
	    electron_integrate(anode_current);
	else
	    electric();
	
	//save lambda mesh
	//electron_integrate(0.001);
	lm.saveLambdaMesh();

    }

    PrintWriter pw_ia = null;

    /* --------------------------------------------------------------------------
    * converge_phi_anode()
    *
    * Converge the anode current to yield the anode potential.
    * Use a Newton scheme. (operation, 4/5/95)
    * --------------------------------------------------------------------------*/
   int converge_phi_anode()
   {
       double current_2;
       double phi_1, phi_2;
       double M_DOT = 1.e-8;
       double MASS_ION = 1e-23;
       double E = 1.602e-19;
       double M_DOT_AEQ = M_DOT/MASS_ION*E;
       double dcurrent = .05 / 4.0 * M_DOT_AEQ;
       int i;
       boolean power_limit;           /* power limited?  y/n  1/0 */
       double power;
       double target_power;
       double power_tolerance;
       double dp_di;               /* d(power)/d(current) */
       double phi_last;
       double i_guess;             /* guess at i to get us back to const. voltage */
       double const_phi_relax=1.0;
       double phi_eps = 1.0;
       double phi_tolerance  = 1.0;
       double anode_potential;

       /* save the temperatures so we can restore them each time we iterate */
       electron_store_t_e();

       /* set phi_last to something way out */
       phi_last = 100000.0;

       /* check if we want to power-limit or potential-limit */
       current_2 = anode_current + dcurrent;
       electron_integrate(current_2);
       phi_2 = phi_anode;

       electron_restore_t_e();
       electron_integrate(anode_current);
       phi_1 = phi_anode;

       //LB not sure if this is what phi_anode is supposed to be
       anode_potential = v_discharge + phi_cathode;

       double pv_pi = (phi_2 - phi_1)/dcurrent;

   /* Guess at what it takes to get back to constant voltage */
       i_guess = anode_current + (anode_potential - phi_1)/pv_pi;

       if ((i_guess*anode_potential) > ppu_p_max) power_limit = true;
       else power_limit = false;

       if (power_limit)
	{
	   /* operate at constant power */
	   target_power = ppu_p_max;
	   power_tolerance = .005*target_power;
	   for (i=0; (i < 10); ++i)
	    {
	       power = phi_1*anode_current;
	       dp_di = (phi_2*current_2 - power)/dcurrent;

	       /* check for convergence */
	       if ((Math.abs(power - target_power) < power_tolerance)
		   || (Math.abs(phi_1 - phi_last) < phi_eps))
		   break;
	       else
		{
		   phi_last = phi_1;

		   /* update the new anode current using Newton's method */
		   anode_current += (target_power - power)/dp_di;

		   if (anode_current < 0.0)
		       {
		       Log.warning(" ERROR converging PPL -- I < 0.0");
		       anode_current = 0.0;	
		       }

		   if (phi_1 < 0.0)
		       {
		       Log.warning(" ERROR converging -- phi < 0.0");
		       }

		   if (anode_current > ppu_i_max)
		       {
		       Log.warning(" ERROR converging -- I > PPU_I_MAX");
		       anode_current = ppu_i_max;
		       }

		   current_2 = anode_current + dcurrent;
		   electron_restore_t_e();
		   electron_integrate(current_2);
		   phi_2 = phi_anode;
		   electron_restore_t_e();
		   electron_integrate(anode_current);
		   phi_1 = phi_anode;

		}
	    }
	}
	else //not power limited
	{
	    /* operate at constant potential */
	   for (i = 0; (i < 10); ++i)
	    {

	       /* check for convergence */
	       if ((Math.abs(phi_1 - anode_potential) < phi_tolerance) || (Math.abs(phi_1 - phi_last) < phi_eps))
		   break;
	       else
		   {
		   phi_last = phi_1;

		   /* update the new anode current using Newton's method */
		   anode_current += const_phi_relax*(anode_potential - phi_1)/pv_pi;

		   if (anode_current < 0.0)
		       {
		       Log.warning(" ERROR converging -- I < 0.0");
		       anode_current = 0.0;		       
		       }

		   if (phi_1 < 0.0)
		       {
		       Log.warning(" ERROR converging -- phi < 0.0");

		       }

		   if (anode_current > ppu_i_max)
		       {
		       Log.warning(" ERROR converging -- I > PPU_I_MAX");
		       anode_current = ppu_i_max;
		       }
		   
		 // LB commenting this out
		 if (anode_current<0.001) anode_current=0.001;

		   /* compute pv_pi */
		   current_2 = anode_current + dcurrent;
		   electron_restore_t_e();
		   electron_integrate(current_2);
		   phi_2 = phi_anode;

		   electron_restore_t_e();
		   electron_integrate(anode_current);
		   phi_1 = phi_anode;

		   pv_pi = (phi_2 - phi_1)/dcurrent;

		   }
	       }
	   }

       System.out.printf("Anode current: %g\n",lm.Ia);
       
       if (pw_ia ==null)
       {
	   try
	   {
	       pw_ia = new PrintWriter(new FileWriter("ia.csv"));
	       pw_ia.println("time,Ia");
	   } catch (IOException ex)
	   {
	       Log.error("Could not open ia.csv");
	   }
       }
       pw_ia.printf("%g, %g\n", Starfish.getTime(),lm.Ia);
       if (Starfish.getIt()%100==0)
	   pw_ia.flush();
       /* return the number of iterations it took */
       return i;
   }


    /* --------------------------------------------------------------------------
     * electron_store_t_e()
     *
     * Store t_e -- Call this to store the temperature in t_e_store.
     * --------------------------------------------------------------------------*/
    void electron_store_t_e()
	{
	int l;
	int j, k;                          /* position indices */

	for (l = 0; l < lm.ni; ++l)
	    t_e_store.data[l] = lm.te_vec.at(l);

	/*
	for (j=1; j <= n_z; ++j)
	    for (k=1; k <= n_r; ++k)
	    {
		(*t_e_store_2d)(j,k) = (*t_e)(j,k);
		(*n_e_store)(j,k) = (*n_e_mean)(j,k);
	    }*/
	}



    /* --------------------------------------------------------------------------
     * electron_restore_t_e()
     *
     * Restore t_e -- Call this to restore the temperature saved in t_e_store.
     * --------------------------------------------------------------------------*/
    void electron_restore_t_e()
	{
	int l;
	int j, k;                          /* position indices */

	for (l = 0; l < lm.ni; ++l)
	    lm.te_vec.data[l] = t_e_store.at(l);

	/*
	for (j=1; j <= n_z; ++j)
	    for (k=1; k <= n_r; ++k)
	    {
		t_e[j][k] = t_e_store_2d[j][k];
		n_e_mean[j][k] = n_e_store[j][k];
	    }*/
	}


    void electric()
    {
	Log.error("electric() is undefined - is this function still being used??");
    }

    /* --------------------------------------------------------------------------
     * electron_integrate()
     *
     * Solve the electron equations for t_e and phi.
     *
     * deleted FIFE_NWC code, replaced by Hofer's model
     * --------------------------------------------------------------------------*/
    void electron_integrate(double Ia)
    {
	/*integrate te with FTCS*/

	//TODO: need to somehow recompute parameters
	/*core*/
	lm.Ia=Ia;	//update anode current on the lambda mesh, used by params.update
	integrals.update();
	
	Field1D te_new = new Field1D(lm.ni);
	
	
	double dt_world = Starfish.getDt();
	int iterations = this.t_e_ratio;
	double dt = dt_world/iterations;
	
	//set cathode temperature
	lm.te_vec.data[lm.ni-1] = t_e_cathode;

	for (int it=0;it<iterations;it++)
	{
	    //neumann boundary at anode
	    lm.te_vec.set(0,lm.te_vec.at(1));

	    for (int l=1;l<lm.ni-1;l++)
	    {
		double A1 = lm.A1.at(l);
		if (A1<=0.0) continue;

		double A2 = lm.A2.at(l);
		double L1 = lm.L1.at(l);
		double L2 = lm.L2.at(l);
		double L3 = lm.L3.at(l);
		double L4 = lm.L4.at(l);
		double L5 = lm.L5.at(l);
		double L6 = lm.L6.at(l);
		double M1 = lm.M1.at(l);
		double M2 = lm.M2.at(l);
		double N1 = lm.N1.at(l);
		double N2 = lm.N2.at(l);
		double N3 = lm.N3.at(l);
		double N4 = lm.N4.at(l);
		double N5 = lm.N5.at(l);
		double N6 = lm.N6.at(l);

		//just in case we have non uniform spacing
		double dl_minus = lm.lambda_vec.at(l)-lm.lambda_vec.at(l-1);
		double dl_plus = lm.lambda_vec.at(l+1)-lm.lambda_vec.at(l);
		double dl2 = dl_minus+dl_plus;
		
		double Te0 = lm.te_vec.at(l);
		double dTe_dl0 = (lm.te_vec.at(l+1)-lm.te_vec.at(l-1))/(dl2);
		double Te1 = 0.5*(lm.te_vec.at(l-1)+lm.te_vec.at(l));
		double Te2 = 0.5*(lm.te_vec.at(l)+lm.te_vec.at(l+1));
		double dTe_dl1 = (lm.te_vec.at(l)-lm.te_vec.at(l-1))/dl_minus;
		double dTe_dl2 = (lm.te_vec.at(l+1)-lm.te_vec.at(l))/dl_plus;

		//note these are also function of Te but using old value, assuming small change
		double Ew = lm.Ew.at(l);
		double Ei = lm.Ei.at(l);
		
		double RHS = A2*Te0 
			+ L1*Te1 + L2*Te2 
			+ L3*Te1*dTe_dl1 + L4*Te2*dTe_dl2 
			+ L5*Te1*Te1 + L6*Te2*Te2 
			+ Ew
			+ M1*Te1*dTe_dl1 + M2*Te2*dTe_dl2
			+ N1 
			+ N2*Te0 + N3*dTe_dl0 + N4*dTe_dl0*dTe_dl0
			+ N5*Te0*dTe_dl0 + N6*Te0*Te0 
			+ Ei
			;
		
		te_new.data[l] = lm.te_vec.at(l) - (dt/A1) * RHS;
	        if (!Double.isFinite(te_new.data[l]))
		    Log.error("Infinite!");
		if (te_new.at(l)<TE_MIN) te_new.data[l] = TE_MIN;
		if (te_new.at(l)>TE_MAX) te_new.data[l] = TE_MAX;
	    }

	    //copy down
	    for (int l=1;l<lm.ni-1;l++)
		lm.te_vec.data[l] = te_new.at(l);

	}
	
	/*for (int l=0;l<lm.ni;l++)
	    if (l>=3 && l<=10) lm.te_vec.data[l]=11604*40;
	    else lm.te_vec.data[l]=11604*1;*/
	
/*	System.out.printf("Te: ");
	for (int l=0;l<lm.ni;l++) System.out.printf("%.3g ",lm.te_vec.at(l));
	System.out.printf("\n");
*/	
	//now that we have temperature, update phi* on lambda mesh
	computePhiStar();
	
    }


    /* --------------------------------------------------------------------------
     * electron_ave()
     *
     * Update averages.
     * --------------------------------------------------------------------------*/
    void electron_ave()
    {
	/*not yet implemented*/
    }

    /*computes phi star, eq. 3.18*/
    void computePhiStar()
    {
	//compute phi_star at the cathode using the average density
	//A1 is int_v(3/2*k*nedV)
	double ne_cathode = lm.A1.at(lm.ni-1)/(1.5*Constants.K*lm.total_volume.at(lm.ni-1));
	double ne_anode = lm.A1.at(0)/(1.5*Constants.K*lm.total_volume.at(0));
	
	//compute phi_star_cathode
	double Te_cathode = lm.te_vec.at(lm.ni-1);
	phi_star_cathode = phi_cathode - (Constants.K/Constants.QE)*Te_cathode*Math.log(ne_cathode);
	
	lm.phi_star.data[lm.ni-1] = phi_star_cathode;
	for (int l=lm.ni-2;l>=0;l--)
	{
	    double dl = lm.lambda_vec.at(l+1)-lm.lambda_vec.at(l);
	    double dte_dl = (lm.te_vec.at(l+1)-lm.te_vec.at(l))/dl;
	    double dphi_dl = lm.h1.at(l) + lm.h2.at(l)*dte_dl + lm.h3.at(l)*lm.te_vec.at(l);
	    lm.phi_star.data[l] = lm.phi_star.data[l+1] - dphi_dl*dl;
	}
	
	//we have neuman phi at the anode incremented by anode drop
	double kTe_anode = Constants.K*lm.te_vec.at(0);	
	double anode_drop = -(kTe_anode/Constants.QE)*
			    Math.log(lm.Ia/(anode_area*Constants.QE*ne_anode*
				     Math.sqrt(kTe_anode/(2*Math.PI*Constants.ME))) +
				    Math.exp(-0.5)*Math.sqrt(2*Math.PI*Constants.ME/Mi));
	lm.phi_star.data[0] += anode_drop;

/*	System.out.printf("phi*:");
	for (int l=0;l<lm.ni;l++) System.out.printf(" %g",lm.phi_star.data[l]);
	System.out.printf("\n");
*/
	phi_anode = lm.phi_star.at(0) + Constants.K/Constants.QE*lm.te_vec.at(0)*Math.log(ne_anode);	//TODO: need actual phi

    }

 
    LambdaMesh lm;
    
    Field1D t_e_store;
    
    int t_e_ratio;
    enum PPU_type {ConstantVoltage, ConstantCurrent};
    PPU_type ppu_type;
    double ppu_i_max;
    double ppu_p_max;
    double beam_current;
    double residual_phi;
    double phi_anode;
    double v_discharge;
    double anode_current;
    double residual_t_e;
    double t_e_cathode;
    double t_e_ground;
    double phi_cathode;
    double phi_star_cathode;
    double phi_ground;
    double anode_area;
    double Mi;
    final double TE_MIN;
    final double TE_MAX;
    

}