package nismod.transport.optimisation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.RealODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.utility.ConfigReader;
import nismod.transport.visualisation.NetworkVisualiser;

/**
 * Tests for the RoadNetworkAssignment class
 * @author Milan Lovric
 *
 */
public class SPSATest {

	public static void main( String[] args ) throws IOException	{
			
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
		
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);
		
		//initial OD matrix
		RealODMatrix odm = new RealODMatrix(baseYearODMatrixFile);
		odm.printMatrixFormatted("Initial passenger matrix:");
		
		double a = 10000;
		double A = 10.0; 
		double c = 10000; 
		double alpha = 0.3;
		double gamma = 0.01;
		
		SPSA optimiser = new SPSA();
		optimiser.initialise(rna, odm, a, A, c, alpha, gamma);
		optimiser.runSPSA(10);
		
		rna.updateLinkVolumeInPCU();
		Map<Integer, Double> dailyVolume = rna.getLinkVolumesInPCU();
		System.out.println(dailyVolume);
		
		NetworkVisualiser.visualise(roadNetwork, "Network from shapefiles");
		NetworkVisualiser.visualise(roadNetwork, "Network with traffic volume", dailyVolume);
		NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateAbsDifferenceCarCounts());

	}
}
