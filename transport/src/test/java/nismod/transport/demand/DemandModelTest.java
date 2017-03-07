/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.geotools.graph.path.Path;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.RoadExpansion;
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
		final String energyUnitCostsFile = "./src/test/resources/testdata/energyUnitCosts.csv";

		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile);
		
		//visualise the shapefiles
		//roadNetwork2.visualise("Test Area");
		
		//export to shapefile
		//roadNetwork2.exportToShapefile("outputNetwork");
		
		List<Intervention> interventions = new ArrayList<Intervention>();
		
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("fromNode", "57");
		props.setProperty("toNode", "39");
		props.setProperty("CP", "26042");
		props.setProperty("number", "2");
		RoadExpansion re = new RoadExpansion(props);
		interventions.add(re);
		
		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork2, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, energyUnitCostsFile, interventions);
		
		dm.predictHighwayDemand(2016, 2015);

		System.out.println("Base-year (2015) passenger matrix: ");
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

		System.out.println("Base-year (2015) freight matrix: ");
		dm.getFreightDemand(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) freight matrix: ");
		dm.getFreightDemand(2016).printMatrixFormatted();

		System.out.println("Base-year (2015) freight time skim matrix:");
		dm.getTimeSkimMatrixFreight(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) freight time skim matrix:");
		dm.getTimeSkimMatrixFreight(2016).printMatrixFormatted();
		System.out.printf("Base-year (2015) average OD time: %.2f.\n", dm.getTimeSkimMatrixFreight(2015).getAverageCost());
		System.out.printf("Base-year (2015) average OD time (weighted by demand): %.2f.\n", dm.getTimeSkimMatrixFreight(2015).getAverageCost(dm.getFreightDemand(2015)));
		System.out.printf("Predicted (2016) average OD time: %.2f.\n", dm.getTimeSkimMatrixFreight(2016).getAverageCost());
		System.out.printf("Predicted (2016) average OD time (weighted by demand): %.2f.\n", dm.getTimeSkimMatrixFreight(2016).getAverageCost(dm.getFreightDemand(2016)));

		System.out.println("Base-year (2015) freight cost skim matrix:");
		dm.getCostSkimMatrixFreight(2015).printMatrixFormatted();
		System.out.println("Predicted (2016) freight cost skim matrix:");
		dm.getCostSkimMatrixFreight(2016).printMatrixFormatted();
		System.out.printf("Base-year (2015) average OD cost: %.2f.\n", dm.getCostSkimMatrixFreight(2015).getAverageCost());
		System.out.printf("Base-year (2015) average OD cost (weighted by demand): %.2f.\n", dm.getCostSkimMatrixFreight(2015).getAverageCost(dm.getFreightDemand(2015)));
		System.out.printf("Predicted (2016) average OD cost: %.2f.\n", dm.getCostSkimMatrixFreight(2016).getAverageCost());
		System.out.printf("Predicted (2016) average OD cost (weighted by demand): %.2f.\n", dm.getCostSkimMatrixFreight(2016).getAverageCost(dm.getFreightDemand(2016)));

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
	}
}
