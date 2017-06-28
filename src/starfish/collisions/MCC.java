/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.collisions;

import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.common.Vector;
import starfish.core.domain.Field2D;
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

    MCC(String source, String target, String product, Sigma sigma, MCCModel model) 
    {	
	/*make sure we have a kinetic source*/
	if (!(Starfish.getMaterial(source) instanceof KineticMaterial))
		Log.error("MCC source material "+source+" must be kinetic");
		
	this.source = (KineticMaterial) Starfish.getMaterial(source);
	this.target = Starfish.getMaterial(target);
	this.product = Starfish.getMaterial(product);
	this.model = model;
    	
	//initialize sigma parameters as needed
	sigma.init(this.source, this.target);
	    
	this.sigma = sigma;
    }
	
    static MCCModel getModel(String type)
    {
	if (type.equalsIgnoreCase("MEX"))
	    return new ModelMEX();
	else if (type.equalsIgnoreCase("CEX"))
	    return new ModelCEX();
	throw new UnsupportedOperationException("Collision model "+type+" undefined");
    }

    @Override
    public void perform() 
    {
	Log.debug("performing MCC");
	for (Mesh mesh:Starfish.getMeshList())
	{
	    Iterator<Particle> iterator = source.getIterator(mesh);
	    Field2D target_den = target.getDen(mesh);
	    Field2D target_T = target.getT(mesh);
    	
	    while (iterator.hasNext())
	    {
		Particle part = iterator.next();
			
		double den_a = target_den.gather(part.lc);
		if (den_a<=0) continue;
				
		/*create random target particle*/
		double target_vel[] = target.sampleVelocity(mesh,part.lc);
		double T = target_T.gather(part.lc);
		if (T<0) T=0;
		double v_th = Utils.computeVth(T, target.getMass());
		double v_max[] = Utils.SampleMaxw3D(v_th);
		for (int i=0;i<3;i++) target_vel[i] += v_max[i];
		
		double g_vec[] = new double[3];
		for (int i=0;i<3;i++) g_vec[i] = target_vel[i] - part.vel[i];				
		double g = Vector.mag3(g_vec);

		/*collision probability*/
		/*TODO: implement multiple interactions*/
		double P = 1-Math.exp(-sigma.eval(g)*g*Starfish.getDt()*den_a);
				
		if (P<Starfish.rnd())
			continue;		/*no collision*/
			
		/*otherwise, perform scatter collision*/
		model.perform(part,target_vel);
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
	public abstract void perform(Particle source, double target_vel[]);
	protected double c[];
		
	protected MCCModel () { /*pass parameters*/}
    }
	
    static class ModelMEX extends MCCModel
    {
	@Override
	public void perform(Particle source, double target_vel[]) 
	{				
	    /*angles*/
	    double theta = Math.acos(2*Starfish.rnd()-1);
	    double phi = 2*Math.PI*Starfish.rnd();
		
	    double vel_mag=Vector.mag3(source.vel);
			
	    /*perform rotation*/
	    source.vel[0] = vel_mag*Math.cos(theta);
	    source.vel[1] = vel_mag*Math.sin(theta)*Math.cos(phi);
	    source.vel[2] = vel_mag*Math.sin(theta)*Math.sin(phi);
	}
    }
	
    static class ModelCEX extends MCCModel
    {
	@Override
	public void perform(Particle part, double target_vel[]) 
	{
	    /*simply replace velocities*/
	    part.vel[0] = target_vel[0];
	    part.vel[1] = target_vel[1];
	    part.vel[2] = target_vel[2];			
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

	    Starfish.interactions_module.addInteraction(new MCC(source,target,product,sigma,model));		
        }
    };

}
