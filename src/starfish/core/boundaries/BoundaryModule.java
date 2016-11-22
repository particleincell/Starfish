/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;
import starfish.core.solver.Matrix;

public class BoundaryModule extends CommandModule
{
    /*variables*/
    protected ArrayList<Boundary> boundary_list = new ArrayList<Boundary>();
    protected FieldManager1D field_manager;
	
    /*methods*/
    public Field1D getField(Boundary boundary, String var_name) {return field_manager.getField(boundary,var_name);}
    public ArrayList<Boundary> getBoundaryList() {return boundary_list;}
    
    public FieldManager1D getFieldManager() {return field_manager;}
    public FieldCollection1D getFieldCollection(String name) {return field_manager.getFieldCollection(name);}

    protected ArrayList<Segment> seg_list;
    
    public void addBoundary(Boundary boundary)
    {
	boundary_list.add(boundary);
    }
	
    public Boundary getBoundary(String name) 
    {
	for (Boundary boundary:boundary_list)
	    if (boundary.name.equalsIgnoreCase(name))
		return boundary;
	throw new NoSuchElementException("boundary "+name+" not found");
    }
		
    @Override
    public void process(Element element) 
    {
	
	/*grab transformation if set*/
	Element transf = InputParser.getChild("transform", element);
	double scaling[] = {1,1};
	double translation[] = {0,0};
	double rotation = 0;
	
	if (transf!=null)
	{
	    scaling = InputParser.getDoubleList("scaling", transf,scaling);
	    translation = InputParser.getDoubleList("translation",transf,translation);
	    rotation = InputParser.getDouble("rotation",transf,rotation);
	}
	
	/*create global transformation matrix*/
	Matrix G = Matrix.makeTransformationMatrix(scaling,rotation,translation);
	
	/*process commands*/
	Iterator<Element> iterator = InputParser.iterator(element);
	
	while(iterator.hasNext())
	{
	    Element el = iterator.next();
	    if (el.getNodeName().equalsIgnoreCase("BOUNDARY"))
		NewBoundary(el,G);
	    else
		Log.warning("Unknown <boundaries> element "+el.getNodeName());
	}
		
    }

    /**ads a new boundary
     *
     * @param element Parent element for this boundary
     * @param G	global transformation matrix
     */
    public void NewBoundary(Element element, Matrix G)
    {
	/*get name and type*/
	String name = InputParser.getValue("name", element);
	String type = InputParser.getValue("type", element,"solid");
		
	/*b.c.*/
	double value = InputParser.getDouble("value", element,0);
		
	/*set boundary type*/
	NodeType boundary_type=null;
	if (type.equalsIgnoreCase("OPEN")) boundary_type = NodeType.OPEN;
	else if (type.equalsIgnoreCase("SOLID")) boundary_type = NodeType.DIRICHLET;
	else if (type.equalsIgnoreCase("SYMMETRY")) boundary_type = NodeType.SYMMETRY;
	else if (type.equalsIgnoreCase("VIRTUAL")) boundary_type = NodeType.VIRTUAL;
	else Log.error("Unknown boundary type "+type);
		
	/*material type if solid*/
	Material material = null;
	if (boundary_type==NodeType.DIRICHLET)
	{
	    try{
		String mat_name = InputParser.getValue("material", element);
		material = Starfish.getMaterial(mat_name);
	    }
	    catch (Exception e) {
		Log.error(e.getMessage());
	    }
	}
		
	/*also try to grab temperature*/
	double temp = InputParser.getDouble("temp", element, 300);
	
	/*make new boundary*/
	Boundary boundary = new Boundary(name, boundary_type, value, material);
	boundary.setTemp(temp);
		
	/*read reverse flag if defined*/
	boolean flip_normals = InputParser.getBoolean("reverse", element, false);

	/*spline split?*/
	//int split = InputParser.getInt("split",element,1);
	
	/*grab local transformation if set*/
	Element transf = InputParser.getChild("transform", element);
	double scaling[] = {1,1};
	double translation[] = {0,0};
	double rotation = 0;
	
	if (transf!=null)
	{
	    scaling = InputParser.getDoubleList("scaling", transf,scaling);
	    translation = InputParser.getDoubleList("translation",transf,translation);
	    rotation = InputParser.getDouble("rotation",transf,rotation);
	}

	/*make local transformation matrix*/
	Matrix L = Matrix.makeTransformationMatrix(scaling,rotation,translation);
	
	/*update total matrix, T=G*L*/
	Matrix T = G.mult(L); 

	/*add points*/
	String path = InputParser.getValue("path", element);
	boundary.setPath(path,T,flip_normals);

	/*add boundary to the list*/
	addBoundary(boundary);
		
	/*log*/
	Log.log("Added " + boundary_type + " boundary '"+name+"'");	
    }
	
    @Override
    public void init()
    {
	/*nothing*/
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
	
	for (Boundary boundary:boundary_list)
	    boundary.init();
			
	/*build a list of all segments for visibility checking*/
	seg_list = new ArrayList();
	for (Boundary boundary:boundary_list)
	    for (int i=0;i<boundary.numSegments();i++)
		seg_list.add(boundary.getSegment(i));
	
	/*init field manager*/
	field_manager = new FieldManager1D(boundary_list);

	/*setup fields, these are the summations of individual materials*/
	
	/*grab field manager for the first material, the data lists are the same on all*/
	/*manually start materials module if not yet started*/
	Starfish.materials_module.start();
	
	FieldManager1D material_fm = Starfish.materials_module.getMaterialsList().get(0).getFieldManager1d();
	
	for (String name:material_fm.getNames())
	{
	    String unit = material_fm.getUnits(name);
	    field_manager.add(name, unit);
	}
	
	has_started = true;
	
    }
    
    
    /**@return true if the point xp is internal to any boundary spline*/
    public boolean isInternal(double xp[])
    {
	Segment seg = Spline.visibleSegment(xp, seg_list);
	if (seg==null) return false;
	
	return Spline.isInternal(xp, seg);
    }

}


