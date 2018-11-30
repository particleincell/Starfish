/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.materials;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Field1D;
import starfish.core.boundaries.FieldCollection1D;
import starfish.core.boundaries.FieldManager1D;
import starfish.core.boundaries.Segment;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.Utils;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.FieldManager2D;
import starfish.core.domain.Mesh;
import starfish.core.interactions.MaterialInteraction;
import starfish.core.common.Vector;
import starfish.core.io.InputParser;
import starfish.core.source.ParticleListSource;

/**
 * Basic definition of a material
 */
public abstract class Material
{

    public int mat_index;	/*material index*/

    public double mass;		/*molecular weight in kg*/
    public double density;	    /*density in kg/m^3*/
    public double charge;	    /*charge in C*/ 
    public double work_function;    /*material work function in J*/
    public double p_vap_coeffs[];   /*vaporization pressure coefficients, log10(P) = A + B/T + C*log10(T) */
    public double q_over_m;
    public String name;		/*material name*/ 
    public boolean frozen;	//update will be skipped if true
    public double diam;		//reference diameter for DSMC
    public double ref_temp;	//reference temperature for DSMC
    public double visc_temp_index;  //DSMC viscosity index
    public double vss_alpha;	    //DSMC VSS alpha
    
    
    ParticleListSource particle_list_source;	//storage for particles to create later
    
     /**
     * Constructor
     * @param name  material name
     * @param element XML element containing material definition
     */
    public Material(String name, Element element)
    {
	this(name);
	charge = InputParser.getDouble("charge", element)*Constants.QE;
	mass = InputParser.getDouble("molwt", element)*Constants.AMU;
	frozen = InputParser.getBoolean("frozen", element, false);
	work_function = InputParser.getDouble("work_function",element, 0.0)*Constants.EVtoJ;
	double def[] = {0.0, 0.0, 0.0};
	p_vap_coeffs = InputParser.getDoubleList("p_vap_coeffs", element, def);
	
	/*try to get DSMC data*/
	ref_temp = InputParser.getDouble("ref_temp", element,275);
	visc_temp_index = InputParser.getDouble("visc_temp_index",element,0.85);
	vss_alpha = InputParser.getDouble("vss_alpha",element,1);
	diam = InputParser.getDouble("diam",element,5e-10);
    }
    
    /**
     * Constructor for materials not built from XML data
     * @param name 
     */
    public Material(String name)
    {
	this.name = name;
	mat_index = Starfish.getMaterialsList().size();
    }


    /**
     *
     * @return
     */
    public String getName()
    {
	return name;
    }

    public double getWorkFunction()
    {
	return work_function;
    }

    /**
     * @return reference to the active particle list source
     */
    public ParticleListSource getParticleListSource() {return particle_list_source;}
    
    /**
     *
     */
    protected class InitVals		/*initialization values*/
    {
	double nd, nd_back;
	double T;
	double u, v;
    };
    
    InitVals init_vals = new InitVals();

    /**
     *
     */
    protected FieldManager2D field_manager2d;	/*field data*/

    /**
     *
     */
    protected FieldManager1D field_manager1d;	/*field data*/

    /**
     *
     */
    protected FieldCollection1D flux_collection;		/*pointer to flux for quick access*/  

    /**
     *
     */
    protected FieldCollection1D flux_normal_collection;    /*pointer to flux_normal for quick access*/  

    /**
     *
     */
    protected FieldCollection1D deprate_collection;	/*pointer to deprate for quick access*/  

    /**
     *
     */
    protected FieldCollection1D depflux_collection;	/*pointer to depflux for quick access*/  
	    
    /**
     *
     * @return
     */
    public FieldManager2D getFieldManager2d()
    {
	return field_manager2d;
    }

    /**
     *
     * @return
     */
    public FieldManager1D getFieldManager1d()
    {
	return field_manager1d;
    }

    /**
     *
     * @return
     */
    public int getIndex()
    {
	return mat_index;
    }

