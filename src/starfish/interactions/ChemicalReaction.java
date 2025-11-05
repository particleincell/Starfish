/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.interactions;

import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.io.InputParser.DoubleStringPair;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.source.VolumeSource;
import starfish.interactions.InteractionsModule.InteractionFactory;

/** base for chemical reactions*/
public class ChemicalReaction extends VolumeInteraction
{
    public Material source_mat[];
    public Material prod_mat[];

    public double source_coeff[];
    public double prod_coeff[];

    final int num_sources,num_products;

    double energy_release;	    //amount of energy to add to products and remove from sources
    boolean initialized=false;

    double num_consumed;	//number of molecules consumed
    double num_created;		//number of molecules created
    /**
     *
     */
    public VolumeSource prod_source[];
    RateParser rate_parser;
    
    /**
     *

     */
    public ChemicalReaction(Element element)
    {
	ArrayList<DoubleStringPair> source_names = InputParser.getDoubleStringPairs("sources", element);
	ArrayList<DoubleStringPair> product_names = InputParser.getDoubleStringPairs("products", element);

	Element el_rate = InputParser.getChild("rate", element);
	
	if (el_rate==null) 
	    Starfish.Log.error("Chemical reaction is missing a <rate> or <sigma> element");
	    
	/*create array of sources and products*/
	num_sources = source_names.size();
	num_products = product_names.size();
	source_mat = new Material[num_sources];
	source_coeff = new double[num_sources];
		
	prod_mat = new Material[num_products];
	prod_coeff = new double[num_products];
		
	/*convert names to materials*/
	for (int i=0;i<num_sources;i++)
	{
	    source_coeff[i] = 1;
			
	    source_mat[i] = Starfish.getMaterial(source_names.get(i).s);
	    source_coeff[i] = source_names.get(i).d;
	}
		
	for (int i=0;i<num_products;i++)
	{
	    prod_coeff[i] = 1;
			
	    prod_mat[i] = Starfish.getMaterial(product_names.get(i).s);
	    prod_coeff[i] = product_names.get(i).d;
	}
	
	rate_parser = new RateParser(el_rate, source_mat, prod_mat);
	    
	/*energy release, this does not yet fully work*/
	energy_release = InputParser.getDouble("energy_release",element, 0);
	energy_release *= Constants.EVtoJ;	
	
	Log.log("Added CHEMICAL_REACTION ");

    }
	
    /**
     *
     */
    @Override
    public void init()
    {
			
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
	
	
	/*get field collection for the dependent variable*/
	rate_parser.dep_var_fc = Starfish.getFieldCollection(rate_parser.dep_var_name);
	if (rate_parser.dep_var_fc==null)
	    Log.error("Failed to find chemical reaction rate dependent variable "+rate_parser.dep_var_name);
	initialized=true;
    }
	
    @Override
    public void clearSamples() {
	/*do nothing*/
    }
    
    
    /**
     *
     */
    @Override
    public void perform() 
    {
	evalRate();	
	updateDn();
	consumeSourceMaterials();
    }

    /**evaluates rate*/
    public void evalRate() 
    {
	double mass_ave = 0;		//average mass of sources
	for (int i=0;i<source_mat.length;i++)
	    mass_ave+=source_mat[i].mass*source_coeff[i];
	mass_ave /= source_mat.length;
	
	for (Mesh mesh:Starfish.getMeshList())
	{
	    int ni = mesh.ni;
	    int nj = mesh.nj;
	    
	    double k[][] = rate.getField(mesh).getData();
	    
	    /*temperature*/
	    double vals[][] = rate_parser.dep_var_fc.getField(mesh).getData();
	    
	    for (int i=0;i<ni;i++)
		for (int j=0;j<nj;j++)
		{	
		    k[i][j] = 0;
		    if (vals[i][j]>0)	    //do not evaluate with negative temperature or density
			k[i][j] = rate_parser.eval(vals[i][j]); 			    
		    
		    //convert sigma to rate 
		    if (rate_parser.is_sigma)
		    {
			double T_ave = 0;
			int count = 0;
			for (Material mat:source_mat) 
			{
			    double T = mat.getT(mesh).at(i, j);
			    if (T>=0) {T_ave+=T;count++;}
			}
			if (count>0) T_ave/=count;
			if (T_ave<0) T_ave=0;
			
			double v_mean = 2*Math.sqrt(2*Constants.K*T_ave/(Constants.PI*mass_ave)); 
			k[i][j] *=v_mean;
		    }
	
		}		    
	}		
    }
	
    /**updates dn and temperature on the underlying volume source*/
    public void updateDn() 
    {
	num_created = 0;
	
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
		    double dn_dt = k[i][j];
		    if (dn_dt==0) continue;
		    
		    double prod_temp=0;
		   
		    for (int s=0;s<num_sources;s++)
		    {
			dn_dt*=den[s][i][j];
			prod_temp+=temp[s][i][j]*source_mat[s].mass;
		    }
	
		    double dn = dn_dt * dt;

		    //clear tiny round off values
		    if (dn<1) 
			continue;
		    
		    //can't create more products than we have sources
		    for (int s=0;s<num_sources;s++)
		    {
			if (dn>den[s][i][j]) dn=den[s][i][j];
		    }
		    
		    //convert back to rate
		    dn_dt = dn/dt;
		    
		    /*update energy density rate*/
		    double W = dn_dt * energy_release;
		    for (int s=0;s<num_sources;s++)
			source_mat[s].getS(mesh).data[i][j] -= source_coeff[s]*W;
		    
		    for (int p=0;p<num_products;p++)
			prod_mat[p].getS(mesh).data[i][j] += prod_coeff[p]*W;
		    
		    /*update rates*/
		    for (int s=0;s<num_sources;s++)
		    {
			dn_src[s].data[i][j] -= source_coeff[s]*dn;
		    }
		    for (int p=0;p<num_products;p++)
		    {
			prod_source[p].getDn(mesh).data[i][j] += prod_coeff[p]*dn;
		    }
		    
		    num_created += dn*mesh.nodeVol(i,j);
		}
	}
    }

	
    /** this function removes mass from source materials that were
    * consumed during the chemical reaction
    * 
    TODO: implement for fluid species
    TODO: speed up this algorithm, linked list of particles per cell?
    */
    void consumeSourceMaterials()
    {
	num_consumed = 0;
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
			if (dm>=part.mpw)
			{
			    dn_cons=part.mpw;
			    iterator.remove();
			    num_consumed+=part.mpw;
			}
			else
			{
			    part.mpw -= dm;
			    dn_cons = dm;
			    num_consumed+=dm;
			}

			dn_cons /=mesh.nodeVol(i,j);
			dn.add(i,j,dn_cons);
		    }
		} /*while iterator*/
	    } /*for mesh*/
	} /*for s*/
	Log.debug("num_consumed: "+num_consumed+", num_created: "+num_created);
    } 
    
    /**Parses <chemical_raction> element*/
    static InteractionFactory chemicalReactionFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{


	    Starfish.interactions_module.addInteraction(new ChemicalReaction(element));
		
	    /*log*/
	}
    };

}
