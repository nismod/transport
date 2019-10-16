package nismod.transport.air;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.air.InternodalPassengerDemand.Passengers;
import nismod.transport.decision.Intervention;
import nismod.transport.utility.InputFileReader;

/**
 * Main air demand prediction model (elasticity-based).
 * @author Milan Lovric
 */
public class AirDemandModel {

	private final static Logger LOGGER = LogManager.getLogger(AirDemandModel.class);

	public static int baseYear;
	
	public static enum ElasticityTypes {
		POPULATION, //population
		GVA, //income
		COST_DOMESTIC, //domestic fare index
		COST_INTERNATIONAL, //foreign fare index
	}

	public static Map<String, Airport> domesticAirports;
	public static Map<String, Airport> internationalAirports;
	
	private Map<ElasticityTypes, Double> elasticities;

	//internodal air passenger demand (domestic and international)
	private HashMap<Integer, InternodalPassengerDemand> yearToDomesticPassengerDemand;
	private HashMap<Integer, InternodalPassengerDemand> yearToInternationalPassengerDemand;
	//private HashMap<Integer, GroupedFlightDemand> yearToGroupedFlightsDemand; //air flight movements demand

	//zonal data (population and GVA)
	private HashMap<Integer, HashMap<String, Integer>> yearToZoneToPopulation;
	private HashMap<Integer, HashMap<String, Double>> yearToZoneToGVA;
	
	//airport data (fare index)
	private HashMap<Integer, HashMap<String, Double>> yearToDomesticAirportFareIndex;
	private HashMap<Integer, HashMap<String, Double>> yearToInternationalAirportFareIndex;

	//trip rates (domestic and international)
	private HashMap<Integer, Double> yearToDomesticTripRates;
	private HashMap<Integer, Double> yearToInternationalTripRates;

	private List<Intervention> interventions;
	private Properties props;

