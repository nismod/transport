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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

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
	public static final boolean FLAG_ASTAR_IF_EMPTY_ROUTE_SET = false; //if there is no pre-generated route set for a node pair, try finding a route with aStar
	public static final int INTERZONAL_TOP_NODES = 5; //how many top nodes (based on gravitated population size) to considers as trip origin/destination

	private Properties params;

	private static RandomSingleton rng = RandomSingleton.getInstance();

	public static enum EngineType {
		PETROL, DIESEL, LPG, ELECTRICITY, HYBRID, HYDROGEN
	}

	public static enum VehicleType {
		CAR(0), ARTIC(1), RIGID(2), VAN(3);
		private int value; 
		private VehicleType(int value) { this.value = value; } 
		public int getValue() { return this.value; } 
	};
		
	public static enum TimeOfDay {
		MIDNIGHT(0), ONEAM(1), TWOAM(2), THREEAM(3), FOURAM(4), FIVEAM(5), SIXAM(6), SEVENAM(7), EIGHTAM(8), NINEAM(9), TENAM(10), ELEVENAM(11),
		NOON (12), ONEPM(13), TWOPM(14), THREEPM(15), FOURPM(16), FIVEPM(17), SIXPM(18), SEVENPM(19), EIGHTPM(20), NINEPM(21), TENPM(22), ELEVENPM(23);
		private int value; 
		private TimeOfDay(int value) { this.value = value; }
	}
	
	private HashMap<VehicleType, Double> vehicleTypeToPCU;

	private HashMap<EngineType, Double> energyUnitCosts;
	private HashMap<EngineType, Double> energyConsumptionsPer100km;
	private HashMap<EngineType, Double> engineTypeFractions;

	private HashMap<TimeOfDay, Double> timeOfDayDistribution;
	
	private RoadNetwork roadNetwork;

	private Map<Integer, Double> linkVolumesInPCU;
	private HashMap<VehicleType, HashMap<Integer, Integer>> linkVolumesPerVehicleType;
	private Map<TimeOfDay, Map<Integer, Double>> linkVolumesInPCUPerTimeOfDay;
	private HashMap<Integer, Double> linkFreeFlowTravelTime;
	//private HashMap<Integer, Double> linkTravelTime;
	private Map<TimeOfDay, Map<Integer, Double>> linkTravelTimePerTimeOfDay;
	
	//storage of performed trips
	private ArrayList<Trip> tripList;

	//the probability of trip starting/ending in the census output area
	private HashMap<String, Double> areaCodeProbabilities;
	//the probability of trip starting/ending in the workplace zone
	private HashMap<String, Double> workplaceZoneProbabilities;
	//the probability of trip starting at a node
	private HashMap<Integer, Double> startNodeProbabilities;
	//the probability of trip ending at a node
	private HashMap<Integer, Double> endNodeProbabilities;
	//the probability of freight trip starting at a node
	private HashMap<Integer, Double> startNodeProbabilitiesFreight;
	//the probability of freight trip ending at a node
	private HashMap<Integer, Double> endNodeProbabilitiesFreight;

	/**
	 * @param roadNetwork Road network.
	 * @param energyUnitCosts Energy unit costs.
	 * @param engineTypeFractions Market shares of different engine/fuel types.
	 * @param defaultLinkTravelTime Default link travel times.
	 * @param areaCodeProbabilities Probabilities of trips starting/ending in each census output area.
	 * @param workplaceZoneProbabilities Probabilities of freight trips starting/ending in each census output area.
	 */
	public RoadNetworkAssignment(RoadNetwork roadNetwork, 
								 HashMap<EngineType, Double> energyUnitCosts, 
								 HashMap<EngineType, Double> engineTypeFractions, 
								 HashMap<TimeOfDay, Double> timeOfDayDistribution, 
								 Map<TimeOfDay, Map<Integer, Double>> defaultLinkTravelTime, 
								 HashMap<String, Double> areaCodeProbabilities, 
								 HashMap<String, Double> workplaceZoneProbabilities) {

		this.roadNetwork = roadNetwork;
		this.linkVolumesInPCU = new HashMap<Integer, Double>();
		this.linkVolumesPerVehicleType = new HashMap<VehicleType, HashMap<Integer, Integer>>();
		for (VehicleType vht: VehicleType.values()) {
			HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
			linkVolumesPerVehicleType.put(vht, map);
		}
		this.linkFreeFlowTravelTime = new HashMap<Integer, Double>();
		this.linkTravelTimePerTimeOfDay = new HashMap<TimeOfDay, Map<Integer, Double>>();
		for (TimeOfDay hour: TimeOfDay.values()) {
			HashMap<Integer, Double> map = new HashMap<Integer, Double>();
			linkTravelTimePerTimeOfDay.put(hour, map);
		}
		
		this.tripList = new ArrayList<Trip>();

		/*
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
		}
		*/
		
		this.linkFreeFlowTravelTime = roadNetwork.getFreeFlowTravelTime();
		
		if (defaultLinkTravelTime == null) //use free flow
			for (TimeOfDay hour: TimeOfDay.values()) {
				Map<Integer, Double> hourlyMap = this.linkTravelTimePerTimeOfDay.get(hour);
				for (Integer edgeID: this.linkFreeFlowTravelTime.keySet())
					hourlyMap.put(edgeID, this.linkFreeFlowTravelTime.get(edgeID));
			}
		else //otherwise copy
			for (TimeOfDay hour: TimeOfDay.values()) {
				Map<Integer, Double> hourlyMap = this.linkTravelTimePerTimeOfDay.get(hour);
				for (Integer edgeID: defaultLinkTravelTime.get(hour).keySet())
					hourlyMap.put(edgeID, defaultLinkTravelTime.get(hour).get(edgeID));
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

		//calculate node probabilities from gravitating population (ignores blacklisted nodes)
		this.startNodeProbabilities = new HashMap<Integer, Double>();
		this.endNodeProbabilities = new HashMap<Integer, Double>();
		//System.out.println(roadNetwork.getZoneToNodes().keySet());
		for (String zone: roadNetwork.getZoneToNodes().keySet()) {
			double sumStart = 0.0, sumEnd = 0.0;
			//System.out.println(roadNetwork.getZoneToNodes().get(zone));
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				if (!roadNetwork.isBlacklistedAsStartNode(node)) sumStart += roadNetwork.getGravitatingPopulation(node);
				if (!roadNetwork.isBlacklistedAsEndNode(node))   sumEnd += roadNetwork.getGravitatingPopulation(node);
			}
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				if (!roadNetwork.isBlacklistedAsStartNode(node)) startNodeProbabilities.put(node, roadNetwork.getGravitatingPopulation(node) / sumStart);
				if (!roadNetwork.isBlacklistedAsEndNode(node))   endNodeProbabilities.put(node, roadNetwork.getGravitatingPopulation(node) / sumEnd);
			}
		}
		System.out.println("Probabilities for nodes:");
		System.out.println(this.startNodeProbabilities);
		System.out.println(this.endNodeProbabilities);

		//calculate node probabilities from gravitating workplace population (ignores blacklisted nodes)
		startNodeProbabilitiesFreight = new HashMap<Integer, Double>();
		endNodeProbabilitiesFreight = new HashMap<Integer, Double>();
		//System.out.println(roadNetwork.getZoneToNodes().keySet());
		for (String zone: roadNetwork.getZoneToNodes().keySet()) {
			double sumStart = 0.0, sumEnd = 0.0;
			//System.out.println(roadNetwork.getZoneToNodes().get(zone));
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				if (!roadNetwork.isBlacklistedAsStartNode(node)) sumStart += roadNetwork.getGravitatingWorkplacePopulation(node);
				if (!roadNetwork.isBlacklistedAsEndNode(node))   sumEnd += roadNetwork.getGravitatingWorkplacePopulation(node);
			}
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				if (!roadNetwork.isBlacklistedAsStartNode(node)) startNodeProbabilitiesFreight.put(node, roadNetwork.getGravitatingWorkplacePopulation(node) / sumStart);
				if (!roadNetwork.isBlacklistedAsEndNode(node))   endNodeProbabilitiesFreight.put(node, roadNetwork.getGravitatingWorkplacePopulation(node) / sumEnd);
			}
		}
		System.out.println("Node probabilities for freight:");
		System.out.println(this.startNodeProbabilitiesFreight);
		System.out.println(this.endNodeProbabilitiesFreight);

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
		
		if (timeOfDayDistribution != null) this.timeOfDayDistribution = timeOfDayDistribution; //TODO check it adds up to one!
		else {
			this.timeOfDayDistribution = new HashMap<TimeOfDay, Double>();
			this.timeOfDayDistribution.put(TimeOfDay.MIDNIGHT, 0.0015);
			this.timeOfDayDistribution.put(TimeOfDay.ONEAM, 0.0006);
			this.timeOfDayDistribution.put(TimeOfDay.TWOAM, 0.0005);
			this.timeOfDayDistribution.put(TimeOfDay.THREEAM, 0.0005);
			this.timeOfDayDistribution.put(TimeOfDay.FOURAM, 0.0017);
			this.timeOfDayDistribution.put(TimeOfDay.FIVEAM, 0.0087);
			this.timeOfDayDistribution.put(TimeOfDay.SIXAM, 0.0236);
			this.timeOfDayDistribution.put(TimeOfDay.SEVENAM, 0.0636);
			this.timeOfDayDistribution.put(TimeOfDay.EIGHTAM, 0.1046);
			this.timeOfDayDistribution.put(TimeOfDay.NINEAM, 0.0679);
			this.timeOfDayDistribution.put(TimeOfDay.TENAM, 0.0587);
			this.timeOfDayDistribution.put(TimeOfDay.ELEVENAM, 0.0589);
			this.timeOfDayDistribution.put(TimeOfDay.NOON, 0.0570);
			this.timeOfDayDistribution.put(TimeOfDay.ONEPM, 0.0549);
			this.timeOfDayDistribution.put(TimeOfDay.TWOPM, 0.0581);
			this.timeOfDayDistribution.put(TimeOfDay.THREEPM, 0.0774);
			this.timeOfDayDistribution.put(TimeOfDay.FOURPM, 0.0834);
			this.timeOfDayDistribution.put(TimeOfDay.FIVEPM, 0.0942);
			this.timeOfDayDistribution.put(TimeOfDay.SIXPM, 0.0708);
			this.timeOfDayDistribution.put(TimeOfDay.SEVENPM, 0.0456);
			this.timeOfDayDistribution.put(TimeOfDay.EIGHTPM, 0.0284);
			this.timeOfDayDistribution.put(TimeOfDay.NINEPM, 0.0187);
			this.timeOfDayDistribution.put(TimeOfDay.TENPM, 0.0136);
			this.timeOfDayDistribution.put(TimeOfDay.ELEVENPM, 0.0071);
		}
	}

	/** 
	 * Assigns passenger origin-destination matrix to the road network.
	 * Calculates the fastest path based on the current values in the linkTravelTime instance field.
	 * @param passengerODM Passenger origin-destination matrix with flows to be assigned.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlows(ODMatrix passengerODM) {

		System.out.println("Assigning the passenger flows from the passenger matrix...");

		//sort nodes based on the gravitating population
		this.roadNetwork.sortGravityNodes();
		
		//to store routes generated during the assignment
		RouteSetGenerator rsg = new RouteSetGenerator(this.roadNetwork);

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		//for each OD pair from the passengerODM		
		for (MultiKey mk: passengerODM.getKeySet()) {
			//System.out.println(mk);
			//System.out.println("origin = " + mk.getKey(0));
			//System.out.println("destination = " + mk.getKey(1));
			String originZone = (String)mk.getKey(0);
			String destinationZone = (String)mk.getKey(1);

			List<Integer> listOfOriginNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(originZone)); //the list is already sorted
			List<Integer> listOfDestinationNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(destinationZone)); //the list is already sorted

			//removing blacklisted nodes
			for (Integer originNode: roadNetwork.getZoneToNodes().get(originZone))
				//check if any of the nodes is blacklisted
				if (this.roadNetwork.isBlacklistedAsStartNode(originNode)) 
					listOfOriginNodes.remove(originNode);

			//removing blacklisted nodes
			for (Integer destinationNode: roadNetwork.getZoneToNodes().get(destinationZone))
				//check if any of the nodes is blacklisted
				if (this.roadNetwork.isBlacklistedAsEndNode(destinationNode)) 
					listOfDestinationNodes.remove(destinationNode);

			//for each trip
			int flow = passengerODM.getFlow(originZone, destinationZone);
			counterTotalFlow += flow;

			for (int i=0; i<flow; i++) {
				
				//choose time of day
				double cumulativeProbability = 0.0;
				double random = rng.nextDouble();
				TimeOfDay hour = null;
				for (Map.Entry<TimeOfDay, Double> entry : timeOfDayDistribution.entrySet()) {
					TimeOfDay key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						hour = key;
						break;
					}
				}
				
				//choose vehicle
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				
				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node

				//choose origin node
				cumulativeProbability = 0.0;
				Integer originNode = null;
				random = rng.nextDouble();
				for (Integer node: listOfOriginNodes) {
					cumulativeProbability += startNodeProbabilities.get(node);
					if (Double.compare(cumulativeProbability, random) > 0) {
						originNode = node;
						break;
					}
				}

				if (originNode == null) System.err.println("Origin node was not chosen!");

				//choose destination node
				cumulativeProbability = 0.0;
				Integer destinationNode = null;
				random = rng.nextDouble();
				//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
				//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
				if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT && originZone.equals(destinationZone) && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
					for (Integer node: listOfDestinationNodes) {
						if (node.intValue() == originNode.intValue()) continue; //skip if the node is the same as origin
						cumulativeProbability += endNodeProbabilities.get(node) / (1.0 - endNodeProbabilities.get(originNode));
						if (Double.compare(cumulativeProbability, random) > 0) {
							destinationNode = node;
							break;
						}
					}
				} else	{ //inter-zonal trip (or intra-zonal with replacement)
					for (Integer node: listOfDestinationNodes) {
						cumulativeProbability += endNodeProbabilities.get(node);
						if (Double.compare(cumulativeProbability, random) > 0) {
							destinationNode = node;
							break;
						}
					}
				}	

				if (destinationNode == null) System.err.println("Destination node was not chosen!");

				DirectedGraph rn = roadNetwork.getNetwork();
				//set source and destination node
				Node from = roadNetwork.getNodeIDtoNode().get(originNode);
				Node to = roadNetwork.getNodeIDtoNode().get(destinationNode);
				//					System.out.println("from " + from + " to " + to);
				Route foundRoute = null;
				try {

					//see if that route already exists in the route storage
					RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
					if (rs != null) foundRoute = rs.getChoiceSet().get(0); //take the first route
	
					//if route does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundRoute == null) {
						//System.out.println("The path does not exist in the path storage");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

						//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, (HashMap<Integer, Double>)this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							System.err.println("Not even aStar could find a route!");
							continue;
						}

						foundRoute = new Route(fastestPath);
						rsg.addRoute(foundRoute); //add to the route set
					}

					counterAssignedTrips++;

					List listOfEdges = foundRoute.getEdges();
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

					for (Object o: listOfEdges) {
						//DirectedEdge e = (DirectedEdge) o;
						Edge e = (Edge) o;

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

					//store trip in trip list
					Trip trip = new Trip(VehicleType.CAR, engine, foundRoute, hour, 0, 0);
					this.tripList.add(trip);

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
	 * @param rsg Route set generator containing the routes.
	 * @param routeChoiceParameters Route choice parameters.
	 */
	//@SuppressWarnings("unused")
	public void assignPassengerFlowsRouteChoice(ODMatrix passengerODM, RouteSetGenerator rsg, Properties routeChoiceParameters) {

		System.out.println("Assigning the passenger flows from the passenger matrix...");
		
		final int totalExpectedFlow = passengerODM.getTotalFlow();
		this.tripList = new ArrayList<Trip>(totalExpectedFlow); //use expected flow as array list initial capacity

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
			String originZone = (String)mk.getKey(0);
			String destinationZone = (String)mk.getKey(1);

			List<Integer> listOfOriginNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(originZone)); //the list is already sorted
			List<Integer> listOfDestinationNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(destinationZone)); //the list is already sorted

			//removing blacklisted nodes
			for (Integer originNode: roadNetwork.getZoneToNodes().get(originZone))
				//check if any of the nodes is blacklisted
				if (this.roadNetwork.getStartNodeBlacklist().contains(originNode)) 
					listOfOriginNodes.remove(originNode);

			//removing blacklisted nodes
			for (Integer destinationNode: roadNetwork.getZoneToNodes().get(destinationZone))
				//check if any of the nodes is blacklisted
				if (this.roadNetwork.getEndNodeBlacklist().contains(destinationNode)) 
					listOfDestinationNodes.remove(destinationNode);

			//for each trip
			int flow = passengerODM.getFlow(originZone, destinationZone);
			counterTotalFlow += flow;

			for (int i=0; i<flow; i++) {
				
				//choose time of day
				double cumulativeProbability = 0.0;
				double random = rng.nextDouble();
				TimeOfDay hour = null;
				for (Map.Entry<TimeOfDay, Double> entry : timeOfDayDistribution.entrySet()) {
					TimeOfDay key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						hour = key;
						break;
					}
				}
				
				//choose vehicle
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
	
				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node

				Integer originNode = null;
				Integer destinationNode = null;

				if (originZone.equals(destinationZone)) { //if intra-zonal

					//choose origin node
					cumulativeProbability = 0.0;
					random = rng.nextDouble();
					for (Integer node: listOfOriginNodes) {
						cumulativeProbability += startNodeProbabilities.get(node);
						if (Double.compare(cumulativeProbability, random) > 0) {
							originNode = node;
							break;
						}
					}

					if (originNode == null) System.err.println("Origin node for intra-zonal trip was not chosen!");

					//choose destination node
					cumulativeProbability = 0.0;
					random = rng.nextDouble();
					//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
					//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
					if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT && listOfDestinationNodes.contains(originNode)) { //no replacement
						for (Integer node: listOfDestinationNodes) {
							if (node.intValue() == originNode.intValue()) continue; //skip if the node is the same as origin
							cumulativeProbability += endNodeProbabilities.get(node) / (1.0 - endNodeProbabilities.get(originNode));
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = node;
								break;
							}
						}
					} else	{ //with replacement
						for (Integer node: listOfDestinationNodes) {
							cumulativeProbability += endNodeProbabilities.get(node);
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = node;
								break;
							}
						}
					}	

					if (destinationNode == null) System.err.println("Destination for intra-zonal trip node was not chosen!");

				} else { //inter-zonal

					/*
					//for (int nodeIndex=0; nodeIndex < INTERZONAL_TOP_NODES && listOfOriginNodes.size(); nodeIndex++)
					//choose random from top_nodes regardless of population size
					int indexOrigin = rng.nextInt(INTERZONAL_TOP_NODES<listOfOriginNodes.size()?INTERZONAL_TOP_NODES:listOfOriginNodes.size());
					int indexDestination = rng.nextInt(INTERZONAL_TOP_NODES<listOfDestinationNodes.size()?INTERZONAL_TOP_NODES:listOfDestinationNodes.size());
					//System.out.println("Index of origin node: " + indexOrigin);
					//System.out.println("Index of destination node: " + indexDestination);
					originNode = listOfOriginNodes.get(indexOrigin);					
					destinationNode = listOfDestinationNodes.get(indexDestination);
					*/
					
					//make a choice based on the gravitating population size
					int originNodesToConsider = INTERZONAL_TOP_NODES<listOfOriginNodes.size()?INTERZONAL_TOP_NODES:listOfOriginNodes.size();
					int destinationNodesToConsider = INTERZONAL_TOP_NODES<listOfDestinationNodes.size()?INTERZONAL_TOP_NODES:listOfDestinationNodes.size();
					//sums of gravitating population
					
					double sum = 0.0;
					for (int j=0; j<originNodesToConsider; j++) sum += startNodeProbabilities.get(listOfOriginNodes.get(j)); 
					//choose origin node
					cumulativeProbability = 0.0;
					random = rng.nextDouble();
					for (Integer node: listOfOriginNodes) {
						cumulativeProbability += startNodeProbabilities.get(node) / sum; //scale with sum
						if (Double.compare(cumulativeProbability, random) > 0) {
							originNode = node;
							break;
						}
					}
					
					sum = 0.0;
					for (int j=0; j<destinationNodesToConsider; j++) sum += endNodeProbabilities.get(listOfDestinationNodes.get(j)); 
					//choose destination node
					cumulativeProbability = 0.0;
					random = rng.nextDouble();
					for (Integer node: listOfDestinationNodes) {
						cumulativeProbability += endNodeProbabilities.get(node) / sum; //scale with sum
						if (Double.compare(cumulativeProbability, random) > 0) {
							destinationNode = node;
							break;
						}
					}
					
					if (originNode == null) System.err.println("Origin node for inter-zonal trip was not chosen!");
					if (destinationNode == null) System.err.println("Destination node for inter-zonal trip was not chosen!");
				}

				Route chosenRoute = null;
				RouteSet fetchedRouteSet = rsg.getRouteSet(originNode.intValue(), destinationNode.intValue());
				if (fetchedRouteSet == null) {
					System.err.printf("Can't fetch the route set between nodes %d and %d! %s", originNode, destinationNode, System.lineSeparator());

					if (!FLAG_ASTAR_IF_EMPTY_ROUTE_SET)	continue;
					else { //try finding a path with aStar
						System.err.println("Trying the astar!");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

						//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							System.err.println("Not even aStar could find a route!");
							continue;
						}
						chosenRoute = new Route(fastestPath);
						if (chosenRoute.isEmpty()) {
							System.err.printf("Empty route between nodes %d and %d! %s", originNode, destinationNode, System.lineSeparator());
							continue;
						}
						//store generated route into the rsg!
						rsg.addRoute(chosenRoute);
					}
				} else { //there is a route set

					//if (fetchedRouteSet.getProbabilities() == null) {
						//probabilities need to be calculated for this route set before a choice can be made
						fetchedRouteSet.setLinkTravelTime(this.linkTravelTimePerTimeOfDay.get(hour));
						fetchedRouteSet.setParameters(routeChoiceParameters);
						fetchedRouteSet.calculateUtilities(this.linkTravelTimePerTimeOfDay.get(hour), routeChoiceParameters);
						fetchedRouteSet.calculateProbabilities(this.linkTravelTimePerTimeOfDay.get(hour), routeChoiceParameters);
						fetchedRouteSet.sortRoutesOnUtility();
					//}

					//choose the route
					chosenRoute = fetchedRouteSet.choose(routeChoiceParameters);
					if (chosenRoute == null) {
						System.err.printf("No chosen route between nodes %d and %d! %s", originNode, destinationNode, System.lineSeparator());
						continue;
					}
				}

				if (chosenRoute.isEmpty()) {
					System.err.println("The chosen route is empty, skipping this trip!");
					continue;
				}

				//there is a chosenRoute
				counterAssignedTrips++;

				/*
				//increase the number of trips starting at origin LAD
				Integer number = this.LADnoTripStarts.get(origin);
				if (number == null) number = 0;
				this.LADnoTripStarts.put(origin, ++number);
				//increase the number of trips ending at destination LAD
				Integer number2 = this.LADnoTripEnds.get(destination);
				if (number2 == null) number2 = 0;
				this.LADnoTripEnds.put(destination, ++number2);
				*/

				List listOfEdges = chosenRoute.getEdges();
				//System.out.println("The path as a list of edges: " + listOfEdges);
				//System.out.println("Path size in the number of edges: " + listOfEdges.size());

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

				//store trip in trip list
				Trip trip = new Trip(VehicleType.CAR, engine, chosenRoute, hour, 0, 0);
				this.tripList.add(trip);
				
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
	 * @param freightMatrix Freight origin-destination matrix.
	 */
	@SuppressWarnings("unused")
	public void assignFreightFlows(FreightMatrix freightMatrix) {


		System.out.println("Assigning the vehicle flows from the freight matrix...");

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;
		
		//to store routes generated during the assignment
		RouteSetGenerator rsg = new RouteSetGenerator(this.roadNetwork);

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
			counterTotalFlow += flow;

			for (int i=0; i<flow; i++) {
				
				//choose time of day
				double cumulativeProbability = 0.0;
				double random = rng.nextDouble();
				TimeOfDay hour = null;
				for (Map.Entry<TimeOfDay, Double> entry : timeOfDayDistribution.entrySet()) {
					TimeOfDay key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						hour = key;
						break;
					}
				}
				
				//choose vehicle
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}

				Integer originNode = null, destinationNode = null;

				//choose origin node based on the gravitating population
				if (origin <= 1032) { //origin freight zone is a LAD

					//choose origin node based on the gravitating population
					//the choice with replacement means that possibly: destination node = origin node
					//the choice without replacement means that destination node has to be different from origin node

					//map freight zone number to LAD code
					String originLAD = roadNetwork.getFreightZoneToLAD().get(origin);
					//List<Integer> listOfOriginNodes = roadNetwork.getZoneToNodes().get(originLAD); //the list is already sorted
					List<Integer> listOfOriginNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(originLAD)); //the list is already sorted

					//removing blacklisted nodes
					for (Integer node: roadNetwork.getZoneToNodes().get(originLAD))
						//check if any of the nodes is blacklisted
						if (this.roadNetwork.isBlacklistedAsStartNode(node)) 
							listOfOriginNodes.remove(node);

					//choose origin node
					cumulativeProbability = 0.0;
					random = rng.nextDouble();
					for (int node: listOfOriginNodes) {
						cumulativeProbability += startNodeProbabilitiesFreight.get(node);
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
					//List<Integer> listOfDestinationNodes = roadNetwork.getZoneToNodes().get(destinationLAD); //the list is already sorted
					List<Integer> listOfDestinationNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(destinationLAD)); //the list is already sorted

					//removing blacklisted nodes
					for (Integer node: roadNetwork.getZoneToNodes().get(destinationLAD))
						//check if any of the nodes is blacklisted
						if (this.roadNetwork.isBlacklistedAsEndNode(node)) 
							listOfDestinationNodes.remove(node);

					//choose origin node
					cumulativeProbability = 0.0;
					random = rng.nextDouble();
					//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
					//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
					if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT && origin == destination && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
						for (int node: listOfDestinationNodes) {
							if (node == originNode.intValue()) continue; //skip if the node is the same as origin
							cumulativeProbability += endNodeProbabilitiesFreight.get(node) / (1.0 - endNodeProbabilitiesFreight.get(originNode));
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = node;
								break;
							}
						}
					} else	{ //inter-zonal trip (or intra-zonal with replacement)
						for (int node: listOfDestinationNodes) {
							cumulativeProbability += endNodeProbabilitiesFreight.get(node);
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
				//					System.out.println("from " + from + " to " + to);
				Route foundRoute = null;
				try {
 						//see if that route already exists in the route storage
						RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
						if (rs != null) foundRoute = rs.getChoiceSet().get(0); //take the first route
		
						//if route does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
						if (foundRoute == null) {
							//System.out.println("The path does not exist in the path storage");

							DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
							DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

							RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
							if (fastestPath == null) {
								System.err.println("Not even aStar could find a route!");
								continue;
							}

							foundRoute = new Route(fastestPath);
							rsg.addRoute(foundRoute); //add to the route set
						}
					
					//if path does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundRoute == null) {

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							System.err.println("Not even aStar could find a route!");
							continue;
						}

						foundRoute = new Route(fastestPath);
					}

					//trip was assigned
					counterAssignedTrips++;

					List listOfEdges = foundRoute.getEdges();
					//System.out.println("The path as a list of edges: " + listOfEdges);
					//System.out.println("Path size in the number of nodes: " + aStarPath.size());
					//System.out.println("Path size in the number of edges: " + listOfEdges.size());

					VehicleType vht = VehicleType.values()[vehicleType];
					
					for (Object o: listOfEdges) {
						//DirectedEdge e = (DirectedEdge) o;
						Edge e = (Edge) o;
	
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

					//store trip in trip list
					Trip trip = new Trip(vht, engine, foundRoute, hour, origin, destination);
					this.tripList.add(trip);
	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}//for each trip
		}//for each OD pair

		System.out.println("Total flow: " + counterTotalFlow);
		System.out.println("Total assigned trips: " + counterAssignedTrips);
		System.out.println("Assignment percentage: " + 100.0* counterAssignedTrips / counterTotalFlow);
	}

	/**
	 * Assigns freight origin-destination matrix to the road network using a route choice model and pre-generated routes.
	 * Zone ID ranges from the BYFM DfT model:
	 * <ul>
	 * 		<li>England: 1 - 867</li>
	 * 		<li>Wales: 901 - 922</li>
	 * 		<li>Scotland: 1001 - 1032</li>
	 * 		<li>Freight airports: 1111 - 1115</li>
	 * 		<li>Major distribution centres: 1201 - 1256</li>
	 * 		<li>Freight ports: 1301 - 1388</li>
	 * </ul>   
	 * @param freightMatrix Freight origin-destination matrix.
	 * @param rsg Route set generator containing the routes.
	 * @param routeChoiceParameters Route choice parameters.
	 */
	@SuppressWarnings("unused")
	public void assignFreightFlowsRouteChoice(FreightMatrix freightMatrix, RouteSetGenerator rsg, Properties routeChoiceParameters) {

		System.out.println("Assigning the vehicle flows from the freight matrix...");

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

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
			counterTotalFlow += flow;

			for (int i=0; i<flow; i++) {
				
				//choose time of day
				double cumulativeProbability = 0.0;
				double random = rng.nextDouble();
				TimeOfDay hour = null;
				for (Map.Entry<TimeOfDay, Double> entry : timeOfDayDistribution.entrySet()) {
					TimeOfDay key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						hour = key;
						break;
					}
				}
				
				//choose vehicle
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}

				Integer originNode = null, destinationNode = null;
				String originLAD = null, destinationLAD = null;

				if (origin <= 1032) //origin freight zone is a LAD
					originLAD = roadNetwork.getFreightZoneToLAD().get(origin);
				else //freight zone is a point/node
					originNode = roadNetwork.getFreightZoneToNearestNode().get(origin);

				if (destination <= 1032) //destination freight zone is a LAD
					destinationLAD = roadNetwork.getFreightZoneToLAD().get(destination);
				else //freight zone is a point/node
					destinationNode = roadNetwork.getFreightZoneToNearestNode().get(destination);

				//check blacklisting just in case
				if (originNode != null && roadNetwork.isBlacklistedAsStartNode(originNode)) {
					System.err.printf("Origin node %d is blacklisted, skipping this trip! %n", originNode);
					continue;
				}
				if (destinationNode != null && roadNetwork.isBlacklistedAsEndNode(destinationNode))	{
					System.err.printf("Destination node %d is blacklisted, skipping this trip! %n", destinationNode);
					continue;	
				}

				if (originLAD != null && destinationLAD != null) { //LAD to LAD

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

					if (originLAD == destinationLAD) { //intra-zonal trip!

						//choose any origin node
						cumulativeProbability = 0.0;
						random = rng.nextDouble();
						for (int node: listOfOriginNodes) {
							cumulativeProbability += startNodeProbabilitiesFreight.get(node);
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
						if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
							for (int node: listOfDestinationNodes) {
								if (node == originNode.intValue()) continue; //skip if the node is the same as origin
								cumulativeProbability += endNodeProbabilitiesFreight.get(node) / (1.0 - endNodeProbabilitiesFreight.get(originNode));
								if (Double.compare(cumulativeProbability, random) > 0) {
									destinationNode = node;
									break;
								}
							}
						} else	{ //intra-zonal with replacement)
							for (int node: listOfDestinationNodes) {
								cumulativeProbability += endNodeProbabilitiesFreight.get(node);
								if (Double.compare(cumulativeProbability, random) > 0) {
									destinationNode = node;
									break;
								}
							}
						}

					} else { //inter-zonal trip!

						//make a choice based on the gravitating workzone population size
						int originNodesToConsider = INTERZONAL_TOP_NODES<listOfOriginNodes.size()?INTERZONAL_TOP_NODES:listOfOriginNodes.size();
						int destinationNodesToConsider = INTERZONAL_TOP_NODES<listOfDestinationNodes.size()?INTERZONAL_TOP_NODES:listOfDestinationNodes.size();
						//sums of gravitating population
						
						double sum = 0.0;
						for (int j=0; j<originNodesToConsider; j++) sum += startNodeProbabilitiesFreight.get(listOfOriginNodes.get(j)); 
						//choose origin node
						cumulativeProbability = 0.0;
						random = rng.nextDouble();
						for (Integer node: listOfOriginNodes) {
							cumulativeProbability += startNodeProbabilitiesFreight.get(node) / sum; //scale with sum
							if (Double.compare(cumulativeProbability, random) > 0) {
								originNode = node;
								break;
							}
						}
						
						sum = 0.0;
						for (int j=0; j<destinationNodesToConsider; j++) sum += endNodeProbabilitiesFreight.get(listOfDestinationNodes.get(j)); 
						//choose destination node
						cumulativeProbability = 0.0;
						random = rng.nextDouble();
						for (Integer node: listOfDestinationNodes) {
							cumulativeProbability += endNodeProbabilitiesFreight.get(node) / sum; //scale with sum
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = node;
								break;
							}
						}
											
						if (originNode == null) System.err.println("Origin node for inter-zonal freight trip was not chosen!");
						if (destinationNode == null) System.err.println("Destination node for inter-zonal freight trip was not chosen!");
						
					}

				} else if (originNode != null && destinationLAD != null) { //point to LAD

					List<Integer> listOfDestinationNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(destinationLAD)); //the list is already sorted
					//removing blacklisted nodes
					for (Integer node: roadNetwork.getZoneToNodes().get(destinationLAD))
						//check if any of the nodes is blacklisted
						if (this.roadNetwork.isBlacklistedAsEndNode(node)) 
							listOfDestinationNodes.remove(node);

					//if originNode is from the same destinationLAD (i.e. intra-zonal) consider all nodes based on population size
					//however, check if replacement has to be done
					if (roadNetwork.getZoneToNodes().get(destinationLAD).contains(originNode)) { 

						//choose destination node
						cumulativeProbability = 0.0;
						random = rng.nextDouble();
						if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
							for (int node: listOfDestinationNodes) {
								if (node == originNode.intValue()) continue; //skip if the node is the same as origin
								cumulativeProbability += endNodeProbabilitiesFreight.get(node) / (1.0 - endNodeProbabilitiesFreight.get(originNode));
								if (Double.compare(cumulativeProbability, random) > 0) {
									destinationNode = node;
									break;
								}
							}
						} else	{ //intra-zonal with replacement)
							for (int node: listOfDestinationNodes) {
								cumulativeProbability += endNodeProbabilitiesFreight.get(node);
								if (Double.compare(cumulativeProbability, random) > 0) {
									destinationNode = node;
									break;
								}
							}
						}

						//if they are from different LADs, consider only top nodes from destination LAD		
					} else {
						int indexDestination = rng.nextInt(INTERZONAL_TOP_NODES<listOfDestinationNodes.size()?INTERZONAL_TOP_NODES:listOfDestinationNodes.size());
						destinationNode = listOfDestinationNodes.get(indexDestination);
					}

				} else if (originLAD != null && destinationNode != null) { //LAD to point

					List<Integer> listOfOriginNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(originLAD)); //the list is already sorted
					//removing blacklisted nodes
					for (Integer node: roadNetwork.getZoneToNodes().get(originLAD))
						//check if any of the nodes is blacklisted
						if (this.roadNetwork.isBlacklistedAsStartNode(node)) 
							listOfOriginNodes.remove(node);

					//if destinationNode is from the same originLAD (i.e. intra-zonal) consider all nodes based on population size
					//however, check if replacement has to be done
					if (roadNetwork.getZoneToNodes().get(originLAD).contains(destinationNode)) { 

						//choose destination node
						cumulativeProbability = 0.0;
						random = rng.nextDouble();
						if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT && listOfOriginNodes.contains(destinationNode)) { //no replacement and intra-zonal trip
							for (int node: listOfOriginNodes) {
								if (node == destinationNode.intValue()) continue; //skip if the node is the same as destination
								cumulativeProbability += startNodeProbabilitiesFreight.get(node) / (1.0 - startNodeProbabilitiesFreight.get(destinationNode));
								if (Double.compare(cumulativeProbability, random) > 0) {
									originNode = node;
									break;
								}
							}
						} else	{ //intra-zonal with replacement)
							for (int node: listOfOriginNodes) {
								cumulativeProbability += startNodeProbabilitiesFreight.get(node);
								if (Double.compare(cumulativeProbability, random) > 0) {
									originNode = node;
									break;
								}
							}
						}

						//if they are from different LADs, consider only top nodes from destination LAD		
					} else {
						int indexOrigin = rng.nextInt(INTERZONAL_TOP_NODES<listOfOriginNodes.size()?INTERZONAL_TOP_NODES:listOfOriginNodes.size());
						originNode = listOfOriginNodes.get(indexOrigin);
					}


				} else if (originNode != null && destinationNode != null) { //point to point

					if (!FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT && originNode == destinationNode) 
						System.err.println("Origin and destination node are the same, but there is no replacement!");
				}

				if (originNode == null) System.err.println("Could not find origin node for a freight trip!");
				if (destinationNode == null) System.err.println("Could not find destination node for a freight trip!");

				Route chosenRoute = null;
				RouteSet fetchedRouteSet = rsg.getRouteSet(originNode.intValue(), destinationNode.intValue());
				if (fetchedRouteSet == null) {
					System.err.printf("Can't fetch the route set between nodes %d and %d! %s", originNode, destinationNode, System.lineSeparator());

					if (!FLAG_ASTAR_IF_EMPTY_ROUTE_SET)	continue;
					else { //try finding a path with aStar
						System.err.println("Trying the astar!");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							System.err.println("Not even aStar could find a route!");
							continue;
						}
						chosenRoute = new Route(fastestPath);
						if (chosenRoute.isEmpty()) {
							System.err.printf("Empty route between nodes %d and %d! %s", originNode, destinationNode, System.lineSeparator());
							continue;
						}

						//store generated route into the rsg!
						rsg.addRoute(chosenRoute);
					}
				} else { //there is a route set

					//if (fetchedRouteSet.getProbabilities() == null) {
						//probabilities need to be calculated for this route set before a choice can be made
						fetchedRouteSet.setLinkTravelTime(this.linkTravelTimePerTimeOfDay.get(hour));
						fetchedRouteSet.setParameters(routeChoiceParameters);
						fetchedRouteSet.calculateUtilities(this.linkTravelTimePerTimeOfDay.get(hour), routeChoiceParameters);
						fetchedRouteSet.calculateProbabilities(this.linkTravelTimePerTimeOfDay.get(hour), routeChoiceParameters);
						fetchedRouteSet.sortRoutesOnUtility();
					//}

					//choose the route
					chosenRoute = fetchedRouteSet.choose(routeChoiceParameters);
					if (chosenRoute == null) {
						System.err.printf("No chosen route between nodes %d and %d! %s", originNode, destinationNode, System.lineSeparator());
						continue;
					}
				}

				if (chosenRoute.isEmpty()) {
					System.err.println("The chosen route is empty, skipping this trip!");
					continue;
				}

				//there is a chosenRoute
				counterAssignedTrips++;

				//check to which LAD chosen origin and destination nodes belong to!
				originLAD = roadNetwork.getNodeToZone().get(originNode);
				destinationLAD = roadNetwork.getNodeToZone().get(destinationNode);

				List listOfEdges = chosenRoute.getEdges();
				//System.out.println("The path as a list of edges: " + listOfEdges);
				//System.out.println("Path size in the number of nodes: " + aStarPath.size());
				//System.out.println("Path size in the number of edges: " + listOfEdges.size());

				VehicleType vht = VehicleType.values()[vehicleType];

				for (Object o: listOfEdges) {
					//DirectedEdge e = (DirectedEdge) o;
					Edge e = (Edge) o;

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
			
				//store trip in trip list
				Trip trip = new Trip(vht, engine, chosenRoute, hour, origin , destination);
				this.tripList.add(trip);

			}//for each trip
		}//for each OD pair

		System.out.println("Total flow: " + counterTotalFlow);
		System.out.println("Total assigned trips: " + counterAssignedTrips);
		System.out.println("Assignment percentage: " + 100.0* counterAssignedTrips / counterTotalFlow);
	}

	/**
	 * Updates link travel times per time of day
	 */
	public void updateLinkTravelTimes() {

		double congestedTravelTime;
		
		this.linkVolumesInPCUPerTimeOfDay = this.calculateLinkVolumeInPCUPerTimeOfDay(this.tripList); //calculate link volumes per time of day

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		HashMap<Integer, Integer> numberOfLanes = roadNetwork.getNumberOfLanes();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			double roadLength = (double) sf.getAttribute("LenNet"); //[km]

			//iterate through all times of day
			for (TimeOfDay hour: TimeOfDay.values()) {

				Map<Integer, Double> hourlyVolumes = this.linkVolumesInPCUPerTimeOfDay.get(hour);
				Double linkVolumeInPCU = hourlyVolumes.get(edge.getID());
				if (linkVolumeInPCU == null) linkVolumeInPCU = 0.0;

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

					double flow = linkVolumeInPCU / numberOfLanes.get(edge.getID());
					double speed = 0.0;
					if (flow < 1398) speed = ((69.96 - 71.95) / 1398 * flow + 71.95) * 1.609344; //[kph]
					else 			 speed = ((34.55 - 69.96) / (2330 - 1398) * (flow - 1398) + 69.96) * 1.609344; //[kph]

					if (speed <= 0.0) {
						System.err.println("Speed is not positive!");
						speed = 34.55; //constraint speed
					}
					congestedTravelTime = roadLength / speed * 60; //[min]

				} else if (roadNumber.charAt(0) == 'A') {//A-road

					double flow = linkVolumeInPCU / numberOfLanes.get(edge.getID());
					double speed = 0.0;
					if (flow < 1251) speed = ((50.14 - 56.05) / 1251 * flow + 56.05) * 1.609344; //[kph]
					else 			 speed = ((27.22 - 50.14) / (1740 - 1251) * (flow - 1251) + 50.14) * 1.609344; //[kph]

					if (speed <= 0.0) {
						System.err.println("Speed is not positive!");
						speed = 27.22; //constraint speed
					}
					congestedTravelTime = roadLength / speed * 60;

				} else //ferry
					congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID()); //ferry travel time is fixed
						
				Map<Integer, Double> hourlyTimes = this.linkTravelTimePerTimeOfDay.get(hour);
				hourlyTimes.put(edge.getID(), congestedTravelTime);
			}
		}
	}

	/**
	 * Updates link travel times using weighted averaging between new values (calculated from link volumes) 
	 * and older values (stored in the instance field).
	 * @param weight Parameter for weighted averaging.
	 */
	public void updateLinkTravelTimes(double weight) {
		
		double congestedTravelTime;
		
		this.linkVolumesInPCUPerTimeOfDay = this.calculateLinkVolumeInPCUPerTimeOfDay(this.tripList); //calculate link volumes per time of day

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		HashMap<Integer, Integer> numberOfLanes = roadNetwork.getNumberOfLanes();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			double roadLength = (double) sf.getAttribute("LenNet"); //[km]

			//iterate through all times of day
			for (TimeOfDay hour: TimeOfDay.values()) {

				Map<Integer, Double> hourlyVolumes = this.linkVolumesInPCUPerTimeOfDay.get(hour);
				Double linkVolumeInPCU = hourlyVolumes.get(edge.getID());
				if (linkVolumeInPCU == null) linkVolumeInPCU = 0.0;

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

					double flow = linkVolumeInPCU / numberOfLanes.get(edge.getID());
					double speed = 0.0;
					if (flow < 1398) speed = ((69.96 - 71.95) / 1398 * flow + 71.95) * 1.609344; //[kph]
					else 			 speed = ((34.55 - 69.96) / (2330 - 1398) * (flow - 1398) + 69.96) * 1.609344; //[kph]

					if (speed <= 0.0) {
						System.err.println("Speed is not positive!");
						speed = 34.55; //constraint speed
					}
					congestedTravelTime = roadLength / speed * 60; //[min]

				} else if (roadNumber.charAt(0) == 'A') {//A-road

					double flow = linkVolumeInPCU / numberOfLanes.get(edge.getID());
					double speed = 0.0;
					if (flow < 1251) speed = ((50.14 - 56.05) / 1251 * flow + 56.05) * 1.609344; //[kph]
					else 			 speed = ((27.22 - 50.14) / (1740 - 1251) * (flow - 1251) + 50.14) * 1.609344; //[kph]

					if (speed <= 0.0) {
						System.err.println("Speed is not positive!");
						speed = 27.22; //constraint speed
					}
					congestedTravelTime = roadLength / speed * 60;

				} else //ferry
					congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID()); //ferry travel time is fixed
				
				Double oldLinkTravelTime = this.linkTravelTimePerTimeOfDay.get(hour).get(edge.getID());
				congestedTravelTime = weight * congestedTravelTime + (1 - weight) * oldLinkTravelTime;
				
				Map<Integer, Double> hourlyTimes = this.linkTravelTimePerTimeOfDay.get(hour);
				hourlyTimes.put(edge.getID(), congestedTravelTime);
			}
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
	 * Assigns passenger and freight origin-destination matrix to the road network
	 * using the fastest path based on the current values in the linkTravelTime field.
	 * Then updates link travel times using weighted averaging.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param weight Weighting parameter.
	 */
	public void assignFlowsAndUpdateLinkTravelTimes(ODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, Properties params, double weight) {

		this.assignPassengerFlowsRouteChoice(passengerODM, rsg, params);
		this.assignFreightFlowsRouteChoice(freightODM, rsg, params);
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
			this.resetTripStorages(); //clear route storages
			this.assignFlowsAndUpdateLinkTravelTimes(passengerODM, freightODM, weight);
		}
	}
	
	/** 
	 * Iterates assignment and travel time update a fixed number of times.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param weight Weighting parameter.
	 * @param iterations Number of iterations.
	 */
	public void assignFlowsAndUpdateLinkTravelTimesIterated(ODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, Properties params, double weight, int iterations) {

		for (int i=0; i<iterations; i++) {
			this.resetLinkVolumes(); //link volumes must be reset or they would compound across all iterations
			this.resetTripStorages(); //clear route storages
			this.assignFlowsAndUpdateLinkTravelTimes(passengerODM, freightODM, rsg, params, weight);
		}
	}

	/**
	 * Updates travel time skim matrix (zone-to-zone travel times).
	 * @param timeSkimMatrix Inter-zonal skim matrix (time).
	 */
	public void updateTimeSkimMatrix(SkimMatrix timeSkimMatrix) {
		
		//this.updateLinkTravelTimes();
		
		SkimMatrix counter = new SkimMatrix();
		
		for (Trip trip: this.tripList) {
			
			if (trip.getVehicle() != VehicleType.CAR) continue; //skip freight vehicles
			
			String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
			String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
			
			Double count = counter.getCost(originLAD, destinationLAD);
			if (count == null) count = 0.0;
			counter.setCost(originLAD, destinationLAD, count + 1);
			
			Double sum = timeSkimMatrix.getCost(originLAD, destinationLAD);
			if (sum == null) sum = 0.0;
			double tripTravelTime = trip.getTravelTime(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistance(), this.AVERAGE_ACCESS_EGRESS_SPEED_CAR);
			timeSkimMatrix.setCost(originLAD, destinationLAD, sum + tripTravelTime);
		}
		
		for (MultiKey mk: timeSkimMatrix.getKeySet()) {
			String originLAD = (String) mk.getKey(0);
			String destinationLAD = (String) mk.getKey(1);
			
			double averageODtraveltime = timeSkimMatrix.getCost(originLAD, destinationLAD) / counter.getCost(originLAD, destinationLAD);
	
			timeSkimMatrix.setCost(originLAD, destinationLAD, averageODtraveltime);
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

		//this.updateLinkTravelTimes();
		
		SkimMatrixFreight counter = new SkimMatrixFreight();
		
		for (Trip trip: this.tripList) {
			
			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN)) continue; //skip non-freight vehicles
			
			int origin = trip.getFreightOriginZone();
			int destination = trip.getFreightDestinationZone();
								
			Double count = counter.getCost(origin, destination, vht.value);
			if (count == null) count = 0.0;
			counter.setCost(origin, destination, vht.value, count + 1);
			
			Double sum = timeSkimMatrixFreight.getCost(origin, destination, vht.value);
			if (sum == null) sum = 0.0;
			double tripTravelTime = trip.getTravelTime(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT);
			timeSkimMatrixFreight.setCost(origin, destination, vht.value, sum + tripTravelTime);
		}
		
		for (MultiKey mk: timeSkimMatrixFreight.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicle = (int) mk.getKey(2);
						
			double averageODtraveltime = timeSkimMatrixFreight.getCost(origin, destination, vehicle) / counter.getCost(origin, destination, vehicle);
			timeSkimMatrixFreight.setCost(origin, destination, vehicle, averageODtraveltime);
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
		
		//this.updateLinkTravelTimes();
		
		SkimMatrix counter = new SkimMatrix();
		
		for (Trip trip: this.tripList) {
			
			if (trip.getVehicle() != VehicleType.CAR) continue; //skip freight vehicles
			
			String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
			String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
			
			Double count = counter.getCost(originLAD, destinationLAD);
			if (count == null) count = 0.0;
			counter.setCost(originLAD, destinationLAD, count + 1);
			
			Double sum = costSkimMatrix.getCost(originLAD, destinationLAD);
			if (sum == null) sum = 0.0;
			double tripFuelCost = trip.getCost(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistance(), AVERAGE_ACCESS_EGRESS_SPEED_CAR, this.energyUnitCosts, this.energyConsumptionsPer100km);
			costSkimMatrix.setCost(originLAD, destinationLAD, sum + tripFuelCost);
		}
		
		for (MultiKey mk: costSkimMatrix.getKeySet()) {
			String originLAD = (String) mk.getKey(0);
			String destinationLAD = (String) mk.getKey(1);
			
			double averageODtraveltime = costSkimMatrix.getCost(originLAD, destinationLAD) / counter.getCost(originLAD, destinationLAD);
	
			costSkimMatrix.setCost(originLAD, destinationLAD, averageODtraveltime);
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
		SkimMatrix counter = new SkimMatrix();
		
		for (Trip trip: this.tripList) {
			
			if (trip.getVehicle() != VehicleType.CAR) continue; //skip freight vehicles
			
			String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
			String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
			
			Double count = counter.getCost(originLAD, destinationLAD);
			if (count == null) count = 0.0;
			counter.setCost(originLAD, destinationLAD, count + 1);
			
			Double sum = distanceSkimMatrix.getCost(originLAD, destinationLAD);
			if (sum == null) sum = 0.0;
			double distance = trip.getLength(this.roadNetwork.getNodeToAverageAccessEgressDistance());
			distanceSkimMatrix.setCost(originLAD, destinationLAD, sum + distance);
		}
		
		for (MultiKey mk: distanceSkimMatrix.getKeySet()) {
			String originLAD = (String) mk.getKey(0);
			String destinationLAD = (String) mk.getKey(1);
			
			double averageODtraveltime = distanceSkimMatrix.getCost(originLAD, destinationLAD) / counter.getCost(originLAD, destinationLAD);
	
			distanceSkimMatrix.setCost(originLAD, destinationLAD, averageODtraveltime);
		}
		
		return distanceSkimMatrix;
	}

	/**
	 * Updates cost skim matrix (zone-to-zone distances) for freight.
	 * @return Inter-zonal skim matrix (distance).
	 */
	public SkimMatrixFreight calculateDistanceSkimMatrixFreight() {

		SkimMatrixFreight distanceSkimMatrixFreight = new SkimMatrixFreight();
		SkimMatrixFreight counter = new SkimMatrixFreight();

		for (Trip trip: this.tripList) {
			
			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN)) continue; //skip non-freight vehicles
			
			int origin = trip.getFreightOriginZone();
			int destination = trip.getFreightDestinationZone();
								
			Double count = counter.getCost(origin, destination, vht.value);
			if (count == null) count = 0.0;
			counter.setCost(origin, destination, vht.value, count + 1);
			
			Double sum = distanceSkimMatrixFreight.getCost(origin, destination, vht.value);
			if (sum == null) sum = 0.0;
			double distance = trip.getLength(this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight());
			distanceSkimMatrixFreight.setCost(origin, destination, vht.value, sum + distance);
		}
		
		for (MultiKey mk: distanceSkimMatrixFreight.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicle = (int) mk.getKey(2);
						
			double averageODtraveltime = distanceSkimMatrixFreight.getCost(origin, destination, vehicle) / counter.getCost(origin, destination, vehicle);
			distanceSkimMatrixFreight.setCost(origin, destination, vehicle, averageODtraveltime);
		}

		return distanceSkimMatrixFreight;
	}


	/**
	 * Updates cost skim matrix (zone-to-zone financial costs) for freight.
	 * @param costSkimMatrixFreight Inter-zonal skim matrix (cost) for freight.
	 */
	public void updateCostSkimMatrixFreight(SkimMatrixFreight costSkimMatrixFreight) {

		//this.updateLinkTravelTimes();
		SkimMatrixFreight counter = new SkimMatrixFreight();
		
		if (this.tripList == null || this.tripList.size() == 0) System.err.println("TripList is empty!");
		
		for (Trip trip: this.tripList) {
			
			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN)) continue; //skip non-freight vehicles
			
			int origin = trip.getFreightOriginZone();
			int destination = trip.getFreightDestinationZone();
							
			Double count = counter.getCost(origin, destination, vht.value);
			if (count == null) count = 0.0;
			counter.setCost(origin, destination, vht.value, count + 1);
			
			Double sum = costSkimMatrixFreight.getCost(origin, destination, vht.value);
			if (sum == null) sum = 0.0;
			double tripFuelCost = trip.getCost(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT, this.energyUnitCosts, this.energyConsumptionsPer100km);
			
			costSkimMatrixFreight.setCost(origin, destination, vht.value, sum + tripFuelCost);
		}
		
		for (MultiKey mk: costSkimMatrixFreight.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicle = (int) mk.getKey(2);
						
			double averageODcost = costSkimMatrixFreight.getCost(origin, destination, vehicle) / counter.getCost(origin, destination, vehicle);
			costSkimMatrixFreight.setCost(origin, destination, vehicle, averageODcost);
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

		HashMap<EngineType, Double> consumptions = new HashMap<EngineType, Double>();
		for (EngineType engine: EngineType.values()) {
			consumptions.put(engine, 0.0);
		}
				
		for (Trip trip: this.tripList) {
			
			if (trip.getVehicle() != VehicleType.CAR) continue; //skip freight vehicles
			EngineType et = trip.getEngine();
			double consumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistance(), AVERAGE_ACCESS_EGRESS_SPEED_CAR, this.energyConsumptionsPer100km);

			Double currentConsumption = consumptions.get(et);
			if (currentConsumption == null) currentConsumption = 0.0;
			
			consumptions.put(et, currentConsumption + consumption);
		}
		
		return consumptions;
	}

	/**
	 * Calculates spatial energy consumption for each car engine type (in litres for fuels and in kWh for electricity).
	 * @param originZoneEnergyWeight Percentage of energy consumption assigned to origin zone (the rest assigned to destination zone).
	 * @return Zonal consumption for each engine type.
	 */
	public HashMap<EngineType, HashMap<String, Double>> calculateZonalCarEnergyConsumptions(final double originZoneEnergyWeight) {

		//initialise hashmaps
		HashMap<EngineType, HashMap<String, Double>> zonalConsumptions = new HashMap<EngineType, HashMap<String, Double>>();
		for (EngineType engine: EngineType.values()) {
			HashMap<String, Double> consumption = new HashMap<String, Double>();
			zonalConsumptions.put(engine, consumption);
		}
		
		for (Trip trip: this.tripList) {
			
			if (trip.getVehicle() != VehicleType.CAR) continue; //skip freight vehicles
			
			EngineType et = trip.getEngine();
			double tripConsumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT, this.energyConsumptionsPer100km);

			String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
			String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
			
			Double currentConsumptionOrigin = zonalConsumptions.get(et).get(originLAD);
			if (currentConsumptionOrigin == null) currentConsumptionOrigin = 0.0;
			
			Double currentConsumptionDestination = zonalConsumptions.get(et).get(destinationLAD);
			if (currentConsumptionDestination == null) currentConsumptionDestination = 0.0;
			
			currentConsumptionOrigin += originZoneEnergyWeight * tripConsumption;
			currentConsumptionDestination += (1.0 - originZoneEnergyWeight) * tripConsumption;
			
			zonalConsumptions.get(et).put(originLAD, currentConsumptionOrigin);
			zonalConsumptions.get(et).put(destinationLAD, currentConsumptionDestination);
		}
		
		return zonalConsumptions;
	}

	/**
	 * Calculates total energy consumption for each freight vehicle engine type (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each engine type.
	 */
	public HashMap<EngineType, Double> calculateFreightEnergyConsumptions() {

		HashMap<EngineType, Double> consumptions = new HashMap<EngineType, Double>();
		for (EngineType engine: EngineType.values()) {
			consumptions.put(engine, 0.0);
		}
		
		for (Trip trip: this.tripList) {
			
			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN)) continue; //skip non-freight vehicles
			
			EngineType et = trip.getEngine();
			double consumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT, this.energyConsumptionsPer100km);

			Double currentConsumption = consumptions.get(et);
			if (currentConsumption == null) currentConsumption = 0.0;
			
			consumptions.put(et, currentConsumption + consumption);
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
	 * Calculate peak-hour link densities (PCU/lane/km/hr).
	 * @return Peak-hour link densities.
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

