/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import starfish.core.common.Constants;

/**Linear Segment*/
class LinearSegment extends Segment
{
    double normal[] = new double[3];
    double tangent[] = new double[3];

    LinearSegment (double x1[],double x2[]) 
    {
	this.x1 = x1.clone(); this.x2=x2.clone();

	/*normal vector*/
	double dx = x2[0]-x1[0];
	double dy = x2[1]-x1[1];

	/*length*/
	length  = Math.sqrt(dx*dx + dy*dy);	
	
	/*normalize*/
	dx /= length;
	dy /= length;

	/*tangent vector*/
	tangent[0] = dx;
	tangent[1] = dy;
	tangent[2] = 0;
	
	/*normal vector*/
	normal[0] = -dy;
	normal[1] = dx;
	normal[2] = 0;

	/*centroid*/
	centroid = pos(0.5);
    }

    @Override
    public double[] normal(double t) {return normal;}

    @Override
    public double[] tangent(double t) {return tangent;}
    
    @Override
    public double closestPos(double xp[])
    {
	return 0.5;
    }
    
    @Override
    public final double[] pos(double t) 
    {
	double x[] = new double[3];

	x[0] = x1[0]+t*(x2[0]-x1[0]);
	x[1] = x1[1]+t*(x2[1]-x1[1]);
	return x;
    }

    @Override
    public double[] intersect(Segment s2) 
    {
	return intersect(s2.firstPoint(),s2.lastPoint());
    }

        /**intersects line segments, 
    * @return t[0]	 parametric intersection for parent segment
    * @return t[1]  parametric intersection for segment s2*/
    @Override
    public double[] intersect(double p3[], double p4[]) 
    {
	double p1[] = firstPoint();
	double p2[] = lastPoint();

	double t[] = new double[2];

	double xp[] = InfiniteLineIntersect(p1,p2,p3,p4);
	if (xp==null)
	{
	    t[0]=-1;t[1]=-1;return t;
	}
	    
	
	/*check that xp is inside the segment 1*/
	if (Math.abs(p2[0]-p1[0])>1e-6)
	    t[0]=(xp[0]-p1[0])/(p2[0]-p1[0]);
	else
	    t[0]=(xp[1]-p1[1])/(p2[1]-p1[1]);

	if (t[0]<-Constants.FLT_EPS || t[0]>(1+Constants.FLT_EPS)) 
	{
	    t[0]=-1;	/*no intersection*/
	    t[1]=-1;
	    return t;
	}

	/*check that xp is inside the segment 2*/
	if (Math.abs(p4[0]-p3[0])>1e-6)
	    t[1]=(xp[0]-p3[0])/(p4[0]-p3[0]);
	else
	    t[1]=(xp[1]-p3[1])/(p4[1]-p3[1]);

	if (t[1]<-Constants.FLT_EPS || t[1]>(1+Constants.FLT_EPS)) 
	{
	    t[0]=-1;
	    t[1]=-1;
	    return t;
	}

	/*get rid of floating point errors*/
	if (t[0]<0) t[0]=0;
	if (t[1]<0) t[1]=0;
	if (t[0]>1) t[0]=1;
	if (t[1]>1) t[1]=1;

	return t;
    }

    /**intersects two infinite lines
     @return intersection point or null if parallel*/
    static public double[] InfiniteLineIntersect(double p1[], double p2[], double p3[], double p4[])
    {
	double xp[]=new double[3];
	double x1,x2,x3,x4;
	double y1,y2,y3,y4;
	x1=p1[0]; x2=p2[0]; x3=p3[0]; x4=p4[0];
	y1=p1[1]; y2=p2[1]; y3=p3[1]; y4=p4[1];

	/*from wikipedia*/
	double den=(x1-x2)*(y3-y4)-(y1-y2)*(x3-x4);
	if (den==0) return null;
	    
	xp[0]=((x1*y2-y1*x2)*(x3-x4)-(x1-x2)*(x3*y4-y3*x4))/den;
	xp[1]=((x1*y2-y1*x2)*(y3-y4)-(y1-y2)*(x3*y4-y3*x4))/den;
	return xp;
    }

    @Override
    public double evalT(double[] pos)
    {
	if (Math.abs(x2[0]-x1[0])>Math.abs(x2[1]-x1[1]))
	{
	    return (pos[0]-x1[0])/(x2[0]-x1[0]);
	}
	else
	{
	    return (pos[1]-x1[1])/(x2[1]-x1[1]);
	}
    }

}
