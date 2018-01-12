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

import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.utility.ConfigReader;

/**
 * @author Milan Lovric
 *
 */
public class RoadExpansionTest {
	
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

		final String roadExpansionFileName = props.getProperty("roadExpansionFile");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		Properties props2 = new Properties();
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("fromNode", "57");
		props2.setProperty("toNode", "39");
		props2.setProperty("CP", "26042");
		props2.setProperty("number", "2");
		RoadExpansion re = new RoadExpansion(props2);
								
		RoadExpansion re2 = new RoadExpansion(roadExpansionFileName);
		System.out.println("Road expansion intervention: " + re2.toString());
		
		interventions.add(re);
		
		int currentYear = 2015;
		int edgeID = re.getExpandedEdgeID(roadNetwork);
		System.out.println("Number of lanes before expansion: " + roadNetwork.getNumberOfLanes().get(edgeID));
		assertEquals("The number of lanes should be correct", 3, (int)roadNetwork.getNumberOfLanes().get(edgeID));
		re.install(roadNetwork);
		System.out.println("Number of lanes after expansion: " + roadNetwork.getNumberOfLanes().get(edgeID));
		assertEquals("The number of lanes should be correct", 5, (int)roadNetwork.getNumberOfLanes().get(edgeID));
		re.uninstall(roadNetwork);
		System.out.println("Number of lanes after uninstallment: " + roadNetwork.getNumberOfLanes().get(edgeID));
		assertEquals("The number of lanes should be correct", 3, (int)roadNetwork.getNumberOfLanes().get(edgeID));

		currentYear = 2014;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should not be installed", !re.getState());
		
		currentYear = 2026;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should not be installed", !re.getState());
		
		currentYear = 2025;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should be installed", re.getState());
	}
}
