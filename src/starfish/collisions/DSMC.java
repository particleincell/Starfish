/* *****************************************************
 * (c) 2015 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.collisions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Element;
import starfish.collisions.SigmaPlus.SigmaBird463;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.interactions.InteractionsModule;
import starfish.core.interactions.InteractionsModule.InteractionFactory;
import starfish.core.interactions.Sigma;
import starfish.core.interactions.VolumeInteraction;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.common.Vector;

public class DSMC extends VolumeInteraction
{
    Sigma sigma;
    DSMCModel model;
    KineticMaterial mat1;
    KineticMaterial mat2;
    int frequency;
   // KineticMaterial product;

    double vss_inv;
    
    DSMC(String mat1, String mat2, Sigma sigma, DSMCModel model, int frequency) 
    {
	/*make sure we have a kinetic source*/
	if (!(Starfish.getMaterial(mat1) instanceof KineticMaterial) ||
	    !(Starfish.getMaterial(mat2) instanceof KineticMaterial))
	    Log.error("DSMC materials must be kinetic");

	this.mat1 = (KineticMaterial) Starfish.getMaterial(mat1);
	this.mat2 = (KineticMaterial)Starfish.getMaterial(mat2);
	//this.product = (KineticMaterial)Starfish.getMaterial(product);
	this.model = model;
	this.vss_inv=0.5*(1.0/this.mat1.vss_alpha+1.0/this.mat2.vss_alpha);

	this.sigma = sigma;
	this.frequency = frequency;	//iteration skip between collisions

	  /*not very elegent but couldn't figure out where else to call this*/
	if (sigma instanceof SigmaBird463)
	{
	    ((SigmaBird463)sigma).init(this.mat1,this.mat2);
	}
	/*TODO: these need to be per pair*/
	fc_real_sum = Starfish.domain_module.getFieldManager().add("dsmc-real-sum", "#",null);
	fc_count_sum = Starfish.domain_module.getFieldManager().add("dsmc-count-sum", "#",null);
	fc_count = Starfish.domain_module.getFieldManager().add("dsmc-count", "#",null);
	fc_nu = Starfish.domain_module.getFieldManager().add("nu", "#/s",null);
    }

    FieldCollection2D fc_count;	    	//number of collisions 
    FieldCollection2D fc_count_sum;	//sum for number of collisions 
    FieldCollection2D fc_real_sum;	//sum for number of collisions scaled by spwt
    FieldCollection2D fc_nu;		//collision frequency
  
    static DSMCModel getModel(String type)
    {
	    if (type.equalsIgnoreCase("Elastic"))
		    return new ModelElastic();
	    throw new UnsupportedOperationException("Collision cross-section "+type+" undefined");
    }


    @Override
    public void init() 
    {
	/*allocate memory*/
	for (Mesh mesh:Starfish.getMeshList())
	{
	    mesh_data.put(mesh, new MeshData(mesh,1e-16));
	}				
    }	

    class CellInfo
    {
	List<Particle> sp1_list = new ArrayList();
	List<Particle> sp2_list = new ArrayList();
	
	double sig_cr_max;	/*TODO: this needs to be per species species pair*/
	private double rem;
	
	CellInfo(double sig_cr) {
	    sig_cr_max=sig_cr;}
    }
    
    class MeshData
    {
	CellInfo cell_info[][];
	MeshData(Mesh mesh,double sig_cr)
	{
	    cell_info = new CellInfo[mesh.ni-1][mesh.nj-1];
	    for (int i=0;i<mesh.ni-1;i++)
		for (int j=0;j<mesh.nj-1;j++)
		    cell_info[i][j] = new CellInfo(sig_cr);
	}
    }
    
    HashMap<Mesh,MeshData> mesh_data = new HashMap();
    
    int num_samples = 0;
    boolean steady_state = false;
    
    @Override
    public void perform() 
    {
	if (Starfish.getIt()%frequency!=0) return;
	
	/*clear samples if we are now at steady state*/
	if (Starfish.steady_state() && !steady_state)
	{
	   steady_state = true;
	   fc_count_sum.clear();
	   fc_real_sum.clear();
	   num_samples = 0;
	}
	
	num_samples++;
	
	Log.debug("Performing DSMC for "+mat1.name+" : "+mat2.name);
	
	/*group particles to cells*/
	for (Mesh mesh:Starfish.getMeshList())
		perform(mesh);
	
	/*update collision count - number of collisions per cell per call to perform*/
	fc_count.copy(fc_count_sum);
	fc_count.mult(1.0/num_samples);
	
	/*update collision frequency*/
	fc_nu.copy(fc_real_sum);
	fc_nu.mult(1.0/(num_samples*frequency*Starfish.getDt()));	
    }

    /** performs DSMC on a mesh*/
    void perform (Mesh mesh)
    {
	CellInfo cell_info[][] = mesh_data.get(mesh).cell_info;
	
	/*cleanup*/
	for (int i=0;i<mesh.ni-1;i++)
	    for (int j=0;j<mesh.nj-1;j++)
	    {
		cell_info[i][j].sp1_list.clear();
		cell_info[i][j].sp2_list.clear();
	    }
	
	/*sort particles into cells
	 * TODO: add support for subcells per Bird*/
	/*source*/
	Iterator<Particle> src_iterator = mat1.getIterator(mesh);
	while (src_iterator.hasNext())
	{
	    Particle part = src_iterator.next();
	    int i=(int)part.lc[0];
	    int j=(int)part.lc[1];
	    if (i>=mesh.ni-1 || j>=mesh.nj-1) continue;	//boundary source can create particles on mesh edge
	    cell_info[i][j].sp1_list.add(part);
	   
	}

	/*target*/
	if (mat2!=mat1)
	{
	    Iterator<Particle> tgt_iterator = mat2.getIterator(mesh);
	    while (tgt_iterator.hasNext())
	    {
		Particle part = tgt_iterator.next();
		int i=(int)part.lc[0];
		int j=(int)part.lc[1];
		if (i>=mesh.ni-1 || j>=mesh.nj-1) continue;   //boundary source can create particles on mesh edge
		cell_info[i][j].sp2_list.add(part);
	    }
	}
	
	long nc_tot=0;

	double sigma_cr_max=0;
	    
	Field2D real_sum = fc_real_sum.getField(mesh);
	Field2D count_sum = fc_count_sum.getField(mesh);
	
	/*loop over cells*/
	for (int i=0;i<mesh.ni-1;i++)
	    for (int j=0;j<mesh.nj-1;j++)
	    {
		double cell_cols=collideCell(cell_info[i][j],mesh.cellVol(i, j),count_sum, real_sum);			
			
		nc_tot+=cell_cols;
		if (cell_info[i][j].sig_cr_max>sigma_cr_max) sigma_cr_max=cell_info[i][j].sig_cr_max;
	    }

	Log.log(String.format("DSMC %s-%s collision count: %d\t sig_cr_max: %.3g",mat1.getName(),mat2.getName(),nc_tot,sigma_cr_max));

    }

    /**performs DSMC collisions for a single cell, uses Boyd 1996 algorithm for variable weight*/
    double collideCell(CellInfo cell_info, double cell_volume, Field2D col_sum, Field2D real_sum)
    {	
	double sig_cr_max=0;	/*used to obtain new value*/	
	
	double delta_t=frequency*Starfish.getDt();

	/*we have just one list if both materials the same*/
	List<Particle>sp1_list = cell_info.sp1_list;
	List<Particle>sp2_list = cell_info.sp2_list;
	if (mat2==mat1) sp2_list = sp1_list;
	
	double np1 = sp1_list.size();
	double np2 = sp2_list.size();
	    
	double spwt1=mat1.getSpwt0();
	double spwt2=mat2.getSpwt0();
		
	/*eq. 5 in Boyd's paper*/
	double Pab = spwt2/spwt1;
	double Pba = spwt1/spwt2;
	
	if (Pab<1) Pab=1;
	if (Pba<1) Pba=1;
	
	double nsel_f = ((np1*spwt1/cell_volume)*np2*delta_t*cell_info.sig_cr_max)/(Pab + (spwt2/spwt1)*Pba); 	
	/*since not doing "q-p" collisions for "p-q", need double if different species*/
	if (mat1!=mat2) nsel_f*=2.0;
	
	/*add reminder*/
	nsel_f += cell_info.rem;
	int nsel = (int)(nsel_f);	/*number of groups, round down*/
	    
	/*make sure we have enough particles to collide*/
	if ((mat1==mat2 && (np1<2 || np2<2))||
		np1<1 || np2<1)
	    nsel=0;
	    
	cell_info.rem=nsel_f-nsel;
	
	double nc = 0;			/*number of collisions*/

	double cr_vec[] = new double[3];

	for (int i=0;i<nsel;i++)
	{
	    Particle part1,part2;
	    int p1,p2;
	    p1 = (int)(Starfish.rnd()*np1);
		    
	    do {p2 = (int)(Starfish.rnd()*np2);}
	    while (mat1==mat2 && p1 == p2);

	    part1=sp1_list.get(p1);
	    part2=sp2_list.get(p2);
		    
	    /*relative velocity*/
	    for (int j=0;j<3;j++)
		cr_vec[j] = part1.vel[j]-part2.vel[j];

	    double cr_mag = mag(cr_vec);

		/*eval cross section*/
	    double sigma_cr = cr_mag*sigma.eval(cr_mag);
		
	    if (sigma_cr>sig_cr_max)
		sig_cr_max=sigma_cr;

	    /*eval prob*/
	    double P=sigma_cr/cell_info.sig_cr_max;

	    if (Starfish.rnd()<P)
	    {
		nc++;
		
		model.perform(part1, part2,vss_inv);
		
		/*start counting only at ss since dividing by time since ss*/
		if (Starfish.steady_state())
		{
		    double lc[] = {0,0};
		    lc[0] = 0.5*(part1.lc[0]+part2.lc[0]);
		    lc[1] = 0.5*(part1.lc[1]+part2.lc[1]);
		    col_sum.scatter(lc[0],lc[1], 1);
		    real_sum.scatter(lc[0],lc[1], 0.5*(part1.spwt+part2.spwt));
		}
	    }
	}

	if (sig_cr_max>0) cell_info.sig_cr_max = sig_cr_max;
	return nc;
    }
  
    /**returns magnitude of a 3 component vector*/
    double mag(double v[])
    {
	return Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
    }

    static abstract class DSMCModel 
    {
	/**returns cross-section for the given relative velocity*/
	public abstract void perform(Particle part1, Particle part2, double vss_inv);
	protected double c[];

	protected DSMCModel () { /*pass parameters*/}
    }

    /**performs momentum transfer collision between two particles
     port of Bird's algorithm from DSMC0.for
     */
    static class ModelElastic extends DSMCModel
    {
	@Override
	public void perform(Particle part1, Particle part2, double vss_inv) 
	{	
	    double vr_cp[] = new double[3];	//post collision relative velocity
	    double vc_cm[] = new double[3];	//centre of mass velocity
	    double rm1=part1.mass/(part1.mass+part2.mass);  //reduced mass 1
	    double rm2=part2.mass/(part1.mass+part2.mass);  //reduced mass 2
	   
	    double A,B,C,D;
	    double OC,SC;
	    
	    /*compute relative velocity, could be passed in */
	    double g[] = new double[3];
	    for (int i=0;i<3;i++)
		g[i] = part1.vel[i]-part2.vel[i];
	    double g_mag=Vector.mag3(g);
	
	    /*velocity of the center of mass*/
	    for (int i=0;i<3;i++)
	    {
		vc_cm[i]=rm1*part1.vel[i]+rm2*part2.vel[i];
	    }
	
	    /*compute post collision velocity in CM coordinates*/
	    if (Math.abs(vss_inv-1.)<1.E-4) /*VHS logic if vss_inv ~ 1.0*/
	    {
		//use the VHS logic
		B=2.*Starfish.rnd()-1.0;	//B is the cosine of a random elevation angle
		A=Math.sqrt(1.-B*B);
		vr_cp[0]=B*g_mag;
		C=2.*Constants.PI*Starfish.rnd(); //C is a random azimuth angle
		vr_cp[1]=A*Math.cos(C)*g_mag;
		vr_cp[2]=A*Math.sin(C)*g_mag;		
	    }
	    else
	    {
		//use the VSS logic
		B=2.*(Math.pow(Starfish.rnd(),vss_inv))-1.;
		//B is the cosine of the deflection angle for the VSS model, eqn (11.8)
		A=Math.sqrt(1.-B*B);
		C=2.*Constants.PI*Starfish.rnd();
		OC=Math.cos(C);
		SC=Math.sin(C);
		D=Math.sqrt(g[1]*g[1]+g[2]*g[2]);
		if (D>1.E-6) {
		    vr_cp[0]=B*g[0]+A*SC*D;
		    vr_cp[1]=B*g[1]+A*(g_mag*g[2]*OC-g[0]*g[1]*SC)/D;
		    vr_cp[2]=B*g[2]-A*(g_mag*g[1]*OC+g[0]*g[2]*SC)/D;
		}
		else {				
		    vr_cp[0]=B*g[1];
		    vr_cp[1]=A*OC*g[1];
		    vr_cp[2]=A*SC*g[1];
		}
	    }

	    //the post-collision rel. velocity components are based on eqn (2.22)
	    //use Boyd's probability algorithm if variable weight
	    if (part1.spwt==part2.spwt)
	    {
		for (int i=0;i<3;i++)
		{
		    part1.vel[i]=vc_cm[i]+vr_cp[i]*rm2;
		    part2.vel[i]=vc_cm[i]-vr_cp[i]*rm1;
		}
	    }
	    else
	    {
		/*variable weight*/
		double Pab = part2.spwt/part1.spwt;
		double Pba = part1.spwt/part2.spwt;	     

		if (Starfish.rnd()<Pab)
		    for (int i=0;i<3;i++)
			part1.vel[i] = vc_cm[i]+rm2*vr_cp[i];

		if (Starfish.rnd()<Pba)
		    for (int i=0;i<3;i++)
			part2.vel[i] = vc_cm[i]-rm1*vr_cp[i];			     
	    }			
        }
    }

    /**Parses &lt;DSMC&gt; element*/
    public static InteractionFactory DSMCFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
	    String pair[] = InputParser.getList("pair", element);
	    if (pair.length!=2)
		Log.error("Must specify collision pair, pair=\"mat1,mat2\"");
	    String model_name = InputParser.getValue("model", element);
	    DSMCModel model = DSMC.getModel(model_name);

	    /*number of time steps between dsmc computations*/
	    int frequency = InputParser.getInt("frequency", element, 1);
	    if (frequency<1) frequency=1;
	    
	    Sigma sigma = InteractionsModule.parseSigma(element);
	    
	    Starfish.interactions_module.addInteraction(new DSMC(pair[0],pair[1],sigma,model,frequency));
	}
    };

}    