	/**
	 * Constructor for the air demand model.
	 * @param domesticAirportsFile List of domestic airports.
	 * @param internationalAirportsFile List of international airports.
	 * @param baseYearDomesticPassengerFile Base year domestic air passenger file (demand).
	 * @param baseYearInternationalPassengerFile Base year international air passenger file (demand).
	 * @param populationFile Population file.
	 * @param GVAFile GVA file.
	 * @param elasticitiesFile Elasticities file.
	 * @param domesticAirportFareIndexFile Domestic airport fare index.
	 * @param internationalAirportFareIndexFile International airport fare index.
	 * @param domesticTripRatesFile Domestic trip rates file.
	 * @param internationalTripRatesFile International trip rates file.
	 * @param interventions List of interventions.
	 * @param props Properties.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public AirDemandModel(String domesticAirportsFile, String internationalAirportsFile, String baseYearDomesticPassengerFile, String baseYearInternationalPassengerFile, String populationFile, String GVAFile, String elasticitiesFile,
						  String domesticAirportFareIndexFile, String internationalAirportFareIndexFile, String domesticTripRatesFile, String internationalTripRatesFile, 
						  List<Intervention> interventions, Properties props) throws FileNotFoundException, IOException {

		this.interventions = interventions;
		this.props = props;
		
		domesticAirports = InputFileReader.readDomesticAirportsFile(domesticAirportsFile);
		internationalAirports = InputFileReader.readInternationalAirportsFile(internationalAirportsFile);
		
		//read all year population predictions
		this.yearToZoneToPopulation = InputFileReader.readPopulationFile(populationFile);
		
		//read all year GVA predictions
		this.yearToZoneToGVA = InputFileReader.readGVAFile(GVAFile);

		//read elasticities file
		this.elasticities = InputFileReader.readAirElasticitiesFile(elasticitiesFile);

		//read airport fares
		this.yearToDomesticAirportFareIndex = InputFileReader.readAirportFareIndexFile(domesticAirportFareIndexFile);
		this.yearToInternationalAirportFareIndex = InputFileReader.readAirportFareIndexFile(internationalAirportFareIndexFile);

		//read trip rates
		this.yearToDomesticTripRates = InputFileReader.readTripRatesFile(domesticTripRatesFile);
		this.yearToInternationalTripRates = InputFileReader.readTripRatesFile(internationalTripRatesFile);

		//create map to store air pasenger demands for all years
		this.yearToDomesticPassengerDemand = new HashMap<Integer, InternodalPassengerDemand>();
		this.yearToInternationalPassengerDemand = new HashMap<Integer, InternodalPassengerDemand>();

		//read the parameters
		baseYear = Integer.parseInt(props.getProperty("baseYear"));

		//read base-year air passenger demand
		InternodalPassengerDemand baseYearDomesticDemand = new DomesticInternodalPassengerDemand(baseYearDomesticPassengerFile);
		InternodalPassengerDemand baseYearInternationalDemand = new InternationalInternodalPassengerDemand(baseYearInternationalPassengerFile);
		
		this.yearToDomesticPassengerDemand.put(baseYear, baseYearDomesticDemand);
		this.yearToInternationalPassengerDemand.put(baseYear, baseYearInternationalDemand);
				
		//save base-year passenger air demand to output folder
		this.saveAllResults(baseYear);
		
		//install interventions (if not already installed)
		if (interventions != null) 
			for (Intervention i: interventions)
				if (!i.getState())				
					i.install(this);
	}

	/**
	 * Predicts air passenger demands (domestic and international)
	 * up to toYear (if flag is true, also intermediate years) and saves results.
	 * @param toYear The final year for which the demand is predicted.
	 * @param fromYear The year from which the predictions are made.
	 */
	public void predictAndSaveAirDemands(int toYear, int fromYear) {

		Boolean flagPredictIntermediateYearsAir = Boolean.parseBoolean(this.props.getProperty("FLAG_PREDICT_INTERMEDIATE_YEARS_AIR"));

		if (flagPredictIntermediateYearsAir) { //predict all intermediate years
			for (int year = fromYear; year <= toYear - 1; year++) {
				this.predictDomesticAirDemandUsingResultsOfFromYear(year + 1, year);
				this.predictInternationalAirDemandUsingResultsOfFromYear(year + 1, year);
				this.saveAllResults(year+1);
			}
		} else { //predict only final year
			this.predictDomesticAirDemandUsingResultsOfFromYear(toYear, fromYear);
			this.predictInternationalAirDemandUsingResultsOfFromYear(toYear, fromYear);
			this.saveAllResults(toYear);
		}
	}
	
