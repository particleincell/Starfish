/*
Defines functions for creating a "lambda" mesh, in which the j lines follow the magnetic field

*/
package starfish.plugins.het;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import starfish.core.boundaries.Contour;
import starfish.core.boundaries.Field1D;
import starfish.core.boundaries.Spline;
import starfish.core.common.Constants;
import static starfish.core.common.Constants.QE;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.domain.DomainModule;
import starfish.core.domain.Field2D;
import starfish.core.domain.FieldCollection2D;
import starfish.core.domain.FieldManager2D;
import starfish.core.domain.Mesh;
import starfish.core.domain.Mesh.NodeType;
import starfish.core.domain.QuadrilateralMesh;
import starfish.core.materials.Material;

/**
 *
 * @author lbrieda
 */
public class LambdaMesh
{
    /*creates ni uniformly spaced lambdas between anode and cathode*/
    public  LambdaMesh(int ni, int nj,double anode_lambda, double cathode_lambda, String bottom_list[], String top_list[])
    {
	double d_lambda = (cathode_lambda-anode_lambda)/(ni-1);
	double lambdas[] = new double[ni];
	for (int i=0;i<ni;i++)
	{
	    lambdas[i] = anode_lambda+d_lambda*i;
	}
	
	/*call constructor*/
	init(lambdas,nj,bottom_list,top_list);	
    }
    
    /*constructor wrapper*/
    public   LambdaMesh (double lambdas[], int nj, String bottom_list[], String top_list[])
    {
	init(lambdas,nj,bottom_list,top_list);
    }
    
    /*actual constructor*/
    final void init (double lambdas[], int nj, String bottom_list[], String top_list[])
    {	
	/*convert string list to boundaries*/
	ArrayList<Spline> b_list = new ArrayList();
	for (int i=0;i<bottom_list.length;i++)
	    b_list.add(Starfish.boundary_module.getBoundary(bottom_list[i]));

	ArrayList<Spline> t_list = new ArrayList();
	for (int i=0;i<top_list.length;i++)
		t_list.add(Starfish.boundary_module.getBoundary(top_list[i]));

	/*turn into a spline*/
	Spline bottom = new Spline(b_list);
	Spline top = new Spline(t_list);
	    
	/*create the mesh*/
	this.ni = lambdas.length;
	this.nj = nj;
	mesh = new QuadrilateralMesh(new int[]{ni,nj},"lambda_mesh",Starfish.domain_module.getDomainType());
	mesh.makeVirtual();

	/*compute anode and cathode lambdas*/
	FieldManager2D fm = Starfish.domain_module.getFieldManager();
	FieldCollection2D lambda = fm.getFieldCollection("lambda");
	if (lambda == null) Log.error("Field collection lambda not found");
	
	cathode_lambda = lambdas[ni-1];
	anode_lambda = lambdas[0];
	lambda_ground = lambda.getRange()[1];	//max value
	    
	Starfish.Log.log(String.format("> Lambda range: %g %g\n",anode_lambda, cathode_lambda));
						
	Z = new Field2D(mesh);
	R = new Field2D(mesh);
	BF = new Field2D(mesh);
	NE = new Field2D(mesh);
	NI = new Field2D(mesh);
	NA = new Field2D(mesh);
	MU = new Field2D(mesh);
	PHI = new Field2D(mesh);
	TE = new Field2D(mesh);
	EPERP = new Field2D(mesh);
	UPERP = new Field2D(mesh);
	LAMBDA = new Field2D(mesh);
	DS = new Field2D(mesh);
	NE_OLD = new Field2D(mesh);
	UE = new Field2D(mesh);
	DNE_DL = new Field2D(mesh);
	DNE_DT = new Field2D(mesh);
	
	II = new Field1D(ni);
	f1 = new Field1D(ni);
	f2 = new Field1D(ni);
	f3 = new Field1D(ni);
	Ew = new Field1D(ni);
	h1 = new Field1D(ni);
	h2 = new Field1D(ni);
	h3 = new Field1D(ni);
	e1 = new Field2D(mesh);
	e2 = new Field2D(mesh);
	j1 = new Field2D(mesh);
	j2 = new Field2D(mesh);
	j3 = new Field2D(mesh);
	k1 = new Field2D(mesh);
	k2 = new Field2D(mesh);
	k3 = new Field2D(mesh);
	Ke_prime = new Field2D(mesh);
	Si = new Field2D(mesh);
	
	/*integrals*/
	A1 = new Field1D(ni);
	A2 = new Field1D(ni);
	L1 = new Field1D(ni);
	L2 = new Field1D(ni);
	L3 = new Field1D(ni);
	L4 = new Field1D(ni);
	L5 = new Field1D(ni);
	L6 = new Field1D(ni);
	M1 = new Field1D(ni);
	M2 = new Field1D(ni);
	N1 = new Field1D(ni);
	N2 = new Field1D(ni);
	N3 = new Field1D(ni);
	N4 = new Field1D(ni);
	N5 = new Field1D(ni);
	N6 = new Field1D(ni);
	Ei = new Field1D(ni);
	Ew = new Field1D(ni);
	
	phi_star = new Field1D(ni);
	te_vec = new Field1D(ni);
	
	/*set lambda vec*/
	lambda_vec = new Field1D(ni);
	for (int i=0;i<ni;i++)
	    lambda_vec.set(i, lambdas[i]);

	double z[][] = Z.getData();
	double r[][] = R.getData();

	for (int i=0;i<ni;i++)
	{
	    /*create the contour line*/
	    double value=lambdas[i];
	    Contour contour = new Contour(value,lambda,bottom, top, nj);
	    //contour.save("contour.csv");

	    /*interpolate values along this line*/
	    contour.pos(z[i],r[i]);
	    contour.ds(DS.getData(i));
	    
	    /*verify DS*/
	    for (int j=0;j<nj-1;j++)
	    {
		double dz = z[i][j+1]-z[i][j];
		double dr = r[i][j+1]-r[i][j];
		double ds = Math.sqrt(dz*dz+dr*dr);
		if (Math.abs((ds-DS.data[i][j])/ds)>1e-3)
		    System.err.printf("wrong ds at (%d,%d): %e vs %e\n",i,j,ds,DS.data[i][j]);
		
	    }
	}
		
	/*set node positions to construct the mesh*/
	mesh.setPos(Z.getData(), R.getData());
	mesh.init();

	/*interpolate b field here, assumed to remain constant*/
	BF.interpMagnitude(fm.getFieldCollection("bfi"),fm.getFieldCollection("bfj"));
	/*sanity check, should be uniform*/
	LAMBDA.interp(fm.getFieldCollection("lambda"));	
	
	/*compute additional geometry data needed by the solver*/
	computeGeometry();
    }
	
