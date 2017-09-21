package nismod.transport.network.road;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;

import nismod.transport.demand.ODMatrix;

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
		
		if (!route.isValid()) {
			System.err.println("Route is not valid. Not adding the route!");
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
	 * Generates a route set between two nodes using the link elimination method -
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
		//System.out.println("Path validity: " + path.isValid());
		
		if (path == null) {
			System.err.printf("Unable to find the fastest path between nodes %d and %d! Link elimination method unsucsessful!\n", origin, destination);
			return;
		}
		
		Route route = new Route(path);
		//System.out.println("Route validity: " + route.isValid());
		//rs.addRoute(route);
		//rs.printChoiceSet();
		
		//check that origin and destination node are correct!
		if (route.getOriginNode().equals(originNode) && route.getDestinationNode().equals(destinationNode))
			this.addRoute(route);
		else {
			System.err.println("Fastest path does not contain correct origin and destination nodes!");
			return;
		}
		
		//link elimination method
		for (Object o: path.getEdges()) {
			HashMap<Integer, Double> linkTravelTimes = new HashMap<Integer, Double>();
			DirectedEdge edge = (DirectedEdge) o;
			linkTravelTimes.put(edge.getID(), Double.MAX_VALUE); //blocks by setting a maximum travel time
			path = this.roadNetwork.getFastestPath(originNode, destinationNode, linkTravelTimes);
			if (path != null) {
				//System.out.println(path.toString());
				//System.out.println("Path validity: " + path.isValid());
				route = new Route(path);
				//System.out.println(route);
				//rs.addRoute(route);
				if (route.getOriginNode().equals(originNode) && route.getDestinationNode().equals(destinationNode))
					this.addRoute(route);
				else 
					System.err.println("Path generated with link elimination does not contain correct origin and destination nodes!");
			}
		}
	}
	
	/**
	 * Generates routes between all combinations of nodes from two LAD zones
	 * @param originLAD
	 * @param destinationLAD
	 */
	public void generateRouteSetWithLinkElimination(String originLAD, String destinationLAD) {
		
		List<Integer> originNodes = this.roadNetwork.getZoneToNodes().get(originLAD);
		List<Integer> destinationNodes = this.roadNetwork.getZoneToNodes().get(destinationLAD);
		System.out.printf("Generating routes from %s (%d nodes) to %s (%d nodes) %n", originLAD, originNodes.size(), destinationLAD, destinationNodes.size());
		for (Integer origin: originNodes)
			for (Integer destination: destinationNodes)
				if (origin != destination) 					
					this.generateRouteSetWithLinkElimination(origin, destination);
	}
	
	/**
	 * Generates routes between top N nodes (sorted by gravitating population) from two LAD zones.
	 * If origin and destination LAD are the same (i.e., intra-zonal), then use all the nodes
	 * @param originLAD
	 * @param destinationLAD
	 * @param topNodes
	 */
	public void generateRouteSetWithLinkElimination(String originLAD, String destinationLAD, int topNodes) {
		
		//if intra-zonal, use all node combinations:
		if (originLAD.equals(destinationLAD)) this.generateRouteSetWithLinkElimination(originLAD, destinationLAD);
		else { //if inter-zonal, use only top nodes
		
		//assume pre-sorted
		List<Integer> originNodes = this.roadNetwork.getZoneToNodes().get(originLAD);
		//System.out.println("origin nodes: " + originNodes);
		List<Integer> destinationNodes = this.roadNetwork.getZoneToNodes().get(destinationLAD);
		//System.out.println("destination nodes: " + destinationNodes);
				
		System.out.printf("Generating routes between top %d nodes from %s to %s %n", topNodes, originLAD, destinationLAD);
		for (int i = 0; i < topNodes && i < originNodes.size(); i++)
			for (int j = 0; j < topNodes && j < destinationNodes.size(); j++)	{
				
				int origin = originNodes.get(i);
				int destination = destinationNodes.get(j);
				//System.out.printf("Generating routes between nodes %d and %d \n", origin, destination);
				this.generateRouteSetWithLinkElimination(origin, destination);
			}
		}
	}
	
	/**
	 * Generates routes for all non-zero OD flows in the OD matrix.
	 * For inter-zonal flows generates routes only between top N nodes.
	 * @param matrix
	 * @param topNodes
	 */
	public void generateRouteSetWithLinkElimination(ODMatrix matrix, int topNodes) {
		
		for (MultiKey mk: matrix.getKeySet()) {
				String originLAD = (String) ((MultiKey)mk).getKey(0);
				String destinationLAD = (String) ((MultiKey)mk).getKey(1);
				if (matrix.getFlow(originLAD, destinationLAD) != 0)
					this.generateRouteSetWithLinkElimination(originLAD, destinationLAD, topNodes);
		}
	}
	
	/**
	 * Generates routes for all non-zero OD flows in the OD matrix.
	 * @param matrix
	 */
	public void generateRouteSetWithLinkElimination(ODMatrix matrix) {
		
		for (MultiKey mk: matrix.getKeySet()) {
				String originLAD = (String) ((MultiKey)mk).getKey(0);
				String destinationLAD = (String) ((MultiKey)mk).getKey(1);
				if (matrix.getFlow(originLAD, destinationLAD) != 0)
					this.generateRouteSetWithLinkElimination(originLAD, destinationLAD);
		}
	}
	
	/**
	 * Generates routes for a slice of the OD matrix (useful for cluster computing).
	 * @param matrix Origin-destination matrix.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 */
	public void generateRouteSetWithLinkElimination(ODMatrix matrix, int sliceIndex, int sliceNumber) {
		
		List<String> origins = matrix.getOrigins();
		List<String> destinations = matrix.getDestinations();
		
		int originsPerSlice = (int) Math.floor(1.0 * origins.size() / sliceNumber); //the last slice may have a different number of origins
		//int originsInLastSlice = origins.size() - (sliceNumber - 1) * originsPerSlice;
		
		if (sliceIndex < sliceNumber) {
			for (int i = (sliceIndex - 1) * originsPerSlice; i < sliceIndex * originsPerSlice && i < origins.size(); i++) {
				String originLAD = origins.get(i);
				for (String destinationLAD: destinations) 
					if (matrix.getFlow(originLAD, destinationLAD) != 0)
						this.generateRouteSetWithLinkElimination(originLAD, destinationLAD);
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				String originLAD = origins.get(i);
				for (String destinationLAD: destinations) 
					if (matrix.getFlow(originLAD, destinationLAD) != 0)
						this.generateRouteSetWithLinkElimination(originLAD, destinationLAD);
			}
		}
	}
	
	/**
	 * Generates routes for a slice of the OD matrix (useful for cluster computing), for topNodes only
	 * @param matrix Origin-destination matrix.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 * @param topNodes Number of topNodes to consider for inter-zonal routes.
	 */
	public void generateRouteSetWithLinkElimination(ODMatrix matrix, int sliceIndex, int sliceNumber, int topNodes) {
		
		List<String> origins = matrix.getOrigins();
		List<String> destinations = matrix.getDestinations();
		
		int originsPerSlice = (int) Math.floor(1.0 * origins.size() / sliceNumber); //the last slice may have a different number of origins
		//int originsInLastSlice = origins.size() - (sliceNumber - 1) * originsPerSlice;
		
		if (sliceIndex < sliceNumber) {
			for (int i = (sliceIndex - 1) * originsPerSlice; i < sliceIndex * originsPerSlice && i < origins.size(); i++) {
				String originLAD = origins.get(i);
				for (String destinationLAD: destinations) 
					if (matrix.getFlow(originLAD, destinationLAD) != 0)
						this.generateRouteSetWithLinkElimination(originLAD, destinationLAD, topNodes);
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				String originLAD = origins.get(i);
				for (String destinationLAD: destinations) 
					if (matrix.getFlow(originLAD, destinationLAD) != 0)
						this.generateRouteSetWithLinkElimination(originLAD, destinationLAD, topNodes);
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
	 * Clears all stored routes.
	 */
	public void clearRoutes() {
		
		this.routes.clear();
	}
	
	/**
	 * Prints all route sets.
	 */
	public void printChoiceSets() {

		for (Object mk: routes.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			int destination = (int) ((MultiKey)mk).getKey(1);
		
			((RouteSet)routes.get(origin, destination)).printChoiceSet();
		}
	}
	
	/**
	 * Prints all route set statistics.
	 */
	public void printStatistics() {

		System.out.println("Number of OD pairs / route sets: " + this.getNumberOfRouteSets());
		System.out.println("Total number of routes: " + this.getNumberOfRoutes());
	}
	
	/**
	 * Gets the numbers of route sets (OD pairs).
	 * @return Number of route sets.
	 */
	public int getNumberOfRouteSets() { 
	
		return this.routes.size();
	}
	
	/**
	 * Gets the total number of routes.
	 * @return Number of routes.
	 */
	public int getNumberOfRoutes() { 
	
		int totalRoutes = 0;
		for (Object mk: routes.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			int destination = (int) ((MultiKey)mk).getKey(1);
			RouteSet rs = (RouteSet)routes.get(origin, destination);
			//rs.printStatistics();
			totalRoutes += rs.getSize();
		}
		return totalRoutes;
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
					fileWriter.write(route.getFormattedString() + System.getProperty("line.separator"));
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
		
		System.out.println("Reading pre-generated routes...");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
		    String line = br.readLine();
		    //System.out.println(line);
		    
		    while (line != null) {
		    	String splitLine[] = line.split(":");
		    	//System.out.println(Arrays.toString(splitLine));
		    	int originID = Integer.parseInt(splitLine[0]);
		    	int destinationID = Integer.parseInt(splitLine[1]);
		    	String edges[] = splitLine[2].split("-");
		    	//System.out.println(Arrays.toString(edges));
		    	
		    	Route route = new Route();
		    	boolean success = false;
		    	for (String edge: edges) {
		    		success = route.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge().get(Integer.parseInt(edge)));
		    		if (!success) break;
		    	}
		    	//System.out.println(route.getFormattedString());
		    	if (success) this.addRoute(route); //add route only if fine;
		    	line = br.readLine();
		    	//System.out.println(line);
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
