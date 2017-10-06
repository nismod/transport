/**
 * 
 */
package nismod.transport.network.road;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.SkimMatrix;
import nismod.transport.demand.SkimMatrixFreight;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.utility.RandomSingleton;

/**
 * Network assignment of origin-destination flows
 * @author Milan Lovric
 *
 */
public class RoadNetworkAssignment {

	//public static final double SPEED_LIMIT_M_ROAD = 112.65; //70mph = 31.29mps = 112.65kph
	//public static final double SPEED_LIMIT_A_ROAD = 96.56; //60mph = 26.82mps = 96.56kph
	public static final double FREE_FLOW_SPEED_M_ROAD = 115.7923; //71.95mph = 32.16mps = 115.79kph
	public static final double FREE_FLOW_SPEED_A_ROAD = 90.203731; //56.05mph = 25.05mps = 90.20kph
	public static final int MAXIMUM_CAPACITY_M_ROAD = 2330; //PCU per lane per hour
	public static final int MAXIMUM_CAPACITY_A_ROAD = 1380; //PCU per lane per hour
	public static final int NUMBER_OF_LANES_M_ROAD = 3; //for one direction
	public static final int NUMBER_OF_LANES_A_ROAD = 1; //for one direction
	public static final double AVERAGE_SPEED_FERRY = 20.0; //kph
	public static final double AVERAGE_ACCESS_EGRESS_SPEED_CAR = 48.0; //30mph = 48kph  
	public static final double AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT = 48.0; //30mph = 48kph 
	public static final double PEAK_HOUR_PERCENTAGE = 0.10322;
	public static final double ALPHA = 0.15;
	public static final double BETA_M_ROAD = 5.55;
	public static final double BETA_A_ROAD = 4;
	public static final boolean FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT = false; //true means that origin and destination nodes can be the same
	public static final int INTERZONAL_TOP_NODES = 10; //how many top nodes (based on gravitated population size) to considers as trip origin/destination

	private static RandomSingleton rng = RandomSingleton.getInstance();

	public static enum EngineType {
		PETROL, DIESEL, LPG, ELECTRICITY, HYBRID, HYDROGEN
	}

	public static enum VehicleType {
		CAR(0), ARTIC(1), RIGID(2), VAN(3);
		private int value; 
		private VehicleType(int value) { this.value = value;} 
	};

	private HashMap<VehicleType, Double> vehicleTypeToPCU;

	private HashMap<EngineType, Double> energyUnitCosts;
	private HashMap<EngineType, Double> energyConsumptionsPer100km;
	private HashMap<EngineType, Double> engineTypeFractions;

	private RoadNetwork roadNetwork;

	private HashMap<Integer, Double> linkVolumesInPCU;
	private HashMap<VehicleType, HashMap<Integer, Integer>> linkVolumesPerVehicleType;
	private HashMap<Integer, Double> linkFreeFlowTravelTime;
	private HashMap<Integer, Double> linkTravelTime;

	//inter-zonal path storage - for every OD pair stores a list of paths
	private MultiKeyMap<String, List<Path>> pathStorage;
	//private MultiKeyMap<Integer, List<Path>> pathStorageFreight;
	private HashMap<VehicleType, MultiKeyMap<Integer, List<Path>>> pathStorageFreight;
	//inter-zonal path storage - for every OD pair stores a list of paths
	private MultiKeyMap<String, List<Route>> routeStorage;
	//private MultiKeyMap<Integer, List<Path>> pathStorageFreight;
	private HashMap<VehicleType, MultiKeyMap<Integer, List<Route>>> routeStorageFreight;
	
	//the probability of trip starting/ending in the census output area
	private HashMap<String, Double> areaCodeProbabilities;
	//the probability of trip starting/ending in the workplace zone
	private HashMap<String, Double> workplaceZoneProbabilities;
	//the probability of trip starting/ending at a node
	private HashMap<Integer, Double> nodeProbabilities;
	//the probability of freight trip starting/ending at a node
	private HashMap<Integer, Double> nodeProbabilitiesFreight;

	//the number of trips starting in each census output area
	private HashMap<String, Integer> areaCodeNoTripStarts;
	//the number of trips ending in each census output area
	private HashMap<String, Integer> areaCodeNoTripEnds;
	//the number of trips starting in each workplace zone
	private HashMap<String, Integer> workplaceZoneNoTripStarts;
	//the number of trips starting in each workplace zone
	private HashMap<String, Integer> workplaceZoneNoTripEnds;
	//the number of trips starting in each LAD where finer location is unknown (not total number)
	private HashMap<String, Integer> LADnoTripStarts;
	//the number of trips ending in each LAD where finer location is unknown (not total number)
	private HashMap<String, Integer> LADnoTripEnds;
	//the number of freight trips starting in each LAD (obtained by mapping trip origin nodes to LADs)
	private HashMap<String, Integer> LADnoTripStartsFreight;
	//the number of freight trips ending in each LAD (obtained by mapping trip destination nodes to LADs)
	private HashMap<String, Integer> LADnoTripEndsFreight;

