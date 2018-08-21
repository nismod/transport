/**
 * 
 */
package nismod.transport.demand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.decision.Intervention;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.InputFileReader;

/**
 * Main demand prediction model (elasticity-based).
 * @author Milan Lovric
  */
public class DemandModel {
	
	private final static Logger LOGGER = LogManager.getLogger(DemandModel.class);
	
	public final static int BASE_YEAR = 2015;
	public final static int BASE_YEAR_FREIGHT = 2006;
	public final static double FREIGHT_SCALING_FACTOR = 18366.0/21848.0;
	public final static double LINK_TRAVEL_TIME_AVERAGING_WEIGHT = 1.0;
	public final static int ASSIGNMENT_ITERATIONS = 1;
	public final static int PREDICTION_ITERATIONS = 1;
	public static enum ElasticityTypes {
		POPULATION, GVA, TIME, COST
	}

	private HashMap<ElasticityTypes, Double> elasticities;
	private HashMap<ElasticityTypes, Double> elasticitiesFreight;
	private HashMap<VehicleType, Double> vehicleTypeToPCU;
	private HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> baseFuelConsumptionRates;
	private HashMap<Integer, HashMap<Pair<VehicleType, EngineType>, Double>> yearToRelativeFuelEfficiencies;
	private Map<Integer, Map<TimeOfDay, Double>> yearToTimeOfDayDistribution;
	private Map<Integer, Map<TimeOfDay, Double>> yearToTimeOfDayDistributionFreight;
	private HashMap<Integer, ODMatrix> yearToPassengerODMatrix; //passenger demand
	private HashMap<Integer, FreightMatrix> yearToFreightODMatrix; //freight demand
	private HashMap<Integer, SkimMatrix> yearToTimeSkimMatrix;
	private HashMap<Integer, SkimMatrixFreight> yearToTimeSkimMatrixFreight;
	private HashMap<Integer, SkimMatrix> yearToCostSkimMatrix;
	private HashMap<Integer, SkimMatrixFreight> yearToCostSkimMatrixFreight;
	private HashMap<Integer, HashMap<String, Integer>> yearToZoneToPopulation;
	private HashMap<Integer, HashMap<String, Double>> yearToZoneToGVA;
	private HashMap<Integer, RoadNetworkAssignment> yearToRoadNetworkAssignment;
	private HashMap<Integer, HashMap<EnergyType, Double>> yearToEnergyUnitCosts;
	private HashMap<Integer, HashMap<EnergyType, Double>> yearToUnitCO2Emissions;
	private HashMap<Integer, HashMap<VehicleType, HashMap<EngineType, Double>>> yearToEngineTypeFractions;
	private HashMap<Integer, HashMap<VehicleType, Double>> yearToAVFractions;
	//private HashMap<Integer, HashMap<Integer, Double>> yearToCongestionCharges;
	private HashMap<Integer, HashMap<String, MultiKeyMap>> yearToCongestionCharges;
	//private SkimMatrix baseYearTimeSkimMatrix,	baseYearCostSkimMatrix;
	
	private RouteSetGenerator rsg;
	private Properties props;
	
	private List<Intervention> interventions;
	
	private RoadNetwork roadNetwork;

