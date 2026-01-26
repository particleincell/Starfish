/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.sources;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Spline;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.domain.DomainModule;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.source.Source;
import starfish.core.source.SourceModule;

/** Source that generates particles along a boundary until a prescribed 
 * total pressure, partial pressure, or number density is reached. 
 */
public class AmbientSource extends Source
{
    double v_th;			/*thermal velocity*/
    double v_norm;           // normal velocity
    enum EnforceType {DENSITY, PARTIAL_PRESSURE, TOTAL_PRESSURE};
    EnforceType enforce;
    double value;
    double temperature;
    double k_P = 1;         // proportional controller coefficient
    
    protected double source_area = 0;
    protected double source_volume = 0;
    
    /**
     *
     * @param name
     * @param source_mat
     * @param boundary
     * @param element
     */
    public AmbientSource(String name, Material source_mat, Boundary boundary, Element element)
    {
		super(name, source_mat, boundary, element);
		
		/*drift velocity and temperature*/
		double def[] = new double[3];
		def[0]=def[1]=def[2]=0;
		v_norm = InputParser.getDouble("v_drift", element, 0);
		    
		//temperature
		temperature = InputParser.getDouble("temperature", element);
		
		/* calculate density */
		//double v_mean = Math.sqrt(8*Constants.K*temperature/(Constants.PI*source_mat.mass));
		//v_th = v_mean/4;  // use this to match to Langmuir flux
		v_th = Utils.computeVth(temperature, source_mat.getMass());
		
		/*what are we enforcing?*/
		String val = InputParser.getValue("enforce", element,"TOTAL_PRESSURE").toUpperCase();
		enforce=EnforceType.TOTAL_PRESSURE;
		if (val.equals("DENSITY")) enforce=EnforceType.DENSITY;
		else if (val.equals("TOTAL_PRESSURE") || val.equals("PRESSURE")) enforce=EnforceType.TOTAL_PRESSURE;
		else if (val.equals("PARTIAL_PRESSURE")) enforce = EnforceType.PARTIAL_PRESSURE;
    
		// magnitude of pressure /density to enforce
		value = InputParser.getDouble("value", element);
	  
		if (enforce==EnforceType.DENSITY)
		{
		    Starfish.Log.log("> density = " + value + " (#/m^3)");
		}
		else if (enforce==EnforceType.TOTAL_PRESSURE)
		{
		    Starfish.Log.log("> total pressure  = " + value + " (Pa)");
		}
		else if (enforce==EnforceType.PARTIAL_PRESSURE)
		{
		    Starfish.Log.log("> partial pressure  = " + value + " (Pa)");
		}
	
		k_P = InputParser.getDouble("K_P",element,k_P);
		
		Starfish.Log.log("Added AMBIENT source '" + name + "'");
		Starfish.Log.log("> spline  = " + boundary.getName());
		Starfish.Log.log("> enforcing "+enforce.name());
		Starfish.Log.log("> soure temperature  = " + temperature + " (K)");
		Starfish.Log.log("> source velocity  = " + v_norm + v_th + " (m/s)");
		Starfish.Log.log("> k_P coeficient = " + k_P);
    }

    /**
     *
     */
    @Override
    public void start()
    {
    	Log.debug("Initializing ambient source "+name);
	
    	source_area = boundary.area();
    	
    	/*use iterative method to figure out which cells the spline passes through*/
    	/*TODO: move this to Spline*/
	
    	//need some non-zero value to prevent injecting at corners which then causes particles inside solids
    	final double t_tol = 1e-6;  
	
    	double dt = 1e-3;
    	/*don't start at t=0 to avoid getting outside cell if starting on cell boundary*/
    	Cell last_cell=null;
	
    	/*excluding max to avoid setting neighbor cells*/
    	for (double t=t_tol;t<boundary.numSegments()-t_tol;t+=dt)
    	{   
    		double x[] = boundary.pos(t);
    		double normal[] = boundary.normal(t);
	    
    		Mesh mesh = Starfish.domain_module.getMesh(x);
    		if (mesh == null)
    			continue;
	    	
    		double lc[] = mesh.XtoL(x);
    		if (lc[0]>=mesh.ni-1) lc[0]=mesh.ni-1;
    		if (lc[1]>=mesh.nj-1) lc[1]=mesh.nj-1;
	    
    		int ic[] = {(int)lc[0], (int)(lc[1])};
    		double di[] = {lc[0]-ic[0], lc[1]-ic[1]};
	    
    		/*if spline is flushed with cell's left or bottom edge, but flow is left or down,
	    	shift cell down, to attach to cell's right or top edge    */
    		if (di[0]<1e-10 && normal[0]<-0.95) ic[0]--;
    		if (di[1]<1e-10 && normal[1]<-0.95) ic[1]--;
	    	    	
    		if (ic[0]<0 || ic[1]<0) continue;
	    
    		if (last_cell==null || (last_cell.i!=ic[0] || last_cell.j!=ic[1]))
    		{
    			/*add cell*/
    			last_cell = new Cell(ic[0],ic[1],mesh,boundary);
    			cells.add(last_cell);
    			source_volume += last_cell.volume;
    			
    			Log.debug("Adding ambient cell "+ic[0]+" "+ic[1]);
    		}

    	}
    	Log.log(">Ambient source "+name+" number of cells = "+cells.size());
    }

    /*list of cells where we load particles*/
    ArrayList<Cell> cells = new ArrayList<Cell>();

    /**
     *
     */
    
    class Cell
    {
		int i, j;
		Mesh mesh;
		double volume;
		double lc[];
		
		Cell (int i,int j,Mesh mesh,Spline spline) 
		{
		    this.i=i;this.j=j;
		    this.mesh = mesh;
		    this.volume = mesh.cellVol(i, j);
		    lc = new double[] {i+0.5, j+0.5};
		}
    }
    