    /**
     *
     * @return
     */
    public double getMass()
    {
	return mass;
    }

    /**
     *
     * @return
     */
    public double getDensity()
    {
	return density;
    }

    /**
     *
     */
    double mass_sum = 0;
    double momentum_sum[] = new double[3];
    double energy_sum = 0;

    /**
     *
     * @return
     */
    public double getMassSum()  {return mass_sum;}
    public double[] getMomentumSum() {return momentum_sum;}
    public double getEnergySum() {return energy_sum;}

    /**
     *
     */
    public static class InteractionList
    {
	InteractionList()
	{
	    int num_mats = Starfish.materials_module.numMats();
	    interaction_list =  new ArrayList[num_mats];
	
	    for (int source_index = 0; source_index < num_mats; source_index++)
	    {
		interaction_list[source_index] = new ArrayList<MaterialInteraction>();
	    }

	}
  
	/**
	 *
	 */
	protected ArrayList<MaterialInteraction> interaction_list[];

	/**
	 *
	 * @param source
	 * @return
	 */
	public ArrayList<MaterialInteraction> getInteractionList(int source)
	{
	    return interaction_list[source];
	}
	
	/**
	 * sets material interaction for material_interaction.source_mat_index
	 * @param material_interaction
	 */
	public void addInteraction(MaterialInteraction material_interaction)
	{
	    interaction_list[material_interaction.getSourceMatIndex()].add(material_interaction);
	}

	/**
	 * returns surface impact handler for the specified material pair
	 * @param source_index
	 * @return 
	 */
	public ArrayList<MaterialInteraction> getMaterialInteractions(int source_index)
	{
	    return interaction_list[source_index];
	}	
    }
    
    /**list of interactions that affect the source particle: diffuse, absorb, etc..*/
    public InteractionList source_interactions;

    /**list of interactions that affect the target material: sputtering, secondary electrons, etc...*/
    public InteractionList target_interactions;
    
    /**
     * performs surface interaction and returns
     *
     * @return false if source material is absorbed
     */
    boolean performSurfaceInteraction(double[] vel, int source_index, Segment segment, double t)
    {
	/*first check for sputtering or surface emission hooks*/
	ArrayList<MaterialInteraction> emission = target_interactions.getInteractionList(source_index);
	for (MaterialInteraction em:emission)
	{
	    em.callSurfaceImpactHandler(vel, segment, t);
	}
	
	/*now process source particl impact*/
	ArrayList<MaterialInteraction> list = source_interactions.getInteractionList(source_index);

	/*check for special case of not set, kill particle*/
	if (list.isEmpty()) 
	{
	    /*TODO: default to diffuse for neutrals, but this requires passing pointer to MaterialInteraction*/
	    //if (Starfish.getMaterial(source_index).charge!=0)
		return false;
	/*    else
	    {
		SurfaceImpactHandler handler = SurfaceInteraction.SurfaceEmissionDiffuse;
		return handler.perform(vel, segment, t, null);
	    }*/
	}
	
	/*otherwise, if only one handler and probability 1, use that one*/
	else if (list.size()==1 && list.get(0).getProbability()==1.0)
	    return list.get(0).callSurfaceImpactHandler(vel, segment, t);
	
	/*TODO: optimize this by precomputing this list and storing it with MatInt list*/

	/*add up probabilities*/
	double sum = 0;
	double prob[] = new double[list.size()];

	int counter = 0;
	for (MaterialInteraction m : list)
	{
	    prob[counter] = m.getProbability();
	    sum += prob[counter];
	    counter++;
	}

	/*normalize*/
	if (sum!=1.0)
	 for (int i=0;i<counter;i++) prob[i]/=sum;

	/*pick one*/
	double R = Starfish.rnd();
	sum = 0;
	for (int i = 0; i < counter; i++)
	{
	    if (R >= sum && R < sum + prob[i])
	    {
		return list.get(i).callSurfaceImpactHandler(vel, segment, t);
	    }
	    sum += prob[i];
	}

	/*kill particle by default*/
	return false;
    }

