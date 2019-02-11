package nismod.transport.rail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.decision.Intervention;
import nismod.transport.demand.AssignableODMatrix;
import nismod.transport.demand.DemandModel;
import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrixMultiKey;
import nismod.transport.demand.SkimMatrix;
import nismod.transport.demand.SkimMatrixFreight;
import nismod.transport.demand.DemandModel.ElasticityTypes;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

public class RailDemandModel {

	private final static Logger LOGGER = LogManager.getLogger(RailDemandModel.class);

	public final int baseYear;
	//public final int predictionIterations;

	public static enum ElasticityTypes {
		POPULATION, //population
		GVA, //income
		COST_RAIL, //rail fare
		COST_CAR, //passenger car trip cost (fuel + congestion)
		TIME, //generalised journey time
	}
	private Map<ElasticityTypes, Double> elasticities;

	private HashMap<Integer, RailStationDemand> yearToRailDemand; //rail demand

	//zonal data
	private HashMap<Integer, HashMap<String, Integer>> yearToZoneToPopulation;
	private HashMap<Integer, HashMap<String, Double>> yearToZoneToGVA;
	private HashMap<Integer, HashMap<String, Double>> yearToCarCosts;

	//station data
	private HashMap<Integer, HashMap<Integer, Double>> yearToStationFares; //journey fares
	private HashMap<Integer, HashMap<Integer, Double>> yearToStationGJTs; //generalised journey times

	private List<Intervention> interventions;
	private Properties props;

	/**
	 * Constructor for the rail demand model.
	 * @param baseYearRailStationUsageFile
	 * @param populationFile
	 * @param GVAFile
	 * @param elasticitiesFile
	 * @param railStationJourneyFaresFile
	 * @param railStationGeneralisedJourneyTimesFile
	 * @param carZonalJourneyCostsFile
	 * @param interventions
	 * @param props
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public RailDemandModel(String baseYearRailStationUsageFile, String populationFile, String GVAFile, String elasticitiesFile, String railStationJourneyFaresFile, String railStationGeneralisedJourneyTimesFile, String carZonalJourneyCostsFile, List<Intervention> interventions, Properties props) throws FileNotFoundException, IOException {

		//read all year population predictions
		this.yearToZoneToPopulation = InputFileReader.readPopulationFile(populationFile);
		//read all year GVA predictions
		this.yearToZoneToGVA = InputFileReader.readGVAFile(GVAFile);
		//read elasticities file
		this.elasticities = InputFileReader.readRailElasticitiesFile(elasticitiesFile);
		//read zonal car costs
		this.yearToCarCosts = InputFileReader.readZonalCarCostsFile(carZonalJourneyCostsFile);
		//read station journey fares
		this.yearToStationFares = InputFileReader.readRailStationCostsFile(railStationJourneyFaresFile);
		//read station generalised journey times
		this.yearToStationGJTs = InputFileReader.readRailStationCostsFile(railStationGeneralisedJourneyTimesFile);

		//create map to store rail station demands for all years
		this.yearToRailDemand = new HashMap<Integer, RailStationDemand>();

		//read the parameters
		this.baseYear = Integer.parseInt(props.getProperty("baseYear"));

		//read base-year passenger rail demand
		RailStationDemand baseYearDemand = new RailStationDemand(baseYearRailStationUsageFile);
		this.yearToRailDemand.put(this.baseYear, baseYearDemand);

		this.interventions = interventions;
		this.props = props;
	}

	/**
	 * Predicts rail station demand (total passenger counts at each station)
	 * for all years from baseYear to toYear.
	 * @param toYear The final year for which the demand is predicted.
	 * @param baseYear The base year from which the predictions are made.
	 */
	public void predictRailwayDemands(int toYear, int baseYear) {

		Boolean flagPredictIntermediateYears = Boolean.parseBoolean(this.props.getProperty("FLAG_PREDICT_INTERMEDIATE_YEARS"));

		if (flagPredictIntermediateYears) { //predict all intermediate years
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

		//check if the right interventions have been installed
		if (interventions != null) 
			for (Intervention i: interventions) {
				if (i.getStartYear() <= predictedYear && i.getEndYear() >= predictedYear && !i.getState())				i.install(this);
				if (i.getEndYear() < predictedYear && i.getState() || i.getStartYear() > predictedYear && i.getState()) i.uninstall(this);
			}


		//old demand
		RailStationDemand fromDemand = this.yearToRailDemand.get(fromYear);

		//predicted demand	
		RailStationDemand predictedDemand = new RailStationDemand(fromDemand.getHeader());
	
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
			int predictedUsage = (int) Math.round(oldUsage	* Math.pow((double) newPopulationRailStationZone / oldPopulationRailStationZone, elasticities.get(ElasticityTypes.POPULATION))
					* Math.pow((double) newGVARailStationZone / oldGVARailStationZone, elasticities.get(ElasticityTypes.GVA))
					* Math.pow((double) newCarCostsRailStationZone / oldCarCostsRailStationZone, elasticities.get(ElasticityTypes.COST_CAR))
					* Math.pow((double) newFaresRailStation / oldFaresRailStation, elasticities.get(ElasticityTypes.COST_RAIL))
					* Math.pow((double) newGJTRailStationZone / oldGJTRailStationZone, elasticities.get(ElasticityTypes.TIME)));

			if (predictedUsage == 0 && oldUsage != 0) 
				predictedUsage = 1; //stops demand from disappearing (unless oldUsage was also 0)

			LOGGER.trace("Old yearly usage: {}, new yearly usage: {}", oldUsage, predictedUsage);

			//create a station copy
			RailStation predictedStation = new RailStation(station);
			predictedStation.setYearlyUsage(predictedUsage);

			int predictedDayUsage = (int) Math.round(predictedUsage / station.getRunDays());
			if (predictedDayUsage == 0) 
				predictedDayUsage = 1; //stops demand from disappearing.
			predictedStation.setDailyUsage(predictedDayUsage);

			//store in predicted demand
			predictedDemand.addStation(predictedStation);
		}
		
		//store predicted demand
		this.yearToRailDemand.put(predictedYear, predictedDemand);

		LOGGER.debug("Finished predicting {} railway demand from {} demand.", predictedYear, fromYear);

		//print from demand and predicted demand
		fromDemand.printRailDemandNLCSorted("From demand:");
		predictedDemand.printRailDemandNLCSorted("Predicted demand:");
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
	 * Saves rail station demand to an output file.
	 * @param year Year of the data.
	 * @param outputFile Output file name (with path).
	 */
	public void saveRailStationDemand(int year, String outputFile) {
		
		this.yearToRailDemand.get(year).saveRailStationDemand(year, outputFile);
	}
}