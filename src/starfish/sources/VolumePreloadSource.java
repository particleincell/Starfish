/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/

package starfish.sources;

import org.w3c.dom.Element;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Mesh;
import starfish.core.io.InputParser;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.source.SourceModule;
import starfish.core.source.VolumeSource;

public class VolumePreloadSource extends VolumeSource
{
   protected double den0;
   protected double temp0;
    
    /*constructor*/
    public VolumePreloadSource(String name, Material source_mat, double temp, double den)
    {
	super(name, source_mat);

	this.den0 = den;
	this.temp0 = temp;			
    }

    protected boolean first_time = true;
      
    @Override
    public void regenerate()
    {
	if (first_time) 
	{
	    double vol=0;
	    for (Mesh mesh:Starfish.getMeshList())
	    {
		dn.getField(mesh).setValue(den0);
		getTemp(mesh).setValue(temp0);
	
		for (int i=0;i<mesh.ni-1;i++)
		    for (int j=0;j<mesh.nj-1;j++)
			vol+=mesh.cellVol(i, j);
	    }
	    KineticMaterial ks = (KineticMaterial) source_mat;
	    Log.log(String.format(" Loading approximately %d %s particles",(int)(vol*den0/ks.getSpwt0()),source_mat.name));
	    num_rem=0;
	    super.regenerate();
	}
	
	first_time = false;		
    }

    @Override
    public void sampleFluid() 
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    int ni = mesh.ni;
	    int nj = mesh.nj;
	    double den_data[][] = source_mat.getDen(mesh).getData();
	    double dn_local[][] = dn.getField(mesh).getData();
	    for (i_sample=0;i_sample<ni;i_sample++)
		for (j_sample=0;j_sample<nj;j_sample++)
		    den_data[i_sample][j_sample]+=dn_local[i_sample][j_sample];
	}
    }

        static public SourceModule.VolumeSourceFactory preloadSourceFactory = new SourceModule.VolumeSourceFactory()
    {
	@Override
	public void makeSource(Element element, String name, Material material)
	{
	    /*drift velocity and temperature*/
	    double temp = Double.parseDouble(InputParser.getValue("temp", element));
	    double den = Double.parseDouble(InputParser.getValue("density", element));

	    VolumePreloadSource source = new VolumePreloadSource(name, material, temp, den);
	    Starfish.source_module.addVolumeSource(source);

	    /*log*/
	    Starfish.Log.log("Added PRELOAD source '" + name + "'");
	    Starfish.Log.log("> den  = " + den);
	}
    };

}