	/**
	 * Predicts domestic air passenger internodal demand.
	 * Uses already existing results of the fromYear, from the output folder.
	 * @param predictedYear The year for which the demand is predicted.
	 * @param fromYear The year from which demand the prediction is made.
	 */
	public void predictDomesticAirDemandUsingResultsOfFromYear(int predictedYear, int fromYear) {

		LOGGER.info("Predicting {} domestic air passenger demand from existing {} demand.", predictedYear, fromYear);

		if (predictedYear < fromYear) {
			LOGGER.error("predictedYear should not be smaller than fromYear!");
			return;
		} 
		
		if (predictedYear == fromYear) return; //skip the rest if predicting the same year
		
		//use output folder for input folder
		String inputFolder = this.props.getProperty("outputFolder") + File.separator + fromYear;
		
		String predictedAirDemandFile = this.props.getProperty("predictedDomesticAirPassengerDemandFile");
		if (fromYear == baseYear) { //use different file names for base year
			predictedAirDemandFile = "baseYearDomesticAirPassengerDemand.csv";
		}
		
		//load air demand from fromYear
		LOGGER.info("Loading output data (air demand) from year {}", fromYear);
		
		try {
			String inputFile = inputFolder + File.separator + predictedAirDemandFile;
			InternodalPassengerDemand fromDemand = new DomesticInternodalPassengerDemand(inputFile);
			this.yearToDomesticPassengerDemand.put(fromYear, fromDemand);
		} catch (Exception e) {
			LOGGER.error("Error while reading domestic air passenger output file from year {}.", fromYear);
			LOGGER.error(e);
			return;
		}
		
		//old demand
		InternodalPassengerDemand fromDemand = this.yearToDomesticPassengerDemand.get(fromYear);
		if (fromDemand == null) {
			LOGGER.error("Domestic air passenger demand for year {} does not exist!", fromYear);
			return;
		}

		//predicted demand	
		InternodalPassengerDemand predictedDemand = this.yearToDomesticPassengerDemand.get(predictedYear);
		if (predictedDemand == null) {
			predictedDemand = new DomesticInternodalPassengerDemand();	
		}
		
		//trip rate factor is calculated by multiplying all relative changes from (fromYear+1) to predictedYear.
		double tripRate = 1.0; 
		for (int year = fromYear+1; year <= predictedYear; year++)		
				tripRate *= this.yearToDomesticTripRates.get(year);
					
		//PREDICTION
		//for each airport pair predict changes from the variables
		for (Object mk: fromDemand.data.keySet()) {
			
			String firstIATA = (String) ((MultiKey)mk).getKey(0);
			String secondIATA = (String) ((MultiKey)mk).getKey(1);
			
			String firstZone = ((DomesticAirport) domesticAirports.get(firstIATA)).getLADCode();
			String firstZoneName = ((DomesticAirport) domesticAirports.get(firstIATA)).getLADName();

			String secondZone = ((DomesticAirport) domesticAirports.get(secondIATA)).getLADCode();
			String secondZoneName = ((DomesticAirport) domesticAirports.get(secondIATA)).getLADName();
			
			long oldUsage = fromDemand.getDemand(firstIATA, secondIATA).get(Passengers.TOTAL);
			
			Integer oldPopulationFirstZone = this.yearToZoneToPopulation.get(fromYear).get(firstZone);
			Integer oldPopulationSecondZone = this.yearToZoneToPopulation.get(fromYear).get(secondZone);
			Integer newPopulationFirstZone = this.yearToZoneToPopulation.get(predictedYear).get(firstZone);
			Integer newPopulationSecondZone = this.yearToZoneToPopulation.get(predictedYear).get(secondZone);
			
			if (oldPopulationFirstZone == null) LOGGER.warn("Missing {} population in zone {} ({}).", fromYear, firstZone, firstZoneName);
			if (oldPopulationSecondZone == null) LOGGER.warn("Missing {} population in zone {} ({}).", fromYear, secondZone, secondZoneName);
			if (newPopulationFirstZone == null) LOGGER.warn("Missing {} population in zone {} ({}).", predictedYear, firstZone, firstZoneName);
			if (newPopulationSecondZone == null) LOGGER.warn("Missing {} population in zone {} ({}).", predictedYear, secondZone, secondZoneName);
			
			Double oldGVAFirstZone = this.yearToZoneToGVA.get(fromYear).get(firstZone);
			Double oldGVASecondZone = this.yearToZoneToGVA.get(fromYear).get(secondZone);
			Double newGVAFirstZone = this.yearToZoneToGVA.get(predictedYear).get(firstZone);
			Double newGVASecondZone = this.yearToZoneToGVA.get(predictedYear).get(secondZone);
			
			if (oldGVAFirstZone == null) LOGGER.warn("Missing {} GVA per head in zone {} ({}).", fromYear, firstZone, firstZoneName);
			if (oldGVASecondZone == null) LOGGER.warn("Missing {} GVA per head in zone {} ({}).", fromYear, secondZone, secondZoneName);
			if (newGVAFirstZone == null) LOGGER.warn("Missing {} GVA per head in zone {} ({}).", predictedYear, firstZone, firstZoneName);
			if (newGVASecondZone == null) LOGGER.warn("Missing {} GVA per head in zone {} ({}).", predictedYear, secondZone, secondZoneName);
		
			Double oldFareIndexFirstAirport = this.yearToDomesticAirportFareIndex.get(fromYear).get(firstIATA);
			Double oldFareIndexSecondAirport = this.yearToDomesticAirportFareIndex.get(fromYear).get(secondIATA);
			Double newFareIndexFirstAirport = this.yearToDomesticAirportFareIndex.get(predictedYear).get(firstIATA);
			Double newFareIndexSecondAirport = this.yearToDomesticAirportFareIndex.get(predictedYear).get(secondIATA);
			
			//first assume a 1.0 ratio
			double populationRatio = 1.0;
			double GVARatio = 1.0;
			double fareIndexRatio = 1.0;

			//if all data is available calculate the actual ratio (otherwise assume 1.0)
			if (newPopulationFirstZone != null && newPopulationSecondZone != null && oldPopulationFirstZone != null && oldPopulationSecondZone != null)
				populationRatio = (double) (newPopulationFirstZone + newPopulationSecondZone) / (oldPopulationFirstZone + oldPopulationSecondZone);
			
			if (newGVAFirstZone != null && newGVASecondZone != null && oldGVAFirstZone != null && oldGVASecondZone != null)
				GVARatio = (double) (newGVAFirstZone + newGVASecondZone) / (oldGVAFirstZone + oldGVASecondZone);
			
			if (newFareIndexFirstAirport != null && newFareIndexSecondAirport != null && oldFareIndexFirstAirport != null && oldFareIndexSecondAirport != null)
				fareIndexRatio = (double) (newFareIndexFirstAirport + newFareIndexSecondAirport) / (oldFareIndexFirstAirport + oldFareIndexSecondAirport);
			
			//predict station usage
			long predictedUsage = (long) Math.round(oldUsage * tripRate 
												* Math.pow(populationRatio, elasticities.get(ElasticityTypes.POPULATION))
												* Math.pow(GVARatio, elasticities.get(ElasticityTypes.GVA))
												* Math.pow(fareIndexRatio, elasticities.get(ElasticityTypes.COST_DOMESTIC)));

			if (predictedUsage == 0 && oldUsage != 0) 
				predictedUsage = 1; //stops demand from disappearing (unless oldUsage was also 0)

			LOGGER.trace("Old yearly usage: {}, new yearly usage: {}", oldUsage, predictedUsage);

			//store in predicted demand
			predictedDemand.setDemand(firstIATA, secondIATA, predictedUsage, 0, 0); //we are predicting only the total
		}
		
		//store predicted demand
		this.yearToDomesticPassengerDemand.put(predictedYear, predictedDemand);

		LOGGER.debug("Finished predicting {} domestic air demand from {} demand.", predictedYear, fromYear);
	}
	
