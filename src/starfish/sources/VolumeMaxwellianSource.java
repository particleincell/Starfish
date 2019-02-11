/* Implements a volume source that loads particles within a specified 
 * region described by a square or a circle
 * (c) 2012-2018 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.sources;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.source.SourceModule;
import starfish.core.source.VolumeSource;

/**
 *
 * @author Lubos Brieda
 */
public class VolumeMaxwellianSource extends VolumeSource
{

    protected double v_th;
    protected double vdrift[];
    protected int start_it;
    protected int end_it;
    
    //shape information
    enum Shape {RECT, CIRCLE};
    Shape shape;
    double x0[], x1[];		//for RECT
    double radius;		//for CIRCLE
    double xc[];		//shape center, used for volume calculation
    double volume;
    
    /*constructor*/

    /**
     *
     * @param name source name
     * @param source_mat source material 
     * @param element XML element containing source information
     */

    public VolumeMaxwellianSource(String name, Material source_mat, Element element)
    {
	super(name, source_mat);

	mdot0 = InputParser.getDouble("mdot",element);
	
	/*drift velocity and temperature*/
	vdrift = InputParser.getDoubleList(name, element,new double[]{0.0,0.0});
	
	double T = Double.parseDouble(InputParser.getValue("temperature", element));
	start_it = InputParser.getInt("start_it",element,0);
	end_it = InputParser.getInt("end_it",element,-1);
	
	v_th = Utils.computeVth(T, source_mat.getMass());	

	String shape_name = InputParser.getValue("shape",element);

	xc = new double[2];
	
	if (shape_name.equalsIgnoreCase("RECTANGLE") ||
	    shape_name.equalsIgnoreCase("RECT"))
	{
	    shape = Shape.RECT;
	    x0=InputParser.getDoubleList("x0",element);
	    x1=InputParser.getDoubleList("x1",element);
	    if (x0.length<2 || x1.length<2)
		Log.error("x0/x1 need to specify two values");
	    xc[0] = x0[0] + 0.5*(x1[0]-x0[0]);
	    xc[1] = x0[1] + 0.5*(x1[1]-x0[1]);
		    
	}
	else if (shape_name.equalsIgnoreCase("CIRCLE"))
	{
	    shape = Shape.CIRCLE;
	    x0=InputParser.getDoubleList("x0",element);
	    radius=InputParser.getDouble("radius",element);
	    if (x0.length<2)
		Log.error("x0 needs to specify two values");
	    
	    xc[0] = x0[0];
	    xc[1] = x0[1];
	}
	else
	    Log.error("Unrecognized shape "+shape_name);
	
	volume = getVolume();
	 /*log*/
	Starfish.Log.log("Added MAXWELLIAN_VOLUME source '" + name + "'");
    }

    protected boolean first_time = true;
    
    //checks if the specified point is inside the shape
    protected boolean inShape(double x[])
    {
	switch (shape)
	{
	    case RECT: return (x[0]>=x0[0] && x[0]<=x1[0] && x[1]>=x0[1] && x[1]<=x1[1]); 
	    case CIRCLE: 
		double dx = x[0]-x0[0];
		double dy = x[1]-x0[1];
		return (dx*dx+dy*dy)<=radius*radius;	
	    default: return false;
	}	
    }
    
    //samples random position in the shape
    protected double[] samplePos()
    {
	double pos[] = new double[2];
	switch (shape)
	{
	    case RECT: 
		pos[0] = x0[0] + Starfish.rnd()*(x1[0]-x0[0]);
		pos[1] = x0[1] + Starfish.rnd()*(x1[1]-x0[1]);
		break;
	    case CIRCLE:
		do
		{
		    //pick random position in square, check if in bounds
		   pos[0] = x0[0]-radius+Starfish.rnd()*2*radius;
		   pos[1] = x0[1]-radius+Starfish.rnd()*2*radius;
		} while (!inShape(pos));	    
	}
	
	return pos;
    }
    
    /*
    returns the shape volume
    TODO: this needs a correction for fluid sampling since we end up sampling a sugarcubed region
    */
    protected final double getVolume()
    {
	double perim = 1;

	if (Starfish.getDomainType()==DomainType.RZ)
	    perim = 2*Math.PI*xc[0];
	else if (Starfish.getDomainType()==DomainType.ZR)
	    perim = 2*Math.PI*xc[1];
		    
	switch (shape)
	{
	    case RECT: return (x1[0]-x0[0])*(x1[1]-x0[1])*perim; 
	    case CIRCLE: return Math.PI*radius*radius*perim;
	    default: return 0;
	}
	
	//need something similar to below to compute volume for fluid material sampling
	/*
	if (first_time) 
	{
	    double vol=0;
	    for (Mesh mesh:Starfish.getMeshList())
	    {
		dn.getField(mesh).setValue(den0);
		getTemp(mesh).setValue(temp0);
	
		for (int i=0;i<mesh.ni-1;i++)
		    for (int j=0;j<mesh.nj-1;j++)
			vol+=mesh.cellVol(i, j);
	    }
	*/	
    }
    
    @Override
    public void regenerate()
    {
	/*check for injection interval*/
	if (Starfish.getIt()<start_it || Starfish.getIt()>end_it)
	{
	    num_mp = 0;
	    num_rem = 0;
	    return;
	}
	
	if (source_mat instanceof KineticMaterial)
	{
	    KineticMaterial km = (KineticMaterial) source_mat;
	    double mp =  (mdot(Starfish.time_module.getTime()) * Starfish.getDt()) / (km.getMass() * km.getSpwt0()) + mp_rem;
	    num_mp = (int) mp;
	    mp_rem = mp-num_mp;
	} else
	{
	    num_mp = 0;		/*for now?*/
	    mp_rem = 0;
	}		
    }

    @Override
    public boolean hasParticles() {return num_mp>0;}
    
    @Override
    public KineticMaterial.Particle sampleParticle() 
    {
	KineticMaterial.Particle part = new KineticMaterial.Particle((KineticMaterial)source_mat);

	/*position*/
	double pos[] = samplePos();
	part.pos[0] = pos[0];
	part.pos[1] = pos[1];
	part.pos[2] = 0;

	/*velocity*/
	part.vel = Utils.SampleMaxw3D(v_th);
	
	/*add drifting velocity*/
	part.vel[0] += vdrift[0];
	part.vel[1] += vdrift[1];
	
	num_mp-=1;

		    
	return part;
    }

    @Override
    public void sampleFluid() 
    {
	throw new UnsupportedOperationException("Not yet implemented");
	/*
	for (Mesh mesh:Starfish.getMeshList())
	{
	    int ni = mesh.ni;
	    int nj = mesh.nj;
	    double den_data[][] = source_mat.getDen(mesh).getData();
	    double dn_local[][] = dn.getField(mesh).getData();
	    for (i_sample=0;i_sample<ni;i_sample++)
		for (j_sample=0;j_sample<nj;j_sample++)
		    den_data[i_sample][j_sample]+=dn_local[i_sample][j_sample];

	}
	*/
    }
    
    /**
     * 
     *
     */
    static public SourceModule.VolumeSourceFactory volumeMaxwellianSourceFactory = new SourceModule.VolumeSourceFactory()
    {
	@Override
	public void makeSource(Element element, String name, Material material)
	{
	    VolumeMaxwellianSource source = new VolumeMaxwellianSource(name, material, element);
	    Starfish.source_module.addVolumeSource(source);
	    

	   
	}
    };

}
