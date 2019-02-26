package nismod.transport.rail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.NewRailStation;
import nismod.transport.utility.ConfigReader;

/**
 * Test class for the RailDemandModel class.
 * @author Milan Lovric
  */
public class RailDemandModelTest {
	
	public static void main( String[] args ) throws IOException	{

		final String configFile = "./src/main/full/config/config.properties";
		//final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		final String railStationDemandFileName = props.getProperty("baseYearRailStationUsageFile");
		
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesRailFile = props.getProperty("elasticitiesRailFile");
		final String railStationJourneyFaresFile = props.getProperty("railStationJourneyFaresFile");
		final String railStationGeneralisedJourneyTimesFile = props.getProperty("railStationGeneralisedJourneyTimesFile");
		final String carZonalJourneyCostsFile = props.getProperty("carZonalJourneyCostsFile");
		final String railTripRatesFile = props.getProperty("railTripRatesFile");
		
		final String outputFolder = props.getProperty("outputFolder");
				
		//create output directory
	     File file = new File(outputFolder);
	        if (!file.exists()) {
	            if (file.mkdirs()) {
	                System.out.println("Output directory is created.");
	            } else {
	                System.err.println("Failed to create output directory.");
	            }
	        }
	        
		RailDemandModel rdm = new RailDemandModel(railStationDemandFileName,
													populationFile,
													GVAFile,
													elasticitiesRailFile,
													railStationJourneyFaresFile,
													railStationGeneralisedJourneyTimesFile,
													carZonalJourneyCostsFile,
													railTripRatesFile,
													null,
													props);
		
		rdm.predictRailwayDemand(2020, 2015);
				
		String predictedRailDemandFile = props.getProperty("predictedRailDemandFile");
		rdm.saveRailStationDemand(2015, file.getPath() + File.separator + 2015 + File.separator + "baseYearRailDemand.csv");
		rdm.saveRailStationDemand(2020, file.getPath() + File.separator + 2020 + File.separator + predictedRailDemandFile);
	}
	
	@BeforeClass
	public static void initialise() {
	
	    File file = new File("./temp");
	    if (!file.exists()) {
	        if (file.mkdir()) {
	            System.out.println("Temp directory is created.");
	        } else {
	            System.err.println("Failed to create temp directory.");
	        }
	    }
	}
	
	@Test
	public void miniTest() throws IOException {

		final String configFile = "./src/test/config/miniTestConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		final String railStationDemandFileName = props.getProperty("baseYearRailStationUsageFile");
		
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesRailFile = props.getProperty("elasticitiesRailFile");
		final String railStationJourneyFaresFile = props.getProperty("railStationJourneyFaresFile");
		final String railStationGeneralisedJourneyTimesFile = props.getProperty("railStationGeneralisedJourneyTimesFile");
		final String carZonalJourneyCostsFile = props.getProperty("carZonalJourneyCostsFile");
		final String railTripRatesFile = props.getProperty("railTripRatesFile");
		
		props.setProperty("FLAG_USE_CAR_COST_FROM_ROAD_MODEL", "false");

		final String newRailStationFile = props.getProperty("railInterventionFile1");
		Intervention nrs = new NewRailStation(newRailStationFile);
		List<Intervention> interventionList = new ArrayList<Intervention>();
		interventionList.add(nrs);
		
		RailDemandModel rdm = new RailDemandModel(railStationDemandFileName,
													populationFile,
													GVAFile,
													elasticitiesRailFile,
													railStationJourneyFaresFile,
													railStationGeneralisedJourneyTimesFile,
													carZonalJourneyCostsFile,
													railTripRatesFile,
													interventionList,
													props);
		
		rdm.predictRailwayDemandUsingResultsOfFromYear(2018, 2015);
		rdm.saveAllResults(2018);
		rdm.predictRailwayDemandUsingResultsOfFromYear(2020, 2018);
		
		rdm.saveRailStationDemand(2015, "./temp/miniRailDemand2015.csv");
		rdm.saveRailStationDemand(2020, "./temp/miniRailDemand2020.csv");
		rdm.saveZonalRailStationDemand(2015, "./temp/miniZonalRailDemand2015.csv");
		rdm.saveZonalRailStationDemand(2020, "./temp/miniZonalRailDemand2020.csv");
	}

	@Test
	public void test() throws IOException {

		final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		final String railStationDemandFileName = props.getProperty("baseYearRailStationUsageFile");
		
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesRailFile = props.getProperty("elasticitiesRailFile");
		final String railStationJourneyFaresFile = props.getProperty("railStationJourneyFaresFile");
		final String railStationGeneralisedJourneyTimesFile = props.getProperty("railStationGeneralisedJourneyTimesFile");
		final String carZonalJourneyCostsFile = props.getProperty("carZonalJourneyCostsFile");
		final String railTripRatesFile = props.getProperty("railTripRatesFile");
		
		props.setProperty("FLAG_USE_CAR_COST_FROM_ROAD_MODEL", "false");
		
		final String newRailStationFile = props.getProperty("railInterventionFile1");
		Intervention nrs = new NewRailStation(newRailStationFile);
		
		final String newRailStationFile2 = props.getProperty("railInterventionFile2");
		Intervention nrs2 = new NewRailStation(newRailStationFile2);
		
		List<Intervention> interventionList = new ArrayList<Intervention>();
		interventionList.add(nrs);
		interventionList.add(nrs2);
			        
		RailDemandModel rdm = new RailDemandModel(railStationDemandFileName,
													populationFile,
													GVAFile,
													elasticitiesRailFile,
													railStationJourneyFaresFile,
													railStationGeneralisedJourneyTimesFile,
													carZonalJourneyCostsFile,
													railTripRatesFile,
													null,
													props);
		
		rdm.predictRailwayDemands(2020, 2015);
		rdm.getRailStationDemand(2018).printRailDemand("2018 demand:");
		rdm.getRailStationDemand(2020).printRailDemand("2020 demand:");
		
		rdm.saveRailStationDemand(2015, "./temp/railDemand2015.csv");
		rdm.saveRailStationDemand(2020, "./temp/railDemand2020.csv");
		rdm.saveZonalRailStationDemand(2015, "./temp/zonalRailDemand2015.csv");
		rdm.saveZonalRailStationDemand(2020, "./temp/zonalRailDemand2020.csv");		
	}
}
