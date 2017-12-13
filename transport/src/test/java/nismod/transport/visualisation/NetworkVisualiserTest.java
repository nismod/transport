package nismod.transport.visualisation;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;

/**
 * Test for the NetworkVisualizer class
 * @author Milan Lovric
 *
 */
public class NetworkVisualiserTest {

	public static void main( String[] args ) throws IOException	{

		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL networkUrlfixedEdgeIDs = new URL("file://src/test/resources/minitestdata/miniOutputNetwork.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");
		
		final String areaCodeFileName = "./src/test/resources/minitestdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/minitestdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/minitestdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/minitestdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/minitestdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/minitestdata/freightZoneToNearestNode.csv";
	
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlfixedEdgeIDs);
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null);
		ODMatrix odm = new ODMatrix("./src/test/resources/minitestdata/passengerODM.csv");
		rna.assignPassengerFlows(odm);
		Map<Integer, Double> dailyVolume = rna.getLinkVolumesInPCU();
		NetworkVisualiser.visualise(roadNetwork, "Network from shapefiles");
		NetworkVisualiser.visualise(roadNetwork, "Network with traffic volume", dailyVolume);
		NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateAbsDifferenceCarCounts());
	}
}