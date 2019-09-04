package nismod.transport.air;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.demand.ODMatrixMultiKey;

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

		if (!(keySet.contains("DomesticIATA") &&  keySet.contains("ForeignIATA") && keySet.contains("ForeignRegionCAA") &&  keySet.contains("CountryCodeISO") && keySet.contains("TotalPax") &&  keySet.contains("ScheduledPax") && keySet.contains("CharterPax"))) {

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
				long scheduledPax = Long.parseLong(record.get("ScheduledPax"));
				long charterPax = Long.parseLong(record.get("CharterPax"));
				
				if (totalPax != scheduledPax + charterPax) {
					LOGGER.warn("For IATA pair ({}, {}), total passengers do not equal sum of scheduled and charter passengers!", firstIATA, secondIATA);
				}
				
				//create map with passenger data
				Map<Passengers, Long> map = new EnumMap<>(Passengers.class);
				map.put(Passengers.TOTAL, totalPax);
				map.put(Passengers.SCHEDULED, scheduledPax);
				map.put(Passengers.CHARTER, charterPax);

				this.data.put(firstIATA, secondIATA, map);
			}
			parser.close();
		}
	
		LOGGER.debug("Finished reading air demand from file.");
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