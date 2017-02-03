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

/**
 * Demand prediction model.
 * @author Milan Lovric
  */
public class DemandModel {

	public final static int BASE_YEAR = 2015;
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
	//private SkimMatrix baseYearTimeSkimMatrix,	baseYearCostSkimMatrix;
	private RoadNetwork roadNetwork;

	/**
	 * The constructor for the demand prediction model.
	 * @param roadNetwork Road network for assignment.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public DemandModel(RoadNetwork roadNetwork, String baseYearODMatrixFile, String baseYearTimeSkimMatrixFile,
						String baseYearCostSkimMatrixFile, String populationFile, String GVAFile) throws FileNotFoundException, IOException {

		yearToPassengerODMatrix = new HashMap<Integer, ODMatrix>();
		yearToTimeSkimMatrix = new HashMap<Integer, SkimMatrix>();
		yearToCostSkimMatrix = new HashMap<Integer, SkimMatrix>();
		yearToZoneToPopulation = new HashMap<Integer, HashMap<String, Integer>>();
		yearToZoneToGVA = new HashMap<Integer, HashMap<String, Double>>();
		yearToRoadNetworkAssignment = new HashMap<Integer, RoadNetworkAssignment>();

		this.roadNetwork = roadNetwork;

		//read base-year passenger matrix
		ODMatrix passengerODMatrix = new ODMatrix(baseYearODMatrixFile);
		passengerODMatrix.printMatrix();
		yearToPassengerODMatrix.put(DemandModel.BASE_YEAR, passengerODMatrix);

		//read base-year time skim matrix
		SkimMatrix baseYearTimeSkimMatrix = new SkimMatrix(baseYearTimeSkimMatrixFile);
		yearToTimeSkimMatrix.put(DemandModel.BASE_YEAR, baseYearTimeSkimMatrix);

		//read base-year cost skim matrix
		SkimMatrix baseYearCostSkimMatrix = new SkimMatrix(baseYearCostSkimMatrixFile);
		yearToCostSkimMatrix.put(DemandModel.BASE_YEAR, baseYearCostSkimMatrix);

		//read all year population predictions
		yearToZoneToPopulation = readPopulationFile(populationFile);

		//read all year GVA predictions
		yearToZoneToGVA = readGVAFile(GVAFile);
		
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

		//check if the demand from year fromYear exists
		if (!this.yearToPassengerODMatrix.containsKey(fromYear)) {

			System.err.printf("Passenger demand from year %d does not exist!\n", fromYear);
			return;

		} else {

			//check if the demand for fromYear has already been assigned.
			RoadNetworkAssignment rna = yearToRoadNetworkAssignment.get(fromYear);
			if (rna == null) {
				//create a network assignment and assign the demand
				rna = new RoadNetworkAssignment(this.roadNetwork, null, null);
				rna.assignPassengerFlows(this.yearToPassengerODMatrix.get(fromYear));
				rna.updateLinkTravelTimes();
			}	

			//update skim matrices, and use those for the predicted year - should be also used for the from year!
			SkimMatrix tsm = new SkimMatrix();
			SkimMatrix csm = new SkimMatrix();
			rna.updateTimeSkimMatrix(tsm);
			rna.updateCostSkimMatrix(csm);
			yearToTimeSkimMatrix.put(predictedYear, tsm);
			yearToCostSkimMatrix.put(predictedYear,  csm);
			yearToRoadNetworkAssignment.put(fromYear, rna);
		}

		//predict the demand	
		ODMatrix predictedPassengerODMatrix = new ODMatrix();
		//for each OD pair
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
			double oldODTravelTime = this.yearToTimeSkimMatrix.get(fromYear).getCost(originZone, destinationZone);
			double newODTravelTime = this.yearToTimeSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);
			double oldODTravelCost = this.yearToCostSkimMatrix.get(fromYear).getCost(originZone, destinationZone);
			double newODTravelCost = this.yearToCostSkimMatrix.get(predictedYear).getCost(originZone, destinationZone);

			double predictedflow = oldFlow * Math.pow(newPopulationOriginZone / oldPopulationOriginZone, elasticities.get(ElasticityTypes.POPULATION)) *
					Math.pow(newGVAOriginZone / oldGVAOriginZone, elasticities.get(ElasticityTypes.GVA)) *
					Math.pow(newPopulationDestinationZone / oldPopulationDestinationZone, elasticities.get(ElasticityTypes.POPULATION)) *
					Math.pow(newGVADestinationZone / oldGVADestinationZone, elasticities.get(ElasticityTypes.GVA)) *
					Math.pow(newODTravelTime / oldODTravelTime, elasticities.get(ElasticityTypes.TIME)) *
					Math.pow(newODTravelCost / oldODTravelCost, elasticities.get(ElasticityTypes.COST));

			predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedflow));
		}
		
		//put predicted OD matrix in the map
		this.yearToPassengerODMatrix.put(predictedYear, predictedPassengerODMatrix);
		
		//assign predicted year
		RoadNetworkAssignment predictedRna = new RoadNetworkAssignment(this.roadNetwork, null, null);
		predictedRna.assignPassengerFlows(predictedPassengerODMatrix);
		predictedRna.updateLinkTravelTimes();
		
		//update skim matrices for predicted year after the assignment
		SkimMatrix tsm = new SkimMatrix();
		SkimMatrix csm = new SkimMatrix();
		predictedRna.updateTimeSkimMatrix(tsm);
		predictedRna.updateCostSkimMatrix(csm);
		yearToTimeSkimMatrix.put(predictedYear, tsm);
		yearToCostSkimMatrix.put(predictedYear, csm);
		yearToRoadNetworkAssignment.put(predictedYear, predictedRna);
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
}