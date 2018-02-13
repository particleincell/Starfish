/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.domain;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule.DomainType;

/* 2D field holding a single scalar*/

/**
 *
 * @author Lubos Brieda
 */

public class Field2D 
{
    /**constructo
     * @param meshr*/
    public Field2D (Mesh mesh) 
    {
	this(mesh, false);
    }
    
    /**
     *
     * @param mesh
     * @param cell_centered
     */
    public Field2D (Mesh mesh, boolean cell_centered) 
    {
	this.mesh = mesh;
    
        if (cell_centered) {ni = mesh.ni-1; nj = mesh.nj-1;}
	else {ni = mesh.ni; nj = mesh.nj;}

        data = new double[ni][nj]; 
    }

    /** constructor for field not associated with a particular mesh, should
    * probably be a superclass of the mesh-associated field...
     * @param ni
     * @param nj
    */
    public Field2D(int ni, int nj)
    {
	this.ni = ni;
	this.nj = nj;
	this.mesh = null;
	data = new double[ni][nj]; 
    }
	
    /**
     *
     * @param mesh
     * @param values
     */
    public Field2D(Mesh mesh, double values[][]) 
    {
	this(mesh);
		
	/*copy data*/
	for (int i=0;i<ni;i++)
	    System.arraycopy(values[i], 0, data[i], 0, nj);
    }
    
    /*variables*/
    public final int ni,

    /**
     *
     */
    nj;
    
    /**
     *
     */
    public double data[][]; 
    final Mesh mesh;
	
    /**
     *
     * @return
     */
    public int getNi() {return ni;}

    /**
     *
     * @return
     */
    public int getNj() {return nj;}

    /**
     *
     * @return
     */
    public Mesh getMesh() {return mesh;}

    /**
     *
     * @param fi
     * @param fj
     * @return
     */
    public double[] pos(double fi, double fj) {return mesh.pos(fi,fj);}
    /*methods*/
    
    /**at: returns value at [i,j
     * @param i
     * @param j]
     * @return */
    public double at(int i, int j) {return data[i][j];}

    /**set: sets value at i,
     * @param ij
     * @param j
     * @param v*/
    public void set(int i, int j, double v) {data[i][j]=v;}
    
    /**returns pointer to dat
     * @return a*/
    public double[][] getData() {return data;}
	
    /**returns data[i
     * @param i]
     * @return */
    public double[] getData(int i) {return data[i];}

    /**replaces field data, shallow replace (reference only
     * @param data)*/
    public void setDataShallow(double data[][]) 
    {
	if (data.length!=ni ||
	    data[0].length!=nj)
	    throw new UnsupportedOperationException("setData: new and old fields are of different size");
		
	this.data = data;  
    }
	
    /**copies data from one field to another, deep cop
     * @param srcy*/
    public void copy(Field2D src) 
    {
	if (src.data.length!=ni ||
	    src.data[0].length!=nj)
	    throw new UnsupportedOperationException("setData: new and old fields are of different size");
		
	for (int i=0;i<ni;i++)
	    System.arraycopy(src.getData(i), 0, data[i], 0, nj);
    }
	
    /**clear the array*/
    public void clear()
    {
	setValue(0);
    }

