package nismod.transport.network.road;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.opengis.feature.simple.SimpleFeature;

import gnu.trove.list.array.TIntArrayList;
import nismod.transport.network.road.RoadNetwork.EdgeType;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.RouteSet.RouteChoiceParams;

/**
 * Route is a sequence of directed edges with a choice utility.
 * @author Milan Lovric
 *
 */
public class Route {
	
	private final static Logger LOGGER = LogManager.getLogger(Route.class);
	
//	//default route-choice parameters
//	public static final double PARAM_TIME = -1.5;
//	public static final double PARAM_LENGTH = -1.0;
//	public static final double PARAM_COST = -3.6;
//	public static final double PARAM_INTERSECTIONS = -0.1;
//
//	public static final double AVERAGE_INTERSECTION_DELAY = 0.8; //[min] 
	
	//initial route size for arraylist
//	public static final int INITIAL_ROUTE_CAPACITY = 10;

//	private static int counter = 0;
//	private int id;
	
	public static enum WebTAG {
		A, B, C, D
	}
	
	private TIntArrayList edges; //optimised list of edge IDs
	private double length;
	private double time;
	private double cost;
	private double utility;
	private Node singleNode;
	
	private static RoadNetwork roadNetwork;
	
	public Route(RoadNetwork roadNetwork) {
	
		Route.roadNetwork = roadNetwork;
		this.edges = new TIntArrayList(RouteSetGenerator.INITIAL_ROUTE_CAPACITY);
//		this.id = ++Route.counter;
	}
	
	/**
	 * Construtor from a given path.
	 * @param path A path from which to construct a route.
	 * @param roadNetwork Road network.
	 */
	public Route(RoadPath path, RoadNetwork roadNetwork) {
		
		if (path == null) {
			System.err.println("Route constructur: Path is null!");
			return;
		}
		
		if (!path.isValid()) {
			System.err.println("Route constructor: Path is not valid!");
			return;
		}
		
		Route.roadNetwork = roadNetwork;
		List<DirectedEdge> builtEdges = (List<DirectedEdge>) path.getEdges(); //builds edges
		
		if (builtEdges == null) {
			System.err.println("Route constructor: Edge list is null!");
			return;
		}
		if (builtEdges.isEmpty()) {
			if (path.getFirst().equals(path.getLast()))
				this.singleNode = path.getFirst(); 	//single node path can be accepted
			else {
				System.err.println("Route constructor: Path has no edges!");
				return;
			}
		}
		
		this.edges = new TIntArrayList(RouteSetGenerator.INITIAL_ROUTE_CAPACITY);
		for (DirectedEdge edge: builtEdges) this.edges.add(edge.getID()); 
			
//		this.id = ++Route.counter;
		//System.out.println("Constructing a route from path (nodes): " + path.toString());
		//System.out.println("Constructing a route from path (edges): " + path.getEdges());
		
		//this.edges = new ArrayList<DirectedEdge>(INITIAL_ROUTE_SIZE);
		//for (Object o: path.getEdges()) {
		//	DirectedEdge edge = (DirectedEdge) o;
		//	this.addEdge(edge);
		//}
	}
	
	/**
	 * Getter method for the list of edges.
	 * @return List of edge IDs.
	 */
	public TIntArrayList getEdges() {
		
		return	this.edges;
	}
	
	
	/**
	 * Trims edges list to size and calculate length (onetime operation).
	 */
	public void trimToSize() {
		
		this.edges.trimToSize();
	}
	
	/**
	 * Adds a directed edge to the end of the current route.
	 * @param edge Directed edge to be added.
	 * @return true if edge addition was successful, false otherwise.
	 */
	public boolean addEdge(DirectedEdge edge) {

		if (this.edges.isEmpty()) this.edges.add(edge.getID());
		else {
			int lastEdgeID = this.edges.get(this.edges.size()-1);
			DirectedEdge lastEdge = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[lastEdgeID];
			if (!lastEdge.getOutNode().equals(edge.getInNode())) {
//				System.err.printf("Trying to add an edge %d that is not connected to the last edge %d in the route (id = %d)\n", edge.getID(), lastEdge.getID(), this.getID());
//				System.err.printf("(%d)-%d->(%d), (%d)-%d->(%d)", lastEdge.getInNode().getID(), lastEdge.getID(), lastEdge.getOutNode().getID(), edge.getInNode().getID(), edge.getID(), edge.getOutNode().getID());
				LOGGER.warn("Trying to add a non-connected edge: ({})-{}->({}), ({})-{}->({}).", lastEdge.getInNode().getID(), lastEdge.getID(), lastEdge.getOutNode().getID(), edge.getInNode().getID(), edge.getID(), edge.getOutNode().getID());
				return false;
			} else
				this.edges.add(edge.getID());
		}
		return true;
	}
	
