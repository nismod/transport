/**
 * 
 */
package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
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
import nismod.transport.utility.RandomSingleton;

/**
 * Network assignment of origin-destination flows
 * @author Milan Lovric
 *
 */
public class RoadNetworkAssignment {

	public static final double SPEED_LIMIT_M_ROAD = 112.65; //70mph = 31.29mps = 112.65kph
	public static final double SPEED_LIMIT_A_ROAD = 96.56; //60mph = 26.82mps = 96.56kph
	public static final int MAXIMUM_CAPACITY_M_ROAD = 2330; //PCU per lane per hour
	public static final int MAXIMUM_CAPACITY_A_ROAD = 1380; //PCU per lane per hour
	public static final int NUMBER_OF_LANES_M_ROAD = 3; //for one direction
	public static final int NUMBER_OF_LANES_A_ROAD = 1; //for one direction
	public static final double AVERAGE_SPEED_FERRY = 30.0; //30.0kph
	public static final double PEAK_HOUR_PERCENTAGE = 0.10322;
	public static final double ALPHA = 0.15;
	public static final double BETA_M_ROAD = 5.55;
	public static final double BETA_A_ROAD = 4;
	
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
	private HashMap<Integer, HashMap<VehicleType, Integer>> linkVolumesPerVehicleType;
	private HashMap<Integer, Double> linkFreeFlowTravelTime;
	private HashMap<Integer, Double> linkTravelTime;

	//inter-zonal path storage - for every OD pair stores a list of paths
	private MultiKeyMap<String, List<Path>> pathStorage;
	//private MultiKeyMap<Integer, List<Path>> pathStorageFreight;
	private HashMap<VehicleType, MultiKeyMap<Integer, List<Path>>> pathStorageFreight;
	
	//the probability of trip starting/ending in the census output area
	private HashMap<String, Double> areaCodeProbabilities;
	
	//the probability of trip starting/ending in the workplace zone
	private HashMap<String, Double> workplaceZoneProbabilities;
	
