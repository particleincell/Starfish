/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

/** 1D field holding a single scalar*/
public class Field1D 
{
    /**constructor
     * @param boundary*/
    public Field1D (Boundary boundary) 
    {
	this.boundary = boundary;
    
	/*for now*/
        this.ni = boundary.numSegments()+1;
 
	data = new double[ni]; 
    }

    /**
     *
     * @param boundary
     * @param values
     */
    public Field1D(Boundary boundary, double values[]) 
    {
	this(boundary);
		
	/*copy data*/
	System.arraycopy(values, 0, data, 0, ni);
    }
    
    /**constructor for an arbitrary non-attached field*/
    public Field1D(int ni)
    {
	this.boundary = null;
	this.ni = ni;
	data = new double[ni];	
    }
    
    /*variables*/
    final int ni;
    
    public double data[]; /*public for easy access*/
    final Boundary boundary;

    /**
     *
     * @return
     */
    public int getNi() {return ni;}

    /**
     *
     * @return
     */
    public Boundary getBoundary() {return boundary;}

    /**
     *
     * @param t
     * @return
     */
    public double[] pos(double t) {return boundary.pos(t);}
    /*methods*/
    
    /**at: returns value at [i
     * @param i]
     * @return */
    public double at(int i) {return data[i];}

    /**set: sets value at i,
     * @param i
     * @param vj*/
    public void set(int i, double v) {data[i]=v;}
    
    /**returns pointer to dat
     * @return a*/
    public double[] getData() {return data;}
	    
    /**replaces field data, shallow replace (reference only
     * @param data)*/
    public void setDataShallow(double data[]) 
    {
	if (data.length!=ni)
	    throw new UnsupportedOperationException("setData: new and old fields are of different size");
		
	this.data = data;  
    }
	
    /**copies data from one field to another, deep copy
     * @param NI*/
    public void copy(Field1D NI) 
    {
	if (data.length!=ni)
	    throw new UnsupportedOperationException("setData: new and old fields are of different size");
		
	System.arraycopy(NI.getData(), 0, data, 0, ni);
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
	    data[i]=value;
    }

    /**adds a scalar to all value
     * @param vals*/
    public void add(double val) 
    {
	for (int i=0;i<ni;i++)
	    data[i]+=val;
    }
    
    /**adds field to this on
     * @param fielde*/
    public void add(Field1D field) 
    {
	for (int i=0;i<ni;i++)
	    data[i]+=field.data[i];
    }
    
    /**
     *
     * @param i
     * @param val
     */
    public void add(int i, double val) 
	{
		data[i]+=val;
    }
    
    /**fills the entire array the valu
     * @param vale*/
    public void fill(double val)
    {
	for (int i=0;i<ni;i++)
	    data[i]=val;
    }

    /**
     *
     * @param fi
     * @param val
     */
    public void scatter(double fi[], double val)
    {
	scatter(fi[0], val);
    }
    
    /** Scatter
     * Distributes val to surroudning nodes
     * @param fi
     * @param val
     */
    public void scatter(double fi, double val)
    {
	int i = (int)fi;
        double di = fi-i;
       
	/*normal scatter*/
	if (i<data.length-1)
	{
	    data[i] += (1-di)*val;
	    data[i+1] += di*val;
	}
	/*if right on last node*/
	else if (i==data.length && di<1e-6)
	{
	    data[i] = val;
	}
    }
    
    /**
     *
     * @param fi
     * @return
     */
    public double gather(double fi)
    {
	int i = (int)fi;
        double di = fi-i;
        double v;
        
        v = (1-di)*data[i];
        v+= di*data[i+1];
        
        return v;	
    }
	
    /*like gather but allows evaluation of position along mesh edges*/

    /**
     *
     * @param fi
     * @return
     */

    public double gather_safe(double fi)
    {
	if (fi==(ni-1))
	    return data[ni-1];
	else return gather(fi);
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
        double di = fi-i;
        double v;
		
	if (i>=ni-1 && di<0.01) {i=ni-2;di=0.99999;}
		
        v = (1-di)*data[i];
        v+= di*data[i+1];
        
        return v;	
    }

    /**divides every node value by a corresponding value in another topologically identical fiel
     * @param fieldd*/
    public void divideByField(Field1D field) 
    {
	if (ni!=field.ni)
	    throw new UnsupportedOperationException("Scaling of non-matching fields not yet implemented");
	
	for (int i=0;i<ni;i++)
	    if (field.data[i]!=0)
		data[i]/=field.data[i];
	    else 
		data[i] = 0;
    }

    /**allocates a 2D ni*nj arra
     * @param niy
     * @param nj
     * @return */
    public static double[][] Alloc2D(int ni, int nj)
    {
	double data[][] = new double[ni][];
	for (int i=0;i<ni;i++)
	    data[i] = new double[nj];
	return data;
    }
	
    /**
     *
     * @param src
     * @param dest
     */
    public static void DataCopy(double[] src, double[] dest) 
    {
	System.arraycopy(src,0,dest,0,dest.length);
    }

    /**returns min/max range of value
     * @return s*/
    public double[] getRange() 
    {
	double range[] = new double[2];
	range[0] = data[0];
	range[1] = data[0];
		
	for (int i=0;i<ni;i++)
	{
	    if (data[i]<range[0]) range[0] = data[i];
	    if (data[i]>range[1]) range[1] = data[i];
	}
	return range;
    }

    /**multiplies data by
     * @param ff*/
    public void mult(double f) 
    {
	for (int i=0;i<ni;i++) data[i]*=f;
    }

    /**divides data by corresponding surface area*/
    public void divideByArea() 
    {
	for (int i=0;i<ni;i++)
	{
	    /*we can have zero area on RZ splines along R=0*/
	    double A=boundary.nodeArea(i);
	    if(A!=0.0) data[i]/=A;
	    else data[i]=0;
	}
    }
    
    /**obtains logical coordinate assuming increasing data
     * @param x value to get index of
     * @return the logical coordinate or -1 if not found
     */
    public double getLC(double x)
    {
	for (int i=0;i<ni-1;i++)
	{
	    if (x>=data[i] && x<=data[i+1])
	    {
		double f = (x-data[i])/(data[i+1]-data[i]);
		return i+f;
	    }
	}
	
	return -1;
    }
    
}
