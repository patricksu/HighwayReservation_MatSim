package reservationSystem;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import reservationSystem.User.StateSet;

public class ReservationControlCenter {
	private int cap; // per interval
	private double artAvgSpd;
	private int[][] resource=new int[120][6];
	private ArrayList<resVSingleUser> resVUsers=new ArrayList<resVSingleUser>();
	private Map<Integer, altInfo> avgAltTravelCost=new HashMap<Integer, altInfo> ();
	public ReservationControlCenter(int cap) {
		super();
		this.cap = cap;
	}
	public class altInfo
	{
		private int DAT;
		private int O;
		private int D;
		private int count=-1;
		private double avgDepartInterval=-1;
		private double avgArrivInterval=-1;
		private double stdArrivInterval=-1;
		private List<Double> arrivIntervalList;
		public altInfo(int dAT, int o, int d, int count, double avgDepartInterval, double avgArrivInterval) 
		{
			super();
			DAT = dAT;
			O = o;
			D = d;
			this.count = count;
			this.avgDepartInterval = avgDepartInterval;
			this.avgArrivInterval = avgArrivInterval;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		public void countPlus()
		{
			this.count=this.count+1;
		}
		public double getAvgDepartInterval() {
			return avgDepartInterval;
		}
		public void setAvgDepartInterval(double avgDepartInterval) {
			this.avgDepartInterval = avgDepartInterval;
		}
		public void avgDepartIntervalPlus(double x)
		{
			this.avgDepartInterval=this.avgDepartInterval+x;
		}
		public double getAvgArrivInterval() {
			return avgArrivInterval;
		}
		public void setAvgArrivInterval(double avgArrivInterval) {
			this.avgArrivInterval = avgArrivInterval;
		}
		public void avgArrivIntervalplus(double x)
		{
			this.avgArrivInterval=this.avgArrivInterval+x;
		}
		public int getDAT() {
			return DAT;
		}
		public void setDAT(int dAT) {
			DAT = dAT;
		}
		public int getO() {
			return O;
		}
		public void setO(int o) {
			O = o;
		}
		public int getD() {
			return D;
		}
		public void setD(int d) {
			D = d;
		}
		public double getStdArrivInterval() {
			return stdArrivInterval;
		}
		public void setStdArrivInterval(double stdArrivInterval) {
			this.stdArrivInterval = stdArrivInterval;
		}
		public void addToArrivList(double x)
		{
			this.arrivIntervalList.add(x);
		}
		public void iniArrivList()
		{
			this.arrivIntervalList=new ArrayList<Double>();
		}
		public List<Double> getArrivIntervalList() {
			return arrivIntervalList;
		}	
	}
	public class resVSingleUser
	{
		private int userID;
		private double biddingAmount;
		public resVSingleUser(int userID, double biddingAmount) {
			super();
			this.userID = userID;
			this.biddingAmount = biddingAmount;
		}
		public int getUserID() {
			return userID;
		}
		public void setUserID(int userID) {
			this.userID = userID;
		}
		public double getBiddingAmount() {
			return biddingAmount;
		}
		public void setBiddingAmount(double biddingAmount) {
			this.biddingAmount = biddingAmount;
		}		
	}
		
	public class myBiddingCompare implements Comparator<resVSingleUser>{
		@Override
		public int compare(resVSingleUser o1, resVSingleUser o2) {
			// TODO Auto-generated method stub
			if(o1.getBiddingAmount()>=o2.getBiddingAmount())
				return -1;
			else
				return 1;
			}
		}

	public void addResVUser(int userID,double biddingAmount)
	{
		resVSingleUser resVUsr=new resVSingleUser(userID,biddingAmount);
		this.resVUsers.add(resVUsr);
	}
	public void sortResVUsers()
	{
		Collections.sort(resVUsers,new myBiddingCompare());
	}	
	
	public void scheduleRequests(Map<Integer, User> userGroup)
	{
		resource= new int[120][6];
		for(resVSingleUser usr:resVUsers)
		{
			int userID=usr.getUserID();
			int O=userGroup.get(userID).getOrigin();
			int D=userGroup.get(userID).getDestination();
			int bidInterval=userGroup.get(userID).getMem().getBiddingInterval0();
			boolean available=true;
			for(int k=O;k<D;k++)
			{
				if(resource[bidInterval+k-O-1][k-1]>=this.cap)
				{
					available=false;
					break;
				}
			}
			userGroup.get(userID).getMem().setResult0(available);
			if(available)
				for(int k=O;k<D;k++)
				{
					resource[bidInterval+k-O-1][k-1]=resource[bidInterval+k-O-1][k-1]+1;
				}
		}
	}

