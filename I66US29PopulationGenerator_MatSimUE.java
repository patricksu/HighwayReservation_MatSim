import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
public class I66US29PopulationGenerator_MatSimUE {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		String routesFile="./Routes_forMatSim2.0.csv";
		String planFile="./Plan_MatSimUE_23000.csv";
		String line;
		String[] links;
		Integer[] linksInt;
		int i;
		double departTimeInterval;
		int O,D,Rev,DAT,RevInterval,hr,min,sec,seconds;
		BufferedReader br=new BufferedReader(new FileReader(routesFile));
		Integer key;
		
		Map<Integer, List<Id>> m=new HashMap<Integer, List<Id>>();
		Map<Integer, Double> m1=new HashMap<Integer,Double>();
		while((line=br.readLine())!=null)
		{
			links=line.split(",");
			key=Integer.parseInt(links[0])*100+Integer.parseInt(links[1])*10+Integer.parseInt(links[2]);
			List<Id> route=new ArrayList<Id>();
			for(i=3;i<links.length;i++)
			{
				route.add(sc.createId(links[i]));
			}
			m.put(key,route);
		}
		br.close();
		br=new BufferedReader(new FileReader(planFile));
		/*
		 * Pick the Network and the Population out of the Scenario for convenience. 
		 */
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();
		/*
		 * Pick the PopulationFactory out of the Population for convenience.
		 * It contains methods to create new Population items.
		 */
		PopulationFactory populationFactory = population.getFactory();

		i=0;
		while((line=br.readLine())!=null)
		{
			i=i+1;
			links=line.split(",");
			O=Integer.parseInt(links[0]);
			D=Integer.parseInt(links[1]);
			DAT=Integer.parseInt(links[2]);
			key=O*100+D*10+1;
			List<Id> route=new ArrayList<Id>(m.get(key));
			double homeEndTime=(DAT+6*30)*120;//20 intervals, or 40 minutes for travel
			double workStartTime=(DAT+6*30)*120;
			/*
				 * Create a Person and add it to the Population.
				 */
				Person person = populationFactory.createPerson(sc.createId(Integer.toString(i)));
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
				route.remove(0);
				Id endLinkId=route.get(route.size()-1);
				route.remove(route.size()-1);
				Activity activity1=populationFactory.createActivityFromLinkId("h10", startLinkId);
				
				if(DAT==60){
					activity1.setType("h11");}
				else if(DAT==75){
					activity1.setType("h12");}
				else if(DAT==90){
					activity1.setType("h13");}
				else if(DAT==105){
					activity1.setType("h14");}
				else{
					activity1.setType("h10");}					
				activity1.setEndTime(homeEndTime); // leave at 6 o'clock
				plan.addActivity(activity1); // add the Activity to the Plan
				
				/*
				 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a car will be used.
				 */
				Leg leg1=populationFactory.createLeg("car");
				plan.addLeg(leg1);
				
				/*
				 * Create a "work" Activity, at a different location.
				 */
				Activity activity2 = populationFactory.createActivityFromLinkId("w8", endLinkId);
				
				if(DAT==60){
					activity2.setType("w11");}
				else if(DAT==75){
					activity2.setType("w12");}
				else if(DAT==90){
					activity2.setType("w13");}
				else if(DAT==105){
					activity2.setType("w14");}
				else{
					activity2.setType("w8");}		
				
				
				activity2.setStartTime(workStartTime);
				plan.addActivity(activity2);
				person.addPlan(plan);
		}
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
		popWriter.write("./input/Population_UE23000.xml");
		
		/*
        PrintWriter outputStream3=new PrintWriter(new FileOutputStream ("out_ActEndTime.txt",true));
        for(Map.Entry<Integer, Double> entry : m1.entrySet())
        {      	
        	outputStream3.print(entry.getKey()+"\t");
        	outputStream3.print(entry.getValue()+"\t");
        	outputStream3.print("\n");
        }
        outputStream3.flush();
        outputStream3.close();
        */
	}
}

