package dtaRun;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class DTA_Output_Reader {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public double AvgTrvTimeReader(int ite) throws IOException {
		// TODO Auto-generated method stub
		String runFileName="output/ITERS/it."+String.valueOf(ite)+"/run0."+String.valueOf(ite)+".tripdurations.txt";
		BufferedReader br=new BufferedReader(new FileReader(runFileName));
		String line;
		String[] elements;
		line=br.readLine();
		line=br.readLine();
		line=br.readLine();
		line=br.readLine();
		line=br.readLine();
		line=br.readLine();
		line=br.readLine();
		
		elements=line.split(":");
		br.close();
		return Double.valueOf(elements[2])+Double.valueOf(elements[3])/60;		
	}
}
