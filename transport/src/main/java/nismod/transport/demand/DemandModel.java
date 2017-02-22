/**
 * 
 */
package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;

/**
 * Demand prediction model.
 * @author Milan Lovric
  */
public class DemandModel {

	public final static int BASE_YEAR = 2015;
	public final static double LINK_TRAVEL_TIME_AVERAGING_WEIGHT = 1.0;
	public final static int ASSIGNMENT_ITERATIONS = 1;
	public final static int PREDICTION_ITERATIONS = 1;
	public static enum ElasticityTypes {
		POPULATION, GVA, TIME, COST
	}

	private HashMap<ElasticityTypes, Double> elasticities;
	private HashMap<Integer, ODMatrix> yearToPassengerODMatrix; //passenger demand
	//	private HashMap<Integer, ODMatrix> yearToFreightODMatrix; //freight demand
	private HashMap<Integer, SkimMatrix> yearToTimeSkimMatrix;
	private HashMap<Integer, SkimMatrix> yearToCostSkimMatrix;
	private HashMap<Integer, HashMap<String, Integer>> yearToZoneToPopulation;
	private HashMap<Integer, HashMap<String, Double>> yearToZoneToGVA;
	private HashMap<Integer, RoadNetworkAssignment> yearToRoadNetworkAssignment;
	private HashMap<Integer, HashMap<EngineType, Double>> yearToEnergyUnitCosts;
	//private SkimMatrix baseYearTimeSkimMatrix,	baseYearCostSkimMatrix;
	private RoadNetwork roadNetwork;