	/**
	 * Adds a directed edge to the end of the current route.
	 * @param edge Directed edge to be added.
	 */
	public void addEdgeWithoutValidityCheck(DirectedEdge edge) {

		this.edges.add(edge.getID());
	}
	
	/**
	 * Adds a directed edge to the end of the current route.
	 * @param edgeID Directed edge to be added.
	 */
	public void addEdgeWithoutValidityCheck(int edgeID) {

		this.edges.add(edgeID);
	}
	
	/**
	 * Calculates the route travel time based on link travel times.
	 * @param linkTravelTime Link travel times.
	 * @param avgIntersectionDelay Average intersection delay (in minutes).
	 */
	public void calculateTravelTime(double[] linkTravelTime, double avgIntersectionDelay) {
		
		double travelTime = 0.0;
		for (int edgeID: edges.toArray()) {
			double time = linkTravelTime[edgeID];
			travelTime += time;
		}
		travelTime += this.getNumberOfIntersections() * avgIntersectionDelay;
		this.time = travelTime;
	}
	
	/**
	 * Calculates the length of the route.
	 */
	public void calculateLength(){
	
		double length = 0.0;
		for (int edgeID: edges.toArray()) {
			length += roadNetwork.getEdgeLength(edgeID);
		}
		this.length = length;
	}
	
	/**
	 * Calculates the cost of the route.
	 * @param vht Vehicle type.
	 * @param et Engine type.
	 * @param linkTravelTime Link travel times.
	 * @param energyConsumptionParameters Base year energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency (compared to base year).
	 * @param energyUnitCosts Energy unit costs.
	 * @param linkCharges Congestion charges.
	 */
	public void calculateCost(VehicleType vht, EngineType et, double[] linkTravelTime, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, Map<EnergyType, Double> energyUnitCosts, HashMap<String, HashMap<Integer, Double>> linkCharges) {

		//temporary map to check if a charging policy has already been applied
		HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
		if (linkCharges != null)
			for (String policyName: linkCharges.keySet()) flags.put(policyName, false);
		
		double fuelCost = 0.0;
		Map<EnergyType, Double> routeConsumptions = this.calculateConsumption(vht, et, linkTravelTime, energyConsumptionParameters, relativeFuelEfficiency);
		
		for (EnergyType energy: EnergyType.values()) fuelCost +=  routeConsumptions.get(energy) * energyUnitCosts.get(energy);
		
		double tollCost = 0.0;
		for (int edgeID: edges.toArray()) {

			if (linkCharges != null)
				for (String policyName: linkCharges.keySet()) {
					if (flags.get(policyName)) continue; //skip if policy already applied
					HashMap<Integer, Double> charges = linkCharges.get(policyName);
					if (charges == null) {
						System.err.println("No link charges for policy " + policyName);
						flags.put(policyName, true);
						continue; //skip this policy then
					}
					if (charges.containsKey(edgeID)) {
						tollCost += charges.get(edgeID);
						flags.put(policyName, true);
					}
				}
		}
				
		this.cost = fuelCost + tollCost;
	}
	
