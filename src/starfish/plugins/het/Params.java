package starfish.plugins.het;

import starfish.plugins.het.IonizationFife;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.core.boundaries.Field1D;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.interactions.RateParser;
import starfish.core.materials.Material;

class Params
{
    int ni, nj;

    final double EI = IonizationFife.EI;
    LambdaMesh lambda_mesh;
    

    protected boolean first_time=true;
    PrintWriter pw;
    Field1D Ie;
	
    /** constructor*/
    Params(Element element)
    {
		Element ele = InputParser.getChild("params", element);
		if (ele==null) Log.error("Did not find <params> element");
		
		// Inputs
		d_eff_A = InputParser.getDouble("d_eff_a",ele,0.141);
		d_eff_B = InputParser.getDouble("d_eff_b",ele,0.576);
		var_phi_A = InputParser.getDouble("var_phi_A",ele,0.254);
		var_phi_B = InputParser.getDouble("var_phi_B",ele,0.677);
		var_phi_C = InputParser.getDouble("var_phi_C",ele,2.00);
		
		this.lambda_wall_begin[0] = InputParser.getDouble("lambda_wall_inner_begin", ele);
		this.lambda_wall_end[0] = InputParser.getDouble("lambda_wall_inner_end",ele);
		lambda_wall_begin[1] = InputParser.getDouble("lambda_wall_outer_begin",ele);
		lambda_wall_end[1] = InputParser.getDouble("lambda_wall_outer_end", ele);
		
		try
		{
		    pw = new PrintWriter(new FileWriter("current.csv"));
		    pw.println("time, Ii_max, Ie_max");
		} catch (IOException ex)
		{
		    Log.error("Failed to open current file");
	}
    }
    
    public void setLambdaMesh(LambdaMesh lambda_mesh)
    {
	this.lambda_mesh = lambda_mesh;
	this.ni = lambda_mesh.ni;
	this.nj = lambda_mesh.nj;
	Ie = new Field1D(ni);
	h_denom = new Field1D(ni);
    }
    
    /**computes e,f,h,j,k params used by the temperature solver,
     * 
     * @param dt
     * @param Ia
     * @param te_l
     * @param phi_s
     * @param d_lambda
     * @param I 
     */
    void update()
    {
	/*compute ion current*/
	lambda_mesh.II.setValue(0);
	ComputeIonCurrentFields();
	//ComputeIonCurrentParticles();
	
	double Ii_max = lambda_mesh.II.getRange()[1];
	System.out.printf("Ii_max %g\n",Ii_max);
	
	if (Ii_max<0.001) Ii_max=0.001;
	
	/*computes e1 and e2, terms for electron velocity in terms of phi star*/
	ComputeE();
	
	/*compute f and h, these are per-lambda values that involve integration along the lambda line*/
	ComputeFandH();
	
	//compute remaining parameters
    	ComputeRest();
	
	//compute parameters that scale with Ia
	ComputeIaParams();
	
	pw.printf("%g, %g, %g\n",Starfish.getTime(),Ii_max, Ie.getRange()[1]);
	pw.flush();
    }
    
    /*computes ion current from fields*/
    void ComputeIonCurrentFields()
    {
	lambda_mesh.II.clear();
	for (int i=0;i<ni;i++)
	{	    
	    for (int j=0;j<nj-1;j++)
	    {
		lambda_mesh.II.data[i]+=Constants.QE*0.5*
			(lambda_mesh.NE.at(i, j)*lambda_mesh.UPERP.at(i,j)+
			 lambda_mesh.NE.at(i, j+1)*lambda_mesh.UPERP.at(i,j+1))*lambda_mesh.edge_area.at(i,j);
	    }
	}
	
    }
   
