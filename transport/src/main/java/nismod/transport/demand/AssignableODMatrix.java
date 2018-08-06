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
public interface AssignableODMatrix {
	
	/**
	 * Gets the flow for a given origin-destination pair as a whole number.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public int getIntFlow(String originZone, String destinationZone);
	
	/**
	 * Gets sum of all the flows in the matrix.
	 * @return Sum of all the flows in the matrix (i.e. number of trips).
	 */
	public int getTotalIntFlow();
	
	/**
	 * Gets the unsorted list of origins.
	 * @return List of origin zones.
	 */
	public List<String> getUnsortedOrigins();
	
	/**
	 * Gets the unsorted list of destinations.
	 * @return List of destination zones.
	 */
	public List<String> getUnsortedDestinations();
	
	/**
	 * Gets the sorted list of origins.
	 * @return List of origin zones.
	 */
	public List<String> getSortedOrigins();
	
	/**
	 * Gets the sroted list of destinations.
	 * @return List of destination zones.
	 */
	public List<String> getSortedDestinations();
}
