/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

/** \page ambient_density_source Ambient Density Source 
 * 
 * Source that generates particles along a boundary until a prescribed 
 total pressure is reached. Source supports multiple species with different 
 partial pressures.*/
package starfish.sources;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Spline;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.source.Source;
import starfish.core.source.SourceModule;

/** Source that generates particles along a boundary until a prescribed 
 total pressure is reached. Source supports multiple species with different 
 partial pressures.*/
public class AmbientDensitySource extends Source
{
    final double v_th;			/*thermal velocity*/
    final double v_drift[] = new double[3];
    final double density;
    final double temp;
    
    public AmbientDensitySource(String name, Material source_mat, Spline spline, 
	    double temp, double density, double v_drift[])
    {
	super(name, source_mat, spline, 0);
	
	this.density = density;
	
	this.temp = temp;
	this.v_drift[0] = v_drift[0];
	this.v_drift[1] = v_drift[1];
	this.v_drift[2] = v_drift[2];
	v_th = Utils.computeVth(temp, source_mat.getMass());
    }

    @Override
    public void start()
    {
	Log.debug("Initializing ambient source "+name);
	
	/*use iterative method to figure out which cells the spline passes through*/
	/*TODO: move this to Spline*/
	
	double t=0;
	Cell last_cell=null;
	do 
	{
	    double x[] = spline.pos(t);
	    Mesh mesh = Starfish.domain_module.getMesh(x);
	    if (mesh == null)
	    {
		t+=0.01;
		continue;
	    }
		
	    int ic[] = mesh.XtoI(x);
	    if (ic[0]>=mesh.ni-1) ic[0]=mesh.ni-2;
	    if (ic[1]>=mesh.nj-1) ic[1]=mesh.nj-2;
	    
	    if (last_cell==null ||
		(last_cell.i!=ic[0] || last_cell.j!=ic[1]))
	    {
		/*add cell*/
		last_cell = new Cell(ic[0],ic[1],mesh);
		cells.add(last_cell);
	    }
	    
	    /*increment*/
	    /*TODO: this should actually implement search where we go back and forth, this was a quick hack to get the code working*/
	    t+=0.01;
	    
	} while(t<spline.numSegments()-0.0001);
    }

    /*list of cells where we load particles*/
    ArrayList<Cell> cells = new ArrayList();
    protected int curr_cell;
    
    class Cell
    {
	int i, j;
	int num_to_create;
	double rem;
	Mesh mesh;
	double volume;
	Cell (int i,int j,Mesh mesh) 
	{
	    this.i=i;this.j=j;
	    this.mesh = mesh;
	    this.volume = mesh.cellVol(i, j);
	}
    }
    
    /**loop through all cells belonging to this source and 
     * compute number of particles to create*/
    @Override 
    public void regenerate()
    {
	FieldCollection2D den = source_mat.getDenCollection();
	
	/*update total pressure*/
	den.eval();
	
	/*reset number of remaining particles*/
	num_mp = 0;
	
	/*for now this only works for kinetic materials*/
	KineticMaterial ks = (KineticMaterial)source_mat;
	
	for (Cell cell:cells)
	{
	    /*compute number of particles in each cell*/
	    Field2D den_f = den.getField(cell.mesh);
	    
	    /*average pressure in cell*/
	    double den_ave = den_f.gather(cell.i+0.5,cell.j+0.5);

	    /*don't do anything if already at total pressure*/
	    if (den_ave>density) continue;

	    /*otherwise, determine how many particles to create in this cell*/
	    double den_delta = density-den_ave;
	    
	    /*now convert pressure to load to number of particles*/
	    double num_part = den_delta*cell.volume;
	    double nmp = num_part/ks.getSpwt0()+cell.rem;
	    cell.num_to_create = (int)nmp;
	    cell.rem = nmp-cell.num_to_create;
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
		
	double x[] = cell.mesh.randomPosInCell(cell.i,cell.j);
	
	/*copy values*/
	part.pos[0] = x[0];
	part.pos[1] = x[1];
	part.pos[2] = 0;

	double v_max[] = Utils.SampleMaxw3D(v_th);

	/*add drift*/
	part.vel[0] = v_max[0] + v_drift[0];
	part.vel[1] = v_max[1] + v_drift[1];
	part.vel[2] = v_max[2] + v_drift[2];
	
	cell.num_to_create--;
	num_mp--;

	return part;
    }

    @Override
    public void sampleFluid()
    {
	/*TODO: Implement!*/
	/*calculate density*/
	//double A = spline.area();
	double den0 = density;

	
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
    
    static public SourceModule.SurfaceSourceFactory ambientDensitySourceFactory = new SourceModule.SurfaceSourceFactory()
    {
	@Override
	public void makeSource(Element element, String name, Boundary boundary, Material material)
	{
	    /*drift velocity and temperature*/
	    double def[] = new double[3];
	    def[0]=def[1]=def[2]=0;
	    double v_drift[] = InputParser.getDoubleList("drift_velocity", element,def);
	    if (v_drift.length!=3)
		Log.error("Ambient Density Source Syntax: <drift_velocity>vx,vy,vz</drift_velocity>");
	    
	    double temp = InputParser.getDouble("temperature", element);

	    /*partial and total pressure*/
	    double density = InputParser.getDouble("density", element);
	    
	    AmbientDensitySource source = new AmbientDensitySource(name, material, boundary, 
						    temp,density,v_drift);
	    boundary.addSource(source);

	    /*log*/
	    Starfish.Log.log("Added AMBIENT_DENSITY source '" + name + "'");
	    Starfish.Log.log("> spline  = " + boundary.getName());
	    Starfish.Log.log("> drift velocity  = <" + v_drift[0]+","+v_drift[1]+","+v_drift[2] + "> (m/s)");
	    Starfish.Log.log("> temperature  = " + temp + " (K)");
	    Starfish.Log.log("> density  = " + density + " (Pa)");
	}
	
    };
   
}