	/**
	 * @param roadNetwork Road network.
	 * @param defaultLinkTravelTime Default link travel times.
	 * @param areCodeProbabilities Probabilities of trips starting/ending in each census output area.
	 */
	public RoadNetworkAssignment(RoadNetwork roadNetwork, HashMap<EngineType, Double> energyUnitCosts, HashMap<EngineType, Double> engineTypeFractions, HashMap<Integer, Double> defaultLinkTravelTime, HashMap<String, Double> areaCodeProbabilities, HashMap<String, Double> workplaceZoneProbabilities) {

		this.roadNetwork = roadNetwork;
		this.linkVolumesInPCU = new HashMap<Integer, Double>();
		this.linkVolumesPerVehicleType = new HashMap<VehicleType, HashMap<Integer, Integer>>();
		for (VehicleType vht: VehicleType.values()) {
			HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
			linkVolumesPerVehicleType.put(vht, map);
		}
		this.linkFreeFlowTravelTime = new HashMap<Integer, Double>();
		this.linkTravelTime = new HashMap<Integer, Double>();
		this.pathStorage = new MultiKeyMap<String, List<Path>>();
		//this.pathStorageFreight = new MultiKeyMap<Integer, List<Path>>();
		this.pathStorageFreight = new HashMap<VehicleType, MultiKeyMap<Integer, List<Path>>>();
		for (VehicleType vht: VehicleType.values()) {
			MultiKeyMap<Integer, List<Path>> map = new MultiKeyMap<Integer, List<Path>>();
			pathStorageFreight.put(vht, map);
		}
		this.routeStorage = new MultiKeyMap<String, List<Route>>();
		this.routeStorageFreight = new HashMap<VehicleType, MultiKeyMap<Integer, List<Route>>>();
		for (VehicleType vht: VehicleType.values()) {
			MultiKeyMap<Integer, List<Route>> map = new MultiKeyMap<Integer, List<Route>>();
			routeStorageFreight.put(vht, map);
		}
		areaCodeNoTripStarts = new HashMap<String, Integer>();
		areaCodeNoTripEnds = new HashMap<String, Integer>();
		workplaceZoneNoTripStarts = new HashMap<String, Integer>();
		workplaceZoneNoTripEnds = new HashMap<String, Integer>();
		LADnoTripStarts = new HashMap<String, Integer>();
		LADnoTripEnds = new HashMap<String, Integer>();
		LADnoTripStartsFreight = new HashMap<String, Integer>();
		LADnoTripEndsFreight = new HashMap<String, Integer>();

		//calculate link travel time
		Iterator edgesIterator = roadNetwork.getNetwork().getEdges().iterator();
		while (edgesIterator.hasNext()) {
			DirectedEdge edge = (DirectedEdge) edgesIterator.next();
			//calculate free-flow travel time
			SimpleFeature feature = (SimpleFeature)edge.getObject();
			String roadNumber = (String) feature.getAttribute("RoadNumber");
			double travelTime = 0;
			if (roadNumber.charAt(0) == 'M') //motorway
				travelTime = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.FREE_FLOW_SPEED_M_ROAD * 60;  //travel time in minutes
			else if (roadNumber.charAt(0) == 'A') //A road
				travelTime = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.FREE_FLOW_SPEED_A_ROAD * 60;  //travel time in minutes
			else //ferry
				travelTime = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.AVERAGE_SPEED_FERRY * 60;  //travel time in minutes
			linkFreeFlowTravelTime.put(edge.getID(), travelTime);

			//if no default link travel time, use free flow travel time as default
			if (defaultLinkTravelTime != null)	
				this.linkTravelTime.put(edge.getID(), defaultLinkTravelTime.get(edge.getID()));
			else
				this.linkTravelTime.put(edge.getID(), linkFreeFlowTravelTime.get(edge.getID()));
		}

		//calculate area code choice probability
		if (areaCodeProbabilities != null)	this.areaCodeProbabilities = areaCodeProbabilities;
		else {
			this.areaCodeProbabilities = new HashMap<String, Double>();
			//for each LAD zone
			for (String zone: roadNetwork.getZoneToAreaCodes().keySet()) {
				int totalPopulationInZone = 0;
				List<String> listOfAreaCodes = roadNetwork.getZoneToAreaCodes().get(zone);
				for (String areaCode: listOfAreaCodes)
					totalPopulationInZone += roadNetwork.getAreaCodeToPopulation().get(areaCode);
				for (String areaCode: listOfAreaCodes)
					this.areaCodeProbabilities.put(areaCode, (double) roadNetwork.getAreaCodeToPopulation().get(areaCode) / totalPopulationInZone);
			}
		}
		//System.out.println("Probabilities for area codes:");
		//System.out.println(this.areaCodeProbabilities);

		//calculate workplace zone choice probability
		if (workplaceZoneProbabilities != null)	this.workplaceZoneProbabilities = workplaceZoneProbabilities;
		else {
			this.workplaceZoneProbabilities = new HashMap<String, Double>();
			//for each LAD zone
			for (String zone: roadNetwork.getZoneToWorkplaceCodes().keySet()) {
				int totalPopulationInZone = 0;
				List<String> listOfWorkplaceCodes = roadNetwork.getZoneToWorkplaceCodes().get(zone);
				for (String workplaceCode: listOfWorkplaceCodes)
					totalPopulationInZone += roadNetwork.getWorkplaceCodeToPopulation().get(workplaceCode);
				for (String workplaceCode: listOfWorkplaceCodes)
					this.workplaceZoneProbabilities.put(workplaceCode, (double) roadNetwork.getWorkplaceCodeToPopulation().get(workplaceCode) / totalPopulationInZone);
			}
		}
		//System.out.println("Probabilities for workplace zones:");
		//System.out.println(this.workplaceZoneProbabilities);

		//calculate node probabilities from gravitating population
		nodeProbabilities = new HashMap<Integer, Double>();
		//System.out.println(roadNetwork.getZoneToNodes().keySet());
		for (String zone: roadNetwork.getZoneToNodes().keySet()) {
			double sum = 0;
			//System.out.println(roadNetwork.getZoneToNodes().get(zone));
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) sum += roadNetwork.getGravitatingPopulation(node);
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				double probability = roadNetwork.getGravitatingPopulation(node) / sum;
				nodeProbabilities.put(node, probability);
			}
		}
		System.out.println("Probabilities for nodes:");
		System.out.println(this.nodeProbabilities);

		//calculate node probabilities from gravitating workplace population
		nodeProbabilitiesFreight = new HashMap<Integer, Double>();
		//System.out.println(roadNetwork.getZoneToNodes().keySet());
		for (String zone: roadNetwork.getZoneToNodes().keySet()) {
			double sum = 0;
			//System.out.println(roadNetwork.getZoneToNodes().get(zone));
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) sum += roadNetwork.getGravitatingWorkplacePopulation(node);
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				double probability = roadNetwork.getGravitatingWorkplacePopulation(node) / sum;
				nodeProbabilitiesFreight.put(node, probability);
			}
		}
		System.out.println("Node probabilities for freight:");
		System.out.println(this.nodeProbabilitiesFreight);

		//set default values for vehicle type to PCU conversion
		vehicleTypeToPCU = new HashMap<VehicleType, Double>();
		vehicleTypeToPCU.put(VehicleType.CAR, 1.0);
		vehicleTypeToPCU.put(VehicleType.ARTIC, 2.0);
		vehicleTypeToPCU.put(VehicleType.RIGID, 2.0);
		vehicleTypeToPCU.put(VehicleType.VAN, 1.0);

		//set default values for energy consumption of different car engine types
		//for petrol/diesel/lpg this is in £/l, for hydrogen in £/kg, for electricity in £/kWh.
		if (energyUnitCosts != null) this.energyUnitCosts = energyUnitCosts;
		else {
			this.energyUnitCosts = new HashMap<EngineType, Double>();
			this.energyUnitCosts.put(EngineType.PETROL, 1.17);
			this.energyUnitCosts.put(EngineType.DIESEL, 1.20);
			this.energyUnitCosts.put(EngineType.LPG, 0.6);
			this.energyUnitCosts.put(EngineType.ELECTRICITY, 0.1);
			this.energyUnitCosts.put(EngineType.HYDROGEN, 4.19);
			this.energyUnitCosts.put(EngineType.HYBRID, 1.17);

		}
		energyConsumptionsPer100km = new HashMap<EngineType, Double>();
		energyConsumptionsPer100km.put(EngineType.PETROL, 5.4);
		energyConsumptionsPer100km.put(EngineType.DIESEL, 4.6);
		energyConsumptionsPer100km.put(EngineType.LPG, 6.75);
		energyConsumptionsPer100km.put(EngineType.ELECTRICITY, 20.0);
		energyConsumptionsPer100km.put(EngineType.HYDROGEN, 0.95);
		energyConsumptionsPer100km.put(EngineType.HYBRID, 7.4);

		if (engineTypeFractions != null) this.engineTypeFractions = engineTypeFractions;
		else {
			this.engineTypeFractions = new HashMap<EngineType, Double>();
			this.engineTypeFractions.put(EngineType.PETROL, 0.45);
			this.engineTypeFractions.put(EngineType.DIESEL, 0.35);
			this.engineTypeFractions.put(EngineType.LPG, 0.1);
			this.engineTypeFractions.put(EngineType.ELECTRICITY, 0.05);
			this.engineTypeFractions.put(EngineType.HYDROGEN, 0.025);
			this.engineTypeFractions.put(EngineType.HYBRID, 0.025);
		}
	}

	/** 
	 * Assigns passenger origin-destination matrix to the road network.
	 * Uses the fastest path based on the current values in the linkTravelTime field.
	 * @param passengerODM Passenger origin-destination matrix.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlows(ODMatrix passengerODM) {

		System.out.println("Assigning the passenger flows from the passenger matrix...");

		//sort nodes based on the gravitating population
		this.roadNetwork.sortGravityNodes();
		
		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		//for each OD pair from the passengerODM		
		for (MultiKey mk: passengerODM.getKeySet()) {
			//System.out.println(mk);
			//System.out.println("origin = " + mk.getKey(0));
			//System.out.println("destination = " + mk.getKey(1));
			String origin = (String)mk.getKey(0);
			String destination = (String)mk.getKey(1);

			//for each trip
			int flow = passengerODM.getFlow(origin, destination);
			counterTotalFlow += flow;
			
			for (int i=0; i<flow; i++) {

				/*
					//choose random trip start/end nodes within the origin and the destination zone
					List listOfOriginNodes = roadNetwork.getZoneToNodes().get(mk.getKey(0));
					List listOfDestinationNodes = roadNetwork.getZoneToNodes().get(mk.getKey(1));
					int numberOriginNodes = listOfOriginNodes.size();
					int numberDestinationNodes = listOfDestinationNodes.size();
					//System.out.println("Number of origin nodes: " + numberOriginNodes);
					//System.out.println("Number of destination nodes: " + numberDestinationNodes);
					int indexOrigin = rng.nextInt(numberOriginNodes);
					int indexDestination = rng.nextInt(numberDestinationNodes);
					//System.out.println("Index of origin node: " + indexOrigin);
					//System.out.println("Index of destination node: " + indexDestination);
					int originNode = (int) listOfOriginNodes.get(indexOrigin);
					int destinationNode = (int) listOfDestinationNodes.get(indexDestination);
					//System.out.println("Origin node: " + originNode);
					//System.out.println("Destination node: " + destinationNode);
				 */

				/*
					//choose origin/destination census output areas and map them to their nearest network nodes
					List<String> listOfOriginAreaCodes = roadNetwork.getZoneToAreaCodes().get(mk.getKey(0));
					List<String> listOfDestinationAreaCodes = roadNetwork.getZoneToAreaCodes().get(mk.getKey(1));

					if (listOfOriginAreaCodes == null) System.err.println("listOfOriginAreaCodes is null for " + mk.getKey(0) );
					if (listOfDestinationAreaCodes == null) System.err.println("listOfDestinationAreaCodes is null for " + mk.getKey(1) );

					//System.out.println("listOfOriginAreaCodes: " + listOfOriginAreaCodes);
					//System.out.println("listOfDestinationAreaCodes: " + listOfDestinationAreaCodes);

					//choose origin census output area
					double cumulativeProbability = 0.0;
					String originAreaCode = null;
					double random = rng.nextDouble();
					for (String areaCode: listOfOriginAreaCodes) {
						cumulativeProbability += areaCodeProbabilities.get(areaCode);
						if (Double.compare(cumulativeProbability, random) > 0) {
							originAreaCode = areaCode;
							break;
						}
					}
					if (originAreaCode == null) System.err.println("Origin output area was not selected.");
					else { //increase the number of tips starting at originAreaCode
						Integer number = this.areaCodeNoTripStarts.get(originAreaCode);
						if (number == null) number = 0;
						this.areaCodeNoTripStarts.put(originAreaCode, ++number);
					}

					//choose destination census output area
					cumulativeProbability = 0.0;
					String destinationAreaCode = null;
					random = rng.nextDouble();
					for (String areaCode: listOfDestinationAreaCodes) {
						cumulativeProbability += areaCodeProbabilities.get(areaCode);
						if (Double.compare(cumulativeProbability, random) > 0) {
							destinationAreaCode = areaCode;
							break;
						}
					}
					if (destinationAreaCode == null) System.err.println("Destination output area was not selected.");
					else { //increase the number of tips ending at destinationAreaCode
						Integer number = this.areaCodeNoTripEnds.get(destinationAreaCode);
						if (number == null) number = 0;
						this.areaCodeNoTripEnds.put(destinationAreaCode, ++number);
					}

					int originNode = -1, destinationNode = -1;
					try {
						//take the nearest node on the network
						originNode = roadNetwork.getAreaCodeToNearestNode().get(originAreaCode);
						destinationNode = roadNetwork.getAreaCodeToNearestNode().get(destinationAreaCode);
					}
					catch (NullPointerException e) {
						e.printStackTrace();
						System.err.printf("Couldn't find the nearest node for %s or %s output area.\n", originAreaCode, destinationAreaCode);
					}
				 */

				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node
				List<Integer> listOfOriginNodes = roadNetwork.getZoneToNodes().get(origin); //the list is already sorted
				List<Integer> listOfDestinationNodes = roadNetwork.getZoneToNodes().get(destination); //the list is already sorted
				//choose origin node
				double cumulativeProbability = 0.0;
				Integer originNode = null;
				double random = rng.nextDouble();
				for (int node: listOfOriginNodes) {
					cumulativeProbability += nodeProbabilities.get(node);
					if (Double.compare(cumulativeProbability, random) > 0) {
						originNode = node;
						break;
					}
				}

				//choose destination node
				cumulativeProbability = 0.0;
				Integer destinationNode = null;
				random = rng.nextDouble();
				//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
				//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
				if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT && origin.equals(destination)) { //no replacement and intra-zonal trip
					for (int node: listOfDestinationNodes) {
						if (node == originNode.intValue()) continue; //skip if the node is the same as origin
						cumulativeProbability += nodeProbabilities.get(node) / (1.0 - nodeProbabilities.get(originNode));
						if (Double.compare(cumulativeProbability, random) > 0) {
							destinationNode = node;
							break;
						}
					}
				} else	{ //inter-zonal trip (or intra-zonal with replacement)
					for (int node: listOfDestinationNodes) {
						cumulativeProbability += nodeProbabilities.get(node);
						if (Double.compare(cumulativeProbability, random) > 0) {
							destinationNode = node;
							break;
						}
					}
				}			

				DirectedGraph rn = roadNetwork.getNetwork();
				//set source and destination node
				Node from = roadNetwork.getNodeIDtoNode().get(originNode);
				Node to = roadNetwork.getNodeIDtoNode().get(destinationNode);
				//					System.out.println("from " + from + " to " + to);
				Path foundPath = null;
				try {

					//see if that path from node 'from' to node 'to' already exists in the path storage
					if (pathStorage.containsKey(origin, destination)) { 
						List<Path> list = (List<Path>) pathStorage.get(origin, destination);
						for (Path p: list) {
							//if (p.getFirst().equals(from) && p.getLast().equals(to)) {
							if (p.getFirst().getID() == from.getID() && p.getLast().getID() == to.getID()) {
								foundPath = p;
								break;
							}
						}
					}

					//if path does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundPath == null) {
						//System.out.println("The path does not exist in the path storage");
						AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctionsTime(to, this.linkTravelTime));
						//AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
						aStarPathFinder.calculate();
						Path aStarPath;
						aStarPath = aStarPathFinder.getPath();
						aStarPath.reverse();
						//System.out.println(aStarPath);
						//System.out.println("The path as a list of nodes: " + aStarPath);

						foundPath = aStarPath;
					}

					counterAssignedTrips++;
					
					//increase the number of trips starting at origin LAD
					Integer number = this.LADnoTripStarts.get(origin);
					if (number == null) number = 0;
					this.LADnoTripStarts.put(origin, ++number);
					//increase the number of trips ending at destination LAD
					Integer number2 = this.LADnoTripEnds.get(destination);
					if (number2 == null) number2 = 0;
					this.LADnoTripEnds.put(destination, ++number2);

					List listOfEdges = foundPath.getEdges();
					//System.out.println("The path as a list of edges: " + listOfEdges);
					//System.out.println("Path size in the number of nodes: " + pathFound.size());
					//System.out.println("Path size in the number of edges: " + listOfEdges.size());
					/*
						DijkstraShortestPathFinder pathFinder = new DijkstraShortestPathFinder(rn, from, roadNetwork.getDijkstraTimeWeighter());
						pathFinder.calculate();
						Path path = pathFinder.getPath(to);
						path.reverse();
						List listOfEdges = path.getEdges();
					 */
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

						//increase volume count (in PCU) for that edge
						Double volumeInPCU = linkVolumesInPCU.get(e.getID());
						if (volumeInPCU == null) volumeInPCU = 0.0;
						volumeInPCU++;
						linkVolumesInPCU.put(e.getID(), volumeInPCU);
						
						//increase volume count for that edge and for a car vehicle type
						Integer volume = linkVolumesPerVehicleType.get(VehicleType.CAR).get(e.getID());
						if (volume == null) volume = 0;
						volume++;
						linkVolumesPerVehicleType.get(VehicleType.CAR).put(e.getID(), volume);
					}
					//System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

					//store path in path storage
					if (pathStorage.containsKey(mk.getKey(0), mk.getKey(1))) 
						list = (List<Path>) pathStorage.get(mk.getKey(0), mk.getKey(1));
					else {
						list = new ArrayList<Path>();
						pathStorage.put((String)mk.getKey(0), (String)mk.getKey(1), list);
					}
					list.add(foundPath); //list.add(path);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.printf("Couldnt find path from node %d to node %d!", from.getID(), to.getID());
				}
			}//for each trip
		}//for each OD pair
		
		System.out.println("Total flow: " + counterTotalFlow);
		System.out.println("Total assigned trips: " + counterAssignedTrips);
		System.out.println("Assignment percentage: " + 100.0* counterAssignedTrips / counterTotalFlow);
	}
	
	/** 
	 * Assigns passenger origin-destination matrix to the road network.
	 * Uses the route choice and pre-generated paths.
	 * @param passengerODM Passenger origin-destination matrix.
	 */
	//@SuppressWarnings("unused")
	public void assignPassengerFlowsRouteChoice(ODMatrix passengerODM, RouteSetGenerator rsg, Properties params) {

		System.out.println("Assigning the passenger flows from the passenger matrix...");

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;
		
		//sort nodes based on the gravitating population
		this.roadNetwork.sortGravityNodes();

		//for each OD pair from the passengerODM		
		for (MultiKey mk: passengerODM.getKeySet()) {
			//System.out.println(mk);
			//System.out.println("origin = " + mk.getKey(0));
			//System.out.println("destination = " + mk.getKey(1));
			String origin = (String)mk.getKey(0);
			String destination = (String)mk.getKey(1);

			//for each trip
			int flow = passengerODM.getFlow(origin, destination);
			counterTotalFlow += flow;
			
			for (int i=0; i<flow; i++) {

				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node
				List<Integer> listOfOriginNodes = roadNetwork.getZoneToNodes().get(origin); //the list is already sorted
				List<Integer> listOfDestinationNodes = roadNetwork.getZoneToNodes().get(destination); //the list is already sorted
			
				Integer originNode = null;
				Integer destinationNode = null;
								
				if (origin.equals(destination)) { //if intra-zonal

					//choose origin node
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
					for (int node: listOfOriginNodes) {
						cumulativeProbability += nodeProbabilities.get(node);
						if (Double.compare(cumulativeProbability, random) > 0) {
							originNode = node;
							break;
						}
					}

					//choose destination node
					cumulativeProbability = 0.0;
					random = rng.nextDouble();
					//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
					//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
					if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT) { //no replacement
						for (int node: listOfDestinationNodes) {
							if (node == originNode.intValue()) continue; //skip if the node is the same as origin
							cumulativeProbability += nodeProbabilities.get(node) / (1.0 - nodeProbabilities.get(originNode));
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = node;
								break;
							}
						}
					} else	{ //with replacement
						for (int node: listOfDestinationNodes) {
							cumulativeProbability += nodeProbabilities.get(node);
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = node;
								break;
							}
						}
					}		
				
				} else { //inter-zonal
					
					//for (int nodeIndex=0; nodeIndex < INTERZONAL_TOP_NODES && listOfOriginNodes.size(); nodeIndex++)
					//choose random from top_nodes regardless of population size
					int indexOrigin = rng.nextInt(INTERZONAL_TOP_NODES<listOfOriginNodes.size()?INTERZONAL_TOP_NODES:listOfOriginNodes.size());
					int indexDestination = rng.nextInt(INTERZONAL_TOP_NODES<listOfDestinationNodes.size()?INTERZONAL_TOP_NODES:listOfDestinationNodes.size());
					//System.out.println("Index of origin node: " + indexOrigin);
					//System.out.println("Index of destination node: " + indexDestination);
					originNode = (int) listOfOriginNodes.get(indexOrigin);					
					destinationNode = (int) listOfDestinationNodes.get(indexDestination);
					
				}
				
				RouteSet fetchedRouteSet = rsg.getRouteSet(originNode, destinationNode);
				if (fetchedRouteSet == null) {
					System.err.printf("Can't fetch the route set between nodes %d and %d! %s", originNode, destinationNode, System.lineSeparator());
					continue;
				}
				
				if (fetchedRouteSet.getProbabilities() == null) {
					//probabilities need to be calculated for this route set before a choice can be made
					fetchedRouteSet.setLinkTravelTime(this.linkTravelTime);
					fetchedRouteSet.setParameters(params);
					fetchedRouteSet.calculateUtilities(this.linkTravelTime, params);
					fetchedRouteSet.calculateProbabilities(this.linkTravelTime, params);
					fetchedRouteSet.sortRoutesOnUtility();
				}
				
				Route chosenRoute = fetchedRouteSet.choose(params);
				if (chosenRoute == null) {
					System.err.printf("No chosen route between nodes %d and %d! %s", originNode, destinationNode, System.lineSeparator());
					continue;
				}
					
					//there is a chosenRoute
					counterAssignedTrips++;

					//increase the number of trips starting at origin LAD
					Integer number = this.LADnoTripStarts.get(origin);
					if (number == null) number = 0;
					this.LADnoTripStarts.put(origin, ++number);
					//increase the number of trips ending at destination LAD
					Integer number2 = this.LADnoTripEnds.get(destination);
					if (number2 == null) number2 = 0;
					this.LADnoTripEnds.put(destination, ++number2);

					List listOfEdges = chosenRoute.getEdges();
					//System.out.println("The path as a list of edges: " + listOfEdges);
					//System.out.println("Path size in the number of edges: " + listOfEdges.size());

					double sum = 0;
					List<Route> list;
					for (Object o: listOfEdges) {
						DirectedEdge e = (DirectedEdge) o;
		
						//increase volume count (in PCU) for that edge
						Double volumeInPCU = linkVolumesInPCU.get(e.getID());
						if (volumeInPCU == null) volumeInPCU = 0.0;
						volumeInPCU++;
						linkVolumesInPCU.put(e.getID(), volumeInPCU);
						
						//increase volume count for that edge and for a car vehicle type
						Integer volume = linkVolumesPerVehicleType.get(VehicleType.CAR).get(e.getID());
						if (volume == null) volume = 0;
						volume++;
						linkVolumesPerVehicleType.get(VehicleType.CAR).put(e.getID(), volume);
					}

					//store path in path storage
					if (routeStorage.containsKey(origin, destination)) 
						list = (List<Route>) routeStorage.get(origin, destination);
					else {
						list = new ArrayList<Route>();
						routeStorage.put(origin, destination, list);
					}
					list.add(chosenRoute); //list.add(path);
		
			}//for each trip
		}//for each OD pair
	
		System.out.println("Total flow: " + counterTotalFlow);
		System.out.println("Total assigned trips: " + counterAssignedTrips);
		System.out.println("Assignment percentage: " + 100.0* counterAssignedTrips / counterTotalFlow);
	}

	/**
	 * Assigns freight origin-destination matrix to the road network.
	 * Zone ID ranges from the BYFM DfT model:
	 * <ul>
	 * 		<li>England: 1 - 867</li>
	 * 		<li>Wales: 901 - 922</li>
	 * 		<li>Scotland: 1001 - 1032</li>
	 * 		<li>Freight airports: 1111 - 1115</li>
	 * 		<li>Major distribution centres: 1201 - 1256</li>
	 * 		<li>Freight ports: 1301 - 1388</li>
	 * </ul>   
	 * @param freightODM Freight origin-destination matrix
	 */
	@SuppressWarnings("unused")
	public void assignFreightFlows(FreightMatrix freightMatrix) {


		System.out.println("Assigning the vehicle flows from the freight matrix...");

		//sort nodes based on the gravitating workplace population
		this.roadNetwork.sortGravityNodesFreight();

		//for each OD pair from the passengerODM		
		for (MultiKey mk: freightMatrix.getKeySet()) {
			//System.out.println(mk);
			//System.out.println("origin = " + mk.getKey(0));
			//System.out.println("destination = " + mk.getKey(1));
			//System.out.println("vehicle type = " + mk.getKey(2));
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicleType = (int) mk.getKey(2);

			//for each trip
			int flow = freightMatrix.getFlow(origin, destination, (int)mk.getKey(2));
			for (int i=0; i<flow; i++) {

				Integer originNode = null, destinationNode = null;

				/*
				//choose random start and end nodes
				if ((int)mk.getKey(0) <= 1032) { //origin freight zone is a LAD
					//choose random trip start node within the origin zone
					String originZone = roadNetwork.getFreightZoneToLAD().get((int)mk.getKey(0)); 
					List listOfOriginNodes = roadNetwork.getZoneToNodes().get(originZone);
					int numberOriginNodes = listOfOriginNodes.size();
					int indexOrigin = rng.nextInt(numberOriginNodes);
					originNode = (int) listOfOriginNodes.get(indexOrigin);
				} else {// freight zone is a point (port, airport or distribution centre)
					originNode = roadNetwork.getFreightZoneToNearestNode().get((int)mk.getKey(0));
				}

				if ((int)mk.getKey(1) <= 1032) { //destination freight zone is a LAD
					//choose random trip end node within the destination zone
					String destinationZone = roadNetwork.getFreightZoneToLAD().get((int)mk.getKey(1)); 
					List listOfDestinationNodes = roadNetwork.getZoneToNodes().get(destinationZone);
					int numberDestinationNodes = listOfDestinationNodes.size();
					int indexDestination = rng.nextInt(numberDestinationNodes);
					destinationNode = (int) listOfDestinationNodes.get(indexDestination);
				} else {// freight zone is a point (port, airport or distribution centre)
					destinationNode = roadNetwork.getFreightZoneToNearestNode().get((int)mk.getKey(1));
				}
				 */

				/*
				//chose origin node based on the population in workplace zones
				if ((int)mk.getKey(0) <= 1032) { //origin freight zone is a LAD

					//choose origin workplace zone within the LAD
					String originZone = roadNetwork.getFreightZoneToLAD().get((int)mk.getKey(0)); 
					List<String> listOfOriginWorkplaceCodes = roadNetwork.getZoneToWorkplaceCodes().get(originZone);
					double cumulativeProbability = 0.0;
					String originWorkplaceCode = null;
					double random = rng.nextDouble();
					for (String workplaceCode: listOfOriginWorkplaceCodes) {
						cumulativeProbability += workplaceZoneProbabilities.get(workplaceCode);
						if (Double.compare(cumulativeProbability, random) > 0) {
							originWorkplaceCode = workplaceCode;
							break;
						}
					}
					if (originWorkplaceCode == null) System.err.println("Origin output area was not selected.");
					else { //increase the number of tips starting at originWorkplaceCode
						Integer number = this.workplaceZoneNoTripStarts.get(originWorkplaceCode);
						if (number == null) number = 0;
						this.workplaceZoneNoTripStarts.put(originWorkplaceCode, ++number);
					}

					//use the network node nearest to the workplace zone (population-weighted) centroid
					originNode = roadNetwork.getWorkplaceZoneToNearestNode().get(originWorkplaceCode);

				} else {// freight zone is a point (port, airport or distribution centre)
					originNode = roadNetwork.getFreightZoneToNearestNode().get((int)mk.getKey(0));
				}

				//choose destination node based on the population in workplace zones
				if ((int)mk.getKey(1) <= 1032) { //destination freight zone is a LAD

					//choose destination workplace zone within the LAD
					String destinationZone = roadNetwork.getFreightZoneToLAD().get((int)mk.getKey(1)); 
					List<String> listOfDestinationWorkplaceCodes = roadNetwork.getZoneToWorkplaceCodes().get(destinationZone);
					double cumulativeProbability = 0.0;
					String destinationWorkplaceCode = null;
					double random = rng.nextDouble();
					for (String workplaceCode: listOfDestinationWorkplaceCodes) {
						cumulativeProbability += workplaceZoneProbabilities.get(workplaceCode);
						if (Double.compare(cumulativeProbability, random) > 0) {
							destinationWorkplaceCode = workplaceCode;
							break;
						}
					}
					if (destinationWorkplaceCode == null) System.err.println("Destination output area was not selected.");
					else { //increase the number of tips starting at originWorkplaceCode
						Integer number = this.workplaceZoneNoTripEnds.get(destinationWorkplaceCode);
						if (number == null) number = 0;
						this.workplaceZoneNoTripEnds.put(destinationWorkplaceCode, ++number);
					}

					//use the network node nearest to the workplace zone (population-weighted) centroid
					destinationNode = roadNetwork.getWorkplaceZoneToNearestNode().get(destinationWorkplaceCode);

				} else {// freight zone is a point (port, airport or distribution centre)
					destinationNode = roadNetwork.getFreightZoneToNearestNode().get((int)mk.getKey(1));
				}
				 */

				//choose origin node based on the gravitating population
				if (origin <= 1032) { //origin freight zone is a LAD

					//choose origin node based on the gravitating population
					//the choice with replacement means that possibly: destination node = origin node
					//the choice without replacement means that destination node has to be different from origin node

					//map freight zone number to LAD code
					String originLAD = roadNetwork.getFreightZoneToLAD().get(origin);
					List<Integer> listOfOriginNodes = roadNetwork.getZoneToNodes().get(originLAD); //the list is already sorted
			
					//choose origin node
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
					for (int node: listOfOriginNodes) {
						cumulativeProbability += nodeProbabilitiesFreight.get(node);
						if (Double.compare(cumulativeProbability, random) > 0) {
							originNode = node;
							break;
						}
					}

				} else {// freight zone is a point (port, airport or distribution centre)
					originNode = roadNetwork.getFreightZoneToNearestNode().get(origin);
				}

				//choose destination node based on the gravitating population
				if (destination <= 1032) { //destination freight zone is a LAD

					//choose origin/destination nodes based on the gravitating population
					//the choice with replacement means that possibly: destination node = origin node
					//the choice without replacement means that destination node has to be different from origin node

					//map freight zone number to LAD code
					String destinationLAD = roadNetwork.getFreightZoneToLAD().get(destination);
					List<Integer> listOfDestinationNodes = roadNetwork.getZoneToNodes().get(destinationLAD); //the list is already sorted
					//choose origin node
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
					//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
					//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
					if (FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT == false && origin == destination) { //no replacement and intra-zonal trip
						for (int node: listOfDestinationNodes) {
							if (node == originNode.intValue()) continue; //skip if the node is the same as origin
							cumulativeProbability += nodeProbabilitiesFreight.get(node) / (1.0 - nodeProbabilitiesFreight.get(originNode));
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = node;
								break;
							}
						}
					} else	{ //inter-zonal trip (or intra-zonal with replacement)
						for (int node: listOfDestinationNodes) {
							cumulativeProbability += nodeProbabilitiesFreight.get(node);
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = node;
								break;
							}
						}
					}

				} else {// freight zone is a point (port, airport or distribution centre)
					destinationNode = roadNetwork.getFreightZoneToNearestNode().get(destination);
				}

				if (originNode == null) System.err.println("Could not find origin node for a freight trip!");
				if (destinationNode == null) System.err.println("Could not find destination node for a freight trip!");
								
				DirectedGraph rn = roadNetwork.getNetwork();
				//set source and destination node
				Node from = roadNetwork.getNodeIDtoNode().get(originNode);
				Node to = roadNetwork.getNodeIDtoNode().get(destinationNode);
				//System.out.println("from " + from + " to " + to);
				Path foundPath = null;
				try {

					VehicleType vht = VehicleType.values()[vehicleType];
					//see if that path from node 'from' to node 'to' already exists in the path storage for that vehicle type
					if (pathStorageFreight.get(vht).containsKey(origin, destination)) { 
						List<Path> list = (List<Path>) pathStorageFreight.get(vht).get(origin, destination);
						for (Path p: list) {
							//if (p.getFirst().equals(from) && p.getLast().equals(to)) {
							if (p.getFirst().getID() == from.getID() && p.getLast().getID() == to.getID()) {
								foundPath = p;
								break;
							}
						}
					}

					//if path does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundPath == null) {
						//System.out.println("The path does not exist in the path storage");
						AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctionsTime(to, this.linkTravelTime));
						//AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
						aStarPathFinder.calculate();
						Path aStarPath;
						aStarPath = aStarPathFinder.getPath();
						aStarPath.reverse();
						//System.out.println(aStarPath);
						//System.out.println("The path as a list of nodes: " + aStarPath);

						foundPath = aStarPath;
					}

					//check to which LAD chosen origin and destination nodes belong to!
					String originLAD = roadNetwork.getNodeToZone().get(originNode);
					String destinationLAD = roadNetwork.getNodeToZone().get(destinationNode);
		
					//increase the number of freight trips starting at origin LAD
					Integer number = this.LADnoTripStartsFreight.get(originLAD);
					if (number == null) number = 0;
					this.LADnoTripStartsFreight.put(originLAD, ++number);
					//increase the number of trips ending at destination LAD
					Integer number2 = this.LADnoTripEndsFreight.get(destinationLAD);
					if (number2 == null) number2 = 0;
					this.LADnoTripEndsFreight.put(destinationLAD, ++number2);

					List listOfEdges = foundPath.getEdges();
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

						//increase volume count for that edge (in PCU)
						Double volumeInPCU = linkVolumesInPCU.get(e.getID());
						if (volumeInPCU == null) volumeInPCU = 0.0;
						volumeInPCU += this.vehicleTypeToPCU.get(vht);
						linkVolumesInPCU.put(e.getID(), volumeInPCU);
						
						//increase volume count for that edge and for that vehicle type
						Integer volume = linkVolumesPerVehicleType.get(vht).get(e.getID());
						if (volume == null) volume = 0;
						volume++;
						linkVolumesPerVehicleType.get(vht).put(e.getID(), volume);
					}
					//System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

					//store path in path storage
					if (pathStorageFreight.get(vht).containsKey(origin, destination)) 
						list = (List<Path>) pathStorageFreight.get(vht).get(origin, destination);
					else {
						list = new ArrayList<Path>();
						pathStorageFreight.get(vht).put(origin, destination, list);
					}
					list.add(foundPath);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}//for each trip
		}//for each OD pair
	}

	/**
	 * Updates link travel times.
	 */
	public void updateLinkTravelTimes() {

		double congestedTravelTime;

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		HashMap<Integer, Integer> numberOfLanes = roadNetwork.getNumberOfLanes();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();

			double linkVolumeInPCU;
			if (linkVolumesInPCU.get(edge.getID()) == null) linkVolumeInPCU = 0.0;
			else linkVolumeInPCU = linkVolumesInPCU.get(edge.getID());

			String roadNumber = (String) sf.getAttribute("RoadNumber");
			double roadLength = (double) sf.getAttribute("LenNet"); //[km]
			
			/*
			//Bureau of Public Roads (1964) formulation
			if (roadNumber.charAt(0) == 'M') //motorway
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID())*(1 + ALPHA * Math.pow(PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID()) / MAXIMUM_CAPACITY_M_ROAD, BETA_M_ROAD));
			else if (roadNumber.charAt(0) == 'A') //A-road
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID())*(1 + ALPHA * Math.pow(PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID()) / MAXIMUM_CAPACITY_A_ROAD, BETA_A_ROAD));
			else //ferry
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID());
			*/
		
			//Speed-flow curves from FORGE (Department for Transport, 2005)
			if (roadNumber.charAt(0) == 'M') {//motorway
				
				double flow = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID());
				double speed = 0.0;
				if (flow < 1398) speed = ((69.96 - 71.95) / 1398 * flow + 71.95) * 1.609344; //[kph]
				else 			 speed = ((34.55 - 69.96) / (2330 - 1398) * (flow - 1398) + 69.96) * 1.609344; //[kph]
				
				if (speed <= 0.0) {
					System.err.println("Speed is not positive!");
					speed = 34.55; //constraint speed
				}
				congestedTravelTime = roadLength / speed * 60; //[min]
						
			} else if (roadNumber.charAt(0) == 'A') {//A-road
				
				double flow = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID());
				double speed = 0.0;
				if (flow < 1251) speed = ((50.14 - 56.05) / 1251 * flow + 56.05) * 1.609344; //[kph]
				else 			 speed = ((27.22 - 50.14) / (1740 - 1251) * (flow - 1251) + 50.14) * 1.609344; //[kph]
				
				if (speed <= 0.0) {
					System.err.println("Speed is not positive!");
					speed = 27.22; //constraint speed
				}
				congestedTravelTime = roadLength / speed * 60;
				
			} else //ferry
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID());
				
			linkTravelTime.put(edge.getID(), congestedTravelTime);
		}
	}

	/**
	 * Updates link travel times using weighted averaging between new values (calculated from link volumes) 
	 * and older values (stored in the instance field).
	 * @param weight Parameter for weighted averaging.
	 */
	public void updateLinkTravelTimes(double weight) {

		double congestedTravelTime;

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		HashMap<Integer, Integer> numberOfLanes = roadNetwork.getNumberOfLanes();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();

			double linkVolumeInPCU;
			if (linkVolumesInPCU.get(edge.getID()) == null) linkVolumeInPCU = 0.0;
			else linkVolumeInPCU = linkVolumesInPCU.get(edge.getID());

			String roadNumber = (String) sf.getAttribute("RoadNumber");
			double roadLength = (double) sf.getAttribute("LenNet"); //[km]
			
			/*
			//Bureau of Public Roads (1964) formulation
			if (roadNumber.charAt(0) == 'M') //motorway
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID())*(1 + ALPHA * Math.pow(PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID()) / MAXIMUM_CAPACITY_M_ROAD, BETA_M_ROAD));
			else if (roadNumber.charAt(0) == 'A') //A-road
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID())*(1 + ALPHA * Math.pow(PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID()) / MAXIMUM_CAPACITY_A_ROAD, BETA_A_ROAD));
			else //ferry
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID());
			*/
		
			//Speed-flow curves from FORGE (Department for Transport, 2005)
			if (roadNumber.charAt(0) == 'M') {//motorway
				
				double flow = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID());
				double speed = 0.0;
				if (flow < 1398) speed = ((69.96 - 71.95) / 1398 * flow + 71.95) * 1.609344; //[kph]
				else 			 speed = ((34.55 - 69.96) / (2330 - 1398) * (flow - 1398) + 69.96) * 1.609344; //[kph]
				
				if (speed <= 0.0) {
					System.err.println("Speed is not positive!");
					speed = 34.55; //constraint speed
				}
				congestedTravelTime = roadLength / speed * 60; //[min]
						
			} else if (roadNumber.charAt(0) == 'A') {//A-road
				
				double flow = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID());
				double speed = 0.0;
				if (flow < 1251) speed = ((50.14 - 56.05) / 1251 * flow + 56.05) * 1.609344; //[kph]
				else 			 speed = ((27.22 - 50.14) / (1740 - 1251) * (flow - 1251) + 50.14) * 1.609344; //[kph]
				
				if (speed <= 0.0) {
					System.err.println("Speed is not positive!");
					speed = 27.22; //constraint speed
				}
				congestedTravelTime = roadLength / speed * 60;
				
			} else //ferry
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID());
		
			//average with the old value (currently stored in the linkTravelTime field)
			Double oldLinkTravelTime = linkTravelTime.get(edge.getID());
			
			if (oldLinkTravelTime == null) {
				int nodeA = this.roadNetwork.getEdgeIDtoEdge().get(edge.getID()).getNodeA().getID();
				int nodeB = this.roadNetwork.getEdgeIDtoEdge().get(edge.getID()).getNodeB().getID();
				System.err.printf("There is not link travel time for edge %d from node %d to node %d.\n", edge.getID(), nodeA, nodeB);
				//calculate free flow travel time for that edge
				SimpleFeature feature = (SimpleFeature) edge.getObject();
				if (roadNumber.charAt(0) == 'M') //motorway
					oldLinkTravelTime = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.FREE_FLOW_SPEED_M_ROAD * 60;  //travel time in minutes
				else if (roadNumber.charAt(0) == 'A') //A road
					oldLinkTravelTime = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.FREE_FLOW_SPEED_A_ROAD * 60;  //travel time in minutes
				else //ferry
					oldLinkTravelTime = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.AVERAGE_SPEED_FERRY * 60;  //travel time in minutes
				}
				
			//congestedTravelTime = weight * congestedTravelTime + (1 - weight) * linkTravelTime.get(edge.getID());
			congestedTravelTime = weight * congestedTravelTime + (1 - weight) * oldLinkTravelTime;

			linkTravelTime.put(edge.getID(), congestedTravelTime);
		}
	}


	/** 
	 * Assigns passenger and freight origin-destination matrix to the road network
	 * using the fastest path based on the current values in the linkTravelTime field.
	 * Then updates link travel times using weighted averaging.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param weight Weighting parameter.
	 */
	public void assignFlowsAndUpdateLinkTravelTimes(ODMatrix passengerODM, FreightMatrix freightODM, double weight) {

		this.assignPassengerFlows(passengerODM);
		this.assignFreightFlows(freightODM);
		this.updateLinkTravelTimes(weight);
	}

	/** 
	 * Iterates assignment and travel time update a fixed number of times.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param weight Weighting parameter.
	 * @param iterations Number of iterations.
	 */
	public void assignFlowsAndUpdateLinkTravelTimesIterated(ODMatrix passengerODM, FreightMatrix freightODM, double weight, int iterations) {

		for (int i=0; i<iterations; i++) {
			this.resetLinkVolumes(); //link volumes must be reset or they would compound across all iterations
			this.resetPathStorages(); //clear path storages
			this.resetTripStartEndCounters(); //reset counters
			this.assignFlowsAndUpdateLinkTravelTimes(passengerODM, freightODM, weight);
		}
	}

	/**
	 * Updates travel time skim matrix (zone-to-zone travel times).
	 * @param timeSkimMatrix Inter-zonal skim matrix (time).
	 */
	public void updateTimeSkimMatrix(SkimMatrix timeSkimMatrix) {

		//for each OD pair
		for (MultiKey mk: pathStorage.keySet()) {
			//System.out.println(mk);
			String originZone = (String) mk.getKey(0);
			String destinationZone = (String) mk.getKey(1);

			List<Path> pathList = pathStorage.get(originZone, destinationZone);
			double totalODtravelTime = 0.0;
			//for each path in the path list calculate total travel time
			for (Path path: pathList) {

				double pathTravelTime = 0.0;
				for (Object o: path.getEdges()) {
					Edge e = (Edge)o;
					pathTravelTime += linkTravelTime.get(e.getID());					
				}
				//System.out.printf("Path travel time: %.3f\n", pathTravelTime);
				totalODtravelTime += pathTravelTime;

				//add average access and egress time to the first and the last node [m -> km] [h -> s]
				double averageAccessDistance = this.roadNetwork.getAverageAcessEgressDistance(path.getFirst().getID()) / 1000;
				double averageEgressDistance = this.roadNetwork.getAverageAcessEgressDistance(path.getLast().getID()) / 1000;
				double averageAccessTime = averageAccessDistance /  AVERAGE_ACCESS_EGRESS_SPEED_CAR * 60;
				double averageEgressTime = averageEgressDistance /  AVERAGE_ACCESS_EGRESS_SPEED_CAR * 60;
				//System.out.printf("Acess time: %.3f min Egress time: %.3f min\n", averageAccessTime, averageEgressTime);
				//System.out.printf("Path travel time with access/egress: %.3f\n", pathTravelTime + averageAccessTime + averageEgressTime);
				totalODtravelTime += (averageAccessTime + averageEgressTime);

			}
			double averageODtravelTime = totalODtravelTime / pathList.size();
			//System.out.printf("Average OD travel time: %.3f min\n", averageODtravelTime);
			//update time skim matrix
			timeSkimMatrix.setCost((String)mk.getKey(0), (String)mk.getKey(1), averageODtravelTime);
		}
	}

	/**
	 * Calculated travel time skim matrix (zone-to-zone travel times).
	 * @return Inter-zonal skim matrix (time).
	 */
	public SkimMatrix calculateTimeSkimMatrix() {

		SkimMatrix timeSkimMatrix = new SkimMatrix();
		this.updateTimeSkimMatrix(timeSkimMatrix);

		return timeSkimMatrix;
	}

	/**
	 * Updates travel time skim matrix (zone-to-zone travel times) for freight.
	 * @param timeSkimMatrixFreight Inter-zonal skim matrix (time).
	 */
	public void updateTimeSkimMatrixFreight(SkimMatrixFreight timeSkimMatrixFreight) {

		//for each vehicle type
		for (VehicleType vht: pathStorageFreight.keySet())
			//for each OD pair that is stored in the path storage
			for (MultiKey mk: pathStorageFreight.get(vht).keySet()) {
				//System.out.println(mk);
				int originZone = (int) mk.getKey(0);
				int destinationZone = (int) mk.getKey(1);

				List<Path> pathList = pathStorageFreight.get(vht).get(originZone, destinationZone);
				double totalODtravelTime = 0.0;
				//for each path in the path list calculate total travel time
				for (Path path: pathList) {

					double pathTravelTime = 0.0;
					for (Object o: path.getEdges()) {
						Edge e = (Edge)o;
						pathTravelTime += linkTravelTime.get(e.getID());					
					}
					//System.out.printf("Path travel time: %.3f\n", pathTravelTime);
					totalODtravelTime += pathTravelTime;

					//add average access and egress time to the first and the last node [m -> km] [h -> min]
					double averageAccessDistance = this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getFirst().getID()) / 1000;
					double averageEgressDistance = this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getLast().getID()) / 1000;
					totalODtravelTime += (averageAccessDistance + averageEgressDistance) / AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT * 60;
				}
				double averageODtravelTime = totalODtravelTime / pathList.size();
				//System.out.printf("Average OD travel time: %.3f min\n", averageODtravelTime);
				//update time skim matrix
				timeSkimMatrixFreight.setCost(originZone, destinationZone, vht.value, averageODtravelTime);
			}
	}

	/**
	 * Calculated travel time skim matrix (zone-to-zone travel times) for freight.
	 * @return Inter-zonal skim matrix (time).
	 */
	public SkimMatrixFreight calculateTimeSkimMatrixFreight() {

		SkimMatrixFreight timeSkimMatrixFreight = new SkimMatrixFreight();
		this.updateTimeSkimMatrixFreight(timeSkimMatrixFreight);

		return timeSkimMatrixFreight;
	}

	/**
	 * Updates cost skim matrix (zone-to-zone financial costs).
	 * @param costSkimMatrix Inter-zonal skim matrix (cost).
	 */
	public void updateCostSkimMatrix(SkimMatrix costSkimMatrix) {

		//for each OD pair
		for (MultiKey mk: pathStorage.keySet()) {
			//System.out.println(mk);
			String originZone = (String) mk.getKey(0);
			String destinationZone = (String) mk.getKey(1);

			List<Path> pathList = pathStorage.get(originZone, destinationZone);
			double totalODdistance = 0.0;
			//for each path in the path list calculate total distance
			for (Path path: pathList) {

				double pathLength = 0.0;
				for (Object o: path.getEdges()) {
					Edge e = (Edge)o;
					SimpleFeature sf = (SimpleFeature) e.getObject();
					double length = (double) sf.getAttribute("LenNet");
					pathLength += length;					
				}
				//System.out.printf("Path length: %.3f\n\n", pathLength);
				totalODdistance += pathLength;

				//add average access and egress distance to the first and the last node [m -> km!]
				totalODdistance += this.roadNetwork.getAverageAcessEgressDistance(path.getFirst().getID()) / 1000;
				totalODdistance += this.roadNetwork.getAverageAcessEgressDistance(path.getLast().getID()) / 1000;
			}
			double averageODdistance = totalODdistance / pathList.size();
			double energyCost = 0.0;
			//iterate over engine types
			for (EngineType engine: EngineType.values())
				energyCost += averageODdistance / 100 * engineTypeFractions.get(engine) * energyConsumptionsPer100km.get(engine) * energyUnitCosts.get(engine);

			//System.out.printf("Average OD distance: %.3f km\t Fuel cost: %.2f GBP\n", averageODdistance, energyCost);
			//update cost skim matrix
			costSkimMatrix.setCost(originZone, destinationZone, energyCost);
		}
	}

	/**
	 * Calculates cost skim matrix (zone-to-zone financial costs).
	 * @return Inter-zonal skim matrix (cost).
	 */
	public SkimMatrix calculateCostSkimMatrix() {

		SkimMatrix costSkimMatrix = new SkimMatrix();
		this.updateCostSkimMatrix(costSkimMatrix);

		return costSkimMatrix;
	}

	/**
	 * Updates cost skim matrix (zone-to-zone distances).
	 * @return Inter-zonal skim matrix (distance).
	 */
	public SkimMatrix calculateDistanceSkimMatrix() {

		SkimMatrix distanceSkimMatrix = new SkimMatrix();

		//for each OD pair
		for (MultiKey mk: pathStorage.keySet()) {
			//System.out.println(mk);
			String originZone = (String) mk.getKey(0);
			String destinationZone = (String) mk.getKey(1);

			List<Path> pathList = pathStorage.get(originZone, destinationZone);
			double totalODdistance = 0.0;
			//for each path in the path list calculate total distance
			for (Path path: pathList) {

				double pathLength = 0.0;
				for (Object o: path.getEdges()) {
					Edge e = (Edge)o;
					SimpleFeature sf = (SimpleFeature) e.getObject();
					double length = (double) sf.getAttribute("LenNet");
					pathLength += length;					
				}
				//System.out.printf("Path length: %.3f\n", pathLength);
				totalODdistance += pathLength;

				//add average access and egress distance to the first and the last node [m -> km!]
				double accessDistance = this.roadNetwork.getAverageAcessEgressDistance(path.getFirst().getID()) / 1000;
				double egressDistance = this.roadNetwork.getAverageAcessEgressDistance(path.getLast().getID()) / 1000;
				//System.out.printf("Acess: %.3f Egress: %.3f\n ", accessDistance, egressDistance);
				//System.out.printf("Path length with access and egress: %.3f km\n", pathLength + accessDistance + egressDistance);
				totalODdistance += (accessDistance + egressDistance);
			}
			double averageODdistance = totalODdistance / pathList.size();

			//System.out.printf("Average OD distance: %.3f km\n", averageODdistance);
			//update distance skim matrix
			distanceSkimMatrix.setCost(originZone, destinationZone, averageODdistance);
		}

		return distanceSkimMatrix;
	}

	/**
	 * Updates cost skim matrix (zone-to-zone distances) for freight.
	 * @return Inter-zonal skim matrix (distance).
	 */
	public SkimMatrix calculateDistanceSkimMatrixFreight() {

		SkimMatrix distanceSkimMatrixFreight = new SkimMatrix();

		//for each vehicle type
		for (VehicleType vht: pathStorageFreight.keySet())
			//for each OD pair that is stored in the path storage
			for (MultiKey mk: pathStorageFreight.get(vht).keySet()) {
				//System.out.println(mk);
				String originZone = (String) mk.getKey(0);
				String destinationZone = (String) mk.getKey(1);

				List<Path> pathList = pathStorageFreight.get(vht).get(originZone, destinationZone);
				double totalODdistance = 0.0;
				//for each path in the path list calculate total distance
				for (Path path: pathList) {
					double pathLength = 0.0;
					for (Object o: path.getEdges()) {
						Edge e = (Edge)o;
						SimpleFeature sf = (SimpleFeature) e.getObject();
						double length = (double) sf.getAttribute("LenNet");
						pathLength += length;					
					}
				//System.out.printf("Path length: %.3f\n", pathLength);
				totalODdistance += pathLength;
				//add average access and egress distance to the first and the last node [m -> km!]
				double accessDistance = this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getFirst().getID()) / 1000;
				double egressDistance = this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getLast().getID()) / 1000;
				//System.out.printf("Acess: %.3f Egress: %.3f\n ", accessDistance, egressDistance);
				//System.out.printf("Path length with access and egress: %.3f km\n", pathLength + accessDistance + egressDistance);
				totalODdistance += (accessDistance + egressDistance);
			}
			double averageODdistance = totalODdistance / pathList.size();

			//System.out.printf("Average OD distance: %.3f km\n", averageODdistance);
			//update distance skim matrix
			distanceSkimMatrixFreight.setCost(originZone, destinationZone, averageODdistance);
		}

		return distanceSkimMatrixFreight;
	}

	
	/**
	 * Updates cost skim matrix (zone-to-zone financial costs) for freight.
	 * @param costSkimMatrix Inter-zonal skim matrix (cost).
	 */
	public void updateCostSkimMatrixFreight(SkimMatrixFreight costSkimMatrixFreight) {

		//for each vehicle type
		for (VehicleType vht: pathStorageFreight.keySet())
			//for each OD pair
			for (MultiKey mk: pathStorageFreight.get(vht).keySet()) {
				//System.out.println(mk);
				int originZone = (int) mk.getKey(0);
				int destinationZone = (int) mk.getKey(1);

				List<Path> pathList = pathStorageFreight.get(vht).get(originZone, destinationZone);
				double totalODdistance = 0.0;
				//for each path in the path list calculate total distance
				for (Path path: pathList) {

					double pathLength = 0.0;
					for (Object o: path.getEdges()) {
						Edge e = (Edge)o;
						SimpleFeature sf = (SimpleFeature) e.getObject();
						double length = (double) sf.getAttribute("LenNet");
						pathLength += length;					
					}
					//System.out.printf("Path length: %.3f\n\n", pathLength);
					totalODdistance += pathLength;
					
					//add average access and egress distance to the first and the last node [m -> km!]
					totalODdistance += this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getFirst().getID()) / 1000;
					totalODdistance += this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getLast().getID()) / 1000;
				}
				double averageODdistance = totalODdistance / pathList.size();
				double energyCost = 0.0;
				//iterate over engine types
				for (EngineType engine: EngineType.values())
					energyCost += averageODdistance / 100 * engineTypeFractions.get(engine) * energyConsumptionsPer100km.get(engine) * energyUnitCosts.get(engine);

				//System.out.printf("Average OD distance: %.3f km\t Fuel cost: %.2f GBP\n", averageODdistance, energyCost);
				//update cost skim matrix
				costSkimMatrixFreight.setCost(originZone, destinationZone, vht.value, energyCost);
			}
	}

	/**
	 * Calculates cost skim matrix (zone-to-zone financial costs) for freight.
	 * @return Inter-zonal skim matrix (cost).
	 */
	public SkimMatrixFreight calculateCostSkimMatrixFreight() {

		SkimMatrixFreight costSkimMatrixFreight = new SkimMatrixFreight();
		this.updateCostSkimMatrixFreight(costSkimMatrixFreight);

		return costSkimMatrixFreight;
	}

	/**
	 * Calculates total energy consumption for each car engine type (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each engine type.
	 */
	public HashMap<EngineType, Double> calculateCarEnergyConsumptions() {

		double totalDistance = 0.0;
		//for each path in the storage
		for (List<Path> pathList: pathStorage.values()) {
			//for each path in the path list calculate total distance
			for (Path path: pathList) { 
				for (Object o: path.getEdges()) {
					Edge e = (Edge)o;
					SimpleFeature sf = (SimpleFeature) e.getObject();
					double length = (double) sf.getAttribute("LenNet");
					totalDistance += length;		
				}
				//add average access and egress distance to the first and the last node [m -> km!]
				double accessDistance = this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getFirst().getID()) / 1000;
				double egressDistance = this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getLast().getID()) / 1000;
				//System.out.printf("Access: %.3f Egress: %.3f\n ", accessDistance, egressDistance);
				//System.out.printf("Path length with access and egress: %.3f km\n", pathLength + accessDistance + egressDistance);
				totalDistance += (accessDistance + egressDistance);
			}
		}
		System.out.printf("Total path distance (car): %.3f km\n", totalDistance);

		HashMap<EngineType, Double> consumptions = new HashMap<EngineType, Double>();
		for (EngineType engine: EngineType.values()) {
			double consumption = totalDistance / 100 * engineTypeFractions.get(engine) * energyConsumptionsPer100km.get(engine);
			consumptions.put(engine, consumption);
		}
		return consumptions;
	}

	/**
	 * Calculates total energy consumption for each freight vehicle engine type (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each engine type.
	 */
	public HashMap<EngineType, Double> calculateFreightEnergyConsumptions() {

		double totalDistance = 0.0;

		//for each vehicle type
		for (VehicleType vht: pathStorageFreight.keySet()) {
			//for each path in the storage
			for (List<Path> pathList: pathStorageFreight.get(vht).values()) {
				//for each path in the path list calculate total distance
				for (Path path: pathList) {
					for (Object o: path.getEdges()) {
						Edge e = (Edge)o;
						SimpleFeature sf = (SimpleFeature) e.getObject();
						double length = (double) sf.getAttribute("LenNet");
						totalDistance += length;					
					}
					//add average access and egress distance to the first and the last node [m -> km!]
					totalDistance += this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getFirst().getID()) / 1000;
					totalDistance += this.roadNetwork.getAverageAcessEgressDistanceFreight(path.getLast().getID()) / 1000;
				}
			}
		}
		System.out.printf("Total path distance (freight): %.3f km\n", totalDistance);

		HashMap<EngineType, Double> consumptions = new HashMap<EngineType, Double>();
		for (EngineType engine: EngineType.values()) {
			double consumption = totalDistance / 100 * engineTypeFractions.get(engine) * energyConsumptionsPer100km.get(engine);
			consumptions.put(engine, consumption);
		}
		return consumptions;
	}

	/**
	 * Calculates total energy consumption for each engine type of passenger cars and freight vehicles (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each engine type.
	 */
	public HashMap<EngineType, Double> calculateEnergyConsumptions() {

		HashMap<EngineType, Double> car = calculateCarEnergyConsumptions();
		HashMap<EngineType, Double> freight = calculateFreightEnergyConsumptions();
		HashMap<EngineType, Double> combined = new HashMap<EngineType, Double>();

		for (EngineType engine: EngineType.values()) {
			double consumption = car.get(engine) + freight.get(engine);
			combined.put(engine, consumption);
		}
		return combined;
	}

	/**
	 * Calculate peak-hour link point capacities (PCU/lane/hr).
	 * @return Peak-hour link point capacities.
	 */
	public HashMap<Integer, Double> calculatePeakLinkPointCapacities() {

		HashMap<Integer, Double> linkPointCapacities = new HashMap<Integer, Double>();
		HashMap<Integer, Integer> numberOfLanes = roadNetwork.getNumberOfLanes();

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			double linkVolumeInPCU = 0.0;
			if (linkVolumesInPCU.get(edge.getID()) != null) linkVolumeInPCU = linkVolumesInPCU.get(edge.getID());
			double capacity = 0.0;
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			if (roadNumber.charAt(0) == 'M') //motorway
				capacity = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID());
			else if (roadNumber.charAt(0) == 'A') //A-road
				capacity = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / numberOfLanes.get(edge.getID());
			else //ferry
				capacity = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU;

			linkPointCapacities.put(edge.getID(), capacity);
		}
		return linkPointCapacities;
	}
	
	/**
	 * Calculate peak-hour link point capacities (PCU/lane/hr) averaged by two directions.
	 * @return Peak-hour link point capacities.
	 */
	private HashMap<Integer, Double> calculateAveragePeakLinkPointCapacities() {
		
		HashMap<Integer, Double> capacities = this.calculatePeakLinkPointCapacities();
		HashMap<Integer, Double> averagedCapacities = new HashMap<Integer, Double>();
	
		for (Integer edgeID: capacities.keySet()) {
			double capacity1 = capacities.get(edgeID);
			Integer otherEdgeID = roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edgeID);
			if (otherEdgeID == null) averagedCapacities.put(edgeID, capacity1); //if just one direction, copy value
			else { //otherwise, store average for both directions
				Double capacity2 = capacities.get(otherEdgeID);
				averagedCapacities.put(edgeID, (capacity1 + capacity2) / 2.0);
				averagedCapacities.put(otherEdgeID, (capacity1 + capacity2) / 2.0);
			}
		}
		
		return averagedCapacities;
	}
	
	/**
	 * Calculate peak-hour link point capacities (PCU/lane/hr) maximum of the two directions.
	 * @return Peak-hour link point capacities.
	 */
	private HashMap<Integer, Double> calculateMaximumPeakLinkPointCapacities() {
		
		HashMap<Integer, Double> capacities = this.calculatePeakLinkPointCapacities();
		HashMap<Integer, Double> maximumCapacities = new HashMap<Integer, Double>();
	
		for (Integer edgeID: capacities.keySet()) {
			double capacity1 = capacities.get(edgeID);
			Integer otherEdgeID = roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edgeID);
			if (otherEdgeID == null) maximumCapacities.put(edgeID, capacity1); //if just one direction, copy value
			else { //otherwise, store maximum of both directions
				double capacity2 = capacities.get(otherEdgeID);
				double maxCapacity = 0.0;
				if (capacity1 > capacity2) maxCapacity = capacity1;
				else 					   maxCapacity = capacity2;
				
				maximumCapacities.put(edgeID, maxCapacity);
				maximumCapacities.put(otherEdgeID, maxCapacity);
			}
		}
		
		return maximumCapacities;
	}

	/**
	 * Calculate peak-hour link densities (PCU/lane/km/hr)
	 */
	public HashMap<Integer, Double> calculatePeakLinkDensities() {

		HashMap<Integer, Double> linkDensities = new HashMap<Integer, Double>();
		HashMap<Integer, Integer> numberOfLanes = roadNetwork.getNumberOfLanes();

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			double linkVol = 0.0;
			if (linkVolumesInPCU.get(edge.getID()) != null) linkVol = linkVolumesInPCU.get(edge.getID());
			double density = 0.0;
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			double length = (double) sf.getAttribute("LenNet");
			if (roadNumber.charAt(0) == 'M') //motorway
				density = PEAK_HOUR_PERCENTAGE * linkVol / numberOfLanes.get(edge.getID()) / length;
			else if (roadNumber.charAt(0) == 'A') //A-road
				density = PEAK_HOUR_PERCENTAGE * linkVol / numberOfLanes.get(edge.getID()) / length;
			else //ferry
				density = PEAK_HOUR_PERCENTAGE * linkVol / length;

			linkDensities.put(edge.getID(), density);
		}
		return linkDensities;
	}

	/**
	 * @return The sum of all link travel times in the network.
	 */
	public double getTotalLinkTravelTimes() {

		double totalTravelTime = 0.0;
		for (Integer key: this.linkTravelTime.keySet()) totalTravelTime += linkTravelTime.get(key);

		return totalTravelTime;
	}

	/**
	 * @return The copy of all link travel times.
	 */
	public HashMap<Integer, Double> getCopyOfLinkTravelTimes() {

		HashMap<Integer, Double> linkTravelTimes = new HashMap<Integer, Double>();
		for (Integer key: this.linkTravelTime.keySet()) linkTravelTimes.put(key, this.linkTravelTime.get(key));

		return linkTravelTimes;
	}


	/**
	 * Calculates the sum of absolute differences in link travel times.
	 * @param other Link travel times to compare with.
	 * @return Sum of absolute differences in link travel times.
	 */
	public double getAbsoluteDifferenceInLinkTravelTimes(HashMap<Integer, Double> other) {

		double difference = 0.0;
		for (Integer key: this.linkTravelTime.keySet())
			difference += Math.abs(this.linkTravelTime.get(key) - other.get(key));

		return difference;
	}

	/**
	 * Saves assignment results to output file.
	 * @param year
	 * @param outputFile
	 */
	public void saveAssignmentResults(int year, String outputFile) {

		//calculate peak capacities and densities
		HashMap<Integer, Double> capacities = this.calculatePeakLinkPointCapacities();
		HashMap<Integer, Double> averageCapacities = this.calculateAveragePeakLinkPointCapacities();
		HashMap<Integer, Double> maximumCapacities = this.calculateMaximumPeakLinkPointCapacities();
		HashMap<Integer, Double> densities = this.calculatePeakLinkDensities();

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("edgeID");
		header.add("roadNumber");
		header.add("freeFlowTravelTime");
		header.add("travelTime");
		header.add("linkVolumeCar");
		header.add("linkVolumeVan");
		header.add("linkVolumeRigid");
		header.add("linkVolumeArtic");
		header.add("linkVolumeInPCU");
		header.add("peakCapacity");
		header.add("peakDensity");
		header.add("maxCapacity");
		header.add("utilisation");
		header.add("AVGutilisation");
		header.add("MAXutilisation");		
		header.add("CP");
		header.add("direction");
		header.add("countCar");
		header.add("countVan");
		header.add("countRigid");
		header.add("countArtic");
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			Iterator<DirectedEdge> iter = (Iterator<DirectedEdge>) this.roadNetwork.getNetwork().getEdges().iterator();
			while (iter.hasNext()) {
				DirectedEdge edge = (DirectedEdge) iter.next();
				record.clear();
				record.add(Integer.toString(year));
				record.add(Integer.toString(edge.getID()));
				SimpleFeature feature = (SimpleFeature)edge.getObject();
				String roadNumber = (String) feature.getAttribute("RoadNumber");
				record.add(roadNumber);
				record.add(Double.toString(this.linkFreeFlowTravelTime.get(edge.getID())));
				record.add(Double.toString(this.linkTravelTime.get(edge.getID())));
				Integer linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge.getID());
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.VAN).get(edge.getID());
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.RIGID).get(edge.getID());
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC).get(edge.getID());
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				Double linkVolumePCU = this.linkVolumesInPCU.get(edge.getID());
				if (linkVolumePCU == null) record.add(Double.toString(0.0));
				else					   record.add(Double.toString(linkVolumePCU));
				record.add(Double.toString(capacities.get(edge.getID())));
				record.add(Double.toString(densities.get(edge.getID())));
				//get max capacity from road type
				if (roadNumber.charAt(0) == 'M') { //motorway
					record.add(Integer.toString(RoadNetworkAssignment.MAXIMUM_CAPACITY_M_ROAD));
					double utilisation = capacities.get(edge.getID()) / RoadNetworkAssignment.MAXIMUM_CAPACITY_M_ROAD;
					record.add(Double.toString(utilisation));
					double averageUtilisation = averageCapacities.get(edge.getID()) / RoadNetworkAssignment.MAXIMUM_CAPACITY_M_ROAD;
					record.add(Double.toString(averageUtilisation));
					double maximumUtilisation = maximumCapacities.get(edge.getID()) / RoadNetworkAssignment.MAXIMUM_CAPACITY_M_ROAD;
					record.add(Double.toString(maximumUtilisation));
				}	
				else if (roadNumber.charAt(0) == 'A') { //A road
						record.add(Integer.toString(RoadNetworkAssignment.MAXIMUM_CAPACITY_A_ROAD));
						double utilisation = capacities.get(edge.getID()) / RoadNetworkAssignment.MAXIMUM_CAPACITY_A_ROAD;
						record.add(Double.toString(utilisation));
						double averageUtilisation = averageCapacities.get(edge.getID()) / RoadNetworkAssignment.MAXIMUM_CAPACITY_A_ROAD;
						record.add(Double.toString(averageUtilisation));
						double maximumUtilisation = maximumCapacities.get(edge.getID()) / RoadNetworkAssignment.MAXIMUM_CAPACITY_A_ROAD;
						record.add(Double.toString(maximumUtilisation));
				}
				else { //ferry
					record.add("N/A");
					record.add("N/A");
					record.add("N/A");
					record.add("N/A");
				}
				Object countPointObject = feature.getAttribute("CP");
				long countPoint;
				if (countPointObject instanceof Double) countPoint = (long) Math.round((double)countPointObject);
				else countPoint = (long) countPointObject;
				record.add(Long.toString(countPoint));
				if (countPoint != 0) { //not a ferry nor a newly developed road with no count point
					String direction = (String) feature.getAttribute("iDir");
					record.add(direction);
					long carCount = (long) feature.getAttribute("FdCar");
					record.add(Long.toString(carCount));
					long vanCount = (long) feature.getAttribute("FdLGV");
					record.add(Long.toString(vanCount));
					long rigidCount = (long) feature.getAttribute("FdHGVR2") + (long) feature.getAttribute("FdHGVR3") + (long) feature.getAttribute("FdHGVR4");
					record.add(Long.toString(rigidCount));
					long articCount = (long) feature.getAttribute("FdHGVA3") + (long) feature.getAttribute("FdHGVA5") + (long) feature.getAttribute("FdHGVA6");
					record.add(Long.toString(articCount));
				}
				else { //ferry or a newly developed road with no count point
					record.add("N/A");
					record.add("N/A");
					record.add("N/A");
					record.add("N/A");
					record.add("N/A");
				}				
				csvFilePrinter.printRecord(record);
			}
		} catch (Exception e) {
			System.err.println("Error in CsvFileWriter!");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing fileWriter/csvPrinter!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Saves total electricity consumption to an output file.
	 */
	public void saveTotalEnergyConsumptions(int year, String outputFile) {

		//calculate energy consumptions
		HashMap<EngineType, Double> energyConsumptions = this.calculateEnergyConsumptions();

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		for (EngineType et: EngineType.values()) header.add(et.name());
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			record.add(Integer.toString(year));
			for (int i=1; i<header.size(); i++)	{
				EngineType et = EngineType.valueOf(header.get(i));
				record.add(String.format("%.2f", energyConsumptions.get(et)));
			}
			csvFilePrinter.printRecord(record);
		} catch (Exception e) {
			System.err.println("Error in CsvFileWriter!");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing fileWriter/csvPrinter!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Saves peak link point capacities into a file.
	 * @param year
	 * @param outputFile
	 */
	public void savePeakLinkPointCapacities (int year, String outputFile) {

		//calculate capacities
		HashMap<Integer, Double> capacities = this.calculatePeakLinkPointCapacities();

		//save them to a file
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("edgeID");
		header.add("capacity");
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			Iterator<DirectedEdge> iter = (Iterator<DirectedEdge>) this.roadNetwork.getNetwork().getEdges().iterator();
			while (iter.hasNext()) {
				DirectedEdge edge = (DirectedEdge) iter.next();
				record.clear();
				record.add(Integer.toString(year));
				record.add(Integer.toString(edge.getID()));
				//record.add(String.format("%.2f", capacities.get(edge.getID())));
				record.add(Double.toString(capacities.get(edge.getID())));
				csvFilePrinter.printRecord(record);
			}
		} catch (Exception e) {
			System.err.println("Error in CsvFileWriter!");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing fileWriter/csvPrinter!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Saves travel times into a file.
	 * @param year
	 * @param outputFile
	 */
	public void saveLinkTravelTimes (int year, String outputFile) {

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("edgeID");
		header.add("freeFlowTravelTime");
		header.add("travelTime");
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			Iterator<DirectedEdge> iter = (Iterator<DirectedEdge>) this.roadNetwork.getNetwork().getEdges().iterator();
			while (iter.hasNext()) {
				DirectedEdge edge = (DirectedEdge) iter.next();
				record.clear();
				record.add(Integer.toString(year));
				record.add(Integer.toString(edge.getID()));
				record.add(Double.toString(this.linkFreeFlowTravelTime.get(edge.getID())));
				record.add(Double.toString(this.linkTravelTime.get(edge.getID())));
				csvFilePrinter.printRecord(record);
			}
		} catch (Exception e) {
			System.err.println("Error in CsvFileWriter!");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing fileWriter/csvPrinter!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Getter method for energy unit costs.
	 * @return Energy unit costs.
	 */   
	public HashMap<EngineType, Double> getEnergyUnitCosts() {

		return this.energyUnitCosts;
	}

	/**
	 * Getter method for energy consumptions per 100 km.
	 * @return Energy consumptions per 100 km.
	 */   
	public HashMap<EngineType, Double> getEnergyConsumptionsPer100km() {

		return this.energyConsumptionsPer100km;
	}

	/**
	 * Getter method for engine type fractions.
	 * @return Engine type fractions.
	 */   
	public HashMap<EngineType, Double> getEngineTypeFractions() {

		return this.engineTypeFractions;
	}

	/**
	 * Getter method for daily link volumes in PCU.
	 * @return Link volumes in PCU
	 */   
	public HashMap<Integer, Double> getLinkVolumesInPCU() {

		return this.linkVolumesInPCU;
	}

	/**
	 * Getter method for the link travel times.
	 * @return Link volumes
	 */
	public HashMap<Integer, Double> getLinkTravelTimes() {

		return this.linkTravelTime;
	}

	/**
	 * Getter method for the link free-flow travel times.
	 * @return Link volumes
	 */
	public HashMap<Integer, Double> getLinkFreeFlowTravelTimes() {

		return this.linkFreeFlowTravelTime;
	}

	/**
	 * Getter method for the path storage.
	 * @return Path storage
	 */
	public MultiKeyMap<String, List<Path>> getPathStorage() {

		return this.pathStorage;
	}

	/**
	 * Getter method for the path storage for freight.
	 * @return Path storage for freight
	 */
	public HashMap<VehicleType, MultiKeyMap<Integer, List<Path>>> getPathStorageFreight() {

		return this.pathStorageFreight;
	}

	/**
	 * Getter method for output area probabilities.
	 * @return Output area probabilities.
	 */
	public HashMap<String, Double> getAreaCodeProbabilities() {

		return this.areaCodeProbabilities;
	}

	/**
	 * Getter method for node probabilities.
	 * @return Node probabilities.
	 */
	public HashMap<Integer, Double> getNodeProbabilities() {

		return this.nodeProbabilities;
	}

	/**
	 * Getter method for the number of trips starting in an output area.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> getAreaCodeTripStarts() {

		return this.areaCodeNoTripStarts;
	}

	/**
	 * Getter method for the number of trips ending in an output area.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> getAreaCodeTripEnds() {

		return this.areaCodeNoTripEnds;
	}

	/**
	 * Getter method for the number of trips starting in a workplace zone.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> getWorkplaceZoneTripStarts() {

		return this.workplaceZoneNoTripStarts;
	}

	/**
	 * Getter method for the number of trips ending in a workplace zone.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> getWorkplaceZoneTripEnds() {

		return this.workplaceZoneNoTripEnds;
	}

	/**
	 * Getter method for workplace zones probabilities.
	 * @return Workplace zones probabilities.
	 */
	public HashMap<String, Double> getWorkplaceZoneProbabilities() {

		return this.workplaceZoneProbabilities;
	}


	/**
	 * Calculates the number of trips starting in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateLADTripStarts() {

		HashMap<String, Integer> totalLADnoTripStarts = new HashMap<String, Integer>();

		for (String LAD: this.roadNetwork.getZoneToAreaCodes().keySet()) {
			int numberLAD = 0;
			for (String areaCode: this.roadNetwork.getZoneToAreaCodes().get(LAD)) {
				Integer number = this.areaCodeNoTripStarts.get(areaCode);
				if (number != null) numberLAD += number;
			}
			totalLADnoTripStarts.put(LAD, numberLAD);
		}

		for (String LAD: this.LADnoTripStarts.keySet()) {
			Integer number = this.LADnoTripStarts.get(LAD);
			Integer number2 = totalLADnoTripStarts.get(LAD);
			if (number2 != null) number += number2;
			totalLADnoTripStarts.put(LAD, number);
		}

		return totalLADnoTripStarts;
	}

	/**
	 * Calculates the number of trips ending in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateLADTripEnds() {

		HashMap<String, Integer> totalLADnoTripEnds = new HashMap<String, Integer>();

		for (String LAD: this.roadNetwork.getZoneToAreaCodes().keySet()) {
			int numberLAD = 0;
			for (String areaCode: this.roadNetwork.getZoneToAreaCodes().get(LAD)) {
				Integer number = this.areaCodeNoTripEnds.get(areaCode);
				if (number != null) numberLAD += number;
			}
			totalLADnoTripEnds.put(LAD, numberLAD);
		}

		for (String LAD: this.LADnoTripEnds.keySet()) {
			Integer number = this.LADnoTripEnds.get(LAD);
			Integer number2 = totalLADnoTripEnds.get(LAD);
			if (number2 != null) number += number2;
			totalLADnoTripEnds.put(LAD, number);
		}

		return totalLADnoTripEnds;
	}

	/**
	 * Calculates the number of freight trips starting in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateFreightLADTripStarts() {

		HashMap<String, Integer> totalLADnoTripStartsFreight = new HashMap<String, Integer>();

		for (String LAD: this.roadNetwork.getZoneToWorkplaceCodes().keySet()) {
			int numberLAD = 0;
			for (String workplaceZone: this.roadNetwork.getZoneToWorkplaceCodes().get(LAD)) {
				Integer number = this.workplaceZoneNoTripStarts.get(workplaceZone);
				if (number != null) numberLAD += number;
			}
			totalLADnoTripStartsFreight.put(LAD, numberLAD);
		}
		
		for (String LAD: this.LADnoTripStartsFreight.keySet()) {
			Integer number = this.LADnoTripStartsFreight.get(LAD);
			Integer number2 = totalLADnoTripStartsFreight.get(LAD);
			if (number2 != null) number += number2;
			totalLADnoTripStartsFreight.put(LAD, number);
		}

		return totalLADnoTripStartsFreight;
	}

	/**
	 * Calculates the number of freight trips ending in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateFreightLADTripEnds() {

		HashMap<String, Integer> totalLADnoTripEndsFreight = new HashMap<String, Integer>();

		for (String LAD: this.roadNetwork.getZoneToWorkplaceCodes().keySet()) {
			int numberLAD = 0;
			for (String workplaceZone: this.roadNetwork.getZoneToWorkplaceCodes().get(LAD)) {
				Integer number = this.workplaceZoneNoTripEnds.get(workplaceZone);
				if (number != null) numberLAD += number;
			}
			totalLADnoTripEndsFreight.put(LAD, numberLAD);
		}

		for (String LAD: this.LADnoTripEndsFreight.keySet()) {
			Integer number = this.LADnoTripEndsFreight.get(LAD);
			Integer number2 = totalLADnoTripEndsFreight.get(LAD);
			if (number2 != null) number += number2;
			totalLADnoTripEndsFreight.put(LAD, number);
		}
		
		return totalLADnoTripEndsFreight;
	}

	/**
	 * Setter method for the electricity unit cost.
	 * @param electricityUnitCost The cost of 1 kWh in £.
	 */
	public void setElectricityUnitCost (double electricityUnitCost) {

		energyUnitCosts.put(EngineType.ELECTRICITY, electricityUnitCost);
	}

	/**
	 * Setter method for the energy unit cost.
	 * @param engineType The type of a car engine.
	 * @param energyUnitCost The cost of 1 L (of fuel) or 1 kWh (of electricity) in £.
	 */
	public void setEnergyUnitCost (EngineType engineType, double energyUnitCost) {

		this.energyUnitCosts.put(engineType, energyUnitCost);
	}

	/**
	 * Setter method for the energy consumption per 100 km.
	 * @param engineType The type of a car engine.
	 * @param energyConsumptionPer100km Energy consumption per 100 km (in L for fuel and kWh for electricity).
	 */
	public void setEnergyConsumptionPer100km (EngineType engineType, double energyConsumptionPer100km) {

		this.energyConsumptionsPer100km.put(engineType, energyConsumptionPer100km);
	}

	/**
	 * Setter method for the energy type fractions.
	 * @param engineType The type of a car engine.
	 * @param engineTypeFractions Engine type fractions.
	 */
	public void setEngineTypeFractions (EngineType engineType, double engineTypeFraction) {

		this.engineTypeFractions.put(engineType, engineTypeFraction);
	}

	/**
	 * Resets link volumes to zero.
	 */
	public void resetLinkVolumes () {

		//reset link volumes in PCU
		for (Integer key: this.linkVolumesInPCU.keySet()) this.linkVolumesInPCU.put(key, 0.0);
		
		//reset link volumes per vehicle type
		for (VehicleType vht: VehicleType.values()) {
			HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
			this.linkVolumesPerVehicleType.put(vht, map);
		}
	}

	/**
	 * Resets path storages for passengers and freight.
	 */
	public void resetPathStorages () {

		this.pathStorage = new MultiKeyMap<String, List<Path>>();
		for (VehicleType vht: VehicleType.values()) {
			MultiKeyMap<Integer, List<Path>> map = new MultiKeyMap<Integer, List<Path>>();
			pathStorageFreight.put(vht, map);
		}
	}

	/**
	 * Resets trip start/end counters.
	 */
	public void resetTripStartEndCounters () {

		this.areaCodeNoTripStarts = new HashMap<String, Integer>();
		this.areaCodeNoTripEnds = new HashMap<String, Integer>();
		this.workplaceZoneNoTripStarts = new HashMap<String, Integer>();
		this.workplaceZoneNoTripEnds = new HashMap<String, Integer>();
		this.LADnoTripStarts = new HashMap<String, Integer>();
		this.LADnoTripEnds = new HashMap<String, Integer>();
		this.LADnoTripStartsFreight = new HashMap<String, Integer>();
		this.LADnoTripEndsFreight = new HashMap<String, Integer>();
	}

	/**
	 * Calculate RMSN for for counts.
	 * @return Normalised root mean square error.
	 */
	public double calculateRMSNforCounts () {

		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
		int countOfCounts = 0;
		long sumOfCounts = 0;
		double sumOfSquaredDiffs = 0.0;
		ArrayList<Long> checkedCP = new ArrayList<Long>(); 

		while (iter.hasNext()) {
			DirectedEdge edge = (DirectedEdge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			String roadNumber = (String) sf.getAttribute("RoadNumber");

			if (roadNumber.charAt(0) != 'M' && roadNumber.charAt(0) != 'A') continue; //ferry

			Long countPoint = (long) sf.getAttribute("CP");

			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long carCount = (long) sf.getAttribute("FdCar");
				long carVolume;
				Integer carVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge.getID());
				if (carVolumeFetch == null) carVolume = 0;
				else 						carVolume = carVolumeFetch;

				countOfCounts++;
				sumOfCounts += carCount;
				sumOfSquaredDiffs += (carCount - carVolume)^2;
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined count
				long carCount = (long) sf.getAttribute("FdCar");

				//get volumes for this direction
				long carVolume;
				Integer carVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge.getID());
				if (carVolumeFetch == null) carVolume = 0;
				else 						carVolume = carVolumeFetch;

				//get volumes for other direction (if exists)
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edge.getID());
				long carVolume2;
				Integer carVolumeFetch2 = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge2);
				if (carVolumeFetch2 == null) carVolume2 = 0;
				else 						 carVolume2 = carVolumeFetch2;

				countOfCounts++;
				sumOfCounts += carCount;
				sumOfSquaredDiffs += (carCount - carVolume - carVolume2)^2;

				checkedCP.add(countPoint);
			}
		}

		double RMSE = Math.sqrt(sumOfSquaredDiffs);
		double averageTrueCount = (double) sumOfCounts / countOfCounts;

		return (RMSE / averageTrueCount ) * 100;
	}
	
	/**
	 * Calculate RMSN for for freight counts.
	 * @return Normalised root mean square error.
	 */
	public double calculateRMSNforFreightCounts () {

		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
		int countOfCounts = 0;
		long sumOfCounts = 0;
		double sumOfSquaredDiffs = 0.0;
		ArrayList<Long> checkedCP = new ArrayList<Long>(); 

		while (iter.hasNext()) {
			DirectedEdge edge = (DirectedEdge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			String roadNumber = (String) sf.getAttribute("RoadNumber");

			if (roadNumber.charAt(0) != 'M' && roadNumber.charAt(0) != 'A') continue; //ferry

			Long countPoint = (long) sf.getAttribute("CP");

			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long vanCount = (long) sf.getAttribute("FdLGV");
				long rigidCount = (long) sf.getAttribute("FdHGVR2") + (long) sf.getAttribute("FdHGVR3") + (long) sf.getAttribute("FdHGVR4");
				long articCount = (long) sf.getAttribute("FdHGVA3") + (long) sf.getAttribute("FdHGVA5") + (long) sf.getAttribute("FdHGVA6");
								
				long vanVolume;
				Integer vanVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.VAN).get(edge.getID());
				if (vanVolumeFetch == null) vanVolume = 0;
				else 						vanVolume = vanVolumeFetch;

				long rigidVolume;
				Integer rigidVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.RIGID).get(edge.getID());
				if (rigidVolumeFetch == null) rigidVolume = 0;
				else 						  rigidVolume = rigidVolumeFetch;
				
				long articVolume;
				Integer articVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC).get(edge.getID());
				if (articVolumeFetch == null) articVolume = 0;
				else 						  articVolume = articVolumeFetch;
				
				countOfCounts += 3;
				sumOfCounts += vanCount + rigidCount + articCount;
				sumOfSquaredDiffs += (vanCount - vanVolume)^2 + (rigidCount - rigidVolume)^2 + (articCount - articVolume)^2;
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined count
				long vanCount = (long) sf.getAttribute("FdLGV");
				long rigidCount = (long) sf.getAttribute("FdHGVR2") + (long) sf.getAttribute("FdHGVR3") + (long) sf.getAttribute("FdHGVR4");
				long articCount = (long) sf.getAttribute("FdHGVA3") + (long) sf.getAttribute("FdHGVA5") + (long) sf.getAttribute("FdHGVA6");

				//get volumes for this direction
				long vanVolume;
				Integer vanVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.VAN).get(edge.getID());
				if (vanVolumeFetch == null) vanVolume = 0;
				else 						vanVolume = vanVolumeFetch;

				long rigidVolume;
				Integer rigidVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.RIGID).get(edge.getID());
				if (rigidVolumeFetch == null) rigidVolume = 0;
				else 						  rigidVolume = rigidVolumeFetch;
				
				long articVolume;
				Integer articVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC).get(edge.getID());
				if (articVolumeFetch == null) articVolume = 0;
				else 						  articVolume = articVolumeFetch;

				//get volumes for other direction (if exists)
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edge.getID());
				
				long vanVolume2;
				Integer vanVolumeFetch2 = this.linkVolumesPerVehicleType.get(VehicleType.VAN).get(edge2);
				if (vanVolumeFetch2 == null) vanVolume2 = 0;
				else 						 vanVolume2 = vanVolumeFetch2;

				long rigidVolume2;
				Integer rigidVolumeFetch2 = this.linkVolumesPerVehicleType.get(VehicleType.RIGID).get(edge2);
				if (rigidVolumeFetch2 == null) rigidVolume2 = 0;
				else 						   rigidVolume2 = rigidVolumeFetch2;
				
				long articVolume2;
				Integer articVolumeFetch2 = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC).get(edge2);
				if (articVolumeFetch2 == null) articVolume2 = 0;
				else 						   articVolume2 = articVolumeFetch2;

				countOfCounts += 3;
				sumOfCounts += vanCount + rigidCount + articCount;
				sumOfSquaredDiffs += (vanCount - vanVolume - vanVolume2)^2 + (rigidCount - rigidVolume - rigidVolume2)^2 + (articCount - articVolume - articVolume2)^2;

				checkedCP.add(countPoint);
			}
		}

		double RMSE = Math.sqrt(sumOfSquaredDiffs);
		double averageTrueCount = (double) sumOfCounts / countOfCounts;

		return (RMSE / averageTrueCount ) * 100;
	}
}