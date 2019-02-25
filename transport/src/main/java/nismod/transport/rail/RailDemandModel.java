package nismod.transport.rail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;

import nismod.transport.decision.Intervention;
import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrixMultiKey;
import nismod.transport.demand.SkimMatrix;
import nismod.transport.demand.SkimMatrixFreight;
import nismod.transport.demand.SkimMatrixFreightArray;
import nismod.transport.demand.SkimMatrixMultiKey;
import nismod.transport.utility.InputFileReader;

public class RailDemandModel {

	private final static Logger LOGGER = LogManager.getLogger(RailDemandModel.class);

	public static int baseYear;

	public static enum ElasticityTypes {
		POPULATION, //population
		GVA, //income
		COST_RAIL, //rail fare
		COST_CAR, //passenger car trip cost (fuel + congestion)
		TIME, //generalised journey time
	}
	
	public static enum ElasticityArea {
		LT, //London Travelcard
		SE, //South East
		PTE, //Passenger Transport Executives
		OTHER //Other areas
	}
	
	private Map<ElasticityTypes, Map<ElasticityArea, Double>> elasticities;

	private HashMap<Integer, RailStationDemand> yearToRailDemand; //rail demand

	//zonal data
	private HashMap<Integer, HashMap<String, Integer>> yearToZoneToPopulation;
	private HashMap<Integer, HashMap<String, Double>> yearToZoneToGVA;
	private HashMap<Integer, HashMap<String, Double>> yearToCarCosts;

	//station data
	private HashMap<Integer, HashMap<Integer, Double>> yearToStationFares; //journey fares
	private HashMap<Integer, HashMap<Integer, Double>> yearToStationGJTs; //generalised journey times
	
	//trip rates
	private HashMap<Integer, Double> yearToTripRate; //trip rates

	private List<Intervention> interventions;
	private Properties props;

	/**
	 * Constructor for the rail demand model.
	 * @param baseYearRailStationUsageFile Base year rail station usage file (demand).
	 * @param populationFile Population file.
	 * @param GVAFile GVA file.
	 * @param elasticitiesFile Elasticites file.
	 * @param railStationJourneyFaresFile Rail fares file.
	 * @param railStationGeneralisedJourneyTimesFile GJT file.
	 * @param carZonalJourneyCostsFile Zonal car journey costs file.
	 * @param railTripRatesFile Rail trip rates file.
	 * @param interventions List of interventions.
	 * @param props Properties.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public RailDemandModel(String baseYearRailStationUsageFile, String populationFile, String GVAFile, String elasticitiesFile, String railStationJourneyFaresFile, String railStationGeneralisedJourneyTimesFile, String carZonalJourneyCostsFile, String railTripRatesFile, List<Intervention> interventions, Properties props) throws FileNotFoundException, IOException {

		this.interventions = interventions;
		this.props = props;
		
		//read all year population predictions
		this.yearToZoneToPopulation = InputFileReader.readPopulationFile(populationFile);
		//read all year GVA predictions
		this.yearToZoneToGVA = InputFileReader.readGVAFile(GVAFile);
		//read elasticities file
		this.elasticities = InputFileReader.readRailElasticitiesFile(elasticitiesFile);

		//check if car travel costs should be used from the input file or from the output of the road traffic model
		boolean flagUseCarCostsFromRoadModel = Boolean.parseBoolean(props.getProperty("FLAG_USE_CAR_COST_FROM_ROAD_MODEL"));
		if (!flagUseCarCostsFromRoadModel) { //if not using costs from the road model, read them from an input file
			//read zonal car costs
			this.yearToCarCosts = InputFileReader.readZonalCarCostsFile(carZonalJourneyCostsFile);
		} else {
			this.yearToCarCosts = new HashMap<Integer, HashMap<String, Double>>();
		}
		
		//read station journey fares
		this.yearToStationFares = InputFileReader.readRailStationCostsFile(railStationJourneyFaresFile);
		//read station generalised journey times
		this.yearToStationGJTs = InputFileReader.readRailStationCostsFile(railStationGeneralisedJourneyTimesFile);
		
		//read trip rates
		this.yearToTripRate = InputFileReader.readRailTripRatesFile(railTripRatesFile);

		//create map to store rail station demands for all years
		this.yearToRailDemand = new HashMap<Integer, RailStationDemand>();

		//read the parameters
		this.baseYear = Integer.parseInt(props.getProperty("baseYear"));

		//read base-year passenger rail demand
		RailStationDemand baseYearDemand = new RailStationDemand(baseYearRailStationUsageFile);
		this.yearToRailDemand.put(this.baseYear, baseYearDemand);
		
		//save base-year passenger rail demand to output folder
		this.saveAllResults(baseYear);
		
		//install interventions (if not already installed)
		if (interventions != null) 
			for (Intervention i: interventions)
				if (!i.getState())				
					i.install(this);
	}

	/**
	 * Predicts rail station demand (total passenger counts at each station)
	 * for all years from baseYear to toYear.
	 * @param toYear The final year for which the demand is predicted.
	 * @param baseYear The base year from which the predictions are made.
	 */
	public void predictRailwayDemands(int toYear, int baseYear) {

		Boolean flagPredictIntermediateYearsRail = Boolean.parseBoolean(this.props.getProperty("FLAG_PREDICT_INTERMEDIATE_YEARS_RAIL"));

		if (flagPredictIntermediateYearsRail) { //predict all intermediate years
			for (int year = baseYear; year <= toYear - 1; year++) {
				this.predictRailwayDemand(year + 1, year);
			}
		} else { //predict only final year
			this.predictRailwayDemand(toYear, baseYear);
		}
	}