//	/**
//	 * @return The sum of all link travel times in the network.
//	 */
//	public double getTotalLinkTravelTimes() {
//
//		double totalTravelTime = 0.0;
//		for (Integer key: this.linkTravelTime.keySet()) totalTravelTime += linkTravelTime.get(key);
//
//		return totalTravelTime;
//	}
//
	/**
	 * @return The copy of all link travel times.
	 */
	public Map<TimeOfDay, Map<Integer, Double>> getCopyOfLinkTravelTimes() {

		Map<TimeOfDay, Map<Integer, Double>> linkTravelTimes = new HashMap<TimeOfDay, Map<Integer, Double>>();
		for (TimeOfDay hour: TimeOfDay.values()) {
			Map<Integer, Double> hourlyMap = new HashMap<Integer, Double>();
			for (Integer edge: this.linkTravelTimePerTimeOfDay.get(hour).keySet())
				hourlyMap.put(edge, this.linkTravelTimePerTimeOfDay.get(hour).get(edge));
			linkTravelTimes.put(hour, hourlyMap);
		}
			
		return linkTravelTimes;
	}

//	/**
//	 * Calculates the sum of absolute differences in link travel times.
//	 * @param other Link travel times to compare with.
//	 * @return Sum of absolute differences in link travel times.
//	 */
//	public double getAbsoluteDifferenceInLinkTravelTimes(HashMap<Integer, Double> other) {
//
//		double difference = 0.0;
//		for (Integer key: this.linkTravelTime.keySet())
//			difference += Math.abs(this.linkTravelTime.get(key) - other.get(key));
//
//		return difference;
//	}
	
	/**
	 * Getter method for the road network.
	 * @return Road network.
	 */
	public RoadNetwork getRoadNetwork() {
		
		return this.roadNetwork;
	}
	
	/**
	 * Saves assignment results to output file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
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
		header.add("peakHourtravelTime");
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
				record.add(Double.toString(this.linkTravelTimePerTimeOfDay.get(TimeOfDay.EIGHTAM).get(edge.getID())));
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
	 * Saves hourly car volumes to output file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
	 */
	public void saveHourlyCarVolumes(int year, String outputFile) {

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("edgeID");
		header.add("roadNumber");
		header.add("CP");
		header.add("direction");
		header.add("countCar");
		header.add("dailyVolume");
		for (TimeOfDay hour: TimeOfDay.values()) header.add(hour.toString());
		
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
				}
				else //ferry or a newly developed road with no count point
					record.add("N/A");
					
				//Integer linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge.getID());
				Double linkVolume = this.linkVolumesInPCU.get(edge.getID());
				
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString((int)Math.round(linkVolume)));
			
				for (TimeOfDay hour: TimeOfDay.values()) {
					Double linkVolumeInPCU = this.linkVolumesInPCUPerTimeOfDay.get(hour).get(edge.getID()); //TODO there should be one per vehicle type
					if (linkVolumeInPCU == null) record.add(Integer.toString(0));
					else 					record.add(Integer.toString((int)Math.round(linkVolumeInPCU)));
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
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
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
	 * Saves zonal car energy consumptions to an output file.
	 * @param year Assignment year.
	 * @param originZoneEnergyWeight Percentage of energy consumption assigned to origin zone (the rest assigned to destination zone).
	 * @param outputFile Output file name (with path).
	 */
	public void saveZonalCarEnergyConsumptions(int year, final double originZoneEnergyWeight, String outputFile) {

		//calculate energy consumptions
		HashMap<EngineType, HashMap<String, Double>> energyConsumptions = this.calculateZonalCarEnergyConsumptions(originZoneEnergyWeight);
		Set<String> zones = energyConsumptions.get(EngineType.ELECTRICITY).keySet();

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("zone");
		for (EngineType et: EngineType.values()) header.add(et.name());

		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);

			for (String zone: zones) {
				ArrayList<String> record = new ArrayList<String>();
				record.add(Integer.toString(year));
				record.add(zone);
				for (int i=2; i<header.size(); i++)	{
					EngineType et = EngineType.valueOf(header.get(i));
					double consumption = energyConsumptions.get(et).get(zone);
					record.add(String.format("%.2f", consumption));
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
	 * Saves zonal vehicle-kilometres.
	 * @param year Assignment year.
	 * @param outputFile Output file name (with path).
	 */
	public void saveZonalVehicleKilometres(int year, String outputFile) {

		Map<String, Double> vehicleKilometres = this.calculateVehicleKilometres();
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("zone");
		header.add("vehicleKm");

		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (Entry<String, Double> entry: vehicleKilometres.entrySet()) {
				record.clear();
				record.add(Integer.toString(year));
				record.add(entry.getKey());
				record.add(String.format("%.2f", entry.getValue()));
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
	 * Saves peak link point capacities into a file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
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
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
	 */
	public void saveLinkTravelTimes (int year, String outputFile) {

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("edgeID");
		header.add("freeFlow");
		for (TimeOfDay hour: TimeOfDay.values()) header.add(hour.toString());
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
				for (TimeOfDay hour: TimeOfDay.values())
					record.add(Double.toString(this.linkTravelTimePerTimeOfDay.get(hour).get(edge.getID())));
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
	public Map<Integer, Double> getLinkVolumesInPCU() {

		return this.linkVolumesInPCU;
	}

//	/**
//	 * Getter method for the link travel times.
//	 * @return Link volumes
//	 */
//	public HashMap<Integer, Double> getLinkTravelTimes() {
//
//		return this.linkTravelTime;
//	}

	/**
	 * Getter method for the link free-flow travel times.
	 * @return Link volumes
	 */
	public HashMap<Integer, Double> getLinkFreeFlowTravelTimes() {

		return this.linkFreeFlowTravelTime;
	}
	
	/**
	 * Getter method for the link travel times per time of day.
	 * @return Link travel times per time of day.
	 */
	public Map<TimeOfDay, Map<Integer, Double>> getLinkTravelTimes() {

		return this.linkTravelTimePerTimeOfDay;
	}
	
	/**
	 * Getter method for the trip list.
	 * @return Trip list.
	 */
	public ArrayList<Trip> getTripList() {
		
		return this.tripList;
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
	public HashMap<Integer, Double> getStartNodeProbabilities() {

		return this.startNodeProbabilities;
	}

	/**
	 * Getter method for node probabilities.
	 * @return Node probabilities.
	 */
	public HashMap<Integer, Double> getEndNodeProbabilities() {

		return this.endNodeProbabilities;
	}

	/**
	 * Getter method for workplace zones probabilities.
	 * @return Workplace zones probabilities.
	 */
	public HashMap<String, Double> getWorkplaceZoneProbabilities() {

		return this.workplaceZoneProbabilities;
	}


	/**
	 * Calculates the number of car trips starting in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateLADTripStarts() {

		HashMap<String, Integer> totalLADnoTripStarts = new HashMap<String, Integer>();
		
		for (Trip trip: this.tripList)
			if (trip.getVehicle() == VehicleType.CAR) {
				String originZone = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
				Integer tripStarts = totalLADnoTripStarts.get(originZone);
				if (tripStarts == null) tripStarts = 0;
				totalLADnoTripStarts .put(originZone, tripStarts + 1);
			}

		return totalLADnoTripStarts;
	}
	
	/**
	 * Calculates the number of car trips ending in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateLADTripEnds() {

		HashMap<String, Integer> totalLADnoTripEnds = new HashMap<String, Integer>();
		
		for (Trip trip: this.tripList) 
			if (trip.getVehicle() == VehicleType.CAR) {
				String destinationZone = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
				Integer tripEnds = totalLADnoTripEnds.get(destinationZone);
				if (tripEnds == null) tripEnds = 0;
				totalLADnoTripEnds.put(destinationZone, tripEnds + 1);
			}

		return totalLADnoTripEnds;
	}

	/**
	 * Calculates the number of freight trips starting in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateFreightLADTripStarts() {

		HashMap<String, Integer> totalLADnoTripStarts = new HashMap<String, Integer>();
		
		for (Trip trip: this.tripList)
			if (trip.getVehicle() != VehicleType.CAR) {
				String originZone = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
				Integer tripStarts = totalLADnoTripStarts.get(originZone);
				if (tripStarts == null) tripStarts = 0;
				totalLADnoTripStarts .put(originZone, tripStarts + 1);
			}

		return totalLADnoTripStarts;
	}

	/**
	 * Calculates the number of freight trips ending in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateFreightLADTripEnds() {

		HashMap<String, Integer> totalLADnoTripEnds = new HashMap<String, Integer>();
		
		for (Trip trip: this.tripList) 
			if (trip.getVehicle() != VehicleType.CAR) {
				String destinationZone = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
				Integer tripEnds = totalLADnoTripEnds.get(destinationZone);
				if (tripEnds == null) tripEnds = 0;
				totalLADnoTripEnds.put(destinationZone, tripEnds + 1);
			}

		return totalLADnoTripEnds;
	}
	
	/**
	 * Calculates vehicle kilometres in each LAD.
	 * Ignores access and egress.
	 * @return Vehicle kilometres.
	 */
	public Map<String, Double> calculateVehicleKilometres() {
	
		Map<String, Double> vehicleKilometres = new HashMap<String, Double>();
			
		for (Map.Entry<Integer, Double> pair: linkVolumesInPCU.entrySet()) {
			Integer edgeID = pair.getKey();
			Double volume = pair.getValue();
			String zone = this.roadNetwork.getEdgeToZone().get(edgeID);
			if (zone != null) {
				//fetch current value
				Double vehkm = vehicleKilometres.get(zone);
				if (vehkm == null) vehkm = 0.0;
				//get edge length
				DirectedEdge edge = (DirectedEdge)this.roadNetwork.getEdgeIDtoEdge().get(edgeID);
				SimpleFeature sf = (SimpleFeature)edge.getObject();
				Double length = (Double)sf.getAttribute("LenNet");
				vehkm += volume * length;
				//store new value
				vehicleKilometres.put(zone,  vehkm);
				continue;
			}
		}
		
		return vehicleKilometres;
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
	 * Setter method for energy type fractions.
	 * @param engineType The type of a car engine.
	 * @param engineTypeFraction Engine type fraction.
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
		
		//reset link volumes per time of day
		this.linkVolumesInPCUPerTimeOfDay = new HashMap<TimeOfDay, Map<Integer, Double>>();
		for (TimeOfDay hour: TimeOfDay.values()) {
			Map<Integer, Double> hourlyMap = new HashMap<Integer, Double>();
			this.linkVolumesInPCUPerTimeOfDay.put(hour, hourlyMap);
		}
	}

	/**
	 * Resets route storages for passengers and freight.
	 */
	public void resetTripStorages () {

		this.tripList = new ArrayList<Trip>();
	}
	
	/**
	 * Calculates link volumes in PCU per time of day.
	 * @param tripList Trip list.
	 * @return Map of link volumes in PCU per time of day
	 */
	public Map<TimeOfDay, Map<Integer, Double>> calculateLinkVolumeInPCUPerTimeOfDay(List<Trip> tripList) {
		
		//Map<Object, Long> collect = tripList.stream().collect(Collectors.groupingBy(t -> t.getTimeOfDay(), Collectors.counting()));
		
		//Long countOfPetrolTrips2 = tripList.parallelStream().filter(t -> t.getEngine() == EngineType.PETROL).count();
		
		Map<TimeOfDay, Map<Integer, Double>> map = new HashMap<TimeOfDay, Map<Integer, Double>>();
		for (TimeOfDay hour: TimeOfDay.values()) {
			Map<Integer, Double> hourlyMap = new HashMap<Integer, Double>();
			map.put(hour, hourlyMap);
		}
			
		for (Trip trip: tripList) {
			Map<Integer, Double> hourlyMap = map.get(trip.getTimeOfDay());
			for (Edge edge: trip.getRoute().getEdges()) {
				Double currentCount = hourlyMap.get(edge.getID());
				if (currentCount == null) currentCount = 0.0;
				currentCount += this.vehicleTypeToPCU.get(trip.getVehicle()); //add PCU of the vehicle
				hourlyMap.put(edge.getID(), currentCount);
			}
		}
		
		return map;
	}
	
	/**
	 * Updates link volumes in PCU per time of day from object's trip list and stores into instance variable.
	 */
	public void updateLinkVolumeInPCUPerTimeOfDay() {
		
		this.linkVolumesInPCUPerTimeOfDay = this.calculateLinkVolumeInPCUPerTimeOfDay(this.tripList);
	}
	
	/**
	 * Getter method for link volumes in PCU per time of day.
	 * @return Link volumes in PCU per time of day.
	 */
	public Map<TimeOfDay, Map<Integer, Double>> getLinkVolumeInPCUPerTimeOfDay() {
		
		return this.linkVolumesInPCUPerTimeOfDay;
	}
	
	/**
	 * Calculates absolute differences between car volumes and traffic counts.
	 * For combined counts, takes the average of two absolute differences.
	 * @return Absolute differences between car volumes and traffic counts.
	 */
	public HashMap<Integer, Double> calculateAbsDifferenceCarCounts () {
		
		HashMap<Integer, Double> absoluteDifferences = new HashMap<Integer, Double>();
		
		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
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

				absoluteDifferences.put(edge.getID(), 1.0 * Math.abs(carCount - carVolume));
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

				long absoluteDifference = Math.abs(carCount - carVolume - carVolume2) / 2;
				checkedCP.add(countPoint);
				absoluteDifferences.put(edge.getID(), 1.0 * absoluteDifference);
			}
		}

		return absoluteDifferences;
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