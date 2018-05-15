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
import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldManager2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.QuadrilateralMesh;

/*-----table format reader --------------*/

/**
 *
 * @author Lubos Brieda
 */

public class TableReader extends Reader
{

    /**
     *
     * @param file_name
     * @param element
     */
    public TableReader(String file_name, Element element)
    {
	/*open file*/
	Log.log("Opening file "+file_name);

	Scanner file = null;
	try {
	    file = new Scanner(new FileInputStream(file_name));
	} catch (FileNotFoundException ex) {
	    Log.error("file not found "+file_name);
	}

	/*first read mesh dimensions*/
	Scanner line = new Scanner(file.nextLine());
	int ni = line.nextInt();
	int nj = line.nextInt();

	/*allocate data*/
	double z[][] = Field2D.Alloc2D(ni, nj);
	double r[][] = Field2D.Alloc2D(ni, nj);
	double B[][] = Field2D.Alloc2D(ni, nj);
	double Bz[][] = Field2D.Alloc2D(ni, nj);
	double Br[][] = Field2D.Alloc2D(ni, nj);

	int i=0, j=0;

	while (file.hasNextLine())
	{
	    line = new Scanner(file.nextLine());
	    z[i][j] = line.nextDouble();
	    r[i][j] = line.nextDouble();
	    B[i][j] = line.nextDouble();
	    Bz[i][j] = line.nextDouble();
	    Br[i][j] = line.nextDouble();

	    j++;
	    if (j>=nj) {j=0;i++;}
	}

	int nn[] = {ni,nj};
	Mesh mesh = new QuadrilateralMesh(nn, z, r, "TableReaderMesh", Starfish.domain_module.getDomainType());

	field_manager = new FieldManager2D(mesh);
	field_manager.add("B", "",new Field2D(mesh,B),null);
	field_manager.add("Bz", "",new Field2D(mesh,Bz),null);
	field_manager.add("Br", "",new Field2D(mesh,Br),null);	
    }

    @Override
    public void parse(String[] coord_vars, String[] field_vars) {
	throw new UnsupportedOperationException("Not supported yet.");
    }
    
    static ReaderFactory tableReaderFactory = new ReaderFactory() {
	@Override
	public Reader makeReader(String file_name, Element element)
	{
	    return new TableReader(file_name,element); 
	}
    };
}

