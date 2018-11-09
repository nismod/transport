package nismod.transport.network.road;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;

import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.zone.Zoning;

/**
 * This class stores information about a performed trip (when using the Tempro zoning system). 
 * @author Milan Lovric
 *
 */
public class TripTempro extends Trip {
	
	private final static Logger LOGGER = LogManager.getLogger(TripTempro.class);
	
	public static Zoning zoning;

		
	/**
	 * Constructor for a passenger car trip using the Tempro zoning system.
	 * Origin and destination are integer IDs of Tempro zones.
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
		//if (TripTempro.zoning == null)
		TripTempro.zoning = zoning;
	}
	
	/**
	 * Constructor for a passenger car trip using the Tempro zoning system.
	 * Origin and destination are integer IDs of Tempro zones.
	 * Multiplier is used to store multiple instances of the same trip (vs creating multiple objects), thus reducing the memory footprint.
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
		//if (TripTempro.zoning == null) 
		TripTempro.zoning = zoning;
	}
	
	/**
	 * Gets trip origin zone (LAD), from tempro to LAD mapping (not from route nodes).
	 * @return Trip origin zone.
	 */
	public String getOriginLAD() {
		
		String originTemproZone = TripTempro.zoning.getZoneIDToCodeMap().get(this.origin);
		String originLADZone = TripTempro.zoning.getZoneToLADMap().get(originTemproZone);
		return originLADZone;
	}
	
	
	/**
	 * Gets trip destination zone (LAD), from tempro to LAD mapping (not from route nodes).
	 * @return Trip destination zone.
	 */
	public String getDestinationLAD() {
		
		String destinationTemproZone = TripTempro.zoning.getZoneIDToCodeMap().get(this.destination);
		String destinationLADZone = TripTempro.zoning.getZoneToLADMap().get(destinationTemproZone);
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

		String temproCode = TripTempro.zoning.getZoneIDToCodeMap().get(this.origin);
		return temproCode;
	}
	
	/**
	 * Gets trip destination tempro zone.
	 * @return Trip destination tempro zone.
	 */
	public String getDestinationTemproZone() {

		String temproCode = TripTempro.zoning.getZoneIDToCodeMap().get(this.destination);
		return temproCode;
	}
	
	/**
	 * Gets trip length including access/egress (from Tempro centroid to node).
	 * @return Trip length [in km].
	 */
	public double getLength() {
    	
		double length = this.route.getLength(); //route length is not changing so it can be calculated only once and stored
		if (length == 0.0) {
			this.route.calculateLength();
			length = this.route.getLength();
		}
		Double access = TripTempro.zoning.getZoneToNearestNodeDistanceMap().get(this.getOriginTemproZone());
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = TripTempro.zoning.getZoneToNearestNodeDistanceMap().get(this.getDestinationTemproZone());
		if (egress == null) egress = 0.0;
		
		return length + access / 1000 + egress / 1000;
	}
	
	@Override
	public double getTravelTime(Map<Integer, Double> linkTravelTime, double avgIntersectionDelay, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, boolean flagIncludeAccessEgress) {
		
		this.route.calculateTravelTime(linkTravelTime, avgIntersectionDelay); //route travel time needs to be recalculated every time (as it depends on time of day).
		Double time = this.route.getTime();

		if (flagIncludeAccessEgress) {
			Double access = distanceFromTemproZoneToNearestNode.get(this.origin);
			if (access == null) access = 0.0; //TODO use some default access/egress distances?
			Double egress = distanceFromTemproZoneToNearestNode.get(this.destination);
			if (egress == null) egress = 0.0;
			double averageAccessTime = access / 1000 / averageAccessEgressSpeed * 60;
			double averageEgressTime = egress / 1000 / averageAccessEgressSpeed * 60;

			time += averageAccessTime + averageEgressTime;
		}

		return time;
	}
	
