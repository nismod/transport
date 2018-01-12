/**
 * 
 */
package nismod.transport.decision;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.geotools.graph.structure.DirectedEdge;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.utility.ConfigReader;

/**
 * @author Milan Lovric
 *
 */
public class RoadDevelopmentTest {
	
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

		final String roadDevelopmentFileName = props.getProperty("roadDevelopmentFile");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		Properties props2 = new Properties();
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("fromNode", "63");
		props2.setProperty("toNode", "23");
		props2.setProperty("biDirectional", "true");
		props2.setProperty("lanesPerDirection", "2");
		props2.setProperty("length", "10.23");
		props2.setProperty("roadCategory", "A");
		RoadDevelopment rd = new RoadDevelopment(props2);
		
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
		assertEquals("From node ID is correct", newEdge.getNodeA().getID(), Integer.parseInt(props2.getProperty("fromNode")));
		assertEquals("To node ID is correct", newEdge.getNodeB().getID(), Integer.parseInt(props2.getProperty("toNode")));
		
		DirectedEdge newEdge2 = (DirectedEdge) roadNetwork.getEdgeIDtoEdge().get(rd.getDevelopedEdgeID2());
		assertEquals("Edge ID from other direction is correct", newEdge2.getID(), (int) roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(newEdge.getID()));
		assertEquals("From node ID is correct", newEdge2.getNodeA().getID(), Integer.parseInt(rd.getProperty("toNode")));
		assertEquals("To node ID is correct", newEdge2.getNodeB().getID(), Integer.parseInt(rd.getProperty("fromNode")));
		
		System.out.println(newEdge.getObject());
		
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
