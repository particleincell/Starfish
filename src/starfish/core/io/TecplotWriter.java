/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.io;

import java.util.ArrayList;
import java.util.Iterator;
import starfish.core.boundaries.Boundary;
import starfish.core.boundaries.Field1D;
import starfish.core.boundaries.Segment;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.domain.Field2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.Node;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;

/**saves 2D data in a simple ASCII Tecplot(R) format*/
public class TecplotWriter extends Writer
{

    /**
     *
     */
    @Override
    public void writeHeader() 
    {
	/*print header*/
	if (Starfish.domain_module.getDomainType()==DomainType.XY)
	    pw.print("VARIABLES = \"x (m)\" \"y (m)\"");
	else if (Starfish.domain_module.getDomainType()==DomainType.RZ)
	    pw.print("VARIABLES = \"r (m)\" \"z (m)\"");
	else if (Starfish.domain_module.getDomainType()==DomainType.ZR)
	    pw.print("VARIABLES = \"z (m)\" \"r (m)\"");
	else 
	    Log.error("unknown domain type\n");
	
	if (output_type==OutputType.FIELD)
	    pw.print(" type");
	
	if (output_type==OutputType.BOUNDARIES)
	    pw.print(" segment ni nj");
		
	for (String var:scalars)
	{
	    /*make sure we have this variable*/
	 //   if (!Starfish.output_module.validateVar(var)) continue;
			
	    /*print var name*/
	    pw.print(" \""+var+"\"");
	}
	
	pw.println();
    }
	
    /**
     *
     * @param animation
     */
    @Override
    public void writeZone2D(boolean animation)
    {		
	/*output all meshes*/
	for (int m=0;m<Starfish.getMeshList().size();m++)
	{
	    Mesh mesh=Starfish.getMeshList().get(m);
	    pw.printf("ZONE T=\"%s\" I=%d J=%d SOLUTIONTIME=%d STRANDID=%d\n",mesh.getName(),mesh.ni,mesh.nj,Starfish.getIt(),m+1);
        
	    /*save fields*/
	    ArrayList<Field2D> field = new ArrayList<Field2D>();
	    for (int v=0;v<scalars.length;v++)
	    {
		field.add(Starfish.getField(mesh, scalars[v]));
	    }
			
	    int nv = field.size();
		
	    for (int j=0;j<mesh.nj;j++)
		for (int i=0;i<mesh.ni;i++)
		{
		    double x[] = mesh.pos(i,j);
				
		    Node node = mesh.getNode(i,j);
		    int type = node.type.value();

		    pw.printf("%g %g %d", x[0], x[1],type);

		    for (int v=0;v<nv;v++)
		    {
			pw.printf(" %g",field.get(v).at(i, j));
		    }
		    pw.println();
		}
	    }
	}
 
	/**
	 * Saves field variables along a single mesh coordinate
	 */
	@Override
	public void writeZone1D() 
	{	
	    int im,ip;
	    int jm,jp;
		
	    im=0;ip=output_mesh.ni;
	    jm=0;jp=output_mesh.nj;
		
	if (dim==Dim.I)
	{
	    pw.printf("ZONE T=\"%s\" I=1 J=%d\n",output_mesh.getName(),output_mesh.nj);
	    im=index;
	    ip=im+1;
	}
	else if (dim==Dim.J)
	{
	    pw.printf("ZONE T=\"%s\" I=%d J=1\n",output_mesh.getName(),output_mesh.ni);
	    jm=index;
	    jp=jm+1;
	}
	else
	    Log.error("index type must be either I or J");

	/*save fields*/
	int nv=scalars.length;
	Field2D field[] = new Field2D[nv];
	for (int v=0;v<nv;v++)
	    field[v] = Starfish.domain_module.getField(output_mesh, scalars[v]);

	for (int j=jm;j<jp;j++)
	    for (int i=im;i<ip;i++)
	    {
		double x[] = output_mesh.pos(i,j);

		int type=-1;
		Node node = output_mesh.getNode(i,j);
		if (node.type==NodeType.DIRICHLET ||
		    node.type==NodeType.OPEN)
		    type = node.type.ordinal();

		output_mesh.getNode(i,j).type.ordinal();

		pw.printf("%g %g %d", x[0], x[1],type);

		for (int v=0;v<nv;v++)
		{
		    pw.printf(" %g",field[v].at(i, j));
		}

		pw.println();
	    }
		
	/*force output*/
	pw.flush();
    }

