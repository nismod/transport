/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.decision.Intervention;
import nismod.transport.decision.RoadDevelopment;
import nismod.transport.decision.RoadExpansion;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;

/**
 * @author Milan Lovric
 *
 */
public class DemandModelTest {
	
	public static void main( String[] args ) throws IOException	{

		//final String configFile = "./src/main/config/config.properties";
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

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
		final String freightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesFile = props.getProperty("elasticitiesFile");
		final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");

		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		final String freightRoutesFile = props.getProperty("freightRoutesFile");
		
		final String roadExpansionFileName = props.getProperty("roadExpansionFile");
		final String roadDevelopmentFileName = props.getProperty("roadDevelopmentFile");
		final String congestionChargingFileName = props.getProperty("congestionChargingFile");

		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		
		final String energyConsumptionsFile = props.getProperty("energyConsumptionsFile"); //output
		final String outputFolder = props.getProperty("outputFolder");
				
		//create output directory
	     File file = new File(outputFolder);
	        if (!file.exists()) {
	            if (file.mkdirs()) {
	                System.out.println("Output directory is created.");
	            } else {
	                System.err.println("Failed to create output directory.");
	            }
	        }

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);

		//visualise the shapefiles
		//roadNetwork2.visualise("Test Area");
		
		//export to shapefile
		//roadNetwork2.exportToShapefile("outputNetwork");
		
		List<Intervention> interventions = new ArrayList<Intervention>();
		
//		Properties props = new Properties();
//		props.setProperty("startYear", "2016");
//		props.setProperty("endYear", "2025");
//		props.setProperty("fromNode", "57");
//		props.setProperty("toNode", "39");
//		props.setProperty("CP", "26042");
//		props.setProperty("number", "2");
//		RoadExpansion re = new RoadExpansion(props);
//		interventions.add(re);
		
		/*
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "11");
		props.setProperty("toNode", "12");
		props.setProperty("CP", "6368");
		props.setProperty("number", "1");
		RoadExpansion re = new RoadExpansion(props);
		interventions.add(re);
		
		props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "12");
		props.setProperty("toNode", "11");
		props.setProperty("CP", "6368");
		props.setProperty("number", "1");
		re = new RoadExpansion(props);
		interventions.add(re);
		
		props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "11");
		props.setProperty("toNode", "76");
		props.setProperty("CP", "73615");
		props.setProperty("number", "1");
		re = new RoadExpansion(props);
		interventions.add(re);
		
		props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "76");
		props.setProperty("toNode", "11");
		props.setProperty("CP", "73615");
		props.setProperty("number", "1");
		re = new RoadExpansion(props);
		interventions.add(re);
		
		props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "42");
		props.setProperty("toNode", "76");
		props.setProperty("CP", "36375");
		props.setProperty("number", "1");
		re = new RoadExpansion(props);
		interventions.add(re);
		
		props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "76");
		props.setProperty("toNode", "42");
		props.setProperty("CP", "36375");
		props.setProperty("number", "1");
		re = new RoadExpansion(props);
		interventions.add(re);
		*/
		
		/*
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "12");
		props.setProperty("toNode", "62");
		props.setProperty("biDirectional", "true");
		props.setProperty("lanesPerDirection", "2");
		props.setProperty("length", "2.73");
		props.setProperty("roadCategory", "A");
		RoadDevelopment rd = new RoadDevelopment(props);
		interventions.add(rd);
	
		*/
			
		RoadExpansion re = new RoadExpansion(roadExpansionFileName);
		RoadDevelopment rd = new RoadDevelopment(roadDevelopmentFileName);
		CongestionCharging cc = new CongestionCharging(congestionChargingFileName);
		
		interventions.add(re);
		interventions.add(rd);
		interventions.add(cc);
			
