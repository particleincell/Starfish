/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import starfish.core.boundaries.Spline;
import starfish.core.common.CommandModule;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.FieldCollection2D.MeshEvalFun;
import starfish.core.domain.Mesh.Face;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;

/** Module for generating 2D field meshes*/
public class DomainModule extends CommandModule
{
    public DomainType getDomainType() 	{return domain_type;}

    static public enum DomainType {XY, RZ, ZR};
    DomainType domain_type;
    
    protected ArrayList<Mesh> mesh_list = new ArrayList<Mesh>();
    
    protected FieldManager2D field_manager;
	
    /** returns mesh list and also performs intersection on first call*/
    public ArrayList<Mesh> getMeshList() {return mesh_list;}
	
    /** returns field manager*/
    public FieldManager2D getFieldManager() {return field_manager;}
	
    /*TODO: clean up this mess, maybe a separate field manager module to check across materials*/
    /**returns appropriate field, smart check for species and average values*/
    public FieldCollection2D getFieldCollection(String var_name) 
    {
	/*first see if we have a species variable*/
	String pieces[] = var_name.split("\\.");
	String base = pieces[0];
		
	String species = null;
	if (pieces.length>1)
	    species = pieces[1];
						
	if (species!=null)	/*species var*/
	{	
	    return Starfish.getMaterial(species).getFieldManager2d().getFieldCollection(base);
	}
	
	/*also check for average, now only for non-species data*/	
	if (base.toLowerCase().matches("(.*)-ave"))
	    return Starfish.averaging_module.getFieldCollection(var_name);
	
	return field_manager.getFieldCollection(var_name);
    }
	
    /**returns appropriate field, smart check for species and average values*/
    public Field2D getField(Mesh mesh, String var_name) 
    {
	return getFieldCollection(var_name).getField(mesh);
    }
	
    public void addMesh(Mesh mesh) {mesh_list.add(mesh);}
	
    /**returns the mesh containing the point x or null*/
    public Mesh getMesh(double x[])
    {
	for (Mesh mesh:mesh_list)
	    if (mesh.containsPosStrict(x))
		return mesh;
	
	/*check for particle being on the boundary*/
	for (Mesh mesh:mesh_list)
	    if (mesh.containsPos(x))
		return mesh;
	
	return null;
    }
			
   /**returns the first mesh with the given name*/
    public Mesh getMesh(String name)
    {
	for (Mesh mesh:mesh_list)
	    if (mesh.name.equalsIgnoreCase(name))
		return mesh;
	return null;
    }
	
    @Override
    public void process(Element element) 
    {
	/*first get the domain type*/
	String type = InputParser.getValue("type", element, "xy");
		
	try{
	domain_type=DomainType.valueOf(type.toUpperCase());
	}
	catch (IllegalArgumentException e)
	{
	    Log.error("Unknown domain type "+type);
	}
		
	/*log*/
	Log.log("Domain type: "+domain_type);
			
	/*process mesh commands*/
	Iterator<Element> iterator = InputParser.iterator(element);
		
	while(iterator.hasNext())
	{
	    Element el = iterator.next();
	    if (el.getNodeName().equalsIgnoreCase("MESH"))
		AddMesh(el);
	    else
		Log.warning("Unknown domain element "+el.getNodeName());
	}
		
	/*init data fields*/
	field_manager = new FieldManager2D(mesh_list);
		
        /*add default fields*/
	field_manager.add("NodeVol", "m^-3",null);
	field_manager.add("phi", "V",null);
        field_manager.add("rho", "C/m^3",null);
        field_manager.add("efi", "V/m",null);
	field_manager.add("efj", "V/m",null);
	field_manager.add("bfi", "T",null);
	field_manager.add("bfj", "T",null);
	field_manager.add("p","Pa",EvalPressure);   //pressure
    }
	
