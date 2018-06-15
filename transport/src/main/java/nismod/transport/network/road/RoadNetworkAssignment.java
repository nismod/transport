/**
 * 
 */
package nismod.transport.network.road;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.utility.RandomSingleton;
import nismod.transport.zone.NodeMatrix;
import nismod.transport.zone.Zoning;

/**
 * Network assignment of origin-destination flows.
 * @author Milan Lovric
 */
public class RoadNetworkAssignment {

	private final static Logger LOGGER = LogManager.getLogger(RoadNetworkAssignment.class);
	
	//	//public static final double SPEED_LIMIT_M_ROAD = 112.65; //70mph = 31.29mps = 112.65kph
	//	//public static final double SPEED_LIMIT_A_ROAD = 96.56; //60mph = 26.82mps = 96.56kph

	public int maximumCapacityMRoad; //PCU per lane per hour
	public int maximumCapacityARoad; //PCU per lane per hour
	public double averageAccessEgressSpeedCar; //kph  
	public double averageAccessEgressSpeedFreight; //kph 
	public double peakHourPercentage;
	public double alpha;
	public double betaMRoad;
	public double betaARoad;
	public boolean flagIntrazonalAssignmentReplacement; //true means that origin and destination nodes can be the same
	public boolean flagAStarIfEmptyRouteSet; //if there is no pre-generated route set for a node pair, try finding a route with aStar
	public int interzonalTopNodes; //how many top nodes (based on gravitated population size) to considers as trip origin/destination
	public double averageIntersectionDelay; //[min]
	public double nodesProbabilityWeighting; //manipulates probabilities of nodes for the node choice
	public double nodesProbabilityWeightingFreight; //manipulates probabilities of nodes for the node choice
	public double assignmentFraction; //the fraction of vehicle flows to actually assign, with later results expansion to 100%
	public boolean flagUseRouteChoiceModel; //use route-choice model (true) or routing with A-Star (false)
	public int topTemproNodes = 2;
	
	private static RandomSingleton rng = RandomSingleton.getInstance();

	public static enum EngineType {
		ICE_PETROL, //internal combustion engine - petrol
		ICE_DIESEL, //internal combustion engine - diesel
		ICE_LPG, //internal combustion engine - LPG
		ICE_H2, //internal combustion engine - hydrogen
		ICE_CNG, //internal combustion engine - CNG
		HEV_PETROL, //hybrid electric vehicle - petrol
		HEV_DIESEL, //hybrid electric vehicle - diesel
		FCEV_H2, //fuel cell electric vehicle - hydrogen
		PHEV_PETROL, //plug-in hybrid electric vehicle - petrol
		PHEV_DIESEL, //plug-in hybrid electric vehicle - diesel
		BEV //battery electric vehicle
	}
	
	public static enum EnergyType {
		PETROL, DIESEL, LPG, ELECTRICITY, HYDROGEN, CNG
	}

	public static enum VehicleType {
		CAR(0), ARTIC(1), RIGID(2), VAN(3), CAR_AV(4), ARTIC_AV(5), RIGID_AV(6), VAN_AV(7);
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

	private HashMap<EnergyType, Double> energyUnitCosts;
	private HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions;
	private HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiencies;
	private HashMap<VehicleType, HashMap<EngineType, Double>> engineTypeFractions;
	private HashMap<VehicleType, Double> AVFractions;

	private HashMap<TimeOfDay, Double> timeOfDayDistribution;
	private HashMap<TimeOfDay, Double> timeOfDayDistributionFreight;
	
	private HashMap<EnergyType, Double> unitCO2Emissions;

	private RoadNetwork roadNetwork;

	private Map<Integer, Double> linkVolumesInPCU;
	private Map<VehicleType, Map<Integer, Integer>> linkVolumesPerVehicleType;
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

	private HashMap<String, MultiKeyMap> congestionCharges; //String is the policy name, MultiKeyMap is (VehicleType, TimeOfDay) -> list of link charges