	/**
	 * Predicts passenger railway demand (passenger counts at each station).
	 * @param predictedYear The year for which the demand is predicted.
	 * @param fromYear The year from which demand the prediction is made.
	 */
	public void predictRailwayDemand(int predictedYear, int fromYear) {

		LOGGER.info("Predicting {} railway demand from {} demand.", predictedYear, fromYear);

		if (predictedYear < fromYear) {
			LOGGER.error("predictedYear should not be smaller than fromYear!");
			return;
			//check if the demand from year fromYear exists
		} else if (!this.yearToRailDemand.containsKey(fromYear)) { 
			LOGGER.error("Passenger rail demand from year {} does not exist!", fromYear);
			return;
		} else if (!this.yearToRailDemand.containsKey(fromYear)) { 
			LOGGER.error("Passenger rail demand from year {} does not exist!", fromYear);
			return;
		}

		if (predictedYear == fromYear) return; //skip the rest if predicting the same year

		//old demand
		RailStationDemand fromDemand = this.yearToRailDemand.get(fromYear);
		if (fromDemand == null) {
			LOGGER.error("Rail demand for year {} does not exist!", fromYear);
			return;
		}

		//predicted demand	
		RailStationDemand predictedDemand = this.yearToRailDemand.get(predictedYear);
		if (predictedDemand == null) {
			predictedDemand = new RailStationDemand(fromDemand.getHeader());	
		}
		
		//check if car travel costs should be used from the input file or from the output of the road traffic model
		boolean flagUseCarCostsFromRoadModel = Boolean.parseBoolean(this.props.getProperty("FLAG_USE_CAR_COST_FROM_ROAD_MODEL"));
		if (flagUseCarCostsFromRoadModel) {
			
			LOGGER.debug("Using car costs from road traffic model: {}", flagUseCarCostsFromRoadModel);
			
			//erase data read from file
			this.yearToCarCosts = new HashMap<Integer, HashMap<String, Double>>();
			
			String outputFolder = this.props.getProperty("outputFolder");
			String costsFile = this.props.getProperty("costSkimMatrixFile");
			String odMatrixFile;
			if (fromYear == this.baseYear) 
				odMatrixFile = "baseYearODMatrix.csv";
			else 
				odMatrixFile = this.props.getProperty("predictedODMatrixFile");
			
			//read OD matrix for fromYear
			ODMatrixMultiKey odMatrix = null;
			try {
				odMatrix = new ODMatrixMultiKey(outputFolder + File.separator + fromYear + File.separator + odMatrixFile);
			} catch (IOException e) {
				LOGGER.error(e);
				LOGGER.error("Unable to read car OD matrix file for year {}", fromYear);
			}
			//read cost skim matrix for fromYear
			SkimMatrixMultiKey carCostMatrix = null;
			try {
				carCostMatrix = new SkimMatrixMultiKey(outputFolder + File.separator + fromYear + File.separator + costsFile, null);
				carCostMatrix.printMatrixFormatted("Car cost matrix for year " + fromYear + ":");
			} catch (IOException e) {
				LOGGER.error(e);
				LOGGER.error("Unable to read car costs file for year {}", fromYear);
			}
			
			HashMap<String, Double> fromYearCarCosts = carCostMatrix.getAverageZonalCosts(odMatrix.getUnsortedOrigins(), odMatrix);
			System.out.println("Zonal car costs in year " + fromYear + ":" + fromYearCarCosts);
			this.yearToCarCosts.put(fromYear, fromYearCarCosts);
					
			//read OD matrix
			odMatrixFile = this.props.getProperty("predictedODMatrixFile");
			try {
				odMatrix = new ODMatrixMultiKey(outputFolder + File.separator + predictedYear + File.separator + odMatrixFile);
			} catch (IOException e) {
				LOGGER.error(e);
				LOGGER.error("Unable to read car OD matrix file for year {}", predictedYear);
			}
			
			try {
				carCostMatrix = new SkimMatrixMultiKey(outputFolder + File.separator + predictedYear + File.separator + costsFile, null);
				carCostMatrix.printMatrixFormatted("Car cost matrix for year " + predictedYear + ":");
			} catch (IOException e) {
				LOGGER.error(e);
				LOGGER.error("Unable to read car costs file for year {}", predictedYear);
			}
			
			HashMap<String, Double> predictedYearCarCosts = carCostMatrix.getAverageZonalCosts(odMatrix.getUnsortedOrigins(), odMatrix);
			System.out.println("Zonal car costs in year " + predictedYear + ":" + predictedYearCarCosts);
			this.yearToCarCosts.put(predictedYear, predictedYearCarCosts);
		}
		
		//get trip rate for predicted year
		double tripRate = this.yearToTripRate.get(predictedYear);
					
		//PREDICTION
		//for each station predict changes from the variables
		for (RailStation station: fromDemand.getRailDemandList()) {

			int oldUsage = station.getYearlyUsage();
			int nlc = station.getNLC();
			String zone = station.getLADCode();
			
			int oldPopulationRailStationZone = this.yearToZoneToPopulation.get(fromYear).get(zone);
			int newPopulationRailStationZone = this.yearToZoneToPopulation.get(predictedYear).get(zone);

			double oldGVARailStationZone = this.yearToZoneToGVA.get(fromYear).get(zone);
			double newGVARailStationZone = this.yearToZoneToGVA.get(predictedYear).get(zone);

			double oldCarCostsRailStationZone = this.yearToCarCosts.get(fromYear).get(zone);
			double newCarCostsRailStationZone = this.yearToCarCosts.get(predictedYear).get(zone);

			double oldFaresRailStation = this.yearToStationFares.get(fromYear).get(nlc);
			double newFaresRailStation = this.yearToStationFares.get(predictedYear).get(nlc);

			double oldGJTRailStationZone = this.yearToStationGJTs.get(fromYear).get(nlc);
			double newGJTRailStationZone = this.yearToStationGJTs.get(predictedYear).get(nlc);

			//predict station usage
			int predictedUsage = (int) Math.round(oldUsage * tripRate 
												* Math.pow((double) newPopulationRailStationZone / oldPopulationRailStationZone, elasticities.get(ElasticityTypes.POPULATION).get(station.getArea()))
												* Math.pow((double) newGVARailStationZone / oldGVARailStationZone, elasticities.get(ElasticityTypes.GVA).get(station.getArea()))
												* Math.pow((double) newCarCostsRailStationZone / oldCarCostsRailStationZone, elasticities.get(ElasticityTypes.COST_CAR).get(station.getArea()))
												* Math.pow((double) newFaresRailStation / oldFaresRailStation, elasticities.get(ElasticityTypes.COST_RAIL).get(station.getArea()))
												* Math.pow((double) newGJTRailStationZone / oldGJTRailStationZone, elasticities.get(ElasticityTypes.TIME).get(station.getArea())));

			if (predictedUsage == 0 && oldUsage != 0) 
				predictedUsage = 1; //stops demand from disappearing (unless oldUsage was also 0)

			LOGGER.trace("Old yearly usage: {}, new yearly usage: {}", oldUsage, predictedUsage);

			//create a station copy
			RailStation predictedStation = new RailStation(station);
			predictedStation.setYearlyUsage(predictedUsage);

			double predictedDayUsage = (double) predictedUsage / station.getRunDays();
			predictedStation.setDailyUsage(predictedDayUsage);

			//store in predicted demand
			predictedDemand.addStation(predictedStation);
		}
		
		//store predicted demand
		this.yearToRailDemand.put(predictedYear, predictedDemand);

		LOGGER.debug("Finished predicting {} railway demand from {} demand.", predictedYear, fromYear);

		//print from demand and predicted demand
		fromDemand.printRailDemandNLCSorted("From demand (year " + fromYear + "):");
		predictedDemand.printRailDemandNLCSorted("Predicted demand (year " + predictedYear + "):");
	}
	
