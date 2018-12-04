package nismod.transport.visualisation;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.showcase.NetworkVisualiserDemo;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Test for the NetworkVisualizer class
 * @author Milan Lovric
 *
 */
public class NetworkVisualiserTest {

	public static void main( String[] args ) throws IOException	{

		final String configFile = "./src/test/config/testConfig.properties";
		//final String configFile = "./src/main/config/config.properties";
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

		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		final String vehicleTypeToPCUFile = props.getProperty("vehicleTypeToPCUFile");
		final String timeOfDayDistributionFile = props.getProperty("timeOfDayDistributionFile");
		final String timeOfDayDistributionFreightFile = props.getProperty("timeOfDayDistributionFreightFile");
		final String baseFuelConsumptionRatesFile = props.getProperty("baseFuelConsumptionRatesFile");
		final String relativeFuelEfficiencyFile = props.getProperty("relativeFuelEfficiencyFile");
		final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));

		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork,
															zoning,
															InputFileReader.readEnergyUnitCostsFile(energyUnitCostsFile).get(BASE_YEAR),
															InputFileReader.readUnitCO2EmissionFile(unitCO2EmissionsFile).get(BASE_YEAR),
															InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile).get(BASE_YEAR),
															InputFileReader.readAVFractionsFile(AVFractionsFile).get(BASE_YEAR),
															InputFileReader.readVehicleTypeToPCUFile(vehicleTypeToPCUFile),
															InputFileReader.readEnergyConsumptionParamsFile(baseFuelConsumptionRatesFile),
															InputFileReader.readRelativeFuelEfficiencyFile(relativeFuelEfficiencyFile).get(BASE_YEAR),
															InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFile).get(BASE_YEAR),
															InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFreightFile).get(BASE_YEAR),
															null,
															null,
															null,
															null,
															props);
		ODMatrix odm = new ODMatrix("./src/test/resources/testdata/csvfiles/passengerODM.csv");
		rna.assignPassengerFlowsRouting(odm, null, props);

		final URL congestionChargeZoneUrl = new URL("file://src/test/resources/testdata/shapefiles/congestionChargingZone.shp");
		String shapefilePath = "./temp/networkWithDailyVolume.shp";
		String shapefilePath2 = "./temp/networkWithCapacityUtilisation.shp";
		String shapefilePath3 = "./temp/networkWithCountComparison.shp";
		String shapefilePath4 = "./temp/networkWithGEH.shp";

		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumePerVehicleType();
		double[] dailyVolume = rna.getLinkVolumeInPCU();
		Map<Integer, Double> dailyVolumeMap = new HashMap<Integer, Double>();
		for (int edgeID = 0; edgeID < dailyVolume.length; edgeID++)
			dailyVolumeMap.put(edgeID, dailyVolume[edgeID]);
//		double[] dailyVolume = new double[rna.getLinkFreeFlowTravelTimes().length];
//		for (int key: dailyVolumeMap.keySet())
//			dailyVolume[key] = dailyVolumeMap.get(key);
		System.out.println(dailyVolumeMap);
		
		Double[] gehStats = rna.calculateGEHStatisticForCarCounts(rna.getVolumeToFlowFactor());
		Map<Integer, Double> gehStatsMap = new HashMap<Integer, Double>();
		for (int edgeID = 0; edgeID < gehStats.length; edgeID++)
			gehStatsMap.put(edgeID, gehStats[edgeID]);
//		double[] gehStats = new double[rna.getLinkFreeFlowTravelTimes().length];
//		for (int key: gehStatsMap.keySet())
//			gehStats[key] = gehStatsMap.get(key);
				
//		NetworkVisualiserDemo.visualise(roadNetwork, "Network from shapefiles");
//		NetworkVisualiserDemo.visualise(roadNetwork, "Network with capacity utilisation", rna.calculateDirectionAveragedPeakLinkCapacityUtilisation(), "CapUtil", shapefilePath);
		NetworkVisualiser.visualise(roadNetwork, "Network with traffic volume", dailyVolumeMap, "DayVolume", shapefilePath);
		NetworkVisualiser.visualise(roadNetwork, "Network with GEH statistics", gehStatsMap, "GEHStats", shapefilePath4);
//		NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "AbsDiffCounts", shapefilePath3);
//		NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "AbsDiffCounts", shapefilePath4, congestionChargeZoneUrl);
	}
}