	/**
	 * @param roadNetwork Road network.
	 * @param energyUnitCosts Energy unit costs.
	 * @param unitCO2Emissions Unit CO2 emissions.
	 * @param engineTypeFractions Market shares of different engine/fuel types.
	 * @param fractionsAV Fraction of autonomous vehicles for different vehicle types.
	 * @param vehicleTypeToPCU Vehicle to PCU conversion.
	 * @param energyConsumptionParams Base fuel consumption rates.
	 * @param relativeFuelEfficiencies Relative fuel efficiencies (compared to base year).
	 * @param timeOfDayDistribution Time of day distribution.
	 * @param timeOfDayDistributionFreight Time of day distribution for freight.
	 * @param defaultLinkTravelTime Default link travel times.
	 * @param areaCodeProbabilities Probabilities of trips starting/ending in each census output area.
	 * @param workplaceZoneProbabilities Probabilities of freight trips starting/ending in each census output area.
	 * @param congestionCharges Congestion charges.
	 * @param params Assignment parameters.
	 */
	public RoadNetworkAssignment(RoadNetwork roadNetwork, 
			HashMap<EnergyType, Double> energyUnitCosts, 
			HashMap<EnergyType, Double> unitCO2Emissions,
			HashMap<VehicleType, HashMap<EngineType, Double>> engineTypeFractions,
			HashMap<VehicleType, Double> fractionsAV,
			HashMap<VehicleType, Double> vehicleTypeToPCU,
			HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptionParams,
			HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiencies,
			HashMap<TimeOfDay, Double> timeOfDayDistribution,
			HashMap<TimeOfDay, Double> timeOfDayDistributionFreight, 
			Map<TimeOfDay, Map<Integer, Double>> defaultLinkTravelTime, 
			HashMap<String, Double> areaCodeProbabilities, 
			HashMap<String, Double> workplaceZoneProbabilities,
			HashMap<String, MultiKeyMap> congestionCharges,
			Properties params) {

		this.roadNetwork = roadNetwork;
		this.linkVolumesInPCU = new HashMap<Integer, Double>();
		this.linkVolumesPerVehicleType = new HashMap<VehicleType, Map<Integer, Integer>>();
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
		this.linkFreeFlowTravelTime = roadNetwork.getFreeFlowTravelTime();

		if (defaultLinkTravelTime == null) { //use free flow
			LOGGER.debug("No link travel time provided, using free-flow link travel time.");
			for (TimeOfDay hour: TimeOfDay.values()) {
				Map<Integer, Double> hourlyMap = this.linkTravelTimePerTimeOfDay.get(hour);
				for (Integer edgeID: this.linkFreeFlowTravelTime.keySet())
					hourlyMap.put(edgeID, this.linkFreeFlowTravelTime.get(edgeID));
			}
		}
		else //otherwise copy
			for (TimeOfDay hour: TimeOfDay.values()) {
				Map<Integer, Double> hourlyMap = this.linkTravelTimePerTimeOfDay.get(hour);
				for (Integer edgeID: defaultLinkTravelTime.get(hour).keySet())
					hourlyMap.put(edgeID, defaultLinkTravelTime.get(hour).get(edgeID));
			}

		if (vehicleTypeToPCU != null) 	this.vehicleTypeToPCU = vehicleTypeToPCU;
		else 							LOGGER.error("Missing vehicle type to PCU conversion.");


		if (energyUnitCosts != null) 	this.energyUnitCosts = energyUnitCosts;
		else 							LOGGER.error("Missing energy unit costs.");
		
		if (unitCO2Emissions != null) 	this.unitCO2Emissions = unitCO2Emissions;
		else 							LOGGER.error("Missing unit CO2 emissions.");

		if (energyConsumptionParams != null) 	this.energyConsumptions = energyConsumptionParams;
		else									LOGGER.error("Missing energy consumption parameters.");
		
		if (relativeFuelEfficiencies != null) 	this.relativeFuelEfficiencies = relativeFuelEfficiencies;
		else									LOGGER.error("Missing relative fuel efficiencies.");

		if (engineTypeFractions != null) 	this.engineTypeFractions = engineTypeFractions;
		else								LOGGER.error("Missing engine type fractions.");
	
		if (timeOfDayDistribution != null) 	this.timeOfDayDistribution = timeOfDayDistribution; //TODO check it adds up to one!
		else 								LOGGER.error("Missing time of day distribution.");
		
		if (timeOfDayDistributionFreight != null) 	this.timeOfDayDistributionFreight = timeOfDayDistributionFreight; //TODO check it adds up to one!
		else 								LOGGER.error("Missing time of day distribution for freight.");

		if (fractionsAV != null)	this.AVFractions = fractionsAV;
		else						LOGGER.error("Missing fractions of autonomous vehicles.");

		this.congestionCharges = congestionCharges;
		//System.out.println("Congestion charges: " + this.congestionCharges);

		//read the parameters
		this.maximumCapacityMRoad = Integer.parseInt(params.getProperty("MAXIMUM_CAPACITY_M_ROAD"));
		this.maximumCapacityARoad = Integer.parseInt(params.getProperty("MAXIMUM_CAPACITY_A_ROAD"));
		this.averageAccessEgressSpeedCar = Double.parseDouble(params.getProperty("AVERAGE_ACCESS_EGRESS_SPEED_CAR"));  
		this.averageAccessEgressSpeedFreight = Double.parseDouble(params.getProperty("AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT")); 
		this.peakHourPercentage = this.timeOfDayDistribution.get(TimeOfDay.EIGHTAM);
		this.alpha = Double.parseDouble(params.getProperty("ALPHA"));
		this.betaMRoad = Double.parseDouble(params.getProperty("BETA_M_ROAD"));
		this.betaARoad = Double.parseDouble(params.getProperty("BETA_A_ROAD"));
		this.flagIntrazonalAssignmentReplacement = Boolean.parseBoolean(params.getProperty("FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT"));
		this.flagAStarIfEmptyRouteSet = Boolean.parseBoolean(params.getProperty("FLAG_ASTAR_IF_EMPTY_ROUTE_SET")); //if there is no pre-generated route set for a node pair, try finding a route with aStar
		this.interzonalTopNodes = Integer.parseInt(params.getProperty("INTERZONAL_TOP_NODES")); //how many top nodes (based on gravitated population size) to considers as trip origin/destination
		this.averageIntersectionDelay = Double.parseDouble(params.getProperty("AVERAGE_INTERSECTION_DELAY"));
		this.nodesProbabilityWeighting = Double.parseDouble(params.getProperty("NODES_PROBABILITY_WEIGHTING"));
		this.nodesProbabilityWeightingFreight = Double.parseDouble(params.getProperty("NODES_PROBABILITY_WEIGHTING_FREIGHT"));
		this.assignmentFraction = Double.parseDouble(params.getProperty("ASSIGNMENT_FRACTION"));
		this.flagUseRouteChoiceModel = Boolean.parseBoolean(params.getProperty("USE_ROUTE_CHOICE_MODEL")); //use route-choice model (true) or routing with A-Star (false)
		
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
				if (!roadNetwork.isBlacklistedAsStartNode(node)) sumStart += Math.pow(roadNetwork.getGravitatingPopulation(node), nodesProbabilityWeighting);
				if (!roadNetwork.isBlacklistedAsEndNode(node))   sumEnd += Math.pow(roadNetwork.getGravitatingPopulation(node), nodesProbabilityWeighting);
			}
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				if (!roadNetwork.isBlacklistedAsStartNode(node)) startNodeProbabilities.put(node, Math.pow(roadNetwork.getGravitatingPopulation(node), nodesProbabilityWeighting) / sumStart);
				if (!roadNetwork.isBlacklistedAsEndNode(node))   endNodeProbabilities.put(node, Math.pow(roadNetwork.getGravitatingPopulation(node), nodesProbabilityWeighting) / sumEnd);
			}
		}
		//System.out.println("Probabilities for nodes:");
		//System.out.println(this.startNodeProbabilities);
		//System.out.println(this.endNodeProbabilities);

		//calculate node probabilities from gravitating workplace population (ignores blacklisted nodes)
		startNodeProbabilitiesFreight = new HashMap<Integer, Double>();
		endNodeProbabilitiesFreight = new HashMap<Integer, Double>();
		//System.out.println(roadNetwork.getZoneToNodes().keySet());
		for (String zone: roadNetwork.getZoneToNodes().keySet()) {
			double sumStart = 0.0, sumEnd = 0.0;
			//System.out.println(roadNetwork.getZoneToNodes().get(zone));
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				if (!roadNetwork.isBlacklistedAsStartNode(node)) sumStart += Math.pow(roadNetwork.getGravitatingWorkplacePopulation(node), nodesProbabilityWeightingFreight);
				if (!roadNetwork.isBlacklistedAsEndNode(node))   sumEnd += Math.pow(roadNetwork.getGravitatingWorkplacePopulation(node), nodesProbabilityWeightingFreight);
			}
			for (Integer node: roadNetwork.getZoneToNodes().get(zone)) {
				if (!roadNetwork.isBlacklistedAsStartNode(node)) startNodeProbabilitiesFreight.put(node, Math.pow(roadNetwork.getGravitatingWorkplacePopulation(node), nodesProbabilityWeightingFreight) / sumStart);
				if (!roadNetwork.isBlacklistedAsEndNode(node))   endNodeProbabilitiesFreight.put(node, Math.pow(roadNetwork.getGravitatingWorkplacePopulation(node), nodesProbabilityWeightingFreight) / sumEnd);
			}
		}
		//System.out.println("Node probabilities for freight:");
		//System.out.println(this.startNodeProbabilitiesFreight);
		//System.out.println(this.endNodeProbabilitiesFreight);
	}
	
	/** 
	 * Assigns passenger origin-destination matrix to the road network using A-star routing algorithm.
	 * Calculates the fastest path based on the current values in the linkTravlinkTravelTimePerTimeOfDayelTime instance field,
	 * however only one route will be used for the same OD pair (the route that was calculated first).
	 * @param passengerODM Passenger origin-destination matrix with flows to be assigned.
	 * @param rsg To store routes during the assignment (reduces the number of routing calls).
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsRouting(ODMatrix passengerODM, RouteSetGenerator rsg) {

		LOGGER.info("Assigning the passenger flows from the passenger matrix...");

		//sort nodes based on the gravitating population
		this.roadNetwork.sortGravityNodes();

		//to store routes generated during the assignment
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork);
		
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
	
			//calculate number of trip assignments
			int flow = (int) Math.floor(passengerODM.getFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

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
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				random  = rng.nextDouble();
				VehicleType vht = null;
				if (Double.compare(1.0 - AVFractions.get(VehicleType.CAR_AV), random) > 0)
					vht = VehicleType.CAR;
				else 
					vht = VehicleType.CAR_AV;
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.get(vht).entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				if (engine == null) LOGGER.warn("Engine type not chosen!");

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

				if (originNode == null) LOGGER.warn("Origin node was not chosen!");

				//choose destination node
				cumulativeProbability = 0.0;
				Integer destinationNode = null;
				random = rng.nextDouble();
				//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
				//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
				if (!flagIntrazonalAssignmentReplacement && originZone.equals(destinationZone) && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
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

				if (destinationNode == null) LOGGER.warn("Destination node was not chosen!");

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
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}

						foundRoute = new Route(fastestPath);
						rsg.addRoute(foundRoute); //add to the route set
					}

					int multiplier = 1;
					if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
					counterAssignedTrips += multiplier;

					//store trip in trip list
					Trip trip = new Trip(vht, engine, foundRoute, hour, 0, 0, multiplier);
					this.tripList.add(trip);

				} catch (Exception e) {
					LOGGER.error(e);
					LOGGER.error("Couldn't find path from node {} to node {}!", from.getID(), to.getID());
				}
			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: {}", counterTotalFlow);
		LOGGER.debug("Total assigned trips: {}", counterAssignedTrips);
		LOGGER.debug("Succesfully assigned trips: {}", 100.0* counterAssignedTrips / counterTotalFlow);
	}
	
	/** 
	 * Assigns passenger origin-destination matrix to the road network using A-star routing algorithm.
	 * Calculates the fastest path based on the current values in the linkTravlinkTravelTimePerTimeOfDayelTime instance field,
	 * which means different fastest routes may be used in different hours of the day.
	 * @param passengerODM Passenger origin-destination matrix with flows to be assigned.
	 * @param routeStorage Stores routes for each hour of the day separately.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsHourlyRouting(ODMatrix passengerODM, HashMap<TimeOfDay, RouteSetGenerator> routeStorage) {

		LOGGER.info("Assigning the passenger flows from the passenger matrix...");

		//sort nodes based on the gravitating population
		this.roadNetwork.sortGravityNodes();

		//to store routes generated during the assignment
		if (routeStorage == null) {
			routeStorage = new HashMap<TimeOfDay, RouteSetGenerator>();
			for (TimeOfDay hour: TimeOfDay.values()) {
				routeStorage.put(hour, new RouteSetGenerator(this.roadNetwork));
			}
		}
		
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
	
			//calculate number of trip assignments
			int flow = (int) Math.floor(passengerODM.getFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

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
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				random  = rng.nextDouble();
				VehicleType vht = null;
				if (Double.compare(1.0 - AVFractions.get(VehicleType.CAR_AV), random) > 0)
					vht = VehicleType.CAR;
				else 
					vht = VehicleType.CAR_AV;
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.get(vht).entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				if (engine == null) LOGGER.warn("Engine type not chosen!");

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

				if (originNode == null) LOGGER.warn("Origin node was not chosen!");

				//choose destination node
				cumulativeProbability = 0.0;
				Integer destinationNode = null;
				random = rng.nextDouble();
				//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
				//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
				if (!flagIntrazonalAssignmentReplacement && originZone.equals(destinationZone) && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
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

				if (destinationNode == null) LOGGER.warn("Destination node was not chosen!");

				DirectedGraph rn = roadNetwork.getNetwork();
				//set source and destination node
				Node from = roadNetwork.getNodeIDtoNode().get(originNode);
				Node to = roadNetwork.getNodeIDtoNode().get(destinationNode);
				//					System.out.println("from " + from + " to " + to);
				Route foundRoute = null;
				try {

					//see if that route already exists in the route storage
					RouteSetGenerator rsg = routeStorage.get(hour);
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
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}

						foundRoute = new Route(fastestPath);
						rsg.addRoute(foundRoute); //add to the route set
					}

					int multiplier = 1;
					if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
					counterAssignedTrips += multiplier;

					//store trip in trip list
					Trip trip = new Trip(vht, engine, foundRoute, hour, 0, 0, multiplier);
					this.tripList.add(trip);

				} catch (Exception e) {
					LOGGER.error(e);
					LOGGER.error("Couldnt find path from node {} to node {}!", from.getID(), to.getID());
				}
			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: {}", + counterTotalFlow);
		LOGGER.debug("Total assigned trips: {}", counterAssignedTrips);
		LOGGER.debug("Succesfully assigned trips: {}", 100.0* counterAssignedTrips / counterTotalFlow);
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

		LOGGER.info("Assigning the passenger flows from the passenger matrix...");
		
		if (passengerODM == null) { LOGGER.warn("Passenger OD matrix is null! Skipping assignment."); return; }
		if (rsg == null) { LOGGER.warn("Route set generator is null! Skipping assignment."); return; }
		if (routeChoiceParameters == null) { LOGGER.warn("Route choice parameters are null! Skipping assignment."); return; }
		
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

			//calculate number of trip assignments
			int flow = (int) Math.floor(passengerODM.getFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

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
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				random  = rng.nextDouble();
				VehicleType vht = null;
				if (Double.compare(1.0 - AVFractions.get(VehicleType.CAR_AV), random) > 0)
					vht = VehicleType.CAR;
				else 
					vht = VehicleType.CAR_AV;
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.get(vht).entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				if (engine == null) LOGGER.warn("Engine type not chosen!");

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

					if (originNode == null) LOGGER.warn("Origin node for intra-zonal trip was not chosen!");

					//choose destination node
					cumulativeProbability = 0.0;
					random = rng.nextDouble();
					//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
					//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
					if (!flagIntrazonalAssignmentReplacement && listOfDestinationNodes.contains(originNode)) { //no replacement
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

					if (destinationNode == null) LOGGER.warn("Destination for intra-zonal trip node was not chosen!");

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
					int originNodesToConsider = interzonalTopNodes<listOfOriginNodes.size()?interzonalTopNodes:listOfOriginNodes.size();
					int destinationNodesToConsider = interzonalTopNodes<listOfDestinationNodes.size()?interzonalTopNodes:listOfDestinationNodes.size();
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

					if (originNode == null) LOGGER.warn("Origin node for inter-zonal trip was not chosen!");
					if (destinationNode == null) LOGGER.warn("Destination node for inter-zonal trip was not chosen!");
				}

				Route chosenRoute = null;
				RouteSet fetchedRouteSet = rsg.getRouteSet(originNode.intValue(), destinationNode.intValue());
				if (fetchedRouteSet == null) {
//					LOGGER.warn("Can't fetch the route set between nodes {} and {}!", originNode, destinationNode);

					if (!flagAStarIfEmptyRouteSet)	continue;
					else { //try finding a path with aStar
//						LOGGER.debug("Trying the astar!");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

						//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}
						chosenRoute = new Route(fastestPath);
						if (chosenRoute.isEmpty()) {
							LOGGER.warn("Empty route between nodes {} and {}!", originNode, destinationNode);
							continue;
						}
						//store generated route into the rsg!
						rsg.addRoute(chosenRoute);
					}
				} else { //there is a route set

					//if only one route in the route set, do not calculate utilities and probabilities, but choose that route
					if (fetchedRouteSet.getSize() == 1) {

						LOGGER.trace("There is just one route in the route set, so choosing that route.");
						//choose that route
						chosenRoute = fetchedRouteSet.getChoiceSet().get(0);

					} else { //choose a route using a discrete-choice model

						LOGGER.trace("There are multiple route in the route set, so choosing with a route-choice model.");
						//if (fetchedRouteSet.getProbabilities() == null) {
						//probabilities need to be calculated for this route set before a choice can be made
						fetchedRouteSet.setLinkTravelTime(this.linkTravelTimePerTimeOfDay.get(hour));
						fetchedRouteSet.setParameters(routeChoiceParameters);

						//fetch congestion charge for the vehicle type
						//HashMap<String, HashMap<Integer, Double>> linkCharges = null;
						HashMap<String, HashMap<Integer, Double>> linkCharges = new HashMap<String, HashMap<Integer, Double>>();
						if (this.congestionCharges != null) 
							for (String policyName: this.congestionCharges.keySet()) {
								//System.out.println("Policy = " + policyName);
								//System.out.println("vht = " + vht + " hour = " + hour);
								//System.out.println("Congestion charges: " + (HashMap<Integer, Double>) this.congestionCharges.get(policyName).get(vht, hour));
								linkCharges.put(policyName, (HashMap<Integer, Double>) this.congestionCharges.get(policyName).get(vht, hour));
							}

						fetchedRouteSet.calculateUtilities(vht, engine, this.linkTravelTimePerTimeOfDay.get(hour), this.energyConsumptions, this.relativeFuelEfficiencies, this.energyUnitCosts, linkCharges, routeChoiceParameters);
						fetchedRouteSet.calculateProbabilities(this.linkTravelTimePerTimeOfDay.get(hour), routeChoiceParameters);
						fetchedRouteSet.sortRoutesOnUtility();
						//}

						//choose the route
						chosenRoute = fetchedRouteSet.choose(routeChoiceParameters);
					}

					if (chosenRoute == null) {
						LOGGER.warn("No chosen route between nodes {} and {}!", originNode, destinationNode);
						continue;
					}
				}

				if (chosenRoute.isEmpty()) {
					LOGGER.warn("The chosen route is empty, skipping this trip!");
					continue;
				}

				int multiplier = 1;
				if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
				counterAssignedTrips += multiplier;

				//store trip in trip list
				Trip trip = new Trip(vht, engine, chosenRoute, hour, 0, 0, multiplier);
				this.tripList.add(trip);

			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: " + counterTotalFlow);
		LOGGER.debug("Total assigned trips: " + counterAssignedTrips);
		LOGGER.debug("Succesfully assigned trips percentage: " + 100.0* counterAssignedTrips / counterTotalFlow);
	}
	

	/** 
	 * Assigns passenger origin-destination matrix to the road network using the Tempro zoning system.
	 * Calculates the fastest path based on the current values in the linkTravelTime instance field.
	 * @param passengerODM Passenger origin-destination matrix with flows to be assigned.
	 * @param zoning Contains Tempro zone information.
	 * @param rsg Route set (here new routes will be stored).
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsTempro(ODMatrix passengerODM, Zoning zoning, RouteSetGenerator rsg) {

		LOGGER.info("Assigning the passenger flows from the tempro passenger matrix...");

		//to store routes generated during the assignment
		//RouteSetGenerator rsg = new RouteSetGenerator(this.roadNetwork);
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork);

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

			/*
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
	
			*/
	
	
			//calculate number of trip assignments
			int flow = (int) Math.floor(passengerODM.getFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

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
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				random  = rng.nextDouble();
				VehicleType vht = null;
				if (Double.compare(1.0 - AVFractions.get(VehicleType.CAR_AV), random) > 0)
					vht = VehicleType.CAR;
				else 
					vht = VehicleType.CAR_AV;
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.get(vht).entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				if (engine == null) LOGGER.warn("Engine type not chosen!");
				
				
				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node
				
				//choose origin/destination node
				Integer originNode = null;
				Integer destinationNode = null;
		
				if (originZone.equals(destinationZone)) { 	//if inter-zonal, pick random node within the zone (based on gravitating population is better)
				
					List<Integer> listOfContainedNodes = zoning.getZoneToListOfContaintedNodes().get(originZone);
	
					//if there are no zones in that node, simply pick the closest node
					if (listOfContainedNodes == null) { 
						originNode = zoning.getZoneToNearestNodeIDMap().get(originZone);
						destinationNode = originNode;
					} else if (listOfContainedNodes.size() == 1) { //if there is just one contained node, use it
						originNode = listOfContainedNodes.get(0);
						destinationNode = originNode;
					} else	{
		
						
						//simply pick random
						originNode = listOfContainedNodes.get(rng.nextInt(listOfContainedNodes.size()));
						destinationNode = listOfContainedNodes.get(rng.nextInt(listOfContainedNodes.size()));
						
						
						/*
						//choose based on gravitating population
						double sumOfGravitatingPopulation = 0.0;
						for (Integer nodeID: listOfContainedNodes) sumOfGravitatingPopulation += this.roadNetwork.getGravitatingPopulation(nodeID); 
						cumulativeProbability = 0.0;
						random = rng.nextDouble();
						for (Integer nodeID: listOfContainedNodes) {
							cumulativeProbability += this.roadNetwork.getGravitatingPopulation(nodeID) / sumOfGravitatingPopulation;
							if (Double.compare(cumulativeProbability, random) > 0) {
								originNode = nodeID;
								break;
							}
						}
						cumulativeProbability = 0.0;
						random = rng.nextDouble();
						for (Integer nodeID: listOfContainedNodes) {
							cumulativeProbability += this.roadNetwork.getGravitatingPopulation(nodeID) / sumOfGravitatingPopulation;
							if (Double.compare(cumulativeProbability, random) > 0) {
								destinationNode = nodeID;
								break;
							}
						}
						*/
						
						/*
						//pick OD pair based on NodeMatrix
						NodeMatrix nm = zoning.getZoneToNodeMatrix().get(originZone);
						cumulativeProbability = 0.0;
						random = rng.nextDouble();
						for (MultiKey multiKey: nm.getKeySet()) {
							Integer originNodeID = (Integer) multiKey.getKey(0);
							Integer destinationNodeID = (Integer) multiKey.getKey(1);
							cumulativeProbability += nm.getValue(originNodeID, destinationNodeID);
							if (Double.compare(cumulativeProbability, random) > 0) {
								originNode = originNodeID;
								destinationNode = destinationNodeID;
								break;
							}
						}
						*/
					}
								
					if (originNode == null) LOGGER.warn("Origin node was not chosen for zone {}", originZone);
					if (destinationNode == null) LOGGER.warn("Destination node was not chosen!");
								
				} else { //if not interzonal, choose from topnodes based on the distance from zone centroid
				
				//choose origin node
				List<Pair<Integer, Double>> listOfOriginNodes = zoning.getZoneToSortedListOfNodeAndDistancePairs().get(originZone);
				//consider only top tempro nodes
				List<Pair<Integer, Double>> listOfTopOriginNodes = listOfOriginNodes.subList(0, this.topTemproNodes);
				
				double sumOfDistances = 0.0;
				for (Pair<Integer, Double> pair: listOfTopOriginNodes) sumOfDistances += pair.getValue(); 
				cumulativeProbability = 0.0;
				//Integer originNode = null;
				random = rng.nextDouble();
				for (Pair<Integer, Double> pair: listOfTopOriginNodes) {
					cumulativeProbability += pair.getRight() / sumOfDistances;
					if (Double.compare(cumulativeProbability, random) > 0) {
						originNode = pair.getKey();
						break;
					}
				}
				
				if (originNode == null) LOGGER.warn("Origin node was not chosen for zone {}", originZone);
								
				//take the nearest node!
				//Integer originNode = zoning.getZoneToNearestNodeIDMap().get(originZone);
				//if (originNode == null) System.err.println("Origin node was not chosen for zone " + originZone);
				
				if (this.roadNetwork.isBlacklistedAsStartNode(originNode)) 
					LOGGER.warn("Origin node is blacklisted! node: {}", originNode);
				

				//choose destination node
				List<Pair<Integer, Double>> listOfDestinationNodes = zoning.getZoneToSortedListOfNodeAndDistancePairs().get(destinationZone);
				//consider only top tempro nodes
				List<Pair<Integer, Double>> listOfTopDestinationNodes = listOfDestinationNodes.subList(0, this.topTemproNodes);
				
				sumOfDistances = 0.0;
				for (Pair<Integer, Double> pair: listOfTopDestinationNodes) sumOfDistances += pair.getValue(); 
				cumulativeProbability = 0.0;
				//Integer destinationNode = null;
				random = rng.nextDouble();
				for (Pair<Integer, Double> pair: listOfTopDestinationNodes) {
					cumulativeProbability += pair.getRight() / sumOfDistances;
					if (Double.compare(cumulativeProbability, random) > 0) {
						destinationNode = pair.getKey();
						break;
					}
				}

				if (destinationNode == null) LOGGER.warn("Destination node was not chosen!");
			
				//take just the nearest one
				//Integer destinationNode = zoning.getZoneToNearestNodeIDMap().get(destinationZone);
				//if (destinationNode == null) System.err.println("Destination node was not chosen for zone " + destinationZone);
				
				if (this.roadNetwork.isBlacklistedAsEndNode(destinationNode)) 
					LOGGER.warn("Destination node is blacklisted! node: {}", destinationNode);
				
				}
				
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
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}

						foundRoute = new Route(fastestPath);
						rsg.addRoute(foundRoute); //add to the route set
					}

					int multiplier = 1;
					if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
					counterAssignedTrips += multiplier;

					//store trip in trip list
					Integer originZoneID = zoning.getZoneCodeToIDMap().get(originZone);
					Integer destinationZoneID = zoning.getZoneCodeToIDMap().get(destinationZone);
					Trip trip = new TripTempro(vht, engine, foundRoute, hour, originZoneID, destinationZoneID, zoning, multiplier);
					this.tripList.add(trip);

				} catch (Exception e) {
					LOGGER.error(e);
					LOGGER.error("Couldnt find path from node {} to node {}!", from.getID(), to.getID());
				}
			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: " + counterTotalFlow);
		LOGGER.debug("Total assigned trips: " + counterAssignedTrips);
		LOGGER.debug("Succesfully assigned trips: " + 100.0* counterAssignedTrips / counterTotalFlow);
	}

	/** 
	 * Assigns passenger origin-destination matrix to the road network using the Tempro zoning system.
	 * Calculates the fastest path based on the current values in the linkTravelTime instance field.
	 * @param passengerODM Passenger origin-destination matrix with flows to be assigned.
	 * @param zoning Contains Tempro zone information.
	 * @param rsg Route set generator containing the routes.
	 * @param routeChoiceParameters Route choice parameters.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsRouteChoiceTempro(ODMatrix passengerODM, Zoning zoning, RouteSetGenerator rsg, Properties routeChoiceParameters) {

		LOGGER.info("Assigning the passenger flows from the tempro passenger matrix...");

		//to store routes generated during the assignment
		//RouteSetGenerator rsg = new RouteSetGenerator(this.roadNetwork);
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork);

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

			/*
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
	
			*/
	
	
			//calculate number of trip assignments
			int flow = (int) Math.floor(passengerODM.getFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

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
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				random  = rng.nextDouble();
				VehicleType vht = null;
				if (Double.compare(1.0 - AVFractions.get(VehicleType.CAR_AV), random) > 0)
					vht = VehicleType.CAR;
				else 
					vht = VehicleType.CAR_AV;
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.get(vht).entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				if (engine == null) LOGGER.warn("Engine type not chosen!");

				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node

				//choose origin node
				
				/*
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
				*/
				
				Integer originNode = zoning.getZoneToNearestNodeIDMap().get(originZone);
				if (originNode == null) LOGGER.warn("Origin node was not chosen for zone {}", originZone);
				
				if (this.roadNetwork.isBlacklistedAsStartNode(originNode)) 
					LOGGER.warn("Origin node is blacklisted! node: {}", originNode);
				

				//choose destination node
				/*
				cumulativeProbability = 0.0;
				Integer destinationNode = null;
				random = rng.nextDouble();
				//if intrazonal trip and replacement is not allowed, the probability of the originNode should be 0 so it cannot be chosen again
				//also, in that case it is important to rescale other node probabilities (now that the originNode is removed) by dividing with (1.0 - p(originNode))!
				if (!flagIntrazonalAssignmentReplacement && originZone.equals(destinationZone) && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
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
				*/
				
				
				Integer destinationNode = zoning.getZoneToNearestNodeIDMap().get(destinationZone);
				if (destinationNode == null) LOGGER.warn("Destination node was not chosen for zone {}", destinationZone);
				
				if (this.roadNetwork.isBlacklistedAsEndNode(destinationNode)) 
					LOGGER.warn("Destination node is blacklisted! node: {}", destinationNode);
				
				DirectedGraph rn = roadNetwork.getNetwork();
				//set source and destination node
				Node from = roadNetwork.getNodeIDtoNode().get(originNode);
				Node to = roadNetwork.getNodeIDtoNode().get(destinationNode);
				//					System.out.println("from " + from + " to " + to);
				
				Route chosenRoute = null;
				RouteSet fetchedRouteSet = rsg.getRouteSet(originNode.intValue(), destinationNode.intValue());
				if (fetchedRouteSet == null) {
					LOGGER.warn("Can't fetch the route set between nodes {} and {}!", originNode, destinationNode);

					if (!flagAStarIfEmptyRouteSet)	continue;
					else { //try finding a path with aStar
						LOGGER.debug("Trying the astar!");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

						//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}
						chosenRoute = new Route(fastestPath);
						if (chosenRoute.isEmpty()) {
							LOGGER.warn("Empty route between nodes {} and {}!", originNode, destinationNode);
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

					//fetch congestion charge for the vehicle type
					//HashMap<String, HashMap<Integer, Double>> linkCharges = null;
					HashMap<String, HashMap<Integer, Double>> linkCharges = new HashMap<String, HashMap<Integer, Double>>();
					if (this.congestionCharges != null) 
						for (String policyName: this.congestionCharges.keySet())
							linkCharges.put(policyName, (HashMap<Integer, Double>) this.congestionCharges.get(policyName).get(vht, hour));

					fetchedRouteSet.calculateUtilities(vht, engine, this.linkTravelTimePerTimeOfDay.get(hour), this.energyConsumptions, this.relativeFuelEfficiencies, this.energyUnitCosts, linkCharges, routeChoiceParameters);
					fetchedRouteSet.calculateProbabilities(this.linkTravelTimePerTimeOfDay.get(hour), routeChoiceParameters);
					fetchedRouteSet.sortRoutesOnUtility();
					//}

					//choose the route
					chosenRoute = fetchedRouteSet.choose(routeChoiceParameters);
					if (chosenRoute == null) {
						LOGGER.warn("No chosen route between nodes {} and {}", originNode, destinationNode);
						continue;
					}
				}

				if (chosenRoute.isEmpty()) {
					LOGGER.warn("The chosen route is empty, skipping this trip!");
					continue;
				}

				int multiplier = 1;
				if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
				counterAssignedTrips += multiplier;

				//store trip in trip list
				Integer originZoneID = zoning.getZoneCodeToIDMap().get(originZone);
				Integer destinationZoneID = zoning.getZoneCodeToIDMap().get(destinationZone);
				Trip trip = new TripTempro(vht, engine, chosenRoute, hour, originZoneID, destinationZoneID, zoning, multiplier);
				this.tripList.add(trip);

			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: {}", counterTotalFlow);
		LOGGER.debug("Total assigned trips: {}", counterAssignedTrips);
		LOGGER.debug("Succesfully assigned trips: {}", 100.0* counterAssignedTrips / counterTotalFlow);
	}

	/**
	 * Assigns freight origin-destination matrix to the road network using A-star routing.
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
	 * @param rsg Route storage (reduces the number of routing calls).
	 */
	@SuppressWarnings("unused")
	public void assignFreightFlowsRouting(FreightMatrix freightMatrix, RouteSetGenerator rsg) {


		LOGGER.info("Assigning the vehicle flows from the freight matrix...");

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		//to store routes generated during the assignment
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork);

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

			//calculate number of trip assignments
			int flow = (int) Math.floor(freightMatrix.getFlow(origin, destination, vehicleType) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = freightMatrix.getFlow(origin, destination, vehicleType)  - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += freightMatrix.getFlow(origin, destination, vehicleType);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

				//choose time of day
				double cumulativeProbability = 0.0;
				double random = rng.nextDouble();
				TimeOfDay hour = null;
				for (Map.Entry<TimeOfDay, Double> entry : timeOfDayDistributionFreight.entrySet()) {
					TimeOfDay key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						hour = key;
						break;
					}
				}
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//get freight vehicle type from the freight matrix value
				VehicleType vht = VehicleType.values()[vehicleType];
				//find autonomous counterpart
				VehicleType avht = null;
				if (vht == VehicleType.VAN) 		avht = VehicleType.VAN_AV;
				else if (vht == VehicleType.RIGID) 	avht = VehicleType.RIGID_AV;
				else if (vht == VehicleType.ARTIC) 	avht = VehicleType.ARTIC_AV;
								
				//choose vehicle
				random  = rng.nextDouble();
				if (Double.compare(AVFractions.get(avht), random) > 0) vht = avht;
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.get(vht).entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				if (engine == null) LOGGER.warn("Engine type not chosen!");

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
					if (!flagIntrazonalAssignmentReplacement && origin == destination && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
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

				if (originNode == null) LOGGER.warn("Could not find origin node for a freight trip!");
				if (destinationNode == null) LOGGER.warn("Could not find destination node for a freight trip!");


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
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
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
							LOGGER.warn("Not even aStar could find a route!");
							continue;
						}

						foundRoute = new Route(fastestPath);
					}

					int multiplier = 1;
					if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
					counterAssignedTrips += multiplier;
					
					//trip was assigned
					counterAssignedTrips += multiplier;

					//store trip in trip list
					Trip trip = new Trip(vht, engine, foundRoute, hour, origin, destination, multiplier);
					this.tripList.add(trip);

				} catch (Exception e) {
					LOGGER.error(e);
				}
			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: {}", counterTotalFlow);
		LOGGER.debug("Total assigned trips: {}", counterAssignedTrips);
		LOGGER.debug("Successfully assigned trips percentage: {}", 100.0* counterAssignedTrips / counterTotalFlow);
	}

	/**
	 * Assigns freight origin-destination matrix to the road network using A-star routing.
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
	 * @param routeStorage Route storage (stores fastest routes separately for each hour of the day).
	 */
	@SuppressWarnings("unused")
	public void assignFreightFlowsHourlyRouting(FreightMatrix freightMatrix, HashMap<TimeOfDay, RouteSetGenerator> routeStorage) {


		LOGGER.info("Assigning the vehicle flows from the freight matrix...");

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		//to store routes generated during the assignment
		if (routeStorage == null) {
			routeStorage = new HashMap<TimeOfDay, RouteSetGenerator>();
			for (TimeOfDay hour: TimeOfDay.values()) {
				routeStorage.put(hour, new RouteSetGenerator(this.roadNetwork));
			}
		}

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

			//calculate number of trip assignments
			int flow = (int) Math.floor(freightMatrix.getFlow(origin, destination, vehicleType) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = freightMatrix.getFlow(origin, destination, vehicleType)  - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += freightMatrix.getFlow(origin, destination, vehicleType);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

				//choose time of day
				double cumulativeProbability = 0.0;
				double random = rng.nextDouble();
				TimeOfDay hour = null;
				for (Map.Entry<TimeOfDay, Double> entry : timeOfDayDistributionFreight.entrySet()) {
					TimeOfDay key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						hour = key;
						break;
					}
				}
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//get freight vehicle type from the freight matrix value
				VehicleType vht = VehicleType.values()[vehicleType];
				//find autonomous counterpart
				VehicleType avht = null;
				if (vht == VehicleType.VAN) 		avht = VehicleType.VAN_AV;
				else if (vht == VehicleType.RIGID) 	avht = VehicleType.RIGID_AV;
				else if (vht == VehicleType.ARTIC) 	avht = VehicleType.ARTIC_AV;
								
				//choose vehicle
				random  = rng.nextDouble();
				if (Double.compare(AVFractions.get(avht), random) > 0) vht = avht;
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.get(vht).entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				if (engine == null) LOGGER.warn("Engine type not chosen!");

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
					if (!flagIntrazonalAssignmentReplacement && origin == destination && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
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

				if (originNode == null) LOGGER.warn("Could not find origin node for a freight trip!");
				if (destinationNode == null) LOGGER.warn("Could not find destination node for a freight trip!");


				DirectedGraph rn = roadNetwork.getNetwork();
				//set source and destination node
				Node from = roadNetwork.getNodeIDtoNode().get(originNode);
				Node to = roadNetwork.getNodeIDtoNode().get(destinationNode);
				//					System.out.println("from " + from + " to " + to);
				Route foundRoute = null;
				try {
					//see if that route already exists in the route storage
					RouteSetGenerator rsg = routeStorage.get(hour);
					RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
					if (rs != null) foundRoute = rs.getChoiceSet().get(0); //take the first route

					//if route does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundRoute == null) {
						//System.out.println("The path does not exist in the path storage");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
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
							LOGGER.warn("Not even aStar could find a route!");
							continue;
						}

						foundRoute = new Route(fastestPath);
					}

					int multiplier = 1;
					if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
					counterAssignedTrips += multiplier;

					//store trip in trip list
					Trip trip = new Trip(vht, engine, foundRoute, hour, origin, destination, multiplier);
					this.tripList.add(trip);

				} catch (Exception e) {
					LOGGER.error(e);
				}
			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: {}", counterTotalFlow);
		LOGGER.debug("Total assigned trips: {}", counterAssignedTrips);
		LOGGER.debug("Successfully assigned trips percentage: {}", 100.0* counterAssignedTrips / counterTotalFlow);
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

		LOGGER.info("Assigning the vehicle flows from the freight matrix...");

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

			//calculate number of trip assignments
			int flow = (int) Math.floor(freightMatrix.getFlow(origin, destination, vehicleType) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = freightMatrix.getFlow(origin, destination, vehicleType)  - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += freightMatrix.getFlow(origin, destination, vehicleType);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

				//choose time of day
				double cumulativeProbability = 0.0;
				double random = rng.nextDouble();
				TimeOfDay hour = null;
				for (Map.Entry<TimeOfDay, Double> entry : timeOfDayDistributionFreight.entrySet()) {
					TimeOfDay key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						hour = key;
						break;
					}
				}
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//get freight vehicle type from the freight matrix value
				VehicleType vht = VehicleType.values()[vehicleType];
				//find autonomous counterpart
				VehicleType avht = null;
				if (vht == VehicleType.VAN) 		avht = VehicleType.VAN_AV;
				else if (vht == VehicleType.RIGID) 	avht = VehicleType.RIGID_AV;
				else if (vht == VehicleType.ARTIC) 	avht = VehicleType.ARTIC_AV;
								
				//choose vehicle
				random  = rng.nextDouble();
				if (Double.compare(AVFractions.get(avht), random) > 0) vht = avht;
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				cumulativeProbability = 0.0;
				random = rng.nextDouble();
				EngineType engine = null;
				for (Map.Entry<EngineType, Double> entry : engineTypeFractions.get(vht).entrySet()) {
					EngineType key = entry.getKey();
					Double value = entry.getValue();	
					cumulativeProbability += value;
					if (Double.compare(cumulativeProbability, random) > 0) {
						engine = key;
						break;
					}
				}
				if (engine == null) LOGGER.warn("Engine type not chosen!");

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
					LOGGER.warn("Origin node {} is blacklisted, skipping this trip!", originNode);
					continue;
				}
				if (destinationNode != null && roadNetwork.isBlacklistedAsEndNode(destinationNode))	{
					LOGGER.warn("Destination node {} is blacklisted, skipping this trip!", destinationNode);
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
						if (!flagIntrazonalAssignmentReplacement && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
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
						int originNodesToConsider = interzonalTopNodes<listOfOriginNodes.size()?interzonalTopNodes:listOfOriginNodes.size();
						int destinationNodesToConsider = interzonalTopNodes<listOfDestinationNodes.size()?interzonalTopNodes:listOfDestinationNodes.size();
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

						if (originNode == null) LOGGER.warn("Origin node for inter-zonal freight trip was not chosen!");
						if (destinationNode == null) LOGGER.warn("Destination node for inter-zonal freight trip was not chosen!");

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
						if (!flagIntrazonalAssignmentReplacement && listOfDestinationNodes.contains(originNode)) { //no replacement and intra-zonal trip
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
						int indexDestination = rng.nextInt(interzonalTopNodes<listOfDestinationNodes.size()?interzonalTopNodes:listOfDestinationNodes.size());
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
						if (!flagIntrazonalAssignmentReplacement && listOfOriginNodes.contains(destinationNode)) { //no replacement and intra-zonal trip
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
						int indexOrigin = rng.nextInt(interzonalTopNodes<listOfOriginNodes.size()?interzonalTopNodes:listOfOriginNodes.size());
						originNode = listOfOriginNodes.get(indexOrigin);
					}


				} else if (originNode != null && destinationNode != null) { //point to point

					if (originNode == destinationNode) 
						LOGGER.debug("Point-to-point freight trip in which both points are mapped to the same network node.");
				}

				if (originNode == null) LOGGER.warn("Could not find origin node for a freight trip!");
				if (destinationNode == null) LOGGER.warn("Could not find destination node for a freight trip!");

				Route chosenRoute = null;
				RouteSet fetchedRouteSet = rsg.getRouteSet(originNode.intValue(), destinationNode.intValue());
				if (fetchedRouteSet == null) {
					LOGGER.warn("Can't fetch the route set between nodes {} and {}!", originNode, destinationNode);

					if (!flagAStarIfEmptyRouteSet && originNode != destinationNode)	continue;
					else { //try finding a path with aStar
						
						if (originNode == destinationNode) 	LOGGER.trace("Generating a single node trip because origin and destination node are the same.");
						else 								LOGGER.debug("Trying the astar to find a missing route from origin to destination node.");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(originNode);
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode().get(destinationNode);

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}
						chosenRoute = new Route(fastestPath);
						if (chosenRoute.isEmpty()) {
							LOGGER.warn("Empty route between nodes {} and {}!", originNode, destinationNode);
							continue;
						}

						//store generated route into the rsg!
						rsg.addRoute(chosenRoute);
					}
				} else { //there is a route set
					
					//if only one route in the route set, do not calculate utilities and probabilities, but choose that route
					if (fetchedRouteSet.getSize() == 1) {
						
						LOGGER.trace("There is just one route in the route set, so choosing that route.");
						//choose that route
						chosenRoute = fetchedRouteSet.getChoiceSet().get(0);

					} else { //choose a route using a discrete-choice model

						LOGGER.trace("There are multiple route in the route set, so choosing with a route-choice model.");
						//if (fetchedRouteSet.getProbabilities() == null) {
						//probabilities need to be calculated for this route set before a choice can be made
						fetchedRouteSet.setLinkTravelTime(this.linkTravelTimePerTimeOfDay.get(hour));
						fetchedRouteSet.setParameters(routeChoiceParameters);

						//fetch congestion charge for the vehicle type
						HashMap<String, HashMap<Integer, Double>> linkCharges = new HashMap<String, HashMap<Integer, Double>>();
						if (this.congestionCharges != null) 
							for (String policyName: this.congestionCharges.keySet())
								linkCharges.put(policyName, (HashMap<Integer, Double>) this.congestionCharges.get(policyName).get(vht, hour));

						fetchedRouteSet.calculateUtilities(vht, engine, this.linkTravelTimePerTimeOfDay.get(hour), this.energyConsumptions, this.relativeFuelEfficiencies, this.energyUnitCosts, linkCharges, routeChoiceParameters);
						fetchedRouteSet.calculateProbabilities(this.linkTravelTimePerTimeOfDay.get(hour), routeChoiceParameters);
						fetchedRouteSet.sortRoutesOnUtility();
						//}

						//choose the route
						chosenRoute = fetchedRouteSet.choose(routeChoiceParameters);
					}
					
					if (chosenRoute == null) {
						LOGGER.warn("No chosen route between nodes {} and {}!", originNode, destinationNode);
						continue;
					}
				}

				if (chosenRoute.isEmpty()) {
					LOGGER.warn("The chosen route is empty, skipping this trip!");
					continue;
				}

				int multiplier = 1;
				if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
				counterAssignedTrips += multiplier;

				//check to which LAD chosen origin and destination nodes belong to!
				originLAD = roadNetwork.getNodeToZone().get(originNode);
				destinationLAD = roadNetwork.getNodeToZone().get(destinationNode);

				//store trip in trip list
				Trip trip = new Trip(vht, engine, chosenRoute, hour, origin , destination, multiplier);
				this.tripList.add(trip);

			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: {}", counterTotalFlow);
		LOGGER.debug("Total assigned trips: {}", counterAssignedTrips);
		LOGGER.debug("Successfully assigned trips percentage: {}", 100.0* counterAssignedTrips / counterTotalFlow);
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
					else if (flow < 2330) speed = ((34.55 - 69.96) / (2330 - 1398) * (flow - 1398) + 69.96) * 1.609344; //[kph]
					else {
						//flow higher than maximum (user over-capacity formula from WebTAG)
						double E = flow / 2330;
						double B = 0.5;
						double speedC = 34.55;
						double tc = roadLength / speedC; //h
						speed = roadLength / (tc + B * (E - 1.0));
						//System.out.println("Overcapacity speed:  " + speed);
					}

					congestedTravelTime = roadLength / speed * 60; //[min]

				} else if (roadNumber.charAt(0) == 'A') {//A-road

					double flow = linkVolumeInPCU / numberOfLanes.get(edge.getID());
					double speed = 0.0;
					if (flow < 1251) speed = ((50.14 - 56.05) / 1251 * flow + 56.05) * 1.609344; //[kph]
					else if (flow < 1740) speed = ((27.22 - 50.14) / (1740 - 1251) * (flow - 1251) + 50.14) * 1.609344; //[kph]
					else {
						//flow higher than maximum (user over-capacity formula from WebTAG)
						double E = flow / 1740;
						double B = 0.5;
						double speedC = 27.22;
						double tc = roadLength / speedC; //h
						speed = roadLength / (tc + B * (E - 1.0));
						//System.out.println("Overcapacity speed:  " + speed);
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
					else if (flow < 2330) speed = ((34.55 - 69.96) / (2330 - 1398) * (flow - 1398) + 69.96) * 1.609344; //[kph]
					else {
						//flow higher than maximum (user over-capacity formula from WebTAG)
						double E = flow / 2330;
						double B = 0.5;
						double speedC = 34.55;
						double tc = roadLength / speedC; //h
						speed = roadLength / (tc + B * (E - 1.0));
						//System.out.println("Overcapacity speed:  " + speed);
					}
					congestedTravelTime = roadLength / speed * 60; //[min]

				} else if (roadNumber.charAt(0) == 'A') {//A-road

					double flow = linkVolumeInPCU / numberOfLanes.get(edge.getID());
					double speed = 0.0;
					if (flow < 1251) speed = ((50.14 - 56.05) / 1251 * flow + 56.05) * 1.609344; //[kph]
					else if (flow < 1740) speed = ((27.22 - 50.14) / (1740 - 1251) * (flow - 1251) + 50.14) * 1.609344; //[kph]
					else {
						//flow higher than maximum (user over-capacity formula from WebTAG)
						double E = flow / 1740;
						double B = 0.15;
						double speedC = 27.22;
						double tc = roadLength / speedC; //h
						speed = roadLength / (tc + B * (E - 1.0));
						//System.out.println("Overcapacity speed:  " + speed);
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
	 * Finally, updates link travel times using weighted averaging.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param rsg Route set generator to store fastest routes generated during the assignment (but could be pregenerated too).
	 * @param weight Weighting parameter.
	 */
	public void assignFlowsAndUpdateLinkTravelTimes(ODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, double weight) {

		this.assignPassengerFlowsRouting(passengerODM, rsg);
		this.assignFreightFlowsRouting(freightODM, rsg);
		this.updateLinkVolumeInPCU();
		this.updateLinkVolumeInPCUPerTimeOfDay();
		this.updateLinkVolumePerVehicleType();
		this.updateLinkTravelTimes(weight);
	}

	/** 
	 * Assigns passenger and freight origin-destination matrix to the road network
	 * using the fastest path based on the current values in the linkTravelTime field.
	 * Finally, updates link travel times using weighted averaging.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param rsg Route set generator object with routes to be used for the assignment (if route choice used) or an object in which to store routes (if routing used).
	 * @param params Parameters from the config file.
	 * @param weight Weighting parameter.
	 */
	public void assignFlowsAndUpdateLinkTravelTimes(ODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, Properties params, double weight) {

		final Boolean flagUseRouteChoiceModel = Boolean.parseBoolean(params.getProperty("USE_ROUTE_CHOICE_MODEL"));
		
		if (flagUseRouteChoiceModel) {
			this.assignPassengerFlowsRouteChoice(passengerODM, rsg, params);
			this.assignFreightFlowsRouteChoice(freightODM, rsg, params);
		} else {
			this.assignPassengerFlowsRouting(passengerODM, rsg);
			this.assignFreightFlowsRouting(freightODM, rsg);
		}
		this.updateLinkVolumeInPCU();
		this.updateLinkVolumeInPCUPerTimeOfDay();
		this.updateLinkVolumePerVehicleType();
		this.updateLinkTravelTimes(weight);
	}

	/** 
	 * Iterates assignment and travel time update a fixed number of times.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param rsg Route set generator object with routes to be used for the assignment (if route choice used) or an object in which to store routes (if routing used).
	 * @param weight Weighting parameter.
	 * @param iterations Number of iterations.
	 */
	public void assignFlowsAndUpdateLinkTravelTimesIterated(ODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, double weight, int iterations) {

		for (int i=0; i<iterations; i++) {
			this.resetLinkVolumes(); //link volumes must be reset or they would compound across all iterations
			this.resetTripStorages(); //clear route storages
			this.assignFlowsAndUpdateLinkTravelTimes(passengerODM, freightODM, rsg, weight);
		}
	}

	/** 
	 * Iterates assignment and travel time update a fixed number of times.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param rsg Route set generator object with routes to be used for the assignment (if route choice used) or an object in which to store routes (if routing used).
	 * @param params Parameters from the config file.
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
			double tripTravelTime = trip.getTravelTime(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), averageIntersectionDelay, this.roadNetwork.getNodeToAverageAccessEgressDistance(), averageAccessEgressSpeedCar);
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
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN ||
					vht == VehicleType.ARTIC_AV || vht == VehicleType.RIGID_AV || vht == VehicleType.VAN_AV)) continue; //skip non-freight vehicles
			
			//map AV types to non-AV types
			if (vht == VehicleType.ARTIC_AV) vht = VehicleType.ARTIC;
			if (vht == VehicleType.RIGID_AV) vht = VehicleType.RIGID;
			if (vht == VehicleType.VAN_AV) vht = VehicleType.VAN;			

			int origin = trip.getFreightOriginZone();
			int destination = trip.getFreightDestinationZone();

			Double count = counter.getCost(origin, destination, vht.value);
			if (count == null) count = 0.0;
			counter.setCost(origin, destination, vht.value, count + 1);

			Double sum = timeSkimMatrixFreight.getCost(origin, destination, vht.value);
			if (sum == null) sum = 0.0;
			double tripTravelTime = trip.getTravelTime(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.averageIntersectionDelay, this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), this.averageAccessEgressSpeedFreight);
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
			int multiplier = trip.getMultiplier();

			Double count = counter.getCost(originLAD, destinationLAD);
			if (count == null) count = 0.0;
			counter.setCost(originLAD, destinationLAD, count + multiplier);

			Double sum = costSkimMatrix.getCost(originLAD, destinationLAD);
			if (sum == null) sum = 0.0;
			double tripFuelCost = trip.getCost(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistance(), averageAccessEgressSpeedCar, this.energyUnitCosts, this.energyConsumptions, this.relativeFuelEfficiencies, this.congestionCharges);
			costSkimMatrix.setCost(originLAD, destinationLAD, sum + tripFuelCost * multiplier);
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

			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles

			String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
			String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
			int multiplier = trip.getMultiplier();

			Double count = counter.getCost(originLAD, destinationLAD);
			if (count == null) count = 0.0;
			counter.setCost(originLAD, destinationLAD, count + multiplier);

			Double sum = distanceSkimMatrix.getCost(originLAD, destinationLAD);
			if (sum == null) sum = 0.0;
			double distance = trip.getLength(this.roadNetwork.getNodeToAverageAccessEgressDistance());
			distanceSkimMatrix.setCost(originLAD, destinationLAD, sum + distance * multiplier);
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
	 * Updates cost skim matrix (zone-to-zone distances).
	 * @return Inter-zonal skim matrix (distance).
	 */
	public SkimMatrix calculateDistanceSkimMatrixTempro() {

		SkimMatrix distanceSkimMatrix = new SkimMatrix();
		SkimMatrix counter = new SkimMatrix();

		for (Trip trip: this.tripList) {

			if (trip instanceof TripTempro && (trip.getVehicle() == VehicleType.CAR || trip.getVehicle() == VehicleType.CAR_AV)) {

				TripTempro temproTrip = (TripTempro) trip;
				
				String originZone = temproTrip.getOriginTemproZone();
				String destinationZone = temproTrip.getDestinationTemproZone();
				int multiplier = trip.getMultiplier();

				Double count = counter.getCost(originZone, destinationZone);
				if (count == null) count = 0.0;
				counter.setCost(originZone, destinationZone, count + multiplier);

				Double sum = distanceSkimMatrix.getCost(originZone, destinationZone);
				if (sum == null) sum = 0.0;
				double distance = temproTrip.getLength();
				distanceSkimMatrix.setCost(originZone, destinationZone, sum + distance * multiplier);
			}
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
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN ||
					vht == VehicleType.ARTIC_AV || vht == VehicleType.RIGID_AV || vht == VehicleType.VAN_AV)) continue; //skip non-freight vehicles

			//map AV types to non-AV types
			if (vht == VehicleType.ARTIC_AV) vht = VehicleType.ARTIC;
			if (vht == VehicleType.RIGID_AV) vht = VehicleType.RIGID;
			if (vht == VehicleType.VAN_AV) vht = VehicleType.VAN;	
			
			int origin = trip.getFreightOriginZone();
			int destination = trip.getFreightDestinationZone();
			int multiplier = trip.getMultiplier();

			Double count = counter.getCost(origin, destination, vht.value);
			if (count == null) count = 0.0;
			counter.setCost(origin, destination, vht.value, count + multiplier);

			Double sum = distanceSkimMatrixFreight.getCost(origin, destination, vht.value);
			if (sum == null) sum = 0.0;
			double distance = trip.getLength(this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight());
			distanceSkimMatrixFreight.setCost(origin, destination, vht.value, sum + distance * multiplier);
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

		if (this.tripList == null || this.tripList.size() == 0) {
			LOGGER.warn("TripList is empty! Cannot update cost skim matrix for freight.");
			return;
		}

		for (Trip trip: this.tripList) {

			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN ||
					vht == VehicleType.ARTIC_AV || vht == VehicleType.RIGID_AV || vht == VehicleType.VAN_AV)) continue; //skip non-freight vehicles

			//map AV types to non-AV types
			if (vht == VehicleType.ARTIC_AV) vht = VehicleType.ARTIC;
			if (vht == VehicleType.RIGID_AV) vht = VehicleType.RIGID;
			if (vht == VehicleType.VAN_AV) vht = VehicleType.VAN;	
			
			int origin = trip.getFreightOriginZone();
			int destination = trip.getFreightDestinationZone();
			int multiplier = trip.getMultiplier();

			Double count = counter.getCost(origin, destination, vht.value);
			if (count == null) count = 0.0;
			counter.setCost(origin, destination, vht.value, count + multiplier);

			Double sum = costSkimMatrixFreight.getCost(origin, destination, vht.value);
			if (sum == null) sum = 0.0;
			double tripFuelCost = trip.getCost(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), averageAccessEgressSpeedFreight, this.energyUnitCosts, this.energyConsumptions, this.relativeFuelEfficiencies, this.congestionCharges);

			costSkimMatrixFreight.setCost(origin, destination, vht.value, sum + tripFuelCost * multiplier);
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
	 * Calculates total energy consumption for each car/AV energy type (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each energy type.
	 */
	public HashMap<EnergyType, Double> calculateCarEnergyConsumptions() {

		HashMap<EnergyType, Double> consumptions = new HashMap<EnergyType, Double>();
		for (EnergyType energy: EnergyType.values()) consumptions.put(energy, 0.0);
				
		for (Trip trip: this.tripList) {
			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles
			HashMap<EnergyType, Double> consumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistance(), averageAccessEgressSpeedCar, this.energyConsumptions, this.relativeFuelEfficiencies);
			int multiplier = trip.getMultiplier();
	
			for (EnergyType energy: EnergyType.values()) {
				Double currentConsumption = consumptions.get(energy);
				if (currentConsumption == null) currentConsumption = 0.0;
				consumptions.put(energy, currentConsumption + consumption.get(energy) * multiplier);
			}
		}

		return consumptions;
	}

	/**
	 * Calculates spatial energy consumption for car vehicles for each energy type (in litres/kg for fuels and in kWh for electricity).
	 * @param originZoneEnergyWeight Percentage of energy consumption assigned to origin zone (the rest assigned to destination zone).
	 * @return Zonal consumption for each energy type.
	 */
	public HashMap<EnergyType, HashMap<String, Double>> calculateZonalCarEnergyConsumptions(final double originZoneEnergyWeight) {

		//initialise hashmaps
		HashMap<EnergyType, HashMap<String, Double>> zonalConsumptions = new HashMap<EnergyType, HashMap<String, Double>>();
		for (EnergyType energy: EnergyType.values()) {
			HashMap<String, Double> consumption = new HashMap<String, Double>();
			zonalConsumptions.put(energy, consumption);
		}

		for (Trip trip: this.tripList) {

			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles

			HashMap<EnergyType, Double> tripConsumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), averageAccessEgressSpeedFreight, this.energyConsumptions, this.relativeFuelEfficiencies);

			String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
			String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
			int multiplier = trip.getMultiplier();
			
			for (EnergyType et: EnergyType.values()) {
				
				Double currentConsumptionOrigin = zonalConsumptions.get(et).get(originLAD);
				if (currentConsumptionOrigin == null) currentConsumptionOrigin = 0.0;

				Double currentConsumptionDestination = zonalConsumptions.get(et).get(destinationLAD);
				if (currentConsumptionDestination == null) currentConsumptionDestination = 0.0;

				currentConsumptionOrigin += originZoneEnergyWeight * tripConsumption.get(et) * multiplier;
				currentConsumptionDestination += (1.0 - originZoneEnergyWeight) * tripConsumption.get(et) * multiplier;

				zonalConsumptions.get(et).put(originLAD, currentConsumptionOrigin);
				zonalConsumptions.get(et).put(destinationLAD, currentConsumptionDestination);
			}
		}

		return zonalConsumptions;
	}

	/**
	 * Calculates total energy consumption for each freight vehicle engine type (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each energy type.
	 */
	public HashMap<EnergyType, Double> calculateFreightEnergyConsumptions() {

		HashMap<EnergyType, Double> consumptions = new HashMap<EnergyType, Double>();
		for (EnergyType energy: EnergyType.values()) {
			consumptions.put(energy, 0.0);
		}

		for (Trip trip: this.tripList) {
			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN ||
					vht == VehicleType.ARTIC_AV || vht == VehicleType.RIGID_AV || vht == VehicleType.VAN_AV)) continue; //skip non-freight vehicles
			HashMap<EnergyType, Double> consumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), averageAccessEgressSpeedFreight, this.energyConsumptions, this.relativeFuelEfficiencies);
			int multiplier = trip.getMultiplier();
			
			for (EnergyType energy: EnergyType.values()) {
				Double currentConsumption = consumptions.get(energy);
				if (currentConsumption == null) currentConsumption = 0.0;
				consumptions.put(energy, currentConsumption + consumption.get(energy) * multiplier);
			}
		}

		return consumptions;
	}

	/**
	 * Calculates total energy consumption for each energy type of passenger cars and freight vehicles (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each engine type.
	 */
	public HashMap<EnergyType, Double> calculateEnergyConsumptions() {

		HashMap<EnergyType, Double> car = calculateCarEnergyConsumptions();
		HashMap<EnergyType, Double> freight = calculateFreightEnergyConsumptions();
		HashMap<EnergyType, Double> combined = new HashMap<EnergyType, Double>();

		for (EnergyType energy: EnergyType.values()) {
			double consumption = car.get(energy) + freight.get(energy);
			combined.put(energy, consumption);
		}
		return combined;
	}
	
	/**
	 * Calculates total CO2 emissions (in kg) for each type of passenger and freight vehicle.
	 * @return Total consumption for each engine type.
	 */
	public HashMap<String, Double> calculateCO2Emissions() {
		
		HashMap<String, Double> totalCO2Emissions = new HashMap<String, Double>();

		HashMap<EnergyType, Double> car = calculateCarEnergyConsumptions();
		double carCO2 = 0.0;
		for (EnergyType energy: EnergyType.values()) {
			carCO2 += car.get(energy) + this.unitCO2Emissions.get(energy);
		}
		totalCO2Emissions.put("PASSENGER", carCO2);
		
		HashMap<EnergyType, Double> freight = calculateFreightEnergyConsumptions();
		double freightCO2 = 0.0;
		for (EnergyType energy: EnergyType.values()) {
			freightCO2 += freight.get(energy) + this.unitCO2Emissions.get(energy);
		}
		totalCO2Emissions.put("FREIGHT", freightCO2);
		
		totalCO2Emissions.put("COMBINED", carCO2 + freightCO2);
	
		return totalCO2Emissions;
	}
	
	/**
	 * Calculate peak-hour link point capacities (PCU/lane/hr).
	 * @return Peak-hour link point capacities.
	 */
	public HashMap<Integer, Double> calculatePeakLinkPointCapacities() {

		Map<Integer, Double> peakLinkVolumes = this.calculateLinkVolumeInPCUPerTimeOfDay(this.tripList).get(TimeOfDay.EIGHTAM);
		HashMap<Integer, Integer> numberOfLanes = roadNetwork.getNumberOfLanes();
		HashMap<Integer, Double> peakLinkCapacitieses = new HashMap<Integer, Double>();
		
		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			double linkVol = 0.0;
			if (peakLinkVolumes.get(edge.getID()) != null) linkVol = peakLinkVolumes.get(edge.getID());
			double capacity = 0.0;
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			if (roadNumber.charAt(0) == 'M') //motorway
				capacity = linkVol / numberOfLanes.get(edge.getID());
			else if (roadNumber.charAt(0) == 'A') //A-road
				capacity = linkVol / numberOfLanes.get(edge.getID());
			else //ferry
				capacity = linkVol;

			peakLinkCapacitieses.put(edge.getID(), capacity);
		}
		return peakLinkCapacitieses;
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
		
		Map<Integer, Double> peakLinkVolumes = this.calculateLinkVolumeInPCUPerTimeOfDay(this.tripList).get(TimeOfDay.EIGHTAM);
		HashMap<Integer, Integer> numberOfLanes = roadNetwork.getNumberOfLanes();
		HashMap<Integer, Double> peakLinkDensities = new HashMap<Integer, Double>();

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			double linkVol = 0.0;
			if (peakLinkVolumes.get(edge.getID()) != null) linkVol = peakLinkVolumes.get(edge.getID());
			double density = 0.0;
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			double length = (double) sf.getAttribute("LenNet");
			if (roadNumber.charAt(0) == 'M') //motorway
				density = linkVol / numberOfLanes.get(edge.getID()) / length;
			else if (roadNumber.charAt(0) == 'A') //A-road
				density = linkVol / numberOfLanes.get(edge.getID()) / length;
			else //ferry
				density = linkVol / length;

			peakLinkDensities.put(edge.getID(), density);
		}
		return peakLinkDensities;
	}
	
	/**
	 * Calculate peak-hour link capacity utilisation (capacity / max. capacity).
	 * @return Peak-hour link capacity utilisation [%].
	 */
	public HashMap<Integer, Double> calculatePeakLinkCapacityUtilisation() {

		HashMap<Integer, Double> peakLinkCapacitieses = this.calculatePeakLinkPointCapacities();
		HashMap<Integer, Double> peakLinkCapacityUtilisataion = new HashMap<Integer, Double>();
		
		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			double capacity = 0.0;
			if (peakLinkCapacitieses.get(edge.getID()) != null) capacity = peakLinkCapacitieses.get(edge.getID());
			double utilisation = 0.0;
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			if (roadNumber.charAt(0) == 'M') //motorway
				utilisation = capacity / this.maximumCapacityMRoad;
			else if (roadNumber.charAt(0) == 'A') //A-road
				utilisation = capacity / this.maximumCapacityARoad;
			else //ferry
				utilisation = 0.0; //undefined for ferry

			peakLinkCapacityUtilisataion.put(edge.getID(), utilisation * 100);
		}
		return peakLinkCapacityUtilisataion;
	}
	
	/**
	 * Calculate peak-hour link capacity utilisation (%) averaged by two directions.
	 * @return Peak-hour link capacity utilisation.
	 */
	public HashMap<Integer, Double> calculateDirectionAveragedPeakLinkCapacityUtilisation() {

		HashMap<Integer, Double> utilisation = this.calculatePeakLinkCapacityUtilisation();
		HashMap<Integer, Double> averagedUtilisation = new HashMap<Integer, Double>();

		for (Integer edgeID: utilisation.keySet()) {
			double utilisation1 = utilisation.get(edgeID);
			Integer otherEdgeID = roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edgeID);
			if (otherEdgeID == null) averagedUtilisation.put(edgeID, utilisation1); //if just one direction, copy value
			else { //otherwise, store average for both directions
				Double utilisation2 = utilisation.get(otherEdgeID);
				averagedUtilisation.put(edgeID, (utilisation1 + utilisation2) / 2.0);
				averagedUtilisation.put(otherEdgeID, (utilisation1 + utilisation2) / 2.0);
			}
		}

		return averagedUtilisation;
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
	 * Getter method for the use route choice model flag.
	 * @return Flag.
	 */
	public boolean getFlagUseRouteChoiceModel() {

		return this.flagUseRouteChoiceModel;
	}

	/**
	 * Saves assignment results to output file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
	 */
	public void saveAssignmentResults(int year, String outputFile) {
		
		LOGGER.debug("Saving link-based assignment results.");

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
		header.add("linkVolumeCarAV");
		header.add("linkVolumeVanAV");
		header.add("linkVolumeRigidAV");
		header.add("linkVolumeArticAV");
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
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR_AV).get(edge.getID());
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.VAN_AV).get(edge.getID());
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.RIGID_AV).get(edge.getID());
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC_AV).get(edge.getID());
				if (linkVolume == null) record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				Double linkVolumePCU = this.linkVolumesInPCU.get(edge.getID());
				if (linkVolumePCU == null) record.add(Double.toString(0.0));
				else					   record.add(Double.toString(linkVolumePCU));
				record.add(Double.toString(capacities.get(edge.getID())));
				record.add(Double.toString(densities.get(edge.getID())));
				//get max capacity from road type
				if (roadNumber.charAt(0) == 'M') { //motorway
					record.add(Integer.toString(maximumCapacityMRoad));
					double utilisation = capacities.get(edge.getID()) / maximumCapacityMRoad;
					record.add(Double.toString(utilisation));
					double averageUtilisation = averageCapacities.get(edge.getID()) / maximumCapacityMRoad;
					record.add(Double.toString(averageUtilisation));
					double maximumUtilisation = maximumCapacities.get(edge.getID()) / maximumCapacityMRoad;
					record.add(Double.toString(maximumUtilisation));
				}	
				else if (roadNumber.charAt(0) == 'A') { //A road
					record.add(Integer.toString(maximumCapacityARoad));
					double utilisation = capacities.get(edge.getID()) / maximumCapacityARoad;
					record.add(Double.toString(utilisation));
					double averageUtilisation = averageCapacities.get(edge.getID()) / maximumCapacityARoad;
					record.add(Double.toString(averageUtilisation));
					double maximumUtilisation = maximumCapacities.get(edge.getID()) / maximumCapacityARoad;
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
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
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
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

	/**
	 * Saves total electricity consumption to an output file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
	 */
	public void saveTotalEnergyConsumptions(int year, String outputFile) {
		
		LOGGER.debug("Saving energy consumptions file.");

		//calculate energy consumptions
		HashMap<EnergyType, Double> energyConsumptions = this.calculateEnergyConsumptions();

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		for (EnergyType energy: EnergyType.values()) header.add(energy.name());
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
				EnergyType energy = EnergyType.valueOf(header.get(i));
				record.add(String.format("%.2f", energyConsumptions.get(energy)));
			}
			csvFilePrinter.printRecord(record);
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
	
	/**
	 * Saves total CO2 emissions to an output file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
	 */
	public void saveTotalCO2Emissions(int year, String outputFile) {
		
		LOGGER.debug("Saving CO2 emissions file.");

		//calculate CO2 emissions
		HashMap<String, Double> totalCO2Emissions = this.calculateCO2Emissions();

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		for (String key: totalCO2Emissions.keySet()) header.add(key);
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
				String key = header.get(i);
				record.add(String.format("%.2f", totalCO2Emissions.get(key)));
			}
			csvFilePrinter.printRecord(record);
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
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
		HashMap<EnergyType, HashMap<String, Double>> energyConsumptions = this.calculateZonalCarEnergyConsumptions(originZoneEnergyWeight);
		Set<String> zones = energyConsumptions.get(EnergyType.ELECTRICITY).keySet();

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
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

	/**
	 * Saves zonal vehicle-kilometres.
	 * @param year Assignment year.
	 * @param outputFile Output file name (with path).
	 */
	public void saveZonalVehicleKilometres(int year, String outputFile) {
		
		LOGGER.debug("Saving zonal vehicle-kilometres.");

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
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
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
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

	/**
	 * Saves travel times into a file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
	 */
	public void saveLinkTravelTimes (int year, String outputFile) {
		
		LOGGER.debug("Saving link travel times.");

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
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}

	/**
	 * Getter method for energy unit costs.
	 * @return Energy unit costs.
	 */   
	public HashMap<EnergyType, Double> getEnergyUnitCosts() {

		return this.energyUnitCosts;
	}

	/**
	 * Getter method for energy consumptions.
	 * @return Energy consumptions.
	 */   
	public HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> getEnergyConsumptions() {

		return this.energyConsumptions;
	}

	/**
	 * Getter method for engine type fractions.
	 * @return Engine type fractions.
	 */   
	public HashMap<VehicleType, HashMap<EngineType, Double>> getEngineTypeFractions() {

		return this.engineTypeFractions;
	}

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
	 * Setter method for node probabilities.
	 * @param startNodeProbabilities Node probabilities.
	 */
	public void setStartNodeProbabilities(HashMap<Integer, Double> startNodeProbabilities) {

		this.startNodeProbabilities = startNodeProbabilities;
	}

	/**
	 * Setter method for node probabilities.
	 * @param endNodeProbabilities Node probabilities.
	 */
	public void setEndNodeProbabilities(HashMap<Integer, Double> endNodeProbabilities) {

		this.endNodeProbabilities = endNodeProbabilities;
	}

	/**
	 * Getter method for workplace zones probabilities.
	 * @return Workplace zones probabilities.
	 */
	public HashMap<String, Double> getWorkplaceZoneProbabilities() {

		return this.workplaceZoneProbabilities;
	}

	//	/**
	//	 * Setter method for congestion charged links.
	//	 * @param congestionCharges The structure with congestion charges
	//	 */
	//	public void setChargedLinks(MultiKeyMap congestionCharges) {
	//		
	//		this.congestionCharges = congestionCharges;
	//	}

	/**
	 * Calculates the number of passenger (car/AV) trips starting in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateLADTripStarts() {

		HashMap<String, Integer> totalLADnoTripStarts = new HashMap<String, Integer>();

		for (Trip trip: this.tripList)
			if (trip.getVehicle() == VehicleType.CAR || trip.getVehicle() == VehicleType.CAR_AV) {
				String originZone = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
				Integer tripStarts = totalLADnoTripStarts.get(originZone);
				if (tripStarts == null) tripStarts = 0;
				int multiplier = trip.getMultiplier();
				totalLADnoTripStarts .put(originZone, tripStarts + multiplier);
			}

		return totalLADnoTripStarts;
	}

	/**
	 * Calculates the number of passenger (car/AV) trips ending in a LAD.
	 * @return Number of trips.
	 */
	public HashMap<String, Integer> calculateLADTripEnds() {

		HashMap<String, Integer> totalLADnoTripEnds = new HashMap<String, Integer>();

		for (Trip trip: this.tripList) 
			if (trip.getVehicle() == VehicleType.CAR || trip.getVehicle() == VehicleType.CAR_AV) {
				String destinationZone = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
				Integer tripEnds = totalLADnoTripEnds.get(destinationZone);
				if (tripEnds == null) tripEnds = 0;
				int multiplier = trip.getMultiplier();
				totalLADnoTripEnds.put(destinationZone, tripEnds + multiplier);
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
			if (trip.getVehicle() == VehicleType.VAN || trip.getVehicle() == VehicleType.RIGID || trip.getVehicle() == VehicleType.ARTIC ||
				trip.getVehicle() == VehicleType.ARTIC_AV || trip.getVehicle() == VehicleType.RIGID_AV || trip.getVehicle() == VehicleType.VAN_AV) {
				String originZone = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
				Integer tripStarts = totalLADnoTripStarts.get(originZone);
				if (tripStarts == null) tripStarts = 0;
				int multiplier = trip.getMultiplier();
				totalLADnoTripStarts .put(originZone, tripStarts + multiplier);
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
			if (trip.getVehicle() == VehicleType.VAN || trip.getVehicle() == VehicleType.RIGID || trip.getVehicle() == VehicleType.ARTIC ||
				trip.getVehicle() == VehicleType.ARTIC_AV || trip.getVehicle() == VehicleType.RIGID_AV || trip.getVehicle() == VehicleType.VAN_AV) {
				String destinationZone = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
				Integer tripEnds = totalLADnoTripEnds.get(destinationZone);
				if (tripEnds == null) tripEnds = 0;
				int multiplier = trip.getMultiplier();
				totalLADnoTripEnds.put(destinationZone, tripEnds + multiplier);
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

		energyUnitCosts.put(EnergyType.ELECTRICITY, electricityUnitCost);
	}

	/**
	 * Setter method for the energy unit cost.
	 * @param energyType The type of a car engine.
	 * @param energyUnitCost The cost of 1 L (of fuel) or 1 kWh (of electricity) in £.
	 */
	public void setEnergyUnitCost (EnergyType energyType, double energyUnitCost) {

		this.energyUnitCosts.put(energyType, energyUnitCost);
	}

	/**
	 * Setter method for the energy consumption parameters.
	 * @param vehicleType Vehicle type
	 * @param engineType Engine type
	 * @param parameters Energy consumptions parameters (A, B, C, D)
	 */
	public void setEnergyConsumptionParameters (VehicleType vehicleType, EngineType engineType, HashMap<String, Double> parameters) {

		this.energyConsumptions.put(Pair.of(vehicleType, engineType), parameters);
	}

	/**
	 * Setter method for energy type fractions.
	 * @param vht Vehicle type
	 * @param engineTypeFractions Map with engine type fractions.
	 */
	public void setEngineTypeFractions (VehicleType vht, HashMap<EngineType, Double> engineTypeFractions) {

		this.engineTypeFractions.put(vht, engineTypeFractions);
	}
	
	/**
	 * Setter method for fractional assignment.
	 * @param assignmentFraction Assignment fraction (&lt;= 1.0).
	 */
	public void setAssignmentFraction (double assignmentFraction) {

		this.assignmentFraction = assignmentFraction;
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
	 * Reset trip list for passengers and freight.
	 */
	public void resetTripStorages () {

		this.tripList.clear();
		//this.tripList = new ArrayList<Trip>();
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
			int multiplier = trip.getMultiplier();
			Map<Integer, Double> hourlyMap = map.get(trip.getTimeOfDay());
			for (Edge edge: trip.getRoute().getEdges()) {
				Double currentCount = hourlyMap.get(edge.getID());
				if (currentCount == null) currentCount = 0.0;
				currentCount += this.vehicleTypeToPCU.get(trip.getVehicle()) * multiplier; //add PCU of the vehicle
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
	 * Calculates daily link volumes in PCU.
	 * @param tripList Trip list.
	 * @return Map of link volumes in PCU.
	 */
	public Map<Integer, Double> calculateLinkVolumeInPCU(List<Trip> tripList) {

		Map<Integer, Double> map = new HashMap<Integer, Double>();
		
		for (Trip trip: tripList) {
			int multiplier = trip.getMultiplier();
			for (Edge edge: trip.getRoute().getEdges()) {
				Double currentCount = map.get(edge.getID());
				if (currentCount == null) currentCount = 0.0;
				currentCount += this.vehicleTypeToPCU.get(trip.getVehicle()) * multiplier; //add PCU of the vehicle
				map.put(edge.getID(), currentCount);
			}
		}

		return map;
	}

	/**
	 * Updates daily link volumes in PCU from the trip list and stores it into instance variable.
	 */
	public void updateLinkVolumeInPCU() {

		this.linkVolumesInPCU = this.calculateLinkVolumeInPCU(this.tripList);
	}

	/**
	 * Getter method for daily link volumes in PCU.
	 * @return Link volumes in PCU.
	 */
	public Map<Integer, Double> getLinkVolumeInPCU() {

		return this.linkVolumesInPCU;
	}
	
	/**
	 * Calculates daily link volumes per vehicle type.
	 * @param tripList Trip list.
	 * @return Map of link volumes per vehicle type.
	 */
	public Map<VehicleType, Map<Integer, Integer>> calculateLinkVolumePerVehicleType(List<Trip> tripList) {

		Map<VehicleType, Map<Integer, Integer>> map = new HashMap<VehicleType, Map<Integer, Integer>>();
		for (VehicleType vht: VehicleType.values()) {
			Map<Integer, Integer> vehicleMap = new HashMap<Integer, Integer>();
			map.put(vht, vehicleMap);
		}

		for (Trip trip: tripList) {
			int multiplier = trip.getMultiplier();
			Map<Integer, Integer> vehicleMap = map.get(trip.getVehicle());
			for (Edge edge: trip.getRoute().getEdges()) {
				Integer currentCount = vehicleMap.get(edge.getID());
				if (currentCount == null) currentCount = 0;
				currentCount += multiplier;
				vehicleMap.put(edge.getID(), currentCount);
			}
		}

		return map;
	}

	/**
	 * Updates daily link volumes per vehicle type from trip list and stores into instance variable.
	 */
	public void updateLinkVolumePerVehicleType() {

		this.linkVolumesPerVehicleType = this.calculateLinkVolumePerVehicleType(this.tripList);
	}

	/**
	 * Getter method for daily link volumes per vehicle type.
	 * @return Link volumes in PCU per time of day.
	 */
	public Map<VehicleType, Map<Integer, Integer>> getLinkVolumePerVehicleType() {

		return this.linkVolumesPerVehicleType;
	}
	
	/**
	 * Getter method for AADF car traffic counts.
	 * @return Car traffic counts.
	 */
	public Map<Integer, Integer> getAADFCarTrafficCounts() {
		
		return this.roadNetwork.getAADFCarTrafficCounts();
		
	}
	
	/**
	 * Calculates absolute differences between car volumes and traffic counts.
	 * For combined counts, takes the average of two absolute differences.
	 * @return Absolute differences between car volumes and traffic counts.
	 */
	public HashMap<Integer, Integer> calculateAbsDifferenceCarCounts () {

		HashMap<Integer, Integer> absoluteDifferences = new HashMap<Integer, Integer>();

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

				absoluteDifferences.put(edge.getID(), (int) Math.abs(carCount - carVolume));
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
				absoluteDifferences.put(edge.getID(), (int) absoluteDifference);
				if (edge2 != null) absoluteDifferences.put(edge2, (int) absoluteDifference); //store in other direction too
			}
		}

		return absoluteDifferences;
	}
	

	/**
	 * Calculates absolute differences between car volumes and traffic counts averaged for both directions.
	 * For combined counts, takes the average of two absolute differences.
	 * @return Direction averaged absolute differences between car volumes and traffic counts.
	 */
	public HashMap<Integer, Double> calculateDirectionAveragedAbsoluteDifferenceCarCounts () {

		HashMap<Integer, Integer> absoluteDifferences = this.calculateDifferenceCarCounts(); //this.calculateAbsDifferenceCarCounts(); //TODO
		HashMap<Integer, Double> directionAveragedAbsoluteDifferences = new HashMap<Integer, Double>();

		LOGGER.trace("Absolute differences: {}", absoluteDifferences);
		
		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
		ArrayList<Integer> checkedLinks = new ArrayList<Integer>(); //list of checked links

		for (Integer edge1: absoluteDifferences.keySet())
			
			if (!checkedLinks.contains(edge1)) {
				
				//check if there is other direction edge, store average value for both
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edge1);
				if (edge2 != null) {
					
					Integer diff1 = absoluteDifferences.get(edge1);
					Integer diff2 = absoluteDifferences.get(edge2);
					
					if (diff1 == null) LOGGER.trace("No absolute difference for edge: {}", edge1);
					if (diff2 == null) LOGGER.trace("No absolute difference for edge: {}", edge2);
					
					Integer average = (diff1 + diff2) / 2;
					
					directionAveragedAbsoluteDifferences.put(edge1, (double) average);
					directionAveragedAbsoluteDifferences.put(edge2, (double) average);
					checkedLinks.add(edge1);
					checkedLinks.add(edge2);

				//if there is just a unidirectional edge, use the same value	
				} else {
					
					Integer diff1 = absoluteDifferences.get(edge1);
					directionAveragedAbsoluteDifferences.put(edge1, (double) diff1);
					checkedLinks.add(edge1);
				}
			}
			
		return directionAveragedAbsoluteDifferences;
	}


	/**
	 * Calculates differences between car volumes and traffic counts.
	 * For combined counts, takes the average of the two differences.
	 * @return Differences between car volumes and traffic counts.
	 */
	public HashMap<Integer, Integer> calculateDifferenceCarCounts () {

		HashMap<Integer, Integer> differences = new HashMap<Integer, Integer>();

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

				differences.put(edge.getID(), (int) (carCount - carVolume));
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

				long difference = (carCount - carVolume - carVolume2) / 2;
				checkedCP.add(countPoint);
				differences.put(edge.getID(), (int) difference);
				if (edge2 != null) differences.put(edge2, (int) difference); //store in other direction too
			}
		}

		return differences;
	}
	
	
	/**
	 * Calculates GEH statistic for simulated and observed hourly car flows.
	 * For combined counts, takes the average of the two differences.
	 * Two obtain hourly flows, divide daily link volumes (and traffic counts) by 24.
	 * The formula is taken from WebTAG Unit M3.1.
	 * @return GEH statistic for simulated and observed hourly car flows.
	 */
	public HashMap<Integer, Double> calculateGEHStatisticForCarCounts () {

		HashMap<Integer, Double> GEH = new HashMap<Integer, Double>();

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
				
				double carFlowSimulated = carVolume / 24.0;
				double carFlowObserved = carCount / 24.0;
				double geh = Math.abs(carFlowSimulated - carFlowObserved) / Math.sqrt((carFlowSimulated + carFlowObserved) / 2.0);
				
				GEH.put(edge.getID(), geh);
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

				checkedCP.add(countPoint);
				
				double carFlowSimulated = (carVolume + carVolume2) / 24.0;
				double carFlowObserved = carCount / 24.0;
				double geh = Math.abs(carFlowSimulated - carFlowObserved) / Math.sqrt((carFlowSimulated + carFlowObserved) / 2.0);
				
				GEH.put(edge.getID(), geh);
				if (edge2 != null) GEH.put(edge2, geh); //store in other direction too
			}
		}

		return GEH;
	}
	
	/**
	 * Prints GEH statistics for comparison between simulated and observed hourly car flows.
	 */
	public void printGEHstatistic() {
		
		HashMap<Integer, Double> GEH = this.calculateGEHStatisticForCarCounts();
		
		int validFlows = 0;
		int suspiciousFlows = 0;
		int invalidFlows = 0;
		for (Integer edgeID: GEH.keySet()) {
			if (GEH.get(edgeID) < 5.0) validFlows++;
			else if (GEH.get(edgeID) < 10.0) suspiciousFlows++;
			else invalidFlows++;
		}
		LOGGER.info("Percentage of edges with valid flows (GEH < 5.0) is: {}%", Math.round((double) validFlows / GEH.size() * 100));
		LOGGER.info("Percentage of edges with suspicious flows (5.0 <= GEH < 10.0) is: {}%", Math.round((double) suspiciousFlows / GEH.size() * 100));
		LOGGER.info("Percentage of edges with invalid flows (GEH >= 10.0) is: {}%", Math.round((double) invalidFlows / GEH.size() * 100));		
	}
	
	/**
	 * Prints RMSN statistic for comparison between simulated daily car volumes and observed daily traffic counts.
	 */
	public void printRMSNstatistic() {
		
		double RMSN = this.calculateRMSNforSimulatedVolumes();
		LOGGER.info("RMSN for traffic counts is: {}%", Math.round(RMSN));
	}
	
	/**
	 * Calculate prediction error (RMSN for simulated volumes and observed traffic counts).
	 * @return Normalised root mean square error.
	 */
	public double calculateRMSNforSimulatedVolumes () {

		return this.calculateRMSNforExpandedSimulatedVolumes(1.0); //do not expand simulated volumes
	}

	/**
	 * Calculate prediction error (RMSN for expanded simulated volumes and observed traffic counts).
	 * @param expansionFactor Expansion factor expands simulated volumes.
	 * @return Normalised root mean square error.
	 */
	public double calculateRMSNforExpandedSimulatedVolumes (double expansionFactor) {

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

				//expand simulated volumes with the expansion factor and round
				carVolume = Math.round(carVolume * expansionFactor);

				countOfCounts++;
				sumOfCounts += carCount;
				sumOfSquaredDiffs += Math.pow(carCount - carVolume, 2);
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined count
				long carCount = (long) sf.getAttribute("FdCar");

				//get volumes for this direction
				long carVolume;
				Integer carVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge.getID());
				if (carVolumeFetch == null) carVolume = 0;
				else 						carVolume = carVolumeFetch;

				//expand simulated volumes with the expansion factor and round
				carVolume = Math.round(carVolume * expansionFactor);

				//get volumes for other direction (if exists)
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edge.getID());
				long carVolume2;
				Integer carVolumeFetch2 = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge2);
				if (carVolumeFetch2 == null) carVolume2 = 0;
				else 						 carVolume2 = carVolumeFetch2;

				//expand simulated volumes with the expansion factor and round
				carVolume2 = Math.round(carVolume2 * expansionFactor);

				countOfCounts++;
				sumOfCounts += carCount;
				sumOfSquaredDiffs += Math.pow(carCount - carVolume - carVolume2, 2);

				checkedCP.add(countPoint);
			}
		}

		//		System.out.println("Sum of squared diffs: " + sumOfSquaredDiffs);

		double RMSE = Math.sqrt(sumOfSquaredDiffs / countOfCounts);
		double averageTrueCount = (double) sumOfCounts / countOfCounts;

		//		System.out.println("Checking RMSE: ");
		//		System.out.println("Is finite: " + Double.isFinite(RMSE));
		//		System.out.println("Is infinite: " + Double.isInfinite(RMSE));
		//		System.out.println("Is NaN: " + Double.isNaN(RMSE));

		//		System.out.println("RMSE = " + RMSE);
		//		System.out.println("averageTrueCount = " + averageTrueCount);


		return (RMSE / averageTrueCount ) * 100;
	}

	/**
	 * Calculate prediction error (RMSN for for simulated freight volumes and observed traffic counts).
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
				sumOfSquaredDiffs += Math.pow(vanCount - vanVolume, 2) + Math.pow(rigidCount - rigidVolume, 2) + Math.pow(articCount - articVolume, 2);
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
				sumOfSquaredDiffs += Math.pow(vanCount - vanVolume - vanVolume2, 2) + Math.pow(rigidCount - rigidVolume - rigidVolume2, 2) + Math.pow(articCount - articVolume - articVolume2, 2);

				checkedCP.add(countPoint);
			}
		}

		double RMSE = Math.sqrt(sumOfSquaredDiffs / countOfCounts);
		double averageTrueCount = (double) sumOfCounts / countOfCounts;

		return (RMSE / averageTrueCount ) * 100;
	}
	
	/**
	 * Calculate prediction error (mean absolute deviation for expanded simulated volumes and observed traffic counts).
	 * @param expansionFactor Expansion factor expands simulated volumes.
	 * @return Mean absolute deviation.
	 */
	public double calculateMADforExpandedSimulatedVolumes (double expansionFactor) {

		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
		int countOfCounts = 0;
		long sumOfCounts = 0;
		double sumOfAbsoluteDiffs = 0.0;
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

				//expand simulated volumes with the expansion factor and round
				carVolume = Math.round(carVolume * expansionFactor);

				countOfCounts++;
				sumOfCounts += carCount;
				sumOfAbsoluteDiffs += Math.abs(carCount - carVolume);
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined count
				long carCount = (long) sf.getAttribute("FdCar");

				//get volumes for this direction
				long carVolume;
				Integer carVolumeFetch = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge.getID());
				if (carVolumeFetch == null) carVolume = 0;
				else 						carVolume = carVolumeFetch;

				//expand simulated volumes with the expansion factor and round
				carVolume = Math.round(carVolume * expansionFactor);

				//get volumes for other direction (if exists)
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edge.getID());
				long carVolume2;
				Integer carVolumeFetch2 = this.linkVolumesPerVehicleType.get(VehicleType.CAR).get(edge2);
				if (carVolumeFetch2 == null) carVolume2 = 0;
				else 						 carVolume2 = carVolumeFetch2;

				//expand simulated volumes with the expansion factor and round
				carVolume2 = Math.round(carVolume2 * expansionFactor);

				countOfCounts++;
				sumOfCounts += carCount;
				sumOfAbsoluteDiffs += Math.abs(carCount - carVolume - carVolume2);

				checkedCP.add(countPoint);
			}
		}

		//		System.out.println("Sum of squared diffs: " + sumOfSquaredDiffs);

		double MAD = sumOfAbsoluteDiffs / countOfCounts;
		//double averageTrueCount = (double) sumOfCounts / countOfCounts;

		//		System.out.println("Checking RMSE: ");
		//		System.out.println("Is finite: " + Double.isFinite(RMSE));
		//		System.out.println("Is infinite: " + Double.isInfinite(RMSE));
		//		System.out.println("Is NaN: " + Double.isNaN(RMSE));

		//		System.out.println("RMSE = " + RMSE);
		//		System.out.println("averageTrueCount = " + averageTrueCount);


		return MAD;
	}
}