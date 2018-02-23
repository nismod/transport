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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;

/**
 * Skim matrix
 * @author Milan Lovric
 *
 */
public class SkimMatrix {
	
	private final static Logger LOGGER = Logger.getLogger(SkimMatrix.class.getName());
	
	private MultiKeyMap matrix;
		
	public SkimMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads skim matrix from an input csv file.
	 * @param fileName Path to the input file.
	 */
	public SkimMatrix(String fileName) throws FileNotFoundException, IOException {
		
		matrix = new MultiKeyMap();
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
					System.err.println("Number format exception in the skim matrix input file: " + e.getMessage());
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
	public Double getCost(String originZone, String destinationZone) {
		
		Double cost = (Double) matrix.get(originZone, destinationZone);
//		if (cost == null) return 0.0;
//		else return cost;
		
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
	 * Prints the matrix.
	 */
	public void printMatrix() {
		
		System.out.println(matrix.toString());
	}
	
	/**
	 * Prints the matrix as a formatted table.
	 */
	public void printMatrixFormatted() {
		
		Set<String> firstKey = new HashSet<String>();
		Set<String> secondKey = new HashSet<String>();
		
		//extract row and column keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			String destination = (String) ((MultiKey)mk).getKey(1);
			firstKey.add(origin);
			secondKey.add(destination);
		}
	
		//put them to a list and sort them
		List<String> firstKeyList = new ArrayList<String>(firstKey);
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		Collections.sort(firstKeyList);
		Collections.sort(secondKeyList);
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
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
	 * Saves the matrix into a csv file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		Set<String> firstKey = new HashSet<String>();
		Set<String> secondKey = new HashSet<String>();
		
		//extract row and column keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			String destination = (String) ((MultiKey)mk).getKey(1);
			firstKey.add(origin);
			secondKey.add(destination);
		}
	
		//put them to a list and sort them
		List<String> firstKeyList = new ArrayList<String>(firstKey);
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		Collections.sort(firstKeyList);
		Collections.sort(secondKeyList);
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
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
			System.err.println("Error in CsvFileWriter!");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing fileWriter/csvPrinter!");
				e.printStackTrace();
			}
		}
	}
		
	/**
	 * Gets the keyset of the multimap.
	 * @return
	 */
	public Set<MultiKey> getKeySet() {
		
		return matrix.keySet();
	}
	
	/**
	 * Gets average OD cost.
	 * @return
	 */
	public double getAverageCost() {
		
		double averageCost = 0.0;
		for (Object cost: matrix.values()) averageCost += (double) cost;
		averageCost /= matrix.size();
		
		return averageCost;
	}
	
	/**
	 * Gets sum of OD costs.
	 * @return
	 */
	public double getSumOfCosts() {
		
		double sumOfCosts = 0.0;
		for (Object cost: matrix.values()) sumOfCosts += (double) cost;
		
		return sumOfCosts;
	}
	
	/**
	 * Gets average OD cost weighted by demand.
	 * @param flows The demand as an origin-destination matrix.
	 * @return
	 */
	public double getAverageCost(ODMatrix flows) {
		
		double averageCost = 0.0;
		long totalFlows = 0;
		for (MultiKey mk: flows.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			averageCost += flows.getFlow(origin, destination) * (double) matrix.get(origin, destination);
			totalFlows += flows.getFlow(origin, destination);
		}
		averageCost /= totalFlows;
		
		return averageCost;
	}
	
	/**
	 * Gets sum of costs weighted by demand.
	 * @param flows The demand as an origin-destination matrix.
	 * @return
	 */
	public double getSumOfCosts(ODMatrix flows) {
		
		double sumOfCosts = 0.0;
		for (MultiKey mk: flows.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			sumOfCosts += flows.getFlow(origin, destination) * (double) matrix.get(origin, destination);
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
		for (MultiKey mk: other.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			difference += Math.abs(this.getCost(origin, destination) - other.getCost(origin, destination));
		}
	
		return difference;
	}
}