   /*computes ion current directly form particles*/
    void ComputeIonCurrentParticles()
    {
	/*clear current*/
	lambda_mesh.II.clear();
	
	/*loop over all particles*/
	for (Material mat:Starfish.getMaterialsList())
	{
	    /*for now process only kinetic materials and also only ions*/
	    if (!(mat instanceof KineticMaterial) || mat.charge<=0) continue;
	    	    
	    KineticMaterial km = (KineticMaterial) mat;
	    for (Mesh mesh:Starfish.getMeshList())
	    {
		Iterator<Particle> it = km.getIterator(mesh);
		while (it.hasNext())
		{
		    Particle part = it.next();
		    
		    /*this is now the actual "core"*/

		    double lc[];
		    
		    //Transform3Dto2D(part.pos,x2d);
		    
		    lc = lambda_mesh.mesh.XtoL(part.pos);

		    double pos_old[] = new double[3];
		    double dt = Starfish.getDt();
		    pos_old[0] = part.pos[0] - part.vel[0]*dt;
		    pos_old[1] = part.pos[1] - part.vel[1]*dt;
		    pos_old[2] = part.pos[2] - part.vel[2]*dt;
		    
		    //repeat for previous position
		   // Transform3Dto2D(pos_old,x2d);
		    double lc_old[] = lambda_mesh.mesh.XtoL(pos_old);
		    
		    if (lc_old[0]<0 && pos_old[0]>=lambda_mesh.mesh.pos1(0,0)
				    && pos_old[0]<lambda_mesh.mesh.pos1(lambda_mesh.ni-1,0))
		    {
			lc_old= lambda_mesh.mesh.XtoL(pos_old);
		    }
		    
		    //this or the past position need to be in lambda mesh
		    if (lc[0]>=0 || lc_old[0]>=0)
		    {
			int sign = 1;
			double l1 = lc_old[0];
			double l2 = lc[0];

			//swap l1 and l2 if particle moving to the left
			if (l1>=0 && l2>=0 && l1>l2)
			{
			    double t=l1;
			    l1=l2;
			    l2=t;
			    sign = -1;
			}

			//TODO: this is not right since only particles crossing lambda lines are counted,
			//any current internal to control volume is not seen unless it crosses the lambda line
			//this assumes i1<i2
			int i1=(int)Math.ceil(l1); //round up
			int i2=(int)Math.floor(l2); //round down
			if (i1<0) i1=0;    //if particle started to the left of the mesh
			if (i2<0 && i1>0) i2=lambda_mesh.ni-1;  //i2=-1 means outside the mesh

			for (int i=i1;i<=i2;i++)
			    lambda_mesh.II.add(i, sign*km.charge*part.mpw/Starfish.getDt());

		    } /*if within lambda mesh*/

		} /*particle loop*/

	    }	/*for mesh*/
	} /*for material*/
   }
   
    /** computes e1 and e2*/
    void ComputeE()
    {
	LambdaMesh lm = lambda_mesh;
	RateParser fife_eval = IonizationFife.MathParserFife;

	for (int l=0;l<lambda_mesh.ni;l++)
	    for (int j=0;j<lambda_mesh.nj;j++)
	    {
		double rB = lm.R.at(l,j)*lm.BF.at(l,j);
		
		lm.e1.data[l][j] = -lm.MU.at(l,j) * rB;
		lm.e2.data[l][j] = -lm.MU.at(l,j) * rB* (Constants.K/Constants.QE) * (Math.log(lm.NE.at(l,j))-1);
	    }
    }

