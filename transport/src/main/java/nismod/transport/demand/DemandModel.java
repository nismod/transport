/**
 * 
 */
package nismod.transport.demand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.PricingPolicy;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSet;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Main demand prediction model (elasticity-based).
 * @author Milan Lovric
  */
public class DemandModel {
	
	private final static Logger LOGGER = LogManager.getLogger(DemandModel.class);
	
	public final int baseYear;
	public final int baseYearFreight;
	public final double freightScalingFactor;
	public final double linkTravelTimeAveragingWeight;
	public final int assignmentIterations;
	public final int predictionIterations;
	
	public static enum ElasticityTypes {
		POPULATION, GVA, TIME, COST
	}

	private Map<ElasticityTypes, Double> elasticities;
	private Map<ElasticityTypes, Double> elasticitiesFreight;
	private Map<VehicleType, Double> vehicleTypeToPCU;
	private Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> baseFuelConsumptionRates;
	private HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>> yearToRelativeFuelEfficiencies;
	private Map<Integer, Map<TimeOfDay, Double>> yearToTimeOfDayDistribution;
	private Map<Integer, Map<VehicleType, Map<TimeOfDay, Double>>> yearToTimeOfDayDistributionFreight;
	private HashMap<Integer, ODMatrixMultiKey> yearToPassengerODMatrix; //passenger demand
	private HashMap<Integer, FreightMatrix> yearToFreightODMatrix; //freight demand
	private HashMap<Integer, SkimMatrix> yearToTimeSkimMatrix;
	private HashMap<Integer, SkimMatrixFreight> yearToTimeSkimMatrixFreight;
	private HashMap<Integer, SkimMatrix> yearToCostSkimMatrix;
	private HashMap<Integer, SkimMatrixFreight> yearToCostSkimMatrixFreight;
	private HashMap<Integer, HashMap<String, Integer>> yearToZoneToPopulation;
	private HashMap<Integer, HashMap<String, Double>> yearToZoneToGVA;
	private HashMap<Integer, RoadNetworkAssignment> yearToRoadNetworkAssignment;
	private HashMap<Integer, Map<EnergyType, Double>> yearToEnergyUnitCosts;
	private HashMap<Integer, Map<EnergyType, Double>> yearToUnitCO2Emissions;
	private HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>> yearToEngineTypeFractions;
	private HashMap<Integer, Map<VehicleType, Double>> yearToAVFractions;
	//private HashMap<Integer, HashMap<Integer, Double>> yearToCongestionCharges;
	private HashMap<Integer, List<PricingPolicy>> yearToCongestionCharges;
	//private SkimMatrix baseYearTimeSkimMatrix,	baseYearCostSkimMatrix;
	private HashMap<Integer, List<List<String>>> yearToListsOfLADsForNewRouteGeneration;
	