    /*accessors*/

    /**
     *
     * @return
     */

    public FieldCollection2D getDenCollection()
    {
	return field_manager2d.getFieldCollection("nd");
    }

    /**
     *
     * @return
     */
    public FieldCollection2D getDenAveCollection()
    {
	return field_manager2d.getFieldCollection("nd-ave");
    }

    /**
     *
     * @return
     */
    public FieldCollection2D getUCollection()
    {
	return field_manager2d.getFieldCollection("u");
    }

    /**
     *
     * @return
     */
    public FieldCollection2D getVCollection()
    {
	return field_manager2d.getFieldCollection("v");
    }
    
    /**
     *
     * @return
     */
    public FieldCollection2D getWCollection()
    {
	return field_manager2d.getFieldCollection("w");
    }
    
    /**
     *
     * @return
     */
    public FieldCollection2D getUAveCollection()
    {
	return field_manager2d.getFieldCollection("u-ave");
    }

    /**
     *
     * @return
     */
    public FieldCollection2D getVAveCollection()
    {
	return field_manager2d.getFieldCollection("v-ave");
    }
    
    /**
     *
     * @return
     */
    public FieldCollection2D getWAveCollection()
    {
	return field_manager2d.getFieldCollection("w-ave");
    }

    /**
     *
     * @return
     */
    public FieldCollection2D getTempCollection()
    {
	return field_manager2d.getFieldCollection("T");
    }
    
    /**
     *
     * @return
     */
    public FieldCollection2D getPressureCollection()
    {
	return field_manager2d.getFieldCollection("p");
    }
    
    /**
     * 
     * @return filed collection storing number density delta "dn" for chemical reactions
     */
    public FieldCollection2D getDeltaNCollection()
    {
	return field_manager2d.getFieldCollection("delta_n");
    }

    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getDen(Mesh mesh)
    {
	return getDenCollection().getField(mesh);
    }

    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getDenAve(Mesh mesh)
    {
	return getDenAveCollection().getField(mesh);
    }

    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getU(Mesh mesh)
    {
	return getUCollection().getField(mesh);
    }

    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getV(Mesh mesh)
    {
	return getVCollection().getField(mesh);
    }
    
    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getW(Mesh mesh)
    {
	return getWCollection().getField(mesh);
    }
    
    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getUAve(Mesh mesh)
    {
	return getUAveCollection().getField(mesh);
    }

    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getVAve(Mesh mesh)
    {
	return getVAveCollection().getField(mesh);
    }
    
    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getWAve(Mesh mesh)
    {
	return getWAveCollection().getField(mesh);
    }

    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getT(Mesh mesh)
    {
	return getTempCollection().getField(mesh);
    }
    
    /**
     *
     * @param mesh
     * @return
     */
    public Field2D getP(Mesh mesh)
    {
	return getPressureCollection().getField(mesh);
    }

    /**
     * 
     * @param mesh mesh for the data
     * @return chemical reaction "delta n"
     */
    public Field2D getDeltaN(Mesh mesh)
    {
	return getDeltaNCollection().getField(mesh);
    }
    
    /**
     *
     * @return
     */
    public double getCharge()
    {
	return charge;
    }

    /**
     *
     * @param init_strings
     */
    public void setInitValues(String init_strings[])
    {
	for (String string : init_strings)
	{
	    String pieces[] = string.split("\\s*=\\s*");
	    if (pieces.length != 2)
	    {
		Log.error(String.format("couldn't parse init string %s, syntax var=value", string));
	    }

	    double value = Double.parseDouble(pieces[1]);
	    if (pieces[0].equalsIgnoreCase("nd"))
	    {
		init_vals.nd = value;
	    } else if (pieces[0].equalsIgnoreCase("nd_back"))
	    {
		init_vals.nd_back = value;
	    } else if (pieces[0].equalsIgnoreCase("u"))
	    {
		init_vals.u = value;
	    } else if (pieces[0].equalsIgnoreCase("v"))
	    {
		init_vals.v = value;
	    } else if (pieces[0].equalsIgnoreCase("T"))
	    {
		init_vals.T = value;
	    } else
	    {
		Log.warning("Unrecognized init variable " + pieces[0] + ", valid options are nd,nd_back,u,v,T");
	    }
	}
    }

