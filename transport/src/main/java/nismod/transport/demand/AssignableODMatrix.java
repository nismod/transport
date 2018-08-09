/**
 * 
 */
package nismod.transport.demand;

import java.util.List;

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
