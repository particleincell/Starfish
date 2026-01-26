/* *****************************************************
 * (c) 2025 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import java.io.PrintWriter;
import java.util.Vector;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.io.InputParser;
import starfish.core.io.VTKWriter;

class AMRNode {
	double pos[];
	public AMRNode(double x, double y) {pos = new double[]{x,y};}
	
}
class AMRCell {
	int nodes[];
	public AMRCell(int n0,int n1, int n2, int n3) {nodes = new int[] {n0,n1,n2,n3};}
}

/**
 *
 * @author Lubos Brieda
 */
public class AMRMesh extends Mesh
{
    /*variables*/
    protected int max_refinement = 2;  // maximum number of refinement levels
    protected int skip = 10;          // number of time steps between updates
    protected int max_it = -1;         // maximum time step at which to run refinement
	
    protected Vector<AMRNode> nodes = new Vector<AMRNode>();
    protected Vector<AMRCell> cells = new Vector<AMRCell>();
    
    protected double x0[] = new double[2];
    protected double xd[] = new double[2];
    protected double dh0[] = new double[2];
    
    /*methods*/

    /** 
     * Creates a Cartesian AMR mesh
     * @param nn number of nodes
     * @param element XML element
     * @param name mesh name
     * @param domain_type
     */
    public AMRMesh (int nn[], Element element, String name, DomainType domain_type)
    {
    	super(new int[]{nn[0]*nn[1],1}, name,domain_type);

		String origin[] = InputParser.getList("origin", element);
		String spacing[] = InputParser.getList("spacing", element);
		max_refinement = InputParser.getInt("max_refinement", element, max_refinement);
		skip = InputParser.getInt("skip", element,skip);
		max_it = InputParser.getInt("max_it", element, max_it);
		
		x0[0] = Double.parseDouble(origin[0]);
		x0[1] = Double.parseDouble(origin[1]);
		
		dh0[0] = Double.parseDouble(spacing[0]);
		dh0[1] = Double.parseDouble(spacing[1]);
		
		xd[0] = x0[0]+dh0[0]*(nn[0]-1);
		xd[1] = x0[1]+dh0[1]*(nn[1]-1);
		
		constructMesh();
		
		/*log*/
		Starfish.Log.log("Added AMR Mesh");
		Starfish.Log.log("> initial nodes   = "+nn[0]+" : "+nn[1]);
		Starfish.Log.log("> initial origin  = "+x0[0]+" : "+x0[1]);
		Starfish.Log.log("> initial spacing = "+dh0[0]+" : "+dh0[1]);
    }
    
    //another constructor that doesn't read element
    public AMRMesh(int nn[], double x0[], double dh[], String name, DomainType domain_type) {
    	super(nn,name,domain_type);
    	
    	this.x0[0] = x0[0];
    	this.x0[1] = x0[1];
    	this.dh0[0] = dh[0];
    	this.dh0[1] = dh[1];
    			
		xd[0] = x0[0]+dh0[0]*(nn[0]-1);
		xd[1] = x0[1]+dh0[1]*(nn[1]-1);
		
		//constructMesh();
				
		/*log*/
		Starfish.Log.log("Added AMR Mesh");
		Starfish.Log.log("> initial nodes   = "+nn[0]+" : "+nn[1]);
		Starfish.Log.log("> initial origin  = "+x0[0]+" : "+x0[1]);
		Starfish.Log.log("> initial spacing = "+dh0[0]+" : "+dh0[1]);
    }
    
   @Override 
   public void update() {
	   if (max_it>0 && Starfish.getIt()>max_it) return;
	   if (Starfish.getIt()%skip!=0) return;
	   // TODO: implement   
   }
   
   /** Creates the initial cartesian mesh*/
   protected void constructMesh(int ni0, int nj0) {
	   nodes.ensureCapacity(ni*nj);
	   for (int j=0;j<nj;j++)
		   for (int i=0;i<ni;i++) {
			   nodes.add(new AMRNode(x0[0]+i*dh0[0], x0[1]+j*dh0[1]));
		   }
	   
	   cells.ensureCapacity((ni-1)*(nj-1));
	   for (int j=0;j<nj-1;j++)
		   for (int i=0;i<ni-1;i++) {
			   int n0 = j*ni + i;
			   
			   cells.add(new AMRCell(n0, n0+1, n0+ni+1,n0+ni));
		   }
	   n_cells = cells.size();
	   
   }
    
    @Override
    /*returns position*/
    public double[] pos(double i, double j)
    {
    //	if (j!=0) Log.error("Call to AMRMesh pos with nonzero j");
    	return new double[] {0,0};	
    	//TODO: implmeent
    }

    /**
     *
     * @param d1
     * @param d2
     * @return
     */
    @Override
    public double[] XtoL(double d1, double d2)
    {
		double lc[] = new double[2];
		
		return lc;
		//throw new UnsupportedOperationException("Not yet defined");
    }

    @Override
    public boolean containsPosStrict(double x[]) 
    {
	if (x[0]>=x0[0] && x[0]<xd[0] &&
	    x[1]>=x0[1] && x[1]<xd[1])
	    return true;
		
	return false;
    }

    @Override
    public double[] faceNormal(Face face, double[] pos)
    {
		double n[] = new double[3];
		
		switch (face)
		{
		    case LEFT: n[0]=1;break;
		    case RIGHT: n[0]=-1; break;
		    case BOTTOM: n[1]=1;break;
		    case TOP: n[1]=-1;break;
		    default: throw new UnsupportedOperationException("Bad Face in a call to faceNormal");
		}
		return n;
    }
    
    @Override
    public String getVTKExtension() {return ".vtu";}
    /**
     * Starts VTK output for this mesh
     */
    @Override
    public void startVTKFile(PrintWriter pw, String endianess, VTKWriter writer) {

    		pw.println("<?xml version=\"1.0\"?>");

    		pw.println("<VTKFile type=\"UnstructuredGrid\"" + endianess + ">");
			pw.printf("<UnstructuredGrid>\n");
			VTKWriter.writeFieldData(pw);

			pw.printf("<Piece NumberOfPoints=\"%d\" NumberOfCells=\"%d\">\n", nodes.size(), cells.size());
		
			//points
		
			pw.println("<Points>");
			double pos[] = new double[nodes.size()*3];
			int a = 0;
			for (int n=0;n<nodes.size();n++) {
					double x[] = nodes.get(n).pos;
					pos[a++] = x[0];
					pos[a++] = x[1];
					pos[a++] = 0;
				}
			
			writer.outputDataArrayVec(pw,"",pos);
			pw.println("</Points>");
			
			//cell connectivity
			pw.println("<Cells>");
			int connectivity[] = new int[cells.size()*4];
			for (int c=0;c<cells.size();c++) {
				for (int p=0;p<4;p++)
					connectivity[c*4+p] = cells.get(c).nodes[p];
			}
			writer.outputDataArrayScalar(pw,"connectivity",connectivity);

			int buf[] = new int[cells.size()];
			for (int c=0;c<cells.size();c++) {
				buf[c] = (c+1)*4;
			}
			writer.outputDataArrayScalar(pw,"offsets",buf);

			for (int c=0;c<cells.size();c++) {
				buf[c] = 9;           // quadrangle per https://docs.vtk.org/en/latest/vtk_file_formats/vtk_legacy_file_format.html#legacy-file-examples
			}
			writer.outputDataArrayScalar(pw,"types",buf);
			pw.printf("</Cells>\n");

    		
    }
    
    @Override
    public void endVTKFile(PrintWriter pw) {

		pw.println("</Piece>");
		pw.println("</UnstructuredGrid>");
    }
    
}