    /**
     * updates density and/or other fields
     */
    public void update()
    {
	/*first update density*/
	updateFields();

	/*add background density*/
	if (init_vals.nd_back != 0)
	{
	    for (Mesh mesh : Starfish.getMeshList())
	    {
		Field2D den = getDen(mesh);
		den.add(init_vals.nd_back);
	    }
	}
    }

    /**
     * must be overriden by non-static materials
     */
    public abstract void updateFields();
    
    /**
     * hook to clear collected data
     */
    public void clearSamples()
    {
	/*do nothing by default*/
    }

    /**
     *
     */
    public void updateBoundaries()
    {
	/*apply boundaries*/
	for (FieldCollection2D fc : field_manager2d.getFieldCollections())
	{
	    fc.syncMeshBoundaries();
	}
    }
    
    /*saves data to file, should be reimplemented by derived classes*/

    /**
     *
     * @param out
     * @throws IOException
     */

    public void saveRestartData(DataOutputStream out)throws IOException {Log.warning("saveRestartData not yet implemented for "+name);}

    /*saves data to file, should be reimplemented by derived classes*/

    /**
     *
     * @param in
     * @throws IOException
     */

    public void loadRestartData(DataInputStream in)throws IOException {Log.warning("loadRestartData not yet implemented for "+name);}

    /**
     * initializes material interactions
     */
    void initInteractions()
    {
	/*set default interactions*/

	if (source_interactions == null)
	{
	    int num_mats = Starfish.materials_module.numMats();
	    source_interactions = new InteractionList();
	    target_interactions = new InteractionList();

	    for (int source_index = 0; source_index < num_mats; source_index++)
	    {
		
		/*default, particles get absorbed*/
	/*	MaterialInteraction def_interaction = new MaterialInteraction();
		def_interaction.setSourceMatIndex(source_index);
		def_interaction.setTargetMatIndex(mat_index);
		addMaterialInteraction(def_interaction);
	*/
	    }
	}
    }

    /**
     * sets background density
     */
    public void init()
    {
	/*compute additional data*/
	this.q_over_m = this.charge / this.mass;
	
	/*create a new field manager*/
	field_manager2d = new FieldManager2D(Starfish.getMeshList());
	field_manager1d = new FieldManager1D(Starfish.getBoundaryList());

	/*add default instantenuous 2d fields*/
	field_manager2d.add("nd", "#/m^3", init_vals.nd,null);
	field_manager2d.add("u", "m/s", init_vals.u,null);
	field_manager2d.add("v", "m/s", init_vals.v,null);
	field_manager2d.add("w", "m/s", init_vals.v,null);
	
	/*fields capturing changes in mass (and eventually momentum and energy) from chem. reactions*/
	field_manager2d.add("delta_n","#/m^3/s",0,null);
	
	/*average data*/
	field_manager2d.add("u-ave","m/s",null);
	field_manager2d.add("v-ave","m/s",null);
	field_manager2d.add("w-ave","m/s",null);
	field_manager2d.add("nd-ave","#/m^3",null);

	/*these are computed using averages*/
	field_manager2d.add("t", "K", init_vals.T,null);
	field_manager2d.add("p", "Pa", 0,null);
	
	/*add default 1d fields*/
	flux_collection = field_manager1d.add("flux", "#/m^2/s");
	flux_normal_collection = field_manager1d.add("flux-normal", "#/m^2/s");
	deprate_collection = field_manager1d.add("deprate", "kg/s");
	depflux_collection = field_manager1d.add("depflux", "kg/m^2/s");
	
	for (Mesh mesh : Starfish.getMeshList())
	{
	    Field2D den = getDen(mesh);
	    den.add(init_vals.nd_back);
	}
	
	/*generate particle list source*/
	particle_list_source = new ParticleListSource(this);
	Starfish.source_module.addVolumeSource(particle_list_source);
    }

