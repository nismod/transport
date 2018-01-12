/**
 * 
 */
package nismod.transport.decision;

import static org.junit.Assert.*;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.junit.Test;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.utility.ConfigReader;
import nismod.transport.visualisation.PieChartVisualiser;

/**
 * @author Milan Lovric
 *
 */
public class VehicleElectrificationTest {
	
	public static void main( String[] args ) throws IOException	{
		
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
		
		String title = "Engine Type Fractions (" + ve.getStartYear() + "-" + ve.getEndYear() + ")";
		DefaultPieDataset pieDataset = ve.getPieDataSet();
		
		//visualise intervention as a pie chart
		PieChartVisualiser pie = new PieChartVisualiser(pieDataset, title, "Set3", false);
		pie.setVisible(true);
		pie.saveToPNG("VehicleElectrification2D.png");
		
		PieChartVisualiser pie2 = new PieChartVisualiser(pieDataset, title, "BrBG", true);
		pie2.setVisible(true);
		pie2.saveToPNG("VehicleElectrification3D.png");
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

		final String vehicleElectrificationFileName = props.getProperty("vehicleElectrificationFile");

		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
	
		Properties props2 = new Properties();
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("PETROL", "0.40");
		props2.setProperty("DIESEL", "0.30");
		props2.setProperty("LPG", "0.1");
		props2.setProperty("ELECTRICITY", "0.15");
		props2.setProperty("HYDROGEN", "0.025");
		props2.setProperty("HYBRID", "0.025");
		VehicleElectrification ve = new VehicleElectrification(props2);
		System.out.println("Vehicle electrification intervention: " + ve.toString());
		
		VehicleElectrification ve2 = new VehicleElectrification(vehicleElectrificationFileName);
		System.out.println("Vehicle electrification intervention: " + ve2.toString());
	
		List<Intervention> interventions = new ArrayList<Intervention>();
		interventions.add(ve2);
		
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, engineTypeFractionsFile, interventions, null, null);
		
		System.out.println("Base-year engine type fractions: ");
		System.out.println(dm.getEngineTypeFractions(2015));
	
		//copy base-year fractions
		for (int year = 2015; year < 2026; year++) {
			
			HashMap<VehicleType, HashMap<EngineType, Double>> map = new HashMap<VehicleType, HashMap<EngineType, Double>>();
			map.putAll(dm.getEngineTypeFractions(2015));
			dm.setEngineTypeFractions(year, map);
		}
		
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
