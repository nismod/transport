package nismod.transport.network.road;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.build.line.BasicDirectedLineGraphBuilder;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Node;
import org.locationtech.jts.geom.Point;

import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrixMultiKey;
import nismod.transport.demand.RealODMatrixTempro;
import nismod.transport.utility.RandomSingleton;
import nismod.transport.zone.Zoning;

/**
 * RouteSetGenerator can generate, save and read route sets for the route choice.
 * @author Milan Lovric
 */
public class RouteSetGenerator{
	
	private final static Logger LOGGER = LogManager.getLogger(RouteSetGenerator.class);
	
	//initial route size
	public static final int INITIAL_ROUTE_CAPACITY = 13;
	//initial route set size
	public static final int INITIAL_ROUTE_SET_CAPACITY = 7;
	
	//storage for route sets between node pairs
	private RouteSet[][] routes;
	
	private RoadNetwork roadNetwork;
	private Properties props;
	
	/**
	 * Constructor for the route set generator.
	 * @param roadNetwork Road network.
	 * @param props Parameters from the config file.
	 */
	public RouteSetGenerator(RoadNetwork roadNetwork, Properties props) {
		
		this.roadNetwork = roadNetwork;
		this.props = props;
		
		int maxNodes = this.roadNetwork.getNodeIDtoNode().length;
		routes = new RouteSet[maxNodes][maxNodes]; //access will be directly with node ID (without -1).
	}
	
	/**
	 * Adds a route to the route set.
	 * @param route Route to be added.
	 */
	public void addRoute(Route route) {
		
		//NOTE this is commented, as single node routes are empty but still valid!
		/*
		if (route.isEmpty()) {
			System.err.println("Cannot add empty route!");
			return;
		}
		*/
		if (!route.isValid()) {
			LOGGER.debug("Route {} is not valid. Not adding the route!", route.toString());
			return;
		}
		int origin = route.getOriginNode().getID();
		int destination = route.getDestinationNode().getID();
		
		RouteSet set = this.routes[origin][destination];
		if (set == null) {
			set = new RouteSet(roadNetwork);
			this.routes[origin][destination] = set;
		}
		set.addRoute(route);
	}
	
	/**
	 * Adds a route to the route set.
	 * @param route Route to be added.
	 */
	public void addRouteWithoutValidityCheck(Route route) {
		
		//NOTE this is commented, as single node routes are empty but still valid!
		if (route.isEmpty()) {
//			System.err.println("Cannot add empty route!");
			return;
		}

		int origin = route.getOriginNode().getID();
		int destination = route.getDestinationNode().getID();
		
		RouteSet set = this.routes[origin][destination];
		if (set == null) {
			set = new RouteSet(roadNetwork);
			this.routes[origin][destination] = set;
		}
		set.addRouteWithoutValidityCheck(route);
		//set.addRouteWithoutValidityAndEndNodesCheck(route);
		//set.addRouteWithoutAnyChecks(route);
	}
	
	/**
	 * Removes all the routes that contain a given edge (used for disruption).
	 * @param edgeID Edge ID.
	 */
	public void removeRoutesWithEdge(int edgeID) {
		
		for (int i = 1; i < this.routes.length; i++)
			for (int j = 1; j < this.routes[i].length; j++)
				if (this.routes[i][j] != null) {
					RouteSet rs = this.routes[i][j];
					//iterate over all routes using iterator (to allow concurrent modification)
					Iterator<Route> iter = rs.getChoiceSet().iterator();
					while (iter.hasNext()) {
						Route route = iter.next();
						if (route.contains(edgeID)) iter.remove();
					}
				//rs.getChoiceSet().removeIf(route -> route.contains(edgeID)); //or alternatively, with Java 8
				//recalculate path sizes!
				rs.calculatePathsizes();
			}
	}
	
	/**
	 * Removes all the routes that contain a given edge and store in the list.
	 * @param edgeID Edge ID.
	 * @param removedRoutes List of removed routes.
	 */
	public void removeRoutesWithEdge(int edgeID, List<Route> removedRoutes) {
		
		for (int i = 1; i < this.routes.length; i++)
			for (int j = 1; j < this.routes[i].length; j++)
				if (this.routes[i][j] != null) {
					RouteSet rs = this.routes[i][j];
					//iterate over all routes using iterator (to allow concurrent modification)
					Iterator<Route> iter = rs.getChoiceSet().iterator();
					while (iter.hasNext()) {
						Route route = iter.next();
						if (route.contains(edgeID)) {
							removedRoutes.add(route);
							iter.remove();
						}
					}
				//recalculate path sizes!
				rs.calculatePathsizes();
			}
	}
	
	/**
	 * Generates a route set between two nodes (if it does not already exist in the route set).
	 * @param origin Origin node ID.
	 * @param destination Destination node ID.
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
	 * @param origin Origin node ID.
	 * @param destination Destination node ID.
	 */
	public void generateRouteSetWithLinkElimination(int origin, int destination) {
		
		DirectedNode originNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[origin];
		DirectedNode destinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destination];
		//RouteSet rs = new RouteSet(originNode, destinationNode);
		
		//find the fastest path from origin to destination
		RoadPath fastestPath  = this.roadNetwork.getFastestPath(originNode, destinationNode, null);
		if (fastestPath == null) {
			LOGGER.warn("Unable to find the fastest path between nodes {} and {}! Link elimination method unsucsessful!", origin, destination);
			return;
		}
		//System.out.println(fastestPath.toString());
		//System.out.println("Path validity: " + fastestPath.isValid());
		
		Route fastestRoute = new Route(fastestPath, roadNetwork);
		//System.out.println("Route validity: " + fastestRoute.isValid());
		//rs.addRoute(fastestRoute);
		//rs.printChoiceSet();
		
