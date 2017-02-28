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
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";

		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		
		final String baseYearODMatrixFile = "./src/test/resources/testdata/passengerODM.csv";
		final String baseYearFreightMatrixFile = "./src/test/resources/testdata/freightMatrix.csv";
		final String populationFile = "./src/test/resources/testdata/population.csv";
		final String GVAFile = "./src/test/resources/testdata/GVA.csv";
		//final String energyUnitCostsFile = "./src/test/resources/testdata/energyUnitCosts.csv";
		
		final String baseYear = args[0];
		final String predictedYear = args[1];
		final String energyUnitCostsFile = args[2];
		final String outputFile = args[3];
		
		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile);
				
		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork2, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, energyUnitCostsFile);
		
		dm.predictHighwayDemand(Integer.parseInt(predictedYear), Integer.parseInt(baseYear));
		
		dm.saveEnergyConsumptions(Integer.parseInt(predictedYear), outputFile);
    }
}
