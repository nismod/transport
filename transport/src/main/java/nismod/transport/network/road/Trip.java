package nismod.transport.network.road;

import java.util.HashMap;
import java.util.Map;

import org.geotools.graph.structure.DirectedNode;

import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;

/**
 * This class stores information about a trip (what kind of vehicle was used and which route was taken). 
 * @author Milan Lovric
 *
 */
public class Trip {
	
	private VehicleType vehicle;
	private EngineType engine;
	private Route route;
	private TimeOfDay hour;
	
	public Trip() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Constructor.
	 * @param vehicle Vehicle type.
	 * @param engine Engine type.
	 * @param route Route.
	 */
	public Trip(VehicleType vehicle, EngineType engine, Route route, TimeOfDay hour) {
		
		this.vehicle = vehicle;
		this.engine = engine;
		this.route = route;
		this.hour = hour;
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
	public String getTripOriginZone(Map<Integer, String> nodeToZoneMap) {
		
		int originNode = this.getOriginNode().getID();
		return nodeToZoneMap.get(originNode);
	}
	
	/**
	 * Gets trip destination zone (LAD).
	 * @param nodeToZoneMap Mapping from nodes to zones.
	 * @return Trip destination zone.
	 */
	public String getTripDestinationZone(Map<Integer, String> nodeToZoneMap) {
		
		int destinationNode = this.getDestinationNode().getID();
		return nodeToZoneMap.get(destinationNode);
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
	
	public double getTotalTripLength(HashMap<Integer, Double> averageAccessEgressMap) {

		Double length = this.route.getLength();
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
	
	public double getTotalTravelTime(HashMap<Integer, Double> linkTravelTime, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed) {
		
		Double time = this.route.getTime();
		if (time == null) {
			this.route.calculateTravelTime(linkTravelTime);
			time = this.route.getTime();
		}
		Double access = averageAccessEgressMap.get(this.getOriginNode().getID());
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = averageAccessEgressMap.get(this.getDestinationNode().getID());
		if (egress == null) egress = 0.0;
		double averageAccessTime = access / 1000 / averageAccessEgressSpeed * 60;
		double averageEgressTime = egress / 1000 / averageAccessEgressSpeed * 60;
		
		return time + averageAccessTime + averageEgressTime;
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
		sb.append(this.route.toString());

		return sb.toString();
	}
}
