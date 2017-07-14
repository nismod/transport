/**
 * 
 */
package nismod.transport.demand;

import java.util.List;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import nismod.transport.decision.Intervention;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;

/**
 * Demand prediction model.
 * @author Milan Lovric
  */
public class DemandModel {

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
	private HashMap<Integer, ODMatrix> yearToPassengerODMatrix; //passenger demand
	private HashMap<Integer, FreightMatrix> yearToFreightODMatrix; //freight demand
	private HashMap<Integer, SkimMatrix> yearToTimeSkimMatrix;
	private HashMap<Integer, SkimMatrixFreight> yearToTimeSkimMatrixFreight;
	private HashMap<Integer, SkimMatrix> yearToCostSkimMatrix;
	private HashMap<Integer, SkimMatrixFreight> yearToCostSkimMatrixFreight;
	private HashMap<Integer, HashMap<String, Integer>> yearToZoneToPopulation;
	private HashMap<Integer, HashMap<String, Double>> yearToZoneToGVA;
	private HashMap<Integer, RoadNetworkAssignment> yearToRoadNetworkAssignment;
	private HashMap<Integer, HashMap<EngineType, Double>> yearToEnergyUnitCosts;
	private HashMap<Integer, HashMap<EngineType, Double>> yearToEngineTypeFractions;
	//private SkimMatrix baseYearTimeSkimMatrix,	baseYearCostSkimMatrix;
	
	private List<Intervention> interventions;
	
	private RoadNetwork roadNetwork;

