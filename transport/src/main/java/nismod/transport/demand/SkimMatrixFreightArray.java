/**
 * 
 */
package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;

/**
 * Skim matrix for storing inter-zonal travel times or costs (for freight vehicles).
 * @author Milan Lovric
 *
 */
public class SkimMatrixFreightArray implements SkimMatrixFreight {
	
	private final static Logger LOGGER = LogManager.getLogger(SkimMatrixFreightArray.class);
	
	//private EnumMap<VehicleType, Double>[][] matrix; //cost = [origin][destination].get(vht)
	private double[][][] matrix; //cost = [origin][destination][vht]
		
	public SkimMatrixFreightArray() {
		
		this.matrix = new double[MAX_FREIGHT_ZONE_ID + 1][MAX_FREIGHT_ZONE_ID + 1][MAX_VEHICLE_ID + 1];
	}
	
	/**
	 * Constructor that reads freight skim matrix from an input csv file.
	 * @param fileName Path to the input file.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public SkimMatrixFreightArray(String fileName) throws FileNotFoundException, IOException {
		
		this.matrix = new double[MAX_FREIGHT_ZONE_ID + 1][MAX_FREIGHT_ZONE_ID + 1][MAX_VEHICLE_ID + 1];
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		int origin, destination, vehicleType;
		double cost;
		for (CSVRecord record : parser) {
			destination  = Integer.parseInt(record.get(0));
			origin = Integer.parseInt(record.get(1));
			vehicleType = Integer.parseInt(record.get(2));
			cost = Double.parseDouble(record.get(3));
			this.setCost(origin, destination, vehicleType, cost);			
			
		}
		parser.close(); 
	}
	
	/**
	 * Gets cost for a given origin-destination pair and a vehicle type.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param vehicleType Vehicle type.
	 * @return Origin-destination cost.
	 */
	public double getCost(int originZone, int destinationZone, int vehicleType) {
		
		double cost = this.matrix[originZone][destinationZone][vehicleType];

		return cost;
	}
	
	/**
	 * Sets cost for a given origin-destination pair and a vehicle type.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param vehicleType Vehicle type.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(int originZone, int destinationZone, int vehicleType, double cost) {
		
		this.matrix[originZone][destinationZone][vehicleType] = cost;
	}
	
	/**
	 * Prints the matrix.
	 */
	public void printMatrix() {
		
		System.out.println(matrix.toString());
	}
	
	/**
	 * Prints the matrix as a formatted table.
	 */
	public void printMatrixFormatted() {
		
		//formatted print
		System.out.printf("%6s%12s%12s%7s\n", "origin", "destination", "vehicleType", "cost");
		for (int origin = 1; origin <= MAX_FREIGHT_ZONE_ID; origin++)
			for (int destination = 1; destination <= MAX_FREIGHT_ZONE_ID; destination++)
				for (int vehicleType = 1; vehicleType <= MAX_VEHICLE_ID; vehicleType++)
					if (this.getCost(origin, destination, vehicleType) > 0.0) //print only if there is a cost
						System.out.printf("%6d%12d%12d%7.2f\n", origin, destination, vehicleType, this.getCost(origin, destination, vehicleType));
	}
	
	/**
	 * Prints the matrix as a formatted table, with a print message.
	 * @param s Print message
	 */
	public void printMatrixFormatted(String s) {
				
		System.out.println(s);
		this.printMatrixFormatted();
	}
	
	/**
	 * Saves the matrix into a csv file.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		LOGGER.debug("Saving freight skim matrix.");
		
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("destination");
		header.add("origin");
		header.add("vehicleType");
		header.add("flow");
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (int origin = 1; origin <= MAX_FREIGHT_ZONE_ID; origin++)
				for (int destination = 1; destination <= MAX_FREIGHT_ZONE_ID; destination++)
					for (int vehicleType = 1; vehicleType <= MAX_VEHICLE_ID; vehicleType++) {
						double cost = this.getCost(origin, destination, vehicleType);
						record.clear();
						record.add(Integer.toString(destination));
						record.add(Integer.toString(origin));
						record.add(Integer.toString(vehicleType));
						if (cost > 0.0)	record.add(String.format("%.2f", cost));
						else				//record.add("N/A");
											continue; //do not save record if unknown cost
						csvFilePrinter.printRecord(record);
					}
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
				
	/**
	 * Gets average OD cost (ignores empty matrix cells).
	 * @return Average cost.
	 */
	public double getAverageCost() {
		
		double averageCost = 0.0;
		int nonZeroCounter = 0;
		for (int origin = 1; origin <= MAX_FREIGHT_ZONE_ID; origin++)
			for (int destination = 1; destination <= MAX_FREIGHT_ZONE_ID; destination++)
				for (int vehicleType = 1; vehicleType <= MAX_VEHICLE_ID; vehicleType++) {
					double cost = this.getCost(origin, destination, vehicleType);
					averageCost += cost; 
					if (cost > 0.0) 
						nonZeroCounter++;
				}
		averageCost /= nonZeroCounter;
		
		return averageCost;
	}
	
	/**
	 * Gets average OD cost weighted by demand.
	 * @param flows The demand as an origin-destination matrix.
	 * @return Average cost.
	 */
	public double getAverageCost(FreightMatrix flows) {
		
		double averageCost = 0.0;
		long totalFlows = 0;
		for (int origin = 1; origin <= MAX_FREIGHT_ZONE_ID; origin++)
			for (int destination = 1; destination <= MAX_FREIGHT_ZONE_ID; destination++)
				for (int vehicleType = 1; vehicleType <= MAX_VEHICLE_ID; vehicleType++) {
					averageCost += flows.getFlow(origin, destination, vehicleType) * this.getCost(origin, destination, vehicleType);
					totalFlows += flows.getFlow(origin, destination, vehicleType);
		}
		averageCost /= totalFlows;
		
		return averageCost;
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(SkimMatrixFreight other) {
		
		double difference = 0.0;
		for (int origin = 1; origin <= MAX_FREIGHT_ZONE_ID; origin++)
			for (int destination = 1; destination <= MAX_FREIGHT_ZONE_ID; destination++)
				for (int vehicleType = 1; vehicleType <= MAX_VEHICLE_ID; vehicleType++) {
			difference += Math.abs(this.getCost(origin, destination, vehicleType) - other.getCost(origin, destination, vehicleType));
		}
	
		return difference;
	}
}
