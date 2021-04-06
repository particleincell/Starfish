/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.diagnostics;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.CommandModule;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.diagnostics.AnimationModule.Animation;
import starfish.core.io.InputParser;
import starfish.core.io.TecplotWriter;
import starfish.core.io.VTKWriter;
import starfish.core.io.Writer;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;

/** Particle trace command, launched by particle_trace */
public class ParticleTraceModule extends CommandModule {
	@Override
	public void init() {
		/* do nothing */ }

	@Override
	public void process(Element element) {

		Tracer tracer = new Tracer(element);
		Starfish.diagnostics_module.addDiagnostic(tracer);
	}

	@Override
	public void start() {
		/* do nothing */}

	@Override
	public void exit() {
		/* do nothing */
	}


	public class ParticleTrace {
		public int id;			/** particle id */
		public ArrayList<Particle> samples = new ArrayList<>();
		public ArrayList<Integer> time_steps = new ArrayList<>();
		public ParticleTrace(int id) {this.id=id;}
		/** saves new trace for the specified particle */
		void addParticle(Particle part) {
			samples.add(new Particle(part));
			time_steps.add(Starfish.getIt());
		}
		boolean active = true;


	}
	
	//data associated with a single "particle_trace" command
	class Tracer implements DiagnosticsModule.Diagnostic {
		int start_ts;  // iteration to start sampling */
		int end_ts; 	// time step to stop sampling
		int skip_ts;	// number of time steps between sampling
		int output_skip;  // number of samples between output
		
		KineticMaterial km;
		Writer writer;
		
		ParticleTrace traces[];
		
		int n_unsaved = 0;
		
		
		Tracer(Element element) {
			String format = InputParser.getValue("format", element, "VTK");
			String material = InputParser.getValue("material", element);

			Material mat = Starfish.getMaterial(material);
			if (mat == null || !(mat instanceof KineticMaterial))
				Log.error(String.format("Material %s not found or not a kinetic material"));
			km = (KineticMaterial) mat;

			int ids[] = InputParser.getIntList("ids", element);
			if (ids.length==0) {
				int rand_ids[] = InputParser.getIntList("random_ids", element);
				if (rand_ids.length!=2) {Log.warning("Either <ids> or <random_ids> needs to be specified"); return;}
				
				if (rand_ids[0]<=0 || rand_ids[1]<=0 || 
					rand_ids[0]> rand_ids[1]) {Log.warning("Syntax <random_ids>num_traces, num_particles</random_ids>"); return;}
				ids = new int[rand_ids[0]];
				for (int i=0;i<ids.length;i++) {
					boolean duplicate;
					do {
						duplicate = false;
						ids[i] = Starfish.rndi(rand_ids[1]);
						for (int j=0;j<i;j++)
							if (ids[j]==ids[i]) {duplicate=true;break;}
					} while(duplicate); 
					
				}
				
			}
			
			traces = new ParticleTrace[ids.length];
			for (int i=0;i<ids.length;i++) {
				traces[i] = new ParticleTrace(ids[i]);
			}
			
			start_ts = InputParser.getInt("start_it", element, 0);
			end_ts = InputParser.getInt("start_it", element, 0);
			skip_ts = InputParser.getInt("skip_it", element,-1);
			output_skip = InputParser.getInt("output_skip", element, 100);
			
			if (format.equalsIgnoreCase("VTK"))
				writer = new VTKWriter(element);
			else if (format.equalsIgnoreCase("TECPLOT"))
				writer = new TecplotWriter(element);
			else
				Log.error("Unsupported writer format " + format);
			writer.initTrace(element);

		}

		
		public void exit() {

			writer.writeTraces(traces);
			writer.close();
		}
		
		public void sample(boolean force) {
			if (km==null) return;
			
			int ts = Starfish.getIt();
			if (force || ts>=start_ts && (end_ts<=0 || end_ts>ts) && (ts-start_ts)%skip_ts==0) {
				for (ParticleTrace trace:traces) {
					if (trace.active) {
						Particle part = km.getParticle(trace.id);
						if (part!=null)
							trace.addParticle(part);
						else if (trace.samples.size()>0)  // deactivate if already got data 
							trace.active=false;
					}
				}
			}
			
			n_unsaved++;
			if (n_unsaved%output_skip==0) {
				writer.writeTraces(traces);
			}
				
		}
	}
}
