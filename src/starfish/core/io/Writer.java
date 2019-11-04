/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.diagnostics.ParticleTraceModule.ParticleTrace;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.Mesh;
import starfish.core.materials.KineticMaterial.Particle;

/**
 *
 * @author Lubos Brieda
 */
public abstract class Writer {
	static enum OutputType {
		FIELD, ONED, THREED, BOUNDARIES, PARTICLES, TRACE
	};

	static enum Dim {
		I, J
	};

	OutputType output_type;
	String scalars[];
	ArrayList<String[]> vectors;
	String cell_data[]; // cell centered variables
	Mesh output_mesh;
	FileOutputStream output_stream; // output stream associated with the PrintWriter

	/* for 1D output */
	Dim dim; // dimension
	int index; // node index
	boolean ave1d; // average 1D data in perp direction
	int time_data_lines; // number of entries in a single time file
	int time_data_write_skip; // number of samples between write outs
	double time_data_time_scale; // time axis multiplier

	/* support for time data */
	protected int time_data_current_line;
	protected double[][][] time_data; // [time_line][variable][i/j]
	protected double[] time_data_time; // time corresponding to each line

	int resolution; // for three-D rotation
	String file_name;

	/* for particles */
	int particle_count;
	boolean rotate;
	String mat_name;

	/* general constructor */
	public Writer(Element element) {
		file_name = InputParser.getValue("file_name", element);
	}

	protected PrintWriter open(String file_name) {
		PrintWriter pw = null;
		String full_name = Starfish.options.wd+file_name;

		try {
			output_stream = new FileOutputStream(full_name);
			pw = new PrintWriter(output_stream);
		} catch (IOException ex) {
			// see if the problem is a missing directory
			try {
				String pieces[] = this.splitFileName(full_name);
				Files.createDirectories(Paths.get(pieces[2]));
				output_stream = new FileOutputStream(full_name);
				pw = new PrintWriter(output_stream);
			} catch (IOException ex2) {
				Log.error("error opening file " + full_name);
			}
		}

		return pw;
	}

	/**
	 * Outputs data revolved around the axis
	 *
	 * @param scalars
	 * @param vectors
	 * @param cell_data
	 * @param element   XML element
	 */
	public void init3D(String[] scalars, ArrayList<String[]> vectors, String[] cell_data, Element element) {

		// number of cells in the theta direction
		resolution = InputParser.getInt("resolution", element, 45);

		/* call main open function */
		init2D(scalars, vectors, cell_data, element);

		/* set output type - after init2D */
		output_type = OutputType.THREED;
	}

	/**
	 * Outputs data along a specified I or J index in a single mesh
	 *
	 * @param scalars
	 * @param vectors
	 * @param cell_data
	 * @param element   XML element
	 */
	public void init1D(String[] scalars, ArrayList<String[]> vectors, String[] cell_data, Element element) {
		String mesh_name = InputParser.getValue("mesh", element);
		String str_index = InputParser.getValue("index", element);
		time_data_lines = InputParser.getInt("time_data_lines", element, 1); // number of time entries in a single time
																				// file
		if (time_data_lines < 1)
			time_data_lines = 1;
		time_data_write_skip = InputParser.getInt("time_data_write_skip", element, 5); // number of samples between
																						// writes
		time_data_time_scale = InputParser.getDouble("time_data_time_scale", element, 1); // multiplier for the
																							// time-axis
		/* grab the mesh */
		output_mesh = Starfish.domain_module.getMesh(mesh_name);
		if (output_mesh == null) {
			Log.error("Mesh " + mesh_name + " does not exist");
			return;
		}

		/* parse index */
		String pieces[] = str_index.split("\\s*=\\s*");
		if (pieces.length != 2)
			Log.error(String.format("couldn't parse index string %s, syntax [I/J]=value", index));

		if (pieces[0].equalsIgnoreCase("I"))
			dim = Dim.I;
		else if (pieces[0].equalsIgnoreCase("J"))
			dim = Dim.J;
		else
			Log.error("Unknown dimension " + pieces[0]);

		/* allocate memory for time data */
		int num_vars = cell_data.length + scalars.length + vectors.size() * 2;
		int nk;
		if (dim == Dim.I)
			nk = output_mesh.nj;
		else
			nk = output_mesh.ni;
		time_data = new double[time_data_lines][num_vars][nk];
		time_data_time = new double[time_data_lines];

		/* check for averaging or set node index */
		if (pieces[1].toUpperCase().contains("AVE")) {
			ave1d = true;
			if (dim == Dim.I)
				index = (int) (0.5 * output_mesh.ni);
			else
				index = (int) (0.5 * output_mesh.nj);
			index = 0;
		} else {
			this.index = Integer.parseInt(pieces[1]);
			ave1d = false;
		}

		/* call main open function */
		init2D(scalars, vectors, cell_data, element);

		/* set output type - after init2D */
		output_type = OutputType.ONED;
	}