    /**processes <mesh> node*/
    protected void AddMesh(Element element)
    {
	
	/*get mesh name*/
	String name = InputParser.getValue("name", element,String.format("mesh%04d",mesh_list.size()));

	/*get type*/
	String type = InputParser.getValue("type", element);

	Mesh abs_mesh = null;	/*abstract mesh*/
		
	/*uniform mesh*/
	if (type.equalsIgnoreCase("UNIFORM"))
	{
	    String origin[] = InputParser.getList("origin", element);
	    String spacing[] = InputParser.getList("spacing", element);
	    String nodes[] = InputParser.getList("nodes", element);

	    /*create new uniform mesh*/
	    UniformMesh mesh = new UniformMesh(Integer.parseInt(nodes[0]),
					    Integer.parseInt(nodes[1]),
					    domain_type);
	    
	    mesh.setOrigin(Double.parseDouble(origin[0]),Double.parseDouble(origin[1]));
	    mesh.setSpacing(Double.parseDouble(spacing[0]), Double.parseDouble(spacing[1]));
	    mesh.setName(name);
	    addMesh(mesh);
			
	    abs_mesh=mesh;
			
	    /*log*/
	    Log.log("Added UNIFORM_MESH");
	    Log.log("> nodes   = "+nodes[0]+" : "+nodes[1]);
	    Log.log("> origin  = "+origin[0]+" : "+origin[1]);
	    Log.log("> spacing = "+spacing[0]+" : "+spacing[1]);
	}
	else if (type.equalsIgnoreCase("ELLIPTIC"))
	{
	    String nodes[] = InputParser.getList("nodes", element);
	    int ni = Integer.parseInt(nodes[0]);
	    int nj = Integer.parseInt(nodes[1]);

	    String left[] = InputParser.getList("left",element);
	    String bottom[] = InputParser.getList("bottom",element);
	    String right[] = InputParser.getList("right",element);
	    String top[] = InputParser.getList("top",element);

	    Spline splines[] = new Spline[4];
	    ArrayList<Spline> list = new ArrayList<Spline>();

	    try{
		/*left*/
		for (String str:left)
		    list.add(Starfish.getBoundary(str));
		splines[Face.LEFT.value()] = new Spline(list);

		/*bottom*/
		list.clear();
		for (String str:bottom)
		    list.add(Starfish.getBoundary(str));
		splines[Face.BOTTOM.value()] = new Spline(list);

		/*right*/
		list.clear();
		for (String str:right)
		    list.add(Starfish.getBoundary(str));
		splines[Face.RIGHT.value()] = new Spline(list);

		/*top*/
		list.clear();
		for (String str:top)
		    list.add(Starfish.getBoundary(str));
		splines[Face.TOP.value()] = new Spline(list);
	    }
	    catch (NoSuchElementException e)
	    {
		Log.error(e.getMessage());
	    }
		
	    /*create new uniform mesh*/
	    EllipticMesh mesh = new EllipticMesh(ni,nj,splines,domain_type);
	    mesh.setName(name);
	    addMesh(mesh);

	    abs_mesh = mesh;

	    /*log*/
	    Log.log("Added ELLIPTIC_MESH");
	    Log.log("> nodes   = "+ni+" : "+nj);
	    Log.log("> left  = "+InputParser.getValue("left", element));
	    Log.log("> bottom = "+InputParser.getValue("bottom",element));
	    Log.log("> right = "+InputParser.getValue("right",element));
	    Log.log("> top = "+InputParser.getValue("top",element));			
	}
	else
	    Log.error("Unrecognized mesh type "+type);
		
	/*check for mesh boundary conditions and set if given*/
	Element bc_eles[] = InputParser.getChildren("mesh-bc",element);
	for (Element bc_ele:bc_eles)
	{
	    Face face = null;
	    Mesh.NodeType node_type = null;
	    double value = InputParser.getDouble("value", bc_ele,0);
			
	    try 
	    {
		String wall_name = InputParser.getValue("wall", bc_ele);
		face = Face.valueOf(wall_name.toUpperCase());
	    }
	    catch (Exception e)
	    {
		Log.error("<wall> must be set and must be one of LEFT, RIGHT, TOP, or BOTTOM");
	    }
			
	    try 
	    {
		String type_name = InputParser.getValue("type", bc_ele);
		node_type = Mesh.NodeType.valueOf(type_name.toUpperCase());
	    }
	    catch (Exception e)
	    {
		Log.error("<type> must be set and must be one of DIRICHLET, NEUMANN or PERIODIC");
	    }
	    abs_mesh.setMeshBCType(face, node_type, value);	
	}
				
    }

    @Override
    public void exit() 
    {
	/*do nothing*/
    }
    
    @Override
    public void start() 
    {
	if (has_started) return;
	
	Log.log("Initializing domain");
	/*TODO: this is currently getting called twice, once 
	* on <domain> and then by <starfish> (after boundaries are loaded*/		
	

	
	
	/*initialize all meshes*/
        for (Mesh mesh:mesh_list)
	{
	    mesh.init();
	    mesh.setBoundaries(Starfish.getBoundaryList());
	}

	/*first init nodes, must be done before we call init*/
        for (Mesh mesh:mesh_list)
	{
	    mesh.initNodes();	    
	}
	
	/*synch mesh volumes*/
	Starfish.getFieldCollection("NodeVol").syncMeshBoundaries();	
	
	has_started = true;
    }
    
    /*accessors*/
    public Field2D getPhi(Mesh mesh) {return getPhi().getField(mesh);}
    public Field2D getRho(Mesh mesh) {return getRho().getField(mesh);}
    public Field2D getEfi(Mesh mesh) {return getEfi().getField(mesh);}
    public Field2D getEfj(Mesh mesh) {return getEfj().getField(mesh);}
    public Field2D getBfi(Mesh mesh) {return getBfi().getField(mesh);}
    public Field2D getBfj(Mesh mesh) {return getBfj().getField(mesh);}
    public Field2D getP(Mesh mesh) {return getPressure().getField(mesh);}

    public FieldCollection2D getPhi() {return field_manager.getFieldCollection("phi");}
    public FieldCollection2D getRho() {return field_manager.getFieldCollection("rho");}
    public FieldCollection2D getEfi() {return field_manager.getFieldCollection("efi");}
    public FieldCollection2D getEfj() {return field_manager.getFieldCollection("efj");}
    public FieldCollection2D getBfi() {return field_manager.getFieldCollection("bfi");}
    public FieldCollection2D getBfj() {return field_manager.getFieldCollection("bfj");}
    public FieldCollection2D getPressure() {return field_manager.getFieldCollection("p");}

    
    /*evaluates pressure*/
    MeshEvalFun EvalPressure = new MeshEvalFun()
    {
    @Override
    public void eval(FieldCollection2D fc)
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    Field2D f = fc.getField(mesh);
	    f.clear();
	    double data[][] = f.getData();
	    
	    for (Material mat:Starfish.getMaterialsList())
	    {
		double pp[][] = mat.getP(mesh).getData();
		for (int i=0;i<mesh.ni;i++)
		    for (int j=0;j<mesh.nj;j++)
		    {
			data[i][j] += pp[i][j];		    
		    }	    
	    }
	}
    }    
    };
}

