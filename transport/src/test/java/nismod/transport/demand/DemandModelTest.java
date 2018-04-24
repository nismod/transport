/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.geotools.graph.path.Path;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.RoadDevelopment;
import nismod.transport.decision.RoadExpansion;
import nismod.transport.decision.VehicleElectrification;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.utility.ConfigReader;

/**
 * @author Milan Lovric
 *
 */
public class DemandModelTest {
	
	public static void main( String[] args ) throws IOException	{

		final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		final String fromYear = props.getProperty("fromYear");
		final String predictedYear = props.getProperty("predictedYear");
		
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
		final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesFile = props.getProperty("elasticitiesFile");
		final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");

		final String roadExpansionFileName = props.getProperty("roadExpansionFile");
		final String roadDevelopmentFileName = props.getProperty("roadDevelopmentFile");
		final String vehicleElectrificationFileName = props.getProperty("vehicleElectrificationFile");
		final String congestionChargeFile = props.getProperty("congestionChargingFile");

		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		
		final String energyConsumptionsFile = props.getProperty("energyConsumptionsFile"); //output
		
		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork2.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
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
			
		Properties props2 = new Properties();
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("vehicleType", "CAR");
		props2.setProperty("PETROL", "0.40");
		props2.setProperty("DIESEL", "0.30");
		props2.setProperty("LPG", "0.1");
		props2.setProperty("ELECTRICITY", "0.15");
		props2.setProperty("HYDROGEN", "0.025");
		props2.setProperty("HYBRID", "0.025");
		VehicleElectrification ve = new VehicleElectrification(props2);
		interventions.add(ve);
		
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork2);
		//rsg.readRoutes("./src/test/resources/testdata/testRoutes.txt");
		rsg.readRoutes("./src/test/resources/testdata/allRoutes.txt");
		
		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork2, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, props);
		
		//copy base-year engine fractions
		for (int year = 2015; year < 2025; year++) {
			HashMap<VehicleType, HashMap<EngineType, Double>> map = new HashMap<VehicleType, HashMap<EngineType, Double>>();
			map.putAll(dm.getEngineTypeFractions(2015));
			dm.setEngineTypeFractions(year, map);
		}
				
		dm.predictHighwayDemands(2025, 2015);
		RoadNetworkAssignment rna2015 = dm.getRoadNetworkAssignment(2015);
		RoadNetworkAssignment rna2025 = dm.getRoadNetworkAssignment(2025);

		System.out.println("Base-year (2015) car energy consumptions:");
		System.out.println(rna2015.calculateCarEnergyConsumptions());
		System.out.println("Predicted (2025) car energy consumptions:");
		System.out.println(rna2025.calculateCarEnergyConsumptions());
		
		/*
		
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

		RoadNetworkAssignment rna2015 = dm.getRoadNetworkAssignment(2015);
		RoadNetworkAssignment rna2016 = dm.getRoadNetworkAssignment(2016);
		
		System.out.println("Base-year (2015) peak-hour travel times:");
		System.out.println(rna2015.getLinkTravelTimes());
		System.out.println("Predicted (2016) peak-hour travel times:");
		System.out.println(rna2016.getLinkTravelTimes());
		
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
		
		System.out.println("Base-year (2015) peak-hour link point capacities:");
		System.out.println(rna2015.calculatePeakLinkPointCapacities());
		System.out.println("Predicted (2016) peak-hour link point capacities:");
		System.out.println(rna2016.calculatePeakLinkPointCapacities());
		
		System.out.println("Base-year (2015) peak-hour link densities:");
		System.out.println(rna2015.calculatePeakLinkDensities());
		System.out.println("Predicted (2016) peak-hour link densities:");
		System.out.println(rna2016.calculatePeakLinkDensities());
		
		*/
		
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
}
