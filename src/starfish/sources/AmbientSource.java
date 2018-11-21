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
    double v_drift[] = new double[3];
    enum EnforceType {DENSITY, PARTIAL_PRESSURE, TOTAL_PRESSURE};
    EnforceType enforce;
    double density;
    double pressure;
    double p_fraction;
    double temperature;
    
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
	v_drift = InputParser.getDoubleList("drift_velocity", element,def);
	if (v_drift.length!=3)
	    Log.error("Ambient Source Syntax: <drift_velocity>vx,vy,vz</drift_velocity>");
	    
	//temperature
	temperature = InputParser.getDouble("temperature", element);
	    
	/*what are we enforcing?*/
	String val = InputParser.getValue("enforce", element,"TOTAL_PRESSURE").toUpperCase();
	enforce=EnforceType.TOTAL_PRESSURE;
	if (val.equals("DENSITY")) enforce=EnforceType.DENSITY;
	else if (val.equals("TOTAL_PRESSURE") || val.equals("PRESSURE")) enforce=EnforceType.TOTAL_PRESSURE;
	else if (val.equals("PARTIAL_PRESSURE")) enforce = EnforceType.PARTIAL_PRESSURE;
    
	density=0;
	p_fraction = 1.0;
	double p_total = 0;

	if (enforce==EnforceType.DENSITY)
	{
	    density = InputParser.getDouble("density", element);
	    Starfish.Log.log("> density = " + density + " (#/m^3)");
	}
	else if (enforce==EnforceType.TOTAL_PRESSURE)
	{
	    p_total = InputParser.getDouble("total_pressure", element);
	    density = p_total/(Constants.K*temperature);
	    Starfish.Log.log("> total pressure  = " + p_total + " (Pa)");
	}
	else if (enforce==EnforceType.PARTIAL_PRESSURE)
	{
	    p_total = InputParser.getDouble("total_pressure", element);
	    double p_partial = InputParser.getDouble("partial_pressure", element);
	    if (p_partial>p_total)
		Log.error("Source "+name+" partial pressure > total pressure, check inputs");
	    p_fraction = p_partial/p_total;	  
	    Starfish.Log.log("> partial pressure  = " + p_partial + " (Pa)");
	    Starfish.Log.log("> total pressure  = " + p_total + " (Pa)");
	}
	
	pressure = p_total;
	v_th = Utils.computeVth(temperature, source_mat.getMass());
	Starfish.Log.log("Added AMBIENT source '" + name + "'");
	Starfish.Log.log("> spline  = " + boundary.getName());
	Starfish.Log.log("> enforcing "+enforce.name());
	Starfish.Log.log("> drift velocity  = <" + v_drift[0]+","+v_drift[1]+","+v_drift[2] + "> (m/s)");
	Starfish.Log.log("> temperature  = " + temperature + " (K)");

    }

    /**
     *
     */
    @Override
    public void start()
    {
	Log.debug("Initializing ambient source "+name);
	
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
	    
	    if (last_cell==null ||
		(last_cell.i!=ic[0] || last_cell.j!=ic[1]))
	    {
		/*add cell*/
		last_cell = new Cell(ic[0],ic[1],mesh,boundary,t);
		cells.add(last_cell);
		Log.debug("Adding ambient cell "+ic[0]+" "+ic[1]);
	    }
	    else    /*same cell*/ 
	    {
		cells.get(cells.size()-1).t2=t;  //increment t2
	    }
	}
	Log.log(">Ambient source "+name+" number of cells = "+cells.size());
    }

    /*list of cells where we load particles*/
    ArrayList<Cell> cells = new ArrayList<Cell>();

    /**
     *
     */
    protected int curr_cell;
    
    class Cell
    {
	int i, j;
	int num_to_create;
	Mesh mesh;
	double volume;
	Spline spline;
	double t1, t2;	    //min and max coord on the spline in the cell
	
	Cell (int i,int j,Mesh mesh,Spline spline,double t) 
	{
	    this.i=i;this.j=j;
	    this.mesh = mesh;
	    this.volume = mesh.cellVol(i, j);
	    this.spline = spline;
	    t1 = t;
	    t2 = t;
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
	
	for (int c=0;c<cells.size();c++)
	{
	    double dn=0;  // nd*vol
   	    double nd_cell=0;
	    double p_partial_cell=0;
	    double p_total_cell=0;
	    
	    /*sample at midpoint, except in the last cell,
	    otherwise we sample too many particles as the density will be lowered by the "right" node
	    which now has fever particles as there is no source on it's "right" side*/
	    double t;
	    Cell cell = cells.get(c);
	    t = cell.t1+0.5*(cell.t2-cell.t1);
	    if (c==cells.size()-1) t=cell.t1;
	    
	    double lc[] = cell.mesh.XtoL(boundary.pos(t));
	       
	    /*compute partial pressure, total pressure, and average temperature*/	    	
	    for (Material mat:Starfish.getMaterialsList())
	    {
		
		double nd = mat.getDenAve(cell.mesh).gather(lc);
		double T = km.getT(cell.mesh).gather(lc);
		double p_partial = nd*Constants.K*T;
		p_total_cell += p_partial;
		
		if (mat==km)
		{
		    nd_cell = nd;
		    p_partial_cell = p_partial;		    
		}		
	    }	
	    
	    if (enforce == EnforceType.DENSITY)
	    {		
		dn = density-nd_cell;
		//if (dn>0.20*density) dn=0.1*density; //limit increase to 20%
	    }
	    else if (enforce == EnforceType.TOTAL_PRESSURE)
	    {		
		dn = (pressure-p_total_cell)/(Constants.K*temperature);
	    }
	    else if (enforce == EnforceType.PARTIAL_PRESSURE)
	    {
		dn = (p_partial_cell - p_fraction*p_total_cell)/(Constants.K*temperature);
	    }
	    else Log.error("Unknown enforce type");
	    
	    double num_load = dn*cell.volume;
	    	    
	    /*don't do anything if already at total pressure*/
	    /*TODO: destroy particles, need particles per cell list, currently done only by DSMC*/
	    if (num_load<0) {continue;}
	    
	    /*now convert pressure to load to number of particles*/
	    double nmp = num_load/km.getSpwt0();
	    
	    /*using rnd instead of "rem" because of the dynamic nature of the problem 
	    just because there not enough particles were not created at last time step doesn't 
	    mean there are now insufficient particles as they could move in from neighbor cell    */
	    cell.num_to_create = (int)(nmp+Starfish.rnd());
	    
	    Log.debug("   Creating "+cell.num_to_create+" in cell "+cell.i+":"+cell.j);
	    num_mp += cell.num_to_create;
	}
	
	/*set current cell to 0*/
	curr_cell = 0;
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
	Cell cell = cells.get(curr_cell);
	if (cell.num_to_create<=0)
	{
	    do {
		curr_cell++;
		cell=cells.get(curr_cell);
	    } while (cell.num_to_create<=0);
	}
	
	/*this returns external point*/
	double t = randomT(cell.t1, cell.t2);
	double x[] = cell.spline.pos(t);
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
	part.vel[0] += v_drift[0];
	part.vel[1] += v_drift[1];
	part.vel[2] += v_drift[2];
		
	cell.num_to_create--;
	num_mp--;

	return part;
    }

    /**
     * @param t1
     * @param t2 *  @return random parametric position taking into account radial weighing*/
    //TODO: need to test if this works and syncs up properly with stripArea
    public double randomT(double t1, double t2) 
    {
	DomainType type = Starfish.getDomainType();
	
	/*sample from uniform distribution on XY*/
	if (type==DomainModule.DomainType.XY) return t1 + (t2-t1)*Starfish.rnd();
	
	/*otherwise, need to weigh by radius*/
		
	double t=-1;
	double A;
	double Atot = boundary.stripArea(t1, t2);
	double dt = t2-t1;
	
	double rp, rm;
	
	do {
	    /*add random distance along the segment*/
	    t = t1 + Starfish.rnd()*dt;
	    
	    /*compute area of a strip centered around the sampled point*/
	    double tp = t+0.01*dt;
	    double tm = t-0.01*dt;
	    if (tp>t2) tp=t2;
	    if (tm<t1) tm=t1;
	    
	    /*r compoment of position*/
	    if (type==DomainModule.DomainType.RZ)
	    {
		rp = boundary.pos(tp)[0];
		rm = boundary.pos(tm)[0]; 
	    }
	    else if (Starfish.getDomainType()==DomainModule.DomainType.ZR)
	    {
		rp = boundary.pos(tp)[1];
		rm = boundary.pos(tm)[1]; 
	    }
	    else throw new UnsupportedOperationException("Unknown domain type in RandomT");
			 
	    /*note, A will be zero if source is parallel with r-axis*/
	    A = Math.PI*(rp*rp-rm*rm);
	  
	} while (A>0 && (A/Atot)<Starfish.rnd());
	
	return t;
    }
    
    @Override
    public void sampleFluid()
    {
	/*calculate density*/
	//double A = spline.area();
	double den0=0;
	if (enforce == EnforceType.DENSITY) den0 = density;
	else if (enforce == EnforceType.PARTIAL_PRESSURE) 
	    den0 = p_fraction*pressure/(Constants.K*temperature);
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
