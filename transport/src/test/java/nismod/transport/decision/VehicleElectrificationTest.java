/**
 * 
 */
package nismod.transport.decision;

import static org.junit.Assert.*;

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
public class VehicleElectrificationTest {
	
	@Test
	public void test() throws IOException {

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
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile);
	
		Properties props = new Properties();
		props.setProperty("startYear", "2016");
		props.setProperty("endYear", "2025");
		props.setProperty("PETROL", "0.40");
		props.setProperty("DIESEL", "0.30");
		props.setProperty("LPG", "0.1");
		props.setProperty("ELECTRICITY", "0.15");
		props.setProperty("HYDROGEN", "0.025");
		props.setProperty("HYBRID", "0.025");
		VehicleElectrification ve = new VehicleElectrification(props);
		System.out.println("Vehicle electrification intervention: " + ve.toString());
		
		final String vehicleElectrificationFileName = "./src/test/resources/testdata/vehicleEletrification.properties";
		VehicleElectrification ve2 = new VehicleElectrification(vehicleElectrificationFileName);
		System.out.println("Vehicle electrification intervention: " + ve2.toString());
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		interventions.add(ve2);
		
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, energyUnitCostsFile, interventions);
		
		System.out.println("Base-year engine type fractions: ");
		System.out.println(dm.getEngineTypeFractions(2015));
		
		int currentYear = 2014;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should not be installed", !ve2.getState());

			
		currentYear = 2026;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should not be installed", !ve2.getState());
		
		currentYear = 2025;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should be installed", ve2.getState());
		
		System.out.println("Engine type fractions in 2015: ");
		System.out.println(dm.getEngineTypeFractions(2015));
		
		System.out.println("Engine type fractions in 2016: ");
		System.out.println(dm.getEngineTypeFractions(2016));
		
		System.out.println("Engine type fractions in 2025: ");
		System.out.println(dm.getEngineTypeFractions(2025));
	}
}
