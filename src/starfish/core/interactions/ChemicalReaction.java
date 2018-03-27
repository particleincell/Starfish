/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.core.interactions;

import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.interactions.InteractionsModule.InteractionFactory;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.source.VolumeSource;

/** base for chemical reactions*/
public class ChemicalReaction extends VolumeInteraction
{

    /**
     *
     */
    public Material source_mat[];

    /**
     *
     */
    public Material prod_mat[];

    /**
     *
     */
    public double source_coeff[];

    /**
     *
     */
    public double prod_coeff[];

    final int num_sources,num_products;
    String source_names[];
    String product_names[];
    boolean initialized=false;

    /**
     *
     */
    public VolumeSource prod_source[];
    RateParser rate_parser;
    
    /**
     *
     * @param source_names
     * @param product_names
     * @param rate_parser
     */
    public ChemicalReaction(String source_names[], String product_names[], RateParser rate_parser)
    {
	this.source_names = source_names;
	this.product_names = product_names;
	this.rate_parser = rate_parser;
	
	num_sources = source_names.length;
	num_products = product_names.length;
    }
	
    /**
     *
     */
    @Override
    public void init()
    {
	/*create array of sources and products*/
	source_mat = new Material[num_sources];
	source_coeff = new double[num_sources];
		
	prod_mat = new Material[num_products];
	prod_coeff = new double[num_products];
		
	/*parse source and product materials, can be specified as coeff*mat_name*/
	for (int i=0;i<num_sources;i++)
	{
	    source_coeff[i] = 1;
			
	    String pieces[] = source_names[i].split("\\s*\\*\\s*");
	    if (pieces.length==1)
		source_mat[i] = Starfish.getMaterial(source_names[i]);
	    else if (pieces.length==2)
	    {
		source_coeff[i] = Double.parseDouble(pieces[0]);
		source_mat[i] = Starfish.getMaterial(pieces[1]);
	    }
	    else
		Log.error(String.format("Unrecognized syntax %s, must be coeff*mat",source_names[i]));
	    
	    if (!(source_mat[i] instanceof KineticMaterial))
	    	Log.warning("consumeSources not yet implemented for non-kinetic materials, "+source_mat[i].name+
			" density will not change in chemical reaction!");
	}
		
	for (int i=0;i<num_products;i++)
	{
	    prod_coeff[i] = 1;
			
	    String pieces[] = product_names[i].split("\\s*\\*\\s*");
	    if (pieces.length==1)
		prod_mat[i] = Starfish.getMaterial(product_names[i]);
	    else if (pieces.length==2)
	    {
		prod_coeff[i] = Double.parseDouble(pieces[0]);
		prod_mat[i] = Starfish.getMaterial(pieces[1]);
		
		/*do we already have this material in source?*/
		for (int j=0;j<num_sources;j++)
		{
		    if (prod_mat[i]==source_mat[j] && prod_mat[i] instanceof KineticMaterial)
		    {
			/*adjust coefficients*/
			if (prod_coeff[i]>= source_coeff[j])
			{
			    prod_coeff[i] -= source_coeff[j];
			    
			    /*for consistency this should be decreased but then the rate won't be calculated right,
			     * TODO: fix me*/
			    //source_coeff[j] = 0;
			}
			else
			{
			//    source_coeff[j] -= prod_coeff[i];
			  //  prod_coeff[i] = 0;
			}
			
		    }
			
		}
	    }
	    else
		Log.error(String.format("Unrecognized syntax %s, must be coeff*mat",product_names[i]));
	}
			
	rate = new FieldCollection2D(Starfish.getMeshList(),null);
	
	dn_source = new FieldCollection2D[num_sources];
	for (int s=0;s<num_sources;s++)
	    dn_source[s] = new FieldCollection2D(Starfish.getMeshList(),null);
	
	/*create new volume source*/
	prod_source = new VolumeSource[num_products];
	for (int p=0;p<num_products;p++)
	{
		prod_source[p] = new VolumeSource("cr:"+prod_mat[p].name,Starfish.getMaterial(prod_mat[p].name));		
		Starfish.source_module.addVolumeSource(prod_source[p]);
	}
	
	
	initialized=true;
    }
			
    /**
     *
     */
    @Override
    public void perform() 
    {
	evalRate();	
	updateSource();
	consumeSources();
    }

    /**evaluates rate*/
    public void evalRate() 
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    int ni = mesh.ni;
	    int nj = mesh.nj;
	    
	    double k[][] = rate.getField(mesh).getData();
	    
	    /*temperature*/
	    double T[][] = Starfish.getMaterial(rate_parser.dep_var_mat).getT(mesh).getData();
	    
