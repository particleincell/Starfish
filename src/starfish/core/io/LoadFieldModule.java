/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldManager2D;
import starfish.core.domain.Mesh;
import starfish.core.io.Reader.ReaderFactory;

/** handles &lt; load_field &gt; */
public class LoadFieldModule extends CommandModule {
	@Override
	public void init() {
		/* register readers */
		registerReader("TECPLOT", TecplotReader.tecplotReaderFactory);
		registerReader("TABLE", TableReader.tableReaderFactory);
	}

	/**
	 * registers a new reader file reader type
	 * 
	 * @param file_type file type
	 * @param fac       reader factory
	 */
	static public void registerReader(String file_type, ReaderFactory fac) {
		Reader.registerReader(file_type, fac);
	}

	@Override
	public void process(Element element) {
		String format = InputParser.getValue("format", element, "TECPLOT");
		String file_name = Starfish.wd+InputParser.getValue("file_name", element);

		Reader reader = Reader.getReader(file_name, format, element);

		String coord_vars[] = InputParser.getList("coords", element);
		String field_vars[] = InputParser.getList("vars", element);

		if (coord_vars.length != 2)
			Log.error("<coords> element must specify exactly 2 coordinate variables");

		/* map file vars to simulation variables */
		String sim_vars[] = new String[field_vars.length];

		for (int i = 0; i < field_vars.length; i++) {
			String pieces[] = field_vars[i].split("\\s*=\\s*");
			sim_vars[i] = pieces[0];
			if (pieces.length == 1)
				field_vars[i] = pieces[0];
			else if (pieces.length == 2)
				field_vars[i] = pieces[1];
			else
				Log.error("<vars> must be specified as sim_var or sim_var=data_var");
		}

		reader.parse(coord_vars, field_vars);

		FieldManager2D field_manager = Starfish.domain_module.getFieldManager();

		for (Mesh mesh : Starfish.getMeshList())
			for (int i = 0; i < field_vars.length; i++) {
				Field2D field = Field2D.FromExisting(mesh, reader.getFieldCollection(field_vars[i]));
				field_manager.add(sim_vars[i], "", field, null);
				Log.log("Added " + sim_vars[i]);
			}
	}

	@Override
	public void start() {
		/* nothing to do */
	}

	@Override
	public void exit() {
		/* nothing to do */
	}
}