    /**
    * Saves data along surface boundaries
    */
    @Override
    public void writeZoneBoundaries() 
    {
	final int NP_SMOOTH=20;		//number of pieces a smooth segment is divided into

	for (Boundary boundary:Starfish.getBoundaryList())
	{
	    int np = 0;
	    
	    /*count number of smooth segments*/
	    for (int i=0;i<boundary.numSegments();i++)
	    {
		if (boundary.getSegment(i).isSmooth())
		    np+=NP_SMOOTH;
		else np+=1;
	    }
	    np++;   /*add final point*/
	    
	    pw.printf("ZONE I=%d T=\"%s\"",np,boundary.getName());
	    pw.println();
	    pw.flush();

	    /*save fields*/
	    int nv=0;
	    Field1D field[] = new Field1D[scalars.length];
	    for (int v=0;v<scalars.length;v++)
	    {
		field[v] = Starfish.boundary_module.getField(boundary, scalars[v]);	
		if (field[v]!=null) nv++;
		else Log.warning("Skipping unknown variable "+scalars[v]);
	    }
	
	    for (int i=0;i<boundary.numSegments();i++)
	    {
		Segment seg = boundary.getSegment(i);

		np=1;	/*default, linear segment*/
		if (seg.isSmooth()) np = NP_SMOOTH;

		/*final segment? need to write final point*/
		int np2=np;
		if (i==boundary.numSegments()-1) np2++;
		
		for (int k=0;k<np2;k++)
		{
		    double t= k/(double)np;
		    double x[] = seg.pos(t);
		    double norm[] = seg.normal(t);
		    if (Double.isNaN(norm[0]))
			seg.normal(t);
		    pw.printf("%g %g %d %g %g", x[0], x[1],i,norm[0],norm[1]);

		    for (int v=0;v<nv;v++)
			if (field[v]!=null)
			    pw.printf(" %g",field[v].gather_safe(i+t));
			else pw.printf(" 0");

		    pw.println();
		}
	    }  
			
	}
    }	

    /**
     *
     */
    @Override
    protected void writeParticles()
    {
	for (Material mat: Starfish.getMaterialsList())
	{
	    if (!(mat instanceof KineticMaterial)) continue;
	    
	    KineticMaterial km = (KineticMaterial) mat;
	    
	    /*TODO: add support for multiple meshes*/
	    if (km.mesh_data.length>1)
		Log.warning("writeParticle right now supports only one mesh");
	    km.mesh_data[0].getIterator();
	    long np = km.mesh_data[0].getNp();	    //long np = km.getNp();
	    
	    long count = Math.min(particle_count, np);
	    
	    if (count<=0) continue;
	    
	    pw.printf("ZONE I=%d T=\"%s\"\n",count,km.getName());
	    
	    double p_delta = count/np;
	    
	    Iterator<Particle>iterator = km.mesh_data[0].getIterator();
	    long p;
	    
	    /*random initial offset*/
	    int offset = (int)(Starfish.rnd()*p_delta);
	    for (p=0;p<offset;p++) iterator.next();

	    /*has next is needed to advance across blocks*/
	    while (p<count && iterator.hasNext())
	    {
		/*select next particle*/
		Particle part = iterator.next();
		
		
		pw.printf("%g %g %g %g %g %g %d\n", part.pos[0],part.pos[1],part.pos[2],
						     part.vel[0],part.vel[1],part.vel[2],
						     part.id);
		
		for (int i=1;i<p_delta;i++) iterator.next();
		p+=p_delta;
	    }

	}
	pw.flush();	
    }
    
    /**
     *
     * @param data
     */
    @Override
    public void writeData(double data[])
    {
	/*TODO: this algorithm may select duplicate particles, add some check to avoid this*/
	for (int i=0;i<data.length;i++)
	{
	    pw.printf("%g ", data[i]);
    	}
	pw.printf("\n");
	pw.flush();	
    }

}
