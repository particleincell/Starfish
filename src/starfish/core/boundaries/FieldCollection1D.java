/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * FieldCollection collects fields corresponding to the same physical quantity 
 * (number density, etc...) across multiple boundaries. It can be created in one of three ways:
 * 1) FieldCollection(Boundary): allocates space for the variable on the specified boundary
 * 2) {@code FieldCollection(ArrayList<boundary>)}: like 1) but for the specified boundary list
 * 3) FieldCollection(Field): wraps an existing field into a single boundary field collection
 */
public class FieldCollection1D
{

    /**
     *
     * @param boundary_list
     */
    public FieldCollection1D(ArrayList<Boundary> boundary_list)
    {
	/*add fields for each boundary*/
	for (Boundary boundary:boundary_list)
	{
	    fields.put(boundary, new Field1D(boundary));
	}	
    }
	
    /**
     *
     * @param boundary
     */
    public FieldCollection1D(Boundary boundary)
    {
	/*add fields for each boundary*/
	fields.put(boundary, new Field1D(boundary));
    }

    /**
     *
     * @param field
     */
    public FieldCollection1D(Field1D field)
    {
	fields.put(field.getBoundary(), field);
    }
	
    /**returns field for the boundary, creates a new field if not yet in the collectio
     * @param boundary
     * @return n*/
    public Field1D getField(Boundary boundary) 
    {
	/*do we already have this boundary in the list?*/
	Field1D field = fields.get(boundary);
	if (field==null)
	{
	    field = new Field1D(boundary);
	    fields.put(boundary, field);
	}
	return field;
    }
	
    /**
     *
     * @return
     */
    public Field1D[] getFields() {return fields.values().toArray(new Field1D[fields.values().size()]);}
    HashMap<Boundary,Field1D> fields = new HashMap<Boundary,Field1D>();
	
    /**sets the entire collection to a constant valu
     * @param valuee*/
    public void setValue(double value) 
    {
	for (Field1D field:this.fields.values())
	    field.setValue(value);
    }

    /**
     *
     * @return
     */
    public double[] getRange() 
    {
	double range[] = new double[2];
	range[0] = 1e66;
	range[1] = -1e66;
		
	for (Field1D field:fields.values())
	{
	    double fr[] = field.getRange();
	    if (fr[0]<range[0]) range[0]=fr[0];
	    if (fr[1]>range[1]) range[1]=fr[1];
	}
		
	return range;
    }

    /** sets all fields to zero*/
    public void clear() 
    {
	for (Field1D field:fields.values())
	    field.clear();
    }

    /**
     *
     * @param loc
     */
    public void add(FieldCollection1D loc)
    {
	for (Field1D field:fields.values())
	{
	    field.add(loc.getField(field.boundary));
	}
    }
}
