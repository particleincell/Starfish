/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.boundaries;

import starfish.core.common.Starfish;
import starfish.core.common.Vector;
import starfish.core.domain.DomainModule.DomainType;

/**Cubic segment given by the cubic Bezier spline*/
class CubicSegment extends Segment
{
    protected double p1[], p2[]; /*control points for cubic bezier spline*/
    protected double bx[], by[]; /*coefficients for computing intersections*/
    
    public CubicSegment(double x1[], double p1[], double p2[], double x2[])
    {
	this.x1 = x1.clone();
	this.x2 = x2.clone();
	this.p1 = p1.clone();
	this.p2 = p2.clone();

	smooth = true;
	computeLength();
	
	bx= bezierCoeffs(x1[0],p1[0],p2[0],x2[0]);
	by= bezierCoeffs(x1[1],p1[1],p2[1],x2[1]);
	
	/*centroid*/
	centroid = pos(0.5);
    }		

    /** 
     * Computes area swept by the segment. This currently multiplies the length by the 
     * midpoint radius. This is just an approximation for now until a more robust method is found.
     * Based on Pappus' theory but without actually computing the geometric centroid (error)
     * http://en.wikipedia.org/wiki/Pappus%27s_centroid_theorem
     */
    @Override	  
    double area(double t) {
	
	if (Starfish.getDomainType()==DomainType.XY) 
	    return t*length;
	
	//position of the midpoint half-way to our t
	double pos_mid[] = pos(0.5*t);
	double length_t = t*length;	//another estimate
	
	double r_mid;
	switch (Starfish.getDomainType()) {
	    case RZ: r_mid = pos_mid[0]; break;
	    case ZR: r_mid = pos_mid[1]; break;
	    default: return 0;
	}
	area = 2*Math.PI*r_mid*length_t;
	return area;
    }


    /**returns normal vector at position t*/
    @Override
    public double[] normal(double t) 
    {
	/*normal vector is given by N=T'/|T|, where T=x'
	 http://www.particleincell.com/2012/bezier-splines/ for equations*/
	double n[] = new double[3];
	
	double a=-3*(1-t)*(1-t);
	double b=3*(1-4*t+3*t*t);
	double c=3*(2*t-3*t*t);
	double d=3*t*t;
	
	n[1]=(a*x1[0]+b*p1[0]+c*p2[0]+d*x2[0]);
	n[0]=-(a*x1[1]+b*p1[1]+c*p2[1]+d*x2[1]);			

	double mag = Math.sqrt(n[0]*n[0]+n[1]*n[1]);
	n[0]/=mag;
	n[1]/=mag;
	return n;
    }

    @Override
    public double[] tangent(double t)
    {
	double tang[] = new double[3];
	tang[0] = 3*bx[0]*t*t+2*bx[1]*t+bx[2];
	tang[1] = 3*by[0]*t*t+2*by[1]*t+by[2];
	
	double mag = Math.sqrt(tang[0]*tang[0]+tang[1]*tang[1]+tang[2]*tang[2]);
	tang[0] /= mag;
	tang[1] /= mag;
	
	return tang;
    }

    @Override
    final public double[] pos(double t) 
    {
	double t2 = t*t;
	double t3 = t2*t;
	
	double x[] = new double[3];
	x[0]=bx[0]*t3+bx[1]*t2+bx[2]*t+bx[3];
	x[1]=by[0]*t3+by[1]*t2+by[2]*t+by[3];
	
	return x;
    }

    protected final double[] bezierCoeffs(double P0, double P1, double P2, double P3)
    {
	double Z[] = new double[4];
	Z[0] = -P0 + 3*P1 - 3*P2 + P3; 
        Z[1] = 3*P0 - 6*P1 + 3*P2;
        Z[2] = -3*P0 + 3*P1;
        Z[3] = P0;
	return Z;
    }
    