    /*computes f, h, and Ew which are 1D parameters*/
     void ComputeFandH()
    {
	LambdaMesh lm = lambda_mesh;	//to save on typing
	int nr = lm.nj;
	Ie.clear();
		    
	for (int l=0;l<lm.ni;l++)
	{
	    double lambda = lm.lambda_vec.at(l);
	    	
	    //grab values at bottom[0] and top[1]
	    double B[] = {lm.BF.at(l,0),lm.BF.at(l,nr-1)};
	    double r[] = {lm.R.at(l,0), lm.R.at(l,nr-1)};
	    double r2[] = {r[0]*r[0], r[1]*r[1]};
	    double ne[] = {lm.NE.at(l,0), lm.NE.at(l,nr-1)};
	    double Te = lm.te_vec.at(l);

	    //evaluate dne_dl using 2.11
	    double dne_dl[] = {0,0};
	    int l1,l2;
	    if (l>0 && l<lm.ni-1) {l2=l+1;l1=l-1;}
	    else if (l==0) {l2=1;l1=0;}
	    else {l2=lm.ni-1;l1=lm.ni-2;}
	    double dne[]=new double[2];
	    dne[0] = lm.NE.at(l2,0) - lm.NE.at(l1,0);
	    dne[1] = lm.NE.at(l2,1) - lm.NE.at(l1,1);
	    double dl = lm.lambda_vec.at(l2)-lm.lambda_vec.at(l1);
	    dne_dl[0] = dne[0]/dl;
	    dne_dl[1] = dne[1]/dl;

	    double c_e = Math.sqrt(8*Constants.K*Te/(Constants.PI*Constants.ME));        //mean electron thermal speed

	    double flux_e[] = {0.25*ne[0]*c_e, 0.25*ne[1]*c_e};
	    double d_eff = Utils.gamma(2+d_eff_B)*d_eff_A*Math.pow(Constants.K*Te/Constants.QE, d_eff_B);

	    /*Fife states that d_eff~1 for ion repelling sheath*/
	    if (Te>16.4*Constants.EVtoK) d_eff=1;

	    double flux_sec[] = {flux_e[0]*d_eff, flux_e[1]*d_eff};        //2.67

	    //we are computing same term on top and bottom wall so using a loop to avoid code duplication
	    lm.f1.set(l,0);
	    lm.f2.set(l,0);
	    lm.f3.set(l,0);
	    for (int k = 0;k<2;k++)
	    {
		//only evaluate if within wall bounds
		if (lambda<lambda_wall_begin[k] || lambda>lambda_wall_end[k]) continue;

		lm.f1.data[l] -= Constants.ME*2*Constants.PI*(r2[k]*flux_sec[k]/B[k]);
		lm.f2.data[l] -= Constants.ME*2*Constants.PI*(r2[k]*flux_sec[k]*Constants.K/(B[k]*Constants.QE*ne[k])*dne_dl[k]);
		lm.f3.data[l] -= Constants.ME*2*Constants.PI*(r2[k]*flux_sec[k]*Constants.K*Math.log(ne[k])/(Constants.QE*B[k]));
	    }

	    //Ew term
	    double Mi = 131.3 *Constants.AMU;    //TODO: for now hardcoded for Xenon
	    double Tsec = 1000;        //TODO: probably should use wall temperature
	    double vb = Math.sqrt(Constants.K*Te/Mi);
	    double phi_w;
	    double qw_prime;
	    
	    if (d_eff<0.999) //regular sheath
	    {
		phi_w = -(Constants.K*Te/Constants.QE)*Math.log((1-d_eff)*Math.exp(0.5)*c_e/(4*vb));
		qw_prime = Math.exp(Constants.QE*phi_w/(Constants.K*Te)) * (2*Constants.K*(Te-d_eff*Tsec)-(1-d_eff)*Constants.QE*phi_w);
	    }
	    else //ion repelling sheath, 2.72
	    {
		phi_w = (Constants.K*Tsec/Constants.QE)*Math.log(d_eff);
		qw_prime = 2*Constants.K*(Te-Tsec);
	    }
		


	    if (!Double.isFinite(qw_prime))
		Log.error("Infinite qw");
	    
	    double area[] = {lm.edge_axial_area.at(l,0), lm.edge_axial_area.at(l,nr-1)};   
	    lm.Ew.set(l,0);
	    for (int k=0;k<2;k++)
	    {
		//only evaluate if within wall bounds
		if (lambda<lambda_wall_begin[k] || lambda>lambda_wall_end[k]) continue;

		lm.Ew.data[l] += ne[k]*area[k];
	    }
	    lm.Ew.data[l] *= qw_prime*c_e/4;

	    //compute integrals for h terms
	    double h2_i=0;
	    h_denom.data[l] = 0;

	    for (int k=0;k<nr-1;k++)
	    {
		/*grab terms at bottom and top of this cell, to help with testing and make more readable*/
		ne[0] = lm.NE.at(l,k);
		ne[1] = lm.NE.at(l,k+1);    //electron density
		
		B[0] = lm.BF.at(l,k);
		B[1] = lm.BF.at(l, k+1);

		double dA = lm.edge_area.at(l,k);

		//ne*mu*B*(ln(ne)-1)*r^2*ds
		h2_i -= Constants.QE * 0.5*(lm.e2.at(l,k)*ne[0] + 
					   lm.e2.at(l,k+1)*ne[1]) * dA;	
		//ne*mu*B*r^2*ds
		h_denom.data[l] -= Constants.QE*0.5*(lm.e1.at(l,k)*ne[0] + 
						     lm.e1.at(l,k+1)*ne[1]) * dA;
		
		//for testing, integral of ne*uen*r*ds to get electron current
		double ue[] = {lm.UE.at(l,k), lm.UE.at(l, k+1)};
		Ie.data[l] += 0.5* (ne[0]*ue[0] + ne[1]*ue[1])*dA;	
	    }

	    h_denom.data[l] -= lm.f1.at(l);
	    
	    Ie.data[l] *= Constants.QE;
	    
	    if (h_denom.at(l)!=0)
	    {		
		lm.h2.set(l, (lm.f3.at(l) - h2_i) / h_denom.at(l));
		lm.h3.set(l, lm.f2.at(l) / h_denom.at(l));
	    }
	}  
	
//	System.out.printf("Ie: ");
//	for (int i=0;i<lm.ni;i++) System.out.printf("%g ", Ie.at(i));
//	System.out.printf("\n");
	
    }

