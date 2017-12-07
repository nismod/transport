/**
 * 
 */
package nismod.transport.decision;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;

/**
 * @author Milan Lovric
 *
 */
public class CongestionChargingTest {
	
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
		
		final String baseYearODMatrixFile = "./src/test/resources/testdata/passengerODM.csv";
		final String baseYearFreightMatrixFile = "./src/test/resources/testdata/freightMatrix.csv";
		final String populationFile = "./src/test/resources/testdata/population.csv";
		final String GVAFile = "./src/test/resources/testdata/GVA.csv";
		final String energyUnitCostsFile = "./src/test/resources/testdata/energyUnitCosts.csv";
		
		final String congestionChargeFile = "./src/test/resources/testdata/congestionCharges.csv";
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
	
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("listOfCongestionChargedEdgeIDs", "561, 562,	574"); //space and tab added on purpose
		props.setProperty("congestionChargeFile", congestionChargeFile);
	
		CongestionCharging cg = new CongestionCharging(props);
		System.out.println("Congestion charging intervention: " + cg.toString());
		
		final String congestionChargingFileName = "./src/test/resources/testdata/congestionCharging.properties";
		CongestionCharging cg2 = new CongestionCharging(congestionChargingFileName);
		System.out.println("Congestion charging intervention: " + cg2.toString());
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		interventions.add(cg2);
		
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, energyUnitCostsFile, interventions, null, null);
		
		System.out.println("Base-year congestion charging: ");
		System.out.println(dm.getCongestionCharges(2015));
		
		int currentYear = 2014;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should not be installed", !cg2.getState());

			
		currentYear = 2026;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should not be installed", !cg2.getState());
		
		currentYear = 2025;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should be installed", cg2.getState());
		
		System.out.println("Congestion charges in 2015: ");
		System.out.println(dm.getCongestionCharges(2015));
		
		System.out.println("Congestion charges in 2016: ");
		System.out.println(dm.getCongestionCharges(2016));
		
		System.out.println("Congestion charges in 2025: ");
		System.out.println(dm.getCongestionCharges(2025));
		
		cg2.uninstall(dm);
		System.out.println("Congestion charges in 2016: ");
		System.out.println(dm.getCongestionCharges(2016));
	}
}
