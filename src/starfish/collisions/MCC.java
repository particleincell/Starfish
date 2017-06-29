/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.collisions;

import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.collisions.DSMC.ModelElastic;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.common.Vector;
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
import starfish.core.materials.Material;

public class MCC extends VolumeInteraction
{
    Sigma sigma;
    MCCModel model;
    KineticMaterial source;
    Material target;
    Material product;
    int frequency;

    MCC(String source, String target, String product, Sigma sigma, MCCModel model, int frequency) 
    {	
	/*make sure we have a kinetic source*/
	if (!(Starfish.getMaterial(source) instanceof KineticMaterial))
		Log.error("MCC source material "+source+" must be kinetic");
		
	this.source = (KineticMaterial) Starfish.getMaterial(source);
	this.target = Starfish.getMaterial(target);
	this.product = Starfish.getMaterial(product);
	this.model = model;
	this.frequency = frequency;
    	
	//initialize sigma parameters as needed
	sigma.init(this.source, this.target);
	    
	this.sigma = sigma;
	
		/*figure out how many other DSMC pairs are defined*/
	ArrayList<VolumeInteraction> vints = Starfish.interactions_module.getInteractionsList();
	int id = 1;
	for (VolumeInteraction vint : vints)
	{
	    if (vint instanceof MCC) id++;
	}
	
	/*for backward compatibility, there is no "-1" only "-2"*/
	String tag = "";
	if (id>1) tag="-"+id;
	
	/*add fields*/
	fc_real_sum = Starfish.domain_module.getFieldManager().add("mcc-real-sum"+tag, "#",null);
	fc_count_sum = Starfish.domain_module.getFieldManager().add("mcc-count-sum"+tag, "#",null);
	fc_count = Starfish.domain_module.getFieldManager().add("mcc-count"+tag, "#",null);
	fc_nu = Starfish.domain_module.getFieldManager().add("mcc-nu"+tag, "#/s",null);
    }
	
    FieldCollection2D fc_count;	    	//number of collisions 
    FieldCollection2D fc_count_sum;	//sum for number of collisions 
    FieldCollection2D fc_real_sum;	//sum for number of collisions scaled by spwt
    FieldCollection2D fc_nu;		//collision frequency
    
    static MCCModel getModel(String type)
    {
	if (type.equalsIgnoreCase("MEX"))
	    return new ModelMEX();
	else if (type.equalsIgnoreCase("CEX"))
	    return new ModelCEX();
	throw new UnsupportedOperationException("Collision model "+type+" undefined");
    }

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
	
	Log.debug("performing MCC");
	for (Mesh mesh:Starfish.getMeshList())
	{
	    perform(mesh);
	}
	
	/*update collision count - number of collisions per cell per call to perform*/
	fc_count.copy(fc_count_sum);
	fc_count.mult(1.0/num_samples);

