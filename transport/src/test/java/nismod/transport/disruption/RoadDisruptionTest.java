package nismod.transport.disruption;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.visualisation.NetworkVisualiser;

/**
 * @author Milan Lovric
 *
 */
public class RoadDisruptionTest {
	
	public static void main( String[] args ) throws IOException	{

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

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		Properties props2 = new Properties();
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("listOfDisruptedEdgeIDs", "561,562,574"); //space and tab added on purpose
		RoadDisruption rd3 = new RoadDisruption(props2);

		//read OD matrix
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
			
		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
	
		//rsg.generateRouteSetWithRandomLinkEliminationRestricted(87, 46);
		rsg.generateRouteSetForODMatrix(odm, 5);
		//rsg.printChoiceSets();
		rsg.printStatistics();

		NetworkVisualiser.visualise(roadNetwork, "Network from shapefiles");
		
		//install road disruption
		rd3.install(rsg);
		rsg.printChoiceSets();
		rsg.printStatistics();
		System.out.println("Disrupted edges: " + rd3.getListOfDisruptedEdgesIDs());
		System.out.println("Removed routes: " + rd3.getListOfRemovedRoutes());

		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);

		//set route choice parameters
		params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
		
		HashMap<String, Double> consumption = new HashMap<String, Double>();
		consumption.put("A", 1.11932239320862);
		consumption.put("B", 0.0440047704089497);
		consumption.put("C", -0.0000813834474888197);
		consumption.put("D", 2.44908328418021E-06);
		
		double unitCost = 1.17;

		rsg.calculateAllUtilities(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, null, params);
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);

		System.out.println(rna.getTripList());
		
		Map<Integer, Double> dailyVolume = rna.getLinkVolumesInPCU();
		System.out.println(dailyVolume);
		
		NetworkVisualiser.visualise(roadNetwork, "Network with traffic volume", dailyVolume);
		
		rd3.uninstall(rsg);
		rsg.printChoiceSets();
		rsg.printStatistics();
		
		rna.resetLinkVolumes();
		rna.resetTripStorages();
		rsg.calculateAllUtilities(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, null, params);
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		
		dailyVolume = rna.getLinkVolumesInPCU();
		NetworkVisualiser.visualise(roadNetwork, "Network with traffic volume", dailyVolume);
	}
	
	@Test
	public void test() throws IOException {

		final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		final String baseYear = props.getProperty("baseYear");
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
		
		final String roadDisruptionFile = props.getProperty("roadDisruptionFile");

		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String energyConsumptionsFile = props.getProperty("energyConsumptionsFile"); //output
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		Properties props2 = new Properties();
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("listOfDisruptedEdgeIDs", "703, 704,	562,561,778,779,621,622,601,602,730,731"); //space and tab added on purpose
		RoadDisruption rd = new RoadDisruption(props2);
		
		rd.install(roadNetwork);
		//assert that the road network does not contain disrupted edges any more
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(703)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(704)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(562)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(561)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(778)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(779)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(621)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(622)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(601)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(602)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(730)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(731)));
		
		rd.uninstall(roadNetwork);
		System.out.println("Disrupted edges: " + rd.getListOfDisruptedEdgesIDs());
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(703)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(704)));
		assertTrue("Removed edge is back in the graph",  roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(562)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(561)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(778)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(779)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(621)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(622)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(601)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(602)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(730)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(731)));
			
		RoadDisruption rd2 = new RoadDisruption(roadDisruptionFile);
		System.out.println("Road disruption: " + rd2.toString());
		
		rd2.install(roadNetwork);
		//assert that the road network does not contain disrupted edges any more
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(703)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(704)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(562)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(561)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(778)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(779)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(621)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(622)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(601)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(602)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(730)));
		assertFalse("Removed edge is not in the graph any more", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(731)));
		System.out.println("Disrupted edges: " + rd.getListOfDisruptedEdgesIDs());
		
		rd2.uninstall(roadNetwork);
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(703)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(704)));
		assertTrue("Removed edge is back in the graph",  roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(562)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(561)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(778)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(779)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(621)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(622)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(601)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(602)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(730)));
		assertTrue("Removed edge is back in the graph", roadNetwork.getNetwork().getEdges().contains(roadNetwork.getEdgeIDtoEdge().get(731)));
		
		props2 = new Properties();
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("listOfDisruptedEdgeIDs", "561"); //space and tab added on purpose
		RoadDisruption rd3 = new RoadDisruption(props2);
		
		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		rsg.generateRouteSetWithRandomLinkEliminationRestricted(87, 46);
		rsg.printChoiceSets();
		rsg.printStatistics();

		//install road disruption
		rd3.install(rsg);
		rsg.printChoiceSets();
		rsg.printStatistics();
		System.out.println("Disrupted edges: " + rd3.getListOfDisruptedEdgesIDs());
		System.out.println("Removed routes: " + rd3.getListOfRemovedRoutes());

		//read OD matrix
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
	
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);

		//set route choice parameters
		params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
		
		HashMap<String, Double> consumption = new HashMap<String, Double>();
		consumption.put("A", 1.11932239320862);
		consumption.put("B", 0.0440047704089497);
		consumption.put("C", -0.0000813834474888197);
		consumption.put("D", 2.44908328418021E-06);
		
		double unitCost = 1.17;

		rsg.calculateAllUtilities(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, null, params);
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		rna.expandTripList();

		assertNull("There should be no traffic volume for the removed link", rna.getLinkVolumesInPCU().get(561));
				
		rd3.uninstall(rsg);
		rsg.printChoiceSets();
		rsg.printStatistics();
	}
}
