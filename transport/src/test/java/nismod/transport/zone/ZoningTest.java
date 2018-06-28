package nismod.transport.zone;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.utility.ConfigReader;

/**
 * Test class for the Zoning class.
 * @author Milan Lovric
  */
public class ZoningTest {

	public static void main(String[] args) throws IOException {

		
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
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		final String freightRoutesFile = props.getProperty("freightRoutesFile");
		final String outputFolder = props.getProperty("outputFolder");
		final String assignmentResultsFile = props.getProperty("assignmentResultsFile");
		
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		System.out.println("Number of TEMPRO zones: " + zoning.getZoneToNearestNodeIDMap().size());
		System.out.println("Number of nodes: " + zoning.getNodeToZoneMap().size());
		
		System.out.println("Zones to nearest Nodes: " + zoning.getZoneToNearestNodeIDMap());
		System.out.println("Zones to nearest nodes distances: " + zoning.getZoneToNearestNodeDistanceMap());
		System.out.println(zoning.getZoneToNearestNodeIDMap().keySet());
		System.out.println("Nodes to zones in which they are located: " + zoning.getNodeToZoneMap());
		
		//check if any zones are assigned to nodes that belong to other zones (which is possible if they are closer to the centroid).
		int counter = 0;
		for (String zone: zoning.getZoneToNearestNodeIDMap().keySet()) {
			
			Integer nodeID = zoning.getZoneToNearestNodeIDMap().get(zone);
			String zoneOfNode = zoning.getNodeToZoneMap().get(nodeID);
			if (!zone.equals(zoneOfNode)) {
				counter++;
				System.out.printf("Zone %s is assigned to node %d which is located in zone %s. %n", zone, nodeID, zoneOfNode);
			}
		}
		System.out.println("Number of cross-assigned zones: " + counter);
		
		System.out.println("Tempro zone to LAD zone map: " + zoning.getZoneToLADMap());
		System.out.println("Tempro zone code to tempro zone ID map: " + zoning.getZoneCodeToIDMap());
		System.out.println("Tempro zone ID to tempro zone code map: " + zoning.getZoneIDToCodeMap());
		
		System.out.println(zoning.getZoneToSortedListOfNodeAndDistancePairs());
		System.out.println(zoning.getZoneToListOfContaintedNodes());
		
//		System.out.println(zoning.getZoneToNodeMatrix());
//		for (String zone: zoning.getZoneToNodeMatrix().keySet()) {
//			zoning.getZoneToNodeMatrix().get(zone).printMatrixFormatted(zone);
//		}
		
		System.out.println("LAD to Tempro zone map: " + zoning.getLADToListOfContainedZones());
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		//test mapping for a few zones
		assertEquals("Zone E02004779 is mapped to the correct node", 79, zoning.getZoneToNearestNodeIDMap().get("E02004779").intValue());
		System.out.println("Distance: " + zoning.getZoneToNearestNodeDistanceMap().get("E02004779"));
		
		assertEquals("Zone E02004795 is mapped to the correct node", 63, zoning.getZoneToNearestNodeIDMap().get("E02004795").intValue());
		assertEquals("Zone E02003568 is mapped to the correct node", 30, zoning.getZoneToNearestNodeIDMap().get("E02003568").intValue());
		
		System.out.println(zoning.getZoneToSortedListOfNodeAndDistancePairs());
	}
	
	@Test
	public void miniTest() throws IOException {

		final String configFile = "./src/test/config/miniTestConfig.properties";
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		System.out.println("Zones to nearest Nodes: " + zoning.getZoneToNearestNodeIDMap());
		System.out.println("Zones to nearest nodes distances: " + zoning.getZoneToNearestNodeDistanceMap());
		
		//test mapping for a few zones
		assertEquals("Zone E02003560 is mapped to the correct node", 31, zoning.getZoneToNearestNodeIDMap().get("E02003560").intValue());
		System.out.println("Distance: " + zoning.getZoneToNearestNodeDistanceMap().get("E02003560"));
		
		assertEquals("Zone E02004795 is mapped to the correct node", 27, zoning.getZoneToNearestNodeIDMap().get("E02003561").intValue());
		assertEquals("Zone E02003568 is mapped to the correct node", 105, zoning.getZoneToNearestNodeIDMap().get("E02003580").intValue());
		
		System.out.println(zoning.getZoneToNearestNodeIDMap().keySet());		
	}
}