	/**
	 * Predicts passenger railway demand (passenger counts at each station).
	 * Uses already existing results of the fromYear.
	 * @param predictedYear The year for which the demand is predicted.
	 * @param fromYear The year from which demand the prediction is made.
	 */
	public void predictRailwayDemandUsingResultsOfFromYear(int predictedYear, int fromYear) {

		LOGGER.info("Predicting {} railway demand from existing {} demand.", predictedYear, fromYear);

		if (predictedYear < fromYear) {
			LOGGER.error("predictedYear should not be smaller than fromYear!");
			return;
		} 
		
		if (predictedYear == fromYear) return; //skip the rest if predicting the same year

		//use output folder for input folder
		String inputFolder = this.props.getProperty("outputFolder") + File.separator + fromYear;
		
		String predictedRailDemandFile = this.props.getProperty("predictedRailDemandFile");
		if (fromYear == baseYear) { //use different file names for base year
			predictedRailDemandFile = "baseYearRailDemand.csv";
		}
		
		//load rail demand from fromYear
		LOGGER.info("Loading output data (rail demand) from year {}", fromYear);
		
		try {
			String inputFile = inputFolder + File.separator + predictedRailDemandFile;
			RailStationDemand fromDemand = new RailStationDemand(inputFile);
			this.yearToRailDemand.put(fromYear, fromDemand);
		} catch (Exception e) {
			LOGGER.error("Error while reading rail demand output file from year {}.", fromYear);
			LOGGER.error(e);
			return;
		}
		
		//old demand
		RailStationDemand fromDemand = this.yearToRailDemand.get(fromYear);
		if (fromDemand == null) {
			LOGGER.error("Rail demand for year {} does not exist!", fromYear);
			return;
		}

		//predicted demand	
		RailStationDemand predictedDemand = this.yearToRailDemand.get(predictedYear);
		if (predictedDemand == null) {
			predictedDemand = new RailStationDemand(fromDemand.getHeader());	
		}
		
		//check if car travel costs should be used from the input file or from the output of the road traffic model
		boolean flagUseCarCostsFromRoadModel = Boolean.parseBoolean(this.props.getProperty("FLAG_USE_CAR_COST_FROM_ROAD_MODEL"));
		if (flagUseCarCostsFromRoadModel) {
			
			LOGGER.info("Using car costs from road traffic model: {}", flagUseCarCostsFromRoadModel);
			
			//erase data read from file
			this.yearToCarCosts = new HashMap<Integer, HashMap<String, Double>>();
			
			String outputFolder = this.props.getProperty("outputFolder");
			String costsFile = this.props.getProperty("costSkimMatrixFile");
			String odMatrixFile;
			if (fromYear == this.baseYear) 
				odMatrixFile = "baseYearODMatrix.csv";
			else 
				odMatrixFile = this.props.getProperty("predictedODMatrixFile");
			
			//read OD matrix for fromYear
			ODMatrixMultiKey odMatrix = null;
			try {
				odMatrix = new ODMatrixMultiKey(outputFolder + File.separator + fromYear + File.separator + odMatrixFile);
			} catch (IOException e) {
				LOGGER.error(e);
				LOGGER.error("Unable to read car OD matrix file for year {}. Make sure to run road model first or set FLAG_USE_CAR_COST_FROM_ROAD_MODEL to false.", fromYear);
				return;
			}
			//read cost skim matrix for fromYear
			SkimMatrixMultiKey carCostMatrix = null;
			try {
				carCostMatrix = new SkimMatrixMultiKey(outputFolder + File.separator + fromYear + File.separator + costsFile, null);
				carCostMatrix.printMatrixFormatted("Car cost matrix for year " + fromYear + ":");
			} catch (IOException e) {
				LOGGER.error(e);
				LOGGER.error("Unable to read car costs file for year {}. Make sure to run road model first or set FLAG_USE_CAR_COST_FROM_ROAD_MODEL to false.", fromYear);
				return;
			}
			
			HashMap<String, Double> fromYearCarCosts = carCostMatrix.getAverageZonalCosts(odMatrix.getUnsortedOrigins(), odMatrix);
			System.out.println("Zonal car costs in year " + fromYear + ":" + fromYearCarCosts);
			this.yearToCarCosts.put(fromYear, fromYearCarCosts);
					
			//read OD matrix
			odMatrixFile = this.props.getProperty("predictedODMatrixFile");
			try {
				odMatrix = new ODMatrixMultiKey(outputFolder + File.separator + predictedYear + File.separator + odMatrixFile);
			} catch (IOException e) {
				LOGGER.error(e);
				LOGGER.error("Unable to read car OD matrix file for year {}. Make sure to run road model first or set FLAG_USE_CAR_COST_FROM_ROAD_MODEL to false.", predictedYear);
				return;
			}
			
			try {
				carCostMatrix = new SkimMatrixMultiKey(outputFolder + File.separator + predictedYear + File.separator + costsFile, null);
				carCostMatrix.printMatrixFormatted("Car cost matrix for year " + predictedYear + ":");
			} catch (IOException e) {
				LOGGER.error(e);
				LOGGER.error("Unable to read car costs file for year {}. Make sure to run road model first or set FLAG_USE_CAR_COST_FROM_ROAD_MODEL to false.", predictedYear);
				return;
			}
			
			HashMap<String, Double> predictedYearCarCosts = carCostMatrix.getAverageZonalCosts(odMatrix.getUnsortedOrigins(), odMatrix);
			System.out.println("Zonal car costs in year " + predictedYear + ":" + predictedYearCarCosts);
			this.yearToCarCosts.put(predictedYear, predictedYearCarCosts);
		}
		
		//get trip rate for predicted year
		double tripRate = this.yearToTripRate.get(predictedYear);
					
		//PREDICTION
		//for each station predict changes from the variables
		for (RailStation station: fromDemand.getRailDemandList()) {

			int oldUsage = station.getYearlyUsage();
			int nlc = station.getNLC();
			String zone = station.getLADCode();

			int oldPopulationRailStationZone = this.yearToZoneToPopulation.get(fromYear).get(zone);
			int newPopulationRailStationZone = this.yearToZoneToPopulation.get(predictedYear).get(zone);

			double oldGVARailStationZone = this.yearToZoneToGVA.get(fromYear).get(zone);
			double newGVARailStationZone = this.yearToZoneToGVA.get(predictedYear).get(zone);

			double oldCarCostsRailStationZone = this.yearToCarCosts.get(fromYear).get(zone);
			double newCarCostsRailStationZone = this.yearToCarCosts.get(predictedYear).get(zone);

			double oldFaresRailStation = this.yearToStationFares.get(fromYear).get(nlc);
			double newFaresRailStation = this.yearToStationFares.get(predictedYear).get(nlc);

			double oldGJTRailStationZone = this.yearToStationGJTs.get(fromYear).get(nlc);
			double newGJTRailStationZone = this.yearToStationGJTs.get(predictedYear).get(nlc);

			//predict station usage
			int predictedUsage = (int) Math.round(oldUsage * tripRate 
												* Math.pow((double) newPopulationRailStationZone / oldPopulationRailStationZone, elasticities.get(ElasticityTypes.POPULATION).get(station.getArea()))
												* Math.pow((double) newGVARailStationZone / oldGVARailStationZone, elasticities.get(ElasticityTypes.GVA).get(station.getArea()))
												* Math.pow((double) newCarCostsRailStationZone / oldCarCostsRailStationZone, elasticities.get(ElasticityTypes.COST_CAR).get(station.getArea()))
												* Math.pow((double) newFaresRailStation / oldFaresRailStation, elasticities.get(ElasticityTypes.COST_RAIL).get(station.getArea()))
												* Math.pow((double) newGJTRailStationZone / oldGJTRailStationZone, elasticities.get(ElasticityTypes.TIME).get(station.getArea())));

			if (predictedUsage == 0 && oldUsage != 0) 
				predictedUsage = 1; //stops demand from disappearing (unless oldUsage was also 0)

			LOGGER.trace("Old yearly usage: {}, new yearly usage: {}", oldUsage, predictedUsage);

			//create a station copy
			RailStation predictedStation = new RailStation(station);
			predictedStation.setYearlyUsage(predictedUsage);

			double predictedDayUsage = (double) predictedUsage / station.getRunDays();
			predictedStation.setDailyUsage(predictedDayUsage);

			//store in predicted demand
			predictedDemand.addStation(predictedStation);
		}
		
		//store predicted demand
		this.yearToRailDemand.put(predictedYear, predictedDemand);

		LOGGER.debug("Finished predicting {} railway demand from {} demand.", predictedYear, fromYear);

		//print from demand and predicted demand
		fromDemand.printRailDemandNLCSorted("From demand (year " + fromYear + "):");
		predictedDemand.printRailDemandNLCSorted("Predicted demand (year " + predictedYear + "):");
	}
	
