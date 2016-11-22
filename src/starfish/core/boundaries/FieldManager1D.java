/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import java.util.ArrayList;
import java.util.HashMap;
import starfish.core.common.Starfish;

/**
 * Field manager holds a list of FieldCollections
 * 
 * Each FieldCollection collects fields corresponding to a single variable
 * over a number of boundaryes
*/
public class FieldManager1D 
{
    /*variable names*/ 
    protected ArrayList<String> names;
	
    /*units*/
    protected HashMap<String,String> units;
	
    /*list of boundaryes*/
    protected ArrayList<Boundary> boundary_list;

    /*collection of the named field on all boundaryes*/
    HashMap<String,FieldCollection1D> field_collection;
	
    /**returns true if the field exists*/
    public boolean hasField(String name) { return names.contains(name);}

    /**constructor for a single boundary*/
    public FieldManager1D(Boundary boundary) 
    {
	ArrayList<Boundary> list = new ArrayList<Boundary>();
	list.add(boundary);
		
	construct(list);
    }
				
    /**constructor for a collection of boundaries*/
    public FieldManager1D(ArrayList<Boundary> boundary_list) 
    {
	construct(boundary_list);
    }
	
    /**initialization function*/
    protected final void construct(ArrayList<Boundary> boundary_list) 
    {
	this.boundary_list = boundary_list;		/*save parent*/
		
	/*allocate memory structues*/
	names = new ArrayList<String>();
	units = new HashMap<String,String>();
	field_collection = new HashMap<String,FieldCollection1D>();
    }

    public HashMap<String,FieldCollection1D> getFieldCollections() {return field_collection;}
    public FieldCollection1D getFieldCollection(String name) {return field_collection.get(name.toLowerCase());}
    public ArrayList<String> getNames() {return names;}
    
    /**returns units*/
    public String getUnits(String name) {return units.get(name);}

    /**add a new named field*/
    public FieldCollection1D add(String name, String unit) 
    {
	/*set to lowercase*/
	name = name.toLowerCase();
		
	FieldCollection1D collection;
		
	/*check if we already have this variable*/
	collection = field_collection.get(name);
	if (collection!=null) return collection;
		
	/*save this variable*/
	names.add(name);
	units.put(name,unit);
    
	/*new field collection*/
	collection = new FieldCollection1D(boundary_list);
		
	/*add to list*/
	field_collection.put(name, collection);
		
	return collection;
    }
	
    /**add a new named field and initializes it according to data from Field*/
    public void add(String name, String unit, Field1D values) 
    {
	FieldCollection1D collection = add(name,unit);
		
	Field1D field = collection.getField(values.getBoundary());
	Field1D.DataCopy(values.data,field.data);	
    }
	
    /**add a new named field and initializes it to a constant value*/
    public void add(String name, String unit, double value) 
    {
	FieldCollection1D collection = add(name,unit);
	collection.setValue(value);
    }
	
    /** returns field for a given variable name, doesn't do any error checking*/
    public Field1D getField(Boundary boundary, String var_name)
    {
	String pieces[] = var_name.split("\\.");
			
	/*if doesn't contain species name*/
	if (pieces.length==1)	/*species var*/
	{	
	    return field_collection.get(var_name).getField(boundary);
	}
	else
	{
	    String base = pieces[0];
	    String species = pieces[1];
	    
	    /*call "self" recursively using the base name*/
	    return Starfish.getMaterial(species).getFieldManager1d().getField(boundary,base);
	}
	
    }

    /**zeroes out all data*/
    public void clearAll()
    {
	for (String name:field_collection.keySet())
	    field_collection.get(name).clear();
    }
}
