package starfish.core.io;

/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.FieldManager2D;
import starfish.core.domain.Mesh;
import starfish.core.io.Reader.ReaderFactory;

/** handles &lt; load_field &gt; */
public class SetFieldModule extends CommandModule {
	@Override
	public void init() {
		/* register readers */
	}

	@Override
	public void process(Element element) {

		String vars[] = InputParser.getList("vars", element);
		
		//opt
		double x1[] = InputParser.getDoubleList("x1", element);
		double x2[] = InputParser.getDoubleList("x2", element);

		for (String var:vars) {
			String pieces[] = var.split("\\s*=\\s*");
			if (pieces.length==2) {
				String var_name = pieces[0];
				double val = Double.parseDouble(pieces[1]);
				FieldCollection2D fc = Starfish.getFieldCollection(var_name);
				if (fc==null) {
					Log.warning("Skipping unrecognized field "+var_name);
					continue;
				}
				for (Mesh mesh : Starfish.getMeshList())
				{
					Field2D field = fc.getField(mesh);
					if (x1.length!=2 || x2.length!=2)
						field.setValue(val);
					else
					{
						for (int i=0;i<mesh.ni;i++)
							for (int j=0;j<mesh.nj;j++)
							{
								double x[] = mesh.pos(i,j);
								if (x[0]>=x1[0] && x[0]<=x2[0] && x[1]>=x1[1] && x[1]<=x2[1]) 
									field.set(i, j, val);
							}
					}
				}	
				Log.log(String.format ("Set %s=%g",var_name,val));
			}
			else
				Log.error("<vars> must be specified in var_name=value format");
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
