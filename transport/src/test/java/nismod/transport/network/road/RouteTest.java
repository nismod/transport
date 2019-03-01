package nismod.transport.network.road;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.junit.Test;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.decision.Intervention;
import nismod.transport.decision.PricingPolicy;
import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.network.road.RouteSet.RouteChoiceParams;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

public class RouteTest {

	@Test
	public void test() throws IOException {
		
		final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);

		final String areaCodeFileName = props.getProperty("areaCodeFileName");
		final String areaCodeNearestNodeFile = props.getProperty("areaCodeNearestNodeFile");
		final String workplaceZoneFileName = props.getProperty("workplaceZoneFileName");
		final String workplaceZoneNearestNodeFile = props.getProperty("workplaceZoneNearestNodeFile");
		final String freightZoneToLADfile = props.getProperty("freightZoneToLADfile");
		final String freightZoneNearestNodeFile = props.getProperty("freightZoneNearestNodeFile");

		final URL zonesUrl = new URL(props.getProperty("zonesUrl"));
		final URL networkUrl = new URL(props.getProperty("networkUrl"));
		final URL networkUrlFixedEdgeIDs = new URL(props.getProperty("networkUrlFixedEdgeIDs"));
		final URL nodesUrl = new URL(props.getProperty("nodesUrl"));
		final URL AADFurl = new URL(props.getProperty("AADFurl"));
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
				
		//create routes
		Route r1 = new Route(roadNetwork);
		Route r2 = new Route(roadNetwork);
		Route r3 = new Route(roadNetwork);
		Route r4 = new Route(roadNetwork);
		
		DirectedNode n1 = (DirectedNode) roadNetwork.getNodeIDtoNode()[7];
		DirectedNode n2 = (DirectedNode) roadNetwork.getNodeIDtoNode()[8];
		DirectedNode n3 = (DirectedNode) roadNetwork.getNodeIDtoNode()[27];
		DirectedNode n4 = (DirectedNode) roadNetwork.getNodeIDtoNode()[9];
		DirectedNode n5 = (DirectedNode) roadNetwork.getNodeIDtoNode()[55];
		DirectedNode n6 = (DirectedNode) roadNetwork.getNodeIDtoNode()[40];
			
		DirectedEdge e1 = (DirectedEdge) n1.getOutEdge(n2);
		DirectedEdge e2 = (DirectedEdge) n2.getOutEdge(n4);
		DirectedEdge e3 = (DirectedEdge) n4.getOutEdge(n6);
		DirectedEdge e4 = (DirectedEdge) n4.getOutEdge(n5);
		DirectedEdge e5 = (DirectedEdge) n5.getOutEdge(n6);
		DirectedEdge e6 = (DirectedEdge) n1.getOutEdge(n3);
		DirectedEdge e7 = (DirectedEdge) n3.getOutEdge(n2);
		
		r1.addEdge(e1);
		r1.addEdge(e2);
		r1.addEdge(e3);

//		System.out.println("Route " + r1.getID() + " is valid: " + r1.isValid());
//		System.out.println("Route " + r1.getID() + ": " + r1.getEdges());
//		System.out.println("Route " + r1.getID() + ": " + r1.toString());
//		System.out.println("Route " + r1.getID() + ": " + r1.getFormattedString());
				
		//set route choice parameters
		Map<RouteChoiceParams, Double> params = new EnumMap<>(RouteChoiceParams.class);
		params.put(RouteChoiceParams.TIME, -1.5);
		params.put(RouteChoiceParams.LENGTH, -1.0);
		params.put(RouteChoiceParams.COST, -3.6);
		params.put(RouteChoiceParams.INTERSEC, -0.1);
		params.put(RouteChoiceParams.DELAY, 0.8);
		
		Map<WebTAG, Double> parameters = new EnumMap<>(WebTAG.class);
		parameters.put(WebTAG.A, 1.11932239320862);
		parameters.put(WebTAG.B, 0.0440047704089497);
		parameters.put(WebTAG.C, -0.0000813834474888197);
		parameters.put(WebTAG.D, 2.44908328418021E-06);
		
		Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> energyConsumptionParameters = new EnumMap<VehicleType, Map<EngineType, Map<WebTAG, Double>>>(VehicleType.class);
		Map<EngineType, Map<WebTAG, Double>> innerMap = new EnumMap<EngineType, Map<WebTAG, Double>>(EngineType.class);
		energyConsumptionParameters.put(VehicleType.CAR, innerMap);
		energyConsumptionParameters.get(VehicleType.CAR).put(EngineType.ICE_PETROL, parameters);
		energyConsumptionParameters.get(VehicleType.CAR).put(EngineType.BEV, parameters);
		