    /*computes 2D parameters, e, j, and k, and also Ke_prime*/
    void ComputeRest()
    {
	LambdaMesh lm = lambda_mesh;
	RateParser fife_eval = IonizationFife.MathParserFife;
    
	
	for (int l=0;l<lambda_mesh.ni;l++)
	    for (int j=0;j<lambda_mesh.nj;j++)
	    {
		double rB = lm.R.at(l,j)*lm.BF.at(l,j);

		/*terms that are function of Ia are h1, j1, k1*/
		lm.j1.data[l][j] = lm.e1.at(l,j) * lm.h1.at(l);
		lm.j2.data[l][j] = lm.e1.at(l,j) * lm.h2.at(l) + lm.e2.at(l,j);
		lm.j3.data[l][j] = lm.e1.at(l,j) * lm.h3.at(l);
		
		//k1 = -r*B*h1
		lm.k1.data[l][j] = -rB * lm.h1.at(l);
		
		//k2 = -r*B*(h2 + (k/e)*ln(ne))
		lm.k2.data[l][j] = -rB * (lm.h2.at(l) + (Constants.K/Constants.QE)*Math.log(lm.NE.at(l,j)));
		
		//k3 = -r*B*(h3 + (k/e)*ln(ne)*dne/dl)
		lm.k3.data[l][j] = -rB * (lm.h3.at(l) + Constants.K/(Constants.QE*lm.NE.at(l,j)) * (lm.DNE_DL.at(l,j)));

		//thermal conductivity normalized by temperature
		lm.Ke_prime.data[l][j] = (5*lm.NE.at(l,j)*Constants.K*Constants.K*lm.MU.at(l,j))/(2*Constants.QE);

		//compute ue_n for testing
		double Te = lm.te_vec.at(l);
		int l2,l1;
		if (l==0) {l2 = l+1; l1=l;}
		else if (l==lm.ni-1) {l2=l;l1=l-1;}
		else {l2=l+1;l1=l-1;}
		double d_lambda = lm.lambda_vec.at(l2)-lm.lambda_vec.at(l1);
		double dTe_dl = (lm.te_vec.at(l2)-lm.te_vec.at(l1))/d_lambda;
		
		lm.UE.data[l][j] = lm.j1.at(l,j) + lm.j2.at(l,j)*dTe_dl + lm.j3.at(l,j)*Te;
		if (!Double.isFinite(lm.UE.data[l][j]))
		    Log.error(String.format("Boo %g, %g %g %g",lm.UE.data[l][j],
			    lm.j1.at(l,j),lm.j2.at(l,j),lm.j3.at(l,j)));
		
		//ionization energy loss
		double var_sigma = fife_eval.eval(Te,new double[1],null);
		double dni_dt = var_sigma*lm.NE.data[l][j]*lm.NA.data[l][j];
		double theta =  Constants.K*Te/EI;
		double var_phi_prime = var_phi_A*Math.exp(-var_phi_B/theta)+var_phi_C;
		lm.Si.data[l][j] = dni_dt*var_phi_prime*EI;

	    }
   
    }
    
    //computes parameters that are a function of anode current
    void ComputeIaParams()
    {
	LambdaMesh lm = lambda_mesh;
	
	for (int l=0;l<lm.ni;l++)
	{
	    if (h_denom.at(l)!=0)
	    {		
		lm.h1.set(l, (-lambda_mesh.Ia + lm.II.at(l))/h_denom.at(l));

	    }
	    
	    for (int j=0;j<lambda_mesh.nj;j++)
	    {
		double rB = lm.R.at(l,j)*lm.BF.at(l,j);
		
		/*terms that are function of Ia are h1, j1, k1*/
		lm.j1.data[l][j] = lm.e1.at(l,j) * lm.h1.at(l);
		
		//k1 = -r*B*h1
		lm.k1.data[l][j] = -rB * lm.h1.at(l);
	    }
	}  
	
    }
    
    
    int lambda_index;
    double d_eff_A, d_eff_B;    //parameters for computing secondary electron yield
    double var_phi_A, var_phi_B, var_phi_C;
    double lambda_wall_begin[] = new double[2]; 
    double lambda_wall_end[] = new double[2];
    boolean file_output;
    Field1D h_denom;
}