    QuadrilateralMesh mesh;
	
    public Field2D Z;
    public Field2D R;
    public Field2D BF;
    public Field2D NE;
    public Field2D NI;
    public Field2D NA;
    public Field2D MU;
    public Field2D PHI;
    public Field2D TE;
    public Field2D EPERP;
    public Field2D UPERP;   //not sure what this is anymore - ion or electron velocity
    public Field2D UE;	    //perpendicular electron velocity
    public Field2D LAMBDA;
    public Field2D DS;
    public Field2D NE_OLD;
    public Field2D DNE_DL;
    public Field2D DNE_DT;
    public Field1D II;	    //ion current
    
    public Field1D lambda_vec;
    public Field1D f1;
    public Field1D f2;
    public Field1D f3;
    public Field1D Ew;
    public Field1D h1;
    public Field1D h2;
    public Field1D h3;
    public Field2D e1;
    public Field2D e2;
    public Field2D j1;
    public Field2D j2;
    public Field2D j3;
    public Field2D k1;
    public Field2D k2;
    public Field2D k3;
    public Field2D Ke_prime;
    public Field2D Si;
    
    public Field1D phi_star;
    public Field1D te_vec;
    
    /*integrals*/
    public Field1D A1,A2;
    public Field1D L1,L2,L3,L4,L5,L6;
    public Field1D M1,M2;
    public Field1D N1,N2,N3,N4,N5,N6;
    public Field1D Ei;
    
