/**
 * 
 */
package nismod.transport.decision;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.geotools.graph.structure.DirectedEdge;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;

/**
 * @author Milan Lovric
 *
 */
public class RoadDevelopmentTest {
	
	@Test
	public void test() throws IOException {

		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";

		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile);
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "63");
		props.setProperty("toNode", "23");
		props.setProperty("biDirectional", "true");
		props.setProperty("lanesPerDirection", "2");
		props.setProperty("length", "10.23");
		props.setProperty("roadCategory", "A");
		RoadDevelopment rd = new RoadDevelopment(props);
		
		final String roadDevelopmentFileName = "./src/test/resources/testdata/roadDevelopment.properties";
		RoadDevelopment rd2 = new RoadDevelopment(roadDevelopmentFileName);
		System.out.println("Road development intervention: " + rd2.toString());
		
		interventions.add(rd);
		
		int currentYear = 2015;
		
		System.out.println("Number of road links/edges before development: " + roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of road links should be correct", 301, roadNetwork.getNetwork().getEdges().size());
	
		rd.install(roadNetwork);
		
		System.out.println("Number of road links/edges after development: " + roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of road links in the graph should be correct", 303, roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of edges in the map should be correct", 303, roadNetwork.getEdgeIDtoEdge().size());
		assertEquals("The number of road lanes should be correct", (int) Integer.parseInt(rd.getProperty("lanesPerDirection")), (int) roadNetwork.getNumberOfLanes().get(rd.getDevelopedEdgeID()));

		DirectedEdge newEdge = (DirectedEdge) roadNetwork.getEdgeIDtoEdge().get(rd.getDevelopedEdgeID());
		assertEquals("From node ID is correct", newEdge.getNodeA().getID(), Integer.parseInt(props.getProperty("fromNode")));
		assertEquals("To node ID is correct", newEdge.getNodeB().getID(), Integer.parseInt(props.getProperty("toNode")));
		
		DirectedEdge newEdge2 = (DirectedEdge) roadNetwork.getEdgeIDtoEdge().get(rd.getDevelopedEdgeID2());
		assertEquals("Edge ID from other direction is correct", newEdge2.getID(), (int) roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(newEdge.getID()));
		assertEquals("From node ID is correct", newEdge2.getNodeA().getID(), Integer.parseInt(rd.getProperty("toNode")));
		assertEquals("To node ID is correct", newEdge2.getNodeB().getID(), Integer.parseInt(rd.getProperty("fromNode")));
		
		//check length
	
		rd.uninstall(roadNetwork);
		System.out.println("Number of road links/edges after uninstallment: " + roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of road links should be correct", 301, roadNetwork.getNetwork().getEdges().size());

		currentYear = 2014;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should not be installed", !rd.getState());
		
		currentYear = 2026;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should not be installed", !rd.getState());
		
		currentYear = 2025;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should be installed", rd.getState());
	}
}