	/**
	 * Calculates energy consumption of the route.
	 * @param vht Vehicle type.
	 * @param et Energy type.
	 * @param linkTravelTime Link travel time.
	 * @param energyConsumptionParameters Base year energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency compared to base year.
	 * @return Consumption for each type.
	 */
	public Map<EnergyType, Double> calculateConsumption(VehicleType vht, EngineType et, double[] linkTravelTime, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency) {

		Map<EnergyType, Double> routeConsumptions = new EnumMap<>(EnergyType.class);
		Map<WebTAG, Double> parameters, parametersFuel, parametersElectricity;

		for (EnergyType energy: EnergyType.values()) routeConsumptions.put(energy, 0.0);

		if (et == EngineType.PHEV_PETROL) {

			parametersFuel = energyConsumptionParameters.get(vht).get(EngineType.ICE_PETROL);
			parametersElectricity = energyConsumptionParameters.get(vht).get(EngineType.BEV);
			double relativeEfficiencyFuel = relativeFuelEfficiency.get(vht).get(EngineType.ICE_PETROL);
			double relativeEfficiencyElectricity = relativeFuelEfficiency.get(vht).get(EngineType.BEV);

			double fuelConsumption = 0.0;
			double electricityConsumption = 0.0;
			for (int edgeID: edges.toArray()) {
				double len = roadNetwork.getEdgeLength(edgeID); //in [km]
				double time = linkTravelTime[edgeID]; //in [min]
				//String cat = (String) sf.getAttribute("RCat"); //road category (PM, PR, Pu, PU, TM, TR, Tu, TU), use Pu, PU, Tu, TU as urban, otherwise rural
				Boolean isUrban = roadNetwork.getIsEdgeUrban()[edgeID];
				double speed = len / (time / 60);

				//if no roadCategory information, assume it is ferry and skip
				if (isUrban == null) {
					LOGGER.trace("No road category information. Assuming it is ferry and skipping for consumption calculation.");
					continue;
				}

				//if edge is urban use electricity, if rural use fuel
				if (isUrban)
					electricityConsumption += len * (parametersElectricity.get(WebTAG.A) / speed + parametersElectricity.get(WebTAG.B) + parametersElectricity.get(WebTAG.C) * speed + parametersElectricity.get(WebTAG.D) * speed  * speed);
				else 
					fuelConsumption += len * (parametersFuel.get(WebTAG.A) / speed + parametersFuel.get(WebTAG.B) + parametersFuel.get(WebTAG.C) * speed + parametersFuel.get(WebTAG.D) * speed  * speed);
			}
			//apply relative fuel efficiency
			electricityConsumption *= relativeEfficiencyElectricity;
			fuelConsumption *= relativeEfficiencyFuel;
			//store
			routeConsumptions.put(EnergyType.ELECTRICITY, electricityConsumption);
			routeConsumptions.put(EnergyType.PETROL, fuelConsumption);

		} else if (et == EngineType.PHEV_DIESEL) {
			parametersFuel = energyConsumptionParameters.get(vht).get(EngineType.ICE_DIESEL);
			parametersElectricity = energyConsumptionParameters.get(vht).get(EngineType.BEV);
			double relativeEfficiencyFuel = relativeFuelEfficiency.get(vht).get(EngineType.ICE_DIESEL);
			double relativeEfficiencyElectricity = relativeFuelEfficiency.get(vht).get(EngineType.BEV);

			double fuelConsumption = 0.0;
			double electricityConsumption = 0.0;
			for (int edgeID: edges.toArray()) {
				double len = roadNetwork.getEdgeLength(edgeID); //in [km]
				double time = linkTravelTime[edgeID]; //in [min]
				Boolean isUrban = roadNetwork.getIsEdgeUrban()[edgeID];
				double speed = len / (time / 60);

				//if no roadCategory information, assume it is ferry and skip
				if (isUrban == null) {
					LOGGER.trace("No road category information. Assuming it is ferry and skipping for consumption calculation.");
					continue;
				}

				//if edge is urban use electricity, if rural use fuel
				if (isUrban)
					electricityConsumption += len * (parametersElectricity.get(WebTAG.A) / speed + parametersElectricity.get(WebTAG.B) + parametersElectricity.get(WebTAG.C) * speed + parametersElectricity.get(WebTAG.D) * speed  * speed);
				else 
					fuelConsumption += len * (parametersFuel.get(WebTAG.A) / speed + parametersFuel.get(WebTAG.B) + parametersFuel.get(WebTAG.C) * speed + parametersFuel.get(WebTAG.D) * speed  * speed);
			}
			//apply relative fuel efficiency
			electricityConsumption *= relativeEfficiencyElectricity;
			fuelConsumption *= relativeEfficiencyFuel;
			//store
			routeConsumptions.put(EnergyType.ELECTRICITY, electricityConsumption);
			routeConsumptions.put(EnergyType.DIESEL, fuelConsumption);

		} else {

			parameters = energyConsumptionParameters.get(vht).get(et);
			double relativeEfficiency = relativeFuelEfficiency.get(vht).get(et);

			double consumption = 0.0;
			for (int edgeID: edges.toArray()) {
				
				//skip ferry
				if (roadNetwork.getEdgesType()[edgeID] == EdgeType.FERRY) {
					LOGGER.trace("Skipping ferry edge in consumption calculation.");
					continue;
				}
				
				double len = roadNetwork.getEdgeLength(edgeID); //in [km]
				double time = linkTravelTime[edgeID]; //in [min]
				double speed = len / (time / 60);
				consumption += len * (parameters.get(WebTAG.A) / speed + parameters.get(WebTAG.B) + parameters.get(WebTAG.C) * speed + parameters.get(WebTAG.D) * speed  * speed);
			}
			//apply relative fuel efficiency
			consumption *= relativeEfficiency;
			//store
			if (et == EngineType.ICE_PETROL || et == EngineType.HEV_PETROL)
				routeConsumptions.put(EnergyType.PETROL, consumption);
			else if (et == EngineType.ICE_DIESEL || et == EngineType.HEV_DIESEL)
				routeConsumptions.put(EnergyType.DIESEL, consumption);
			else if (et == EngineType.ICE_CNG)
				routeConsumptions.put(EnergyType.CNG, consumption);
			else if (et == EngineType.ICE_H2)
				routeConsumptions.put(EnergyType.HYDROGEN, consumption);
			else if (et == EngineType.BEV)
				routeConsumptions.put(EnergyType.ELECTRICITY, consumption);
		}

		return routeConsumptions;
	}

	
	/**
	 * Calculates the utility of the route.
	 * @param vht Vehicle type.
	 * @param et Engine type.
	 * @param linkTravelTime Link travel times.
	 * @param energyConsumptionParameters Energy consumption parameters (A, B, C, D) for a combination of vehicle type and engine type.
	 * @param relativeFuelEfficiency Relative fuel efficiency compared to the base year.
	 * @param energyUnitCosts Energy unit costs.
	 * @param linkCharges Congestion charges.
	 * @param params Route choice parameters.
	 */
	public void calculateUtility(VehicleType vht, EngineType et, double[] linkTravelTime, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, Map<EnergyType, Double> energyUnitCosts, HashMap<String, HashMap<Integer, Double>> linkCharges, Map<RouteChoiceParams, Double> params) {		
		
		//if a single node route, utility is zero
		if (this.edges.isEmpty() && this.singleNode != null) {
			this.utility = 0;
			return;
		}
		
		//otherwise
		if (Double.compare(this.length, 0.0d) == 0) 
			this.calculateLength(); //calculate only once (length is not going to change)
				
		double avgIntersectionDelay = params.get(RouteChoiceParams.DELAY);
	
		this.calculateTravelTime(linkTravelTime, avgIntersectionDelay); //always (re)calculate
		this.calculateCost(vht, et, linkTravelTime, energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, linkCharges); //always (re)calculate
		
		double length = this.getLength();
		double time = this.getTime();
		double cost = this.getCost();
		int intersec = this.getNumberOfIntersections();
		
		double paramTime = params.get(RouteChoiceParams.TIME);
		double paramLength = params.get(RouteChoiceParams.LENGTH);
		double paramCost = params.get(RouteChoiceParams.COST);
		double paramIntersections = params.get(RouteChoiceParams.INTERSEC);
		
		double utility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersec;  
		this.utility = utility;
	}
	
