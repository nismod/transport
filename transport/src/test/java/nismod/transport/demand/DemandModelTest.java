/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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

/**
 * @author Milan Lovric
 *
 */
public class DemandModelTest {
	
	public static void main( String[] args ) throws IOException	{

		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";

		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlfixedEdgeIDs = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		
		final String baseYearODMatrixFile = "./src/test/resources/testdata/passengerODM.csv";
		final String baseYearFreightMatrixFile = "./src/test/resources/testdata/freightMatrix.csv";
		final String populationFile = "./src/test/resources/testdata/population.csv";
		final String GVAFile = "./src/test/resources/testdata/GVA.csv";
		final String energyUnitCostsFile = "./src/test/resources/testdata/energyUnitCosts.csv";
		
		
//		final URL zonesUrl2 = new URL("file://src/main/resources/data/zones.shp");
//		final URL networkUrl2 = new URL("file://src/main/resources/data/network.shp");
//		final URL nodesUrl2 = new URL("file://src/main/resources/data/nodes.shp");
//		final URL AADFurl2 = new URL("file://src/main/resources/data/AADFdirected2015.shp");
//		
//		final String areaCodeFileName = "./src/main/resources/data/population_OA_GB.csv";
//		final String areaCodeNearestNodeFile = "./src/main/resources/data/nearest_node_OA_GB.csv";
//		final String workplaceZoneFileName = "./src/main/resources/data/workplacePopulationFakeSC.csv";
//		final String workplaceZoneNearestNodeFile = "./src/main/resources/data/nearest_node_WZ_GB_fakeSC.csv";
//		final String freightZoneToLADfile = "./src/main/resources/data/freightZoneToLAD.csv";
//		final String freightZoneNearestNodeFile = "./src/main/resources/data/freightZoneToNearestNode.csv";
//		
//		final String baseYearODMatrixFile = "./src/main/resources/data/balancedODMatrixOldLengths.csv";
//		final String baseYearFreightMatrixFile = "./src/main/resources/data/freightMatrix.csv";
//		final String populationFile = "./src/main/resources/data/populationfull.csv";
//		final String GVAFile = "./src/main/resources/data/GVAperHeadFull.csv";
//		final String energyUnitCostsFile = "./src/main/resources/data/energyUnitCosts.csv";
		
		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork2.replaceNetworkEdgeIDs(networkUrlfixedEdgeIDs);
		
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
			
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("PETROL", "0.40");
		props.setProperty("DIESEL", "0.30");
		props.setProperty("LPG", "0.1");
		props.setProperty("ELECTRICITY", "0.15");
		props.setProperty("HYDROGEN", "0.025");
		props.setProperty("HYBRID", "0.025");
		VehicleElectrification ve = new VehicleElectrification(props);
		interventions.add(ve);
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVG_INTERSECTION_DELAY", "0.8");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork2);
		//rsg.readRoutes("./src/test/resources/testdata/testRoutes.txt");
		rsg.readRoutes("./src/test/resources/testdata/allRoutes.txt");
		
		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork2, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, energyUnitCostsFile, interventions, rsg, params);
		
		dm.predictHighwayDemand(2025, 2015);
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
