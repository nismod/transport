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
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.zone.Zoning;

/**
 * Origin-destination matrix with real values, memory use optimised for Tempro.
 * @author Milan Lovric
 *
 */
public class RealODMatrix2 implements AssignableODMatrix {
	
	public static final int MAX_ZONES = 7700;
	
	private final static Logger LOGGER = LogManager.getLogger(RealODMatrix2.class);
	
	private double[][] matrix;
	
	private Zoning zoning;
	
	public RealODMatrix2(Zoning zoning) {
		
		this.zoning = zoning;
		int maxZones = Collections.max(zoning.getZoneIDToCodeMap().keySet()); //find maximum index
		matrix = new double[maxZones][maxZones];
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file.
	 * @param fileName Path to the input file.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public RealODMatrix2(String fileName, Zoning zoning) throws FileNotFoundException, IOException {
		
		LOGGER.info("Reading OD matrix from file: {}", fileName);
		
		this.zoning = zoning;
		int maxZones = Collections.max(zoning.getZoneIDToCodeMap().keySet());
		matrix = new double[maxZones][maxZones];
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("origin");
		//System.out.println("keySet = " + keySet);
		double flow;
		for (CSVRecord record : parser) { 
			//System.out.println(record);
			//System.out.println("Origin zone = " + record.get(0));
			for (String destination: keySet) {
				//System.out.println("Destination zone = " + destination);
				flow = Double.parseDouble(record.get(destination));
				this.setFlow(record.get(0), destination, flow);			
			}
		}
		parser.close();
		LOGGER.debug("Finished reading OD matrix from file.");
	}
	
	/**
	 * Gets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public double getFlow(String originZone, String destinationZone) {
		
		int i = this.zoning.getZoneCodeToIDMap().get(originZone) - 1;
		int j = this.zoning.getZoneCodeToIDMap().get(destinationZone) - 1;
		
		return this.matrix[i][j];
		
	}
	
	/**
	 * Gets the flow for a given origin-destination pair, rounded to a whole number.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public int getIntFlow(String originZone, String destinationZone) {
		
		return (int) Math.round(getFlow(originZone, destinationZone));
	}
	
	/**
	 * Sets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(String originZone, String destinationZone, double flow) {
	
		/*
		if (flow > 0.0)		matrix.put(originZone, destinationZone, flow);
		//do not store zero flows into the matrix (skip zero flow or remove if already exists)
		else // flow == 0 
			if (this.getFlow(originZone, destinationZone) > 0.0)
				matrix.removeMultiKey(originZone, destinationZone);
		*/
		
		int i = this.zoning.getZoneCodeToIDMap().get(originZone) - 1;
		int j = this.zoning.getZoneCodeToIDMap().get(destinationZone) - 1;

		matrix[i][j] = flow;
	}
	
	/**
	 * Gets sum of all the (rounded) flows in the matrix.
	 * @return Sum of all the (rounded) flows in the matrix (i.e. number of trips).
	 */
	public int getTotalIntFlow() {
		
		int totalFlow = 0;
		
		for (int i=0; i<matrix.length; i++)
			for (int j=0; j<matrix[0].length; j++)
				totalFlow += matrix[i][j];
			
		return totalFlow;
	}
	
	/**
	 * Calculates the number of trips starting in each origin zone
	 * @return Number of trip starts.
	 */
	public HashMap<String, Integer> calculateTripStarts() {
		
		HashMap<String, Integer> tripStarts = new HashMap<String, Integer>();
		
		for (int i=0; i<matrix.length; i++) {
			String origin = zoning.getZoneIDToCodeMap().get(i);
			for (int j=0; j<matrix[0].length; j++) {
				String destination = zoning.getZoneIDToCodeMap().get(j);
				Integer number = tripStarts.get(origin);
				if (number == null) number = 0;
				number += (int) Math.round(this.getFlow(origin, destination));
				tripStarts.put(origin, number);
			}
		}
		
		return tripStarts;
	}
	
	/**
	 * Calculates the number of trips ending in each destination zone
	 * @return Number of trip ends.
	 */
	public HashMap<String, Integer> calculateTripEnds() {
		
		HashMap<String, Integer> tripEnds = new HashMap<String, Integer>();
		
		for (int j=0; j<matrix[0].length; j++) {
			String destination = zoning.getZoneIDToCodeMap().get(j);
			for (int i=0; j<matrix.length; i++) {
				String origin = zoning.getZoneIDToCodeMap().get(i);
				Integer number = tripEnds.get(destination);
				if (number == null) number = 0;
				number += (int) Math.round(this.getFlow(origin, destination));
				tripEnds.put(destination, number);
			}
		}
		
		return tripEnds;
	}
	
	/**
	 * Prints the full matrix.
	 */
	public void printMatrix() {
		
		System.out.println(matrix.toString());
	}
	
	/**
	 * Prints the matrix as a formatted table.
	 * @param precision Number of decimal places for the matrix value.
	 */
	public void printMatrixFormatted(int precision) {
		
		List<String> firstKeyList = this.getOrigins();
		List<String> secondKeyList = this.getDestinations();
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		//formatted print
		System.out.print("origin    "); for (String s: secondKeyList) System.out.printf("%10s",s);
		System.out.println();
		for (String o: firstKeyList) {
			System.out.printf("%-10s", o);
			for (String s: secondKeyList) System.out.printf("%10." + precision + "f", this.getFlow(o,s));
			System.out.println();
		}
	}
	
