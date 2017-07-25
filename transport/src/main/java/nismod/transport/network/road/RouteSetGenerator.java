package nismod.transport.network.road;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;

/**
 * RouteSetGenerator can generate, save and read route sets for route choice.
 * @author Milan Lovric
 *
 */
public class RouteSetGenerator {

	private MultiKeyMap routes;
	
	private RoadNetwork roadNetwork;
	
	public RouteSetGenerator(RoadNetwork roadNetwork) {
		
		this.roadNetwork = roadNetwork;
		this.routes = new MultiKeyMap();
	}
	
	/**
	 * Adds a route to the set of routes.
	 * @param route
	 */
	public void addRoute(Route route) {
		
		if (route.isEmpty()) {
			System.err.println("Cannot add empty route!");
			return;
		}
		
		int origin = route.getOriginNode().getID();
		int destination = route.getDestinationNode().getID();
		
		RouteSet set = getRouteSet(origin, destination);
		if (set == null) {
			set = new RouteSet(route.getOriginNode(),route.getDestinationNode());
			routes.put(origin, destination, set);
		}
		set.addRoute(route);
	}
	
	/**
	 * Generates a route set using the link elimination method -
	 * It first finds the fastest path and then blocks each of its links and tries to find an alternative path.
	 * @param origin
	 * @param destination
	 */
	public void generateRouteSetWithLinkElimination(int origin, int destination) {
		
		DirectedNode originNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(origin);
		DirectedNode destinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destination);
		//RouteSet rs = new RouteSet(originNode, destinationNode);
		
		//find the fastest path from origin to destination
		RoadPath path  = this.roadNetwork.getFastestPath(originNode, destinationNode, null);
		//System.out.println(path.toString());
		Route route = new Route(path);
		//rs.addRoute(route);
		//rs.printChoiceSet();
		this.addRoute(route);
		
		//link elimination method
		for (Object o: path.getEdges()) {
			HashMap<Integer, Double> linkTravelTimes = new HashMap<Integer, Double>();
			DirectedEdge edge = (DirectedEdge) o;
			linkTravelTimes.put(edge.getID(), Double.MAX_VALUE); //blocks by setting a maximum travel time
			path = this.roadNetwork.getFastestPath(originNode, destinationNode, linkTravelTimes);
			if (path != null) {
				System.out.println(path.toString());
				route = new Route(path);
				//rs.addRoute(route);
				this.addRoute(route);
			}
		}
	}

	/**
	 * Getter method for a route set between a specific origin and a destination.
	 * @param origin Origin node ID.
	 * @param destination Destination node ID.
	 * @return
	 */
	public RouteSet getRouteSet(int origin, int destination) {
		
		RouteSet set = (RouteSet) routes.get(origin, destination);
		return set;
	}
	
	/**
	 * Prints all route sets.
	 */
	public void print() {

		for (Object mk: routes.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			int destination = (int) ((MultiKey)mk).getKey(1);
		
			((RouteSet)routes.get(origin, destination)).printChoiceSet();
			
		}
	}
	
	/**
	 * Saves all route sets into a text file.
	 * @param fileName File name.
	 * @param append Whether to append to an existing file.
	 */
	public void saveRoutes(String fileName, boolean append) {
		
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(fileName, append);
			//iterate over all route sets
			for (Object value: routes.values()) {
				RouteSet rs = (RouteSet)value;
				//iterate over all routes
				for (Route route: rs.getChoiceSet()) 
					fileWriter.write(route.getFormattedString() + "\n");
			}
		} catch (Exception e) {
			System.err.println("Error in fileWriter!");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing fileWriter!");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Reads route sets from a text file.
	 * @param fileName File name.
	 */
	public void readRoutes(String fileName) {
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
		    String line = br.readLine();
		    while (line != null) {
		    	String splitLine[] = line.split(":");
		    	//System.out.println(Arrays.toString(splitLine));
		    	int originID = Integer.parseInt(splitLine[0]);
		    	int destinationID = Integer.parseInt(splitLine[1]);
		    	String edges[] = splitLine[2].split("-");
		    	Route route = new Route();
		    	for (String edge: edges) 
		    		route.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge().get(Integer.parseInt(edge)));
		    	//System.out.println(route.getFormattedString());
		    	this.addRoute(route);
		    	line = br.readLine();
		    }
		} catch (Exception e) {
			System.err.println("Error in fileReader!");
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				System.err.println("Error while closing BufferedReader!");
				e.printStackTrace();
			}
		}
	}
}
