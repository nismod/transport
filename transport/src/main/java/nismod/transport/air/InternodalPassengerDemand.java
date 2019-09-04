package nismod.transport.air;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;
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

public abstract class InternodalPassengerDemand {

	private final static Logger LOGGER = LogManager.getLogger(InternodalPassengerDemand.class);

	//private MultiKeyMap<MultiKey<String>, Map<Passengers, Long>> data;
	protected MultiKeyMap data;

	public static enum Passengers {
		TOTAL,
		SCHEDULED,
		CHARTER
	}

	public InternodalPassengerDemand() {

		data = new MultiKeyMap();
	}
	
	public void printDemand() {
		
		for (Object entry: data.entrySet()) {
		//	Map<Passengers, Long> map = (Map<Passengers, Long>) value; 
			System.out.println(entry);
		}
	}
}