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

/**
 *
 * @author Lubos Brieda
 */
public class MCC extends VolumeInteraction
{
    Sigma sigma;
    MCCModel model;
    KineticMaterial source;
    Material target;
    Material product;
    int frequency;
    int stop_it;
    double max_T;

    MCC(Element element) 
    {	
		/*figure out how many other interaction pairs are defined to get our default tag id*/
		int id = Starfish.interactions_module.getInteractionsList().size();
			
		/*for backward compatibility, there is no "-1" only "-2"*/
		String tag = "";
		if (id>1) tag="-"+id;
			
		/*parse data*/
		String source_name = InputParser.getValue("source", element);
		String product_name = InputParser.getValue("product",element,source_name);
		String target_name = InputParser.getValue("target",element);
		String model_name = InputParser.getValue("model", element);
		tag = InputParser.getValue("name",element,tag);
	
		sigma = InteractionsModule.parseSigma(element);
		model = MCC.getModel(model_name);
		    
		/*number of time steps between mcc computations*/
		frequency = InputParser.getInt("frequency", element, 1);
		if (frequency<1) frequency=1;
		
		stop_it = InputParser.getInt("stop_it", element, -1);
		    
		//no limit by default
		max_T = InputParser.getDouble("max_target_temp",element, -1);
		    
		/*make sure we have a kinetic source*/
		if (!(Starfish.getMaterial(source_name) instanceof KineticMaterial))
			Log.error("MCC source material "+source_name+" must be kinetic");
			
		this.source = (KineticMaterial) Starfish.getMaterial(source_name);
		this.target = Starfish.getMaterial(target_name);
		this.product = Starfish.getMaterial(product_name);
		
		if (model instanceof ModelIonization)
		{
		    /*make sure ionization energy is specified*/
		    if (target.ionization_energy<=0)
			Log.error("<ionization_energy> needs to be specified for MCC target mat "+target_name);
		}
		
		//initialize sigma parameters as needed
		sigma.init(this.source, this.target);
		  	
		/*add fields*/
		fc_real_sum = Starfish.domain_module.getFieldManager().add("col-real-sum-"+tag, "#",null);
		fc_count_sum = Starfish.domain_module.getFieldManager().add("col-count-sum-"+tag, "#",null);
		fc_count = Starfish.domain_module.getFieldManager().add("col-count-"+tag, "#",null);
		fc_nu = Starfish.domain_module.getFieldManager().add("nu-"+tag, "#/s",null);
    }
		
    FieldCollection2D fc_count;	    	//number of collisions 
    FieldCollection2D fc_count_sum;	//sum for number of collisions 
    FieldCollection2D fc_real_sum;	//sum for number of collisions scaled by spwt
    FieldCollection2D fc_nu;		//collision frequency
    FieldCollection2D dn_target;		// change in target count for ionization
    
    static MCCModel getModel(String type)
    {
		if (type.equalsIgnoreCase("MEX") || type.equalsIgnoreCase("ELASTIC"))
		    return new ModelMEX();
		else if (type.equalsIgnoreCase("CEX"))
		    return new ModelCEX();
		else if (type.equalsIgnoreCase("IONIZATION"))
		    return new ModelIonization();
		throw new UnsupportedOperationException("Collision model "+type+" undefined");
    }

    double time_sampling_start=0;
    int num_samples = 0;
    boolean steady_state = false;
    
    @Override
    public void clearSamples() {
	fc_count_sum.clear();
	fc_real_sum.clear();	   
	num_samples = 0;
    }
    
    /**
     *
     */
    @Override
    public void perform() 
    {
    	if (stop_it>0 && Starfish.getIt()>stop_it) return;
    	
		if (Starfish.getIt()%frequency!=0) return;
			
		if (num_samples==0) 
		    time_sampling_start = Starfish.getTime();
		
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
		fc_nu.mult(1.0/((Starfish.getTime()-time_sampling_start)));
		for (Mesh mesh:Starfish.getMeshList())
		{
		    double data[][] = fc_nu.getField(mesh).data;
		    for (int i=0;i<mesh.ni;i++)
			for (int j=0;j<mesh.nj;j++)
			    data[i][j] /= mesh.nodeVol(i,j);
		}
		
		if (dn_target != null) 
			target.deleteMass();
    }

