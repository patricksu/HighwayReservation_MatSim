package dtaRun;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
public class DTA_Result_Analysis {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		DTA_Output_Reader dor=new DTA_Output_Reader();
		Double[] avgTravelTime=new Double[201];
		int i=0;
		for(i=0;i<=200;i++)
			avgTravelTime[i]=dor.AvgTrvTimeReader(i);
		
		PrintWriter outputStream=new PrintWriter(new FileOutputStream ("myoutput/DTA_States.csv"));	
        for(int ite=0;ite<=200;ite++)
        {
        	outputStream.print(avgTravelTime[ite]+",");  	
			outputStream.print("\n");
        }
        outputStream.flush();
        outputStream.close();
	}

}