		Map<VehicleType, Map<EngineType, Double>> relativeFuelEfficiency = new EnumMap<VehicleType, Map<EngineType, Double>>(VehicleType.class);
		Map<EngineType, Double> engineMap = new EnumMap<EngineType, Double>(EngineType.class);
		relativeFuelEfficiency.put(VehicleType.CAR, engineMap);
		relativeFuelEfficiency.get(VehicleType.CAR).put(EngineType.ICE_PETROL, 0.9);
		relativeFuelEfficiency.get(VehicleType.CAR).put(EngineType.BEV, 0.9);
		
		Map<EnergyType, Double> energyUnitCosts = new EnumMap<>(EnergyType.class);
		energyUnitCosts.put(EnergyType.PETROL, 1.17);
		energyUnitCosts.put(EnergyType.DIESEL, 1.17);
		energyUnitCosts.put(EnergyType.CNG, 1.17);
		energyUnitCosts.put(EnergyType.LPG, 1.17);
		energyUnitCosts.put(EnergyType.HYDROGEN, 1.17);
		energyUnitCosts.put(EnergyType.ELECTRICITY, 1.17);
				
		System.out.println(energyConsumptionParameters);
		System.out.println(energyUnitCosts);
		
		r1.calculateUtility(VehicleType.CAR, EngineType.PHEV_PETROL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		
		double time = r1.getTime();
		double length = r1.getLength();
		double cost = r1.getCost();
		double intersections = r1.getNumberOfIntersections();
		double utility = r1.getUtility();
		
		System.out.println("Time: " + time);
		System.out.println("Length: " + length);
		System.out.println("Cost: " + cost);
		System.out.println("Intersections: " + intersections);
		System.out.println("Utility: " + utility);
		
		double paramTime = params.get(RouteChoiceParams.TIME);
		double paramLength = params.get(RouteChoiceParams.LENGTH);
		double paramCost = params.get(RouteChoiceParams.COST);
		double paramIntersections = params.get(RouteChoiceParams.INTERSEC);
		
		double calculatedUtility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersections;
		
		final double EPSILON = 1e-11; //may fail for higher accuracy
		
		assertEquals("Utility should be correctly calculated", utility, calculatedUtility, EPSILON);
		
		r2.addEdge(e1);
		r2.addEdge(e2);
		r2.addEdge(e4);
		r2.addEdge(e5);
		
//		System.out.println("Route " + r2.getID() + " is valid: " + r2.isValid());
//		System.out.println("Route " + r2.getID() + ": " + r2.getEdges());
//		System.out.println("Route " + r2.getID() + ": " + r2.toString());
//		System.out.println("Route " + r2.getID() + ": " + r2.getFormattedString());
				
		//set route choice parameters
		params = new EnumMap<>(RouteChoiceParams.class);
		params.put(RouteChoiceParams.TIME, -1.5);
		params.put(RouteChoiceParams.LENGTH, -1.5);
		params.put(RouteChoiceParams.COST, -3.6);
		params.put(RouteChoiceParams.INTERSEC, -0.1);
		params.put(RouteChoiceParams.DELAY, 0.8);
			
		r2.calculateUtility(VehicleType.CAR, EngineType.ICE_PETROL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		
		time = r2.getTime();
		length = r2.getLength();
		cost = r2.getCost();
		intersections = r2.getNumberOfIntersections();
		utility = r2.getUtility();
		
		System.out.println("Time: " + time);
		System.out.println("Length: " + length);
		System.out.println("Intersections: " + intersections);
		System.out.println("Utility: " + utility);
		
		paramTime = params.get(RouteChoiceParams.TIME);
		paramLength = params.get(RouteChoiceParams.LENGTH);
		paramCost = params.get(RouteChoiceParams.COST);
		paramIntersections = params.get(RouteChoiceParams.INTERSEC);
		
		calculatedUtility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersections;
		
		assertEquals("Utility should be correctly calculated", utility, calculatedUtility, EPSILON);
			
		r3.addEdge(e6);
		r3.addEdge(e7);
		r3.addEdge(e2);
		r3.addEdge(e3);
		r3.calculateUtility(VehicleType.CAR, EngineType.ICE_PETROL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		
//		System.out.println("Route " + r3.getID() + " is valid: " + r3.isValid());
//		System.out.println("Route " + r3.getID() + ": " + r3.getEdges());
//		System.out.println("Route " + r3.getID() + ": " + r3.toString());
//		System.out.println("Route " + r3.getID() + ": " + r3.getFormattedString());
				
		//set route choice parameters
		params = new EnumMap<>(RouteChoiceParams.class);
		params.put(RouteChoiceParams.TIME, -2.5);
		params.put(RouteChoiceParams.LENGTH, -1.5);
		params.put(RouteChoiceParams.COST, -3.6);
		params.put(RouteChoiceParams.INTERSEC, -0.1);
		params.put(RouteChoiceParams.DELAY, 0.8);
		
		r3.calculateUtility(VehicleType.CAR, EngineType.ICE_PETROL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		
		time = r3.getTime();
		length = r3.getLength();
		cost = r3.getCost();
		intersections = r3.getNumberOfIntersections();
		utility = r3.getUtility();
		
		System.out.println("Time: " + time);
		System.out.println("Length: " + length);
		System.out.println("Intersections: " + intersections);
		System.out.println("Utility: " + utility);
		
		paramTime = params.get(RouteChoiceParams.TIME);
		paramLength = params.get(RouteChoiceParams.LENGTH);
		paramCost = params.get(RouteChoiceParams.COST);
		paramIntersections = params.get(RouteChoiceParams.INTERSEC);
		
		calculatedUtility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersections;
		
		assertEquals("Utility should be correctly calculated", utility, calculatedUtility, EPSILON);
			
		r4.addEdge(e6);
		r4.addEdge(e7);
		r4.addEdge(e2);
		r4.addEdge(e4);
		r4.addEdge(e5);

//		System.out.println("Route " + r4.getID() + " is valid: " + r4.isValid());
//		System.out.println("Route " + r4.getID() + ": " + r4.getEdges());
//		System.out.println("Route " + r4.getID() + ": " + r4.toString());
//		System.out.println("Route " + r4.getID() + ": " + r4.getFormattedString());
				
		//set route choice parameters
		params = new EnumMap<>(RouteChoiceParams.class);
		params.put(RouteChoiceParams.TIME, -1.5);
		params.put(RouteChoiceParams.LENGTH, -1.0);
		params.put(RouteChoiceParams.COST, -3.6);
		params.put(RouteChoiceParams.INTERSEC, -0.1);
		params.put(RouteChoiceParams.DELAY, 0.8);
		
		r4.calculateUtility(VehicleType.CAR, EngineType.ICE_PETROL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		
		time = r4.getTime();
		length = r4.getLength();
		cost = r4.getCost();
		intersections = r4.getNumberOfIntersections();
		utility = r4.getUtility();
		
		System.out.println("Time: " + time);
		System.out.println("Length: " + length);
		System.out.println("Cost: " + cost);
		System.out.println("Intersections: " + intersections);
		System.out.println("Utility: " + utility);
		
		paramTime = params.get(RouteChoiceParams.TIME);
		paramLength = params.get(RouteChoiceParams.LENGTH);
		paramCost = params.get(RouteChoiceParams.COST);
		paramIntersections = params.get(RouteChoiceParams.INTERSEC);
		
		calculatedUtility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersections;
		
		assertEquals("Utility should be correctly calculated", utility, calculatedUtility, EPSILON);
		
		//create a one-node route
		Route r5 = new Route(roadNetwork);
		RoadPath rp = new RoadPath();
		rp.add(n1);
		System.out.println(rp.toString());
		System.out.println(rp.buildEdges());

		r5 = new Route(rp, roadNetwork);
		System.out.println("Valid: " + r5.isValid());
		System.out.println("Empty: " + r5.isEmpty());
		System.out.println(r5.toString());
		System.out.println(r5.getFormattedString());
		
		r5.calculateLength();
		r5.calculateTravelTime(roadNetwork.getFreeFlowTravelTime(), 0.8);
		r5.calculateCost(VehicleType.CAR, EngineType.ICE_PETROL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null);
		System.out.println("Intersections: " + r5.getNumberOfIntersections());
		r5.calculateUtility(VehicleType.CAR, EngineType.ICE_PETROL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		System.out.println("Length: " + r5.getLength());
		System.out.println("Time: " + r5.getTime());
		System.out.println("Cost: " + r5.getCost());
		System.out.println("Utility: " + r5.getUtility());
		
		System.out.println("First node: " + r5.getOriginNode());
		System.out.println("Last node: " + r5.getDestinationNode());
		
		//check demand model
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
		final String freightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesFile = props.getProperty("elasticitiesFile");
		final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		final String freightRoutesFile = props.getProperty("freightRoutesFile");
		
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		rsg.readRoutesBinary(passengerRoutesFile);
		rsg.readRoutesBinary(freightRoutesFile);
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);

		final String congestionChargingFile = "./src/test/resources/testdata/interventions/congestionCharging.properties";
		CongestionCharging cg2 = new CongestionCharging(congestionChargingFile);
		
		final String congestionChargingSouthampton =  "./src/test/resources/testdata/interventions/congestionChargingSouthampton.properties";
		CongestionCharging cg3 = new CongestionCharging(congestionChargingSouthampton);
		
		List<Intervention> interventions = new ArrayList<Intervention>();
		interventions.add(cg2);
		interventions.add(cg3);
		
		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, freightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, unitCO2EmissionsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, zoning, props);
		
		//install interventions
		for (Intervention i: interventions)
			i.install(dm);
			
		System.out.println(dm.getCongestionCharges(2016));
		
		//create route
		Route r6 = new Route(roadNetwork);
		List<PricingPolicy> congestionCharges = dm.getCongestionCharges(2016);
		
		System.out.println(congestionCharges);

		final String baseFuelConsumptionRatesFile = props.getProperty("baseFuelConsumptionRatesFile");
		final String relativeFuelEfficiencyFile = props.getProperty("relativeFuelEfficiencyFile");
		
		Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> baseFuelConsumptionRates = InputFileReader.readEnergyConsumptionParamsFile(baseFuelConsumptionRatesFile);
		HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>> yearToRelativeFuelEfficiencies = InputFileReader.readRelativeFuelEfficiencyFile(relativeFuelEfficiencyFile);
		HashMap<Integer, Map<EnergyType, Double>> yearToEnergyUnitCosts = InputFileReader.readEnergyUnitCostsFile(energyUnitCostsFile);
		
		//check if congestion charges are applied properly to a route
		r6.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[702]);
		r6.calculateCost(VehicleType.RIGID, EngineType.ICE_DIESEL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), baseFuelConsumptionRates, yearToRelativeFuelEfficiencies.get(2016), yearToEnergyUnitCosts.get(2016), congestionCharges);
		System.out.println("Route cost: " + r6.getCost());
		assertEquals("Route costs are correct", 0.11907393929146934, r6.getCost(), EPSILON); //only fuel cost
		
		r6.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[719]);
		r6.calculateCost(VehicleType.RIGID, EngineType.ICE_DIESEL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), baseFuelConsumptionRates, yearToRelativeFuelEfficiencies.get(2016), yearToEnergyUnitCosts.get(2016), congestionCharges);	
		System.out.println("Route cost: " + r6.getCost());
		assertEquals("Route costs are correct", 25.178610908937205, r6.getCost(), EPSILON); //with Itchen bridge toll and additional fuel cost
		
