package nismod.transport.network.road;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.decision.PricingPolicy;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.zone.Zoning;

/**
 * This class stores information about a performed trip on minor roads (for which the network is not modelled).
 * @author Milan Lovric
 *
 */
public class TripMinor extends Trip {
	
	private final static Logger LOGGER = LogManager.getLogger(TripMinor.class);
	
	private double length; //trip length in [km]
	
	public static Zoning zoning;

		
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
	 * @param length Trip length;
	 * @param zoning Zoning system.
	 */
	public TripMinor (VehicleType vehicle, EngineType engine, TimeOfDay hour, Integer originTemproZoneID, Integer destinationTemproZoneID, double length, Zoning zoning) {
		
		super(vehicle, engine, null, hour, originTemproZoneID, destinationTemproZoneID);
		this.length = length;
		//if (TripTempro.zoning == null)
		TripMinor.zoning = zoning;
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
	 * @param length Trip length;
	 * @param zoning Zoning system.
	 * @param multiplier Multiplies the same trip.
	 */
	public TripMinor (VehicleType vehicle, EngineType engine, TimeOfDay hour, Integer originTemproZoneID, Integer destinationTemproZoneID, double length, Zoning zoning, int multiplier) {
		
		super(vehicle, engine, null, hour, originTemproZoneID, destinationTemproZoneID, multiplier);
		this.length = length;
		//if (TripTempro.zoning == null) 
		TripMinor.zoning = zoning;
	}
	
	/**
	 * Gets trip origin zone (LAD), from tempro to LAD mapping (not from route nodes).
	 * @return Trip origin zone.
	 */
	public String getOriginLAD() {
		
		int originLadID = TripTempro.zoning.getZoneIDToLadID()[this.origin];
		String originLADCode = TripTempro.zoning.getLadIDToCodeMap()[originLadID];
		
		return originLADCode;
	}
	
	
	/**
	 * Gets trip destination zone (LAD), from tempro to LAD mapping (not from route nodes).
	 * @return Trip destination zone.
	 */
	public String getDestinationLAD() {
				
		int destinationLadID = TripTempro.zoning.getZoneIDToLadID()[this.destination];
		String destinationLADCode = TripTempro.zoning.getLadIDToCodeMap()[destinationLadID];
		
		return destinationLADCode;
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
	 * Gets trip origin LAD zone ID.
	 * @return Origin zone LAD ID.
	 */
	@Override
	public int getOriginLadID() {

		int originLadID = TripTempro.zoning.getZoneIDToLadID()[this.origin];
		
		return originLadID;
	}
	
	/**
	 * Gets trip destination LAD zone ID.
	 * @return Trip destination zone LAD ID.
	 */
	@Override
	public int getDestinationLadID() {
		
		int destinationLadID = TripTempro.zoning.getZoneIDToLadID()[this.destination];

		return destinationLadID;
	}
	
	/**
	 * Gets trip origin tempro zone code.
	 * @return Trip origin tempro zone code.
	 */
	public String getOriginTemproZone() {

		String temproCode = TripMinor.zoning.getTemproIDToCodeMap()[this.origin];
		return temproCode;
	}
	
	/**
	 * Gets trip destination tempro zone.
	 * @return Trip destination tempro zone.
	 */
	public String getDestinationTemproZone() {

		String temproCode = TripMinor.zoning.getTemproIDToCodeMap()[this.destination];
		return temproCode;
	}
	
	/**
	 * Gets trip length (no separate access/egress for minor road trips).
	 * @return Trip length [in km].
	 */
	public double getLength() {
    	
		return this.length;
	}
	
	/**
	 * Gets travel time for the minor trip.
	 * @param averageSpeed Average speed for a minor trip.
	 * @return Travel time.
	 */
	public double getTravelTime(double averageSpeed) {
		
		return this.getTravelTime(null, 0, null, averageSpeed, false);
	}
	
	@Override
	public double getTravelTime(double[] linkTravelTime, double avgIntersectionDelay, double[] distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, boolean flagIncludeAccessEgress) {
		
		double time = this.length / averageAccessEgressSpeed * 60; // in [min]
		return time;
	}
	
	/**
	 * Gets fuel cost for the minor trip.
	 * @param averageSpeed Average speed for a minor trip.
	 * @param energyUnitCosts Energy unit costs.
	 * @param energyConsumptionParameters Energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency.
	 * @return Energy consumptions for the trip.
	 */
	public double getCost(double averageSpeed, Map<EnergyType, Double> energyUnitCosts, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency) {
		
		return this.getCost(null, null, averageSpeed, energyUnitCosts, energyConsumptionParameters, relativeFuelEfficiency, null, false);
	}
	
	@Override
	public double getCost(double[] linkTravelTime, double[] distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, Map<EnergyType, Double> energyUnitCosts, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, List<PricingPolicy> congestionCharges, boolean flagIncludeAccessEgress) {
		
		Map<EnergyType, Double> tripConsumptions = this.getConsumption(linkTravelTime, distanceFromTemproZoneToNearestNode, averageAccessEgressSpeed, energyConsumptionParameters, relativeFuelEfficiency, false);
		
		double tripCost = 0.0;
		for (EnergyType et: EnergyType.values()) {
			Double tripConsumption = tripConsumptions.get(et);
			if (tripConsumption == null) tripConsumption = 0.0;
			tripCost += tripConsumption * energyUnitCosts.get(et);
		}
					
		return tripCost;
	}
	
	/**
	 * Gets energy consumptions for the minor trip.
	 * @param averageSpeed Average speed for a minor trip.
	 * @param energyConsumptionParameters Energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency.
	 * @return Energy consumptions for the trip.
	 */
	public Map<EnergyType, Double> getConsumption(double averageSpeed, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency) {
		
		return this.getConsumption(null, null, averageSpeed, energyConsumptionParameters, relativeFuelEfficiency, false);
	}
	
	@Override
	public Map<EnergyType, Double> getConsumption(double[] linkTravelTime, double[] distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, boolean flagIncludeAccessEgress) {

		Map<EnergyType, Double> tripConsumptions = new EnumMap<>(EnergyType.class);
		for (EnergyType et: EnergyType.values())
				tripConsumptions.put(et, 0.0);
				
		//parameters
		Map<WebTAG, Double> parameters = null, parametersElectricity = null;
		//consumptions
		double consumption = 0.0, electricityConsumption = 0.0;
		//relative efficiencies
		double relativeEfficiency = 0.0, relativeEfficiencyElectricity = 0.0;

		//fetch the right parameters and existing consumptions
		//PHEVs are more complicated as they have a combination of fuel and electricity consumption
		//ASSUMPTION: assume only electricity is used on minor trips
		if (this.engine == EngineType.PHEV_PETROL || this.engine == EngineType.PHEV_DIESEL) {
			parametersElectricity = energyConsumptionParameters.get(this.vehicle).get(EngineType.BEV);
			relativeEfficiencyElectricity = relativeFuelEfficiency.get(this.vehicle).get(EngineType.BEV);
		} else { //all other engine types have only one type of energy consumption (either fuel or electricity)
			parameters = energyConsumptionParameters.get(this.vehicle).get(this.engine);
			relativeEfficiency = relativeFuelEfficiency.get(this.vehicle).get(this.engine);
		}

		if (this.engine == EngineType.PHEV_PETROL || this.engine == EngineType.PHEV_DIESEL) { 
			electricityConsumption += this.length * (parametersElectricity.get(WebTAG.A) / averageAccessEgressSpeed + parametersElectricity.get(WebTAG.B) + parametersElectricity.get(WebTAG.C) * averageAccessEgressSpeed + parametersElectricity.get(WebTAG.D) * averageAccessEgressSpeed  * averageAccessEgressSpeed);
			//apply relative fuel efficiency
			electricityConsumption *= relativeEfficiencyElectricity;
		} else { //other engine types
			consumption += this.length * (parameters.get(WebTAG.A) / averageAccessEgressSpeed + parameters.get(WebTAG.B) + parameters.get(WebTAG.C) * averageAccessEgressSpeed + parameters.get(WebTAG.D) * averageAccessEgressSpeed  * averageAccessEgressSpeed);
			//apply relative fuel efficiency
			consumption *= relativeEfficiency;
		}

		if (this.engine == EngineType.PHEV_PETROL) {
			tripConsumptions.put(EnergyType.ELECTRICITY, electricityConsumption);
		} else if (this.engine == EngineType.PHEV_DIESEL) {
			tripConsumptions.put(EnergyType.ELECTRICITY, electricityConsumption);
		} else if (this.engine == EngineType.ICE_PETROL || this.engine == EngineType.HEV_PETROL) {
			tripConsumptions.put(EnergyType.PETROL, consumption);
		} else if (this.engine == EngineType.ICE_DIESEL || this.engine == EngineType.HEV_DIESEL) {
			tripConsumptions.put(EnergyType.DIESEL, consumption);
		} else if (this.engine == EngineType.ICE_CNG) {
			tripConsumptions.put(EnergyType.CNG, consumption);
		} else if (this.engine == EngineType.ICE_LPG) {
			tripConsumptions.put(EnergyType.LPG, consumption);
		} else if (this.engine == EngineType.ICE_H2 || this.engine == EngineType.FCEV_H2) {
			tripConsumptions.put(EnergyType.HYDROGEN, consumption);
		} else if (this.engine == EngineType.BEV) {
			tripConsumptions.put(EnergyType.ELECTRICITY, consumption);
		} else
			LOGGER.warn("Unknown engine type {} detected during calculation of minor trip fuel consumption.", this.engine);
		
		return tripConsumptions;
	}
	
	@Override
	protected Map<EnergyType, Double> getAccessEgressConsumption(double[] linkTravelTime, double[] distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency) {
			
		Map<EnergyType, Double> accessEgressConsumptions = new EnumMap<>(EnergyType.class);

		return accessEgressConsumptions;
	}
	
	/**
	 * Gets CO2 emission for the minor trip.
	 * @param averageSpeed Average speed for a minor trip.
	 * @param energyConsumptionParameters Energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency.
	 * @return CO2 emission for the trip.
	 */
	public double getCO2emission(double averageSpeed, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, Map<EnergyType, Double> unitCO2Emissions) {
		
		return this.getCO2emission(null, null, averageSpeed, energyConsumptionParameters, relativeFuelEfficiency, unitCO2Emissions, false);
	}
	
	@Override
	public Double getCO2emission(double[] linkTravelTime, double[] distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, Map<EnergyType, Double> unitCO2Emissions, boolean flagIncludeAccessEgress) {
		
		Map<EnergyType, Double> consumption = this.getConsumption(null, null, averageAccessEgressSpeed, energyConsumptionParameters, relativeFuelEfficiency, false);
		
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
		
		return TripMinor.zoning;
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
		sb.append(this.length);

		return sb.toString();
	}
}
