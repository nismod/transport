package nismod.transport.network.road;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.graph.structure.DirectedNode;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.zone.Zoning;

/**
 * This class stores information about a trip. 
 * @author Milan Lovric
 *
 */
public class TripTempro extends Trip {
	
	private final static Logger LOGGER = Logger.getLogger(TripTempro.class.getName());
	
	private static Zoning zoning;
		
	/**
	 * Constructor for a trip. Origin and destination are used for freight trips (according to DfT's BYFM zonal coding).
	 * Origin and destination for passenger car/AV trips are 0 as their correct origin and destination zone can be 
	 * obtained using the first and the last node of the route.
	 * @param vehicle Vehicle type.
	 * @param engine Engine type.
	 * @param route Route.
	 * @param hour Time of day.
	 * @param originTemproZone Origin tempro zone ID.
	 * @param destinationTemproZone Destination tempro zone ID.
	 * @param zoning Zoning system.
	 */
	public TripTempro (VehicleType vehicle, EngineType engine, Route route, TimeOfDay hour, Integer originTemproZoneID, Integer destinationTemproZoneID, Zoning zoning) {
		
		super(vehicle, engine, route, hour, originTemproZoneID, destinationTemproZoneID);
		this.zoning = zoning;
	}
	
	/**
	 * Constructor for a trip. Origin and destination are used for freight trips (according to DfT's BYFM zonal coding).
	 * Origin and destination for passenger car/AV trips are 0 as their correct origin and destination zone can be 
	 * obtained using the first and the last node of the route.
	 * @param vehicle Vehicle type.
	 * @param engine Engine type.
	 * @param route Route.
	 * @param hour Time of day.
	 * @param originTemproZone Origin tempro zone ID.
	 * @param destinationTemproZone Destination tempro zone ID.
	 * @param zoning Zoning system.
	 * @param multiplier Multiplies the same trip.
	 */
	public TripTempro (VehicleType vehicle, EngineType engine, Route route, TimeOfDay hour, Integer originTemproZoneID, Integer destinationTemproZoneID, Zoning zoning, int multiplier) {
		
		super(vehicle, engine, route, hour, originTemproZoneID, destinationTemproZoneID, multiplier);
		this.zoning = zoning;
	}
	
	
//	There should be mapping from Tempro to LAD zones in the Zoning class	
//	/**
//	 * Gets trip origin zone (LAD).
//	 * @param nodeToZoneMap Mapping from nodes to zones.
//	 * @return Trip origin zone.
//	 */
//	public String getOriginLAD(Map<Integer, String> nodeToZoneMap) {
//		
//		int originNode = this.getOriginNode().getID();
//		return nodeToZoneMap.get(originNode);
//	}
//	
//	
//	/**
//	 * Gets trip destination zone (LAD).
//	 * @param nodeToZoneMap Mapping from nodes to zones.
//	 * @return Trip destination zone.
//	 */
//	public String getDestinationLAD(Map<Integer, String> nodeToZoneMap) {
//		
//		int destinationNode = this.getDestinationNode().getID();
//		return nodeToZoneMap.get(destinationNode);
//	}
	
	/**
	 * Gets trip origin tempro zone code.
	 * @return Trip origin tempro zone code.
	 */
	public String getOriginTemproZone() {

		String temproCode = this.zoning.getZoneIDToCodeMap().get(this.origin);
		return temproCode;
	}
	
	/**
	 * Gets trip destination tempro zone.
	 * @return Trip destination tempro zone.
	 */
	public String getDestinationTemproZone() {

		String temproCode = this.zoning.getZoneIDToCodeMap().get(this.destination);
		return temproCode;
	}
	
	public double getLength() {
    	
		Double length = this.route.getLength(); //route length is not changing so if can be calculated only once and stored
		if (length == null) {
			this.route.calculateLength();
			length = this.route.getLength();
		}
		Double access = this.zoning.getZoneToNearestNodeDistanceMap().get(this.getOriginTemproZone());
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = this.zoning.getZoneToNearestNodeDistanceMap().get(this.getDestinationTemproZone());
		if (egress == null) egress = 0.0;
		
		return length + access / 1000 + egress / 1000;
	}
	
	@Override
	public double getTravelTime(Map<Integer, Double> linkTravelTime, double avgIntersectionDelay, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed) {
		
		//Double time = this.route.getTime();
		//if (time == null) {
			this.route.calculateTravelTime(linkTravelTime, avgIntersectionDelay); //route travel time needs to be recalculated every time (as it depends on time of day).
			Double time = this.route.getTime();
		//}
		Double access = distanceFromTemproZoneToNearestNode.get(this.origin);
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = distanceFromTemproZoneToNearestNode.get(this.destination);
		if (egress == null) egress = 0.0;
		double averageAccessTime = access / 1000 / averageAccessEgressSpeed * 60;
		double averageEgressTime = egress / 1000 / averageAccessEgressSpeed * 60;
		
		return time + averageAccessTime + averageEgressTime;
	}
	
	@Override
	public double getCost(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, HashMap<EngineType, Double> energyUnitCosts, HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions, HashMap<String, MultiKeyMap> congestionCharges) {
		
		//double distance = this.getLength(averageAccessEgressMap);
		//double cost = distance / 100 * energyConsumptionsPer100km.get(this.engine) * energyUnitCosts.get(this.engine);
		
		//fetch congestion charge for the vehicle type
		HashMap<String, HashMap<Integer, Double>> linkCharges = null;
		if (congestionCharges != null) 
			for (String policyName: congestionCharges.keySet())
				linkCharges.put(policyName, (HashMap<Integer, Double>) congestionCharges.get(policyName).get(this.vehicle, this.hour));
				
		this.route.calculateCost(linkTravelTime, energyConsumptions.get(Pair.of(this.vehicle, this.engine)), energyUnitCosts.get(this.engine), linkCharges);
		double tripCost = this.route.getCost();
		
		//TODO add access/egress cost
				
		return tripCost;
	}
	
	@Override
	public double getConsumption(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions) {
		
		//double distance = this.getLength(averageAccessEgressMap);
		//double consumption = distance / 100 * energyConsumptionsPer100km.get(this.engine);
		
		double consumption = this.route.calculateConsumption(linkTravelTime, energyConsumptions.get(Pair.of(this.vehicle, this.engine)));
		
		//TODO add access/egress consumption
		
		return consumption;
	}
}
