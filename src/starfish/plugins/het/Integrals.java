/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package starfish.plugins.het;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import starfish.core.boundaries.Field1D;
import starfish.core.common.Constants;
import starfish.core.common.Starfish;

/**
 *
 * @author Lubos Brieda
 */
public class Integrals
{
    LambdaMesh lm;
    Params params;
    
    
    Integrals(LambdaMesh lm, Params params)
    {
	this.lm = lm;
	this.params = params;
    }
    
    
    
    //core1
    void update() 
    {
	
	//recompute parameters
	params.ComputeIaParams();
	
	//IntegrationTest();
	
	LambdaMesh lm = this.lm;
	int nr = lm.nj;
	double MKS_kb = Constants.K;
	double MKS_e = Constants.QE;

	for (int l=0;l<lm.ni;l++)
	{
	    lm.A1.data[l] = 0;
	    lm.A2.data[l] = 0;

	    for (int j=0;j<nr;j++)
	    {
		double dV = lm.node_volume.at(l,j);
		//A1 = 3/2*k*ne
		lm.A1.data[l] += (3/2.0)*MKS_kb*lm.NE.at(l,j)*dV;
		//A2 = 3/2*k*(dne/dt)
		lm.A2.data[l] += (3/2.0)*MKS_kb*lm.DNE_DT.at(l,j)*dV;
	    }

	    int l1 = l-1;
	    int l2 = l+1;
	    if (l1<0) l1=0;
	    if (l2>lm.ni-1) l2=lm.ni-1;
		
	    lm.L1.data[l] = 0;
	    lm.L2.data[l] = 0;
	    lm.L3.data[l] = 0;
	    lm.L4.data[l] = 0;
	    lm.L5.data[l] = 0;
	    lm.L6.data[l] = 0;

	    /*integrals are evaluated using trapezoidal rule, 0.5*(data[l][k]+data[l][k+1])*dA
	      we are also evaluating at the midpoint between two lambda lines so need to get 
	      the "left" value from 0.5*(data[l-1]+data[l])
	    */
	    for (int j=0;j<nr-1;j++)
	    {
		double dA_minus = 0.5*(lm.edge_area.at(l1,j) + lm.edge_area.at(l,j));
		double dA_plus = 0.5*(lm.edge_area.at(l,j) + lm.edge_area.at(l2,j));
	
		//(5/2)*k*n_e*j1 on S1 and S2
		lm.L1.data[l] += (5/2.0)*MKS_kb*0.5*(0.5*(lm.NE.at(l1,j)*lm.j1.at(l1,j)+lm.NE.at(l,j)*lm.j1.at(l,j)) +
						     0.5*(lm.NE.at(l1,j+1)*lm.j1.at(l1,j+1)+lm.NE.at(l,j+1)*lm.j1.at(l,j+1))) * dA_minus;
		lm.L2.data[l] -= (5/2.0)*MKS_kb*0.5*(0.5*(lm.NE.at(l,j)*lm.j1.at(l,j)+lm.NE.at(l2,j)*lm.j1.at(l2,j)) +
						     0.5*(lm.NE.at(l,j+1)*lm.j1.at(l,j+1)+lm.NE.at(l2,j+1)*lm.j1.at(l2,j+1))) * dA_plus;
			
		//(5/2)*k*n_e*j2 on S1 and S2
		lm.L3.data[l] += (5/2.0)*MKS_kb*0.5*(0.5*(lm.NE.at(l1,j)*lm.j2.at(l1,j)+lm.NE.at(l,j)*lm.j2.at(l,j)) +
						     0.5*(lm.NE.at(l1,j+1)*lm.j2.at(l1,j+1)+lm.NE.at(l,j+1)*lm.j2.at(l,j+1))) * dA_minus;
		lm.L4.data[l] -= (5/2.0)*MKS_kb*0.5*(0.5*(lm.NE.at(l,j)*lm.j2.at(l,j)+lm.NE.at(l2,j)*lm.j2.at(l2,j)) +
						     0.5*(lm.NE.at(l,j+1)*lm.j2.at(l,j+1)+lm.NE.at(l2,j+1)*lm.j2.at(l2,j+1))) * dA_plus;		
		//(5/2)*k*n_e*j3 on S1 and S2
		lm.L5.data[l] += (5/2.0)*MKS_kb*0.5*(0.5*(lm.NE.at(l1,j)*lm.j3.at(l1,j)+lm.NE.at(l,j)*lm.j3.at(l,j)) +
						     0.5*(lm.NE.at(l1,j+1)*lm.j3.at(l1,j+1)+lm.NE.at(l,j+1)*lm.j3.at(l,j+1))) * dA_minus;
		lm.L6.data[l] -= (5/2.0)*MKS_kb*0.5*(0.5*(lm.NE.at(l,j)*lm.j3.at(l,j)+lm.NE.at(l2,j)*lm.j3.at(l2,j)) +
						     0.5*(lm.NE.at(l,j+1)*lm.j3.at(l,j+1)+lm.NE.at(l2,j+1)*lm.j3.at(l2,j+1))) * dA_plus;
	    }
	    
	    /*M terms*/
	    lm.M1.data[l] = 0;
	    lm.M2.data[l] = 0;
	    for (int j=0;j<lm.nj-1;j++)
	    {
		double dA_minus = 0.5*(lm.edge_area.at(l1,j) + lm.edge_area.at(l,j));
		double dA_plus = 0.5*(lm.edge_area.at(l,j) + lm.edge_area.at(l2,j));

		double term_left_b = lm.Ke_prime.at(l1,j)*lm.R.at(l1,j)*lm.BF.at(l1,j);
		double term_mid_b = lm.Ke_prime.at(l,j)*lm.R.at(l,j)*lm.BF.at(l,j);
		double term_right_b = lm.Ke_prime.at(l2,j)*lm.R.at(l2,j)*lm.BF.at(l2,j) ;
		
		double term_left_t = lm.Ke_prime.at(l1,j+1)*lm.R.at(l1,j+1)*lm.BF.at(l1,j+1);
		double term_mid_t = lm.Ke_prime.at(l,j+1)*lm.R.at(l,j+1)*lm.BF.at(l,j+1);
		double term_right_t = lm.Ke_prime.at(l2,j+1)*lm.R.at(l2,j+1)*lm.BF.at(l2,j+1) ;
		
		//Ke_prime*rB on S1 and S2
		lm.M1.data[l] += 0.5 * ( 0.5*(term_left_b+term_mid_b) + 0.5*(term_left_t+term_mid_t) ) *dA_minus;
		lm.M2.data[l] -= 0.5 * ( 0.5*(term_mid_b+term_right_b) + 0.5*(term_mid_t+term_right_t)) *dA_plus;
	    }

	    /*N terms*/

	    lm.N1.data[l] = 0;
	    lm.N2.data[l] = 0;
	    lm.N3.data[l] = 0;
	    lm.N4.data[l] = 0;
	    lm.N5.data[l] = 0;
	    lm.N6.data[l] = 0;
	    for (int j=0;j<nr;j++)
	    {
		double dV = lm.node_volume.at(l,j);
		lm.N1.data[l] -= MKS_e*lm.NE.at(l,j)*lm.j1.at(l,j)*lm.k1.at(l,j)*dV;        //-e*ne*j1*k1
		lm.N2.data[l] -= MKS_e*lm.NE.at(l,j)*(lm.j1.at(l,j)*lm.k3.at(l,j) + lm.j3.at(l,j)*lm.k1.at(l,j))*dV; //-e*ne*(j1*k3+j3*k1)
		lm.N3.data[l] -= MKS_e*lm.NE.at(l,j)*(lm.j1.at(l,j)*lm.k2.at(l,j) + lm.j2.at(l,j)*lm.k1.at(l,j))*dV; //-*ne*(j1*k2+j2*k1)
		lm.N4.data[l] -= MKS_e*lm.NE.at(l,j)*lm.j2.at(l,j)*lm.k2.at(l,j)*dV;        //-ne*j2*k1
		lm.N5.data[l] -= MKS_e*lm.NE.at(l,j)*(lm.j2.at(l,j)*lm.k3.at(l,j) + lm.j3.at(l,j)*lm.k2.at(l,j))*dV; //-e(ne*(j2*k3+j3*k2)
		lm.N6.data[l] -= MKS_e*lm.NE.at(l,j)*(lm.j3.at(l,j)*lm.k3.at(l,j))*dV;        //-e*ne*j3*k3
	    }

	    /*Ei term*/
	    lm.Ei.data[l] = 0;
	    for (int j=0;j<nr;j++)
	    {
		double dV = lm.node_volume.at(l,j);
		lm.Ei.data[l] += lm.Si.at(l,j)*dV;
	    }

	}

    }
    
