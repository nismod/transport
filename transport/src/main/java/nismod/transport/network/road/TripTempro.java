package nismod.transport.network.road;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.zone.Zoning;

/**
 * This class stores information about a performed trip (when using the Tempro zoning system). 
 * @author Milan Lovric
 *
 */
public class TripTempro extends Trip {
	
	private final static Logger LOGGER = LogManager.getLogger(TripTempro.class);
	
	private static Zoning zoning;
		
	/**
	 * Constructor for a trip. Origin and destination are used for freight trips (according to DfT's BYFM zonal coding).
	 * Origin and destination for passenger car/AV trips are 0 as their correct origin and destination zone can be 
	 * obtained using the first and the last node of the route.
	 * @param vehicle Vehicle type.
	 * @param engine Engine type.
	 * @param route Route.
	 * @param hour Time of day.
	 * @param originTemproZoneID Origin tempro zone ID.
	 * @param destinationTemproZoneID Destination tempro zone ID.
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
	 * @param originTemproZoneID Origin tempro zone ID.
	 * @param destinationTemproZoneID Destination tempro zone ID.
	 * @param zoning Zoning system.
	 * @param multiplier Multiplies the same trip.
	 */
	public TripTempro (VehicleType vehicle, EngineType engine, Route route, TimeOfDay hour, Integer originTemproZoneID, Integer destinationTemproZoneID, Zoning zoning, int multiplier) {
		
		super(vehicle, engine, route, hour, originTemproZoneID, destinationTemproZoneID, multiplier);
		this.zoning = zoning;
	}
	
	/**
	 * Gets trip origin zone (LAD), from tempro to LAD mapping (not from route nodes).
	 * @return Trip origin zone.
	 */
	public String getOriginLAD() {
		
		String originTemproZone = this.zoning.getZoneIDToCodeMap().get(this.origin);
		String originLADZone = this.zoning.getZoneToLADMap().get(originTemproZone);
		return originLADZone;
	}
	
	
	/**
	 * Gets trip destination zone (LAD), from tempro to LAD mapping (not from route nodes).
	 * @return Trip destination zone.
	 */
	public String getDestinationLAD() {
		
		String destinationTemproZone = this.zoning.getZoneIDToCodeMap().get(this.destination);
		String destinationLADZone = this.zoning.getZoneToLADMap().get(destinationTemproZone);
		return destinationLADZone;
	}
	
	/**
	 * Gets trip origin zone (LAD), from tempro to LAD mapping (not from route nodes).
	 * @param nodeToZoneMap Mapping from nodes to zones.
	 * @return Trip origin zone.
	 */
	@Override
	public String getOriginLAD(Map<Integer, String> nodeToZoneMap) {
		
		return this.getOriginLAD();
	}
	
	
	/**
	 * Gets trip destination zone (LAD), from tempro to LAD mapping (not from route nodes).
	 * @param nodeToZoneMap Mapping from nodes to zones.
	 * @return Trip destination zone.
	 */
	@Override
	public String getDestinationLAD(Map<Integer, String> nodeToZoneMap) {
		
		return this.getDestinationLAD();
	}
	
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
	public double getCost(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, HashMap<EnergyType, Double> energyUnitCosts, HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency, HashMap<String, MultiKeyMap> congestionCharges) {
		
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
	
	@Override
	public HashMap<EnergyType, Double> getConsumption(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency) {
		
		//double distance = this.getLength(averageAccessEgressMap);
		//double consumption = distance / 100 * energyConsumptionsPer100km.get(this.engine);
		
		HashMap<EnergyType, Double> consumption = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptions, relativeFuelEfficiency);
		
		//TODO add access/egress consumption
		
		return consumption;
	}
	
	@Override
	public Double getCO2emission(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> energyConsumptions, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency, HashMap<EnergyType, Double> unitCO2Emissions) {
		
		HashMap<EnergyType, Double> consumption = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptions, relativeFuelEfficiency);
		
		double CO2 = 0.0;
		for (EnergyType et: consumption.keySet()) {
			
			CO2 += consumption.get(et) * unitCO2Emissions.get(et);
		}
		
		return CO2;
	}
	
	/**
	 * Getter for the zoning system.
	 * @return Zoning.
	 */
	public Zoning getZoning() {
		
		return this.zoning;
	}
}
