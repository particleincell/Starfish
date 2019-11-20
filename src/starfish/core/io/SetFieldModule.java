package starfish.core.io;

import org.w3c.dom.Element;

import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.FieldCollection2D;

public class SetFieldModule extends CommandModule {

	@Override
	public void process(Element element) {
		String exprs[] = InputParser.getList("vals", element);

		for (String expr:exprs) {
			String pieces[] = expr.split("\\s*=\\s*");
			if (pieces.length!=2) {Log.warning("Bad syntax in "+expr); continue;}
			String field_name = pieces[0];
			double value = Double.parseDouble(pieces[1]);
			FieldCollection2D fc=Starfish.getFieldCollection(field_name);
			if (fc==null) {Log.warning("Unknown field "+field_name); continue;}
			fc.setValue(value);
			Log.log("Set "+field_name+" = "+String.format("%g",value));
		}
		
	}

}
