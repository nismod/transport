/**
 * 
 */
package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;

/**
 * Demand prediction model.
 * @author Milan Lovric
 *
 */
public class DemandModel {

	public final static int BASE_YEAR = 2015;
	public final static double ELASTICITY_POPULATION = 1.0;
	public final static double ELASTICITY_GVA = 0.63;
	public final static double ELASTICITY_TIME = -0.41;
	public final static double ELASTICITY_COST = -0.215;

	private HashMap<Integer, ODMatrix> yearToPassengerODMatrix; //passenger demand
	//	private HashMap<Integer, ODMatrix> yearToFreightODMatrix; //freight demand
	private HashMap<Integer, SkimMatrix> yearToTimeSkimMatrix;
	private HashMap<Integer, SkimMatrix> yearToCostSkimMatrix;
	private HashMap<Integer, HashMap<String, Integer>> yearToZoneToPopulation;
	private HashMap<Integer, HashMap<String, Double>> yearToZoneToGVA;
	private HashMap<Integer, RoadNetworkAssignment> yearToRoadNetworkAssignment;

	private SkimMatrix baseYearTimeSkimMatrix,	baseYearCostSkimMatrix;
	private RoadNetwork roadNetwork;

	/**
	 * The constructor for the demand prediction model.
	 * @param roadNetwork Road network for assignment.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public DemandModel(RoadNetwork roadNetwork) throws FileNotFoundException, IOException {

		yearToPassengerODMatrix = new HashMap<Integer, ODMatrix>();
		yearToTimeSkimMatrix = new HashMap<Integer, SkimMatrix>();
		yearToCostSkimMatrix = new HashMap<Integer, SkimMatrix>();
		yearToZoneToPopulation = new HashMap<Integer, HashMap<String, Integer>>();
		yearToZoneToGVA = new HashMap<Integer, HashMap<String, Double>>();
		yearToRoadNetworkAssignment = new HashMap<Integer, RoadNetworkAssignment>();

		this.roadNetwork = roadNetwork;

		//read base-year passenger matrix
		ODMatrix passengerODMatrix = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");
		passengerODMatrix.printMatrix();
		yearToPassengerODMatrix.put(this.BASE_YEAR, passengerODMatrix);

		//read base-year time skim matrix
		SkimMatrix baseYearTimeSkimMatrix = new SkimMatrix("./src/test/resources/testdata/timeSkimMatrix.csv");
		yearToTimeSkimMatrix.put(this.BASE_YEAR, baseYearTimeSkimMatrix);

		//read base-year cost skim matrix
		SkimMatrix baseYearCostSkimMatrix = new SkimMatrix("./src/test/resources/testdata/costSkimMatrix.csv");
		yearToCostSkimMatrix.put(this.BASE_YEAR, baseYearCostSkimMatrix);

		//read all year population predictions
		yearToZoneToPopulation = readPopulationFile("./src/test/resources/testdata/population.csv");

		//read all year GVA predictions
		yearToZoneToGVA = readGVAFile("./src/test/resources/testdata/GVA.csv");
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
			RoadNetworkAssignment rda = yearToRoadNetworkAssignment.get(fromYear);
			if (rda == null) {
				//create a network assignment and assign the demand
				rda = new RoadNetworkAssignment(this.roadNetwork, null);
				rda.assignPassengerFlows(this.yearToPassengerODMatrix.get(fromYear));
				rda.updateLinkTravelTimes();
			}	

			//update skim matrices, and use those for the predicted year
			SkimMatrix tsm = new SkimMatrix();
			SkimMatrix csm = new SkimMatrix();
			rda.updateTimeSkimMatrix(tsm);
			rda.updateCostSkimMatrix(csm);
			yearToTimeSkimMatrix.put(predictedYear, tsm);
			yearToCostSkimMatrix.put(predictedYear,  csm);
			yearToRoadNetworkAssignment.put(fromYear, rda);
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

			double predictedflow = oldFlow * Math.pow(newPopulationOriginZone / oldPopulationOriginZone, ELASTICITY_POPULATION) *
					Math.pow(newGVAOriginZone / oldGVAOriginZone, ELASTICITY_GVA) *
					Math.pow(newPopulationDestinationZone / oldPopulationDestinationZone, ELASTICITY_POPULATION) *
					Math.pow(newGVADestinationZone / oldGVADestinationZone, ELASTICITY_GVA) *
					Math.pow(newODTravelTime / oldODTravelTime, ELASTICITY_TIME) *
					Math.pow(newODTravelCost / oldODTravelCost, ELASTICITY_COST);

			predictedPassengerODMatrix.setFlow(originZone, destinationZone, (int) Math.round(predictedflow));
		}
		
		//put predicted OD matrix in the map
		this.yearToPassengerODMatrix.put(predictedYear, predictedPassengerODMatrix);
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
		//keySet.remove("year");
		System.out.println("keySet = " + keySet);
		int population;
		for (CSVRecord record : parser) {
			System.out.println(record);
			//System.out.println("Origin zone = " + record.get(0));
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
			System.out.println(record);
			//System.out.println("Origin zone = " + record.get(0));
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