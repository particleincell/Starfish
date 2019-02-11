/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.materials;

import org.w3c.dom.Element;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.Field2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.domain.UniformMesh;
import starfish.core.io.InputParser;
import starfish.core.materials.MaterialsModule.MaterialParser;

/*9.2.1 in Tanehill*/
public class FluidDiffusionMaterial extends Material
{
    /*constructor*/

    public FluidDiffusionMaterial(String name, Element element)
    {
	super(name, element);
	/*fluid elements need viscosity*/
	mu = InputParser.getDouble("mu", element);
	
	/*log*/
	Log.log("Added FLUID material '"+name+"'");
	Log.log("> charge   = "+charge);
	Log.log("> mass  = "+mass);	
	Log.log("> mu  = "+mu);
    }

    @Override
    public void init()
    {
	super.init();
	Mesh mesh = Starfish.getMeshList().get(0);
	Log.warning("FLUID-DIFFUSION CURRENTLY ONLY UPDATING 0D TEMPERATRE");
	
	/*check CFL*/
	double dx = mesh.pos1(1, 0) - mesh.pos1(0, 0);
	double dy = mesh.pos2(0, 1) - mesh.pos2(0, 0);
	double u = 1000;				/*1000 is assumed speed*/
	double Re = mass * 1e19 * dx * u / mu;
	double a = Math.sqrt((5.0 / 3.0) * Constants.K * 300 / mass);	/*assuming 300K*/
	double dt_cfl = 1.0 / (u / dx + a * Math.sqrt(1 / (dx * dx) + 1 / (dy * dy)));
	double dt = 0.9 * dt_cfl / (1 + 2.0 / Re);
	Log.message("Computed dt for stability is " + dt);

    }
    /*variables*/
    protected double mu;    /*viscosity*/

    /*methods*/

    @Override
    public void updateFields()
    {
	if (true)
	{
	    UpdateTemperature0D();
	    return;
	}
	
	int i, j;
	int ni, nj;
	double dt = Starfish.getDt();

	/*TODO: write multi-mesh solver*/
	UniformMesh mesh = (UniformMesh) Starfish.getMeshList().get(0);
	Field2D Den = getDen(mesh);
	Field2D U = getU(mesh);
	Field2D V = getV(mesh);

	ni = mesh.ni;
	nj = mesh.nj;

	double D[][][];
	double D2[][][];
	double E[][][];
	double F[][][];
	int s;

	D = new double[3][][];
	D2 = new double[3][][];
	E = new double[3][][];
	F = new double[3][][];

	double dx = mesh.getDi();
	double dy = mesh.getDj();

	for (s = 0; s < 3; s++)
	{
	    D[s] = new double[ni][nj];
	    D2[s] = new double[ni][nj];
	    E[s] = new double[ni][nj];
	    F[s] = new double[ni][nj];
	}

	updateVectors(mesh, D, E, F, true);

	for (i = 0; i < ni; i++)
	{
	    System.arraycopy(D[0][i], 0, D2[0][i], 0, nj);
	    System.arraycopy(D[1][i], 0, D2[1][i], 0, nj);
	    System.arraycopy(D[2][i], 0, D2[2][i], 0, nj);
	}
	/*update properties, MacCormack method*/
	/*predictor*/
	for (s = 0; s < 3; s++)
	{
	    for (i = 1; i < ni; i++)
	    {
		for (j = 0; j < nj; j++)
		{
		    if (mesh.nodeType(i, j) != NodeType.OPEN)
		    {
			continue;
		    }

		    double dE;
		    double dF;
		    
		    dE = 0;
		    if (i < ni - 1)
		    {
			dE = E[s][i + 1][j] - E[s][i][j];
		    }
		    dF = 0;
		    if (j < nj - 1)
		    {
			dF = F[s][i][j + 1] - F[s][i][j];
		    }

		    D2[s][i][j] = D[s][i][j] - dt / dx * dE - dt / dy * dF;
		}
	    }
	}
	updateDenVel(mesh, D2);

	/*update vectors using n+1 solution*/
	updateVectors(mesh, D, E, F, false);

	/*corrector*/
	for (s = 0; s < 3; s++)
	{
	    for (i = 1; i < ni; i++)
	    {
		for (j = 0; j < nj; j++)
		{
		    if (mesh.nodeType(i, j) != NodeType.OPEN)
		    {
			continue;
		    }

		    double dE;
		    double dF;
		    if ((Starfish.getIt() % 2 == 0) && (i > 0))
		    {
			dE = E[s][i][j] - E[s][i - 1][j];
		    } else if ((Starfish.getIt() % 2 == 1) && (i < ni - 1))
		    {
			dE = E[s][i + 1][j] - E[s][i][j];
		    }

		    if ((Starfish.getIt() % 2 == 0) && (j > 0))
		    {
			dF = (F[s][i][j] - F[s][i][j - 1]);
		    } else if ((Starfish.getIt() % 2 == 1) && (j < nj - 1))
		    {
			dF = (F[s][i][j + 1] - F[s][i][j]);
		    }

		    dE = 0;
		    if (i > 0)
		    {
			dE = E[s][i][j] - E[s][i - 1][j];
		    }
		    dF = 0;
		    if (j > 0)
		    {
			dF = F[s][i][j] - F[s][i][j - 1];
		    }

		    D[s][i][j] = 0.5 * (D[s][i][j] + D2[s][i][j] - dt / dx * dE - dt / dy * dF);

		}
	    }
	}
	updateDenVel(mesh, D);
	updateBoundaries();
    }

