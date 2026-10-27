package starfish.core.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldManager2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.QuadrilateralMesh;
import starfish.core.domain.UniformMesh;

public class CSVReader extends Reader {
	/**
	 *
	 * @param file_name
	 * @param element
	 */
	public CSVReader(String file_name, Element element) {
		super(element);

		uniform = InputParser.getBoolean("uniform", element, false); // if true, will create Cartesian mesh for faster interpolation
		
		int dims[] = InputParser.getIntList("dims", element, new int[] {ni,nj});
		ni = dims[0];
		nj = dims[1];

		this.file_name = file_name;
		try {
			sc = new Scanner(new FileInputStream(file_name));
		} catch (FileNotFoundException ex) {
			Log.error("file not found " + file_name);
		}
	}

	protected String file_name;			// input file name

	protected Scanner sc = null;	// scanner used for file reading
	boolean uniform; 				// use uniform mesh for interpolation?
	int ni = -1, nj = -1;			// mesh dimensions

	@Override
	public void parse(String coord_vars[], String field_vars[]) {
		// fields
		int vi = -1, vj = -1;
		int fv_index[] = new int[field_vars.length];

		// init fv_index to -1
		for (int i = 0; i < fv_index.length; i++)
			fv_index[i] = -1;

		// open file
		Log.log("Reading " + file_name);
		
		boolean first_line = true;
		int i = 0, j = 0;
		
		// data arrays
		double IPOS[][] = null;
		double JPOS[][] = null;
		double FVAR[][][] = null;

		// parse file
		while (sc.hasNextLine()) {
			Scanner line_sc = new Scanner(sc.nextLine().trim());
			String line = line_sc.next().trim();
			
			if (line.charAt(0)=='#') {
				
				// look for mesh dimensions on the first line
				if (first_line) {
					String pieces[];
					try {
						pieces =  line.substring(1).split(",");
						ni = Integer.parseInt(pieces[0]);
						nj = Integer.parseInt(pieces[1]);
						Log.debug("Read in mesh dimensions from the header comment");				
					}
					catch (Exception e){
						
					}
				}
				continue;		// skip comments
				
			}
			
			// process header
			if (first_line) {
				
				String vars[] = line.split(",");
				int var_index = 0;
				
				for (String var_name : vars) {
					/* is this one of the coordinates? */
					if (coord_vars[0].equalsIgnoreCase(var_name))
						vi = var_index;
					else if (coord_vars[1].equalsIgnoreCase(var_name))
						vj = var_index;
					else // do we have this variable in our list? //
						for (int v = 0; v < field_vars.length; v++) {
							if (field_vars[v].equalsIgnoreCase(var_name)) {
								fv_index[v] = var_index;
								Log.debug("Reading " + field_vars[v] + " from index " + var_index);
							}
						}
					var_index++;   // increment index
				}

				// make sure we found all the variables
				for (int v = 0; i < fv_index.length; i++)
					if (fv_index[i] < 0)
						Log.error("Failed to find variable " + field_vars[v] + " in the input file");

				if (vi < 0 || vj < 0)
					Log.error("Failed to find coordinates variable " + coord_vars[0] + " or " + coord_vars[1]);
				
				// make sure we have dimensions
				if (ni<=0 || nj<=0) 
					Log.error("Mesh dimensions not specified");
				Log.log(String.format("Mesh dimensions: ni=%d, nj=%d", ni, nj));

				// allocate memory
				IPOS = new double[ni][nj];
				JPOS = new double[ni][nj];
				FVAR = new double[fv_index.length][ni][nj];
				
				i = 0;
				j = 0;

				first_line = false;		
			}  
			else {		// read data
				
				String pieces[] = line.split(",");
				if (pieces.length < (fv_index.length+2))	// skip incomplete lines 
					continue;
				
				if (j>=nj) {
					Log.warning("Too many values, mesh dimension mismatch?");
					break;
				}

				IPOS[i][j] = Double.parseDouble(pieces[vi]) * dim_scale;
				JPOS[i][j] = Double.parseDouble(pieces[vj]) * dim_scale;

				for (int v = 0; v < fv_index.length; v++)
					FVAR[v][i][j] = Double.parseDouble(pieces[fv_index[v]]) * var_scale;
				
				i++;
				if (i>=ni) {j++; i=0;}
				
				
			}
		} // file reading

		// create a new mesh 
		int nn[] = { ni, nj };
		Mesh mesh;
		if (!uniform)
			mesh = new QuadrilateralMesh(nn, IPOS, JPOS, "CSVReaderMesh",
					Starfish.domain_module.getDomainType());
		else {
			double x0[] = { IPOS[0][0], JPOS[0][0] };
			double xm[] = { IPOS[ni - 1][nj - 1], JPOS[ni - 1][nj - 1] };

			double dh[] = { (xm[0] - x0[0]) / (ni - 1), (xm[1] - x0[1]) / (nj - 1) };

			mesh = new UniformMesh(nn, x0, dh, "CSVReaderMesh", Starfish.domain_module.getDomainType());
		}

		// presently supporting only one mesh
		field_manager = new FieldManager2D(mesh);

		for (int v = 0; v < fv_index.length; v++)
			field_manager.add(field_vars[v], "", new Field2D(mesh, FVAR[v]), null);		
	}

	static ReaderFactory csvReaderFactory = new ReaderFactory() {
		@Override
		public Reader makeReader(String file_name, Element element) {
			return new CSVReader(file_name, element);
		}
	};

}