	/**
	 * open function for 2D field data
	 * 
	 * @param scalars
	 * @param vectors
	 * @param cell_data
	 * @param element   xml data element
	 */
	protected void init2D(String[] scalars, ArrayList<String[]> vectors, String[] cell_data, Element element) {
		output_type = OutputType.FIELD;

		String vars_temp[] = new String[scalars.length];
		int temp_length = 0;

		Mesh mesh = Starfish.getMeshList().get(0);
		for (int v = 0; v < scalars.length; v++) {
			try {
				Starfish.getField(mesh, scalars[v]);
				vars_temp[temp_length++] = scalars[v];
			} catch (Exception e) {
				Log.warning("Skipping unrecognized variable " + scalars[v]);
			}
		}

		/* save vars */
		this.scalars = new String[temp_length];
		System.arraycopy(vars_temp, 0, this.scalars, 0, temp_length);

		/* now repeat for cell variables */
		String cell_data_temp[] = new String[cell_data.length];
		int cell_data_temp_length = 0;

		for (int v = 0; v < cell_data.length; v++) {
			try {
				Starfish.getField(mesh, cell_data[v]);
				cell_data_temp[cell_data_temp_length++] = cell_data[v];
			} catch (Exception e) {
				Log.warning("Skipping unrecognized variable " + cell_data[v]);
			}
		}
		/* save vars */
		this.cell_data = new String[cell_data_temp_length];
		System.arraycopy(cell_data_temp, 0, this.cell_data, 0, cell_data_temp_length);

		this.vectors = new ArrayList<String[]>();
		for (String[] pair : vectors) {
			try {
				Starfish.getField(mesh, pair[0]);
				Starfish.getField(mesh, pair[1]);
				this.vectors.add(pair);
			} catch (Exception e) {
				Log.warning("Skipping unrecognized vector pair " + pair[0] + ":" + pair[1]);
			}
		}
	}

	/**
	 * open function for boundary data
	 * 
	 * @param variables
	 * @param element   xml element
	 */
	protected void initBoundaries(String[] variables, Element element) {
		output_type = OutputType.BOUNDARIES;

		String vars_temp[] = new String[variables.length];
		int temp_length = 0;

		/* validate variables */
		for (int v = 0; v < variables.length; v++) {
			try {
				Starfish.boundary_module.getFieldCollection(variables[v]);
				vars_temp[temp_length++] = variables[v];
			} catch (Exception e) {
				Log.warning("Skipping unrecognized variable " + variables[v]);
			}
		}

		/* save vars */
		this.scalars = new String[temp_length];
		System.arraycopy(vars_temp, 0, this.scalars, 0, temp_length);
	}