    /*performs collisions on a single mesh*/
    void perform(Mesh mesh)
    {
    	Iterator<Particle> iterator = source.getIterator(mesh);
    	Field2D target_den = target.getDen(mesh);
	
    	Field2D real_sum = fc_real_sum.getField(mesh);
    	Field2D count_sum = fc_count_sum.getField(mesh);
    	double dt = frequency*Starfish.getDt();
	
		//loop over particles
		while (iterator.hasNext())
		{
		    Particle part = iterator.next();
	
		    double den_a = target_den.gather(part.lc);
		    if (den_a<=0) continue;
	
		    /*create random target particle according to target T and stream velocity*/
		    double target_vel[] = target.sampleVelocity(mesh, part.lc);
	
		    double g_vec[] = new double[3];
		    for (int i=0;i<3;i++) g_vec[i] = target_vel[i] - part.vel[i];				
		    double g = Vector.mag3(g_vec);
	
		    /*collision probability*/
		    /*TODO: implement multiple interactions*/
		    double sig = sigma.eval(g,part.mass);
		    double P = 1-Math.exp(-sig*g*dt*den_a);
	
		    if (P<Starfish.rnd())
			    continue;		/*no collision*/
	
		    Particle virt_part = new Particle(part);
		    //virt_part.vel = target_vel;
		    virt_part.vel = target.sampleMaxwellianVelocity(mesh, part.lc, 0, max_T);
		    virt_part.mass = target.mass;
	
		    /*save pre-collision energy*/
		    double E1 = 0.5*source.mass*Vector.mag3(part.vel);
		    /*otherwise, perform collision*/
		    model.perform(part,virt_part, this, mesh);
		    
		    /*update target material energy term*/
		    double E2 = 0.5*source.mass*Vector.mag3(part.vel);
		    double vol = mesh.nodeVol(part.lc[0], part.lc[1]);
		    /*compute power density rate J/m^3/s*/
		    double dS = part.mpw*(E1-E2)/(dt*vol);	// J/s/m^3
		    target.getS(mesh).scatter(part.lc, dS);
		    
		    int i = (int) part.lc[0];
		    int j = (int) part.lc[1];
		    count_sum.add(i,j,1);	    //cell data
		    real_sum.add(i,j,part.mpw);
		}	
    }
    
    /**
     *
     */
    @Override
    public void init() 
    {
    	if (model instanceof ModelIonization)
    		dn_target = this.target.getDeltaNCollection();
    }
	
    static abstract class MCCModel 
    {
	/**returns cross-section for the given relative velocity*/
	public abstract void perform(Particle source, Particle target, MCC mcc, Mesh mesh);
	protected double c[];
	
	protected MCCModel() {}
    }
	
    static class ModelMEX extends MCCModel
    {
	@Override
	public void perform(Particle source, Particle target, MCC mcc, Mesh mesh) 
	{			
	    //elastic model from DSMC
	    double vr_cp[] = new double[3];	//post collision relative velocity
	    double vc_cm[] = new double[3];	//centre of mass velocity
	    
	    double rm1=source.mass/(source.mass+target.mass);  //reduced mass 1
	    double rm2=target.mass/(source.mass+target.mass);  //reduced mass 2
	   
	    double A,B,C;
	    
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
	public void perform(Particle part, Particle target, MCC mcc, Mesh mesh) 
	{
	    /*simply replace velocities*/
	    part.vel[0] = target.vel[0];
	    part.vel[1] = target.vel[1];
	    part.vel[2] = target.vel[2];			
	}
    }
    
     /*
    Ionization model assumes that the source is an electron and target is a neutral.
    The product specified in the input file is the ion to generate. The code will also create an additional electron.
    
    */
    static class ModelIonization extends MCCModel
    {
		@Override
		public void perform(Particle source, Particle target, MCC mcc, Mesh mesh) 
		{
		    /*reduce initial energy*/
		    double e1 = 0.5*source.mass*Vector.dot3(source.vel,source.vel)/Constants.QE;
		    double e2 = e1 - mcc.target.ionization_energy;
		    if (e2<0) return;	//sanity check, should not happen
		    
		    //randomly redistribute the remaining energy to the two electrons
		    double e2a = Starfish.rnd()*e2;
		    double e2b = e2 - e2a;
		    
		    //speed reduced by the ionization energy
		    double speed2a = Math.sqrt(e2a*Constants.QE*2/source.mass);
		    double speed2b = Math.sqrt(e2b*Constants.QE*2/source.mass);
		    	    
		    /*give the source electron isotropic direction*/
		    source.vel = Utils.isotropicVel(speed2a);
	
		    //assume the new electron and ion are created at the neutral temperature
		    double target_temp = mcc.target.getTempCollection().eval(source.pos,300);
		    
		    //sampling sometimes return negative values (if temperature not computed), 
		    //in that case, assume 100K
		    if (target_temp<=0)
			target_temp = 100;
		    	    
		    /*create new ion and electron*/
		    double vel2b[] = Utils.isotropicVel(speed2a);
		    mcc.source.getParticleListSource().addParticle(new Particle(source.pos, vel2b, source.mpw, mcc.source));
		    	    
		    if (mcc.product instanceof KineticMaterial)
		    {
				KineticMaterial prod = (KineticMaterial)mcc.product;
				int mp_gen = (int)(mcc.source.getSpwt0()/prod.getSpwt0()+Starfish.rnd());
				if (mp_gen>0) {
					Field2D dn_field = mcc.dn_target.getField(mesh);
					double v_th = Utils.computeVth(target_temp, prod.mass);
					for (int i=0;i<mp_gen;i++)
					{
						double vel3[] = Utils.SampleMaxw3D(v_th);
						prod.addParticle(source.pos, vel3);
					}
				
					dn_field.add((int)source.lc[0],(int)source.lc[1], prod.getSpwt0()*mp_gen);
				}

			    	
		    }
		}
    }
    
        /**Parses &lt;MCC&gt; element*/
    public static InteractionFactory MCCFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
	    Starfish.interactions_module.addInteraction(new MCC(element));		
        }
    };
    
   
}
