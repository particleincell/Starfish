/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*need to add to reader

else if (file_type.equalsIgnoreCase("HPHALL"))
	    return new HPHallReader(file_name, element);
*/
package starfish.plugins.het;

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
import starfish.core.io.InputParser;
import starfish.core.io.Reader;
import starfish.core.io.VTKWriter;

/**
 *
 * @author lbrieda
 */
public class HPHallReader extends Reader
{

    public HPHallReader(String file_name, Element element) 
    {
    	super(element);
    	this.file_name = file_name;
    	try {
    		sc = new Scanner(new FileInputStream(file_name));
    	} catch (FileNotFoundException ex) {
    		Log.error("file not found "+file_name);
    	}	
	
    	this.grid_file_name = InputParser.getValue("grid_file", element,"grid.dat");
    	Log.log("Reading grid data from "+grid_file_name);
	
    	try {
    		gf_sc = new Scanner(new FileInputStream(grid_file_name));
    	} catch (FileNotFoundException ex) {
    		Log.error("file not found "+grid_file_name);
    	}	
    }

    protected String file_name;
    protected String grid_file_name;
    protected Scanner sc;
    protected Scanner gf_sc;
    
    @Override
    public void parse(String[] coord_vars, String[] field_vars)
    {
	/*fields*/
	int vi=-1,vj=-1;
	int fv_index[] = new int[field_vars.length];
		
	/*init fv_index to -1*/
	for (int i=0;i<fv_index.length;i++)
	    fv_index[i]=-1;
		
	/*open file*/
	Log.log("Reading "+file_name);
		
	//available data in b.dat
	String b_var_names[] = {"sigma","lambda","bz","br","b"};
	String grid_var_names[] = {"z","r"};
			
	for (int i=0;i<coord_vars.length;i++)	    
	{
	    String s = coord_vars[i];
	    for (int j=0;j<2;j++)
	    {
		String s2 = grid_var_names[j];
		if (s2.equalsIgnoreCase(s))
		{
		    if (i==0) vi=j;
		    else if (i==1) vj=j;
		    break;
		}		    
	    }
	}
	
	//get variables
	for (int i=0;i<field_vars.length;i++)
	{
	    String s = field_vars[i];
	    
	    for (int j=0;j<b_var_names.length;j++)
	    {
		String s2 = b_var_names[j];
		if (s2.equalsIgnoreCase(s))
		{
		    fv_index[i] = j;
		    break;
		}	
	    }
	}
	
	
	/*make sure we found all the variables*/
	for (int i=0;i<fv_index.length;i++)
	    if (fv_index[i]<0)
		Log.error("Failed to find variable "+field_vars[i]+" in the input file");

	if (vi<0 || vj<0)
	    Log.error("Failed to find coordinates variable "+coord_vars[0]+" or "+coord_vars[1]);
	
	/*make sure we have already processed the VARIABLES line*/
	
	int ni, nj;
	
	ni = gf_sc.nextInt();
	nj = gf_sc.nextInt();
	
	if (sc.nextInt()!=ni || sc.nextInt()!=nj)
	    Log.error("Data mismatch between b and grid file");
	
	gf_sc.nextLine();   //skip rest of the line
	sc.nextLine();
		
	/*allocate variables*/
	double IPOS[][] = new double[ni][nj];
	double JPOS[][] = new double[ni][nj];
	double FVAR[][][] = new double[fv_index.length][ni][nj];

	/*start parsing the file*/
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		//get position from grid file
		String line = gf_sc.nextLine();
		String pieces[] = line.trim().split("\\s+");
		
		IPOS[i][j] = Double.parseDouble(pieces[vi]);
		JPOS[i][j] = Double.parseDouble(pieces[vj]);
		
		//now read a line from b file
		line = sc.nextLine();
		pieces = line.trim().split("\\s+");
		
		for (int v=0;v<fv_index.length;v++)
		{
		    FVAR[v][i][j] = Double.parseDouble(pieces[fv_index[v]]);
		    //FVAR[0][i][j] = i;
		    //FVAR[1][i][j] = j;
		    
		}
	    }

		/*create a new mesh*/
		Mesh mesh = new QuadrilateralMesh(ni,nj,IPOS,JPOS,"hpall_reader_mesh",Starfish.domain_module.getDomainType());	
		/*presently supporting only one mesh*/
		field_manager = new FieldManager2D(mesh);

		for (int v=0;v<fv_index.length;v++)
	    field_manager.add(field_vars[v],"",new Field2D(mesh,FVAR[v]),null);
	
		VTKWriter vtk_writer = new VTKWriter("qmesh.vts", VTKWriter.OutputFormat.ASCII);
		vtk_writer.write();
				
	
    }
    
    public static ReaderFactory hphallReaderFactory = new ReaderFactory() {
	@Override
	public Reader makeReader(String file_name, Element element)
	{
	    return new HPHallReader(file_name,element); 
	}
    };
}
