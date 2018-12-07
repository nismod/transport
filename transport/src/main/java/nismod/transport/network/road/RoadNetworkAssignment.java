/**
 * 
 */
package nismod.transport.network.road;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.demand.AssignableODMatrix;
import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrixMultiKey;
import nismod.transport.demand.SkimMatrix;
import nismod.transport.demand.SkimMatrixArray;
import nismod.transport.demand.SkimMatrixArrayTempro;
import nismod.transport.demand.SkimMatrixFreightMultiKey;
import nismod.transport.demand.SkimMatrixMultiKey;
import nismod.transport.network.road.RoadNetwork.EdgeType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.network.road.RouteSet.RouteChoiceParams;
import nismod.transport.utility.InputFileReader;
import nismod.transport.utility.RandomSingleton;
import nismod.transport.zone.Zoning;

/**
 * Network assignment of origin-destination flows.
 * @author Milan Lovric
 */
public class RoadNetworkAssignment {

	private final static Logger LOGGER = LogManager.getLogger(RoadNetworkAssignment.class);

	//public static final double SPEED_LIMIT_M_ROAD = 112.65; //70mph = 31.29mps = 112.65kph
	//public static final double SPEED_LIMIT_A_ROAD = 96.56; //60mph = 26.82mps = 96.56kph