	/**
	 * Checks if route is empty or not.
	 * @return True if route is empty.
	 */
	public boolean isEmpty() {
		
		return (this.edges.isEmpty() && this.singleNode == null);
	}
	
	/**
	 * Checks if route contains the edge.
	 * @param edge Edge object.
	 * @return True if route contains the edge.
	 */
	public boolean contains(Edge edge) {
		
		return this.edges.contains(edge.getID());
	}
	
	/**
	 * Checks if route contains the edge.
	 * @param edgeID Edge id.
	 * @return True if route contains the edge.
	 */
	public boolean contains(int edgeID) {
		
		return this.edges.contains(edgeID);
	}

	/**
	 * Getter method for route length.
	 * @return Route length.
	 */
	public double getLength() {
		
		return length;
	}
	
	/**
	 * Getter method for route time.
	 * @return Route time.
	 */
	public double getTime() {
	
		return time;
	}
	
	/**
	 * Getter method for route cost.
	 * @return Route cost.
	 */
	public double getCost() {
		
		return cost;
	}
	
	/**
	 * Getter method for number of intersections.
	 * @return Number of intersections.
	 */
	public int getNumberOfIntersections() {
		
		if (this.edges.isEmpty())
			return 0;
		else 
			return (this.edges.size() - 1);
	}
	
	/**
	 * Getter method for route utility.
	 * @return Route utility.
	 */
	public double getUtility() {
		
		//if (this.utility == null) 
		//	System.err.println("Route utility needs to be first calculated using link travel time and other variables!");
		return utility;
	}
	
