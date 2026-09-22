/*
 * 2D Fluid Hall thruster solver
 * Written by Jinyue
 */
package starfish.plugins.het;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.LinearList;
import starfish.core.domain.Field2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.io.InputParser;
import starfish.core.solver.Solver;
import starfish.core.solver.SolverModule;
import starfish.core.solver.SolverModule.SolverFactory;

public class FluidHETSolver extends Solver
{
    public static SolverFactory SolverFactory = new SolverModule.SolverFactory()
    {
	@Override
	public Solver makeSolver(Element element)
	{ 
	    double discharge_voltage = InputParser.getDouble("discharge_voltage",element,275);
	    double cathode_phi = InputParser.getDouble("cathode_phi",element,25);
	    
	    String te_file = InputParser.getValue("te_file",element);
	    double te_sample_r = InputParser.getDouble("te_sample_r", element);
	    String bottom_list[] = InputParser.getList("bottom", element);
	    String top_list[] = InputParser.getList("top", element);
	    double anode_pos[] = InputParser.getDoubleList("anode_pos", element);
	    double cathode_pos[] = InputParser.getDoubleList("cathode_pos", element);
	    FluidHETSolver solver = new FluidHETSolver(te_file, te_sample_r, bottom_list, top_list);
	    
	    double anode_phi = cathode_phi+discharge_voltage;
	    solver.setAnodeCathode(anode_pos, cathode_pos, anode_phi, cathode_phi);
	    
	    Element mob_ele = InputParser.getChild("mobility", element);
	    solver.mobility = Mobility.makeMobility(mob_ele);
	
	    return solver;
	}
    };        
    
    private String bottom_list[],top_list[];
    
    /**CONSTRUCTOR**/
     private FluidHETSolver(String te_file, double te_sample_r, String bottom_list[], String top_list[])
    {
	mesh = Starfish.getMeshList().get(0);
	
	/*these are flipped for now since starfish is RZ vs ZR*/
	NI = mesh.ni;
	NJ = mesh.nj;
	DZ = mesh.pos1(1,0)-mesh.pos1(0,0); /*really DZ*/
	DR = mesh.pos2(0,1)-mesh.pos2(0,0);
	
	z = new double[NI][NJ];
	r = new double[NI][NJ];
	b = new double[NI][NJ];
	bz = new double[NI][NJ];
	br = new double[NI][NJ];
	btheta = new double[NI][NJ];
	te = Starfish.materials_module.getMaterial("e-").getT(mesh).getData();
	
	this.bottom_list = bottom_list;
	this.top_list = top_list;
	
	/*electron temperature loading from a file*/
	this.te_sample_r =te_sample_r;
	try
	{
	    Scanner sc = new Scanner(new FileReader(te_file));
	    
	    while (sc.hasNextLine())
	    {
		String line = sc.nextLine();
		/*try to split and parse*/
		String pieces[] = line.split("\\s+");
		if (pieces.length==2)
		{
		    try {
			
			double z = Double.parseDouble(pieces[0]);
			double te = Constants.EVtoK*Double.parseDouble(pieces[1]);
			TE_list.insert(z, te);
			
		    } catch (NumberFormatException ex)    
		    {
			/*do nothing*/
		    }
		    
		}
		
	    }	    
	} catch (FileNotFoundException ex)
	{
	    Starfish.Log.error("Failed to open "+te_file);
	}
    }
     
    /**sets anode and cathode extends. Lambda is modified to be zero at anode_pos*/
    void setAnodeCathode(double anode_pos[], double cathode_pos[], double anode_phi, double cathode_phi)
    {
	this.anode_pos = anode_pos;
	this.cathode_pos = cathode_pos;
	this.anode_phi = anode_phi;
	this.cathode_phi = cathode_phi;
    }
	    
    LinearList TE_list = new LinearList();
    double te_sample_r;
    Mesh mesh = null;
    Mobility mobility;
    
