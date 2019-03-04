/*
 * (c) 2012-2019 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.core.common;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Lubos Brieda
 */
public class LinearList
{

    public LinearList()
    {
	/*empty constructor*/
    }

    public LinearList(ArrayList<double []> data)
    {

	for (double[] d : data)
	{
	    if (d.length!=2)
		Starfish.Log.error("Wrong array length in LinearList");
	    insert(d[0],d[1]);
	}
    }


    public LinearList(double x[], double y[])
    {
	if (x.length!=y.length)
	    Starfish.Log.error("x and y lists must have the same the length");

	for (int i=0;i<x.length;i++)
	{
	    insert(x[i],y[i]);
	}
    }
    /**
     *
     */
    public ArrayList<XYData> data = new ArrayList<XYData>();

    /**
     *
     * @param x
     * @param y
     */
    final public void insert(double x, double y)
    {
	data.add(new XYData(x,y));
	dirty = true;
    }

    private boolean dirty = true;

    /** @return True if empty list*/
    public boolean isEmpty()
    {
	return data.isEmpty();
    }
    /**
     *
     * @param x
     * @return
     */
    public double eval(double x)
    {
	    if (data.isEmpty()) return 0;
	    if (dirty) {
		Collections.sort(data);
		dirty=false;
	    }

	    int data_index = 0; /*this can be made a global if we know it will be called in sequence*/
	    while(true)
	    {
		XYData current = data.get(data_index);
		if (data_index<data.size()-1)
		{
		    XYData next = data.get(data_index+1);
		    if (x>next.x)
			data_index++;
		    else
		    {
			double t = (x-current.x)/(next.x-current.x);
			if (t<0) t=0;

			/*interpolate*/
			return current.y+t*(next.y-current.y);
		    }			
		}
		else return current.y;	/*reached end of data*/
	    }
    }

    /**
     *
     */
    public class XYData implements Comparable
    {
	/**
	 *
	 */
	public double x;

	/**
	 *
	 */
	public double y;
	XYData(double x, double y) {this.x = x; this.y=y;}

	@Override
	public int compareTo(Object o)
	{
	    XYData t2 = (XYData)o;

	    if (x<t2.x) return -1;
	    else if (x==t2.x) return 0;
	    else return 1;
	}
    }

}