    /**computes bezier curve segment lengths using algorithm from
    from http://processingjs.nihongoresources.com/bezierinfo/#intoffsets_c*/
    public final void computeLength()
    {
	/*table for quassian quadrature of order 4*/
	double gw[] = {0.6521451548625461,0.6521451548625461,0.3478548451374538,0.3478548451374538};
	double gx[] = {-0.3399810435848563,0.3399810435848563,-0.8611363115940526,0.8611363115940526};

	double sum=0;

	/*gaussian quadrature*/
	for (int j=0;j<4;j++)
	{
	    double t = 0.5*(gx[j]+1);
	    double bx = base3(t,x1[0],p1[0],p2[0],x2[0]);
	    double by = base3(t,x1[1],p1[1],p2[1],x2[1]);
	    double ft = Math.sqrt(bx*bx+by*by);
	    sum+=gw[j]*ft;
	}

	length=0.5*sum;		
    }

    /**from http://processingjs.nihongoresources.com/bezierinfo/#intoffsets_c*/
    protected double base3(double t, double p1, double p2, double p3, double p4)
    {
	return t*(t*(-3*p1+9*p2-9*p3+3*p4)+6*p1-12*p2+6*p3)-3*p1+3*p2;
    }

    /** returns position on the segment closest to some arbitrary point xp using Newton's method*/
    @Override
    public double closestPos(double[] xp)
    {
   	double s=0;
	double min=1e66;
	
	for (double t=0;t<=1.0;t+=0.1)
	{
	    double dist = Vector.dist2(xp, pos(t));
	    if (dist<min) {min=dist;s=t;}  
	}
	
	double dprime,ddprime;
	int it = 0;
	double dist;
	
	/*use this as initial guess to newton's method*/
	do 
	{
	final double dh=0.005;
	double pl[] = pos(s-dh);
	double pr[] = pos(s+dh);
	double pc[] = pos(s);
	    
	    
	double dl = (pl[0]-xp[0])*(pl[0]-xp[0])+
		    (pl[1]-xp[1])*(pl[1]-xp[1]);
	double dc = (pc[0]-xp[0])*(pc[0]-xp[0])+
			(pc[1]-xp[1])*(pc[1]-xp[1]);
	double dr = (pr[0]-xp[0])*(pr[0]-xp[0])+
			(pr[1]-xp[1])*(pr[1]-xp[1]);
	
	if (s>=dh && s<=(1-dh))
	{
	    dprime = (dr-dl)/(2*dh);
	    ddprime = (dr-2*dc+dl)/(dh*dh);
	}
	else if (s<dh)
	{
	    dprime = (dr-dc)/(dh);
	    ddprime = (dr-2*dc+dl)/(dh*dh);
	}
	else
	{
	    dprime = (dc-dl)/(dh);
	    ddprime = (dr-2*dc+dl)/(dh*dh);
	}
	    
	s -= dprime/ddprime;
	
	/*do not capture end points since this function is used to compute node location and at end point we 
	 * have another line */
	if (s<1e-4) s=1e-4;
	if (s>0.9999) s=0.9999;
	dist = Vector.dist2(xp, pos(s));
	it++;
	} while (Math.abs(dprime/ddprime)>1e-3 && it<10);
	
	return s;
	
    }
    
     /*TODO: this is just an approximation, develop a more robust algorithm*/
    @Override
    public double evalT(double xp[])
    {
	return closestPos(xp);
    }

    @Override
    public double[] intersect(Segment s2)
    {
	if (!s2.isSmooth())
	    return intersect(s2.firstPoint(),s2.lastPoint());
	else
	    throw new UnsupportedOperationException("Not supported yet."); 
    }

