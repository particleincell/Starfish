/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.util.HashMap;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.FieldManager2D;
import org.w3c.dom.Element;

/** file reader base class */
public abstract class Reader {

	/**
	 *
	 */
	double dim_scale = 1.0; // scaling for dimensions
	double var_scale = 1.0;

	protected Reader(Element element) {
		String dim_units = InputParser.getValue("dim_units", element, "m").toUpperCase();
		if (dim_units.startsWith("IN"))
			dim_scale = 0.0254;
		else if (dim_units.startsWith("CM"))
			dim_scale = 0.01;
		else if (dim_units.startsWith("MM"))
			dim_scale = 0.001;

		var_scale = InputParser.getDouble("scale", element, var_scale);

	}

	public interface ReaderFactory {
		/**
		 * creates a file loader
		 *
		 * @param file_name file to load
		 * @param element   XML parent element
		 * @return new file loader
		 */
		public Reader makeReader(String file_name, Element element);
	}

	static HashMap<String, ReaderFactory> file_readers = new HashMap();

	/**
	 * Returns an instance of a reader for the specified file_type
	 * 
	 * @param file_name file to read
	 * @param file_type file type
	 * @param element   XML element
	 * @return
	 */
	static public Reader getReader(String file_name, String file_type, Element element) {
		ReaderFactory fac = file_readers.get(file_type.toUpperCase());
		if (fac != null)
			return fac.makeReader(file_name, element);

		throw new UnsupportedOperationException("Unknown file reader type  " + file_type);
	}

	static public void registerReader(String file_type, ReaderFactory fac) {
		file_readers.put(file_type.toUpperCase(), fac);
		Log.log("Registered file reader type " + file_type.toUpperCase());
	}

	/**
	 *
	 */
	public FieldManager2D field_manager = null;

	/**
	 * @param name * @return field corresponding to the specified variable
	 */
	public FieldCollection2D getFieldCollection(String name) {
		if (field_manager == null)
			Log.error("need to call parse first");

		return field_manager.getFieldCollection(name);
	}

	/**
	 * reads the input file, loads variables in field_vars, with mesh coords from
	 * coord_var
	 * 
	 * @param coord_varss
	 * @param field_vars
	 */
	public abstract void parse(String coord_vars[], String field_vars[]);
}
