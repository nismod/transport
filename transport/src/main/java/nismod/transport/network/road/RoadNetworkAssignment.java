/**
 * 
 */
package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.SkimMatrix;

/**
 * Network assignment of origin-destination flows
 * @author Milan Lovric
 *
 */
public class RoadNetworkAssignment {

	public static final double SPEED_LIMIT_M_ROAD = 112.65; //70mph = 31.29mps = 112.65kph
	public static final double SPEED_LIMIT_A_ROAD = 96.56; //60mph = 26.82mps = 96.56kph

	private RoadNetwork roadNetwork;

	private HashMap<Integer, Integer> linkVolumes;
	private HashMap<Integer, HashMap<String, Double>> linkVolumesPerVehicleType;
	private HashMap<Integer, Double> linkTravelTime;

	//inter-zonal path storage - for every OD pair stores a list of paths
	private MultiKeyMap<String, List<Path>> pathStorage;

	/**
	 * @param roadNetwork
	 */
	public RoadNetworkAssignment(RoadNetwork roadNetwork) {

		this.roadNetwork = roadNetwork;
		this.linkVolumes = new HashMap<Integer, Integer>();
		this.linkVolumesPerVehicleType = new HashMap<Integer, HashMap<String, Double>>();
		this.linkTravelTime = new HashMap<Integer, Double>();
		this.pathStorage = new MultiKeyMap<String, List<Path>>();
	}

	/** 
	 * Assigns passenger origin-destination matrix to the road network
	 * @param passengerODM Passenger origin-destination matrix
	 */
	public void assignPassengerFlows(ODMatrix passengerODM) {

		System.out.println("Assigning the passenger flows from the passenger matrix...");

		//for each OD pair from the passengerODM		
		for (MultiKey mk: passengerODM.getKeySet()) {
			//System.out.println(mk);
			//System.out.println("origin = " + mk.getKey(0));
			//System.out.println("destination = " + mk.getKey(1));

			//for each trip
			int flow = passengerODM.getFlow((String)mk.getKey(0), (String)mk.getKey(1));
			for (int i=0; i<flow; i++) {
				//choose random trip start/end nodes within the origin and the destination zone
				List listOfOriginNodes = roadNetwork.getZoneToNodes().get(mk.getKey(0));
				List listOfDestinationNodes = roadNetwork.getZoneToNodes().get(mk.getKey(1));
				int numberOriginNodes = listOfOriginNodes.size();
				int numberDestinationNodes = listOfDestinationNodes.size();
				//System.out.println("Number of origin nodes: " + numberOriginNodes);
				//System.out.println("Number of destination nodes: " + numberDestinationNodes);
				int indexOrigin = new Random().nextInt(numberOriginNodes);
				int indexDestination = new Random().nextInt(numberDestinationNodes);
				//System.out.println("Index of origin node: " + indexOrigin);
				//System.out.println("Index of destination node: " + indexDestination);
				int originNode = (int) listOfOriginNodes.get(indexOrigin);
				int destinationNode = (int) listOfDestinationNodes.get(indexDestination);
				//System.out.println("Origin node: " + originNode);
				//System.out.println("Destination node: " + destinationNode);

				//get the shortest path from origin to destination node using AStar algorithm
				DirectedGraph rn = roadNetwork.getNetwork();
				//set source and destination node
				Iterator iter = rn.getNodes().iterator();
				Node from = null, to = null;
				while (iter.hasNext() && (from == null || to == null)) {
					DirectedNode node = (DirectedNode) iter.next();
					if (node.getID() == originNode) from = node;
					if (node.getID() == destinationNode) to = node;
				}
				try {
					AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
					aStarPathFinder.calculate();
					Path aStarPath;
					aStarPath = aStarPathFinder.getPath();
					aStarPath.reverse();
					//System.out.println(aStarPath);
					//System.out.println("The path as a list of nodes: " + aStarPath);
					List listOfEdges = aStarPath.getEdges();
					//System.out.println("The path as a list of edges: " + listOfEdges);
					//System.out.println("Path size in the number of nodes: " + aStarPath.size());
					//System.out.println("Path size in the number of edges: " + listOfEdges.size());
					double sum = 0;
					List<Path> list;
					for (Object o: listOfEdges) {
						//DirectedEdge e = (DirectedEdge) o;
						Edge e = (Edge) o;
						//System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
						SimpleFeature sf = (SimpleFeature) e.getObject();
						double length = (double) sf.getAttribute("LenNet");
						//System.out.println(length);
						sum += length;

						//increase volume count for that edge
						Integer volume = linkVolumes.get(e.getID());
						if (volume == null) volume = 0;
						//volume++;
						volume = volume + passengerODM.getFlow((String)mk.getKey(0), (String)mk.getKey(1));
						linkVolumes.put(e.getID(), volume);
					}
					//System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

					//store path in path storage
					if (pathStorage.containsKey(mk.getKey(0), mk.getKey(1))) 
						list = (List<Path>) pathStorage.get(mk.getKey(0), mk.getKey(1));
					else {
						list = new ArrayList<Path>();
						pathStorage.put((String)mk.getKey(0), (String)mk.getKey(1), list);
					}
					list.add(aStarPath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}//for each trip
		}//for each OD pair
	}

	/**
	 * Assigns freight origin-destination matrix to the road network
	 * @param freightODM Freight origin-destination matrix
	 */
	public void assignFreightFlows(ODMatrix freightODM) {

	}

	/**
	 * Updates link travel times.
	 */
	public void updateLinkTravelTimes() {

	}

	/**
	 * Updates travel time skim matrix (zone-to-zone travel times).
	 * @param timeSkimMatrix Inter-zonal skim matrix (time)
	 */
	public void updateTimeSkimMatrix(SkimMatrix timeSkimMatrix) {

	}

	/**
	 * Updates cost skim matrix (zone-to-zone financial costs).
	 * @param costSkimMatrix Inter-zonal skim matrix (cost)
	 */
	public void updateCostSkimMatrix(SkimMatrix costSkimMatrix) {

	}

	/**
	 * Saves assignment results to output files.
	 */
	public void saveAssignmentResults() {

	}

	/**
	 * Getter method for the link volumes.
	 * @return Link volumes
	 */
	public HashMap<Integer, Integer> getLinkVolumes() {

		return this.linkVolumes;
	}

	/**
	 * Getter method for the path storage.
	 * @return Path storage
	 */
	public MultiKeyMap getPathStorage() {

		return this.pathStorage;
	}
}