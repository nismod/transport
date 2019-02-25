package nismod.transport.rail;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.rail.RailDemandModel.ElasticityArea;
import nismod.transport.rail.RailStation.RailModeType;
import nismod.transport.utility.ConfigReader;

/**
 * Test class for the RailStationDemand and Station class.
 * @author Milan Lovric
  */
public class RailStationDemandTest {
	
	@Test
	public void miniTest() throws IOException {

		final String configFile = "./src/test/config/miniTestConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		RailStation st1 = new RailStation(0, RailModeType.NRAIL, "London Waterloo", "London Waterloo Rail Station", 12340, 12450, 12450, 15044, 363, "E06000006", "London Camden", ElasticityArea.LT);
		System.out.println(st1);
		
		final String railStationDemandFileName = props.getProperty("baseYearRailStationUsageFile");
		RailStationDemand rsd = new RailStationDemand(railStationDemandFileName);
		
		rsd.printRailDemand("This is rail demand:");
		rsd.printRailDemandNLCSorted("This is rail demand sorted on NLC:");
		rsd.printRailDemandNameSorted("This is rail demand sorted on station name:");
		rsd.printRailDemandUsageSorted("This is rail demand sorted on station usage:");
		
		HashMap<String, Integer> zonalUsageTotal = rsd.calculateYearlyZonalUsageTotal();
		HashMap<String, Integer> zonalUsageAverage = rsd.calculateYearlyZonalUsageAverage();
		HashMap<String, Double> dailyZonalUsageTotal = rsd.calculateDailyZonalUsageTotal();
		HashMap<String, Double> dailyZonalUsageAverage = rsd.calculateDailyZonalUsageAverage();
		HashMap<String, List<RailStation>> zoneToList = rsd.createListOfStationsWithinEachLAD();
		
		System.out.println("Zonal usage yearly total: " + zonalUsageTotal);
		System.out.println("Zonal usage yearly average: " + zonalUsageAverage);
		System.out.println("Zonal usage daily total: " + dailyZonalUsageTotal);
		System.out.println("Zonal usage daily average: " + dailyZonalUsageAverage);
		System.out.println("Number of rail stations in Southampton LAD: " + zoneToList.get("E06000045").size());
		
		for (String zone: zonalUsageTotal.keySet()) {
	
			int total = zonalUsageTotal.get(zone);
			int average = zonalUsageAverage.get(zone);
			List<RailStation> list = zoneToList.get(zone);
			
			assertEquals("Average usage uses correct number of stations", list.size(), (int) Math.round((double)total/average));
		}
		
		for (String zone: dailyZonalUsageTotal.keySet()) {
			
			double total = dailyZonalUsageTotal.get(zone);
			double average = dailyZonalUsageAverage.get(zone);
			List<RailStation> list = zoneToList.get(zone);
			
			assertEquals("Average usage uses correct number of stations", list.size(), (int) Math.round(total/average));
		}
	}

	@Test
	public void test() throws IOException {

		final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		RailStation st1 = new RailStation(0, RailModeType.NRAIL, "London Waterloo", "London Waterloo Rail Station", 12340, 12450, 12450, 15044, 363, "E06000006", "London Camden", ElasticityArea.LT);
		System.out.println(st1);
		
		final String railStationDemandFileName = props.getProperty("baseYearRailStationUsageFile");
		RailStationDemand rsd = new RailStationDemand(railStationDemandFileName);
		
		rsd.printRailDemand("This is rail demand:");
		rsd.printRailDemandNLCSorted("This is rail demand sorted on NLC:");
		rsd.printRailDemandNameSorted("This is rail demand sorted on station name:");
		rsd.printRailDemandUsageSorted("This is rail demand sorted on station usage:");
		
		HashMap<String, Integer> zonalUsageTotal = rsd.calculateYearlyZonalUsageTotal();
		HashMap<String, Integer> zonalUsageAverage = rsd.calculateYearlyZonalUsageAverage();
		HashMap<String, Double> dailyZonalUsageTotal = rsd.calculateDailyZonalUsageTotal();
		HashMap<String, Double> dailyZonalUsageAverage = rsd.calculateDailyZonalUsageAverage();
		HashMap<String, List<RailStation>> zoneToList = rsd.createListOfStationsWithinEachLAD();
		
		for (String zone: zonalUsageTotal.keySet()) {
			
			int total = zonalUsageTotal.get(zone);
			int average = zonalUsageAverage.get(zone);
			List<RailStation> list = zoneToList.get(zone);
			
			assertEquals("Average usage uses correct number of stations", list.size(), (int) Math.round((double)total/average));
		}
		
		for (String zone: dailyZonalUsageTotal.keySet()) {
			
			double total = dailyZonalUsageTotal.get(zone);
			double average = dailyZonalUsageAverage.get(zone);
			List<RailStation> list = zoneToList.get(zone);
			
			assertEquals("Average usage uses correct number of stations", list.size(), (int) Math.round(total/average));
		}
		
		int size1 = rsd.getRailDemandList().size();
		int size2 = rsd.getRailDemandMap().size();
		System.out.println("Number of stations: " + size1);
		System.out.println("Number of stations: " + size2);
		assertEquals("Number of stations is equal", size1, size2);
		
		//add station
		rsd.addStation(st1);
		rsd.addStation(st1); //it should not add duplicate
		rsd.addStation(st1); //it should not add duplicate
		int size3 = rsd.getRailDemandList().size();
		int size4 = rsd.getRailDemandMap().size();
		System.out.println("Number of stations: " + size3);
		System.out.println("Number of stations: " + size4);
		assertEquals("Number of stations is equal", size3, size4);
		assertEquals("Number of stations is correct", size1+1, size3);
		
		//remove station
		boolean result = rsd.removeStation(0);
		if (result) System.out.println("Station removed.");
		result = rsd.removeStation(0);
		if (result) System.out.println("Station removed."); //it should not print (already removed)
		int size5 = rsd.getRailDemandList().size();
		int size6 = rsd.getRailDemandMap().size();
		System.out.println("Number of stations: " + size5);
		System.out.println("Number of stations: " + size6);
		assertEquals("Number of stations is equal", size5, size6);
		assertEquals("Number of stations is correct", size1, size5);
	}
}
