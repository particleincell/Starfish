/* Support for specifying energy sources and sinks
 *
 * (c) 2012-2019 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 */
package starfish.interactions;

import java.util.ArrayList;
import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.common.LinearList;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.Material;
import starfish.interactions.InteractionsModule.InteractionFactory;

/**
 *
 * @author Lubos Brieda
 */
public class EnergyTransfer extends VolumeInteraction
{
    ArrayList<Material> sources = new ArrayList();

    public enum EnergyTransferModel {LINE_EMISSION, SAHA};
    EnergyTransferModel model;
    LinearList line_em_list;
    double mult;
    double degeneracy_ratio;
    double delta_e;
    
    EnergyTransfer(Element element)
    {
	/*get sources and products*/
	String source_names[]= InputParser.getList("sources", element);
	
	for (String name:source_names)
	{
	    Material mat = Starfish.getMaterial(name);
	    sources.add(mat);
	}
	
	String model_name = InputParser.getValue("model", element);
	try{
	    model = EnergyTransferModel.valueOf(model_name.toUpperCase());
	} catch (Exception e)
	{
	    Log.error("Unknown model "+model_name);
	}
	
	if (model==EnergyTransferModel.LINE_EMISSION)
	{
	    double ev[] = InputParser.getDoubleList("line_emission_ev", element);
	    double erg[] = InputParser.getDoubleList("line_emission_erg",element);
	    line_em_list = new LinearList(ev,erg);
	    if (line_em_list.isEmpty())
		Log.error("No data specified for line emission");
	    mult = InputParser.getDouble("multiplier",element,1.0e-7);	//conversion from erg to J
	}
	else if (model==EnergyTransferModel.SAHA)
	{
	    degeneracy_ratio = InputParser.getDouble("degeneracy_ratio", element);
	    delta_e = InputParser.getDouble("delta_e", element)*Constants.EVtoJ;
	    mult = InputParser.getDouble("multiplier",element,1.0);	

	}
	
    }
        @Override
    public void clearSamples() {
	Log.error("Not yet implemented!");
    }
    
    @Override
    public void perform()
    {
	switch(model)
	{
	    case LINE_EMISSION: performLineEmission();break;
	    case SAHA: performSaha();break;
	    
	}
    }

    @Override
    public void init()
    {
	/*don't do naything*/
    }
    
    /*loops trhough all nodes and on each computes the energy loss due to line emission 
    using provided density normalized AMDIS data (erg/s/# / (#/m^3) = (erg/s)*m^3
    */
    void performLineEmission()
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    Material source = sources.get(0);
	    double S[][] = source.getS(mesh).getData();
	    double nd[][] = source.getDen(mesh).getData();
	    double Te[][] = source.getT(mesh).getData();
	    for (int i=0;i<mesh.ni;i++)
		for (int j=0;j<mesh.nj;j++)
		{
		    //convert to eV
		    double Te_eV = Te[i][i]*Constants.KtoEV;
		    
		    double r = line_em_list.eval(Te_eV)*mult;  //data is assumed to be in ergs
		    double dS = r*nd[i][j]/mesh.nodeVol(i,j);
		    
		    S[i][j] -= dS;	    //data is assumed to be in ergs
		}
	}
	
    }
    
    /*estimates energy loss from ionization per the Saha equation
    */
    void performSaha()
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    Material source0 = sources.get(0);
	    Material source1 = sources.get(1);
	    
	    double S0[][] = source0.getS(mesh).getData();
	    double nd0[][] = source0.getDen(mesh).getData();
	    double T0[][] = source0.getT(mesh).getData();
	    
	    double S1[][] = source1.getS(mesh).getData();
	    double nd1[][] = source1.getDen(mesh).getData();
	    double T1[][] = source1.getT(mesh).getData();
	    
	    for (int i=0;i<mesh.ni;i++)
		for (int j=0;j<mesh.nj;j++)
		{
		    /*density averaged temperature*/
		    double T = ((T0[i][j]*nd0[i][j] + T1[i][j]*nd1[i][j])/(nd0[i][j]+nd1[i][j]));
		    
		    if (Constants.K*T<delta_e)
			continue;
		
		    double lambda = Math.sqrt(Constants.H*Constants.H/(2*Math.PI*Constants.ME*Constants.K*T));
		    
		    double s = 2/(lambda*lambda*lambda)*degeneracy_ratio*Math.exp(-delta_e/(Constants.K*T));
		   
		    double n = nd0[i][j] + nd1[i][j];
		    double ne = (-s+Math.sqrt(s*s+4*s*n))/2;
		    if (ne<0) ne=0;
		    if (ne>n) ne = n;
		    
		    double dn = ne-nd1[i][j];	//positive #/m^3 if ionization
		    
		    double dS = dn*delta_e/Starfish.getDt()*mult;
		    
		    S0[i][j] -= dS;	    //data is assumed to be in ergs
		}
	}
	
    }
       /**Parses <chemical_raction> element*/
    static InteractionFactory energyTransferFactory = new InteractionFactory()
    {
	@Override
	public void getInteraction(Element element)
	{
   
	    /*TODO: create appropriate model*/
	    Starfish.interactions_module.addInteraction(new EnergyTransfer(element));
		
	    /*log*/
	    //sim.log(Level.LOG, "Added CHEMICAL_REACTION "+source_name+" + " +target_name + " -> "+product_name);
	}
    };
}
