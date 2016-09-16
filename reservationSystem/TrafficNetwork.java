package reservationSystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class TrafficNetwork {
	private double freewaySpeed;
	private double artLSpeed;
	
	private Map<Integer, List<Id>> routes=new HashMap<Integer, List<Id>> ();
	public Map<Integer, List<Id>> getRoutes() {
		return routes;
	}
	public void setRoutes(Map<Integer, List<Id>> routes) {
		this.routes = routes;
	}
}
