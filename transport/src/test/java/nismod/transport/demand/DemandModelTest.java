/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.geotools.graph.path.Path;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;

/**
 * @author Milan Lovric
 *
 */
public class DemandModelTest {
	
	public static void main( String[] args ) throws IOException	{

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

		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile);
		
		//visualise the shapefiles
		//roadNetwork2.visualise("Test Area");
		
		//export to shapefile
		//roadNetwork2.exportToShapefile("outputNetwork");

		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork2, baseYearODMatrixFile, baseYearTimeSkimMatrixFile, baseYearCostSkimMatrixFile, populationFile, GVAFile);
		dm.predictPassengerDemand(2016, 2015);
		
		System.out.println("Base-year (2015) matrix: ");
		dm.getPassengerDemand(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) passenger matrix: ");
		dm.getPassengerDemand(2016).printMatrixFormatted();

		System.out.println("Base-year (2015) time skim matrix:");
		dm.getTimeSkimMatrix(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) time skim matrix:");
		dm.getTimeSkimMatrix(2016).printMatrixFormatted();
		System.out.printf("Base-year (2015) average OD time: %.2f.\n", dm.getTimeSkimMatrix(2015).getAverageCost());
		System.out.printf("Base-year (2015) average OD time (weighted by demand): %.2f.\n", dm.getTimeSkimMatrix(2015).getAverageCost(dm.getPassengerDemand(2015)));
		System.out.printf("Predicted (2016) average OD time: %.2f.\n", dm.getTimeSkimMatrix(2016).getAverageCost());
		System.out.printf("Predicted (2016) average OD time (weighted by demand): %.2f.\n", dm.getTimeSkimMatrix(2016).getAverageCost(dm.getPassengerDemand(2016)));

		System.out.println("Base-year (2015) cost skim matrix:");
		dm.getCostSkimMatrix(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) cost skim matrix:");
		dm.getCostSkimMatrix(2016).printMatrixFormatted();
		System.out.printf("Base-year (2015) average OD cost: %.2f.\n", dm.getCostSkimMatrix(2015).getAverageCost());
		System.out.printf("Base-year (2015) average OD cost (weighted by demand): %.2f.\n", dm.getCostSkimMatrix(2015).getAverageCost(dm.getPassengerDemand(2015)));
		System.out.printf("Predicted (2016) average OD cost: %.2f.\n", dm.getCostSkimMatrix(2016).getAverageCost());
		System.out.printf("Predicted (2016) average OD cost (weighted by demand): %.2f.\n", dm.getCostSkimMatrix(2016).getAverageCost(dm.getPassengerDemand(2016)));

		RoadNetworkAssignment rna2015 = dm.getRoadNetworkAssignment(2015);
		RoadNetworkAssignment rna2016 = dm.getRoadNetworkAssignment(2016);
		
		System.out.println("Base-year (2015) peak-hour travel times:");
		System.out.println(rna2015.getLinkTravelTimes());
		System.out.println("Predicted (2016) peak-hour travel times:");
		System.out.println(rna2016.getLinkTravelTimes());
		
		System.out.println("Base-year (2015) total energy consumptions:");
		System.out.println(rna2015.calculateEnergyConsumptions());
		System.out.println("Predicted (2016) total energy consumptions:");
		System.out.println(rna2016.calculateEnergyConsumptions());
		
		System.out.println("Base-year (2015) peak-hour link point capacities:");
		System.out.println(rna2015.calculatePeakLinkPointCapacities());
		System.out.println("Predicted (2016) peak-hour link point capacities:");
		System.out.println(rna2016.calculatePeakLinkPointCapacities());
		
		System.out.println("Base-year (2015) peak-hour link densities:");
		System.out.println(rna2015.calculatePeakLinkDensities());
		System.out.println("Predicted (2016) peak-hour link densities:");
		System.out.println(rna2016.calculatePeakLinkDensities());
		
		
//		rna2015.getPathStorage();
//		rna2016.getPathStorage();
//		
//		for (MultiKey mk: rna2015.getPathStorage().keySet()) {
//			//System.out.println(mk);
//			String originZone = (String) mk.getKey(0);
//			String destinationZone = (String) mk.getKey(1);
//			
//			List<Path> pathList2015 = rna2015.getPathStorage().get(originZone, destinationZone);
//			List<Path> pathList2016 = rna2016.getPathStorage().get(originZone, destinationZone);
//			
//			System.out.println(pathList2015);
//			System.out.println(pathList2016);
//			
//		}
	}
}