    void updateDenVel(Mesh mesh, double D[][][])
    {
	int i, j;

	Field2D Den = getDen(mesh);
	Field2D U = getU(mesh);
	Field2D V = getV(mesh);

	int ni = mesh.ni;
	int nj = mesh.nj;

	for (i = 1; i < ni; i++)
	{
	    for (j = 0; j < nj; j++)
	    {
		if (mesh.nodeType(i, j) != NodeType.OPEN)
		{
		    continue;
		}

		double den = D[0][i][j];
		double u = 0, v = 0;

		if (den < 0)
		{
//					Log.error("Negative density");
		    den = 0;
		}

		Den.set(i, j, den / mass);

		if (den > 0)
		{
		    u = D[1][i][j] / den;
		    v = D[2][i][j] / den;
		}


		U.set(i, j, u);
		V.set(i, j, v);

	    }
	}

    }

    void updateVectors(Mesh mesh, double D[][][], double E[][][], double F[][][], boolean updateD)
    {
	int i, j;

	Field2D Den = getDen(mesh);
	Field2D U = getU(mesh);
	Field2D V = getV(mesh);
	Field2D Temp = getT(mesh);

	for (i = 0; i < mesh.ni; i++)
	{
	    for (j = 1; j < mesh.nj; j++)
	    {
		double n = Den.at(i, j);
		double u = U.at(i, j);
		double v = V.at(i, j);
		double T = Temp.at(i, j);
		double P = n * Constants.K * T;

		double rho = n * mass;

		double txx, txy, tyy;
		double du_dx, dv_dy;
		double du_dy, dv_dx;

		du_dx = U.ddx(i, j);
		du_dy = U.ddy(i, j);
		dv_dx = V.ddx(i, j);
		dv_dy = V.ddy(i, j);

		txx = 2.0 / 3.0 * mu * (2.0 * du_dx - dv_dy);
		tyy = 2.0 / 3.0 * mu * (2.0 * dv_dy - du_dx);
		txy = mu * (du_dy + dv_dx);

		if ((i > 0 && mesh.nodeType(i, j) != NodeType.OPEN))
		{
		    //			u=0;
		    //			v=0;
		}
		P = 0;
		if (updateD)
		{
		    D[0][i][j] = rho;
		    D[1][i][j] = rho * u;
		    D[2][i][j] = rho * v;
		}

		E[0][i][j] = rho * u;
		E[1][i][j] = rho * u * u + P - txx;
		E[2][i][j] = rho * u * v - txy;

		F[0][i][j] = rho * v;
		F[1][i][j] = rho * u * v - txy;
		F[2][i][j] = rho * v * v + P - tyy;
	    }
	}
    }
    
    public static MaterialParser FluidDiffusionMaterialParser = new MaterialParser() {
	@Override
	public Material addMaterial(String name, Element element)
	{
	    Material material = new FluidDiffusionMaterial(name,element);
	    return material;
        }
    };
    
    /*uses 0D simplification of the energy equation to update temperature, ignores divergence terms
      d/dt (3/2*n*kT)=S
    
    TODO: the "S" term is generally non-linear, not exactly sure how to tie it in
    in a general way
    */
    private void UpdateTemperature0D()
    {
	for (Mesh mesh:Starfish.getMeshList())
	{
	    double n[][] = getDen(mesh).data;
	    double S[][] = getS(mesh).data;
	    double T[][] = getT(mesh).data;
	    for (int i=0;i<mesh.ni;i++)
		for (int j=0;j<mesh.nj;j++)
		{
		 /*the integration is (nT)^(i+1) = (nT)^i + dt*2/(3*k)*S
		    the problem is we don't have the old value of density
		*/
		    double den = n[i][j];
		    den /= 1e5;
		    
		    double nT = den*T[i][j];
		    
		    if (S[i][j]!=0)
			nT = nT;
		    
		    double dTn = Starfish.getDt()*2/(3*Constants.K)*S[i][j];
		    double nT_new = nT + dTn;
		   		
		    if (dTn!=0)
			nT_new=nT_new;
		    
		    if (den>0)
			T[i][j] = nT_new/den;
		    else 
			T[i][j] = 0;
		    
		    /*make sure we stay within reasonable limits*/
		    if (T[i][j]<0) T[i][j]=0;
		    if (T[i][j]>1e12) 
			T[i][j] = 1e12;	/*proton fusion reaction is 14e6 K*/
		}
	}
	
	/*clear S data*/
	getSCollection().clear();
    }
}
