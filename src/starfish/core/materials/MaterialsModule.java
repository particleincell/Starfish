/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.materials;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import starfish.core.boundaries.FieldCollection1D;
import starfish.core.common.CommandModule;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.FieldCollection2D;
import starfish.core.io.InputParser;

/** definition of materials*/
public class MaterialsModule extends CommandModule
{

    /**
     *
     */
    protected ArrayList<Material> materials_list = new ArrayList<Material>();

    /**
     *
     * @return
     */
    public ArrayList<Material> getMaterialsList() {return materials_list;}
	
    /**adds a material to the list
     * @param material*/
    protected void AddMaterial(Material material) 
    {
	materials_list.add(material);
    }

    /**returns material of the given nam
     * @param name
     * @return e*/
    public Material getMaterial(String name) throws NoSuchElementException
    {
	for (Material mat:materials_list)
	    if (name.equalsIgnoreCase(mat.name))
		return mat;
		
	throw new NoSuchElementException("could not find material "+name);
    }
	
    /**
     *
     * @param mat_index
     * @return
     */
    public Material getMaterial(int mat_index) {return materials_list.get(mat_index);}

    /**
     * @param mat *  @return true if material mat is defined
     * @return */
    public boolean hasMaterial(String mat) 
    {
	try {return getMaterial(mat)!=null;}
	catch (NoSuchElementException e)
	{
	    return false;
	}
    }
	
    @Override
    public void init()
    {	
	/*register material types*/
	registerMaterialType("SOLID",SolidMaterial.SolidMaterialParser);
	registerMaterialType("KINETIC",KineticMaterial.KineticMaterialParser);
	registerMaterialType("FLUID_DIFFUSION",FluidDiffusionMaterial.FluidDiffusionMaterialParser);
	registerMaterialType("BOLTZMANN_ELECTRONS",BoltzmannElectronsMaterial.BoltzmannElectronsMaterialParser);
	

    }

    /** registers a new material type parser
     * @param type_name
     * @param parser*/
    static public void registerMaterialType(String type_name, MaterialParser parser)
    {
	parser_list.put(type_name.toUpperCase(), parser);
	Log.log("Added material type "+type_name.toUpperCase());
    }
    
    static HashMap<String,MaterialParser> parser_list = new HashMap<String,MaterialParser>();

    /*TODO: comput on demand but also save so don't have to compute multiple times per time step*/
    /** @return Total neutral density */
     
    public FieldCollection2D getNeutralDensity()
    {
	FieldCollection2D den = new FieldCollection2D(Starfish.getMeshList(),null);
	
	for (Material mat:materials_list)
	{
	    if (mat.charge==0)
		den.addData(mat.getDenCollection());	
	}
	return den;
    }
    
    /*TODO: comput on demand but also save so don't have to compute multiple times per time step*/
    /** @return Total neutral density
     */
    public FieldCollection2D getIonDensity()
    {
	FieldCollection2D den = new FieldCollection2D(Starfish.getMeshList(),null);
	boolean set = false;
	
	for (Material mat:materials_list)
	{
	    if (mat.charge>0)
		{den.addData(mat.getDenCollection());set=true;}
	}
	
	/*add small floor if no ions*/
	if (!set) den.setValue(1e4);
	
	return den;
    }
    
    /**
     *
     * @return ion current density field collection i-component, ji=q*n*u
     */
    public FieldCollection2D getIonJi()
    {
	FieldCollection2D ji = new FieldCollection2D(Starfish.getMeshList(),null);
	
	for (Material mat: materials_list)
	{
	    if (mat.charge>0)
		ji.addData(FieldCollection2D.mult(mat.getUCollection(),mat.getDenCollection(),Constants.QE));
	}
	
	return ji;
    }

    /**
     *
     * @return ion current density field collection i-component, jj=q*n*v
     */
    public FieldCollection2D getIonJj()
    {
	FieldCollection2D jj = new FieldCollection2D(Starfish.getMeshList(),null);
	
	for (Material mat: materials_list)
	{
	    if (mat.charge>0)
		jj.addData(FieldCollection2D.mult(mat.getVCollection(),mat.getDenCollection(),Constants.QE));
	}
	
	return jj;
    }
    
    /**
     *
     */
    public static interface MaterialParser {

	/**
	 *
	 * @param name
	 * @param element
	 * @return
	 */
	public Material addMaterial(String name, Element element);
    }
    
    @Override
    public void process(Element element) 
    {	
	/*process mesh commands*/
	Iterator<Element> iterator = InputParser.iterator(element);
		
	while(iterator.hasNext())
	{
	    Element el = iterator.next();
	    if (el.getNodeName().equalsIgnoreCase("MATERIAL"))
		NewMaterial(el);
	    else
		Log.warning("Unknown materials element "+el.getNodeName());
	}

	/*init all material handlers*/
	for (Material mat:materials_list)
	    mat.initInteractions();	
    }
	
    /**
     *
     * @param element
     */
    public void NewMaterial(Element element)
    {
	/*get name*/
	String name = InputParser.getValue("name", element);

	/*get type*/
	String type = InputParser.getValue("type", element);
	
	/*optional background number density*/
	String init_vals[] = InputParser.getList("init",element);
	
	/*create material*/
	MaterialParser parser = parser_list.get(type.toUpperCase());
	if (parser!=null)
	{
	    Material material = parser.addMaterial(name,element);
	    AddMaterial(material);
	    material.setInitValues(init_vals);			
	}
	else Log.error("Unrecognized material type "+type);
    }

    @Override
    public void exit() 
    {
	/*do nothing*/
    }

    @Override
    /*initialize all materials*/
    public void start() 
    {
	if (has_started) return;
	has_started= true;

	/*init materials*/
	for (Material mat:materials_list)
	    mat.init();

    }

    /**updates densities of all flying materials*/
    public void updateMaterials() 
    {
    	for (Material mat:materials_list)
    		if (!mat.frozen) mat.update();	
	
    	Starfish.domain_module.getPressure().eval();
    	Starfish.domain_module.getTemperature().eval();
    }

    /**@return number of materials*/
    public int numMats() {return materials_list.size();}
	
    /*called after time loop ends*/
    @Override
    public void finish()
    {
	Starfish.boundary_module.getFieldManager().clearAll();
	   
	for (Material mat:materials_list)
	{
	    mat.finish();
	    /*update boundary data*/
	    
	    HashMap<String,FieldCollection1D> map = mat.field_manager1d.getFieldCollections();
	    for (String name:map.keySet())
	    {
		FieldCollection1D glob = Starfish.boundary_module.getFieldCollection(name);
		FieldCollection1D loc = mat.field_manager1d.getFieldCollection(name);
		
		/*add "local" data*/
		glob.add(loc);			
	    }
	
	}
	
	
    }
}