	/**
	 * Saves all results into the output folder.
	 * @param year Year of the data.
	 */
	public void saveAllResults (int year) {
		
		LOGGER.info("Outputing all rail model results for year {}.", year);

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
	
		String predictedRailDemandFile = props.getProperty("predictedRailDemandFile");
		if (year == Integer.parseInt(baseYear)) { //rename output files for base year
			predictedRailDemandFile = "baseYearRailDemand.csv";
		}
		
		String outputFile = file.getPath() + File.separator + predictedRailDemandFile;
		this.yearToRailDemand.get(year).saveRailStationDemand(year, outputFile);
		
		String zonalRailDemandFile = props.getProperty("zonalRailDemandFile");
		outputFile = file.getPath() + File.separator + zonalRailDemandFile;
		this.yearToRailDemand.get(year).saveZonalRailStationDemand(year, outputFile);
	}

	/**
	 * Getter method for the passenger rail station demand in a given year.
	 * @param year Year for which the demand is requested.
	 * @return Rail station demand with total passenger counts (entry + exit).
	 */
	public RailStationDemand getRailStationDemand (int year) {

		return this.yearToRailDemand.get(year);
	}
	
	/**
	 * Setter method for the passenger rail station demand in a given year.
	 * @param year Year for which the demand is set.
	 * @param demand Rail station demand.
	 */
	public void setRailStationDemand (int year, RailStationDemand demand) {

		this.yearToRailDemand.put(year, demand);
	}

	/**
	 * Saves rail station demand to an output file.
	 * @param year Year of the data.
	 * @param outputFile Output file name (with path).
	 */
	public void saveRailStationDemand(int year, String outputFile) {
		
		this.yearToRailDemand.get(year).saveRailStationDemand(year, outputFile);
	}
	
	/**
	 * Saves zonal rail station demand to an output file.
	 * @param year Year of the data.
	 * @param outputFile Output file name (with path).
	 */
	public void saveZonalRailStationDemand(int year, String outputFile) {
		
		this.yearToRailDemand.get(year).saveZonalRailStationDemand(year, outputFile);
	}
}