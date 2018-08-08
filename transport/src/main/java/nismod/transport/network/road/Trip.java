package nismod.transport.network.road;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;

import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;

/**
 * This class stores information about a performed trip. 
 * @author Milan Lovric
 *
 */
public class Trip {
	
	private final static Logger LOGGER = LogManager.getLogger(Trip.class);
	
	protected VehicleType vehicle;
	protected EngineType engine;
	protected Route route;
	protected TimeOfDay hour;
	protected Integer origin; //for freight trips
	protected Integer destination; //for freight trips
	protected int multiplier; //multiplies the same trip multiplier times
		
	/**
	 * Constructor for a trip. Origin and destination are used for freight trips (according to DfT's BYFM zonal coding).
	 * Origin and destination for passenger car/AV trips are 0 as their correct origin and destination zone can be 
	 * obtained using the first and the last node of the route.
	 * @param vehicle Vehicle type.
	 * @param engine Engine type.
	 * @param route Route.
	 * @param hour Time of day.
	 * @param origin Origin zone for freight trips (null for passenger trips).
	 * @param destination Destination zone for freight trips (null for passenger trips).
	 */
	public Trip(VehicleType vehicle, EngineType engine, Route route, TimeOfDay hour, Integer origin, Integer destination) {
		
		this.vehicle = vehicle;
		this.engine = engine;
		this.route = route;
		this.hour = hour;
		this.origin = origin;
		this.destination = destination;
		this.multiplier = 1; //create just one trip
		
		/*
		if (vehicle == VehicleType.CAR || vehicle == VehicleType.AV)
			if (origin != 0 || destination != 0)
				System.err.println("Origin and destination for non-freight trips must be 0 as their ODs should be fetched from the route.");
		*/
	}
	
	/**
	 * Constructor for a trip. Origin and destination are used for freight trips (according to DfT's BYFM zonal coding).
	 * Origin and destination for passenger car/AV trips are 0 as their correct origin and destination zone can be 
	 * obtained using the first and the last node of the route.
	 * @param vehicle Vehicle type.
	 * @param engine Engine type.
	 * @param route Route.
	 * @param hour Time of day.
	 * @param origin Origin zone for freight trips (null for passenger trips).
	 * @param destination Destination zone for freight trips (null for passenger trips).
	 * @param multiplier Multiplies the same trip.
	 */
	public Trip(VehicleType vehicle, EngineType engine, Route route, TimeOfDay hour, Integer origin, Integer destination, int multiplier) {
		
		this.vehicle = vehicle;
		this.engine = engine;
		this.route = route;
		this.hour = hour;
		this.origin = origin;
		this.destination = destination;
		this.multiplier = multiplier; //creates multiples of the same trip
		
		/*
		if (vehicle == VehicleType.CAR || vehicle == VehicleType.AV)
			if (origin != 0 || destination != 0)
				System.err.println("Origin and destination for non-freight trips must be 0 as their ODs should be fetched from the route.");
		*/
	}
	
	/**
	 * Gets the trip origin node.
	 * @return Origin node.
	 */
	public DirectedNode getOriginNode () {
		
		return this.route.getOriginNode();
	}
	
	/**
	 * Gets the trip destination node.
	 * @return Destination node.
	 */
	public DirectedNode getDestinationNode () {
		
		return this.route.getDestinationNode();
	}
	
	/**
	 * Gets trip origin zone (LAD).
	 * @param nodeToZoneMap Mapping from nodes to zones.
	 * @return Trip origin zone.
	 */
	public String getOriginLAD(Map<Integer, String> nodeToZoneMap) {
		
		int originNode = this.getOriginNode().getID();
		return nodeToZoneMap.get(originNode);
	}
	
	/**
	 * Gets trip destination zone (LAD).
	 * @param nodeToZoneMap Mapping from nodes to zones.
	 * @return Trip destination zone.
	 */
	public String getDestinationLAD(Map<Integer, String> nodeToZoneMap) {
		
		int destinationNode = this.getDestinationNode().getID();
		return nodeToZoneMap.get(destinationNode);
	}
	
	/**
	 * Gets freight trip origin zone (using DfT BYFM zone coding).
	 * @return Freight trip origin zone.
	 */
	public int getFreightOriginZone() {

		return origin;
	}
	
	/**
	 * Gets freight trip destination zone (using DfT BYFM zone coding).
	 * @return Freight trip destination zone.
	 */
	public int getFreightDestinationZone() {

		return destination;
	}
	
	/**
	 * Getter method for engine type.
	 * @return Vehicle engine type.
	 */
	public EngineType getEngine() {
		
		return this.engine;
	}
	
	/**
	 * Getter method for vehicle type.
	 * @return Vehicle type.
	 */
	public VehicleType getVehicle() {
	
		return this.vehicle;
	}
	
	/**
	 * Getter method for the route.
	 * @return Route.
	 */
	public Route getRoute() {
		
		return this.route;
	}
	
