/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldManager2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.QuadrilateralMesh;

/** reader for simple ASCII Tecplot(r) - formatted files*/
public class TecplotReader extends Reader
{

    /**
     *
     * @param file_name
     * @param element
     */
    public TecplotReader(String file_name, Element element)
    {
	this.file_name = file_name;
	try {
	    sc = new Scanner(new FileInputStream(file_name));
	} catch (FileNotFoundException ex) {
	    Log.error("file not found "+file_name);
	}		
    }
    
    /**
     *
     */
    protected String file_name;

    /**
     *
     */
    protected Scanner sc = null;
			
    @Override
    public void parse(String coord_vars[], String field_vars[])
    {
	/*fields*/
	int vi=-1,vj=-1;
	int fv_index[] = new int[field_vars.length];
		
	/*init fv_index to -1*/
	for (int i=0;i<fv_index.length;i++)
	    fv_index[i]=-1;
		
	/*open file*/
	Log.log("Reading "+file_name);
		
	/*look for VARIABLES line*/
	while (sc.hasNextLine())
	{	
	    Scanner line_sc = new Scanner(sc.nextLine().trim());
			
	    String cmd = line_sc.next();
	    if (cmd.equalsIgnoreCase("VARIABLES"))
	    {
		/*process variables*/
		line_sc.findInLine("=");
				
		int var_index=0;
				
		/*this regex will match whole strings in quotations marks with spaces
		* and space-separated strings not in quotes*/
		String regex = "\"([^\"]*)\"|(\\S+)";
		Matcher m = Pattern.compile(regex).matcher(line_sc.nextLine().trim());
		while (m.find()) 
		{
		    String name;
		    if (m.group(1) != null) 
			name = m.group(1);
		    else
			name = m.group(2);

		    /*the regex also matches commas, ignore these*/
		    if (name.charAt(0)==',')
			continue;

		    /*strip unit name*/
		    String var_name =(name.split("\\("))[0].trim();

		    /*is this one of the coordinates?*/
		    if (coord_vars[0].equalsIgnoreCase(var_name))
			vi = var_index;
		    else if (coord_vars[1].equalsIgnoreCase(var_name))
			vj = var_index;
		    else /*do we have this variable in our list?*/
			for (int i=0;i<field_vars.length;i++)
			{
			    if (field_vars[i].equalsIgnoreCase(var_name))
			    {
				fv_index[i]=var_index;
				Log.debug("Reading "+field_vars[i]+" from index "+var_index);
			    }
			}

		    /*increment variable index*/
		    var_index++;
		}

		/*make sure we found all the variables*/
		for (int i=0;i<fv_index.length;i++)
		    if (fv_index[i]<0)
			Log.error("Failed to find variable "+field_vars[i]+" in the input file");

		if (vi<0 || vj<0)
		    Log.error("Failed to find coordinates variable "+coord_vars[0]+" or "+coord_vars[1]);
	    } /*variables*/
	    else if (cmd.equalsIgnoreCase("ZONE"))
	    {
		/*make sure we have already processed the VARIABLES line*/
		if (vi<0)
		    Log.error("VARIABLES line not found prior to the first ZONE");

		int ni, nj;

		String buff = line_sc.findInLine("[Ii]=\\d*");
		if (buff==null)
		    Log.error("Failed to find I=...");
		
		ni = Integer.parseInt(buff.substring(2));

		buff = line_sc.findInLine("[Jj]=\\d*");
		if (buff==null)
		    Log.error("Failed to find J=...");	
		nj = Integer.parseInt(buff.substring(2));

		Log.debug(String.format("ni=%d, nj=%d", ni,nj));

		/*allocate variables*/
		double IPOS[][] = new double[ni][nj];
		double JPOS[][] = new double[ni][nj];
		double FVAR[][][] = new double[fv_index.length][ni][nj];

		/*start parsing the file*/
		for (int j=0;j<nj;j++)
		    for (int i=0;i<ni;i++)
		    {
			String pieces[] = sc.nextLine().trim().split("\\s+");

			IPOS[i][j] = Double.parseDouble(pieces[vi]);
			JPOS[i][j] = Double.parseDouble(pieces[vj]);

			for (int v=0;v<fv_index.length;v++)
			    FVAR[v][i][j] = Double.parseDouble(pieces[fv_index[v]]);
		    }

		/*create a new mesh*/
		Mesh mesh = new QuadrilateralMesh(ni,nj,IPOS,JPOS,Starfish.domain_module.getDomainType());	

		/*presently supporting only one mesh*/
		field_manager = new FieldManager2D(mesh);

		for (int v=0;v<fv_index.length;v++)
		    field_manager.add(field_vars[v],"",new Field2D(mesh,FVAR[v]),null);
	    } /*ZONE*/
	}
    }
    
    static ReaderFactory tecplotReaderFactory = new ReaderFactory() {
	@Override
	public Reader makeReader(String file_name, Element element)
	{
	    return new TecplotReader(file_name,element); 
	}
    };
}