	/**
	 * Gets the sorted list of origins.
	 * @return List of origins.
	 */
	public List<String> getOrigins() {
		
		Set<String> firstKey = zoning.getZoneCodeToIDMap().keySet();
		
		//put them into a list and sort them
		List<String> firstKeyList = new ArrayList<String>(firstKey);
		Collections.sort(firstKeyList);
		
		return firstKeyList;
	}
	
	/**
	 * Gets the sorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getDestinations() {
		
		Set<String> secondKey = zoning.getZoneCodeToIDMap().keySet();
		//put them into a list and sort them
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		Collections.sort(secondKeyList);
		
		return secondKeyList;
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(RealODMatrix2 other) {
		
		double difference = 0.0;
		
		for (int i=0; i<matrix.length; i++)
			for (int j=0; j<matrix[0].length; j++)
				difference += Math.abs(matrix[i][j] - other.matrix[i][j]);

		return difference;
	}
	
	/**
	 * Scales matrix values with a scaling factor.
	 * @param factor Scaling factor.
	 */
	public void scaleMatrixValue(double factor) {
		
		for (int i=0; i<matrix.length; i++)
			for (int j=0; j<matrix[0].length; j++)
				matrix[i][j] *= factor;

	}
	
	/**
	 * Scales matrix values with another matrix (element-wise multiplication).
	 * @param scalingMatrix Scaling matrix.
	 */
	public void scaleMatrixValue(RealODMatrix2 scalingMatrix) {
		
		for (int i=0; i<matrix.length; i++)
			for (int j=0; j<matrix[0].length; j++)
				matrix[i][j] *= scalingMatrix.matrix[i][j];		
	}
	
	/**
	 * Rounds OD matrix values.
	 */
	public void roundMatrixValues() {

		for (int i=0; i<matrix.length; i++)
			for (int j=0; j<matrix[0].length; j++)
				matrix[i][j] = Math.round(matrix[i][j]);	
	}
	
	/**
	 * Floor OD matrix values.
	 */
	public void floorMatrixValues() {

		for (int i=0; i<matrix.length; i++)
			for (int j=0; j<matrix[0].length; j++)
				matrix[i][j] = Math.floor(matrix[i][j]);	
	}
	
	/**
	 * Ceil OD matrix values.
	 */
	public void ceilMatrixValues() {

		for (int i=0; i<matrix.length; i++)
			for (int j=0; j<matrix[0].length; j++)
				matrix[i][j] = Math.ceil(matrix[i][j]);	
	}
	
	/**
	 * Prints message followed by the formatted matrix.
	 * @param s Message.
	 * @param precision Number of decimal places.
	 */
	public void printMatrixFormatted(String s, int precision) {
		
		System.out.println(s);
		this.printMatrixFormatted(precision);
	}
	
	/**
	 * Creates a unit OD matrix for given lists of origin and destination zones.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrix2 createUnitMatrix(List<String> origins, List<String> destinations, Zoning zoning) {
		
		RealODMatrix2 odm = new RealODMatrix2(zoning);
		
		for (String origin: origins)
			for (String destination: destinations)
				odm.setFlow(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones List of zones.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrix2 createUnitMatrix(List<String> zones, Zoning zoning) {
		
		RealODMatrix2 odm = new RealODMatrix2(zoning);
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones Set of zones.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrix2 createUnitMatrix(Set<String> zones, Zoning zoning) {
		
		RealODMatrix2 odm = new RealODMatrix2(zoning);
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones Set of zones.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrix2 createUnitMatrix(Zoning zoning) {
		
		RealODMatrix2 odm = new RealODMatrix2(zoning);

		for (int i=0; i<odm.matrix.length; i++)
			for (int j=0; j<odm.matrix[0].length; j++)
				odm.matrix[i][j] = 1.0;
		
		return odm;
	}
	
	/**
	 * Deletes all inter-zonal flows to/from a particular zone (leaving only intra-zonal flows)
	 * @param zone Zone for which inter-zonal flows need to be deleted from the origin-destination matrix.
	 */
	public void deleteInterzonalFlows(String zone) {
		
		for (String origin: this.getOrigins())
			for (String destination: this.getDestinations())
				if (origin.equals(zone) && !destination.equals(zone) || !origin.equals(zone) && destination.equals(zone)) { //this will leave intra-zonal flow
				this.setFlow(origin, destination, 0);
			}
	}
	
	@Override
	public RealODMatrix2 clone() {

		RealODMatrix2 odm = new RealODMatrix2(this.zoning);
		
		for (int i=0; i<odm.matrix.length; i++)
			for (int j=0; j<odm.matrix[0].length; j++)
				odm.matrix[i][j] = this.matrix[i][j];

		return odm;
	}
	
	/**
	 * Saves the matrix into a csv file.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		LOGGER.info("Saving OD matrix to a csv file...");
		
		List<String> firstKeyList = this.getOrigins();
		List<String> secondKeyList = this.getDestinations();
	
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
				//for (String destination: secondKeyList) record.add(String.format("%.2f", this.getFlow(origin, destination)));
				for (String destination: secondKeyList) record.add(String.format("%d", (int) Math.round(this.getFlow(origin, destination))));
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
		
		LOGGER.debug("OD matrix saved to a csv file.");
	}
}