	/**
	 * @param roadNetwork Road network.
	 * @param defaultLinkTravelTime Default link travel times.
	 * @param areCodeProbabilities Probabilities of trips starting/ending in each census output area.
	 */
	public RoadNetworkAssignment(RoadNetwork roadNetwork, HashMap<EngineType, Double> energyUnitCosts, HashMap<Integer, Double> defaultLinkTravelTime, HashMap<String, Double> areaCodeProbabilities, HashMap<String, Double> workplaceZoneProbabilities) {

		this.roadNetwork = roadNetwork;
		this.linkVolumesInPCU = new HashMap<Integer, Double>();
		//this.linkVolumesPerVehicleType = new HashMap<Integer, HashMap<String, Double>>();
		this.linkFreeFlowTravelTime = new HashMap<Integer, Double>();
		this.linkTravelTime = new HashMap<Integer, Double>();
		this.pathStorage = new MultiKeyMap<String, List<Path>>();
		//this.pathStorageFreight = new MultiKeyMap<Integer, List<Path>>();
		this.pathStorageFreight = new HashMap<VehicleType, MultiKeyMap<Integer, List<Path>>>();
		for (VehicleType vht: VehicleType.values()) {
			MultiKeyMap<Integer, List<Path>> map = new MultiKeyMap<Integer, List<Path>>();
			pathStorageFreight.put(vht, map);
		}
		
		//calculate link travel time
		Iterator edgesIterator = roadNetwork.getNetwork().getEdges().iterator();
		while (edgesIterator.hasNext()) {
			DirectedEdge edge = (DirectedEdge) edgesIterator.next();
			//calculate free-flow travel time
			SimpleFeature feature = (SimpleFeature)edge.getObject();
			String roadNumber = (String) feature.getAttribute("RoadNumber");
			double travelTime = 0;
			if (roadNumber.charAt(0) == 'M') //motorway
				travelTime = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.SPEED_LIMIT_M_ROAD * 60;  //travel time in minutes
			else if (roadNumber.charAt(0) == 'A') //A road
				travelTime = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.SPEED_LIMIT_A_ROAD * 60;  //travel time in minutes
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
		System.out.println("Probabilities for workplace zones:");
		System.out.println(this.workplaceZoneProbabilities);
		
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
		
		engineTypeFractions = new HashMap<EngineType, Double>();
		engineTypeFractions.put(EngineType.PETROL, 0.45);
		engineTypeFractions.put(EngineType.DIESEL, 0.35);
		engineTypeFractions.put(EngineType.LPG, 0.1);
		engineTypeFractions.put(EngineType.ELECTRICITY, 0.05);
		engineTypeFractions.put(EngineType.HYDROGEN, 0.025);
		engineTypeFractions.put(EngineType.HYBRID, 0.025);
	}

	/** 
	 * Assigns passenger origin-destination matrix to the road network.
	 * Uses the fastest path based on the current values in the linkTravelTime field.
	 * @param passengerODM Passenger origin-destination matrix.
	 */
	public void assignPassengerFlows(ODMatrix passengerODM) {

	//	System.out.println("Assigning the passenger flows from the passenger matrix...");

		//for each OD pair from the passengerODM		
		for (MultiKey mk: passengerODM.getKeySet()) {
			//System.out.println(mk);
			//System.out.println("origin = " + mk.getKey(0));
			//System.out.println("destination = " + mk.getKey(1));

			//for each trip
			int flow = passengerODM.getFlow((String)mk.getKey(0), (String)mk.getKey(1));
			for (int i=0; i<flow; i++) {
				
				/*
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
				*/
					
				List<String> listOfOriginAreaCodes = roadNetwork.getZoneToAreaCodes().get(mk.getKey(0));
				List<String> listOfDestinationAreaCodes = roadNetwork.getZoneToAreaCodes().get(mk.getKey(1));
				
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
				if (destinationAreaCode == null) System.err.println("Destination otuput area was not selected.");
				
				//take the nearest node on the network
				int originNode = roadNetwork.getAreaCodeToNearestNode().get(originAreaCode);
				int destinationNode = roadNetwork.getAreaCodeToNearestNode().get(destinationAreaCode);
				
				//get the shortest path from the origin node to the destination node using AStar algorithm
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
					
					AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctionsTime(to, this.linkTravelTime));
					//AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
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
					}
					//System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

					//store path in path storage
					if (pathStorage.containsKey(mk.getKey(0), mk.getKey(1))) 
						list = (List<Path>) pathStorage.get(mk.getKey(0), mk.getKey(1));
					else {
						list = new ArrayList<Path>();
						pathStorage.put((String)mk.getKey(0), (String)mk.getKey(1), list);
					}
					list.add(aStarPath); //list.add(path);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}//for each trip
		}//for each OD pair
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
	public void assignFreightFlows(FreightMatrix freightMatrix) {

		
		//System.out.println("Assigning the vehicle flows from the freight matrix...");

		//for each OD pair from the passengerODM		
		for (MultiKey mk: freightMatrix.getKeySet()) {
			//System.out.println(mk);
			//System.out.println("origin = " + mk.getKey(0));
			//System.out.println("destination = " + mk.getKey(1));
			//System.out.println("vehicle type = " + mk.getKey(2));

			//for each trip
			int flow = freightMatrix.getFlow((int)mk.getKey(0), (int)mk.getKey(1), (int)mk.getKey(2));
			for (int i=0; i<flow; i++) {
				
				int originNode, destinationNode;

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
					
					//use the network node nearest to the workplace zone (population-weighted) centroid
					destinationNode = roadNetwork.getWorkplaceZoneToNearestNode().get(destinationWorkplaceCode);
					
				} else {// freight zone is a point (port, airport or distribution centre)
					destinationNode = roadNetwork.getFreightZoneToNearestNode().get((int)mk.getKey(1));
				}
				
				//get the shortest path from the origin node to the destination node using AStar algorithm
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
					AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctionsTime(to, this.linkTravelTime));
					//AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
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

						//increase volume count for that edge
						Double volumeInPCU = linkVolumesInPCU.get(e.getID());
						if (volumeInPCU == null) volumeInPCU = 0.0;
						volumeInPCU += this.vehicleTypeToPCU.get(VehicleType.values()[(int)mk.getKey(2)]);
						linkVolumesInPCU.put(e.getID(), volumeInPCU);
					}
					//System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

//					//store path in path storage
//					if (pathStorageFreight.containsKey(mk.getKey(0), mk.getKey(1))) 
//						list = (List<Path>) pathStorageFreight.get(mk.getKey(0), mk.getKey(1));
//					else {
//						list = new ArrayList<Path>();
//						pathStorageFreight.put((int)mk.getKey(0), (int)mk.getKey(1), list);
//					}
//					list.add(aStarPath); //list.add(path);
					
					//store path in path storage
					VehicleType vht = VehicleType.values()[(int)mk.getKey(2)];
					if (pathStorageFreight.get(vht).containsKey(mk.getKey(0), mk.getKey(1))) 
						list = (List<Path>) pathStorageFreight.get(vht).get(mk.getKey(0), mk.getKey(1));
					else {
						list = new ArrayList<Path>();
						pathStorageFreight.get(vht).put((int)mk.getKey(0), (int)mk.getKey(1), list);
					}
					list.add(aStarPath); //list.add(path);
					
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
		while(iter.hasNext()) {
			
			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			
			double linkVolumeInPCU;
			if (linkVolumesInPCU.get(edge.getID()) == null) linkVolumeInPCU = 0.0;
			else linkVolumeInPCU = linkVolumesInPCU.get(edge.getID());
			
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			if (roadNumber.charAt(0) == 'M') //motorway
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID())*(1 + ALPHA * Math.pow(PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / NUMBER_OF_LANES_M_ROAD / MAXIMUM_CAPACITY_M_ROAD, BETA_M_ROAD));
			else if (roadNumber.charAt(0) == 'A') //A-road
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID())*(1 + ALPHA * Math.pow(PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / NUMBER_OF_LANES_A_ROAD / MAXIMUM_CAPACITY_A_ROAD, BETA_A_ROAD));
			else //ferry
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
		while(iter.hasNext()) {
			
			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			
			double linkVol;
			if (linkVolumesInPCU.get(edge.getID()) == null) linkVol = 0.0;
			else linkVol = linkVolumesInPCU.get(edge.getID());
			
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			if (roadNumber.charAt(0) == 'M') //motorway
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID())*(1 + ALPHA * Math.pow(PEAK_HOUR_PERCENTAGE * linkVol / NUMBER_OF_LANES_M_ROAD / MAXIMUM_CAPACITY_M_ROAD, BETA_M_ROAD));
			else if (roadNumber.charAt(0) == 'A') //A-road
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID())*(1 + ALPHA * Math.pow(PEAK_HOUR_PERCENTAGE * linkVol / NUMBER_OF_LANES_A_ROAD / MAXIMUM_CAPACITY_A_ROAD, BETA_A_ROAD));
			else //ferry
				congestedTravelTime = linkFreeFlowTravelTime.get(edge.getID());
			
			//average with the old value (currently stored in the linkTravelTime field)
			congestedTravelTime = weight * congestedTravelTime + (1 - weight) * linkTravelTime.get(edge.getID());
			
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
			this.resetLinkVolumesInPCU(); //link volumes must be reset or they would compound across all iterations
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
				//System.out.printf("Path travel time: %.3f\n\n", pathTravelTime);
				totalODtravelTime += pathTravelTime;
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
					//System.out.printf("Path travel time: %.3f\n\n", pathTravelTime);
					totalODtravelTime += pathTravelTime;
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
			for (Path path: pathList) 
				for (Object o: path.getEdges()) {
					Edge e = (Edge)o;
					SimpleFeature sf = (SimpleFeature) e.getObject();
					double length = (double) sf.getAttribute("LenNet");
					totalDistance += length;					
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
				for (Path path: pathList) 
					for (Object o: path.getEdges()) {
						Edge e = (Edge)o;
						SimpleFeature sf = (SimpleFeature) e.getObject();
						double length = (double) sf.getAttribute("LenNet");
						totalDistance += length;					
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
				capacity = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / NUMBER_OF_LANES_M_ROAD;
			else if (roadNumber.charAt(0) == 'A') //A-road
				capacity = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU / NUMBER_OF_LANES_A_ROAD;
			else //ferry
				capacity = PEAK_HOUR_PERCENTAGE * linkVolumeInPCU;
			
			linkPointCapacities.put(edge.getID(), capacity);
		}
		return linkPointCapacities;
	}
	
	/**
	 * Calculate peak-hour link densities (PCU/lane/km/hr)
	 */
	public HashMap<Integer, Double> calculatePeakLinkDensities() {

		HashMap<Integer, Double> linkDensities = new HashMap<Integer, Double>();
		
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
				density = PEAK_HOUR_PERCENTAGE * linkVol / NUMBER_OF_LANES_M_ROAD / length;
			else if (roadNumber.charAt(0) == 'A') //A-road
				density = PEAK_HOUR_PERCENTAGE * linkVol / NUMBER_OF_LANES_A_ROAD / length;
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
	 * Saves assignment results to output files.
	 */
	public void saveAssignmentResults() {

	}
	
	/**
	 * Saves total electricity consumption to an output file.
	 */
	public void saveTotalElectricityConsumption(String OutputFile) {

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
	 * Getter method for workplace zones probabilities.
	 * @return Workplace zones probabilities.
	 */
	public HashMap<String, Double> getWorkplaceZoneProbabilities() {

		return this.workplaceZoneProbabilities;
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
	public void resetLinkVolumesInPCU () {
		
		for (Integer key: this.linkVolumesInPCU.keySet()) this.linkVolumesInPCU.put(key, 0.0);
	}
}