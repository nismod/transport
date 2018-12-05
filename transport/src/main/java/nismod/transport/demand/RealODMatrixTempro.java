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

import org.apache.commons.collections4.keyvalue.MultiKey;
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
public class RealODMatrixTempro implements AssignableODMatrix {
	
	//public static final int MAX_ZONES = 7700;
	
	private final static Logger LOGGER = LogManager.getLogger(RealODMatrixTempro.class);
	
	private double[][] matrix;
	
	private Zoning zoning;
	
	/**
	 * Constructor for an empty OD matrix.
	 * Uses the maximum Tempro zone ID which will create a rather large matrix.
	 * @param zoning Zoning system.
	 */
	public RealODMatrixTempro(Zoning zoning) {
		
		this.zoning = zoning;
		int maxZones = zoning.getTemproIDToCodeMap().length;
		matrix = new double[maxZones][maxZones]; //[0][0] is not used to allow a direct fetch via tempro ID
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file. Can use both matrix and list format.
	 * @param fileName Path to the input file.
	 * @param zoning Zoning system.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public RealODMatrixTempro(String fileName, Zoning zoning) throws FileNotFoundException, IOException {
		
		LOGGER.info("Reading OD matrix from file: {}", fileName);
		
		this.zoning = zoning;
		int maxZones = zoning.getTemproIDToCodeMap().length;
		matrix = new double[maxZones][maxZones];
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();

		if (keySet.contains("origin") &&  keySet.contains("destination") && keySet.contains("flow")) { //use list format
			for (CSVRecord record : parser) { 
					String origin = record.get(0);
					String destination = record.get(1);
					double flow = Double.parseDouble(record.get(2));
					this.setFlow(origin, destination, flow);
			}
		} else { //use matrix format
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
		
		int i = this.zoning.getTemproCodeToIDMap().get(originZone);
		int j = this.zoning.getTemproCodeToIDMap().get(destinationZone);
		
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
		
		int i = this.zoning.getTemproCodeToIDMap().get(originZone);
		int j = this.zoning.getTemproCodeToIDMap().get(destinationZone);

		matrix[i][j] = flow;
	}
	
	/**
	 * Sets the flow for a given origin-destination pair.
	 * @param originCode Origin zone integer code.
	 * @param destinationCode Destination zone integer code.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(int originCode, int destinationCode, double flow) {
	
		/*
		if (flow > 0.0)		matrix.put(originZone, destinationZone, flow);
		//do not store zero flows into the matrix (skip zero flow or remove if already exists)
		else // flow == 0 
			if (this.getFlow(originZone, destinationZone) > 0.0)
				matrix.removeMultiKey(originZone, destinationZone);
		*/
		
		int i = originCode;
		int j = destinationCode;

		matrix[i][j] = flow;
	}
	
	/**
	 * Gets sum of all the (rounded) flows in the matrix.
	 * @return Sum of all the (rounded) flows in the matrix (i.e. number of trips).
	 */
	public int getTotalIntFlow() {
		
		int totalFlow = 0;
		
		for (int i=1; i<matrix.length; i++)
			for (int j=1; j<matrix[0].length; j++)
				totalFlow += matrix[i][j];
			
		return totalFlow;
	}
	
	/**
	 * Calculates the number of trips starting in each origin zone
	 * @return Number of trip starts.
	 */
	public HashMap<String, Integer> calculateTripStarts() {
		
		HashMap<String, Integer> tripStarts = new HashMap<String, Integer>();
		
		List<String> origins = this.getUnsortedOrigins();
		List<String> destinations = this.getUnsortedDestinations();
				
		for (String origin: origins)
			for (String destination: destinations) {
				Integer number = tripStarts.get(origin);
				if (number == null) number = 0;
				number += (int) Math.round(this.getFlow(origin, destination));
				tripStarts.put(origin, number);
			}
		
		return tripStarts;
	}
	
	/**
	 * Calculates the number of trips ending in each destination zone
	 * @return Number of trip ends.
	 */
	public HashMap<String, Integer> calculateTripEnds() {
		
		HashMap<String, Integer> tripEnds = new HashMap<String, Integer>();

		List<String> origins = this.getUnsortedOrigins();
		List<String> destinations = this.getUnsortedDestinations();
				
		for (String origin: origins)
			for (String destination: destinations) {
				Integer number = tripEnds.get(destination);
				if (number == null) number = 0;
				number += (int) Math.round(this.getFlow(origin, destination));
				tripEnds.put(destination, number);
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
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
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
		//put them into a list and sort them
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		
		return secondKeyList;
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(RealODMatrixTempro other) {
		
		double difference = 0.0;
		
		for (int i=1; i<matrix.length; i++)
			for (int j=1; j<matrix[0].length; j++)
				difference += Math.abs(matrix[i][j] - other.matrix[i][j]);

		return difference;
	}
	
	/**
	 * Scales matrix values with a scaling factor.
	 * @param factor Scaling factor.
	 */
	public void scaleMatrixValue(double factor) {
		
		for (int i=1; i<matrix.length; i++)
			for (int j=1; j<matrix[0].length; j++)
				matrix[i][j] *= factor;

	}
	
	/**
	 * Scales matrix values with another matrix (element-wise multiplication).
	 * @param scalingMatrix Scaling matrix.
	 */
	public void scaleMatrixValue(RealODMatrixTempro scalingMatrix) {
		
		for (int i=1; i<matrix.length; i++)
			for (int j=1; j<matrix[0].length; j++)
				matrix[i][j] *= scalingMatrix.matrix[i][j];		
	}
	
	/**
	 * Rounds OD matrix values.
	 */
	public void roundMatrixValues() {

		for (int i=1; i<matrix.length; i++)
			for (int j=1; j<matrix[0].length; j++)
				matrix[i][j] = Math.round(matrix[i][j]);	
	}
	
	/**
	 * Floor OD matrix values.
	 */
	public void floorMatrixValues() {

		for (int i=1; i<matrix.length; i++)
			for (int j=1; j<matrix[0].length; j++)
				matrix[i][j] = Math.floor(matrix[i][j]);	
	}
	
	/**
	 * Ceil OD matrix values.
	 */
	public void ceilMatrixValues() {

		for (int i=1; i<matrix.length; i++)
			for (int j=1; j<matrix[0].length; j++)
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
	 * @param zoning Zoning system.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrixTempro createUnitMatrix(List<String> origins, List<String> destinations, Zoning zoning) {
		
		RealODMatrixTempro odm = new RealODMatrixTempro(zoning);
		
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
	public static RealODMatrixTempro createUnitMatrix(List<String> zones, Zoning zoning) {
		
		RealODMatrixTempro odm = new RealODMatrixTempro(zoning);
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones Set of zones.
	 * @param zoning Zoning system.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrixTempro createUnitMatrix(Set<String> zones, Zoning zoning) {
		
		RealODMatrixTempro odm = new RealODMatrixTempro(zoning);
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix.
	 * @param zoning Zoning system.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrixTempro createUnitMatrix(Zoning zoning) {
		
		RealODMatrixTempro odm = new RealODMatrixTempro(zoning);
		Set<String> zones = zoning.getTemproCodeToIDMap().keySet();

		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1.0);
			
		return odm;
	}
	
	/**
	 * Sums the elements of a matrix subset (provided as two lists of origins and destinations).
	 * @param origins List of origin zones (a subset).
	 * @param destinations List of destination zones (a subset).
	 * @return Sum of the subset.
	 */
	public double sumMatrixSubset(List<String> origins, List<String> destinations) {
		
		double sum = 0.0;
		for (String origin: origins)
			for (String destination: destinations)
				sum += this.getFlow(origin, destination);
		
		return sum;
	}
	
	/**
	 * Creates tempro OD matrix from LAD OD matrix.
	 * @param ladODMatrix LAD to LAD OD matrix.
	 * @param baseTempro TEMPro ODMatrix used as weights to disaggregate LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * @return TEMPro based OD matrix.
	 */
	public static RealODMatrixTempro createTEMProFromLadMatrix(RealODMatrix ladODMatrix, RealODMatrixTempro baseTempro, Zoning zoning) {
		
		RealODMatrixTempro temproMatrix = new RealODMatrixTempro(zoning);
		
		for (MultiKey mk: ladODMatrix.getKeySet()) {
			String originLAD = (String) mk.getKey(0);
			String destinationLAD = (String) mk.getKey(1);
			
			double ladFlow = ladODMatrix.getFlow(originLAD, destinationLAD);
					
			//get tempro zones contained within originLAD and destinationLAD
			List<String> temproOrigins = zoning.getLADToListOfContainedZones().get(originLAD);
			List<String> temproDestinations = zoning.getLADToListOfContainedZones().get(destinationLAD);
			
			//sum all elements of the base tempro submatrix
			double temproSum = baseTempro.sumMatrixSubset(temproOrigins, temproDestinations);
			
			//disaggregate LAD flow using the weights of the underlying tempro submatrix
			for (String origin: temproOrigins)
				for (String destination: temproDestinations) {
					double temproFlow = 0.0;
					if (temproSum > 0.0) temproFlow = baseTempro.getFlow(origin, destination) * ladFlow / temproSum;
					temproMatrix.setFlow(origin, destination, temproFlow);
				}
		}
		
		return temproMatrix;
	}
	
	/**
	 * Creates tempro OD matrix from LAD OD matrix.
	 * @param ladODMatrix LAD to LAD OD matrix.
	 * @param baseTempro TEMPro ODMatrix used as weights to disaggregate LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * @return TEMPro based OD matrix.
	 */
	public static RealODMatrixTempro createTEMProFromLadMatrix(ODMatrix ladODMatrix, RealODMatrixTempro baseTempro, Zoning zoning) {
		
		RealODMatrixTempro temproMatrix = new RealODMatrixTempro(zoning);
		
		for (MultiKey mk: ladODMatrix.getKeySet()) {
			String originLAD = (String) mk.getKey(0);
			String destinationLAD = (String) mk.getKey(1);
			
			int ladFlow = ladODMatrix.getFlow(originLAD, destinationLAD);
					
			//get tempro zones contained within originLAD and destinationLAD
			List<String> temproOrigins = zoning.getLADToListOfContainedZones().get(originLAD);
			List<String> temproDestinations = zoning.getLADToListOfContainedZones().get(destinationLAD);
			
			//sum all elements of the base tempro submatrix
			double temproSum = baseTempro.sumMatrixSubset(temproOrigins, temproDestinations);
			
			//disaggregate LAD flow using the weights of the underlying tempro submatrix
			for (String origin: temproOrigins)
				for (String destination: temproDestinations) {
					double temproFlow = 0.0;
					if (temproSum > 0.0) temproFlow = baseTempro.getFlow(origin, destination) * ladFlow / temproSum;
					temproMatrix.setFlow(origin, destination, temproFlow);
				}
		}
		
		return temproMatrix;
	}
	
	/**
	 * Creates real-valued LAD OD matrix from real-valued TEMPro OD matrix.
	 * @param temproMatrix TEMPro ODMatrix which should be aggregated to LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * @return LAD based real-valued OD matrix.
	 */
	public static RealODMatrix createLadMatrixFromTEMProMatrix(RealODMatrixTempro temproMatrix, Zoning zoning) {
		
		RealODMatrix ladMatrix = new RealODMatrix();
			
		for (int i=1; i<temproMatrix.matrix.length; i++)
			for (int j=1; j<temproMatrix.matrix[0].length; j++) {
							
				String originTempro = zoning.getTemproIDToCodeMap()[i];
				String destinationTempro = zoning.getTemproIDToCodeMap()[j];
				
				if (originTempro == null || destinationTempro == null) continue; //this will only occur for test datasets
				
				double flow = temproMatrix.matrix[i][j];
				
				String originLAD = zoning.getZoneToLADMap().get(originTempro);
				String destinationLAD = zoning.getZoneToLADMap().get(destinationTempro);
				
				if (originLAD == null) LOGGER.warn("originLAD is null!");
				if (destinationLAD == null) LOGGER.warn("destinationLAD is null!");
				
				double flowLAD = ladMatrix.getFlow(originLAD, destinationLAD);
				flowLAD += flow;
				ladMatrix.setFlow(originLAD, destinationLAD, flowLAD);
			}
		
		return ladMatrix;
	}
	
	/**
	 * Deletes all inter-zonal flows to/from a particular zone (leaving only intra-zonal flows)
	 * @param zone Zone for which inter-zonal flows need to be deleted from the origin-destination matrix.
	 */
	public void deleteInterzonalFlows(String zone) {

		LOGGER.debug("Deleting inter-zonal flows from/to zone {}...", zone);
		
		List<String> origins = this.getUnsortedOrigins();
		List<String> destinations = this.getUnsortedDestinations();
				
		for (String origin: origins)
			for (String destination: destinations)
				if (origin.equals(zone) && !destination.equals(zone) || !origin.equals(zone) && destination.equals(zone)) { //this will leave intra-zonal flow
				this.setFlow(origin, destination, 0);
		}
		
		LOGGER.debug("Done deleting inter-zonal flows.");
	}
	
	/**
	 * Gets sum of all the flows.
	 * @return Sum of flows.
	 */
	public double getSumOfFlows() {
		
		double sumOfFlows = 0.0;
		for (int i=1; i<this.matrix.length; i++)
			for (int j=1; j<this.matrix[0].length; j++)
				sumOfFlows += this.matrix[i][j];
		
		return sumOfFlows;
	}
	
	@Override
	public RealODMatrixTempro clone() {

		RealODMatrixTempro odm = new RealODMatrixTempro(this.zoning);
		
		for (int i=1; i<odm.matrix.length; i++)
			for (int j=1; j<odm.matrix[0].length; j++)
				odm.matrix[i][j] = this.matrix[i][j];

		return odm;
	}
	
	/**
	 * Saves the matrix into a csv file. Uses a rectangular/matrix format.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		LOGGER.info("Saving OD matrix to a csv file...");
		
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
	
	/**
	 * Saves the matrix into a csv file. Uses a list format (origin, destination, flow).
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted2(String outputFile) {
		
		LOGGER.info("Saving OD matrix to a csv file...");
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("origin");
		header.add("destination");
		header.add("flow");
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
					double flow = this.getFlow(origin, destination);
					if (flow > 0) {
						record.clear();
						record.add(origin);
						record.add(destination);
						record.add(String.format("%d", (int) Math.round(flow)));
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
		
		LOGGER.debug("OD matrix saved to a csv file.");
	}
	
	/**
	 * Saves the matrix into a csv file. Uses a list format (origin, destination, flow) and number codes for zones.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted3(String outputFile) {
		
		LOGGER.info("Saving OD matrix to a csv file...");
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("origin");
		header.add("destination");
		header.add("flow");
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
					double flow = this.getFlow(origin, destination);
					if (flow > 0) {
						record.clear();
						record.add(String.valueOf(this.zoning.getTemproCodeToIDMap().get(origin)));
						record.add(String.valueOf(this.zoning.getTemproCodeToIDMap().get(destination)));
						record.add(String.format("%d", (int) Math.round(flow)));
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
		
		LOGGER.debug("OD matrix saved to a csv file.");
	}
}