	/**
	 * The constructor for the demand prediction model.
	 * @param roadNetwork Road network for assignment.
	 * @param baseYearODMatrixFile Base-year origin-destination matrix file name.
	 * @param baseYearFreightMatrixFile Base-year freight matrix file name.
	 * @param populationFile Population file name.
	 * @param GVAFile GVA file name.
	 * @param elasticitiesFile Elasticities file name.
	 * @param elasticitiesFreightFile Elasticities freight file name.
	 * @param energyUnitCostsFile Energy unit costs file name.
	 * @param unitCO2EmissionsFile Unit CO2 emissions file name.
	 * @param engineTypeFractionsFile Engine type fractions file.
	 * @param autonomousVehiclesFractionsFile Autonomous vehicles fractions file.
	 * @param interventions List of interventions.
	 * @param rsg Route Set Generator with routes for both cars and freight.
	 * @param props Properties file.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public DemandModel(RoadNetwork roadNetwork, String baseYearODMatrixFile, String baseYearFreightMatrixFile, String populationFile, String GVAFile, String elasticitiesFile, String elasticitiesFreightFile, String energyUnitCostsFile, String unitCO2EmissionsFile, String engineTypeFractionsFile, String autonomousVehiclesFractionsFile, List<Intervention> interventions, RouteSetGenerator rsg, Properties props) throws FileNotFoundException, IOException {

		this.yearToPassengerODMatrix = new HashMap<Integer, ODMatrix>();
		this.yearToFreightODMatrix = new HashMap<Integer, FreightMatrix>();
		this.yearToTimeSkimMatrix = new HashMap<Integer, SkimMatrix>();
		this.yearToTimeSkimMatrixFreight = new HashMap<Integer, SkimMatrixFreight>();
		this.yearToCostSkimMatrix = new HashMap<Integer, SkimMatrix>();
		this.yearToCostSkimMatrixFreight = new HashMap<Integer, SkimMatrixFreight>();
		this.yearToZoneToPopulation = new HashMap<Integer, HashMap<String, Integer>>();
		this.yearToZoneToGVA = new HashMap<Integer, HashMap<String, Double>>();
		this.yearToRoadNetworkAssignment = new HashMap<Integer, RoadNetworkAssignment>();
		this.yearToEnergyUnitCosts = new HashMap<Integer, HashMap<EnergyType, Double>>();
		this.yearToUnitCO2Emissions = new HashMap<Integer, HashMap<EnergyType, Double>>();
		this.yearToEngineTypeFractions = new HashMap<Integer, HashMap<VehicleType, HashMap<EngineType, Double>>>();
		this.yearToAVFractions = new HashMap<Integer, HashMap<VehicleType, Double>>();
		this.yearToCongestionCharges = new HashMap<Integer, HashMap<String, MultiKeyMap>>();
		
		this.rsg = rsg;
		this.props = props;
		this.roadNetwork = roadNetwork;
		this.interventions = interventions;

		//read base-year passenger matrix
		ODMatrix passengerODMatrix = new ODMatrix(baseYearODMatrixFile);
		//passengerODMatrix.printMatrixFormatted();
		this.yearToPassengerODMatrix.put(DemandModel.BASE_YEAR, passengerODMatrix);
		
		//read base-year freight matrix
		FreightMatrix freightMatrix = new FreightMatrix(baseYearFreightMatrixFile);
		//freightMatrix.printMatrixFormatted();
		//System.out.println("Freight matrix scaled to 2015:");
		FreightMatrix freightMatrixScaled = freightMatrix.getScaledMatrix(FREIGHT_SCALING_FACTOR);
		//freightMatrixScaled.printMatrixFormatted();
		this.yearToFreightODMatrix.put(DemandModel.BASE_YEAR, freightMatrixScaled);
				
		//read all year population predictions
		this.yearToZoneToPopulation = InputFileReader.readPopulationFile(populationFile);

		//read all year GVA predictions
		this.yearToZoneToGVA = InputFileReader.readGVAFile(GVAFile);
		
		this.yearToEnergyUnitCosts = InputFileReader.readEnergyUnitCostsFile(energyUnitCostsFile);
		
		this.yearToUnitCO2Emissions = InputFileReader.readUnitCO2EmissionFile(unitCO2EmissionsFile);
		
		this.yearToEngineTypeFractions = InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile);
		
		this.yearToAVFractions = InputFileReader.readAVFractionsFile(autonomousVehiclesFractionsFile);
		
		this.elasticities = InputFileReader.readElasticitiesFile(elasticitiesFile);
		this.elasticitiesFreight = InputFileReader.readElasticitiesFile(elasticitiesFreightFile);
		
		final String vehicleTypeToPCUFile = props.getProperty("vehicleTypeToPCUFile");
		final String baseFuelConsumptionRatesFile = props.getProperty("baseFuelConsumptionRatesFile");
		final String relativeFuelEfficiencyFile = props.getProperty("relativeFuelEfficiencyFile");
		final String timeOfDayDistributionFile = props.getProperty("timeOfDayDistributionFile");
		final String timeOfDayDistributionFreightFile = props.getProperty("timeOfDayDistributionFreightFile");
		
		this.vehicleTypeToPCU = InputFileReader.readVehicleTypeToPCUFile(vehicleTypeToPCUFile);
		this.baseFuelConsumptionRates = InputFileReader.readEnergyConsumptionParamsFile(baseFuelConsumptionRatesFile);
		this.yearToTimeOfDayDistribution = InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFile);
		this.yearToTimeOfDayDistributionFreight = InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFreightFile);
		this.yearToRelativeFuelEfficiencies = InputFileReader.readRelativeFuelEfficiencyFile(relativeFuelEfficiencyFile);
	}
	
	
	/**
	 * Predicts (passenger and freight) highway demand (origin-destination vehicle flows)
	 * for all years from baseYear to toYear
	 * @param toYear The final year for which the demand is predicted.
	 * @param baseYear The base year from which the predictions are made.
	 */
	public void predictHighwayDemands(int toYear, int baseYear) {
		
		Boolean flagPredictIntermediateYears = Boolean.parseBoolean(this.props.getProperty("FLAG_PREDICT_INTERMEDIATE_YEARS"));
		
		if (flagPredictIntermediateYears) { //predict all intermediate years
			for (int year = baseYear; year <= toYear - 1; year++) {
				this.predictHighwayDemand(year + 1, year);
			}
		} else { //predict only final year
			this.predictHighwayDemand(toYear, baseYear);
		}
	}
	
	/**
	 * Saves all results from baseYear to toYear (including intermediate if flat is set)
	 * @param toYear The final year for which the demand is predicted.
	 * @param baseYear The base year from which the predictions are made.
	 */
	public void saveAllResults(int toYear, int baseYear) {
		
		Boolean flagPredictIntermediateYears = Boolean.parseBoolean(this.props.getProperty("FLAG_PREDICT_INTERMEDIATE_YEARS"));
		
		//save base year
		this.saveAllResults(baseYear);
		if (flagPredictIntermediateYears) { //save all intermediate years
			for (int year = baseYear; year <= toYear - 1; year++) {
				this.saveAllResults(year + 1);
			}
		} else { //save only final year
			this.saveAllResults(toYear);
		}
	}