    public int ni,nj;
    double anode_lambda,cathode_lambda;
    double lambda_ground;
    double Ia;	    //anode current;
    	
  	
    /**
	* Interpolates data from global mesh onto the lambda mesh
    */
    private boolean first_time = true;
    public void domainToLambda()
    {
	DomainModule dm = Starfish.domain_module;

	/* 1) plasma and neutral density, ion velociy*/
	NE.clear();
	NI.clear();
	NA.clear();
	UPERP.clear();

	Material mat_e = Starfish.getMaterial("e-");
	mat_e.getDenCollection().clear();
	
	for (Material mat:Starfish.getMaterialsList())
	{
	    /*ion?*/
	    if (mat.getCharge()>0)
	    {
		    /*assume quasineutrality*/
		    NI.interpScaled(mat.getDenCollection(),mat.getCharge()/QE);
		    
		    /*set electron density data on global mesh, neede to set potential*/
		    mat_e.getDenCollection().addData(mat.getDenCollection(),mat.getCharge()/QE);    
		    
		    /*ion velocities, 
			* TODO: this only works for one species*/
		    UPERP.interpNormal(mat.getUCollection(),mat.getVCollection());
	    }
	    else if (mat.getCharge()==0)
		    NA.interp(mat.getDenCollection());		
	}
			
	/*set electron density*/
	double ne[][] = NE.getData();
	double nion[][] = NI.getData();
	double ne_old[][] = NE_OLD.getData();

	NE.copy(NI);	
	
	
	//clear gradients first time since first NE may be some guess
	if (first_time)
	{
	    NE_OLD.copy(NE);
	    first_time = false;
	}
	
	/*compute DNE_DT*/
	double dt = Starfish.getDt();
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		DNE_DT.data[i][j] = (NE.data[i][j]-NE_OLD.data[i][j])/dt;
	NE_OLD.copy(NE);
	
	//compute DNE_DL
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		int i1,i2;
		if (i==0) {i1=i;i2=i+1;}
		else if (i==ni-1) {i1=i-1;i2=i;}
		else {i1=i-1;i2=i+1;}
		double dl = lambda_vec.at(i2)-lambda_vec.at(i1);
		DNE_DL.data[i][j] = (NE.at(i2,j)-NE.at(i1,j))/dl;
	    }
	
	
	/*potential, output*/
	PHI.clear();
	PHI.interp(dm.getPhi());

	/** normal vector quantities*/
	EPERP.clear();
	EPERP.interpNormal(dm.getEfi(),dm.getEfj());

	TE.clear();
	TE.interp(Starfish.getMaterial("e-").getTempCollection());
	