	private RouteSetGenerator rsg;
	private Properties props;
	private List<Intervention> interventions;
	private RoadNetwork roadNetwork;
	private Zoning zoning;
	private ODMatrixMultiKey temproMatrixTemplate;

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
	 * @param zoning Zoning system (used for 'tempro' and 'combined' assignment type).
	 * @param props Properties file.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public DemandModel(RoadNetwork roadNetwork, String baseYearODMatrixFile, String baseYearFreightMatrixFile, String populationFile, String GVAFile, String elasticitiesFile, String elasticitiesFreightFile, String energyUnitCostsFile, String unitCO2EmissionsFile, String engineTypeFractionsFile, String autonomousVehiclesFractionsFile, List<Intervention> interventions, RouteSetGenerator rsg, Zoning zoning, Properties props) throws FileNotFoundException, IOException {

		this.yearToPassengerODMatrix = new HashMap<Integer, ODMatrixMultiKey>();
		this.yearToFreightODMatrix = new HashMap<Integer, FreightMatrix>();
		this.yearToTimeSkimMatrix = new HashMap<Integer, SkimMatrix>();
		this.yearToTimeSkimMatrixFreight = new HashMap<Integer, SkimMatrixFreight>();
		this.yearToCostSkimMatrix = new HashMap<Integer, SkimMatrix>();
		this.yearToCostSkimMatrixFreight = new HashMap<Integer, SkimMatrixFreight>();
		this.yearToZoneToPopulation = new HashMap<Integer, HashMap<String, Integer>>();
		this.yearToZoneToGVA = new HashMap<Integer, HashMap<String, Double>>();
		this.yearToRoadNetworkAssignment = new HashMap<Integer, RoadNetworkAssignment>();
		this.yearToEnergyUnitCosts = new HashMap<Integer, Map<EnergyType, Double>>();
		this.yearToUnitCO2Emissions = new HashMap<Integer, Map<EnergyType, Double>>();
		this.yearToEngineTypeFractions = new HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>>();
		this.yearToAVFractions = new HashMap<Integer, Map<VehicleType, Double>>();
		this.yearToCongestionCharges = new HashMap<Integer, List<PricingPolicy>>();
		this.yearToListsOfLADsForNewRouteGeneration = new HashMap<Integer, List<List<String>>>();
		
		this.rsg = rsg;
		this.props = props;
		this.roadNetwork = roadNetwork;
		this.interventions = interventions;
		this.zoning = zoning;

		//read the parameters
		this.baseYear = Integer.parseInt(props.getProperty("baseYear"));
		this.baseYearFreight = Integer.parseInt(props.getProperty("baseYearFreight"));
		this.freightScalingFactor = Double.parseDouble(props.getProperty("FREIGHT_SCALING_FACTOR"));
		this.linkTravelTimeAveragingWeight = Double.parseDouble(props.getProperty("LINK_TRAVEL_TIME_AVERAGING_WEIGHT")); 
		this.assignmentIterations = Integer.parseInt(props.getProperty("ASSIGNMENT_ITERATIONS"));
		this.predictionIterations = Integer.parseInt(props.getProperty("PREDICTION_ITERATIONS"));
				
		//read base-year passenger matrix
		ODMatrixMultiKey passengerODMatrix = new ODMatrixMultiKey(baseYearODMatrixFile);
		//passengerODMatrix.printMatrixFormatted();
		this.yearToPassengerODMatrix.put(this.baseYear, passengerODMatrix);
		
		//read base-year freight matrix
		FreightMatrix freightMatrix = new FreightMatrix(baseYearFreightMatrixFile);
		//freightMatrix.printMatrixFormatted();
		//System.out.println("Freight matrix scaled to 2015:");
		FreightMatrix freightMatrixScaled = freightMatrix.getScaledMatrix(this.freightScalingFactor);
		//freightMatrixScaled.printMatrixFormatted();
		this.yearToFreightODMatrix.put(this.baseYear, freightMatrixScaled);
				
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
		this.yearToTimeOfDayDistributionFreight = InputFileReader.readTimeOfDayDistributionFreightFile(timeOfDayDistributionFreightFile);
		this.yearToRelativeFuelEfficiencies = InputFileReader.readRelativeFuelEfficiencyFile(relativeFuelEfficiencyFile);
		
		//load Tempro matrix template if necessary
		final String assignmentType = props.getProperty("ASSIGNMENT_TYPE").toLowerCase();
		if (assignmentType.equals("tempro") || assignmentType.equals("combined")) {
			
			final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
			this.temproMatrixTemplate = new ODMatrixMultiKey(temproODMatrixFile);
		}
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
		
		if (toYear == baseYear) return;
		
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
		
		if (predictedYear < fromYear) {
			LOGGER.error("predictedYear should not be smaller than fromYear!");
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

				//read default link travel time
				final String defaultLinkTravelTimeFile = props.getProperty("defaultLinkTravelTimeFile");
				Map<TimeOfDay, Map<Integer, Double>> defaultLinkTravelTime = null;
				if (defaultLinkTravelTimeFile != null) 
					defaultLinkTravelTime = InputFileReader.readLinkTravelTimeFile(this.baseYear, defaultLinkTravelTimeFile);
				
				//create a network assignment and assign the demand
				rna = new RoadNetworkAssignment(this.roadNetwork,
												this.zoning,
												this.yearToEnergyUnitCosts.get(fromYear),
												this.yearToUnitCO2Emissions.get(fromYear),
												this.yearToEngineTypeFractions.get(fromYear), 
												this.yearToAVFractions.get(fromYear),
												this.vehicleTypeToPCU,
												this.baseFuelConsumptionRates,
												this.yearToRelativeFuelEfficiencies.get(fromYear),
												this.yearToTimeOfDayDistribution.get(fromYear),
												this.yearToTimeOfDayDistributionFreight.get(fromYear),
												defaultLinkTravelTime, 
												null,
												null, 
												this.yearToCongestionCharges.get(fromYear),
												this.props);
				
				AssignableODMatrix passengerODM;
				//if tempro or combined assignment used, disaggregate LAD-based matrix to tempro level using the template
				final String assignmentType = props.getProperty("ASSIGNMENT_TYPE").toLowerCase();
				if (assignmentType.equals("tempro") || assignmentType.equals("combined")) {
					passengerODM = ODMatrixMultiKey.createTEMProFromLadMatrix(this.yearToPassengerODMatrix.get(fromYear), this.temproMatrixTemplate, zoning);
				} else
					passengerODM = this.yearToPassengerODMatrix.get(fromYear);
				
				FreightMatrix freightODM = this.yearToFreightODMatrix.get(fromYear);
				
				int expectedTripListSize = passengerODM.getTotalIntFlow() + freightODM.getTotalIntFlow();
				rna.initialiseTripList(expectedTripListSize);
				
				rna.assignFlowsAndUpdateLinkTravelTimesIterated(passengerODM, freightODM, this.rsg, this.zoning, this.props, this.linkTravelTimeAveragingWeight, this.assignmentIterations);
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
				
				//if assigning base year, print traffic count comparison data
				if (fromYear == this.baseYear) {
					rna.printRMSNstatistic();
					rna.printGEHstatistic();
				}
					
			}
			
			if (predictedYear == fromYear) return; //skip the rest if predicting the same year
			
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
			ODMatrixMultiKey predictedPassengerODMatrix = new ODMatrixMultiKey();
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
			//if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();
					
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
			//if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();
			
			//SECOND STAGE PREDICTION (FROM CHANGES IN COST AND TIME)
			
			SkimMatrix tsm = null, csm = null;
			SkimMatrixFreight tsmf = null, csmf = null;
			RoadNetworkAssignment predictedRna = null;
			for (int i=0; i<this.predictionIterations; i++) {

				if (predictedRna == null)
					//assign predicted year - using link travel times from fromYear
					predictedRna = new RoadNetworkAssignment(this.roadNetwork,
															 this.zoning,
															 this.yearToEnergyUnitCosts.get(predictedYear),
															 this.yearToUnitCO2Emissions.get(predictedYear),
															 this.yearToEngineTypeFractions.get(predictedYear), 
															 this.yearToAVFractions.get(predictedYear),
															 this.vehicleTypeToPCU,
															 this.baseFuelConsumptionRates,
															 this.yearToRelativeFuelEfficiencies.get(predictedYear),
															 this.yearToTimeOfDayDistribution.get(predictedYear),
															 this.yearToTimeOfDayDistributionFreight.get(predictedYear),
															 rna.getCopyOfLinkTravelTimesAsMap(), 
															 rna.getAreaCodeProbabilities(), 
															 rna.getWorkplaceZoneProbabilities(),
															 this.yearToCongestionCharges.get(predictedYear),
															 this.props);
				else
					//using latest link travel times
					predictedRna = new RoadNetworkAssignment(this.roadNetwork,
															 this.zoning,
															 this.yearToEnergyUnitCosts.get(predictedYear),
															 this.yearToUnitCO2Emissions.get(predictedYear),
															 this.yearToEngineTypeFractions.get(predictedYear), 
															 this.yearToAVFractions.get(predictedYear),
															 this.vehicleTypeToPCU,
															 this.baseFuelConsumptionRates,
															 this.yearToRelativeFuelEfficiencies.get(predictedYear),
															 this.yearToTimeOfDayDistribution.get(predictedYear),
															 this.yearToTimeOfDayDistributionFreight.get(predictedYear),
															 predictedRna.getCopyOfLinkTravelTimesAsMap(), 
															 predictedRna.getAreaCodeProbabilities(), 
															 predictedRna.getWorkplaceZoneProbabilities(),
															 this.yearToCongestionCharges.get(predictedYear),
															 this.props);
				
				AssignableODMatrix predictedPassengerODMatrixToAssign;
				//if tempro or combined assignment used, disaggregate LAD-based matrix to tempro level using the template
				final String assignmentType = props.getProperty("ASSIGNMENT_TYPE").toLowerCase();
				if (assignmentType.equals("tempro") || assignmentType.equals("combined")) {
					predictedPassengerODMatrixToAssign = ODMatrixMultiKey.createTEMProFromLadMatrix(predictedPassengerODMatrix, this.temproMatrixTemplate, zoning);
				} else
					predictedPassengerODMatrixToAssign = predictedPassengerODMatrix;
				
				int expectedTripListSize = predictedPassengerODMatrixToAssign.getTotalIntFlow() + predictedFreightODMatrix.getTotalIntFlow();
				predictedRna.initialiseTripList(expectedTripListSize);
							
				predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrixToAssign, predictedFreightODMatrix, this.rsg, this.zoning, this.props, this.linkTravelTimeAveragingWeight, this.assignmentIterations);
				
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
										
					double oldODTravelTime = this.yearToTimeSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
					double newODTravelTime = tsm.getCost(originZone, destinationZone);
					double oldODTravelCost = this.yearToCostSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
					double newODTravelCost = csm.getCost(originZone, destinationZone);
					
					if (oldODTravelTime == 0.0) LOGGER.warn("Unknown old travel time between zone {} and zone {}.", originZone, destinationZone);
					if (newODTravelTime == 0.0) LOGGER.warn("Unknown new travel time between zone {} and zone {}.", originZone, destinationZone);
					if (oldODTravelCost == 0.0) LOGGER.warn("Unknown old travel cost between zone {} and zone {}.", originZone, destinationZone);
					if (newODTravelCost == 0.0) LOGGER.warn("Unknown new travel cost between zone {} and zone {}.", originZone, destinationZone);
					if (oldODTravelTime == 0.0 || newODTravelTime == 0.0)	{ //if either is undefined assume the ratio is 1, i.e. not affecting the prediction
						oldODTravelTime = 1.0;
						newODTravelTime = 1.0;
					}
					if (oldODTravelCost == 0.0 || newODTravelCost == 0.0) { 
						oldODTravelCost = 1.0;
						newODTravelCost = 1.0;
					}
					
					double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticities.get(ElasticityTypes.TIME)) *
							Math.pow(newODTravelCost / oldODTravelCost, elasticities.get(ElasticityTypes.COST));

					predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedflow));
				}

				LOGGER.debug("Second stage prediction passenger matrix (from changes in skim matrices):");
				//if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();
				//if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();
								
				//for each OD pair predict the change in freight vehicle flow from the change in skim matrices
				for (MultiKey mk: this.yearToFreightODMatrix.get(fromYear).getKeySet()) {
					int origin = (int) mk.getKey(0);
					int destination = (int) mk.getKey(1);
					int vehicleType = (int) mk.getKey(2);

					double oldFlow = predictedFreightODMatrix.getFlow(origin, destination, vehicleType);

					double oldODTravelTime = this.yearToTimeSkimMatrixFreight.get(predictedYear).getCost(origin, destination, vehicleType);
					double newODTravelTime = tsmf.getCost(origin, destination, vehicleType);
					double oldODTravelCost = this.yearToCostSkimMatrixFreight.get(predictedYear).getCost(origin, destination, vehicleType);
					double newODTravelCost = csmf.getCost(origin, destination, vehicleType);
					
					if (oldODTravelTime == 0.0) LOGGER.warn("Unknown old travel time between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
					if (newODTravelTime == 0.0) LOGGER.warn("Unknown new travel time between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
					if (oldODTravelCost == 0.0) LOGGER.warn("Unknown old travel cost between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
					if (newODTravelCost == 0.0) LOGGER.warn("Unknown new travel cost between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
					if (oldODTravelTime == 0.0 || newODTravelTime == 0.0) { //if either is undefined assume the ratio is 1, i.e. not affecting the prediction
						oldODTravelTime = 1.0;
						newODTravelTime = 1.0;
					}
					if (oldODTravelCost == 0.0 || newODTravelCost == 0.0) { 
						oldODTravelCost = 1.0;
						newODTravelCost = 1.0;
					}

					double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticitiesFreight.get(ElasticityTypes.TIME)) *
							Math.pow(newODTravelCost / oldODTravelCost, elasticitiesFreight.get(ElasticityTypes.COST));

					predictedFreightODMatrix.setFlow(origin, destination, vehicleType, (int) Math.round(predictedflow));
				}

				LOGGER.debug("Second stage prediction freight matrix (from changes in skim matrices):");
				//if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();
				//if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();
				
				//assign predicted year again using latest link travel times
				predictedRna = new RoadNetworkAssignment(this.roadNetwork,
														 this.zoning,
														 this.yearToEnergyUnitCosts.get(predictedYear),
														 this.yearToUnitCO2Emissions.get(predictedYear),
														 this.yearToEngineTypeFractions.get(predictedYear), 
														 this.yearToAVFractions.get(predictedYear),
														 this.vehicleTypeToPCU,
														 this.baseFuelConsumptionRates,
														 this.yearToRelativeFuelEfficiencies.get(predictedYear),
														 this.yearToTimeOfDayDistribution.get(predictedYear),
														 this.yearToTimeOfDayDistributionFreight.get(predictedYear),
														 predictedRna.getCopyOfLinkTravelTimesAsMap(), 
														 predictedRna.getAreaCodeProbabilities(), 
														 predictedRna.getWorkplaceZoneProbabilities(),
														 this.yearToCongestionCharges.get(predictedYear),
														 this.props);
				//predictedRna.resetLinkVolumes();
				//predictedRna.assignPassengerFlows(predictedPassengerODMatrix);
				//predictedRna.updateLinkTravelTimes(ALPHA_LINK_TRAVEL_TIME_AVERAGING);
				
				//if tempro or combined assignment used, disaggregate LAD-based matrix to tempro level using the template
				if (assignmentType.equals("tempro") || assignmentType.equals("combined")) {
					predictedPassengerODMatrixToAssign = ODMatrixMultiKey.createTEMProFromLadMatrix(predictedPassengerODMatrix, this.temproMatrixTemplate, zoning);
				} else
					predictedPassengerODMatrixToAssign = predictedPassengerODMatrix;
				
				expectedTripListSize = predictedPassengerODMatrixToAssign.getTotalIntFlow() + predictedFreightODMatrix.getTotalIntFlow();
				predictedRna.initialiseTripList(expectedTripListSize);
				
				predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrixToAssign, predictedFreightODMatrix, this.rsg, this.zoning, this.props, this.linkTravelTimeAveragingWeight, this.assignmentIterations);				
				
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
	 * Assigned base year demand.
	 */
	public void assignBaseYear() {
		
		//check if the right interventions have been installed
		if (interventions != null)
			for (Intervention i: interventions) {
				if (i.getStartYear() <= baseYear && i.getEndYear() >= baseYear && !i.getState())				i.install(this);
				if (i.getEndYear() < baseYear && i.getState() || i.getStartYear() > baseYear && i.getState())	i.uninstall(this);
			}
					
		LOGGER.debug("Assigning the base year {}.", baseYear);

		//read default link travel time
		final String defaultLinkTravelTimeFile = props.getProperty("defaultLinkTravelTimeFile");
		Map<TimeOfDay, Map<Integer, Double>> defaultLinkTravelTime = null;
		if (defaultLinkTravelTimeFile != null) 
			defaultLinkTravelTime = InputFileReader.readLinkTravelTimeFile(this.baseYear, defaultLinkTravelTimeFile);
		
		//create a network assignment and assign the demand
		RoadNetworkAssignment rna = new RoadNetworkAssignment(this.roadNetwork,
											this.zoning,
											this.yearToEnergyUnitCosts.get(baseYear),
											this.yearToUnitCO2Emissions.get(baseYear),
											this.yearToEngineTypeFractions.get(baseYear), 
											this.yearToAVFractions.get(baseYear),
											this.vehicleTypeToPCU,
											this.baseFuelConsumptionRates,
											this.yearToRelativeFuelEfficiencies.get(baseYear),
											this.yearToTimeOfDayDistribution.get(baseYear),
											this.yearToTimeOfDayDistributionFreight.get(baseYear),
											defaultLinkTravelTime, 
											null,
											null, 
											this.yearToCongestionCharges.get(baseYear),
											this.props);
			
			AssignableODMatrix passengerODM;
			//if tempro or combined assignment used, disaggregate LAD-based matrix to tempro level using the template
			final String assignmentType = props.getProperty("ASSIGNMENT_TYPE").toLowerCase();
			if (assignmentType.equals("tempro") || assignmentType.equals("combined")) {
				passengerODM = ODMatrixMultiKey.createTEMProFromLadMatrix(this.yearToPassengerODMatrix.get(baseYear), this.temproMatrixTemplate, zoning);
			} else
				passengerODM = this.yearToPassengerODMatrix.get(baseYear);
			
			FreightMatrix freightODM = this.yearToFreightODMatrix.get(baseYear);
			
			int expectedTripListSize = passengerODM.getTotalIntFlow() + freightODM.getTotalIntFlow();
			rna.initialiseTripList(expectedTripListSize);
			
			rna.assignFlowsAndUpdateLinkTravelTimesIterated(passengerODM, freightODM, this.rsg, this.zoning, this.props, this.linkTravelTimeAveragingWeight, this.assignmentIterations);
			yearToRoadNetworkAssignment.put(baseYear, rna);
	
			//calculate skim matrices
			SkimMatrix tsm = rna.calculateTimeSkimMatrix();
			SkimMatrix csm = rna.calculateCostSkimMatrix();
			SkimMatrixFreight tsmf = rna.calculateTimeSkimMatrixFreight();
			SkimMatrixFreight csmf = rna.calculateCostSkimMatrixFreight();
			
			yearToTimeSkimMatrix.put(baseYear, tsm);
			yearToCostSkimMatrix.put(baseYear, csm);
			yearToTimeSkimMatrixFreight.put(baseYear, tsmf);
			yearToCostSkimMatrixFreight.put(baseYear, csmf);
			
			//print traffic count comparison data
			rna.printRMSNstatistic();
			rna.printGEHstatistic();
			rna.printGEHstatistic(this.yearToTimeOfDayDistribution.get(baseYear).get(TimeOfDay.EIGHTAM));
			rna.printRMSNstatisticFreight();
			rna.printGEHstatisticFreight();
			
			//observed trip length distribution
			LOGGER.debug("Trip length distributions:");
			LOGGER.debug("Empirical: \n{}", Arrays.toString(EstimatedODMatrix.OTLD));
			LOGGER.debug("Without access/egress: \n{}", Arrays.toString(rna.getObservedTripLengthDistribution(EstimatedODMatrix.BIN_LIMITS_KM, false, false)));
			LOGGER.debug("With access/egress: \n{}", Arrays.toString(rna.getObservedTripLengthDistribution(EstimatedODMatrix.BIN_LIMITS_KM, true, false)));
			LOGGER.debug("Without access/egress, with minor trips: \n{}", Arrays.toString(rna.getObservedTripLengthDistribution(EstimatedODMatrix.BIN_LIMITS_KM, false, true)));
			LOGGER.debug("With access/egress, with minor trips: \n{}", Arrays.toString(rna.getObservedTripLengthDistribution(EstimatedODMatrix.BIN_LIMITS_KM, true, true)));
			LOGGER.debug("Trip length frequencies:");
			LOGGER.debug("Without access/egress: \n{}", Arrays.toString(rna.getObservedTripLengthFrequencies(EstimatedODMatrix.BIN_LIMITS_KM, false, false)));
			LOGGER.debug("With access/egress: \n{}", Arrays.toString(rna.getObservedTripLengthFrequencies(EstimatedODMatrix.BIN_LIMITS_KM, true, false)));
			LOGGER.debug("Without access/egress, with minor trips: \n{}", Arrays.toString(rna.getObservedTripLengthFrequencies(EstimatedODMatrix.BIN_LIMITS_KM, false, true)));
			LOGGER.debug("With access/egress, with minor trips: \n{}", Arrays.toString(rna.getObservedTripLengthFrequencies(EstimatedODMatrix.BIN_LIMITS_KM, true, true)));
	}
		
	/**
	 * Predicts (passenger and freight) highway demand (origin-destination vehicle flows).
	 * Uses already existing results of the fromYear.
	 * @param predictedYear The year for which the demand is predicted.
	 * @param fromYear The year from which demand the prediction is made.
	 */
	public void predictHighwayDemandUsingResultsOfFromYear(int predictedYear, int fromYear) {

		LOGGER.info("Predicting {} highway demand from existing {} demand.", predictedYear, fromYear);

		if (predictedYear < fromYear) {
			LOGGER.error("predictedYear should not be smaller than fromYear!");
			return;
		}

		//use output folder for input folder
		String inputFolder = this.props.getProperty("outputFolder") + File.separator + fromYear;

		String predictedODMatrixFile = this.props.getProperty("predictedODMatrixFile");
		String predictedFreightMatrixFile = this.props.getProperty("predictedFreightMatrixFile");
		String linkTravelTimesFile = this.props.getProperty("linkTravelTimesFile");
		String timeSkimMatrixFile = this.props.getProperty("timeSkimMatrixFile");
		String costSkimMatrixFile = this.props.getProperty("costSkimMatrixFile");
		String timeSkimMatrixFreightFile = this.props.getProperty("timeSkimMatrixFreightFile");
		String costSkimMatrixFreightFile = this.props.getProperty("costSkimMatrixFreightFile");

		if (fromYear == baseYear) { //use different file names for base year
			predictedODMatrixFile = "baseYearODMatrix.csv";
			predictedFreightMatrixFile = "baseYearFreightMatrix.csv";
		}

		//load OD matrices, time and cost skim matrices, and link travel times for fromYear
		LOGGER.info("Loading output data (OD matrices, time/cost skim matrices and link travel times) from year {}", fromYear);

		Map<TimeOfDay, Map<Integer, Double>> linkTravelTime = null;

		try {
			String inputFile = inputFolder + File.separator + predictedODMatrixFile;
			ODMatrixMultiKey passengerODM = new ODMatrixMultiKey(inputFile);
			this.yearToPassengerODMatrix.put(fromYear, passengerODM);

			inputFile = inputFolder + File.separator + predictedFreightMatrixFile;
			FreightMatrix freightODM = new FreightMatrix(inputFile);
			this.yearToFreightODMatrix.put(fromYear, freightODM);

			inputFile = inputFolder + File.separator +  timeSkimMatrixFile;
			SkimMatrix timeSkimMatrix = new SkimMatrixMultiKey(inputFile, zoning);
			this.yearToTimeSkimMatrix.put(fromYear, timeSkimMatrix);

			inputFile = inputFolder + File.separator +  costSkimMatrixFile;
			SkimMatrix costSkimMatrix = new SkimMatrixMultiKey(inputFile, zoning);
			this.yearToCostSkimMatrix.put(fromYear, costSkimMatrix);

			inputFile = inputFolder + File.separator +  timeSkimMatrixFreightFile;
			SkimMatrixFreight timeSkimMatrixFreight = new SkimMatrixFreightArray(inputFile);
			this.yearToTimeSkimMatrixFreight.put(fromYear, timeSkimMatrixFreight);

			inputFile = inputFolder + File.separator +  costSkimMatrixFreightFile;
			SkimMatrixFreight costSkimMatrixFreight = new SkimMatrixFreightArray(inputFile);
			this.yearToCostSkimMatrixFreight.put(fromYear, costSkimMatrixFreight);

			inputFile = inputFolder + File.separator + linkTravelTimesFile;
			//linkTravelTime = InputFileReader.readLinkTravelTimeFile(this.baseYear, linkTravelTimesFile);
			linkTravelTime = InputFileReader.readLinkTravelTimeFile(fromYear, inputFile);

		} catch (Exception e) {
			LOGGER.error("Error while reading output files from year {}.", fromYear);
			return;
		}

		//check if the right interventions have been installed
		if (interventions != null) 
			for (Intervention i: interventions) {
				if (i.getStartYear() <= predictedYear && i.getEndYear() >= predictedYear && !i.getState())				i.install(this);
				if (i.getEndYear() < predictedYear && i.getState() || i.getStartYear() > predictedYear && i.getState()) i.uninstall(this);
			}

		//check if any new routes need to be generated
		for (int year: this.yearToListsOfLADsForNewRouteGeneration.keySet()) {
			
			LOGGER.info("New route sets to be generated for road development in year {}", year);
			List<List<String>> lists = this.yearToListsOfLADsForNewRouteGeneration.get(year);
			LOGGER.info("LADs between which routes need to be generated: ", lists);
			
			//for each new set of LADs create new matrix, generate routes, and add into the existing route set
			for (List<String> LADs: lists) {
				
				//get tempro zones within LADs
				List<String> arcTemproZonesList = new ArrayList<String>();
				
				int count = 0;
				for (String lad: LADs) {
					List<String> tempro = this.zoning.getLADToListOfContainedZones().get(lad);
					count += tempro.size();
					arcTemproZonesList.addAll(tempro);
				}
				
				//create new matrix 
				RealODMatrixTempro odm = new RealODMatrixTempro(this.zoning);
				
				//set flow to 1 only if it exists in the tempro template matrix
				for (String originZone: arcTemproZonesList)
					for (String destinationZone: arcTemproZonesList)
						if (this.temproMatrixTemplate.getIntFlow(originZone, destinationZone) > 0)
							odm.setFlow(originZone, destinationZone, 1.0);
				
				//generate new route set
				RouteSetGenerator rsg2 = new RouteSetGenerator(this.roadNetwork, this.props);
				rsg2.generateRouteSetForODMatrixTemproDistanceBased(odm, this.zoning, 1, 1);
				rsg2.generateSingleNodeRoutes();
				LOGGER.debug(rsg2.getStatistics());
				
				LOGGER.debug("Route set before addition of new routes: {}", this.rsg.getStatistics());

				LOGGER.debug("Adding new routes into the existing route set.");
				//add new routes into rsg
				for (String originZone: arcTemproZonesList)
					for (String destinationZone: arcTemproZonesList)
						if (this.temproMatrixTemplate.getIntFlow(originZone, destinationZone) > 0) {
							
							int origin = this.zoning.getTemproCodeToIDMap().get(originZone);
							int destination = this.zoning.getTemproCodeToIDMap().get(destinationZone);
							
							int originNode = this.zoning.getZoneIDToNearestNodeIDMap()[origin];
							int destinationNode = this.zoning.getZoneIDToNearestNodeIDMap()[destination];
							
							RouteSet rs = this.rsg.getRouteSet(originNode, destinationNode);
							RouteSet rs2 = rsg2.getRouteSet(originNode, destinationNode);
							
							for (Route r: rs2.getChoiceSet())
								rs.addRoute(r);
						}
				
				LOGGER.debug("Route set after addition of new routes: {}", this.rsg.getStatistics());
			}
		}
			
		//copy skim matrices from fromYear into predictedYear
		yearToTimeSkimMatrix.put(predictedYear, yearToTimeSkimMatrix.get(fromYear));
		yearToCostSkimMatrix.put(predictedYear, yearToCostSkimMatrix.get(fromYear));
		yearToTimeSkimMatrixFreight.put(predictedYear, yearToTimeSkimMatrixFreight.get(fromYear));
		yearToCostSkimMatrixFreight.put(predictedYear, yearToCostSkimMatrixFreight.get(fromYear));

		//predicted demand	
		ODMatrixMultiKey predictedPassengerODMatrix = new ODMatrixMultiKey();
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
		//if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();

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
		//if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();

		//SECOND STAGE PREDICTION (FROM CHANGES IN COST AND TIME)

		SkimMatrix tsm = null, csm = null;
		SkimMatrixFreight tsmf = null, csmf = null;
		RoadNetworkAssignment predictedRna = null;
		for (int i=0; i<this.predictionIterations; i++) {

			if (predictedRna == null)
				//assign predicted year - using link travel times from fromYear
				predictedRna = new RoadNetworkAssignment(this.roadNetwork,
						this.zoning,
						this.yearToEnergyUnitCosts.get(predictedYear),
						this.yearToUnitCO2Emissions.get(predictedYear),
						this.yearToEngineTypeFractions.get(predictedYear), 
						this.yearToAVFractions.get(predictedYear),
						this.vehicleTypeToPCU,
						this.baseFuelConsumptionRates,
						this.yearToRelativeFuelEfficiencies.get(predictedYear),
						this.yearToTimeOfDayDistribution.get(predictedYear),
						this.yearToTimeOfDayDistributionFreight.get(predictedYear),
						linkTravelTime, 
						null, 
						null,
						this.yearToCongestionCharges.get(predictedYear),
						this.props);
			else
				//using latest link travel times
				predictedRna = new RoadNetworkAssignment(this.roadNetwork,
						this.zoning,
						this.yearToEnergyUnitCosts.get(predictedYear),
						this.yearToUnitCO2Emissions.get(predictedYear),
						this.yearToEngineTypeFractions.get(predictedYear), 
						this.yearToAVFractions.get(predictedYear),
						this.vehicleTypeToPCU,
						this.baseFuelConsumptionRates,
						this.yearToRelativeFuelEfficiencies.get(predictedYear),
						this.yearToTimeOfDayDistribution.get(predictedYear),
						this.yearToTimeOfDayDistributionFreight.get(predictedYear),
						predictedRna.getCopyOfLinkTravelTimesAsMap(), 
						predictedRna.getAreaCodeProbabilities(), 
						predictedRna.getWorkplaceZoneProbabilities(),
						this.yearToCongestionCharges.get(predictedYear),
						this.props);

			AssignableODMatrix predictedPassengerODMatrixToAssign;
			//if tempro or combined assignment used, disaggregate LAD-based matrix to tempro level using the template
			final String assignmentType = props.getProperty("ASSIGNMENT_TYPE").toLowerCase();
			if (assignmentType.equals("tempro") || assignmentType.equals("combined")) {
				predictedPassengerODMatrixToAssign = ODMatrixMultiKey.createTEMProFromLadMatrix(predictedPassengerODMatrix, this.temproMatrixTemplate, zoning);
			} else
				predictedPassengerODMatrixToAssign = predictedPassengerODMatrix;

			int expectedTripListSize = predictedPassengerODMatrixToAssign.getTotalIntFlow() + predictedFreightODMatrix.getTotalIntFlow();
			predictedRna.initialiseTripList(expectedTripListSize);

			predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrixToAssign, predictedFreightODMatrix, this.rsg, this.zoning, this.props, this.linkTravelTimeAveragingWeight, this.assignmentIterations);

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

				double oldODTravelTime = this.yearToTimeSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
				double newODTravelTime = tsm.getCost(originZone, destinationZone);
				double oldODTravelCost = this.yearToCostSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
				double newODTravelCost = csm.getCost(originZone, destinationZone);

				if (oldODTravelTime == 0.0) LOGGER.warn("Unknown old travel time between zone {} and zone {}.", originZone, destinationZone);
				if (newODTravelTime == 0.0) LOGGER.warn("Unknown new travel time between zone {} and zone {}.", originZone, destinationZone);
				if (oldODTravelCost == 0.0) LOGGER.warn("Unknown old travel cost between zone {} and zone {}.", originZone, destinationZone);
				if (newODTravelCost == 0.0) LOGGER.warn("Unknown new travel cost between zone {} and zone {}.", originZone, destinationZone);
				if (oldODTravelTime == 0.0 || newODTravelTime == 0.0)	{ //if either is undefined assume the ratio is 1, i.e. not affecting the prediction
					oldODTravelTime = 1.0;
					newODTravelTime = 1.0;
				}
				if (oldODTravelCost == 0.0 || newODTravelCost == 0.0) { 
					oldODTravelCost = 1.0;
					newODTravelCost = 1.0;
				}

				double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticities.get(ElasticityTypes.TIME)) *
						Math.pow(newODTravelCost / oldODTravelCost, elasticities.get(ElasticityTypes.COST));

				predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedflow));
			}

			LOGGER.debug("Second stage prediction passenger matrix (from changes in skim matrices):");
			//if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();
			//if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedPassengerODMatrix.printMatrixFormatted();

			//for each OD pair predict the change in freight vehicle flow from the change in skim matrices
			for (MultiKey mk: this.yearToFreightODMatrix.get(fromYear).getKeySet()) {
				int origin = (int) mk.getKey(0);
				int destination = (int) mk.getKey(1);
				int vehicleType = (int) mk.getKey(2);

				double oldFlow = predictedFreightODMatrix.getFlow(origin, destination, vehicleType);

				double oldODTravelTime = this.yearToTimeSkimMatrixFreight.get(predictedYear).getCost(origin, destination, vehicleType);
				double newODTravelTime = tsmf.getCost(origin, destination, vehicleType);
				double oldODTravelCost = this.yearToCostSkimMatrixFreight.get(predictedYear).getCost(origin, destination, vehicleType);
				double newODTravelCost = csmf.getCost(origin, destination, vehicleType);

				if (oldODTravelTime == 0.0) LOGGER.warn("Unknown old travel time between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
				if (newODTravelTime == 0.0) LOGGER.warn("Unknown new travel time between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
				if (oldODTravelCost == 0.0) LOGGER.warn("Unknown old travel cost between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
				if (newODTravelCost == 0.0) LOGGER.warn("Unknown new travel cost between freight zone {} and freight zone {} for vehicle {}.", origin, destination, vehicleType);
				if (oldODTravelTime == 0.0 || newODTravelTime == 0.0)	{ //if either is undefined assume the ratio is 1, i.e. not affecting the prediction
					oldODTravelTime = 1.0;
					newODTravelTime = 1.0;
				}
				if (oldODTravelCost == 0.0 || newODTravelCost == 0.0) { 
					oldODTravelCost = 1.0;
					newODTravelCost = 1.0;
				}

				double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticitiesFreight.get(ElasticityTypes.TIME)) *
						Math.pow(newODTravelCost / oldODTravelCost, elasticitiesFreight.get(ElasticityTypes.COST));

				predictedFreightODMatrix.setFlow(origin, destination, vehicleType, (int) Math.round(predictedflow));
			}

			LOGGER.debug("Second stage prediction freight matrix (from changes in skim matrices):");
			//if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();
			//if (LogManager.getRootLogger().getLevel().isLessSpecificThan(Level.DEBUG)) predictedFreightODMatrix.printMatrixFormatted();

			//assign predicted year again using latest link travel times
			predictedRna = new RoadNetworkAssignment(this.roadNetwork,
					this.zoning,
					this.yearToEnergyUnitCosts.get(predictedYear),
					this.yearToUnitCO2Emissions.get(predictedYear),
					this.yearToEngineTypeFractions.get(predictedYear), 
					this.yearToAVFractions.get(predictedYear),
					this.vehicleTypeToPCU,
					this.baseFuelConsumptionRates,
					this.yearToRelativeFuelEfficiencies.get(predictedYear),
					this.yearToTimeOfDayDistribution.get(predictedYear),
					this.yearToTimeOfDayDistributionFreight.get(predictedYear),
					predictedRna.getCopyOfLinkTravelTimesAsMap(), 
					predictedRna.getAreaCodeProbabilities(), 
					predictedRna.getWorkplaceZoneProbabilities(),
					this.yearToCongestionCharges.get(predictedYear),
					this.props);
			//predictedRna.resetLinkVolumes();
			//predictedRna.assignPassengerFlows(predictedPassengerODMatrix);
			//predictedRna.updateLinkTravelTimes(ALPHA_LINK_TRAVEL_TIME_AVERAGING);

			//if tempro or combined assignment used, disaggregate LAD-based matrix to tempro level using the template
			if (assignmentType.equals("tempro") || assignmentType.equals("combined")) {
				predictedPassengerODMatrixToAssign = ODMatrixMultiKey.createTEMProFromLadMatrix(predictedPassengerODMatrix, this.temproMatrixTemplate, zoning);
			} else
				predictedPassengerODMatrixToAssign = predictedPassengerODMatrix;

			expectedTripListSize = predictedPassengerODMatrixToAssign.getTotalIntFlow() + predictedFreightODMatrix.getTotalIntFlow();
			predictedRna.initialiseTripList(expectedTripListSize);

			predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrixToAssign, predictedFreightODMatrix, this.rsg, this.zoning, this.props, this.linkTravelTimeAveragingWeight, this.assignmentIterations);				

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
                                                                                                                
	/**
	 * Getter method for the passenger demand in a given year.
	 * @param year Year for which the demand is requested.
	 * @return Origin-destination matrix with passenger vehicle flows.
	 */
	public ODMatrixMultiKey getPassengerDemand (int year) {

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
	public void setEngineTypeFractions(int year, Map<VehicleType, Map<EngineType, Double>> engineTypeFractions) {
		
		this.yearToEngineTypeFractions.put(year, engineTypeFractions);
	}
	
	/**
	 * Setter method for engine type fractions in a given year for a specific vehicle type.
	 * @param year Year of the data.
	 * @param vht Vehicle type.
	 * @param engineTypeFractions Map with engine type fractions.
	 */
	public void setEngineTypeFractions(int year, VehicleType vht, Map<EngineType, Double> engineTypeFractions) {
		
		this.yearToEngineTypeFractions.get(year).put(vht, engineTypeFractions);
	}
	
	/**
	 * Setter method for congestion charges (overrides them completely).
	 * @param year Year of the congestion charges.
	 * @param congestionCharges Congestion charges.
	 */
	public void setCongestionCharges(int year, List<PricingPolicy> congestionCharges) {
		
		this.yearToCongestionCharges.put(year, congestionCharges);
	}

	/**
	 * Getter method for congestion charges.
	 * @param year Year of the congestion charges.
	 * @return Congestion charges.
	 */
	public List<PricingPolicy> getCongestionCharges(int year) {
		
		return this.yearToCongestionCharges.get(year);
	}
	
	/**
	 * Adds congestion charges to the list of the existing ones.
	 * @param year Year of the policy.
	 * @param congestionCharges Congestion charges.
	 */
	public void addCongestionCharges(int year, PricingPolicy congestionCharges) {
		
		List<PricingPolicy> list = this.yearToCongestionCharges.get(year);
		//if there is no congestion charges yet for this year, create the list
		if (list == null) {
			list = new ArrayList<PricingPolicy>();
			this.yearToCongestionCharges.put(year, list);
		}
		//add the congestion charge if not already included
		if (!list.contains(congestionCharges))
			list.add(congestionCharges);
	}
	
	/**
	 * Removes congestion charges from the list of the existing ones.
	 * @param year Year of the congestion charges.
	 * @param congestionCharges Congestion charges.
	 */
	public void removeCongestionCharges(int year, PricingPolicy congestionCharges) {
		
		List<PricingPolicy> list = this.yearToCongestionCharges.get(year);

		//if there is no existing congestion charge, there is nothing to remove
		if (list == null) {
				return;
		} else { //remove the existing ones
			list.remove(congestionCharges);
		}
	}
	
	/**
	 * Removes congestion charges from the list of the existing ones using the policy name.
	 * @param year Year of the congestion charges.
	 * @param policyName Name of the policy.
	 */
	public void removeCongestionCharges(int year, String policyName) {
		
		List<PricingPolicy> list = this.yearToCongestionCharges.get(year);
		//if there is no existing congestion charge, there is nothing to remove
		if (list == null) return;
				
		//remove the existing ones
		Iterator<PricingPolicy> iter = list.iterator();
		while (iter.hasNext()) {
			PricingPolicy policy = iter.next();
			if (policy.getPolicyName().equals(policyName))
				iter.remove();
		}
	}
	
	/**
	 * Saves road network assignment results into a csv file.
	 * @param year Year of the data.
	 * @param outputFile Output file name.
	 */
	public void saveAssignmentResults(int year, String outputFile) {

		this.yearToRoadNetworkAssignment.get(year).saveAssignmentResults(year, outputFile);
	}
			
	/**
	 * Saves all results into the output folder.
	 * @param year Year of the data.
	 */
	public void saveAllResults(int year) {
		
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
		String zonalTemporalEVTripStartsFile = this.props.getProperty("zonalTemporalEVTripStartsFile");
		String zonalTemporalEVTripElectricityFile = this.props.getProperty("zonalTemporalEVTripElectricityFile");
		String tripsFile = this.props.getProperty("tripsFile");
		String outputNetworkFile = this.props.getProperty("outputNetworkFile");
	
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
		outputFile = outputFile.substring(0, outputFile.length()-5) + "WithAccessEgress.csv";
		this.yearToRoadNetworkAssignment.get(year).saveZonalVehicleKilometresWithAccessEgress(year, outputFile);
		
		outputFile = file.getPath() + File.separator +  energyConsumptionsFile;
		this.yearToRoadNetworkAssignment.get(year).saveTotalEnergyConsumptions(year, outputFile);
		outputFile = outputFile.substring(0, outputFile.length()-5) + "ZonalCar.csv";
		this.yearToRoadNetworkAssignment.get(year).saveZonalCarEnergyConsumptions(year, 1.0, outputFile);
		
		outputFile = file.getPath() + File.separator +  totalCO2EmissionsFile;
		this.yearToRoadNetworkAssignment.get(year).saveTotalCO2Emissions(year, outputFile);
		outputFile = outputFile.substring(0, outputFile.length()-5) + "ZonalPerVehicleType.csv";
		this.yearToRoadNetworkAssignment.get(year).saveZonalVehicleCO2Emissions(year, 0.5, outputFile);
	
		outputFile = file.getPath() + File.separator +  assignmentResultsFile;
		this.yearToRoadNetworkAssignment.get(year).saveAssignmentResults(year, outputFile);
		
		outputFile = file.getPath() + File.separator + linkTravelTimesFile;
		this.yearToRoadNetworkAssignment.get(year).saveLinkTravelTimes(year, outputFile);
		
		outputFile = file.getPath() + File.separator + zonalTemporalEVTripStartsFile;
		this.yearToRoadNetworkAssignment.get(year).saveZonalTemporalTripStartsForEVs(year, outputFile);
		
		outputFile = file.getPath() + File.separator + zonalTemporalEVTripElectricityFile;
		this.yearToRoadNetworkAssignment.get(year).saveZonalTemporalCarElectricity(year, 1.0, outputFile);
		
		//before saving output network file - make sure correct interventions are installed!
		if (interventions != null) 
			for (Intervention i: interventions) {
				if (i.getStartYear() <= year && i.getEndYear() >= year && !i.getState())				i.install(this);
				if (i.getEndYear() < year && i.getState() || i.getStartYear() > year && i.getState()) 	i.uninstall(this);
			}
		
		outputFile = file.getPath() + File.separator + outputNetworkFile;
		double[] capUtil = this.yearToRoadNetworkAssignment.get(year).calculateDirectionAveragedPeakLinkCapacityUtilisation();
		Map<Integer, Double> capUtilMap = new HashMap<Integer, Double>();
		for (int edgeID = 0; edgeID < capUtil.length; edgeID++)
			capUtilMap.put(edgeID, capUtil[edgeID]);
		try {
			LOGGER.debug("Saving road network shapefile for year {}.", year);
			SimpleFeatureCollection sfc = roadNetwork.createNetworkFeatureCollection(capUtilMap, "CapUtil", outputFile);
		} catch (IOException e) {
			LOGGER.error("Error while saving road network for year {}",  year);
		}
	}
	
	/**
	 * Getter method for engine type fractions in a given year.
	 * @param year Year of the data.
	 * @return Map with engine type fractions.
	 */
	public Map<VehicleType, Map<EngineType, Double>> getEngineTypeFractions(int year) {
		
		return this.yearToEngineTypeFractions.get(year);
	}
	
	/**
	 * Getter method for the road network.
	 * @return Road network.
	 */
	public RoadNetwork getRoadNetwork() {
		
		return this.roadNetwork;
	}
	
	/**
	 * Getter method for the list of LADs.
	 * @return Lists of LADs for new route generation.
	 */
	public HashMap<Integer, List<List<String>>> getListsOfLADsForNewRouteGeneration () {
		
		return this.yearToListsOfLADsForNewRouteGeneration;
	}
}