	/**
	 * The constructor for the demand prediction model.
	 * @param roadNetwork Road network for assignment.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public DemandModel(RoadNetwork roadNetwork, String baseYearODMatrixFile, String baseYearTimeSkimMatrixFile,
						String baseYearCostSkimMatrixFile, String populationFile, String GVAFile, String energyUnitCostsFile) throws FileNotFoundException, IOException {

		yearToPassengerODMatrix = new HashMap<Integer, ODMatrix>();
		yearToTimeSkimMatrix = new HashMap<Integer, SkimMatrix>();
		yearToCostSkimMatrix = new HashMap<Integer, SkimMatrix>();
		yearToZoneToPopulation = new HashMap<Integer, HashMap<String, Integer>>();
		yearToZoneToGVA = new HashMap<Integer, HashMap<String, Double>>();
		yearToRoadNetworkAssignment = new HashMap<Integer, RoadNetworkAssignment>();
		yearToEnergyUnitCosts = new HashMap<Integer, HashMap<EngineType, Double>>();

		this.roadNetwork = roadNetwork;

		//read base-year passenger matrix
		ODMatrix passengerODMatrix = new ODMatrix(baseYearODMatrixFile);
		passengerODMatrix.printMatrix();
		yearToPassengerODMatrix.put(DemandModel.BASE_YEAR, passengerODMatrix);

		//read base-year time skim matrix
		//SkimMatrix baseYearTimeSkimMatrix = new SkimMatrix(baseYearTimeSkimMatrixFile);
		//yearToTimeSkimMatrix.put(DemandModel.BASE_YEAR, baseYearTimeSkimMatrix);

		//read base-year cost skim matrix
		//SkimMatrix baseYearCostSkimMatrix = new SkimMatrix(baseYearCostSkimMatrixFile);
		//yearToCostSkimMatrix.put(DemandModel.BASE_YEAR, baseYearCostSkimMatrix);

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
	}

	/**
	 * Predicts passenger demand (origin-destination flows).
	 * @param predictedYear The year for which the demand is predicted.
	 * @param fromYear The year from which demand the prediction is made.
	 */
	public void predictPassengerDemand(int predictedYear, int fromYear) {

		System.out.printf("Predicting %d demand from %d demand\n", predictedYear, fromYear);
		
		if (predictedYear <= fromYear) {
			System.err.println("predictedYear should be greater than fromYear!");
			return;
		//check if the demand from year fromYear exists
		} else if (!this.yearToPassengerODMatrix.containsKey(fromYear)) { 
			System.err.printf("Passenger demand from year %d does not exist!\n", fromYear);
			return;
		} else {
			//check if the demand for fromYear has already been assigned, if not assign it
			RoadNetworkAssignment rna = yearToRoadNetworkAssignment.get(fromYear);
			if (rna == null) {
				System.out.printf("%d year has not been assigned to the network, so assigning it now.\n", fromYear);
				
				//create a network assignment and assign the demand
				rna = new RoadNetworkAssignment(this.roadNetwork, null, null, null);
				rna.assignFlowsAndUpdateLinkTravelTimesIterated(this.yearToPassengerODMatrix.get(fromYear), LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);
				yearToRoadNetworkAssignment.put(fromYear, rna);
	
				//calculate skim matrices
				SkimMatrix tsm = rna.calculateTimeSkimMatrix();
				SkimMatrix csm = rna.calculateCostSkimMatrix();
				yearToTimeSkimMatrix.put(fromYear, tsm);
				yearToCostSkimMatrix.put(fromYear, csm);
			}
			
			//copy skim matrices from fromYear into predictedYear
			yearToTimeSkimMatrix.put(predictedYear, yearToTimeSkimMatrix.get(fromYear));
			yearToCostSkimMatrix.put(predictedYear, yearToCostSkimMatrix.get(fromYear));

			//predicted demand	
			ODMatrix predictedPassengerODMatrix = new ODMatrix();
			
			//for each OD pair first predict the change in flow from the changes in population and GVA
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

				double predictedflow = oldFlow * Math.pow(newPopulationOriginZone / oldPopulationOriginZone, elasticities.get(ElasticityTypes.POPULATION)) *
						Math.pow(newGVAOriginZone / oldGVAOriginZone, elasticities.get(ElasticityTypes.GVA)) *
						Math.pow(newPopulationDestinationZone / oldPopulationDestinationZone, elasticities.get(ElasticityTypes.POPULATION)) *
						Math.pow(newGVADestinationZone / oldGVADestinationZone, elasticities.get(ElasticityTypes.GVA));

				predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedflow));
			}
			
			System.out.println("First stage prediction matrix (from population and GVA):");
			predictedPassengerODMatrix.printMatrixFormatted();
			
			SkimMatrix tsm = null, csm = null;
			RoadNetworkAssignment predictedRna = null;
			for (int i=0; i<PREDICTION_ITERATIONS; i++) {

				if (predictedRna == null)
					//assign predicted year - using link travel times from fromYear
					predictedRna = new RoadNetworkAssignment(this.roadNetwork, rna.getLinkTravelTimes(), rna.getAreaCodeProbabilities(), rna.getWorkplaceZoneProbabilities());
				else
					//using latest link travel times
					predictedRna = new RoadNetworkAssignment(this.roadNetwork, predictedRna.getLinkTravelTimes(), predictedRna.getAreaCodeProbabilities(), predictedRna.getWorkplaceZoneProbabilities());

				predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrix, LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);
				
				//update skim matrices for predicted year after the assignment
				tsm = predictedRna.calculateTimeSkimMatrix();
				csm = predictedRna.calculateCostSkimMatrix();

				//for each OD pair predict the change in flow from the change in skim matrices
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

				System.out.println("Second stage prediction matrix (from changes in skim matrices):");
				predictedPassengerODMatrix.printMatrixFormatted();

				//assign predicted year again using latest link travel times
				predictedRna = new RoadNetworkAssignment(this.roadNetwork, predictedRna.getLinkTravelTimes(), predictedRna.getAreaCodeProbabilities(), predictedRna.getWorkplaceZoneProbabilities());
				//predictedRna.resetLinkVolumes();
				//predictedRna.assignPassengerFlows(predictedPassengerODMatrix);
				//predictedRna.updateLinkTravelTimes(ALPHA_LINK_TRAVEL_TIME_AVERAGING);
				predictedRna.assignFlowsAndUpdateLinkTravelTimesIterated(predictedPassengerODMatrix, LINK_TRAVEL_TIME_AVERAGING_WEIGHT, ASSIGNMENT_ITERATIONS);				
				
				//store skim matrices into hashmaps
				yearToTimeSkimMatrix.put(predictedYear, tsm);
				yearToCostSkimMatrix.put(predictedYear, csm);
								
				//update skim matrices for predicted year after the assignment
				tsm = predictedRna.calculateTimeSkimMatrix();
				csm = predictedRna.calculateCostSkimMatrix();

				System.out.println("Difference in consecutive time skim matrices: " + tsm.getAbsoluteDifference(yearToTimeSkimMatrix.get(predictedYear)));
				System.out.println("Difference in consecutive cost skim matrix: " + csm.getAbsoluteDifference(yearToCostSkimMatrix.get(predictedYear)));
				
				//store road network assignment
				yearToRoadNetworkAssignment.put(predictedYear, predictedRna);

				//store predicted OD matrix in the map
				this.yearToPassengerODMatrix.put(predictedYear, predictedPassengerODMatrix);
			}//for loop
			
			//store latest skim matrices
			yearToTimeSkimMatrix.put(predictedYear, tsm);
			yearToCostSkimMatrix.put(predictedYear, csm);
		}
	}                                                                                                                                               

	/**
	 * Getter method for the passenger demand in a given year.
	 * @param year Year for which the demand is requested.
	 * @return Origin-destination matrix with passenger flows.
	 */
	public ODMatrix getPassengerDemand (int year) {

		return yearToPassengerODMatrix.get(year);
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
	 * Getter method for cost skim matrix in a given year.
	 * @param year Year for which the the skim matrix is requested.
	 * @return Cost skim matrix.
	 */
	public SkimMatrix getCostSkimMatrix (int year) {

		return 	yearToCostSkimMatrix.get(year);
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

		return map;
	}
}