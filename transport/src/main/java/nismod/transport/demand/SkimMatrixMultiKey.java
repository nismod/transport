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
import org.apache.commons.collections4.map.MultiKeyMap;
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
public class SkimMatrixMultiKey implements SkimMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(SkimMatrixMultiKey.class);
	
	private MultiKeyMap matrix;
	
	private Zoning zoning;
		
	/**
	 * Skim matrix constructors.
	 * @param zoning Zoning system.
	 */
	public SkimMatrixMultiKey(Zoning zoning) {
		
		this.matrix = new MultiKeyMap();
		this.zoning = zoning;
	}
	
	/**
	 * Constructor that reads skim matrix from an input csv file.
	 * @param fileName Path to the input file.
	 * @param zoning Zoning system.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public SkimMatrixMultiKey(String fileName, Zoning zoning) throws FileNotFoundException, IOException {
		
		this.matrix = new MultiKeyMap();
		this.zoning = zoning;
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("origin");
		//System.out.println("keySet = " + keySet);
		double cost;
		for (CSVRecord record : parser) { 
			//System.out.println(record);
			//System.out.println("Origin zone = " + record.get(0));
			for (String destination: keySet) {
				//System.out.println("Destination zone = " + destination);
				try {
					cost = Double.parseDouble(record.get(destination));
					this.setCost(record.get(0), destination, cost);
				} catch(NumberFormatException e) {
					LOGGER.error(e);
					this.setCost(record.get(0), destination, Double.NaN);
				}
			}
		}
		parser.close(); 
	}
	
	/**
	 * Gets cost for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination cost.
	 */
	public double getCost(String originZone, String destinationZone) {
		
		Double cost = (Double) matrix.get(originZone, destinationZone);
		if (cost == null) cost = 0.0;
		
		return cost;
	}
	
	/**
	 * Sets cost for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(String originZone, String destinationZone, double cost) {
		
		matrix.put(originZone, destinationZone, cost);
	}
	
	/**
	 * Gets cost for a given origin-destination pair.
	 * @param originZone Origin zone ID.
	 * @param destinationZone Destination zone ID.
	 * @return Origin-destination cost.
	 */
	public double getCost(int originZoneID, int destinationZoneID) {
		
		String originZone = this.zoning.getLadIDToCodeMap()[originZoneID];
		String destinationZone = this.zoning.getLadIDToCodeMap()[destinationZoneID];

		Double cost = (Double) this.matrix.get(originZone, destinationZone);
		if (cost == null) cost = 0.0;
		
		return cost;
	}
	
	/**
	 * Sets cost for a given origin-destination pair.
	 * @param originZone Origin zone ID.
	 * @param destinationZone Destination zone ID.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(int originZoneID, int destinationZoneID, double cost) {
		
		String originZone = this.zoning.getLadIDToCodeMap()[originZoneID];
		String destinationZone = this.zoning.getLadIDToCodeMap()[destinationZoneID];
		
		this.matrix.put(originZone, destinationZone, cost);
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
		
		Set<String> firstKey = new HashSet<String>();
		
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			firstKey.add(origin);
		}
		//put them into a list and sort them
		List<String> firstKeyList = new ArrayList(firstKey);
		Collections.sort(firstKeyList);
		
		return firstKeyList;
	}
	
	/**
	 * Gets the unsorted list of origins.
	 * @return List of origins.
	 */
	public List<String> getUnsortedOrigins() {
		
		Set<String> firstKey = new HashSet<String>();
		
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			firstKey.add(origin);
		}
		//put them into a list
		List<String> firstKeyList = new ArrayList(firstKey);
		
		return firstKeyList;
	}
	
	/**
	 * Gets the sorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getSortedDestinations() {
		
		Set<String> secondKey = new HashSet<String>();
		
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			String destination = (String) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list and sort them
		List<String> secondKeyList = new ArrayList(secondKey);
		Collections.sort(secondKeyList);
		
		return secondKeyList;
	}
	
	/**
	 * Gets the unsorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getUnsortedDestinations() {
		
		Set<String> secondKey = new HashSet<String>();
		
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			String destination = (String) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list
		List<String> secondKeyList = new ArrayList(secondKey);
		
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
				Double cost = this.getCost(o,s);
				if (cost != null)	System.out.printf("%10.2f", this.getCost(o,s));
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
		
		LOGGER.debug("Saving passenger skim matrix.");
		
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
					Double cost = this.getCost(origin, destination);
					if (cost != null)	record.add(String.format("%.2f", cost));
					else				record.add("N/A");
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
					Double cost = this.getCost(origin, destination);
					if (cost != null) {
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
	 * Gets the keyset of the multimap.
	 * @return Keyset.
	 */
	public Set<MultiKey> getKeySet() {
		
		return matrix.keySet();
	}
	
	/**
	 * Gets average OD cost.
	 * @return Average cost.
	 */
	public double getAverageCost() {
		
		double averageCost = 0.0;
		int number = 0;
		for (Object value: matrix.values()) {
			Double cost = (double) value;
			if (cost.isNaN()) continue; //ignore NaN values
			averageCost += cost;
			number++;
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
		for (Object value: matrix.values()) {
			Double cost = (double) value;
			if (cost.isNaN()) continue; //ignore NaN values
			sumOfCosts += cost;
		}
		
		return sumOfCosts;
	}
	
	/**
	 * Gets average OD cost weighted by demand.
	 * @param flows The demand as an origin-destination matrix.
	 * @return Average cost.
	 */
	public double getAverageCost(ODMatrix flows) {
		
		double averageCost = 0.0;
		long totalFlows = 0;
		for (MultiKey mk: flows.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			Double cost = (double) matrix.get(origin, destination);
			if (cost.isNaN()) continue; //ignore NaN values
			averageCost += flows.getFlow(origin, destination) * (double) matrix.get(origin, destination);
			totalFlows += flows.getFlow(origin, destination);
		}
		averageCost /= totalFlows;
		
		return averageCost;
	}
	
	/**
	 * Gets sum of costs multiplied by demand flows.
	 * @param flows The demand as an origin-destination matrix.
	 * @return Sum of costs.
	 */
	public double getSumOfCosts(ODMatrix flows) {
		
		double sumOfCosts = 0.0;
		for (MultiKey mk: flows.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			Double cost = this.getCost(origin, destination);
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
