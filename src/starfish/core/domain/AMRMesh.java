/* *****************************************************
 * (c) 2025 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;


import org.w3c.dom.Element;

import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Segment;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.io.InputParser;
import starfish.core.io.VTKWriter;

class AMRNode {
	double pos[];
	public AMRNode(double x, double y) {pos = new double[]{x,y};}
	
}

// nodes are ordered counterclockwise n0->n1->n2->n3
class AMRCell  {
	int nodes[];
	public AMRCell(int n0,int n1, int n2, int n3, long z) {nodes = new int[] {n0,n1,n2,n3}; this.z=z;}
	public AMRCell(long z) {this.z = z;}  // for comparison
	
	public long getZ() {return z;}
	protected long z = 0;
}


/**
 *
 * @author Lubos Brieda
 */
public class AMRMesh extends Mesh
{
    /*variables*/
    protected short max_refinement = 2;  // maximum number of refinement levels
    protected int skip = 10;          // number of time steps between updates
    protected int max_it = -1;         // maximum time step at which to run refinement
	
    protected ArrayList<AMRNode> nodes = new ArrayList<AMRNode>();
    protected ArrayList<AMRCell> cells = new ArrayList<AMRCell>();
    
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
		max_refinement = (short)InputParser.getInt("max_refinement", element, max_refinement);
		skip = InputParser.getInt("skip", element,skip);
		max_it = InputParser.getInt("max_it", element, max_it);
		
		x0[0] = Double.parseDouble(origin[0]);
		x0[1] = Double.parseDouble(origin[1]);
		
		dh0[0] = Double.parseDouble(spacing[0]);
		dh0[1] = Double.parseDouble(spacing[1]);
		
		xd[0] = x0[0]+dh0[0]*(nn[0]-1);
		xd[1] = x0[1]+dh0[1]*(nn[1]-1);
		
		constructMesh(nn[0], nn[1]);
		
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
		
		constructMesh(nn[0], nn[1]);
				
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
	   nodes.ensureCapacity(ni0*nj0);
	   for (int j=0;j<ni0;j++)
		   for (int i=0;i<nj0;i++) {
			   nodes.add(new AMRNode(x0[0]+i*dh0[0], x0[1]+j*dh0[1]));
		   }
	   
	   cells.ensureCapacity((ni0-1)*(nj0-1));
	   int mult = (1<<max_refinement);
	   
	   for (int j=0;j<nj0-1;j++)
		   for (int i=0;i<ni0-1;i++) {
			   int n0 = j*ni0 + i;
			   
			   int i_fine = (i*mult);
			   int j_fine = (j*mult);
			   long z = getZ(i_fine,j_fine);
					   
			   cells.add(new AMRCell(n0, n0+1, n0+ni0+1,n0+ni0, z));
		   }
	   
	   Collections.sort(cells,Comparator.comparingLong(AMRCell::getZ));
		
	   n_cells = cells.size();
	   