	public final int maximumCapacityMRoad; //PCU per lane per hour
	public final int maximumCapacityARoad; //PCU per lane per hour
	public final double averageAccessEgressSpeedCar; //kph  
	public final double averageAccessEgressSpeedFreight; //kph 
	public final double peakHourPercentage;
	public final double alpha;
	public final double betaMRoad;
	public final double betaARoad;
	public final boolean flagIntrazonalAssignmentReplacement; //true means that origin and destination nodes can be the same
	public final boolean flagAStarIfEmptyRouteSet; //if there is no pre-generated route set for a node pair, try finding a route with aStar
	public final int interzonalTopNodes; //how many top nodes (based on gravitated population size) to considers as trip origin/destination
	public final double averageIntersectionDelay; //[min]
	public final double nodesProbabilityWeighting; //manipulates probabilities of nodes for the node choice
	public final double nodesProbabilityWeightingFreight; //manipulates probabilities of nodes for the node choice
	public final double assignmentFraction; //the fraction of vehicle flows to actually assign, with later results expansion to 100%
	public final double volumeToFlowFactor; //converts daily vehicle volume to hourly vehicle flow for GEH statistic.
	public final boolean flagUseRouteChoiceModel; //use route-choice model (true) or routing with A-Star (false)
	public final boolean flagIncludeAccessEgress; //use access/egress in the calculation of outputs.
	public final int topTemproNodes = 1;
	public final int baseYear;

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
		int value; 
		private VehicleType(int value) { this.value = value; } 
		public int getValue() { return this.value; } 
	};

	public static enum TimeOfDay {
		MIDNIGHT(0), ONEAM(1), TWOAM(2), THREEAM(3), FOURAM(4), FIVEAM(5), SIXAM(6), SEVENAM(7), EIGHTAM(8), NINEAM(9), TENAM(10), ELEVENAM(11),
		NOON (12), ONEPM(13), TWOPM(14), THREEPM(15), FOURPM(16), FIVEPM(17), SIXPM(18), SEVENPM(19), EIGHTPM(20), NINEPM(21), TENPM(22), ELEVENPM(23);
		private int value; 
		private TimeOfDay(int value) { this.value = value; }
	}

	private Map<VehicleType, Double> vehicleTypeToPCU;
	private Map<EnergyType, Double> energyUnitCosts;

	private Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptions;
	private Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiencies;
	
	private Map<VehicleType, Map<EngineType, Double>> engineTypeFractions;
	private Map<VehicleType, Double> AVFractions;

	private Map<TimeOfDay, Double> timeOfDayDistribution;
	private Map<TimeOfDay, Double> timeOfDayDistributionFreight;

	private Map<EnergyType, Double> unitCO2Emissions;

	private RoadNetwork roadNetwork;
	private Zoning zoning;

	private double[] linkVolumesInPCU;
	private Map<VehicleType, int[]> linkVolumesPerVehicleType;
	private Map<TimeOfDay, double[]> linkVolumesInPCUPerTimeOfDay;
	private Map<TimeOfDay, double[]> linkTravelTimePerTimeOfDay;

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

	private Properties params;
	
	/**
	 * @param roadNetwork Road network.
	 * @param zoning Zoning system.
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
			Zoning zoning,
			Map<EnergyType, Double> energyUnitCosts, 
			Map<EnergyType, Double> unitCO2Emissions,
			Map<VehicleType, Map<EngineType, Double>> engineTypeFractions,
			Map<VehicleType, Double> fractionsAV,
			Map<VehicleType, Double> vehicleTypeToPCU,
			Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParams,
			Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiencies,
			Map<TimeOfDay, Double> timeOfDayDistribution,
			Map<TimeOfDay, Double> timeOfDayDistributionFreight, 
			Map<TimeOfDay, Map<Integer, Double>> defaultLinkTravelTime, 
			HashMap<String, Double> areaCodeProbabilities, 
			HashMap<String, Double> workplaceZoneProbabilities,
			HashMap<String, MultiKeyMap> congestionCharges,
			Properties params) {

		this.roadNetwork = roadNetwork;
		this.zoning = zoning;
		this.params = params;
		this.linkVolumesInPCU = new double[this.roadNetwork.getMaximumEdgeID()];
		this.linkVolumesPerVehicleType = new EnumMap<VehicleType, int[]>(VehicleType.class);
		for (VehicleType vht: VehicleType.values()) {
			int[] volumes = new int[this.roadNetwork.getMaximumEdgeID()];
			linkVolumesPerVehicleType.put(vht, volumes);
		}
		this.linkTravelTimePerTimeOfDay = new EnumMap<TimeOfDay, double[]>(TimeOfDay.class);
		for (TimeOfDay hour: TimeOfDay.values()) {
			double[] hourlyTimes = new double[this.roadNetwork.getFreeFlowTravelTime().length];
			linkTravelTimePerTimeOfDay.put(hour, hourlyTimes);
		}
		
		this.tripList = new ArrayList<Trip>();

		if (defaultLinkTravelTime == null) { //use free flow
			LOGGER.debug("No link travel time provided, using free-flow link travel time.");
			for (TimeOfDay hour: TimeOfDay.values()) {
				double[] hourlyTimes = this.linkTravelTimePerTimeOfDay.get(hour);
				for (int i=1; i < hourlyTimes.length; i++)
					hourlyTimes[i] = this.roadNetwork.getFreeFlowTravelTime()[i];
			}
		}
		else //otherwise copy
			for (TimeOfDay hour: TimeOfDay.values()) {
				double[] hourlyTimes = this.linkTravelTimePerTimeOfDay.get(hour);
				for (Integer edgeID: defaultLinkTravelTime.get(hour).keySet())
					hourlyTimes[edgeID] = defaultLinkTravelTime.get(hour).get(edgeID);
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
		this.baseYear = Integer.parseInt(params.getProperty("baseYear"));
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
		this.volumeToFlowFactor = Double.parseDouble(params.getProperty("VOLUME_TO_FLOW_FACTOR"));
		this.flagUseRouteChoiceModel = Boolean.parseBoolean(params.getProperty("USE_ROUTE_CHOICE_MODEL")); //use route-choice model (true) or routing with A-Star (false)
		this.flagIncludeAccessEgress = Boolean.parseBoolean(params.getProperty("FLAG_INCLUDE_ACCESS_EGRESS")); //include access/egress into the calculations of outputs

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
	 * @param props Routing parameters.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsRouting(AssignableODMatrix passengerODM, RouteSetGenerator rsg, Properties props) {

		LOGGER.info("Assigning the passenger flows from the passenger matrix...");

		//sort nodes based on the gravitating population
		this.roadNetwork.sortGravityNodes();

		//to store routes generated during the assignment
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork, props);

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		List<String> origins = passengerODM.getUnsortedOrigins();
		List<String> destinations = passengerODM.getUnsortedDestinations();
		
		//for each OD pair from the passengerODM
		for (String originZone: origins)
			for (String destinationZone: destinations) {
				
			if (passengerODM.getIntFlow(originZone, destinationZone) == 0) continue;
		
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
			int flow = (int) Math.floor(passengerODM.getIntFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getIntFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getIntFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

				//choose time of day
				TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistribution);
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				VehicleType vht = this.chooseCarVehicleType(AVFractions);
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
				if (engine == null) LOGGER.warn("Engine type not chosen!");

				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node

				//choose origin node
				double cumulativeProbability = 0.0;
				Integer originNode = null;
				double random = rng.nextDouble();
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
				Node from = roadNetwork.getNodeIDtoNode()[originNode];
				Node to = roadNetwork.getNodeIDtoNode()[destinationNode];
				//					System.out.println("from " + from + " to " + to);
				Route foundRoute = null;
				try {

					//see if that route already exists in the route storage
					RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
					if (rs != null) foundRoute = rs.getChoiceSet().get(0); //take the first route

					//if route does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundRoute == null) {
						//System.out.println("The path does not exist in the path storage");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}

						foundRoute = new Route(fastestPath, roadNetwork);
						rsg.addRoute(foundRoute); //add to the route set
					}

					int multiplier = 1;
					if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
					counterAssignedTrips += multiplier;
					
					//get zone IDs for storing into trips
					int originZoneID = this.zoning.getLadCodeToIDMap().get(originZone);
					int destinationZoneID = this.zoning.getLadCodeToIDMap().get(destinationZone);

					//store trip in trip list
					Trip trip = new Trip(vht, engine, foundRoute, hour, originZoneID, destinationZoneID, multiplier);
					this.tripList.add(trip);

				} catch (Exception e) {
					LOGGER.error(e);
					LOGGER.error("Couldn't find path from node {} to node {}!", from.getID(), to.getID());
				}
			}//for each trip
		}//for each destination pair
	
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
	 * @param props Properties.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsHourlyRouting(AssignableODMatrix passengerODM, Map<TimeOfDay, RouteSetGenerator> routeStorage, Properties props) {

		LOGGER.info("Assigning the passenger flows from the passenger matrix...");

		//sort nodes based on the gravitating population
		this.roadNetwork.sortGravityNodes();

		//to store routes generated during the assignment
		if (routeStorage == null) {
			routeStorage = new EnumMap<>(TimeOfDay.class);
			for (TimeOfDay hour: TimeOfDay.values()) {
				routeStorage.put(hour, new RouteSetGenerator(this.roadNetwork, props));
			}
		}

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		List<String> origins = passengerODM.getUnsortedOrigins();
		List<String> destinations = passengerODM.getUnsortedDestinations();
		
		//for each OD pair from the passengerODM
		for (String originZone: origins)
			for (String destinationZone: destinations) {
				
			if (passengerODM.getIntFlow(originZone, destinationZone) == 0) continue;	
			
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
			int flow = (int) Math.floor(passengerODM.getIntFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getIntFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getIntFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

				//choose time of day
				TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistribution);
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				VehicleType vht = this.chooseCarVehicleType(AVFractions);
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
				if (engine == null) LOGGER.warn("Engine type not chosen!");

				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node

				//choose origin node
				double cumulativeProbability = 0.0;
				Integer originNode = null;
				double random = rng.nextDouble();
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
				Node from = roadNetwork.getNodeIDtoNode()[originNode];
				Node to = roadNetwork.getNodeIDtoNode()[destinationNode];
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

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}

						foundRoute = new Route(fastestPath, roadNetwork);
						rsg.addRoute(foundRoute); //add to the route set
					}

					int multiplier = 1;
					if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
					counterAssignedTrips += multiplier;
					
					//get zone IDs for storing into trips
					int originZoneID = this.zoning.getLadCodeToIDMap().get(originZone);
					int destinationZoneID = this.zoning.getLadCodeToIDMap().get(destinationZone);

					//store trip in trip list
					Trip trip = new Trip(vht, engine, foundRoute, hour, originZoneID, destinationZoneID, multiplier);
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
	public void assignPassengerFlowsRouteChoice(AssignableODMatrix passengerODM, RouteSetGenerator rsg, Properties routeChoiceParameters) {

		LOGGER.info("Assigning the passenger flows from the passenger matrix...");

		if (passengerODM == null) { LOGGER.warn("Passenger OD matrix is null! Skipping assignment."); return; }
		if (rsg == null) { LOGGER.warn("Route set generator is null! Skipping assignment."); return; }
		if (routeChoiceParameters == null) { LOGGER.warn("Route choice parameters are null! Skipping assignment."); return; }
		
		//read route choice parameters from the properties file
		double paramTime = Double.parseDouble(routeChoiceParameters.getProperty("TIME"));
		double paramLength = Double.parseDouble(routeChoiceParameters.getProperty("LENGTH"));
		double paramCost = Double.parseDouble(routeChoiceParameters.getProperty("COST"));
		double paramIntersections = Double.parseDouble(routeChoiceParameters.getProperty("INTERSECTIONS"));
		double avgIntersectionDelay = Double.parseDouble(routeChoiceParameters.getProperty("AVERAGE_INTERSECTION_DELAY"));
		
		//set route choice parameters
		Map<RouteChoiceParams, Double> params = new EnumMap<>(RouteChoiceParams.class);
		params.put(RouteChoiceParams.TIME, paramTime);
		params.put(RouteChoiceParams.LENGTH, paramLength);
		params.put(RouteChoiceParams.COST, paramCost);
		params.put(RouteChoiceParams.INTERSEC, paramIntersections);
		params.put(RouteChoiceParams.DELAY, avgIntersectionDelay);
		
		final int totalExpectedFlow = passengerODM.getTotalIntFlow();
		this.tripList = new ArrayList<Trip>((int)Math.ceil(totalExpectedFlow / 0.75)); //use expected flow divided by load factor as array list initial capacity

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		//sort nodes based on the gravitating population
		this.roadNetwork.sortGravityNodes();

		List<String> origins = passengerODM.getUnsortedOrigins();
		List<String> destinations = passengerODM.getUnsortedDestinations();
		
		//for each OD pair from the passengerODM
		for (String originZone: origins)
			for (String destinationZone: destinations) {
				
			if (passengerODM.getIntFlow(originZone, destinationZone) == 0) continue;	
	
			List<Integer> listOfOriginNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(originZone)); //the list is already sorted
			List<Integer> listOfDestinationNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(destinationZone)); //the list is already sorted

			//removing blacklisted nodes
			for (Integer originNode: roadNetwork.getZoneToNodes().get(originZone))
				//check if any of the nodes is blacklisted
				if (this.roadNetwork.getStartNodeBlacklist()[originNode]) 
					listOfOriginNodes.remove(originNode);

			//removing blacklisted nodes
			for (Integer destinationNode: roadNetwork.getZoneToNodes().get(destinationZone))
				//check if any of the nodes is blacklisted
				if (this.roadNetwork.getEndNodeBlacklist()[destinationNode]) 
					listOfDestinationNodes.remove(destinationNode);

			//calculate number of trip assignments
			int flow = (int) Math.floor(passengerODM.getIntFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getIntFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getIntFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

				//choose time of day
				TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistribution);
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				VehicleType vht = this.chooseCarVehicleType(AVFractions);
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
				if (engine == null) LOGGER.warn("Engine type not chosen!");

				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node

				Integer originNode = null;
				Integer destinationNode = null;

				if (originZone.equals(destinationZone)) { //if intra-zonal

					//choose origin node
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
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
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
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

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}
						chosenRoute = new Route(fastestPath, roadNetwork);
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

						fetchedRouteSet.calculateUtilities(vht, engine, this.linkTravelTimePerTimeOfDay.get(hour), this.energyConsumptions, this.relativeFuelEfficiencies, this.energyUnitCosts, linkCharges, params);
						fetchedRouteSet.calculateProbabilities();
						//}

						//choose the route
						chosenRoute = fetchedRouteSet.choose();
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
				
				//get zone IDs for storing into trips
				int originZoneID = this.zoning.getLadCodeToIDMap().get(originZone);
				int destinationZoneID = this.zoning.getLadCodeToIDMap().get(destinationZone);

				//store trip in trip list
				Trip trip = new Trip(vht, engine, chosenRoute, hour, originZoneID, destinationZoneID, multiplier);
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
	 * @param props Properties.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsTempro(AssignableODMatrix passengerODM, Zoning zoning, RouteSetGenerator rsg, Properties props) {

		LOGGER.info("Assigning the passenger flows from the tempro passenger matrix...");

		//to store routes generated during the assignment
		//RouteSetGenerator rsg = new RouteSetGenerator(this.roadNetwork);
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork, props);

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		List<String> origins = passengerODM.getUnsortedOrigins();
		List<String> destinations = passengerODM.getUnsortedDestinations();
		
		//for each OD pair from the passengerODM
		for (String originZone: origins)
			for (String destinationZone: destinations) {
				
			if (passengerODM.getIntFlow(originZone, destinationZone) == 0) continue;	
			

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
			int flow = (int) Math.floor(passengerODM.getIntFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getIntFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getIntFlow(originZone, destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

				TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistribution);
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				VehicleType vht = this.chooseCarVehicleType(AVFractions);
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
				if (engine == null) LOGGER.warn("Engine type not chosen!");

				//choose origin/destination nodes based on the gravitating population
				//the choice with replacement means that possibly: destination node = origin node
				//the choice without replacement means that destination node has to be different from origin node

				//choose origin/destination node
				Integer originNode = null;
				Integer destinationNode = null;

				if (originZone.equals(destinationZone)) { 	//if inter-zonal, pick random node within the zone (based on gravitating population is better)

					List<Integer> listOfContainedNodes = zoning.getZoneToListOfContainedNodes().get(originZone);

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
					double cumulativeProbability = 0.0;
					//Integer originNode = null;
					double random = rng.nextDouble();
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
				Node from = roadNetwork.getNodeIDtoNode()[originNode];
				Node to = roadNetwork.getNodeIDtoNode()[destinationNode];
				//					System.out.println("from " + from + " to " + to);
				Route foundRoute = null;
				try {

					//see if that route already exists in the route storage
					RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
					if (rs != null) foundRoute = rs.getChoiceSet().get(0); //take the first route

					//if route does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundRoute == null) {
						//System.out.println("The path does not exist in the path storage");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}

						foundRoute = new Route(fastestPath, roadNetwork);
						rsg.addRoute(foundRoute); //add to the route set
					}

					int multiplier = 1;
					if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
					counterAssignedTrips += multiplier;

					//store trip in trip list
					Integer originZoneID = zoning.getTemproCodeToIDMap().get(originZone);
					Integer destinationZoneID = zoning.getTemproCodeToIDMap().get(destinationZone);
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
	 * Uses the route choice and pre-generated paths.
	 * @param passengerODM Passenger origin-destination matrix with flows to be assigned.
	 * @param zoning Contains Tempro zone information.
	 * @param rsg Route set generator containing the routes.
	 * @param routeChoiceParameters Route choice parameters.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsRouteChoiceTempro(AssignableODMatrix passengerODM, Zoning zoning, RouteSetGenerator rsg, Properties routeChoiceParameters) {

		LOGGER.info("Assigning the passenger flows from the tempro passenger matrix...");

		//to store routes generated during the assignment
		//RouteSetGenerator rsg = new RouteSetGenerator(this.roadNetwork);
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork, routeChoiceParameters);
		
		//read route choice parameters from the properties file
		double paramTime = Double.parseDouble(routeChoiceParameters.getProperty("TIME"));
		double paramLength = Double.parseDouble(routeChoiceParameters.getProperty("LENGTH"));
		double paramCost = Double.parseDouble(routeChoiceParameters.getProperty("COST"));
		double paramIntersections = Double.parseDouble(routeChoiceParameters.getProperty("INTERSECTIONS"));
		double avgIntersectionDelay = Double.parseDouble(routeChoiceParameters.getProperty("AVERAGE_INTERSECTION_DELAY"));
		
		//set route choice parameters
		Map<RouteChoiceParams, Double> params = new EnumMap<>(RouteChoiceParams.class);
		params.put(RouteChoiceParams.TIME, paramTime);
		params.put(RouteChoiceParams.LENGTH, paramLength);
		params.put(RouteChoiceParams.COST, paramCost);
		params.put(RouteChoiceParams.INTERSEC, paramIntersections);
		params.put(RouteChoiceParams.DELAY, avgIntersectionDelay);

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		List<String> origins = passengerODM.getUnsortedOrigins();
		List<String> destinations = passengerODM.getUnsortedDestinations();
		
		//for each OD pair from the passengerODM
		for (String originZone: origins)
			for (String destinationZone: destinations) {
				
			if (passengerODM.getIntFlow(originZone, destinationZone) == 0) continue;	

			//calculate number of trip assignments
			int flow = (int) Math.floor(passengerODM.getIntFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
			int remainder = passengerODM.getIntFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
			counterTotalFlow += passengerODM.getIntFlow(originZone, destinationZone);
			
			//get zone IDs
			Integer originZoneID = zoning.getTemproCodeToIDMap().get(originZone);
			Integer destinationZoneID = zoning.getTemproCodeToIDMap().get(destinationZone);

			//for each trip
			for (int i=0; i < (flow + remainder); i++) {

				//choose time of day
				TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistribution);
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//choose vehicle
				VehicleType vht = this.chooseCarVehicleType(AVFractions);
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
				if (engine == null) LOGGER.warn("Engine type not chosen!");

				//choose origin node
				int originNode = zoning.getZoneIDToNearestNodeIDMap()[originZoneID];
				if (originNode == 0) LOGGER.warn("Origin node was not chosen for zone {}", originZone);

				if (this.roadNetwork.isBlacklistedAsStartNode(originNode)) 
					LOGGER.warn("Origin node is blacklisted! node: {}", originNode);

				//choose destination node
				int destinationNode = zoning.getZoneIDToNearestNodeIDMap()[destinationZoneID];
				if (destinationNode == 0) LOGGER.warn("Destination node was not chosen for zone {}", destinationZone);

				if (this.roadNetwork.isBlacklistedAsEndNode(destinationNode)) 
					LOGGER.warn("Destination node is blacklisted! node: {}", destinationNode);

				//choose the route
				Route chosenRoute = this.chooseRoute(originNode, destinationNode, vht, engine, hour, rsg, params);
				if (chosenRoute == null) {
					LOGGER.warn("No chosen route between nodes {} and {}", originNode, destinationNode);
					continue;
				}
				if (chosenRoute.isEmpty()) {
					LOGGER.warn("The chosen route is empty, skipping this trip!");
					continue;
				}

				int multiplier = 1;
				if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
				counterAssignedTrips += multiplier;

				Trip trip = new TripTempro(vht, engine, chosenRoute, hour, originZoneID, destinationZoneID, zoning, multiplier);
				this.tripList.add(trip);

			}//for each trip
		}//for each OD pair

		LOGGER.debug("Total flow: {}", counterTotalFlow);
		LOGGER.debug("Total assigned trips: {}", counterAssignedTrips);
		LOGGER.debug("Succesfully assigned trips: {}", 100.0* counterAssignedTrips / counterTotalFlow);
	}

	/** 
	 * Assigns passenger origin-destination matrix to the road network using the combined Tempro/LAD zoning system.
	 * When Tempro zones a farther than a distance threshold, it seeks for the nodes within LAD zones that have a route set.
	 * Uses the route choice and pre-generated paths (after a distance threshold, there will be less inter-zonal routes).
	 * @param passengerODM Passenger origin-destination matrix with flows to be assigned.
	 * @param zoning Contains Tempro zone information.
	 * @param rsg Route set generator containing the routes.
	 * @param routeChoiceParameters Route choice parameters.
	 */
	@SuppressWarnings("unused")
	public void assignPassengerFlowsRouteChoiceTemproDistanceBased(AssignableODMatrix passengerODM, Zoning zoning, RouteSetGenerator rsg, Properties routeChoiceParameters) {

		LOGGER.info("Assigning the passenger flows from the tempro passenger matrix using a combined tempro/LAD route set...");

		final double distanceThreshold = Double.parseDouble(routeChoiceParameters.getProperty("DISTANCE_THRESHOLD"));
		final double minLength = Double.parseDouble(routeChoiceParameters.getProperty("MINIMUM_MINOR_TRIP_LENGTH"));
		final double maxLengthFactor = Double.parseDouble(routeChoiceParameters.getProperty("MAXIMUM_MINOR_TRIP_FACTOR"));
		//read route choice parameters from the properties file
		final double paramTime = Double.parseDouble(routeChoiceParameters.getProperty("TIME"));
		final double paramLength = Double.parseDouble(routeChoiceParameters.getProperty("LENGTH"));
		final double paramCost = Double.parseDouble(routeChoiceParameters.getProperty("COST"));
		final double paramIntersections = Double.parseDouble(routeChoiceParameters.getProperty("INTERSECTIONS"));
		final double avgIntersectionDelay = Double.parseDouble(routeChoiceParameters.getProperty("AVERAGE_INTERSECTION_DELAY"));
		
		//set route choice parameters
		Map<RouteChoiceParams, Double> params = new EnumMap<>(RouteChoiceParams.class);
		params.put(RouteChoiceParams.TIME, paramTime);
		params.put(RouteChoiceParams.LENGTH, paramLength);
		params.put(RouteChoiceParams.COST, paramCost);
		params.put(RouteChoiceParams.INTERSEC, paramIntersections);
		params.put(RouteChoiceParams.DELAY, avgIntersectionDelay);

		//to store routes generated during the assignment
		//RouteSetGenerator rsg = new RouteSetGenerator(this.roadNetwork);
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork, routeChoiceParameters);

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		List<String> origins = passengerODM.getUnsortedOrigins();
		List<String> destinations = passengerODM.getUnsortedDestinations();

		//for each OD pair from the passengerODM
		for (String originZone: origins)
			for (String destinationZone: destinations) {

				if (passengerODM.getIntFlow(originZone, destinationZone) == 0) continue;

				//get distance between Tempro zone centroids
				int originZoneID = zoning.getTemproCodeToIDMap().get(originZone);
				int destinationZoneID = zoning.getTemproCodeToIDMap().get(destinationZone);
				final double centroidDistance = zoning.getZoneToZoneDistanceMatrix()[originZoneID][destinationZoneID];

				//calculate number of trip assignments
				int flow = (int) Math.floor(passengerODM.getIntFlow(originZone, destinationZone) * this.assignmentFraction); //assigned fractionally and later augmented
				int remainder = passengerODM.getIntFlow(originZone, destinationZone) - (int) Math.round(flow / this.assignmentFraction); //remainder of trips will be assigned individually (each trip)
				counterTotalFlow += passengerODM.getIntFlow(originZone, destinationZone);

				//for each trip
				for (int i=0; i < (flow + remainder); i++) {

					//choose time of day
					TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistribution);
					if (hour == null) LOGGER.warn("Time of day not chosen!");

					//choose vehicle
					VehicleType vht = this.chooseCarVehicleType(AVFractions);
					if (vht == null) LOGGER.warn("Vehicle type not chosen!");

					//choose engine
					EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
					if (engine == null) LOGGER.warn("Engine type not chosen!");

					//choose origin and destination node
					Integer originNode = null, destinationNode = null;

					if (originZoneID != destinationZoneID) { //if not tempro intra-zonal (minor road) trip

						if (centroidDistance <= distanceThreshold) { //use nodes nearest to the tempro zone centroid

							originNode = zoning.getZoneIDToNearestNodeIDMap()[originZoneID];
							if (originNode == 0) LOGGER.warn("Origin node was not chosen for zone {}", originZone);

							//if (this.roadNetwork.isBlacklistedAsStartNode(originNode)) 
							//	LOGGER.warn("Origin node is blacklisted! node: {}", originNode);

							destinationNode = zoning.getZoneIDToNearestNodeIDMap()[destinationZoneID];
							if (destinationNode == 0) LOGGER.warn("Destination node was not chosen for zone {}", destinationZone);

							//if (this.roadNetwork.isBlacklistedAsEndNode(destinationNode)) 
							//	LOGGER.warn("Destination node is blacklisted! node: {}", destinationNode);

						} else { //otherwise use one of the nodes from the LAD zone

							String originLAD = zoning.getZoneToLADMap().get(originZone);
							String destinationLAD = zoning.getZoneToLADMap().get(destinationZone);

							//use precomputed nearest nodes from the zoning
							originNode = zoning.getZoneIDToNearestNodeIDFromLADTopNodesMap()[originZoneID];
							destinationNode = zoning.getZoneIDToNearestNodeIDFromLADTopNodesMap()[destinationZoneID];

							if (originNode == 0) LOGGER.warn("Origin node was not chosen for zone {}", originZone);
							if (destinationNode == 0) LOGGER.warn("Destination node was not chosen for zone {}", destinationZone);
						}

						//choose the route
						Route chosenRoute = this.chooseRoute(originNode.intValue(), destinationNode.intValue(), vht, engine, hour, rsg, params);
						if (chosenRoute == null) {
							LOGGER.warn("No chosen route between nodes {} and {}", originNode, destinationNode);
							continue;
						}
						if (chosenRoute.isEmpty()) {
							LOGGER.warn("The chosen route is empty, skipping this trip!");
							continue;
						}

						int multiplier = 1;
						if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
						counterAssignedTrips += multiplier;

						//store trip in trip list
						Trip trip = new TripTempro(vht, engine, chosenRoute, hour, originZoneID, destinationZoneID, zoning, multiplier);
						this.tripList.add(trip);

					} else { //tempro inter-zonal (minor road) trip

						int multiplier = 1;
						if (i < flow) multiplier = (int) Math.round(1 / this.assignmentFraction);
						counterAssignedTrips += multiplier;

						//generate minor trip length
						//double minLength = zoning.getZoneToMinMaxDimension()[originZoneID][0];
						double maxLength = zoning.getZoneToMinMaxDimension()[originZoneID][1] * maxLengthFactor;
						double length = minLength + rng.nextDouble() * (maxLength - minLength);

						//store trip in trip list
						Trip trip = new TripMinor(vht, engine, hour, originZoneID, destinationZoneID, length, zoning, multiplier);
						this.tripList.add(trip);
					}
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
	 * @param props Properties.
	 */
	@SuppressWarnings("unused")
	public void assignFreightFlowsRouting(FreightMatrix freightMatrix, RouteSetGenerator rsg, Properties props) {


		LOGGER.info("Assigning the vehicle flows from the freight matrix...");

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		//to store routes generated during the assignment
		if (rsg == null) rsg = new RouteSetGenerator(this.roadNetwork, props);

		//sort nodes based on the gravitating workplace population
		this.roadNetwork.sortGravityNodesFreight();

		//for each OD pair from the passengerODM		
		for (MultiKey mk: freightMatrix.getKeySet()) {
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
				TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistributionFreight);
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//get freight vehicle type from the freight matrix value
				VehicleType fvht = VehicleType.values()[vehicleType];
				//choose actual vehicle type (autonomous or non-autonomous)
				VehicleType vht = this.chooseFreightVehicleType(fvht, this.AVFractions);
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
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
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
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
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
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
				Node from = roadNetwork.getNodeIDtoNode()[originNode];
				Node to = roadNetwork.getNodeIDtoNode()[destinationNode];
				//					System.out.println("from " + from + " to " + to);
				Route foundRoute = null;
				try {
					//see if that route already exists in the route storage
					RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
					if (rs != null) foundRoute = rs.getChoiceSet().get(0); //take the first route

					//if route does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundRoute == null) {
						//System.out.println("The path does not exist in the path storage");

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}

						foundRoute = new Route(fastestPath, roadNetwork);
						rsg.addRoute(foundRoute); //add to the route set
					}

					//if path does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundRoute == null) {

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route!");
							continue;
						}

						foundRoute = new Route(fastestPath, roadNetwork);
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
	 * @param props Properties.
	 */
	@SuppressWarnings("unused")
	public void assignFreightFlowsHourlyRouting(FreightMatrix freightMatrix, Map<TimeOfDay, RouteSetGenerator> routeStorage, Properties props) {


		LOGGER.info("Assigning the vehicle flows from the freight matrix...");

		//counters to calculate percentage of assignment success
		long counterAssignedTrips = 0;
		long counterTotalFlow = 0;

		//to store routes generated during the assignment
		if (routeStorage == null) {
			routeStorage = new EnumMap<>(TimeOfDay.class);
			for (TimeOfDay hour: TimeOfDay.values()) {
				routeStorage.put(hour, new RouteSetGenerator(this.roadNetwork, props));
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
				TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistributionFreight);
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//get freight vehicle type from the freight matrix value
				VehicleType fvht = VehicleType.values()[vehicleType];
				//choose actual vehicle type (autonomous or non-autonomous)
				VehicleType vht = this.chooseFreightVehicleType(fvht, this.AVFractions);
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
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
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
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
					double cumulativeProbability = 0.0;
					double random = rng.nextDouble();
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
				Node from = roadNetwork.getNodeIDtoNode()[originNode];
				Node to = roadNetwork.getNodeIDtoNode()[destinationNode];
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

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}

						foundRoute = new Route(fastestPath, roadNetwork);
						rsg.addRoute(foundRoute); //add to the route set
					}

					//if path does not already exist, get the shortest path from the origin node to the destination node using AStar algorithm
					if (foundRoute == null) {

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route!");
							continue;
						}

						foundRoute = new Route(fastestPath, roadNetwork);
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
		
		//read route choice parameters from the properties file
		double paramTime = Double.parseDouble(routeChoiceParameters.getProperty("TIME"));
		double paramLength = Double.parseDouble(routeChoiceParameters.getProperty("LENGTH"));
		double paramCost = Double.parseDouble(routeChoiceParameters.getProperty("COST"));
		double paramIntersections = Double.parseDouble(routeChoiceParameters.getProperty("INTERSECTIONS"));
		double avgIntersectionDelay = Double.parseDouble(routeChoiceParameters.getProperty("AVERAGE_INTERSECTION_DELAY"));
		
		//set route choice parameters
		Map<RouteChoiceParams, Double> params = new EnumMap<>(RouteChoiceParams.class);
		params.put(RouteChoiceParams.TIME, paramTime);
		params.put(RouteChoiceParams.LENGTH, paramLength);
		params.put(RouteChoiceParams.COST, paramCost);
		params.put(RouteChoiceParams.INTERSEC, paramIntersections);
		params.put(RouteChoiceParams.DELAY, avgIntersectionDelay);		

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
				TimeOfDay hour = this.chooseTimeOfDay(timeOfDayDistributionFreight);
				if (hour == null) LOGGER.warn("Time of day not chosen!");

				//get freight vehicle type from the freight matrix value
				VehicleType fvht = VehicleType.values()[vehicleType];
				//choose actual vehicle type (autonomous or non-autonomous)
				VehicleType vht = this.chooseFreightVehicleType(fvht, this.AVFractions);
				if (vht == null) LOGGER.warn("Vehicle type not chosen!");

				//choose engine
				EngineType engine = this.chooseEngineType(engineTypeFractions.get(vht));
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
						double cumulativeProbability = 0.0;
						double random = rng.nextDouble();
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
						double cumulativeProbability = 0.0;
						double random = rng.nextDouble();
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
						double cumulativeProbability = 0.0;
						double random = rng.nextDouble();
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
						double cumulativeProbability = 0.0;
						double random = rng.nextDouble();
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

						DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
						DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

						RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
						if (fastestPath == null) {
							LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
							continue;
						}
						chosenRoute = new Route(fastestPath, roadNetwork);
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

						//fetch congestion charge for the vehicle type
						HashMap<String, HashMap<Integer, Double>> linkCharges = new HashMap<String, HashMap<Integer, Double>>();
						if (this.congestionCharges != null) 
							for (String policyName: this.congestionCharges.keySet())
								linkCharges.put(policyName, (HashMap<Integer, Double>) this.congestionCharges.get(policyName).get(vht, hour));

						fetchedRouteSet.calculateUtilities(vht, engine, this.linkTravelTimePerTimeOfDay.get(hour), this.energyConsumptions, this.relativeFuelEfficiencies, this.energyUnitCosts, linkCharges, params);
						fetchedRouteSet.calculateProbabilities();
				
						//choose the route
						chosenRoute = fetchedRouteSet.choose();
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
	 * Updates link travel times per time of day.
	 */
	public void updateLinkTravelTimes() {

		this.updateLinkTravelTimes(1.0);
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
		int[] numberOfLanes = roadNetwork.getNumberOfLanes();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];
			double roadLength = this.roadNetwork.getEdgeLength(edgeID); //[km]

			//iterate through all times of day
			for (TimeOfDay hour: TimeOfDay.values()) {

				double[] hourlyVolumes = this.linkVolumesInPCUPerTimeOfDay.get(hour);
				double linkVolumeInPCU = hourlyVolumes[edgeID];
				
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
				if (edgeType == EdgeType.MOTORWAY) {//motorway

					double flow = linkVolumeInPCU / numberOfLanes[edgeID];
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

				} else if (edgeType == EdgeType.AROAD) {//A-road

					double flow = linkVolumeInPCU / numberOfLanes[edgeID];
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
					congestedTravelTime = this.roadNetwork.getFreeFlowTravelTime()[edgeID]; //ferry travel time is fixed

				double[] hourlyTimes = this.linkTravelTimePerTimeOfDay.get(hour);
				if (hourlyTimes == null) LOGGER.error("No hourly travel times for hour {}", hour);
				
				double oldLinkTravelTime = hourlyTimes[edgeID];
				if (oldLinkTravelTime == 0.0) {
					//fetch from the road network, which was likely modified by a road development intervention
					oldLinkTravelTime = this.roadNetwork.getFreeFlowTravelTime()[edgeID];
					if (oldLinkTravelTime == 0.0) LOGGER.error("No link travel time for edge {}", edgeID);
				}
				
				double averagedCongestedTravelTime = weight * congestedTravelTime + (1 - weight) * oldLinkTravelTime; //averaging
				hourlyTimes[edgeID] = averagedCongestedTravelTime;
				
			}//for time of day
		}//while edges
	}

	/** 
	 * Assigns passenger and freight origin-destination matrix to the road network
	 * using the fastest path based on the current values in the linkTravelTime field.
	 * Finally, updates link travel times using weighted averaging.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param rsg Route set generator to store fastest routes generated during the assignment (but could be pregenerated too).
	 * @param props Parameters from the config file.
	 * @param weight Weighting parameter.
	 */
	public void assignFlowsAndUpdateLinkTravelTimes(AssignableODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, Properties props, double weight) {

		this.assignPassengerFlowsRouting(passengerODM, rsg, props);
		this.assignFreightFlowsRouting(freightODM, rsg, props);
		this.updateLinkVolumeInPCU();
		this.updateLinkVolumeInPCUPerTimeOfDay();
		this.updateLinkVolumePerVehicleType();
		this.updateLinkTravelTimes(weight);
	}

	/** 
	 * Assigns passenger and freight origin-destination matrix to the road network
	 * using specification in the config file.
	 * Finally, updates link travel times using weighted averaging.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param rsg Route set generator object with routes to be used for the assignment (if route choice used) or an object in which to store routes (if routing used).
	 * @param zoning Zoning system (necessary for 'tempro' and 'combined' assignment types).
	 * @param params Parameters from the config file.
	 * @param weight Weighting parameter.
	 */
	public void assignFlowsAndUpdateLinkTravelTimes(AssignableODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, Zoning zoning, Properties params, double weight) {

		final String assignmentType = params.getProperty("ASSIGNMENT_TYPE").toLowerCase();
		final Boolean flagUseRouteChoiceModel = Boolean.parseBoolean(params.getProperty("USE_ROUTE_CHOICE_MODEL"));

		//assign passenger demand
		if (assignmentType.equals("lad")) {
			if (flagUseRouteChoiceModel) {
				this.assignPassengerFlowsRouteChoice(passengerODM, rsg, params);
			} else {
				this.assignPassengerFlowsRouting(passengerODM, rsg, params);
			}
		} else if (assignmentType.equals("tempro")) {
			if (flagUseRouteChoiceModel) {
				this.assignPassengerFlowsRouteChoiceTempro(passengerODM, zoning, rsg, params);
			} else {
				this.assignPassengerFlowsTempro(passengerODM, zoning, rsg, params);
			}
		} else if (assignmentType.equals("combined")) {
			if (flagUseRouteChoiceModel) {
				this.assignPassengerFlowsRouteChoiceTemproDistanceBased(passengerODM, zoning, rsg, params);
			} else {
				this.assignPassengerFlowsTempro(passengerODM, zoning, rsg, params);
			}
		} else {
			LOGGER.error("Unkown assignment type in the config file. Allowed values: 'lad', 'tempro', 'combined'");
			return;
		}
		
		//assign freight demand
		if (flagUseRouteChoiceModel) {
			this.assignFreightFlowsRouteChoice(freightODM, rsg, params);
		} else {
			this.assignFreightFlowsRouting(freightODM, rsg, params);
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
	 * @param props Properties.
	 * @param weight Weighting parameter.
	 * @param iterations Number of iterations.
	 */
	public void assignFlowsAndUpdateLinkTravelTimesIterated(AssignableODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, Properties props, double weight, int iterations) {

		for (int i=0; i<iterations; i++) {
			this.resetLinkVolumes(); //link volumes must be reset or they would compound across all iterations
			this.resetTripList(); //clear route storages
			this.assignFlowsAndUpdateLinkTravelTimes(passengerODM, freightODM, rsg, props, weight);
		}
	}

	/** 
	 * Iterates assignment and travel time update a fixed number of times.
	 * @param passengerODM Passenger origin-destination matrix.
	 * @param freightODM Freight origin-destination matrix.
	 * @param rsg Route set generator object with routes to be used for the assignment (if route choice used) or an object in which to store routes (if routing used).
	 * @param zoning Zoning system (necessary for 'tempro' and 'combined' assignment types).
	 * @param params Parameters from the config file.
	 * @param weight Weighting parameter.
	 * @param iterations Number of iterations.
	 */
	public void assignFlowsAndUpdateLinkTravelTimesIterated(AssignableODMatrix passengerODM, FreightMatrix freightODM, RouteSetGenerator rsg, Zoning zoning, Properties params, double weight, int iterations) {

		for (int i=0; i<iterations; i++) {
			this.resetLinkVolumes(); //link volumes must be reset or they would compound across all iterations
			this.resetTripList(); //clear route storages
			this.assignFlowsAndUpdateLinkTravelTimes(passengerODM, freightODM, rsg, zoning, params, weight);
		}
	}
	
	/**
	 * Calculate assigned OD matrix from trip list.
	 * @param ODMatrixMultiKey OD matrix.
	 */
	public ODMatrixMultiKey calculateAssignedODMatrix() {

		ODMatrixMultiKey counter = new ODMatrixMultiKey();

		for (Trip trip: this.tripList) {

			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles

			int originLadID = trip.getOriginLadID();
			int destinationLadID = trip.getDestinationLadID();
			int multiplier = trip.getMultiplier();
			
			String originLAD = this.zoning.getLadIDToCodeMap()[originLadID];
			String destinationLAD = this.zoning.getLadIDToCodeMap()[destinationLadID];
			
			int count = counter.getFlow(originLAD, destinationLAD);
			counter.setFlow(originLAD, destinationLAD, count + multiplier);
		}
		
		return counter;
	}
	
	/**
	 * Updates travel time skim matrix (zone-to-zone travel times).
	 * @param timeSkimMatrix Inter-zonal skim matrix (time).
	 */
	public void updateTimeSkimMatrix(SkimMatrix timeSkimMatrix) {

		//this.updateLinkTravelTimes();

		SkimMatrix counter = new SkimMatrixArray(zoning);

		for (Trip trip: this.tripList) {

			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles

			int originLAD = trip.getOriginLadID();
			int destinationLAD = trip.getDestinationLadID();
			int multiplier = trip.getMultiplier();
				
			double count = counter.getCost(originLAD, destinationLAD);
			counter.setCost(originLAD, destinationLAD, count + multiplier);

			double sum = timeSkimMatrix.getCost(originLAD, destinationLAD);
			double tripTravelTime = trip.getTravelTime(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), averageIntersectionDelay, this.roadNetwork.getNodeToAverageAccessEgressDistance(), this.averageAccessEgressSpeedCar, this.flagIncludeAccessEgress);
			timeSkimMatrix.setCost(originLAD, destinationLAD, sum + tripTravelTime * multiplier);
		}
		
		List<String> origins = timeSkimMatrix.getUnsortedOrigins();
		List<String> destinations = timeSkimMatrix.getUnsortedDestinations();
		for (String originLAD: origins)
			for (String destinationLAD: destinations) {
				double averageODtraveltime = timeSkimMatrix.getCost(originLAD, destinationLAD) / counter.getCost(originLAD, destinationLAD);
				timeSkimMatrix.setCost(originLAD, destinationLAD, averageODtraveltime);
		}
	}

	/**
	 * Calculated travel time skim matrix (zone-to-zone travel times).
	 * @return Inter-zonal skim matrix (time).
	 */
	public SkimMatrix calculateTimeSkimMatrix() {

		SkimMatrix timeSkimMatrix = new SkimMatrixArray(zoning);
		this.updateTimeSkimMatrix(timeSkimMatrix);

		return timeSkimMatrix;
	}

	/**
	 * Updates travel time skim matrix (zone-to-zone travel times) for freight.
	 * @param timeSkimMatrixFreight Inter-zonal skim matrix (time).
	 */
	public void updateTimeSkimMatrixFreight(SkimMatrixFreightMultiKey timeSkimMatrixFreight) {

		//this.updateLinkTravelTimes();

		SkimMatrixFreightMultiKey counter = new SkimMatrixFreightMultiKey();

		for (Trip trip: this.tripList) {

			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN ||
					vht == VehicleType.ARTIC_AV || vht == VehicleType.RIGID_AV || vht == VehicleType.VAN_AV)) continue; //skip non-freight vehicles

			//map AV types to non-AV types
			if (vht == VehicleType.ARTIC_AV) vht = VehicleType.ARTIC;
			if (vht == VehicleType.RIGID_AV) vht = VehicleType.RIGID;
			if (vht == VehicleType.VAN_AV) vht = VehicleType.VAN;			

			int origin = trip.getOrigin();
			int destination = trip.getDestination();

			double count = counter.getCost(origin, destination, vht.value);
			counter.setCost(origin, destination, vht.value, count + 1);

			double sum = timeSkimMatrixFreight.getCost(origin, destination, vht.value);
			double tripTravelTime = trip.getTravelTime(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.averageIntersectionDelay, this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), this.averageAccessEgressSpeedFreight, this.flagIncludeAccessEgress);
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
	public SkimMatrixFreightMultiKey calculateTimeSkimMatrixFreight() {

		SkimMatrixFreightMultiKey timeSkimMatrixFreight = new SkimMatrixFreightMultiKey();
		this.updateTimeSkimMatrixFreight(timeSkimMatrixFreight);

		return timeSkimMatrixFreight;
	}

	/**
	 * Updates cost skim matrix (zone-to-zone financial costs).
	 * @param costSkimMatrix Inter-zonal skim matrix (cost).
	 */
	public void updateCostSkimMatrix(SkimMatrix costSkimMatrix) {

		//this.updateLinkTravelTimes();

		SkimMatrix counter = new SkimMatrixArray(zoning);

		for (Trip trip: this.tripList) {

			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles

			int originLAD = trip.getOriginLadID();
			int destinationLAD = trip.getDestinationLadID();
			int multiplier = trip.getMultiplier();

			double count = counter.getCost(originLAD, destinationLAD);
			counter.setCost(originLAD, destinationLAD, count + multiplier);

			double sum = costSkimMatrix.getCost(originLAD, destinationLAD);
			double tripFuelCost = trip.getCost(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistance(), averageAccessEgressSpeedCar, this.energyUnitCosts, this.energyConsumptions, this.relativeFuelEfficiencies, this.congestionCharges, this.flagIncludeAccessEgress);
			costSkimMatrix.setCost(originLAD, destinationLAD, sum + tripFuelCost * multiplier);
		}
		
		List<String> origins = costSkimMatrix.getUnsortedOrigins();
		List<String> destinations = costSkimMatrix.getUnsortedDestinations();
		for (String originLAD: origins)
			for (String destinationLAD: destinations) {
				double averageODcost = costSkimMatrix.getCost(originLAD, destinationLAD) / counter.getCost(originLAD, destinationLAD);
				costSkimMatrix.setCost(originLAD, destinationLAD, averageODcost);
		}

	}

	/**
	 * Calculates cost skim matrix (zone-to-zone financial costs).
	 * @return Inter-zonal skim matrix (cost).
	 */
	public SkimMatrix calculateCostSkimMatrix() {

		SkimMatrix costSkimMatrix = new SkimMatrixArray(zoning);
		this.updateCostSkimMatrix(costSkimMatrix);

		return costSkimMatrix;
	}

	/**
	 * Updates cost skim matrix (zone-to-zone distances).
	 * @return Inter-zonal skim matrix (distance).
	 */
	public SkimMatrix calculateDistanceSkimMatrix() {

		SkimMatrix distanceSkimMatrix = new SkimMatrixArray(zoning);
		SkimMatrix counter = new SkimMatrixArray(zoning);

		for (Trip trip: this.tripList) {

			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles

			String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
			String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
			int multiplier = trip.getMultiplier();

			double count = counter.getCost(originLAD, destinationLAD);
			counter.setCost(originLAD, destinationLAD, count + multiplier);

			double sum = distanceSkimMatrix.getCost(originLAD, destinationLAD);
			double distance = trip.getLength(this.roadNetwork.getNodeToAverageAccessEgressDistance());
			distanceSkimMatrix.setCost(originLAD, destinationLAD, sum + distance * multiplier);
		}

		List<String> origins = distanceSkimMatrix.getUnsortedOrigins();
		List<String> destinations = distanceSkimMatrix.getUnsortedDestinations();
		for (String originLAD: origins)
			for (String destinationLAD: destinations) {
				double averageODtraveltime = distanceSkimMatrix.getCost(originLAD, destinationLAD) / counter.getCost(originLAD, destinationLAD);
				distanceSkimMatrix.setCost(originLAD, destinationLAD, averageODtraveltime);
		}


		return distanceSkimMatrix;
	}

	/**
	 * Updates cost skim matrix (zone-to-zone distances).
	 * @param zoning Zoning system.
	 * @return Inter-zonal skim matrix (distance).
	 */
	public SkimMatrix calculateDistanceSkimMatrixTempro() {

		SkimMatrix distanceSkimMatrix = new SkimMatrixArrayTempro(zoning);
		SkimMatrix counter = new SkimMatrixArrayTempro(zoning);

		for (Trip trip: this.tripList) {

			if (trip instanceof TripTempro && (trip.getVehicle() == VehicleType.CAR || trip.getVehicle() == VehicleType.CAR_AV)) {

				TripTempro temproTrip = (TripTempro) trip;

				String originZone = temproTrip.getOriginTemproZone();
				String destinationZone = temproTrip.getDestinationTemproZone();
				int multiplier = trip.getMultiplier();

				double count = counter.getCost(originZone, destinationZone);
				counter.setCost(originZone, destinationZone, count + multiplier);

				double sum = distanceSkimMatrix.getCost(originZone, destinationZone);
				double distance = temproTrip.getLength();
				distanceSkimMatrix.setCost(originZone, destinationZone, sum + distance * multiplier);
			}
		}
		
		List<String> origins = distanceSkimMatrix.getUnsortedOrigins();
		List<String> destinations = distanceSkimMatrix.getUnsortedDestinations();
		for (String origin: origins)
			for (String destination: destinations) {
				double averageODtraveltime = distanceSkimMatrix.getCost(origin, destination) / counter.getCost(origin, destination);
				distanceSkimMatrix.setCost(origin, destination, averageODtraveltime);
		}

		return distanceSkimMatrix;
	}

	/**
	 * Updates cost skim matrix (zone-to-zone distances) for freight.
	 * @return Inter-zonal skim matrix (distance).
	 */
	public SkimMatrixFreightMultiKey calculateDistanceSkimMatrixFreight() {

		SkimMatrixFreightMultiKey distanceSkimMatrixFreight = new SkimMatrixFreightMultiKey();
		SkimMatrixFreightMultiKey counter = new SkimMatrixFreightMultiKey();

		for (Trip trip: this.tripList) {

			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN ||
					vht == VehicleType.ARTIC_AV || vht == VehicleType.RIGID_AV || vht == VehicleType.VAN_AV)) continue; //skip non-freight vehicles

			//map AV types to non-AV types
			if (vht == VehicleType.ARTIC_AV) vht = VehicleType.ARTIC;
			if (vht == VehicleType.RIGID_AV) vht = VehicleType.RIGID;
			if (vht == VehicleType.VAN_AV) vht = VehicleType.VAN;	

			int origin = trip.getOrigin();
			int destination = trip.getDestination();
			int multiplier = trip.getMultiplier();

			double count = counter.getCost(origin, destination, vht.value);
			counter.setCost(origin, destination, vht.value, count + multiplier);

			double sum = distanceSkimMatrixFreight.getCost(origin, destination, vht.value);
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
	 * Calculates observed trip length frequencies.
	 * @param binLimitsInKm Bin limits in kilometres.
	 * @param flagIncludeAccessEgress If true include access and egress into trip distance calculation.
	 * @param flagIncludeMinorTrips If true include minor road trips into trip distance calculation.
	 * @return Observed trip length distribution.
	 */
	public double[] getObservedTripLengthFrequencies(double[] binLimitsInKm, boolean flagIncludeAccessEgress, boolean flagIncludeMinorTrips) {
		
		double[] frequences = new double[binLimitsInKm.length];
		
		for (Trip trip: this.tripList) {
			
			if (!(trip.getVehicle() == VehicleType.CAR || trip.getVehicle() == VehicleType.CAR_AV)) continue; //skip freight
			
			Double tripDistance;
			
			if (trip instanceof TripMinor) //minor trips
				if (flagIncludeMinorTrips)
					tripDistance = ((TripMinor)trip).getLength();
				else
					continue;
			
			else if (trip instanceof TripTempro) //tempro trips
				if (flagIncludeAccessEgress)
					tripDistance = ((TripTempro)trip).getLength(); //with access/egress
				else 
					tripDistance = trip.getRoute().getLength(); //without access/egress
			
			else //lad trips
				if (flagIncludeAccessEgress)
					tripDistance = trip.getLength(roadNetwork.getNodeToAverageAccessEgressDistance()); //with access/egress
				else
					tripDistance = trip.getRoute().getLength(); //without access/egress
			
			//find in which bin it falls
			for (int i=1; i<binLimitsInKm.length; i++) {
				if (tripDistance < binLimitsInKm[i]) {
					frequences[i-1] += trip.multiplier;
					break;
				}
				if (tripDistance >= binLimitsInKm[binLimitsInKm.length-1])
					frequences[binLimitsInKm.length-1] += trip.multiplier;
			}
		}
	
		return frequences;
	}

	/**
	 * Calculates observed trip length distribution.
	 * @param binLimitsInKm Bin limits in kilometres.
	 * @param flagIncludeAccessEgress If true include access and eggress into trip distance calculation.
	 * 	 * @param flagIncludeMinorTrips If true include minor road trips into trip distance calculation.

	 * @return Observed trip length distribution.
	 */
	public double[] getObservedTripLengthDistribution(double[] binLimitsInKm, boolean flagIncludeAccessEgress, boolean flagIncludeMinorTrips) {
		
		double[] distribution = this.getObservedTripLengthFrequencies(binLimitsInKm, flagIncludeAccessEgress, flagIncludeMinorTrips);
		
		//normalise distribution
		double sum = 0.0;
		for (int i=0; i<binLimitsInKm.length; i++) sum += distribution[i];
		for (int i=0; i<binLimitsInKm.length; i++) distribution[i] /= sum;
		
		return distribution;
	}
	
	/**
	 * Updates cost skim matrix (zone-to-zone financial costs) for freight.
	 * @param costSkimMatrixFreight Inter-zonal skim matrix (cost) for freight.
	 */
	public void updateCostSkimMatrixFreight(SkimMatrixFreightMultiKey costSkimMatrixFreight) {

		//this.updateLinkTravelTimes();
		SkimMatrixFreightMultiKey counter = new SkimMatrixFreightMultiKey();

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

			int origin = trip.getOrigin();
			int destination = trip.getDestination();
			int multiplier = trip.getMultiplier();

			double count = counter.getCost(origin, destination, vht.value);
			counter.setCost(origin, destination, vht.value, count + multiplier);

			double sum = costSkimMatrixFreight.getCost(origin, destination, vht.value);
			double tripFuelCost = trip.getCost(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), averageAccessEgressSpeedFreight, this.energyUnitCosts, this.energyConsumptions, this.relativeFuelEfficiencies, this.congestionCharges, this.flagIncludeAccessEgress);

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
	public SkimMatrixFreightMultiKey calculateCostSkimMatrixFreight() {

		SkimMatrixFreightMultiKey costSkimMatrixFreight = new SkimMatrixFreightMultiKey();
		this.updateCostSkimMatrixFreight(costSkimMatrixFreight);

		return costSkimMatrixFreight;
	}

	/**
	 * Calculates total energy consumption for each car/AV energy type (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each energy type.
	 */
	public Map<EnergyType, Double> calculateCarEnergyConsumptions() {

		Map<EnergyType, Double> consumptions = new EnumMap<>(EnergyType.class);
		for (EnergyType energy: EnergyType.values()) consumptions.put(energy, 0.0);

		for (Trip trip: this.tripList) {
			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles
			Map<EnergyType, Double> consumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistance(), averageAccessEgressSpeedCar, this.energyConsumptions, this.relativeFuelEfficiencies, this.flagIncludeAccessEgress);
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
	public Map<EnergyType, HashMap<String, Double>> calculateZonalCarEnergyConsumptions(final double originZoneEnergyWeight) {

		//initialise hashmaps
		Map<EnergyType, HashMap<String, Double>> zonalConsumptions = new EnumMap<>(EnergyType.class);
		for (EnergyType energy: EnergyType.values()) {
			HashMap<String, Double> consumption = new HashMap<String, Double>();
			zonalConsumptions.put(energy, consumption);
		}

		for (Trip trip: this.tripList) {

			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles

			Map<EnergyType, Double> tripConsumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), averageAccessEgressSpeedFreight, this.energyConsumptions, this.relativeFuelEfficiencies, this.flagIncludeAccessEgress);

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
	 * Calculates zonal (per LAD) and temporal (per hour) electricity consumption for car vehicles (in kWh).
	 * @param originZoneEnergyWeight Percentage of energy consumption assigned to origin zone (the rest assigned to destination zone).
	 * @return Electricity consumption per zone and time of day.
	 */
	public HashMap<String, Map<TimeOfDay, Double>> calculateZonalTemporalCarElectricityConsumptions(final double originZoneEnergyWeight) {

		//initialise hashmap
		HashMap<String, Map<TimeOfDay, Double>> zonalConsumptions = new HashMap<String, Map<TimeOfDay, Double>>();

		for (Trip trip: this.tripList)
			if ((trip.getVehicle() == VehicleType.CAR || trip.getVehicle() == VehicleType.CAR_AV) &&
				(trip.getEngine() == EngineType.BEV || trip.getEngine() == EngineType.PHEV_PETROL || trip.getEngine() == EngineType.PHEV_DIESEL)) {

				Map<EnergyType, Double> tripConsumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), averageAccessEgressSpeedFreight, this.energyConsumptions, this.relativeFuelEfficiencies, this.flagIncludeAccessEgress);
				Double tripConsumptionElectricity = tripConsumption.get(EnergyType.ELECTRICITY);
				if (tripConsumptionElectricity == null) continue; //skip if zero electricity consumption for this trip

				String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
				String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
				TimeOfDay hour = trip.getTimeOfDay();
				int multiplier = trip.getMultiplier();

				Map<TimeOfDay, Double> currentTemporalOrigin = zonalConsumptions.get(originLAD);
				if (currentTemporalOrigin == null) currentTemporalOrigin = new EnumMap<>(TimeOfDay.class);
				zonalConsumptions.put(originLAD, currentTemporalOrigin);

				Map<TimeOfDay, Double> currentTemporalDestination = zonalConsumptions.get(destinationLAD);
				if (currentTemporalDestination == null) currentTemporalDestination = new EnumMap<>(TimeOfDay.class);
				zonalConsumptions.put(destinationLAD, currentTemporalDestination);

				Double currentConsumptionOrigin = currentTemporalOrigin.get(hour);
				if (currentConsumptionOrigin == null) currentConsumptionOrigin = 0.0;

				Double currentConsumptionDestination = currentTemporalDestination.get(hour);
				if (currentConsumptionDestination == null) currentConsumptionDestination = 0.0;

				currentConsumptionOrigin += originZoneEnergyWeight * tripConsumptionElectricity * multiplier;
				currentConsumptionDestination += (1.0 - originZoneEnergyWeight) * tripConsumptionElectricity * multiplier;

				currentTemporalOrigin.put(hour, currentConsumptionOrigin);
				currentTemporalDestination.put(hour, currentConsumptionDestination);
			}

		return zonalConsumptions;
	}

	/**
	 * Calculates the number of electric (BV, PHEV) passenger (car/AV) trips starting in each LAD in each hour.
	 * @return Number of trips.
	 */
	public HashMap<String, Map<TimeOfDay, Integer>> calculateZonalTemporalTripStartsForElectricVehicles() {

		//initialise hashmap
		HashMap<String, Map<TimeOfDay, Integer>> zonalTripStarts = new HashMap<String, Map<TimeOfDay, Integer>>();

		for (Trip trip: this.tripList)
			if ((trip.getVehicle() == VehicleType.CAR || trip.getVehicle() == VehicleType.CAR_AV) &&
				(trip.getEngine() == EngineType.BEV || trip.getEngine() == EngineType.PHEV_PETROL || trip.getEngine() == EngineType.PHEV_DIESEL)) {
				String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
				TimeOfDay hour = trip.getTimeOfDay();
				int multiplier = trip.getMultiplier();	
				
				Map<TimeOfDay, Integer> temporalTripStarts = zonalTripStarts.get(originLAD);
				if (temporalTripStarts == null) temporalTripStarts = new EnumMap<>(TimeOfDay.class);
				zonalTripStarts.put(originLAD, temporalTripStarts);
				
				Integer tripStarts = temporalTripStarts.get(hour);
				if (tripStarts == null) tripStarts = 0;
				tripStarts += multiplier;
				temporalTripStarts.put(hour, tripStarts);
			}

		return zonalTripStarts;
	}
	
	/**
	 * Calculates origin-destination energy consumption for car vehicles for each energy type (in litres/kg for fuels and in kWh for electricity).
	 * @return Zonal consumption for each energy type.
	 */
	public Map<EnergyType, SkimMatrix> calculateODCarEnergyConsumptions() {

		//initialise hashmaps
		Map<EnergyType, SkimMatrix> zonalConsumptions = new EnumMap<>(EnergyType.class);
		for (EnergyType energy: EnergyType.values()) {
			SkimMatrix consumption = new SkimMatrixArray(zoning);
			zonalConsumptions.put(energy, consumption);
		}

		for (Trip trip: this.tripList) {
			if (trip.getVehicle() != VehicleType.CAR && trip.getVehicle() != VehicleType.CAR_AV) continue; //skip freight vehicles

			Map<EnergyType, Double> tripConsumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), averageAccessEgressSpeedFreight, this.energyConsumptions, this.relativeFuelEfficiencies, this.flagIncludeAccessEgress);

			String originLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
			String destinationLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
			int multiplier = trip.getMultiplier();

			for (EnergyType et: EnergyType.values()) {
				double currentConsumption = zonalConsumptions.get(et).getCost(originLAD, destinationLAD);
				currentConsumption += tripConsumption.get(et) * multiplier;
				zonalConsumptions.get(et).setCost(originLAD, destinationLAD, currentConsumption);
			}
		}

		return zonalConsumptions;
	}

	/**
	 * Calculates total energy consumption for each freight vehicle engine type (in litres for fuels and in kWh for electricity).
	 * @return Total consumption for each energy type.
	 */
	public Map<EnergyType, Double> calculateFreightEnergyConsumptions() {

		Map<EnergyType, Double> consumptions = new EnumMap<>(EnergyType.class);
		for (EnergyType energy: EnergyType.values()) {
			consumptions.put(energy, 0.0);
		}

		for (Trip trip: this.tripList) {
			VehicleType vht = trip.getVehicle();
			if ( ! (vht == VehicleType.ARTIC || vht == VehicleType.RIGID || vht == VehicleType.VAN ||
					vht == VehicleType.ARTIC_AV || vht == VehicleType.RIGID_AV || vht == VehicleType.VAN_AV)) continue; //skip non-freight vehicles
			Map<EnergyType, Double> consumption = trip.getConsumption(this.linkTravelTimePerTimeOfDay.get(trip.getTimeOfDay()), this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight(), averageAccessEgressSpeedFreight, this.energyConsumptions, this.relativeFuelEfficiencies, this.flagIncludeAccessEgress);
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
	public Map<EnergyType, Double> calculateEnergyConsumptions() {

		Map<EnergyType, Double> car = calculateCarEnergyConsumptions();
		Map<EnergyType, Double> freight = calculateFreightEnergyConsumptions();
		Map<EnergyType, Double> combined = new EnumMap<>(EnergyType.class);

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

		Map<EnergyType, Double> car = calculateCarEnergyConsumptions();
		double carCO2 = 0.0;
		for (EnergyType energy: EnergyType.values()) {
			carCO2 += car.get(energy) + this.unitCO2Emissions.get(energy);
		}
		totalCO2Emissions.put("PASSENGER", carCO2);

		Map<EnergyType, Double> freight = calculateFreightEnergyConsumptions();
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
	public double[] calculatePeakLinkPointCapacities() {

		double[] peakLinkVolumes = this.calculateLinkVolumeInPCUPerTimeOfDay(this.tripList).get(TimeOfDay.EIGHTAM);
		int[] numberOfLanes = roadNetwork.getNumberOfLanes();
		double[] peakLinkCapacities = new double[this.roadNetwork.getMaximumEdgeID()];

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];
			double linkVol = peakLinkVolumes[edgeID];
			double capacity = 0.0;
			if (edgeType == EdgeType.MOTORWAY) //motorway
				capacity = linkVol / numberOfLanes[edgeID];
			else if (edgeType == EdgeType.AROAD) //A-road
				capacity = linkVol / numberOfLanes[edgeID];
			else //ferry
				capacity = linkVol;

			peakLinkCapacities[edgeID] = capacity;
		}
		return peakLinkCapacities;
	}


	/**
	 * Calculate peak-hour link point capacities (PCU/lane/hr) averaged by two directions.
	 * @return Peak-hour link point capacities.
	 */
	private double[] calculateAveragePeakLinkPointCapacities() {

		double[] capacities = this.calculatePeakLinkPointCapacities();
		double[] averagedCapacities = new double[this.roadNetwork.getMaximumEdgeID()];

		for (int edgeID = 1; edgeID < capacities.length; edgeID++) {
			double capacity1 = capacities[edgeID];
			Integer otherEdgeID = roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
			if (otherEdgeID == null) averagedCapacities[edgeID] = capacity1; //if just one direction, copy value
			else { //otherwise, store average for both directions
				double capacity2 = capacities[otherEdgeID];
				averagedCapacities[edgeID] = (capacity1 + capacity2) / 2.0;
				averagedCapacities[otherEdgeID] = (capacity1 + capacity2) / 2.0;
			}
		}

		return averagedCapacities;
	}

	/**
	 * Calculate peak-hour link point capacities (PCU/lane/hr) maximum of the two directions.
	 * @return Peak-hour link point capacities.
	 */
	private double[] calculateMaximumPeakLinkPointCapacities() {

		double[] capacities = this.calculatePeakLinkPointCapacities();
		double[] maximumCapacities = new double[this.roadNetwork.getMaximumEdgeID()];

		for (int edgeID = 1; edgeID < capacities.length; edgeID++) {
			double capacity1 = capacities[edgeID];
			Integer otherEdgeID = roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
			if (otherEdgeID == null) maximumCapacities[edgeID] = capacity1; //if just one direction, copy value
			else { //otherwise, store maximum of both directions
				double capacity2 = capacities[otherEdgeID];
				double maxCapacity = 0.0;
				if (capacity1 > capacity2) maxCapacity = capacity1;
				else 					   maxCapacity = capacity2;

				maximumCapacities[edgeID] = maxCapacity;
				maximumCapacities[otherEdgeID] = maxCapacity;
			}
		}

		return maximumCapacities;
	}

	/**
	 * Calculate peak-hour link densities (PCU/lane/km/hr).
	 * @return Peak-hour link densities.
	 */
	public double[] calculatePeakLinkDensities() {

		double[] peakLinkVolumes = this.calculateLinkVolumeInPCUPerTimeOfDay(this.tripList).get(TimeOfDay.EIGHTAM);
		int[] numberOfLanes = this.roadNetwork.getNumberOfLanes();
		double[] peakLinkDensities = new double[this.roadNetwork.getMaximumEdgeID()];

		//iterate through all the edges in the graph
		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];
			double linkVol = peakLinkVolumes[edgeID];
			double density = 0.0;
			double length = this.roadNetwork.getEdgeLength(edgeID);
			if (edgeType == EdgeType.MOTORWAY) //motorway
				density = linkVol / numberOfLanes[edgeID] / length;
			else if (edgeType == EdgeType.AROAD) //A-road
				density = linkVol / numberOfLanes[edgeID] / length;
			else //ferry
				density = linkVol / length;

			peakLinkDensities[edgeID] = density;
		}
		
		return peakLinkDensities;
	}

	/**
	 * Calculate peak-hour link capacity utilisation (capacity / max. capacity).
	 * @return Peak-hour link capacity utilisation [%].
	 */
	public double[] calculatePeakLinkCapacityUtilisation() {

		double[] peakLinkCapacities = this.calculatePeakLinkPointCapacities();
		double[] peakLinkCapacityUtilisation = new double[this.roadNetwork.getMaximumEdgeID()];

		//iterate through all the edges in the graph
		Iterator iter = roadNetwork.getNetwork().getEdges().iterator();
		while(iter.hasNext()) {

			Edge edge = (Edge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];
			double capacity = peakLinkCapacities[edgeID];
			double utilisation = 0.0;
			if (edgeType == EdgeType.MOTORWAY) //motorway
				utilisation = capacity / this.maximumCapacityMRoad;
			else if (edgeType == EdgeType.AROAD) //A-road
				utilisation = capacity / this.maximumCapacityARoad;
			else //ferry
				utilisation = 0.0; //undefined for ferry

			peakLinkCapacityUtilisation[edgeID] = utilisation * 100;
		}
		return peakLinkCapacityUtilisation;
	}

	/**
	 * Calculate peak-hour link capacity utilisation (%) averaged by two directions.
	 * @return Peak-hour link capacity utilisation.
	 */
	public double[] calculateDirectionAveragedPeakLinkCapacityUtilisation() {

		double[] utilisation = this.calculatePeakLinkCapacityUtilisation();
		double[] averagedUtilisation = new double[this.roadNetwork.getMaximumEdgeID()];

		for (int edgeID = 1; edgeID < utilisation.length; edgeID++) {
			double utilisation1 = utilisation[edgeID];
			Integer otherEdgeID = roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
			if (otherEdgeID == null) averagedUtilisation[edgeID] = utilisation1; //if just one direction, copy value
			else { //otherwise, store average for both directions
				double utilisation2 = utilisation[otherEdgeID];
				averagedUtilisation[edgeID] = (utilisation1 + utilisation2) / 2.0;
				averagedUtilisation[otherEdgeID] = (utilisation1 + utilisation2) / 2.0;
			}
		}

		return averagedUtilisation;
	}

	/**
	 * @return The copy of all link travel times.
	 */
	public Map<TimeOfDay, double[]> getCopyOfLinkTravelTimes() {

		Map<TimeOfDay, double[]> linkTravelTimes = new EnumMap<>(TimeOfDay.class);
		for (TimeOfDay hour: TimeOfDay.values()) {
			double[] hourlyTimes = new double[this.linkTravelTimePerTimeOfDay.get(hour).length];
			for (int i = 1; i < hourlyTimes.length; i++)
				hourlyTimes[i] = this.linkTravelTimePerTimeOfDay.get(hour)[i];
			linkTravelTimes.put(hour, hourlyTimes);
		}

		return linkTravelTimes;
	}
	
	/**
	 * @return The copy of all link travel times as map.
	 */
	public Map<TimeOfDay, Map<Integer, Double>> getCopyOfLinkTravelTimesAsMap() {

		Map<TimeOfDay, Map<Integer, Double>> linkTravelTimes = new EnumMap<TimeOfDay, Map<Integer, Double>>(TimeOfDay.class);
		for (TimeOfDay hour: TimeOfDay.values()) {
			Map<Integer, Double> hourlyTimes = new HashMap<Integer, Double>();
			for (int i = 1; i < this.linkTravelTimePerTimeOfDay.get(hour).length; i++)
				hourlyTimes.put(i, this.linkTravelTimePerTimeOfDay.get(hour)[i]);
			linkTravelTimes.put(hour, hourlyTimes);
		}

		return linkTravelTimes;
	}

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
	 * Getter method for the volume to flow factor.
	 * @return Volume to flow factor.
	 */
	public double getVolumeToFlowFactor() {

		return this.volumeToFlowFactor;
	}

	/**
	 * Saves assignment results to output file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
	 */
	public void saveAssignmentResults(int year, String outputFile) {

		LOGGER.debug("Saving link-based assignment results.");

		//calculate peak capacities and densities
		double[] capacities = this.calculatePeakLinkPointCapacities();
		double[] averageCapacities = this.calculateAveragePeakLinkPointCapacities();
		double[] maximumCapacities = this.calculateMaximumPeakLinkPointCapacities();
		double[] densities = this.calculatePeakLinkDensities();
		Double[] GEHStats = null;
		Map<VehicleType, Double[]> GEHStatsFreight = null;
		if (year == this.baseYear) {
			GEHStats = this.calculateGEHStatisticForCarCounts(this.volumeToFlowFactor); //GEH can be calculated for base year only
			GEHStatsFreight = this.calculateGEHStatisticForFreightCounts(this.volumeToFlowFactor);
		}

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("edgeID");
		header.add("nodeA");
		header.add("nodeB");
		header.add("roadNumber");
		header.add("length");
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
		header.add("GEHCar");
		header.add("GEHVan");
		header.add("GEHRigid");
		header.add("GEHArtic");
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
				int edgeID = edge.getID();
				EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];
				record.clear();
				record.add(Integer.toString(year));
				record.add(Integer.toString(edgeID));
				record.add(Integer.toString(edge.getNodeA().getID()));
				record.add(Integer.toString(edge.getNodeB().getID()));
				SimpleFeature feature = (SimpleFeature)edge.getObject();
				String roadNumber = (String) feature.getAttribute("RoadNumber");
				record.add(roadNumber);
				record.add(String.format("%.3f", this.roadNetwork.getEdgeLength(edgeID)));
				record.add(String.format("%.4f", this.roadNetwork.getFreeFlowTravelTime()[edgeID]));
				record.add(String.format("%.4f", this.linkTravelTimePerTimeOfDay.get(TimeOfDay.EIGHTAM)[edgeID]));
				int linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.VAN)[edgeID];
				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.RIGID)[edgeID];
				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC)[edgeID];
				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR_AV)[edgeID];
				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.VAN_AV)[edgeID];
				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.RIGID_AV)[edgeID];
				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				linkVolume = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC_AV)[edgeID];
				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString(linkVolume));
				double linkVolumePCU = this.linkVolumesInPCU[edgeID];
				if (linkVolumePCU == 0.0) record.add(Double.toString(0.0));
				else					  record.add(Double.toString(linkVolumePCU));
				record.add(String.format("%.2f", capacities[edgeID]));
				record.add(String.format("%.2f", densities[edgeID]));
				//get max capacity from road type
				if (edgeType == EdgeType.MOTORWAY) { //motorway
					record.add(Integer.toString(maximumCapacityMRoad));
					double utilisation = capacities[edgeID] / maximumCapacityMRoad;
					record.add(String.format("%.4f", utilisation));
					double averageUtilisation = averageCapacities[edgeID] / maximumCapacityMRoad;
					record.add(String.format("%.4f", averageUtilisation));
					double maximumUtilisation = maximumCapacities[edgeID] / maximumCapacityMRoad;
					record.add(String.format("%.4f", maximumUtilisation));
				}	
				else if (edgeType == EdgeType.AROAD) { //A road
					record.add(Integer.toString(maximumCapacityARoad));
					double utilisation = capacities[edgeID] / maximumCapacityARoad;
					record.add(String.format("%.4f", utilisation));
					double averageUtilisation = averageCapacities[edgeID] / maximumCapacityARoad;
					record.add(String.format("%.4f", averageUtilisation));
					double maximumUtilisation = maximumCapacities[edgeID] / maximumCapacityARoad;
					record.add(String.format("%.4f", maximumUtilisation));
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
				if (year == this.baseYear && countPoint != 0) { //only for base year and not a ferry nor a newly developed road with no count point
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
					double gehCar = GEHStats[edgeID];
					record.add(String.format("%.4f", gehCar));
					double gehVan = GEHStatsFreight.get(VehicleType.VAN)[edgeID];
					record.add(String.format("%.4f", gehVan));
					double gehRigid = GEHStatsFreight.get(VehicleType.RIGID)[edgeID];
					record.add(String.format("%.4f", gehRigid));
					double gehArtic = GEHStatsFreight.get(VehicleType.ARTIC)[edgeID];
					record.add(String.format("%.4f", gehArtic));
				}
				else { //future years, ferry or a newly developed road with no count point
					record.add("N/A");
					record.add("N/A");
					record.add("N/A");
					record.add("N/A");
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
		
		LOGGER.debug("Saving hourly car volumes.");

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
				double linkVolume = this.linkVolumesInPCU[edge.getID()];

				if (linkVolume == 0.0) 	record.add(Integer.toString(0));
				else 					record.add(Integer.toString((int)Math.round(linkVolume)));

				for (TimeOfDay hour: TimeOfDay.values()) {
					double linkVolumeInPCU = this.linkVolumesInPCUPerTimeOfDay.get(hour)[edge.getID()]; //TODO there should be one per vehicle type
					if (linkVolumeInPCU == 0.0) record.add(Integer.toString(0));
					else 						record.add(Integer.toString((int)Math.round(linkVolumeInPCU)));
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
		Map<EnergyType, Double> energyConsumptions = this.calculateEnergyConsumptions();

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

		LOGGER.debug("Saving zonal car energy consumptions.");
		
		//calculate energy consumptions
		Map<EnergyType, HashMap<String, Double>> energyConsumptions = this.calculateZonalCarEnergyConsumptions(originZoneEnergyWeight);
		Set<String> zones = energyConsumptions.get(EnergyType.ELECTRICITY).keySet();
		
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("zone");
		for (EnergyType et: EnergyType.values()) header.add(et.name());

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
					EnergyType et = EnergyType.valueOf(header.get(i));
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
	 * Saves zonal (LAD) and temporal (hourly) car electricity consumptions to an output file.
	 * @param year Assignment year.
	 * @param originZoneEnergyWeight Percentage of energy consumption assigned to origin zone (the rest assigned to destination zone).
	 * @param outputFile Output file name (with path).
	 */
	public void saveZonalTemporalCarElectricity(int year, final double originZoneEnergyWeight, String outputFile) {

		LOGGER.debug("Saving zonal and temporal car electricity consumptions.");
		
		//calculate energy consumptions
		HashMap<String, Map<TimeOfDay, Double>> electricityConsumptions = this.calculateZonalTemporalCarElectricityConsumptions(originZoneEnergyWeight);
		Set<String> zones = electricityConsumptions.keySet();
		
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("zone");
		for (TimeOfDay hour: TimeOfDay.values()) header.add(hour.name());

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
					TimeOfDay hour = TimeOfDay.valueOf(header.get(i));
					Double consumption = electricityConsumptions.get(zone).get(hour);
					if (consumption == null) consumption = 0.0;
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
	 * Saves origin-destination matrix of car electricity consumption.
	 * @param outputFile Output file name (with path).
	 */
	public void saveOriginDestinationCarElectricityConsumption(String outputFile) {
		
		LOGGER.debug("Saving OD matrix for car electricity consumption.");
		
		//calculate OD energy consumptions
		Map<EnergyType, SkimMatrix> energyConsumptions = this.calculateODCarEnergyConsumptions();
		
		energyConsumptions.get(EnergyType.ELECTRICITY).saveMatrixFormatted(outputFile);
		
	}
	
	/**
	 * Saves zonal (LAD) and temporal (hourly) number of EV trips to an output file.
	 * @param year Assignment year.
	 * @param outputFile Output file name (with path).
	 */
	public void saveZonalTemporalTripStartsForEVs(int year, String outputFile) {

		LOGGER.debug("Saving number of EV trips starting in each zone in each hour.");
		
		//calculate energy consumptions
		HashMap<String, Map<TimeOfDay, Integer>> zonalTemporalTripStartForEVs = this.calculateZonalTemporalTripStartsForElectricVehicles();
		Set<String> zones = zonalTemporalTripStartForEVs.keySet();
		
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("zone");
		for (TimeOfDay hour: TimeOfDay.values()) header.add(hour.name());

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
					TimeOfDay hour = TimeOfDay.valueOf(header.get(i));
					Integer trips = zonalTemporalTripStartForEVs.get(zone).get(hour);
					if (trips == null) trips = 0;
					record.add(String.format("%d", trips));
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

		Map<String, Map<VehicleType, Double>> vehicleKilometres = this.calculateZonalVehicleKilometresPerVehicleType();

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("zone");
		for (VehicleType vht: VehicleType.values())	header.add(vht.name());

		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (String zone: vehicleKilometres.keySet()) {
				record.clear();
				record.add(Integer.toString(year));
				record.add(zone);
				for (VehicleType vht: VehicleType.values()) {
					Double vhkm = vehicleKilometres.get(zone).get(vht);
					if (vhkm == null) vhkm = 0.0;
					record.add(String.format("%.2f", vhkm));
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
	 * Saves zonal vehicle-kilometres that include access/egress and minor trips
	 * @param year Assignment year.
	 * @param outputFile Output file name (with path).
	 */
	public void saveZonalVehicleKilometresWithAccessEgress(int year, String outputFile) {

		LOGGER.debug("Saving zonal vehicle-kilometres with access and egress.");
		
		final String assignmentType = this.params.getProperty("ASSIGNMENT_TYPE").toLowerCase();

		Map<String, Map<VehicleType, Double>> vehicleKilometres;
		if (assignmentType.equals("tempro") || assignmentType.equals("combined"))
			vehicleKilometres = this.calculateZonalVehicleKilometresPerVehicleTypeFromTemproTripList(true, true);
		else
			vehicleKilometres = this.calculateZonalVehicleKilometresPerVehicleTypeFromTripList(true);
		
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("year");
		header.add("zone");
		for (VehicleType vht: VehicleType.values())	header.add(vht.name());

		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (String zone: vehicleKilometres.keySet()) {
				record.clear();
				record.add(Integer.toString(year));
				record.add(zone);
				for (VehicleType vht: VehicleType.values()) {
					Double vhkm = vehicleKilometres.get(zone).get(vht);
					if (vhkm == null) vhkm = 0.0;
					record.add(String.format("%.2f", vhkm));
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
	 * Saves peak link point capacities into a file.
	 * @param year Year of the assignment.
	 * @param outputFile Output file name (with path).
	 */
	public void savePeakLinkPointCapacities (int year, String outputFile) {

		LOGGER.debug("Saving peak link point capacities.");
		
		//calculate capacities
		double[] capacities = this.calculatePeakLinkPointCapacities();

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
				record.add(Double.toString(capacities[edge.getID()]));
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
				record.add(Double.toString(this.roadNetwork.getFreeFlowTravelTime()[edge.getID()]));
				for (TimeOfDay hour: TimeOfDay.values())
					record.add(String.format("%.4f", this.linkTravelTimePerTimeOfDay.get(hour)[edge.getID()]));
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
	 * Loads link travel times from a file.
	 * @param year Year of the assignment.
	 * @param fileName Input file name (with path).
	 */
	public void loadLinkTravelTimes(int year, String fileName) {

		Map<TimeOfDay, Map<Integer, Double>> linkTravelTime = InputFileReader.readLinkTravelTimeFile(year, fileName);
		
		//store/overwrite into the defaultLinkTravelTime
		for (TimeOfDay hour: TimeOfDay.values()) {
			double[] hourlyTimes = this.linkTravelTimePerTimeOfDay.get(hour);
			for (Integer edgeID: linkTravelTime.get(hour).keySet())
				hourlyTimes[edgeID] = linkTravelTime.get(hour).get(edgeID);
		}
	}

	/**
	 * Getter method for energy unit costs.
	 * @return Energy unit costs.
	 */   
	public Map<EnergyType, Double> getEnergyUnitCosts() {

		return this.energyUnitCosts;
	}

	/**
	 * Getter method for energy consumption WebTAG parameters.
	 * @return Energy consumption parameters.
	 */   
	public Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> getEnergyConsumptionParameters() {

		return this.energyConsumptions;
	}

	/**
	 * Getter method for engine type fractions.
	 * @return Engine type fractions.
	 */   
	public Map<VehicleType, Map<EngineType, Double>> getEngineTypeFractions() {

		return this.engineTypeFractions;
	}

	/**
	 * Getter method for the link free-flow travel times.
	 * @return Link volumes
	 */
	public double[] getLinkFreeFlowTravelTimes() {

		return this.roadNetwork.getFreeFlowTravelTime();
	}

	/**
	 * Getter method for the link travel times per time of day.
	 * @return Link travel times per time of day.
	 */
	public Map<TimeOfDay, double[]> getLinkTravelTimes() {

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
	 * Calculates vehicle kilometres in each LAD and per vehicle type.
	 * Optionally includes access and egress (for LAD-based model).
	 * @param includeAccessEgress True if access and egress should be included in the calculation.
	 * @return Vehicle kilometres.
	 */
	public Map<String, Map<VehicleType, Double>> calculateZonalVehicleKilometresPerVehicleTypeFromTripList(boolean includeAccessEgress) {

		//zone to vehicle type to vehicle kilometres
		Map<String, Map<VehicleType, Double>> vehicleKilometres = new HashMap<String, Map<VehicleType, Double>>();

		for (Trip trip: tripList) {
			int multiplier = trip.getMultiplier();
			VehicleType vht = trip.getVehicle();
			Route route = trip.getRoute();
			if (route == null) continue;
			
			//for each edge of the route add vehkm depending on the zone of the edge
			for (int edgeID: route.getEdges().toArray()) {
				
				//link length
				double length = roadNetwork.getEdgeLength(edgeID);

				//get zone
				String zone = this.roadNetwork.getEdgeToZone().get(edgeID);
				if (zone != null) { //edge to zone mapping exists for this edge
					//fetch current map for the zone
					Map<VehicleType, Double> map = vehicleKilometres.get(zone);
					if (map == null) {
						map = new EnumMap<>(VehicleType.class);
						vehicleKilometres.put(zone, map);
					}

					//fetch current value for the vehicle type
					Double vehkm = vehicleKilometres.get(zone).get(vht);
					if (vehkm == null) vehkm = 0.0;
					//volume of vehicles is equal to the multiplier
					vehkm += multiplier * length;
					
					//store new value
					vehicleKilometres.get(zone).put(vht, vehkm);
				} else { //zone == null
					//check if ferry
					DirectedEdge edge = (DirectedEdge)this.roadNetwork.getEdgeIDtoEdge()[edgeID];
					EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];
					if (edgeType != EdgeType.FERRY) {
						LOGGER.trace("Edge {} is not a ferry edge, but it is not mapped to any zone!", edgeID);

						//we will split vehicle kilometres between two zones
						String zone1 = null, zone2 = null;

						//get zones of edge nodes
						String zoneA = this.roadNetwork.getNodeToZone().get(edge.getNodeA().getID());
						String zoneB = this.roadNetwork.getNodeToZone().get(edge.getNodeB().getID());
						if (zoneA != null && zoneB != null) {
							zone1 = zoneA;
							zone2 = zoneB;
						} else if (zoneA == null && zoneB == null) {
							//use nodes of neighbouring edges
							for (Object o: edge.getNodeA().getEdges()) {
								Edge e = (Edge)o;
								if (e.getID() != edge.getID()) {
									zone1 = this.roadNetwork.getEdgeToZone().get(e.getID());
									break;
								}
							}
							for (Object o: edge.getNodeB().getEdges()) {
								Edge e = (Edge)o;
								if (e.getID() != edge.getID()) {
									zone2 = this.roadNetwork.getEdgeToZone().get(e.getID());
									break;
								}
							}
						}
						LOGGER.trace("Splitting vehicle kilometres for edge {} between zone {} and zone {}.", edge.getID(), zone1, zone2);
						//fetch current map for the zone
						Map<VehicleType, Double> map = vehicleKilometres.get(zone1);
						if (map == null) {
							map = new EnumMap<>(VehicleType.class);
							vehicleKilometres.put(zone1, map);
						}
						//fetch current value
						Double vehkm = vehicleKilometres.get(zone1).get(vht);
						if (vehkm == null) vehkm = 0.0;
						//volume of vehicles is equal to the multiplier
						vehkm += multiplier * length / 2.0;
						//store new value
						vehicleKilometres.get(zone1).put(vht, vehkm);
						
						//fetch current map for the zone
						map = vehicleKilometres.get(zone2);
						if (map == null) {
							map = new EnumMap<>(VehicleType.class);
							vehicleKilometres.put(zone2, map);
						}
						//fetch current value
						vehkm = vehicleKilometres.get(zone2).get(vht);
						if (vehkm == null) vehkm = 0.0;
						//volume of vehicles is equal to the multiplier
						vehkm += multiplier * length / 2.0;
						//store new value
						vehicleKilometres.get(zone2).put(vht, vehkm);
					}//if not ferry
				}//zone is null
			}//edge loop

			if (includeAccessEgress) {

				//get origin node
				Node originNode = trip.getOriginNode();
				if (originNode == null) LOGGER.error("Trip does not have origin node!");
				double access = roadNetwork.getNodeToAverageAccessEgressDistance()[originNode.getID()] / 1000; //km
				String accessZone = roadNetwork.getNodeToZone().get(originNode.getID());
								
				//fetch current map
				Map<VehicleType, Double> map = vehicleKilometres.get(accessZone);
				if (map == null) {
					map = new EnumMap<>(VehicleType.class);
					vehicleKilometres.put(accessZone, map);
				}

				//fetch current value
				Double vehkm = vehicleKilometres.get(accessZone).get(vht);
				if (vehkm == null) vehkm = 0.0;
				vehkm += access * multiplier;

				//store new value
				vehicleKilometres.get(accessZone).put(vht, vehkm);

				Node destinationNode = trip.getDestinationNode();
				if (destinationNode == null) LOGGER.error("Trip does not have destination node!");
				double egress = roadNetwork.getNodeToAverageAccessEgressDistance()[destinationNode.getID()] / 1000; //km
				String egressZone = roadNetwork.getNodeToZone().get(destinationNode.getID());

				//fetch current map
				map = vehicleKilometres.get(egressZone);
				if (map == null) {
					map = new EnumMap<>(VehicleType.class);
					vehicleKilometres.put(egressZone, map);
				}

				//fetch current value
				vehkm = vehicleKilometres.get(egressZone).get(vht);
				if (vehkm == null) vehkm = 0.0;
				vehkm += egress * multiplier;

				//store new value
				vehicleKilometres.get(egressZone).put(vht, vehkm);
			}
		} //trip loop

		return vehicleKilometres;
	}

	/**
	 * Calculates vehicle kilometres in each LAD and for each vehicle type.
	 * Ignores access and egress to major roads. Ignores minor roads.
	 * @return Vehicle kilometres.
	 */
	public Map<String, Map<VehicleType, Double>> calculateZonalVehicleKilometresPerVehicleType() {

		//zone to vehicle type to vkm
		Map<String, Map<VehicleType, Double>> vehicleKilometres = new HashMap<String, Map<VehicleType, Double>>();

		for (VehicleType vht: VehicleType.values()) {
			int[] linkVolumes = this.linkVolumesPerVehicleType.get(vht);
			for (int edgeID = 1; edgeID < linkVolumes.length; edgeID++) {
				
				DirectedEdge edge = (DirectedEdge)this.roadNetwork.getEdgeIDtoEdge()[edgeID];
				if (edge == null) continue; //skip if not pointing to an edge
				
				int volume = linkVolumes[edgeID];
				//get zone
				String zone = this.roadNetwork.getEdgeToZone().get(edgeID);
				if (zone != null) { //edge to zone mapping exists for this edge
					//fetch current map
					Map<VehicleType, Double> map = vehicleKilometres.get(zone);
					if (map == null) {
						map = new EnumMap<>(VehicleType.class);
						vehicleKilometres.put(zone, map);
					}

					//fetch current value
					Double vehkm = vehicleKilometres.get(zone).get(vht);
					if (vehkm == null) vehkm = 0.0;
					//get edge length
					double length = roadNetwork.getEdgeLength(edgeID);
					if (Double.compare(length, 0.0) == 0) LOGGER.warn("0 edge length!");
					vehkm += volume * length;
					//store new value
					vehicleKilometres.get(zone).put(vht, vehkm);
				} else { //zone == null
					//check if ferry
					SimpleFeature sf = (SimpleFeature)edge.getObject();
					String roadNumber = (String) sf.getAttribute("RoadNumber");
					if (roadNumber.charAt(0) != 'F') {
						LOGGER.debug("Edge {} is not a ferry edge, but it is not mapped to any zone!", edgeID);

						//we will split vehicle kilometres between two zones
						String zone1 = null, zone2 = null;

						//get zones of edge nodes
						String zoneA = this.roadNetwork.getNodeToZone().get(edge.getNodeA().getID());
						String zoneB = this.roadNetwork.getNodeToZone().get(edge.getNodeB().getID());
						if (zoneA != null && zoneB != null) {
							zone1 = zoneA;
							zone2 = zoneB;
						} else if (zoneA == null && zoneB == null) {
							//use nodes of neighbouring edges
							for (Object o: edge.getNodeA().getEdges()) {
								Edge e = (Edge)o;
								if (e.getID() != edge.getID()) {
									zone1 = this.roadNetwork.getEdgeToZone().get(e.getID());
									break;
								}
							}
							for (Object o: edge.getNodeB().getEdges()) {
								Edge e = (Edge)o;
								if (e.getID() != edge.getID()) {
									zone2 = this.roadNetwork.getEdgeToZone().get(e.getID());
									break;
								}
							}
						}
						LOGGER.debug("Splitting vehicle kilometres for edge {} between zone {} and zone {}.", edge.getID(), zone1, zone2);
						//fetch current value
						Double vehkm = vehicleKilometres.get(zone1).get(vht);
						if (vehkm == null) vehkm = 0.0;
						//get edge length
						double length = roadNetwork.getEdgeLength(edgeID);
						vehkm += volume * length / 2.0;
						//store new value
						vehicleKilometres.get(zone1).put(vht, vehkm);
						//fetch current value
						vehkm = vehicleKilometres.get(zone2).get(vht);
						if (vehkm == null) vehkm = 0.0;
						//get edge length
						vehkm += volume * length / 2.0;
						//store new value
						vehicleKilometres.get(zone2).put(vht, vehkm);
					}//if not ferry
				}//zone is null
			}//link volume entry
		}//vehicle type

		return vehicleKilometres;
	}

	/**
	 * Calculates vehicle kilometres in each LAD using Tempro-based trips.
	 * Optionally includes access and egress (for Tempro-based model).
	 * Optionally includes minor trips (Tempro intra-zonal).
	 * @param includeAccessEgress True if access and egress should be included in the calculation.
	 * @param includeMinorTrips True if minor trips should be included in the calculation.
	 * @return Vehicle kilometres.
	 */
	public Map<String, Map<VehicleType, Double>> calculateZonalVehicleKilometresPerVehicleTypeFromTemproTripList(boolean includeAccessEgress, boolean includeMinorTrips) {

		//zone to vehicle type to vehicle kilometres
		Map<String, Map<VehicleType, Double>> vehicleKilometres = new HashMap<String, Map<VehicleType, Double>>();

		for (Trip trip: tripList) {
			
			int multiplier = trip.getMultiplier();
			VehicleType vht = trip.getVehicle();
			Route route = trip.getRoute();
			
			if (trip instanceof TripMinor && includeMinorTrips) {
				
				String zone = ((TripMinor)trip).getOriginLAD();
				
				//fetch current map for the zone
				Map<VehicleType, Double> map = vehicleKilometres.get(zone);
				if (map == null) {
					map = new EnumMap<>(VehicleType.class);
					vehicleKilometres.put(zone, map);
				}
				
				//fetch current value for the vehicle type
				Double vehkm = vehicleKilometres.get(zone).get(vht);
				if (vehkm == null) vehkm = 0.0;
				vehkm += ((TripMinor)trip).getLength() * multiplier;

				//store new value
				vehicleKilometres.get(zone).put(vht, vehkm);
			}
		
			if (route == null) continue;
						
			//for each edge of the route add vehkm depending on the zone of the edge
			for (int edgeID: route.getEdges().toArray()) {

				//get zone
				String zone = roadNetwork.getEdgeToZone().get(edgeID);
				//check if ferry
				DirectedEdge edge = (DirectedEdge)this.roadNetwork.getEdgeIDtoEdge()[edgeID];
				EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];
				if (zone == null) {
					if (edgeType != EdgeType.FERRY) LOGGER.debug("Edge {} is not a ferry edge, but it is not mapped to any zone!", edgeID);
					continue; //skipping this edge
				}

				//fetch current map for the zone
				Map<VehicleType, Double> map = vehicleKilometres.get(zone);
				if (map == null) {
					map = new EnumMap<>(VehicleType.class);
					vehicleKilometres.put(zone, map);
				}

				//fetch current value for the vehicle type
				Double vehkm = vehicleKilometres.get(zone).get(vht);
				if (vehkm == null) vehkm = 0.0;
				vehkm += roadNetwork.getEdgeLength(edgeID) * multiplier;

				//store new value
				vehicleKilometres.get(zone).put(vht, vehkm);
			}//edge loop

			if (includeAccessEgress) {

				//get origin 
				Node originNode = trip.getOriginNode();
				if (originNode == null) LOGGER.error("Trip does not have origin node!");

				Double access = 0.0;
				String accessLAD = null;
				if (trip instanceof TripTempro) {

					accessLAD = ((TripTempro)trip).getOriginLAD();
					//String originTemproZone = ((TripTempro)trip).getOriginTemproZone();
					int originTemproZoneID = ((TripTempro)trip).getOrigin();
					/*
					//this is a more general (slower) version where Tempro trip can start from any node (node just the nearest).
					List<Pair<Integer, Double>> accessList = ((TripTempro)trip).getZoning().getZoneToSortedListOfNodeAndDistancePairs().get(originTemproZone);
					for (Pair<Integer, Double> pair: accessList) {
					if (pair.getKey() == originNode.getID()) {
						access = pair.getValue() / 1000;
						break;
					}
					}
					*/
					access = TripTempro.zoning.getZoneIDToNearestNodeDistanceMap()[originTemproZoneID];
					access /= 1000;

				} else { //LAD-based trip

					accessLAD = trip.getOriginLAD(this.roadNetwork.getNodeToZone());
					
					if (vht == VehicleType.CAR || vht == VehicleType.CAR_AV)
						access = this.roadNetwork.getNodeToAverageAccessEgressDistance()[originNode.getID()];
					else //freight vehicle
						access = this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight()[originNode.getID()];
					access /= 1000;
				}
								
				//fetch current map
				Map<VehicleType, Double> map = vehicleKilometres.get(accessLAD);
				if (map == null) {
					map = new EnumMap<>(VehicleType.class);
					vehicleKilometres.put(accessLAD, map);
				}

				//fetch current value
				Double vehkm = vehicleKilometres.get(accessLAD).get(vht);
				if (vehkm == null) vehkm = 0.0;
				vehkm += access * multiplier;

				//store new value
				vehicleKilometres.get(accessLAD).put(vht, vehkm);

				
				//get destination 
				Node destinationNode = trip.getDestinationNode();
				if (destinationNode == null) LOGGER.error("Trip does not have destination node!");

				Double egress = 0.0;
				String egressLAD = null;
				if (trip instanceof TripTempro) {

					egressLAD = ((TripTempro)trip).getDestinationLAD();
					//String destinationTemproZone = ((TripTempro)trip).getDestinationTemproZone();
					int destinationTemproZoneID = ((TripTempro)trip).getDestination();
					/*
					//this is a more general (slower) version where Tempro trip can end in any node (node just the nearest).
					List<Pair<Integer, Double>> egressList = ((TripTempro)trip).getZoning().getZoneToSortedListOfNodeAndDistancePairs().get(destinationTemproZone);
					for (Pair<Integer, Double> pair: egressList) {
					if (pair.getKey() == destinationNode.getID()) {
						egress = pair.getValue() / 1000;
						break;
					}
					}
					*/
					egress = TripTempro.zoning.getZoneIDToNearestNodeDistanceMap()[destinationTemproZoneID];
					egress /= 1000;
					
				} else { //LAD-based trip

					egressLAD = trip.getDestinationLAD(this.roadNetwork.getNodeToZone());
					
					if (vht == VehicleType.CAR || vht == VehicleType.CAR_AV)
						egress = this.roadNetwork.getNodeToAverageAccessEgressDistance()[destinationNode.getID()];
					else //freight vehicle
						egress = this.roadNetwork.getNodeToAverageAccessEgressDistanceFreight()[destinationNode.getID()];
					egress /= 1000;
				}
								
				//fetch current map
				map = vehicleKilometres.get(egressLAD);
				if (map == null) {
					map = new EnumMap<>(VehicleType.class);
					vehicleKilometres.put(egressLAD, map);
				}

				//fetch current value
				vehkm = vehicleKilometres.get(egressLAD).get(vht);
				if (vehkm == null) vehkm = 0.0;
				vehkm += egress * multiplier;

				//store new value
				vehicleKilometres.get(egressLAD).put(vht, vehkm);
			} //access/egress
		} //trip loop

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
	public void setEnergyConsumptionParameters (VehicleType vehicleType, EngineType engineType, Map<WebTAG, Double> parameters) {

		this.energyConsumptions.get(vehicleType).put(engineType, parameters);
	}

	/**
	 * Setter method for energy type fractions.
	 * @param vht Vehicle type
	 * @param engineTypeFractions Map with engine type fractions.
	 */
	public void setEngineTypeFractions (VehicleType vht, Map<EngineType, Double> engineTypeFractions) {

		this.engineTypeFractions.put(vht, engineTypeFractions);
	}

//	/**
//	 * Setter method for fractional assignment.
//	 * @param assignmentFraction Assignment fraction (&lt;= 1.0).
//	 */
//	public void setAssignmentFraction (double assignmentFraction) {
//
//		this.assignmentFraction = assignmentFraction;
//	}

	/**
	 * Resets link volumes to zero.
	 */
	public void resetLinkVolumes () {

		//reset link volumes in PCU
		if (this.linkVolumesInPCU == null)
			this.linkVolumesInPCU = new double[this.roadNetwork.maximumEdgeID];
		for (int edgeID = 1; edgeID < this.linkVolumesInPCU.length; edgeID++)
			this.linkVolumesInPCU[edgeID] = 0.0;

		//reset link volumes per vehicle type
		if (this.linkVolumesPerVehicleType == null)
			this.linkVolumesPerVehicleType = new EnumMap<VehicleType, int[]>(VehicleType.class);
		for (VehicleType vht: VehicleType.values()) {
			int[] linkVolumes = this.linkVolumesPerVehicleType.get(vht);
			if (linkVolumes == null) {
				linkVolumes = new int[this.roadNetwork.getMaximumEdgeID()];
				this.linkVolumesPerVehicleType.put(vht, linkVolumes);
			}
			for (int edgeID = 1; edgeID < linkVolumes.length; edgeID++)
				linkVolumes[edgeID] = 0;
		}

		//reset link volumes per time of day
		if (this.linkVolumesInPCUPerTimeOfDay == null) 
			this.linkVolumesInPCUPerTimeOfDay = new EnumMap<TimeOfDay, double[]>(TimeOfDay.class);
		for (TimeOfDay hour: TimeOfDay.values()) {
			double[] hourlyVolumes = this.linkVolumesInPCUPerTimeOfDay.get(hour);
			if (hourlyVolumes == null) {
				hourlyVolumes = new double[this.roadNetwork.getMaximumEdgeID()];
				this.linkVolumesInPCUPerTimeOfDay.put(hour, hourlyVolumes);
			}
			for (int edgeID = 1; edgeID < hourlyVolumes.length; edgeID++)
				hourlyVolumes[edgeID] = 0.0;
		}
	}

	/**
	 * Reset trip list for passengers and freight.
	 */
	public void resetTripList () {

		this.tripList.clear();
		//this.tripList = new ArrayList<Trip>();
	}
	
	/**
	 * Initialise trip list for passengers and freight (e.g. expected total sum or passenger and freight flows).
	 * @param initialCapacity Initial capacity of the trip list.
	 */
	public void initialiseTripList (int initialCapacity) {

		this.tripList = new ArrayList<Trip>(initialCapacity);
	}

	/**
	 * Calculates link volumes in PCU per time of day.
	 * @param tripList Trip list.
	 * @return Link volumes in PCU per time of day
	 */
	public Map<TimeOfDay, double[]> calculateLinkVolumeInPCUPerTimeOfDay(List<Trip> tripList) {

		//Map<Object, Long> collect = tripList.stream().collect(Collectors.groupingBy(t -> t.getTimeOfDay(), Collectors.counting()));

		//Long countOfPetrolTrips2 = tripList.parallelStream().filter(t -> t.getEngine() == EngineType.PETROL).count();

		Map<TimeOfDay, double[]> map = new EnumMap<TimeOfDay, double[]>(TimeOfDay.class);
		for (TimeOfDay hour: TimeOfDay.values()) {
			double[] hourlyVolumes = new double[this.roadNetwork.getMaximumEdgeID()];
			map.put(hour, hourlyVolumes);
		}

		for (Trip trip: tripList) {
			Route route = trip.getRoute();
			if (route == null) continue;
			int multiplier = trip.getMultiplier();
			double[] hourlyVolumes = map.get(trip.getTimeOfDay());
			for (int edgeID: route.getEdges().toArray()) {
				hourlyVolumes[edgeID] += this.vehicleTypeToPCU.get(trip.getVehicle()) * multiplier; //add PCU of the vehicle
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
	public Map<TimeOfDay, double[]> getLinkVolumeInPCUPerTimeOfDay() {

		return this.linkVolumesInPCUPerTimeOfDay;
	}

	/**
	 * Calculates daily link volumes in PCU.
	 * @param tripList Trip list.
	 * @return Map of link volumes in PCU.
	 */
	public double[] calculateLinkVolumeInPCU(List<Trip> tripList) {

		double[] volumes = new double[this.roadNetwork.getMaximumEdgeID()];

		for (Trip trip: tripList) {
			Route route = trip.getRoute();
			if (route == null) continue;
			int multiplier = trip.getMultiplier();
			for (int edgeID: route.getEdges().toArray()) {
				volumes[edgeID] += this.vehicleTypeToPCU.get(trip.getVehicle()) * multiplier; //add PCU of the vehicle
			}
		}

		return volumes;
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
	public double[] getLinkVolumeInPCU() {

		return this.linkVolumesInPCU;
	}

	/**
	 * Calculates daily link volumes per vehicle type.
	 * @param tripList Trip list.
	 * @return Map of link volumes per vehicle type.
	 */
	public Map<VehicleType, int[]> calculateLinkVolumePerVehicleType(List<Trip> tripList) {

		Map<VehicleType, int[]> map = new EnumMap<VehicleType, int[]>(VehicleType.class);
		for (VehicleType vht: VehicleType.values()) {
			int[] vehicleVolumes = new int[this.roadNetwork.getMaximumEdgeID()];
			map.put(vht, vehicleVolumes);
		}

		for (Trip trip: tripList) {
			Route route = trip.getRoute();
			if (route == null) continue;
			int multiplier = trip.getMultiplier();
			int[] vehicleVolumes = map.get(trip.getVehicle());
			for (int edgeID: trip.getRoute().getEdges().toArray()) {
				vehicleVolumes[edgeID] += multiplier;
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
	public Map<VehicleType, int[]> getLinkVolumePerVehicleType() {

		return this.linkVolumesPerVehicleType;
	}

	/**
	 * Getter method for AADF car traffic counts.
	 * @return Car traffic counts.
	 */
	public Integer[] getAADFCarTrafficCounts() {

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
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];

			if (edgeType == EdgeType.FERRY) continue; //ferry
			
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			Long countPoint = (long) sf.getAttribute("CP");
			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long carCount = (long) sf.getAttribute("FdCar");
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
				
				absoluteDifferences.put(edgeID, (int) Math.abs(carCount - carVolume));
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined count
				long carCount = (long) sf.getAttribute("FdCar");

				//get volumes for this direction
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
		
				//get volumes for other direction (if exists)
				int edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
				long carVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edge2];
			
				long absoluteDifference = Math.abs(carCount - carVolume - carVolume2) / 2;
				checkedCP.add(countPoint);
				absoluteDifferences.put(edgeID, (int) absoluteDifference);
				if (edge2 != 0) absoluteDifferences.put(edge2, (int) absoluteDifference); //store in other direction too
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
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edge1];
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
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];

			if (edgeType == EdgeType.FERRY) continue; //ferry

			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			Long countPoint = (long) sf.getAttribute("CP");

			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long carCount = (long) sf.getAttribute("FdCar");
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
			
				differences.put(edgeID, (int) (carCount - carVolume));
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined count
				long carCount = (long) sf.getAttribute("FdCar");

				//get volumes for this direction
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
			
				//get volumes for other direction (if exists)
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
				long carVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edge2];

				long difference = (carCount - carVolume - carVolume2) / 2;
				checkedCP.add(countPoint);
				differences.put(edgeID, (int) difference);
				if (edge2 != null) differences.put(edge2, (int) difference); //store in other direction too
			}
		}

		return differences;
	}


	/**
	 * Calculates GEH statistic for simulated and observed hourly car flows.
	 * For combined counts, combines the volumes on two road directions.
	 * Two obtain hourly flows, multiplies daily link volumes (and traffic counts) with volumeToFlowFactor.
	 * The formula is taken from WebTAG Unit M3.1.
	 * @param volumeToFlowFactor Converts daily vehicle volume to hourly flow (e.g. 0.1 for peak flow; 1/24.0 for daily average)  
	 * @return GEH statistic for simulated and observed hourly car flows.
	 */
	public Double[] calculateGEHStatisticForCarCounts (double volumeToFlowFactor) {

		Double[] GEH = new Double[this.roadNetwork.getMaximumEdgeID()]; //null used when GEH is undefined, e.g. for ferry edges

		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
		ArrayList<Long> checkedCP = new ArrayList<Long>(); 

		while (iter.hasNext()) {
			DirectedEdge edge = (DirectedEdge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];

			if (edgeType == EdgeType.FERRY) continue; //ferry

			SimpleFeature sf = (SimpleFeature) edge.getObject();
			Long countPoint = (long) sf.getAttribute("CP");
			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long carCount = (long) sf.getAttribute("FdCar");
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
		
				double carFlowSimulated = carVolume * volumeToFlowFactor;
				double carFlowObserved = carCount * volumeToFlowFactor;
				double geh = Math.abs(carFlowSimulated - carFlowObserved) / Math.sqrt((carFlowSimulated + carFlowObserved) / 2.0);

				GEH[edgeID] = geh;
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined count
				long carCount = (long) sf.getAttribute("FdCar");

				//get volumes for this direction
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];

				//get volumes for other direction (if exists)
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
				long carVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edge2];

				checkedCP.add(countPoint);

				double carFlowSimulated = (carVolume + carVolume2) * volumeToFlowFactor;
				double carFlowObserved = carCount * volumeToFlowFactor;
				double geh = Math.abs(carFlowSimulated - carFlowObserved) / Math.sqrt((carFlowSimulated + carFlowObserved) / 2.0);

				GEH[edgeID] = geh;
				if (edge2 != null) GEH[edge2] = geh; //store in other direction too
			}
		}

		return GEH;
	}
	
	
	/**
	 * Prints GEH statistics for comparison between simulated and observed hourly car flows.
	 */
	public void printGEHstatistic() {
		
		this.printGEHstatistic(this.volumeToFlowFactor);
	}

	/**
	 * Prints GEH statistics for comparison between simulated and observed hourly car flows.
	 * @param volumeToFlowFactor Converts daily vehicle volume to hourly flow (e.g. 0.1 for peak flow; 1/24.0 for daily average)  
	 */
	public void printGEHstatistic(double volumeToFlowFactor) {

		Double[] GEH = this.calculateGEHStatisticForCarCounts(volumeToFlowFactor);

		int counter = 0;
		int validFlows = 0;
		int suspiciousFlows = 0;
		int invalidFlows = 0;
		for (int edgeID = 1; edgeID < GEH.length; edgeID++) {
			if (GEH[edgeID] == null) continue; //skip null values
			if (GEH[edgeID] < 5.0) validFlows++;
			else if (GEH[edgeID] < 10.0) suspiciousFlows++;
			else invalidFlows++;
			counter++;
		}
		LOGGER.info("Percentage of edges with valid flows (GEH < 5.0) is: {}%", Math.round((double) validFlows / counter * 100));
		LOGGER.info("Percentage of edges with suspicious flows (5.0 <= GEH < 10.0) is: {}%", Math.round((double) suspiciousFlows / counter * 100));
		LOGGER.info("Percentage of edges with invalid flows (GEH >= 10.0) is: {}%", Math.round((double) invalidFlows / counter * 100));		
	}
	
	/**
	 * Prints GEH statistics for comparison between simulated and observed hourly freight vehicle flows.
	 */
	public void printGEHstatisticFreight() {
		
		this.printGEHstatisticFreight(this.volumeToFlowFactor);
	}
	
	/**
	 * Prints GEH statistics for comparison between simulated and observed hourly freight vehicle flows.
	 * @param volumeToFlowFactor Converts daily vehicle volume to hourly flow (e.g. 0.1 for peak flow; 1/24.0 for daily average)  
	 */
	public void printGEHstatisticFreight(double volumeToFlowFactor) {
		
		Map<VehicleType, Double[]> freightGEH = this.calculateGEHStatisticForFreightCounts(volumeToFlowFactor);
		
		Double[] GEH = freightGEH.get(VehicleType.VAN);
		int counter = 0;
		int validFlows = 0;
		int suspiciousFlows = 0;
		int invalidFlows = 0;
		for (int edgeID = 1; edgeID < GEH.length; edgeID++) {
			if (GEH[edgeID] == null) continue;
			if (GEH[edgeID] < 5.0) validFlows++;
			else if (GEH[edgeID] < 10.0) suspiciousFlows++;
			else invalidFlows++;
			counter++;
		}
		LOGGER.info("Percentage of edges with valid van flows (GEH < 5.0) is: {}%", Math.round((double) validFlows / counter * 100));
		LOGGER.info("Percentage of edges with suspicious van flows (5.0 <= GEH < 10.0) is: {}%", Math.round((double) suspiciousFlows / counter * 100));
		LOGGER.info("Percentage of edges with invalid van flows (GEH >= 10.0) is: {}%", Math.round((double) invalidFlows / counter * 100));
		
		GEH = freightGEH.get(VehicleType.RIGID);
		counter = 0;
		validFlows = 0;
		suspiciousFlows = 0;
		invalidFlows = 0;
		for (int edgeID = 1; edgeID < GEH.length; edgeID++) {
			if (GEH[edgeID] == null) continue;
			if (GEH[edgeID] < 5.0) validFlows++;
			else if (GEH[edgeID] < 10.0) suspiciousFlows++;
			else invalidFlows++;
			counter++;
		}
		LOGGER.info("Percentage of edges with valid rigid flows (GEH < 5.0) is: {}%", Math.round((double) validFlows / counter * 100));
		LOGGER.info("Percentage of edges with suspicious rigid flows (5.0 <= GEH < 10.0) is: {}%", Math.round((double) suspiciousFlows / counter * 100));
		LOGGER.info("Percentage of edges with invalid rigid flows (GEH >= 10.0) is: {}%", Math.round((double) invalidFlows / counter * 100));	
		
		GEH = freightGEH.get(VehicleType.ARTIC);
		counter = 0;
		validFlows = 0;
		suspiciousFlows = 0;
		invalidFlows = 0;
		for (int edgeID = 1; edgeID < GEH.length; edgeID++) {
			if (GEH[edgeID] == null) continue;
			if (GEH[edgeID] < 5.0) validFlows++;
			else if (GEH[edgeID] < 10.0) suspiciousFlows++;
			else invalidFlows++;
			counter++;
		}
		LOGGER.info("Percentage of edges with valid artic flows (GEH < 5.0) is: {}%", Math.round((double) validFlows / counter * 100));
		LOGGER.info("Percentage of edges with suspicious artic flows (5.0 <= GEH < 10.0) is: {}%", Math.round((double) suspiciousFlows / counter * 100));
		LOGGER.info("Percentage of edges with invalid artic flows (GEH >= 10.0) is: {}%", Math.round((double) invalidFlows / counter * 100));	
	}
	
	/**
	 * Calculates GEH statistic for simulated and observed hourly flow.
	 * It uses linkVolumesInPCUPerTimeOfDay, so make sure only car flows have been assigned. 
	 * For combined counts, takes the average of the two differences.
	 * The formula is taken from WebTAG Unit M3.1.
	 * @param hour Hour for which to calculate GEH statistics.
	 * @return GEH statistic for simulated and observed hourly car flows.
	 */
	public Double[] calculateGEHStatisticPerTimeOfDay (TimeOfDay hour) {
		
		Double[] GEH = new Double[this.roadNetwork.getMaximumEdgeID()];

		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
		ArrayList<Long> checkedCP = new ArrayList<Long>(); 

		while (iter.hasNext()) {
			DirectedEdge edge = (DirectedEdge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];

			if (edgeType == EdgeType.FERRY) continue; //ferry

			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			Long countPoint = (long) sf.getAttribute("CP");
			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long carCount = (long) sf.getAttribute("FdCar");
				double carVolume = this.linkVolumesInPCUPerTimeOfDay.get(hour)[edgeID];
			
				double carFlowSimulated = carVolume;
				double carFlowObserved = carCount * this.timeOfDayDistribution.get(hour);
				double geh = Math.abs(carFlowSimulated - carFlowObserved) / Math.sqrt((carFlowSimulated + carFlowObserved) / 2.0);

				GEH[edgeID] = geh;
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined count
				long carCount = (long) sf.getAttribute("FdCar");

				//get volumes for this direction
				double carVolume = this.linkVolumesInPCUPerTimeOfDay.get(hour)[edgeID];
	
				//get volumes for other direction (if exists)
				Integer edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
				double carVolume2 = this.linkVolumesInPCUPerTimeOfDay.get(hour)[edge2];
	
				checkedCP.add(countPoint);

				double carFlowSimulated = carVolume + carVolume2;
				double carFlowObserved = carCount * this.timeOfDayDistribution.get(hour);
				double geh = Math.abs(carFlowSimulated - carFlowObserved) / Math.sqrt((carFlowSimulated + carFlowObserved) / 2.0);

				GEH[edgeID] = geh;
				if (edge2 != null) GEH[edge2] = geh; //store in other direction too
			}
		}

		return GEH;
	}
		
	/**
	 * Prints GEH statistics for comparison between simulated and observed hourly car flows.
	 */
	public void printHourlyGEHstatistic() {
		
		Map<TimeOfDay, Triple<Integer, Integer, Integer>> hourlyGEHTriples = new EnumMap<>(TimeOfDay.class);

		for (TimeOfDay hour: TimeOfDay.values()) {
			
			Double[] GEH = this.calculateGEHStatisticPerTimeOfDay(hour);
			int counter = 0;
			int validFlows = 0;
			int suspiciousFlows = 0;
			int invalidFlows = 0;
			for (int edgeID = 1; edgeID < GEH.length; edgeID++) {
				if (GEH[edgeID] == null) continue;
				if (GEH[edgeID] < 5.0) validFlows++;
				else if (GEH[edgeID] < 10.0) suspiciousFlows++;
				else invalidFlows++;
				counter++;
			}
			
			double valid = (double) validFlows / counter * 100;
			double suspicious = (double) suspiciousFlows / counter * 100;
			double invalid = (double) invalidFlows / counter * 100;
			
			/*
			LOGGER.info("GEH statistic for hour: {}", hour);
			LOGGER.info("Percentage of edges with valid flows (GEH < 5.0) is: {}%", Math.round(valid));
			LOGGER.info("Percentage of edges with suspicious flows (5.0 <= GEH < 10.0) is: {}%", Math.round(suspicious));
			LOGGER.info("Percentage of edges with invalid flows (GEH >= 10.0) is: {}%", Math.round(invalid));
			*/
			
			Triple<Integer, Integer, Integer> t = Triple.of((int)Math.round(valid), (int)Math.round(suspicious), (int)Math.round(invalid));
			hourlyGEHTriples.put(hour, t);
		}
		LOGGER.info("GEH statistic per hour (valid %, suspicious %, invalid %):");
		LOGGER.info(hourlyGEHTriples.toString());
	}
	
	/**
	 * Prints RMSN statistic for comparison between simulated daily car volumes and observed daily traffic counts.
	 */
	public void printRMSNstatistic() {

		double RMSN = this.calculateRMSNforSimulatedVolumes();
		LOGGER.info("RMSN for car traffic counts is: {}%", Math.round(RMSN));
	}
	
	/**
	 * Prints RMSN statistic for comparison between simulated daily freight volumes and observed daily freight traffic counts.
	 */
	public void printRMSNstatisticFreight() {

		double RMSN = this.calculateRMSNforFreightCounts();
		LOGGER.info("RMSN for freight traffic counts is: {}%", Math.round(RMSN));
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
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];

			if (edgeType == EdgeType.FERRY) continue; //ferry

			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			Long countPoint = (long) sf.getAttribute("CP");
			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long carCount = (long) sf.getAttribute("FdCar");
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
	
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
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
		
				//expand simulated volumes with the expansion factor and round
				carVolume = Math.round(carVolume * expansionFactor);

				//get volumes for other direction (if exists)
				int edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
				long carVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edge2];
			
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
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];

			if (edgeType == EdgeType.FERRY) continue; //ferry

			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			Long countPoint = (long) sf.getAttribute("CP");
			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long vanCount = (long) sf.getAttribute("FdLGV");
				long rigidCount = (long) sf.getAttribute("FdHGVR2") + (long) sf.getAttribute("FdHGVR3") + (long) sf.getAttribute("FdHGVR4");
				long articCount = (long) sf.getAttribute("FdHGVA3") + (long) sf.getAttribute("FdHGVA5") + (long) sf.getAttribute("FdHGVA6");

				long vanVolume = this.linkVolumesPerVehicleType.get(VehicleType.VAN)[edgeID];
				long rigidVolume = this.linkVolumesPerVehicleType.get(VehicleType.RIGID)[edgeID];
				long articVolume = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC)[edgeID];

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
				long vanVolume = this.linkVolumesPerVehicleType.get(VehicleType.VAN)[edgeID];
				long rigidVolume = this.linkVolumesPerVehicleType.get(VehicleType.RIGID)[edgeID];
				long articVolume = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC)[edgeID];

				//get volumes for other direction (if exists)
				int edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];

				long vanVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.VAN)[edge2];
				long rigidVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.RIGID)[edge2];
				long articVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC)[edge2];
	
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
	 * Calculates GEH statistic for simulated and observed hourly freight vehicle flows.
	 * For combined counts, combines the volumes on two road directions.
	 * Two obtain hourly flows, multiplies daily link volumes (and traffic counts) with volumeToFlowFactor.
	 * The formula is taken from WebTAG Unit M3.1.
	 * @param volumeToFlowFactor Converts daily vehicle volume to hourly flow (e.g. 0.1 for peak flow; 1/24.0 for daily average)  
	 * @return GEH statistic for simulated and observed hourly freight vehicle flows, per vehicle type.
	 */
	public Map<VehicleType, Double[]> calculateGEHStatisticForFreightCounts (double volumeToFlowFactor) {

		Map<VehicleType, Double[]> GEH = new EnumMap<VehicleType, Double[]>(VehicleType.class);
		Double[] vanGEH = new Double[this.roadNetwork.getMaximumEdgeID()];
		Double[] rigidGEH = new Double[this.roadNetwork.getMaximumEdgeID()];
		Double[] articGEH = new Double[this.roadNetwork.getMaximumEdgeID()];
		GEH.put(VehicleType.VAN, vanGEH);
		GEH.put(VehicleType.RIGID, rigidGEH);
		GEH.put(VehicleType.ARTIC, articGEH);
		
		Iterator iter = this.roadNetwork.getNetwork().getEdges().iterator();
		ArrayList<Long> checkedCP = new ArrayList<Long>(); 

		while (iter.hasNext()) {
			DirectedEdge edge = (DirectedEdge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];

			if (edgeType == EdgeType.FERRY) continue; //ferry

			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			Long countPoint = (long) sf.getAttribute("CP");
			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long vanCount = (long) sf.getAttribute("FdLGV");
				long rigidCount = (long) sf.getAttribute("FdHGVR2") + (long) sf.getAttribute("FdHGVR3") + (long) sf.getAttribute("FdHGVR4");
				long articCount = (long) sf.getAttribute("FdHGVA3") + (long) sf.getAttribute("FdHGVA5") + (long) sf.getAttribute("FdHGVA6");

				long vanVolume = this.linkVolumesPerVehicleType.get(VehicleType.VAN)[edgeID];
				long rigidVolume = this.linkVolumesPerVehicleType.get(VehicleType.RIGID)[edgeID];
				long articVolume = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC)[edgeID];

				double vanFlowSimulated = vanVolume * volumeToFlowFactor;
				double vanFlowObserved = vanCount * volumeToFlowFactor;
				double geh = Math.abs(vanFlowSimulated - vanFlowObserved) / Math.sqrt((vanFlowSimulated + vanFlowObserved) / 2.0);
				vanGEH[edgeID] = geh;
				
				double rigidFlowSimulated = rigidVolume * volumeToFlowFactor;
				double rigidFlowObserved = rigidCount * volumeToFlowFactor;
				geh = Math.abs(rigidFlowSimulated - rigidFlowObserved) / Math.sqrt((rigidFlowSimulated + rigidFlowObserved) / 2.0);
				rigidGEH[edgeID] = geh;
				
				double articFlowSimulated = articVolume * volumeToFlowFactor;
				double articFlowObserved = articCount * volumeToFlowFactor;
				geh = Math.abs(articFlowSimulated - articFlowObserved) / Math.sqrt((articFlowSimulated + articFlowObserved) / 2.0);
				articGEH[edgeID] = geh;
			}

			if (dir == 'C' && !checkedCP.contains(countPoint)) { //for combined counts check if this countPoint has been processed already

				//get combined counts
				long vanCount = (long) sf.getAttribute("FdLGV");
				long rigidCount = (long) sf.getAttribute("FdHGVR2") + (long) sf.getAttribute("FdHGVR3") + (long) sf.getAttribute("FdHGVR4");
				long articCount = (long) sf.getAttribute("FdHGVA3") + (long) sf.getAttribute("FdHGVA5") + (long) sf.getAttribute("FdHGVA6");

				//get volumes for this direction
				long vanVolume = this.linkVolumesPerVehicleType.get(VehicleType.VAN)[edgeID];
				long rigidVolume = this.linkVolumesPerVehicleType.get(VehicleType.RIGID)[edgeID];
				long articVolume = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC)[edgeID];
	
				//get volumes for other direction (if exists)
				int edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
				long vanVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.VAN)[edge2];
				long rigidVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.RIGID)[edge2];
				long articVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.ARTIC)[edge2];
				
				checkedCP.add(countPoint);

				double vanFlowSimulated = (vanVolume + vanVolume2) * volumeToFlowFactor;
				double vanFlowObserved = vanCount * volumeToFlowFactor;
				double geh = Math.abs(vanFlowSimulated - vanFlowObserved) / Math.sqrt((vanFlowSimulated + vanFlowObserved) / 2.0);
				vanGEH[edgeID] = geh;
				if (edge2 != 0) vanGEH[edge2] = geh; //store in other direction too
				
				double rigidFlowSimulated = (rigidVolume + rigidVolume2) * volumeToFlowFactor;
				double rigidFlowObserved = rigidCount * volumeToFlowFactor;
				geh = Math.abs(rigidFlowSimulated - rigidFlowObserved) / Math.sqrt((rigidFlowSimulated + rigidFlowObserved) / 2.0);
				rigidGEH[edgeID] = geh;
				if (edge2 != 0) rigidGEH[edge2] = geh; //store in other direction too
				
				double articFlowSimulated = (articVolume + articVolume2) * volumeToFlowFactor;
				double articFlowObserved = articCount * volumeToFlowFactor;
				geh = Math.abs(articFlowSimulated - articFlowObserved) / Math.sqrt((articFlowSimulated + articFlowObserved) / 2.0);
				articGEH[edgeID] = geh;
				if (edge2 != 0) articGEH[edge2] = geh; //store in other direction too
			}
		}

		return GEH;
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
			int edgeID = edge.getID();
			EdgeType edgeType = this.roadNetwork.getEdgesType()[edgeID];

			if (edgeType == EdgeType.FERRY) continue; //ferry

			Long countPoint = (long) sf.getAttribute("CP");

			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			//ignore combined counts 'C' for now
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E') {

				long carCount = (long) sf.getAttribute("FdCar");
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
	
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
				long carVolume = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edgeID];
		
				//expand simulated volumes with the expansion factor and round
				carVolume = Math.round(carVolume * expansionFactor);

				//get volumes for other direction (if exists)
				int edge2 = this.roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID];
				long carVolume2 = this.linkVolumesPerVehicleType.get(VehicleType.CAR)[edge2];
	
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
	
	/**
	 * Choose time of day.
	 * @param timOfDayDistribution Time of day distribution.
	 * @return Chosen time of day (hour).
	 */
	private TimeOfDay chooseTimeOfDay(Map<TimeOfDay, Double> timOfDayDistribution) {
		
		//choose time of day
		double cumulativeProbability = 0.0;
		double random = rng.nextDouble();
		TimeOfDay hour = null;
		for (Map.Entry<TimeOfDay, Double> entry : timOfDayDistribution.entrySet()) {
			TimeOfDay key = entry.getKey();
			Double value = entry.getValue();	
			cumulativeProbability += value;
			if (Double.compare(cumulativeProbability, random) > 0) {
				hour = key;
				break;
			}
		}
		
		return hour;
	}
	
	/**
	 * Choose car vehicle type (autonomous or non-autonomous).
	 * @param AVFractions Fraction of autonomous cars.
	 * @return Chosen car vehicle type.
	 */
	private VehicleType chooseCarVehicleType(Map<VehicleType, Double> AVFractions) {
		
		//choose vehicle type (autonomous or non-autonomous)
		double random  = rng.nextDouble();
		VehicleType vht = null;
		if (Double.compare(1.0 - AVFractions.get(VehicleType.CAR_AV), random) > 0)
			vht = VehicleType.CAR;
		else 
			vht = VehicleType.CAR_AV;
		
		return vht;
	}
	
	/**
	 * Choose freight vehicle type (autonomous or non-autonomous).
	 * @param fvht Freight vehicle type (non-autonomous).
	 * @param AVFractions Fraction of autonomous freight vehicle types.
	 * @return Chosen freight vehicle type (autonomous or non-autonomous).
	 */
	private VehicleType chooseFreightVehicleType(VehicleType fvht, Map<VehicleType, Double> AVFractions) {
		
		//find autonomous counterpart
		VehicleType avht = null;
		if (fvht == VehicleType.VAN) 			avht = VehicleType.VAN_AV;
		else if (fvht == VehicleType.RIGID) 	avht = VehicleType.RIGID_AV;
		else if (fvht == VehicleType.ARTIC) 	avht = VehicleType.ARTIC_AV;

		//choose vehicle type (autonomous or non-autonomous)
		double random  = rng.nextDouble();
		VehicleType vht = null;
		if (Double.compare(AVFractions.get(avht), random) > 0) 
			vht = avht; //autonomous version
		else
			vht = fvht; //non-autonomous version
		
		return vht;
	}
		
	/**
	 * Choose engine type.
	 * @param engineTypeDistribution Engine type distribution (for a given vehicle type).
	 * @return Chosen engine type.
	 */
	private EngineType chooseEngineType(Map<EngineType, Double> engineTypeDistribution) {

		//choose engine type
		double cumulativeProbability = 0.0;
		double random = rng.nextDouble();
		EngineType engine = null;
		for (Map.Entry<EngineType, Double> entry : engineTypeDistribution.entrySet()) {
			EngineType key = entry.getKey();
			Double value = entry.getValue();	
			cumulativeProbability += value;
			if (Double.compare(cumulativeProbability, random) > 0) {
				engine = key;
				break;
			}
		}
		return engine;
	}
	
	/**
	 * Chooses the route using route choice model.
	 * @param originNode
	 * @param destinationNode
	 * @param vht
	 * @param engine
	 * @param hour
	 * @param rsg
	 * @param routeChoiceParameters
	 * @return
	 */
	private Route chooseRoute(int originNode, int destinationNode, VehicleType vht, EngineType engine, TimeOfDay hour, RouteSetGenerator rsg, Map<RouteChoiceParams, Double> routeChoiceParameters) {


		Route chosenRoute = null;
		RouteSet fetchedRouteSet = rsg.getRouteSet(originNode, destinationNode);
		if (fetchedRouteSet == null) {
			LOGGER.warn("Can't fetch the route set between nodes {} and {}!", originNode, destinationNode);

			if (!flagAStarIfEmptyRouteSet)	return null;
			else { //try finding a path with aStar
				LOGGER.debug("Trying the astar!");

				DirectedNode directedOriginNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[originNode];
				DirectedNode directedDestinationNode = (DirectedNode) this.roadNetwork.getNodeIDtoNode()[destinationNode];

				//RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTime);
				RoadPath fastestPath = this.roadNetwork.getFastestPath(directedOriginNode, directedDestinationNode, this.linkTravelTimePerTimeOfDay.get(hour));
				if (fastestPath == null) {
					LOGGER.warn("Not even aStar could find a route between node {} and node {}!", originNode, destinationNode);
					return null;
				}
				chosenRoute = new Route(fastestPath, roadNetwork);
				if (chosenRoute.isEmpty()) {
					LOGGER.warn("Empty route between nodes {} and {}!", originNode, destinationNode);
					return null;
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

				//fetch congestion charge for the vehicle type
				//HashMap<String, HashMap<Integer, Double>> linkCharges = null;
				HashMap<String, HashMap<Integer, Double>> linkCharges = new HashMap<String, HashMap<Integer, Double>>();
				if (this.congestionCharges != null) 
					for (String policyName: this.congestionCharges.keySet())
						linkCharges.put(policyName, (HashMap<Integer, Double>) this.congestionCharges.get(policyName).get(vht, hour));

				fetchedRouteSet.calculateUtilities(vht, engine, this.linkTravelTimePerTimeOfDay.get(hour), this.energyConsumptions, this.relativeFuelEfficiencies, this.energyUnitCosts, linkCharges, routeChoiceParameters);
				fetchedRouteSet.calculateProbabilities();
				//}

				//choose the route
				chosenRoute = fetchedRouteSet.choose();
			}
		}

		return chosenRoute;
	}
}