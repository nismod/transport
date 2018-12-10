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

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.locationtech.jts.geom.Point;

import nismod.transport.zone.Zoning;

/**
 * Origin-destination matrix for passenger vehicles.
 * @author Milan Lovric
 *
 */
public class ODMatrixArray implements AssignableODMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(ODMatrixArray.class);
	
	private int[][] matrix;
	
	private Zoning zoning;
	
	/**
	 * Constructor.
	 * @param zoning Zoning system.
	 */
	public ODMatrixArray(Zoning zoning) {

		this.zoning = zoning;
		int maxZones = zoning.getLadIDToCodeMap().length;
		this.matrix = new int[maxZones][maxZones]; //[0][0] is not used to allow a direct fetch via LAD ID
	}
	
	/**
	 * Constructor that rounds the flows of a real-valued OD matrix. 
	 * @param realMatrix Origin-destination matrix with real-valued flows.
	 * @param zoning Zoning system.
	 */
	public ODMatrixArray(RealODMatrix realMatrix, Zoning zoning) {
		
		this.zoning = zoning;
		int maxZones = zoning.getLadIDToCodeMap().length;
		this.matrix = new int[maxZones][maxZones]; //[0][0] is not used to allow a direct fetch via LAD ID
		
		List<String> origins = realMatrix.getUnsortedOrigins();
		List<String> destinations = realMatrix.getUnsortedDestinations();
		
		for (String o: origins)
			for (String d: destinations) {
				int flow = (int) Math.round(realMatrix.getFlow(o, d));
				this.setFlow(o, d, flow);
			}
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file.
	 * @param fileName Path to the input file.
	 * @param zoning Zoning system.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public ODMatrixArray(String fileName, Zoning zoning) throws FileNotFoundException, IOException {
		
		LOGGER.info("Reading OD matrix from file: {}", fileName);

		this.zoning = zoning;
		int maxZones = zoning.getLadIDToCodeMap().length;
		this.matrix = new int[maxZones][maxZones]; //[0][0] is not used to allow a direct fetch via LAD ID
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();

		if (keySet.contains("origin") &&  keySet.contains("destination") && keySet.contains("flow")) { //use list format
			for (CSVRecord record : parser) { 
				String origin = record.get(0);
				String destination = record.get(1);
				int flow = Integer.parseInt(record.get(2));
				this.setFlow(origin, destination, flow);
			}
		} else { //use matrix format

			keySet.remove("origin");
			//System.out.println("keySet = " + keySet);
			int flow;
			for (CSVRecord record : parser) { 
				//System.out.println(record);
				//System.out.println("Origin zone = " + record.get(0));
				for (String destination: keySet) {
					//System.out.println("Destination zone = " + destination);
					flow = Integer.parseInt(record.get(destination));
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
	public int getFlow(String originZone, String destinationZone) {

		int i = this.zoning.getLadCodeToIDMap().get(originZone);
		int j = this.zoning.getLadCodeToIDMap().get(destinationZone);
		
		return this.matrix[i][j];
	}
	
	/**
	 * Gets the flow for a given origin-destination pair.
	 * @param originZoneID Origin zone ID.
	 * @param destinationZoneID Destination zone ID.
	 * @return Origin-destination flow.
	 */
	public int getFlow(int originZoneID, int destinationZoneID) {

		return this.matrix[originZoneID][destinationZoneID];
	}
	
	/**
	 * Gets the flow for a given origin-destination pair as a whole number.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public int getIntFlow(String originZone, String destinationZone) {
		
		return getFlow(originZone, destinationZone);
	}
	
	/**
	 * Gets the flow for a given origin-destination pair as a whole number.
	 * @param originZoneID Origin zone ID.
	 * @param destinationZoneID Destination zone ID.
	 * @return Origin-destination flow.
	 */
	public int getIntFlow(int originZoneID, int destinationZoneID) {
		
		return getFlow(originZoneID, destinationZoneID);
	}
	
	/**
	 * Sets the flow for a given origin-destination pair.
	 * @param originZoneID Origin zone ID.
	 * @param destinationZoneID Destination zone ID.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(int originZoneID, int destinationZoneID, int flow) {
		
		this.matrix[originZoneID][destinationZoneID] = flow;
	}
	
	/**
	 * Sets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(String originZone, String destinationZone, int flow) {
		
		int i = this.zoning.getLadCodeToIDMap().get(originZone);
		int j = this.zoning.getLadCodeToIDMap().get(destinationZone);
	
		this.matrix[i][j] = flow;
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
				number += this.getFlow(origin, destination);
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
				number += this.getFlow(origin, destination);
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
	 */
	public void printMatrixFormatted() {
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		//formatted print
		System.out.print("origin    "); for (String s: secondKeyList) System.out.printf("%10s",s);
		System.out.println();
		for (String o: firstKeyList) {
			System.out.printf("%-10s", o);
			for (String s: secondKeyList) System.out.printf("%10d", this.getFlow(o,s));
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
	 * Gets the sorted list of origins.
	 * @return List of origins.
	 */
	public List<String> getSortedOrigins() {
		
		Set<String> firstKey = zoning.getLadCodeToIDMap().keySet();
		
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
		
		Set<String> secondKey = zoning.getLadCodeToIDMap().keySet();
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
		
		Set<String> firstKey = zoning.getLadCodeToIDMap().keySet();
		//put them into a list
		List<String> firstKeyList = new ArrayList<String>(firstKey);
		
		return firstKeyList;
	}
	
	/**
	 * Gets the unsorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getUnsortedDestinations() {
		
		Set<String> secondKey = zoning.getLadCodeToIDMap().keySet();
		//put them into a list
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		
		return secondKeyList;
	}
	
	/**
	 * Gets sum of all the flows in the matrix.
	 * @return Sum of all the flows in the matrix (i.e. number of trips).
	 */
	public int getTotalFlow() {
		
		int totalFlow = 0;
		for (int i=1; i<this.matrix.length; i++)
			for (int j=1; j<this.matrix[0].length; j++) {
				double flow = this.matrix[i][j];
				if (flow > 0.0) totalFlow += flow;
			}
		
		return totalFlow;
	}
	
	/**
	 * Gets sum of all the flows in the matrix.
	 * @return Sum of all the flows in the matrix (i.e. number of trips).
	 */
	public int getTotalIntFlow() {
		
		return getTotalFlow();
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public int getAbsoluteDifference(ODMatrixArray other) {
		
		int difference = 0;
		
		List<String> firstKeyList = this.getUnsortedOrigins();
		List<String> secondKeyList = this.getUnsortedDestinations();
		
		for (String origin: firstKeyList)
			for (String destination: secondKeyList) {
				double thisFlow = this.getFlow(origin, destination);
				double otherFlow = other.getFlow(origin, destination);
				difference += Math.abs(thisFlow - otherFlow);
			}
	
		return difference;
	}
	

	/**
	 * Scales (and rounds) matrix values with a scaling factor.
	 * @param factor Scaling factor.
	 */
	public void scaleMatrixValue(double factor) {
		
		List<String> firstKeyList = this.getUnsortedOrigins();
		List<String> secondKeyList = this.getUnsortedDestinations();
		
		for (String origin: firstKeyList)
			for (String destination: secondKeyList) {
				int flow = this.getFlow(origin, destination);
				this.setFlow(origin, destination, (int) Math.round(flow * factor));
			}
	}
	
	/**
	 * Creates a unit OD matrix for given lists of origin and destination zones.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @param zoning Zoning system.
	 * @return Unit OD matrix.
	 */
	public static ODMatrixArray createUnitMatrix(List<String> origins, List<String> destinations, Zoning zoning) {
		
		ODMatrixArray odm = new ODMatrixArray(zoning);
		
		for (String origin: origins)
			for (String destination: destinations)
				odm.setFlow(origin, destination, 1);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones List of origin zones.
	 * @param zoning Zoning system.
	 * @return Unit OD matrix.
	 */
	public static ODMatrixArray createUnitMatrix(List<String> zones, Zoning zoning) {
		
		ODMatrixArray odm = new ODMatrixArray(zoning);
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones Set of origin zones.
	 * @param zoning Zoning system.
	 * @return Unit OD matrix.
	 */
	public static ODMatrixArray createUnitMatrix(Set<String> zones, Zoning zoning) {
		
		LOGGER.info("Creating the unit matrix for {} x {} zones.", zones.size(), zones.size());

		ODMatrixArray odm = new ODMatrixArray(zoning);
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1);
	
		LOGGER.debug("Done creating the unit matrix.");
		return odm;
	}
		
	/**
	 * Creates tempro OD matrix from LAD OD matrix.
	 * @param ladODMatrix LAD to LAD OD matrix.
	 * @param baseTempro TEMPro ODMatrix used as weights to disaggregate LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * @return TEMPro based OD matrix.
	 */
	public static ODMatrixArrayTempro createTEMProFromLadMatrix(ODMatrixArray ladODMatrix, ODMatrixArrayTempro baseTempro, Zoning zoning) {
		
		LOGGER.info("Dissaggregating LAD OD matrix to TEMPro OD matrix.");
		
		ODMatrixArrayTempro temproMatrix = new ODMatrixArrayTempro(zoning);
		
		List<String> origins = ladODMatrix.getUnsortedOrigins();
		List<String> destinations = ladODMatrix.getUnsortedDestinations();
		
		for (String originLAD: origins)
			for (String destinationLAD: destinations) {
			
			int ladFlow = ladODMatrix.getFlow(originLAD, destinationLAD);
					
			//get tempro zones contained within originLAD and destinationLAD
			List<String> temproOrigins = zoning.getLADToListOfContainedZones().get(originLAD);
			List<String> temproDestinations = zoning.getLADToListOfContainedZones().get(destinationLAD);
			
			//sum all elements of the base tempro submatrix
			int temproSum = baseTempro.sumMatrixSubset(temproOrigins, temproDestinations);
			
			//disaggregate LAD flow using the weights of the underlying tempro submatrix
			for (String origin: temproOrigins)
				for (String destination: temproDestinations) {
					int temproFlow = 0;
					if (temproSum > 0)
						temproFlow = (int) Math.round(1.0 * baseTempro.getFlow(origin, destination) / temproSum * ladFlow);
					temproMatrix.setFlow(origin, destination, temproFlow);
				}
		}
		
		LOGGER.debug("Finished disaggregating LAD OD matrix to TEMPro OD matrix.");
		return temproMatrix;
	}
	
	/**
	 * Creates LAD OD matrix from TEMPro OD matrix.
	 * @param temproMatrix TEMPro ODMatrix used as weights to disaggregate LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * @return LAD based OD matrix.
	 */
	public static ODMatrixArray createLadMatrixFromTEMProMatrix(ODMatrixArrayTempro temproMatrix, Zoning zoning) {
		
		LOGGER.info("Aggregating TEMPro OD matrix into LAD matrix.");
		
		ODMatrixArray ladMatrix = new ODMatrixArray(zoning);
				
		List<String> origins = temproMatrix.getUnsortedOrigins();
		List<String> destinations = temproMatrix.getUnsortedDestinations();
		
		for (String originTempro: origins)
			for (String destinationTempro: destinations) {

			int flow = temproMatrix.getFlow(originTempro, destinationTempro);
			
			String originLAD = zoning.getZoneToLADMap().get(originTempro);
			String destinationLAD = zoning.getZoneToLADMap().get(destinationTempro);
			
			if (originLAD == null) LOGGER.warn("originLAD is null!");
			if (destinationLAD == null) LOGGER.warn("destinationLAD is null!");
			
			int flowLAD = ladMatrix.getFlow(originLAD, destinationLAD);
			flowLAD += flow;
			ladMatrix.setFlow(originLAD, destinationLAD, flowLAD);
		}
		
		LOGGER.info("Finished aggregating TEMPro OD matrix into LAD matrix.");

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
	 * Saves the matrix into a csv file.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		LOGGER.info("Saving passenger OD matrix to a csv file.");
		
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
				for (String destination: secondKeyList) record.add(String.format("%d", this.getFlow(origin, destination)));
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
		
		LOGGER.debug("OD matrix saved to a file.");
	}
	
	/**
	 * Saves the matrix into a csv file. Uses a list format (origin, destination, flow).
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormattedList(String outputFile) {
		
		LOGGER.info("Saving OD matrix to a csv file using list format...");
		
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
					int flow = this.getFlow(origin, destination);
					if (flow > 0) {
						record.clear();
						record.add(origin);
						record.add(destination);
						record.add(String.format("%d", flow));
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