    void addSurfaceMomentum(Boundary boundary, double spline_t, double vel[], double spwt)
    {
	if (!Starfish.time_module.steady_state)
	{
	    return;
	}

	Field1D flux_field = flux_collection.getField(boundary);

	if (spline_t >= 0 && spline_t < flux_field.getNi())
	{
	    flux_field.scatter(spline_t, spwt);
	} else
	{
	    return;
	}

	/*normal vector*/
	double n[] = boundary.normal(spline_t);
	double dot = Vector.dot(n, vel)/Vector.mag3(vel);	
	flux_normal_collection.getField(boundary).scatter(spline_t, dot * spwt);
    }

    void addSurfaceMassDeposit(Boundary boundary, double spline_t, double spwt)
    {
	if (!Starfish.time_module.steady_state)
	{
	    return;
	}
		
	Field1D dep = deprate_collection.getField(boundary);
	dep.scatter(spline_t, spwt);
    }

    void finish()
    {
	Field1D flux_normal[] = flux_normal_collection.getFields();
	Field1D flux[] = flux_collection.getFields();
	Field1D deprate[] = deprate_collection.getFields();
	Field1D depflux[] = depflux_collection.getFields();
	
	double f=1;
	if (Starfish.time_module.getSteadyStateTime()>0) f = 1.0 / (Starfish.time_module.getSteadyStateTime());

	for (int j = 0; j < flux.length; j++)
	{
	    flux_normal[j].mult(f);
	    flux[j].mult(f);
	    deprate[j].mult(f*mass);	/*convert to kg/s*/
	    
	    /*divide by area, not deprate is kg/s and hence not included here*/
	    flux_normal[j].divideByArea();
	    flux[j].divideByArea();
	    
	    /*set depflux to deprate divided by area*/
	    depflux[j].copy(deprate[j]);
	    depflux[j].divideByArea();	    /*flux is deposition rate (kg/s) divided by area*/
	    
	    /*log total deposited flux*/
	    double sum=0;
	    for (int i=0;i<depflux[j].getNi();i++)
		sum+=depflux[j].at(i)*depflux[j].getBoundary().nodeArea(i);
	    
	    if (sum!=0)
		Log.log(String.format("Total Dep Rate (kg/s), %s on %s:%g", name,depflux[j].getBoundary().getName(),sum));
	}
	
    }

    /*returns average stream velocity at specified point*/

    /**
     *
     * @param mesh
     * @param lc
     * @return
     */

    public double[] sampleVelocity(Mesh mesh, double[] lc)
    {
	double vel[] = new double[3];
	vel[0] = getUAve(mesh).gather(lc);
	vel[1] = getVAve(mesh).gather(lc);
	vel[2] = getWAve(mesh).gather(lc);
	
	return vel;	
    }

    /*returns random velocity sampled based on temperature and average stream velocity*/

    /**
     *
     * @param mesh
     * @param lc
     * @param T_min minimum temperature 
     * @param T_max maximum temperature or -1 if no limit
     * @return
     */

    public double[] sampleMaxwellianVelocity(Mesh mesh, double[] lc, double T_min, double T_max)
    {
	//stream velocity
	double vel[] = sampleVelocity(mesh, lc);
	
	//add thermal component
	double T = getT(mesh).gather(lc);
	if (T<T_min) T=T_min;
	if (T<0) T=0;	    //can't have negative temperature	
	if (T_max>0 && T>T_max) T=T_max;
	
	double v_th = Utils.computeVth(T, getMass());
	double v_max[] = Utils.SampleMaxw3D(v_th);
	for (int i=0;i<3;i++) vel[i] += v_max[i];

	return vel;
    }
}