	/*mobility*/
	MU.clear();
	MU.interp(dm.getFieldCollection("mu"));
    }

	/**interpolates values from lambda mesh to the simulation mesh*/
     void lambdaToDomain(ElectronSolver solver)
    {
	/*interpolate lambda mesh to 2D*/
	FieldCollection2D fc = new FieldCollection2D(PHI,null);
	FieldCollection2D fct = new FieldCollection2D(TE,null);
	FieldCollection2D fcn = new FieldCollection2D(NE,null);
	
	/*first set 2D TE based on te_vec*/
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
		TE.data[i][j] = te_vec.at(i);

		
	for (Mesh mesh:Starfish.getMeshList())
	{
	    Field2D phi2d = Starfish.domain_module.getPhi(mesh);
	    phi2d.clear();

	    Field2D te2d = Starfish.getMaterial("e-").getT(mesh);
	    te2d.clear();

	    Field2D ne2d = Starfish.getMaterial("e-").getDen(mesh);

	    /*set phi*/
	    double l[][] = Starfish.domain_module.getFieldManager().getFieldCollection("lambda").getField(mesh).getData();
	    double p[][] = phi2d.getData();
	    double t[][] = te2d.getData();
	    double ne[][] = ne2d.getData();

	    for (int i=0;i<phi2d.getNi();i++)
		for (int j=0;j<phi2d.getNj();j++)
		{
		    if (mesh.nodeType(i, j)==NodeType.DIRICHLET) continue;
		    
		    double phi_star_val;
		    double te_val;
		     
		    /*get logical coordinate*/
		    double lc = lambda_vec.getLC(l[i][j]);
		     
		    if (l[i][j]<=0)	//not set
		    {
			phi_star_val = 0;	//need ground values
			te_val = solver.t_e_ground;
		    }
		    else if (l[i][j]<=anode_lambda) 
		    {
			/*TODO: replace with phi_anode*/	    
			p[i][j] = solver.phi_anode;
			t[i][j] = 0.1*Constants.EVtoK;	    //COLD anode region
			continue;
		    }
		    else if (l[i][j]>=cathode_lambda) 
		    {
			/*TODO: need to compute phi_star_ground given some phi_ground*/
			double f = (lambda_ground-l[i][j])/(lambda_ground-cathode_lambda);
			phi_star_val = solver.phi_star_cathode*(lambda_ground-l[i][j])/(lambda_ground-cathode_lambda);
			te_val = solver.t_e_ground+(solver.t_e_cathode-solver.t_e_ground)*f;
		    }
		    else
		    {
			phi_star_val = phi_star.gather(lc);
			te_val = te_vec.gather(lc);
		    }
		
		
		    double ne_val = ne[i][j];
		    if (ne_val<1e4) ne_val=1e4;        //floor since can't take log of 0

		    p[i][j] = phi_star_val + Constants.K*te_val/Constants.QE*Math.log(ne_val);
		    t[i][j] = te_val;
		}
	}	
    }
    
    /*2D data is typically per cell so only [ni][nj-1]*/
    Field2D edge_area;
    Field2D edge_length;
    Field2D normal_i,normal_j;
    Field2D edge_axial_area;
    Field1D total_area,total_volume,total_length;
    Field2D node_volume;
    //,node_area;
    
    /*computes additional geometry*/
    void computeGeometry()
    {
	edge_area = new Field2D(ni,nj);
	edge_length = new Field2D(ni,nj);
	normal_i = new Field2D(ni,nj);
	normal_j = new Field2D(ni,nj);
	  
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj-1;j++)
	    {
		double dz = Z.at(i,j+1)-Z.at(i,j);
		double dr = R.at(i,j+1)-R.at(i,j);
		double l = Math.sqrt(dz*dz+dr*dr);        //edge length
		double r_mid = 0.5*( R.at(i,j+1) + R.at(i,j));    // r value of edge center

		normal_i.data[i][j] = -dr/l;
		normal_j.data[i][j] = dz/l;
		edge_area.data[i][j] = 2*Math.PI*r_mid*l;
		edge_length.data[i][j] = l;
	    }
	
	//total length
	total_length = new Field1D(ni);
	for (int i=0;i<ni;i++)
	{
	    total_length.data[i] = 0;
	    for (int j=0;j<nj-1;j++)
		total_length.data[i]+=edge_length.data[i][j];
	}
	

	edge_axial_area = new Field2D(ni,nj);
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
            int i1 = i>0?i-1:0;
            int i2 = i<ni-1?i+1:ni-1;
            int j1 = j>0?j-1:0;
            int j2 = j<nj-1?j+1:nj-1;
            //segment to the left and right of the node
            double dz1 = Z.at(i,j)-Z.at(i1,j1);
            double dr1 = R.at(i,j)-R.at(i1,j1);
            double dz2 = Z.at(i2,j2)-Z.at(i,j);
            double dr2 = R.at(i2,j2)-R.at(i,j);

            //length, half on each side
            double dl1 = 0.5*Math.sqrt(dz1*dz1+dr1*dr1);
            double dl2 = 0.5*Math.sqrt(dz2*dz2+dr2*dr2);

            //midpoint r for the two segments;
            double r1 = 0.5*(R.at(i,j)+R.at(i1,j1));
            double r2 = 0.5*(R.at(i,j)+R.at(i2,j2));

            edge_axial_area.data[i][j] = 2*Constants.PI*(dl1*r1 + dl2*r2);    //2*pi*r*l
        }

	//get total edge area
	total_area = new Field1D(ni);
	for (int i=0;i<ni;i++)
	{
	    total_area.data[i] = 0;
	    for (int j=0;j<nj-1;j++) 
		total_area.data[i]+=edge_area.at(i,j);
	}


	/*compute node-centered volumes*/
	node_volume = new Field2D(ni,nj);
	for (int i=0;i<ni;i++)
	    for (int j=0;j<nj;j++)
	    {
		int ip=i+1,im=i-1;
		int jp=j+1,jm=j-1;
		if (im<0) im=0; if (jm<0) jm=0;
		if (ip>=ni-1) ip=ni-1; if (jp>=nj-1) jp=nj-1;

		/*coordinates of midpoints*/
		double A[] = {0.5*(Z.at(i,j)+Z.at(im,jm)), 0.5*(R.at(i,j)+R.at(im,jm))};
		double B[] = {0.5*(Z.at(i,j)+Z.at(ip,jm)), 0.5*(R.at(i,j)+R.at(ip,jm))};
		double C[] = {0.5*(Z.at(i,j)+Z.at(ip,jp)), 0.5*(R.at(i,j)+R.at(ip,jp))};
		double D[] = {0.5*(Z.at(i,j)+Z.at(im,jp)), 0.5*(R.at(i,j)+R.at(im,jp))};

		/*area of a 2D quad is 0.5*|ac_x*bd_y-ac_y*bd_x|*/
		double ac_x = C[0]-A[0];
		double ac_y = C[1]-A[1];
		double bd_x = D[0]-B[0];
		double bd_y = D[1]-B[1];
		double area = 0.5*Math.abs(ac_x*bd_y-ac_y*bd_x);

		/*for node area we need (i,j+0.5) and (i,j-0.5) which are midpoints of CD and AB*/
		double Pp[] = {0.5*(C[0]+D[0]),0.5*(C[1]+D[1])};
		double Pm[] = {0.5*(A[0]+B[0]),0.5*(A[1]+B[1])};
		double dz = Pp[0]-Pm[0];
		double dr = Pp[1]-Pm[1];
		double length = Math.sqrt(dz*dz+dr*dr);

		double r_mid = 0.5*(Pp[1]+Pm[1]);

		node_volume.data[i][j]=2*Constants.PI*r_mid*area;
	    }

	/*sum node volumes to get total volume for each radial line*/
	total_volume = new Field1D(ni);
	for (int i=0;i<ni;i++)
	{
	    total_volume.data[i] = 0;
	    for (int j=0;j<nj;j++) 
		total_volume.data[i]+=node_volume.at(i,j);
	}	
    }
	
    public void saveLambdaMesh()
    {
	/*save mesh*/
	LinkedHashMap<String,Field2D> out2d = new LinkedHashMap<>();
	out2d.put("bf", BF);
	out2d.put("ne", NE);
	out2d.put("ni", NI);
	out2d.put("na", NA);
	out2d.put("phi", PHI);
	out2d.put("te", TE);
	out2d.put("mu", MU);
	out2d.put("lambda", LAMBDA);
	out2d.put("eperp", EPERP);
	out2d.put("uperp", UPERP);
	out2d.put("ue", UE);
	out2d.put("e1", e1);
	out2d.put("e2", e2);
	out2d.put("j1", j1);
	out2d.put("j2", j2);
	out2d.put("j3", j3);
	out2d.put("k1", k1);
	out2d.put("k2", k2);
	out2d.put("k3", k3);
	out2d.put("Ke_prime",Ke_prime);
	out2d.put("dne/dl", DNE_DL);
	out2d.put("dne/dt", DNE_DT);
	out2d.put("Si",Si);

	LinkedHashMap<String, Field1D> out1d = new LinkedHashMap<>();
	out1d.put("Ii",II);
	out1d.put("f1",f1);
	out1d.put("f2",f2);
	out1d.put("f3",f3);
	out1d.put("h1",h1);
	out1d.put("h2",h2);
	out1d.put("h3",h3);
	out1d.put("A1",A1);
	out1d.put("A2",A2);
	out1d.put("L1",L1);
	out1d.put("L2",L2);
	out1d.put("L3",L3);
	out1d.put("L4",L4);
	out1d.put("L5",L5);
	out1d.put("L6",L6);
	out1d.put("M1",M1);
	out1d.put("M2",M2);
	out1d.put("N1",N1);
	out1d.put("N2",N2);
	out1d.put("N3",N3);
	out1d.put("N4",N4);
	out1d.put("N5",N5);
	out1d.put("N6",N6);
	out1d.put("Ei",Ei);
	out1d.put("Ew",Ew);
	out1d.put("phi_star",phi_star);
	
	out2d.put("edge_length",this.edge_length);
	out2d.put("edge_area",this.edge_area);
	out2d.put("edge_axial_area",this.edge_axial_area);
	out2d.put("node_volume",this.node_volume);
	
	mesh.save("lambda-mesh.vts",out2d, out1d);	
    }
}