	public void generatePopulationFile(Map<Integer, User> userGroup,Map<Integer, List<Id>> routes)
	{
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		Map<Integer,User> treeMap=new TreeMap<Integer,User>(userGroup);
		for(Map.Entry<Integer, User> usr:treeMap.entrySet())
		{
			int O=usr.getValue().getOrigin();
			int D=usr.getValue().getDestination();
			int ResV=0;
			if(usr.getValue().getMem().getState0()!=StateSet.ALT && usr.getValue().getMem().isResult0())
				ResV=1;
			int DAT=usr.getValue().getDat();
			//int RevInterval=usr.getValue().getMem().getBiddingInterval0();
			double seconds=usr.getValue().getMem().getDepTime();
			int key=O*10000+D*1000+ResV;
			List<Id> route=routes.get(key);		
			/*
				 * Create a Person and add it to the Population.
				 */
				Person person = populationFactory.createPerson(sc.createId(Integer.toString(usr.getKey())));
				population.addPerson(person);

				/*
				 * Create a Plan for the Person
				 */
				Plan plan = populationFactory.createPlan();
				
				/*
				 * Create a "home" Activity for the Person. In order to have the Person end its day at the same location,
				 * we keep the home coordinates for later use (see below).
				 * Note that we use the CoordinateTransformation created above.
				 */
				Id startLinkId=route.get(0);
				Id endLinkId=route.get(route.size()-1);
				
				Activity activity1=populationFactory.createActivityFromLinkId("h10", startLinkId);
				activity1.setEndTime(seconds); // leave at 6 o'clock
				plan.addActivity(activity1); // add the Activity to the Plan
				
				/*
				 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a car will be used.
				 */
				Leg leg1=populationFactory.createLeg("car");
				leg1.setDepartureTime(seconds);
				
				Route rt= new org.matsim.core.population.routes.LinkNetworkRouteImpl(startLinkId, route.subList(1, route.size()-1), endLinkId);
				leg1.setRoute(rt);
				plan.addLeg(leg1);
				
				/*
				 * Create a "work" Activity, at a different location.
				 */
				Activity activity2 = populationFactory.createActivityFromLinkId("w8", endLinkId);
				plan.addActivity(activity2);
				person.addPlan(plan);
		}
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
		popWriter.write("./input/Population_4.xml");
	}
	public void outputResvResults(Map<Integer, User> userGroup) throws FileNotFoundException
	{
        PrintWriter outputStream=new PrintWriter(new FileOutputStream ("./myoutput/reservationResult.csv",true));        
		Map<Integer,User> treeMap=new TreeMap<Integer,User>(userGroup);
		outputStream.print("UserID"+",");
		outputStream.print("Origin"+",");
		outputStream.print("Destination"+",");
		outputStream.print("DAT"+",");
		outputStream.print("BiddingInterval"+",");
		outputStream.print("Result"+",");
		outputStream.print("DepartureTime"+",");
		outputStream.print("\n");
		for(Map.Entry<Integer, User> usr:treeMap.entrySet())
		{
			outputStream.print(usr.getKey()+",");
			outputStream.print(usr.getValue().getOrigin()+",");
			outputStream.print(usr.getValue().getDestination()+",");
			outputStream.print(usr.getValue().getDat()+",");
			outputStream.print(usr.getValue().getMem().getBiddingInterval0()+",");
			outputStream.print(usr.getValue().getMem().isResult0()?1:0 +",");
			outputStream.print(usr.getValue().getMem().getDepTime()+",");
			outputStream.print("\n");
		}
        outputStream.flush();
        outputStream.close();
	}
	public void resetResVUser()
	{
		this.resVUsers=null;
		this.resVUsers=new ArrayList<resVSingleUser>();
	}
	public int getCap() {
		return cap;
	}
	public void setCap(int cap) {
		this.cap = cap;
	}
	public int[][] getResource() {
		return resource;
	}
	public void setResource(int[][] resource) {
		this.resource = resource;
	}
	public ArrayList<resVSingleUser> getResVUsers() {
		return resVUsers;
	}
	public void setResVUsers(ArrayList<resVSingleUser> resVUsers) {
		this.resVUsers = resVUsers;
	}
	public Map<Integer, altInfo> getAvgAltTravelCost() {
		return avgAltTravelCost;
	}
	public void setAvgAltTravelCost(Map<Integer, altInfo> avgAltTravelCost) {
		this.avgAltTravelCost = avgAltTravelCost;
	}
	public double getArtAvgSpd() {
		return artAvgSpd;
	}
	public void setArtAvgSpd(double artAvgSpd) {
		this.artAvgSpd = artAvgSpd;
	}	
}