    final double  DZ;    ///
    final double  DR;    //with multiples
    final double  OMEGA   =   1.9;
    final double  TOL  =   1.e-4;   
    int NI;     //with multiples
    int NJ;     

    final double K = 1.3806505e-23; // Boltzmann constant
    final double ME = 9.109e-31; 			// electron mass kg
    final double QE = 1.602e-19; 	// electron charge c
    final double PI = 3.1415926535;
    final double Amu0 = 1.2566e-6;

    double z[][];
    double r[][];
    double b[][];
    double bz[][];
    double br[][];
    double[][] btheta ;
    double te[][];
    
    boolean visited[][];
    double anode_pos[], cathode_pos[];
    double cathode_phi, anode_phi;
    
    	
    @Override
    public void init()
    {
	int i,j;


	//--------------------------------------------define the magnetic case3------------------------------------------------------	  Scanner fp7;
	Field2D bz_field = Starfish.domain_module.getFieldCollection("bfi").getField(mesh);
	Field2D br_field = Starfish.domain_module.getFieldCollection("bfj").getField(mesh);
		
	/*load B field data*/
	for(j=0;j<NJ;j++)     
	{
	    for(i=0;i<NI;i++)   
	    {
		double pos[] = mesh.pos(i, j);
		z[i][j] = pos[0];
		r[i][j] = pos[1];
		
		double bz_val = bz_field.at(i, j);
		double br_val = br_field.at(i, j);
		
		b[i][j]=Math.sqrt(bz_val*bz_val + br_val*br_val);
		bz[i][j]=bz_val;
		br[i][j]=br_val;
		btheta[i][j]=0;
	    }
	} 

	/*integrate lambda*/
	visited = new boolean[NI][NJ];
	
	/*set electron temperature*/
	for ( i=0;i<NI;i++)
	    for (j=0;j<NJ;j++)
	    {
		if (mesh.pos2(i,j)<0.036)
		    te[i][j] = TE_list.eval(mesh.pos1(i,j));	
		else te[i][j] = 1160.4;
	    }	
    }
    