    /**Intersects a cubic segment with a line
     vaguely based on 
     * http://stackoverflow.com/questions/14005096/mathematical-solution-for-bezier-curve-and-line-intersection-in-coffeescript-or
     */
    @Override
    public double[] intersect(double L1[], double L2[])
    {
	double A=L2[1]-L1[1];	    //A=y2-y1
	double B=L1[0]-L2[0];	    //B=x1-x2
	double C=L1[0]*(L1[1]-L2[1]) + 
		L1[1]*(L2[0]-L1[0]);	//C=x1*(y1-y2)+y1*(x2-x1)

	double P[] = new double[4];
    	P[0] = A*bx[0]+B*by[0];		/*t^3*/
	P[1] = A*bx[1]+B*by[1];		/*t^2*/
	P[2] = A*bx[2]+B*by[2];		/*t*/
	P[3] = A*bx[3]+B*by[3] + C;	/*1*/
	
	double r[] = cubicRoots(P);
	
	double tt[] = {-1,-1}; /*t[0] is intersection along the cubic, t[1] is along the line*/
	
	/*verify the roots are in bounds of the linear segment*/
	for (int i=0;i<3;i++)
	{
	    double t = r[i];
	    
	    /*is this intersection in spec?*/
	    if (t<-1e-8 || t>1.00000001) continue;
	    if (t<0) t=0;
	    if (t>1) t=1;
	    
	    double X[] = pos(t);
	    
	    /*above is intersection point assuming infinitely long line segment, make sure we are also in bounds of the line*/
	    double s;
	    if ((L2[0]-L1[0])!=0)           /*if not horizontal line*/
		s=(X[0]-L1[0])/(L2[0]-L1[0]);
	    else
		s=(X[1]-L1[1])/(L2[1]-L1[1]);
        
	    /*in bounds and new low?*/
 	    /*note, we are checking for >+1e-6 since we don't want intersections at t[1]~0, since this is the particle 
	     * starting position, and we could miss another hit with the same curved segment*/
	    if (s>=+1e-6 && s<=1.000001 && (s<tt[1] || tt[1]<0))
	    {
		if (s<0) t=0;
		if (s>1) t=1;
	    
		tt[0] = t;
		tt[1] = s;
	    }
	}
	
	return tt;
    }

    /* computes and returns real only roots of a cubic equation
     * based on http://mysite.verizon.net/res148h4j/javascript/script_exact_cubic.html#the%20source%20code*/
    protected double[] cubicRoots(double P[])
    {
	double A=P[1]/P[0];
	double B=P[2]/P[0];
	double C=P[3]/P[0];

	double Q = (3*B - A*A)/9.0;
	double R = (9*A*B - 27*C - 2*A*A*A)/54.0;
	double D = Q*Q*Q + R*R;    // polynomial discriminant

	double t[] = {-1,-1,-1};
	
	if (D >= 0)                                 // complex or duplicate roots
	{
	    double sqrtD=Math.sqrt(D);
	    double S = Math.signum(R + sqrtD)*Math.pow(Math.abs(R + sqrtD),(1/3.0));
	    double T = Math.signum(R - sqrtD)*Math.pow(Math.abs(R - sqrtD),(1/3.0));

	    t[0] = -A/3.0 + (S + T);                    // real root
	    
	    /*
       	    final double sqrt3=Math.sqrt(3);
	    t[1] = -A/3.0 - 0.5*(S + T);                  // real part of complex root
	    t[2] = -A/3.0 - 0.5*(S + T);                  // real part of complex root
	    double Im = Math.abs(sqrt3*0.5*(S - T));    // complex part of root pair   
        
	    if (Im!=0)
	    {
		t[1]=-1;
		t[2]=-1;
	    } 
	    */
	}
	else	/*real roots only*/
	{
	    double th = Math.acos(R/Math.pow(-Q,1.5));
	    double sqrtQ=Math.sqrt(-Q);
	    
	    t[0] = 2*sqrtQ*Math.cos(th/3.0) - A/3.0;
	    t[1] = 2*sqrtQ*Math.cos((th + 2*Math.PI)/3.0) - A/3.0;
	    t[2] = 2*sqrtQ*Math.cos((th + 4*Math.PI)/3.0) - A/3.0;
	}
    
	return t;
    }

    
    static CubicSegment newCubicSegment(double x1[], double p1[], double p2[], double x2[])
    {
	return new CubicSegment(x1,p1,p2,x2);
    }
}
