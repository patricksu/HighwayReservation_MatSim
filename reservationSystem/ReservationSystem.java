package reservationSystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import reservationSystem.ReservationControlCenter.altInfo;
import reservationSystem.ReservationControlCenter.resVSingleUser;
import reservationSystem.User.StateSet;
import reservationSystem.User.memory;

public class ReservationSystem {
	public final static double freewaySpeed=26.8224; //meters per second
	public final static double artLSpeed=8.941; //meters per second
	public final static double connectorSpeed=8.941; //meters per second
	public final static double freewayLinkLength=3218.69; //meters
	public final static double artLLinkLength=3218.69; //meters
	public final static double connectorLength=500; //meters
	
	public static void main(String[] args) throws NumberFormatException, IOException, CloneNotSupportedException {		
		
		ReservationControlCenter controlCenter=new ReservationControlCenter(240); //controlCenter, the key of all simulations
		//read in the traffic network file, or routes file, and also the average arterial travel cost file
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		String routesFile="./Routes_forMatSim3.0.csv";
		String line;
		String[] links;
		int i;
		int ITENUM=400;
		BufferedReader br=new BufferedReader(new FileReader(routesFile));
		Integer key;
		TrafficNetwork resAltNetwork=new TrafficNetwork(); //resAltNetwork stores the network info
		while((line=br.readLine())!=null)
		{
			links=line.split(",");
			int O=Integer.parseInt(links[0]);
			int D=Integer.parseInt(links[1]);
			key=O*10000+D*1000+Integer.parseInt(links[2]);
			List<Id> route=new ArrayList<Id>(); 
			for(i=3;i<links.length;i++)
			{
				route.add(sc.createId(links[i]));
			}
			resAltNetwork.getRoutes().put(key,route);
			if(Integer.parseInt(links[2])==0)
			{
				controlCenter.getAvgAltTravelCost().put(key+60, controlCenter.new altInfo(60,O,D,-1,-1,-1));
				controlCenter.getAvgAltTravelCost().put(key+75, controlCenter.new altInfo(75,O,D,-1,-1,-1));
				controlCenter.getAvgAltTravelCost().put(key+90, controlCenter.new altInfo(90,O,D,-1,-1,-1));
				controlCenter.getAvgAltTravelCost().put(key+105, controlCenter.new altInfo(105,O,D,-1,-1,-1));
			}
		}
		br.close();
		
		
		//read in the population file
		String planFile="./Plan_MatSimReservationDollarValueLognorm_29000.csv";
		br=new BufferedReader(new FileReader(planFile));
		Map<Integer, User> userGroup=new HashMap<Integer, User> (); //userGroup stores data of all the travelers
		int userID=0;
		while((line=br.readLine())!=null)
		{
			userID=userID+1;
			User singleUser=new User();
			singleUser.setUserID(userID);
			singleUser.createMem();
			singleUser.createProfile();
			links=line.split(",");
			singleUser.setOrigin(Integer.parseInt(links[0]));
			singleUser.setDestination(Integer.parseInt(links[1]));
			singleUser.setDat(Integer.parseInt(links[2]));
			singleUser.setEarlyArrivalVOT(Double.parseDouble(links[3]));
			singleUser.setTravelTimeVOT(Double.parseDouble(links[4]));
			singleUser.setLateArrivalVOT(Double.parseDouble(links[5]));
			singleUser.setMostDesiredInterval((int)Math.floor(singleUser.getDat()-singleUser.getDestination()+singleUser.getOrigin()-2*connectorLength/connectorSpeed/120));
			singleUser.setAltTravelCost(-99); //******************important*************
						
			//********* the very initial bidding interval****************//
			singleUser.getMem().setBiddingInterval0((int)Math.floor(singleUser.getMostDesiredInterval()));
			singleUser.getMem().setIter0(1);
			singleUser.getMem().setState0(StateSet.Initial);
			//********* the very initial bidding interval****************//
			
			singleUser.setDelta(1000);
			singleUser.setAlpha(0.1);
			userGroup.put(userID,singleUser);
		}
		
		
		
		//1st iteration
		int ite=1;		
		//controlCenter.getResVUsers()=new ArrayList<Double>();
		for(Map.Entry<Integer,User> usr : userGroup.entrySet())
		{
			int O=usr.getValue().getOrigin();
			int D=usr.getValue().getDestination();
			userID=usr.getValue().getUserID();
			double biddingAmount=((D-O)*artLLinkLength/artLSpeed-(D-O)*freewayLinkLength/freewaySpeed)/3600*usr.getValue().getTravelTimeVOT();
			if(biddingAmount<0)
				biddingAmount=0;
			usr.getValue().getMem().setBiddingAmount0(biddingAmount);
			controlCenter.addResVUser(userID, biddingAmount);
		}
		controlCenter.sortResVUsers();
		controlCenter.scheduleRequests(userGroup);		
		for(Map.Entry<Integer,User> usr : userGroup.entrySet())
		{
			usr.getValue().deptTimeLogic1();
		}
		
		//controlCenter.outputResvResults(userGroup);
		controlCenter.generatePopulationFile(userGroup, resAltNetwork.getRoutes());
		//FileUtils.cleanDirectory(new File("./output"));		
		
		config = ConfigUtils.loadConfig("input/MyConfig.xml");
		Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.run();
		System.out.println(eventsFileHandler(ite,userGroup,controlCenter));
		
		
		//2nd and more iterations
		for(ite=2;ite<ITENUM;ite++)
		{
			controlCenter.resetResVUser();
			for(Map.Entry<Integer,User> usr : userGroup.entrySet())
			{
				usr.getValue().logic1(ite,controlCenter);
			}
			controlCenter.sortResVUsers();
			controlCenter.scheduleRequests(userGroup);
			for(Map.Entry<Integer,User> usr : userGroup.entrySet())
			{
				usr.getValue().deptTimeLogic3(controlCenter);
			}
			//controlCenter.outputResvResults(userGroup);
			controlCenter.generatePopulationFile(userGroup, resAltNetwork.getRoutes());
			//FileUtils.cleanDirectory(new File("./output"));		
			
			config = ConfigUtils.loadConfig("input/MyConfig.xml");
			controler = new Controler(config);
			controler.setOverwriteFiles(true);
			controler.run();
			System.out.println("Average arterial speed in m/s is "+eventsFileHandler(ite,userGroup,controlCenter));
		}
	}
	public static double eventsFileHandler(int iter,Map<Integer, User> userGroup,ReservationControlCenter controlCenter) throws FileNotFoundException, IOException, CloneNotSupportedException
	{
		InputStream eventsFile=new GZIPInputStream(new FileInputStream("output/ITERS/it.0/run0.0.events.xml.gz"));
		BufferedReader eventsReader=new BufferedReader(new InputStreamReader(eventsFile));
		String eventsFileName="myoutput/run"+String.valueOf(iter)+".events.xml";
		BufferedWriter eventsWriter=new BufferedWriter(new FileWriter(eventsFileName));
		char[] buff=new char[4096];
		int len = 0;
		while( (len = eventsReader.read( buff )) > 0 )
			eventsWriter.write( buff, 0, len);
		eventsWriter.flush();
		eventsWriter.close();
        Document document;
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        NodeList nodeList;
        File xmlInputFile;
        try
        {
            xmlInputFile = new File(eventsFileName);
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(xmlInputFile);
            nodeList = document.getElementsByTagName("event");
            System.out.println("Total no of events : " + nodeList.getLength());

            document.getDocumentElement().normalize();
            int personID;
            double time;
            String actType;
            for (int index = 0; index < nodeList.getLength(); index++)
            {
                Node node = nodeList.item(index);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) node;
                    personID=Integer.valueOf(element.getAttribute("person"));
                    time=Double.parseDouble((element.getAttribute("time")));
                    actType=element.getAttribute("type");                  
                    if(actType.equals("actstart"))
                    {
                    	userGroup.get(personID).getMem().setArrTime(time);
                    }
                    if(actType.equals("actend"))
                    {
                    	userGroup.get(personID).getMem().setDepTime(time);
                    }
                }
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        File eventFile=new File("myoutput/run"+String.valueOf(iter)+".events.xml");
        eventFile.delete();
        for(Map.Entry<Integer, altInfo> altRouteCost:controlCenter.getAvgAltTravelCost().entrySet())
        {
        	altRouteCost.getValue().setCount(0);
        	altRouteCost.getValue().setAvgDepartInterval(0);
        	altRouteCost.getValue().setAvgArrivInterval(0);
        	altRouteCost.getValue().setStdArrivInterval(0);
        	altRouteCost.getValue().iniArrivList();
        }
        double altVSecondsT=0;
        double altVMetersT=0;
        double avgSpeed=0;
		for(Map.Entry<Integer,User> usr : userGroup.entrySet())
		{
			if(usr.getValue().getMem().getArrTime()==0)
				usr.getValue().getMem().setArrTime(39600);
			double travelCost=0;
			double depInterval=usr.getValue().getMem().getDepTime()/120-6*30;
			double arrInterval=usr.getValue().getMem().getArrTime()/120-6*30;
			if(arrInterval>usr.getValue().getDat())
				travelCost=(arrInterval-usr.getValue().getDat())/30*usr.getValue().getLateArrivalVOT()+
				(arrInterval-depInterval)/30*usr.getValue().getTravelTimeVOT();
			else
				travelCost=(usr.getValue().getDat()-arrInterval)/30*usr.getValue().getEarlyArrivalVOT()+
				(arrInterval-depInterval)/30*usr.getValue().getTravelTimeVOT();
			usr.getValue().getMem().setTravelCost(travelCost);
			if(usr.getValue().getMem().getState0()!=StateSet.ALT && usr.getValue().getMem().isResult0())
			{			
				usr.getValue().getResMemory().add(usr.getValue().copyMemory());
			}
			else
			{
				usr.getValue().getAltMemory().add(usr.getValue().copyMemory());
				altVSecondsT=altVSecondsT+(usr.getValue().getMem().getArrTime()-usr.getValue().getMem().getDepTime());
				altVMetersT=altVMetersT+(usr.getValue().getDestination()-usr.getValue().getOrigin())*artLLinkLength+2*connectorLength;
			}
	        if(!usr.getValue().getMem().isResult0())
	        {
	        	int key=usr.getValue().getOrigin()*10000+usr.getValue().getDestination()*1000+usr.getValue().getDat();
	        	controlCenter.getAvgAltTravelCost().get(key).countPlus();
	        	controlCenter.getAvgAltTravelCost().get(key).avgDepartIntervalPlus(usr.getValue().getMem().getDepTime());
	        	controlCenter.getAvgAltTravelCost().get(key).avgArrivIntervalplus(usr.getValue().getMem().getArrTime());
	        	controlCenter.getAvgAltTravelCost().get(key).addToArrivList(usr.getValue().getMem().getArrTime());
	        }
		}
        for(Map.Entry<Integer, altInfo> altRouteCost:controlCenter.getAvgAltTravelCost().entrySet())
        {
        	int count=altRouteCost.getValue().getCount();
        	if(count>0)
        	{
        		double arrivTimeMean=altRouteCost.getValue().getAvgArrivInterval()/count; // in seconds
        		altRouteCost.getValue().setAvgDepartInterval(altRouteCost.getValue().getAvgDepartInterval()/count/120-6*30);
        		altRouteCost.getValue().setAvgArrivInterval(altRouteCost.getValue().getAvgArrivInterval()/count/120-6*30);
        		List<Double> arrivTimeList=altRouteCost.getValue().getArrivIntervalList();
        		if(arrivTimeList.size()<2)
        			altRouteCost.getValue().setStdArrivInterval(0);
        		else
        		{
        			double sqrSum=0;
        			for(int i=0;i<arrivTimeList.size();i++)
        			{
        				sqrSum=sqrSum+(arrivTimeList.get(i)-arrivTimeMean)*(arrivTimeList.get(i)-arrivTimeMean);
        			}
        			altRouteCost.getValue().setStdArrivInterval(     Math.sqrt(sqrSum/(arrivTimeList.size()-1))/120    );
        		}
        	}
        }
        
		avgSpeed=altVMetersT/altVSecondsT;//average speed in meters per second
		controlCenter.setArtAvgSpd(avgSpeed);
		
        PrintWriter outputStream=new PrintWriter(new FileOutputStream ("myoutput/run"+String.valueOf(iter)+".csv"));		
		Map<Integer,User> treeMap=new TreeMap<Integer,User>(userGroup);
		outputStream.print("userID"+",");
		outputStream.print("Origin"+",");
		outputStream.print("Destination"+",");
		outputStream.print("DAT"+",");
		outputStream.print("MostDesired"+",");
		outputStream.print("State"+",");
		outputStream.print("BiddingInterval"+",");
		outputStream.print("Result"+ ",");
		outputStream.print("DepTime"+",");
		outputStream.print("ArrTime"+",");
		outputStream.print("EarlyArrivalVOT"+",");
		outputStream.print("TravelTimeVOT"+",");
		outputStream.print("LateArrivalVot"+",");
		outputStream.print("BiddingCost"+",");
		outputStream.print("TravelCost");
		outputStream.print("\n");
		for(Map.Entry<Integer, User> usr:treeMap.entrySet())
		{
			outputStream.print(usr.getKey()+",");
			outputStream.print(usr.getValue().getOrigin()+",");
			outputStream.print(usr.getValue().getDestination()+",");
			outputStream.print(usr.getValue().getDat()+",");
			outputStream.print(usr.getValue().getMostDesiredInterval()+",");
			outputStream.print(usr.getValue().getMem().getState0()+",");
			outputStream.print(usr.getValue().getMem().getBiddingInterval0()+",");
			outputStream.print((usr.getValue().getMem().isResult0()? 1:0) + ",");
			outputStream.print((usr.getValue().getMem().getDepTime()/120-6*30)+",");
			outputStream.print((usr.getValue().getMem().getArrTime()/120-6*30)+",");
			outputStream.print(usr.getValue().getEarlyArrivalVOT()+",");
			outputStream.print(usr.getValue().getTravelTimeVOT()+",");
			outputStream.print(usr.getValue().getLateArrivalVOT()+",");
			outputStream.print(usr.getValue().getMem().getBiddingAmount0()+",");
			outputStream.print(usr.getValue().getMem().getTravelCost());
			outputStream.print("\n");
		}
        outputStream.flush();
        outputStream.close();
        
        outputStream=new PrintWriter(new FileOutputStream ("myoutput/run"+String.valueOf(iter)+"_altInfo.csv"));		
		outputStream.print("DAT"+",");
		outputStream.print("Origin"+",");
		outputStream.print("Destination"+",");
		outputStream.print("Count"+",");
		outputStream.print("AvgDepartInterval"+",");
		outputStream.print("AvgArrivInterval"+",");
		outputStream.print("stdArrivInterval"+",");
		outputStream.print("Overall Average Speed");
		outputStream.print("\n");
        for(Map.Entry<Integer, altInfo> altRouteCost:controlCenter.getAvgAltTravelCost().entrySet())
        {
        	int O=altRouteCost.getKey()/10000;
        	int D=(altRouteCost.getKey()-O*10000)/1000;
        	int DAT=altRouteCost.getKey()-O*10000-D*1000;        	
			outputStream.print(DAT+",");
			outputStream.print(O+",");
			outputStream.print(D+",");
			outputStream.print(altRouteCost.getValue().getCount()+",");
			outputStream.print(altRouteCost.getValue().getAvgDepartInterval()+",");
			outputStream.print(altRouteCost.getValue().getAvgArrivInterval()+",");
			outputStream.print(altRouteCost.getValue().getStdArrivInterval()+",");
			outputStream.print(controlCenter.getArtAvgSpd()+",");
			outputStream.print("\n");
        }
        outputStream.flush();
        outputStream.close();
        return avgSpeed;
	}
	public static void outputUserHistory(int uID,int ITENUM,Map<Integer, User> userGroup) throws FileNotFoundException
	{
        PrintWriter outputStream=new PrintWriter(new FileOutputStream ("myoutput/user_"+String.valueOf(uID)+".csv"));		
		User ur=userGroup.get(uID);
		outputStream.print("userID"+","+uID+"\n");
		outputStream.print("Origin"+","+ur.getOrigin()+"\n");
		outputStream.print("Destination"+","+ur.getDestination()+"\n");
		outputStream.print("DAT"+","+ur.getDat()+"\n");
		outputStream.print("MostDesired"+","+ur.getMostDesiredInterval()+"\n");
		outputStream.print("EarlyArrivalVOT"+","+ur.getEarlyArrivalVOT()+"\n");
		outputStream.print("TravelTimeVOT"+","+ur.getTravelTimeVOT()+"\n");
		outputStream.print("LateArrivalVot"+","+ur.getLateArrivalVOT()+"\n");
		
		outputStream.print("State"+",");
		outputStream.print("Result"+ ",");
		outputStream.print("BiddingInterval"+",");
		outputStream.print("BiddingCost"+",");
		outputStream.print("DepTime"+",");
		outputStream.print("ArrTime"+",");		
		outputStream.print("TravelCost");
		outputStream.print("\n");
		
		int altSize=ur.getAltMemory().size();
		int resSize=ur.getResMemory().size();
		int i=0;
		int j=0;
		for(int ite=1;ite<ITENUM;ite++)
		{
			if(i<altSize && ur.getAltMemory().get(i).getIter0()==ite)
			{
				outputStream.print(ur.getAltMemory().get(i).getState0()+",");
				outputStream.print(ur.getAltMemory().get(i).isResult0()+",");
				outputStream.print(ur.getAltMemory().get(i).getBiddingInterval0()+",");
				outputStream.print(ur.getAltMemory().get(i).getBiddingAmount0()+",");
				outputStream.print((ur.getAltMemory().get(i).getDepTime()-6*3600)/120+",");
				outputStream.print((ur.getAltMemory().get(i).getArrTime()-6*3600)/120+",");
				outputStream.print(ur.getAltMemory().get(i).getTravelCost()+",");
				outputStream.print("\n");
				i=i+1;
			}
			else
				if(j<resSize && ur.getResMemory().get(j).getIter0()==ite)
				{
					outputStream.print(ur.getResMemory().get(j).getState0()+",");
					outputStream.print(ur.getResMemory().get(j).isResult0()+",");
					outputStream.print(ur.getResMemory().get(j).getBiddingInterval0()+",");
					outputStream.print(ur.getResMemory().get(j).getBiddingAmount0()+",");
					outputStream.print((ur.getResMemory().get(j).getDepTime()-6*3600)/120+",");
					outputStream.print((ur.getResMemory().get(j).getArrTime()-6*3600)/120+",");
					outputStream.print(ur.getResMemory().get(j).getTravelCost()+",");
					outputStream.print("\n");
					j=j+1;
				}
		}
        outputStream.flush();
        outputStream.close();
	}
}
