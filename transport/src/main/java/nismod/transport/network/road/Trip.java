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
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;

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
	 * Constructor for an LAD-based trip. Origin and destination fields are used for freight trips (according to DfT's BYFM zonal coding).
	 * Origin and destination for passenger car/AV trips are 0 as their correct origin and destination LAD zone can be 
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
		if (vehicle == VehicleType.CAR || vehicle == VehicleType.CAR_AV)
			if (origin != 0 || destination != 0)
				System.err.println("Origin and destination for non-freight trips must be 0 as their ODs should be fetched from the route.");
		*/
	}
	
	/**
	 * Constructor for a trip. Origin and destination are used for freight trips (according to DfT's BYFM zonal coding).
	 * Origin and destination for passenger car/AV trips are 0 as their correct origin and destination LAD zone can be 
	 * obtained using the first and the last node of the route.
	 * Multiplier is used to store multiple instances of the same trip (vs creating multiple objects), thus reducing the memory footprint.
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
		if (vehicle == VehicleType.CAR || vehicle == VehicleType.CAR_AV)
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
	
	/**
	 * Get trip length including access/egress.
	 * @param averageAccessEgressMap Mapping between nodeID and average access/egress for that node.
	 * @return Trip length including access/egress [in km]
	 */
	public double getLength(HashMap<Integer, Double> averageAccessEgressMap) {

		double length = this.route.getLength(); //route length is not changing so it can be calculated only once and stored
		if (length == 0.0) {
			this.route.calculateLength();
			length = this.route.getLength();
		}
		Double access = averageAccessEgressMap.get(this.getOriginNode().getID());
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = averageAccessEgressMap.get(this.getDestinationNode().getID());
		if (egress == null) egress = 0.0;
		
		return length + access / 1000 + egress / 1000;
	}
	
	/**
	 * Calculates travel time including access/egress.
	 * @param linkTravelTime Link-based travel time (should be for the same hour as the trip's time of day).
	 * @param avgIntersectionDelay Average intersection delay.
	 * @param averageAccessEgressMap Mapping between nodeID and average access/egress for that node. 
	 * @param averageAccessEgressSpeed Average access/egress speed.
	 * @param flagIncludeAccessEgress Whether to include access/egress travel time.
	 * @return Trip travel time including access/egress [in min].
	 */
	public double getTravelTime(Map<Integer, Double> linkTravelTime, double avgIntersectionDelay, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed, boolean flagIncludeAccessEgress) {
		
		this.route.calculateTravelTime(linkTravelTime, avgIntersectionDelay); //route travel time needs to be recalculated every time (as it depends on time of day).
		Double time = this.route.getTime();
			
			if (flagIncludeAccessEgress) {	
				Double access = averageAccessEgressMap.get(this.getOriginNode().getID());
				if (access == null) access = 0.0; //TODO use some default access/egress distances?
				Double egress = averageAccessEgressMap.get(this.getDestinationNode().getID());
				if (egress == null) egress = 0.0;
				double averageAccessTime = access / 1000 / averageAccessEgressSpeed * 60;
				double averageEgressTime = egress / 1000 / averageAccessEgressSpeed * 60;
				time += averageAccessTime + averageEgressTime;
			}

			return time;
	}
	
	/**
	 * Calculate cost of the trip (fuel cost + congestion charge, if any).
	 * @param linkTravelTime Link travel time.
	 * @param averageAccessEgressMap Average access/egress distance to a node for LAD-based trips.
	 * @param averageAccessEgressSpeed Average access/egress speed.
	 * @param energyUnitCosts Energy unit costs.
	 * @param energyConsumptionParameters Energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency.
	 * @param congestionCharges Congestion charges.
	 * @param boolean flagIncludeAccessEgress Whether to include access/egress.
	 * @return Total trip cost.
	 */
	public double getCost(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed, Map<EnergyType, Double> energyUnitCosts, HashMap<Pair<VehicleType, EngineType>, Map<WebTAG, Double>> energyConsumptionParameters, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency, HashMap<String, MultiKeyMap> congestionCharges, boolean flagIncludeAccessEgress) {
		
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
			Map<EnergyType, Double> accessEgressConsumptions = this.getAccessEgressConsumption(linkTravelTime, averageAccessEgressMap, averageAccessEgressSpeed, energyConsumptionParameters, relativeFuelEfficiency);
			for (EnergyType et: EnergyType.values()) {
				Double accessEgressConsumption = accessEgressConsumptions.get(et);
				if (accessEgressConsumption == null) accessEgressConsumption = 0.0;
				tripCost += accessEgressConsumption * energyUnitCosts.get(et);
			}
		}
				
		return tripCost;
	}
	
	/**
	 * Calculate trip consumption including access and egress.
	 * @param linkTravelTime Link travel time.
	 * @param averageAccessEgressMap Average access/egress distance to a node for LAD-based trips.
	 * @param averageAccessEgressSpeed Average acess/egress speed.
	 * @param energyConsumptionParameters Energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency.
	 * @param boolean flagIncludeAccessEgress Whether to include access/egress.
	 * @return Trip consumptions.
	 */
	public Map<EnergyType, Double> getConsumption(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed, HashMap<Pair<VehicleType, EngineType>, Map<WebTAG, Double>> energyConsumptionParameters, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency, boolean flagIncludeAccessEgress) {
		
		//double distance = this.getLength(averageAccessEgressMap);
		//double consumption = distance / 100 * energyConsumptionsPer100km.get(this.engine);

		//get consumptions on the route itself
		Map<EnergyType, Double> tripConsumptions = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptionParameters, relativeFuelEfficiency);

		//add access/egress consumption
		if (flagIncludeAccessEgress) {
			Map<EnergyType, Double> accessEgressConsumptions = this.getAccessEgressConsumption(linkTravelTime, averageAccessEgressMap, averageAccessEgressSpeed, energyConsumptionParameters, relativeFuelEfficiency);
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
	
	/**
	 * Calculate trip consumption only on access and egress.
	 * @param linkTravelTime
	 * @param averageAccessEgressMap
	 * @param averageAccessEgressSpeed
	 * @param energyConsumptions
	 * @param relativeFuelEfficiency
	 * @return Trip consumptions.
	 */
	protected Map<EnergyType, Double> getAccessEgressConsumption(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed, HashMap<Pair<VehicleType, EngineType>, Map<WebTAG, Double>> energyConsumptionParameters, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency) {
		
		//double distance = this.getLength(averageAccessEgressMap);
		//double consumption = distance / 100 * energyConsumptionsPer100km.get(this.engine);

		Map<EnergyType, Double> accessEgressConsumptions = new EnumMap<>(EnergyType.class);

		//parameters
		Map<WebTAG, Double> parameters = null, parametersFuel = null, parametersElectricity = null;
		//consumptions
		double consumption = 0.0, fuelConsumption = 0.0, electricityConsumption = 0.0;
		//relative efficiencies
		double relativeEfficiency = 0.0, relativeEfficiencyFuel = 0.0, relativeEfficiencyElectricity = 0.0;

		//fetch the right parameters and existing consumptions
		//PHEVs are more complicated as they have a combination of fuel and electricity consumption
		if (this.engine == EngineType.PHEV_PETROL) {
			parametersFuel = energyConsumptionParameters.get(Pair.of(this.vehicle, EngineType.ICE_PETROL));
			parametersElectricity = energyConsumptionParameters.get(Pair.of(this.vehicle, EngineType.BEV));
			relativeEfficiencyFuel = relativeFuelEfficiency.get(Pair.of(this.vehicle, EngineType.ICE_PETROL));
			relativeEfficiencyElectricity = relativeFuelEfficiency.get(Pair.of(this.vehicle, EngineType.BEV));

		} else if (this.engine == EngineType.PHEV_DIESEL) {
			parametersFuel = energyConsumptionParameters.get(Pair.of(this.vehicle, EngineType.ICE_DIESEL));
			parametersElectricity = energyConsumptionParameters.get(Pair.of(this.vehicle, EngineType.BEV));
			relativeEfficiencyFuel = relativeFuelEfficiency.get(Pair.of(this.vehicle, EngineType.ICE_DIESEL));
			relativeEfficiencyElectricity = relativeFuelEfficiency.get(Pair.of(this.vehicle, EngineType.BEV));

		} else { //all other engine types have only one type of energy consumption (either fuel or electricity)
			parameters = energyConsumptionParameters.get(Pair.of(this.vehicle, this.engine));
			relativeEfficiency = relativeFuelEfficiency.get(Pair.of(this.vehicle, this.engine));
		}

		//get access and egress
		Double access = averageAccessEgressMap.get(this.getOriginNode().getID());
		if (access == null) access = 0.0; //TODO use some default access/egress distances?
		Double egress = averageAccessEgressMap.get(this.getDestinationNode().getID());
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
				electricityConsumption += (access / 1000) * (parametersElectricity.get(WebTAG.A) / averageAccessEgressSpeed + parametersElectricity.get(WebTAG.A) + parametersElectricity.get(WebTAG.C) * averageAccessEgressSpeed + parametersElectricity.get(WebTAG.D) * averageAccessEgressSpeed  * averageAccessEgressSpeed);
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
	
	/**
	 * Calculates total CO2 emissions per energy type.
	 * @param linkTravelTime Link travel time.
	 * @param averageAccessEgressMap Average access/egress distance to a node for LAD-based trips.
	 * @param averageAccessEgressSpeed Average accces/egress speed.
	 * @param energyConsumptionParameters Energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency.
	 * @param unitCO2Emissions Unit CO2 emissions.
	 * @param boolean flagIncludeAccessEgress Whether to include access/egress.
	 * @return CO2 emissions per energy type.
	 */
	public Double getCO2emission(Map<Integer, Double> linkTravelTime, HashMap<Integer, Double> averageAccessEgressMap, double averageAccessEgressSpeed, HashMap<Pair<VehicleType, EngineType>, Map<WebTAG, Double>> energyConsumptionParameters, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency, Map<EnergyType, Double> unitCO2Emissions, boolean flagIncludeAccessEgress) {
		
		//Map<EnergyType, Double> consumption = this.route.calculateConsumption(this.vehicle, this.engine, linkTravelTime, energyConsumptionParameters, relativeFuelEfficiency);
		Map<EnergyType, Double> consumption = this.getConsumption(linkTravelTime, averageAccessEgressMap, averageAccessEgressSpeed, energyConsumptionParameters, relativeFuelEfficiency, flagIncludeAccessEgress);
		
		double CO2 = 0.0;
		for (EnergyType et: consumption.keySet()) {
			CO2 += consumption.get(et) * unitCO2Emissions.get(et);
		}
		
		return CO2;
	}
	
	/**
	 * Check whether trip is going through a congestion charging zone for a particular policy.
	 * @param policyName Policy name.
	 * @param congestionCharges Congestion charges.
	 * @return True if it is going through the congestion charging zone.
	 */
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