	/**
	 * Predicts (passenger and freight) highway demand (origin-destination vehicle flows).
	 * @param predictedYear The year for which the demand is predicted.
	 * @param fromYear The year from which demand the prediction is made.
	 */
	public void predictHighwayDemand(int predictedYear, int fromYear) {

		LOGGER.info("Predicting {} highway demand from {} demand.", predictedYear, fromYear);
		
		if (predictedYear <= fromYear) {
			LOGGER.error("predictedYear should be greater than fromYear!");
			return;
		//check if the demand from year fromYear exists
		} else if (!this.yearToPassengerODMatrix.containsKey(fromYear)) { 
			LOGGER.error("Passenger demand from year {} does not exist!", fromYear);
			return;
		} else if (!this.yearToFreightODMatrix.containsKey(fromYear)) { 
			LOGGER.error("Freight demand from year {} does not exist!", fromYear);
			return;
		} else {
			
			//check if the right interventions have been installed
			if (interventions != null)
				for (Intervention i: interventions) {
					if (i.getStartYear() <= fromYear && i.getEndYear() >= fromYear && !i.getState())				i.install(this);
					if (i.getEndYear() < fromYear && i.getState() || i.getStartYear() > fromYear && i.getState())	i.uninstall(this);
				}
						
			//check if the demand for fromYear has already been assigned, if not assign it
			RoadNetworkAssignment rna = yearToRoadNetworkAssignment.get(fromYear);
			if (rna == null) {
				LOGGER.debug("{} year has not been assigned to the network, so assigning it now.", fromYear);
				
				//create a network assignment and assign the demand
				rna = new RoadNetworkAssignment(this.roadNetwork, 
												this.yearToEnergyUnitCosts.get(fromYear),
												this.yearToUnitCO2Emissions.get(fromYear),
												this.yearToEngineTypeFractions.get(fromYear), 
												this.yearToAVFractions.get(fromYear),
												this.vehicleTypeToPCU,
												this.baseFuelConsumptionRates,
												this.yearToRelativeFuelEfficiencies.get(fromYear),
												this.yearToTimeOfDayDistribution.get(fromYear),
												this.yearToTimeOfDayDistributionFreight.get(fromYear),
												null, 
												null,
												null, 
												this.yearToCongestionCharges.get(fromYear),
												this.props);
				rna.assignFlowsAndUpdateLinkTravelTimesIterated(this.yearToPassengerODMatrix.get(fromYear), this.yearToFreightODMatrix.get(fromYear), this.rsg, this.props, LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);
				yearToRoadNetworkAssignment.put(fromYear, rna);
		
				//calculate skim matrices
				SkimMatrix tsm = rna.calculateTimeSkimMatrix();
				SkimMatrix csm = rna.calculateCostSkimMatrix();
				SkimMatrixFreight tsmf = rna.calculateTimeSkimMatrixFreight();
				SkimMatrixFreight csmf = rna.calculateCostSkimMatrixFreight();
				yearToTimeSkimMatrix.put(fromYear, tsm);
				yearToCostSkimMatrix.put(fromYear, csm);
				yearToTimeSkimMatrixFreight.put(fromYear, tsmf);
				yearToCostSkimMatrixFreight.put(fromYear, csmf);
			}
			
			//check if the right interventions have been installed
			if (interventions != null) 
				for (Intervention i: interventions) {
					if (i.getStartYear() <= predictedYear && i.getEndYear() >= predictedYear && !i.getState())				i.install(this);
					if (i.getEndYear() < predictedYear && i.getState() || i.getStartYear() > predictedYear && i.getState()) i.uninstall(this);
				}
			
			//copy skim matrices from fromYear into predictedYear
			yearToTimeSkimMatrix.put(predictedYear, yearToTimeSkimMatrix.get(fromYear));
			yearToCostSkimMatrix.put(predictedYear, yearToCostSkimMatrix.get(fromYear));
			yearToTimeSkimMatrixFreight.put(predictedYear, yearToTimeSkimMatrixFreight.get(fromYear));
			yearToCostSkimMatrixFreight.put(predictedYear, yearToCostSkimMatrixFreight.get(fromYear));

			//predicted demand	
			ODMatrix predictedPassengerODMatrix = new ODMatrix();
			FreightMatrix predictedFreightODMatrix = new FreightMatrix();
			
			//FIRST STAGE PREDICTION (FROM POPULATION AND GVA)
			
			//for each OD pair first predict the change in passenger vehicle flows from the changes in population and GVA
			for (MultiKey mk: this.yearToPassengerODMatrix.get(fromYear).getKeySet()) {
				String originZone = (String) mk.getKey(0);
				String destinationZone = (String) mk.getKey(1);

				double oldFlow = this.yearToPassengerODMatrix.get(fromYear).getFlow(originZone, destinationZone);
				
				double oldPopulationOriginZone = this.yearToZoneToPopulation.get(fromYear).get(originZone);
				double oldPopulationDestinationZone = this.yearToZoneToPopulation.get(fromYear).get(destinationZone);
				double newPopulationOriginZone = this.yearToZoneToPopulation.get(predictedYear).get(originZone);
				double newPopulationDestinationZone = this.yearToZoneToPopulation.get(predictedYear).get(destinationZone);
				double oldGVAOriginZone = this.yearToZoneToGVA.get(fromYear).get(originZone);
				double oldGVADestinationZone = this.yearToZoneToGVA.get(fromYear).get(destinationZone);
				double newGVAOriginZone = this.yearToZoneToGVA.get(predictedYear).get(originZone);
				double newGVADestinationZone = this.yearToZoneToGVA.get(predictedYear).get(destinationZone);

				double predictedFlow = oldFlow * Math.pow((newPopulationOriginZone + newPopulationDestinationZone) / (oldPopulationOriginZone + oldPopulationDestinationZone), elasticities.get(ElasticityTypes.POPULATION)) *
						Math.pow((newGVAOriginZone + newGVADestinationZone) / (oldGVAOriginZone + oldGVADestinationZone), elasticities.get(ElasticityTypes.GVA));
				//System.out.printf("%d = %d * (%.0f / %.0f) ^ %.2f * (%.1f / %.1f) ^ %.2f * (%.0f / %.0f) ^ %.2f * (%.1f / %.1f) ^ %.2f\n", (int) Math.round(predictedFlow), (int) Math.round(oldFlow),
				//newPopulationOriginZone, oldPopulationOriginZone, elasticities.get(ElasticityTypes.POPULATION),
				//newGVAOriginZone, oldGVAOriginZone, elasticities.get(ElasticityTypes.GVA),
				//newPopulationDestinationZone, oldPopulationDestinationZone, elasticities.get(ElasticityTypes.POPULATION),
				//newGVADestinationZone, oldGVADestinationZone, elasticities.get(ElasticityTypes.GVA));
				
				predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedFlow));
			} 
			
