/* MagCoil
 * 
 * Implements Python code of M. Stasiukevicius to compute magnetic field due to arbitrary coils
 * 
 */
package starfish.core.solver;

import starfish.core.common.Vector;

public class MagCoil {

    void make_b_dat(double h, double j, int number_coils, int dist_cl) {
    	
	    // Define Field Mesh
	    int Nz = 250;
	    int Nr = 100;
	    double A[][] = new double[Nr][Nz];
	    double J[][] = new double[Nr][Nz];
	    double R[][] = new double[Nr][Nz];
	    double Br[][] = new double[Nr][Nz];
	    double Bz[][] = new double[Nr][Nz];
	    //double Adip[][] = new double[Nr][Nz];

	    // Initial constants
	    double mu = 4*Math.PI*1e-7;
	    //double eo = 8.854*1e-12;
	    double e = 1e-10;
	    double t = Math.cos(Math.PI/Nz) + Math.cos(Math.PI/Nr);
	    double w = (8-Math.sqrt(64-16*t*t))/(t*t);

	    // Define coils
	    int coil_size = 1;
	    int div = (int)(Math.round(Nz/(number_coils+1.0))-Math.floor(coil_size/2.0));
	    
	    double coil_centers[][] = new double[number_coils][2];
	    for (int k=0;k<number_coils;k++) {
	        for (int l=0; l<coil_size;l++) {
	            for (int m=0; m<coil_size; m++)
	                J[(dist_cl+m)-1][((k+1)*div+l)-1] = j;
	        }
	        int m = coil_size-1;
	        int l = m;      
	        coil_centers[k][1] = ((((k+1)*div+l)+((k+1)*div))/2);
	        coil_centers[k][0] = (dist_cl+m/2);
	    }

	    // Manual Coil Input
	    // coil_centers = [10 85; 10 100; 10 115];
	    // number_coils = size(coil_centers,1);
	    // for k = 1:number_coils
	    //     J(coil_centers(k,1),coil_centers(k,2)) = j;
	    // end
	            
	    // i,j -> r,z

	    // Setting boundaries to dipole at coil centers

	    // Boundary conditions opposite center line
	    for (int n=0; n<number_coils; n++) 
	        for (int z=2;z<Nz;z++) {
	            double dipole_moment = j*(h*h*h*h)*coil_centers[n][0]*coil_centers[n][0]*Math.PI;
	            double mag_moment[] = {0,dipole_moment,0};
	            double dist[] = {Math.abs(Nr-coil_centers[n][0])*h,Math.abs(coil_centers[n][1]-z)*h,0};
	            double dist_mag = Vector.norm(dist);
	            A[Nr-1][z-1] = Vector.norm(Vector.mult(mu/(4*Math.PI*dist_mag*dist_mag*dist_mag),Vector.cross(mag_moment,dist))) + A[Nr-1][z-1];
	        }

	    // Boundry conditions parallel to r
	    for (int n=0;n<number_coils;n++) {
	        for (int r=0;r<Nr;r++) {
	            double dipole_moment = j*(h*h*h*h)*(coil_centers[n][0]*coil_centers[n][0])*Math.PI;
	            double mag_moment[] = {0,dipole_moment,0};
	            double dist[] = {Math.abs((r+1)-coil_centers[n][0])*h,Math.abs(coil_centers[n][1]-1)*h,0};
	            double dist_mag = Vector.norm(dist);
	            A[r][0] = Vector.norm(Vector.mult(mu/(4*Math.PI*dist_mag*dist_mag*dist_mag),Vector.cross(mag_moment,dist))) + A[r][0];
	            double dist2[] = {dist[0],Math.abs(Nz-coil_centers[n][1])*h,0};
	            double dist_mag_b = Vector.norm(dist);
	            A[r][Nz-1] = Vector.norm(Vector.mult(mu/(4*Math.PI*dist_mag_b*dist_mag_b*dist_mag_b),Vector.CrossProduct3(mag_moment,dist2))) + A[r][Nz-1];
	        }
	    }
	    
	    int itr = 0;
	    double res = 1;
	    double reshold = 10;

	    // Magnetic Vector Potential

	    //start = time.time()
	    while (res > e) {
	        for (int r=0;r<Nr-1;r++) { 
	            for (int z=1;z<Nz-1;z++) {
	                if (r == 0)
	                    R[0][z] = (-A[2][z]+(10/3.0)*A[1][z]+(2/3.0)*(-A[1][z+1]-A[1][z-1]+mu*J[1][z]*h*h))-A[0][z];
	                else
	                    R[r][z] = 1/(4+1/(r*r))*(A[r+1][z]*(1+1/(2*(r)))+A[r-1][z]*(1-1/(2*(r)))+A[r][z+1]+A[r][z-1]-mu*J[r][z]*h*h)-A[r][z];
	                A[r][z] += R[r][z]*w;
	            }
	        }
	        itr++;
	        res = 0;
	        for (int r=0;r<Nr;r++) 
	        	for (int z=0;z<Nz;z++) if (Math.abs(R[r][z])>res) res=Math.abs(R[r][z]);
	        if (res/reshold<0.1) 
	            reshold = res;
	    }
	    
	   // print(time.time() - start)
	   // print(itr)

	    // Magnetic Field

	    for (int r=0;r<Nr-1;r++)
	        for (int z=1;z<Nz-1;z++) {
	            // Bz centerline nodes
	            if (r == 0)
	                Bz[0][z] = (-3*A[0][z]+4*A[1][z]-A[2][z])/h;    // Second Order
	            // Bz internal nodes
	            else
	                Bz[r][z] = A[r][z]/(h*(r))+(A[r+1][z]-A[r-1][z])/(2*h);
	            // Br all nodes
	            Br[r][z] = (A[r][z-1]-A[r][z+1])/(2*h);

	        }
	    // np.savetxt("mat_py.csv", B, delimiter = ",")
	    /*
	    f = open("B.dat", "w")
	    f.write("Variables = x y B Br Bz\nZone I=%d, J=%d, F=POINT\n"%(Nr,Nz))
	    for r in range(0,Nr):
	        for z in range(0,Nz):
	            f.write(str(r*h)+" "+str(z*h)+" "+str(B[r,z])+" "+str(Br[r,z])+" "+str(Bz[r,z])+"\n")
	    f.close
	    */
    }
    
    void run() {
    	make_b_dat(.001,10E7,2,3);
    }
}
