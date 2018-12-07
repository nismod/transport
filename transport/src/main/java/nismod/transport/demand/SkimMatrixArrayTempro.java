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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.zone.Zoning;

/**
 * Skim matrix for storing inter-zonal travel times or costs (for passenger vehicles).
 * @author Milan Lovric
 *
 */
public class SkimMatrixArrayTempro implements SkimMatrix{
	
	private final static Logger LOGGER = LogManager.getLogger(SkimMatrixArrayTempro.class);
	
	private double[][] matrix;
	
	private Zoning zoning;
		
	/**
	 * Constructor for an empty skim matrix.
	 * Uses the maximum Tempro ID.
	 * @param zoning Zoning system.
	 */
	public SkimMatrixArrayTempro(Zoning zoning) {
						
		this.zoning = zoning;
		int maxZones = zoning.getTemproIDToCodeMap().length;
		this.matrix = new double[maxZones][maxZones]; //[0][0] is not used to allow a direct fetch via Tempro ID
	}
	
	/**
	 * Constructor that reads skim matrix from an input csv file. Can use both matrix and list format.
	 * @param fileName Path to the input file.
	 * @param zoning Zoning system.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public SkimMatrixArrayTempro(String fileName, Zoning zoning) throws FileNotFoundException, IOException {
		
		this.zoning = zoning;
		int maxZones = zoning.getTemproIDToCodeMap().length;
		this.matrix = new double[maxZones][maxZones]; //[0][0] is not used to allow a direct fetch via Tempro ID
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		Set<String> keySet = parser.getHeaderMap().keySet();

		if (keySet.contains("origin") &&  keySet.contains("destination") && keySet.contains("cost")) { //use list format
			for (CSVRecord record : parser) { 
					String origin = record.get(0);
					String destination = record.get(1);
					try {
						double cost = Double.parseDouble(record.get(2));
						this.setCost(origin, destination, cost);
					} catch(NumberFormatException e) {
						LOGGER.error(e);
						this.setCost(origin, destination, 0.0);
					}
			}
		} else { //use matrix format
			keySet.remove("origin");
			double cost;
			for (CSVRecord record : parser) { 
				for (String destination: keySet) {
					try {
						cost = Double.parseDouble(record.get(destination));
						this.setCost(record.get(0), destination, cost);
					} catch(NumberFormatException e) {
						LOGGER.error(e);
						this.setCost(record.get(0), destination, 0.0);
					}
				}
			}
		}
		parser.close();
		LOGGER.debug("Finished reading skim matrix from file.");
	}
	
	/**
	 * Gets cost for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination cost.
	 */
	public double getCost(String originZone, String destinationZone) {
		
		int i = this.zoning.getTemproCodeToIDMap().get(originZone);
		int j = this.zoning.getTemproCodeToIDMap().get(destinationZone);
		
		return this.matrix[i][j];
	}
	
	/**
	 * Sets cost for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(String originZone, String destinationZone, double cost) {
		
		int i = this.zoning.getTemproCodeToIDMap().get(originZone);
		int j = this.zoning.getTemproCodeToIDMap().get(destinationZone);
		
		this.matrix[i][j] = cost;
	}
	
	/**
	 * Gets cost for a given origin-destination pair.
	 * @param originZoneID Origin zone ID.
	 * @param destinationZoneID Destination zone ID.
	 * @return Origin-destination cost.
	 */
	public double getCost(int originZoneID, int destinationZoneID) {
		
		return this.matrix[originZoneID][destinationZoneID];
	}
	
	/**
	 * Sets cost for a given origin-destination pair.
	 * @param originZoneID Origin zone ID.
	 * @param destinationZoneID Destination zone ID.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(int originZoneID, int destinationZoneID, double cost) {
		
		this.matrix[originZoneID][destinationZoneID] = cost;
	}
	
	
	/**
	 * Prints the matrix.
	 */
	public void printMatrix() {
		
		System.out.println(matrix.toString());
	}
	
	/**
	 * Gets the sorted list of origins.
	 * @return List of origins.
	 */
	public List<String> getSortedOrigins() {
		
		Set<String> firstKey = zoning.getTemproCodeToIDMap().keySet();
		
		//put them into a list and sort them
		List<String> firstKeyList = new ArrayList<String>(firstKey);
		Collections.sort(firstKeyList);
		
		return firstKeyList;
	}
	
