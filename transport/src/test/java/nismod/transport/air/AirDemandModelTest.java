package nismod.transport.air;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.utility.ConfigReader;

/**
 * Test class for the AirDemandModel class.
 * @author Milan Lovric
  */
public class AirDemandModelTest {
	
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
	public void test() throws IOException {

		final String configFile = "./src/test/config/airTestConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		final String domesticAirportsFileName = props.getProperty("domesticAirportsFile");
		final String internationalAirportsFileName = props.getProperty("internationalAirportsFile");
		final String domesticDemandFileName = props.getProperty("baseYearDomesticAirPassengerDemandFile");
		final String internationalDemandFileName = props.getProperty("baseYearInternationalAirPassengerDemandFile");
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesAirFile = props.getProperty("elasticitiesAirFile");
		final String domesticAirportFaresFile = props.getProperty("domesticAirportFareIndexFile");
		final String internationalAirportFaresFile = props.getProperty("internationalAirportFareIndexFile");
		final String domesticAirportTripRatesFile = props.getProperty("domesticAirportTripRatesFile");
		final String internationalAirportTripRatesFile = props.getProperty("internationalAirportTripRatesFile");
		
   
		//create an air demand model
		AirDemandModel adm = new AirDemandModel(domesticAirportsFileName, 
				internationalAirportsFileName,
				domesticDemandFileName,
				internationalDemandFileName,
				populationFile,
				GVAFile,
				elasticitiesAirFile, 
				domesticAirportFaresFile, 
				internationalAirportFaresFile, 
				domesticAirportTripRatesFile, 
				internationalAirportTripRatesFile, 
				null,
				props);

		//predict and save air demands
		adm.predictAndSaveAirDemands(2020, 2015);
	}
}
