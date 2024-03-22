/* Simple code to output a disretized circle in
the Starfish format
 */
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

public class MakeCircle
{

	public static void main(String[] args) 
	{
		PrintWriter pw = null;
		
		try {
		pw = new PrintWriter(new FileWriter("cylinder.xml"));
		}
		catch (IOException e) {
		System.err.println("Failed to open output file");
		}
		
		
		/*set circle parameters*/
		double x0=0;
		double y0=0;
		double r=0.05;
		int segments = 21;
		double theta0 = 0;

		/*convert to radians and compute theta spacing*/
		theta0 *= Math.PI/180.0;
		double d_theta = (2.0*Math.PI)/(segments-1);
		
		/*output header info*/
		pw.println("<boundaries>");
		pw.println("<boundary name=\"cylinder\" phi=\"-100\">");
		pw.println("<material>SS</material>");
		
		pw.printf("<path>M");

		/*output segments*/
		for (int i=0;i<segments;i++)
		{
			double theta=theta0+i*d_theta;
			System.out.println(theta*180.0/Math.PI);
			double x=x0+Math.cos(theta)*r;
			double y=y0+Math.sin(theta)*r;
			
			pw.printf(" %g,%g",x,y);
			
			if (i==0)
				pw.printf(" L");
		}
		pw.println("</path>");
		pw.println("</boundary>");
		pw.println("</boundaries>");
		pw.close();
		
		
	}
}


        
