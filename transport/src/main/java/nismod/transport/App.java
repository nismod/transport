package nismod.transport;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;

/**
 * NISMOD V2.0.0 Transport Model
 * @author Milan Lovric
 *
 */
public class App {
	
    public static void main( String[] args ) throws IOException {
    	
        System.out.println( "NISMOD V2.0.0 Transport Model" );
        
		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";

		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		
		final String baseYearODMatrixFile = "./src/test/resources/testdata/passengerODM.csv";
		final String baseYearTimeSkimMatrixFile = "./src/test/resources/testdata/timeSkimMatrix.csv";
		final String baseYearCostSkimMatrixFile = "./src/test/resources/testdata/costSkimMatrix.csv";
		final String populationFile = "./src/test/resources/testdata/population.csv";
		final String GVAFile = "./src/test/resources/testdata/GVA.csv";
		final String energyUnitCostsFile = "./src/test/resources/testdata/energyUnitCosts.csv";
	
		//final String energyUnitCostsFile = args[0];
		System.out.println(energyUnitCostsFile);
		
		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile);

		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork2, baseYearODMatrixFile, baseYearTimeSkimMatrixFile, baseYearCostSkimMatrixFile, populationFile, GVAFile, energyUnitCostsFile);
		
		dm.predictPassengerDemand(2016, 2015);
		
		
    }
}