			LOGGER.debug("First stage prediction passenger matrix (from population and GVA):");
			//if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();
			if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();
					
			//for each OD pair first predict the change in freight vehicle flows from the changes in population and GVA
			for (MultiKey mk: this.yearToFreightODMatrix.get(fromYear).getKeySet()) {
				int origin = (int) mk.getKey(0);
				int destination = (int) mk.getKey(1);
				int vehicleType = (int) mk.getKey(2);

				double oldFlow = this.yearToFreightODMatrix.get(fromYear).getFlow(origin, destination, vehicleType);
				double predictedFlow = oldFlow;

				//if origin is a LAD (<= 1032, according to the DfT BYFM model)
				if (origin <= 1032 && destination > 1032) {

					String originZone = roadNetwork.getFreightZoneToLAD().get(origin); 
					double oldPopulationOriginZone = this.yearToZoneToPopulation.get(fromYear).get(originZone);
					double newPopulationOriginZone = this.yearToZoneToPopulation.get(predictedYear).get(originZone);
					double oldGVAOriginZone = this.yearToZoneToGVA.get(fromYear).get(originZone);
					double newGVAOriginZone = this.yearToZoneToGVA.get(predictedYear).get(originZone);

					predictedFlow = predictedFlow * Math.pow(newPopulationOriginZone / oldPopulationOriginZone, elasticitiesFreight.get(ElasticityTypes.POPULATION)) *
							Math.pow(newGVAOriginZone / oldGVAOriginZone, elasticitiesFreight.get(ElasticityTypes.GVA));
				}
				//if destination is a LAD (<= 1032, according to the DfT BYFM model)
				else if (destination <= 1032 && origin > 1032) {

					String destinationZone = roadNetwork.getFreightZoneToLAD().get(destination); 
					double oldPopulationDestinationZone = this.yearToZoneToPopulation.get(fromYear).get(destinationZone);
					double newPopulationDestinationZone = this.yearToZoneToPopulation.get(predictedYear).get(destinationZone);
					double oldGVADestinationZone = this.yearToZoneToGVA.get(fromYear).get(destinationZone);
					double newGVADestinationZone = this.yearToZoneToGVA.get(predictedYear).get(destinationZone);

					predictedFlow = predictedFlow *	Math.pow(newPopulationDestinationZone / oldPopulationDestinationZone, elasticitiesFreight.get(ElasticityTypes.POPULATION)) *
							Math.pow(newGVADestinationZone / oldGVADestinationZone, elasticitiesFreight.get(ElasticityTypes.GVA));
				}
				//if both origin and destination zone are LADs
				else if (origin <= 1032 && destination <= 1032) {
					
					String originZone = roadNetwork.getFreightZoneToLAD().get(origin); 
					double oldPopulationOriginZone = this.yearToZoneToPopulation.get(fromYear).get(originZone);
					double newPopulationOriginZone = this.yearToZoneToPopulation.get(predictedYear).get(originZone);
					double oldGVAOriginZone = this.yearToZoneToGVA.get(fromYear).get(originZone);
					double newGVAOriginZone = this.yearToZoneToGVA.get(predictedYear).get(originZone);
					String destinationZone = roadNetwork.getFreightZoneToLAD().get(destination); 
					double oldPopulationDestinationZone = this.yearToZoneToPopulation.get(fromYear).get(destinationZone);
					double newPopulationDestinationZone = this.yearToZoneToPopulation.get(predictedYear).get(destinationZone);
					double oldGVADestinationZone = this.yearToZoneToGVA.get(fromYear).get(destinationZone);
					double newGVADestinationZone = this.yearToZoneToGVA.get(predictedYear).get(destinationZone);
					
					predictedFlow = predictedFlow *	Math.pow((newPopulationOriginZone + newPopulationDestinationZone) / (oldPopulationOriginZone + oldPopulationDestinationZone), elasticitiesFreight.get(ElasticityTypes.POPULATION)) *
							Math.pow((newGVAOriginZone + newGVADestinationZone) / (oldGVAOriginZone + oldGVADestinationZone), elasticitiesFreight.get(ElasticityTypes.GVA));
					
				}
					
				predictedFreightODMatrix.setFlow(origin, destination, vehicleType, (int) Math.round(predictedFlow));
			}