	/**
	 * Gets the sorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getSortedDestinations() {
		
		Set<String> secondKey = zoning.getTemproCodeToIDMap().keySet();
		//put them into a list and sort them
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		Collections.sort(secondKeyList);
		
		return secondKeyList;
	}
	
	/** 
	 * Gets the unsorted list of origins.
	 * @return List of origins.
	 */
	public List<String> getUnsortedOrigins() {
		
		Set<String> firstKey = zoning.getTemproCodeToIDMap().keySet();
		//put them into a list
		List<String> firstKeyList = new ArrayList<String>(firstKey);
		
		return firstKeyList;
	}
	
	/**
	 * Gets the unsorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getUnsortedDestinations() {
		
		Set<String> secondKey = zoning.getTemproCodeToIDMap().keySet();
		//put them into a list
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		
		return secondKeyList;
	}
	
	/**
	 * Prints the matrix as a formatted table.
	 */
	public void printMatrixFormatted() {
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
	
		//formatted print
		System.out.print("origin   "); for (String s: secondKeyList) System.out.printf("%10s",s);
		System.out.println();
		for (String o: firstKeyList) {
			System.out.print(o);
			for (String s: secondKeyList) {
				double cost = this.getCost(o,s);
				if (cost != 0.0)	System.out.printf("%10.2f", this.getCost(o,s));
				else				System.out.printf("%10s", "N/A");
			}
			System.out.println();
		}
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
		
		LOGGER.debug("Saving passenger skim matrix to a csv file...");
			
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("origin");
		for (String s: secondKeyList) header.add(s);
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (String origin: firstKeyList) {
				record.clear();
				record.add(origin);
				for (String destination: secondKeyList) {
					double cost = this.getCost(origin, destination);
					if (cost > 0.0)	record.add(String.format("%.2f", cost));
					else			record.add("0.0");
				}
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
	 * Saves the matrix into a csv file. Uses a list format (origin, destination, cost).
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormattedList(String outputFile) {
		
		LOGGER.info("Saving skim matrix to a csv file...");
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("origin");
		header.add("destination");
		header.add("cost");
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (String origin: firstKeyList)
				for (String destination: secondKeyList) {
					double cost = this.getCost(origin, destination);
					if (cost > 0.0) {
						record.clear();
						record.add(origin);
						record.add(destination);
						record.add(String.format("%.2f", cost));
						csvFilePrinter.printRecord(record);
					}
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
		
		LOGGER.debug("Skim matrix saved to a csv file.");
	}
		
	/**
	 * Gets average OD cost.
	 * @return Average cost.
	 */
	public double getAverageCost() {
		
		double averageCost = 0.0;
		int number = 0;
		for (int i=1; i<this.matrix.length; i++)
			for (int j=1; j<this.matrix[0].length; j++) {
				double cost = this.matrix[i][j];
				if (cost > 0.0) {
					averageCost += cost;
					number++;
				}
		}
		averageCost /= number;
		
		return averageCost;
	}
	
	/**
	 * Gets sum of OD costs.
	 * @return Sum of costs.
	 */
	public double getSumOfCosts() {
		
		double sumOfCosts = 0.0;
		for (int i=1; i<this.matrix.length; i++)
			for (int j=1; j<this.matrix[0].length; j++) {
				double cost = this.matrix[i][j];
				if (cost > 0.0) sumOfCosts += cost;
			}
		
		return sumOfCosts;
	}
	
	/**
	 * Gets average OD cost weighted by demand.
	 * @param flows The demand as an origin-destination matrix.
	 * @return Average cost.
	 */
	public double getAverageCost(ODMatrixMultiKey flows) {
		
		double averageCost = 0.0;
		long totalFlows = 0;
		for (MultiKey mk: flows.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			double cost = this.getCost(origin, destination);
			if (cost > 0.0) {
				averageCost += flows.getFlow(origin, destination) * cost;
				totalFlows += flows.getFlow(origin, destination);
			}
		}
		averageCost /= totalFlows;
		
		return averageCost;
	}
	
	/**
	 * Gets sum of costs multiplied by demand flows.
	 * @param flows The demand as an origin-destination matrix.
	 * @return Sum of costs.
	 */
	public double getSumOfCosts(ODMatrixMultiKey flows) {
		
		double sumOfCosts = 0.0;
		for (MultiKey mk: flows.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			double cost = this.getCost(origin, destination);
			int flow = flows.getFlow(origin, destination);
			if (cost > 0.0)
				sumOfCosts += cost * flow;
		}
		return sumOfCosts;
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(SkimMatrix other) {
		
		double difference = 0.0;
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
		
		for (String origin: firstKeyList)
			for (String destination: secondKeyList) {
				double thisCost = this.getCost(origin, destination);
				double otherCost = other.getCost(origin, destination);
				difference += Math.abs(thisCost - otherCost);
			}
	
		return difference;
	}
}
