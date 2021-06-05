/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 3/2019: Added support for binary output
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.core.boundaries.Boundary;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.diagnostics.ParticleTraceModule.ParticleTrace;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.Mesh;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;

/** writer for ASCII VTK files */
public class VTKWriter extends Writer {

	ByteOrder byte_order = ByteOrder.LITTLE_ENDIAN;

	public enum OutputFormat {
		ASCII, BINARY, APPENDED
	};

	OutputFormat output_format;
	ByteArrayOutputStream appendedData;
	String endianess; // for .xml header

	public VTKWriter(Element element) {
		super(element);
		String output_type_name = InputParser.getValue("output_format", element, "binary");
		try {
			this.output_format = OutputFormat.valueOf(output_type_name.toUpperCase());
		} catch (IllegalArgumentException e) {
			Log.error("Unrecognized <output_type>, expecting [ASCII,BINARY,APPENDED]");

		}

		endianess = "";
		if (output_format == OutputFormat.BINARY) {
			endianess = " byte_order=\"";
			if (byte_order == ByteOrder.BIG_ENDIAN)
				endianess += "BigEndian";
			else
				endianess += "LittleEndian";
			endianess += "\"";
		}
	}
	public VTKWriter(String file_path, OutputFormat output_format) {
		super(file_path);
		endianess = "";
		this.output_format = output_format;
		if (output_format == OutputFormat.BINARY) {
			endianess = " byte_order=\"";
			if (byte_order == ByteOrder.BIG_ENDIAN)
				endianess += "BigEndian";
			else
				endianess += "LittleEndian";
			endianess += "\"";
		}
	}

	/** writes 3D data (2D data rotated to 3D) */
	@Override
	public void write3D(boolean animation) {
		if (Starfish.getDomainType() == DomainType.XY)
			Log.warning("Write3D is not (yet) supported for DomainType XY");

		String format_name = output_type.name().toLowerCase();

		int part = 0;
		for (Mesh mesh : Starfish.getMeshList()) {
			// split out extension from the file name

			String substr[] = splitFileName(fileName);
			String name = substr[0] + "_" + mesh.getName();
			if (animation)
				name += String.format("_%06d", Starfish.getIt());
			name += substr[1];

			// add to collection but remove path since relative to pvd file
			substr = splitFileName(name);
			// collection.add(new CollectionData(time_step,part,substr[3]+substr[1]));
			collection.add(new CollectionData(Starfish.getIt(), part, substr[3] + substr[1]));

			PrintWriter pw = open(name);
			appendedData = new ByteArrayOutputStream();

			pw.println("<?xml version=\"1.0\"?>");

			pw.println("<VTKFile type=\"UnstructuredGrid\"" + endianess + ">");
			pw.println("<UnstructuredGrid>");
			pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfCells=\"%d\">\n", mesh.n_nodes * (theta_sections),
					mesh.n_cells * (theta_sections - 1));

			pw.println("<Points>");

			double pos[] = new double[mesh.ni * mesh.nj * theta_sections];
			int a = 0;

			for (int k = 0; k < theta_sections; k++) {
				// first and last slice is duplicated to simplify cell writing, hopefully
				// Paraview can deal with this fine
				double theta = k * 2 * Math.PI / (theta_sections - 1);
				for (int i = 0; i < mesh.ni; i++)
					for (int j = 0; j < mesh.nj; j++) {
						double x[] = mesh.pos(i, j);
						if (Starfish.getDomainType() == DomainType.RZ) {
							pos[a++] = Math.cos(theta) * x[0];
							pos[a++] = x[1];
							pos[a++] = Math.sin(theta) * x[0];
						} else {
							pos[a++] = x[0];
							pos[a++] = Math.cos(theta) * x[1];
							pos[a++] = Math.sin(theta) * x[1];
						}
						pw.println();
					}
			}
			outputDataArrayVec(pw, "pos", pos);

			// these get written out as ASCII for now
			pw.println("<Cells>");

			int con[] = new int[8 * (mesh.ni - 1) * (mesh.nj - 1) * (theta_sections - 1)];
			a = 0;
			for (int m = 0; m < theta_sections - 1; m++) {
				for (int j = 0; j < mesh.nj - 1; j++)
					for (int i = 0; i < mesh.ni - 1; i++) {
						int d1 = m * mesh.n_nodes;
						int d2 = (m + 1) * mesh.n_nodes;
						con[a++] = d1 + mesh.IJtoN(i, j);
						con[a++] = d1 + mesh.IJtoN(i + 1, j);
						con[a++] = d1 + mesh.IJtoN(i + 1, j + 1);
						con[a++] = d1 + mesh.IJtoN(i, j + 1);
						con[a++] = d2 + mesh.IJtoN(i, j);
						con[a++] = d2 + mesh.IJtoN(i + 1, j);
						con[a++] = d2 + mesh.IJtoN(i + 1, j + 1);
						con[a++] = d2 + mesh.IJtoN(i, j + 1);
					}
			}
			outputDataArrayScalar(pw, "connectivity", con);

			int offsets[] = new int[(theta_sections - 1) * mesh.n_cells];
			for (int c = 0; c < (theta_sections - 1) * mesh.n_cells; c++)
				offsets[c] = (c + 1) * 8;
			outputDataArrayScalar(pw, "offsets", offsets);

			int types[] = new int[(theta_sections - 1) * mesh.n_cells];
			for (int c = 0; c < (theta_sections - 1) * mesh.n_cells; c++)
				types[c] = 12; // VTK_HEXAHEDRON
			outputDataArrayScalar(pw, "types", types);

			pw.println("</Cells>");

			/*
			 * hard coded for now until I get some more robust way to output cell and vector
			 * data
			 */
			pw.println("<CellData>");

			for (String var : cell_data) {
				double data[][] = Starfish.domain_module.getField(mesh, var).getData();

				double data3c[] = new double[(theta_sections - 1) * (mesh.nj - 1) * (mesh.ni - 1)];

				for (int m = 0; m < theta_sections - 1; m++)
					for (int j = 0; j < mesh.nj - 1; j++)
						for (int i = 0; i < mesh.ni - 1; i++)
							data3c[a++] = data[i][j];

				outputDataArrayScalar(pw, var, data3c);
			}
			pw.println("</CellData>");

			// ***********
			pw.println("<PointData>");

			int data3i[] = new int[mesh.ni * mesh.nj * theta_sections];
			a = 0;
			for (int m = 0; m < theta_sections; m++)
				for (int j = 0; j < mesh.nj; j++)
					for (int i = 0; i < mesh.ni; i++)
						data3i[a++] = mesh.getNode(i, j).type.value();
			outputDataArrayScalar(pw, "type", data3i);

			for (String var : scalars) {
				/* make sure we have this variable */
				// if (!Starfish.output_module.validateVar(var)) continue;
				double data[][] = Starfish.domain_module.getField(mesh, var).getData();

				double data3[] = new double[mesh.ni * mesh.nj * theta_sections];
				a = 0;

				for (int m = 0; m < theta_sections; m++)
					for (int j = 0; j < mesh.nj; j++)
						for (int i = 0; i < mesh.ni; i++)
							data3[a++] = data[i][j];

				outputDataArrayScalar(pw, var, data3);
			}

			for (String[] vars : vectors) {
				/* make sure we have this variable */
				// if (!Starfish.output_module.validateVar(var)) continue;
				double data1[][] = Starfish.domain_module.getField(mesh, vars[0]).getData();
				double data2[][] = Starfish.domain_module.getField(mesh, vars[1]).getData();
				double vec3[] = new double[mesh.ni * mesh.nj * theta_sections * 3];
				a = 0;

				for (int m = 0; m < theta_sections; m++)
					for (int j = 0; j < mesh.nj; j++)
						for (int i = 0; i < mesh.ni; i++) {
							vec3[a++] = data1[i][j];
							vec3[a++] = data2[i][j];
							vec3[a++] = 0;
						}
				outputDataArrayVec(pw, "" + vars[0] + "_" + vars[1] + "", vec3);
			}

			pw.println("</PointData>");

			pw.println("</Piece>");

			pw.println("</UnstructuredGrid>");
			outputAppendedData(pw);
			pw.println("</VTKFile>");
			/* save output file */
			pw.close();

			part++;
		}