	/**
	 * Setter method for route utility.
	 * @param utility Route utility.
	 */
	public void setUtility(double utility) {
		
		this.utility = utility;
	}
	
	/**
	 * Getter method for route origin node.
	 * @return Origin node.
	 */
	public DirectedNode getOriginNode() {
		
		if (edges.isEmpty())
			return (DirectedNode) this.singleNode;
		else {
			int firstEdgeID = this.edges.get(0);
			DirectedEdge firstEdge = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[firstEdgeID];
			return firstEdge.getInNode();
		}
	}
	
	/**
	 * Getter method for destination node.
	 * @return Destination node.
	 */
	public DirectedNode getDestinationNode() {
		if (edges.isEmpty())
			return (DirectedNode) this.singleNode;
		else {
			int lastEdgeID = this.edges.get(edges.size() - 1);
			DirectedEdge lastEdge = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[lastEdgeID];
			return lastEdge.getOutNode();	
		}
	}
	
	/**
	 * Getter method for the road network.
	 * @return Road network.
	 */
	public RoadNetwork getRoadNetwork() {
		
		return this.roadNetwork;
		
	}
	
//	public int getID() {
//		
//		return id;
//	}
	
	/**
	 * Checks if route is valid (successive edges in the route are connected in a directional way).
	 * @return True if route is valid.
	 */
	public boolean isValid() {
		
		//if (this.edges.size() == 1) return true; //single node route still considered valid!
		
		for (int i = 1; i < this.edges.size(); i++) {
			int edgeID1 = this.edges.get(i-1);
			int edgeID2 = this.edges.get(i);
			
			DirectedEdge edge1 = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[edgeID1];
			DirectedEdge edge2 = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[edgeID2];
			
			if (!edge1.getOutNode().equals(edge2.getInNode())) return false; //if edges are not connected
		}
		return true;
	}
	
	@Override
	public boolean equals(Object obj) { //TODO override hashCode too
	    if (obj == null) {
	        return false;
	    }
	    if (!Route.class.isAssignableFrom(obj.getClass())) {
	        return false;
	    }
	    final Route other = (Route) obj;
	    if ((this.edges == null) ? (other.edges != null) : !this.edges.equals(other.edges)) {
	        return false;
	    }
	    return true;
	}
	
	@Override
	public String toString() {

		if (edges.isEmpty() && this.singleNode == null) return "";

		StringBuilder sb = new StringBuilder();

		if (this.singleNode != null) {
			sb.append("(");
			sb.append(this.getOriginNode().getID());
			sb.append(")->(");
			sb.append(this.getDestinationNode().getID());
			sb.append(")");
		} else {
			for (int edgeID: edges.toArray()) {
				DirectedEdge edge = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[edgeID];
				sb.append("(");
				sb.append(edge.getInNode().getID());
				sb.append(")-");
				sb.append(edge.getID());
				sb.append("->");
				//sb.append("(");
				//sb.append(edge.getOutNode().getID());
				//sb.append(")");
			}
			sb.append("(");
			int lastEdgeID = edges.get(edges.size()-1);
			DirectedEdge lastEdge = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[lastEdgeID];
			sb.append(lastEdge.getOutNode());
			sb.append(")");
		}

		return sb.toString();
	}
	
	/**
	 * Gets formatted string representation of the route.
	 * @return Route as a string.
	 */
	public String getFormattedString() {
		
		if (edges.isEmpty() && this.singleNode == null) return "";
		
		StringBuilder sb = new StringBuilder();
		
		//origin and destination node IDs
		sb.append(this.getOriginNode().getID());
		sb.append(":");
		sb.append(this.getDestinationNode().getID());
		sb.append(":");
				
		for (int edgeID: edges.toArray()) {
			sb.append(edgeID);
			sb.append("-");
		}
		sb.delete(sb.length()-1, sb.length()); //delete last dash
		
		return sb.toString();
	}
	
	/**
	 * Gets formatted string representation of the route using edge IDs only.
	 * @return Route as a string.
	 */
	public String getFormattedStringEdgeIDsOnly() {
		
		if (edges.isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder();
		
		for (int edgeID: edges.toArray()) {
			sb.append(edgeID);
			sb.append("-");
		}
		sb.delete(sb.length()-1, sb.length()); //delete last dash
		
		return sb.toString();
	}
}