	   /*long z_last = -1;
	   for (int c=0;c<n_cells;c++) {
		   long z = cells.get(c).getZ();
		   if (z<=z_last) {
			   System.err.printf("Z mismatch, z:%d, z_last:%d\n",z,z_last);
		   }
		   z_last = z;
	   }
	   
	   System.out.println("done");
	   */
   }
    
    @Override
    /*returns position*/
    public double[] pos(double fi, double fj)
    {
    	if (fj<-1.1 || fj>1.1)  // the calling function uses 1.01 for limits
    		Log.error("Call to AMRMesh pos with nonzero j");
    	int ii = (int)fi;
    	int jj = (int)fj;
    	
    	double di = fi - ii;
    	double dj = fj - jj;
    	
    	int c = getCell(ii,jj);
    	if (c<0 || c>= cells.size()) {
        	c = getCell(ii,jj);
    		Log.error("out of bounds");
    	}
    	
    	AMRCell cell = cells.get(c);
    	
    	int n0 = cell.nodes[0];
    	int n2 = cell.nodes[2];
    	double x0[] = nodes.get(n0).pos;
    	double xm[] = nodes.get(n2).pos;
    	
    	double dx = xm[0]-x0[0];
    	double dy = xm[1]-x0[1];
    	
    	double pos[] = new double[2];
    	pos[0] = x0[0] + di*dx;
    	pos[1] = x0[1] + dj*dy;
    	
    	return pos;
	}

    /** This function locates the cell index for a point contain d1 and d2
     *
     * @param d1
     * @param d2
     * @return
     */
    @Override
    public double[] XtoL(double d1, double d2)
    {
    	if (d1<x0[0] || d2<x0[1] || d1>=xd[0] || d2>=xd[1]) {
    		return new double[] {-1.,-1.};
    	}
    	
		int i = (int)((d1-x0[0])/dh0[0]);
		int j = (int)((d2-x0[1])/dh0[1]);
		
		int index = getCell(i,j);
		return new double[] {index,0};
    }

    /** Returns cell containing the (i,j) coordinate on the finesh mesh
     * 
     * @return cell index
     */
    protected int getCell(int fine_i, int fine_j) {
    	long z = getZ(fine_i,fine_j);
		AMRCell cell_z=new AMRCell(z);
		int index = Collections.binarySearch(cells,cell_z,Comparator.comparingLong(AMRCell::getZ));
		return index;
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
    
    protected long getZ(int i, int j) {
        long z = 0;
        
        int max_b = 15;
        for (int b=0;b<max_b;b++) {
        	int mask = 1<<b;
        	z+=((i&mask)>0?1:0)<<(2*b);
          	z+=((j&mask)>0?1:0)<<(2*b+1);
        }
    
        /*
         * testing
         */
        /*System.out.printf("i: ");
        for (short b=(short)(max_b-1);b>=0;b--) {
        	short mask = (short)(1<<b);
        	int val = ((i&mask)>0)?1:0;
        	System.out.printf("%d",val);  	
        }
        System.out.println();
        System.out.printf("j: ");
        for (short b=(short)(max_b-1);b>=0;b--) {
        	short mask = (short)(1<<b);
        	int val = ((j&mask)>0)?1:0;
        	System.out.printf("%d",val); 	
        }
        System.out.println();
        System.out.printf("z: ");
        for (short b=(short)(2*max_b-1);b>=0;b--) {
        	short mask = (short)(1<<b);
        	int val = ((z&mask)>0)?1:0;
        	System.out.printf("%d",val);        	
        }
        System.out.println();
         */
        
        return z;
    }
    
	/**
	 * marks boundaries located in a volume centered about each node, implementation for AMR mesh
	 * 
	 * @param boundary_list
	 */
    @Override
	protected void setNodeControlVolumes(ArrayList<Boundary> boundary_list) {
		int i, j;

		/* set node control volumes */
		for (Boundary boundary : boundary_list) {
			for (Segment segment : boundary.getSegments()) {
				/* get spline range */
				double box[][] = segment.getBox();

				/* convert to logical coordinates */
				double lc1[] = XtoL(box[0]);
				double lc2[] = XtoL(box[1]);

				int cell1 = (int)lc1[0];
				int cell2 = (int)lc2[0];
				
				/* loop through all cells*/
				for (int c=cell1;c<=cell2;c++)  {
				
					double cell_box[][] = getCellBox(c);
					
					if (!segment.segmentInBox(cell_box[0], cell_box[1]))
						continue;
/*
						boolean found = false;

						// see if we already have this boundary 
						for (Segment seg : node[i][j].segments) {
							if (seg.getBoundary() == boundary && seg.id() == segment.id()) {
								found = true;
								break;
							}
						}

						// not found, add 
						if (!found) {
							if (node[i][j].segments == null)
								node[i][j].segments = new ArrayList<>();
							node[i][j].segments.add(segment);
						}
*/
					} /* node loop */
			} /* segment */
		} /* boundary */
	}
    
    /*TODO: implement*/
    protected double[][] getCellBox(int c) {
    	double box[][] = new double[2][];
    	return box;
    }
    
}