	/*update collision frequency*/
	fc_nu.copy(fc_real_sum);
	fc_nu.mult(1.0/(num_samples*frequency*Starfish.getDt()));	
    }

    /*performs collisions on a single mesh*/
    void perform(Mesh mesh)
    {
	Iterator<Particle> iterator = source.getIterator(mesh);
	Field2D target_den = target.getDen(mesh);
	Field2D target_T = target.getT(mesh);
	
	Field2D real_sum = fc_real_sum.getField(mesh);
	Field2D count_sum = fc_count_sum.getField(mesh);
	double dt = frequency*Starfish.getDt();
	
	//loop over particles
	while (iterator.hasNext())
	{
	    Particle part = iterator.next();

	    double den_a = target_den.gather(part.lc);
	    if (den_a<=0) continue;

	    /*create random target particle*/
	    double target_vel[] = target.sampleVelocity(mesh,part.lc);
	    double T = target_T.gather(part.lc);
	    if (T<0) T=0;
	    T=0;	//test
	    double v_th = Utils.computeVth(T, target.getMass());
	    double v_max[] = Utils.SampleMaxw3D(v_th);
	    for (int i=0;i<3;i++) target_vel[i] += v_max[i];

	    double g_vec[] = new double[3];
	    for (int i=0;i<3;i++) g_vec[i] = target_vel[i] - part.vel[i];				
	    double g = Vector.mag3(g_vec);

	    /*collision probability*/
	    /*TODO: implement multiple interactions*/
	    double P = 1-Math.exp(-sigma.eval(g)*g*dt*den_a);

	    if (P<Starfish.rnd())
		    continue;		/*no collision*/

	    Particle virt_part = new Particle(part);
	    virt_part.vel = target_vel;
	    virt_part.mass = target.mass;

	    /*otherwise, perform collision*/
	    model.perform(part,virt_part);
	    
	    /*start counting only at ss since dividing by time since ss*/
	    if (Starfish.steady_state())
	    {
		int i = (int) part.lc[0];
		int j = (int) part.lc[1];
		count_sum.add(i,j,1);	    //cell data
		real_sum.add(i,j,part.spwt);
	    }
	}	
    }
    
    @Override
    public void init() 
    {
	
    }
	
    static abstract class MCCModel 
    {
	/**returns cross-section for the given relative velocity*/
	public abstract void perform(Particle source, Particle target);
	protected double c[];
		
	protected MCCModel () { /*pass parameters*/}
    }
	
    static class ModelMEX extends MCCModel
    {
	@Override
	public void perform(Particle source, Particle target) 
	{			
	    //elastic model from DSMC
	    double vr_cp[] = new double[3];	//post collision relative velocity
	    double vc_cm[] = new double[3];	//centre of mass velocity
	    
	    double rm1=source.mass/(source.mass+target.mass);  //reduced mass 1
	    double rm2=target.mass/(source.mass+target.mass);  //reduced mass 2
	   
	    double A,B,C,D;
	    double OC,SC;
	    
	    /*compute relative velocity, could be passed in */
	    double g[] = new double[3];
	    for (int i=0;i<3;i++)
		g[i] = source.vel[i]-target.vel[i];
	    double g_mag=Vector.mag3(g);
	
	    /*velocity of the center of mass*/
	    for (int i=0;i<3;i++)
	    {
		vc_cm[i]=rm1*source.vel[i]+rm2*target.vel[i];
	    }
	
	    /*compute post collision velocity in CM coordinates*/
	    //use the VHS logic
	    B=2.*Starfish.rnd()-1.0;	//B is the cosine of a random elevation angle
	    A=Math.sqrt(1.-B*B);
	    vr_cp[0]=B*g_mag;
	    C=2.*Constants.PI*Starfish.rnd(); //C is a random azimuth angle
	    vr_cp[1]=A*Math.cos(C)*g_mag;
	    vr_cp[2]=A*Math.sin(C)*g_mag;	
	    
	    //post collision velocity
	    for (int i=0;i<3;i++)
	    {
	        source.vel[i]=vc_cm[i]+vr_cp[i]*rm2;
	        target.vel[i]=vc_cm[i]-vr_cp[i]*rm1;
	    }
	    
	}
    }
	
    static class ModelCEX extends MCCModel
    {
	@Override
	public void perform(Particle part, Particle target) 
	{
	    /*simply replace velocities*/
	    part.vel[0] = target.vel[0];
	    part.vel[1] = target.vel[1];
	    part.vel[2] = target.vel[2];			
	}
    }
    
        /**Parses &lt;MCC&gt; element*/
    public static InteractionFactory MCCFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
	    String source = InputParser.getValue("source", element);
	    String product = InputParser.getValue("product",element,source);
	    String target = InputParser.getValue("target",element);
	    String model_name = InputParser.getValue("model", element);

	    Sigma sigma = InteractionsModule.parseSigma(element);
	    MCCModel model = MCC.getModel(model_name);
	    
	    /*number of time steps between dsmc computations*/
	    int frequency = InputParser.getInt("frequency", element, 1);
	    if (frequency<1) frequency=1;

	    Starfish.interactions_module.addInteraction(new MCC(source,target,product,sigma,model,frequency));		
        }
    };

}
