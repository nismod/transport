package nismod.transport.disruption;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RouteSetGenerator;

/**
 * @author Milan Lovric
 *
 */
public class RoadDisruptionTest {
	
	@Test
	public void test() throws IOException {

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
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlfixedEdgeIDs);
		
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("listOfDisruptedEdgeIDs", "703, 704,	562,561,778,779,621,622,601,602,730,731"); //space and tab added on purpose
		RoadDisruption rd = new RoadDisruption(props);
		
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
			
		final String roadDisruptionFileName = "./src/test/resources/testdata/roadDisruption.properties";
		RoadDisruption rd2 = new RoadDisruption(roadDisruptionFileName);
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
		
		props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("listOfDisruptedEdgeIDs", "561"); //space and tab added on purpose
		RoadDisruption rd3 = new RoadDisruption(props);
		
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
			
		rd3.uninstall(rsg);
		rsg.printChoiceSets();
		rsg.printStatistics();
	}
}