    /**sets all data to the same valu
     * @param valuee*/
    public void setValue(double value) 
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		data[i][j]=value;
    }

    /**adds a scalar to all values
     * @param val*/
    public void mult(double val) 
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		data[i][j]*=val;
    }

    /**adds a scalar to all values
     * @param val*/
    public void add(double val) 
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		data[i][j]+=val;
    }
    
    /**
     *
     * @param i
     * @param j
     * @param val
     */
    public void add(int i, int j, double val) 
    {
	data[i][j]+=val;
    }
    
    
    /**adds values of another field
     * @param field field to add*/
    public void add(Field2D field)
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		data[i][j] += field.data[i][j];
    }
    
    /**fills the entire array the valu
     * @param vale*/
    public void fill(double val)
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		data[i][j]=val;
    }

    /**
     *
     * @param fi
     * @param val
     */
    public void scatter(double fi[], double val)
    {
	scatter(fi[0], fi[1], val);
    }
    
    /** Scatter
     * Distributes val to surrounding node
     * @param fis
     * @param fj
     * @param val*/
    public void scatter(double fi, double fj, double val)
    {
	int i = (int)fi;
        int j = (int)fj;
        double di = fi-i;
        double dj = fj-j;
	
	/*make sure we are not out of bounds*/
	if (i<0 || j<0 || i>=ni-1 || j>=nj-1) return;

	if (Starfish.domain_module.domain_type==DomainType.RZ)
	{   /*equation 4.2 in Ruyten (93)*/
	    double rp = mesh.R(i+1, fj);
	    double rm = mesh.R(i,fj);
	    double r = mesh.R(fi, fj);
	    di = 1-(0.5*(rp-r)*(2*rp+3*rm-r)/(rp*rp-rm*rm));
	}
	else if (Starfish.domain_module.domain_type==DomainType.ZR)
	{   /*equation 4.2 in Ruyten (93)*/
	    double rp = mesh.R(fi, j+1);
	    double rm = mesh.R(fi,j);
	    double r = mesh.R(fi, fj);
	    di = fi-i;
	    dj = 1-(0.5*(rp-r)*(2*rp+3*rm-r)/(rp*rp-rm*rm));
	}
	
	if (Double.isNaN(dj) || Double.isNaN(di) || Double.isNaN(val))
	    System.out.printf("Nan in scatter");
	
	data[i][j] += (1-di)*(1-dj)*val;
	data[i+1][j] += di*(1-dj)*val;
	data[i+1][j+1] += di*dj*val;
	data[i][j+1] += (1-di)*dj*val;
	
    }
    
    /**Interpolates data from the four corner nodes surroudning fi/f
     * @param fij
     * @return */
    public double gather(double fi[])
    {
	try
	{
	    return gather(fi[0], fi[1]);
	}
	catch (IndexOutOfBoundsException e)
	{
	    return gather_safe(fi[0],fi[1]);
	}
    }
    
    /**
     *
     * @param fi
     * @param fj
     * @return
     */
    public double gather(double fi, double fj)
    {
	int i = (int)fi;
        int j = (int)fj;
        double di = fi-i;
        double dj = fj-j;
        double v;
        
        v = (1-di)*(1-dj)*data[i][j];
        v+= di*(1-dj)*data[i+1][j];
        v+= di*dj*data[i+1][j+1];
        v+= (1-di)*dj*data[i][j+1];
        
        return v;	
    }
	
    /*like gather but allows evaluation of position along mesh edges*/

    /**
     *
     * @param fi
     * @return
     */

    public double gather_safe(double fi[])
    {
	return gather_safe(fi[0],fi[1]);
    }
	
    /**
     *
     * @param fi
     * @param fj
     * @return
     */
    public double gather_safe(double fi, double fj)
    {
	int i = (int)fi;
        int j = (int)fj;
        double di = fi-i;
        double dj = fj-j;
        double v;
		
	if (i<0) {i=0;di=0;}
	if (j<0) {j=0;dj=0;}
	if (i>=ni-1) {i=ni-2;di=0.99999;}
	if (j>=nj-1) {j=nj-2;dj=0.99999;}
		
        v = (1-di)*(1-dj)*data[i][j];
        v+= di*(1-dj)*data[i+1][j];
        v+= di*dj*data[i+1][j+1];
        v+= (1-di)*dj*data[i][j+1];
        
        return v;	
    }

    /**like gather but with physical coordinate inpu
     * @param x
     * @return t*/
    public double eval(double[] x) 
    {
	double lc[] = mesh.XtoL(x);
	return gather(lc);
    }

    /**divides every node value by a corresponding value in another topologically identical fiel
     * @param fieldd*/
    public void divideByField(Field2D field) 
    {
	if (ni!=field.ni || nj!=field.nj)
	    throw new UnsupportedOperationException("Scaling of non-matching fields not yet implemented");
	
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		if (field.data[i][j]!=0)
		    data[i][j]/=field.data[i][j];
		else 
		    data[i][j] = 0;
    }

	
    /**scales each node value by node volume*/
    public void scaleByVol() 
    {
	double node_vol[][] = mesh.node_vol.data;
	
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		data[i][j]/=node_vol[i][j];	
		
		if (Double.isNaN(data[i][j])||Double.isInfinite(data[i][j]))
		    System.out.printf("NaN at %d %d, vol:%g\n",i,j,node_vol[i][j]);
		
	    }
    }

    /**computes d/dx (x derivative
     * @param i
     * @param j)
     * @return */
    public double ddx(int i, int j) 
    {
	if (i>0 && i<mesh.ni-1)
	{
	    double dx = mesh.pos1(i+1,j) - mesh.pos1(i-1,j);
	    return (data[i+1][j]-data[i-1][j])/dx;
	}
	else if (i>0)
	{
	    double dx = mesh.pos1(i,j) - mesh.pos1(i-1,j);
	    return (data[i][j]-data[i-1][j])/dx;
	}
	else
	{
	    double dx = mesh.pos1(i+1,j) - mesh.pos1(i,j);
	    return (data[i+1][j]-data[i][j])/dx;
	}
    }

    /**computes d/dy (y derivative
     * @param i)
     * @param j
     * @return */
    public double ddy(int i, int j) 
    {
	if (j>0 && j<mesh.nj-1)
	{
	    double dy = mesh.pos2(i,j+1) - mesh.pos2(i,j-1);
	    return (data[i][j+1]-data[i][j-1])/dy;
	}
	else if (j>0)
	{
	    double dy = mesh.pos2(i,j) - mesh.pos2(i,j-1);
	    return (data[i][j]-data[i][j-1])/dy;
	}
	else
	{
	    double dy = mesh.pos2(i,j+1) - mesh.pos2(i,j);
	    return (data[i][j+1]-data[i][j])/dy;
	}
    }

    /**returns value at center of cell i,
     * @param ij
     * @param j
     * @return */
    public double cellAt(int i, int j) 
    {
	return gather(i+0.5,j+0.5);
    }
	
    /**allocates a 2D ni*nj arra
     * @param ni
     * @param nj
     * @return y*/
    public static double[][] Alloc2D(int ni, int nj)
    {
	double data[][] = new double[ni][];
	for (int i=0;i<ni;i++)
	    data[i] = new double[nj];
	return data;
    }
	
    /**creates a new field on the mesh and interpolates data from the collection to it
     * @param mesh
     * @param field_collection
     * @return */
    public static Field2D FromExisting(Mesh mesh, FieldCollection2D field_collection) 
    {
	Field2D out = new Field2D(mesh);
			
	Field2D[] in_fields = field_collection.getFields();

	for (Field2D in:in_fields)
	{
	    Mesh in_mesh = in.getMesh();
	    for (int i=0;i<mesh.ni;i++)
		for (int j=0;j<mesh.nj;j++)
		{	
		    double pos[] = mesh.pos(i,j);
		    double lc[] = in_mesh.XtoL(pos);
					
		    if (lc[0]<0 || lc[1]<0 ||
			lc[0]>in_mesh.ni || lc[1]>in_mesh.nj)
			continue;
					
		    out.set(i, j, in.gather(lc));
					
		}
	}
		
	return out;		
    }

    /**
     *
     * @param src
     * @param dest
     */
    public static void DataCopy(double[][] src, double[][] dest) 
    {
	int ni = dest.length;
	int nj = dest[0].length;
		
	for(int i=0;i<ni;i++)
	    System.arraycopy(src[i],0,dest[i],0,nj);
    }

    /**returns min/max range of value
     * @return s*/
    public double[] getRange() 
    {
	double range[] = new double[2];
	range[0] = data[0][0];
	range[1] = data[0][0];
		
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		if (data[i][j]<range[0]) range[0] = data[i][j];
		if (data[i][j]>range[1]) range[1] = data[i][j];
	    }
	return range;
    }

    /**interpolates data from fc, adding to existing value
     * @param fcs*/
    public void interp(FieldCollection2D fc) 
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		try{

		    data[i][j] += fc.eval(mesh.pos(i,j));
		} catch (NoSuchElementException e)
		{
		    /*do nothing*/
		}
    }
    
        /**interpolates data from field, adding to existing value
     * @param fs*/
    public void interpWithOverwrite(Field2D f) 
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		double pos[] = mesh.pos(i,j);
		if (f.mesh.containsPos(pos))
		{
		    data[i][j] = f.eval(pos);
		}
	    }
    }

    /**replaces data instead of adding as with regular inter
     * @param fcp*/
    public void interpWithOverwrite(FieldCollection2D fc) 
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		try{
		    data[i][j] = fc.eval(mesh.pos(i,j));
		    } catch (NoSuchElementException e)
		    {
			/*do nothing*/
		    }
    }

    /**
     *
     * @param fc
     * @param scalar
     */
    public void interpScaled(FieldCollection2D fc, double scalar) 
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		data[i][j] += scalar*fc.eval(mesh.pos(i,j));
    }

    /**
     *
     * @param fi
     * @param fj
     */
    public void interpMagnitude(FieldCollection2D fi, FieldCollection2D fj) 
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		double x[] = mesh.pos(i,j);
		double v1 = fi.eval(x);
		double v2 = fj.eval(x);
		data[i][j] += Math.sqrt(v1*v1+v2*v2);
	    }
    }

    /**
     *
     * @param fi
     * @param fj
     */
    public void interpNormal(FieldCollection2D fi, FieldCollection2D fj) 
    {
	/*returns component normal to the contour*/
	double n[] = new double[2];
	double v[] = new double[2];

	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		/*tangent vector*/
		double t[] = tangent(i,j);

		/*normal vector*/
		n[0]=t[1];
		n[1]=-t[0];

		/*interpolate vector quantities*/
		double x[] = mesh.pos(i,j);
		v[0] = fi.eval(x);
		v[1] = fj.eval(x);

		data[i][j] += v[0]*n[0]+v[1]*n[1];	/*dot product*/
	    }	
    }

    /*return tangent vector at location i*/

    /**
     *
     * @param i
     * @param j
     * @return
     */

    public double[] tangent(int i, int j) 
    {
	double t[] = new double[2];

	if (j==0)
	{
	    double x1[] = mesh.pos(i,0);
	    double x2[] = mesh.pos(i,1);

	    t[0]=x2[0]-x1[0];
	    t[1]=x2[1]-x1[1];
	}
	else if (j==nj-1)
	{
	    double x1[] = mesh.pos(i,nj-2);
	    double x2[] = mesh.pos(i,nj-1);

	    t[0]=x2[0]-x1[0];
	    t[1]=x2[1]-x1[1];				
	}
	else
	{
	    double x1[] = mesh.pos(i,j-1);
	    double x2[] = mesh.pos(i,j+1);

	    t[0]=0.5*(x2[0]-x1[0]);
	    t[1]=0.5*(x2[1]-x1[1]);
	}

	/*normalize*/
	double ds = Math.sqrt(t[0]*t[0]+t[1]*t[1]);
	t[0]/=ds;
	t[1]/=ds;

	return t;
    }

    /*saves data to a binary file*/

    /**
     *
     * @param out
     * @throws IOException
     */

    public void binaryWrite(DataOutputStream out) throws IOException
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		out.writeDouble(data[i][j]);
    }
    
    /*reads data from a binary file*/

    /**
     *
     * @param in
     * @throws IOException
     */

    public void binaryRead(DataInputStream in) throws IOException
    {
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		data[i][j] = in.readDouble();
	
    }
}
