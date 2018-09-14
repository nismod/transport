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
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.demand.DemandModel;
import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.visualisation.NetworkVisualiser;

/**
 * @author Milan Lovric
 *
 */
public class CongestionChargingTest {
	
	public static void main( String[] args ) throws IOException	{
		
		
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
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		final String freightRoutesFile = props.getProperty("freightRoutesFile");

		final String congestionChargingFile = "./src/test/resources/testdata/interventions/congestionChargingSouthampton.properties";
		
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");

		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);

		CongestionCharging cg = new CongestionCharging(congestionChargingFile);
		System.out.println("Congestion charging intervention: " + cg.toString());
		//cg.install(dm);
		
		List<Intervention> interventions = new ArrayList<Intervention>();
		interventions.add(cg);
		
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		rsg.readRoutesBinary(passengerRoutesFile);
		rsg.readRoutesBinary(freightRoutesFile);
		rsg.printStatistics();
		
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, unitCO2EmissionsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, null, props);

		System.out.println("Base-year congestion charging: ");
		System.out.println(dm.getCongestionCharges(2015));
		
//		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);
//		ODMatrix odm = new ODMatrix("./src/test/resources/testdata/csvfiles/passengerODM.csv");
//		rna.assignPassengerFlows(odm);
		
		cg.install(dm);
		System.out.println("Congestion charges 2015: " + dm.getCongestionCharges(2015));
		System.out.println("Congestion charges 2016: " + dm.getCongestionCharges(2016));
		
		ODMatrix passengerODM = new ODMatrix(baseYearODMatrixFile);
//		dm.getRoadNetworkAssignment(2015).assignPassengerFlowsRouteChoice(passengerODM, rsg, props);
		
		dm.predictHighwayDemand(2016, 2015);
		RoadNetworkAssignment rna1 = dm.getRoadNetworkAssignment(2015);
		RoadNetworkAssignment rna2 = dm.getRoadNetworkAssignment(2016);
		
		final URL congestionChargeZoneUrl = new URL("file://src/test/resources/testdata/shapefiles/congestionChargingZone.shp");
		
		NetworkVisualiser.visualise(roadNetwork, "2015 no intervention", rna1.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "CapUtil", null, congestionChargeZoneUrl);
		NetworkVisualiser.visualise(roadNetwork, "2016 with congestion charging", rna2.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "CapUtil", null, congestionChargeZoneUrl);
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

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
		final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesFile = props.getProperty("elasticitiesFile");
		final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");

		final String congestionChargingFile = "./src/test/resources/testdata/interventions/congestionCharging.properties";

		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
	
		Properties props2 = new Properties();
		props2.setProperty("type", "congestionCharging");
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("listOfCongestionChargedEdgeIDs", "561, 562,	574"); //space and tab added on purpose
		final String congestionChargingPricing = "./src/test/resources/testdata/csvfiles/congestionChargingPricing.csv";
		props2.setProperty("congestionChargingPricing", congestionChargingPricing);
	
		CongestionCharging cg = new CongestionCharging(props2);
		System.out.println("Congestion charging intervention: " + cg.toString());
		
		CongestionCharging cg2 = new CongestionCharging(congestionChargingFile);
		System.out.println("Congestion charging intervention: " + cg2.toString());
		
		final String congestionChargingSouthampton =  "./src/test/resources/testdata/interventions/congestionChargingSouthampton.properties";
		CongestionCharging cg3 = new CongestionCharging(congestionChargingSouthampton);
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		interventions.add(cg2);
		interventions.add(cg3);
		
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, unitCO2EmissionsFile, engineTypeFractionsFile, AVFractionsFile, interventions, null, null, props);
		
		System.out.println("Base-year congestion charging: ");
		System.out.println(dm.getCongestionCharges(2015));
		
		int currentYear = 2014;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should not be installed", !cg2.getState());
		assertTrue("Intervention should not be installed", !cg3.getState());
			
		currentYear = 2026;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should not be installed", !cg2.getState());
		assertTrue("Intervention should not be installed", !cg3.getState());
		
		currentYear = 2025;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(dm);
		}
		assertTrue("Intervention should be installed", cg2.getState());
		assertTrue("Intervention should be installed", cg3.getState());
		
		System.out.println("Congestion charges in 2015: ");
		System.out.println(dm.getCongestionCharges(2015));
		
		System.out.println("Congestion charges in 2016: ");
		System.out.println(dm.getCongestionCharges(2016));
		
		System.out.println("Congestion charges in 2025: ");
		System.out.println(dm.getCongestionCharges(2025));
		
		cg3.uninstall(dm);
		System.out.println("Congestion charges in 2016: ");
		System.out.println(dm.getCongestionCharges(2016));
	}
}