	/**
	 * open function for particle output
	 * 
	 * @param element
	 */
	public void initParticles(Element element) {
		output_type = OutputType.PARTICLES;

		particle_count = InputParser.getInt("count", element, 1000);
		mat_name = InputParser.getValue("material", element);
		rotate = InputParser.getBoolean("rotate", element, true);

		/* save vars */
		scalars = new String[5];

		if (Starfish.domain_module.getDomainType() == DomainType.XY) {
			scalars[0] = "z (m)";
			scalars[1] = "u";
			scalars[2] = "v";
			scalars[3] = "w";
		} else if (Starfish.domain_module.getDomainType() == DomainType.RZ) {
			scalars[0] = "theta (rad)";
			scalars[1] = "ur";
			scalars[2] = "uz";
			scalars[3] = "utheta";
		} else if (Starfish.domain_module.getDomainType() == DomainType.ZR) {
			scalars[0] = "theta (rad)";
			scalars[1] = "uz";
			scalars[2] = "ur";
			scalars[3] = "utheta";
		}

		scalars[4] = "id";
	}

	/**
	 * open function for particle output
	 * 
	 * @param element
	 */
	public void initTrace(Element element) {
		output_type = OutputType.TRACE;
		rotate = InputParser.getBoolean("rotate", element, true);
	}

	/**
	 * Wrapper for default parameter
	 *
	 */
	public final void write() {
		write(false);
	}

	/**
	 * Writes latest data to a file
	 *
	 * @param animation
	 */
	public final void write(boolean animation) {
		/*
		 * update average data, this is needed mainly to capture anything if not yet at
		 * steady state, true parameter to force sampling even if not yet in steady
		 * state
		 */
		// Starfish.averaging_module.sample(true);

		switch (output_type) {
		case FIELD:
			write2D(animation);
			break;
		case ONED:
			write1D(animation);
			break;
		case THREED:
			write3D(animation);
			break;
		case BOUNDARIES:
			writeBoundaries(animation);
			break;
		case PARTICLES:
			writeParticles(animation);
			break;
		case TRACE:
			Log.warning("write not supported for TRACE");
			break;
		}
	}

	/**
	 * Saves axisymmetric data as a revolved solid
	 *
	 * @param animation
	 */
	protected abstract void write3D(boolean animation);

	/**
	 *
	 * @param animation
	 */
	protected abstract void write2D(boolean animation);

	/**
	 *
	 */
	protected abstract void write1D(boolean animation);

	/**
	 *
	 */
	protected abstract void writeBoundaries(boolean animation);

	/**
	 *
	 */
	protected abstract void writeParticles(boolean animation);

	/**
	 *
	 */
	public abstract void writeTraces(ParticleTrace traces[]);

	/** placeholder for file close out operations to be overriden as needed */
	public void close() {

	}

	/**
	 * convenience method for one stop writing
	 * 
	 * @param file_name
	 * @param scalars
	 * @param vectors
	 * @param cell_data
	 */
	public final void saveMesh(String[] scalars, ArrayList<String[]> vectors, String[] cell_data) {
		init2D(scalars, vectors, cell_data, null);
		write();
		close();
	}

	/**
	 * splits file name into directory, prefix, and extension
	 * 
	 * @param name
	 * @return Array containing [file name with path, extension, name, prefix], for
	 *         instance "results/field.vts" returns
	 *         [results/field,.vts,results,field]
	 */
	protected String[] splitFileName(String name) {
		int p1, p2;

		/* find the last back or forward slash */
		for (p1 = name.length() - 1; p1 >= 0; p1--)
			if (name.charAt(p1) == '/' || name.charAt(p1) == '\\')
				break;

		for (p2 = name.length() - 1; p2 >= 0; p2--)
			if (name.charAt(p2) == '.')
				break;

		// if extension not found, set to end
		if (p2 <= 0)
			p2 = name.length() - 1;

		// if no path specified
		if (p1 <= 0)
			p1 = 0;

		String substr[] = { name.substring(0, p2), name.substring(p2), name.substring(0, p1),
				name.substring((p1 > 0) ? (p1 + 1) : 0, p2) };
		return substr;
	}

}