			LOGGER.debug("First stage prediction freight matrix (from population and GVA):");
			//if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();
			if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();
			
			//SECOND STAGE PREDICTION (FROM CHANGES IN COST AND TIME)
			
			SkimMatrix tsm = null, csm = null;
			SkimMatrixFreight tsmf = null, csmf = null;
			RoadNetworkAssignment predictedRna = null;
			for (int i=0; i<PREDICTION_ITERATIONS; i++) {

				if (predictedRna == null)
					//assign predicted year - using link travel times from fromYear
					predictedRna = new RoadNetworkAssignment(this.roadNetwork, 
															 this.yearToEnergyUnitCosts.get(predictedYear),
															 this.yearToUnitCO2Emissions.get(predictedYear),
															 this.yearToEngineTypeFractions.get(predictedYear), 
															 this.yearToAVFractions.get(predictedYear),
															 this.vehicleTypeToPCU,
															 this.baseFuelConsumptionRates,
															 this.yearToRelativeFuelEfficiencies.get(predictedYear),
															 this.yearToTimeOfDayDistribution.get(predictedYear),
															 this.yearToTimeOfDayDistributionFreight.get(predictedYear),
															 rna.getLinkTravelTimes(), 
															 rna.getAreaCodeProbabilities(), 
															 rna.getWorkplaceZoneProbabilities(),
															 this.yearToCongestionCharges.get(predictedYear),
															 this.props);
				else
					//using latest link travel times
					predictedRna = new RoadNetworkAssignment(this.roadNetwork, 
															 this.yearToEnergyUnitCosts.get(predictedYear),
															 this.yearToUnitCO2Emissions.get(predictedYear),
															 this.yearToEngineTypeFractions.get(predictedYear), 
															 this.yearToAVFractions.get(predictedYear),
															 this.vehicleTypeToPCU,
															 this.baseFuelConsumptionRates,
															 this.yearToRelativeFuelEfficiencies.get(predictedYear),
															 this.yearToTimeOfDayDistribution.get(predictedYear),
															 this.yearToTimeOfDayDistributionFreight.get(predictedYear),
															 predictedRna.getLinkTravelTimes(), 
															 predictedRna.getAreaCodeProbabilities(), 
															 predictedRna.getWorkplaceZoneProbabilities(),
															 this.yearToCongestionCharges.get(predictedYear),
															 this.props);

				predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrix, predictedFreightODMatrix, this.rsg, this.props, LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);
				
				//update skim matrices for predicted year after the assignment
				tsm = predictedRna.calculateTimeSkimMatrix();
				csm = predictedRna.calculateCostSkimMatrix();
				tsmf = predictedRna.calculateTimeSkimMatrixFreight();
				csmf = predictedRna.calculateCostSkimMatrixFreight();

				//for each OD pair predict the change in passenger vehicle flow from the change in skim matrices
				for (MultiKey mk: this.yearToPassengerODMatrix.get(fromYear).getKeySet()) {
					String originZone = (String) mk.getKey(0);
					String destinationZone = (String) mk.getKey(1);

					double oldFlow = predictedPassengerODMatrix.getFlow(originZone, destinationZone);
					
					if (this.yearToTimeSkimMatrix.get(predictedYear) == null) LOGGER.error("No time skim matrix in the demand model.");
					if (this.yearToCostSkimMatrix.get(predictedYear) == null) LOGGER.error("No cost skim matrix in the demand model.");
										
					Double oldODTravelTime = this.yearToTimeSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
					Double newODTravelTime = tsm.getCost(originZone, destinationZone);
					Double oldODTravelCost = this.yearToCostSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
					Double newODTravelCost = csm.getCost(originZone, destinationZone);
					
					if (oldODTravelTime == null) LOGGER.warn("Unknown old travel time between zone {} and zone {}.", originZone, destinationZone);
					if (newODTravelTime == null) LOGGER.warn("Unknown new travel time between zone {} and zone {}.", originZone, destinationZone);
					if (oldODTravelCost == null) LOGGER.warn("Unknown old travel cost between zone {} and zone {}.", originZone, destinationZone);
					if (newODTravelCost == null) LOGGER.warn("Unknown new travel cost between zone {} and zone {}.", originZone, destinationZone);
					if (oldODTravelTime == null || newODTravelTime == null)	{ //if either is undefined assume the ratio is 1, i.e. not affecting the prediction
						oldODTravelTime = 1.0;
						newODTravelTime = 1.0;
					}
					if (oldODTravelCost == null || newODTravelCost == null) { 
						oldODTravelCost = 1.0;
						newODTravelCost = 1.0;
					}
					
					double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticities.get(ElasticityTypes.TIME)) *
							Math.pow(newODTravelCost / oldODTravelCost, elasticities.get(ElasticityTypes.COST));

					predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedflow));
				}

				LOGGER.debug("Second stage prediction passenger matrix (from changes in skim matrices):");
				//if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();
				if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();
								
				//for each OD pair predict the change in freight vehicle flow from the change in skim matrices
				for (MultiKey mk: this.yearToFreightODMatrix.get(fromYear).getKeySet()) {
					int origin = (int) mk.getKey(0);
					int destination = (int) mk.getKey(1);
					int vehicleType = (int) mk.getKey(2);

					double oldFlow = predictedFreightODMatrix.getFlow(origin, destination, vehicleType);

					Double oldODTravelTime = this.yearToTimeSkimMatrixFreight.get(predictedYear).getCost(origin, destination, vehicleType);
					Double newODTravelTime = tsmf.getCost(origin, destination, vehicleType);
					Double oldODTravelCost = this.yearToCostSkimMatrixFreight.get(predictedYear).getCost(origin, destination, vehicleType);
					Double newODTravelCost = csmf.getCost(origin, destination, vehicleType);
					
					if (oldODTravelTime == null) LOGGER.warn("Unknown old travel time between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
					if (newODTravelTime == null) LOGGER.warn("Unknown new travel time between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
					if (oldODTravelCost == null) LOGGER.warn("Unknown old travel cost between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
					if (newODTravelCost == null) LOGGER.warn("Unknown new travel cost between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
					if (oldODTravelTime == null || newODTravelTime == null)	{ //if either is undefined assume the ratio is 1, i.e. not affecting the prediction
						oldODTravelTime = 1.0;
						newODTravelTime = 1.0;
					}
					if (oldODTravelCost == null || newODTravelCost == null) { 
						oldODTravelCost = 1.0;
						newODTravelCost = 1.0;
					}

					double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticitiesFreight.get(ElasticityTypes.TIME)) *
							Math.pow(newODTravelCost / oldODTravelCost, elasticitiesFreight.get(ElasticityTypes.COST));

					predictedFreightODMatrix.setFlow(origin, destination, vehicleType, (int) Math.round(predictedflow));
				}

				LOGGER.debug("Second stage prediction freight matrix (from changes in skim matrices):");
				//if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();
				if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();
				
				//assign predicted year again using latest link travel times
				predictedRna = new RoadNetworkAssignment(this.roadNetwork, 
														 this.yearToEnergyUnitCosts.get(predictedYear),
														 this.yearToUnitCO2Emissions.get(predictedYear),
														 this.yearToEngineTypeFractions.get(predictedYear), 
														 this.yearToAVFractions.get(predictedYear),
														 this.vehicleTypeToPCU,
														 this.baseFuelConsumptionRates,
														 this.yearToRelativeFuelEfficiencies.get(predictedYear),
														 this.yearToTimeOfDayDistribution.get(predictedYear),
														 this.yearToTimeOfDayDistributionFreight.get(predictedYear),
														 predictedRna.getLinkTravelTimes(), 
														 predictedRna.getAreaCodeProbabilities(), 
														 predictedRna.getWorkplaceZoneProbabilities(),
														 this.yearToCongestionCharges.get(predictedYear),
														 this.props);
				//predictedRna.resetLinkVolumes();
				//predictedRna.assignPassengerFlows(predictedPassengerODMatrix);
				//predictedRna.updateLinkTravelTimes(ALPHA_LINK_TRAVEL_TIME_AVERAGING);
				predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrix, predictedFreightODMatrix, this.rsg, this.props, LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);				
				
				//store skim matrices into hashmaps
				yearToTimeSkimMatrix.put(predictedYear, tsm);
				yearToCostSkimMatrix.put(predictedYear, csm);
				yearToTimeSkimMatrixFreight.put(predictedYear, tsmf);
				yearToCostSkimMatrixFreight.put(predictedYear, csmf);
								
				//update skim matrices for predicted year after the assignment
				tsm = predictedRna.calculateTimeSkimMatrix();
				csm = predictedRna.calculateCostSkimMatrix();
				tsmf = predictedRna.calculateTimeSkimMatrixFreight();
				csmf = predictedRna.calculateCostSkimMatrixFreight();

				/*
				System.out.println("Difference in consecutive time skim matrices: " + tsm.getAbsoluteDifference(yearToTimeSkimMatrix.get(predictedYear)));
				System.out.println("Difference in consecutive cost skim matrix: " + csm.getAbsoluteDifference(yearToCostSkimMatrix.get(predictedYear)));
				System.out.println("Difference in consecutive time skim matrices for freight: " + tsmf.getAbsoluteDifference(yearToTimeSkimMatrixFreight.get(predictedYear)));
				System.out.println("Difference in consecutive cost skim matrix for freight: " + csmf.getAbsoluteDifference(yearToCostSkimMatrixFreight.get(predictedYear)));
				*/
				
				//store road network assignment
				yearToRoadNetworkAssignment.put(predictedYear, predictedRna);

				//store predicted OD matrix in the map
				this.yearToPassengerODMatrix.put(predictedYear, predictedPassengerODMatrix);
				this.yearToFreightODMatrix.put(predictedYear, predictedFreightODMatrix);
			}//for loop
			
			//store latest skim matrices
			yearToTimeSkimMatrix.put(predictedYear, tsm);
			yearToCostSkimMatrix.put(predictedYear, csm);
			yearToTimeSkimMatrixFreight.put(predictedYear, tsmf);
			yearToCostSkimMatrixFreight.put(predictedYear, csmf);
		}
	}                                                                                                                                               

	/**
	 * Getter method for the passenger demand in a given year.
	 * @param year Year for which the demand is requested.
	 * @return Origin-destination matrix with passenger vehicle flows.
	 */
	public ODMatrix getPassengerDemand (int year) {

		return yearToPassengerODMatrix.get(year);
	}
	
	/**
	 * Getter method for the freight demand in a given year.
	 * @param year Year for which the demand is requested.
	 * @return Origin-destination matrix with freight vehicle flows.
	 */
	public FreightMatrix getFreightDemand (int year) {

		return yearToFreightODMatrix.get(year);
	}

	/**
	 * Getter method for the road network assignment in a given year.
	 * @param year Year for which the road network assignment is requested.
	 * @return Road network assignment.
	 */
	public RoadNetworkAssignment getRoadNetworkAssignment (int year) {

		return 	yearToRoadNetworkAssignment.get(year);
	}

	/**
	 * Getter method for time skim matrix in a given year.
	 * @param year Year for which the the skim matrix is requested.
	 * @return Time skim matrix.
	 */
	public SkimMatrix getTimeSkimMatrix (int year) {

		return 	yearToTimeSkimMatrix.get(year);
	}
	
	/**
	 * Getter method for freight time skim matrix in a given year.
	 * @param year Year for which the the skim matrix is requested.
	 * @return Time skim matrix.
	 */
	public SkimMatrixFreight getTimeSkimMatrixFreight (int year) {

		return 	yearToTimeSkimMatrixFreight.get(year);
	}

	/**
	 * Getter method for cost skim matrix in a given year.
	 * @param year Year for which the the skim matrix is requested.
	 * @return Cost skim matrix.
	 */
	public SkimMatrix getCostSkimMatrix (int year) {

		return 	yearToCostSkimMatrix.get(year);
	}

	/**
	 * Getter method for freight cost skim matrix in a given year.
	 * @param year Year for which the the skim matrix is requested.
	 * @return Cost skim matrix.
	 */
	public SkimMatrixFreight getCostSkimMatrixFreight (int year) {

		return 	yearToCostSkimMatrixFreight.get(year);
	}

	/**
	 * Saves energy consumptions into a csv file.
	 * @param year Year of the data.
	 * @param outputFile Output file name.
	 */
	public void saveEnergyConsumptions (int year, String outputFile) {

		this.yearToRoadNetworkAssignment.get(year).saveTotalEnergyConsumptions(year, outputFile);
	}
	
	/**
	 * Setter method for engine type fractions in a given year.
	 * @param year Year of the data.
	 * @param engineTypeFractions Map with engine type fractions.
	 */
	public void setEngineTypeFractions(int year, HashMap<VehicleType, HashMap<EngineType, Double>> engineTypeFractions) {
		
		this.yearToEngineTypeFractions.put(year, engineTypeFractions);
	}
	
	/**
	 * Setter method for engine type fractions in a given year for a specific vehicle type.
	 * @param year Year of the data.
	 * @param vht Vehicle type.
	 * @param engineTypeFractions Map with engine type fractions.
	 */
	public void setEngineTypeFractions(int year, VehicleType vht, HashMap<EngineType, Double> engineTypeFractions) {
		
		this.yearToEngineTypeFractions.get(year).put(vht, engineTypeFractions);
	}
	
	/**
	 * Setter method for congestion charges (overrides them).
	 * @param year Year of the congestion charges.
	 * @param congestionCharges Congestion charges.
	 */
	public void setCongestionCharges(int year, HashMap<String, MultiKeyMap> congestionCharges) {
		
		this.yearToCongestionCharges.put(year, congestionCharges);
	}

	/**
	 * Getter method for congestion charges.
	 * @param year Year of the congestion charges.
	 * @return Congestion charges.
	 */
	public HashMap<String, MultiKeyMap> getCongestionCharges(int year) {
		
		return this.yearToCongestionCharges.get(year);
	}
	
	/**
	 * Adds congestion charges to the list of the existing ones.
	 * @param year Year of the policy.
	 * @param policyName Name of the policy.
	 * @param congestionCharges Congestion charges.
	 */
	public void addCongestionCharges(int year, String policyName, MultiKeyMap congestionCharges) {
		
		HashMap<String, MultiKeyMap> map = this.yearToCongestionCharges.get(year);
		//if there is no congestion charges yet for this year, create the list
		if (map == null) map = new HashMap<String, MultiKeyMap>();
		map.put(policyName, congestionCharges); //add the congestion charge
		this.yearToCongestionCharges.put(year, map);	
	}
	
	/**
	 * Removes congestion charges from the list of existing ones.
	 * @param year Year of the congestion charges.
	 * @param policyName Name of the policy.
	 */
	public void removeCongestionCharges(int year, String policyName) {
		
		HashMap<String, MultiKeyMap> map = this.yearToCongestionCharges.get(year);
		
//		//if there is no existing congestion charge, there is nothing to remove
//		if (map == null) {
//				return;
//		} else { //remove the existing ones
//				for (Iterator<?> it = map.iterator(); it.hasNext(); ) {
//					MultiKeyMap cg = (MultiKeyMap) it.next();
//				    if (cg.equals(congestionCharges)) it.remove();
//			}
//		}
		
		//if there is no existing congestion charge, there is nothing to remove
		if (map == null) {
				return;
		} else { //remove the existing ones
			
			map.remove(policyName);
		}
	}
	
	/**
	 * Saves road network assignment results into a csv file.
	 * @param year Year of the data.
	 * @param outputFile Output file name.
	 */
	public void saveAssignmentResults (int year, String outputFile) {

		this.yearToRoadNetworkAssignment.get(year).saveAssignmentResults(year, outputFile);
	}
	
		
	/**
	 * Saves all results into the output folder.
	 * @param year Year of the data.
	 */
	public void saveAllResults (int year) {
		
		LOGGER.info("Outputing all results for year {}.", year);

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
		String predictedODMatrixFile = this.props.getProperty("predictedODMatrixFile");
		String predictedFreightMatrixFile = this.props.getProperty("predictedFreightMatrixFile");
		String assignmentResultsFile = this.props.getProperty("assignmentResultsFile");
		String linkTravelTimesFile = this.props.getProperty("linkTravelTimesFile");
		String timeSkimMatrixFile = this.props.getProperty("timeSkimMatrixFile");
		String costSkimMatrixFile = this.props.getProperty("costSkimMatrixFile");
		String timeSkimMatrixFreightFile = this.props.getProperty("timeSkimMatrixFreightFile");
		String costSkimMatrixFreightFile = this.props.getProperty("costSkimMatrixFreightFile");
		String vehicleKilometresFile = this.props.getProperty("vehicleKilometresFile");
		String energyConsumptionsFile = this.props.getProperty("energyConsumptionsFile");
		String totalCO2EmissionsFile = this.props.getProperty("totalCO2EmissionsFile");
		String tripsFile = this.props.getProperty("tripsFile");
	
		if (year == Integer.parseInt(baseYear)) { //rename output files for base year
			predictedODMatrixFile = "baseYearODMatrix.csv";
			predictedFreightMatrixFile = "baseYearFreightMatrix.csv";
		}
		
		String outputFile = file.getPath() + File.separator + predictedODMatrixFile;
		this.yearToPassengerODMatrix.get(year).saveMatrixFormatted(outputFile);
			
		outputFile = file.getPath() + File.separator + predictedFreightMatrixFile;
		this.yearToFreightODMatrix.get(year).saveMatrixFormatted(outputFile);
		
		outputFile = file.getPath() + File.separator +  timeSkimMatrixFile;
		this.yearToTimeSkimMatrix.get(year).saveMatrixFormatted(outputFile);
		
		outputFile = file.getPath() + File.separator +  costSkimMatrixFile;
		this.yearToCostSkimMatrix.get(year).saveMatrixFormatted(outputFile);
		
		outputFile = file.getPath() + File.separator +  timeSkimMatrixFreightFile;
		this.yearToTimeSkimMatrixFreight.get(year).saveMatrixFormatted(outputFile);
		
		outputFile = file.getPath() + File.separator +  costSkimMatrixFreightFile;
		this.yearToCostSkimMatrixFreight.get(year).saveMatrixFormatted(outputFile);
		
		outputFile = file.getPath() + File.separator +  vehicleKilometresFile;
		this.yearToRoadNetworkAssignment.get(year).saveZonalVehicleKilometres(year, outputFile);
		
		outputFile = file.getPath() + File.separator +  energyConsumptionsFile;
		this.yearToRoadNetworkAssignment.get(year).saveTotalEnergyConsumptions(year, outputFile);
		
		outputFile = file.getPath() + File.separator +  totalCO2EmissionsFile;
		this.yearToRoadNetworkAssignment.get(year).saveTotalCO2Emissions(year, outputFile);
	
		outputFile = file.getPath() + File.separator +  assignmentResultsFile;
		this.yearToRoadNetworkAssignment.get(year).saveAssignmentResults(year, outputFile);
		
		outputFile = file.getPath() + File.separator + linkTravelTimesFile;
		this.yearToRoadNetworkAssignment.get(year).saveLinkTravelTimes(year, outputFile);
	}
	
	/**
	 * Getter method for engine type fractions in a given year.
	 * @param year Year of the data.
	 * @return Map with engine type fractions.
	 */
	public HashMap<VehicleType, HashMap<EngineType, Double>> getEngineTypeFractions(int year) {
		
		return this.yearToEngineTypeFractions.get(year);
	}
	
	/**
	 * Getter method for the road network.
	 * @return Road network.
	 */
	public RoadNetwork getRoadNetwork() {
		
		return this.roadNetwork;
	}
}