	/**
	 * Predicts international air passenger internodal demand.
	 * Uses already existing results of the fromYear, from the output folder.
	 * @param predictedYear The year for which the demand is predicted.
	 * @param fromYear The year from which demand the prediction is made.
	 */
	public void predictInternationalAirDemandUsingResultsOfFromYear(int predictedYear, int fromYear) {

		LOGGER.info("Predicting {} international air passenger demand from existing {} demand.", predictedYear, fromYear);

		if (predictedYear < fromYear) {
			LOGGER.error("predictedYear should not be smaller than fromYear!");
			return;
		} 
		
		if (predictedYear == fromYear) return; //skip the rest if predicting the same year
		
		//use output folder for input folder
		String inputFolder = this.props.getProperty("outputFolder") + File.separator + fromYear;
		
		String predictedAirDemandFile = this.props.getProperty("predictedInternationalAirPassengerDemandFile");
		if (fromYear == baseYear) { //use different file names for base year
			predictedAirDemandFile = "baseYearInternationalAirPassengerDemand.csv";
		}
		
		//load air demand from fromYear
		LOGGER.info("Loading output data (air demand) from year {}", fromYear);
		
		try {
			String inputFile = inputFolder + File.separator + predictedAirDemandFile;
			InternodalPassengerDemand fromDemand = new InternationalInternodalPassengerDemand(inputFile);
			this.yearToInternationalPassengerDemand.put(fromYear, fromDemand);
		} catch (Exception e) {
			LOGGER.error("Error while reading international air passenger output file from year {}.", fromYear);
			LOGGER.error(e);
			return;
		}
		
		//old demand
		InternodalPassengerDemand fromDemand = this.yearToInternationalPassengerDemand.get(fromYear);
		if (fromDemand == null) {
			LOGGER.error("International air passenger demand for year {} does not exist!", fromYear);
			return;
		}

		//predicted demand	
		InternodalPassengerDemand predictedDemand = this.yearToInternationalPassengerDemand.get(predictedYear);
		if (predictedDemand == null) {
			predictedDemand = new InternationalInternodalPassengerDemand();	
		}
		
		//trip rate factor is calculated by multiplying all relative changes from (fromYear+1) to predictedYear.
		double tripRate = 1.0; 
		for (int year = fromYear+1; year <= predictedYear; year++)		
				tripRate *= this.yearToInternationalTripRates.get(year);
					
		//PREDICTION
		//for each airport pair predict changes from the variables
		for (Object mk: fromDemand.data.keySet()) {
			
			String firstIATA = (String) ((MultiKey)mk).getKey(0); //domestic
			String secondIATA = (String) ((MultiKey)mk).getKey(1); //international
			
			String firstZone = ((DomesticAirport) domesticAirports.get(firstIATA)).getLADCode();
			String firstZoneName = ((DomesticAirport) domesticAirports.get(firstIATA)).getLADName();
			
			long oldUsage = fromDemand.getDemand(firstIATA, secondIATA).get(Passengers.TOTAL);
			
			Integer oldPopulationFirstZone = this.yearToZoneToPopulation.get(fromYear).get(firstZone);
			Integer newPopulationFirstZone = this.yearToZoneToPopulation.get(predictedYear).get(firstZone);
			
			if (oldPopulationFirstZone == null) LOGGER.warn("Missing {} population in zone {} ({}).", fromYear, firstZone, firstZoneName);
			if (newPopulationFirstZone == null) LOGGER.warn("Missing {} population in zone {} ({}).", predictedYear, firstZone, firstZoneName);
		
			Double oldGVAFirstZone = this.yearToZoneToGVA.get(fromYear).get(firstZone);
			Double newGVAFirstZone = this.yearToZoneToGVA.get(predictedYear).get(firstZone);
			
			if (oldGVAFirstZone == null) LOGGER.warn("Missing {} GVA per head in zone {} ({}).", fromYear, firstZone, firstZoneName);
			if (newGVAFirstZone == null) LOGGER.warn("Missing {} GVA per head in zone {} ({}).", predictedYear, firstZone, firstZoneName);

			Double oldFareIndexFirstAirport = this.yearToDomesticAirportFareIndex.get(fromYear).get(firstIATA);
			Double oldFareIndexSecondAirport = this.yearToInternationalAirportFareIndex.get(fromYear).get(secondIATA);
			Double newFareIndexFirstAirport = this.yearToDomesticAirportFareIndex.get(predictedYear).get(firstIATA);
			Double newFareIndexSecondAirport = this.yearToInternationalAirportFareIndex.get(predictedYear).get(secondIATA);
			
			//first assume a 1.0 ratio
			double populationRatio = 1.0;
			double GVARatio = 1.0;
			double fareIndexRatio = 1.0;

			//if all data is available calculate the actual ratio (otherwise assume 1.0)
			if (newPopulationFirstZone != null && oldPopulationFirstZone != null)
				populationRatio = (double) newPopulationFirstZone / oldPopulationFirstZone;
			
			if (newGVAFirstZone != null && oldGVAFirstZone != null)
				GVARatio = (double) (newGVAFirstZone / oldGVAFirstZone);
			
			if (newFareIndexFirstAirport != null && newFareIndexSecondAirport != null && oldFareIndexFirstAirport != null && oldFareIndexSecondAirport != null)
				fareIndexRatio = (double) (newFareIndexFirstAirport + newFareIndexSecondAirport) / (oldFareIndexFirstAirport + oldFareIndexSecondAirport);

			//predict station usage
			long predictedUsage = (long) Math.round(oldUsage * tripRate 
												* Math.pow(populationRatio, elasticities.get(ElasticityTypes.POPULATION))
												* Math.pow(GVARatio, elasticities.get(ElasticityTypes.GVA))
												* Math.pow(fareIndexRatio, elasticities.get(ElasticityTypes.COST_INTERNATIONAL)));

			if (predictedUsage == 0 && oldUsage != 0) 
				predictedUsage = 1; //stops demand from disappearing (unless oldUsage was also 0)

			LOGGER.trace("Old yearly usage: {}, new yearly usage: {}", oldUsage, predictedUsage);

			//store in predicted demand
			predictedDemand.setDemand(firstIATA, secondIATA, predictedUsage, 0, 0); //we are predicting only the total
		}
		
		//store predicted demand
		this.yearToInternationalPassengerDemand.put(predictedYear, predictedDemand);

		LOGGER.debug("Finished predicting {} international air demand from {} demand.", predictedYear, fromYear);
	}
	
