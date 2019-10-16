package nismod.transport.air;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.air.InternodalPassengerDemand.Passengers;

/**
 * This class encapsulates international internodal (domestic airport to international airport) passenger data.
 * @author Milan Lovric
 */
public class InternationalInternodalPassengerDemand extends InternodalPassengerDemand{

	private final static Logger LOGGER = LogManager.getLogger(InternationalInternodalPassengerDemand.class);
	
	public InternationalInternodalPassengerDemand() {

		super();
	}

	/**
	 * Constructor that reads OD matrix from an input csv file.
	 * @param fileName Path to the input file.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public InternationalInternodalPassengerDemand(String fileName) throws FileNotFoundException, IOException {

		super();
		
		LOGGER.info("Reading international internodal passenger air demand from file: {}", fileName);

		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();

		if (!(keySet.contains("DomesticIATA") &&  keySet.contains("ForeignIATA") && keySet.contains("TotalPax"))) {

			LOGGER.error("Input file {} does not have a correct header.");
			parser.close();
			return;
			
		} else {
			
			for (CSVRecord record : parser) { 
				
				String firstIATA = record.get("DomesticIATA");
				String secondIATA = record.get("ForeignIATA");
				//String foreignRegion = record.get("ForeignRegion");
				//String countryCode = record.get("CountryCode");
				long totalPax = Long.parseLong(record.get("TotalPax"));
				
				//create map with passenger data
				Map<Passengers, Long> map = new EnumMap<>(Passengers.class);
				map.put(Passengers.TOTAL, totalPax);
				
				if (keySet.contains("ScheduledPax") && keySet.contains("CharterPax")) {
				
					long scheduledPax = Long.parseLong(record.get("ScheduledPax"));
					long charterPax = Long.parseLong(record.get("CharterPax"));
				
					if (totalPax != scheduledPax + charterPax) {
						LOGGER.warn("For IATA pair ({}, {}), total passengers do not equal sum of scheduled and charter passengers!", firstIATA, secondIATA);
					}

					map.put(Passengers.SCHEDULED, scheduledPax);
					map.put(Passengers.CHARTER, charterPax);
				}
				
				this.data.put(firstIATA, secondIATA, map);
			}
			parser.close();
		}
	
		LOGGER.debug("Finished reading air demand from file.");
	}
	
	/**
	 * Saves air passenger demand to an output file.
	 * @param year Year of the data.
	 * @param outputFile Output file name (with path).
	 */
	public void saveAirPassengerDemand(int year, String outputFile) {

		LOGGER.debug("Saving international air passenger demand to a file.");

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> outputHeader = new ArrayList<String>();
		outputHeader.add("year");
		outputHeader.add("DomesticIATA");
		outputHeader.add("ForeignIATA");
		outputHeader.add("TotalPax");
		
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(outputHeader);
			ArrayList<String> record = new ArrayList<String>();

			for (Object mk: this.data.keySet()) {
				
				record.clear();
				record.add(Integer.toString(year));
								
				String firstIATA = (String) ((MultiKey)mk).getKey(0); //domestic
				String secondIATA = (String) ((MultiKey)mk).getKey(1); //international
				record.add(firstIATA);
				record.add(secondIATA);
				
				Long totalPax = this.getDemand(firstIATA, secondIATA).get(Passengers.TOTAL);
				record.add(String.valueOf(totalPax));

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
	
	/*
	public void printDemand() {
		
		for (Object entry: data.entrySet()) {
		//	Map<Passengers, Long> map = (Map<Passengers, Long>) value; 
			System.out.println(entry);
		}
	}
	*/
}