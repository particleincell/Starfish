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
    /**constructor*/
    public Field1D (Boundary boundary) 
    {
	this.boundary = boundary;
    
	/*for now*/
        this.ni = boundary.numSegments()+1;
 
	data = new double[ni]; 
    }

    public Field1D(Boundary boundary, double values[]) 
    {
	this(boundary);
		
	/*copy data*/
	System.arraycopy(values, 0, data, 0, ni);
    }
    
    /*variables*/
    final int ni;
    
    double data[]; /*public for easy access*/
    final Boundary boundary;

    public int getNi() {return ni;}
    public Boundary getBoundary() {return boundary;}
    public double[] pos(double t) {return boundary.pos(t);}
    /*methods*/
    
    /**at: returns value at [i]*/
    public double at(int i) {return data[i];}

    /**set: sets value at i,j*/
    public void set(int i, double v) {data[i]=v;}
    
    /**returns pointer to data*/
    public double[] getData() {return data;}
	    
    /**replaces field data, shallow replace (reference only)*/
    public void setDataShallow(double data[]) 
    {
	if (data.length!=ni)
	    throw new UnsupportedOperationException("setData: new and old fields are of different size");
		
	this.data = data;  
    }
	
    /**copies data from one field to another, deep copy*/
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

    /**sets all data to the same value*/
    public void setValue(double value) 
    {
	for (int i=0;i<ni;i++)
	    data[i]=value;
    }

    /**adds a scalar to all values*/
    public void add(double val) 
    {
	for (int i=0;i<ni;i++)
	    data[i]+=val;
    }
    
    /**adds field to this one*/
    public void add(Field1D field) 
    {
	for (int i=0;i<ni;i++)
	    data[i]+=field.data[i];
    }
    
    
    public void add(int i, double val) 
	{
		data[i]+=val;
    }
    
    /**fills the entire array the value*/
    public void fill(double val)
    {
	for (int i=0;i<ni;i++)
	    data[i]=val;
    }

    public void scatter(double fi[], double val)
    {
	scatter(fi[0], val);
    }
    
    /** Scatter
     * Distributes val to surroudning nodes
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
    public double gather_safe(double fi)
    {
	if (fi==(ni-1))
	    return data[ni-1];
	else return gather(fi);
    }
	
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

    /**divides every node value by a corresponding value in another topologically identical field*/
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

    /**allocates a 2D ni*nj array*/
    public static double[][] Alloc2D(int ni, int nj)
    {
	double data[][] = new double[ni][];
	for (int i=0;i<ni;i++)
	    data[i] = new double[nj];
	return data;
    }
	
    public static void DataCopy(double[] src, double[] dest) 
    {
	System.arraycopy(src,0,dest,0,dest.length);
    }

    /**returns min/max range of values*/
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

    /**multiplies data by f*/
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
}
