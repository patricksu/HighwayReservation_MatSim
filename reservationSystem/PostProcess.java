package reservationSystem;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import reservationSystem.ReservationControlCenter.altInfo;

public class PostProcess {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		int ITENUM=399;
		int[][] States=new int[ITENUM][7];
		double[][][][][] altInfo=new double[ITENUM][7][7][4][1];
		double[][][][][] ODDATalt=new double[ITENUM][7][7][4][4];
		double[][][][] ODDAT_User=new double[7][7][4][4];

		String planFileName="./Plan_MatSimReservationDollarValueLognorm_29000.csv";			
		BufferedReader br3=new BufferedReader(new FileReader(planFileName));
		String line;
		String[] elements;
		line=br3.readLine();			
		while((line=br3.readLine())!=null)
		{
			elements=line.split(",");
			int O=Integer.parseInt(elements[0]);
			int D=Integer.parseInt(elements[1]);
			int DAT=Integer.parseInt(elements[2]);
			int k=(DAT-45)/15;
			ODDAT_User[O-1][D-1][k-1][0]=ODDAT_User[O-1][D-1][k-1][0]+1;
			ODDAT_User[O-1][D-1][k-1][1]=Double.parseDouble(elements[3]);
			ODDAT_User[O-1][D-1][k-1][2]=Double.parseDouble(elements[4]);
			ODDAT_User[O-1][D-1][k-1][3]=Double.parseDouble(elements[5]);
		}
		for(int O=1;O<=5;O++)
		{
			for(int D=O+2;D<=7;D++)
			{
				for(int k=1;k<=4;k++)
				{
					ODDAT_User[O-1][D-1][k-1][1]=ODDAT_User[O-1][D-1][k-1][1]/ODDAT_User[O-1][D-1][k-1][0];
					ODDAT_User[O-1][D-1][k-1][2]=ODDAT_User[O-1][D-1][k-1][2]/ODDAT_User[O-1][D-1][k-1][0];
					ODDAT_User[O-1][D-1][k-1][3]=ODDAT_User[O-1][D-1][k-1][3]/ODDAT_User[O-1][D-1][k-1][0];
				}
			}
		}
		
		
		for(int ite=0;ite<ITENUM;ite++)
		{
			String runFileName="myoutput/run"+String.valueOf(ite+1)+".csv";			
			BufferedReader br=new BufferedReader(new FileReader(runFileName));
			line=br.readLine();			
			while((line=br.readLine())!=null)
			{
				elements=line.split(",");
				if(elements[5].equals("ALT"))
					States[ite][0]=States[ite][0]+1;
				if(elements[5].equals("Decreasing"))
					States[ite][1]=States[ite][1]+1;
				if(elements[5].equals("Increasing"))
					States[ite][2]=States[ite][2]+1;
				if(elements[5].equals("Initial"))
					States[ite][3]=States[ite][3]+1;
				if(elements[5].equals("Stable"))
					States[ite][4]=States[ite][4]+1;
				if(elements[7].equals("1"))
				{
					States[ite][5]=States[ite][5]+1;
					States[ite][6]=States[ite][6]+(int)Math.round(Double.parseDouble(elements[13]));
				}
			}
			br.close();
			
			String altInfoFileName="myoutput/run"+String.valueOf(ite+1)+"_altInfo.csv";
			BufferedReader br2=new BufferedReader(new FileReader(altInfoFileName));
			String line2;
			String[] elements2;
			line2=br2.readLine();
			while((line2=br2.readLine())!=null)
			{
				elements2=line2.split(",");
				int O=Integer.parseInt(elements2[1]);
				int D=Integer.parseInt(elements2[2]);
				int DAT=Integer.parseInt(elements2[0]);
				int k=(DAT-45)/15;
				altInfo[ite][O-1][D-1][k-1][0]=Double.parseDouble(elements2[7]);
				ODDATalt[ite][O-1][D-1][k-1][0]=Double.parseDouble(elements2[3]);
				ODDATalt[ite][O-1][D-1][k-1][1]=Double.parseDouble(elements2[4]);
				ODDATalt[ite][O-1][D-1][k-1][2]=Double.parseDouble(elements2[5]);
				ODDATalt[ite][O-1][D-1][k-1][3]=Double.parseDouble(elements2[6]);
			}
			br2.close();
		}
		

		
		PrintWriter outputStream=new PrintWriter(new FileOutputStream ("myoutput/States.csv"));	
		outputStream.print("ITE"+",");
		outputStream.print("ALT"+",");
		outputStream.print("Decreasing"+",");
		outputStream.print("Increasing"+",");
		outputStream.print("Intial"+",");
		outputStream.print("Stable"+",");
		outputStream.print("ResultSuc"+",");
		outputStream.print("TotalRevenue"+",");
		outputStream.print("ArtAvgSpeed");
		outputStream.print("\n");
        for(int ite=0;ite<ITENUM;ite++)
        {
        	outputStream.print((ite+1)+",");
        	outputStream.print(States[ite][0]+",");
    		outputStream.print(States[ite][1]+",");
    		outputStream.print(States[ite][2]+",");
    		outputStream.print(States[ite][3]+",");
    		outputStream.print(States[ite][4]+",");
    		outputStream.print(States[ite][5]+",");    	
    		outputStream.print(States[ite][6]+",");
    		outputStream.print(altInfo[ite][0][6][0][0]);
			outputStream.print("\n");
        }
        outputStream.flush();
        outputStream.close();
        
		for(int O=1;O<=5;O++)
		{
			for(int D=O+2;D<=7;D++)
			{
				for(int k=1;k<=4;k++)
				{
					int DAT=k*15+45;
					PrintWriter outputStream2=new PrintWriter(new FileOutputStream 
							("myoutput/O"+String.valueOf(O)+"D"+String.valueOf(D)+"DAT"+String.valueOf(DAT)+"alt.csv"));	
					outputStream2.print("IteID"+",");
					outputStream2.print("Count"+",");
					outputStream2.print("Art%"+",");
					outputStream2.print("DepInterval"+",");
					outputStream2.print("ArrInterval"+",");		
					outputStream2.print("stdArrInterval"+",");
					outputStream2.print("overArtAvgSpeed"+",");
					outputStream2.print("\n");
			        for(int ite=0;ite<ITENUM;ite++)
			        {
			        	outputStream2.print((ite+1)+",");
			        	outputStream2.print(ODDATalt[ite][O-1][D-1][k-1][0]+",");
			        	outputStream2.print(ODDATalt[ite][O-1][D-1][k-1][0]/ODDAT_User[O-1][D-1][k-1][0]+",");
			    		outputStream2.print(ODDATalt[ite][O-1][D-1][k-1][1]+",");
			    		outputStream2.print(ODDATalt[ite][O-1][D-1][k-1][2]+",");
			    		outputStream2.print(ODDATalt[ite][O-1][D-1][k-1][3]+",");
			    		outputStream2.print(altInfo[ite][O-1][D-1][k-1][0]);			    		
						outputStream2.print("\n");
			        }
			        outputStream2.flush();
			        outputStream2.close();
				}
			}
		}
	}
}