	@Override
	public double getCost(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, Map<EnergyType, Double> energyUnitCosts, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, HashMap<String, MultiKeyMap> congestionCharges, boolean flagIncludeAccessEgress) {
		
		//double distance = this.getLength(averageAccessEgressMap);
		//double cost = distance / 100 * energyConsumptionsPer100km.get(this.engine) * energyUnitCosts.get(this.engine);
		
		//fetch congestion charge for the vehicle type
		HashMap<String, HashMap<Integer, Double>> linkCharges = new HashMap<String, HashMap<Integer, Double>>();
		if (congestionCharges != null) 
			for (String policyName: congestionCharges.keySet())
				linkCharges.put(policyName, (HashMap<Integer, Double>) congestionCharges.get(policyName).get(this.vehicle, this.hour));
				
		this.route.calculateCost(this.vehicle, this.engine, linkTravelTime, energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, linkCharges);
		double tripCost = this.route.getCost();
		
		//add access/egress cost
		if (flagIncludeAccessEgress) {
			Map<EnergyType, Double> accessEgressConsumptions = this.getAccessEgressConsumption(linkTravelTime, distanceFromTemproZoneToNearestNode, averageAccessEgressSpeed, energyConsumptionParameters, relativeFuelEfficiency);
			for (EnergyType et: EnergyType.values()) {
				Double accessEgressConsumption = accessEgressConsumptions.get(et);
				if (accessEgressConsumption == null) accessEgressConsumption = 0.0;
				tripCost += accessEgressConsumption * energyUnitCosts.get(et);
			}
		}
				
		return tripCost;
	}
	
	@Override
	public Map<EnergyType, Double> getConsumption(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, boolean flagIncludeAccessEgress) {

		//double distance = this.getLength(averageAccessEgressMap);
		//double consumption = distance / 100 * energyConsumptionsPer100km.get(this.engine);

		//get consumptions on the route itself
		Map<EnergyType, Double> tripConsumptions = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptionParameters, relativeFuelEfficiency);

		//add access/egress consumption
		if (flagIncludeAccessEgress) {
			Map<EnergyType, Double> accessEgressConsumptions = this.getAccessEgressConsumption(linkTravelTime, distanceFromTemproZoneToNearestNode, averageAccessEgressSpeed, energyConsumptionParameters, relativeFuelEfficiency);
			for (EnergyType et: EnergyType.values()) {
				Double tripConsumption = tripConsumptions.get(et);
				if (tripConsumption == null) tripConsumption = 0.0;
				Double accessEgressConsumption = accessEgressConsumptions.get(et);
				if (accessEgressConsumption == null) accessEgressConsumption = 0.0;
				tripConsumptions.put(et, tripConsumption + accessEgressConsumption);
			}
		}
		