	/**
	 * The constructor for the demand prediction model.
	 * @param roadNetwork Road network for assignment.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public DemandModel(RoadNetwork roadNetwork, String baseYearODMatrixFile, String baseYearFreightMatrixFile, String populationFile, String GVAFile, String energyUnitCostsFile, List<Intervention> interventions) throws FileNotFoundException, IOException {

		yearToPassengerODMatrix = new HashMap<Integer, ODMatrix>();
		yearToFreightODMatrix = new HashMap<Integer, FreightMatrix>();
		yearToTimeSkimMatrix = new HashMap<Integer, SkimMatrix>();
		yearToTimeSkimMatrixFreight = new HashMap<Integer, SkimMatrixFreight>();
		yearToCostSkimMatrix = new HashMap<Integer, SkimMatrix>();
		yearToCostSkimMatrixFreight = new HashMap<Integer, SkimMatrixFreight>();
		yearToZoneToPopulation = new HashMap<Integer, HashMap<String, Integer>>();
		yearToZoneToGVA = new HashMap<Integer, HashMap<String, Double>>();
		yearToRoadNetworkAssignment = new HashMap<Integer, RoadNetworkAssignment>();
		yearToEnergyUnitCosts = new HashMap<Integer, HashMap<EngineType, Double>>();
		yearToEngineTypeFractions = new HashMap<Integer, HashMap<EngineType, Double>>();
		
		this.roadNetwork = roadNetwork;
		
		this.interventions = interventions;

		//read base-year passenger matrix
		ODMatrix passengerODMatrix = new ODMatrix(baseYearODMatrixFile);
		passengerODMatrix.printMatrixFormatted();
		yearToPassengerODMatrix.put(DemandModel.BASE_YEAR, passengerODMatrix);
		
		//read base-year freight matrix
		FreightMatrix freightMatrix = new FreightMatrix(baseYearFreightMatrixFile);
		freightMatrix.printMatrixFormatted();
		System.out.println("Freight matrix scaled to 2015:");
		FreightMatrix freightMatrixScaled = freightMatrix.getScaledMatrix(FREIGHT_SCALING_FACTOR);
		freightMatrixScaled.printMatrixFormatted();
		yearToFreightODMatrix.put(DemandModel.BASE_YEAR, freightMatrixScaled);
		
		//read all year population predictions
		yearToZoneToPopulation = readPopulationFile(populationFile);

		//read all year GVA predictions
		yearToZoneToGVA = readGVAFile(GVAFile);
		
		yearToEnergyUnitCosts = readEnergyUnitCostsFile(energyUnitCostsFile);
		
		//default elasticity values
		elasticities = new HashMap<ElasticityTypes, Double>();
		elasticities.put(ElasticityTypes.POPULATION, 1.0);
		elasticities.put(ElasticityTypes.GVA, 0.63);
		elasticities.put(ElasticityTypes.TIME, -0.41);
		elasticities.put(ElasticityTypes.COST, -0.215);
		
		elasticitiesFreight = new HashMap<ElasticityTypes, Double>();
		elasticitiesFreight.put(ElasticityTypes.POPULATION, 1.0);
		elasticitiesFreight.put(ElasticityTypes.GVA, 0.7);
		elasticitiesFreight.put(ElasticityTypes.TIME, -0.41);
		elasticitiesFreight.put(ElasticityTypes.COST, -0.1);
		
		//base-year engine type fractions
		HashMap<EngineType, Double> engineTypeFractions = new HashMap<EngineType, Double>();
		engineTypeFractions.put(EngineType.PETROL, 0.45);
		engineTypeFractions.put(EngineType.DIESEL, 0.35);
		engineTypeFractions.put(EngineType.LPG, 0.1);
		engineTypeFractions.put(EngineType.ELECTRICITY, 0.05);
		engineTypeFractions.put(EngineType.HYDROGEN, 0.025);
		engineTypeFractions.put(EngineType.HYBRID, 0.025);
		this.yearToEngineTypeFractions.put(BASE_YEAR, engineTypeFractions);
	}

	/**
	 * Predicts (passenger and freight) highway demand (origin-destination vehicle flows).
	 * @param predictedYear The year for which the demand is predicted.
	 * @param fromYear The year from which demand the prediction is made.
	 */
	public void predictHighwayDemand(int predictedYear, int fromYear) {

		System.out.printf("Predicting %d highway demand from %d demand\n", predictedYear, fromYear);
		
		if (predictedYear <= fromYear) {
			System.err.println("predictedYear should be greater than fromYear!");
			return;
		//check if the demand from year fromYear exists
		} else if (!this.yearToPassengerODMatrix.containsKey(fromYear)) { 
			System.err.printf("Passenger demand from year %d does not exist!\n", fromYear);
			return;
		} else if (!this.yearToFreightODMatrix.containsKey(fromYear)) { 
			System.err.printf("Freight demand from year %d does not exist!\n", fromYear);
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
				System.out.printf("%d year has not been assigned to the network, so assigning it now.\n", fromYear);
				
				//create a network assignment and assign the demand
				rna = new RoadNetworkAssignment(this.roadNetwork, this.yearToEnergyUnitCosts.get(fromYear), this.yearToEngineTypeFractions.get(fromYear), null, null, null);
				rna.assignFlowsAndUpdateLinkTravelTimesIterated(this.yearToPassengerODMatrix.get(fromYear), this.yearToFreightODMatrix.get(fromYear), LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);
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

				double predictedFlow = oldFlow * Math.pow(newPopulationOriginZone / oldPopulationOriginZone, elasticities.get(ElasticityTypes.POPULATION)) *
						Math.pow(newGVAOriginZone / oldGVAOriginZone, elasticities.get(ElasticityTypes.GVA)) *
						Math.pow(newPopulationDestinationZone / oldPopulationDestinationZone, elasticities.get(ElasticityTypes.POPULATION)) *
						Math.pow(newGVADestinationZone / oldGVADestinationZone, elasticities.get(ElasticityTypes.GVA));
				//System.out.printf("%d = %d * (%.0f / %.0f) ^ %.2f * (%.1f / %.1f) ^ %.2f * (%.0f / %.0f) ^ %.2f * (%.1f / %.1f) ^ %.2f\n", (int) Math.round(predictedFlow), (int) Math.round(oldFlow),
				//newPopulationOriginZone, oldPopulationOriginZone, elasticities.get(ElasticityTypes.POPULATION),
				//newGVAOriginZone, oldGVAOriginZone, elasticities.get(ElasticityTypes.GVA),
				//newPopulationDestinationZone, oldPopulationDestinationZone, elasticities.get(ElasticityTypes.POPULATION),
				//newGVADestinationZone, oldGVADestinationZone, elasticities.get(ElasticityTypes.GVA));
				
				predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedFlow));
			}
			
			System.out.println("First stage prediction passenger matrix (from population and GVA):");
			predictedPassengerODMatrix.printMatrixFormatted();
			
			//for each OD pair first predict the change in freight vehicle flows from the changes in population and GVA
			for (MultiKey mk: this.yearToFreightODMatrix.get(fromYear).getKeySet()) {
				int origin = (int) mk.getKey(0);
				int destination = (int) mk.getKey(1);
				int vehicleType = (int) mk.getKey(2);

				double oldFlow = this.yearToFreightODMatrix.get(fromYear).getFlow(origin, destination, vehicleType);
				double predictedFlow = oldFlow;

				//if origin is a LAD (<= 1032, according to the DfT BYFM model)
				if (origin <= 1032) {

					String originZone = roadNetwork.getFreightZoneToLAD().get(origin); 
					double oldPopulationOriginZone = this.yearToZoneToPopulation.get(fromYear).get(originZone);
					double newPopulationOriginZone = this.yearToZoneToPopulation.get(predictedYear).get(originZone);
					double oldGVAOriginZone = this.yearToZoneToGVA.get(fromYear).get(originZone);
					double newGVAOriginZone = this.yearToZoneToGVA.get(predictedYear).get(originZone);

					predictedFlow = predictedFlow * Math.pow(newPopulationOriginZone / oldPopulationOriginZone, elasticitiesFreight.get(ElasticityTypes.POPULATION)) *
							Math.pow(newGVAOriginZone / oldGVAOriginZone, elasticitiesFreight.get(ElasticityTypes.GVA));
				}
				//if destination is a LAD (<= 1032, according to the DfT BYFM model)
				if (destination <= 1032) {

					String destinationZone = roadNetwork.getFreightZoneToLAD().get(destination); 
					double oldPopulationDestinationZone = this.yearToZoneToPopulation.get(fromYear).get(destinationZone);
					double newPopulationDestinationZone = this.yearToZoneToPopulation.get(predictedYear).get(destinationZone);
					double oldGVADestinationZone = this.yearToZoneToGVA.get(fromYear).get(destinationZone);
					double newGVADestinationZone = this.yearToZoneToGVA.get(predictedYear).get(destinationZone);

					predictedFlow = predictedFlow *	Math.pow(newPopulationDestinationZone / oldPopulationDestinationZone, elasticitiesFreight.get(ElasticityTypes.POPULATION)) *
							Math.pow(newGVADestinationZone / oldGVADestinationZone, elasticitiesFreight.get(ElasticityTypes.GVA));
				}
				predictedFreightODMatrix.setFlow(origin, destination, vehicleType, (int) Math.round(predictedFlow));
			}

			System.out.println("First stage prediction freight matrix (from population and GVA):");
			predictedFreightODMatrix.printMatrixFormatted();
			
			//SECOND STAGE PREDICTION (FROM CHANGES IN COST AND TIME)
		
			SkimMatrix tsm = null, csm = null;
			SkimMatrixFreight tsmf = null, csmf = null;
			RoadNetworkAssignment predictedRna = null;
			for (int i=0; i<PREDICTION_ITERATIONS; i++) {

				if (predictedRna == null)
					//assign predicted year - using link travel times from fromYear
					predictedRna = new RoadNetworkAssignment(this.roadNetwork, this.yearToEnergyUnitCosts.get(predictedYear), this.yearToEngineTypeFractions.get(predictedYear), rna.getLinkTravelTimes(), rna.getAreaCodeProbabilities(), rna.getWorkplaceZoneProbabilities());
				else
					//using latest link travel times
					predictedRna = new RoadNetworkAssignment(this.roadNetwork, this.yearToEnergyUnitCosts.get(predictedYear), this.yearToEngineTypeFractions.get(predictedYear), predictedRna.getLinkTravelTimes(), predictedRna.getAreaCodeProbabilities(), predictedRna.getWorkplaceZoneProbabilities());

				predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrix, predictedFreightODMatrix, LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);
				
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

					double oldODTravelTime = this.yearToTimeSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
					double newODTravelTime = tsm.getCost(originZone, destinationZone);
					double oldODTravelCost = this.yearToCostSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
					double newODTravelCost = csm.getCost(originZone, destinationZone);

					double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticities.get(ElasticityTypes.TIME)) *
							Math.pow(newODTravelCost / oldODTravelCost, elasticities.get(ElasticityTypes.COST));

					predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedflow));
				}

				System.out.println("Second stage prediction passenger matrix (from changes in skim matrices):");
				predictedPassengerODMatrix.printMatrixFormatted();
				
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

					double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticitiesFreight.get(ElasticityTypes.TIME)) *
							Math.pow(newODTravelCost / oldODTravelCost, elasticitiesFreight.get(ElasticityTypes.COST));

					predictedFreightODMatrix.setFlow(origin, destination, vehicleType, (int) Math.round(predictedflow));
				}

				System.out.println("Second stage prediction freight matrix (from changes in skim matrices):");
				predictedFreightODMatrix.printMatrixFormatted();
				
				//assign predicted year again using latest link travel times
				predictedRna = new RoadNetworkAssignment(this.roadNetwork, this.yearToEnergyUnitCosts.get(predictedYear), this.yearToEngineTypeFractions.get(predictedYear), predictedRna.getLinkTravelTimes(), predictedRna.getAreaCodeProbabilities(), predictedRna.getWorkplaceZoneProbabilities());
				//predictedRna.resetLinkVolumes();
				//predictedRna.assignPassengerFlows(predictedPassengerODMatrix);
				//predictedRna.updateLinkTravelTimes(ALPHA_LINK_TRAVEL_TIME_AVERAGING);
				predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrix, predictedFreightODMatrix, LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);				
				
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

				System.out.println("Difference in consecutive time skim matrices: " + tsm.getAbsoluteDifference(yearToTimeSkimMatrix.get(predictedYear)));
				System.out.println("Difference in consecutive cost skim matrix: " + csm.getAbsoluteDifference(yearToCostSkimMatrix.get(predictedYear)));
				System.out.println("Difference in consecutive time skim matrices for freight: " + tsmf.getAbsoluteDifference(yearToTimeSkimMatrixFreight.get(predictedYear)));
				System.out.println("Difference in consecutive cost skim matrix for freight: " + csmf.getAbsoluteDifference(yearToCostSkimMatrixFreight.get(predictedYear)));
				
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
	 * @param year
	 * @param outputFile
	 */
	public void saveEnergyConsumptions (int year, String outputFile) {

		this.yearToRoadNetworkAssignment.get(year).saveTotalEnergyConsumptions(year, outputFile);
	}
	
	/**
	 * Setter method for engine type fractions in a given year.
	 * @param year
	 * @param engineTypeFractions
	 */
	public void setEngineTypeFractions(int year, HashMap<EngineType, Double> engineTypeFractions) {
		
		this.yearToEngineTypeFractions.put(year, engineTypeFractions);
	}
	
	/**
	 * Saves road network assignment results into a csv file.
	 * @param year
	 * @param outputFile
	 */
	public void saveAssignmentResults (int year, String outputFile) {

		this.yearToRoadNetworkAssignment.get(year).saveAssignmentResults(year, outputFile);
	}
	
	
	/**
	 * Getter method for engine type fractions in a given year.
	 * @param year
	 */
	public HashMap<EngineType, Double> getEngineTypeFractions(int year) {
		
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
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private HashMap<Integer, HashMap<String, Integer>> readPopulationFile (String fileName) throws FileNotFoundException, IOException {

		HashMap<Integer, HashMap<String, Integer>> map = new HashMap<Integer, HashMap<String, Integer>>();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		int population;
		for (CSVRecord record : parser) {
			//System.out.println(record);
			int year = Integer.parseInt(record.get(0));
			HashMap<String, Integer> zoneToPopulation = new HashMap<String, Integer>();
			for (String zone: keySet) {
				//System.out.println("Destination zone = " + destination);
				population = Integer.parseInt(record.get(zone));
				zoneToPopulation.put(zone, population);			
			}
			map.put(year, zoneToPopulation);
		} parser.close(); 

		return map;
	}

	/**
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private HashMap<Integer, HashMap<String, Double>> readGVAFile (String fileName) throws FileNotFoundException, IOException {

		HashMap<Integer, HashMap<String, Double>> map = new HashMap<Integer, HashMap<String, Double>>();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("year");
		//System.out.println("keySet = " + keySet);
		double GVA;
		for (CSVRecord record : parser) {
			//System.out.println(record);
			int year = Integer.parseInt(record.get(0));
			HashMap<String, Double> zoneToGVA = new HashMap<String, Double>();
			for (String zone: keySet) {
				//System.out.println("Destination zone = " + destination);
				GVA = Double.parseDouble(record.get(zone));
				zoneToGVA.put(zone, GVA);			
			}
			map.put(year, zoneToGVA);
		} parser.close(); 

		return map;
	}
	
	/**
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private HashMap<Integer, HashMap<EngineType, Double>> readEnergyUnitCostsFile (String fileName) throws FileNotFoundException, IOException {

		HashMap<Integer, HashMap<EngineType, Double>> map = new HashMap<Integer, HashMap<EngineType, Double>>();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("year");
		//System.out.println("keySet = " + keySet);
		double unitPrice;
		for (CSVRecord record : parser) {
			//System.out.println(record);
			int year = Integer.parseInt(record.get(0));
			HashMap<EngineType, Double> engineTypeToPrice = new HashMap<EngineType, Double>();
			for (String et: keySet) {
				//System.out.println("Destination zone = " + destination);
				EngineType engineType = EngineType.valueOf(et);
				unitPrice = Double.parseDouble(record.get(engineType));
				engineTypeToPrice.put(engineType, unitPrice);			
			}
			map.put(year, engineTypeToPrice);
		} parser.close();
		
		System.out.println(map);

		return map;
	}
}