		//read routes
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork);
		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		rsg.printStatistics();
		rsg.readRoutesBinaryWithoutValidityCheck(freightRoutesFile);
		rsg.printStatistics();
		
		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, freightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, unitCO2EmissionsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, null, props);
		
		//copy base-year engine fractions
		//for (int year = 2015; year < 2025; year++) {
		for (int year = 2015; year < 2016; year++) {
			HashMap<VehicleType, HashMap<EngineType, Double>> map = new HashMap<VehicleType, HashMap<EngineType, Double>>();
			map.putAll(dm.getEngineTypeFractions(2015));
			dm.setEngineTypeFractions(year, map);
		}
	
		dm.predictHighwayDemand(2016, 2015);
		RoadNetworkAssignment rna2015 = dm.getRoadNetworkAssignment(2015);
		RoadNetworkAssignment rna2016 = dm.getRoadNetworkAssignment(2016);
		System.out.println("Base-year (2015) car energy consumptions:");
		System.out.println(rna2015.calculateCarEnergyConsumptions());
		System.out.println("Predicted (2016) car energy consumptions:");
		System.out.println(rna2016.calculateCarEnergyConsumptions());
		
		/*
		dm.predictHighwayDemands(2025, 2015);
		RoadNetworkAssignment rna2015 = dm.getRoadNetworkAssignment(2015);
		RoadNetworkAssignment rna2025 = dm.getRoadNetworkAssignment(2025);

		System.out.println("Base-year (2015) car energy consumptions:");
		System.out.println(rna2015.calculateCarEnergyConsumptions());
		System.out.println("Predicted (2025) car energy consumptions:");
		System.out.println(rna2025.calculateCarEnergyConsumptions());
		*/
		
		
		System.out.println("Base-year (2015) passenger matrix: ");
		dm.getPassengerDemand(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) passenger matrix: ");
		dm.getPassengerDemand(2016).printMatrixFormatted();

		System.out.println("Base-year (2015) time skim matrix:");
		dm.getTimeSkimMatrix(2015).printMatrixFormatted();

		
		System.out.println("Predicted (2016) time skim matrix:");
		dm.getTimeSkimMatrix(2016).printMatrixFormatted();
		System.out.printf("Base-year (2015) average OD time: %.2f.\n", dm.getTimeSkimMatrix(2015).getAverageCost());
		System.out.printf("Base-year (2015) average OD time (weighted by demand): %.2f.\n", dm.getTimeSkimMatrix(2015).getAverageCost(dm.getPassengerDemand(2015)));
		System.out.printf("Predicted (2016) average OD time: %.2f.\n", dm.getTimeSkimMatrix(2016).getAverageCost());
		System.out.printf("Predicted (2016) average OD time (weighted by demand): %.2f.\n", dm.getTimeSkimMatrix(2016).getAverageCost(dm.getPassengerDemand(2016)));

		System.out.println("Base-year (2015) cost skim matrix:");
		dm.getCostSkimMatrix(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) cost skim matrix:");
		dm.getCostSkimMatrix(2016).printMatrixFormatted();
		System.out.printf("Base-year (2015) average OD cost: %.2f.\n", dm.getCostSkimMatrix(2015).getAverageCost());
		System.out.printf("Base-year (2015) average OD cost (weighted by demand): %.2f.\n", dm.getCostSkimMatrix(2015).getAverageCost(dm.getPassengerDemand(2015)));
		System.out.printf("Predicted (2016) average OD cost: %.2f.\n", dm.getCostSkimMatrix(2016).getAverageCost());
		System.out.printf("Predicted (2016) average OD cost (weighted by demand): %.2f.\n", dm.getCostSkimMatrix(2016).getAverageCost(dm.getPassengerDemand(2016)));

		System.out.println("Base-year (2015) freight matrix: ");
		dm.getFreightDemand(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) freight matrix: ");
		dm.getFreightDemand(2016).printMatrixFormatted();

		System.out.println("Base-year (2015) freight time skim matrix:");
		dm.getTimeSkimMatrixFreight(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) freight time skim matrix:");
		dm.getTimeSkimMatrixFreight(2016).printMatrixFormatted();
		System.out.printf("Base-year (2015) average OD time: %.2f.\n", dm.getTimeSkimMatrixFreight(2015).getAverageCost());
		System.out.printf("Base-year (2015) average OD time (weighted by demand): %.2f.\n", dm.getTimeSkimMatrixFreight(2015).getAverageCost(dm.getFreightDemand(2015)));
		System.out.printf("Predicted (2016) average OD time: %.2f.\n", dm.getTimeSkimMatrixFreight(2016).getAverageCost());
		System.out.printf("Predicted (2016) average OD time (weighted by demand): %.2f.\n", dm.getTimeSkimMatrixFreight(2016).getAverageCost(dm.getFreightDemand(2016)));

		System.out.println("Base-year (2015) freight cost skim matrix:");
		dm.getCostSkimMatrixFreight(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) freight cost skim matrix:");
		dm.getCostSkimMatrixFreight(2016).printMatrixFormatted();
		System.out.printf("Base-year (2015) average OD cost: %.2f.\n", dm.getCostSkimMatrixFreight(2015).getAverageCost());
		System.out.printf("Base-year (2015) average OD cost (weighted by demand): %.2f.\n", dm.getCostSkimMatrixFreight(2015).getAverageCost(dm.getFreightDemand(2015)));
		System.out.printf("Predicted (2016) average OD cost: %.2f.\n", dm.getCostSkimMatrixFreight(2016).getAverageCost());
		System.out.printf("Predicted (2016) average OD cost (weighted by demand): %.2f.\n", dm.getCostSkimMatrixFreight(2016).getAverageCost(dm.getFreightDemand(2016)));

		rna2015 = dm.getRoadNetworkAssignment(2015);
		rna2016 = dm.getRoadNetworkAssignment(2016);
		
		System.out.println("Base-year (2015) peak-hour travel times:");
		System.out.println(rna2015.getLinkTravelTimes().get(TimeOfDay.EIGHTAM));
		System.out.println("Predicted (2016) peak-hour travel times:");
		System.out.println(rna2016.getLinkTravelTimes().get(TimeOfDay.EIGHTAM));
		
		System.out.println("Base-year (2015) car energy consumptions:");
		System.out.println(rna2015.calculateCarEnergyConsumptions());
		System.out.println("Predicted (2016) car energy consumptions:");
		System.out.println(rna2016.calculateCarEnergyConsumptions());
		
		System.out.println("Base-year (2015) freight energy consumptions:");
		System.out.println(rna2015.calculateFreightEnergyConsumptions());
		System.out.println("Predicted (2016) freight energy consumptions:");
		System.out.println(rna2016.calculateFreightEnergyConsumptions());
		
		System.out.println("Base-year (2015) total energy consumptions:");
		System.out.println(rna2015.calculateEnergyConsumptions());
		System.out.println("Predicted (2016) total energy consumptions:");
		System.out.println(rna2016.calculateEnergyConsumptions());
		
		System.out.println("Base-year (2015) total CO2 emissions:");
		System.out.println(rna2015.calculateCO2Emissions());
		System.out.println("Predicted (2016) total CO2 emissions:");
		System.out.println(rna2016.calculateCO2Emissions());
				
		System.out.println("Base-year (2015) peak-hour link point capacities:");
		System.out.println(rna2015.calculatePeakLinkPointCapacities());
		System.out.println("Predicted (2016) peak-hour link point capacities:");
		System.out.println(rna2016.calculatePeakLinkPointCapacities());
		
		System.out.println("Base-year (2015) peak-hour link densities:");
		System.out.println(rna2015.calculatePeakLinkDensities());
		System.out.println("Predicted (2016) peak-hour link densities:");
		System.out.println(rna2016.calculatePeakLinkDensities());
		
		//dm.saveAssignmentResults(2015, "assignment2015noIntervention.csv");
		//dm.saveAssignmentResults(2016, "assignment2016noIntervention.csv");
		//dm.saveAssignmentResults(2016, "assignment2016roadExpansion.csv");
		//dm.saveAssignmentResults(2016, "assignment2016roadDevelopment.csv");
		//dm.saveAssignmentResults(2016, "assignment2016electrification.csv");
		//roadNetwork2.exportToShapefile("networkRoadDevelopment");
		
		//dm.saveAssignmentResults(2015, "assignment2015noInterventionFull.csv");
		//dm.saveAssignmentResults(2016, "assignment2016electrificationFull.csv");
		//roadNetwork2.exportToShapefile("networkFullModel");
	}
	
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
		
		//these were not mapped because the count point falls on the river between the two zones
		roadNetwork.getEdgeToZone().put(718, "E07000091"); //New Forest
		roadNetwork.getEdgeToZone().put(719, "E07000091");
					
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
		
		//read routes
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork);
		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		rsg.printStatistics();
		rsg.readRoutesBinaryWithoutValidityCheck(freightRoutesFile);
		rsg.printStatistics();
		
		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, freightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, unitCO2EmissionsFile, engineTypeFractionsFile, AVFractionsFile, null, rsg, null, props);
		
		//copy base-year engine fractions
		//for (int year = 2015; year < 2025; year++) {
		for (int year = 2015; year < 2016; year++) {
			HashMap<VehicleType, HashMap<EngineType, Double>> map = new HashMap<VehicleType, HashMap<EngineType, Double>>();
			map.putAll(dm.getEngineTypeFractions(2015));
			dm.setEngineTypeFractions(year, map);
		}
	
		dm.predictHighwayDemand(2016, 2015);

		RoadNetworkAssignment rna2015 = dm.getRoadNetworkAssignment(2015);
		RoadNetworkAssignment rna2016 = dm.getRoadNetworkAssignment(2016);
		System.out.println("Base-year (2015) car energy consumptions:");
		System.out.println(rna2015.calculateCarEnergyConsumptions());
		System.out.println("Predicted (2016) car energy consumptions:");
		System.out.println(rna2016.calculateCarEnergyConsumptions());
		
		ODMatrix odm2015 = dm.getPassengerDemand(2015);
		ODMatrix odm2016 = dm.getPassengerDemand(2016);
		SkimMatrix tm2015 = dm.getTimeSkimMatrix(2015);
		SkimMatrix tm2016 = dm.getTimeSkimMatrix(2016);
		SkimMatrix cm2015 = dm.getCostSkimMatrix(2015);
		SkimMatrix cm2016 = dm.getCostSkimMatrix(2016);

		odm2015.printMatrixFormatted("2015 demand:");
		odm2016.printMatrixFormatted("2016 demand:");
		tm2015.printMatrixFormatted("2015 travel time:");
		tm2016.printMatrixFormatted("2016 travel time:");
		cm2015.printMatrixFormatted("2015 travel cost:");
		cm2016.printMatrixFormatted("2016 travel cost:");
		
		HashMap<Integer, HashMap<String, Integer>> population = InputFileReader.readPopulationFile(populationFile);
		HashMap<String, Integer> pop2015 = population.get(2015);
		HashMap<String, Integer> pop2016 = population.get(2016);
		
		for (String originZone: odm2016.getSortedOrigins())
			for (String destinationZone: odm2016.getSortedDestinations()) {
				int flow2015 = odm2015.getFlow(originZone, destinationZone);
				int flow2016 = odm2016.getFlow(originZone, destinationZone);
				
				double time2015 = tm2015.getCost(originZone, destinationZone);
				double time2016 = tm2016.getCost(originZone, destinationZone);
				double cost2015 = cm2015.getCost(originZone, destinationZone);
				double cost2016 = cm2016.getCost(originZone, destinationZone);
				int po2015 = pop2015.get(originZone);
				int pd2015 = pop2015.get(destinationZone);
				int po2016 = pop2016.get(originZone);
				int pd2016 = pop2016.get(destinationZone);
				
				//if the population has increased and costs have decreased, the flow should increase
				if (po2016 > po2015 && pd2016 > pd2015 && time2016 < time2015 && cost2016 < cost2015) {
					System.out.printf("Origin = %s destination = %s %n", originZone, destinationZone);
					assertTrue("Flow should increase", flow2016 >= flow2015);
				}
			}
	}
}