    PrintWriter pw;
    Field1D S1,S2,V, V_OLD;    
    int last_it=-1;
    
    //integrates d(dne/dt) + nabla*(ne*u)
    void IntegrationTest()
    {
	if (pw==null)
	{
	    try
	    {
	    //for testing
	    pw = new PrintWriter(new FileWriter("integrals.csv"));	    
	    } catch (IOException ex)
	    {
		Logger.getLogger(Integrals.class.getName()).log(Level.SEVERE, null, ex);
	    }	
	
	    pw.print("time");
	    for (int i=0;i<lm.ni;i++)
		pw.printf(",L%d_V,L%d_S,L%d_ue",i,i,i);
	    pw.println();
	    S1 = new Field1D(lm.ni);
	    S2 = new Field1D(lm.ni);
	    V = new Field1D(lm.ni);
	    V_OLD = new Field1D(lm.ni);
	}
	
	if (last_it==Starfish.getIt()) return;
	last_it = Starfish.getIt();
	
	//recompute parameters
	params.ComputeIaParams();
	
	LambdaMesh lm = this.lm;
	int nr = lm.nj;
	double MKS_e = Constants.QE;

	pw.printf("%g", Starfish.getTime());
	
	for (int l=0;l<lm.ni;l++)
	{
	    
	    V_OLD.data[l] = V.data[l];
	    V.data[l] = 0;
	    
	    for (int j=0;j<nr;j++)
	    {
		double dV = lm.node_volume.at(l,j);
		//A1 = dne/dt
		V.data[l] += lm.NE.at(l,j)*dV;				
	    }

	    int l1 = l-1;
	    int l2 = l+1;
	    if (l1<0) l1=0;
	    if (l2>lm.ni-1) l2=lm.ni-1;
		
	    S1.data[l] = 0;
	    S2.data[l] = 0;
	    
	    double ne_ave = V.data[l]/lm.total_volume.at(l);	    
	    double ue = lm.II.at(l)/(ne_ave*Constants.QE*lm.total_area.at(l));
	    /*integrals are evaluated using trapezoidal rule, 0.5*(data[l][k]+data[l][k+1])*dA
	      we are also evaluating at the midpoint between two lambda lines so need to get 
	      the "left" value from 0.5*(data[l-1]+data[l])
	    */
	    for (int j=0;j<nr-1;j++)
	    {
		double dA_minus = 0.5*(lm.edge_area.at(l1,j) + lm.edge_area.at(l,j));
		double dA_plus = 0.5*(lm.edge_area.at(l,j) + lm.edge_area.at(l2,j));

		//density on left and right face of the lambda-centered node volume
		double nu1_b = 0.5*(lm.NE.at(l1,j)*lm.UPERP.at(l1,j) + lm.NE.at(l,j)*lm.UPERP.at(l, j));
		double nu2_b = 0.5*(lm.NE.at(l,j)*lm.UPERP.at(l,j) + lm.NE.at(l2,j)*lm.UPERP.at(l2,j));
		
		double nu1_t = 0.5*(lm.NE.at(l1,j+1)*lm.UPERP.at(l1,j+1) + lm.NE.at(l,j+1)*lm.UPERP.at(l, j+1));
		double nu2_t = 0.5*(lm.NE.at(l,j+1)*lm.UPERP.at(l,j+1) + lm.NE.at(l2,j+1)*lm.UPERP.at(l2,j+1));
		
		//ne*u
		S1.data[l] += 0.5*(nu1_b + nu1_t) * dA_minus;		
		S2.data[l] -= 0.5*(nu2_b + nu2_t) *dA_plus;
	    }
	    
	    double dV_dt = (V.data[l]-V_OLD.data[l])/Starfish.getDt();
	    //double val = V.data[l]+S1.data[l]+S2.data[l];
	    double val = dV_dt+S1.data[l]+S2.data[l];
	    
	    pw.printf(",%g,%g,%g", dV_dt,S1.data[l]+S2.data[l],ue);
	}
	pw.println();	
	pw.flush();
    }
}
