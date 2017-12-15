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

/**
 * @author Milan Lovric
 *
 */
public class RoadExpansionTest {
	
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
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		
		final String assignmentParamsFile = "./src/test/resources/testdata/assignment.properties";
		Properties params = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(assignmentParamsFile);
			// load properties file
			params.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, params);
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "57");
		props.setProperty("toNode", "39");
		props.setProperty("CP", "26042");
		props.setProperty("number", "2");
		RoadExpansion re = new RoadExpansion(props);
								
		final String roadExpansionFileName = "./src/test/resources/testdata/roadExpansion.properties";
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