	/**
	 * Saves all results into the output folder.
	 * @param year Year of the data.
	 */
	public void saveAllResults (int year) {
		
		LOGGER.info("Outputing all air model results for year {}.", year);

		String outputFolder = this.props.getProperty("outputFolder");
				
		//create output directory for this year
		File file = new File(outputFolder + year);
		if (!file.exists()) {
			if (file.mkdirs()) {
				LOGGER.debug("Output directory for year {} is created.", year);
			} else {
				LOGGER.error("Failed to create output directory for year {}.", year);
			}
		}
		LOGGER.debug("Output folder: {}", file.getPath());
						
		String baseYear = this.props.getProperty("baseYear");
	
		String predictedAirDemandFile = props.getProperty("predictedDomesticAirPassengerDemandFile");
		if (year == Integer.parseInt(baseYear)) { //rename output files for base year
			predictedAirDemandFile = "baseYearDomesticAirPassengerDemand.csv";
		}
		
		String outputFile = file.getPath() + File.separator + predictedAirDemandFile;
		this.yearToDomesticPassengerDemand.get(year).saveAirPassengerDemand(year, outputFile);
		
		predictedAirDemandFile = props.getProperty("predictedInternationalAirPassengerDemandFile");
		if (year == Integer.parseInt(baseYear)) { //rename output files for base year
			predictedAirDemandFile = "baseYearInternationalAirPassengerDemand.csv";
		}
		
		outputFile = file.getPath() + File.separator + predictedAirDemandFile;
		this.yearToInternationalPassengerDemand.get(year).saveAirPassengerDemand(year, outputFile);
	}

	/**
	 * Getter method for the air passenger demand in a given year.
	 * @param year Year for which the demand is requested.
	 * @return Air passenger demand.
	 */
	public InternodalPassengerDemand getDomesticAirPassengerDemand (int year) {

		return this.yearToDomesticPassengerDemand.get(year);
	}
	
	/**
	 * Getter method for the air passenger demand in a given year.
	 * @param year Year for which the demand is requested.
	 * @return Air passenger demand.
	 */
	public InternodalPassengerDemand getInternationalAirPassengerDemand (int year) {

		return this.yearToInternationalPassengerDemand.get(year);
	}
	
	/**
	 * Saves domestic air demand to an output file.
	 * @param year Year of the data.
	 * @param outputFile Output file name (with path).
	 */
	public void saveDomesticAirDemand(int year, String outputFile) {
		
		this.yearToDomesticPassengerDemand.get(year).saveAirPassengerDemand(year, outputFile);
	}
	
	/**
	 * Saves international air demand to an output file.
	 * @param year Year of the data.
	 * @param outputFile Output file name (with path).
	 */
	public void saveInternationalAirDemand(int year, String outputFile) {
		
		this.yearToInternationalPassengerDemand.get(year).saveAirPassengerDemand(year, outputFile);
	}
}