		r6.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[718]);
		r6.calculateCost(VehicleType.RIGID, EngineType.ICE_DIESEL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), baseFuelConsumptionRates, yearToRelativeFuelEfficiencies.get(2016), yearToEnergyUnitCosts.get(2016), congestionCharges);	
		System.out.println("Route cost: " + r6.getCost());
		assertEquals("Route costs are correct", 25.23814787858294, r6.getCost(), EPSILON); //Itchen bridge toll not applied more than once, only additional fuel cost
		
		r6.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[701]);
		r6.calculateCost(VehicleType.RIGID, EngineType.ICE_DIESEL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), baseFuelConsumptionRates, yearToRelativeFuelEfficiencies.get(2016), yearToEnergyUnitCosts.get(2016), congestionCharges);	
		System.out.println("Route cost: " + r6.getCost());
		assertEquals("Route costs are correct", 25.357221817874407, r6.getCost(), EPSILON); //additional fuel cost
		
		r6.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[615]);
		r6.calculateCost(VehicleType.RIGID, EngineType.ICE_DIESEL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), baseFuelConsumptionRates, yearToRelativeFuelEfficiencies.get(2016), yearToEnergyUnitCosts.get(2016), congestionCharges);	
		System.out.println("Route cost: " + r6.getCost());
		assertEquals("Route costs are correct", 25.684675150925948, r6.getCost(), EPSILON); //additional fuel cost
		
		r6.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[504]);
		r6.calculateCost(VehicleType.RIGID, EngineType.ICE_DIESEL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), baseFuelConsumptionRates, yearToRelativeFuelEfficiencies.get(2016), yearToEnergyUnitCosts.get(2016), congestionCharges);	
		System.out.println("Route cost: " + r6.getCost());
		assertEquals("Route costs are correct", 51.547961210789104, r6.getCost(), EPSILON); //with Southampton city toll and additional fuel cost.
		
		r6.addEdge((DirectedEdge) roadNetwork.getEdgeIDtoEdge()[603]);
		r6.calculateCost(VehicleType.RIGID, EngineType.ICE_DIESEL, TimeOfDay.EIGHTAM, roadNetwork.getFreeFlowTravelTime(), baseFuelConsumptionRates, yearToRelativeFuelEfficiencies.get(2016), yearToEnergyUnitCosts.get(2016), congestionCharges);	
		System.out.println("Route cost: " + r6.getCost());
		assertEquals("Route costs are correct", 51.637266665257705, r6.getCost(), EPSILON); //Southampton city toll not applied more than once, only additional fuel cost.
	}
}
