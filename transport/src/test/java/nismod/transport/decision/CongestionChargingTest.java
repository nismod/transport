/**
 * 
 */
package nismod.transport.decision;

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.utility.ConfigReader;

/**
 * @author Milan Lovric
 *
 */
public class CongestionChargingTest {
	
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

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
		final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesFile = props.getProperty("elasticitiesFile");
		final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");

		final String congestionChargingFile = props.getProperty("congestionChargingFile");

		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
	
		Properties props2 = new Properties();
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("listOfCongestionChargedEdgeIDs", "561, 562,	574"); //space and tab added on purpose
		final String congestionChargingPricing = "./src/test/resources/testdata/csvfiles/congestionChargingPricing.csv";
		props2.setProperty("congestionChargingPricing", congestionChargingPricing);
	
		CongestionCharging cg = new CongestionCharging(props2);
		System.out.println("Congestion charging intervention: " + cg.toString());
		
		CongestionCharging cg2 = new CongestionCharging(congestionChargingFile);
		System.out.println("Congestion charging intervention: " + cg2.toString());
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		interventions.add(cg2);
		
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, engineTypeFractionsFile, interventions, null, null);
		
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