	/**
	 * Getter method for the time of day.
	 * @return Time of day.
	 */
	public TimeOfDay getTimeOfDay() {
		
		return this.hour;
	}
	
	/**
	 * Getter method for the multiplier.
	 * @return Multiplier.
	 */
	public int getMultiplier() {
		
		return this.multiplier;
	}
	
	public double getLength(HashMap<Integer, Double> averageAccessEgressMap) {

		Double length = this.route.getLength(); //route length is not changing so if can be calculated only once and stored
		if (length == null) {
			this.route.calculateLength();
			length = this.route.getLength();
		}
		Double access = averageAccessEgressMap.get(this.getOriginNode().getID());
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = averageAccessEgressMap.get(this.getDestinationNode().getID());
		if (egress == null) egress = 0.0;
		
		return length + access / 1000 + egress / 1000;
	}
	
	public double getTravelTime(Map<Integer, Double> linkTravelTime, double avgIntersectionDelay, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed) {
		
		//Double time = this.route.getTime();
		//if (time == null) {
			this.route.calculateTravelTime(linkTravelTime, avgIntersectionDelay); //route travel time needs to be recalculated every time (as it depends on time of day).
			Double time = this.route.getTime();
		//}
		Double access = averageAccessEgressMap.get(this.getOriginNode().getID());
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = averageAccessEgressMap.get(this.getDestinationNode().getID());
		if (egress == null) egress = 0.0;
		double averageAccessTime = access / 1000 / averageAccessEgressSpeed * 60;
		double averageEgressTime = egress / 1000 / averageAccessEgressSpeed * 60;
		
		return time + averageAccessTime + averageEgressTime;
	}
	
	public double getCost(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed, HashMap<EnergyType, Double> energyUnitCosts, HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency, HashMap<String, MultiKeyMap> congestionCharges) {
		
		//double distance = this.getLength(averageAccessEgressMap);
		//double cost = distance / 100 * energyConsumptionsPer100km.get(this.engine) * energyUnitCosts.get(this.engine);
		
		//fetch congestion charge for the vehicle type
		HashMap<String, HashMap<Integer, Double>> linkCharges = new HashMap<String, HashMap<Integer, Double>>();
		if (congestionCharges != null) 
			for (String policyName: congestionCharges.keySet())
				linkCharges.put(policyName, (HashMap<Integer, Double>) congestionCharges.get(policyName).get(this.vehicle, this.hour));
				
		this.route.calculateCost(this.vehicle, this.engine, linkTravelTime, energyConsumptions, relativeFuelEfficiency, energyUnitCosts, linkCharges);
		double tripCost = this.route.getCost();
		
		//TODO add access/egress cost
				
		return tripCost;
	}
	
	public HashMap<EnergyType, Double> getConsumption(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed, HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency) {
		
		//double distance = this.getLength(averageAccessEgressMap);
		//double consumption = distance / 100 * energyConsumptionsPer100km.get(this.engine);
		
		HashMap<EnergyType, Double> consumption = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptions, relativeFuelEfficiency);
		
		//TODO add access/egress consumption
		
		return consumption;
	}
	
	public Double getCO2emission(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed, HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency, HashMap<EnergyType, Double> unitCO2Emissions) {
		
		HashMap<EnergyType, Double> consumption = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptions, relativeFuelEfficiency);
		
		double CO2 = 0.0;
		for (EnergyType et: consumption.keySet()) {
			
			CO2 += consumption.get(et) * unitCO2Emissions.get(et);
		}
		
		return CO2;
	}
	
	public boolean isTripGoingThroughCongestionChargingZone(String policyName, HashMap<String, MultiKeyMap> congestionCharges) {
		
		MultiKeyMap charges = congestionCharges.get(policyName);
		HashMap<Integer, Double> linkCharges = (HashMap<Integer, Double>) charges.get(this.vehicle, this.hour);
		
		for (int edgeID: this.route.getEdges().toArray()) {
			//if (linkCharges.containsKey(edge.getID()) && linkCharges.get(edge.getID()) > 0.0) return true;
			if (linkCharges.containsKey(edgeID)) return true; //we actually do not care if the charging is applied
		}
//		for (int edgeID: this.route.getEdges()) {
//			//if (linkCharges.containsKey(edge.getID()) && linkCharges.get(edge.getID()) > 0.0) return true;
//			if (linkCharges.containsKey(edgeID)) return true; //we actually do not care if the charging is applied
//		}
		
		return false;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(this.hour);
		sb.append(", ");
		sb.append(this.vehicle);
		sb.append(", ");
		sb.append(this.engine);
		sb.append(", ");
		sb.append(this.origin);
		sb.append(", ");
		sb.append(this.destination);
		sb.append(", ");
		sb.append(this.multiplier);
		sb.append(", ");
		sb.append(this.route.toString());

		return sb.toString();
	}
}