		//check that origin and destination node are correct!
		if (fastestRoute.getOriginNode().equals(originNode) && fastestRoute.getDestinationNode().equals(destinationNode))
			this.addRoute(fastestRoute);
		else {
			LOGGER.warn("Fastest path does not contain correct origin and destination nodes!");
			return;
		}
		
		//link elimination method
		for (Object o: fastestPath.getEdges()) {
			double[] linkTravelTimes = new double[this.roadNetwork.getFreeFlowTravelTime().length];
			DirectedEdge edge = (DirectedEdge) o;
			linkTravelTimes[edge.getID()] = Double.MAX_VALUE; //blocks by setting a maximum travel time
			RoadPath path = this.roadNetwork.getFastestPath(originNode, destinationNode, linkTravelTimes);
			if (path != null) {
				//System.out.println(path.toString());
				//System.out.println("Path validity: " + path.isValid());
				Route route = new Route(path, roadNetwork);
				//System.out.println(route);
				//rs.addRoute(route);
				if (route.getOriginNode().equals(originNode) && route.getDestinationNode().equals(destinationNode))
					this.addRoute(route);
				else 
					LOGGER.warn("Path generated with link elimination does not contain correct origin and destination nodes!");
			}
		}
	}
	
	/**
	 * Generates a route set between two nodes using the random link elimination method -
	 * It first finds the fastest path and then blocks random links within the fastest path and tries to find an alternative path.
	 * The search is limited by the total number of path finding calls and the required number of generated paths.
	 * @param origin Origin node ID.
	 * @param destination Destination node ID.
	 */
	public void generateRouteSetWithRandomLinkEliminationRestricted(int origin, int destination) {

		if (props == null) {
			LOGGER.error("Route set generator does not have required parameters!");
			return;
		} else {
		
			final Integer routeLimit = Integer.parseInt(props.getProperty("ROUTE_LIMIT"));
			final Integer generationLimit = Integer.parseInt(props.getProperty("GENERATION_LIMIT"));
			this.generateRouteSetWithRandomLinkEliminationRestricted(origin, destination, routeLimit, generationLimit);
		}
	}

	/**
	 * Generates a route set between two nodes using the random link elimination method -
	 * It first finds the fastest path and then blocks random links within the fastest path and tries to find an alternative path.
	 * The search is limited by the total number of path finding calls and the required number of generated paths.
	 * @param origin Origin node ID.
	 * @param destination Destination node ID.
	 * @param routeLimit Maximum allowed number of generated routes.
	 * @param generationLimit Number of generation trials to get a potentially new route.
	 */
	public void generateRouteSetWithRandomLinkEliminationRestricted(int origin, int destination, int routeLimit, int generationLimit) {

		RandomSingleton rng = RandomSingleton.getInstance();
		
		//do not generate route set if origin or destination node is blacklisted as no routes are possible
		if (this.roadNetwork.isBlacklistedAsStartNode(origin) || this.roadNetwork.isBlacklistedAsEndNode(destination)) return;
		
		//System.out.printf("Generating route set between nodes %d and %d with random link elimination.\n", origin, destination);

		//if they are the same, create a single node path
		if (origin == destination) {
			RoadPath rp = new RoadPath();
			rp.add(roadNetwork.getNodeIDtoNode()[origin]);
			Route route = new Route(rp, roadNetwork);
			this.addRoute(route);
			return;
		}
		
		DirectedNode originNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[origin];
		DirectedNode destinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destination];
		//RouteSet rs = new RouteSet(originNode, destinationNode);

		//find the fastest path from origin to destination (astar or dijkstra)
		RoadPath fastestPath  = this.roadNetwork.getFastestPath(originNode, destinationNode, null);
		//RoadPath fastestPath  = this.roadNetwork.getFastestPathDijkstra(originNode, destinationNode, null);

		if (fastestPath == null) {
			LOGGER.warn("Unable to find the fastest path between nodes {} and {}! Link elimination method unsucsessful!", origin, destination);
			return;
		}
		//System.out.println("RoadPath " + fastestPath.toString());
		//System.out.println("Path validity: " + fastestPath.isValid());

		Route fastestRoute = new Route(fastestPath, roadNetwork);
		//System.out.println("Route: " + fastestRoute.getFormattedString());
		//System.out.println("Route validity: " + fastestRoute.isValid());
		//System.out.println("Route origin node: " + fastestRoute.getOriginNode().getID());
		//System.out.println("Route destination node: " + fastestRoute.getDestinationNode().getID());
		//check that origin and destination node are correct!
		if (fastestRoute.getOriginNode().equals(originNode) && fastestRoute.getDestinationNode().equals(destinationNode))
			this.addRoute(fastestRoute);
		else {
			LOGGER.warn("Fastest route between nodes {} and {} does not contain correct origin and destination nodes! Link elimination method unsucsessful!", origin, destination);
			return;
		}
		
		if (routeLimit == 1) return; //only fastest path had to be added

		int pathSizeInLinks = fastestPath.size() - 1; //number of edges = number of nodes - 1

		//if the number of links in the fastest path is smaller than the ROUTE_LIMIT,
		//then block each link successively (this will generate up to ROUTE_LIMIT routes using less calculations than the random method)
		if (pathSizeInLinks < routeLimit) {

			for (Object o: fastestPath.getEdges()) {

				double[] linkTravelTimes = new double[this.roadNetwork.getFreeFlowTravelTime().length];
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
					Route route = new Route(path, roadNetwork);
					//System.out.println("Route: " + route.getFormattedString());
					//rs.addRoute(route);
					if (route.getOriginNode().equals(originNode) && route.getDestinationNode().equals(destinationNode))
						this.addRoute(route);
					else 
						LOGGER.warn("Route generated with link elimination does not contain correct origin and destination nodes! Skipping this route.");
				}
			}
		} else { //otherwise use the actual random link elimination method 

			for (int i = 0; i < generationLimit; i++) {

				int randomIndex = rng.nextInt(pathSizeInLinks);
				Object o = fastestPath.getEdges().get(randomIndex); //pick random edge
				double[] linkTravelTimes = new double[this.roadNetwork.getFreeFlowTravelTime().length];
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
					Route route = new Route(path, roadNetwork);
					//System.out.println("Route: " + route.getFormattedString());
					//rs.addRoute(route);
					if (route.getOriginNode().equals(originNode) && route.getDestinationNode().equals(destinationNode))
						this.addRoute(route);
					else 
						LOGGER.warn("Route generated with link elimination does not contain correct origin and destination nodes! Skipping this route.");
				}
				RouteSet rs = this.getRouteSet(origin, destination);
				
				//if (rs == null) ; //System.err.println("Empty routeset!");
				//if (rs!= null && rs.getSize() >= routeLimit) break; //stop if sufficient number of routes has been generated 
				if (rs.getSize() >= routeLimit) break; //stop if sufficient number of routes has been generated
			}
		}
	}
	
	/**
	 * Generates routes between all combinations of nodes from two LAD zones
	 * @param originLAD Origin LAD.
	 * @param destinationLAD Destination LAD.
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
	 * Generates routes between the nearest nodes of two Tempro zones.
	 * @param originZone Origin Tempro zone.
	 * @param destinationZone Destination Tempro zone.
	 * @param zoning Tempro zoning system.
	 */
	public void generateRouteSetZoneToZoneTempro(String originZone, String destinationZone, Zoning zoning) {
		
//		List<Integer> originNodes = this.roadNetwork.getZoneToNodes().get(originLAD);
//		List<Integer> destinationNodes = this.roadNetwork.getZoneToNodes().get(destinationLAD);
//		//System.out.printf("Generating routes from %s (%d nodes) to %s (%d nodes) %n", originLAD, originNodes.size(), destinationLAD, destinationNodes.size());
//		for (Integer origin: originNodes)
//			for (Integer destination: destinationNodes)
//				if (origin != destination) 					
//					this.generateRouteSetNodeToNode(origin, destination);
		Integer originNode = zoning.getZoneToNearestNodeIDMap().get(originZone);
		Integer destinationNode = zoning.getZoneToNearestNodeIDMap().get(destinationZone);
		this.generateRouteSetNodeToNode(originNode, destinationNode);
	}
	
	/**
	 * Generates routes between the nearest nodes of two Tempro zones.
	 * @param originZone Origin Tempro zone.
	 * @param destinationZone Destination Tempro zone.
	 * @param zoning Tempro zoning system.
	 */
	public void generateRouteSetZoneToZoneTemproDistanceBased(String originZone, String destinationZone, Zoning zoning) {
		
		if (props == null) {
			LOGGER.error("Route set generator does not have required parameters!");
			return;
		}
		
		final int generationLimit = Integer.parseInt(props.getProperty("GENERATION_LIMIT"));
		
//		List<Integer> originNodes = this.roadNetwork.getZoneToNodes().get(originLAD);
//		List<Integer> destinationNodes = this.roadNetwork.getZoneToNodes().get(destinationLAD);
//		//System.out.printf("Generating routes from %s (%d nodes) to %s (%d nodes) %n", originLAD, originNodes.size(), destinationLAD, destinationNodes.size());
//		for (Integer origin: originNodes)
//			for (Integer destination: destinationNodes)
//				if (origin != destination) 					
//					this.generateRouteSetNodeToNode(origin, destination);
		int originNode = zoning.getZoneToNearestNodeIDMap().get(originZone);
		int destinationNode = zoning.getZoneToNearestNodeIDMap().get(destinationZone);
		
		RouteSet fetchedRouteSet = this.getRouteSet(originNode, destinationNode);
		//generate only if it does not already exist
		if (fetchedRouteSet == null) {
			Point originCentroid = zoning.getZoneToCentroid().get(originZone);
			Point destinationCentroid = zoning.getZoneToCentroid().get(destinationZone);
			
			double centroidDistance = originCentroid.distance(destinationCentroid);
			int routeLimit = 0;
			if (centroidDistance <= 500000 && centroidDistance > 400000) routeLimit = 1;
			else if (centroidDistance <= 400000 && centroidDistance > 300000) routeLimit = 2;
			else if (centroidDistance <= 300000 && centroidDistance > 200000) routeLimit = 3;
			else if (centroidDistance <= 200000 && centroidDistance > 100000) routeLimit = 4;
			else if (centroidDistance <= 100000) routeLimit = 5;
			
			LOGGER.trace("Route limit for centroidDistance = {} km is: {}", centroidDistance/1000, routeLimit);
			generateRouteSetWithRandomLinkEliminationRestricted(originNode, destinationNode, routeLimit, generationLimit);
		}
	}
		
	/**
	 * Generates routes between top N nodes (sorted by gravitating population) from two LAD zones.
	 * If origin and destination LAD are the same (i.e., intra-zonal), then use all the nodes
	 * @param originLAD Origin LAD.
	 * @param destinationLAD Destination LAD.
	 * @param topNodes Number of top nodes within LAD to consider.
	 */
	public void generateRouteSetZoneToZone(String originLAD, String destinationLAD, int topNodes) {
		
		//if intra-zonal, use all node combinations:
		if (originLAD.equals(destinationLAD)) this.generateRouteSetZoneToZone(originLAD, destinationLAD);
		else { //if inter-zonal, use only top nodes
		
		//assume pre-sorted (sortGravityNodes method must be used!)
		//List<Integer> originNodes = this.roadNetwork.getZoneToNodes().get(originLAD);
		//List<Integer> destinationNodes = this.roadNetwork.getZoneToNodes().get(destinationLAD);
			
		//copy list and remove blacklisted nodes
		List<Integer> listOfOriginNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(originLAD)); //the list is already sorted
		//removing blacklisted nodes
		for (Integer node: roadNetwork.getZoneToNodes().get(originLAD))
			//check if any of the nodes is blacklisted
			if (this.roadNetwork.isBlacklistedAsStartNode(node)) 
				listOfOriginNodes.remove(node);
		List<Integer> listOfDestinationNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(destinationLAD)); //the list is already sorted
		//removing blacklisted nodes
		for (Integer node: roadNetwork.getZoneToNodes().get(destinationLAD))
			//check if any of the nodes is blacklisted
			if (this.roadNetwork.isBlacklistedAsEndNode(node))
				listOfDestinationNodes.remove(node);
					
		//System.out.printf("Generating routes between top %d nodes from %s to %s %n", topNodes, originLAD, destinationLAD);
		for (int i = 0; i < topNodes && i < listOfOriginNodes.size(); i++)
			for (int j = 0; j < topNodes && j < listOfDestinationNodes.size(); j++)	{
				
				int origin = listOfOriginNodes.get(i);
				int destination = listOfDestinationNodes.get(j);
				//System.out.printf("Generating routes between nodes %d and %d \n", origin, destination);
				this.generateRouteSetNodeToNode(origin, destination);
			}
		}
	}
	
	/**
	 * Generates routes for all non-zero OD flows in the OD matrix.
	 * For inter-zonal flows generates routes only between top N nodes.
	 * @param matrix Origin-destination matrix.
	 * @param topNodes Number of topNodes to consider for inter-zonal routes.
	 */
	public void generateRouteSetForODMatrix(ODMatrixMultiKey matrix, int topNodes) {
		
		for (MultiKey mk: matrix.getKeySet()) {
				String originLAD = (String) ((MultiKey)mk).getKey(0);
				String destinationLAD = (String) ((MultiKey)mk).getKey(1);
				if (matrix.getFlow(originLAD, destinationLAD) != 0)
					this.generateRouteSetZoneToZone(originLAD, destinationLAD, topNodes);
		}
	}
	
	/**
	 * Generates routes for all non-zero OD flows in the OD matrix.
	 * @param matrix Origin-destination matrix.
	 */
	public void generateRouteSetForODMatrix(ODMatrixMultiKey matrix) {
		
		for (MultiKey mk: matrix.getKeySet()) {
				String originLAD = (String) ((MultiKey)mk).getKey(0);
				String destinationLAD = (String) ((MultiKey)mk).getKey(1);
				if (matrix.getFlow(originLAD, destinationLAD) != 0)
					this.generateRouteSetZoneToZone(originLAD, destinationLAD);
		}
	}
	
	/**
	 * Generates routes for all non-zero OD flows in the OD matrix.
	 * @param matrix Origin-destination matrix.
	 * @param zoning Tempro zoning system.
	 */
	public void generateRouteSetForODMatrixTempro(ODMatrixMultiKey matrix, Zoning zoning) {
		
		for (MultiKey mk: matrix.getKeySet()) {
				String originZone = (String) ((MultiKey)mk).getKey(0);
				String destinationZone = (String) ((MultiKey)mk).getKey(1);
				if (matrix.getFlow(originZone, destinationZone) != 0)
					this.generateRouteSetZoneToZoneTempro(originZone, destinationZone, zoning);
		}
	}
	
	
	/**
	 * Generates routes for a slice of the OD matrix (useful for cluster computing).
	 * @param matrix Origin-destination matrix.
	 * @param zoning Tempro zoning system.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 */
	public void generateRouteSetForODMatrixTempro(RealODMatrixTempro matrix, Zoning zoning, int sliceIndex, int sliceNumber) {
		
		List<String> origins = matrix.getSortedOrigins();
		List<String> destinations = matrix.getSortedDestinations();
		
		int originsPerSlice = (int) Math.floor(1.0 * origins.size() / sliceNumber); //the last slice may have a different number of origins
		//int originsInLastSlice = origins.size() - (sliceNumber - 1) * originsPerSlice;
		
		if (sliceIndex < sliceNumber) {
			for (int i = (sliceIndex - 1) * originsPerSlice; i < sliceIndex * originsPerSlice && i < origins.size(); i++) {
				String originZone = origins.get(i);
				for (String destinationZone: destinations) 
					if (matrix.getIntFlow(originZone, destinationZone) != 0)
						this.generateRouteSetZoneToZoneTempro(originZone, destinationZone, zoning);
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				String originZone = origins.get(i);
				for (String destinationZone: destinations) 
					if (matrix.getIntFlow(originZone, destinationZone) != 0)
						this.generateRouteSetZoneToZoneTempro(originZone, destinationZone, zoning);
			}
		}
	}
	
	/**
	 * Generates routes for a slice of the OD matrix (useful for cluster computing).
	 * The number of routes increases the smaller the distance between two Tempro zones.
	 * @param matrix Origin-destination matrix.
	 * @param zoning Tempro zoning system.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 */
	public void generateRouteSetForODMatrixTemproDistanceBased(RealODMatrixTempro matrix, Zoning zoning, int sliceIndex, int sliceNumber) {
		
		List<String> origins = matrix.getSortedOrigins();
		List<String> destinations = matrix.getSortedDestinations();
		
		int originsPerSlice = (int) Math.floor(1.0 * origins.size() / sliceNumber); //the last slice may have a different number of origins
		//int originsInLastSlice = origins.size() - (sliceNumber - 1) * originsPerSlice;
		
		if (sliceIndex < sliceNumber) {
			for (int i = (sliceIndex - 1) * originsPerSlice; i < sliceIndex * originsPerSlice && i < origins.size(); i++) {
				String originZone = origins.get(i);
				for (String destinationZone: destinations) 
					if (matrix.getIntFlow(originZone, destinationZone) != 0)
						this.generateRouteSetZoneToZoneTemproDistanceBased(originZone, destinationZone, zoning);
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				String originZone = origins.get(i);
				for (String destinationZone: destinations) 
					if (matrix.getIntFlow(originZone, destinationZone) != 0)
						this.generateRouteSetZoneToZoneTemproDistanceBased(originZone, destinationZone, zoning);
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
	public void generateRouteSetForODMatrix(ODMatrixMultiKey matrix, int sliceIndex, int sliceNumber, int topNodes) {
		
		List<String> origins = matrix.getSortedOrigins();
		List<String> destinations = matrix.getSortedDestinations();
		
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
	 * Generates routes for a slice of the Tempro OD matrix (useful for cluster computing).
	 * @param matrix Origin-destination matrix.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 */
	public void generateRouteSetForODMatrix(ODMatrixMultiKey matrix, int sliceIndex, int sliceNumber) {
		
		List<String> origins = matrix.getSortedOrigins();
		List<String> destinations = matrix.getSortedDestinations();
		
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
	 * Generates routes for all non-zero OD flows in the freight OD matrix.
	 * Zone ID ranges from the BYFM DfT model:
	 * @param freightMatrix Freight matrix.
	 * @param topNodes Number of topNodes to consider for inter-zonal routes.
	 */
	public void generateRouteSetForFreightMatrix(FreightMatrix freightMatrix, int topNodes) {
		
		List<Integer> origins = freightMatrix.getUnsortedOrigins();
		List<Integer> destinations = freightMatrix.getUnsortedDestinations();
		List<Integer> vehicles = freightMatrix.getVehicleTypes();
		
		for (Integer originFreightZone: origins)
			for (Integer destinationFreightZone: destinations)
				for (Integer vehicleType: vehicles) 
					if (freightMatrix.getFlow(originFreightZone, destinationFreightZone, vehicleType) != 0) {
						this.generateRouteSetBetweenFreightZones(originFreightZone, destinationFreightZone, topNodes);
						break; //no need to repeat if there is flow for a different vehicle type!
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
	 * @param freightMatrix Freight matrix.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 */
	public void generateRouteSetForFreightMatrix(FreightMatrix freightMatrix, int sliceIndex, int sliceNumber) {
		
		List<Integer> origins = freightMatrix.getSortedOrigins();
		List<Integer> destinations = freightMatrix.getUnsortedDestinations();
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
							break; //no need to repeat if there is a flow for a different vehicle type!
						}
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				Integer originFreightZone = origins.get(i);
				for (Integer destinationFreightZone: destinations)
					for (Integer vehicleType: vehicles)
						if (freightMatrix.getFlow(originFreightZone, destinationFreightZone, vehicleType) != 0) {
							this.generateRouteSetBetweenFreightZones(originFreightZone, destinationFreightZone);
							break; //no need to repeat if there is a flow for a different vehicle type!
						}
			}
		}
	}
	
	/**
	 * Generates routes for a slice of the OD matrix (useful for cluster computing), for topNodes only.
	 * There might still be some overlap between the slices as some nodes (to which point freight zones 
	 * are assigned appear again in LAD freight zones).
	 * @param freightMatrix Freight matrix.
	 * @param sliceIndex Index of the OD matrix slice for which to generate routes [1..N].
	 * @param sliceNumber Number of slices to divide matrix into (N).
	 * @param topNodes Number of topNodes to consider for inter-zonal routes.
	 */
	public void generateRouteSetForFreightMatrix(FreightMatrix freightMatrix, int sliceIndex, int sliceNumber, int topNodes) {
		
		List<Integer> origins = freightMatrix.getSortedOrigins();
		List<Integer> destinations = freightMatrix.getUnsortedDestinations();
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
							break; //no need to repeat if there is a flow for a different vehicle type!
						}
			}
		} else { //for the last slice there may be more origins, so go all the way to the end of the list
			for (int i = (sliceIndex - 1) * originsPerSlice; i < origins.size(); i++) {
				Integer originFreightZone = origins.get(i);
				for (Integer destinationFreightZone: destinations)
					for (Integer vehicleType: vehicles)
						if (freightMatrix.getFlow(originFreightZone, destinationFreightZone, vehicleType) != 0) {
							this.generateRouteSetBetweenFreightZones(originFreightZone, destinationFreightZone, topNodes);
							break; //no need to repeat if there is a flow for a different vehicle type!
						}
			}
		}
	}
	
	/**
	 * Generates routes between two freight zones.
	 * A freight zone can be either an LAD (&lt;= 1032) or a point.
	 * Zone ID ranges from the BYFM DfT model:
	 * <ul>
	 * 		<li>England: 1 - 867
	 * 		<li>Wales: 901 - 922
	 * 		<li>Scotland: 1001 - 1032
	 * 		<li>Freight airports: 1111 - 1115
	 * 		<li>Major distribution centres: 1201 - 1256
	 * 		<li>Freight ports: 1301 - 1388
	 * </ul> 
	 * @param originFreightZone Origin freight zone.
	 * @param destinationFreightZone Destination freight zone.
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
		} else 
			LOGGER.warn("Problem in generating route set for freight for originZone {} and destinationZone {}!", originFreightZone, destinationFreightZone);
	}
	
	/**
	 * Generates routes between two freight zones.
	 * A freight zone can be either an LAD (&lt;= 1032) or a point.
	 * Zone ID ranges from the BYFM DfT model:
	 * <ul>
	 * 		<li>England: 1 - 867</li>
	 * 		<li>Wales: 901 - 922</li>
	 * 		<li>Scotland: 1001 - 1032</li>
	 * 		<li>Freight airports: 1111 - 1115</li>
	 * 		<li>Major distribution centres: 1201 - 1256</li>
	 * 		<li>Freight ports: 1301 - 1388</li>
	 * </ul> 
	 * @param originFreightZone Origin freight zone.
	 * @param destinationFreightZone Destination freight zone.
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
		if (originNode != null && destinationNode != null) {
			this.generateRouteSetNodeToNode(originNode, destinationNode);
		
		} else if (originLAD != null && destinationLAD != null) {
			this.generateRouteSetZoneToZone(originLAD, destinationLAD, topNodes);
		
		} else if (originNode != null && destinationLAD != null) {
			//assume pre-sorted (sortGravityNodesFreight method must be used!)
			List<Integer> listOfDestinationNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(destinationLAD)); //the list is already sorted
			//removing blacklisted nodes
			for (Integer node: roadNetwork.getZoneToNodes().get(destinationLAD))
				//check if any of the nodes is blacklisted
				if (this.roadNetwork.isBlacklistedAsEndNode(node))
					listOfDestinationNodes.remove(node);
			for (int j = 0; j < topNodes && j < listOfDestinationNodes.size(); j++)
				this.generateRouteSetNodeToNode(originNode, listOfDestinationNodes.get(j));
		
		} else if (originLAD != null && destinationNode != null) {
			//assume pre-sorted (sortGravityNodesFreight method must be used!)
			List<Integer> listOfOriginNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(originLAD)); //the list is already sorted
			//removing blacklisted nodes
			for (Integer node: roadNetwork.getZoneToNodes().get(originLAD))
				//check if any of the nodes is blacklisted
				if (this.roadNetwork.isBlacklistedAsStartNode(node)) 
					listOfOriginNodes.remove(node);
			for (int i= 0; i < topNodes && i < listOfOriginNodes.size(); i++)
				this.generateRouteSetNodeToNode(listOfOriginNodes.get(i), destinationNode);
			
		} else 
			LOGGER.warn("Problem in generating route set for freight for originZone {} and destinationZone {}!", originFreightZone, destinationFreightZone);
	}
	
	/**
	 * Generates single nodes routes.
	 */
	public void generateSingleNodeRoutes() {
		
		LOGGER.info("Generating single node routes for the whole network...");
		
		for (Node n: roadNetwork.getNodeIDtoNode()) 
			if (n != null) {
				RoadPath rp = new RoadPath();
				rp.add(n);
				Route r = new Route(rp, roadNetwork);
				//this.addRoute(r);
				this.addRouteWithoutValidityCheck(r);
		}
	}
	
	/**
	 * Getter method for a route set between a specific origin and a destination.
	 * @param origin Origin node ID.
	 * @param destination Destination node ID.
	 * @return Route set.
	 */
	public RouteSet getRouteSet(int origin, int destination) {
		
		return this.routes[origin][destination];
	}
	
	/**
	 * Clears all stored routes.
	 */
	public void clearRoutes() {
		
		for(int i = 0; i < routes.length; i++)
			Arrays.fill(routes[i], null);
	}
	
	/**
	 * Prints all route sets.
	 */
	public void printChoiceSets() {
		
		for (int i = 1; i < this.routes.length; i++)
			for (int j = 1; j < this.routes[i].length; j++) {
				if (this.routes[i][j] != null) {
					this.routes[i][j].printChoiceSet();
				}
			}
	}
	
	/**
	 * Prints all route set statistics.
	 */
	public void printStatistics() {

		LOGGER.info("Number of OD pairs / route sets: {}", this.getNumberOfRouteSets());
		LOGGER.info("Total number of routes: {}", this.getNumberOfRoutes());
	}
	
	/**
	 * Gets route set statistics in a string.
	 * @return Route set statistics.
	 */
	public String getStatistics() {

		String s = this.getNumberOfRouteSets() + " route sets and " + this.getNumberOfRoutes() + " routes.";
	
		return s;
	}
	
	/**
	 * Gets the numbers of route sets (OD pairs).
	 * @return Number of route sets.
	 */
	public int getNumberOfRouteSets() { 
		
		int totalRouteSets = 0;
		for (int i = 1; i < this.routes.length; i++)
			for (int j = 1; j < this.routes[i].length; j++)
				if (this.routes[i][j] != null)
					totalRouteSets++;

		return totalRouteSets;
	}
	
	/**
	 * Gets the total number of routes.
	 * @return Number of routes.
	 */
	public int getNumberOfRoutes() { 
	
		int totalRoutes = 0;
		for (int i = 1; i < this.routes.length; i++)
			for (int j = 1; j < this.routes[i].length; j++)
				if (this.routes[i][j] != null)
					totalRoutes += this.routes[i][j].getSize();
			
		return totalRoutes;
	}
	
	/**
	 * Getter method for the road network.
	 * @return Road network.
	 */
	public RoadNetwork getRoadNetwork() {
		
		return this.roadNetwork;
	}

//	/**
//	 * Calculates utilities for all the routes in all the route sets.
//	 * @param linkTravelTime Link travel times.
//	 * @param consumption Engine fuel consumption per 100 km.
//	 * @param unitCost Unit cost of fuel.
//	 * @param params Parameters of the route choice model.
//	 */
//	public void calculateAllUtilities(HashMap<Integer, Double> linkTravelTime, HashMap<String, Double> energyConsumptionParameters, double unitCost, HashMap<String, HashMap<Integer, Double>> linkCharges, Properties params) {
//
//		for (Object mk: routes.keySet()) {
//			int origin = (int) ((MultiKey)mk).getKey(0);
//			int destination = (int) ((MultiKey)mk).getKey(1);
//		
//			RouteSet rs = (RouteSet)routes.get(origin, destination);
//			rs.calculateUtilities(linkTravelTime, energyConsumptionParameters, unitCost, linkCharges, params);
//			rs.sortRoutesOnUtility(); //will update probabilities as well
//		}
//	}
		
	/**
	 * Calculates all pathsizes for all the route sets (expensive operation).
	 */
	public void calculateAllPathsizes() {
		
		LOGGER.info("Calculating path sizes for all the route sets...");
		
		//iterate over all route sets
		for (int i = 1; i < this.routes.length; i++)
			for (int j = 1; j < this.routes[i].length; j++)
				if (this.routes[i][j] != null)
					this.routes[i][j].calculatePathsizes();
		
		LOGGER.debug("Finished path size calculation.");
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
			for (int i = 1; i < this.routes.length; i++)
				for (int j = 1; j < this.routes[i].length; j++)
					if (this.routes[i][j] != null)
						for (Route route: this.routes[i][j].getChoiceSet())
							bufferedWriter.write(route.getFormattedString() + System.getProperty("line.separator"));
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				bufferedWriter.flush();
				bufferedWriter.close();
				//fileWriter.flush();
				//fileWriter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
			LOGGER.debug("Routes successfully saved into a text file.");
		}
	}
	
	/**
	 * Saves all route sets into a binary file.
	 * @param fileName File name.
	 * @param append Whether to append to an existing file.
	 */
	public void saveRoutesBinary(String fileName, boolean append) {
		
		LOGGER.info("Saving the routes into a binary file.");
		
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedStream = null;
        DataOutputStream dataStream = null;
		try {
			outputStream = new FileOutputStream(fileName, append);
			bufferedStream = new BufferedOutputStream(outputStream);
			dataStream = new DataOutputStream(bufferedStream);
			//iterate over all route sets
			for (int i = 1; i < this.routes.length; i++)
				for (int j = 1; j < this.routes[i].length; j++)
					if (this.routes[i][j] != null)
						for (Route route: this.routes[i][j].getChoiceSet())	 {
							for (int edgeID: route.getEdges().toArray())
								dataStream.writeInt(edgeID);
							dataStream.writeInt(0);
						}
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				dataStream.flush();
				dataStream.close();
				bufferedStream.flush();
				bufferedStream.close();
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
			LOGGER.debug("Routes successfully saved into a binary file.");
		}
	}
	
	/**
	 * Saves all route sets into a binary file.
	 * It also uses unsigned short (2 Bytes, which has a max. value of 65535).
	 * @param fileName File name.
	 * @param append Whether to append to an existing file.
	 */
	public void saveRoutesBinaryShort(String fileName, boolean append) {
		
		LOGGER.info("Saving the routes into a binary file.");
		
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedStream = null;
        DataOutputStream dataStream = null;
		try {
			outputStream = new FileOutputStream(fileName, append);
			bufferedStream = new BufferedOutputStream(outputStream);
			dataStream = new DataOutputStream(bufferedStream);
			//iterate over all route sets
			//iterate over all route sets
			for (int i = 1; i < this.routes.length; i++)
				for (int j = 1; j < this.routes[i].length; j++)
					if (this.routes[i][j] != null)
						for (Route route: this.routes[i][j].getChoiceSet())	 {
							for (int edgeID: route.getEdges().toArray()) {
								if (edgeID > 65535) {
									LOGGER.error("Edge ID larger than 65535 cannot be stored as short. Use saveRoutesBinary method instead.");
									return;
								}
							dataStream.writeShort(edgeID);
							}
						dataStream.writeShort(0);
					}
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				dataStream.flush();
				dataStream.close();
				bufferedStream.flush();
				bufferedStream.close();
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
			LOGGER.debug("Routes successfully saved into a binary file.");
		}
	}
	
	/**
	 * Saves all route sets into a binary file.
	 * @param fileName File name.
	 * @param append Whether to append to an existing file.
	 */
	public void saveRoutesBinaryGZIPped(String fileName, boolean append) {
		
		LOGGER.info("Saving the routes into a zipped binary file.");
		
        FileOutputStream outputStream = null;
        GZIPOutputStream gzipStream = null;
        BufferedOutputStream bufferedStream = null;
        DataOutputStream dataStream = null;
		try {
			outputStream = new FileOutputStream(fileName, append);
			gzipStream = new GZIPOutputStream(outputStream);
			bufferedStream = new BufferedOutputStream(gzipStream);
			dataStream = new DataOutputStream(bufferedStream);
			//iterate over all route sets
			for (int i = 1; i < this.routes.length; i++)
				for (int j = 1; j < this.routes[i].length; j++)
					if (this.routes[i][j] != null)
						for (Route route: this.routes[i][j].getChoiceSet())	 {
							for (int edgeID: route.getEdges().toArray())
								dataStream.writeInt(edgeID);
							dataStream.writeInt(0);
						}
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				dataStream.flush();
				dataStream.close();
				bufferedStream.flush();
				bufferedStream.close();
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
			LOGGER.debug("Routes successfully saved into a binary file.");
		}
	}
		
	/**
	 * Reads route sets from a text file.
	 * @param fileName File name.
	 */
	public void readRoutes(String fileName) {
		
		LOGGER.info("Reading pre-generated routes...");
		
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
		    		Route route = new Route(roadNetwork);
		    		boolean success = false;
		    		for (String edge: edges) {
		    			success = route.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[Integer.parseInt(edge)]);
		    			if (!success) break;
		    		}
		    		//System.out.println(route.getFormattedString());
		    		if (success) this.addRoute(route); //add route only if fine;
		    	}
		    	line = br.readLine();
		    	//System.out.println(line);
		    }
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
	
	/**
	 * Reads route sets from a text file without checking whether the routes are valid.
	 * @param fileName File name.
	 */
	public void readRoutesWithoutValidityCheck(String fileName) {
		
		LOGGER.info("Reading pre-generated routes...");
		
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
		    		Route route = new Route(roadNetwork);
		    		for (String edge: edges)
		    			route.addEdgeWithoutValidityCheck((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[Integer.parseInt(edge)]);
		    		//System.out.println(route.getFormattedString());
					//trim to size
					route.trimToSize();
		    		this.addRouteWithoutValidityCheck(route);
		    	}
		    	line = br.readLine();
		    	//System.out.println(line);
		    }
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
	
	/**
	 * Reads route sets from a text file.
	 * @param fileName File name.
	 */
	public void readRoutesBinary(String fileName) {
		
		LOGGER.info("Reading pre-generated routes from " + fileName);
		
		FileInputStream input = null;
		BufferedInputStream buff = null;
		DataInputStream data = null;
		int counterBadRoutes = 0;
		try {
			input = new FileInputStream(fileName);
			buff = new BufferedInputStream(input);
			data = new DataInputStream(buff);
			
			Route route = new Route(roadNetwork);
			//boolean success = true;
			while (true) { 
				int edgeID = data.readInt();
				if (edgeID != 0) { //keep adding edge to the route
					//success = success && route.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge().get(edgeID));
					route.addEdgeWithoutValidityCheck((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[edgeID]);
				} else {
					//trim to size
					route.trimToSize();
					//add route to the route set if all edge additions have been successful
					//if (success) this.addRoute(route);
					if (route.isValid()) this.addRouteWithoutValidityCheck(route);
					else counterBadRoutes++;
					//create new route if there are more bytes
					//if (data.available() > 0) route = new Route();
					route = new Route(roadNetwork);
					//success = true;
				}
			}
		} catch (EOFException e) {
			LOGGER.debug("End of the binary route file reached. ");
			LOGGER.debug("{} bad routes ignored.", counterBadRoutes);
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				input.close();
				buff.close();
				data.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
	
	/**
	 * Reads route sets from a text file.
	 * @param fileName File name.
	 */
	public void readRoutesBinaryWithoutValidityCheck(String fileName) {
		
		LOGGER.info("Reading pre-generated routes from " + fileName);
		
		FileInputStream input = null;
		BufferedInputStream buff = null;
		DataInputStream data = null;
		try {
			input = new FileInputStream(fileName);
			buff = new BufferedInputStream(input);
			data = new DataInputStream(buff);
			
			Route route = new Route(roadNetwork);
			
			while (true) { 
				int edgeID = data.readInt();
				if (edgeID != 0) { //keep adding edge to the route
					route.addEdgeWithoutValidityCheck(edgeID);
				} else {
					//trim to size
					route.trimToSize();
					//add route to the route set if all edge additions have been successful
					this.addRouteWithoutValidityCheck(route);					
					//create new route if there are more bytes
					//if (data.available() > 0) route = new Route();
					route = new Route(roadNetwork);
				}
			}
		} catch (EOFException e) {
			LOGGER.debug("End of the binary route file reached.");
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				input.close();
				buff.close();
				data.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
	
	/**
	 * Reads route sets from a text file.
	 * @param fileName File name.
	 */
	public void readRoutesBinaryShortWithoutValidityCheck(String fileName) {
		
		LOGGER.info("Reading pre-generated routes from " + fileName);
		
		FileInputStream input = null;
		BufferedInputStream buff = null;
		DataInputStream data = null;
		try {
			input = new FileInputStream(fileName);
			buff = new BufferedInputStream(input);
			data = new DataInputStream(buff);
			
			Route route = new Route(roadNetwork);
			while (true) { 
				int edgeID = data.readUnsignedShort();
				if (edgeID != 0) { //keep adding edge to the route
					route.addEdgeWithoutValidityCheck(edgeID);
				} else {
					//trim to size
					route.trimToSize();
					//add route to the route set if all edge additions have been successful
					this.addRouteWithoutValidityCheck(route);
					//create new route if there are more bytes
					//if (data.available() > 0) route = new Route();
					route = new Route(roadNetwork);
				}
			}
		} catch (EOFException e) {
			LOGGER.debug("End of the binary route file reached.");
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				input.close();
				buff.close();
				data.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
	
	/**
	 * Reads route sets from a text file.
	 * @param fileName File name.
	 */
	public void readRoutesBinaryGZIPpedWithoutValidityCheck(String fileName) {
		
		LOGGER.info("Reading pre-generated routes from " + fileName);

		FileInputStream input = null;
		GZIPInputStream gzip = null;
		BufferedInputStream buff = null;
		DataInputStream data = null;
		try {
			input = new FileInputStream(fileName);
			gzip = new GZIPInputStream(input);
			buff = new BufferedInputStream(gzip);
			data = new DataInputStream(buff);
			
			Route route = new Route(roadNetwork);
			while (true) { 
				int edgeID = data.readInt();
				if (edgeID != 0) { //keep adding edge to the route
					route.addEdgeWithoutValidityCheck(edgeID);
				} else {
					//trim to size
					route.trimToSize();
					//add route to the route set if all edge additions have been successful
					this.addRouteWithoutValidityCheck(route);
					//create new route if there are more bytes
					//if (data.available() > 0) route = new Route();
					route = new Route(roadNetwork);
				}
			}
		} catch (EOFException e) {
			LOGGER.debug("End of the binary route file reached.");
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				input.close();
				gzip.close();
				buff.close();
				data.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
}
