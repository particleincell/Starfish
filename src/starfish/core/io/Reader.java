/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import starfish.core.common.Starfish.Log;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.FieldManager2D;
import org.w3c.dom.Element;

/** file reader base class*/
public abstract class Reader 
{	
    protected Reader()
    {
    }

    /*returns reader for a particular file type*/
    static public Reader getReader(String file_name, String file_type, Element element) 
    {
	if (file_type.equalsIgnoreCase("TECPLOT"))
	    return new TecplotReader(file_name, element);
	else if (file_type.equalsIgnoreCase("TABLE"))
	    return new TableReader(file_name, element);
	else
	    throw new UnsupportedOperationException("unknown file type "+file_type);		
    }

    public FieldManager2D field_manager = null;

    /**@returns field corresponding to the specified variable*/
    public FieldCollection2D getFieldCollection(String name) 
    {
	if (field_manager == null)
	    Log.error("need to call parse first");

	return field_manager.getFieldCollection(name);
    }
	
    /**reads the input file, loads variables in field_vars, with mesh coords from coord_vars*/
    public abstract void parse(String coord_vars[], String field_vars[]);
}