	    for (int i=0;i<ni;i++)
		for (int j=0;j<nj;j++)
		{	    

		    k[i][j] = rate_parser.eval(T[i][j]); 	
		    if (i==0 && T[i][j]>0.1*Constants.EVtoK)
			i=i;
		}
		    
	}		
    }
	
    /**updates dn and temperature on the underlying volume source*/
    public void updateSource() 
    {
	double dt=Starfish.getDt();
	for (Mesh mesh:Starfish.getMeshList())
	{
	    int ni = mesh.ni;
	    int nj = mesh.nj;
	    
	    double temp[][][] = new double[num_sources][][];
	    double den[][][] = new double[num_sources][][];
	    
	    for (int s=0;s<num_sources;s++)
	    {
		temp[s] = source_mat[s].getT(mesh).getData();
		den[s] = source_mat[s].getDen(mesh).getData();
	    }
	    
	    Field2D dn_src[] = getDnSource(mesh);
	    double k[][] = rate.getField(mesh).getData();
	 
	    double tot_source_mass=0;
	    for (int s=0;s<num_sources;s++)
		tot_source_mass+=source_mat[s].mass;
	    
	    /*todo: optimize all this by moving the loops outside i/j?*/
	    for (int i=0;i<ni;i++)
		for (int j=0;j<nj;j++)
		{
		    double dn = k[i][j] * dt;
		    if (dn==0) continue;
		    
		    double prod_temp=0;
		   
		    /*max we can remove is k*dt*n so need n<1*/
		    if (dn>1.0) 
			dn=1.0;
		    		    
		    for (int s=0;s<num_sources;s++)
		    {
			dn*=den[s][i][j];
			prod_temp+=temp[s][i][j]*source_mat[s].mass;
		    }
	
		    //clear tiny round off values
		    if (dn<1) dn=0;
		    
		    /*set temperature*/
		    /*TODO: take into account energy equation,etc..., just setting to target temp for now*/
		    for (int p=0;p<num_products;p++)
			prod_source[p].getTemp(mesh).data[i][j]=prod_temp/tot_source_mass;
		    
		    /*update rates*/
		    for (int s=0;s<num_sources;s++)
		    {
			dn_src[s].data[i][j] -= source_coeff[s]*dn;
		    }
		    for (int p=0;p<num_products;p++)
		    {
			prod_source[p].getDn(mesh).data[i][j] += prod_coeff[p]*dn;
		    }
		}
	}
    }

	
    /** this function removes mass from source materials that were
    * consumed during the chemical reaction
    * 
    TODO: implement for fluid species
    TODO: speed up this algorithm, linked list of particles per cell?
    */
    void consumeSources()
    {
	int count=0;
	for (int s=0;s<num_sources;s++)
	{
	    if (!(source_mat[s] instanceof KineticMaterial) )
		    continue;
	  
	    KineticMaterial ks = (KineticMaterial) source_mat[s];
		
	    for (Mesh mesh:Starfish.getMeshList())
	    {
		Iterator<Particle> iterator = ks.getIterator(mesh);
		Field2D dn = dn_source[s].getField(mesh);
				
		while(iterator.hasNext())
		{
		    Particle part = iterator.next();
		    int i=(int)(part.lc[0]+0.5);
		    int j=(int)(part.lc[1]+0.5);
				
		    double dm =  -dn.at(i,j)*mesh.nodeVol(i,j);
		    double dn_cons;
		    if (dm>0)
		    {
			if (dm>=part.spwt)
			{
			    dn_cons=part.spwt;
			    iterator.remove();
			}
			else
			{
			    part.spwt -= dm;
			    dn_cons = dm;
			}

			dn_cons /=mesh.nodeVol(i,j);
			dn.add(i,j,dn_cons);
		    }
		} /*while iterator*/
	    } /*for mesh*/
	}
    } /*for s*/
    
    /**Parses <chemical_raction> element*/
    static InteractionFactory chemicalReactionFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
	    /*get sources and products*/
	    String sources[] = InputParser.getList("sources", element);
	    String products[] = InputParser.getList("products",element);

	    Element el = InputParser.getChild("rate", element);
	    if (el==null) Starfish.Log.error("Chemical reaction <rate> not specified.");
	    
	    RateParser rp = new RateParser(el);
	    
	    /*TODO: create appropriate model*/
	    Starfish.interactions_module.addInteraction(new ChemicalReaction(sources,products,rp));
		
	    /*log*/
	    //sim.log(Level.LOG, "Added CHEMICAL_REACTION "+source_name+" + " +target_name + " -> "+product_name);
	}
    };

}