    //-------------------------------------------------------------------------------------------------------------------------
    //---------------------------------------Electric field calculation subroutine--------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------
    @Override
    public void update()
    {
	int     i,j;
	int     k=0;            //Magnetic iterative step number
	
	double grad_j_sum=0;
	double sum=0;
	double phi_old[][] = new double[NI][NJ];
	
	double   mu11[][] = new double[NI][NJ];
	double   mu12[][] = new double[NI][NJ];
	double   mu21[][] = new double[NI][NJ];
	double   mu22[][] = new double[NI][NJ];
	double   sig[][] = new double[NI][NJ];
	double eze[][] = new double[NI][NJ];
	double ezw[][] = new double[NI][NJ];
	double ezn[][] = new double[NI][NJ];
	double ezs[][] = new double[NI][NJ];
	double ere[][] = new double[NI][NJ];
	double erw[][] = new double[NI][NJ];
	double ern[][] = new double[NI][NJ];
	double ers[][] = new double[NI][NJ];
	double[][] Z1 = new double[NI][NJ];
	double[][] Z2 = new double[NI][NJ];
	double[][] Z3 = new double[NI][NJ];
	double[][] R1 = new double[NI][NJ];
	double[][] R2 = new double[NI][NJ];
	double[][] R3 = new double[NI][NJ];
	double z1e[][] = new double[NI][NJ];
	double z1w[][] = new double[NI][NJ];
	double z1n[][] = new double[NI][NJ];
	double z1s[][] = new double[NI][NJ];
	double z2e[][] = new double[NI][NJ];
	double z2w[][] = new double[NI][NJ];
	double z2n[][] = new double[NI][NJ];
	double z2s[][] = new double[NI][NJ];
	double z3e[][] = new double[NI][NJ];
	double z3w[][] = new double[NI][NJ];
	double z3n[][] = new double[NI][NJ];
	double z3s[][] = new double[NI][NJ];
	double r1e[][] = new double[NI][NJ];
	double r1w[][] = new double[NI][NJ];
	double r1n[][] = new double[NI][NJ];
	double r1s[][] = new double[NI][NJ];
	double r2e[][] = new double[NI][NJ];
	double r2w[][] = new double[NI][NJ];
	double r2n[][] = new double[NI][NJ];
	double r2s[][] = new double[NI][NJ];
	double r3e[][] = new double[NI][NJ];
	double r3w[][] = new double[NI][NJ];
	double r3n[][] = new double[NI][NJ];
	double r3s[][] = new double[NI][NJ];
	double grad_j[][] = new double[NI][NJ];
	double jz[][] = new double[NI][NJ];
	double jr[][] = new double[NI][NJ];

	  /*get existing phi*/
	double phi[][] = Starfish.domain_module.getPhi(mesh).getData();
	double ez[][] = Starfish.domain_module.getEfi(mesh).getData();
	double er[][] = Starfish.domain_module.getEfj(mesh).getData();
	
	/*update electron temperature*/
	//double te[][] = Starfish.materials_module.getMaterial("e-").getT(mesh).getData();
	
	/*get ion and electron density, ne should = ni*/
	double ni[][] = Starfish.materials_module.getIonDensity().getField(mesh).getData();
	double ne[][] = new double[NI][NJ];
	for (i=0;i<NI;i++)
	    for (j=0;j<NJ;j++)
	    {
		sum=0;
		int count=0;
		if (i>0 && mesh.nodeType(i-1, j)!=NodeType.DIRICHLET) {sum+=ni[i-1][j];count++;}
		if (i<NI-1 && mesh.nodeType(i+1, j)!=NodeType.DIRICHLET) {sum+=ni[i+1][j];count++;}
		if (j>0 && mesh.nodeType(i, j-1)!=NodeType.DIRICHLET) {sum+=ni[i][j-1];count++;}
		if (j>NJ-1 && mesh.nodeType(i, j+1)!=NodeType.DIRICHLET) {sum+=ni[i][j+1];count++;}
		
		sum+= 4*ni[i][j];
		count+=4;
		ne[i][j] = sum/count;		
		if (ne[i][j]<1e4) ne[i][j]=1e4;
	    }
	
	/*also get ion velocity*/
	double jz_ion[][] = Starfish.materials_module.getIonJi().getField(mesh).getData();
	double jr_ion[][] = Starfish.materials_module.getIonJj().getField(mesh).getData();
	for (i=0;i<NI;i++) jr_ion[i][0]=0;  /*centerline*/
		
	double mu[][] = Starfish.getField(mesh, "mu").getData();
	
	/*update mobility*/
	mobility.update();

	
	/*this solver assumes Te is in Ev*/
	for ( i=0;i<NI;i++)
	    for (j=0;j<NJ;j++)
	    {
		te[i][j] *= Constants.KtoEV;	/*this solver assumes Te is in ev*/
	    }
	
	/*find channel midpoint*/
	int j_mid_left;
	int j_mid_right;
	
	int start=0,end=0;
	for (j=0;j<NJ;j++)
	{
	    if (mesh.nodeType(0, j)==NodeType.OPEN &&
		   (j==0 || mesh.nodeType(0,j-1)==NodeType.DIRICHLET))
		start=j;
	    if (mesh.nodeType(0, j)==NodeType.OPEN &&
		   (j==NJ-1 || mesh.nodeType(0,j+1)==NodeType.DIRICHLET))
		    end=j; 
	}
	j_mid_left = (int) (0.5*(start+end));
	
	/*repeat on right*/
	for (j=0;j<NJ;j++)
	{
	    if (mesh.nodeType(NI-1, j)==NodeType.OPEN &&
		   (j==0 || mesh.nodeType(NI-1,j-1)==NodeType.DIRICHLET))
		start=j;
	    if (mesh.nodeType(NI-1, j)==NodeType.OPEN &&
		   (j==NJ-1 || mesh.nodeType(NI-1,j+1)==NodeType.DIRICHLET))
		    end=j; 
	}
	j_mid_right = (int) (0.5*(start+end));
	
	
	//-----------------------------------------define the variables except magnetic---------------------------------------------	  
	for(i=0;i<NI;i++)
	    for(j=0;j<NJ;j++)
	    {
		ez[i][j]=0;
		er[i][j]=0;
		sig[i][j]=QE*ne[i][j]*mu[i][j];		    /*conductivity, mu*n*e*/
	    } /*for*/

    /*compute node coefficients*/
    for(i=0;i<NI;i++)
       for(j=0;j<NJ;j++)
       {	   
	   /*matrix components for equations 9 and 10*/
	   double denom = (1.+mu[i][j]*mu[i][j]*b[i][j]*b[i][j]);	/*1+mu^2*B^2*/
	   
	   mu11[i][j]=(1. + mu[i][j]*mu[i][j]*bz[i][j]*bz[i][j]) / denom;
	   mu12[i][j]=(mu[i][j]*btheta[i][j] + mu[i][j]*mu[i][j]*bz[i][j]*br[i][j]) / denom;
	   mu21[i][j]=(-mu[i][j]*btheta[i][j] + mu[i][j]*mu[i][j]*bz[i][j]*br[i][j]) / denom;
	   mu22[i][j]=(1. + mu[i][j]*mu[i][j]*br[i][j]*br[i][j]) / denom;
	   	   
	   Z1[i][j]=mu11[i][j]*sig[i][j];
	   Z2[i][j]=mu12[i][j]*sig[i][j];
	   R1[i][j]=mu21[i][j]*sig[i][j];
	   R2[i][j]=mu22[i][j]*sig[i][j];
	   
	   double gradz_term = 0;
	   double gradr_term = 0;
	   if (i>0 && i<NI-1 && mesh.nodeType(i-1,j)==NodeType.OPEN && mesh.nodeType(i+1,j)==NodeType.OPEN)	/*internal cell*/
	       gradz_term = ((te[i+1][j]-te[i-1][j]) + te[i][j]* (Math.log( ne[i+1][j]/ne[i-1][j]) ))/(2.0*DZ);
	   else if (i==0 || (i<NI-1 && mesh.nodeType(i-1,j)!=NodeType.OPEN))
	       gradz_term = ((te[i+1][j]-te[i][j]) + te[i][j]*(Math.log(ne[i+1][j]/ne[i][j])))/(DZ);
	   else
	       gradz_term = ((te[i][j]-te[i-1][j]) + te[i][j]*(Math.log(ne[i][j]/ne[i-1][j])))/(DZ);
	   
	   if (j>0 && j<NJ-1 && mesh.nodeType(i,j-1)==NodeType.OPEN && mesh.nodeType(i,j+1)==NodeType.OPEN)	/*internal cell*/
	       gradr_term = ((te[i][j+1]-te[i][j-1]) + te[i][j]*(Math.log(ne[i][j+1] / ne[i][j-1])))/(2.0*DR);
	   else if (j==0)
	       gradr_term = 0;
	   else if (j<NJ-1 && mesh.nodeType(i,j-1)!=NodeType.OPEN)
	       gradr_term = ((te[i][j+1]-te[i][j]) + te[i][j]*(Math.log(ne[i][j+1]/ne[i][j])))/(DR);
	   else
	       gradr_term = ((te[i][j]-te[i][j-1]) + te[i][j]*(Math.log(ne[i][j]/ne[i][j-1])))/(DR);
	   
	   /*todo: temporary hack to get rid of the negative point at i=8, j=4 due to extremely larged gradz and grad_r*/
	  /* gradr_term = (gradr_term<-50000)?-50000:gradr_term;
	   gradr_term = (gradr_term>50000)?50000:gradr_term;
	   gradz_term = (gradz_term<-50000)?-50000:gradz_term;
	   gradz_term = (gradz_term>50000)?50000:gradz_term;
	   */
	   
	   Z3[i][j] = Z1[i][j]*gradz_term + Z2[i][j]*gradr_term;
	   R3[i][j] = R1[i][j]*gradz_term + R2[i][j]*gradr_term;
	   
	   /*add ion term*/
	   Z3[i][j] -= jz_ion[i][j];
	   R3[i][j] -= jr_ion[i][j];
	   
	 //  Z3[i][j] = 0;
	 //  R3[i][j] = 0;
	   
	   
       }

    /*compute values at node control volume faces*/
    for(i=1;i<NI-1;i++)
	for(j=1;j<NJ-1;j++)
	{
	    z1e[i][j]=0.5*(Z1[i][j]+Z1[i+1][j]);
	    z1w[i][j]=0.5*(Z1[i-1][j]+Z1[i][j]);
	    z1n[i][j]=0.5*(Z1[i][j]+Z1[i][j+1]);
	    z1s[i][j]=0.5*(Z1[i][j-1]+Z1[i][j]);

	    z2e[i][j]=0.5*(Z2[i][j]+Z2[i+1][j]);
	    z2w[i][j]=0.5*(Z2[i-1][j]+Z2[i][j]);
	    z2n[i][j]=0.5*(Z2[i][j]+Z2[i][j+1]);
	    z2s[i][j]=0.5*(Z2[i][j-1]+Z2[i][j]);

	    z3e[i][j]=0.5*(Z3[i][j]+Z3[i+1][j]);
	    z3w[i][j]=0.5*(Z3[i-1][j]+Z3[i][j]);
	    z3n[i][j]=0.5*(Z3[i][j]+Z3[i][j+1]);
	    z3s[i][j]=0.5*(Z3[i][j-1]+Z3[i][j]);
	    
	    r1e[i][j]=0.5*(R1[i][j]+R1[i+1][j]);
	    r1w[i][j]=0.5*(R1[i-1][j]+R1[i][j]);
	    r1n[i][j]=0.5*(R1[i][j]+R1[i][j+1]);
	    r1s[i][j]=0.5*(R1[i][j-1]+R1[i][j]);

	    r2e[i][j]=0.5*(R2[i][j]+R2[i+1][j]);
	    r2w[i][j]=0.5*(R2[i-1][j]+R2[i][j]);
	    r2n[i][j]=0.5*(R2[i][j]+R2[i][j+1]);
	    r2s[i][j]=0.5*(R2[i][j-1]+R2[i][j]);
	    
	    r3e[i][j]=0.5*(R3[i][j]+R3[i+1][j]);
	    r3w[i][j]=0.5*(R3[i-1][j]+R3[i][j]);
	    r3n[i][j]=0.5*(R3[i][j]+R3[i][j+1]);
	    r3s[i][j]=0.5*(R3[i][j-1]+R3[i][j]);
	}

    //--------------------------------------boundary condition left and right--------------------------------------------------
    double phi_delta = anode_phi-cathode_phi; // (phi[NI-1][16]-phi[0][16])
    for(j=0;j<NJ;j++)
    {
	if (mesh.nodeType(0, j)!=NodeType.DIRICHLET)
	  //  phi[0][j]=-phi_delta/(lambda[NI-1][j_mid_right]-lambda[0][j_mid_left]) * (lambda[0][j]-lambda[0][j_mid_left]) + anode_phi;
	    phi[0][j] = anode_phi;
	
		
	
	if (mesh.nodeType(NI-1, j)!=NodeType.DIRICHLET) 
	  //  phi[NI-1][j]=-phi_delta/(lambda[NI-1][j_mid_right]-lambda[0][j_mid_left]) * (lambda[NI-1][j]-lambda[0][j_mid_left]) + anode_phi;
	    phi[NI-1][j]=10;
    }

    /*
    double lambda_max = lambda[NI-1][0];
    for (i=0;i<NI;i++)
	for (j=0;j<NJ;j++)
	{
	    if (mesh.nodeType(i, j)==NodeType.DIRICHLET) continue;
	   // if (lambda[i][j]<=anode_lambda) phi[i][j] = anode_phi;
	    if (lambda[i][j]>=cathode_lambda) phi[i][j] =  cathode_phi*(lambda_max-lambda[i][j])/(lambda_max-cathode_lambda);
	    else if (i>0) phi[i][j]=0;
	}
    */
    
   /************* MAIN LOOP *********************************/
    while(true)
    {
	
	/*save phi*/
	for (i=0;i<NI;i++)
	    for (j=0;j<NJ;j++) phi_old[i][j] = phi[i][j];
	
	/*TODO: compute electric field along boundaries*/
	/*electric field components on control volume faces*/
        for(i=0;i<NI;i++)
	    for(j=0;j<NJ;j++)
	    {
		if (i<NI-1) eze[i][j]=-(phi[i+1][j]-phi[i][j])/DZ;
		if (i>0) ezw[i][j]=-(phi[i][j]-phi[i-1][j])/DZ;
		if (i>0 && j>0 && i<NI-1 && j<NJ-1) ezn[i][j]=-0.25*(phi[i+1][j+1]+phi[i+1][j]-phi[i-1][j+1]-phi[i-1][j])/DZ;
		if (i>0 && j>0 && i<NI-1 && j<NJ-1) ezs[i][j]=-0.25*(phi[i+1][j-1]+phi[i+1][j]-phi[i-1][j-1]-phi[i-1][j])/DZ;

		if (i>0 && j>0 && i<NI-1 && j<NJ-1) ere[i][j]=-0.25*(phi[i+1][j+1]+phi[i][j+1]-phi[i+1][j-1]-phi[i][j-1])/DR;
		if (i>0 && j>0 && i<NI-1 && j<NJ-1) erw[i][j]=-0.25*(phi[i-1][j+1]+phi[i][j+1]-phi[i-1][j-1]-phi[i][j-1])/DR;
		if (j<NJ-1) ern[i][j]=-(phi[i][j+1]-phi[i][j])/DR;
		if (j>0) ers[i][j]=-(phi[i][j]-phi[i][j-1])/DR;		
	    }
	
	/*compute node-based ef*/
	for (i=0;i<NI;i++)
	    for (j=0;j<NJ;j++)
	    {
		ez[i][j] = 0.5*(eze[i][j]+ezw[i][j]);
		er[i][j] = 0.5*(ern[i][j]+ers[i][j]);
	    }
	//-------------------------------------Calculate potential ----------------------------------------------------------------
	for(i=1;i<NI-1;i++)
	    for(j=0;j<NJ;j++)
	    {   
		/*skip over dirichlet nodes and nodes outside the computational region*/
		if (/*lambda[i][j]>=cathode_lambda ||*/
		    mesh.nodeType(i, j)==NodeType.DIRICHLET) continue;
					
		double phiE,phiW,phiN,phiS;
		double aW,aC,aE;
		double aS=0,aN=0,S=0;
	
		/*get neighbor values and default to zero EF at boundaries or walls*/
		if (i<NI-1 && mesh.nodeType(i+1,j)!=NodeType.DIRICHLET) phiE=phi[i+1][j]; else phiE=phi[i-1][j];
		if (i>0 && mesh.nodeType(i-1,j)!=NodeType.DIRICHLET) phiW=phi[i-1][j]; else phiW=phi[i+1][j];
		if (j<NJ-1 && mesh.nodeType(i,j+1)!=NodeType.DIRICHLET) phiN=phi[i][j+1]; else phiN=phi[i][j-1];
		if (j>0 && mesh.nodeType(i,j-1)!=NodeType.DIRICHLET) phiS=phi[i][j-1]; else phiS=phi[i][j+1];
		
		/*my derrivation, the one in paper is missing the DR/DZ terms plus wrong sign on S*/
		
		S = ((z2e[i][j]*ere[i][j] + z3e[i][j]) - (z2w[i][j]*erw[i][j] + z3w[i][j]))*DR 
		    + ((r1n[i][j]*ezn[i][j] + r3n[i][j]) - (r1s[i][j]*ezs[i][j] + r3s[i][j]))*DZ;
		
		if (r[i][j]>0)
		{  
		    S += (R1[i][j]*ez[i][j] + R2[i][j]*er[i][j] + R3[i][j])*DZ*DR/r[i][j];
		}
		
		aS = -r2s[i][j]*DZ*DZ;
		aN = -r2n[i][j]*DZ*DZ;
		aE = -z1e[i][j]*DR*DR;
		aW = -z1w[i][j]*DR*DR;
		aC = (z1e[i][j] + z1w[i][j])*DR*DR + (r2n[i][j]+r2s[i][j])*DZ*DZ;
		
		if (aC==0 && j==0) phi[i][j] = phi[i][j+1]; //todo: hack to get neumann on symmetry
		if (aC==0) continue;
	
		phi[i][j] = -(S*DZ*DR + aS*phiS + aW*phiW + aE*phiE + aN*phiN)/aC; 
		//System.out.println(phi[i][j]);
		
		
	    }
	    
	
//---------------------------------------- loop termination condition -----------------------------------------------------------	  
	/*should check for grad(j)=0*/
	for (i=0;i<NI;i++)
	    for (j=0;j<NJ;j++)
	    {
		jz[i][j] = Z1[i][j]*ez[i][j] + Z2[i][j]*er[i][j] +Z3[i][j];
		jr[i][j] = R1[i][j]*ez[i][j] + R2[i][j]*er[i][j] +R3[i][j];
	    }

	grad_j_sum=0;
	for (i=1;i<NI-1;i++)
	    for (j=1;j<NJ-1;j++)
	    {
		if (mesh.nodeType(i, j)!=NodeType.OPEN) continue;
		grad_j[i][j] = ((jz[i+1][j]-jz[i-1][j])/(2*DZ) + (jr[i][j+1]-jr[i][j-1])/(2*DR) + jr[i][j]/r[i][j])*(DZ*DR*r[i][j]);
		//grad_j[i][j]= (jz[i+1][j]-jz[i-1][j])/(2*DZ);
		grad_j_sum+=grad_j[i][j];
	    }
	
	/*the grad_j check doesn't work right, compute delta_phi*/
	sum = 0;
	for (i=0;i<NI;i++)
	    for (j=0;j<NJ;j++)
	    {
		double delta = phi[i][j]-phi_old[i][j];
		sum+=delta*delta;
	    }
	sum/=(NI*NJ);
	//if (k%100==0)	System.out.printf("%d %.3g\n",k,grad_j_sum);
	if (k>5000 || (Math.abs(sum)<1e-4)) break;
	k++;
//	System.out.printf("error=%g\n",error);
    }
    
    System.out.printf(" FHT solver finished in %d iterations with sum(grad_j)=%.3g\n",k-1,grad_j_sum);

    /*revert temperature back to K*/
    for ( i=0;i<NI;i++)
	for (j=0;j<NJ;j++)
	{
	    te[i][j] /= Constants.KtoEV;	/*this solver assumes Te is in ev*/
	}
	
//------------------------------------------------output electron temperature------------------------------------------------------
    //------------------------------------------------output potential ------------------------------------------------------
    double phi_range[] = Starfish.domain_module.getPhi().getRange();
    System.out.printf("phi range: %.3g : %.3g\n",phi_range[0],phi_range[1]);
}

    @Override
    public void updateGradientField()
    {
	
    }

}   /*end of fluid solver*/