		return tripConsumptions;
	}
	
	@Override
	protected Map<EnergyType, Double> getAccessEgressConsumption(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency) {
			
		//double distance = this.getLength(averageAccessEgressMap);
		//double consumption = distance / 100 * energyConsumptionsPer100km.get(this.engine);

		//get consumptions on the route itself
		Map<EnergyType, Double> accessEgressConsumptions = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptionParameters, relativeFuelEfficiency);

		//parameters
		Map<WebTAG, Double> parameters = null, parametersFuel = null, parametersElectricity = null;
		//consumptions
		double consumption = 0.0, fuelConsumption = 0.0, electricityConsumption = 0.0;
		//relative efficiencies
		double relativeEfficiency = 0.0, relativeEfficiencyFuel = 0.0, relativeEfficiencyElectricity = 0.0;

		//fetch the right parameters and existing consumptions
		//PHEVs are more complicated as they have a combination of fuel and electricity consumption
		if (this.engine == EngineType.PHEV_PETROL) {
			parametersFuel = energyConsumptionParameters.get(this.vehicle).get(EngineType.ICE_PETROL);
			parametersElectricity = energyConsumptionParameters.get(this.vehicle).get(EngineType.BEV);
			relativeEfficiencyFuel = relativeFuelEfficiency.get(this.vehicle).get(EngineType.ICE_PETROL);
			relativeEfficiencyElectricity = relativeFuelEfficiency.get(this.vehicle).get(EngineType.BEV);

		} else if (this.engine == EngineType.PHEV_DIESEL) {
			parametersFuel = energyConsumptionParameters.get(this.vehicle).get(EngineType.ICE_DIESEL);
			parametersElectricity = energyConsumptionParameters.get(this.vehicle).get(EngineType.BEV);
			relativeEfficiencyFuel = relativeFuelEfficiency.get(this.vehicle).get(EngineType.ICE_DIESEL);
			relativeEfficiencyElectricity = relativeFuelEfficiency.get(this.vehicle).get(EngineType.BEV);

		} else { //all other engine types have only one type of energy consumption (either fuel or electricity)
			parameters = energyConsumptionParameters.get(this.vehicle).get(this.engine);
			relativeEfficiency = relativeFuelEfficiency.get(this.vehicle).get(this.engine);
		}

		//get access and egress
		Double access = distanceFromTemproZoneToNearestNode.get(this.origin);
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = distanceFromTemproZoneToNearestNode.get(this.destination);
		if (egress == null) egress = 0.0;

		//for PHEVs it is more complicated (depends on urban/rural road type
		//it is assumed electricity is used on urban road links and fuel on rural road links
		if (this.engine == EngineType.PHEV_PETROL || this.engine == EngineType.PHEV_DIESEL) { 

			//calculate consumption for access
			Boolean isUrban = null;
			if (!this.route.getEdges().isEmpty()) { //if there is an edge list
				int firstEdgeID = this.route.getEdges().get(0);
				DirectedEdge firstEdge = (DirectedEdge) this.route.getRoadNetwork().getEdgeIDtoEdge().get(firstEdgeID);
				isUrban = this.route.getRoadNetwork().getIsEdgeUrban().get(firstEdgeID);

				//if no roadCategory information (e.g. ferry) use urban/rural classification of related edge
				if (isUrban == null) {
					DirectedNode nodeA = (DirectedNode)firstEdge.getNodeA();
					List<Edge> inEdges = nodeA.getInEdges();
					for (Edge e: inEdges)
						if (this.route.getRoadNetwork().getIsEdgeUrban().get(e.getID()) != null) {
							isUrban = this.route.getRoadNetwork().getIsEdgeUrban().get(e.getID()); //use information of first related edge with urban/rural information
							break;
						}
				}
			} else { //edge list is empty, so route is a single node
				DirectedNode nodeA = this.route.getOriginNode();
				List<Edge> inEdges = nodeA.getInEdges();
				for (Edge e: inEdges)
					if (this.route.getRoadNetwork().getIsEdgeUrban().get(e.getID()) != null) {
						isUrban = this.route.getRoadNetwork().getIsEdgeUrban().get(e.getID()); //use information of first related edge with urban/rural information
						break;
					}
			}
			if (isUrban == null)
				LOGGER.error("It was not possible to determine whether access was urban or rural.");
			//if the first edge is urban use electricity for access, otherwise (rural) use fuel for access
			if (isUrban)
				electricityConsumption += (access / 1000) * (parametersElectricity.get(WebTAG.A) / averageAccessEgressSpeed + parametersElectricity.get(WebTAG.B) + parametersElectricity.get(WebTAG.C) * averageAccessEgressSpeed + parametersElectricity.get(WebTAG.D) * averageAccessEgressSpeed  * averageAccessEgressSpeed);
			else
				fuelConsumption += (access / 1000) * (parametersFuel.get(WebTAG.A) / averageAccessEgressSpeed + parametersFuel.get(WebTAG.B) + parametersFuel.get(WebTAG.C) * averageAccessEgressSpeed + parametersFuel.get(WebTAG.D) * averageAccessEgressSpeed  * averageAccessEgressSpeed);

			//calculate consumption for egress
			isUrban = null;
			if (!this.route.getEdges().isEmpty()) { //if there is an edge list
				int lastEdgeID = this.route.getEdges().get(this.route.getEdges().size()-1);
				DirectedEdge lastEdge = (DirectedEdge) this.route.getRoadNetwork().getEdgeIDtoEdge().get(lastEdgeID);
				isUrban = this.route.getRoadNetwork().getIsEdgeUrban().get(lastEdgeID);

				//if no roadCategory information (e.g. ferry) use urban/rural classification of related edge
				if (isUrban == null) {
					DirectedNode nodeB = (DirectedNode)lastEdge.getNodeB();
					List<Edge> outEdges = nodeB.getOutEdges();
					for (Edge e: outEdges)
						if (this.route.getRoadNetwork().getIsEdgeUrban().get(e.getID()) != null) {
							isUrban = this.route.getRoadNetwork().getIsEdgeUrban().get(e.getID()); //use information of first related edge with urban/rural information
							break;
						}
				}
			} else { //edge list is empty, so route is a single node
				DirectedNode nodeB = this.route.getDestinationNode();
				List<Edge> outEdges = nodeB.getOutEdges();
				for (Edge e: outEdges)
					if (this.route.getRoadNetwork().getIsEdgeUrban().get(e.getID()) != null) {
						isUrban = this.route.getRoadNetwork().getIsEdgeUrban().get(e.getID()); //use information of first related edge with urban/rural information
						break;
					}
			}
			if (isUrban == null)
				LOGGER.error("It was not possible to determine whether egress was urban or rural.");
			//if the last edge is urban use electricity for access, otherwise (rural) use fuel for access
			if (isUrban)
				electricityConsumption += (egress / 1000) * (parametersElectricity.get(WebTAG.A) / averageAccessEgressSpeed + parametersElectricity.get(WebTAG.B) + parametersElectricity.get(WebTAG.C) * averageAccessEgressSpeed + parametersElectricity.get(WebTAG.D) * averageAccessEgressSpeed  * averageAccessEgressSpeed);
			else
				fuelConsumption += (egress / 1000) * (parametersFuel.get(WebTAG.A) / averageAccessEgressSpeed + parametersFuel.get(WebTAG.B) + parametersFuel.get(WebTAG.C) * averageAccessEgressSpeed + parametersFuel.get(WebTAG.D) * averageAccessEgressSpeed  * averageAccessEgressSpeed);

			//apply relative fuel efficiency
			electricityConsumption *= relativeEfficiencyElectricity;
			fuelConsumption *= relativeEfficiencyFuel;

		} else { //other engine types
			consumption += (access + egress) / 1000 * (parameters.get(WebTAG.A) / averageAccessEgressSpeed + parameters.get(WebTAG.B) + parameters.get(WebTAG.C) * averageAccessEgressSpeed + parameters.get(WebTAG.D) * averageAccessEgressSpeed  * averageAccessEgressSpeed);
			//apply relative fuel efficiency
			consumption *= relativeEfficiency;
		}

		if (this.engine == EngineType.PHEV_PETROL) {
			accessEgressConsumptions.put(EnergyType.ELECTRICITY, electricityConsumption);
			accessEgressConsumptions.put(EnergyType.PETROL, fuelConsumption);

		} else if (this.engine == EngineType.PHEV_DIESEL) {
			accessEgressConsumptions.put(EnergyType.ELECTRICITY, electricityConsumption);
			accessEgressConsumptions.put(EnergyType.DIESEL, fuelConsumption);

		} else if (this.engine == EngineType.ICE_PETROL || this.engine == EngineType.HEV_PETROL) {
			accessEgressConsumptions.put(EnergyType.PETROL, consumption);

		} else if (this.engine == EngineType.ICE_DIESEL || this.engine == EngineType.HEV_DIESEL) {
			accessEgressConsumptions.put(EnergyType.DIESEL, consumption);

		} else if (this.engine == EngineType.ICE_CNG) {
			accessEgressConsumptions.put(EnergyType.CNG, consumption);

		} else if (this.engine == EngineType.ICE_H2) {
			accessEgressConsumptions.put(EnergyType.HYDROGEN, consumption);

		}	else if (this.engine == EngineType.BEV) {
			accessEgressConsumptions.put(EnergyType.ELECTRICITY, consumption);
		}

		return accessEgressConsumptions;
	}
	
	@Override
	public Double getCO2emission(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> distanceFromTemproZoneToNearestNode, double averageAccessEgressSpeed, Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters, Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency, Map<EnergyType, Double> unitCO2Emissions, boolean flagIncludeAccessEgress) {
		
		//Map<EnergyType, Double> consumption = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptions, relativeFuelEfficiency);
		Map<EnergyType, Double> consumption = this.getConsumption(linkTravelTime, distanceFromTemproZoneToNearestNode, averageAccessEgressSpeed, energyConsumptionParameters, relativeFuelEfficiency, flagIncludeAccessEgress);
		
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
		
		return TripTempro.zoning;
	}
}
