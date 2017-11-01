package nismod.transport.network.road;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.geotools.graph.build.line.BasicDirectedLineGraphBuilder;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;

import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrix;
import nismod.transport.utility.RandomSingleton;

/**
 * RouteSetGenerator can generate, save and read route sets for route choice.
 * @author Milan Lovric
 *
 */
/**
 * @author Milan Lovric
 *
 */
/**
 * @author Milan Lovric
 *
 */
public class RouteSetGenerator {
	
	public static final int ROUTE_LIMIT = 5;
	public static final int GENERATION_LIMIT = 10;

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
		
		//NOTE this is commented, as single node routes are empty but still valid!
//		if (route.isEmpty()) {
//			System.err.println("Cannot add empty route!");
//			return;
//		}
		
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
	 * Generates a route set between two nodes (if it does not already exist in the route set).
	 * @param origin
	 * @param destination
	 */
	public void generateRouteSetNodeToNode(int origin, int destination) {
		
		//if (origin == destination) System.err.println("Origin and destination node are the same.");
				
		RouteSet fetchedRouteSet = this.getRouteSet(origin, destination);
		//generate only if it does not already exist
		if (fetchedRouteSet == null)
			generateRouteSetWithRandomLinkEliminationRestricted(origin, destination);
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
		RoadPath fastestPath  = this.roadNetwork.getFastestPath(originNode, destinationNode, null);
		if (fastestPath == null) {
			System.err.printf("Unable to find the fastest path between nodes %d and %d! Link elimination method unsucsessful!\n", origin, destination);
			return;
		}
		//System.out.println(fastestPath.toString());
		//System.out.println("Path validity: " + fastestPath.isValid());
		
		Route fastestRoute = new Route(fastestPath);
		//System.out.println("Route validity: " + fastestRoute.isValid());
		//rs.addRoute(fastestRoute);
		//rs.printChoiceSet();
		
		//check that origin and destination node are correct!
		if (fastestRoute.getOriginNode().equals(originNode) && fastestRoute.getDestinationNode().equals(destinationNode))
			this.addRoute(fastestRoute);
		else {
			System.err.println("Fastest path does not contain correct origin and destination nodes!");
			return;
		}
		
		//link elimination method
		for (Object o: fastestPath.getEdges()) {
			HashMap<Integer, Double> linkTravelTimes = new HashMap<Integer, Double>();
			DirectedEdge edge = (DirectedEdge) o;
			linkTravelTimes.put(edge.getID(), Double.MAX_VALUE); //blocks by setting a maximum travel time
			RoadPath path = this.roadNetwork.getFastestPath(originNode, destinationNode, linkTravelTimes);
			if (path != null) {
				//System.out.println(path.toString());
				//System.out.println("Path validity: " + path.isValid());
				Route route = new Route(path);
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
	 * Generates a route set between two nodes using the random link elimination method -
	 * It first finds the fastest path and then blocks random links within the fastest path and tries to find an alternative path.
	 * The search is limited by the total number of path finding calls and the required number of generated paths.
	 * @param origin
	 * @param destination
	 */
	public void generateRouteSetWithRandomLinkEliminationRestricted(int origin, int destination) {

		RandomSingleton rng = RandomSingleton.getInstance();

		//do not generate route set if origin or destination node is blacklisted as no routes are possible
		if (this.roadNetwork.isBlacklistedAsStartNode(origin) || this.roadNetwork.isBlacklistedAsEndNode(destination)) return;
		
		//System.out.printf("Generating route set between nodes %d and %d with random link elimination.\n", origin, destination);

		//if they are the same, create a single node path
		if (origin == destination) {
			RoadPath rp = new RoadPath();
			rp.add(roadNetwork.getNodeIDtoNode().get(origin));
			Route route = new Route(rp);
			this.addRoute(route);
			return;
		}
		
		DirectedNode originNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(origin);
		DirectedNode destinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destination);
		//RouteSet rs = new RouteSet(originNode, destinationNode);

		//find the fastest path from origin to destination (astar or dijkstra)
		RoadPath fastestPath  = this.roadNetwork.getFastestPath(originNode, destinationNode, null);
		//RoadPath fastestPath  = this.roadNetwork.getFastestPathDijkstra(originNode, destinationNode, null);

		if (fastestPath == null) {
			System.err.printf("Unable to find the fastest path between nodes %d and %d! Link elimination method unsucsessful!\n", origin, destination);
			return;
		}
		//System.out.println("RoadPath " + fastestPath.toString());
		//System.out.println("Path validity: " + fastestPath.isValid());

		Route fastestRoute = new Route(fastestPath);
		//System.out.println("Route: " + fastestRoute.getFormattedString());
		//System.out.println("Route validity: " + fastestRoute.isValid());
		//System.out.println("Route origin node: " + fastestRoute.getOriginNode().getID());
		//System.out.println("Route destination node: " + fastestRoute.getDestinationNode().getID());
		//check that origin and destination node are correct!
		if (fastestRoute.getOriginNode().equals(originNode) && fastestRoute.getDestinationNode().equals(destinationNode))
			this.addRoute(fastestRoute);
		else {
			System.err.printf("Fastest route between nodes %d and %d does not contain correct origin and destination nodes! Link elimination method unsucsessful!\n", origin, destination);
			return;
		}

		int pathSizeInLinks = fastestPath.size() - 1; //number of edges = number of nodes - 1

		//if the number of links in the fastest path is smaller than the ROUTE_LIMIT,
		//then block each link successively (this will generate up to ROUTE_LIMIT routes using less calculations than the random method)
		if (pathSizeInLinks < ROUTE_LIMIT) {

			for (Object o: fastestPath.getEdges()) {

				HashMap<Integer, Double> linkTravelTimes = new HashMap<Integer, Double>();
				DirectedEdge edge = (DirectedEdge) o;
				//System.out.printf("Blocking edge (%d)-%d->(%d) \n", edge.getInNode().getID(), edge.getID(), edge.getOutNode().getID());

				//linkTravelTimes.put(edge.getID(), Double.POSITIVE_INFINITY); //blocks by setting a maximum travel time (does not work for astar)

				//block the edge by removing it temporarily from the graph
				BasicDirectedLineGraphBuilder graphBuilder = new BasicDirectedLineGraphBuilder();
				graphBuilder.importGraph(this.roadNetwork.getNetwork());
				int edgeID = edge.getID();
				graphBuilder.removeEdge(edge);

				//find the fastest path
				RoadPath path = this.roadNetwork.getFastestPath(originNode, destinationNode, linkTravelTimes);
				//RoadPath path = this.roadNetwork.getFastestPathDijkstra(originNode, destinationNode, linkTravelTimes);

				//return the removed edge
				graphBuilder.addEdge(edge);
				edge.setID(edgeID);

				if (path != null) {
					//System.out.println("Nodes: " + path.toString());
					//System.out.println("Path validity: " + path.isValid());
					Route route = new Route(path);
					//System.out.println("Route: " + route.getFormattedString());
					//rs.addRoute(route);
					if (route.getOriginNode().equals(originNode) && route.getDestinationNode().equals(destinationNode))
						this.addRoute(route);
					else 
						System.err.println("Route generated with link elimination does not contain correct origin and destination nodes! Skipping this route.");
				}
			}
		} else { //otherwise use the actual random link elimination method 

			for (int i = 0; i < GENERATION_LIMIT; i++) {

				int randomIndex = rng.nextInt(pathSizeInLinks);
				Object o = fastestPath.getEdges().get(randomIndex); //pick random edge
				HashMap<Integer, Double> linkTravelTimes = new HashMap<Integer, Double>();
				DirectedEdge edge = (DirectedEdge) o;
				//System.out.printf("Blocking edge (%d)-%d->(%d) \n", edge.getInNode().getID(), edge.getID(), edge.getOutNode().getID());

				//linkTravelTimes.put(edge.getID(), Double.POSITIVE_INFINITY); //blocks by setting a maximum travel time (does not work for astar)

				//block the edge by removing it temporarily from the graph
				BasicDirectedLineGraphBuilder graphBuilder = new BasicDirectedLineGraphBuilder();
				graphBuilder.importGraph(this.roadNetwork.getNetwork());
				int edgeID = edge.getID();
				graphBuilder.removeEdge(edge);

				//find the fastest path
				RoadPath path = this.roadNetwork.getFastestPath(originNode, destinationNode, linkTravelTimes);
				//RoadPath path = this.roadNetwork.getFastestPathDijkstra(originNode, destinationNode, linkTravelTimes);

				//return the removed edge
				graphBuilder.addEdge(edge);
				edge.setID(edgeID);

				if (path != null) {
					//System.out.println("Nodes: " + path.toString());
					//System.out.println("Path validity: " + path.isValid());
					Route route = new Route(path);
					//System.out.println("Route: " + route.getFormattedString());
					//rs.addRoute(route);
					if (route.getOriginNode().equals(originNode) && route.getDestinationNode().equals(destinationNode))
						this.addRoute(route);
					else 
						System.err.println("Route generated with link elimination does not contain correct origin and destination nodes! Skipping this route.");
				}
				RouteSet rs = (RouteSet)routes.get(origin, destination);
				if (rs.getSize() >= ROUTE_LIMIT) break; //stop if sufficient number of routes has been generated 
			}
		}
	}

	/**
	 * Generates routes between all combinations of nodes from two LAD zones
	 * @param originLAD
	 * @param destinationLAD
	 */
	public void generateRouteSetZoneToZone(String originLAD, String destinationLAD) {
		
		List<Integer> originNodes = this.roadNetwork.getZoneToNodes().get(originLAD);
		List<Integer> destinationNodes = this.roadNetwork.getZoneToNodes().get(destinationLAD);
		//System.out.printf("Generating routes from %s (%d nodes) to %s (%d nodes) %n", originLAD, originNodes.size(), destinationLAD, destinationNodes.size());
		for (Integer origin: originNodes)
			for (Integer destination: destinationNodes)
				if (origin != destination) 					
					this.generateRouteSetNodeToNode(origin, destination);
	}
	
	/**
	 * Generates routes between top N nodes (sorted by gravitating population) from two LAD zones.
	 * If origin and destination LAD are the same (i.e., intra-zonal), then use all the nodes
	 * @param originLAD
	 * @param destinationLAD
	 * @param topNodes
	 */
	public void generateRouteSetZoneToZone(String originLAD, String destinationLAD, int topNodes) {
		
		//if intra-zonal, use all node combinations:
		if (originLAD.equals(destinationLAD)) this.generateRouteSetZoneToZone(originLAD, destinationLAD);
		else { //if inter-zonal, use only top nodes
		
		//assume pre-sorted (sortGravityNodes method must be used!)
		List<Integer> originNodes = this.roadNetwork.getZoneToNodes().get(originLAD);
		//System.out.println("origin nodes: " + originNodes);
		List<Integer> destinationNodes = this.roadNetwork.getZoneToNodes().get(destinationLAD);
		//System.out.println("destination nodes: " + destinationNodes);
				
		//System.out.printf("Generating routes between top %d nodes from %s to %s %n", topNodes, originLAD, destinationLAD);
		for (int i = 0; i < topNodes && i < originNodes.size(); i++)
			for (int j = 0; j < topNodes && j < destinationNodes.size(); j++)	{
				
				int origin = originNodes.get(i);
				int destination = destinationNodes.get(j);
				//System.out.printf("Generating routes between nodes %d and %d \n", origin, destination);
				this.generateRouteSetNodeToNode(origin, destination);
			}
		}
	}
	
	/**
	 * Generates routes for all non-zero OD flows in the OD matrix.
	 * For inter-zonal flows generates routes only between top N nodes.
	 * @param matrix
	 * @param topNodes
	 */
	public void generateRouteSetForODMatrix(ODMatrix matrix, int topNodes) {
		
		for (MultiKey mk: matrix.getKeySet()) {
				String originLAD = (String) ((MultiKey)mk).getKey(0);
				String destinationLAD = (String) ((MultiKey)mk).getKey(1);
				if (matrix.getFlow(originLAD, destinationLAD) != 0)
					this.generateRouteSetZoneToZone(originLAD, destinationLAD, topNodes);
		}
	}
	
	/**
	 * Generates routes for all non-zero OD flows in the OD matrix.
	 * @param matrix
	 */
	public void generateRouteSetForODMatrix(ODMatrix matrix) {
		
		for (MultiKey mk: matrix.getKeySet()) {
				String originLAD = (String) ((MultiKey)mk).getKey(0);
				String destinationLAD = (String) ((MultiKey)mk).getKey(1);
				if (matrix.getFlow(originLAD, destinationLAD) != 0)
					this.generateRouteSetZoneToZone(originLAD, destinationLAD);
		}
	}
	
	/**
	 * Generates routes for a slice of the OD matrix (useful for cluster computing).
	 * @param matrix Origin-destination matrix.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 */
	public void generateRouteSetForODMatrix(ODMatrix matrix, int sliceIndex, int sliceNumber) {
		
		List<String> origins = matrix.getOrigins();
		List<String> destinations = matrix.getDestinations();
		
		int originsPerSlice = (int) Math.floor(1.0 * origins.size() / sliceNumber); //the last slice may have a different number of origins
		//int originsInLastSlice = origins.size() - (sliceNumber - 1) * originsPerSlice;
		
		if (sliceIndex < sliceNumber) {
			for (int i = (sliceIndex - 1) * originsPerSlice; i < sliceIndex * originsPerSlice && i < origins.size(); i++) {
				String originLAD = origins.get(i);
				for (String destinationLAD: destinations) 
					if (matrix.getFlow(originLAD, destinationLAD) != 0)
						this.generateRouteSetZoneToZone(originLAD, destinationLAD);
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				String originLAD = origins.get(i);
				for (String destinationLAD: destinations) 
					if (matrix.getFlow(originLAD, destinationLAD) != 0)
						this.generateRouteSetZoneToZone(originLAD, destinationLAD);
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
	public void generateRouteSetForODMatrix(ODMatrix matrix, int sliceIndex, int sliceNumber, int topNodes) {
		
		List<String> origins = matrix.getOrigins();
		List<String> destinations = matrix.getDestinations();
		
		int originsPerSlice = (int) Math.floor(1.0 * origins.size() / sliceNumber); //the last slice may have a different number of origins
		//int originsInLastSlice = origins.size() - (sliceNumber - 1) * originsPerSlice;
		
		if (sliceIndex < sliceNumber) {
			for (int i = (sliceIndex - 1) * originsPerSlice; i < sliceIndex * originsPerSlice && i < origins.size(); i++) {
				String originLAD = origins.get(i);
				for (String destinationLAD: destinations) 
					if (matrix.getFlow(originLAD, destinationLAD) != 0)
						this.generateRouteSetZoneToZone(originLAD, destinationLAD, topNodes);
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				String originLAD = origins.get(i);
				for (String destinationLAD: destinations) 
					if (matrix.getFlow(originLAD, destinationLAD) != 0)
						this.generateRouteSetZoneToZone(originLAD, destinationLAD, topNodes);
			}
		}
	}
	
	/**
	 * Generates routes for all non-zero OD flows in the freight OD matrix.
	 * Zone ID ranges from the BYFM DfT model:
	 * @param freightMatrix Freight matrix.
	 * @param topNodes
	 */
	public void generateRouteSetForFreightMatrix(FreightMatrix freightMatrix, int topNodes) {
		
		List<Integer> origins = freightMatrix.getOrigins();
		List<Integer> destinations = freightMatrix.getDestinations();
		List<Integer> vehicles = freightMatrix.getVehicleTypes();
		
		for (Integer originFreightZone: origins)
			for (Integer destinationFreightZone: destinations)
				for (Integer vehicleType: vehicles) 
					if (freightMatrix.getFlow(originFreightZone, destinationFreightZone, vehicleType) != 0) {
						this.generateRouteSetBetweenFreightZones(originFreightZone, destinationFreightZone, topNodes);
						continue; //no need to repeat if there is flow for a different vehicle type!
					}
	}
	
	/*
	public void generateRouteSetForFreightMatrix2(FreightMatrix freightMatrix, int topNodes) {
		
		List<Integer> origins = freightMatrix.getOrigins();
		List<Integer> destinations = freightMatrix.getDestinations();
		List<Integer> vehicles = freightMatrix.getVehicleTypes();
		
		for (MultiKey mk: freightMatrix.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicleType = (int) mk.getKey(2);

			int flow = freightMatrix.getFlow(origin, destination, vehicleType);
			if (flow != 0) {
				generateRouteSetBetweenFreightZones(origin, destination, topNodes);
			}
		}
	}
	*/
	
	/**
	 * Generates routes for a slice of the OD matrix (useful for cluster computing), for topNodes only.
	 * There might still be some overlap between the slices as some nodes (to which point freight zones 
	 * are assigned appear again in LAD freight zones).
	 * @param matrix Origin-destination matrix.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 */
	public void generateRouteSetForFreightMatrix(FreightMatrix freightMatrix, int sliceIndex, int sliceNumber) {
		
		List<Integer> origins = freightMatrix.getOrigins();
		List<Integer> destinations = freightMatrix.getDestinations();
		List<Integer> vehicles = freightMatrix.getVehicleTypes();
		
		int originsPerSlice = (int) Math.floor(1.0 * origins.size() / sliceNumber); //the last slice may have a different number of origins
		//int originsInLastSlice = origins.size() - (sliceNumber - 1) * originsPerSlice;
		
		if (sliceIndex < sliceNumber) {
			for (int i = (sliceIndex - 1) * originsPerSlice; i < sliceIndex * originsPerSlice && i < origins.size(); i++) {
				Integer originFreightZone = origins.get(i);
				for (Integer destinationFreightZone: destinations)
					for (Integer vehicleType: vehicles) 
						if (freightMatrix.getFlow(originFreightZone, destinationFreightZone, vehicleType) != 0) {
							this.generateRouteSetBetweenFreightZones(originFreightZone, destinationFreightZone);
							continue; //no need to repeat if there is a flow for a different vehicle type!
						}
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				Integer originFreightZone = origins.get(i);
				for (Integer destinationFreightZone: destinations)
					for (Integer vehicleType: vehicles)
						if (freightMatrix.getFlow(originFreightZone, destinationFreightZone, vehicleType) != 0) {
							this.generateRouteSetBetweenFreightZones(originFreightZone, destinationFreightZone);
							continue; //no need to repeat if there is a flow for a different vehicle type!
						}
			}
		}
	}
	
	/**
	 * Generates routes for a slice of the OD matrix (useful for cluster computing), for topNodes only.
	 * There might still be some overlap between the slices as some nodes (to which point freight zones 
	 * are assigned appear again in LAD freight zones).
	 * @param matrix Origin-destination matrix.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 * @param topNodes Number of topNodes to consider for inter-zonal routes.
	 */
	public void generateRouteSetForFreightMatrix(FreightMatrix freightMatrix, int sliceIndex, int sliceNumber, int topNodes) {
		
		List<Integer> origins = freightMatrix.getOrigins();
		List<Integer> destinations = freightMatrix.getDestinations();
		List<Integer> vehicles = freightMatrix.getVehicleTypes();
		
		int originsPerSlice = (int) Math.floor(1.0 * origins.size() / sliceNumber); //the last slice may have a different number of origins
		//int originsInLastSlice = origins.size() - (sliceNumber - 1) * originsPerSlice;
		
		if (sliceIndex < sliceNumber) {
			for (int i = (sliceIndex - 1) * originsPerSlice; i < sliceIndex * originsPerSlice && i < origins.size(); i++) {
				Integer originFreightZone = origins.get(i);
				for (Integer destinationFreightZone: destinations)
					for (Integer vehicleType: vehicles) 
						if (freightMatrix.getFlow(originFreightZone, destinationFreightZone, vehicleType) != 0) {
							this.generateRouteSetBetweenFreightZones(originFreightZone, destinationFreightZone, topNodes);
							continue; //no need to repeat if there is a flow for a different vehicle type!
						}
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				Integer originFreightZone = origins.get(i);
				for (Integer destinationFreightZone: destinations)
					for (Integer vehicleType: vehicles)
						if (freightMatrix.getFlow(originFreightZone, destinationFreightZone, vehicleType) != 0) {
							this.generateRouteSetBetweenFreightZones(originFreightZone, destinationFreightZone, topNodes);
							continue; //no need to repeat if there is a flow for a different vehicle type!
						}
			}
		}
	}
	
	/**
	 * Generates routes between two freight zones.
	 * A freight zone can be either an LAD (<= 1032) or a point.
	 * Zone ID ranges from the BYFM DfT model:
	 * <ul>
	 * 		<li>England: 1 - 867</li>
	 * 		<li>Wales: 901 - 922</li>
	 * 		<li>Scotland: 1001 - 1032</li>
	 * 		<li>Freight airports: 1111 - 1115</li>
	 * 		<li>Major distribution centres: 1201 - 1256</li>
	 * 		<li>Freight ports: 1301 - 1388</li>
	 * </ul> 
	 * @param originFreightZone
	 * @param destinationFreightZone
	 */
	public void generateRouteSetBetweenFreightZones(int originFreightZone, int destinationFreightZone) {
		
		String originLAD = null, destinationLAD = null;
		Integer originNode = null, destinationNode = null;

		if (originFreightZone <= 1032) { 
			//origin freight zone is a LAD
			originLAD = roadNetwork.getFreightZoneToLAD().get(originFreightZone);
		} else {
			//freight zone is a point/node
			originNode = roadNetwork.getFreightZoneToNearestNode().get(originFreightZone);
		}
		//System.out.println("originLAD = " + originLAD + " originNode = " + originNode);
		
		if (destinationFreightZone <= 1032) { 
			//destination freight zone is a LAD
			destinationLAD = roadNetwork.getFreightZoneToLAD().get(destinationFreightZone);
		} else {
			//freight zone is a point/node
			destinationNode = roadNetwork.getFreightZoneToNearestNode().get(destinationFreightZone);
		}
		//System.out.println("destinationLAD = " + destinationLAD + " destinationNode = " + destinationNode);
		
		//generate routes depending if it is node-to-node, LAD-to-LAD, node-to-LAD or LAD-to-node
		if (originNode != null && destinationNode != null) 						this.generateRouteSetNodeToNode(originNode, destinationNode);
		else if (originLAD != null && destinationLAD != null) 					this.generateRouteSetZoneToZone(originLAD, destinationLAD);
		else if (originNode != null && destinationLAD != null) {
			//assume pre-sorted (sortGravityNodesFreight method must be used!)
			List<Integer> destinationNodes = 
					this.roadNetwork.getZoneToNodes().get(destinationLAD);
			for (int j= 0; j < destinationNodes.size(); j++)	this.generateRouteSetNodeToNode(originNode, destinationNodes.get(j));
		}
		else if (originLAD != null && destinationNode != null) {
			//assume pre-sorted (sortGravityNodesFreight method must be used!)
			List<Integer> originNodes = 
					this.roadNetwork.getZoneToNodes().get(originLAD);
			for (int i= 0; i < originNodes.size(); i++)			this.generateRouteSetNodeToNode(originNodes.get(i), destinationNode);
		} else System.err.println("Problem in generating route set for freight!");
	}
	
	/**
	 * Generates routes between two freight zones.
	 * A freight zone can be either an LAD (<= 1032) or a point.
	 * Zone ID ranges from the BYFM DfT model:
	 * <ul>
	 * 		<li>England: 1 - 867</li>
	 * 		<li>Wales: 901 - 922</li>
	 * 		<li>Scotland: 1001 - 1032</li>
	 * 		<li>Freight airports: 1111 - 1115</li>
	 * 		<li>Major distribution centres: 1201 - 1256</li>
	 * 		<li>Freight ports: 1301 - 1388</li>
	 * </ul> 
	 * @param originFreightZone
	 * @param destinationFreightZone
	 * @param topNodes Number of topNodes to consider for inter-zonal routes.
	 */
	public void generateRouteSetBetweenFreightZones(int originFreightZone, int destinationFreightZone, int topNodes) {
		
		String originLAD = null, destinationLAD = null;
		Integer originNode = null, destinationNode = null;

		if (originFreightZone <= 1032) { 
			//origin freight zone is a LAD
			originLAD = roadNetwork.getFreightZoneToLAD().get(originFreightZone);
		} else {
			//freight zone is a point/node
			originNode = roadNetwork.getFreightZoneToNearestNode().get(originFreightZone);
		}
		//System.out.println("originLAD = " + originLAD + " originNode = " + originNode);
		
		if (destinationFreightZone <= 1032) { 
			//destination freight zone is a LAD
			destinationLAD = roadNetwork.getFreightZoneToLAD().get(destinationFreightZone);
		} else {
			//freight zone is a point/node
			destinationNode = roadNetwork.getFreightZoneToNearestNode().get(destinationFreightZone);
		}
		//System.out.println("destinationLAD = " + destinationLAD + " destinationNode = " + destinationNode);
		
		//generate routes depending if it is node-to-node, LAD-to-LAD, node-to-LAD or LAD-to-node
		if (originNode != null && destinationNode != null) 						this.generateRouteSetNodeToNode(originNode, destinationNode);
		else if (originLAD != null && destinationLAD != null) 					this.generateRouteSetZoneToZone(originLAD, destinationLAD, topNodes);
		else if (originNode != null && destinationLAD != null) {
			//assume pre-sorted (sortGravityNodesFreight method must be used!)
			List<Integer> destinationNodes = 
					this.roadNetwork.getZoneToNodes().get(destinationLAD);
			for (int j= 0; j < topNodes && j < destinationNodes.size(); j++)	this.generateRouteSetNodeToNode(originNode, destinationNodes.get(j));
		}
		else if (originLAD != null && destinationNode != null) {
			//assume pre-sorted (sortGravityNodesFreight method must be used!)
			List<Integer> originNodes = 
					this.roadNetwork.getZoneToNodes().get(originLAD);
			for (int i= 0; i < topNodes && i < originNodes.size(); i++)			this.generateRouteSetNodeToNode(originNodes.get(i), destinationNode);
		} else System.err.println("Problem in generating route set for freight!");
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
	 * Calculates utilities for all the routes in all the route sets.
	 * @param linkTravelTime
	 * @param params
	 */
	public void calculateAllUtilities(HashMap<Integer, Double> linkTravelTime, Properties params) {

		for (Object mk: routes.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			int destination = (int) ((MultiKey)mk).getKey(1);
		
			RouteSet rs = (RouteSet)routes.get(origin, destination);
			rs.calculateUtilities(linkTravelTime, params);
			rs.sortRoutesOnUtility(); //will update probabilities as well
		}
	}
	
	/**
	 * Saves all route sets into a text file.
	 * @param fileName File name.
	 * @param append Whether to append to an existing file.
	 */
	public void saveRoutes(String fileName, boolean append) {
		
		FileWriter fileWriter = null;
		BufferedWriter bufferedWriter = null;
		try {
			fileWriter = new FileWriter(fileName, append);
			bufferedWriter = new BufferedWriter(fileWriter);
			//iterate over all route sets
			for (Object value: routes.values()) {
				RouteSet rs = (RouteSet)value;
				//iterate over all routes
				for (Route route: rs.getChoiceSet()) 
					bufferedWriter.write(route.getFormattedString() + System.getProperty("line.separator"));
			}
		} catch (Exception e) {
			System.err.println("Error in fileWriter!");
			e.printStackTrace();
		} finally {
			try {
				bufferedWriter.flush();
				bufferedWriter.close();
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing fileWriter!");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Saves all route sets into a binary file.
	 * @param fileName File name.
	 * @param append Whether to append to an existing file.
	 */
	public void saveRoutesBinary(String fileName, boolean append) {
		
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedStream = null;
        DataOutputStream dataStream = null;
		try {
			outputStream = new FileOutputStream(fileName, append);
			bufferedStream = new BufferedOutputStream(outputStream);
			dataStream = new DataOutputStream(bufferedStream);
			//iterate over all route sets
			for (Object value: routes.values()) {
				RouteSet rs = (RouteSet)value;
				//iterate over all routes and save only edges (start/end nodes are redundant information)
				for (Route route: rs.getChoiceSet()) {
					for (DirectedEdge edge: route.getEdges())
						dataStream.writeInt(edge.getID());
					dataStream.writeInt(0);
				}
			}
		} catch (Exception e) {
			System.err.println("Error in outputStream!");
			e.printStackTrace();
		} finally {
			try {
				dataStream.flush();
				dataStream.close();
				bufferedStream.flush();
				bufferedStream.close();
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing outputStream!");
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
		    	//int originID = Integer.parseInt(splitLine[0]); //nodes not needed
		    	//int destinationID = Integer.parseInt(splitLine[1]); //nodes not needed
		    	if (splitLine.length > 2) { //only if there are edges
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
		    	}
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
	
	/**
	 * Reads route sets from a text file.
	 * @param fileName File name.
	 */
	public void readRoutesBinary(String fileName) {
		
		System.out.println("Reading pre-generated routes...");
		FileInputStream input = null;
		BufferedInputStream buff = null;
		DataInputStream data = null;
		int counterBadRoutes = 0;
		try {
			input = new FileInputStream(fileName);
			buff = new BufferedInputStream(input);
			data = new DataInputStream(buff);
			
			Route route = new Route();
			boolean success = true;
			while (true) { 
				int edgeID = data.readInt();
				if (edgeID != 0) { //keep adding edge to the route
					success = success && route.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge().get(edgeID));
				} else {
					//add route to the route set if all edge additions have been successful
					if (success) this.addRoute(route);
					else counterBadRoutes++;
					//create new route if there are more bytes
					//if (data.available() > 0) route = new Route();
					route = new Route();
					success = true;
				}
			}
		} catch (EOFException e) {
			System.out.print("End of the binary route file reached. ");
			System.out.println(counterBadRoutes + " bad routes ignored.");
		} catch (Exception e) {
			System.err.println("Error in fileReader!");
			e.printStackTrace();
		} finally {
			try {
				input.close();
				buff.close();
				data.close();
			} catch (IOException e) {
				System.err.println("Error while closing input stream!");
				e.printStackTrace();
			}
		}
	}
}