		/* write the collection file */
		writeCollection(animation);

	}

	public enum VTK_Type {
		RECT, STRUCT, POLY
	};

	/**
	 * saves 2D data in VTK ASCII format, supports .vts, .vtr, and .vtp
	 * 
	 * @param animation if true, will open new file for each save
	 */
	@Override
	public void write2D(boolean animation) {
		int part = 0;
		for (Mesh mesh : Starfish.getMeshList()) {
			// split out extension from the file name
			String substr[] = splitFileName(fileName);
			String name = substr[0] + "_" + mesh.getName();
			if (animation)
				name += String.format("_%06d", Starfish.getIt());
			name += substr[1];

			VTK_Type vtkType;

			if (substr[1].equalsIgnoreCase(".vts"))
				vtkType = VTK_Type.STRUCT;
			else if (substr[1].equalsIgnoreCase(".vtr"))
				vtkType = VTK_Type.RECT;
			else {
				Log.warning("Unrecognized VTK data type " + substr[1] + ", assuming VTS");
				vtkType = VTK_Type.STRUCT;
			}

			// add to collection but remove path since relative to pvd file
			substr = splitFileName(name);
			// collection.add(new CollectionData(time_step,part,substr[3]+substr[1]));
			collection.add(new CollectionData(Starfish.getIt(), part, substr[3] + substr[1]));

			write2DToFile(mesh, Starfish.options.wd + name, vtkType);

			part++;
		}

		/* write the collection file */
		writeCollection(animation);
	}

	/**
	 * @param mesh Mesh to write
	 * @param outputFilePath absolute path to output file
	 * @param vtk_type
	 */
	public void write2DToFile(Mesh mesh, String outputFilePath, VTK_Type vtk_type) {
		PrintWriter pw = openAbsolutePath(outputFilePath);
		appendedData = new ByteArrayOutputStream();

		pw.println("<?xml version=\"1.0\"?>");

		String close_tag = "";

		if (vtk_type == VTK_Type.STRUCT) {
			pw.println("<VTKFile type=\"StructuredGrid\"" + endianess + ">");
			pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, mesh.nj - 1);
			pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, mesh.nj - 1);
			close_tag = "</StructuredGrid>";
		} else if (vtk_type == VTK_Type.RECT) {
			pw.println("<VTKFile type=\"RectilinearGrid\"" + endianess + ">");
			pw.printf("<RectilinearGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, mesh.nj - 1);
			pw.printf("<Piece Extent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, mesh.nj - 1);
			close_tag = "</RectilinearGrid>";
		}

		int a = 0;

		if (vtk_type == VTK_Type.STRUCT) {
			pw.println("<Points>");

			double pos[] = new double[mesh.ni * mesh.nj * 3];
			a = 0;
			for (int j = 0; j < mesh.nj; j++) {
				for (int i = 0; i < mesh.ni; i++) {
					double x[] = mesh.pos(i, j);
					pos[a++] = x[0];
					pos[a++] = x[1];
					pos[a++] = 0;
				}
			}
			outputDataArrayVec(pw, "pos", pos);
			pw.println("</Points>");
		} else if (vtk_type == VTK_Type.RECT) {
			pw.println("<Coordinates>");
			double pos_x[] = new double[mesh.ni];
			double pos_y[] = new double[mesh.nj];
			double pos_z[] = new double[1];
			for (int i = 0; i < mesh.ni; i++) {
				pos_x[i] = mesh.pos1(i, 0);
			}
			for (int j = 0; j < mesh.nj; j++) {
				pos_y[j] = mesh.pos1(j, 0);
			}
			pos_z[0] = 0;
			outputDataArrayScalar(pw, "x", pos_x);
			outputDataArrayScalar(pw, "y", pos_y);
			outputDataArrayScalar(pw, "z", pos_z);
			pw.println("</Coordinates>");
		}

		/*
		 * hard coded for now until I get some more robust way to output cell and vector
		 * data
		 */
		pw.println("<CellData>");

		for (String var : cell_data) {
			double data[][] = Starfish.domain_module.getField(mesh, var).getData();
			double data_c[] = new double[(mesh.nj - 1) * (mesh.ni - 1)];
			a = 0;
			for (int j = 0; j < mesh.nj - 1; j++) {
				for (int i = 0; i < mesh.ni - 1; i++) {
					data_c[a++] = data[i][j];
				}
			}
			outputDataArrayScalar(pw, var, data_c);
		}
		pw.println("</CellData>");

		pw.println("<PointData>");
		int type[] = new int[mesh.ni * mesh.nj];
		a = 0;
		for (int j = 0; j < mesh.nj; j++) {
			for (int i = 0; i < mesh.ni; i++) {
				type[a++] = mesh.getNode(i, j).type.value();
			}
		}
		outputDataArrayScalar(pw, "type", type);

		for (String var : scalars) {
			/* make sure we have this variable */
			double data3[] = new double[mesh.ni * mesh.nj];
			a = 0;
			double data[][] = Starfish.domain_module.getField(mesh, var).getData();
			for (int j = 0; j < mesh.nj; j++) {
				for (int i = 0; i < mesh.ni; i++) {
					data3[a++] = data[i][j];
				}
			}
			outputDataArrayScalar(pw, var, data3);
		}

		for (String[] vars : vectors) {
			double data1[][] = Starfish.domain_module.getField(mesh, vars[0]).getData();
			double data2[][] = Starfish.domain_module.getField(mesh, vars[1]).getData();
			double vec[] = new double[mesh.ni * mesh.nj * 3];
			a = 0;
			for (int j = 0; j < mesh.nj; j++) {
				for (int i = 0; i < mesh.ni; i++) {
					vec[a++] = data1[i][j];
					vec[a++] = data2[i][j];
					vec[a++] = 0;
				}
			}

			outputDataArrayVec(pw, "" + vars[0] + "_" + vars[1] + "", vec);
		}

		pw.println("</PointData>");

		pw.println("</Piece>");

		pw.println(close_tag);

		outputAppendedData(pw);

		pw.println("</VTKFile>");
		/* save output file */
		pw.close();
	}


	/* colletor for generated files to add to collection */
	class CollectionData {
		int time_step;
		int part;
		String file_name;

		CollectionData(int time_step, int part, String file_name) {
			this.time_step = time_step;
			this.part = part;
			this.file_name = file_name;
		}
	}

	protected ArrayList<CollectionData> collection = new ArrayList();

	/**
	 * Writes the .pvd file for paraview
	 * 
	 * @param animation
	 */
	protected void writeCollection(boolean animation) {
		String substr[] = splitFileName(fileName);
		String pvd_name = substr[0];
		if (animation)
			pvd_name += "_anim";
		pvd_name += ".pvd";

		PrintWriter pw = open(pvd_name);

		pw.println("<?xml version=\"1.0\"?>");
		pw.println("<VTKFile type=\"Collection\" version=\"0.1\">");
		// byte_order = "LittleEndian" compressor="vtkZLibDataCompresssor"
		pw.println("<Collection>");
		for (CollectionData cd : collection) {
			pw.printf("<DataSet timestep=\"%d\" group=\"\" part=\"%d\" file=\"%s\" />\n", cd.time_step, cd.part,
					cd.file_name);
		}
		pw.println("</Collection>");
		pw.println("</VTKFile>");
		pw.close();
	}

	private String file_name_1d;
	private int time_data_it0; // time step of created file

	/**
	 * Saves data along a single I/J grid line on a single mesh Data can be saved as
	 * individual files or as a single "time data" 2D mesh, with y-axis being time
	 */
	@Override
	public void write1D(boolean animation) {

		/* first fill the time data array with the latest data */
		set1Ddata(time_data_current_line);

		/* TODO: add collection support */

		/*
		 * set file name, done using a class variable so we can overwrite prior time
		 * data
		 */
		if (file_name_1d == null || !animation || (animation && time_data_current_line == 0)) {
			String substr[] = splitFileName(fileName);
			file_name_1d = substr[0];
			time_data_it0 = Starfish.getIt();
			if (animation)
				file_name_1d += String.format("_%06d", time_data_it0);
			file_name_1d += substr[1];

		}

		/*
		 * write if not doing animation, or if we have filled our time data for proper
		 * 1D output, number of lines is 1 so we output every time
		 */
		if (!animation || (animation && (time_data_current_line % time_data_write_skip == 0
				|| time_data_current_line == time_data_lines - 1))) {
			PrintWriter pw = open(file_name_1d);
			appendedData = new ByteArrayOutputStream();

			Mesh mesh = output_mesh;

			pw.println("<?xml version=\"1.0\"?>");
			pw.println("<VTKFile type=\"StructuredGrid\"" + endianess + ">");
			if (dim == Dim.I) {
				pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", time_data_lines - 1, mesh.nj - 1);
			}
			if (dim == Dim.J) {
				pw.printf("<StructuredGrid WholeExtent=\"0 %d 0 %d 0 0\">\n", mesh.ni - 1, time_data_lines - 1);
			} else
				Log.error("Unsupported dimension in write1D");

			pw.println("<Points>");
			int a = 0;

			if (dim == Dim.I) {
				double pos[] = new double[time_data_lines * mesh.nj * 3];
				a = 0;
				for (int l = 0; l < time_data_lines; l++)
					for (int j = 0; j < mesh.nj; j++) {
						double x[] = mesh.pos(index, j);
						/* replace non-varying dimension with time */
						if (time_data_lines > 1)
							x[0] = time_data_time[l] * time_data_time_scale;
						pos[a++] = x[0];
						pos[a++] = x[1];
						pos[a++] = 0;
					}
				outputDataArrayVec(pw, "pos", pos);
			} else {
				double pos[] = new double[time_data_lines * mesh.ni * 3];
				a = 0;
				for (int l = 0; l < time_data_lines; l++)
					for (int i = 0; i < mesh.ni; i++) {
						double x[] = mesh.pos(i, index);
						if (time_data_lines > 1)
							x[1] = time_data_time[l] * time_data_time_scale;
						pos[a++] = x[0];
						pos[a++] = x[1];
						pos[a++] = 0;
					}
				outputDataArrayVec(pw, "pos", pos);
			}
			pw.println("</Points>");

			int var = 0;
			int nk;
			if (dim == Dim.I)
				nk = mesh.nj;
			else
				nk = mesh.ni;

			/* cell data */
			pw.println("<CellData>");
			for (String var_name : cell_data) {
				double data_c[] = new double[time_data_lines * (nk - 1)];
				a = 0;
				for (int l = 0; l < time_data_lines; l++)
					for (int k = 0; k < nk - 1; k++)
						data_c[a++] = time_data[l][var][k];
				outputDataArrayScalar(pw, var_name, data_c);
				var++;
			}
			pw.println("</CellData>");

			/* point data */
			pw.println("<PointData>");

			int pdata_i[] = new int[time_data_lines * nk];
			a = 0;

			/* node type - don't bother averaging this one */
			for (int l = 0; l < time_data_lines; l++)
				for (int k = 0; k < nk; k++) {
					if (dim == Dim.I)
						pdata_i[a++] = mesh.getNode(index, k).type.value();
					else
						pdata_i[a++] = mesh.getNode(k, index).type.value();
				}
			outputDataArrayScalar(pw, "type", pdata_i);

			/* actual scalars */
			for (String var_name : scalars) {
				double pdata[] = new double[time_data_lines * nk];
				a = 0;
				for (int l = 0; l < time_data_lines; l++)
					for (int k = 0; k < nk; k++)
						pdata[a++] = time_data[l][var][k];
				outputDataArrayScalar(pw, var_name, pdata);
				var++;
			}

			/* vectors */
			for (String[] var_name : vectors) {
				double vdata[] = new double[time_data_lines * nk * 3];
				a = 0;
				for (int l = 0; l < time_data_lines; l++)
					for (int k = 0; k < nk; k++) {
						vdata[a++] = time_data[l][var][k];
						vdata[a++] = time_data[l][var + 1][k];
					}
				outputDataArrayVec(pw, "" + var_name[0] + "_" + var_name[1] + "", vdata);
				var += 2;
			}

			pw.println("</PointData>");
			pw.println("</StructuredGrid>");
			outputAppendedData(pw);
			pw.println("</VTKFile>");
			/* save output file */
			pw.close();
		}

		/* rewind - needs to be here since above loop checking current line */
		time_data_current_line++;
		if (time_data_current_line >= time_data_lines) {
			time_data_current_line = 0; // rewind

			/* clear data */
			int num_vars = time_data[0].length;
			int nk = time_data[0][0].length;
			for (int l = 0; l < time_data_lines; l++)
				for (int v = 0; v < num_vars; v++)
					for (int k = 0; k < nk; k++)
						time_data[l][v][k] = 0;
		}

	}

	/* fills one line in time data array */
	void set1Ddata(int l) {
		int part = 0;
		// split out extension from the file name
		Mesh mesh = output_mesh;

		int var = 0;

		double line[][] = time_data[l];
		time_data_time[l] = Starfish.time_module.getTime();
		/* fill future time with our best estimate */
		if (l > 1) {
			double dt = time_data_time[l] - time_data_time[l - 1];
			for (int l2 = l + 1; l2 < this.time_data_lines; l2++)
				time_data_time[l2] = time_data_time[l] + dt * (l2 - l);
		}

		for (String var_name : cell_data) {
			double data[][] = Starfish.domain_module.getField(mesh, var_name).getData();

			if (dim == Dim.I) {
				for (int j = 0; j < mesh.nj - 1; j++) {
					double val = data[index][j];

					if (ave1d) {
						val = 0;
						for (int i = 0; i < mesh.ni - 1; i++)
							val += data[i][j];
						val /= (mesh.ni - 1);
					}
					line[var][j] = val;
				}
			} else {
				for (int i = 0; i < mesh.ni - 1; i++) {
					double val = data[i][index];
					if (ave1d) {
						val = 0;
						for (int j = 0; j < mesh.nj - 1; j++)
							val += data[i][j];
						val /= (mesh.nj - 1);
					}
					line[var][i] = val;
				}
			}
			var++;
		}

		/* point data */
		for (String var_name : scalars) {
			/* make sure we have this variable */
			double data[][] = Starfish.domain_module.getField(mesh, var_name).getData();
			if (dim == Dim.I) {
				for (int j = 0; j < mesh.nj; j++) {
					double val = data[index][j];
					if (ave1d) {
						val = 0;
						for (int i = 0; i < mesh.ni; i++)
							val += data[i][j];
						val /= mesh.ni;
					}
					line[var][j] = val;
				}
			} else {
				for (int i = 0; i < mesh.ni; i++) {
					double val = data[i][index];
					if (ave1d) {
						val = 0;
						for (int j = 0; j < mesh.nj; j++)
							val += data[i][j];
						val /= mesh.nj;
					}
					line[var][i] = val;
				}
			}

			var++;
		}

		for (String[] vars : vectors) {
			/* make sure we have this variable */
			double data1[][] = Starfish.domain_module.getField(mesh, vars[0]).getData();
			double data2[][] = Starfish.domain_module.getField(mesh, vars[1]).getData();

			if (dim == Dim.I) {
				for (int j = 0; j < mesh.nj; j++) {
					double v1 = data1[index][j];
					double v2 = data2[index][j];
					if (ave1d) {
						v1 = 0;
						v2 = 0;
						for (int i = 0; i < mesh.ni; i++) {
							v1 += data1[i][j];
							v2 += data2[i][j];
						}
						v1 /= mesh.ni;
						v2 /= mesh.ni;
					}
					line[var][j] = v1;
					line[var + 1][j] = v2;
				}
			} else {
				for (int i = 0; i < mesh.ni; i++) {
					double v1 = data1[i][index];
					double v2 = data2[i][index];
					if (ave1d) {
						v1 = 0;
						v2 = 0;
						for (int j = 0; j < mesh.nj; j++) {
							v1 += data1[i][j];
							v2 += data2[i][j];
						}
						v1 /= mesh.nj;
						v2 /= mesh.nj;
					}
					line[var][i] = v1;
					line[var + 1][i] = v2;
				}
			}

			var += 2;
		}
	}

	/**
	 * Writes the "surface" file
	 *
	 */
	@Override
	public void writeBoundaries(boolean animation) {
		/* count number of points */
		int num_points = 0;
		int num_lines = 0;
		int num_polys = 0;
		ArrayList<Boundary> bl = Starfish.getBoundaryList();

		for (Boundary boundary : bl) {
			num_points += boundary.numPoints();
			num_lines += boundary.numPoints() - 1;
		}
		
		num_points *= theta_sections;
		if (theta_sections>1) {
			num_polys = num_lines*theta_sections;
			num_lines = 0;
		}
		
		DomainType dt = Starfish.getDomainType();

		PrintWriter pw = open(fileName);
		appendedData = new ByteArrayOutputStream();

		pw.println("<?xml version=\"1.0\"?>");
		pw.println("<VTKFile type=\"PolyData\"" + endianess + ">");
		pw.println("<PolyData>");
		pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfVerts=\"0\" "
				+ "NumberOfLines=\"%d\" NumberOfStrips=\"0\" NumberOfPolys=\"%d\">\n", 
				num_points, num_lines, num_polys);

		pw.println("<Points>");
		double vec[] = new double[num_points * 3];
		int a = 0;
		
		for (int k=0;k<theta_sections;k++)
		{
			double theta = 2*Math.PI*k/((double)theta_sections);	// so that k=theta_sections would close the circle
			for (Boundary boundary : bl)
				for (int i = 0; i < boundary.numPoints(); i++) {
					double x[] = boundary.getPoint(i);
					if (dt==DomainType.XY)  {
						vec[a++] = x[0];
						vec[a++] = x[1];
						vec[a++] = 0;
					}
					else if (dt==DomainType.RZ) {
						vec[a++] = x[0]*Math.cos(theta);
						vec[a++] = x[0]*Math.sin(theta);
						vec[a++] = x[1];
					}
					else if (dt==DomainType.ZR) {
						vec[a++] = x[0];
						vec[a++] = x[1]*Math.cos(theta);
						vec[a++] = x[1]*Math.sin(theta);
						
					}
				}
		}
		outputDataArrayVec(pw, "pos", vec);
		pw.println("</Points>");

		if (num_lines>0) {
			pw.println("<Lines>");
			int con[] = new int[num_points * 2];
			a = 0;
			int p0 = 0;
			for (Boundary boundary : bl) {
				for (int i = 0; i < boundary.numPoints() - 1; i++) {
					con[a++] = p0 + i;
					con[a++] = p0 + i + 1;
				}
				p0 += boundary.numPoints();
			}
			outputDataArrayScalar(pw, "connectivity", con);
	
			int off[] = new int[num_points];
			p0 = 2;
			a = 0;
			for (Boundary boundary : bl) {
				for (int i = 0; i < boundary.numPoints() - 1; i++, p0 += 2)
					off[a++] = p0;
			}
			outputDataArrayScalar(pw, "offsets", con);
			pw.println("</Lines>");
		}
		else {   /*output polygons*/
			pw.println("<Polys>");
			int con[] = new int[num_polys*4];
			a = 0;
			int num_points_line = num_points/theta_sections;
			for (int k=0;k<theta_sections;k++) {
				int p0 = 0;
				
				for (Boundary boundary : bl) {					
					for (int i = 0; i < boundary.numPoints() - 1; i++) {
						int i1 = k*num_points_line +i;
						int i2 = ((k<theta_sections-1)?(k+1):0)*num_points_line+i;
						
						con[a++] = p0+i1;
						con[a++] = p0+i1+1;					
						con[a++] = p0+i2+1;
						con[a++] = p0+i2;					
						
					}
					p0 += boundary.numPoints();
				}
			}
			outputDataArrayScalar(pw, "connectivity", con);
	
			a = 0;
			int off[] = new int[num_polys];
			for (int i=0;i<num_polys;i++)
				off[a++] = 4*(i+1);
			outputDataArrayScalar(pw, "offsets", off);
			pw.println("</Polys>");
		}	
		
	
		/* normals */
		pw.println("<CellData>");
		double vec_c[] = new double[num_points * 3];
		a = 0;		
		for (int k=0;k<theta_sections;k++) {
			double theta = 2*Math.PI*k/((double)theta_sections);	// so that k=theta_sections would close the circle

			for (Boundary boundary : bl)
				for (int i = 0; i < boundary.numPoints() - 1; i++) {
					double norm[] = boundary.normal(i + 0.5);					
					if (dt==DomainType.XY)  {
						vec_c[a++] = norm[0];
						vec_c[a++] = norm[1];
						vec_c[a++] = 0;
					}
					else if (dt==DomainType.RZ) {
						vec_c[a++] = norm[0]*Math.cos(theta);
						vec_c[a++] = norm[0]*Math.sin(theta);
						vec_c[a++] = norm[1];
					}
					else if (dt==DomainType.ZR) {
						vec_c[a++] = norm[1]*Math.cos(theta);
						vec_c[a++] = norm[1]*Math.sin(theta);
						vec_c[a++] = norm[0];											
					}					
				}
		}
		outputDataArrayVec(pw, "normals", vec_c);

		int data_c[] = new int[num_points];
		a = 0;
		for (int k=0;k<theta_sections;k++) {
			for (Boundary boundary : bl)
				for (int i = 0; i < boundary.numPoints() - 1; i++) {
					data_c[a++] = boundary.getType().value();
				}
		}
		outputDataArrayScalar(pw, "type", data_c);

		a = 0;
		for (int k=0;k<theta_sections;k++) {
			for (int b = 0; b < bl.size(); b++) {
				Boundary boundary = bl.get(b);
				for (int i = 0; i < boundary.numPoints() - 1; i++) {
					data_c[a++] = b;
				}
			}
		}
		outputDataArrayScalar(pw, "boundary_id", data_c);

		pw.println("</CellData>");

		/* data */
		pw.println("<PointData>");
		double data[] = new double[num_points];
		a = 0;
		for (int k=0;k<theta_sections;k++) {
			/* first save node area */
			for (Boundary boundary : bl) {
				for (int i = 0; i < boundary.numPoints() - 1; i++)
					data[a++] = boundary.nodeArea(i);
			}
		}
		outputDataArrayScalar(pw, "area", data);

		for (String var : scalars) {
			/* make sure we have this variable */
			if (!Starfish.output_module.validateVar(var))
				continue;
			a = 0;

			for (int k=0;k<theta_sections;k++) {
				for (Boundary boundary : bl) {
					double bdata[] = Starfish.boundary_module.getField(boundary, var).getData();
					for (int i = 0; i < boundary.numPoints(); i++)
						data[a++] = bdata[i];
				}
			}
			outputDataArrayScalar(pw, var, data);
		}

		pw.println("</PointData>");

		pw.println("</Piece>");
		pw.println("</PolyData>");
		outputAppendedData(pw);
		pw.println("</VTKFile>");

		/* save output file */
		pw.flush();
	}

	/**
	 *
	 */
	@Override
	protected void writeParticles(boolean animation) {
		ArrayList<Particle> parts = new ArrayList();
		KineticMaterial mat = Starfish.getKineticMaterial(mat_name);
		if (mat == null) {
			Log.warning("Material " + mat_name + " is not a kinetic material");
			return;
		}

		double prob = (double) particle_count / mat.getNp();
		for (Mesh mesh : Starfish.getMeshList()) {
			Iterator<Particle> it = mat.getIterator(mesh);
			while (it.hasNext()) {
				Particle part = it.next();
				if (prob >= 1.0 || Starfish.rnd() < prob)
					parts.add(part);
			}
		}

		String substr[] = splitFileName(fileName);
		String name = substr[0];
		if (animation)
			name += String.format("_%06d", Starfish.getIt());
		name += substr[1];
		PrintWriter pw = open(name);
		appendedData = new ByteArrayOutputStream();

		pw.println("<?xml version=\"1.0\"?>");
		pw.println("<VTKFile type=\"PolyData\"" + endianess + ">");
		pw.println("<PolyData>");
		pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfVerts=\"0\" "
				+ "NumberOfLines=\"0\" NumberOfStrips=\"0\" NumberOfPolys=\"0\">\n", parts.size());

		pw.println("<Points>");
		double vec[] = new double[parts.size() * 3];
		int a = 0;

		for (int i = 0; i < parts.size(); i++) {
			Particle part = parts.get(i);
			double pos[] = { part.pos[0], part.pos[1], 0 };

			if (rotate) {
				switch (Starfish.getDomainType()) {
				case RZ:
					pos[0] = part.pos[0] * Math.cos(part.pos[2]);
					pos[1] = part.pos[1];
					pos[2] = part.pos[0] * Math.sin(part.pos[2]);
					break;
				case ZR:
					pos[0] = part.pos[0];
					pos[1] = part.pos[1] * Math.cos(part.pos[2]);
					pos[2] = part.pos[1] * Math.sin(part.pos[2]);
					break;
				default:
					break;
				}
			}
			vec[a++] = pos[0];
			vec[a++] = pos[1];
			vec[a++] = pos[2];
		}
		outputDataArrayVec(pw, "pos", vec);

		pw.println("</Points>");

		/* data */
		pw.println("<PointData>");
		a = 0;
		/* save particle velocities */
		for (int i = 0; i < parts.size(); i++) {
			Particle part = parts.get(i);
			vec[a++] = part.vel[0];
			vec[a++] = part.vel[1];
			vec[a++] = part.vel[2];
		}
		outputDataArrayVec(pw, "velocity", vec);

		double s[] = new double[parts.size()];
		a = 0;
		for (int i = 0; i < parts.size(); i++) {
			Particle part = parts.get(i);
			s[a++] = part.radius;
		}
		outputDataArrayScalar(pw, "radius", s);

		pw.println("</PointData>");

		pw.println("</Piece>");
		pw.println("</PolyData>");
		outputAppendedData(pw);
		pw.println("</VTKFile>");

		/* save output file */
		pw.flush();
	}

	/**
	 * writes particle trace
	 * 
	 * @param particles
	 * @param time_steps
	 */
	@Override
	public void writeTraces(ParticleTrace traces[]) {
		if (traces.length==0)
			return;

		//count total number of points
		int num_points = 0;
		for (ParticleTrace trace:traces) 
			num_points+=trace.samples.size();
		
		PrintWriter pw = open(fileName);
		appendedData = new ByteArrayOutputStream();

		pw.println("<?xml version=\"1.0\"?>");
		pw.println("<VTKFile type=\"PolyData\"" + endianess + ">");
		pw.println("<PolyData>");
		pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfVerts=\"0\" "
						+ "NumberOfLines=\"%d\" NumberOfStrips=\"0\" NumberOfPolys=\"0\">\n",
				num_points, traces.length);

		double pos[] = new double[num_points*3];
		double vel[] = new double[num_points*3];
		int ids[] = new int[num_points];
		int con[] = new int[num_points];
		int time_steps[] = new int[num_points];
		
		int a1 = 0;
		int a3 = 0;

		for (ParticleTrace trace:traces) {	
			for (Particle part:trace.samples) {
				DomainType domt = Starfish.getDomainType();
				
				if (rotate && domt == DomainType.RZ) {
					pos[a3] = part.pos[0] * Math.cos(part.pos[2]);
					pos[a3+1] = part.pos[1];
					pos[a3+2] = -part.pos[0] * Math.sin(part.pos[2]);
				}
				else if (rotate && domt == DomainType.ZR) {
					pos[a3] = part.pos[0];
					pos[a3+1] = part.pos[1] * Math.cos(part.pos[2]);
					pos[a3+2] = part.pos[1] * Math.sin(part.pos[2]);
				}
				else {
					pos[a3] = part.pos[0];
					pos[a3+1] = part.pos[1];
					pos[a3+2] = part.pos[2];
				}
				
				//TODO: this is likely incorrect for axisymmetric
				vel[a3] = part.vel[0];
				vel[a3+1] = part.vel[1];
				vel[a3+2] = part.vel[2];
				
				ids[a1] = part.id;
				con[a1] = a1;		//connectivity are just the node indexes
				a1+=1;	//update indexes
				a3+=3;								
			}
		}
		
		//also collect time steps
		a1=0;
		for (ParticleTrace trace:traces) {	
			for (int ts:trace.time_steps) {
				time_steps[a1++] = ts;
			}
		}
		
		
		pw.println("<Points>");
		outputDataArrayVec(pw, "pos", pos);
		pw.println("</Points>");

		pw.println("<Lines>");
		
		outputDataArrayScalar(pw, "connectivity", con);

		int offsets[] = new int[traces.length];
		for (int i=0;i<traces.length;i++) {
			ParticleTrace trace = traces[i];
			if (i==0) offsets[i] = trace.samples.size();
			else offsets[i] = offsets[i-1]+trace.samples.size();
		}
		outputDataArrayScalar(pw, "offsets", offsets);

		pw.println("</Lines>");

		/* data */
		pw.println("<PointData>");

		outputDataArrayVec(pw, "vel", vel);
		outputDataArrayScalar(pw, "time_step", time_steps);
		outputDataArrayScalar(pw, "part_id", ids);

		pw.println("</PointData>");

		pw.println("</Piece>");
		pw.println("</PolyData>");
		outputAppendedData(pw);
		pw.println("</VTKFile>");

		/* save output file */
		pw.flush();
	}

	/***** DATA WRITERS ****************/
	/* writes out a data array buffer in binary or ascii format */
	void outputDataArray(PrintWriter pw, String var_name, double data[], int num_comps) {
		int ni = data.length;
		String nc_string = "NumberOfComponents=\"" + num_comps + "\"";

		if (output_format == OutputFormat.BINARY || output_format == OutputFormat.APPENDED) {
			int num_bytes = Double.BYTES * ni;
			ByteBuffer bb = ByteBuffer.allocate(num_bytes + Long.BYTES);
			bb.order(byte_order);
			bb.putInt(num_bytes);
			for (int i = 0; i < ni; i++)
				bb.putDouble(data[i]);

			if (output_format == OutputFormat.BINARY) {
				String encoded = Base64.getEncoder().encodeToString(bb.array());
				pw.println(
						"<DataArray Name=\"" + var_name + "\" type=\"Float64\" " + nc_string + " format=\"binary\">");
				pw.println(encoded);
				pw.println("</DataArray>");
			} else { // save raw bites in appended section
				pw.println("<DataArray Name=\"" + var_name + "\" type=\"Float64\" " + nc_string
						+ " format=\"appeneded\" offset=\"" + appendedData.size() + "\">");

				try {
					appendedData.write(bb.array());
				} catch (IOException ex) {
					Log.error("Error in appended data write");
				}
				pw.println("</DataArray>");
			}
		} else if (output_format == OutputFormat.ASCII) {
			pw.println("<DataArray Name=\"" + var_name + "\" type=\"Float64\" " + nc_string + " format=\"ascii\">");
			for (int i = 0; i < ni; i++)
				pw.printf("%g ", (data[i]));
			pw.println("\n</DataArray>");
		}
	}

	/* writes out a data buffer in binary or ascii format, integer version */
	void outputDataArray(PrintWriter pw, String var_name, int data[], int num_comps) {
		int ni = data.length;
		String nc_string = "NumberOfComponents=\"" + num_comps + "\"";

		if (output_format == OutputFormat.BINARY || output_format == OutputFormat.APPENDED) {
			int num_bytes = Integer.BYTES * ni;
			ByteBuffer bb = ByteBuffer.allocate(num_bytes + Long.BYTES);
			bb.order(byte_order);
			bb.putInt(num_bytes);
			for (int i = 0; i < ni; i++)
				bb.putInt(data[i]);

			if (output_format == OutputFormat.BINARY) {
				String encoded = Base64.getEncoder().encodeToString(bb.array());
				pw.println("<DataArray Name=\"" + var_name + "\" type=\"Int32\" " + nc_string + " format=\"binary\">");
				pw.println(encoded);
				pw.println("</DataArray>");
			} else { // save raw bites in appended section
				pw.println("<DataArray Name=\"" + var_name + "\" type=\"Int32\" " + nc_string
						+ " format=\"appeneded\" offset=\"" + appendedData.size() + "\">");

				try {
					appendedData.write(bb.array());
				} catch (IOException ex) {
					Log.error("Error in appended data write");
				}
				pw.println("</DataArray>");
			}
		} else if (output_format == OutputFormat.ASCII) {
			pw.println("<DataArray Name=\"" + var_name + "\" type=\"Int32\" " + nc_string + " format=\"ascii\">");
			for (int i = 0; i < ni; i++)
				pw.printf("%d ", (data[i]));
			pw.println("\n</DataArray>");
		}
	}

	/* convenience functions */
	void outputDataArrayScalar(PrintWriter pw, String var_name, double data[]) {
		outputDataArray(pw, var_name, data, 1);
	}

	void outputDataArrayScalar(PrintWriter pw, String var_name, int data[]) {
		outputDataArray(pw, var_name, data, 1);
	}

	void outputDataArrayVec(PrintWriter pw, String var_name, double data[]) {
		outputDataArray(pw, var_name, data, 3);
	}

	void outputDataArrayVec(PrintWriter pw, String var_name, int data[]) {
		outputDataArray(pw, var_name, data, 3);
	}

	/* writes out the binary appended data */
	void outputAppendedData(PrintWriter pw) {
		if (output_format == OutputFormat.APPENDED) {
			try {
				pw.println("<AppendedData encoding=\"raw\">");
				pw.flush(); // write out the file;
				output_stream.write('_');
				output_stream.write(appendedData.toByteArray());
				pw.println("\n</AppendedData>");
			} catch (IOException ex) {
				Log.warning("Error writing binary data to the output file");
			}
		}
	}

}