    /**loop through all cells belonging to this source and 
     * compute number of particles to create*/
    @Override 
    public void regenerate()
    {	
		/*reset number of remaining particles*/
		num_mp = 0;
		
		/*for now this only works for kinetic materials*/
		KineticMaterial km = (KineticMaterial)source_mat;
	
		double denV_sum = 0;
		double TV_sum = 0;
		double pV_total_sum = 0;
		double pV_partial_sum = 0;
		double V_sum = 0;
		
		for (int c=0;c<cells.size();c++)
		{
		    /*sample at midpoint, except in the last cell,
		    otherwise we sample too many particles as the density will be lowered by the "right" node
		    which now has fever particles as there is no source on it's "right" side*/
		    double t;
		    Cell cell = cells.get(c);
		    
		    double lc[] = cell.lc;
		       
		    /*compute partial pressure, total pressure, and average temperature*/	    	
		    for (Material mat:Starfish.getMaterialsList())
		    {
		    	double den_cell = mat.getDenAve(cell.mesh).gather(lc);
				double T_cell = mat.getT(cell.mesh).gather(lc);
				double p_cell = den_cell*Constants.K*T_cell;
				
				denV_sum += den_cell*cell.volume;
				TV_sum += T_cell*cell.volume;
				pV_total_sum += p_cell*cell.volume;
				V_sum += cell.volume;
				
				if (mat==source_mat)
				{
				    pV_partial_sum += p_cell*cell.volume;		    
				}		
		    }
		}
		
		// compute average
		double den_ave = denV_sum/V_sum;
		double T_ave = TV_sum/V_sum;
		double pressure_total_ave = pV_total_sum/V_sum;
		double pressure_partial_ave = pV_partial_sum/V_sum;

		double dn = 0;
	    if (enforce == EnforceType.DENSITY)
	    	dn = value - den_ave;
	    else if (enforce == EnforceType.TOTAL_PRESSURE)  //  n=p/kT
	    	dn = (value - pressure_total_ave)/(Constants.K*T_ave);
        else if (enforce == EnforceType.PARTIAL_PRESSURE)
        	dn = (value - pressure_partial_ave)/(Constants.K*T_ave);
        else Log.error("Unknown enforce type");
	    
	    double num_load = k_P*dn*source_area*(v_norm+v_th)*Starfish.getDt();
//	    double num_load = k_P*dn*source_volume;
		    	    
	    /*don't do anything if already at total pressure*/
	    /*TODO: destroy particles, need particles per cell list, currently done only by DSMC*/
	    if (num_load<0) {num_mp = 0; return;}
	    
	    /*now convert pressure to load to number of particles*/
	    double nmp_f = num_load/km.getSpwt0();
	    
	    /*using rnd instead of "rem" because of the dynamic nature of the problem 
	    just because there not enough particles were not created at last time step doesn't 
	    mean there are now insufficient particles as they could move in from neighbor cell    */
	    num_mp = (int)(nmp_f+Starfish.rnd());
	    
	    Log.debug("   Creating "+num_mp+" particles");
	   
		/*set current cell to 0*/
    }
     
    @Override
    public boolean hasParticles()
    {
    	if (num_mp>0) 
    		return true;
	
    	return false;
    }
    
    @Override
    public Particle sampleParticle()
    {
    	Particle part = new Particle((KineticMaterial) source_mat);
    	
		/*get a random point on the boundary spline*/
		double t = boundary.randomT();
		double x[] = boundary.pos(t);
		double norm[] = boundary.normal(t);
		double tang[] = boundary.tangent(t);
		
		/*copy values*/
		part.pos[0] = x[0];
		part.pos[1] = x[1];
		part.pos[2] = 0;
	
		/*sample half maxwellian in norm direction*/
		part.vel = Utils.diffuseReflVel(v_th,norm, tang);
		
		/*old way, doesn't generate correct temperature*/
		//part.vel = Utils.isotropicVel(Utils.SampleMaxwSpeed(v_th));
		
		/*add drift*/
		part.vel[0] += v_norm*norm[0];
		part.vel[1] += v_norm*norm[1];
		part.vel[2] += v_norm*norm[2];
			
		num_mp--;
	
		return part;
    }
	
    @Override
    public void sampleFluid()
    {
		/*calculate density*/
		//double A = spline.area();
		double den0=0;
		if (enforce == EnforceType.DENSITY) den0 = value;
		else if (enforce == EnforceType.PARTIAL_PRESSURE)  // TODO: fix 
		    den0 = value/(Constants.K*temperature);
		else if (enforce == EnforceType.TOTAL_PRESSURE)
		{
		    Log.error("TOTAL_PRESSURE not yet defined for fluid materials");
		}
		
		//for (Mesh mesh:Starfish.getMeshList())
		Mesh mesh = Starfish.getMeshList().get(0);
	
		double den[][] = source_mat.getDen(mesh).getData();
		double U[][] = source_mat.getU(mesh).getData();
		double V[][] = source_mat.getV(mesh).getData();
	
		int i = 0;
		for (int j = 5; j < 9; j++)
		{
		    den[i][j] = den0;
		    U[i][j] = 0;
		    V[i][j] = 0;
		}
    }
    
    /**
     *
     */
    static public SourceModule.SurfaceSourceFactory ambientSourceFactory = new SourceModule.SurfaceSourceFactory()
    {
		@Override
		public void makeSource(Element element, String name, Boundary boundary, Material material)
		{
		    /*create new source*/
		    AmbientSource source = new AmbientSource(name, material, boundary, element);    
		    boundary.addSource(source);
		}	
    };
}
